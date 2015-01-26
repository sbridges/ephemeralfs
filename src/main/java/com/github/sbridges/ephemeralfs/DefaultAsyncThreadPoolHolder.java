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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Creates a default thread pool for asynchronous file operations.<p>
 * 
 * From AsynchronousFileChannel,<p>
 * 
 * <pre>
 * When an code AsynchronousFileChannel is
 * created without specifying a thread pool then the channel is associated with
 * a system-dependent default thread pool that may be shared with other
 * channels. The default thread pool is configured by the system properties
 * defined by the AsynchronousChannelGroup class
 * </pre>
 */
class DefaultAsyncThreadPoolHolder {
    
    private static final String THREAD_FACTORY_PROP = System.getProperty("java.nio.channels.DefaultThreadPool.threadFactory");
    private static final String INITIAL_SIZE_PROP =   System.getProperty("java.nio.channels.DefaultThreadPool.initialSize");

    private final Object lock = new Object();
    private boolean closed = false;
    
    private ExecutorService executor;
    
    public ExecutorService getThreadPool() {
        synchronized(lock) {
            if(closed) {
                throw new IllegalStateException("already closed!");
            }
            if(executor != null) {
                return executor;
            }
            try
            {
                //I don't see this being used in the jvm impl,
                //or useful. parse it for completeness, but
                //otherwise ignore
                if(INITIAL_SIZE_PROP != null) {
                    Integer.parseInt(INITIAL_SIZE_PROP);
                }
            } catch(Exception e) {
                throw new Error(
                        "Invalid prop for system property:java.nio.channels.DefaultThreadPool.initialSize, "
                        + "value:" + INITIAL_SIZE_PROP, e);
            }

            ThreadFactory factory = daemonThreadFactory();
            if(THREAD_FACTORY_PROP != null) {
                try {
                    factory = (ThreadFactory) Class.forName(THREAD_FACTORY_PROP).newInstance();
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
            
            executor = Executors.newCachedThreadPool(factory);
            return executor;
        }
    }

    private static ThreadFactory daemonThreadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread answer = new Thread(r);
                answer.setDaemon(true);
                return answer;
            }
        };
    }

    public void close() {
        synchronized(lock) {
            if(executor != null) {
                executor.shutdown();
            }
            closed = true;
        }       
    }
    
}
