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
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.NotLinkException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A file or a directory. 
 */
class INode {
    
    //only set if this is a directory
    private final Map<FileName, DirectoryEntry> children;
    //this is set if this is a directory, allows
    private final FileContents contents;
    private final EphemeralFsFileSystem fs;
    private final List<INode> parents = new ArrayList<>();
    private final boolean root;
    
    
    private int hardLinks = -1;
    private int openFileHandles;
    
    private final FileProperties fileProperties;
    
    /**
     * Create an directory INode
     */
    private INode(
            Map<FileName, DirectoryEntry> children,
            EphemeralFsFileSystem fileSystem,
            FilePermissions filePermissions,
            boolean root) {
        this.children = children;
        //NOTE - in unix we can read the contents of a directory
        //for java this has no effect other than allowing
        //fsyncing the directory
        this.contents = new FileContents(fileSystem, this);
        //directories are initially clean
        this.contents.setDirty(false);
        this.fs = fileSystem;
        this.root = root;
        this.fileProperties = new FileProperties(fileSystem, filePermissions, false);
    }

    /**
     * Create a file INode
     */
    private INode(EphemeralFsFileSystem fileSystem, FilePermissions filePermissions) {
        this.contents = new FileContents(fileSystem, this);
        this.children = null;
        this.fs = fileSystem;
        this.root = false;
        this.fileProperties = new FileProperties(fileSystem, filePermissions, true);
        
    }

    static INode createRoot(EphemeralFsFileSystem fileSystem) {
        return new INode(new HashMap<FileName, DirectoryEntry>(), fileSystem, FilePermissions.createDefaultDirectory(), true);
    }
    
    public INode addFile(EphemeralFsPath name, FilePermissions filePermissions) throws IOException {
        assertCanAddChild(name);
        INode answer = new INode(fs, filePermissions);
        add(name, answer);
        return answer;
    }
    
    public INode addDir(EphemeralFsPath name, FilePermissions filePermissions) throws IOException {
        assertCanAddChild(name);
        INode answer = new INode(new HashMap<FileName, DirectoryEntry>(), fs, filePermissions, false);
        add(name, answer);
        return answer;
    }
    
    public void add(EphemeralFsPath name, INode child) throws IOException {
        assertCanAddChild(name);
        children.put(name.toFileName(), new DirectoryEntry(child));
        child.parents.add(this);
        child.addLink();
        EphemeralFsWatchEvent event = new EphemeralFsWatchEvent(name, StandardWatchEventKinds.ENTRY_CREATE);
        fs.getWatchRegistry().hearChange(this,  event);
        contents.setDirty(true);
    }
    
    public void remove(EphemeralFsPath name) {
        if(!isDir()) {
            throw new IllegalStateException();
        }
        assertOnlyFileName(name);
        
        DirectoryEntry entry = children.remove(name.toFileName());
        if(entry == null) {
            throw new IllegalStateException("removing but nothing exists, name:" + name);
        }
        if(!entry.isSymbolicLink() && !entry.getDestination().parents.remove(this)) {
            throw new IllegalStateException("failed to remove parent? this:" + this + " entry:" + entry);
        }
        if(entry.getDestination() != null) {
            entry.getDestination().removeLink();
        }
        EphemeralFsWatchEvent event = new EphemeralFsWatchEvent(name, StandardWatchEventKinds.ENTRY_DELETE);
        fs.getWatchRegistry().hearChange(this, event);
        contents.setDirty(true);
    }
    
    public boolean isSymbolicLink(EphemeralFsPath name) {
        assertOnlyFileName(name);
        DirectoryEntry de = children.get(name.toFileName());
        if(de == null) {
            return false;
        }
        return de.isSymbolicLink();
    }
    
    public void addSymlink(EphemeralFsPath name, EphemeralFsPath to) throws IOException {
        if(!fs.getSettings().allowSymlink()) {
            throw new FileSystemException("symlinks are not supported");
        }
        assertCanAddChild(name);
        children.put(name.toFileName(),
                new DirectoryEntry(to));
    }
    
    public void addOpenFileHandle() { 
        openFileHandles++;
    }
    
    public void removeOpenFileHandle() {
        openFileHandles--;
        if(openFileHandles < 0) {
            throw new IllegalStateException();
        }
        freeIfNoReferences();
    }
    
    /**
     * Get an absolute path that will resolve to this INode, or null
     * if no such path exists.
     */
    EphemeralFsPath getPathToRoot() {
        Deque<String> paths = new ArrayDeque<>();
        INode current = this;
        while(true) {
            if(current == fs.getRoot()) {
                StringBuilder sb = new StringBuilder(fs.getSettings().getRoot());
                for(String path : paths) {
                    sb.append(path);
                    sb.append(fs.getSeparator());
                }
                return new EphemeralFsPath(fs, sb.toString());
            } else if(current.parents.isEmpty()) {
                //an orphaned directory, fail
                return null;
            } else {
                INode last = current;
                current = current.parents.get(0);
                for(Map.Entry<FileName, DirectoryEntry> entry : current.children.entrySet()) {
                    if(entry.getValue().getDestination() == last) {
                        paths.addFirst(entry.getKey().getPath().toString());
                    }
                }
            }
        }
    }
    
    private void assertCanAddChild(EphemeralFsPath name) throws IOException {
        if(isFile()) {
            throw new NotDirectoryException("can't add children to file");
        }
        if(name.toString().equals(".") || name.toString().equals("..")) {
            throw new IllegalStateException("invalid path:" + name);
        }
        if(fs.getSettings().getMaxPathLength() != Long.MAX_VALUE &&
                getPathToRoot().resolve(name).toString().length() > fs.getSettings().getMaxPathLength()) {
            throw new FileSystemException("Path too long");
        }
        
        assertOnlyFileName(name);
        if(children.containsKey(name.toFileName())) {
            throw new FileAlreadyExistsException("a child with name:" + name + " already exists");
        }
    }
    
    public boolean isFile() {
        return children == null;
    }
    
    public boolean isDirty() {
        return contents.isDirty();
    }
    
    public boolean isDir() {
        return !isFile();
    }

    public DirectoryEntry resolve(EphemeralFsPath name) {
        assertOnlyFileName(name);
        if(isFile()) { 
            return null;
        }
        
        FileName key = name.toFileName();
        if(!children.containsKey(key)) {
            return null;
        }
        return children.get(key);
    }
    
    public FileAttributesSnapshot getAttributes() throws IOException {
        int size;
        if(isFile()) {
            size = contents.getSize();
        }
        else if(fs.getSettings().isPosix()) {
            //linux reports non zero size for directories
            size = 4096;
        } else {
            size = 0;
        }
        return new FileAttributesSnapshot(
                isFile(),
                isDir(),
                false, /* symbolic link */
                false /* other */, 
                size,
                isDir() ? hardLinks + 1 : hardLinks,
                fileProperties
                );
    }
    
    /**
     * Note, this is not the size attribute!  directories have size 
     */
    public int getContentsSize() {
        if(!isFile()) {
            throw new IllegalStateException();
        }
        return contents.getSize();
    }
    
    public FileProperties getProperties() {
        return fileProperties;
    }

    public void notifyChange(EphemeralFsPath path) throws NoSuchFileException {
        ResolvedPath resolvedPath;
        try {
            resolvedPath = ResolvedPath.resolve(path.getParent(), false);
        } catch (FileSystemException e) {
            //we can't resolve the path
            //ignore and skip notifying
            return;
        }
        if(!resolvedPath.hasTarget()) {
            return;
        }
        
        if(resolvedPath.getTarget().isDir() && 
           resolvedPath.getTarget().getName(this) != null) {
            EphemeralFsWatchEvent event = new EphemeralFsWatchEvent(
                    path, 
                    StandardWatchEventKinds.ENTRY_MODIFY);
            
            fs.getWatchRegistry().hearChange(resolvedPath.getTarget(), event);    
        }
    }

    public EphemeralFsFileChannel newFileChannel(
            Set<? extends OpenOption> options,
            boolean deleteOnClose,
            ResolvedPath resolvedPath) throws IOException {
        
        //CREATE_NEW is ignored if we don't have the WRITE option
        if(options.contains(StandardOpenOption.CREATE_NEW) &&
           options.contains(StandardOpenOption.WRITE)) {
            throw new FileAlreadyExistsException(resolvedPath.getPath().toString());    
        }
        if(isDir()) {
            if(fs.getSettings().isWindows()) {
                throw new IOException("can't create channel for directory:" + resolvedPath.getPath().toString());
            }
            return contents.newChannel(
                    true,
                    false,
                    true,
                    false,
                    false,
                    false,
                    resolvedPath,
                    fs
                    );
        }
        boolean writeable = options.contains(StandardOpenOption.WRITE) ||
                            options.contains(StandardOpenOption.APPEND);
        boolean readable = options.contains(StandardOpenOption.READ) || 
                           !writeable;  
        
        if(writeable && fs.getSettings().isWindows() && resolvedPath.getResolvedProperties().getDosIsReadOnly()) {
            throw new AccessDeniedException(resolvedPath.getPath().toString());
        }
        
        return contents.newChannel(
                readable,
                writeable,
                options.contains(StandardOpenOption.APPEND),
                deleteOnClose,
                options.contains(StandardOpenOption.TRUNCATE_EXISTING),
                options.contains(StandardOpenOption.SYNC),
                resolvedPath,
                fs
                );
    }

    /**
     * If this is a directory, is the directory empty.
     * 
     * If this is not a directory, throws an IllegalStateException()
     */
    public boolean isEmpty() {
        if(!isDir()) {
            throw new IllegalStateException("not a directory");
        }
        return children.isEmpty();
    }

    public Iterable<EphemeralFsPath> getChildNames() {
        if(!isDir()) {
            throw new IllegalStateException();
        }
        List<EphemeralFsPath> answer = new ArrayList<>(children.size());
        for(FileName f : children.keySet()) {
            answer.add(f.getPath());
        }
        return answer;
    }
    
    public boolean exists() {
        if(root) {
            return true;
        }
        if(parents.isEmpty()) {
            return false;
        }
        for(INode parent : parents) {
            if(parent.exists()) {
                return true;
            }
        }
        return false;
    }
    
    private void assertOnlyFileName(EphemeralFsPath name) {
        if(name.getNameCount() != 1) {
            throw new IllegalStateException();
        }
        if(name.isAbsolute()) {
            throw new IllegalStateException();
        }
    }

    /**
     * Get the name of the given INode in this directory, or null
     * if there is no name. 
     */
    public EphemeralFsPath getName(INode iNode) {
        if(!isDir()) {
            throw new IllegalStateException("not a dir");
        }
        //TODO - the INOde can exist multiple times if it was hardlinked
        //multiple times
        for(Map.Entry<FileName, DirectoryEntry> e : children.entrySet()) {
            if(e.getValue().getDestination() == iNode) {
                return e.getKey().getPath();
            }
        }
        return null;
    }
   
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("INode[children=");
        builder.append(children);
        builder.append(", contents=");
        builder.append(contents);
        builder.append(", fs=");
        builder.append(fs);
        builder.append(", parents=");
        builder.append(parents);
        builder.append(", root=");
        builder.append(root);
        builder.append(", hardLinks=");
        builder.append(hardLinks);
        builder.append(", openFileHandles=");
        builder.append(openFileHandles);
        builder.append(", fileProperties=");
        builder.append(fileProperties);
        builder.append("]");
        return builder.toString();
    }

    public EphemeralFsPath getSymbolicLink(Path parent, EphemeralFsPath fileName) throws FileSystemException {
        return (EphemeralFsPath) parent.resolve(getRawSymbolicLink(parent, fileName));
    }

    public EphemeralFsPath getRawSymbolicLink(Path parent, EphemeralFsPath fileName) throws FileSystemException {
        DirectoryEntry entry = children.get(fileName.toFileName());
        if(entry == null) {
            throw new NoSuchFileException(parent.resolve(fileName).toString());
        }
        if(!entry.isSymbolicLink()) {
            throw new NotLinkException(parent.resolve(fileName).toString());
        }
        return entry.getSymbolicLink();
    }
    
    public void copyPermissions(INode other) {
        fileProperties.getFilePermissions().copyFrom(other.fileProperties.getFilePermissions());
    }
    
    public void setPermissions(Set<PosixFilePermission> perms) {
        fileProperties.getFilePermissions().setPermissions(perms);
    }
    
    public boolean canWrite() {
        return fileProperties.getFilePermissions().canWrite();
    }
    
    public boolean canRead() {
        return fileProperties.getFilePermissions().canRead();
    }
    
    public boolean canExecute() {
        if(fs.getSettings().isWindows()) {
            return fileProperties.getFilePermissions().canWrite();
        }
        return fileProperties.getFilePermissions().canExecute();
    }
    
    public EphemeralFsFileSystem getFs() {
        return fs;
    }

    private void addLink() {
        if(hardLinks == -1) {
            hardLinks = 1;
            return;
        }
        //if we are unlinked, don't allow us to be resurrected
        //this ensures move adds us to target 
        //before deleting from source
        //and prevents bugs where
        //we are unlinked multiple times due
        //to a move
        if(hardLinks == 0) {
            throw new IllegalStateException("resurrecting unlinked file");
        }
        hardLinks++;
    }
    
    void removeLink() {
        hardLinks--;
        if(hardLinks < 0) {
            throw new IllegalStateException("negative links?");
        }
        freeIfNoReferences();
    }

    private void freeIfNoReferences() {
        if(contents != null && hardLinks == 0 && openFileHandles == 0) {
            fs.getLimits().releaseDiskSpace(contents.getSize());
        }
        
    }



}