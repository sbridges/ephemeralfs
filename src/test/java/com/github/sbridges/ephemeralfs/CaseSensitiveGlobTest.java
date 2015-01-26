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

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class CaseSensitiveGlobTest {

    Path root;
    
    
    Path camelCase;
    Path lowercase;
    Path UPPERCASE;
    
    Path src;
    
    Set<Path> candidates;
    
    @Before
    public void setup() throws Exception {
        
        src = root.resolve("src");
        Files.createDirectory(src);
        
        camelCase = src.resolve("camelCase");
        lowercase = src.resolve("lowercase");
        UPPERCASE = src.resolve("UPPERCASE");
        
        candidates = new HashSet<>(
                Arrays.asList(camelCase, lowercase, UPPERCASE));
        
        for(Path p : candidates) {
            Files.createFile(p);
        }
        
    }
    
    @IgnoreIf(FsType.UNIX)
    @Test
    public void testRegexIgnoreCase() throws Exception {
        assertRegexFound(
                src, 
                ".*case*",
                camelCase, lowercase, UPPERCASE
                );
    }
   
    @IgnoreIf(FsType.UNIX)
    @Test
    public void testRegexIgnoreCase2() throws Exception {
        assertRegexFound(
                src, 
                ".*cAsE*",
                camelCase, lowercase, UPPERCASE
                );
    }
    
    @IgnoreUnless(FsType.UNIX)
    @Test
    public void testRegexMatchCase() throws Exception {
        assertRegexFound(
                src, 
                ".*case*",
                lowercase
                );
    }
    
    @IgnoreIf(FsType.UNIX)
    @Test
    public void testGlobIgnoreCase() throws Exception {
        assertGlobFound(
                src, 
                "**/*cAse*",
                camelCase, lowercase, UPPERCASE
                );
    }

    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testGlobIgnoreCaseWindows() throws Exception {
        assertGlobFound(
                src, 
                "**\\\\*cAse*",
                camelCase, lowercase, UPPERCASE
                );
    }
    
    
    @IgnoreUnless(FsType.UNIX)
    @Test
    public void testGlobMatchCase() throws Exception {
        assertGlobFound(
                src, 
                "**/*case*",
                lowercase
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
