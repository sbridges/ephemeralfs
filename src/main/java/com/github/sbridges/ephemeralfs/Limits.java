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

import java.io.IOException;

class Limits {

    //don't use fs.lock
    //since we can add/remove disk space
    //while holding the lock for a file
    private final Object lock = new Object();
    private final Settings settings;
    private long diskRemaining;
    private long fileHandlesRemaining;
    
    public Limits(Settings settings) {
        this.settings = settings;
        diskRemaining = settings.getTotalSpace();
        fileHandlesRemaining = settings.getMaxOpenFileHandles();
    }
    
    public void tryAcquireFileHandle() throws IOException {
        synchronized(lock) {
            if(fileHandlesRemaining > 0) {
                fileHandlesRemaining--;
            } else {
                throw new IOException("file handles exhausted");
            }
        }
    }
    
    public void releaseFileHandle() {
        synchronized(lock) {
            fileHandlesRemaining++;
        }
    }

    public void tryAcquireDiskSpace(long space) throws IOException {
        synchronized(lock) {
            if(diskRemaining >= space) {
                diskRemaining -= space;
                if(diskRemaining < 0) {
                    throw new IllegalStateException();
                }
            } else {
                throw new IOException("Out of disk space");
            }
        }
    }
    
    public void releaseDiskSpace(long space) {
        synchronized(lock) {
            diskRemaining += space;
            if(diskRemaining < 0) {
                throw new IllegalStateException();
            }
        }
    }
    
    public long getDiskSpaceUsed() {
        synchronized(lock) {
            return settings.getTotalSpace() - diskRemaining;
        }
    }
    
    public long getFreeSpace() {
        synchronized(lock) {
            return diskRemaining;
        }
    }
}
