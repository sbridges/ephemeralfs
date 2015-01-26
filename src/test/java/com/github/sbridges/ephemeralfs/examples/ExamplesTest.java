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

package com.github.sbridges.ephemeralfs.examples;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.Test;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemChecker;

public class ExamplesTest {

    @Test
    public void testUnixExample() throws Exception {
        FileSystem fs = EphemeralFsFileSystemBuilder
                .unixFs()
                .build();
                
        Path testDir = fs.getPath("/testDir");
        Files.createDirectory(testDir);
        Files.write(testDir.resolve("cafe"), new byte[] {'c', 'a', 'f', 'e'});
    }
    
    @Test
    public void testWindowsExample() throws Exception {
        FileSystem fs = EphemeralFsFileSystemBuilder
                .windowsFs()
                .build();
        
        Path testDir = fs.getPath("m:\\windwosTestDir");
        Files.createDirectory(testDir);
        Files.write(testDir.resolve("dir"), new byte[] {'d', 'o', '5'});
    }
    
    
    

    @Test(expected=AssertionError.class)
    public void testFailsOnOpenResource() throws Exception {
        FileSystem fs = EphemeralFsFileSystemBuilder
                .macFs()
                .setRecordStackTracesOnOpen(true)
                .build();
        
        Files.newOutputStream(fs.getPath("/testFile"), StandardOpenOption.CREATE);
        
        //this will throws as the stream above was not closed
        //the AssertionError will contain the stack
        //trace of where the stream was opened
        EphemeralFsFileSystemChecker.assertNoOpenResources(fs);    
    }
}
