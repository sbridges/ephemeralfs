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
import java.nio.file.FileSystemException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;


class FileAttributesViewBuilder {

    protected final EphemeralFsFileSystem fs;
    protected final EphemeralFsPathProvider pathProvider;
    protected final boolean noFollowLinks;
    protected final CloseChecker closeChecker;

    public FileAttributesViewBuilder(EphemeralFsFileSystem fs,
            EphemeralFsPathProvider path, CloseChecker closeChecker,
            LinkOption... linkOptions) {
        this.fs = fs;
        this.pathProvider = path;
        this.closeChecker = closeChecker;
        boolean hasNoFollow = false;
        for(LinkOption opt : linkOptions) {
            if(opt == LinkOption.NOFOLLOW_LINKS) {
                hasNoFollow = true;
            }
        }
        noFollowLinks = hasNoFollow;

    }
    
    public <V extends FileAttributeView> V build(Class<V> type) {
        if (type == BasicFileAttributeView.class) {
            return (V) new EphemeralFsBasicFileAttributesView();
        } else if (type == PosixFileAttributeView.class) {
            return (V) new EphemeralFsPosixFileAttributesView();
        } else if (type == DosFileAttributeView.class) {
            return (V) new EphemeralFsDosFileAttributesView();
        } else {
            throw new UnsupportedOperationException("type:" + type
                    + " is not supported");
        }
    }

    FileAttributesSnapshot snapshotProperties() throws IOException {
        synchronized (fs.fsLock) {
            closeChecker.assertNotClosed();
            EphemeralFsPath path = pathProvider.get();
            ResolvedPath resolved = resolve(path);
            if (resolved.resolvedToSymbolicLink()) {
                DirectoryEntry entry = resolved.getParent().resolve(
                        path.getFileName());
                return new FileAttributesSnapshot(fs, entry.getFileTimes(),
                        false, false, true, false, 1, entry.getId(),
                        FilePermissions.createDefaultFile()
                                .toPosixFilePermissions());
            }

            return resolved.getTarget().getAttributes();
        }
    }

    private void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime,
            FileTime createTime) throws IOException {
        closeChecker.assertNotClosed();
        synchronized (fs.fsLock) {
            ResolvedPath rs = resolve(pathProvider.get());
            
            //TODO - handle setting file times on symbolic links
            if(!rs.resolvedToSymbolicLink()) {
                if (lastModifiedTime != null) {
                    rs.getTarget().setLastModifiedTime(lastModifiedTime.toMillis());
                }
                if (createTime != null) {
                    rs.getTarget().setCreationTime(createTime.toMillis());
                }
                if (lastAccessTime != null) {
                    rs.getTarget().setLastModifiedTime(lastAccessTime.toMillis());
                }   
                rs.getTarget().notifyChange(rs.getPath());
            }
        }

    }

    private ResolvedPath resolve(EphemeralFsPath path) throws FileSystemException,
            NoSuchFileException {
        ResolvedPath resolved = ResolvedPath.resolve(path, noFollowLinks);
        if(!resolved.didResolve()) {
            throw new NoSuchFileException(pathProvider.toString());
        }
        return resolved;
    }

    class EphemeralFsBasicFileAttributesView implements BasicFileAttributeView {

        @Override
        public void setTimes(FileTime lastModifiedTime,
                FileTime lastAccessTime, FileTime createTime)
                throws IOException {
            FileAttributesViewBuilder.this.setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            return snapshotProperties().cast(BasicFileAttributes.class);
        }

        @Override
        public String name() {
            return "basic";
        }
    }

    class EphemeralFsPosixFileAttributesView implements PosixFileAttributeView {

        @Override
        public void setTimes(FileTime lastModifiedTime,
                FileTime lastAccessTime, FileTime createTime)
                throws IOException {
            FileAttributesViewBuilder.this.setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public PosixFileAttributes readAttributes() throws IOException {
            return snapshotProperties().cast(PosixFileAttributes.class);
        }

        @Override
        public String name() {
            return "posix";
        }

        @Override
        public UserPrincipal getOwner() throws IOException {
            return readAttributes().owner();
        }

        @Override
        public void setOwner(UserPrincipal owner) throws IOException {
            if (owner != getOwner()) {
                throw new UnsupportedOperationException();
            }

        }

        @Override
        public void setPermissions(Set<PosixFilePermission> perms)
                throws IOException {
            synchronized (fs.fsLock) {
                EphemeralFsPath path = pathProvider.get();
                ResolvedPath resolved = resolve(path);
                if(!resolved.hasTarget()) {
                    throw new UnsupportedOperationException();
                }
                resolved.getTarget().setPermissions(perms);
            }
        }

        @Override
        public void setGroup(GroupPrincipal group) throws IOException {
            if (group != readAttributes().group()) {
                throw new UnsupportedOperationException();
            }

        }
    }

    class EphemeralFsDosFileAttributesView implements DosFileAttributeView {

        @Override
        public void setTimes(FileTime lastModifiedTime,
                FileTime lastAccessTime, FileTime createTime)
                throws IOException {
            FileAttributesViewBuilder.this.setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public DosFileAttributes readAttributes() throws IOException {
            return snapshotProperties().cast(DosFileAttributes.class);
        }

        @Override
        public String name() {
            return "dos";
        }

        @Override
        public void setReadOnly(boolean value) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        public void setHidden(boolean value) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSystem(boolean value) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setArchive(boolean value) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

}
