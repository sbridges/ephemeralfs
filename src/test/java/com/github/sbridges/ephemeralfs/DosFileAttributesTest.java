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
import static org.junit.Assume.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class DosFileAttributesTest {

    Path root;
    
    @Before
    public void checkDos() {
        //some linux file systems do not support dos attributes, some do
        try {
            Files.readAttributes(root, DosFileAttributes.class).isSystem();
        } catch(IOException e) {
            assumeTrue(false);
        }
    }
    
    @After
    public void tearDown() throws IOException {
        
        //clean up and set things not 
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path,
                    BasicFileAttributes attrs) throws IOException {
                clearReadOnly(path);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException exc)
                    throws IOException {
                clearReadOnly(path);
                return FileVisitResult.CONTINUE;
            }
            
        });
    }
    
    @Test
    public void testReadDos() throws Exception {
        DosFileAttributes attributes = Files.readAttributes(root, DosFileAttributes.class);
        assertNotNull(attributes);
        assertNotNull(attributes.creationTime());
    }
    
    @Test
    public void testSetGetSystem() throws Exception {
        
        for(Path path : Arrays.asList(
                Files.createDirectory(root.resolve("dir")),
                Files.createFile(root.resolve("file")))) {
            DosFileAttributes attributes = Files.readAttributes(path, DosFileAttributes.class);
            assertFalse(attributes.isSystem());
            
            
            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            view.setSystem(true);
            
            attributes = Files.readAttributes(path, DosFileAttributes.class);
            assertTrue(attributes.isSystem());
        }
        
    }
    
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testSetGetArchiveUnix() throws Exception {
        for(Path path : Arrays.asList(
                Files.createDirectory(root.resolve("dir")),
                Files.createFile(root.resolve("file")))) {
        
            DosFileAttributes attributes = Files.readAttributes(path, DosFileAttributes.class);
            assertFalse(attributes.isArchive());
            
            
            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            view.setArchive(true);
            
            attributes = Files.readAttributes(path, DosFileAttributes.class);
            assertTrue(attributes.isArchive());
        }
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testSetGetArchiveWindowsFile() throws Exception {
        
    
       Path path = Files.createFile(root.resolve("file"));
    
        DosFileAttributes attributes = Files.readAttributes(path, DosFileAttributes.class);
        assertTrue(attributes.isArchive());
        
        
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
        view.setArchive(false);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        assertFalse(attributes.isArchive());
    
        Files.write(path, new byte[] {1});
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        assertTrue(attributes.isArchive());
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testSetGetArchiveWindowsDir() throws Exception {
        
       Path path = Files.createDirectory(root.resolve("dir"));
    
        DosFileAttributes attributes = Files.readAttributes(path, DosFileAttributes.class);
        assertFalse(attributes.isArchive());
        
        
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
        view.setArchive(true);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        assertTrue(attributes.isArchive());
    }
    
    
    @Test
    public void testSetGetReadOnly() throws Exception {
        for(Path path : Arrays.asList(
                Files.createDirectory(root.resolve("dir")),
                Files.createFile(root.resolve("file")))) {
            DosFileAttributes attributes = Files.readAttributes(path, DosFileAttributes.class);
            assertFalse(attributes.isReadOnly());
            
            
            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            try {
                view.setReadOnly(true);
                
                attributes = Files.readAttributes(path, DosFileAttributes.class);
                assertTrue(attributes.isReadOnly());
            } finally {
                view.setReadOnly(false);
            }
        }
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testReadOnlyCanStillWriteNotWindows() throws Exception {
        Path path = Files.createFile(root.resolve("file"));
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
        view.setReadOnly(true);
        Files.write(path, new byte[] {1});
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testReadOnlyCanStillWriteWindowsFile() throws Exception {
        Path path = Files.createFile(root.resolve("file"));
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
    
        view.setReadOnly(true);
        try {
            Files.write(path, new byte[] {1});
            fail();
        } catch(AccessDeniedException e) {
            //pass
        }
        try {
            try(OutputStream os = Files.newOutputStream(path)) {}
            fail();
        } catch(AccessDeniedException e) {
            //pass
        }
        try {
            Files.delete(path);
            fail();
        } catch(AccessDeniedException e) {
            //pass
        }
        Files.move(path, root.resolve("moveTo"));
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testReadOnlyCanStillWriteWindowsDir() throws Exception {
        Path path = Files.createDirectory(root.resolve("dir"));
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
    
        view.setReadOnly(true);
        try {
            Files.delete(path);
            fail();
        } catch(AccessDeniedException e) {
            //pass
        }
        Files.move(path, root.resolve("moveTo"));
    }
    
    @Test
    public void testSetGetHidden() throws Exception {
        for(Path path : Arrays.asList(
                Files.createDirectory(root.resolve("dir")),
                Files.createFile(root.resolve("file")))) {
        
            DosFileAttributes attributes = Files.readAttributes(path, DosFileAttributes.class);
            assertFalse(attributes.isHidden());
            
            
            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            view.setHidden(true);
            
            attributes = Files.readAttributes(path, DosFileAttributes.class);
            assertTrue(attributes.isHidden());
        }
    }
    
    @Test
    public void testViaSetGet() throws Exception {
        for(String attribute : new String[] {"dos:hidden", "dos:readonly", "dos:system"}) {
            for(Path path : Arrays.asList(
                    Files.createDirectory(root.resolve("dir")),
                    Files.createFile(root.resolve("file")))) {
                
                
                    assertSame("attribute:" + attribute, Boolean.FALSE, Files.getAttribute(path, attribute));
                    Files.setAttribute(path, attribute, Boolean.TRUE);
                    assertSame(Boolean.TRUE, Files.getAttribute(path, attribute));
                    
                    clearReadOnly(path);
                    Files.delete(path);
            }
        }
    }

    private void clearReadOnly(Path path) throws IOException {
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
        view.setReadOnly(false);
    }
}
