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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;



@RunWith(MultiFsRunner.class)
public class WriteTest {

    Random random = new Random();
    Path root;
    
    @Test
    public void testReadWrite() throws Exception {
        
        Path source = Files.createFile(root.resolve("file"));
        byte[] contents = new byte[129];
        random.nextBytes(contents);
        
        Files.write(source, contents);
        assertArrayEquals(contents, Files.readAllBytes(source));
        assertEquals(contents.length, Files.size(source));
    }
    
    @Test
    public void testOverWriteSameSize() throws Exception {
        
        Path source = Files.createFile(root.resolve("file"));
        byte[] contents1 = new byte[128];
        random.nextBytes(contents1);
        byte[] contents2 = new byte[128];
        random.nextBytes(contents2);

        
        Files.write(source, contents1);
        Files.write(source, contents2);
        assertArrayEquals(contents2, Files.readAllBytes(source));
    }
    
    @Test
    public void testOverWriteSmallerSize() throws Exception {
        
        Path source = Files.createFile(root.resolve("file"));
        byte[] contents1 = new byte[128];
        random.nextBytes(contents1);
        byte[] contents2 = new byte[12];
        random.nextBytes(contents2);

        
        Files.write(source, contents1);
        Files.write(source, contents2);
        assertArrayEquals(contents2, Files.readAllBytes(source));
    }
    
    @Test
    public void testOverWriteLargerSize() throws Exception {
        
        Path source = Files.createFile(root.resolve("file"));
        byte[] contents1 = new byte[128];
        random.nextBytes(contents1);
        byte[] contents2 = new byte[278];
        random.nextBytes(contents2);

        
        Files.write(source, contents1);
        Files.write(source, contents2);
        assertArrayEquals(contents2, Files.readAllBytes(source));
    }
    
    @Test
    public void testMultipleStreamsTruncate() throws Exception {
        Path source = Files.createFile(root.resolve("file"));
        try(OutputStream f1 = Files.newOutputStream(source)) {
            f1.write(new byte[] {1,2,3,4});
            f1.flush();
            
            //this will truncate the file
            try(OutputStream f2 = Files.newOutputStream(source)) {
            }
            
            f1.write(new byte[] {5});
        }
        assertArrayEquals(new byte[] {0,0,0,0,5}, Files.readAllBytes(source));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testWriteFailsNoFollowOnSymlink() throws Exception {
        
        Path source = Files.createFile(root.resolve("file"));
        Path link = Files.createSymbolicLink(root.resolve("link"), source);
        byte[] contents = new byte[129];
        random.nextBytes(contents);
        try {
            Files.write(link, contents, LinkOption.NOFOLLOW_LINKS);
            fail();
        } catch(IOException e) {
            assertEquals("Too many levels of symbolic links (NOFOLLOW_LINKS specified)", e.getMessage());
        }
    }
    
    @Test
    public void testWriteFailsCreateNewExisting() throws Exception {
        
        Path file = Files.createFile(root.resolve("file"));
        byte[] contents = new byte[129];
        random.nextBytes(contents);
        Files.write(file, contents);
        
        try(OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @Test
    public void testWriteCreateExisting() throws Exception {
        
        Path file = Files.createFile(root.resolve("file"));
        byte[] contents = new byte[129];
        random.nextBytes(contents);
        Files.write(file, contents);
        
        try(OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
          
        } 
    }
    
}
