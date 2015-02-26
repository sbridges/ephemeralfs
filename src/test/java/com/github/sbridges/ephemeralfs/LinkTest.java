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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotLinkException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class LinkTest {

    Path root;
    
    @Test
    public void testHardLinkDestDoesNotExist() throws Exception {
        Path source = root.resolve("source");
        Path other = root.resolve("other");
        
        try {
            Files.createLink(other, source);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testSymLinkDestDoesNotExist() throws Exception {
        Path source = root.resolve("source");
        Path other = root.resolve("other");

        Files.createSymbolicLink(other, source);
        
        assertFalse(Files.isDirectory(other));
        assertFalse(Files.isReadable(other));
        assertFalse(Files.isWritable(other));
        assertFalse(Files.exists(other));
        
        assertEquals(source, Files.readSymbolicLink(other));
        
        assertEquals(Arrays.asList(other), list(root));
        try {
            Files.readAllBytes(other);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @Test
    public void testHardLinkParentDoesNotExist() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        
        Path other =  root.resolve("level1").resolve("level2");
        
        try {
            Files.createLink(other, source);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testSymbolicLinkParentDoesNotExist() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        
        Path other =  root.resolve("level1").resolve("level2");
        
        try {
            Files.createSymbolicLink(other, source);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @Test
    public void testHardLinkFailsAlreadyExists() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        Path other = Files.createFile(root.resolve("other"));
        
        try {
            Files.createLink(source, other);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @Test
    public void testHardLinkFailsToSelf() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        try {
            Files.createLink(source, source);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testSymbolicLinkFailsAlreadyExists() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        Path other = Files.createFile(root.resolve("other"));
        
        try {
            Files.createSymbolicLink(source, other);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testSymbolicLinkFailsToSelf() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        try {
            Files.createSymbolicLink(source, source);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }   
    
    @Test
    public void testHardLinkSameFile() throws Exception {
        byte[] contents = new byte[] {42};
        Path source = Files.write(root.resolve("source"), contents);
        Path link = Files.createLink(root.resolve("link"), source);
        
        assertTrue(Files.isSameFile(source, link));
    }
    
    @Test
    public void testHardLinkReadWrite() throws Exception {
        byte[] contents = new byte[] {42};
        Path source = Files.write(root.resolve("source"), contents);
        Path link = Files.createLink(root.resolve("link"), source);
        
        assertArrayEquals(contents, Files.readAllBytes(link));
        
        byte[] newContents = new byte[] {1};
        Files.write(link, newContents);
        
        assertArrayEquals(newContents, Files.readAllBytes(source));
    }
    
    @Test
    public void testDeleteHardLink() throws Exception {
        byte[] contents = new byte[] {42};
        Path source = Files.write(root.resolve("source"), contents);
        Path link = Files.createLink(root.resolve("link"), source);
        
        Files.delete(link);
        assertFalse(Files.exists(link));
        assertTrue(Files.exists(source));
    }    
    
    @IgnoreIfNoSymlink
    @Test
    public void testHardLinkReadWriteNoFollowSymlink() throws Exception {
        byte[] contents = new byte[] {42};
        Path source = Files.write(root.resolve("source"), contents);
        Path link = Files.createSymbolicLink(root.resolve("link"), source);
        
        try {
            Files.newByteChannel(link, LinkOption.NOFOLLOW_LINKS);
        } catch(IOException e) {
            assertEquals("Too many levels of symbolic links (NOFOLLOW_LINKS specified)", e.getMessage());
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testRecusiveSymLinks() throws Exception {
        Path link1 = root.resolve("link1");
        Path link2 = root.resolve("link2");
        
        Files.createSymbolicLink(link1, link2);
        Files.createSymbolicLink(link2, link1);
        
        try {
            Files.newByteChannel(link1);
            fail();
        } catch(IOException e) {
            assertTrue(e.getMessage().contains(" Too many levels of symbolic links"));
        }
    }
    
    @Test
    public void testHardLinkReadWriteAfterMove() throws Exception {
        byte[] contents = new byte[] {42};
        Path source = Files.write(root.resolve("source"), contents);
        Path link = Files.createLink(root.resolve("link"), source);
        
        assertArrayEquals(contents, Files.readAllBytes(link));
        
        Path moved = Files.move(source, root.resolve("moved"));
        
        
        byte[] newContents = new byte[] {1};
        Files.write(link, newContents);
        
        assertArrayEquals(newContents, Files.readAllBytes(moved));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testSymLinkReadWriteSameFile() throws Exception {
        byte[] contents = new byte[] {42};
        Path source = Files.write(root.resolve("source"), contents);
        Path link = Files.createSymbolicLink(root.resolve("link"), source);
        
        assertFalse(Files.isDirectory(link));
        
        assertArrayEquals(contents, Files.readAllBytes(link));
        
        byte[] newContents = new byte[] {1};
        Files.write(link, newContents);
        
        assertArrayEquals(newContents, Files.readAllBytes(source));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testSymLinkReadAfterMove() throws Exception {
        byte[] contents1 = new byte[] {1};
        byte[] contents2 = new byte[] {2};
        Path source1 = Files.write(root.resolve("source1"), contents1);
        Path source2 = Files.write(root.resolve("source2"), contents2);
        
        Path link = Files.createSymbolicLink(root.resolve("link"), source1);
        
        assertArrayEquals(contents1, Files.readAllBytes(link));
        
        Files.delete(source1);
        Files.move(source2, source1);
        
        assertArrayEquals(contents2, Files.readAllBytes(link));
    }
    
    @Test
    public void testHardLinkDirFails() throws Exception {
        Path source = Files.createDirectory(root.resolve("source"));
        Path other = root.resolve("other");
        
        try {
            Files.createLink(other, source);
            fail();
        } catch(FileSystemException e) {
        	//pass
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testSymLinkDir() throws Exception {
        Path source = Files.createDirectory(root.resolve("source"));
        Path other = root.resolve("other");
        
        Files.createSymbolicLink(other, source);
        assertTrue(Files.isDirectory(other));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testResolveThroughSymlink() throws Exception {
        // root/link -> root/other
        // 
        Path link = root.resolve("link");
        Path other = root.resolve("other");
        Path otherChild = other.resolve("otherChild");
        Path fileInOtherChild = otherChild.resolve("file");
        
        byte[] contents = new byte[] {12};
        Files.createDirectories(otherChild);
        Files.write(fileInOtherChild, contents);
        
        
        Files.createSymbolicLink(link, other);
        
        assertEquals(list(link), Arrays.asList(link.resolve("otherChild")));
        
        assertArrayEquals(contents, Files.readAllBytes(link.resolve("otherChild").resolve("file")));
    }

    
    @Test
    public void testReadSymbolicLinkNoSymbolicLink() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Files.createDirectories(notSymLink);
        try {
            Files.readSymbolicLink(notSymLink);
            fail();
        } catch(NotLinkException e) {
            //pass
        }
    }
    
    @Test
    public void testReadSymbolicLinkNotExists() throws Exception {
        Path notExists = root.resolve("notExists");
        
        try {
            Files.readSymbolicLink(notExists);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
        
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testReadSymbolicLink() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Path symLink = root.resolve("symLink");
        
        Files.createSymbolicLink(symLink, notSymLink);
        assertEquals(notSymLink, Files.readSymbolicLink(symLink));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testReadSymbolicLinkContent() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Files.write(notSymLink, new byte[] {1});
        Path symLink = root.resolve("symLink");
        
        Files.createSymbolicLink(symLink, notSymLink);
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(symLink));
    }

    @IgnoreIfNoSymlink
    @Test
    public void testReadTwoLevelSymbolicLinkContent() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Files.write(notSymLink, new byte[] {1});
        Path symLink1 = root.resolve("symLink1");
        Path symLink2 = root.resolve("symLink2");
        
        Files.createSymbolicLink(symLink1, symLink2);
        Files.createSymbolicLink(symLink2, notSymLink);
        
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(symLink1));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(symLink2));
    }

    @IgnoreIfNoSymlink
    @Test
    public void testReadThreeLevelSymbolicLinkContent() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Files.write(notSymLink, new byte[] {1});
        Path symLink1 = root.resolve("symLink1");
        Path symLink2 = root.resolve("symLink2");
        Path symLink3 = root.resolve("symLink3");
        
        Files.createSymbolicLink(symLink1, symLink2);
        Files.createSymbolicLink(symLink2, symLink3);
        Files.createSymbolicLink(symLink3, notSymLink);
        
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(symLink3));
    }

    @IgnoreIfNoSymlink
    @Test
    public void testReadMultiLevelSymbolicLinkContentNotExists() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Path symLink1 = root.resolve("symLink1");
        Path symLink2 = root.resolve("symLink2");
        
        Files.createSymbolicLink(symLink1, symLink2);
        Files.createSymbolicLink(symLink2, notSymLink);
        
        try {
            assertArrayEquals(new byte[] {1}, Files.readAllBytes(symLink2));
            fail();
        } catch(NoSuchFileException e){
           //pass
        } 
        
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testDeleteSymLinkNotExist() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Path symLink = root.resolve("symLink");
        
        Files.createSymbolicLink(symLink, notSymLink);
        Files.delete(symLink);
        
        
        assertFalse(Files.exists(symLink));
        assertFalse(Files.exists(notSymLink));
    }    
    
    @IgnoreIfNoSymlink
    @Test
    public void testDeleteSymLinkExist() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Files.write(notSymLink, new byte[] {0});
        Path symLink = root.resolve("symLink");
        
        Files.createSymbolicLink(symLink, notSymLink);
        Files.delete(symLink);
        
        
        assertFalse(Files.exists(symLink));
        assertTrue(Files.exists(notSymLink));
    }    
    
    @IgnoreIfNoSymlink
    @Test
    public void testMultiLevelSymLink() throws Exception {
        Path child1 = root.resolve("child1");
        Path child2 = root.resolve("child2");
        Files.createDirectories(child1);
        Files.createDirectories(child2);
        
        Path target = child1.resolve("actual");
        Files.createFile(target);
        
        
        Path link = child2.resolve("link");
        
        Files.createSymbolicLink(link, target);
        
        assertEquals(0, Files.readAllBytes(link).length);
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testReadRelativeSymbolicLink() throws Exception {
        Path notSymLink = root.resolve("notSymLink");
        Path symLink = root.resolve("symLink");
        
        Files.createSymbolicLink(symLink, notSymLink.getFileName());
        assertEquals(notSymLink.getFileName(), Files.readSymbolicLink(symLink));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testCreateSymLinkFileAlreadyExists() throws Exception {
        Path real = root.resolve("real");
        Path link = root.resolve("link");
        
        Files.createFile(real);
        try {
            Files.createSymbolicLink(real, link.getFileName());
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
        
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testCreateFileSymLinkAlreadyExists() throws Exception {
        Path real = root.resolve("real");
        Path link = root.resolve("link");
        
        Files.createSymbolicLink(real, link.getFileName());
        try {
            Files.createFile(real);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testCopySymbolicLinkReplace() throws IOException {

        Path file1 = root.resolve("file1");
        Path file2 = root.resolve("file2");
        Files.createFile(file1);
        Files.createFile(file2);

        Path target = file1.resolveSibling("target");

        Files.createSymbolicLink(target, file1);

        Files.copy(file2, target, REPLACE_EXISTING);

        assertTrue(Files.exists(target));
        assertFalse(Files.isSymbolicLink(target));
    }
    
    private List<Path> list(Path path) throws IOException {
       List<Path> answer = new ArrayList<>();
       try(DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
           for(Path p : ds) {
               answer.add(p);
           }
       }
       return answer;
       
        
    }
    
}
