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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class DotsInFileNamesTest {

	
	Path root;
	
	@Test
	public void testCreateFileWithDot() throws Exception {
		Path dot = root.resolve(".");
		try
		{
			Files.createFile(dot);
			fail();
		} catch(IOException e) {
			//pass
		}
	}
	
	@Test
	public void testCreateFileWithDotDot() throws Exception {
		Path dot = root.resolve("..");
		try
		{
			Files.createFile(dot);
			fail();
		} catch(IOException e) {
			//pass
		}
	}
	
	@Test
	public void testListWithDot() throws Exception {
		Path testFile = root.resolve("test");
		Files.createFile(testFile);
		
		Path withDot = root.resolve("."); 
		
		TestUtil.assertChildren(
				withDot, 
				withDot.resolve("test"));
	}
	
	   @Test
	    public void testListFileWithDotDot() throws Exception {
	        Path testFile = root.resolve("test");
	        Files.createFile(testFile);
	        Files.createDirectory(root.resolve("foo"));
	        
	        Path withDotDot = root.resolve("foo").resolve("..");
	        
	        TestUtil.assertChildren(
	                withDotDot, 
	                withDotDot.resolve("test"),
	                withDotDot.resolve("foo"));
	    }
	
	@IgnoreUnless(FsType.WINDOWS)
	@Test
	public void testListFileWithDotDotNonExistentWindows() throws Exception {
		Path testFile = root.resolve("test");
		Files.createFile(testFile);
		
		Path withDotDot = root.resolve("foo").resolve("..");
		
		TestUtil.assertChildren(
				withDotDot, 
				withDotDot.resolve("test"));
	}

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testListFileWithDotDotNonExistentLinux() throws Exception {
        Path testFile = root.resolve("test");
        Files.createFile(testFile);
        
        Path withDotDot = root.resolve("foo").resolve("..");
        
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(withDotDot)) {
            fail();   
        } catch(NoSuchFileException e) {
            //pass
        }
    }
	
	
	@Test
	public void testReadWithDot() throws Exception {
		Path testFile = root.resolve("test");
		Files.write(testFile, new byte[] {42});
		
		Path withDot = root.resolve(".").resolve("test"); 
		
		assertArrayEquals(new byte[] {42}, Files.readAllBytes(withDot));
		
	}
	
	   
    @Test
    public void testReadWithDotDot() throws Exception {
        Path testFile = root.resolve("test");
        Files.write(testFile, new byte[] {42});
        Files.createDirectory(root.resolve("foo"));
        Path withDot = root.resolve("foo").resolve("..").resolve("test"); 
        
        assertArrayEquals(new byte[] {42}, Files.readAllBytes(withDot));
        
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testReadWithDotDotNonExistentWindows() throws Exception {
        Path testFile = root.resolve("test");
        Files.write(testFile, new byte[] {42});
        
        
        
        Path withDot = root.resolve("foo").resolve("..").resolve("test"); 
        
        assertArrayEquals(new byte[] {42}, Files.readAllBytes(withDot));
        
    }
	
    @IgnoreIf(FsType.WINDOWS)
	@Test
	public void testReadWithDotDotNonExistentLinux() throws Exception {
		Path testFile = root.resolve("test");
		Files.write(testFile, new byte[] {42});
		
		
		
		Path withDot = root.resolve("foo").resolve("..").resolve("test"); 
		
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(withDot)) {
            fail();   
        } catch(NoSuchFileException e) {
            //pass
        }
		
	}
}
