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

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class FileLockTest {

    final Random random = new Random();
    
    Path root;

    
    @Test
    public void testFileLock() throws Exception {
        Path file = root.resolve("locked");
        Files.createFile(file);
        
        FileChannel channel = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.READ);
        try
        {
            FileLock lock = channel.tryLock();
            assertNotNull(lock);
            assertFalse(lock.isShared());
            assertTrue(lock.isValid());
            
            lock.release();
            assertFalse(lock.isValid());
            
            lock = channel.tryLock();
            assertNotNull(lock);
            assertFalse(lock.isShared());
            assertTrue(lock.isValid());
            
            
        } finally {
            channel.close();
        }
    }
    
    @Test
    public void testFileLockNotReadable() throws Exception {
        Path file = root.resolve("locked");
        Files.createFile(file);
        
        FileChannel channel = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE);
        try
        {
            FileLock lock = channel.tryLock();
            assertNotNull(lock);
            assertFalse(lock.isShared());
            assertTrue(lock.isValid());
            
            lock.release();
            assertFalse(lock.isValid());
            
            lock = channel.tryLock();
            assertNotNull(lock);
            assertFalse(lock.isShared());
            assertTrue(lock.isValid());
            
            
        } finally {
            channel.close();
        }
    }
    
    @Test
    public void testLockShared() throws Exception {
        Path file = root.resolve("locked");
        Files.createFile(file);
        
        FileChannel channel = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.READ);
        try
        {
            FileLock lock = channel.tryLock(0, 5, true);
            assertNotNull(lock);
            assertTrue(lock.isShared());
            assertTrue(lock.isValid());
            
            try
            {
                lock = channel.tryLock(0, 5, true);
                fail();
            } catch(OverlappingFileLockException e) {
                //pass
            }
            
        } finally {
            channel.close();
        }
    }
    
    
    @Test
    public void testNonOverlappingLocks() throws Exception {
        Path file = root.resolve("locked");
        Files.createFile(file);
        
        FileChannel channel = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.READ);
        try
        {
            FileLock lock = channel.tryLock(0, 5, false);
            assertNotNull(lock);
            
            lock = channel.tryLock(5, 10, false);
            assertNotNull(lock);
        } finally {
            channel.close();
        }
    }
    
    @Test
    public void testMultipleLocksSameFile() throws Exception {
        Path file = root.resolve("locked");
        Files.createFile(file);
        
        FileChannel channel = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.READ);
        try
        {
            FileLock lock = channel.tryLock();
            assertNotNull(lock);
            
            try
            {
                channel.tryLock();
                fail();
            } catch(OverlappingFileLockException e) {
                //pass
            }
            
            
            try
            {
                channel.lock();
                fail();
            } catch(OverlappingFileLockException e) {
                //pass
            }
            
            
        } finally {
            channel.close();
        }
    }

    @Test
    public void testChannelClose() throws Exception {
        Path file = root.resolve("locked");
        Files.createFile(file);
        FileLock lock;
        
        try(FileChannel channel = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            lock = channel.tryLock();
            assertNotNull(lock);
        } 
        
        assertFalse(lock.isValid());
    }
    
    @Test
    public void testChannelCloseDoesntRemovesAllLocks() throws Exception {
        Path file = root.resolve("locked");
        Files.createFile(file);
        FileLock lock;
        
        FileChannel channel1 = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.READ);
        
        FileChannel channel2 = (FileChannel) 
                Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.READ);

        try
        {
            lock = channel1.tryLock();
            assertNotNull(lock);
            
            channel2.close();
            
            assertTrue(lock.isValid());
            
        } finally {
            channel1.close();
            channel2.close();
        }
        
        assertFalse(lock.isValid());
    }

}
