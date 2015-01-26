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
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Implementation of AsynchronousFileChannel, simply wraps a synchronous file channel
 */
class EphemeralFsAsynchronousFileChannel extends AsynchronousFileChannel {

    private final EphemeralFsFileChannel channel;
    private final Executor executor;
    
    EphemeralFsAsynchronousFileChannel(EphemeralFsFileChannel channel,
            Executor executor) {
        
        if(channel == null) {
            throw new NullPointerException("channel is null");
        }
        if(executor == null) {
            throw new NullPointerException("executor is null");
        }    
        
        this.executor = executor;
        this.channel = channel;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }

    @Override
    public AsynchronousFileChannel truncate(long size) throws IOException {
        channel.truncate(size);
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
        channel.force(metaData);
    }

    @Override
    public <A> void lock(final long position, final long size, final boolean shared,
            final A attachment, final CompletionHandler<FileLock, ? super A> handler) {
        
        if(handler == null) {
            throw new NullPointerException("handler is null");
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    handler.completed(channel.lock(position, size, shared), attachment);
                } catch(Exception e) {
                    handler.failed(e, attachment);
                }
            }
        });
        
    }

    @Override
    public Future<FileLock> lock(final long position, final long size, final boolean shared) {
        return submit(new Callable<FileLock>() {
            @Override
            public FileLock call() throws Exception {
                return channel.lock(position, size, shared);
            }
        });
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared)
            throws IOException {
        return channel.tryLock(position, size, shared);
    }

    @Override
    public <A> void read(final ByteBuffer dst, final long position, final A attachment,
            final CompletionHandler<Integer, ? super A> handler) {
        
        if(handler == null) {
            throw new NullPointerException("handler is null");
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    handler.completed(channel.read(dst, position), attachment);
                } catch(Exception e) {
                    handler.failed(e, attachment);
                }
            }
        });
        
    }

    @Override
    public Future<Integer> read(final ByteBuffer dst, final long position) {
        return submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return channel.read(dst, position);
            }
        });
    }

    @Override
    public <A> void write(final ByteBuffer src, final long position, final A attachment,
            final CompletionHandler<Integer, ? super A> handler) {
        
        if(handler == null) {
            throw new NullPointerException("handler is null");
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    handler.completed(channel.write(src, position), attachment);
                } catch(Exception e) {
                    handler.failed(e, attachment);
                }
            }
        });
        
    }

    @Override
    public Future<Integer> write(final ByteBuffer src, final long position) {
        return submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return channel.write(src, position);
            }
        });
    }
    
    private <T> Future<T> submit(Callable<T> callable) {
        if(executor instanceof ExecutorService) {
            return ((ExecutorService) executor).submit(callable);
        }
        
        FutureTask<T> answer = new FutureTask<>(callable);
        executor.execute(answer);
        return answer;
    }
}
