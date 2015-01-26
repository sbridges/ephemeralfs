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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Test;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemProvider;

public class EphemeralFsProviderTest {

    EphemeralFsFileSystemProvider fixture = new EphemeralFsFileSystemProvider();
    
    @After
    public void tearDown() {
        EphemeralFsFileSystemProvider.closeAll();
    }
    
    @Test
    public void testCreate() throws Exception {
        createTestFs();
    }
    
    @Test(expected=FileSystemNotFoundException.class)
    public void testGetThrowsNotCreated() throws Exception {
        getTestFs();
    }
    
    @Test
    public void testGetReturnsCreated() throws Exception {
        assertSame(
                createTestFs(),
                getTestFs()
                );
    }
    
    @Test
    public void testCreateThenCloseAll() throws Exception {
        EphemeralFsFileSystemProvider.closeAll();
        createTestFs();
    }
    
    @Test
    public void testCreateThenClose() throws Exception {
        createTestFs().close();
        createTestFs();
    }

    @Test(expected=FileSystemAlreadyExistsException.class)
    public void testCreateTwiceSameURIFails() throws Exception { 
        createTestFs();
        createTestFs();
    }
    
    @Test
    public void testGetFsName() throws Exception {
        assertEquals("test", fixture.validateUriAndGetName(new URI(EphemeralFsFileSystemProvider.SCHEME + ":///?name=test")));
        assertEquals("test", fixture.validateUriAndGetName(new URI(EphemeralFsFileSystemProvider.SCHEME + ":///?name=test")));
    }
    
    @Test
    public void testGetRootPath() throws Exception {
        Path path = fixture.getPath(new URI(EphemeralFsFileSystemProvider.SCHEME + ":///?name=test"));
        assertNotNull(path);
        assertEquals(0, path.getNameCount());
        assertTrue(path.isAbsolute());
    }
    @Test
    public void testGetPath() throws Exception {
        Path path = fixture.getPath(new URI(EphemeralFsFileSystemProvider.SCHEME + ":///a?name=test"));
        assertNotNull(path);
        assertEquals(1, path.getNameCount());
        assertTrue(path.isAbsolute());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNoQuery() throws Exception {
        fixture.validateUriAndGetName(new URI(EphemeralFsFileSystemProvider.SCHEME + ":///foo"));
    }

    private FileSystem getTestFs() throws IOException, URISyntaxException {
        return fixture.getFileSystem(new URI(EphemeralFsFileSystemProvider.SCHEME + ":///?name=test"));
    }
    
    private FileSystem createTestFs() throws IOException, URISyntaxException {
        return fixture.newFileSystem(new URI(EphemeralFsFileSystemProvider.SCHEME + ":///?name=test"), EphemeralFsFileSystemBuilder.unixFs() .buildEnv());
    }
}
