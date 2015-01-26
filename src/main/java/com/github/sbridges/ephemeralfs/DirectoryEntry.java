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


/**
 * An entry in a directory 
 */
class DirectoryEntry {
    
    //an entry is either a pointer to another inode
    //or a symlink which points to another part of the file system

    private final INode destination;
    private final EphemeralFsPath symbolicLink;
    private final EphemeralFsFileTimes created = new EphemeralFsFileTimes();
    private final Long id = INode.iNodeCounter.incrementAndGet();
    
    public DirectoryEntry(EphemeralFsPath link) {
        this.destination = null;
        this.symbolicLink = link;
    }
    
    public EphemeralFsPath getSymbolicLink() {
        return symbolicLink;
    }

    public boolean isSymbolicLink() {
        return symbolicLink != null;
    }
    
    public INode getDestination() {
        return destination;
    }

    public DirectoryEntry(INode destination) {
        this.destination = destination;
        this.symbolicLink = null;
    }
    
    public EphemeralFsFileTimes getFileTimes() {
        return created;
    }
    
    public Object getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DirectoryEntry[destination=");
        builder.append(destination);
        builder.append(", symbolicLink=");
        builder.append(symbolicLink);
        builder.append(", created=");
        builder.append(created);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }
}