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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemChecker;

public class EphemeralFsFileSystemCheckerTest {

    FileSystem fs;
    Path root;
    Path file;
    
    @Before
    public void setUp() throws IOException {
        fs = EphemeralFsFileSystemBuilder
                .unixFs()
                .build();
        root = fs.getRootDirectories().iterator().next();
        file = root.resolve("file");
        Files.write(file, new byte[] {1,2,3});
        
    }
    
    @After
    public void tearDown() throws IOException {
        fs.close();
    }
    
    @Test
    public void testDontThrowIfChannelClosed() throws Exception {
        Files.newByteChannel(file).close();
        EphemeralFsFileSystemChecker.assertNoOpenResources(fs);
    }
    
    @Test
    public void testThrowIfChannelOpen() throws Exception {
        Files.newByteChannel(file);
        assertNotOpenThrows();
    }
    
    @Test
    public void testThrowIfChannelOpenFileDeleted() throws Exception {
        Files.newByteChannel(file);
        Files.delete(file);
        assertNotOpenThrows();
    }

    @Test
    public void testDontThrowIfAsyncChannelClosed() throws Exception {
        fs.provider().newAsynchronousFileChannel(file, new HashSet<OpenOption>(), null).close();
        fs.close();
    }
    
    @Test
    public void testThrowIfAsyncChannelOpen() throws Exception {
        fs.provider().newAsynchronousFileChannel(file, new HashSet<OpenOption>(), null);
        assertNotOpenThrows();
    }

    @Test
    public void testDontThrowIfDirectoryStreamClosed() throws Exception {
        Files.newDirectoryStream(root).close();
        fs.close();
    }
    
    @Test
    public void testThrowIfDirectoryStreamOpen() throws Exception {
        Files.newDirectoryStream(root);
        assertNotOpenThrows();
    }
    
    @Test
    public void testDontThrowIfWatchServiceClosed() throws Exception {
        fs.newWatchService().close();
        fs.close();
    }
    
    @Test
    public void testThrowIfWatchServiceOpen() throws Exception {
        fs.newWatchService();
        assertNotOpenThrows();
    }
    
    
    @Test
    public void testDirtyFile() throws Exception {
        Files.write(file, new byte[] {1});
        assertFsyncFileThrows(file);
    }

    @Test
    public void testDirtyFileInSubDir() throws Exception {
        Path dir = Files.createDirectory(root.resolve("dir"));
        Path fileInDir = Files.createFile(dir.resolve("fileInDir"));
        assertFsyncFileThrows(dir);
        FileSyncTest.sync(fileInDir);
        EphemeralFsFileSystemChecker.assertAllFilesFsynced(dir);
    }
    
    
    @Test
    public void testDsyncNonExistentFile() throws Exception {
        try {
            EphemeralFsFileSystemChecker.assertAllFilesFsynced(root.resolve("doesNotExist"));
            fail();
        } catch(IllegalArgumentException e) {
            //pass
        }
    }
    
    @Test
    public void testCleanFile() throws Exception {
        FileSyncTest.sync(file);
        EphemeralFsFileSystemChecker.assertAllFilesFsynced(file);
    }

    @Test
    public void testFsyncDirOnFile() throws Exception {
        try {
            EphemeralFsFileSystemChecker.assertAllDirectoriesFsynced(file, false);
            fail();
        } catch(IllegalArgumentException e) {
            //pass
        }
    }
    
    @Test
    public void testFsyncDirOnNonExistent() throws Exception {
        try {
            EphemeralFsFileSystemChecker.assertAllDirectoriesFsynced(root.resolve("dir"), false);
            fail();
        } catch(IllegalArgumentException e) {
            //pass
        }
    }
    
    
    @Test
    public void testFsyncDirOnDirtyDirs() throws Exception {
        Path dir = Files.createDirectory(root.resolve("dir"));
        Files.createFile(dir.resolve("fileInDir"));
        FileSyncTest.sync(root);
        
        assertFsyncDirectoryThrows(root, true);
        assertFsyncDirectoryThrows(dir, true);
        assertFsyncDirectoryThrows(dir, false);
        
        FileSyncTest.sync(dir);
        
        EphemeralFsFileSystemChecker.assertAllDirectoriesFsynced(root, true);
        EphemeralFsFileSystemChecker.assertAllDirectoriesFsynced(dir, true);
        EphemeralFsFileSystemChecker.assertAllDirectoriesFsynced(dir, false);
    }
    
    private void assertNotOpenThrows() {
        boolean notThrown = false;
        try {
            EphemeralFsFileSystemChecker.assertNoOpenResources(fs);
            notThrown = true;
        } catch(AssertionError e) {
            assertTrue(e.getMessage().contains("Failed to close"));
        }
        if(notThrown) {
            fail();
        }
    }
    
    private void assertFsyncFileThrows(Path path) {
        boolean notThrown = false;
        try {
            EphemeralFsFileSystemChecker.assertAllFilesFsynced(path);
            notThrown = true;
        } catch(AssertionError e) {
            assertTrue(e.getMessage().contains("Failed to sync"));
        }
        if(notThrown) {
            fail();
        }
    }
    
    
    private void assertFsyncDirectoryThrows(Path path, boolean recursive) {
        boolean notThrown = false;
        try {
            EphemeralFsFileSystemChecker.assertAllDirectoriesFsynced(path, recursive);
            notThrown = true;
        } catch(AssertionError e) {
            assertTrue(e.getMessage().contains("Failed to sync"));
        }
        if(notThrown) {
            fail();
        }
    }
}