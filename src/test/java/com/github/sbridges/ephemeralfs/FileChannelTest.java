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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class FileChannelTest {

    final Random random = new Random();
    
    Path root;
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testDeleteOnClose() throws Exception {
        
        //Oddly, in unix Delete on close really 
        //means delete the file on open!
        
        Path testFile = root.resolve("test");
        Files.createFile(testFile);
        try(FileChannel channel = FileChannel.open(testFile, 
                StandardOpenOption.DELETE_ON_CLOSE, 
                StandardOpenOption.WRITE)) {
            
            assertFalse(Files.exists(testFile));
        }
        
        assertFalse(Files.exists(testFile));
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testDeleteOnCloseWindows() throws Exception {
        Path testFile = root.resolve("test");
        Files.createFile(testFile);
        try(FileChannel channel = FileChannel.open(testFile, 
                StandardOpenOption.DELETE_ON_CLOSE, 
                StandardOpenOption.WRITE)) {
            
            assertTrue(Files.exists(testFile));
        }
        
        assertFalse(Files.exists(testFile));
    }
   
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testDeleteOnCloseAnotherChannelOpen() throws Exception {
        Path testFile = root.resolve("test");
        Files.createFile(testFile);

        try(FileChannel extraChannel = FileChannel.open(testFile, StandardOpenOption.WRITE)) {
        	extraChannel.write(ByteBuffer.wrap(new byte[] {4}));
	        try(FileChannel channel = FileChannel.open(testFile, 
	                StandardOpenOption.DELETE_ON_CLOSE, 
	                StandardOpenOption.WRITE)) {
	            
	            assertTrue(Files.exists(testFile));
	        }
	        assertFalse(Files.exists(testFile));
        }
        assertFalse(Files.exists(testFile));
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testDeleteOnCloseAndCreateWindows() throws Exception {
        Path testFile = root.resolve("test");
        try(FileChannel channel = FileChannel.open(testFile, 
                StandardOpenOption.DELETE_ON_CLOSE,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            
            assertTrue(Files.exists(testFile));
        }
        
        assertFalse(Files.exists(testFile));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testDeleteOnCloseAndCreate() throws Exception {
    	
        //Oddly, in unix Delete on close really 
        //means delete the file on open!
        Path testFile = root.resolve("test");
        try(FileChannel channel = FileChannel.open(testFile, 
                StandardOpenOption.DELETE_ON_CLOSE,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            
            assertFalse(Files.exists(testFile));
        }
        
        assertFalse(Files.exists(testFile));
    }
    
    @Test
    public void testReadWrite() throws Exception {
        Path testFile = root.resolve("test");
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer buf = ByteBuffer.wrap("test".getBytes());
            channel.write(buf);
        }
        
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(512);
            assertEquals(4, channel.read(buf));
            buf.flip();
            assertEquals("test", TestUtil.toString(buf));
            
            
            assertEquals(-1, channel.read(ByteBuffer.allocateDirect(512)));
        }
    }

    @Test
    public void testPositionRead() throws Exception {
        byte[] contents = new byte[1024];
        new Random().nextBytes(contents);
        
        Path testFile = root.resolve("test");
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            assertEquals(0, channel.position());
            ByteBuffer buf = ByteBuffer.wrap(contents);
            channel.write(buf);
            assertEquals(contents.length, channel.position());
        }
        
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
            assertEquals(0, channel.position());
            channel.position(100);
            ByteBuffer buf = ByteBuffer.allocate(512);
            assertEquals(512, channel.read(buf));
            assertArrayEquals(Arrays.copyOfRange(contents, 100, 512 + 100), buf.array());
            
            channel.position(4 * contents.length);
            assertEquals(4 * contents.length, channel.position());
            assertEquals(-1, channel.read(ByteBuffer.allocateDirect(100)));
        }
    }
    
    @Test
    public void testPositionAfterAbsoluteRead() throws Exception {
        byte[] contents = new byte[1024];
        new Random().nextBytes(contents);
        
        Path testFile = root.resolve("test");
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer buf = ByteBuffer.wrap(contents);
            channel.write(buf);
            assertEquals(contents.length, channel.position());
        }
        
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(512);
            assertEquals(512, channel.read(buf, 500));
            assertArrayEquals(Arrays.copyOfRange(contents, 500, 500 + 512), buf.array());
            assertEquals(0, channel.position());
        }
    }
    
    @Test
    public void testPositionAfterAbsoluteWrite() throws Exception {
        byte[] contents = new byte[1024];
        new Random().nextBytes(contents);
        
        Path testFile = root.resolve("test");
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer buf = ByteBuffer.wrap(contents);
            channel.write(buf, 500);
            assertEquals(0, channel.position());
        }
    }
    
    
    @Test
    public void testPositionWrite() throws Exception {
        byte[] contents = new byte[1024];
        new Random().nextBytes(contents);
        
        Path testFile = root.resolve("test");
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            channel.position(600);
            ByteBuffer buf = ByteBuffer.wrap(contents);
            channel.write(buf);
            assertEquals(contents.length + 600, channel.position());
        }
        
        
        try(FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
            assertEquals(0, channel.position());
            channel.position(700);
            ByteBuffer buf = ByteBuffer.allocate(512);
            assertEquals(512, channel.read(buf));
            assertArrayEquals(Arrays.copyOfRange(contents, 100, 512 + 100), buf.array());
        }
    }
    
    @Test
    public void testForceOnClose() throws IOException {
        Path testFile = root.resolve("test");
        FileChannel channel = FileChannel.open(testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        channel.close();
        try
        {
            channel.force(true);
            fail();
        } catch(ClosedChannelException  e) {
            //pass
        }
    }
    
    @Test
    public void testTransferFrom() throws Exception {
        byte[] contents = new byte[4071];
        new Random().nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        try(FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
            FileChannel destChannel = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.wrap(contents);
            sourceChannel.write(buf);
            sourceChannel.position(0);
            
            assertEquals(contents.length, destChannel.transferFrom(sourceChannel, 0, contents.length));
            assertEquals(0, destChannel.position());
            assertEquals(contents.length, sourceChannel.position());
            destChannel.position(0);
            buf = ByteBuffer.allocate(contents.length);
            assertEquals(contents.length, destChannel.read(buf));
            assertArrayEquals(contents, buf.array());
        }
    }
    
    
    @Test
    public void testTransferTo() throws Exception {
        byte[] contents = new byte[1038];
        new Random().nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        try(FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
            FileChannel destChannel = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.wrap(contents);
            sourceChannel.write(buf);
            
            assertEquals(contents.length, sourceChannel.transferTo(0, contents.length, destChannel));
            assertEquals(contents.length, destChannel.position());
            assertEquals(contents.length, sourceChannel.position());
            destChannel.position(0);
            buf = ByteBuffer.allocate(contents.length);
            assertEquals(contents.length, destChannel.read(buf));
            assertArrayEquals(contents, buf.array());
        }
    }

    @Test
    public void testTransferToPastPosition() throws Exception {
        byte[] contents = new byte[719];
        new Random().nextBytes(contents);
        
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        try(FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
            FileChannel destChannel = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.wrap(contents);
            sourceChannel.write(buf);
            
            assertEquals(0, sourceChannel.transferTo(contents.length + 100, contents.length, destChannel));
        }
    }
    
    @IgnoreIf(FsType.MAC)
    @Test
    public void testTransferFromSelf() throws Exception {
        byte[] contents = new byte[817];
        new Random().nextBytes(contents);
        
        Path source = root.resolve("source");
       try(FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.wrap(contents);
            sourceChannel.write(buf);
            sourceChannel.position(0);

            sourceChannel.transferFrom(sourceChannel, 0, contents.length);
            sourceChannel.position(0);
            buf = ByteBuffer.allocate(contents.length);
            assertEquals(contents.length, sourceChannel.read(buf));
            assertArrayEquals(contents, buf.array());
        }
    }
    
    @IgnoreIf(FsType.MAC)
    @Test
    public void testTransferToSelf() throws Exception {
        byte[] contents = new byte[817];
        new Random().nextBytes(contents);
        
        Path source = root.resolve("source");
       try(FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.wrap(contents);
            sourceChannel.write(buf);
            sourceChannel.position(0);

            sourceChannel.transferTo(0, contents.length, sourceChannel);
            sourceChannel.position(0);
            buf = ByteBuffer.allocate(contents.length);
            assertEquals(contents.length, sourceChannel.read(buf));
            assertArrayEquals(contents, buf.array());
        }
    }
    
    @Test
    public void testOpenCreateNewExistingNoWrite() throws Exception {
        Path test = root.resolve("test");
        Files.createFile(test);
        FileChannel.open(test,
                //It looks like CREATE_NEW is ignored
                //if we don't have WRITE
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.READ
                ).close();
    }
    
    
    @Test
    public void testOpenCreateNewExistingParentDoesNotExisst() throws Exception {
        try
        {
            Path test = root.resolve("parent").resolve("test");
            Files.createFile(test);
            FileChannel.open(test,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
                    ).close();
           fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @Test
    public void testOpenCreateNewExisting() throws Exception {
        try
        {
            Path test = root.resolve("test");
            Files.createFile(test);
            FileChannel.open(test,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
                    ).close();
           fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @Test
    public void testOpenCreateNewButNotWrite() throws Exception {
        try
        {
            //this needs StandardOpenOptions.WRITE
            FileChannel.open(root.resolve("test"),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.READ
                    );
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testReadWriteIgnoreSymlinks() throws Exception {
        
        //NOFOLLOW_LINKS doesn't seem to have an effect
        
        Path actualDir = root.resolve("actual");
        Files.createDirectory(actualDir);
        Path symlink = Files.createSymbolicLink(root.resolve("symlink"), actualDir);
        Path symlinkFile = symlink.resolve("test");
        
        
        try(FileChannel channel = FileChannel.open(symlinkFile, LinkOption.NOFOLLOW_LINKS, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer buf = ByteBuffer.wrap("test".getBytes());
            channel.write(buf);
        }
        
        try(FileChannel channel = FileChannel.open(symlinkFile, LinkOption.NOFOLLOW_LINKS, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(512);
            assertEquals(4, channel.read(buf));
            buf.flip();
            assertEquals("test", TestUtil.toString(buf));
            
            
            assertEquals(-1, channel.read(ByteBuffer.allocateDirect(512)));
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testWriteToSymlinkExists() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actual = root.resolve("actual");
        Files.createFile(actual);
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        Files.write(link, contents);
        
        assertArrayEquals(contents, Files.readAllBytes(link));
        assertArrayEquals(contents, Files.readAllBytes(actual));
        assertFalse(Files.isSymbolicLink(actual));
        assertTrue(Files.isSymbolicLink(link));
    }  
    
    @IgnoreIfNoSymlink
    @Test
    public void testCreateFileSymlinkNotExists() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actual = root.resolve("actual");
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        try {
            Files.createFile(link);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testWriteToSymlinkNotExists() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actual = root.resolve("actual");
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        Files.write(link, contents);
        
        assertArrayEquals(contents, Files.readAllBytes(link));
        assertArrayEquals(contents, Files.readAllBytes(actual));
        assertFalse(Files.isSymbolicLink(actual));
        assertTrue(Files.isSymbolicLink(link));
    }

    @IgnoreIfNoSymlink
    @Test
    public void testWriteToSymlinkParentNotExists() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actual = root.resolve("nonExistent").resolve("actual");
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        try {
            Files.write(link, contents);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
        
    }    
    
    @IgnoreIfNoSymlink
    @Test
    public void testWriteToSymlinkToSymlinkExists() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actual = root.resolve("actual");
        Files.createFile(actual);
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        Path linkLink = Files.createSymbolicLink(root.resolve("linkLink"), link);
        Files.write(linkLink, contents);
        
        assertArrayEquals(contents, Files.readAllBytes(link));
        assertArrayEquals(contents, Files.readAllBytes(linkLink));
        assertArrayEquals(contents, Files.readAllBytes(actual));
        assertFalse(Files.isSymbolicLink(actual));
        assertTrue(Files.isSymbolicLink(link));
        assertTrue(Files.isSymbolicLink(linkLink));
    }

    @IgnoreIfNoSymlink    
    @Test
    public void testWriteToSymlinkToSymlinkNotExists() throws Exception {
        byte[] contents = new byte[20];
        random.nextBytes(contents);
        
        Path actual = root.resolve("actual");
        Path link = Files.createSymbolicLink(root.resolve("link"), actual);
        Path linkLink = Files.createSymbolicLink(root.resolve("linkLink"), link);
        Files.write(linkLink, contents);
        
        assertArrayEquals(contents, Files.readAllBytes(link));
        assertArrayEquals(contents, Files.readAllBytes(linkLink));
        assertArrayEquals(contents, Files.readAllBytes(actual));
        assertFalse(Files.isSymbolicLink(actual));
        assertTrue(Files.isSymbolicLink(link));
        assertTrue(Files.isSymbolicLink(linkLink));
    }
    
    
    

    @Test
    public void testReadDirectory() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        Files.newDirectoryStream(dir).close();
    }
}
