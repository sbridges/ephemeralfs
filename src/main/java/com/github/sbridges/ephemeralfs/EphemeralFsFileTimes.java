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

class EphemeralFsFileTimes {

    private long lastModifiedTime;
    private long creationTime;
    private long lastAccessTime;
    
    public EphemeralFsFileTimes() {
        setDefaultTimes();
    }
    
    private void setDefaultTimes() {
        this.creationTime = roundTime(System.currentTimeMillis());
        this.lastAccessTime = creationTime;
        this.lastModifiedTime = creationTime;
    }
    
    
    private long roundTime(long timeMs) {
        //TODO - timestamps in most unix systems are stored with a granularity of 1 second
        //except for ext4, which is apparently a granularity of 1 ns, however
        //the stat system call looks to only return granularity of 1 second in either case
        //ntfs stores granularity of 100 ns, http://msdn.microsoft.com/en-us/library/windows/desktop/ms724290(v=vs.85).aspx
        //but may delay updates to last access time
        return timeMs / 1000 * 1000;
    }
    
    public void setLastModifiedTime(long timeMs) {
        this.lastModifiedTime = roundTime(timeMs);
    }
    
    public void setLastAccessTime(long timeMs) {
        this.lastAccessTime = roundTime(timeMs);
    }
    
    public void setCreationTime(long timeMs) {
        this.creationTime = roundTime(timeMs);
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EphemeralFsFileTimes[lastModifiedTime=");
        builder.append(lastModifiedTime);
        builder.append(", creationTime=");
        builder.append(creationTime);
        builder.append(", lastAccessTime=");
        builder.append(lastAccessTime);
        builder.append("]");
        return builder.toString();
    }
    
    
    
}
