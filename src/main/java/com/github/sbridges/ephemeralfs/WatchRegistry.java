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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class WatchRegistry {
    
    private final Object lock = new Object();
    private final Map<INode, CopyOnWriteArrayList<EphemeralFsWatchKey>> watches = new ConcurrentHashMap<>();
    
    
    public void hearChange(INode directory, EphemeralFsWatchEvent event) {
        List<EphemeralFsWatchKey> watchKeys = watches.get(directory);
        if(watchKeys != null) {
            for(EphemeralFsWatchKey watchKey : watchKeys) {
                watchKey.hear(event);
            }
        }
    }
    
    public void register(INode directory, EphemeralFsWatchKey watchKey) {
        synchronized(lock) {
            CopyOnWriteArrayList<EphemeralFsWatchKey> existing = watches.get(directory);
            if(existing != null) {
                existing.add(watchKey);
            } else {
                watches.put(directory, new CopyOnWriteArrayList<EphemeralFsWatchKey>());
                watches.get(directory).add(watchKey);
            }
        }
    }
    
    public void deregister(INode directory, EphemeralFsWatchKey watchKey) {
        synchronized(lock) {
            CopyOnWriteArrayList<EphemeralFsWatchKey> existing = watches.get(directory);
            existing.remove(watchKey);
            if(existing.isEmpty()) {
                watches.remove(directory);
            }
        }
    }
}
