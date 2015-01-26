/*
 * Copyright 2015 Sean Bridges. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * 
 * 
 */

package com.github.sbridges.ephemeralfs;

import static com.github.sbridges.ephemeralfs.TestUtil.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;
import com.github.sbridges.ephemeralfs.junit.RunWithTypes;

@RunWithTypes(FsType.UNIX)
@RunWith(MultiFsRunner.class)
public class SecureDirectoryStreamTest {

    Path root;
    Path dir;
    Path moveTo;
    
    SecureDirectoryStream<Path> fixture;
    
    
    @Before
    public void setUp() throws IOException {
        dir = root.resolve("dir");
        Files.createDirectories(dir);
        fixture = (SecureDirectoryStream<Path>) Files.newDirectoryStream(dir);
        
        moveTo = root.resolve("moveTo");
        Files.move(dir, moveTo);
    }

    @After
    public void tearDown() throws IOException {
        fixture.close();
    }

    @Test
    public void testNewDirectoryStreamAfterMove() throws IOException {
        Files.createFile(moveTo.resolve("newFile"));
        
        try(SecureDirectoryStream<Path> newStream = fixture.newDirectoryStream(root.getFileSystem().getPath("."))) {
            assertFound(newStream, dir.resolve(".").resolve("newFile"));
        }
    }
    
    @Test
    public void testNewDirectoryStreamAfterMoveFailsSymlink() throws IOException {
        Files.createDirectory(moveTo.resolve("newDir"));
        Files.createSymbolicLink(moveTo.resolve("link"), moveTo.resolve("newDir"));
        
        
        try(SecureDirectoryStream<Path> newStream = fixture.newDirectoryStream(root.getFileSystem().getPath("link"), LinkOption.NOFOLLOW_LINKS)) {
            fail();
        } catch(FileSystemException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("Too many levels of symbolic links"));
        }
    }
    
    @Test
    public void testNewDirectoryStreamAfterMoveMultiLevel() throws IOException {
        Files.createDirectory(moveTo.resolve("child"));
        
        Files.createFile(moveTo.resolve("child").resolve("newFile"));
        
        try(SecureDirectoryStream<Path> newStream = fixture.newDirectoryStream(root.getFileSystem().getPath("child"))) {
            assertFound(newStream, dir.resolve("child").resolve("newFile"));
        }
    }
    
    @Test
    public void testRead() throws IOException {
        Files.write(moveTo.resolve("newFile"), new byte[] {1, 2, 3});
        
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        try(SeekableByteChannel channel = fixture.newByteChannel(root.getFileSystem().getPath("newFile"), openOptions)) {
            assertEquals(3, channel.size());
        }
    }
    
    @Test
    public void testReadDirectoryDeleted() throws IOException {
        
        Files.delete(moveTo);
        try {
            Files.write(moveTo.resolve("newFile"), new byte[] {1, 2, 3});
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
        
    }
    
    @Test
    public void testDeleteFile() throws IOException {
        Files.write(moveTo.resolve("newFile"), new byte[] {1, 2, 3});
        
        fixture.deleteFile(root.getFileSystem().getPath("newFile"));
        assertChildren(moveTo);
        
    }
    
    @Test
    public void testDeleteFileDeleteDirectory() throws IOException {
        Files.createDirectory(moveTo.resolve("newFile"));
        
        try {
            fixture.deleteFile(root.getFileSystem().getPath("newFile"));
        } catch(FileSystemException e) {
            assertEquals("newFile: Is a directory", e.getMessage());
        }
    }
        
    
    @Test
    public void testDeleteFileSymlinkToFile() throws IOException {
        Files.write(moveTo.resolve("newFile"), new byte[] {1, 2, 3});
        Files.createSymbolicLink(moveTo.resolve("symlink"), moveTo.resolve("newFile"));
        
        fixture.deleteFile(root.getFileSystem().getPath("symlink"));
        assertChildren(moveTo, moveTo.resolve("newFile"));
    }

    @Test
    public void testDeleteFileSymlinkToDirectory() throws IOException {
        Files.createDirectory(moveTo.resolve("newFile"));
        Files.createSymbolicLink(moveTo.resolve("symlink"), moveTo.resolve("newFile"));
        
        fixture.deleteFile(root.getFileSystem().getPath("symlink"));
        assertChildren(moveTo, moveTo.resolve("newFile"));
    }

    @Test
    public void testDeleteDirectorySymlinkToDirectory() throws IOException {
        Files.createDirectory(moveTo.resolve("newFile"));
        Files.createSymbolicLink(moveTo.resolve("symlink"), moveTo.resolve("newFile"));
        
        try {
            fixture.deleteDirectory(root.getFileSystem().getPath("symlink"));
            fail();
        } catch(FileSystemException e) {
            assertEquals("symlink: Not a directory", e.getMessage());
        }
        
    }
    
    @Test
    public void testDeleteDirWithFile() throws IOException {
        Files.write(moveTo.resolve("newFile"), new byte[] {1, 2, 3});
        
        try {
            fixture.deleteDirectory(root.getFileSystem().getPath("newFile"));
            fail();
        } catch(FileSystemException e) {
            assertEquals("newFile: Not a directory", e.getMessage());
        }
    }
    
    @Test
    public void testDeleteEmptyDir() throws IOException {
        Files.createDirectory(moveTo.resolve("newDirectory"));
        
        fixture.deleteDirectory(root.getFileSystem().getPath("newDirectory"));
        assertChildren(moveTo);
    }
    
    @Test
    public void testDeleteNonEmptyDir() throws IOException {
        Files.createDirectory(moveTo.resolve("newDirectory"));
        Files.createDirectory(moveTo.resolve("newDirectory").resolve("child"));
     
        try {
            fixture.deleteDirectory(root.getFileSystem().getPath("newDirectory"));
            fail();
        } catch(DirectoryNotEmptyException e) {
            //pass
        }
    }
    
    @Test
    public void testMove() throws IOException {
        Files.createDirectory(moveTo.resolve("oldName"));
        
        fixture.move(
                root.getFileSystem().getPath("oldName"),
                fixture,
                root.getFileSystem().getPath("newName")
                );
        
        assertChildren(moveTo, moveTo.resolve("newName"));
    }
    
    @Test
    public void testMoveTargetAlreadyExists() throws IOException {
        Files.createDirectory(moveTo.resolve("oldName"));
        Files.createDirectory(moveTo.resolve("newName"));
        
        fixture.move(
                root.getFileSystem().getPath("oldName"),
                fixture,
                root.getFileSystem().getPath("newName")
                );
        
        assertChildren(moveTo, moveTo.resolve("newName"));
    }

    @Test
    public void testGetFileAttributes() throws Exception {
        BasicFileAttributeView view = fixture.getFileAttributeView(BasicFileAttributeView.class);
        long lastModified = view.readAttributes().lastModifiedTime().toMillis();
        assertTrue(lastModified > 0);
        view.setTimes(FileTime.fromMillis(lastModified - 10_000), null, null);
        assertEquals(lastModified - 10_000, view.readAttributes().lastModifiedTime().toMillis());
        
        fixture.close();
        
        try {
            view.readAttributes();
            fail();
        } catch(ClosedDirectoryStreamException e) {
            //pass
        }
    }
    
    
    @Test
    public void testGetFileAttributesMoveAfterRead() throws Exception {
        BasicFileAttributeView view = fixture.getFileAttributeView(BasicFileAttributeView.class);
        Files.move(moveTo, moveTo.getParent().resolve("movedAgain"));
        long lastModified = view.readAttributes().lastModifiedTime().toMillis();
        assertTrue(lastModified > 0);
        view.setTimes(FileTime.fromMillis(lastModified - 10_000), null, null);
        assertEquals(lastModified - 10_000, view.readAttributes().lastModifiedTime().toMillis());
        
        fixture.close();
        
        try {
            view.readAttributes();
            fail();
        } catch(ClosedDirectoryStreamException e) {
            //pass
        }
    }
    
    @Test
    public void testGetFileAttributesOfFile() throws Exception {
        Files.createFile(moveTo.resolve("newFile"));
        
        BasicFileAttributeView view = fixture.getFileAttributeView(root.getFileSystem().getPath("newFile"), BasicFileAttributeView.class);
        long lastModified = view.readAttributes().lastModifiedTime().toMillis();
        assertTrue(lastModified > 0);
    }
    
    @Test
    public void testGetFileAttributesOfFileAfterMove() throws Exception {
        Files.createFile(moveTo.resolve("newFile"));
        
        BasicFileAttributeView view = fixture.getFileAttributeView(root.getFileSystem().getPath("newFile"), BasicFileAttributeView.class);
        Files.move(moveTo, moveTo.getParent().resolve("movedAgain"));
        long lastModified = view.readAttributes().lastModifiedTime().toMillis();
        assertTrue(lastModified > 0);
    }

    @Test
    public void testNewByteChannelNoFollowLink() throws Exception {
        Files.write(moveTo.resolve("newFile"), new byte[] {1, 2, 3});
        Files.createSymbolicLink(moveTo.resolve("link"), moveTo.resolve("newFile"));
        
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        openOptions.add(LinkOption.NOFOLLOW_LINKS);
        
        
        try(SeekableByteChannel channel = fixture.newByteChannel(root.getFileSystem().getPath("link"), openOptions)) {
            fail();
        } catch(IOException e) {
            assertEquals("Too many levels of symbolic links (NOFOLLOW_LINKS specified)", e.getMessage());
        }
    }
}
