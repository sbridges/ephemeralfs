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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;
import com.github.sbridges.ephemeralfs.junit.RunUnlessType;

//mac watch service relies on polling in the jdk right now
//skip it until its fast
@RunUnlessType(FsType.MAC)
@RunWith(MultiFsRunner.class)
public class WatchServiceTest {

    
    Path root;
    
    @Test
    public void testRegisterNonDirectory() throws IOException, Exception {
        
        Path file = root.resolve("file");
        Files.createFile(file);
                
        
        try(WatchService service = root.getFileSystem().newWatchService()) {
            try
            {
                file.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
                fail();
            } catch(NotDirectoryException e) {
                //pass
            }
        }
        
    }
    
    @Test
    public void testRegisterDirDoesNotExist() throws IOException, Exception {
        
        Path dir = root.resolve("dir");
                
        
        try(WatchService service = root.getFileSystem().newWatchService()) {
            try
            {
                dir.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
                fail();
            } catch(NoSuchFileException e) {
                //pass
            }
        }
        
    }
    
    @Test
    public void testSeeNewChildren() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_CREATE));
            
            Files.createFile(root.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            List<WatchEvent<?>> events = key.pollEvents();
            
            assertEquals(1, events.size());
            assertEquals(1, events.get(0).count());
            assertEquals(StandardWatchEventKinds.ENTRY_CREATE, events.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events.get(0).context());
            
            assertTrue(key.pollEvents().isEmpty());
            
            assertTrue(key.reset());
        }
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testHardLinks() throws IOException, Exception {
        
        Path dir1 = root.resolve("dir1");
        Path dir2 = root.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        
        Path test1 = dir1.resolve("test");
        Path test2 = dir2.resolve("test");
        
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            Files.createFile(test1);
            Files.createLink(test2, test1);
            
            assertNotNull(dir1.register(service, StandardWatchEventKinds.ENTRY_MODIFY));
            
            Files.write(test2, new byte[] {42});
            assertArrayEquals(new byte[] {42}, Files.readAllBytes(test1));
            
            WatchKey key = service.poll(1, TimeUnit.SECONDS);
            
            //an entry has been modified
            //but the modifications occurred
            //on the hardlinked file, in a different
            //dir, this modification does not
            //trigger a watch
            //except in windows
            assertNull(key);
            
        }
    }

    
    @Test
    public void testReRegisterAfterDirDeletedCreated() throws IOException, Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            assertNotNull(dir.register(service, StandardWatchEventKinds.ENTRY_CREATE));
            
            Files.createFile(dir.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            List<WatchEvent<?>> events = key.pollEvents();
            
            assertEquals(1, events.size());
            assertEquals(1, events.get(0).count());
            assertEquals(StandardWatchEventKinds.ENTRY_CREATE, events.get(0).kind());
            
            TestUtil.deleteTempDirRecursive(dir);
            Files.createDirectories(dir);
            
            assertKeyDoesNotReset(key);
        }
        
    }
    
    
    @Test
    public void testMove() throws IOException, Exception {
        
        //in which we demonstrate that a watch on a dir
        //is not really a watch on the dir, but on the INode
        //for the dir
        //we register a watch on <root>/dir1
        //then move dir1 to <root>/dir2
        //create a file in <root>/dir2
        //and that triggers our original watch
        
        Path dir1 = root.resolve("dir1");
        Files.createDirectories(dir1);
        Path dir2 = root.resolve("dir2");
        
        Files.createDirectories(dir1);
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            assertNotNull(dir1.register(service, StandardWatchEventKinds.ENTRY_CREATE));
        
            Files.move(dir1, dir2);
            
            Files.createFile(dir2.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            List<WatchEvent<?>> events = key.pollEvents();
            
            assertEquals(1, events.size());
            assertEquals(1, events.get(0).count());
            assertEquals(StandardWatchEventKinds.ENTRY_CREATE, events.get(0).kind());
            assertTrue(((Path) key.watchable()).isAbsolute());
            assertEquals(dir1, key.watchable());
            
            assertTrue(key.reset());
            
            Files.createFile(dir2.resolve("test2"));
            
            key = poll(service);
            assertNotNull(key);
        }
        
    }
        
    
    @Test
    public void testSeeContentsWritten() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            Path test = Files.createFile(root.resolve("test"));
            
            
            assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_MODIFY));
            
            Files.write(test, new byte[] {5});
            
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            List<WatchEvent<?>> events = key.pollEvents();
            
            assertEquals(1, events.size());
            //different for windows and linux
            assertTrue(events.get(0).count() >= 1);
            assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events.get(0).context());
            
            assertTrue(key.pollEvents().isEmpty());
            
            assertTrue(key.reset());
        }
        
    }
    
    @Test
    public void testSeeContentsWrittenMultipleParents() throws IOException, Exception {
        
        Path dir1 = root.resolve("dir1");
        Path dir2 = root.resolve("dir2");
        
        Files.createDirectory(dir1);
        Files.createDirectory(dir2);
        
        
        try(WatchService d1Service = dir1.getFileSystem().newWatchService();
            WatchService d2Service = dir2.getFileSystem().newWatchService()) {
            
            Path test = Files.createFile(dir1.resolve("test"));
            Path link = dir2.resolve("link");
            Files.createLink(link, test);
            
            
            assertNotNull(dir1.register(d1Service, StandardWatchEventKinds.ENTRY_MODIFY));
            assertNotNull(dir2.register(d2Service, StandardWatchEventKinds.ENTRY_MODIFY));
            
            Files.write(test, new byte[] {5});
            
            
            WatchKey key1 = poll(d1Service);
            assertNotNull(key1);
            
            //we are modifying a file with two parents, but only
            //the parent which was used to open the file
            //sees the event
            WatchKey key2 = d2Service.poll(1, TimeUnit.SECONDS);
            assertNull(key2);
            
            List<WatchEvent<?>> events1 = key1.pollEvents();
            
            assertEquals(1, events1.size());
            //2 entries, 1 for meta data, 1 for contents?
            //different for windows and linux
            assertTrue(events1.get(0).count() >= 1);
            assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events1.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events1.get(0).context());
        }
        
    }
    
    @Test
    public void testSeeContentsChanged() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            Path test = Files.createFile(root.resolve("test"));
            Files.write(test, new byte[] {5});
            
            assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_MODIFY));
            
            Files.write(test, new byte[] {6});
            

            WatchKey key = poll(service);
            assertNotNull(key);
            
            List<WatchEvent<?>> events = key.pollEvents();

            Thread.sleep(50);
            
            assertEquals(1, events.size());
            //more than 1 count, meta data and contents, this differs on windows/linux
            assertTrue(events.get(0).count() >= 1);
            assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events.get(0).context());
            
            assertTrue(key.pollEvents().isEmpty());
            
            assertTrue(key.reset());
        }
        
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testSeeContentsChangedStream() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            Path test = Files.createFile(root.resolve("test"));
            
            try(OutputStream out = Files.newOutputStream(test)) {
                out.write(new byte[] {5});
                
                assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_MODIFY));
                
                out.write(new byte[] {6});
                
                WatchKey key = poll(service);
                assertNotNull(key);
                
                List<WatchEvent<?>> events = key.pollEvents();

                Thread.sleep(50);
                
                assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events.get(0).kind());
                assertEquals(root.resolve("test").getFileName(), events.get(0).context());
                
                assertTrue(key.pollEvents().isEmpty());
                
                assertTrue(key.reset());
            }
        }
        
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testSeeContentsChangedStreamAfterRename() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            Path test = Files.createFile(root.resolve("test"));
            
            try(OutputStream out = Files.newOutputStream(test)) {
                
                Files.move(test, root.resolve("test2"));
                
                out.write(new byte[] {5});
                
                assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_MODIFY));
                
                out.write(new byte[] {6});
                
                WatchKey key = poll(service);
                assertNotNull(key);
                
                List<WatchEvent<?>> events = key.pollEvents();

                Thread.sleep(50);
                
                assertEquals(1, events.size());
               
                assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events.get(0).kind());
                
                assertTrue(key.pollEvents().isEmpty());
                
                assertTrue(key.reset());
            }
        }
        
    }
    
    
    @Test
    public void testSeeContentsChangedMultipleTimes() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            Path test = Files.createFile(root.resolve("test"));
            Files.write(test, new byte[] {5});
            
            assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_MODIFY));
            
            Files.write(test, new byte[] {6});
            Files.write(test, new byte[] {7});
            
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            List<WatchEvent<?>> events = key.pollEvents();
            
            assertEquals(1, events.size());
            //2 entries, 1 for meta data, 1 for contents?
            //the actual number is different on windows and linux
            assertTrue(events.get(0).count() >= 1);
            assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events.get(0).context());
            
            assertTrue(key.pollEvents().isEmpty());
            
            assertTrue(key.reset());
        }
        
    }
    
    @Test
    public void testSeeContentsChangedProps() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            
            Path test = Files.createFile(root.resolve("test"));
            
            
            assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_MODIFY));
            
            Files.setLastModifiedTime(test, FileTime.fromMillis(System.currentTimeMillis() - 10000));
            
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            List<WatchEvent<?>> events = key.pollEvents();
            
            assertEquals(1, events.size());
            assertEquals(1, events.get(0).count());
            assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events.get(0).context());
            
            assertTrue(key.pollEvents().isEmpty());
            
            assertTrue(key.reset());
        }
        
    }
    
    @Test
    public void testResetDirDoesNotExist() throws IOException, Exception {
        
        Path child = root.resolve("child");
        Files.createDirectories(child);
        
        try(WatchService service = child.getFileSystem().newWatchService()) {
            child.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            
            Files.createFile(child.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            key.pollEvents();
            
            TestUtil.deleteTempDirRecursive(child);
            assertKeyDoesNotReset(key);
        }
        
    }
    
    @Test
    public void testResetDirIsNowFile() throws IOException, Exception {
        
        Path child = root.resolve("child");
        Files.createDirectories(child);
        
        try(WatchService service = child.getFileSystem().newWatchService()) {
            child.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            
            Files.createFile(child.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            key.pollEvents();
            
            TestUtil.deleteTempDirRecursive(child);
            Files.createFile(child);
            
            assertKeyDoesNotReset(key);
        }
        
    }
    
    @Test
    public void testResetNewDirSameName() throws IOException, Exception {
        
        Path child = root.resolve("child");
        Files.createDirectories(child);
        
        try(WatchService service = child.getFileSystem().newWatchService()) {
            child.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            
            Files.createFile(child.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            key.pollEvents();
            
            TestUtil.deleteTempDirRecursive(child);
            Files.createDirectories(child);
            assertKeyDoesNotReset(key);
        }
        
    }
    
    @Test
    public void testPollAfterCancel() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            root.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            
            Files.createFile(root.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            key.cancel();
            
            List<WatchEvent<?>> events = key.pollEvents();
            
            assertEquals(1, events.size());
            assertEquals(1, events.get(0).count());
            assertEquals(StandardWatchEventKinds.ENTRY_CREATE, events.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events.get(0).context());
            
            assertTrue(key.pollEvents().isEmpty());
            
            assertFalse(key.reset());
        }
        
    }
    
    
    @Test
    public void testSeeNewChildrenAfterPolled() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            root.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            
            Files.createFile(root.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            Files.createFile(root.resolve("test2"));
            
            List<WatchEvent<?>> events = new ArrayList<>(key.pollEvents());
            int attempt = 0;
            while(events.size() < 2 && attempt < 10) {
                Thread.sleep(attempt * 5);
                events.addAll(key.pollEvents());
                attempt++;
            }
            
            assertEquals(2, events.size());
            assertEquals(1, events.get(0).count());
            assertEquals(StandardWatchEventKinds.ENTRY_CREATE, events.get(0).kind());
            assertEquals(root.resolve("test").getFileName(), events.get(0).context());
            assertEquals(root.resolve("test2").getFileName(), events.get(1).context());
            
            assertTrue(key.pollEvents().isEmpty());
            assertTrue(key.reset());
        }
    }
    
    @Test
    public void testSeeNewChildrenAfterPolledAndReset() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            assertNotNull(root.register(service, StandardWatchEventKinds.ENTRY_CREATE));
            
            Files.createFile(root.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            assertEquals(1, key.pollEvents().size());
            
            key.reset();
            
            
            Files.createFile(root.resolve("test2"));
            
            List<WatchEvent<?>> events = new ArrayList<>(key.pollEvents());
            int attempt = 0;
            while(events.isEmpty() && attempt < 10) {
                Thread.sleep(attempt * 5);
                events  = key.pollEvents();
                attempt++;
            }

            assertEquals(1, events.size());
            
            key = service.take();
            assertNotNull(key);
            assertTrue(key.pollEvents().isEmpty());
        }
    }
    
    
    @Test
    public void testMultipleReset() throws IOException, Exception {
        try(WatchService service = root.getFileSystem().newWatchService()) {
            root.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            
            Files.createFile(root.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            for(int i =0; i < 100l; i++) {
                Thread.sleep(1);
                assertTrue(key.reset());    
            }

            //added once to the watch service for each reset call
            //this seems like a bug, but thats the way it works
            WatchKey last = null;
            for(int i =0; i < 100l; i++) {
                WatchKey current = poll(service);
                assertNotNull(current);
                if(last != null) {
                    assertEquals(last, current);
                }
                last = current;
            }
        }
    }
    
    
    @Test
    public void testResetPendingEventsButFileDoesNotExist() throws IOException, Exception {
        Path child = root.resolve("child");
        Files.createDirectories(child);
        try(WatchService service = child.getFileSystem().newWatchService()) {
            child.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            
            Files.createFile(child.resolve("test"));
            
            WatchKey key = poll(service);
            assertNotNull(key);
            
            TestUtil.deleteTempDirRecursive(child);
            assertKeyDoesNotReset(key);
        }
    }

    private WatchKey poll(WatchService service) throws InterruptedException {
        return service.poll(10, TimeUnit.SECONDS);
    }
    
    private void assertKeyDoesNotReset(WatchKey key) throws InterruptedException {
        key.pollEvents();
        for(int i =0; i < 200; i++) {
            Thread.sleep(200);
            if(!key.reset()) {
                return;
            }    
        }
        fail("key did not reset");
    }
}
