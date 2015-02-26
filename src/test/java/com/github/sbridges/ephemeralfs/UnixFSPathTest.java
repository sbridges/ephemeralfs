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

import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;
import com.github.sbridges.ephemeralfs.junit.RunWithTypes;


@RunWithTypes(FsType.UNIX)
@RunWith(MultiFsRunner.class)
public class UnixFSPathTest {

    Path root;
    
    @Test
    public void testGetFileSystem() throws Exception {
        Path simple = toPath("test");
        assertEquals(getFileSystem(), simple.getFileSystem());
    }
    
    @Test
    public void testParse() throws Exception {
        assertEquals(
                toPath("a/b/c"),
                toPath("a", "b", "c")
                );
    }
    
    @Test
    public void testParseAbs() throws Exception {
        assertEquals(
                toPath("/a/b/c"),
                toPath("/", "a", "b", "c")
                );
    }
    
    @Test
    public void testParseAbsTrailingSlashToString() throws Exception {
        assertEquals(
                toPath("/a/b/c/").toString(),
                toPath("/a/b/c").toString()
                );
    }
    
    @Test
    public void testParseGratuitiousSlashesAndEmpty() throws Exception {
        assertEquals(
                toPath("/a/b/c"),
                toPath("/", "/a/", "", "/b", "c/", "")
                );
    }
    
    @Test
    public void testParseExtraBlank() throws Exception {
        assertEquals(
                toPath("a/b/c"),
                toPath("a", "b", "c", "")
                );
    }
    
    @Test
    public void testNonAbsolutePath() throws Exception {
        Path simple = toPath("test");
        assertEquals(1, simple.getNameCount());
        assertEquals(simple, simple.getName(0));
        assertFalse(simple.isAbsolute());
    }
    
    @Test
    public void testEmptyPathsIsNotAbsolute() throws Exception {
        assertFalse(toPath("", "").isAbsolute());
    }
    
    @Test
    public void testEmptyPaths() throws Exception {
        assertEquals(
                toPath("", ""),
                toPath("")
                );
    }
    
    @Test
    public void testEmptyPathsSize() throws Exception {
        assertEquals(
                1,
                toPath("").getNameCount()
                );
    }
    
    @Test
    public void testSomeEmptyPaths() throws Exception {
        assertEquals(
                toPath("", "", "a", "", ""),
                toPath("a")
                );
    }    
    
    @Test
    public void testSomeEmptyPaths2() throws Exception {
        assertEquals(
                toPath("", "", "a", "", "", "", "b", "", ""),
                toPath("a", "b")
                );
    }
    
    @Test
    public void testEmptyPath() throws Exception {
        Path empty = toPath("");
        assertEquals(1, empty.getNameCount());
        assertFalse(empty.isAbsolute());
        assertTrue(empty.iterator().hasNext());
        try {
            empty.subpath(0, 0);
            fail();
        } catch(IllegalArgumentException e) {
            //pass
        }
    }
    
    @Test
    public void testSingleSubpath() throws Exception {
        Path single = toPath("a");
        assertEquals(1, single.getNameCount());
        assertFalse(single.isAbsolute());
        assertTrue(single.iterator().hasNext());
        try {
            single.subpath(0, 0);
            fail();
        } catch(IllegalArgumentException e) {
            //pass
        }
    }
    
    @Test
    public void testSlashSubpath() throws Exception {
        Path single = toPath("/");
        assertEquals(0, single.getNameCount());
        assertTrue(single.isAbsolute());
        assertFalse(single.iterator().hasNext());
        try {
            single.subpath(0, 0);
            fail();
        } catch(IllegalArgumentException e) {
            //pass
        }
    }
    
    @Test
    public void testAbsolutePath() throws Exception {
        Path simple = toPath("/test");
        assertEquals(1, simple.getNameCount());
        assertTrue(simple.isAbsolute());
    }
    
    @Test
    public void testAbsolutePathTrailingSlash() throws Exception {
        Path simple = toPath("/test/");
        assertEquals(1, simple.getNameCount());
        assertTrue(simple.isAbsolute());
    }
    
    @Test
    public void testAbsoluteParent() throws Exception {
        Path simple = toPath("/test/bar");
        assertEquals(2, simple.getNameCount());
        assertTrue(simple.getParent().isAbsolute());
    }
    
    @Test
    public void testGetRootParent() throws Exception {
        assertNull(toPath("/").getParent());
    }

    @Test
    public void testNonAbsolutePart() throws Exception {
        Path simple = toPath("/test");
        assertEquals(1, simple.getNameCount());
        assertFalse(simple.getName(0).isAbsolute());
    }
    
    @Test
    public void testGetRoot() throws Exception {
        Path simple = toPath("/test");
        Path root = simple.getRoot();
        assertTrue(root.isAbsolute());
        assertEquals(root, toPath("/"));
    }

    @Test
    public void testGetRootNonAbsolute() throws Exception {
        Path simple = toPath("test");
        Path root = simple.getRoot();
        assertNull(root);
    }
    
    @Test
    public void testGetFileName() throws Exception {
        Path expected = toPath("test");
        assertEquals(expected,  toPath("/test").getFileName());
        assertEquals(expected,  toPath("/foo/test").getFileName());
        assertEquals(expected,  toPath("test").getFileName());
        assertEquals(expected,  toPath("foo/test").getFileName());
        assertEquals(expected,  toPath("foo", "test").getFileName());
        assertEquals(expected,  toPath("foo", "/test/").getFileName());
        assertEquals(expected,  toPath("foo", "something/test").getFileName());
    }
    
    @Test
    public void testGetFileNameEmpty() throws Exception {
        assertEquals(null, toPath("/").getFileName());
        assertEquals(toPath(""), getFileSystem().getPath("").getFileName());
    }

    @Test
    public void testGetParent() throws Exception {
        Path expected = toPath("/foo/bar");
        assertEquals(expected,  toPath("/foo/bar/test").getParent());
        assertEquals(expected,  toPath("/foo/bar/.").getParent());
        assertEquals(expected,  toPath("/foo/bar/..").getParent());
    }

    @Test
    public void testNonAbsoluteParent() throws Exception {
        Path expected = toPath("foo/bar");
        assertEquals(expected,  toPath("foo/bar/test").getParent());
        assertEquals(expected,  toPath("foo/bar/.").getParent());
        assertEquals(expected,  toPath("foo/bar/..").getParent());
    }

    @Test
    public void assertRootParent() throws Exception {
        assertEquals(
                toPath("/"),
                toPath("/foo").getParent());
        
    }
    
    @Test
    public void testNullParent() throws Exception {
        assertNull(toPath("foo").getParent());
        assertNull(toPath("/").getParent());
    }

    @Test
    public void testNameCount() throws Exception {
        assertEquals(0, toPath("/").getNameCount());
        assertEquals(1, toPath("").getNameCount());
        assertEquals(1, toPath("a").getNameCount());
        assertEquals(2, toPath("/a/b").getNameCount());
        assertEquals(2, toPath("a", "b", "").getNameCount());
        assertEquals(2, toPath("a", "b", "/", "").getNameCount());
        assertEquals(4, toPath("a", "b", ".", "..").getNameCount());
    }
    
    @Test
    public void testGetName() throws Exception {
        Path path = toPath("a", "b", "c", "..");
        assertEquals(toPath("a"), path.getName(0));
        assertEquals(toPath("b"), path.getName(1));
        assertEquals(toPath("c"), path.getName(2));
        assertEquals(toPath(".."), path.getName(3));
    }
    
    @Test
    public void testGetNameAbs() throws Exception {
        Path path = toPath("/a/b");
        assertFalse(path.getName(0).isAbsolute());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetNameNegativeThrows() throws Exception {
        toPath("a").getName(-1);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetNamePastIndexThrows() throws Exception {
        toPath("a").getName(1);
    }

    @Test
    public void testSubPath() throws Exception {
        Path path = toPath("a", "b", "c", "..");
        assertEquals(toPath("a"), path.subpath(0, 1));
        assertEquals(toPath("b"), path.subpath(1, 2));
        assertEquals(toPath("b", "c"), path.subpath(1, 3));
    }
    
    @Test
    public void testSubPathAbs() throws Exception {
        Path path = toPath("/a");
        assertFalse(path.subpath(0, 1).isAbsolute());
    }
    
    @Test
    public void testStartsWith() throws Exception {
        Path path = toPath("a", "b", "c");
        assertTrue(path.startsWith(toPath("a")));
        assertTrue(path.startsWith(toPath("a", "b")));
        assertTrue(path.startsWith(toPath("a", "b", "c")));
        assertTrue(path.startsWith(toPath("a", "b", "c", "")));
        assertTrue(path.startsWith(path));
        
        assertFalse(path.startsWith(toPath("b")));
    }
    
    @Test
    public void testStartsWithAbs() throws Exception {
        Path path = toPath("/a", "b", "c");
        assertTrue(path.isAbsolute());
        assertTrue(path.startsWith(toPath("/a")));
        assertTrue(path.startsWith(toPath("/")));
        
        assertFalse(path.startsWith(toPath("a")));
    }
    
    @Test
    public void testStartsWithString() throws Exception {
        Path path = toPath("a", "b", "c");
        assertTrue(path.startsWith("a"));
        assertTrue(path.startsWith("a/b"));
        assertTrue(path.startsWith("a/b/"));
        
        assertFalse(path.startsWith("b"));
    }
    
    @Test
    public void testStartsWithDotDot() throws Exception {
        Path path = toPath("a", "b", "..");
        assertTrue(path.startsWith(toPath("a")));
        assertTrue(path.startsWith(toPath("a", "b")));
        assertTrue(path.startsWith(toPath("a", "b", "..")));
    }
    
    @Test
    public void testStartsWithStringDotDot() throws Exception {
        Path path = toPath("a", "b", "c", "..");
        assertTrue(path.startsWith("a"));
        assertTrue(path.startsWith("a/b"));
        assertTrue(path.startsWith("a/b/c/.."));
    }

    @Test
    public void testEndsWith() throws Exception {
        Path path = toPath("a", "b", "c");
        
        assertTrue(path.endsWith(toPath("a", "b", "c")));
        assertTrue(path.endsWith(toPath("a", "b", "c", "")));
        assertTrue(path.endsWith(toPath("b", "c")));
        assertTrue(path.endsWith(toPath("c")));
        
        assertFalse(path.endsWith(toPath("d")));
        assertFalse(path.endsWith(toPath("/a/b/c")));
        assertFalse(path.endsWith(toPath("/a/b/c/d")));
    }
    
    @Test
    public void testEndsWithAbs() throws Exception {
        Path path = toPath("/", "a", "b", "c");
        
        assertTrue(path.endsWith(toPath("a", "b", "c")));
        assertTrue(path.endsWith(toPath("/", "a", "b", "c")));
        assertTrue(path.endsWith(toPath("a", "b", "c", "")));
        assertTrue(path.endsWith(toPath("b", "c")));
        assertTrue(path.endsWith(toPath("c")));
        
        assertFalse(path.endsWith(toPath("d")));
        assertFalse(path.endsWith(toPath("/")));
    }
    
    @Test
    public void testEndsWithString() throws Exception {
        Path path = toPath("a", "b", "c");
        
        assertTrue(path.endsWith("a/b/c"));
        assertTrue(path.endsWith("b/c"));
        assertTrue(path.endsWith("b/c/"));
        assertTrue(path.endsWith("c"));
        assertTrue(path.endsWith("c/"));
        
        
        assertFalse(path.endsWith("d"));
    }
    
    @Test
    public void testNoramlizeSelf() throws Exception {
        assertEquals(
                toPath("a", "b"),
                toPath("a", "b").normalize()
                );
    }
    
    @Test
    public void testNoramlizeSelfAbs() throws Exception {
        assertEquals(
                toPath("/a", "b"),
                toPath("/", "a", "b").normalize()
                );
    }
    
    
    @Test
    public void testNormalize() throws Exception {
        assertEquals(
                toPath("a", "b"),
                toPath("a", ".", "b").normalize()
                );
        assertEquals(
                toPath("a", "b"),
                toPath("a", ".", ".", "b").normalize()
                );
        assertEquals(
                toPath("a"),
                toPath("a", "b", "..").normalize()
                );
        assertEquals(
                toPath(".."),
                toPath("a", "..", "..").normalize()
                );  
        assertEquals(
                toPath(""),
                toPath("a", "..", ".").normalize()
                );
        assertEquals(
                toPath("..", "b"),
                toPath("..", ".", "b").normalize()
                );  
        assertEquals(
                toPath("b", "..", "a").normalize(),
                toPath("a")
                );    
        assertEquals(
                toPath("b", "c", "..", ".", "..", "a").normalize(),
                toPath("a")
                );     
        assertEquals(
                toPath("/", "..").normalize(),
                toPath("/")
                );
        assertEquals(
                toPath("/", "..", "..").normalize(),
                toPath("/")
                );  
        assertEquals(
                toPath("/", "..", "..", "b").normalize(),
                toPath("/b")
                );  
    }
    
    @Test
    public void testResolveAbsOtherAbs() throws Exception {
        Path abs = toPath("/", "a");
        assertEquals(
                abs,
                toPath("a").resolve(abs)
                );
    }
    
    @Test
    public void testResolve() throws Exception {
        assertEquals(
                toPath("a", "b"),
                toPath("a").resolve("b")
                );
    }
    
    @Test
    public void testResolveOtherEmpty() throws Exception {
        assertEquals(
                toPath("a"),
                toPath("a").resolve("")
                );
    }
    
    @Test
    public void testResolveOtherEmptyPath() throws Exception {
        assertEquals(
                toPath("a"),
                toPath("a").resolve(toPath(""))
                );
    }
    
    @Test
    public void testResolveOtherEmptyEmpty() throws Exception {
        assertEquals(
                toPath("a"),
                toPath("a").resolve(toPath("", ""))
                );
    }
    
    @Test
    public void testResolveAbs() throws Exception {
        assertEquals(
                toPath("/a", "b"),
                toPath("/a").resolve("b")
                );
    }
    
    
    @Test
    public void testResolveWithDots() throws Exception {
        assertEquals(
                toPath("a", "b", "."),
                toPath("a").resolve(toPath("b", "."))
                );
    }
    
    @Test
    public void testResolveSibling() throws Exception {
        assertEquals(
                toPath("a", "b", "d"),
                toPath("a", "b", "c").resolveSibling("d")
                );
    }
    
    @Test
    public void testResolveSiblingOtherAbs() throws Exception {
        assertEquals(
                toPath("/a"),
                toPath("a", "b", "c").resolveSibling("/a")
                );
    }
    
    @Test
    public void testResolveSiblingThisEmpty() throws Exception {
        assertEquals(
                toPath("a"),
                toPath("").resolveSibling("a")
                );
    }
    
    @Test
    public void testResolveSiblingThisRoot() throws Exception {
        assertEquals(
                toPath("a"),
                toPath("/").resolveSibling("a")
                );
    }
    
    @Test
    public void testResolveSiblingOtherEmpty() throws Exception {
        assertEquals(
                toPath("a"),
                toPath("a", "b").resolveSibling("")
                );
    }
    
    @Test
    public void testResolveSiblingParentAndOtherEmpty() throws Exception {
        assertEquals(
                toPath(""),
                toPath("a").resolveSibling("")
                );
    }
    
    @Test
    public void testRelativizeEquals() throws Exception {
        assertEquals(
                toPath(""),
                toPath("a").relativize(toPath("a"))
        );
        assertEquals(
                toPath(""),
                toPath("/a").relativize(toPath("/a"))
        );
    }
    
    @Test
    public void testRelativizeEqualsSize() throws Exception {
        assertEquals(
                1,
                toPath("a").relativize(toPath("a")).getNameCount()
        );
    }
    
    @Test
    public void testRelativizeDifferingRoot() throws Exception {
        try {
            toPath("/a").relativize(toPath("a"));
            fail();
        } catch(IllegalArgumentException e) {
            //ignore
        }
        
        try {
            toPath("a").relativize(toPath("/a"));
            fail();
        } catch(IllegalArgumentException e) {
            //ignore
        }
    }
    
    @Test
    public void testRelativizeSimpleAbs() throws Exception {
        assertEquals(
                toPath("c/d"),
                toPath("/a/b").relativize(toPath("/a/b/c/d"))
        );
    }
    
    @Test
    public void testRelativizeSimpleAbs2() throws Exception {
        assertEquals(
                toPath("../.."),
                toPath("/a/b/c/d").relativize(toPath("/a/b"))
        );
    }
    
    @Test
    public void testRelativizeSimple() throws Exception {
        assertEquals(
                toPath("c/d"),
                toPath("a/b").relativize(toPath("a/b/c/d"))
        );
    }
    
    @Test
    public void testRelativizeSimple2() throws Exception {
        assertEquals(
                toPath("../.."),
                toPath("a/b/c/d").relativize(toPath("a/b"))
        );
    }
    
    @Test
    public void testRelativizeSiblings() throws Exception {
        assertEquals(
                toPath("../x"),
                toPath("a/b").relativize(toPath("a/x"))
        );
    }
    
    @Test
    public void testRelativizeRelative() throws Exception {
        assertEquals(
                toPath("../../x"),
                toPath("a/b/..").relativize(toPath("a/x/"))
        );
    }
    
    @Test
    public void testRelativizeDot() throws Exception {
        assertEquals(
                toPath("../../x"),
                toPath("a/b/.").relativize(toPath("a/x/"))
        );
    }

    @Test
    public void testToAbsolutePathAbs() throws Exception {
        assertEquals(
                toPath("/a/b"),
                toPath("/a/b").toAbsolutePath()
                );
    }

    @Test
    public void testNotCaseSensitive() throws Exception {
		assertFalse(
				toPath("a").equals(
				toPath("A"))
				);
		
	}

    @Test
    public void testNullByte() throws Exception {
        try
        {
            root.resolve("a\0a");
            fail();
        } catch(InvalidPathException e) {
           //pass
        }
	}
    
    FileSystem getFileSystem() {
        return root.getFileSystem();
    }

    Path toPath(String string, String... rest) {
        return getFileSystem().getPath(string, rest);
    }

}

