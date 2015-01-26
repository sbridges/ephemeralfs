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
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class EphemeralFsWatchService implements WatchService {

    private final EphemeralFsFileSystem fs;
    
    public EphemeralFsFileSystem getFs() {
        return fs;
    }

    private boolean closed = false;
    private final LinkedBlockingQueue<EphemeralFsWatchKey> queue = new LinkedBlockingQueue<>();
    private final CloseTracker closeTracker;
    
    
    public EphemeralFsWatchService(EphemeralFsFileSystem fs) {
        this.fs = fs;
        this.closeTracker = fs.trackClose(EphemeralFsWatchService.class, null);
    }

    @Override
    public void close() throws IOException {
        synchronized(fs.fsLock) {
            closed = true;
        }
        closeTracker.onClose();
        
    }

    @Override
    public WatchKey poll() {
        EphemeralFsWatchKey answer = queue.poll();
        if(answer != null) {
            answer.setTriggered();
        }
        return answer;
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        EphemeralFsWatchKey answer = queue.poll(timeout, unit);
        if(answer != null) {
            answer.setTriggered();
        }
        return answer;
    }

    @Override
    public WatchKey take() throws InterruptedException {
        EphemeralFsWatchKey answer = queue.take();
        if(answer != null) {
            answer.setTriggered();
        }
        return answer;

    }

    
    boolean isClosed() {
        synchronized(fs.fsLock) {
            return closed;
        }
    }

    public void queue(EphemeralFsWatchKey efsWatchKey) {
        queue.add(efsWatchKey);
        
    }
}
