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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.After;
import org.junit.Test;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystem;
import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemChecker;
import com.github.sbridges.ephemeralfs.EphemeralFsPath;
import com.github.sbridges.ephemeralfs.ResolvedPath;

public class FileSyncTest {

    EphemeralFsFileSystem fs = (EphemeralFsFileSystem) 
            EphemeralFsFileSystemBuilder
                .unixFs()
                .build();
    EphemeralFsPath root = fs.getRootPath();
    EphemeralFsPath file = root.resolve("file");
    EphemeralFsPath dir = root.resolve("dir");
    EphemeralFsPath fileInDir = dir.resolve("fileInDir");
    EphemeralFsPath dirInDir = dir.resolve("dirInDir");
    EphemeralFsPath otherFile = root.resolve("otherFile");
    
    @After
    public void tearDown() throws IOException {
        fs.close();
    }
    
    @Test
    public void testNewFileIsDirty() throws Exception {
        Files.createFile(file);
        assertDirty(file);
    }

    
    @Test
    public void testWriteDirties() throws Exception {
        Files.createFile(file);
        sync(file);
        assertClean(file);
        Files.write(file, new byte[] {1});
        assertDirty(file);
    }

    @Test
    public void testOpenSyncCleans() throws Exception {
        OutputStream fos = Files.newOutputStream(
                file,
                
                StandardOpenOption.CREATE,
                StandardOpenOption.SYNC
                );
        
        sync(file);
        assertClean(file);
        fos.write(new byte[] {1});
        assertClean(file);
    }
    
    @Test
    public void testOpenDSyncCleans() throws Exception {
        OutputStream fos = Files.newOutputStream(
                file,
                
                StandardOpenOption.CREATE,
                StandardOpenOption.DSYNC
                );
        
        sync(file);
        assertClean(file);
        fos.write(new byte[] {1});
        assertDirty(file);
    }
    
    @Test
    public void testForceCleans() throws Exception {
        
        FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE
                );
        
        sync(file);
        assertClean(file);
        channel.write(ByteBuffer.wrap(new byte[] {1}));
        assertDirty(file);
        channel.force(false);
        assertDirty(file);
        channel.force(true);
        assertClean(file);
    }
    
    @Test
    public void testTruncateDirties() throws Exception {

        FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE
                );
        
        channel.write(ByteBuffer.wrap(new byte[] {1}));
        channel.force(true);
        assertClean(file);
        channel.truncate(0);
        assertDirty(file);
    }
    
    @Test
    public void testTranferFromDirties() throws Exception {

        Files.write(otherFile, new byte[] {1});

        FileChannel source = FileChannel.open(
                otherFile,
                StandardOpenOption.READ
                );

        
        FileChannel target = FileChannel.open(
                file,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE
                );
        sync(file);
        assertClean(file);
        target.transferFrom(source, 0, 1);
        assertDirty(file);
    }
    
    
    @Test
    public void testVerifyNothingNeedsToFsync() throws Exception {
        Files.write(file, new byte[] {1,2});
        sync(file);
        EphemeralFsFileSystemChecker.assertAllFilesFsynced(root);
    }
    
    @Test
    public void testVerifySomethingNotFsyncedFile() throws Exception {
        Files.write(file, new byte[] {1,2});
        try {
            EphemeralFsFileSystemChecker.assertAllFilesFsynced(file);
            throw new IllegalStateException("failed");
        } catch(AssertionError e) {
            assertTrue(e.getMessage().contains("Failed to sync"));
        }
    }

    @Test
    public void testVerifySomethingNotFsyncedRoot() throws Exception {
        Files.write(file, new byte[] {1,2});
        try {
            EphemeralFsFileSystemChecker.assertAllFilesFsynced(root);
            throw new IllegalStateException("failed");
        } catch(AssertionError e) {
            assertTrue(e.getMessage().contains("Failed to sync"));
        }
    }

    @Test
    public void testVerifySomethingNotFsyncedChildDir() throws Exception {
        Path child = Files.createDirectories(root.resolve("child"));
        Path childFile = child.resolve("childFile");
        Files.write(childFile, new byte[] {1,2});
        try {
            EphemeralFsFileSystemChecker.assertAllFilesFsynced(root);
            throw new IllegalStateException("failed");
        } catch(AssertionError e) {
            assertTrue(e.getMessage().contains("Failed to sync"));
        }
    }
    
    
    @Test
    public void testNewDirectoryIsNotDirty() throws Exception {
        Files.createDirectory(dir);
        assertClean(dir);
    }
    
    @Test
    public void testAddFileDirtiesDirecotry() throws Exception {
        Files.createDirectory(dir);
        Files.createFile(fileInDir);
        assertDirty(dir);
        sync(dir);
        assertClean(dir);
    }

    public void testAddDirDirtiesDirecotry() throws Exception {
        Files.createDirectory(dir);
        Files.createDirectory(dirInDir);
        assertDirty(dir);
        sync(dir);
        assertClean(dir);
    }

    
    @Test
    public void testRemoveFileDirtiesDirectory() throws Exception {
        Files.createDirectory(dir);
        Files.createFile(fileInDir);
        sync(dir);
        assertClean(dir);
        Files.delete(fileInDir);
        assertDirty(dir);
    }

    @Test
    public void testMoveFileDirtiesDirectory() throws Exception {
        Files.createDirectory(dir);
        Files.createFile(file);
        sync(dir);
        assertClean(dir);
        Files.move(file, fileInDir);
        assertDirty(dir);
    }
    
    static void sync(Path file) throws IOException {
        FileChannel.open(
                file,
                StandardOpenOption.READ
                ).force(true);;
    }
    
    private void assertDirty(EphemeralFsPath path) throws NoSuchFileException, FileSystemException {
        assertTrue(ResolvedPath.resolve(path).getTarget().isDirty());
    }
    
    private void assertClean(EphemeralFsPath path) throws NoSuchFileException, FileSystemException {
        assertFalse(ResolvedPath.resolve(path).getTarget().isDirty());
    }


}
