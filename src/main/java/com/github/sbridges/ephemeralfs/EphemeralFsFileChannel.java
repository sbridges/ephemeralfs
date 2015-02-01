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
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

class EphemeralFsFileChannel extends FileChannel {

    private boolean closed;
    private int position;
    private final boolean canRead;
    private final boolean canWrite;
    private final FileContents fc;
    private final boolean deleteOnClose;
    private final EphemeralFsFileSystem fs;
    private final INode iNode;
    private final EphemeralFsPath path;
    private final CloseTracker closeTracker;
    private final boolean sync;
    
    public EphemeralFsFileChannel(
            FileContents fc, 
            boolean canRead, 
            boolean canWrite, 
            boolean deleteOnClose,
            boolean sync,
            ResolvedPath resolvedPath,
            EphemeralFsFileSystem fs
            ) {
        this.fc = fc;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.deleteOnClose = deleteOnClose;
        this.sync = sync;
        this.fs = fs;
        try {
            this.iNode = resolvedPath.getTarget();
        } catch (NoSuchFileException e) {
            throw new IllegalStateException(e);
        }
        this.path = resolvedPath.getPath();
        this.closeTracker = path.getFileSystem().trackClose(FileChannel.class, path);
        
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        synchronized(fc.lock) {
            assertReadable();
            assertNotClosed(); 
            if(position >= fc.getSize()) {
                return -1;
            }
            int toRead = Math.min(
                    dst.remaining(),
                    fc.getSize() - position
                    );
            fc.getContents().position(position);
            fc.getContents().limit(position + toRead);
            try {
                dst.put(fc.getContents());
                position += toRead;
            } finally {
                fc.getContents().limit(fc.getContents().capacity());
            }
            return toRead;
        }
    }


    @Override
    public long read(ByteBuffer[] dsts, int offset, int length)
            throws IOException {
        synchronized(fc.lock) {
            assertNotClosed(); 
            assertValidIndexes(dsts, offset, length);
    
            long total = 0;
            for(int i = offset; i < offset + length; i++) {
                total += read(dsts[i]);
            }
            return total;
        }
    }
        

    @Override
    public int write(ByteBuffer src) throws IOException {
        int answer;
        synchronized(fc.lock) {
            assertNotClosed();
            assertWritable();
            ensureCapacity(position + (long) src.remaining());
            
            int toWrite = src.remaining();
            int newPosition = position + toWrite;
            fc.getContents().position(position);
            try {
                fc.getContents().limit(fc.getContents().capacity());
                fc.setSize(Math.max(fc.getSize(), newPosition));
                //reset limit
            } finally {
                fc.getContents().limit(fc.getContents().capacity());
            }
            fc.getContents().put(src);
            position = newPosition;
            //don't just use positions, as
            //we may not have written at the end 
            //of the file...
            
            answer = toWrite;
            markDirty();
        }
        notifyModified();
        return answer;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException {
        synchronized(fc.lock) {
            assertNotClosed(); 
            assertValidIndexes(srcs, offset, length);
    
            long total = 0;
            for(int i = offset; i < offset + length; i++) {
                total += write(srcs[i]);
            }
            return total;
        }
    }

    @Override
    public long position() throws IOException {
        synchronized(fc.lock) { 
            assertNotClosed();
            return position;
        }
        
    }


    @Override
    public FileChannel position(long newPosition)
            throws IOException {
        synchronized(fc.lock) {
            assertNotClosed();
            if(position < 0) {
                throw new IllegalArgumentException("position must be > 0, not:" + newPosition);
            }
            if(newPosition > Integer.MAX_VALUE) {
                throw new IOException("position > max file length");
            }
            this.position = (int) newPosition;
            return this;
        }
    }

    @Override
    public long size() throws IOException {
        synchronized(fc.lock) {
            assertNotClosed();
            return fc.getSize();
        }
    }

    @Override
    public FileChannel truncate(long newSize) throws IOException {
        synchronized(fc.lock) {
            assertNotClosed();
            assertWritable();
            if(newSize < 0) {
                throw new IllegalArgumentException("size must be > 0, not:" + newSize);
            }
            if(newSize > Integer.MAX_VALUE) {
                throw new IOException("max file size exceeded");
            }
            if(newSize >= fc.getSize()) { 
                return this;
            }
            fc.setSize((int) newSize);
            if(fc.getContents().capacity() > 2 * FileContents.INITIAL_BUFFER_SIZE) {
                fc.createNewBuffer(FileContents.INITIAL_BUFFER_SIZE, true);
            }
            position = Math.min(position, fc.getSize());
            markDirty();
        }
        notifyModified();
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
        assertNotClosed(); 
        
        //only forcing contents, don't clear the dirty flag
        if(metaData) {
            fc.setDirty(false);
        }
    }

    @Override
    public long transferTo(long position, long count,
            WritableByteChannel target) throws IOException {
        
        if(!target.isOpen()) {
            throw new ClosedChannelException();
        }
        
        if(position < 0 || 
           count < 0
                ) {
            throw new IllegalArgumentException();
        }
        
        if(count > Integer.MAX_VALUE) {
            count = Integer.MAX_VALUE;
        }
        
        //we don't want a reference to our internal buffer escaping
        //so use a temporary buffer to do the actual read
        ByteBuffer tempBuf = ByteBuffer.allocate((int) count);
        synchronized(fc.lock) {
            
            int read = read(tempBuf, position);
            if(read <= 0) {
                return 0;
            }
        }
        
        tempBuf.flip();
        return target.write(tempBuf);
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position,
            long count) throws IOException {
        
        if(!src.isOpen()) {
            throw new ClosedChannelException();
        }

        
        if(position < 0 || 
           count < 0
                ) {
            throw new IllegalArgumentException();
        }
        
        if(count > Integer.MAX_VALUE) {
            count = Integer.MAX_VALUE;
        }

        //we don't want a reference to our internal buffer escaping
        //so use a temporary buffer to do the actual read
        ByteBuffer tempBuf = ByteBuffer.allocate((int) count);
        int read = src.read(tempBuf);
        if(read <= 0) {
            return 0;
        }
        
        tempBuf.flip();
        
        //we don't want to call an external method while holding the lock
        //so don't acquire it until here
        long answer;
        synchronized(fc.lock) {
            if(position > size()) {
                return 0;
            }
            answer = write(tempBuf, position);                
        }
        return answer;
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        synchronized(fc.lock) {
            assertNotClosed(); 
            long startPosition = position();
            try
            {
                position(position);
                return read(dst);
            } finally {
                position(startPosition);
            }
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        synchronized(fc.lock) {
            assertNotClosed(); 
            long startPosition = position();
            try
            {
                position(position);
                return write(src);
            } finally {
                position(startPosition);
            }
        }
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size)
            throws IOException {
        //this seems impossible to implement, for example
        //MappedByteBuffer#isLoaded method  is final, 
        //and internally calls private/native methods
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLock lock(long position, long size, boolean shared)
            throws IOException {
        return tryLock(position, size, shared);
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared)
            throws IOException {
        synchronized(fc.lock) {
            assertNotClosed(); 
            assertWritable();
            return fc.tryLock(this, position, size, shared);
        }
    }
    
    @Override
    protected void implCloseChannel() throws IOException {
        synchronized(fc.lock) {
            if(closed) {
                return;
            }
            closed = true;
        }
        fs.getLimits().releaseFileHandle();
        if(deleteOnClose) {
            Files.delete(path);
        }
        closeTracker.onClose();
        synchronized(fs.fsLock) {
            iNode.removeOpenFileHandle();
        }
    }
    
    private void assertValidIndexes(ByteBuffer[] array, int offset, int length) {
        if(offset < 0 || 
           length < 0 ||
           offset + length >= array.length) {
            throw new IllegalArgumentException("invalid offsets");
        }
    }
    
    private void assertNotClosed() throws ClosedChannelException {
        if(closed) {
            throw new ClosedChannelException();
        }
    }
    
    private void assertReadable() throws NonReadableChannelException, IOException {
        if(iNode.isDir()) {
            throw new IOException("Is a directory");
        }
        if(!canRead) {
            throw new NonReadableChannelException();
        }
    }
    
    private void assertWritable() throws NonWritableChannelException {
        if(!canWrite) {
            throw new NonWritableChannelException();
        }
    }
    
    private void ensureCapacity(long newSize) throws IOException { //NOPMD
        if(newSize > Integer.MAX_VALUE) {
            throw new IOException("max file size is" + Integer.MAX_VALUE);
        }
        if(fc.getContents().capacity() >= newSize) { 
            return;
        }
        long newCapacity = Math.max(fc.getContents().capacity() * 2, newSize); 
        if(newCapacity > Integer.MAX_VALUE) {
            newCapacity = Integer.MAX_VALUE;
        }
        fc.createNewBuffer(
                (int) newCapacity, 
                true);
    }

    
    private void notifyModified() throws NoSuchFileException {

        synchronized(fs.fsLock) {
            iNode.setLastModifiedTime(System.currentTimeMillis());
            
            //notify twice?  once for meta data once for contents?
            iNode.notifyChange(path);
            iNode.notifyChange(path);
            
        }
    }
    
    private void markDirty() {
        if(sync) {
            fc.setDirty(false);
        } else {
            fc.setDirty(true);
        }
    }
}