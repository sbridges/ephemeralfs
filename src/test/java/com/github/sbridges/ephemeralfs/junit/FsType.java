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

package com.github.sbridges.ephemeralfs.junit;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder;
import com.github.sbridges.ephemeralfs.OSTest;
import com.github.sbridges.ephemeralfs.TestUtil;

public enum FsType {
    
    UNIX {
        @Override
        public Path createTestRoot(Class testKlass) {
        	FileSystem fs = EphemeralFsFileSystemBuilder.unixFs().build();
            return fs.getRootDirectories().iterator().next();
        }

        @Override
        public void cleanUpRoot(Path root) throws IOException {
            root.getFileSystem().close();
        }
    },
    WINDOWS {
        @Override
        public Path createTestRoot(Class testKlass) {
        	FileSystem fs = EphemeralFsFileSystemBuilder.windowsFs().build();
            return fs.getRootDirectories().iterator().next();
        }

        @Override
        public void cleanUpRoot(Path root) throws IOException {
            root.getFileSystem().close();
        }
    },
    MAC {
        @Override
        public Path createTestRoot(Class testKlass) {
            FileSystem fs = EphemeralFsFileSystemBuilder.macFs().build();
            return fs.getRootDirectories().iterator().next();
        }
        @Override
        public void cleanUpRoot(Path root) throws IOException {
            root.getFileSystem().close();
        }
    },
    SYSTEM {
        @Override
        public Path createTestRoot(Class testKlass) {
            try {
                File tempDir = Files.createTempDirectory(testKlass.getSimpleName()).toFile();
                tempDir = tempDir.getCanonicalFile();
                return tempDir.toPath();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void cleanUpRoot(Path root) {
            TestUtil.deleteTempDirRecursive(root);
            
        }
        
        @Override
        public String toString() {
            if(OSTest.isMac()) {
                return "SYSTEM-MAC";
            }
            if(OSTest.isWindows()) {
                return "SYSTEM-WINDOWS";
            }
            if(OSTest.isUnix()) {
                return "SYSTEM-UNIX";
            }
            throw new IllegalStateException();
        }
    };
    
    public abstract void cleanUpRoot(Path root) throws IOException;
    public abstract Path createTestRoot(Class testKlass);
    
    
}
