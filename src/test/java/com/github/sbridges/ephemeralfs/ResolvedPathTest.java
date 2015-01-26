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


import static org.junit.Assert.*;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.EphemeralFsPath;
import com.github.sbridges.ephemeralfs.ResolvedPath;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResolvedPathTest {

    final Random random = new Random();
    
    EphemeralFsPath root;
    
    @Before
    public void setUp() {
        FileSystem fs = EphemeralFsFileSystemBuilder.unixFs().build();
        root = (EphemeralFsPath) fs.getRootDirectories().iterator().next();
    }

    
    @Test
    public void testResolveRoot() throws Exception {
        ResolvedPath resolved = ResolvedPath.resolve(root);
        assertTrue(resolved.didResolve());
        assertSame(resolved.getTarget(), root.getFileSystem().getRoot());
        assertFalse(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertEquals(0, resolved.getSteps().size());
        assertEquals(resolved.getPath(), root.getFileSystem().getRootPath());
    }
    
    @Test
    public void testResolveFile() throws Exception {
        EphemeralFsPath file = root.resolve("file");
        Files.createFile(file);
        ResolvedPath resolved = ResolvedPath.resolve(file);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertEquals(
                 Arrays.asList("file"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), file);
    }
    
    @Test
    public void testResolveFileInRootMissing() throws Exception {
        EphemeralFsPath file = root.resolve("file");
        ResolvedPath resolved = ResolvedPath.resolve(file);

        assertFalse(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        
        assertEquals(
                 Arrays.asList("file"),
                 resolved.getSteps()
                 );
        
        assertEquals(resolved.getPath(), file);
    }
    
    @Test
    public void testResolveMultiLevelFileInRootMissing() throws Exception {
        EphemeralFsPath dir = root.resolve("dir");
        Files.createDirectories(dir);
        EphemeralFsPath file = dir.resolve("file");
        
        ResolvedPath resolved = ResolvedPath.resolve(file);

        assertFalse(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        
        assertEquals(
                 Arrays.asList("dir", "file"),
                 resolved.getSteps()
                 );
        
        assertEquals(resolved.getPath(), file);
    }
    
    @Test
    public void testResolveDir() throws Exception {
        EphemeralFsPath dir = root.resolve("dir");
        Files.createDirectory(dir);
        ResolvedPath resolved = ResolvedPath.resolve(dir);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertEquals(
                 Arrays.asList("dir"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), dir);
    }
    
    @Test
    public void testResolveMultiLevelFile() throws Exception {
        EphemeralFsPath dir = root.resolve("dir");
        Files.createDirectories(dir);
        EphemeralFsPath file = dir.resolve("file");
        Files.createFile(file);

        ResolvedPath resolved = ResolvedPath.resolve(file);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertEquals(
                resolved.getTarget(),
                resolved.getParent().resolve(file.getFileName()).getDestination());
        assertEquals(
                 Arrays.asList("dir", "file"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), file);
    }

    
    @Test
    public void testResolveMultiLevelFileWithDotDots() throws Exception {
        EphemeralFsPath dir = root.resolve("dir");
        Files.createDirectories(dir);
        EphemeralFsPath file = dir.resolve("file");
        Files.createFile(file);

        EphemeralFsPath pathWithDotDots = root
                .resolve("dir")
                .resolve("..")
                .resolve("dir")
                .resolve("file");
        ResolvedPath resolved = ResolvedPath.resolve(pathWithDotDots);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertEquals(
                resolved.getTarget(),
                resolved.getParent().resolve(file.getFileName()).getDestination());
        assertEquals(
                 Arrays.asList("dir", "file"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), file);
    }
    
    
    @Test
    public void testResolveMultiLevelFileWithDotss() throws Exception {
        EphemeralFsPath dir = root.resolve("dir");
        Files.createDirectories(dir);
        EphemeralFsPath file = dir.resolve("file");
        Files.createFile(file);

        EphemeralFsPath pathWithDots = root
                .resolve("dir")
                .resolve(".")
                .resolve("file");
        
        ResolvedPath resolved = ResolvedPath.resolve(pathWithDots);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertEquals(
                resolved.getTarget(),
                resolved.getParent().resolve(file.getFileName()).getDestination());
        assertEquals(
                 Arrays.asList("dir", "file"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), file);
    }
    
    @Test
    public void testResolveMultiLevelAllMissing() throws Exception {
        EphemeralFsPath dir = root.resolve("dir");
        EphemeralFsPath file = dir.resolve("file");

        ResolvedPath resolved = ResolvedPath.resolve(file);
        assertFalse(resolved.didResolve());
        assertFalse(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        
    }
    
    @Test
    public void testResolveAbsoluteSymlinkInRoot() throws Exception {
        EphemeralFsPath file = root.resolve("file");
        Files.createFile(file);
        
        EphemeralFsPath link = root.resolve("link");
        Files.createSymbolicLink(link, file);
        
        ResolvedPath resolved = ResolvedPath.resolve(file);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        
        assertEquals(
                 Arrays.asList("file"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), file);
    }

    @Test
    public void testResolveRelativeSymlinkInRoot() throws Exception {
        EphemeralFsPath file = root.resolve("file");
        Files.createFile(file);
        
        EphemeralFsPath link = root.resolve("link");
        Files.createSymbolicLink(link, file.getFileName());
        
        ResolvedPath resolved = ResolvedPath.resolve(link);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        
        assertEquals(
                 Arrays.asList("file"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), file);
    }
    
    @Test
    public void testResolveSymlinkNoFollow() throws Exception {
        EphemeralFsPath file = root.resolve("file");
        Files.createFile(file);
        
        EphemeralFsPath link = root.resolve("link");
        Files.createSymbolicLink(link, file.getFileName());
        
        ResolvedPath resolved = ResolvedPath.resolve(link, true);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertTrue(resolved.resolvedToSymbolicLink());
        
        assertEquals(
                 Arrays.asList("link"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), link);
    }
    
    @Test
    public void testResolveDotDotRoot() throws Exception {
        ResolvedPath resolved = ResolvedPath.resolve(root.resolve(".."));
        assertTrue(resolved.didResolve());
        assertSame(resolved.getTarget(), root.getFileSystem().getRoot());
        assertFalse(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        assertEquals(0, resolved.getSteps().size());
        assertEquals(resolved.getPath(), root.getFileSystem().getRootPath());
    }

    
    

    @Test
    public void testRelativeSymlinkMulitLevel() throws Exception {
        EphemeralFsPath child = root.resolve("child");
        EphemeralFsPath grandChild = child.resolve("grandChild");
        EphemeralFsPath sibling = root.resolve("sibling");
        
        Files.createDirectory(sibling);
        Files.createDirectories(grandChild);

        EphemeralFsPath symlink = child.resolve("symlink");
        
        EphemeralFsPath realFile = grandChild.resolve("realFile");
        Files.write(realFile, new byte[] {1});
        
        Files.createSymbolicLink(symlink, grandChild.getFileName().resolve(realFile.getFileName()));
        
        
        ResolvedPath resolved = ResolvedPath.resolve(symlink);
        assertTrue(resolved.didResolve());
        assertTrue(resolved.hasValidParent());
        assertFalse(resolved.resolvedToSymbolicLink());
        
        assertEquals(
                 Arrays.asList("child", "grandChild", "realFile"),
                 resolved.getSteps()
                 );
        assertEquals(resolved.getPath(), realFile);
        
    }
    
}
