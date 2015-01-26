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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

class EphemeralFsPath implements Path {
    
    final EphemeralFsFileSystem fs;
    private final String path;
    private List<String> cachedParts;
    
    private static String concat(String seperator, String first, String ... more) {
        StringBuilder builder = new StringBuilder();
        builder.append(first);
        for(String s : more) {
            if(s.isEmpty()) {
                continue;
            }
            if(builder.length() > 0) {
                builder.append(seperator);
            }
            builder.append(s);
        }
        String built = builder.toString();
        
        while(built.contains(seperator + seperator)) {
            built = built.replace(seperator + seperator, seperator);
        }
        return built;
    }
    
    EphemeralFsPath(EphemeralFsFileSystem fs, String first, String... more) {
        this(fs, concat(fs.getSeparator(), first, more));
    }
    
    EphemeralFsPath(EphemeralFsFileSystem fs, String path) {
        if(path == null || fs == null) {
            throw new NullPointerException();
        }
        
        this.fs = fs;
        
        if(fs.getSettings().isPosix() && path.length() > 1 && path.endsWith(fs.getSeparator())) {
            path = path.substring(0, path.length() - 1);
        }
        
        if(!fs.getSettings().isPosix() && 
           !path.matches("[A-Za-z]:\\\\") &&
           path.endsWith(fs.getSeparator()) &&
           path.length() > 1
           ) {
            path = path.substring(0, path.length() - 1);
        }
        
        if(fs.getSettings().isPosix()) {
            //the root path can be empty, but no other path is
            this.path = path;    
        } else {
            this.path = path.replaceAll("/+", "\\\\");
        }
        
        checkValidPath();
    }
    
    private void checkValidPath() {
        if(path.indexOf('\0') != -1) {
            throw new InvalidPathException(path, "Nul character not allowed");
        }
        
    }

    @Override
    public EphemeralFsFileSystem getFileSystem() {
        return fs;
    }
    @Override
    public boolean isAbsolute() {
        return fs.getSettings().getAbsolutePathPattern().matcher(path).matches();
    }
    
    @Override
    public EphemeralFsPath getRoot() {
        if(!isAbsolute()) {
            return null;
        }
          return newPath(getRootString());
    }
    
    @Override
    public EphemeralFsPath getFileName() {
        List<String> parts = split();
        if(parts.isEmpty()) {
            return null;
        }
        return newPath(parts.get(parts.size() -  1));
    }
    
    @Override
    public EphemeralFsPath getParent() {
        List<String> parts = split();
        if(parts.isEmpty()) {
            return null;
        }
        if(!isAbsolute() && parts.size() == 1) {
            if(!fs.getSettings().isPosix() && path.startsWith("\\")) {
                return newPath("\\");
            }
            return null;
        }
        
        String newPath = join(fs, (isAbsolute() ? getRootString() : null), parts.subList(0,  parts.size() - 1));
        if(!fs.getSettings().isPosix() && path.startsWith("\\")) {
            newPath = "\\" + newPath;
        }
        return newPath(newPath);
    }
    
    @Override
    public int getNameCount() {
        return split().size();
    }
    
    @Override
    public EphemeralFsPath getName(int index) {
        List<String> parts = split();
        if(index < 0 || index >= parts.size()) { 
            throw new IllegalArgumentException("invalid index:" + index + " nameCount:" + getNameCount());
        }
            
        return newPath(parts.get(index));
    }
    
    @Override
    public EphemeralFsPath subpath(int beginIndex, int endIndex) {
        List<String> parts = split();
        try
        {
            return newPath(join(fs, null, parts.subList(beginIndex, endIndex)));
        } catch(IndexOutOfBoundsException e) {
            //let lists.subList do the bound checking for us, 
            //but translate the exception to match the java docs
            throw new IllegalArgumentException(e);
        }
    }
    @Override
    public boolean startsWith(Path other) {
        if(other.getFileSystem() != getFileSystem()) {
            return false;
        }
        EphemeralFsPath otherFs = toefsPath(other);
        if(this.isAbsolute() != other.isAbsolute()) { 
            return false;
        }
        
        List<String> thisParts = this.split();
        List<String> otherParts = otherFs.split();
        
        if(otherParts.size() > thisParts.size()) {
            return false;
        }
        for(int i = 0; i < otherParts.size(); i++) {
            if(!areEqual(otherParts.get(i), thisParts.get(i))) {
                return false;
            }
        }
        return true;
            
    }

    @Override
    public boolean startsWith(String other) {
        return startsWith(newPath(other));
    }
    
    @Override
    public boolean endsWith(Path other) {
        if(other.getFileSystem() != getFileSystem()) {
            return false;
        }
        EphemeralFsPath otherFs = toefsPath(other);
        
        List<String> thisParts = this.split();
        List<String> otherParts = otherFs.split();

        
        if(otherParts.size() > thisParts.size()) {
            return false;
        }
        if(otherFs.isAbsolute()) { 
            if(!isAbsolute()) {
                return false;
            }
            for(int i =0; i < otherParts.size(); i++) {
                if(!areEqual(otherParts.get(i), thisParts.get(i))) {
                    return false;
                }
            }
        } else {
            int offset = thisParts.size() - otherParts.size();
            for(int i =0; i < otherParts.size(); i++) {
                if(!areEqual(otherParts.get(i), thisParts.get(i + offset))) {
                    return false;
                }            
            }
        }
        return true;
    }
    
    @Override
    public boolean endsWith(String other) {
        return endsWith(newPath(other));
    }
    
    @Override
    public EphemeralFsPath normalize() {
        return normalize(false);
    }
    
    public EphemeralFsPath normalize(boolean skipDots) {
        List<String> parts = split();
        List<String> normalizedParts = new ArrayList<String>();
        for(String s : parts) {
            if(!skipDots && s.equals(".")) {
                continue;
            }
            if(s.equals("..")) {
                if(!normalizedParts.isEmpty()) {
                    normalizedParts.remove(normalizedParts.size() - 1);    
                } else {
                    if(!isAbsolute()) {
                        normalizedParts.add(s);
                    }
                }
            }  else {
                normalizedParts.add(s);
            }
        }
        if(normalizedParts.isEmpty() && !isAbsolute()) {
            normalizedParts.add("");
        }
        return newPath(join(fs, (isAbsolute() ? getRootString() : null), normalizedParts));
    }

    
    @Override
    public EphemeralFsPath resolve(Path other) {
        EphemeralFsPath otherFs = toefsPath(other);
        
        if(otherFs.isAbsolute()) {
            return otherFs;
        }
        if(otherFs.isEmpty()) {        
            return this;
        }
        List<String> parts = new ArrayList<String>(split());
        parts.addAll(otherFs.split());
        String newPath = join(fs, (isAbsolute() ? getRootString() : null), parts);
        if(!fs.getSettings().isPosix() && path.startsWith("\\")) {
            newPath = "\\" + newPath;
        }
        return newPath(newPath);
    }
    
    private boolean isEmpty() {
        return path.isEmpty();
    }
    
    @Override
    public EphemeralFsPath resolve(String other) {
        return resolve(newPath(other));
    }
    
    @Override
    public EphemeralFsPath resolveSibling(Path other) {
        //check that other is of the right fs
        toefsPath(other);
        if(getParent() == null) {
            return (EphemeralFsPath) other;
        }
        if(other.isAbsolute()) { 
            return (EphemeralFsPath) other;
        }
        return getParent().resolve(other);
       
    }
    @Override
    public EphemeralFsPath resolveSibling(String other) {
        return resolveSibling(newPath(other));
    }


    @Override
    public EphemeralFsPath relativize(Path other) {
        EphemeralFsPath otherFs = toefsPath(other);
        if(otherFs.equals(this)) { 
            return newPath("");
        }
        if(this.isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException("paths must both be absolute, or not");
        }
        if(!fs.getSettings().isPosix() && this.path.startsWith("\\") != otherFs.path.startsWith("\\")) {
            throw new IllegalArgumentException("'other' is different type of Path");
        }
        
        if(isEmpty()) { 
            return otherFs;
        }
        
        List<String> newParts = new ArrayList<String>();
        int i = 0;
        
        List<String> thisParts = this.split();
        List<String> otherParts = otherFs.split();
        
        while(i < thisParts.size() && i < otherParts.size()) {
            String otherPart = otherParts.get(i);
            String thisPart = thisParts.get(i);
            if(otherPart.equals(thisPart)) {
                i++;
            } else {
                break;
            }
        }
        
        for(int j = i; j <  thisParts.size(); j++) {
            newParts.add("..");
        } 
        for(int j = i; j < otherParts.size(); j++) {
            newParts.add(otherParts.get(j));
        }
        return newPath(join(fs, null, newParts));
    }

    @Override
    public URI toUri() {
        try {
            return new URI(
                    EphemeralFsFileSystemProvider.SCHEME,
                    null,
                    this.toAbsolutePath().toString(),
                    "name="  + fs.getName(),
                    null
                    );
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public EphemeralFsPath toAbsolutePath() {
        if(isAbsolute()) {
            return this;
        }
        return getRoot().resolve(this);
    }
    @Override
    public EphemeralFsPath toRealPath(LinkOption... options) throws IOException {
        boolean noFollow = false;
        for(LinkOption option : options) {
            if(option == LinkOption.NOFOLLOW_LINKS) {
                noFollow = true;
            }
        }
         
        ResolvedPath resolved = ResolvedPath.resolve(this, noFollow);
        if(!resolved.didResolve()) {
            throw new NoSuchFileException(toString());
        }
        
        return resolved.getPath().toAbsolutePath();
    }
    
    @Override
    public File toFile() {
        throw new UnsupportedOperationException(
                "You can only call toFile on Path's associated with the default provider"
                );
    }
    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events,
            Modifier... modifiers) throws IOException {
        
        //no standard modifiers are defined
        if(modifiers != null && modifiers.length != 0) {
            throw new IllegalStateException("unsupported modifiers");
        }

        synchronized(fs.fsLock) {
            ResolvedPath resolvedThis = ResolvedPath.resolve(this, false);
            if(!resolvedThis.hasTarget()) {
                throw new NoSuchFileException(toString());
            }
            if(!resolvedThis.getTarget().isDir()) {
                throw new NotDirectoryException(toString());
            }
            if(!(watcher instanceof EphemeralFsWatchService)) {
                throw new IllegalArgumentException("watcher not created by this fs");
            }
            EphemeralFsWatchService service = (EphemeralFsWatchService) watcher;
            if(service.getFs() != fs) {
                throw new IllegalArgumentException("watch service from different fs");
            }
            EphemeralFsWatchKey answer = 
                    new EphemeralFsWatchKey(service, this, resolvedThis.getTarget(),  fs, events);
            fs.getWatchRegistry().register(resolvedThis.getTarget(), answer);
            return answer;
        }
    }
    
    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events)
            throws IOException {
        return register(watcher, events, new Modifier[0]);
    }
    
    @Override
    public Iterator<Path> iterator() {
        return (Iterator<Path>) (Object) splitPaths().iterator();
    }
    
    public List<EphemeralFsPath> splitPaths() {
        final List<String> parts = split();
        return new AbstractList<EphemeralFsPath>() {
            @Override
            public EphemeralFsPath get(int index) {
                return newPath(parts.get(index));
            }

            @Override
            public int size() {
                return parts.size();
            }
        };
    }
    
    @Override
    public int compareTo(Path other) {
        //javadocs say this throw ClassCastException if
        //the paths are from different providers
        EphemeralFsPath otherPath = (EphemeralFsPath) other;
        return normalizedToString().compareTo(otherPath.normalizedToString());
    }
    
    @Override
    public int hashCode() {
        return normalizedToString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EphemeralFsPath other = (EphemeralFsPath) obj;
        if(fs != other.fs) {
            return false;
        }
        return normalizedToString().equals(other.normalizedToString());
    }
    
    private String normalizedToString() {
        //linux -> case sensitive file system, equals compares case sensitive
        //windows -> case insensitive, equals compares case insentive
        //mac -> case insensitive, equals compares case sensitive
        if(fs.getSettings().isWindows()) {
            return path.toLowerCase(Locale.ENGLISH);
        }
        return path;
    }
    
    @Override
    public String toString() {
        return path;
    }
    
    FileName toFileName() {
        EphemeralFsPath fileName = getFileName();
        if(fs.getSettings().caseSensitive()) {
            return new FileName(fileName.toString(), fileName);
        } 
        return new FileName(getFileName().toString().toLowerCase(Locale.ENGLISH), fileName);
    }
    
    private EphemeralFsPath toefsPath(Path other) {
        if(other == null) {
            throw new NullPointerException();
        }
        try
        {
            return (EphemeralFsPath) other;
        } catch(ClassCastException e) {
            throw new ProviderMismatchException();
        }
    }
    
    static String join(EphemeralFsFileSystem fs, String rootString, List<String> parts) {
        StringBuilder builder = new StringBuilder();
        if(rootString != null) {
            builder.append(rootString);
        }
        Iterator<String> iter = parts.iterator();
        while(iter.hasNext()) {
            String part = iter.next();
            builder.append(part);
            if(iter.hasNext()) {
                builder.append(fs.getSettings().getSeperator());
            }
        }
        return builder.toString();
    }
    
    /**
     * Split the path into it's parts.  If this is a windows path, 
     * the drive letter will not be returned 
     */
    List<String> split() {
        if(path == null) {
            throw new NullPointerException();
        }
        if(path.isEmpty()) {
            return Collections.singletonList("");
        }
        if(this.cachedParts != null) {
            return this.cachedParts;
        }
        List<String> parts = new ArrayList<String>();
        parts.addAll(Arrays.asList(fs.getSettings().getSeperatorPattern().split(path)));
        Iterator<String> iter = parts.iterator();
        while(iter.hasNext()) { 
            if(iter.next().isEmpty()) {
                iter.remove();
            }
        }
        
        //when parsing c:\ 
        //c: will be in parts
        if(!parts.isEmpty() && 
           isAbsolute() && 
           !fs.getSettings().isPosix() ) {
            parts.remove(0);
        }
        
        this.cachedParts = Collections.unmodifiableList(parts);
        return cachedParts;
    }
    
    static boolean isAbsolute(EphemeralFsFileSystem fs, String path) {
        return fs.getSettings().getAbsolutePathPattern().matcher(path).matches();
    }
    
    boolean areEqual(String path1, String path2) {
        if(fs.getSettings().caseSensitive()) {
            return path1.equals(path2);
        } else {
            //case insensitive comparison
            //TODO - how does windows handle comparing filenames
            //in different locales?
            return path1.toLowerCase(Locale.ENGLISH).equals(
                   path2.toLowerCase(Locale.ENGLISH));
        }
        
        
    }

    String getRootString() {
        if(!fs.getSettings().isPosix()) {
            return path.substring(0, path.indexOf(":")) + ":" + fs.getSeparator();
        } else {
            return fs.getSettings().getRoot();
        }

    }    

    private EphemeralFsPath newPath(String path) {
        return new EphemeralFsPath(fs, path);
    }

}
