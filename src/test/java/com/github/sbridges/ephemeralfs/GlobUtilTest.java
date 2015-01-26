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

import java.util.regex.Pattern;

import org.junit.Test;

import com.github.sbridges.ephemeralfs.GlobUtil;

public class GlobUtilTest {

    @Test
    public void testSimple() throws Exception {
        assertMatch("abc", "abc");
        assertMatch("a/c", "a/c");
        assertMatch("a(c", "a(c");
        assertMatch("a\\\\c", "a\\c");
        
        assertNotMatch("abc", "abd");
    }
    
    @Test
    public void testStar() {
        assertMatch("*.java", "test.java");
        assertMatch("test*", "test.java");
        assertMatch("*.java", ".java");
        assertMatch("*", "test");
        assertMatch("*e*", "test");
        assertMatch("t*e*", "test");
        assertMatch("foo/*e*", "foo/test");
        assertMatch("*/*e*", "foo/test");
        assertMatch("foo/*e*", "foo/test");
        assertMatch("*/test", "foo/test");
        
        assertNotMatch("*.java", "testjava");
        assertNotMatch("*.java", "java");
        assertNotMatch("*e*", "foo");
        assertNotMatch("*e*", "foo/test");
        assertNotMatch("fee/*", "foo/test");
    }
    

    @Test
    public void testDoubleStar() {
        assertMatch("**.java", "/foo/test.java");
        assertMatch("**.java", "test.java");
        
        assertNotMatch("**.java", "test.py");
    }
    
    @Test
    public void testQuestion() {
        assertMatch("????.????", "test.java");
        assertMatch("test?java", "test.java");
        assertMatch("t?st/java", "test/java");
        
        assertNotMatch("test?java", "testjava");
        assertNotMatch("test?java", "test/java");
    }

    
    @Test
    public void testRange() throws Exception {
        assertMatch("test.jav[a]", "test.java");
        assertMatch("test.jav[!b]", "test.java");
        assertMatch("test.jav[a-c]", "test.java");
        assertMatch("test.jav[*]", "test.jav*");
        assertMatch("test.jav[?]", "test.jav?");
        assertMatch("test.jav[-]", "test.jav-");
        assertMatch("test.jav[!-]", "test.java");
        assertMatch("test.jav[abce-g]", "test.javf");
        assertMatch("test.jav[e!]", "test.jav!");
        assertMatch("test.jav[*]", "test.jav*");
        
        assertNotMatch("test.jav[b]", "test.java");
        assertNotMatch("test.jav[b-e]", "test.java");
        assertNotMatch("test.jav[b-e]", "test.java");
        assertNotMatch("test.jav[-]", "test.java");
        assertNotMatch("test.jav[*]", "test.java");
        assertNotMatch("test[/]java", "test/java");
        assertNotMatch("test[/]java", "test/java");
        
    }
    
    @Test
    public void testGroup() throws Exception {
        assertMatch("*.{java,class}", "test.java");
        assertMatch("*.{java}", "test.java");
        assertMatch("*.{java}", "test.java");
        assertMatch("*.{java,class}", "test.class");
        assertMatch("*.{j?va,class}", "test.java");
        assertMatch("*.{j[a-z]va,class}", "test.java");
        assertMatch("*.{j*,foo}", "test.java");
        assertMatch("*.{j*,foo}", "test.java");
        
        assertMatch("*.{j\\?va,class}", "test.j?va");
        assertMatch("*.{j\\*va,class}", "test.j*va");
        
    }
    
    @Test
    public void testRegexCars() throws Exception {
        assertMatch(".+", ".+");
        
        
        assertNotMatch(".+", "a");
        
    }
    
    @Test
    public void testEscape() throws Exception {
        assertMatch("\\*.java", "*.java");
        assertMatch("\\?.java", "?.java");
    }
    
    private void assertMatch(String glob, String test) {
        assertMatch(glob, test, true);
    }

    private void assertNotMatch(String glob, String test) {
        assertMatch(glob, test, false);
    }

    public void assertMatch(String glob, String test, boolean expected) {
        String regex = GlobUtil.globToRegex(glob, true);
        assertEquals(
                "glob:" + glob + " regex:" + regex + " input:" + test,
                expected,
                Pattern.matches(
                    regex,
                    test)
                    );    
    
    }

}
