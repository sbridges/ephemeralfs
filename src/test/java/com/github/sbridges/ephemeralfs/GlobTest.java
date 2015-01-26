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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class GlobTest {

    Path root;
    
    
    Path path_java;
    Path path_py;
    Path path_c;
    
    Path src;
    
    Set<Path> candidates;
    
    @Before
    public void setup() throws Exception {
        
        src = root.resolve("src");
        Files.createDirectory(src);
        
        path_java = src.resolve("path.java");
        path_py = src.resolve("path.py");
        path_c = src.resolve("path.c");
        
        candidates = new HashSet<>(
                Arrays.asList(path_java, path_py, path_c));
        
        for(Path p : candidates) {
            Files.createFile(p);
        }
        
    }
    
    @Test
    public void testSimpleRegex() throws Exception {
        assertRegexFound(
                src, 
                ".*path.*",
                path_java, path_py, path_c
                );
    }
    
    
    @Test
    public void testRegex() throws Exception {
        assertRegexFound(
                src, 
                "ath.*"
                
                );
    }
    
    @Test
    public void testRegexOverDir() throws Exception {
        assertRegexFound(
                src, 
                ".+path.*",
                path_java, path_py, path_c
                );
    }

    @Test
    public void testSimpleGlobWindowsSeperator() throws Exception {
        assertGlobFound(
                src, 
                "**\\path.*",
                path_java, path_py, path_c
                );
    }
    
    
    @Test
    public void testSimpleGlob() throws Exception {
        assertGlobFound(
                src, 
                "**/path.*",
                path_java, path_py, path_c
                );
    }
    
    @Test
    public void testGroupGlob() throws Exception {
        assertGlobFound(
                src, 
                "**/path.{py,c}",
                path_py, path_c
                );
    }
    
    @Test
    public void testGroupWildGlob() throws Exception {
        assertGlobFound(
                src, 
                "**/path.{?y,c}",
                path_py, path_c
                );
    }
    
    private void assertRegexFound(Path dir, String glob, Path...paths) throws IOException {
        FileSystem fs = dir.getFileSystem();
        final PathMatcher matcher = fs.getPathMatcher("regex:" + glob);
        assertFound(
                matcher,
                paths);
        
    }
    
    private void assertGlobFound(Path dir, String glob, Path...paths) throws IOException {
         FileSystem fs = dir.getFileSystem();
        final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
        assertFound(
                matcher,
                paths);
        
    }

    private void assertFound(PathMatcher matcher, Path...paths) {
        Set<Path> actual = new HashSet<>();
        Set<Path> expected = new HashSet<>();
        for(Path p : candidates ) {
            if(matcher.matches(p) && !actual.add(p)) {
                throw new AssertionError("dupe");
            }
        }
        for(Path p : paths) {
            if(!expected.add(p)) {
                throw new AssertionError("dupe");
            }
        }
        
        assertEquals(expected, actual);
    }
    
   
}
