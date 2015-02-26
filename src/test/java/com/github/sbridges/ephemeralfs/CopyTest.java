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

import static org.junit.Assert.*;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class CopyTest {

    final Random random = new Random();
    
    Path root;
    
    @Test
    public void testCopyFile() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Files.copy(source, dest);
        
        assertArrayEquals(contents, Files.readAllBytes(dest));
        Files.write(source, new byte[] {1});
        assertArrayEquals(contents, Files.readAllBytes(dest));
    }
    
    @Test
    public void testCopyFileLarge() throws Exception {
        byte[] contents = new byte[3 * 4096 + 18];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Files.copy(source, dest);
        
        assertArrayEquals(contents, Files.readAllBytes(dest));
        Files.write(source, new byte[] {1});
        assertArrayEquals(contents, Files.readAllBytes(dest));
    }
    
    @Test
    public void testCopyFileFailsIfTargetParentDoesNotExist() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest").resolve("child");
        Files.write(source, contents);
        
        try {
            Files.copy(source, dest);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
        
    }
    
    @Test
    public void testCopyFileFailsIfTargetParentIsFileExist() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest").resolve("child");
        Files.write(source, contents);
        Files.createFile(dest.getParent());
        
        try {
            Files.copy(source, dest);
            fail();
        } catch(FileSystemException e) {
            //pass
        }
        
    }    
    
    @Test
    public void testCopySameFile() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        
        Files.write(source, contents);
        
        Files.copy(source, source);
        
    }
    
    @Test
    public void testCopyFileReplace() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Files.createFile(dest);
        
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        
        assertArrayEquals(contents, Files.readAllBytes(dest));
        Files.write(source, new byte[] {1});
        assertArrayEquals(contents, Files.readAllBytes(dest));
    }

    
    @Test
    public void testCopyFileAtomicFails() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        try
        {
            Files.copy(source, dest, StandardCopyOption.ATOMIC_MOVE);
            fail();
        } catch(UnsupportedOperationException e) {
            //pass
        }
    }
   
    @Test
    public void testCopyFileExistingFails() throws Exception {
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.createFile(dest);
        Files.createFile(source);
        
        try {
            Files.copy(source, dest);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
        
    } 
    

    @Test
    public void testCopyFileSourceDoesNotExist() throws Exception {
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        
        try {
            Files.copy(source, dest);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
        
    }
    
    @Test
    public void testCopyDir() throws Exception {
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.createDirectories(source);
        
        
        Files.copy(source, dest);
        
        assertTrue(Files.exists(dest));
        assertTrue(Files.isDirectory(dest));
        assertTrue(Files.exists(source));
    }
    
    @Test
    public void testCopyDirFailsDestExists() throws Exception {
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);
        
        try
        {
            Files.copy(source, dest);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @Test
    public void testCopyDirFailsDestIfDestNotEmptyE() throws Exception {
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);
        Files.createDirectories(dest.resolve("destChild"));
        
        try
        {
            Files.copy(source, dest);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @Test
    public void testCopyDirFailsDestIfDestNotEmptyEvenWithReplaceExisting() throws Exception {
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);
        Files.createDirectories(dest.resolve("destChild"));
        
        try
        {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            fail();
        } catch(DirectoryNotEmptyException e) {
            //pass
        }
    }
    
    @Test
    public void testCopyDirDestExistsReplaceExisting() throws Exception {
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);
        
        
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        
        assertTrue(Files.exists(dest));
        assertTrue(Files.isDirectory(dest));
        assertTrue(Files.exists(source));
    }
    
    @Test
    public void testCopyDirDoesNotCopyChildren() throws Exception {
        
        Path source = root.resolve("source");
        Path child = source.resolve("child");
        Path dest = root.resolve("dest");
        Files.createDirectories(source);
        Files.createFile(child);
        
        
        
        Files.copy(source, dest);
        
        assertTrue(Files.isDirectory(dest));
        
        //files are not copied with the directory
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dest)) {
        	assertFalse(stream.iterator().hasNext());
        }
        
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testCopyAttributesUnix() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Thread.sleep(1000);
        
        Files.copy(source, dest);
        
        
        assertEquals(
                Files.getLastModifiedTime(dest).toMillis(),
                Files.getLastModifiedTime(source).toMillis()
                );
        
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testCopyAttributesLinux() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Thread.sleep(1000);
        
        Files.copy(source, dest);
        
        
        assertNotEquals(
                Files.getLastModifiedTime(dest).toMillis(),
                Files.getLastModifiedTime(source).toMillis()
                );
        
    }
 
    @Test
    public void testCopyAttributesCopyAttributes() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Thread.sleep(1000);
        
        Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES);
        
        
        assertEquals(
                Files.getLastModifiedTime(dest).toMillis(),
                Files.getLastModifiedTime(source).toMillis()
                );
        
    }

    @IgnoreIfNoSymlink
    @Test
    public void testCopyFileNoFollowLinks() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actualDir = root.resolve("actualDir");
        Files.createDirectory(actualDir);
        Path link = Files.createSymbolicLink(root.resolve("link"), actualDir);
        
        Path source = link.resolve("source");
        Path dest = link.resolve("dest");
        Files.write(source, contents);
        
        Files.copy(source, dest, LinkOption.NOFOLLOW_LINKS);
        
        assertArrayEquals(contents, Files.readAllBytes(dest));
        Files.write(source, new byte[] {1});
        assertArrayEquals(contents, Files.readAllBytes(dest));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testCopySymlink() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actual = root.resolve("actual");
        
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        
        Path source = link;
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Files.copy(source, dest);
        
        assertArrayEquals(contents, Files.readAllBytes(dest));
        Files.write(source, new byte[] {1});
        assertArrayEquals(contents, Files.readAllBytes(dest));
        assertFalse(Files.isSymbolicLink(dest));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testCopySymlinkNoFollow() throws Exception {
        byte[] contents = new byte[20];
        
        
        Path actual = root.resolve("actual");
        
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        
        Path source = link;
        Path dest = root.resolve("dest");
        Files.write(source, contents);
        
        Files.copy(source, dest, LinkOption.NOFOLLOW_LINKS);
        assertTrue(Files.isSymbolicLink(dest));
        
        assertArrayEquals(contents, Files.readAllBytes(dest));
        Files.write(source, new byte[] {1});
        assertTrue(Files.isSymbolicLink(source));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(actual));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(dest));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testCopySymlinkRelative() throws Exception {
        
        byte[] contents1 = new byte[20];
        random.nextBytes(contents1);
        byte[] contents2 = new byte[20];
        random.nextBytes(contents2);
        
        
        Path sourceDir = root.resolve("sourceDir");
        Path targetDir = root.resolve("targetDir");
        
        Files.createDirectory(targetDir);
        Files.createDirectory(sourceDir);
        
        Path actualSource = sourceDir.resolve("actual");
        Path actualTarget = targetDir.resolve("actual");
        
        Files.write(actualSource, contents1);
        Files.write(actualTarget, contents2);
        
        Path sourceLink = sourceDir.resolve("link");
        Path targetLink = targetDir.resolve("link");

        Files.createSymbolicLink(sourceLink, actualSource.getFileName());
        
        assertArrayEquals(contents1, Files.readAllBytes(sourceLink));
        
        Files.move(sourceLink, targetLink);
        
        assertArrayEquals(contents2, Files.readAllBytes(targetLink));
    }
    
    @Test
    public void testCopyFileToSelf() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        Files.copy(source, source);
        assertTrue(Files.exists(source));
    }
    
    @Test
    public void testCopyDirToSelf() throws Exception {
        Path source = Files.createDirectories(root.resolve("source"));
        Files.copy(source, source);
        assertTrue(Files.exists(source));
    }
    
}
