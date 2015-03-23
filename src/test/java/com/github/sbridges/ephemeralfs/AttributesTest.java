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

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class AttributesTest {

    long someTime = 1425000000000L;
    Path root;

    @Before
    public void setUp() {}

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testSupports() throws Exception {
        assertTrue(root.getFileSystem().supportedFileAttributeViews().contains("basic"));
        assertTrue(root.getFileSystem().supportedFileAttributeViews().contains("posix"));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testSupportsFileStore() throws Exception {
        for(FileStore fs : root.getFileSystem().getFileStores()) {
            assertTrue(fs.supportsFileAttributeView(BasicFileAttributeView.class));
            assertTrue(fs.supportsFileAttributeView(PosixFileAttributeView.class));
            //on linux, some file stores support DOS, some don't
            //assertFalse(fs.supportsFileAttributeView(DosFileAttributeView.class));
            assertTrue(fs.supportsFileAttributeView("basic"));
            assertTrue(fs.supportsFileAttributeView("posix"));
        }
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testSupportsWin() throws Exception {
        assertTrue(root.getFileSystem().supportedFileAttributeViews().contains("basic"));
        assertTrue(root.getFileSystem().supportedFileAttributeViews().contains("dos"));
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testSupportsFileStoreWin() throws Exception {
        for(FileStore fs : root.getFileSystem().getFileStores()) {
            assertTrue(fs.supportsFileAttributeView(BasicFileAttributeView.class));
            assertFalse(fs.supportsFileAttributeView(PosixFileAttributeView.class));
            assertTrue(fs.supportsFileAttributeView(DosFileAttributeView.class));
            assertTrue(fs.supportsFileAttributeView("basic"));
            assertTrue(fs.supportsFileAttributeView("dos"));
        }
    }
    
    @Test
    public void testGetWithNoView() throws Exception {
        Map<String, Object> answer = root.getFileSystem().provider().readAttributes(root, "isDirectory");
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("isDirectory", true);
        assertEquals(
                expected,
                answer);
    }
    
    @Test
    public void testGetWithView() throws Exception {
        Map<String, Object> answer = root.getFileSystem().provider().readAttributes(root, "basic:isDirectory");
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("isDirectory", true);
        assertEquals(
                expected,
                answer);
    }
    
    @Test
    public void testGetMultiple() throws Exception {
        Map<String, Object> answer = root.getFileSystem().provider().readAttributes(root, "basic:isRegularFile,isDirectory");
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("isDirectory", true);
        expected.put("isRegularFile", false);
        assertEquals(
                expected,
                answer);
    }
    
    @Test
    public void testGetMultipleNoView() throws Exception {
        Map<String, Object> answer = root.getFileSystem().provider().readAttributes(root, "isRegularFile,isDirectory");
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("isDirectory", true);
        expected.put("isRegularFile", false);
        assertEquals(
                expected,
                answer);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testGetMultiplePosix() throws Exception {
        Map<String, Object> answer = root.getFileSystem().provider().readAttributes(root, "posix:isRegularFile,isDirectory");
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("isDirectory", true);
        expected.put("isRegularFile", false);
        assertEquals(
                expected,
                answer);
    }

    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testGetPosixFailsWindows() throws Exception {
        try {
            Map<String, Object> answer = root.getFileSystem().provider().readAttributes(root, "posix:isRegularFile");
            fail();
        } catch(UnsupportedOperationException e) {
            assertEquals(e.getMessage(), "View 'posix' not available");
        }
    }

    
    @Test
    public void testReadBasic() throws Exception {
        BasicFileAttributes attributes = Files.readAttributes(root, BasicFileAttributes.class);
        assertNotNull(attributes);
        assertNotNull(attributes.creationTime());
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testReadPosix() throws Exception {
        PosixFileAttributes attributes = Files.readAttributes(root, PosixFileAttributes.class);
        assertNotNull(attributes);
        assertNotNull(attributes.creationTime());
    }
    
   
    @Test
    public void testCreationTime() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        FileTime ft = (FileTime) Files.getAttribute(file, "basic:creationTime");
        assertFalse(ft.toMillis() == someTime);
    }

    @Test
    public void testBasicCreationTime() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        FileTime ft = (FileTime) Files.getAttribute(file, "basic:creationTime");
        assertFalse(ft.toMillis() == someTime);
    }
    
    @Test
    public void testLastModifiedTime() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        FileTime ft = (FileTime) Files.getAttribute(file, "lastModifiedTime");
        assertFalse(ft.toMillis() == someTime);
        Files.setAttribute(file, "lastModifiedTime", FileTime.fromMillis(someTime));
        ft = (FileTime) Files.getAttribute(file, "lastModifiedTime");
        assertTrue("actual:" + ft.toMillis(), ft.toMillis() == someTime);
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testReadLastModifiedTimeSymlink() throws Exception {
        Path link = Files.createSymbolicLink(root.resolve("link"), root.resolve("nonexistent"));
        
        FileTime ft = (FileTime) Files.getAttribute(link, "lastModifiedTime", LinkOption.NOFOLLOW_LINKS);
        assertFalse(ft.toMillis() == someTime);
    }
    
    
    @IgnoreIfNoSymlink
    @Test
    public void testLastModifiedTimeSymlink() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createSymbolicLink(root.resolve("link"), root.resolve("file"));
        
        Files.setAttribute(file, "lastModifiedTime", FileTime.fromMillis(someTime));
        FileTime follow = (FileTime) Files.getAttribute(link, "lastModifiedTime");
        assertTrue("actual:" + follow.toMillis(), follow.toMillis() == someTime);
        
        Files.setAttribute(file, "lastModifiedTime", FileTime.fromMillis(someTime));
        FileTime ftNoFollow = (FileTime) Files.getAttribute(link, "lastModifiedTime", LinkOption.NOFOLLOW_LINKS);
        assertTrue("actual:" + ftNoFollow.toMillis(), ftNoFollow.toMillis() != someTime);
    }
    
    @Test
    public void testInvalid() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        
        try {
            Files.getAttribute(file, "notFound");
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals(e.getMessage(), "'notFound' not recognized");
        }
        
        try {
            Files.getAttribute(file, "basic:notFound");
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals(e.getMessage(), "'notFound' not recognized");
        }
        
        try {
            Files.getAttribute(file, "foo:notFound");
            fail();
        } catch(UnsupportedOperationException e) {
            assertEquals(e.getMessage(), "View 'foo' not available");
        }
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testFileKey() throws Exception {
        Path file1 = Files.createFile(root.resolve("file1"));
        Path file2 = Files.createFile(root.resolve("file2"));
        
        assertNotNull(
                Files.getAttribute(file1, "fileKey")
                );
        
        assertNotEquals(
                Files.getAttribute(file1, "fileKey"),
                Files.getAttribute(file2, "fileKey")
                );
        
        try {
            Files.setAttribute(file1, "fileKey", "rejected");
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals("'basic:fileKey' not recognized", e.getMessage());
        }
    }
    
    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testFileKeyWindows() throws Exception {
        Path file1 = Files.createFile(root.resolve("file1"));
        
        assertNull(
                Files.getAttribute(file1, "fileKey")
                );
        
    }

    
    @IgnoreIfNoSymlink
    @Test
    public void testFileKeySymlink() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createSymbolicLink(root.resolve("link"), file);
        
        assertEquals(
                Files.getAttribute(file, "fileKey"),
                Files.getAttribute(link, "fileKey")
                );
        
        assertNotEquals(
                Files.getAttribute(file, "fileKey"),
                Files.getAttribute(link, "fileKey", LinkOption.NOFOLLOW_LINKS)
                );
        
        try {
            Files.setAttribute(file, "fileKey", "rejected");
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals("'basic:fileKey' not recognized", e.getMessage());
        }
    }
    
    @Test
    public void testNonExistentPath() throws Exception {
        try {
            assertSame(Boolean.TRUE, Files.getAttribute(root.resolve("notExists"), "isDirectory"));
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @Test
    public void testOwner() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        UserPrincipal owner = (UserPrincipal) Files.getAttribute(file, "owner:owner");
        assertEquals(owner, Files.getOwner(file));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testOwnerSymlink() throws Exception {
        Path file = Files.createSymbolicLink(root.resolve("symlink"), root.resolve("nonexistent"));
        UserPrincipal owner = (UserPrincipal) Files.getAttribute(file, "owner:owner", LinkOption.NOFOLLOW_LINKS);
        assertEquals(owner, Files.getOwner(file, LinkOption.NOFOLLOW_LINKS));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testGroup() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        GroupPrincipal group = (GroupPrincipal) Files.getAttribute(file, "posix:group");
        assertNotNull(group);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testPermissions() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        PosixFilePermission permission = 
                ((Set<PosixFilePermission>) Files.getAttribute(file, "posix:permissions")).iterator().next();
        assertNotNull(permission);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testIno() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Long ino = (Long) Files.getAttribute(file, "unix:ino");
        assertTrue(ino > 0);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testUid() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Integer uid = (Integer) Files.getAttribute(file, "unix:uid");
        assertTrue(uid > 0);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testGid() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Integer gid = (Integer) Files.getAttribute(file, "unix:gid");
        assertTrue(gid > 0);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testRdev() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Long rdev = (Long) Files.getAttribute(file, "unix:rdev");
        assertNotNull(rdev);
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testModeFile() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Integer mode = (Integer) Files.getAttribute(file, "unix:mode");
        assertMode(0100664, mode);
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testModeSetPermissions() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Files.setPosixFilePermissions(file, new HashSet<PosixFilePermission>(
                Arrays.asList(
                        PosixFilePermission.GROUP_EXECUTE, 
                        PosixFilePermission.OTHERS_READ, 
                        PosixFilePermission.OWNER_READ, 
                        PosixFilePermission.OWNER_WRITE)
                ));
        Integer mode = (Integer) Files.getAttribute(file, "unix:mode");
        assertMode(0100614, mode);
    }

    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testModeDir() throws Exception {
        Path dir = Files.createDirectory(root.resolve("dir"));
        Integer mode = (Integer) Files.getAttribute(dir, "unix:mode");
        assertMode(040775, mode);
    }

    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testModeSymlink() throws Exception {
        Path link = Files.createSymbolicLink(root.resolve("link"), root.resolve("file"));
        Integer mode = (Integer) Files.getAttribute(link, "unix:mode", LinkOption.NOFOLLOW_LINKS);
        assertMode(0120777, mode);
    }
    
    @Test
    public void testFileTypesNoSymlink() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path dir = Files.createDirectories(root.resolve("dir"));
        
        
        assertSame(Boolean.TRUE, Files.getAttribute(dir, "isDirectory"));
        assertSame(Boolean.FALSE, Files.getAttribute(file, "isDirectory"));
        
        assertSame(Boolean.FALSE, Files.getAttribute(dir, "isRegularFile"));
        assertSame(Boolean.TRUE, Files.getAttribute(file, "isRegularFile"));
        
        assertSame(Boolean.FALSE, Files.getAttribute(dir, "isOther"));
        assertSame(Boolean.FALSE, Files.getAttribute(file, "isOther"));
        
        assertSame(Boolean.FALSE, Files.getAttribute(dir, "isSymbolicLink"));
        assertSame(Boolean.FALSE, Files.getAttribute(file, "isSymbolicLink"));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testIsDirectory() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createSymbolicLink(root.resolve("link"), file);
        Path dir = Files.createDirectories(root.resolve("dir"));
        Path dirLink = Files.createSymbolicLink(root.resolve("dirLink"), dir);
        
        assertSame(Boolean.TRUE, Files.getAttribute(root, "isDirectory"));
        assertSame(Boolean.TRUE, Files.getAttribute(dirLink, "isDirectory"));
        assertSame(Boolean.FALSE, Files.getAttribute(dirLink, "isDirectory", LinkOption.NOFOLLOW_LINKS));
        assertSame(Boolean.FALSE, Files.getAttribute(file, "isDirectory"));
        assertSame(Boolean.FALSE, Files.getAttribute(link, "isDirectory"));
        assertSame(Boolean.FALSE, Files.getAttribute(link, "isDirectory", LinkOption.NOFOLLOW_LINKS));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testIsRegularFile() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createSymbolicLink(root.resolve("link"), file);
        Path dir = Files.createDirectories(root.resolve("dir"));
        Path dirLink = Files.createSymbolicLink(root.resolve("dirLink"), dir);
        
        assertSame(Boolean.FALSE, Files.getAttribute(root, "basic:isRegularFile"));
        assertSame(Boolean.FALSE, Files.getAttribute(dirLink, "posix:isRegularFile"));
        assertSame(Boolean.FALSE, Files.getAttribute(dirLink, "isRegularFile", LinkOption.NOFOLLOW_LINKS));
        assertSame(Boolean.TRUE, Files.getAttribute(file, "isRegularFile"));
        assertSame(Boolean.TRUE, Files.getAttribute(link, "isRegularFile"));
        assertSame(Boolean.FALSE, Files.getAttribute(link, "isRegularFile", LinkOption.NOFOLLOW_LINKS));

    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testIsOther() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createSymbolicLink(root.resolve("link"), file);
        Path dir = Files.createDirectories(root.resolve("dir"));
        Path dirLink = Files.createSymbolicLink(root.resolve("dirLink"), dir);
        
        assertSame(Boolean.FALSE, Files.getAttribute(root, "isOther"));
        assertSame(Boolean.FALSE, Files.getAttribute(dirLink, "isOther"));
        assertSame(Boolean.FALSE, Files.getAttribute(dirLink, "isOther", LinkOption.NOFOLLOW_LINKS));
        assertSame(Boolean.FALSE, Files.getAttribute(file, "isOther"));
        assertSame(Boolean.FALSE, Files.getAttribute(link, "isOther"));
        assertSame(Boolean.FALSE, Files.getAttribute(link, "isOther", LinkOption.NOFOLLOW_LINKS));
    }

    @IgnoreIfNoSymlink
    @Test
    public void testIsSymbolicLink() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createSymbolicLink(root.resolve("link"), file);
        Path dir = Files.createDirectories(root.resolve("dir"));
        Path dirLink = Files.createSymbolicLink(root.resolve("dirLink"), dir);
        
        assertSame(Boolean.FALSE, Files.getAttribute(root, "isSymbolicLink"));
        assertSame(Boolean.FALSE, Files.getAttribute(dirLink, "isSymbolicLink"));
        assertSame(Boolean.TRUE, Files.getAttribute(dirLink, "isSymbolicLink", LinkOption.NOFOLLOW_LINKS));
        assertSame(Boolean.FALSE, Files.getAttribute(file, "isSymbolicLink"));
        assertSame(Boolean.FALSE, Files.getAttribute(link, "isSymbolicLink"));
        assertSame(Boolean.TRUE, Files.getAttribute(link, "isSymbolicLink", LinkOption.NOFOLLOW_LINKS));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testNLinkDir() throws Exception {
        Path dir = Files.createDirectory(root.resolve("dir"));
        int nLink = (Integer) Files.getAttribute(dir, "unix:nlink");
        assertEquals(2, nLink);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testNLinkFile() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        int nLink = (Integer) Files.getAttribute(file, "unix:nlink");
        assertEquals(1, nLink);
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testNLinkWithSymlink() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path symlink = Files.createSymbolicLink(root.resolve("symlink"), root.resolve("file"));
        int nLink = (Integer) Files.getAttribute(file, "unix:nlink");
        assertEquals(1, nLink);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testNLinkSymlink() throws Exception {
        Path symlink = Files.createSymbolicLink(root.resolve("symlink"), root.resolve("nonExistent"));
        int nLink = (Integer) Files.getAttribute(symlink, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        assertEquals(1, nLink);
    }

    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testNLinkHardLink() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createLink(root.resolve("link"), root.resolve("file"));
        int nLink = (Integer) Files.getAttribute(link, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        assertEquals(2, nLink);
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testNLinkWithHardLink() throws Exception {
        Path file = Files.createFile(root.resolve("file"));
        Path link = Files.createLink(root.resolve("link"), root.resolve("file"));
        int nLink = (Integer) Files.getAttribute(file, "unix:nlink");
        assertEquals(2, nLink);
    }
    
    private void assertMode(int expected, int actual) {
        String expectedString = Integer.toOctalString(expected) + ":" + Integer.toBinaryString(expected);
        String actualStriong = Integer.toOctalString(actual) + ":" + Integer.toBinaryString(actual);
        assertEquals(expectedString, actualStriong);
    }
}
