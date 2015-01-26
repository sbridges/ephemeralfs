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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.junit.Test;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystem;
import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.EphemeralFsPath;

public class FileSystemSizeLimitsTest {

    int totalSize = 1000;
    
    EphemeralFsFileSystem fs = (EphemeralFsFileSystem) 
            EphemeralFsFileSystemBuilder
                .unixFs()
                .setTotalSpace(totalSize)
                .build();
    EphemeralFsPath root = fs.getRootPath();
    EphemeralFsPath file = root.resolve("file");
    EphemeralFsPath otherFile = root.resolve("otherFile");
    EphemeralFsPath link = root.resolve("link");
    
    @Test
    public void testSizeRemainingInitial() throws Exception {
        assertUsed(0);
    }

    @Test
    public void testUsedAfterSingleFileWrite() throws Exception {
        Files.write(file, new byte[] {1});
        assertUsed(1);
        
        Files.delete(file);
        assertUsed(0);
    }

    
    @Test
    public void testUsedAfterMultipleFileWrite() throws Exception {
        Files.write(file, new byte[] {1});
        assertUsed(1);
        
        Files.write(file, new byte[] {1});
        assertUsed(1);
        
        Files.write(file, new byte[] {1, 2});
        assertUsed(2);
        
        
        Files.write(file, new byte[] {1});
        assertUsed(1);
        
        Files.delete(file);
        assertUsed(0);
    }
    
    @Test
    public void testUsedAfterFileWriteToOutputStream() throws Exception {
        try(OutputStream fos = Files.newOutputStream(file)) {
            for(int i =1; i < 10; i++) {
                fos.write(new byte[] {1});
                assertUsed(i);
            }
        }
        
        Files.delete(file);
        assertUsed(0);
    }
    
    @Test
    public void testHardLinkedFilesStillTakeSpace() throws Exception {
        Files.write(file, new byte[] {1});
        Files.createLink(link, file);
        assertUsed(1);
        
        Files.delete(file);
        assertUsed(1);
        
        Files.delete(link);
        assertUsed(0);
    }

    @Test
    public void testOpenFilesTakeSpace() throws Exception {
        Files.write(file, new byte[] {1});
        InputStream is = Files.newInputStream(file);
        
        Files.delete(file);
        assertUsed(1);

        is.close();
        assertUsed(0);
    }

    
    @Test
    public void testMovePreservesSpace() throws Exception {
        Files.write(file, new byte[] {1});
        Files.move(file, otherFile);
        
        assertUsed(1);
        
        Files.delete(otherFile);
        assertUsed(0);
    }

    @Test
    public void testFailToWriteUsingSpace() throws Exception {
        try {
            Files.write(file, new byte[totalSize + 1]);
            fail();
        } catch(IOException e) {
            assertEquals("Out of disk space", e.getMessage());
        }
        assertUsed(0);
        assertTrue(Files.exists(file));
    }
    
    @Test
    public void testFailToCopyUsingSpace() throws Exception {
        Files.write(file, new byte[totalSize -1]);
        try {
            Files.copy(file, otherFile);
            fail();
        } catch(IOException e) {
            assertEquals("Out of disk space", e.getMessage());
        }
        assertUsed(totalSize -1);
        assertTrue(Files.exists(otherFile));
    }
    
    
    private void assertUsed(int used) throws IOException {
        long free = fs.getFileStores().iterator().next().getUsableSpace();
        long actual = totalSize - free;
        assertEquals(
                used, actual);
    }
}
