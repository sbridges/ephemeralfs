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

import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class EphemeralFsWatchKey implements WatchKey {

    private final EphemeralFsWatchService watchService;
    private final INode iNode;
    private final Watchable watchable;
    private final Set<Kind<?>> interestOps;
    
    private final List<EphemeralFsWatchEvent> events = new ArrayList<>();
    
    private boolean cancelled = false;
    private boolean triggered = false;
    
    private final EphemeralFsFileSystem fs;
    
    EphemeralFsWatchKey(
            EphemeralFsWatchService watchService, 
            EphemeralFsPath watchable,
            INode iNode,
            EphemeralFsFileSystem fs,
            Kind<?>... events) {
        this.watchService = watchService;
        if(!watchable.isAbsolute()) {
            throw new IllegalArgumentException("path must be absolute");
        }
        this.watchable = watchable;
        this.iNode = iNode;
        this.fs = fs;
        interestOps = new HashSet<>();
        for(Kind<?> event : events) {
            interestOps.add(event);
        }
                
    }

    @Override
    public boolean isValid() {
        synchronized(fs.fsLock) {
            if(cancelled) {
                return false;
            }
            return !watchService.isClosed();
        }
    }

    @Override
    public List<WatchEvent<?>> pollEvents() {
        synchronized(fs.fsLock) {
            List<WatchEvent<?>> answer = new ArrayList<>();
            answer.addAll(events);
            events.clear();
            return answer;
        }
    }

    @Override
    public boolean reset() {
        synchronized(fs.fsLock) {
            if(!iNode.exists()) {
                return false;
            }
            if(!isValid()) {
                return false;
            }
            
            //if already queued, then do nothing
            //if already 
            if(!events.isEmpty()) {
                triggered = true;
                watchService.queue(this);
            } else {
                triggered = false;
            }
            return true;
        }
    }

    @Override
    public void cancel() {
        synchronized(fs.fsLock) {
            cancelled = true;
        }
    }

    public void hear(EphemeralFsWatchEvent e) {
        synchronized (fs.fsLock) {
            if(!isValid()) {
                return;
            }
            
            boolean consumed = false;
            for(EphemeralFsWatchEvent existing : events) {
                if(existing.isSame(e)) {
                    existing.addCount(e);
                    consumed = true;
                    break;
                }
            }
            
            if(!consumed) {
                events.add(e);
            }
            if(!triggered) {
                triggered = true;
                watchService.queue(this);
            }
        }
    }
    
    @Override
    public Watchable watchable() {
        return watchable;
    }

    public void setTriggered() {
       synchronized(fs.fsLock) {
           triggered = true;
       }
        
    }
}
