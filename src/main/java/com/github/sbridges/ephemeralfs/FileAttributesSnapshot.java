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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An immutable snapshot of the attributes of a file.
 */
class FileAttributesSnapshot {
    
    private final EphemeralFsFileSystem fs;
    private final long lastModifiedTime;
    private final long lastAccessTime;
    private final long creationTime;
    private final boolean regularFile;
    private final boolean directory;
    private final boolean symbolicLink;
    private final boolean other;
    private final long size;
    private final Object key;
    private final Set<PosixFilePermission> permissions;
    
    public FileAttributesSnapshot(
            EphemeralFsFileSystem fs,
            EphemeralFsFileTimes fileTimes,
            boolean regularFile, 
            boolean directory, 
            boolean symbolicLink,
            boolean other, 
            long size, 
            Object key,
            Set<PosixFilePermission> permissions) {
        
        this.fs =  fs;
        this.creationTime = fileTimes.getCreationTime();
        this.lastModifiedTime = fileTimes.getLastModifiedTime();
        this.lastAccessTime = fileTimes.getLastAccessTime();
        
        this.regularFile = regularFile;
        this.directory = directory;
        this.symbolicLink = symbolicLink;
        this.other = other;
        this.size = size;
        this.key = key;
        this.permissions = Collections.unmodifiableSet(
                EnumSet.copyOf(permissions)
                );
    }
    
    public boolean isRegularFile() {
        return regularFile;
    }
    
    public boolean isDirectory() {
        return directory;
    }
    
    public boolean isSymbolicLink() {
        return symbolicLink;
    }
    
    public boolean isOther() {
        return other;
    }
    
    public FileTime lastModifiedTime() {
        return FileTime.from(lastModifiedTime, TimeUnit.MILLISECONDS);
    }
    
    public FileTime lastAccessTime() {
        return  FileTime.from(lastAccessTime, TimeUnit.MILLISECONDS);
    }
    
    public FileTime creationTime() {
        return  FileTime.from(creationTime, TimeUnit.MILLISECONDS);
    }
    
    public long size() {
        return size;
    }
    
    public Object fileKey() {
        return key;
    }
    
    public Set<PosixFilePermission> permissions() {
        return permissions;
    }
    
    public <V extends BasicFileAttributes> V cast(Class<V> type) {
        if(type == BasicFileAttributes.class) {
            return (V) new EphemeralFsBasicFileAttributes();
       } else if(type == PosixFileAttributes.class) {
            return (V) new EphemeralFsPosixFileAttributes();
       } else if(type == DosFileAttributes.class) { 
           return (V) new EphemeralFsDosFileAttributes();
       } else {
           throw new UnsupportedOperationException("type:" + type + " is not supported");
       }
   }
    
   class EphemeralFsBasicFileAttributes implements BasicFileAttributes {

        @Override
        public FileTime lastModifiedTime() {
             return FileAttributesSnapshot.this.lastModifiedTime();
        }

        @Override
        public FileTime lastAccessTime() {
            return FileAttributesSnapshot.this.lastAccessTime();
        }

        @Override
        public FileTime creationTime() {
            return FileAttributesSnapshot.this.creationTime();
                    
        }

        @Override
        public boolean isRegularFile() {
            return FileAttributesSnapshot.this.isRegularFile();
        }

        @Override
        public boolean isDirectory() {
            return FileAttributesSnapshot.this.isDirectory();
        }

        @Override
        public boolean isSymbolicLink() {
            return FileAttributesSnapshot.this.isSymbolicLink();
        }

        @Override
        public boolean isOther() {
            return FileAttributesSnapshot.this.isOther();
        }

        @Override
        public long size() {
            return FileAttributesSnapshot.this.size();
        }

        @Override
        public Object fileKey() {
            return FileAttributesSnapshot.this.fileKey();
        }
        
    }
    
    class EphemeralFsPosixFileAttributes extends EphemeralFsBasicFileAttributes implements PosixFileAttributes {

        @Override
        public UserPrincipal owner() {
            return ((EphemeralFsUserPrincipalLookupService) fs.getUserPrincipalLookupService()).userPrincipal;
        }

        @Override
        public GroupPrincipal group() {
            return ((EphemeralFsUserPrincipalLookupService) fs.getUserPrincipalLookupService()).groupPrincipal;
        }

        @Override
        public Set<PosixFilePermission> permissions() {
            return permissions;
        }
        
    }
 
    class EphemeralFsDosFileAttributes extends EphemeralFsBasicFileAttributes implements DosFileAttributes {

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public boolean isArchive() {
            return false;
        }

        @Override
        public boolean isSystem() {
            return false;
        }
    }
}
