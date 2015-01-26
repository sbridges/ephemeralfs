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

import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class DirectoryWalkTest {

    Path root;
    
    @Test
    public void testFileNotFound() throws Exception {
        Path notExistent = root.resolve("test");
        assertFalse(Files.exists(notExistent));
    }
    
    @Test
    public void testCreateRoot() throws Exception {
        try {
            Files.createDirectory(root.getRoot());
            fail();
        } catch(FileSystemException e) {
            //pass
        }
    }
    
    @Test
    public void testCreateDirectory() throws Exception {
        Path child = Files.createDirectory(root.resolve("test"));
        assertTrue(Files.exists(child));
        assertTrue(Files.isDirectory(child));
    }
    
    @Test
    public void testCreateDirectoryParentDoesNotExist() throws Exception {
        try
        {
            Files.createDirectory(root.resolve("test").resolve("test2"));
            fail();
        } catch(NoSuchFileException e) {
            
        }
    }
    
    @Test
    public void testCreateDirectoryParentDoesNotExistWithFs() throws Exception {
        try
        {
            Path toCreate = root.resolve("test").resolve("test2");
            toCreate.getFileSystem().provider().createDirectory(toCreate);
            fail();
        } catch(NoSuchFileException e) {
            
        }
    }
    
    @Test
    public void testCreateDirectoriesTwice() throws Exception {
        Path toCreate = root.resolve("test");
        assertEquals(toCreate, Files.createDirectories(toCreate));
        assertEquals(toCreate, Files.createDirectories(toCreate));
    }
    
    @Test
    public void testCreateDirectiesParentDoesNotExist() throws Exception {
        Path toCreate = root.resolve("test").resolve("test2");
        assertEquals(toCreate, Files.createDirectories(toCreate));
    }
    
    @Test
    public void testCreateDirectoryParentIsNotDir() throws Exception {
        Path parent = root.resolve("test");
        Files.createFile(parent);
        try
        {
            Files.createDirectory(parent.resolve("test2"));
            fail();
        } catch(FileSystemException e) {
            //pass
        }
    }

    @Test
    public void testGetAttributesThenMoveFile() throws Exception {
        long start = System.currentTimeMillis();
        Path parent = root.resolve("test");
        Files.createFile(parent);
        BasicFileAttributeView attributes = Files.getFileAttributeView(parent, BasicFileAttributeView.class);
        assertTrue(attributes.readAttributes().creationTime().toMillis() > start - 6000);
        Files.move(parent, parent.resolveSibling("test2"));
        try
        {
            attributes.readAttributes();
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
        
    }
    
    @Test
    public void testCreateFile() throws Exception {
        Path child = Files.createFile(root.resolve("test"));
        assertTrue(Files.exists(child));
        assertFalse(Files.isDirectory(child));
    }
    
    
    @IgnoreUnless(FsType.UNIX)
    @Test
    public void testCaseSensitiveCreate() throws Exception {
		Path upper = root.resolve("A");
		Path lower = root.resolve("a");
		
		Files.createFile(lower);
		Files.createFile(upper);
		
		Files.write(upper, "A".getBytes());
		Files.write(lower, "a".getBytes());
		
		assertEquals("A", new String(Files.readAllBytes(upper)));
		assertEquals("a", new String(Files.readAllBytes(lower)));
	}
    
    @IgnoreIf(FsType.UNIX)
    @Test
    public void testCaseInSensitiveCreate() throws Exception {
		Path upper = root.resolve("A");
		Path lower = root.resolve("a");
		
		Files.createFile(lower);
		try
		{
			Files.createFile(upper);
			fail();
		} catch(FileAlreadyExistsException e) {
			//pass
		}
		
		Files.write(lower, "A".getBytes());
		
		assertEquals("A", new String(Files.readAllBytes(upper)));
		
		//case should be preserved
		try(DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
			Iterator<Path> iter = ds.iterator();
			assertTrue(iter.hasNext());
			assertEquals("a", iter.next().getFileName().toString());
			assertFalse(iter.hasNext());
		}
	}
    
}
