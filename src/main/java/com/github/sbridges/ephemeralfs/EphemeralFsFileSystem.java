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
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

class EphemeralFsFileSystem extends FileSystem {

    //lock protecting this file system
    //all file system directory and meta data
    //operations must hold this lock
    //
    //reads/writes to streams on individual
    //files are not protected by this lock
    final Object fsLock = new Object();
    
    private final Settings settings;
    private final String name;
    private volatile boolean closed = false;
    private final EphemeralFsFileSystemProvider provider;
    private final INode root;
    private final DefaultAsyncThreadPoolHolder asyncThreadPoolHolder = new
            DefaultAsyncThreadPoolHolder();
    private final UserPrincipalLookupService userPrincipalLookupService = 
            new EphemeralFsUserPrincipalLookupService();
    private final WatchRegistry watchRegistry = new WatchRegistry();
    private final EphemeralFsFileStore fileStore = new EphemeralFsFileStore(this);
    private final Limits limits;
    private final AttributeLookup attributes;
    
    public AttributeLookup getAttributes() {
        return attributes;
    }

    private final Set<CloseTracker> notClosed = Collections.newSetFromMap(new ConcurrentHashMap<CloseTracker, Boolean>());
    
    public WatchRegistry getWatchRegistry() {
        return watchRegistry;
    }

    public void closed(CloseTracker tracker) {
        notClosed.remove(tracker);
    }
    
    public CloseTracker trackClose(Class<?> type, EphemeralFsPath path) {
        CloseTracker answer = new CloseTracker(type, this, path);
        notClosed.add(answer);
        return answer;
    }
    
    EphemeralFsFileSystem(String name, Settings settings, EphemeralFsFileSystemProvider provider) {
        this.name = name;
        this.settings = settings;
        this.provider = provider;
        this.root = INode.createRoot(this);
        this.limits = new Limits(settings);
        
        if(settings.isWindows()) {
            attributes = new AttributeLookup(
                    AttributeSet.BASIC,
                    AttributeSet.OWNER,
                    AttributeSet.DOS
                    );
        } else {
            attributes = new AttributeLookup(
                    AttributeSet.BASIC,
                    AttributeSet.DOS,
                    AttributeSet.POSIX,
                    AttributeSet.UNIX,
                    AttributeSet.OWNER
                    );
        }
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        provider.closing(this);
        asyncThreadPoolHolder.close();
        closed = true;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return settings.getSeperator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton((Path) getRootPath());
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.unmodifiableList(Arrays.asList((FileStore) fileStore));
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return attributes.getViews();
    }

    @Override
    public EphemeralFsPath getPath(String first, String... more) {
        assertOpen();
        return new EphemeralFsPath(this, first, more);
    }
    
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        if(syntaxAndPattern.startsWith("regex:")) {
            return regexPathMatcher(syntaxAndPattern.substring("regex:".length()));
        } else if(syntaxAndPattern.startsWith("glob:")) {
            String glob = syntaxAndPattern.substring("glob:".length());
            String regex = GlobUtil.globToRegex(glob, 
                    settings.isPosix()
                    );
            return regexPathMatcher(regex); 
        } else if(!Pattern.matches(".+:.+", syntaxAndPattern)) {
            throw new IllegalArgumentException("syntaxAndPattern must take the form syntax:patterbn, not" + syntaxAndPattern);
        } else {
            throw new UnsupportedOperationException("invlalid syntaxAndPattern:" + syntaxAndPattern);
        }
    }

    private PathMatcher regexPathMatcher(String regex) {
        int flags = 0;
        if(!getSettings().caseSensitive()) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        final Pattern p = Pattern.compile(regex, flags);
        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return p.matcher(path.toString()).matches();
            }
        };
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return userPrincipalLookupService;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return new EphemeralFsWatchService(this);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name=" + name + "]";
    }

    public AsynchronousFileChannel newAsynchronousByteChannel(
            EphemeralFsPath efsPath, Set<? extends OpenOption> options,
            ExecutorService executor, FileAttribute<?>[] attrs) throws IOException {
        if(executor == null) {
            executor = asyncThreadPoolHolder.getThreadPool();
        }
        EphemeralFsFileChannel channel = newByteChannel(efsPath, options, attrs);
        return new EphemeralFsAsynchronousFileChannel(channel, executor);
    }
    
    EphemeralFsFileChannel newByteChannel(EphemeralFsPath path,
            Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        
        if(getSettings().isWindows()) {
            for(FileAttribute<?> attr : attrs) {
                if(attr.name().equals("posix:permissions")) {
                    throw new UnsupportedOperationException("'posix:permissions' not supported as initial attribute");
                }
            }
        }
        
        synchronized(fsLock) {
            
            if(!isOpen()) {
                throw new FileSystemException("closed");
            }
            
            
            boolean noFollow = options.contains(LinkOption.NOFOLLOW_LINKS);
            
            ResolvedPath resolvedPath = ResolvedPath.resolve(path, noFollow);
            
            if(resolvedPath.resolvedToSymbolicLink() && noFollow) {
                throw new IOException("Too many levels of symbolic links (NOFOLLOW_LINKS specified)");
            }

            //don't delete on close if we are posix, instead we delete before returning
            boolean deleteOnClose = !settings.isPosix() && options.contains(StandardOpenOption.DELETE_ON_CLOSE);
            //in unix, delete on close is implemented as delete on
            //open
            boolean deleteOnOpen = settings.isPosix() && options.contains(StandardOpenOption.DELETE_ON_CLOSE);
            
            
            
            if(resolvedPath.hasTarget()) { 
                limits.tryAcquireFileHandle();
                EphemeralFsFileChannel answer = resolvedPath.getTarget().newFileChannel(
                        options, deleteOnClose, resolvedPath);

                if(deleteOnOpen && !resolvedPath.getTarget().isDir()) {
                    delete(path);
                }
                return answer;
            }
            
            if(options.contains(StandardOpenOption.CREATE) ||
               options.contains(StandardOpenOption.CREATE_NEW)) {
                
                //CREATE and CREATE_NEW are ignored if
                //WRITE is not set
                if(!options.contains(StandardOpenOption.WRITE)) {
                    throw new NoSuchFileException(path.toString());
                }
                if(!resolvedPath.hasValidParent()) {
                    throw new NoSuchFileException(path.toString());
                }

                
                //if we have a file system like
                //   /a (a file)
                //   /b (link -> a)
                //  
                //  when we do Files.write(b) we actually write to a
                //  if we do Files.create(b) we fail since
                //  b already exists, we can tell the difference
                //  between the two using the CREATE_NEW setting
                //
                EphemeralFsPath realPath;
                if(options.contains(StandardOpenOption.CREATE_NEW)) {
                    ResolvedPath resolvedForCreate = ResolvedPath.resolve(path, true);
                    realPath = resolvedForCreate.getPath();
                } else {
                    realPath = resolvedPath.getPath();
                }

                if(realPath.getParent() == null) {
                    throw new IOException("No Parent");
                }
                if(realPath.getFileName().toString().equals("..") ||
                        realPath.getFileName().toString().equals(".")) {
                    throw new IOException("invalid path:" + realPath);
                }
                
                INode parent = resolvedPath.getParent();

                
                if(!parent.isDir()) {
                    throw new IOException("not a directory");
                }
                //acquire a file handle before we create the directory entry
                limits.tryAcquireFileHandle();
                INode child = parent.addFile(realPath.getFileName(), new FilePermissions(false, attrs));
                Set<OpenOption> openOptionsCopy = new HashSet<>(options);
                openOptionsCopy.remove(StandardOpenOption.CREATE);
                openOptionsCopy.remove(StandardOpenOption.CREATE_NEW);
                
                EphemeralFsFileChannel answer = child.newFileChannel(openOptionsCopy, deleteOnClose, ResolvedPath.resolve(realPath, false));
                //in unix, delete on close is implemented as delete on
                //open
                if(deleteOnOpen) {
                    delete(path);
                }
                return answer;
            }
            
            throw new NoSuchFileException(path.toString());
            
        }
    }
    
    void checkAccess(EphemeralFsPath path, AccessMode... modes) throws IOException {
        synchronized(fsLock) {
            ResolvedPath resolved = ResolvedPath.resolve(path, false);
            if(!resolved.hasTarget()) {
                throw new NoSuchFileException("Could not find:"  + path);
            }
            
            for(AccessMode m : modes) {
                switch(m) {
                case READ :
                    if(!resolved.getTarget().canRead()) {
                        throw new AccessDeniedException(path.toString());
                    } 
                    break;
                case WRITE :
                    if(!resolved.getTarget().canWrite()) {
                        throw new AccessDeniedException(path.toString());
                    } 
                    break;
                case EXECUTE :
                    if(!resolved.getTarget().canExecute()) {
                        throw new AccessDeniedException(path.toString());
                    } 
                    break;  
                default :
                    throw new IllegalStateException();
                }
            }
            
        }
    }
    
    void createDirectory(EphemeralFsPath dir, FileAttribute<?>... attrs)
            throws IOException {
        dir = dir.toAbsolutePath();
        synchronized(fsLock) {
            //this is root
            if(dir.getParent() == null) {
                throw new FileAlreadyExistsException(dir.toString());
            }
            ResolvedPath resolvedPath = ResolvedPath.resolve(dir.getParent(), false);
            if(!resolvedPath.hasTarget()) { 
                throw new NoSuchFileException(dir.getParent().toString());
            }
            if(!resolvedPath.getTarget().isDir()) {
                throw new FileSystemException(dir.getParent() + " : is Not a directory");
            } 
            resolvedPath.getTarget().addDir(dir.getFileName(), new FilePermissions(true, attrs));
        }
    }
    

    void createSymbolicLink(EphemeralFsPath link, EphemeralFsPath target, FileAttribute<?>[] attrs) throws IOException {
        synchronized(fsLock) {
            EphemeralFsPath dir = link.getParent();
            ResolvedPath resolvedPath = ResolvedPath.resolve(dir, false);
            if(!resolvedPath.hasTarget()) { 
                throw new NoSuchFileException(dir.toString());
            }
            if(!resolvedPath.getTarget().isDir()) {
                throw new FileSystemException(dir + " : is Not a directory");
            } 
            resolvedPath.getTarget().addSymlink(link.getFileName(), target);
        }
    }
    

    void createLink(EphemeralFsPath link, EphemeralFsPath existing) throws IOException {
        synchronized(fsLock) {
            EphemeralFsPath dir = link.getParent();
            ResolvedPath resolvedPath = ResolvedPath.resolve(dir, false);
            if(!resolvedPath.hasTarget()) { 
                throw new NoSuchFileException(dir.toString());
            }
            if(!resolvedPath.getTarget().isDir()) {
                throw new FileSystemException(dir + " : is Not a directory");
            }
            ResolvedPath existingResolved = ResolvedPath.resolve(existing);
            if(!existingResolved.hasTarget()) {
                throw new NoSuchFileException(link.toString());
            }
            if(existingResolved.getTarget().isDir()) {
                throw new FileSystemException(link +  " -> " + existing + ": Operation not permitted");                
            }
            resolvedPath.getTarget().add(link.getFileName(), existingResolved.getTarget());
        }
    }


    
    void delete(EphemeralFsPath path) throws IOException {
        synchronized(fsLock) {
            ResolvedPath resolvedPath = ResolvedPath.resolve(path, true);
            if(resolvedPath.hasTarget()) {
                INode iNode = resolvedPath.getTarget();
                if(iNode.isDir() && !iNode.isEmpty()) {
                    throw new DirectoryNotEmptyException(path.toString());
                }
            }
            if(!resolvedPath.didResolve()) {
                throw new NoSuchFileException(path.toString());
            }
            if(getSettings().isWindows() && resolvedPath.getResolvedProperties().getDosIsReadOnly()) {
                throw new AccessDeniedException(path.toString());
            }
            resolvedPath.getParent().remove(resolvedPath.getPath().getFileName());
        }
        
    }
    
    void move(EphemeralFsPath source, EphemeralFsPath target, CopyOption[] options) throws IOException {
        EnumSet<StandardCopyOption> optionsSet = EnumSet.noneOf(StandardCopyOption.class);
        if(options != null) {
            for(CopyOption option : options) {
                if(option instanceof StandardCopyOption) {
                    optionsSet.add((StandardCopyOption) option);
                } else {
                    throw new IllegalArgumentException("unrecognized option:" + option);
                }
            }
        }
        
        if(isSameFile(source, target)) {
            return;
        }
        
        synchronized(fsLock) {
            ResolvedPath sourceResolved = ResolvedPath.resolve(source, true);
            ResolvedPath targetResolved = ResolvedPath.resolve(target);

            if(!sourceResolved.hasTarget() && !sourceResolved.resolvedToSymbolicLink()) {
                throw new NoSuchFileException(source.toString());
            }
            if(sourceResolved.hasTarget() && sourceResolved.getTarget() == root) {
                throw new IOException("cant move root");
            }
            if(!targetResolved.hasValidParent()) {
                throw new NoSuchFileException(target.toString());
            }
            if(targetResolved.hasTarget()) {
                if(!optionsSet.contains(StandardCopyOption.REPLACE_EXISTING) &&
                   //ATOMIC_MOVE on unix at least implies replace existing
                   !optionsSet.contains(StandardCopyOption.ATOMIC_MOVE)) {
                    throw new FileAlreadyExistsException(target.toString());
                }
                if(getSettings().isWindows() &&
                    optionsSet.contains(StandardCopyOption.ATOMIC_MOVE)) {
                    throw new AccessDeniedException(target.toString());
                }
                if(targetResolved.getTarget().isDir() && !targetResolved.getTarget().isEmpty()) {
                    throw new DirectoryNotEmptyException(target.toString());
                }
                //remove target
                targetResolved.getParent().remove(target.getFileName());
            }
            
            if(sourceResolved.resolvedToSymbolicLink()) {
                targetResolved.getParent().addSymlink(target.getFileName(), sourceResolved.getRawSymbolicLink());
            } else {
                targetResolved.getParent().add(target.getFileName(), sourceResolved.getTarget());
            }
            sourceResolved.getParent().remove(source.getFileName());
        }
        
    }
    

    public void copy(EphemeralFsPath source, EphemeralFsPath target, CopyOption... options) throws IOException {
        
        boolean noFollowLinks = false;
        EnumSet<StandardCopyOption> optionsSet = EnumSet.noneOf(StandardCopyOption.class);
        if(options != null) {
            for(CopyOption option : options) {
                if(option instanceof StandardCopyOption) {
                    optionsSet.add((StandardCopyOption) option);
                } else if( option == LinkOption.NOFOLLOW_LINKS){
                    noFollowLinks = true;
                }
            }
        }
        
        if(optionsSet.contains(StandardCopyOption.ATOMIC_MOVE)) {
            throw new UnsupportedOperationException("Atomic Move is not supported");
        }
        
        synchronized(fsLock) {
            final ResolvedPath resolvedSource = ResolvedPath.resolve(source, noFollowLinks);
            final ResolvedPath resolvedTarget = ResolvedPath.resolve(target);
            
            //same file
            if(resolvedSource.hasTarget() && resolvedTarget.hasTarget() &&
               resolvedSource.getTarget() == resolvedTarget.getTarget()) {
                return;
            }

            if(resolvedSource.hasTarget() && resolvedSource.getTarget() == root) {
                throw new IOException("can't copy root");
            }
            
            if(!resolvedSource.didResolve()) {
                throw new NoSuchFileException(source.toString());
            }
            if(resolvedTarget.didResolve() && !optionsSet.contains(StandardCopyOption.REPLACE_EXISTING)) {
                throw new FileAlreadyExistsException(target.toString());
            }
            if(resolvedTarget.hasTarget() && resolvedTarget.getTarget().isDir() && !resolvedTarget.getTarget().isEmpty()) {
                throw new DirectoryNotEmptyException(target.toString());
            }
            
            //at this point, if target exists, we are replacing existing
            if(resolvedTarget.didResolve()) {
                resolvedTarget.getParent().remove(target.getFileName());
            }
            else if(!resolvedTarget.hasValidParent()) {
                throw new NoSuchFileException(target.toString());
            }
            
            
            INode modified = null;
            if(resolvedSource.hasTarget() && resolvedSource.getTarget().isDir()) {
                modified = resolvedTarget.getParent().addDir(target.getFileName(), FilePermissions.createDefaultDirectory());
            } else if(resolvedSource.resolvedToSymbolicLink()) {
                resolvedTarget.getParent().addSymlink(target.getFileName(), resolvedSource.getRawSymbolicLink());
            }
            else {
                modified = resolvedTarget.getParent().addFile(target.getFileName(), FilePermissions.createDefaultFile());
                try(SeekableByteChannel targetChannel = newByteChannel(
                        //resolve again, the original resolved target
                        //may not point to the right (or any) INode
                        target,
                        EnumSet.of(StandardOpenOption.WRITE)
                        );
                    SeekableByteChannel sourceChannel = newByteChannel(
                            resolvedSource.getPath(), 
                            EnumSet.of(StandardOpenOption.READ))) {

                    ByteBuffer buf = ByteBuffer.allocate(Math.min(4096, resolvedSource.getTarget().getContentsSize()));
                    while (sourceChannel.read(buf) >= 0 || buf.position() != 0) {
                         buf.flip();
                         targetChannel.write(buf);
                         buf.compact();  
                     }
                }
                modified.copyPermissions(resolvedSource.getTarget());
            }
            
            if(modified != null) {
                if(settings.isPosix()) {
                    if(optionsSet.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
                        modified.getProperties().getFileTimes().setLastModifiedTime(
                                resolvedSource.getResolvedProperties().getFileTimes().getLastModifiedTime());
                    }
                } else {
                    //windows always copies last modified time it seems
                    modified.getProperties().getFileTimes().setLastModifiedTime(
                            resolvedSource.getResolvedProperties().getFileTimes().getLastModifiedTime());
                }
            }
            
        }
        
    }

    
    private void assertOpen() {
        if(!isOpen()) { 
            throw new IllegalStateException("already closed");
        }
    }

    public EphemeralFsPath getRootPath() {
        return getPath(settings.getRoot());
    }
    
    public INode getRoot() {
        return root;
    }

    public DirectoryStream<Path> newDirectoryStream(EphemeralFsPath dir,
            Filter<? super Path> filter) throws IOException {
        return newDirectoryStream(dir, dir, filter);
    }
    
    public <V extends FileAttributeView> V getFileAttributeView(
            EphemeralFsPathProvider pathProvider,
            Class<V> type,
            CloseChecker closeChecker,
            LinkOption... options) {
        return getFileAttributesViewBuilder(
                pathProvider, closeChecker, options).build(type);
    }

    public FileAttributesViewBuilder getFileAttributesViewBuilder(
            EphemeralFsPathProvider pathProvider, CloseChecker closeChecker,
            LinkOption... options) {
        FileAttributesViewBuilder builder;
        synchronized(fsLock) {
                builder = new FileAttributesViewBuilder(
                        this, 
                        pathProvider, 
                        closeChecker, 
                        options);
        }
        return builder;
    }
    
    DirectoryStream<Path> newDirectoryStream(
            EphemeralFsPath dir,
            EphemeralFsPath relativeDir,
            Filter<? super Path> filter) throws IOException {
        
        synchronized(fsLock) {
           ResolvedPath resolvedDir = ResolvedPath.resolve(dir);
           if(!resolvedDir.hasTarget()) {
               throw new NoSuchFileException(dir.toString());
           }
           if(!resolvedDir.getTarget().isDir()) {
               throw new NotDirectoryException(dir.toString());
           }
           
           List<Path> parts = new ArrayList<>();
           
           for(EphemeralFsPath childName : resolvedDir.getTarget().getChildNames()) {
               Path child = dir.resolve(childName);
               if(filter.accept(child)) {
                   parts.add(relativeDir.resolve(child.getFileName()));
               }
           }
           return EphemeralFsSecureDirectoryStream.makeDirectoryStream(
                   resolvedDir.getTarget(),
                   relativeDir, 
                   parts);
        }
    }

    public boolean isSameFile(EphemeralFsPath path1, EphemeralFsPath path2) throws FileSystemException {
        synchronized (fsLock) {
            ResolvedPath resolved1 = ResolvedPath.resolve(path1, false);
            ResolvedPath resolved2 = ResolvedPath.resolve(path2, false);
            if(!resolved1.hasTarget() || !resolved2.hasTarget()) {
                return false;
            }
            return resolved1.getTarget() == resolved2.getTarget();
        }
    }


    public Settings getSettings() {
        return settings;
    }

    public Path readSymbolicLink(EphemeralFsPath link) throws FileSystemException {
        synchronized(fsLock) {
            ResolvedPath resolved = ResolvedPath.resolve(link.getParent());
            if(!resolved.hasTarget()) {
                throw new NoSuchFileException(link.toString());
            }
            return resolved.getTarget().getRawSymbolicLink(link.getParent(), link.getFileName());
            
        }
        
    }

    public Limits getLimits() {
        return limits;
    }
    
    public void assertNoOpenResources() throws AssertionError {
        synchronized(fsLock) {
            Set<CloseTracker> open = new HashSet<>(notClosed);
            if(open.isEmpty()) {
                return;
            }
            StringBuilder builder = new StringBuilder("Failed to close " + open.size() + " resource(s)");
            int failedCount = 1;
            
            for(CloseTracker tracker : open) {
                builder.append("\n").append(failedCount).append(") ");
                builder.append(tracker.getErrorString().trim());
                failedCount++;
                if(failedCount == 100) {
                    builder.append("\n...");
                    break;
                }
            }
            throw new AssertionError(builder.toString().trim());
        }
    }

    public void assertAllFilesFsynced(EphemeralFsPath path) throws AssertionError {
        synchronized(fsLock) {
            final List<Path> notFsynced = new ArrayList<>();
            ResolvedPath resolved;
            try {
                resolved = ResolvedPath.resolve(path);
            
                if(!resolved.didResolve() || resolved.resolvedToSymbolicLink()) {
                    throw new IllegalArgumentException("not found:" + path);
                }
                
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file,
                            BasicFileAttributes attrs) throws IOException {
                        ResolvedPath resolved = ResolvedPath.resolve((EphemeralFsPath) file);
                        if(resolved.getTarget().isDirty()) {
                            notFsynced.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                
                if(notFsynced.isEmpty()) {
                    return;
                }
                throw new AssertionError("Failed to sync " + notFsynced);
                
                
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void assertAllDirectoriesFsynced(
            EphemeralFsPath dir,
            boolean recursive) {
        synchronized(fsLock) {
            final List<Path> notFsynced = new ArrayList<>();
            ResolvedPath resolved;
            try {
                resolved = ResolvedPath.resolve(dir);
            
                if(!resolved.didResolve() || resolved.resolvedToSymbolicLink()) {
                    throw new IllegalArgumentException("not found:" + dir);
                }
                if(resolved.getTarget().isFile()) {
                    throw new IllegalArgumentException(dir + " is not a directory");
                }
                
                if(!recursive && resolved.getTarget().isDirty()) {
                    throw new AssertionError("Failed to sync " + dir);
                }
                
                Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
    
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir,
                            BasicFileAttributes attrs) throws IOException {
                        ResolvedPath resolved = ResolvedPath.resolve((EphemeralFsPath) dir);
                        if(resolved.getTarget().isDirty()) {
                            notFsynced.add(dir);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                
                if(notFsynced.isEmpty()) {
                    return;
                }
                throw new AssertionError("Failed to sync " + notFsynced);
                
                
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
