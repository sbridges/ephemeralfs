/*
 * Copyright 2012 Sean Bridges. All rights reserved.
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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for absolute paths, this doesn't work
 * well with real file systems, so use a test one 
 */
public class RootPathsTest {

    
    FileSystem fs;

    @Before
    public void setUp() {
        fs = EphemeralFsFileSystemBuilder.unixFs().build();
    }

    @After
    public void tearDown() throws IOException {
        fs.close();
    }
    
    @Test
    public void testCreateFileInRoot() throws Exception {
        Files.createFile(fs.getPath("file"));
    }
    
    @Test
    public void testCreateDirectoryInRoot() throws Exception {
        Files.createDirectory(fs.getPath("dir"));
    }

    @Test
    public void testCopyToFromRoot() throws Exception {
        Path root = fs.getPath("/");
        Path dest = fs.getPath("/dest");
        try {
          Files.copy(root, dest);
          fail();
        } catch (IOException e) {
          // pass
        }
        try {
          Files.copy(dest, root);
          fail();
        } catch (IOException e) {
          // pass
        }
    }

    @Test
    public void testCopyRootToSelf() throws IOException {
      Path root = fs.getPath("/");
      Files.copy(root, root);
    }
    
    
    @Test
    public void testMoveRootToSelf() throws IOException {
      Path root = fs.getPath("/");
      Files.move(root, root);
    }
    
    @Test
    public void testMoveRoot() throws IOException {
      Path root = fs.getPath("/");
      Path a = fs.getPath("/a");
      try {
          Files.move(root, a);
          fail();
      } catch(IOException e) {
          //pass
      }
    }
    
    
}
