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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class BasicFileAttributesTest {

    
    Path root;
    
    @Test
    public void testSetLastModifiedTime() throws Exception {
        Path path = root.resolve("test");
        Files.createFile(path);
        long newModified = roundToSeconds(System.currentTimeMillis()) - 10000;
        Files.setLastModifiedTime(path, FileTime.fromMillis(newModified));
        assertEquals(newModified, Files.getLastModifiedTime(path).toMillis());

    }
    
    @Test
    public void testSetLastModifiedTimeAfterMove() throws Exception {
        Path path = root.resolve("test");
        Files.createFile(path);
        BasicFileAttributeView view = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        
        Path movedTo = root.resolve("test2");
        Files.move(path, movedTo);
        long newModified = roundToSeconds(System.currentTimeMillis()) - 10000;
        
        try {
            view.setTimes(FileTime.fromMillis(newModified), null, null);
            fail();
        } catch (NoSuchFileException e) {
            //pass
        }
        
    }
 
    
    @Test
    public void testSetLastModifiedTimeThroughWrite() throws Exception {
        long now = roundToSeconds(System.currentTimeMillis());
        Path path = root.resolve("test");
        Files.createFile(path);
        
        //set the last modified to the past
        long newModified = now - 10000;
        Files.setLastModifiedTime(path, FileTime.fromMillis(newModified));
        
        //write, this should update last modified again
        Files.write(path, new byte[] {5});
        
        assertTrue(Files.getLastModifiedTime(path).toMillis() >= now);
    }

    @Test
    public void testSetLastAccessNonExistent() throws Exception {
        Path path = root.resolve("test");
        long newModified = roundToSeconds(System.currentTimeMillis()) - 10000;
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(newModified));
            fail();
        } catch(NoSuchFileException e) {
            //ignore
        }

    }
    
    @Test
    public void testCheckAccessNonExistent() throws Exception {
        Path path = root.resolve("path");
        
        assertFalse(Files.isReadable(path));
        assertFalse(Files.isWritable(path));
        assertFalse(Files.exists(path));
        assertFalse(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isExecutable(path));
    }

    @Test
    public void testCheckAccessDir() throws Exception {
        Path path = root.resolve("path");
        Files.createDirectory(path);
        
        assertTrue(Files.isReadable(path));
        assertTrue(Files.isWritable(path));
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isExecutable(path));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testCheckAccessFilePosix() throws Exception {
        Path path = root.resolve("path");
        Files.createFile(path);
        
        assertTrue(Files.isReadable(path));
        assertTrue(Files.isWritable(path));
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isExecutable(path));
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testCheckAccessFileWindows() throws Exception {
        Path path = root.resolve("path");
        Files.createFile(path);
        
        assertTrue(Files.isReadable(path));
        assertTrue(Files.isWritable(path));
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isExecutable(path));
    }

    
    @IgnoreIfNoSymlink
    @Test
    public void testCheckAccessSymlinkFileExists() throws Exception {
        Path path = root.resolve("path");
        Path otherPath = root.resolve("otherPath");
        Files.createSymbolicLink(path, otherPath);
        Files.createFile(otherPath);
        
        assertTrue(Files.isReadable(path));
        assertTrue(Files.isWritable(path));
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isExecutable(path));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testCheckAccessSymlinkDirExists() throws Exception {
        Path path = root.resolve("path");
        Path otherPath = root.resolve("otherPath");
        Files.createSymbolicLink(path, otherPath);
        Files.createDirectories(otherPath);
        
        assertTrue(Files.isReadable(path));
        assertTrue(Files.isWritable(path));
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isExecutable(path));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testCheckAccessSymlinkNotExists() throws Exception {
        Path path = root.resolve("path");
        Files.createSymbolicLink(path, root.resolve("otherPath"));
        
        assertFalse(Files.isReadable(path));
        assertFalse(Files.isWritable(path));
        assertFalse(Files.exists(path));
        assertTrue(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isExecutable(path));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testCheckAccessExecutableFile() throws Exception {
        Path path = root.resolve("path");
        Files.createFile(path, PosixFilePermissions.asFileAttribute(
                EnumSet.allOf(PosixFilePermission.class))
                );
        
        assertTrue(Files.isReadable(path));
        assertTrue(Files.isWritable(path));
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(path, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isExecutable(path));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testCheckAccessCopyOfExecutableFileWindows() throws Exception {
        Path path = root.resolve("path");
        Files.createFile(path, PosixFilePermissions.asFileAttribute(
                EnumSet.allOf(PosixFilePermission.class))
                );
        Path copy = root.resolve("copy");
        Files.copy(path, copy);
        
        assertTrue(Files.isReadable(copy));
        assertTrue(Files.isWritable(copy));
        assertTrue(Files.exists(copy));
        assertTrue(Files.exists(copy, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isExecutable(copy));
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testPosixFilePermissionsFailWindows() throws Exception {
        Path path = root.resolve("path");
        try {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(
                    EnumSet.allOf(PosixFilePermission.class))
               );
            fail();
        } catch(UnsupportedOperationException e) {
            //pass
        }
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testCheckAccessCopyOfReadOnlyFile() throws Exception {
        Path path = root.resolve("path");
        Files.createFile(path, PosixFilePermissions.asFileAttribute(
                EnumSet.of(PosixFilePermission.OWNER_READ))
                );
        Path copy = root.resolve("copy");
        Files.copy(path, copy);
        
        assertTrue(Files.isReadable(copy));
        assertFalse(Files.isWritable(copy));
        assertTrue(Files.exists(copy));
        assertTrue(Files.exists(copy, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isExecutable(copy));
    }
    
    
    

    @Test
    public void testGetAttributesNotExist() throws Exception {
        Path path = root.resolve("path");
        try {
            Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
        try {
            Files.readAttributes(path, BasicFileAttributes.class);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }

    @Test
    public void testGetAttributesFileExists() throws Exception {
        Path path = root.resolve("path");
        Files.createFile(path);
        assertNotNull(Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
        assertNotNull(Files.readAttributes(path, BasicFileAttributes.class));
    }
    
    @Test
    public void testGetAttributesDirExists() throws Exception {
        Path path = root.resolve("path");
        Files.createDirectory(path);
        assertNotNull(Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
        assertNotNull(Files.readAttributes(path, BasicFileAttributes.class));
    }

    @IgnoreIfNoSymlink
    @Test
    public void testGetAttributesSymlinkBothExist() throws Exception {
        Path path = root.resolve("path");
        Path other = root.resolve("other");
        Files.createFile(other);
        //sleep to allow the file time to change
        Thread.sleep(1001);
        Files.createSymbolicLink(path, other);
        
        BasicFileAttributes linkAttributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        BasicFileAttributes fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        
        assertNotEquals(linkAttributes.creationTime().toMillis(), fileAttributes.creationTime().toMillis());
        
        assertFalse(linkAttributes.isOther());
        assertTrue(linkAttributes.isSymbolicLink());
        assertEquals(fileAttributes.size(), 0);
        assertTrue(linkAttributes.size() > 0);
        assertNotNull(linkAttributes.fileKey());
        assertNotEquals(linkAttributes.fileKey(), fileAttributes.fileKey());
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testGetAttributesSymlinkTargetChanges() throws Exception {
        
        Path target1 = root.resolve("target1");
        Path target2 = root.resolve("target2");
        Path link = root.resolve("link");
        
        Files.write(target1, new byte[] {1});
        Files.write(target2, new byte[] {2});

        Files.createSymbolicLink(link, target1);
        
        BasicFileAttributes fileAttributes = Files.readAttributes(link, BasicFileAttributes.class);
        
        assertEquals(1, fileAttributes.size());
        
        Files.delete(target1);
        Files.move(target2, target1);
        
        assertEquals(1, fileAttributes.size());
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testGetAttributesSymlinkTargetDoesNotExist() throws Exception {
        Path path = root.resolve("path");
        Path other = root.resolve("other");
        
        Files.createSymbolicLink(path, other);
        
        assertNotNull(Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
        
        try {
            Files.readAttributes(path, BasicFileAttributes.class);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    
    
    @IgnoreIfNoSymlink
    @Test
    public void testMultiLevelSymlink() throws Exception {
        Path child1 = root.resolve("child1");
        Path child2 = root.resolve("child2");
        
        Files.createDirectories(child1);
        
        Path target = child1.resolve("actual");
        Files.createFile(target);
        
        Path link = child1.resolve("link");
        Files.createSymbolicLink(link, target);

        Files.move(child1, child2);
        
        Path movedLink = child2.resolve("link");
        
        assertFalse(Files.exists(movedLink));
        assertNotNull(Files.readAttributes(movedLink, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
    
        try {
            Files.readAttributes(movedLink, BasicFileAttributes.class);
            fail();
        } catch(NoSuchFileException e) {
            
        }
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testSizeDirectoryWindows() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectory(dir);
        assertEquals(0, Files.getFileAttributeView(dir, BasicFileAttributeView.class).readAttributes().size());
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testSizeDirectoryPosix() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectory(dir);
        assertTrue(Files.getFileAttributeView(dir, BasicFileAttributeView.class).readAttributes().size() > 0);
    }

    
    @Test
    public void testTypesDir() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectory(dir);
        BasicFileAttributes att = Files.getFileAttributeView(dir, BasicFileAttributeView.class).readAttributes();
        assertTrue(att.isDirectory());
        assertFalse(att.isSymbolicLink());
        assertFalse(att.isRegularFile());
        assertFalse(att.isOther());
    }

    @Test
    public void testTypesFile() throws Exception {
        Path file = root.resolve("file");
        Files.createFile(file);
        BasicFileAttributes att = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();
        assertFalse(att.isDirectory());
        assertFalse(att.isSymbolicLink());
        assertTrue(att.isRegularFile());
        assertFalse(att.isOther());
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testTypesSymlink() throws Exception {
        Path link = root.resolve("link");
        Files.createSymbolicLink(link, root.resolve("nonExistent"));
        BasicFileAttributes att = Files.getFileAttributeView(link, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
        assertFalse(att.isDirectory());
        assertTrue(att.isSymbolicLink());
        assertFalse(att.isRegularFile());
        assertFalse(att.isOther());
    }
    
    private long roundToSeconds(long timeMs) {
        return (timeMs / 1000) * 1000;
    } 
}
