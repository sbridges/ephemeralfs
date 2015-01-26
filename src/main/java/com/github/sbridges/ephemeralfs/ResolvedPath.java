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

import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A path that has been resolved.<P>
 * 
 */
class ResolvedPath {
    
    static class ResolvedStep {
        //the directory for this step
        //the first step will have the root
        //as the directory
        final INode directory;
        //the name of the directory
        //entry that points to the next directory/or
        //File
        final String nextStep;
        
        public ResolvedStep(INode directory, String nextStep) {
            this.directory = directory;
            this.nextStep = nextStep;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ResolvedStep[directory=");
            builder.append(directory);
            builder.append(", nextStep=");
            builder.append(nextStep);
            builder.append("]");
            return builder.toString();
        }
    }
    
    public static ResolvedPath resolve(EphemeralFsPath path) throws FileSystemException {
        return resolve(path, false);
    }
    
    public static ResolvedPath resolve(EphemeralFsPath path, boolean noFollowLastSymlink) throws FileSystemException {
        
        try {
            
            if(!path.isAbsolute()) {
                Path rootPath = path.getFileSystem().getRootPath();
                return resolve((EphemeralFsPath) rootPath.resolve(path), noFollowLastSymlink);
            }
            
            //if we look for existing/nonExistent/..
            //windows resolves this by skipping looking
            //up nonExistent, 
            //linux will fail though when it sees the nonExistent Directory
            if(!path.getFileSystem().getSettings().isPosix()) {
                path = path.normalize();
            }
            
            List<EphemeralFsPath> parts = new ArrayList<>(path.splitPaths());
            Collections.reverse(parts);
            List<ResolvedStep> steps = new ArrayList<>(parts.size());
            
            return resolveAbsolutePath(path, path.getFileSystem(), path.getFileSystem().getRoot(), parts, steps, noFollowLastSymlink);
        } catch(StackOverflowError e) {
            //recursive links
            throw new FileSystemException(path +  ": Too many levels of symbolic links");
        }
    }

    //the recursion for this method is deliberate, it allows us
    //to detect recursive symlinks via stack overflow
    private static ResolvedPath resolveAbsolutePath(
            EphemeralFsPath oiginalPath,
            EphemeralFsFileSystem fs,
            //the current directory
            //that parts will be resolved from
            INode current,
            //the parts of the path that must still be resolved
            //in reverse order
            List<EphemeralFsPath> remaining,
            //the steps we have already resolved
            List<ResolvedStep> steps,
            boolean noFollowLastSymlink) throws FileSystemException {
        
        if(remaining.isEmpty()) {
            return new ResolvedPath(fs, steps, current, null, !steps.isEmpty());
        }
        
        EphemeralFsPath currentPath = remaining.remove(remaining.size() -1);
        String fileName = currentPath.toString();

        if(fileName.equals(".")) {
            if(!current.isDir()) {
                throw new FileSystemException(oiginalPath + ": Not a directory");
            }
            return resolveAbsolutePath(oiginalPath, fs, current, remaining, steps, noFollowLastSymlink);
        }
        else if(fileName.equals("..")) {
            if(!current.isDir()) {
                throw new FileSystemException(oiginalPath + ": Not a directory");
            }
            //we always have the root as the first step
            //
            if(steps.isEmpty()) {
                //we are trying to .. above the root
                //use root to explore
                //on linux, ls /../tmp will list /tmp
                return resolveAbsolutePath(oiginalPath, fs, fs.getRoot(), remaining, steps, noFollowLastSymlink);
            }
            ResolvedStep parent = steps.remove(steps.size() - 1);
            return resolveAbsolutePath(oiginalPath, fs, parent.directory, remaining, steps, noFollowLastSymlink);
        }
        
        if(current.isDir()) {
            DirectoryEntry entry = current.resolve(currentPath);
            if(entry != null) {
                if(entry.isSymbolicLink()) {
                    if(noFollowLastSymlink && remaining.isEmpty()) {
                        steps.add(new ResolvedStep(current, fileName));
                        return new ResolvedPath(fs, steps, null, current, true);
                    }
                    EphemeralFsPath linkTarget = entry.getSymbolicLink();
                    EphemeralFsPath absolutePathSoFar = getPaths(fs, steps);
                    List<EphemeralFsPath> newParts = absolutePathSoFar.resolve(linkTarget).splitPaths();
                    for(int i = newParts.size() -1; i >= 0; i--) {
                        remaining.add(newParts.get(i));
                    }
                    //we have a new absolute path to resolve
                    //start over
                    steps.clear();
                    return resolveAbsolutePath(oiginalPath, fs, fs.getRoot(), remaining, steps, noFollowLastSymlink);
                }
                steps.add(new ResolvedStep(current, fileName));
                return resolveAbsolutePath(oiginalPath, fs, entry.getDestination(), remaining, steps, noFollowLastSymlink);
            }
        } 

        //we can't resolve everything, stop
        //what we have resolved is still useful
        //for example resolving a non existing
        //path which we want to create
        steps.add(new ResolvedStep(current, fileName));
        if(remaining.isEmpty()) {
            return new ResolvedPath(fs, steps, null, null, true);
        } else {
            return new ResolvedPath(fs, steps, null, null, false);    
        }
        
    }
    
    //the steps we successfully took resolving the
    //path
    private final List<ResolvedStep> steps;
    private final INode target;
    private final INode directoryContainingSymlink;
    //was everything but the parent resolved
    //for example if we resolve
    // /a/b but only /a exists
    // /a is the valid parent
    //if we try to resolve /a/b/c and only
    // /a exists, there is no valid parent
    private final boolean hasValidParent;
    private final EphemeralFsFileSystem fs;

    private ResolvedPath(
            EphemeralFsFileSystem fs,
            List<ResolvedStep> steps,
            INode target,
            INode directoryContainingSymlink,
            boolean hasValidParent
            ) {
        this.fs = fs;
        this.steps = steps;
        this.target = target;
        this.directoryContainingSymlink = directoryContainingSymlink;
        this.hasValidParent = hasValidParent;
    }
    
    public boolean didResolve() {
        return hasTarget() || resolvedToSymbolicLink();
    }
    
    //for testing
    List<String> getSteps() {
        return new AbstractList<String>() {

            @Override
            public String get(int index) {
                return steps.get(index).nextStep;
            }

            @Override
            public int size() {
                return steps.size();
            }
        };
    }
    
    /**
     * Did we resolve to an existing file/directory
     * If true getTarget will not return null. 
     */
    public boolean hasTarget() {
        return target != null;
    }
    
    public boolean resolvedToSymbolicLink() {
        return directoryContainingSymlink != null;
    } 
    
    public INode getSymLinkParentDirectory() {
        return steps.get(steps.size() -1).directory;
    }
    
    /**
     * The full path, after symlinks are resolved.<P>
     * 
     * Note - we can get the full path if the
     * last element to be resolved does not exist, but
     * if multiple parts don't exist, we cant
     */
    public EphemeralFsPath getPath() {
        return getPaths(fs, steps);

    }
    
    private static EphemeralFsPath getPaths(EphemeralFsFileSystem fs, List<ResolvedStep> steps) {
        EphemeralFsPath answer = fs.getRootPath();
        for(int i = 0; i < steps.size(); i++) {
            answer = answer.resolve(steps.get(i).nextStep);
        }
        return answer;
    }

    public INode getTarget() throws NoSuchFileException {
        if(target == null) {
            throw new IllegalStateException("no follow and last is symlink:" + this);
        }
        return target;
    }
    
    public boolean hasValidParent() {
        return hasValidParent;
    }
    
    public INode getParent() {
        if(!hasValidParent) {
            throw new IllegalStateException();
        }
        return steps.get(steps.size() -1).directory;
    }
    

    public EphemeralFsPath getRawSymbolicLink() throws FileSystemException {
        if(!resolvedToSymbolicLink()) {
            throw new IllegalStateException();
        }
        return getParent().getRawSymbolicLink(null, fs.getPath(steps.get(steps.size() -1).nextStep));
    }
    
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResolvedPath[steps=");
        builder.append(steps);
        builder.append(", target=");
        builder.append(target);
        builder.append(", directoryContainingSymlink=");
        builder.append(directoryContainingSymlink);
        builder.append("]");
        return builder.toString();
    }


}
