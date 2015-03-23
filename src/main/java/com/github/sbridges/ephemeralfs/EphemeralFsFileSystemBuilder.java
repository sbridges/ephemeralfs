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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class to build an EphemeralFSFileSystem 
 */
public final class EphemeralFsFileSystemBuilder {

    private static final String UNIX = "unix";
    private static final String WINDOWS = "windows";
    private static final String MAC = "mac";
    private boolean built;

    private static final AtomicLong ID_PREFIX = new AtomicLong(0); 
    
    static final String SEPERATOR_PROP = "SEPERATOR";
    static final String ROOT_PROP = "ROOT";
    static final String CASE_SENSITIVE_PROP = "CASE_SENSITIVE";
    static final String MAX_FILE_HANDLES = "MAX_FILE_HANDLES";
    static final String TOTAL_SPACE = "TOTAL_SPACE";
    static final String RECORD_RESOURCE_CREATION_STACK_TRACES = "RECORD_RESOURCE_CREATION_STACK_TRACES";
    static final String MAX_PATH_LENGTH = "MAX_PATH_LENGTH";
    
    private String name;
    private final Map<String, String> props = new HashMap<>();
    
    private EphemeralFsFileSystemBuilder(String type) {
        if(type.equals(MAC)) {
            props.put(SEPERATOR_PROP, "/");
            props.put(ROOT_PROP, "/");
            props.put(CASE_SENSITIVE_PROP, "false");
        } else if(type.equals(WINDOWS)) {
            props.put(SEPERATOR_PROP, "\\");
            props.put(ROOT_PROP, "m:\\");
            props.put(CASE_SENSITIVE_PROP, "false");
            props.put(MAX_PATH_LENGTH, "260");
        } else if(type.equals(UNIX)) {
            props.put(SEPERATOR_PROP, "/");
            props.put(ROOT_PROP, "/");
            props.put(CASE_SENSITIVE_PROP, "true");
        }
        this.name = "ephemeralFs_" + type + "_" + ID_PREFIX.incrementAndGet();
        setRecordStackTracesOnOpen(true);
    }
    
    /**
     * Build a file system that best matches the file system of the 
     * underlying operating system. 
     */
    public static EphemeralFsFileSystemBuilder defaultFs() {
        if(OS.isMac()) {
            return new EphemeralFsFileSystemBuilder(MAC);
        } else if(OS.isWindows()) {
            return new EphemeralFsFileSystemBuilder(WINDOWS);
        } else if(OS.isUnix()) {
            return new EphemeralFsFileSystemBuilder(UNIX);
        } else {
            throw new IllegalStateException();
        }
    }
    
    /**
     * Build a windows like file system
     */
    public static EphemeralFsFileSystemBuilder windowsFs() {
        return new EphemeralFsFileSystemBuilder(WINDOWS);
    }

    /**
     * Build a mac like file system
     */
    public static EphemeralFsFileSystemBuilder macFs() {
        return new EphemeralFsFileSystemBuilder(MAC);
    }

    /**
     * Build a unix like file system
     */
    public static EphemeralFsFileSystemBuilder unixFs() {
        return new EphemeralFsFileSystemBuilder(UNIX);
    }

    /**
     * Record stack traces when opening resources such as {@link FileChannel}s and {@link DirectoryStream}s.<P>
     * 
     * The stack traces are available when calling {@link EphemeralFsFileSystemChecker#assertNoOpenResources(FileSystem)}<P>
     * 
     * Defaults to true.
     */
    public EphemeralFsFileSystemBuilder setRecordStackTracesOnOpen(boolean recordStackTracesOnOpen) {
        props.put(RECORD_RESOURCE_CREATION_STACK_TRACES, Boolean.toString(recordStackTracesOnOpen));
        return this;
    }
    
    /**
     *  Set the name of this file system.  The name should
     *  be unique among open ephemeralFs file systems.<P>
     *  
     *  Defaults to a unique name.<P>
     */
    public EphemeralFsFileSystemBuilder setName(String name) {
        if(name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        return this;
    }
    
    /**
     * If set, this limits the maximum number of open file handles.<P>
     * 
     * Defaults to Long.MAX_VALUE
     */
    public EphemeralFsFileSystemBuilder setMaxFileHandles(long maxFileHandles) {
        if(maxFileHandles <= 0) {
            throw new IllegalArgumentException("maxFileHandles must be > 0, not:" + maxFileHandles);
        }
        props.put(MAX_FILE_HANDLES, Long.toString(maxFileHandles));
        return this;
    }
    
    /**
     * If set, sets the total space available for this file system.<P>
     * 
     * Defaults to Long.MAX_VALUE
     */
    public EphemeralFsFileSystemBuilder setTotalSpace(long totalSpace) {
        if(totalSpace <= 0) {
            throw new IllegalArgumentException("long totalSpace must be > 0, not:" + totalSpace);
        }
        props.put(TOTAL_SPACE, Long.toString(totalSpace));
        return this;
    }
    
    
    Map<String, ?> buildEnv() {
        return new HashMap<>(props);
    }
    
    URI buildURI() {
        try {
            return URI.create(EphemeralFsFileSystemProvider.SCHEME + "://?name=" + URLEncoder.encode(name, "UTF-8")
                    );
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Build a file system.  This method can only be called once, subsequent calls
     * will throw an {@link IllegalStateException}
     * 
     * @throws IllegalStateException
     */
    public FileSystem build() throws IllegalStateException {
        if(built) {
            throw new IllegalStateException("already built");
        }
        built = true;
        try {
            return new EphemeralFsFileSystemProvider().newFileSystem(
                    buildURI(), 
                    buildEnv());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
}
