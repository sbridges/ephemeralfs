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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class EphemeralFsSecureDirectoryStream implements SecureDirectoryStream<Path> {

    private boolean used = false;
    private volatile boolean closed = false;
    private final List<Path> paths;
    
    private final EphemeralFsPath myPath;
    private final INode myDirectory;
    private final CloseChecker closeChecker = new CloseChecker() {
        
        @Override
        public void assertNotClosed() throws FileSystemException {
            if(closed) {
                throw new ClosedDirectoryStreamException(); 
            }
            
        }
    }; 
    private final CloseTracker closeTracker;
    
    public static DirectoryStream<Path> makeDirectoryStream(
            INode directory,
            EphemeralFsPath path, 
            List<Path> paths) {
        
        EphemeralFsSecureDirectoryStream answer = new EphemeralFsSecureDirectoryStream(directory, path, paths);
        if(directory.getFs().getSettings().isWindows() || directory.getFs().getSettings().isMac()) {
            return answer.nonSecure();
        }
        return answer;
    }
    
    private EphemeralFsSecureDirectoryStream(INode directory, EphemeralFsPath path, List<Path> paths) {
        this.paths = new ArrayList<Path>(paths);
        this.myDirectory = directory;
        this.myPath = path;
        this.closeTracker = path.getFileSystem().trackClose(DirectoryStream.class, path);
    }
    
    @Override
    public void close() throws IOException {
        closed = true;
        closeTracker.onClose();
    }

    @Override
    public Iterator<Path> iterator() {
        if(used) {
            throw new IllegalStateException("you can only iterate once over a DirectoryStream");
        }
        if(closed) {
            throw new IllegalStateException("already closed");
        }
        used = true;
        
        
        //close() should mean that we don't return more results
        //other than what we have already computed by calls to hasNext()
        return new Iterator<Path>() {
            final Iterator<Path> delegate = paths.iterator();
            Path peeked;
            
            @Override
            public boolean hasNext() {
                if(closed) {
                    empty();
                }
                if(peeked != null) {
                    return true;
                }
                if(delegate.hasNext()) {
                    peeked = delegate.next();
                    return true;
                }
                return false;
            }

            @Override
            public Path next() {
                if(closed) {
                    empty();
                }
                if(peeked != null) {
                    Path answer = peeked;
                    peeked = null;
                    return answer;
                }
                //the caller hasn't called hasNext()
                return delegate.next();
            }

            private void empty() {
                while(delegate.hasNext()) {
                    delegate.next();
                }
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public SecureDirectoryStream<Path> newDirectoryStream(Path path,
            LinkOption... options) throws IOException {
        EphemeralFsPath efsPath = cast(path);
        synchronized(efsPath.fs.fsLock) {
            EphemeralFsPath actualPath = translate(efsPath);
            for(LinkOption option : options) {
                if(option == LinkOption.NOFOLLOW_LINKS) {
                    ResolvedPath resolved = ResolvedPath.resolve(actualPath, true);
                    if(resolved.resolvedToSymbolicLink()) {
                        throw new FileSystemException(path + ": Too many levels of symbolic links");
                    }
                }
            }
            
            return (SecureDirectoryStream<Path>) actualPath.fs.newDirectoryStream(
                    actualPath,
                    efsPath.isAbsolute() ? efsPath : myPath.resolve(efsPath),
                    new Filter<Path>() {
                @Override
                public boolean accept(Path entry) throws IOException {
                    return true;
                }
            } );
        }
        
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
            Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        EphemeralFsPath efsPath = cast(path);
        synchronized(efsPath.fs.fsLock) {
            EphemeralFsPath actualPath = translate(efsPath);
            return efsPath.fs.newByteChannel(actualPath, options, attrs);
        }
    }

    @Override
    public void deleteFile(Path path) throws IOException {
        EphemeralFsPath efsPath = cast(path);
        synchronized(efsPath.fs.fsLock) {
            EphemeralFsPath actualPath = translate(efsPath);
            if(actualPath == null) {
                throw new NoSuchFileException(path.toString());
            }
            ResolvedPath resolved = ResolvedPath.resolve(actualPath, true);
            if(resolved.hasTarget() && resolved.getTarget().isDir()) {
                throw new FileSystemException(path + ": Is a directory");
            } 
            actualPath.fs.delete(actualPath);
        }
    }

    @Override
    public void deleteDirectory(Path path) throws IOException {
        EphemeralFsPath efsPath = cast(path);
        synchronized(efsPath.fs.fsLock) {
            EphemeralFsPath actualPath = translate(efsPath);
            if(actualPath == null) {
                throw new NoSuchFileException(path.toString());
            }
            
            ResolvedPath resolved = ResolvedPath.resolve(actualPath, true);
            if(resolved.resolvedToSymbolicLink()) {
                throw new FileSystemException("symlink: Not a directory");
            }
            if(!resolved.getTarget().isDir()) {
                throw new FileSystemException(path + ": Not a directory");
            }
            actualPath.fs.delete(actualPath);
            return;
        }
        
    }

    @Override
    public void move(
            Path srcpath, 
            SecureDirectoryStream<Path> targetdir,
            Path targetpath) throws IOException {
        EphemeralFsPath efsSrcPath = cast(srcpath);
        EphemeralFsPath efsTargetPath = cast(targetpath);
        EphemeralFsSecureDirectoryStream efsTargetDir = cast(targetdir);
        synchronized(efsSrcPath.fs.fsLock) {
            
            EphemeralFsPath actualSrcPath = translate(efsSrcPath);
            EphemeralFsPath actualTargetPath = efsTargetDir.translate(efsTargetPath);
            
            efsSrcPath.fs.move(actualSrcPath, actualTargetPath, new CopyOption[] {StandardCopyOption.ATOMIC_MOVE});
        }
        
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
        synchronized(myPath.fs.fsLock) {
            return myPath.fs.getFileAttributeView(
                    new EphemeralFsPathProvider() {

                        @Override
                        public EphemeralFsPath get() {
                            return myDirectory.getPathToRoot();
                        }
                    },
                    type, 
                    closeChecker);
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path,
            Class<V> type, LinkOption... options) {
        final EphemeralFsPath efsPath = cast(path);
        synchronized(efsPath.fs.fsLock) {
            return efsPath.fs.getFileAttributeView(
                    new EphemeralFsPathProvider() {
                        @Override
                        public EphemeralFsPath get() {
                            return translate(efsPath);
                        }
                    }, 
                    type, 
                    closeChecker, 
                    options);
        }
    }
    
    private EphemeralFsPath cast(Path p) {
        if(p.getFileSystem() != this.myPath.fs) {
            throw new IllegalStateException("wrong fs");
        }
        return (EphemeralFsPath) p;
    }
    
    private EphemeralFsSecureDirectoryStream cast(SecureDirectoryStream<Path> p) {
        if(!(p instanceof EphemeralFsSecureDirectoryStream)) {
            throw new IllegalStateException("wrong file system:" + p);
        }
        EphemeralFsSecureDirectoryStream answer = (EphemeralFsSecureDirectoryStream) p;
        if(answer.myPath.fs != myPath.fs) {
            throw new IllegalStateException("wrong fs");
        }
        return answer;
    }
    
    private EphemeralFsPath translate(EphemeralFsPath path) {
        if(path.isAbsolute()) {
            return path;
        }
        EphemeralFsPath pathToRoot = myDirectory.getPathToRoot();
        if(pathToRoot == null) {
            return null;
        }
        return pathToRoot.resolve(path);
    }
    
    private DirectoryStream<Path> nonSecure() {
        return new DirectoryStream<Path>() {

            @Override
            public void close() throws IOException {
                EphemeralFsSecureDirectoryStream.this.close();
            }

            @Override
            public Iterator<Path> iterator() {
                return EphemeralFsSecureDirectoryStream.this.iterator();
            }
            
        };
    }
}
