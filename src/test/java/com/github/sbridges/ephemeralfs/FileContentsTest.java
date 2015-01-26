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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystem;
import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.EphemeralFsPath;
import com.github.sbridges.ephemeralfs.FileContents;
import com.github.sbridges.ephemeralfs.FilePermissions;
import com.github.sbridges.ephemeralfs.INode;
import com.github.sbridges.ephemeralfs.ResolvedPath;



public class FileContentsTest {

    EphemeralFsFileSystem fs;
    FileContents fixture;
    Random random = new Random(42);
    EphemeralFsPath root;
    ResolvedPath resolvedPath;
    INode iNode;
    
    @Before
    public void setUp() throws IOException {
        fs = (EphemeralFsFileSystem) EphemeralFsFileSystemBuilder.unixFs().setName("test").build();
        root = (EphemeralFsPath) fs.getRootDirectories().iterator().next();
        EphemeralFsPath filePath = root.resolve("test");
        ResolvedPath.resolve(root).getTarget().addFile(filePath.getFileName(), FilePermissions.createDefaultFile());
        resolvedPath = ResolvedPath.resolve(root.resolve(filePath.getFileName()));
        fixture = new FileContents(fs, resolvedPath.getTarget());
    }
    
    @After
    public void tearDown() throws IOException {
        if(fs != null) {
            fs.close();
        }
    }
    
    @Test
    public void testReadWrite() throws Exception {
        byte[] contents = new byte[2049];
        random.nextBytes(contents);
        ByteArrayInputStream source = new ByteArrayInputStream(contents);
        copy(source, os());
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        copy(is(), sink);
        assertArrayEquals(contents, sink.toByteArray());
        assertEquals(2049, fixture.getSize());
    }
    
    @Test
    public void testIsOpen() throws Exception {
        SeekableByteChannel channel = fixture.newChannel(true, true, false, false, false, false, resolvedPath, fs);
        assertTrue(channel.isOpen());
        channel.close();
        assertFalse(channel.isOpen());
    }
    
    @Test
    public void testReadEmpty() throws Exception {
        SeekableByteChannel channel = fixture.newChannel(true, true, false, false, false, false, resolvedPath, fs);
        ByteBuffer buffer = ByteBuffer.allocate(1000);
        assertEquals(-1,  channel.read(buffer));
    }

    @Test
    public void testMultipleReadsEventuallyEmpty() throws Exception {
        
        SeekableByteChannel channel = fixture.newChannel(true, true, false, false, false, false, resolvedPath, fs);
        
        ByteBuffer buffer = ByteBuffer.allocate(1000);
        random.nextBytes(buffer.array());
        channel.write(buffer);
        
        channel.position(0);
        buffer.position(0);
        channel.read(buffer);
        assertEquals(-1,  channel.read(buffer));
    }

    @Test
    public void testWriteAfterSetPosition() throws Exception {
        SeekableByteChannel channel = fixture.newChannel(true, true, false, false, false, false, resolvedPath, fs);
        channel.position(500);
        assertEquals(0, fixture.getSize());
        channel.write(ByteBuffer.allocate(1));
        assertEquals(501, fixture.getSize());
    }
    
    @Test
    public void testTruncate() throws Exception {
        SeekableByteChannel channel = fixture.newChannel(true, true, false, false, false, false, resolvedPath, fs);
        channel.write(ByteBuffer.allocateDirect(500));
        assertEquals(500, fixture.getSize());
        channel.truncate(100);
        assertEquals(100, fixture.getSize());
        
        channel.write(ByteBuffer.allocateDirect(1));
        assertEquals(101, fixture.getSize());
    }
    
    private InputStream is() {
        return Channels.newInputStream(fixture.newChannel(true, true, false, false, false, false, resolvedPath, fs));
    }
    
    private OutputStream os() {
        return Channels.newOutputStream(fixture.newChannel(true, true, false, false, false, false, resolvedPath, fs));
    }
    
    private void copy(InputStream source, OutputStream sink) throws IOException {
        byte[] buf = new byte[128];
        for(int read = source.read(buf, 0, buf.length); read > 0; read = source.read(buf, 0, buf.length)) {
            sink.write(buf, 0, read);
        }
    }
    
}
