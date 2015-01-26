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

import java.nio.file.FileSystem;
import java.nio.file.Path;


/**
 * Utility methods for verifying the state of a file system. 
 */
public final class EphemeralFsFileSystemChecker {

    
    /**
     * Throw if there are any open resources for this file system.<P>
     * 
     * @param fs - A FileSystem created with {@link EphemeralFsFileSystemBuilder#build()}
     * 
     * @throws AssertionError if a resource is open
     * @throws ClassCastException if fs is of the wrong type
     * 
     * @see EphemeralFsFileSystemBuilder#setRecordStackTracesOnOpen(boolean)
     */
    public static void assertNoOpenResources(FileSystem fs) throws AssertionError, ClassCastException {
        ((EphemeralFsFileSystem) fs).assertNoOpenResources();
    }
    
    /**
     * If path is a file, asserts that all changes to the file have been fsynced, if path is a directory,
     * recursively scans the directory and asserts that all changes to files found have been fsynced.<P>
     * 
     * @param path - A {@link Path} whose {@link java.nio.file.spi.FileSystemProvider} is {@link EphemeralFsFileSystemProvider}
     * 
     * @throws AssertionError if a file has not been fsynced
     * @throws IllegalArgumentException if path does not exist
     * @throws ClassCastException if path is of the wrong type
     * 
     */
    public static void assertAllFilesFsynced(Path path) throws AssertionError, ClassCastException, IllegalArgumentException {
        EphemeralFsPath efsPath = (EphemeralFsPath) path; 
        efsPath.getFileSystem().assertAllFilesFsynced(efsPath);
    }
    
    
    /**
     * Scans directories to make sure the directories have been fsycned.<P>
     * 
     * This does not make sure that files have been fsynced, just the directories. In unix 
     * when adding/removing a file from a directory, the directory must be fsynced.  Java inadvertently
     * provides a way to do this on unix file systems by opening a FileChannel on a directory for reading
     * and fsyncing that directory.<P>
     * 
     * Fsyncing directories may not be supported in java 9, 
     * see <a href="https://issues.apache.org/jira/browse/LUCENE-6169">LUCENE-6169</a> <p>
     * 
     * @param dir - A directory Path whose {@link java.nio.file.spi.FileSystemProvider} is {@link EphemeralFsFileSystemProvider}
     * @param recursive - if true recursively scans sub directories to find non fsycned directories
     * 
     * @throws AssertionError if a directory has not been closed
     * @throws IllegalArgumentException if path does not exist, or is not a directory
     * @throws ClassCastException if path is of the wrong type
     * 
     */
    public static void assertAllDirectoriesFsynced(Path dir, boolean recursive) 
            throws AssertionError, ClassCastException, IllegalArgumentException {
        EphemeralFsPath efsDir = (EphemeralFsPath) dir; 
        efsDir.getFileSystem().assertAllDirectoriesFsynced(efsDir, recursive);
    }    
}
