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

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class PathTest {

    Path root;
    
    @Test
    public void testToRealPathNonExistent() throws Exception {
        try {
            root.resolve("foo").toRealPath();
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }

    @Test
    public void testGetNameEmpty() {
      assertEquals("", root.getFileSystem().getPath("").getName(0).toString());
    }

    @Test
    public void testToRealPathSimple() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        assertEquals(dir, dir.toRealPath());
    }
    
    @Test
    public void testToRealPathDots() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        assertEquals(dir, dir.resolve(".").toRealPath());
    }
    
    @Test
    public void testToRealPathDotsMultiLevel() throws Exception {
        Path child = root.resolve("child");
        Path grandchild = child.resolve("grandchild");
        Files.createDirectories(grandchild);
        
        assertEquals(grandchild, grandchild.resolve("..").resolve("grandchild").resolve(".").toRealPath());
    }
    
    @Test
    public void testToRealPathDotDot() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        assertEquals(dir, dir.resolve("..").resolve("dir").toRealPath());
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testToRealPathSymlink() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        Path link = Files.createSymbolicLink(root.resolve("link"), dir);
        
        assertEquals(dir, link.toRealPath());
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testToRealPathSymlinkNoFollow() throws Exception {
        Path dir = root.resolve("dir");
        Files.createDirectories(dir);
        Path link = Files.createSymbolicLink(root.resolve("link"), dir);
        
        assertEquals(link, link.toRealPath(LinkOption.NOFOLLOW_LINKS));
    }
    
    @Test
    public void testDotAndDotDotInFileNameCompare() throws Exception {
        
        assertFalse(
                root.resolve("a").resolve("..").resolve("b").equals(
                root.resolve("a").resolve("b"))
                );
        
        assertFalse(
                root.resolve("a").resolve("..").resolve("b").equals(
                root.resolve("b"))
                );
    }
    
    @Test
    public void testNormalizeMultipleDotDots() {
        FileSystem fs = root.getFileSystem();
        assertEquals(fs.getPath("../.."), fs.getPath("../..").normalize());
        assertEquals(fs.getPath("../.."), fs.getPath(".././..").normalize());
        assertEquals(fs.getPath("../../a/b/c"), fs.getPath("../../a/b/c").normalize());
        assertEquals(fs.getPath("../../a/b"), fs.getPath("../../a/b/c/..").normalize());
        assertEquals(fs.getPath("../../a/b"), fs.getPath("../../a/b/c/./..").normalize());
    }

    @Test
    public void testResolveRootRelative() {
        FileSystem fs = root.getFileSystem();
        assertFalse(fs.getPath("").isAbsolute());
        assertFalse(fs.getPath("").resolve("a").isAbsolute());
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testResolveDotRelative() throws Exception {
        Path a = root.resolve("a");
        Path b = root.resolve("./b/../a");

        Files.createFile(a);
        assertTrue(Files.exists(a));
        assertFalse(Files.exists(b));
    }
}
