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
import java.nio.file.FileSystemException;
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
public class DeleteTest {

    Path root;
    
    @Test
    public void testDeleteFile() throws Exception {
        Path toDelete = root.resolve("deleteMe");
        Files.createFile(toDelete);
        
        Files.delete(toDelete);
        
        assertFalse(Files.exists(toDelete));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testDeleteDotFileInNonDirectoryUnix() throws Exception {
        Path toDelete = root.resolve("deleteMe");
        Files.createFile(toDelete);
        
        try {
            Files.delete(toDelete.resolve("."));
            fail();
        } catch(FileSystemException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Not a directory"));
        }
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testDeleteDotFileInNonDirectoryWindows() throws Exception {
        Path toDelete = root.resolve("deleteMe");
        Files.createFile(toDelete);
        Files.delete(toDelete.resolve("."));
        assertFalse(Files.exists(toDelete));
    }
    
    
    @Test
    public void testDeleteFileMultiLevel() throws Exception {
        Path child = root.resolve("child");
        Files.createDirectory(child);
        Path toDelete = child.resolve("deleteMe");
        Files.createFile(toDelete);
        
        Files.delete(toDelete);
        
        assertFalse(Files.exists(toDelete));
        assertTrue(Files.exists(child));
    }
    
    @Test
    public void testDeleteFileThreeLevel() throws Exception {
        Path child = root.resolve("child");
        Path grandChild = child.resolve("grandChild");
        Files.createDirectories(grandChild);
        Path toDelete = grandChild.resolve("deleteMe");

        Files.createFile(toDelete);
        
        Files.delete(toDelete);
        
        assertFalse(Files.exists(toDelete));
        assertTrue(Files.exists(child));
        assertTrue(Files.exists(grandChild));
    }
    
    @Test
    public void testDeleteDirectory() throws Exception {
        Path toDelete = root.resolve("deleteMe");
        Files.createDirectory(toDelete);
        
        Files.delete(toDelete);
        
        assertFalse(Files.exists(toDelete));
    }
    
    @Test
    public void testDeleteNonExistentFails() throws Exception {
        Path toDelete = root.resolve("deleteMe");
        
        try {
            Files.delete(toDelete);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }

    @Test
    public void testDeleteFileParentDoesNotExist() throws Exception {
        Path toDelete = root.resolve("deleteMe");
        toDelete = toDelete.resolve("child");
        
        try {
            Files.delete(toDelete);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @Test
    public void testDeleteDirectoryNotEmptyFails() throws Exception {
        Path toDelete = root.resolve("deleteMe");
        Files.createDirectory(toDelete);
        Files.createFile(toDelete.resolve("child"));
        
        try {
            Files.delete(toDelete);
            fail();
        } catch(DirectoryNotEmptyException e) {
            //pass
        }
    }
    
}
