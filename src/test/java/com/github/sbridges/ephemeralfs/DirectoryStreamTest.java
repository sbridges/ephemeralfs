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

import static com.github.sbridges.ephemeralfs.TestUtil.assertFound;
import static org.junit.Assert.*;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class DirectoryStreamTest {

    Path root;
    
    @Test
    public void testSimple() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        
        Path child = dir.resolve("child");
        
        Files.createDirectories(child);
        
        
        try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            Files.move(dir, root.resolve("dir2"));
            assertFound(directoryStream, child);    
        } 
        
    }
    
    @Test
    public void testClose() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        
        Path child = dir.resolve("child");
        
        Files.createDirectories(child);
        
        DirectoryStream<Path> ds = Files.newDirectoryStream(dir);
        Iterator<Path> iterator = ds.iterator();
        ds.close();
        assertFalse(iterator.hasNext());
        
    }

    @Test
    public void testCloseAfterHasNext() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);

        Files.createDirectories(dir.resolve("child"));
        Files.createDirectories(dir.resolve("child2"));
        
        DirectoryStream<Path> ds = Files.newDirectoryStream(dir);
        Iterator<Path> iterator = ds.iterator();
        iterator.hasNext();
        ds.close();
        assertTrue(iterator.hasNext());
        //could be child or child2
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());
        
    }
    
    @Test
    public void testCloseAndNext() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);

        Files.createDirectories(dir.resolve("child"));
        Files.createDirectories(dir.resolve("child2"));
        
        DirectoryStream<Path> ds = Files.newDirectoryStream(dir);
        Iterator<Path> iterator = ds.iterator();
        
        ds.close();
        try
        {
            iterator.next();
            fail();
        } catch(NoSuchElementException e) {
            //pass
        }
        
        
    }
    
    @Test
    public void testCloseAndNextAfterHasNext() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);

        Files.createDirectories(dir.resolve("child"));
        Files.createDirectories(dir.resolve("child2"));
        
        DirectoryStream<Path> ds = Files.newDirectoryStream(dir);
        Iterator<Path> iterator = ds.iterator();
        iterator.hasNext();
        ds.close();
        iterator.next();
        try
        {
            iterator.next();
            fail();
        } catch(NoSuchElementException e) {
            //pass
        }
        
        
    }
    
    @Test
    public void testMultiple() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        
        Path child = dir.resolve("child");
        Path child2 = dir.resolve("child2");
        Path child3 = dir.resolve("child3");
        
        Files.createDirectories(child);
        Files.createDirectories(child2);
        Files.createDirectories(child3);
        
        assertFound(Files.newDirectoryStream(dir), child, child2, child3);
    }
    
    @Test
    public void testGlob() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        
        Path child = dir.resolve("1");
        Path child2 = dir.resolve("11");
        Path child3 = dir.resolve("2");
        
        Files.createDirectories(child);
        Files.createDirectories(child2);
        Files.createDirectories(child3);
        
        assertFound(Files.newDirectoryStream(dir, "1*"), child, child2);
    }
    
    @Test
    public void testEmpty() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        
        Files.createDirectories(dir);
        
        assertFound(Files.newDirectoryStream(dir));
    }
    
    @Test
    public void testNotFound() throws Exception {
        Path dir = root.resolve("dir");
        
        try {
            Files.newDirectoryStream(dir);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }

    }
    
    @Test
    public void testNotADirectory() throws Exception {
        Path file = root.resolve("dir");
        Files.createFile(file);
        
        try {
            Files.newDirectoryStream(file);
            fail();
        } catch(NotDirectoryException e) {
            //pass
        }

    }
    
    @Test
    public void testFailMultipleIterator() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
	        stream.iterator();
	        
	        try {
	            stream.iterator();
	            fail();
	        } catch(IllegalStateException e) {
	            //pass
	        }
        }
    }
    
    
    @Test
    public void testCanDeleteDirWithOpenDirStraem() throws Exception {
       
    	Path dir = root.resolve("dir");
    	Files.createDirectories(dir);
    	
        Path child = dir.resolve("child");
        Files.createDirectories(child);
        
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
        	Files.delete(child);
        	assertFalse(Files.exists(child));
        	Files.delete(dir);
       }
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testCanDeleteDirWithOpenDirStreamOnChild() throws Exception {
       
    	Path dir = root.resolve("dir");
    	Files.createDirectories(dir);
    	
        Path child = dir.resolve("child");
        Files.createDirectories(child);
        
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(child)) {
        	Files.delete(child);
        	assertFalse(Files.exists(child));
        	Files.delete(dir);
       }
        
    }
    

    
}
