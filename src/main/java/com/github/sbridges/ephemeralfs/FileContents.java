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
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The contents of a file
 */
class FileContents {

    static final int INITIAL_BUFFER_SIZE = 1024;

    //all modifications done on this file are done while holding this lock
    //you should not acquire the file system lock
    //while holding this lock
    final Object lock = new Object();
    private ByteBuffer contents;
    private final EphemeralFsFileSystem fs;

    private int size = 0;
    private final List<EphemeralFsFileLock> locks = new ArrayList<>();

    private final INode iNode;
    //do we have contents that have not been
    //fsynced
    private boolean isDirty = true;

    public FileContents(EphemeralFsFileSystem fs, INode iNode) {
        this.fs = fs;
        this.iNode = iNode;
        createNewBuffer(INITIAL_BUFFER_SIZE, false);
    }
    
    public EphemeralFsFileChannel newChannel(
            boolean canRead, 
            boolean canWrite, 
            boolean append, 
            boolean deleteOnClose, 
            boolean truncate,
            boolean sync,
            ResolvedPath resolvedPath, 
            EphemeralFsFileSystem fs) {
        if(!canRead && !canWrite) {
            throw new IllegalArgumentException("can't read or write?");
        }
        synchronized(fs.fsLock) {
             iNode.addOpenFileHandle();
        }
        synchronized(lock) {
            EphemeralFsFileChannel answer = 
                    new EphemeralFsFileChannel(
                            this, 
                            canRead, 
                            canWrite, 
                            deleteOnClose,
                            sync,
                            resolvedPath,
                            fs);
            if(append) {
                try {
                    answer.position(size);
                } catch (IOException e) {
                    //in memory, shouldn't happen
                    throw new IllegalStateException(e);
                }
            }
            if(truncate) {
                try {
                    //we are always freeing space, so we shouldn't throw
                    setSize(0);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                createNewBuffer(INITIAL_BUFFER_SIZE, false);
            }
            return answer;
        }
    }

    
    public void setSize(int newSize) throws IOException {
        if(newSize == size) {
            return;
        }
        if(newSize > size) {
            fs.getLimits().tryAcquireDiskSpace(newSize - size);
        } else {
            fs.getLimits().releaseDiskSpace(size - newSize);
        }
        this.size = newSize;
    }

    public void createNewBuffer(int newSize, boolean copyOld) {
        ByteBuffer newContents = ByteBuffer.allocate(newSize);
        if(copyOld && size > 0) {
            contents.position(0);
            contents.limit(size);
            newContents.put(contents);
        }
        contents = newContents;
    }
    
    public ByteBuffer getContents() {
        return contents;
    }

    public FileLock tryLock(Channel channel, long start, long size, boolean shared) throws IOException{
        
        if(!channel.isOpen()) {
            throw new ClosedChannelException();
        }
        
        Iterator<EphemeralFsFileLock> iter = locks.iterator();
        while(iter.hasNext()) {
            EphemeralFsFileLock oldLock = iter.next();
            if(!oldLock.isValid()) {
                iter.remove();
            }
            else if(oldLock.overlaps(start, size)) {
                throw new OverlappingFileLockException();
            }
        }
        EphemeralFsFileLock newLock = 
                channel instanceof FileChannel ? 
                new EphemeralFsFileLock((FileChannel) channel, start, size, shared) :
                new EphemeralFsFileLock((AsynchronousFileChannel) channel, start, size, shared);
       locks.add(newLock);
       return newLock;
    
    }

    public int getSize() {
        synchronized(lock) {
            return size;
        }
    }

    public void setDirty(boolean dirty) {
        synchronized(lock) {
            this.isDirty = dirty;
        }
    }

    public boolean isDirty() {
        synchronized(lock) {
            return isDirty;
        }
    }
}