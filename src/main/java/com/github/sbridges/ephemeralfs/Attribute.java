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
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;

enum Attribute {

    BASIC_CREATION_TIME("creationTime") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().creationTime();
        }
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(BasicFileAttributeView.class).setTimes(null, null, (FileTime) value);
        }
    },
    BASIC_FILE_KEY("fileKey") {

        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            if(view.fs.getSettings().isWindows()) {
                return null;
            }
            return view.build(BasicFileAttributeView.class).readAttributes().fileKey();
        }
    },
    BASIC_IS_DIRECTORY("isDirectory") {

        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().isDirectory();
        }
    },
    BASIC_IS_OTHER("isOther") {

        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().isOther();
        }
    },
    BASIC_IS_REGULAR_FILE("isRegularFile") {

        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().isRegularFile();
        }
    },
    BASIC_IS_SYMBOLIC_LINK("isSymbolicLink") {

        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().isSymbolicLink();
        }
    },
    BASIC_LAST_ACCESS_TIME("lastAccessTime") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().lastAccessTime();
        }
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(BasicFileAttributeView.class).setTimes(null, (FileTime) value, null);
        }
    },
    BASIC_LAST_MODIFIED_TIME("lastModifiedTime") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().lastModifiedTime();
        }
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(BasicFileAttributeView.class).setTimes((FileTime) value, null, null);
        }
    },
    BASIC_SIZE("size") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(BasicFileAttributeView.class).readAttributes().size();
        }
    },
    DOS_ARCHIVE("archive") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(DosFileAttributeView.class).readAttributes().isArchive();
        }
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(DosFileAttributeView.class).setArchive((Boolean) value);
        }
    },
    DOS_IS_HIDDEN("hidden") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(DosFileAttributeView.class).readAttributes().isHidden();
        }
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(DosFileAttributeView.class).setHidden((Boolean) value);
        }
    },
    DOS_IS_READ_ONLY("readonly") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(DosFileAttributeView.class).readAttributes().isReadOnly();
        }
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(DosFileAttributeView.class).setReadOnly((Boolean) value);
        }
    },
    DOS_SYSTEM("system") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.build(DosFileAttributeView.class).readAttributes().isSystem();
        }
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(DosFileAttributeView.class).setSystem((Boolean) value);
        }
    },
    OWNER_OWNER("owner") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.snapshotProperties().getOwner();
        }
        
        @Override
        public
        void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
            view.build(FileOwnerAttributeView.class).setOwner((UserPrincipal) value);
        }
    },
    POSIX_GROUP("group") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.snapshotProperties().getGroup();
        }
    },
    POSIX_PERMISSIONS("permissions") {
        
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.snapshotProperties().permissions();
        }
    },
    UNIX_CTIME("ctime") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            //TODO - this isn't quite right, we need to
            //track modifications to file meta data here
            //http://www.unix.com/tips-and-tutorials/20526-mtime-ctime-atime.html
            return view.snapshotProperties().lastModifiedTime();
        }
    },
    UNIX_GID("gid") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.snapshotProperties().getGroup().getGid();
        }
    },
    UNIX_INO("ino") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.snapshotProperties().getINodeNumber();
        }
    },
    UNIX_MODE("mode") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            FileAttributesSnapshot snapshotProperties = view.snapshotProperties();
            int mode = 0;
            if(snapshotProperties.isRegularFile()) {
                mode += 0b1_000_000_000_000_000;
            } else if(snapshotProperties.isDirectory()) {
                mode += 0b100_000_000_000_000;
            } else if(snapshotProperties.isSymbolicLink()) {
                mode += 0b1_010_000_000_000_000;
            } else {
                throw new IllegalStateException();
            }
            
            for(PosixFilePermission perm : snapshotProperties.permissions()) {
                switch(perm) {
                case OWNER_READ :     mode += 0b100_000_000; 
                break;
                case OWNER_WRITE :    mode += 0b010_000_000; 
                break;
                case OWNER_EXECUTE :  mode += 0b001_000_000; 
                break;
                case GROUP_READ :     mode += 0b000_100_000; 
                break;
                case GROUP_WRITE :    mode += 0b000_010_000; 
                break;
                case GROUP_EXECUTE :  mode += 0b000_001_000; 
                break;
                case OTHERS_READ :    mode += 0b000_000_100; 
                break;
                case OTHERS_WRITE :   mode += 0b000_000_010; 
                break;
                case OTHERS_EXECUTE : mode += 0b000_000_001; 
                break;
                //should never happen
                default : throw new IllegalStateException("unrecognized:" + perm);
                }
                
            }
            
            return mode;
        }
    },
    UNIX_NLINK("nlink") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.snapshotProperties().getNLink();
        }
    },
    UNIX_RDEV("rdev") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return 0L;
        }
    },
    UNIX_UID("uid") {
        @Override
        public
        Object read(FileAttributesViewBuilder view) throws IOException {
            return view.snapshotProperties().getOwner().getUid();
        }
    };
    
    private final String name;

    private Attribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public abstract Object read(FileAttributesViewBuilder view) throws IOException;
    
    public void write(FileAttributesViewBuilder view, String fullName, Object value) throws IOException {
        throw new IllegalArgumentException("'" + fullName + "' not recognized");        
    }

    
    
}
