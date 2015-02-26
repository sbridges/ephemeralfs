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

import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.FsType;
import com.github.sbridges.ephemeralfs.junit.IgnoreIf;
import com.github.sbridges.ephemeralfs.junit.IgnoreIfNoSymlink;
import com.github.sbridges.ephemeralfs.junit.IgnoreUnless;
import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;



@RunWith(MultiFsRunner.class)
public class MoveTest {

    Path root;
    
    @Test
    public void testSimpleMove() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        
        Path target = root.resolve("target");
        Files.move(source, target);
        checkExistence(source, target);
    }
    
    @Test
    public void testMoveFileToSelf() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        Files.move(source, source);
        assertTrue(Files.exists(source));
    }
    
    @Test
    public void testMoveDirToSelf() throws Exception {
        Path source = Files.createDirectories(root.resolve("source"));
        Files.move(source, source);
        assertTrue(Files.exists(source));
    }
    
    @Test
    public void testMoveDoesntModifyFileTimes() throws Exception {
        
        
        Path source = Files.createFile(root.resolve("source"));
        
        long creation = TestUtil.getCreationTime(source);
        long modifiedTime = Files.getLastModifiedTime(source).toMillis();
        
        Path target = root.resolve("target");
        
        Thread.sleep(1000);
        
        Files.move(source, target);

        assertEquals(creation, TestUtil.getCreationTime(target));
        assertEquals(modifiedTime, Files.getLastModifiedTime(target).toMillis());
        
    }

    @Test
    public void testSimpleMoveDifferentDirs() throws Exception {
        Path sourceDir = Files.createDirectory(root.resolve("sourceDir"));
        Path source = Files.createFile(sourceDir.resolve("source"));
        
        Path target = root.resolve("target");
        Files.move(source, target);
        checkExistence(source, target);
    }
   
    @Test
    public void testDirMove() throws Exception {
        Path sourceDir = Files.createDirectory(root.resolve("sourceDir"));
        Files.createFile(sourceDir.resolve("aFile"));
        
        Path target = root.resolve("targetDir");
        Files.move(sourceDir, target);
        
        assertTrue(Files.exists(target.resolve("aFile")));
    }
    
    @Test
    public void testDirMoveFailsExistingFile() throws Exception {
        Path sourceDir = Files.createDirectory(root.resolve("sourceDir"));
        Files.createFile(sourceDir.resolve("aFile"));
        
        Path target = Files.createDirectory(root.resolve("targetDir"));
        
        try
        {
            Files.move(sourceDir, target);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @Test
    public void testDirMoveAlreadyExists() throws Exception {
        Path sourceDir = Files.createDirectory(root.resolve("sourceDir"));
        Files.createFile(sourceDir.resolve("aFile"));
        
        Path targetDir = Files.createDirectory(root.resolve("targetDir"));
        Files.move(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
        
        assertTrue(Files.exists(targetDir.resolve("aFile")));
    }
    
    @IgnoreIf(FsType.WINDOWS)
    @Test
    public void testDirMoveAlreadyExistsAtomicPosix() throws Exception {
        Path sourceDir = Files.createDirectory(root.resolve("sourceDir"));
        Files.createFile(sourceDir.resolve("aFile"));
        
        Path targetDir = Files.createDirectory(root.resolve("targetDir"));
        Files.move(sourceDir, targetDir, StandardCopyOption.ATOMIC_MOVE);
        
        assertTrue(Files.exists(targetDir.resolve("aFile")));
        assertFalse(Files.exists(sourceDir.resolve("aFile")));
    }

    @IgnoreUnless(FsType.WINDOWS)
    @Test
    public void testDirMoveAlreadyExistsAtomicWindows() throws Exception {
        Path sourceDir = Files.createDirectory(root.resolve("sourceDir"));
        Files.createFile(sourceDir.resolve("aFile"));
        
        Path targetDir = Files.createDirectory(root.resolve("targetDir"));
        try {
        	Files.move(sourceDir, targetDir, StandardCopyOption.ATOMIC_MOVE);
        	fail();
        } catch(AccessDeniedException e) {
        	//pass
        }
        assertTrue(Files.exists(sourceDir.resolve("aFile")));
        assertFalse(Files.exists(targetDir.resolve("aFile")));
    }
    
    
    @Test
    public void testDirMoveAlreadyExistsNotEmpty() throws Exception {
        Path sourceDir = Files.createDirectory(root.resolve("sourceDir"));
        Files.createFile(sourceDir.resolve("aFile"));
        
        Path targetDir = Files.createDirectory(root.resolve("targetDir"));
        Files.createFile(targetDir.resolve("aFile"));
        
        try
        {
            Files.move(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
            fail();
        } catch(DirectoryNotEmptyException e) {
            //pass
        }
        
    }
    
    @Test
    public void testMoveSourceDoesNotExist() throws Exception {
        
        Path source = root.resolve("source");
        Path target = root.resolve("target");
        try {
            Files.move(source, target);
            fail();
        } catch(NoSuchFileException e) {
            //pass
        }
    }
    
    @Test
    public void testAtomicSimpleMove() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        
        Path target = root.resolve("target");
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        checkExistence(source, target);
    }

    @Test
    public void testMoveTargetExistsFails() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        Path target = Files.createFile(root.resolve("target"));
        
        try
        {
            Files.move(source, target);
            fail();
        } catch(FileAlreadyExistsException e) {
            //pass
        }
    }
    
    @Test
    public void testMoveTargetExistsPassesIfIgnore() throws Exception {
        Path source = Files.createFile(root.resolve("source"));
        Path target = Files.createFile(root.resolve("target"));
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        checkExistence(source, target);
    }
   
    @IgnoreIfNoSymlink
    @Test
    public void testMoveSymlink() throws Exception {
        Path symlink = root.resolve("symlink");
        Path realFile1 = root.resolve("realFile1");
        Path realFile2 = root.resolve("realFile2");
        Path moveTo = root.resolve("moveTo");
        
        Files.write(realFile1, new byte[] {1});
        Files.write(realFile2, new byte[] {2});
        
        Files.createSymbolicLink(symlink, realFile1);
        Files.move(symlink, moveTo);
        
        assertTrue(Files.exists(realFile1));
        assertTrue(Files.exists(moveTo));
        assertFalse(Files.exists(symlink));
        assertTrue(Files.isSymbolicLink(moveTo));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(moveTo));
        
        //make sure the symlink is really a symlink
        Files.delete(realFile1);
        Files.move(realFile2, realFile1);
        assertArrayEquals(new byte[] {2}, Files.readAllBytes(moveTo));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testMoveRelativeSymlink() throws Exception {
        Path symlink = root.resolve("symlink");
        Path realFile1 = root.resolve("realFile1");
        Path realFile2 = root.resolve("realFile2");
        Path moveTo = root.resolve("moveTo");
        
        Files.write(realFile1, new byte[] {1});
        Files.write(realFile2, new byte[] {2});
        
        Files.createSymbolicLink(symlink, realFile1.getFileName());
        Files.move(symlink, moveTo);
        
        assertTrue(Files.exists(realFile1));
        assertTrue(Files.exists(moveTo));
        assertFalse(Files.exists(symlink));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(moveTo));
        
        //make sure the symlink is really a symlink
        Files.delete(realFile1);
        Files.move(realFile2, realFile1);
        assertArrayEquals(new byte[] {2}, Files.readAllBytes(moveTo));
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testMoveSymlinkMultiLevel() throws Exception {
        
        Path a = root.resolve("a");
        Files.createDirectories(a);
        Path b = root.resolve("b");
        Files.createDirectory(b);
        
        Path symlink = a.resolve("symlink");
        Path realFile1 = b.resolve("realFile1");
        Path realFile2 = b.resolve("realFile2");
        Path moveTo = b.resolve("moveTo");
        
        Files.write(realFile1, new byte[] {1});
        Files.write(realFile2, new byte[] {2});
        
        Files.createSymbolicLink(symlink, realFile1);
        Files.move(symlink, moveTo);
        
        assertTrue(Files.exists(realFile1));
        assertTrue(Files.exists(moveTo));
        assertFalse(Files.exists(symlink));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(moveTo));
        
        //make sure the symlink is really a symlink
        Files.delete(realFile1);
        Files.move(realFile2, realFile1);
        assertArrayEquals(new byte[] {2}, Files.readAllBytes(moveTo));
    }

    @IgnoreIfNoSymlink
    @Test
    public void testMoveRelativesSymlinkMultiLevel() throws Exception {
        
        Path child = root.resolve("child");
        Path grandChild = child.resolve("grandChild");
        Path sibling = root.resolve("sibling");
        
        Files.createDirectory(sibling);
        Files.createDirectories(grandChild);

        Path symlink = child.resolve("symlink");
        Path moveTo = child.resolve("moveTo");
        
        Path realFile = grandChild.resolve("realFile");
        Files.write(realFile, new byte[] {1});
        
        Files.createSymbolicLink(symlink, grandChild.getFileName().resolve(realFile.getFileName()));
        
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(symlink));
        
        Files.move(symlink, moveTo);
        
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(moveTo));
        
        Path moveToInSibling = sibling.resolve("moveTo");
        Files.move(moveTo, moveToInSibling);
        
        assertTrue(Files.exists(moveToInSibling, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(moveToInSibling));
        
    }
    
    @IgnoreIfNoSymlink
    @Test
    public void testMoveDanglingSymlink() throws Exception {
        Path symlink = root.resolve("symlink");
        Path nonExistent = root.resolve("nonExistent");
        Path moveTo = root.resolve("moveTo");
        
        Files.createSymbolicLink(symlink, nonExistent);
        Files.move(symlink, moveTo);
    }

    
    private void checkExistence(Path source, Path target) {
        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
    }
    
}
