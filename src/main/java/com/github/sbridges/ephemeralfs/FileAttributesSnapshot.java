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
    private final Long iNodeNumber;
    private final Set<PosixFilePermission> permissions;
    
    private final boolean dosIsArchive;
    private final boolean dosIsHidden;
    private final boolean dosIsReadOnly;
    private final boolean dosIsSystem;
    
    private final EphemeralFsUserPrincipal owner;
    private final GroupPrincipal group;
    private final int nLink;
    
    public FileAttributesSnapshot(
            boolean regularFile, 
            boolean directory, 
            boolean symbolicLink,
            boolean other, 
            long size, 
            int nLink,
            FileProperties fileProperties) {
        
        this.fs =  fileProperties.getFs();
        this.creationTime = fileProperties.getFileTimes().getCreationTime();
        this.lastModifiedTime = fileProperties.getFileTimes().getLastModifiedTime();
        this.lastAccessTime = fileProperties.getFileTimes().getLastAccessTime();
        
        this.regularFile = regularFile;
        this.directory = directory;
        this.symbolicLink = symbolicLink;
        this.other = other;
        this.size = size;
        this.nLink = nLink;
        this.key = fileProperties.getiNodeNumber();
        this.permissions = Collections.unmodifiableSet(
                EnumSet.copyOf(fileProperties.getFilePermissions().toPosixFilePermissions())
                );
        
        this.dosIsArchive = fileProperties.getDosIsArchive();
        this.dosIsHidden = fileProperties.getDosIsHidden();
        this.dosIsReadOnly = fileProperties.getDosIsReadOnly();
        this.dosIsSystem = fileProperties.getDosIsSystem();
        this.owner = fileProperties.getOwner();
        this.group = fileProperties.getGroup();
        this.iNodeNumber = fileProperties.getiNodeNumber();
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
    
    public EphemeralFsUserPrincipal getOwner() {
        return owner;
    }
    
    public EphemeralFsGroupPrincipal getGroup() {
        return (EphemeralFsGroupPrincipal) group;
    }
    
    public <V extends BasicFileAttributes> V cast(Class<V> type) {
        if(type == BasicFileAttributes.class) {
            return (V) new EphemeralFsBasicFileAttributes();
       } else if(type == PosixFileAttributes.class) {
            return (V) new EphemeralFsPosixFileAttributes();
       } else if(type == DosFileAttributes.class) { 
           return (V) new EphemeralFsDosFileAttributes();
       }
       //there is no FileOwnerAttributes ?
       else {
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
            return FileAttributesSnapshot.this.owner;
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
            return FileAttributesSnapshot.this.dosIsReadOnly;
        }

        @Override
        public boolean isHidden() {
            return FileAttributesSnapshot.this.dosIsHidden;
        }

        @Override
        public boolean isArchive() {
            return FileAttributesSnapshot.this.dosIsArchive;
        }

        @Override
        public boolean isSystem() {
            return FileAttributesSnapshot.this.dosIsSystem;
        }
    }

    public Long getINodeNumber() {
        return iNodeNumber;
    }

    public int getNLink() {
        return nLink;
    }
}
