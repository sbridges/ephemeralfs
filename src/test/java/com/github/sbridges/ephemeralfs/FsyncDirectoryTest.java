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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;
import com.github.sbridges.ephemeralfs.junit.RunUnlessType;

@RunUnlessType(FsType.WINDOWS)
@RunWith(MultiFsRunner.class)
public class FsyncDirectoryTest {

    
    Path root;
    
    @Test
    public void testFsyncDirectory() throws Exception {
        try(FileChannel channel = FileChannel.open(root)) {
            channel.force(true);
        }
    }
    
    @Test
    public void testDeleteOnClose() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        try(FileChannel channel = FileChannel.open(dir, StandardOpenOption.DELETE_ON_CLOSE)) {
            
        } 
        assertTrue(Files.exists(dir));
    }
    
    @Test
    public void testReadPositionDirectory() throws Exception {
        try(FileChannel channel = FileChannel.open(root)) {
            assertEquals(0, channel.position());
        } 
    }
    
    @Test
    public void testSetPositionDirectory() throws Exception {
        try(FileChannel channel = FileChannel.open(root)) {
            channel.position(2);
        } 
    }
    
    @Test
    public void testReadDirectory() throws Exception {
        try(FileChannel channel = FileChannel.open(root)) {
            channel.read(ByteBuffer.wrap(new byte[1]));
        } catch(IOException e) {
            assertEquals("Is a directory", e.getMessage());
        }
    }
    
    @Test
    public void testWriteDirectoryFails() throws Exception {
        try(FileChannel channel = FileChannel.open(root)) {
            channel.write(ByteBuffer.wrap(new byte[] {1}));
            fail();
        } catch(NonWritableChannelException e) {
            //pass
        }
    }
    
    
}
