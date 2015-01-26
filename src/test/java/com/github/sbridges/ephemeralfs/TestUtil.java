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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemProvider;
import com.github.sbridges.ephemeralfs.EphemeralFsPath;

public class TestUtil {



    public static Path createEphemeralfsTestRoot() {
        EphemeralFsFileSystemProvider provider = new EphemeralFsFileSystemProvider();
        FileSystem fs;
        try {
            fs = provider.newFileSystem(new URI(EphemeralFsFileSystemProvider.SCHEME + "///?name=test"), new HashMap<String, Object>());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return fs.getRootDirectories().iterator().next();
    }
    
    public static void deleteTempDirRecursive(Path tempDir) {
        if(tempDir == null) {
            return;
        }
        
        Path tempRoot;
        try {
            tempRoot = Paths.get(System.getProperty("java.io.tmpdir")).toFile().getCanonicalFile().toPath();
        } catch (IOException e1) {
            throw new IllegalStateException(e1);
        }
        if(!(tempDir instanceof EphemeralFsPath) && ! tempDir.startsWith(tempRoot)) {
            throw new IllegalStateException();
        }
        try {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                        IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }    
    
    public static long getCreationTime(Path path) throws IOException {
        return Files.getFileAttributeView(path, BasicFileAttributeView.class)
                    .readAttributes()
                    .creationTime()
                    .toMillis();
    }
    
    /**
     * Assumes the string was written as ASCII 
     */
    public static String toString(ByteBuffer b) {
        StringBuilder builder = new StringBuilder();
        while(b.hasRemaining()) {
            builder.append((char) b.get());
        }
        return builder.toString();
    }
    
    public static void assertChildren(Path dir, Path...expected) throws IOException {
    	Set<Path> actual = new HashSet<>();
    	try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
    		for(Path p : stream) {
    			actual.add(p);
    		}	
    	}
    	
    	assertEquals(new HashSet<Path>(Arrays.asList(expected)), actual);
    	
    }
    
    public static void assertFound(DirectoryStream<Path> stream, Path...paths) throws IOException {
        try {
         
            Set<Path> actual = new HashSet<>();
            Set<Path> expected = new HashSet<>();
            for(Path p : stream) {
                if(!actual.add(p)) {
                    throw new AssertionError("dupe");
                }
            }
            for(Path p : paths) {
                if(!expected.add(p)) {
                    throw new AssertionError("dupe");
                }
            }
            
            assertEquals(expected, actual);
        } finally {
            if(stream != null) {
                stream.close();
            }
        }
    }
}
