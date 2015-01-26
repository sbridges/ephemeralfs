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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class Settings {
    
    private final String root;
    private final String seperator;
    private final boolean caseSensitive;
    private final Pattern seperatorPattern;
    private final Pattern absolutePathPattern;
    private final boolean posix;
    private final long maxOpenFileHandles;
    private final long totalSpace;
    private final long maxPathLength;
    private final boolean recordStackTracesOnOpen;
    
    public Settings(Map<String, ?> props) {
        Map<String, Object> propsCopy = new HashMap<String, Object>(props);
        root = getProp(propsCopy, EphemeralFsFileSystemBuilder.ROOT_PROP);
        seperator = getProp(propsCopy, EphemeralFsFileSystemBuilder.SEPERATOR_PROP);
        caseSensitive = Boolean.parseBoolean(getProp(propsCopy, EphemeralFsFileSystemBuilder.CASE_SENSITIVE_PROP));

        if(seperator.equals("/")) {
            seperatorPattern = Pattern.compile("/");
            absolutePathPattern = Pattern.compile("/.*");
            posix = true;
        } else {
            seperatorPattern = Pattern.compile("\\\\");
            absolutePathPattern = Pattern.compile("[A-Za-z]:\\\\.*");
            posix = false;
        }
        maxOpenFileHandles = getOptionalLong(propsCopy, EphemeralFsFileSystemBuilder.MAX_FILE_HANDLES, Long.MAX_VALUE);
        totalSpace = getOptionalLong(propsCopy, EphemeralFsFileSystemBuilder.TOTAL_SPACE, Long.MAX_VALUE);
        maxPathLength = getOptionalLong(propsCopy, EphemeralFsFileSystemBuilder.MAX_PATH_LENGTH, Long.MAX_VALUE);
        recordStackTracesOnOpen = Boolean.valueOf(getProp(propsCopy, EphemeralFsFileSystemBuilder.RECORD_RESOURCE_CREATION_STACK_TRACES));
        
        if(!propsCopy.isEmpty()) {
            throw new IllegalArgumentException("unrecognized props:" + propsCopy.keySet());
        }
     }
    
    public long getMaxOpenFileHandles() {
        return maxOpenFileHandles;
    }

    public long getTotalSpace() {
        return totalSpace;
    }

    private long getOptionalLong(Map<String, ?> propsCopy, String propertyName, long defaultValue) {
        Object val = propsCopy.remove(propertyName);
        if(val == null) {
            return defaultValue;
        }
        if(!(val instanceof String)) {
            throw new IllegalStateException("invalid prop:" + propertyName + " val:" + val);
        }
        long answer = Long.parseLong(val.toString());
        if(answer < 0) {
            throw new IllegalStateException("invalid prop:" + propertyName + " val:" + val);
        }
        return answer;
    }

    private String getProp(Map<String, Object> propsCopy, String propertyName) {
        Object val = propsCopy.remove(propertyName);
        if(val == null || !(val instanceof String)) {
            throw new IllegalStateException("invalid prop:" + propertyName + " val:" + val);
        }
        return val.toString();
    }

    public String getRoot() {
        return root;
    }
    
    public String getSeperator() {
        return seperator;
    }
    
    public boolean caseSensitive() {
        return caseSensitive;
    }
    
    public Pattern getSeperatorPattern() {
        return seperatorPattern;
    }
    
    public Pattern getAbsolutePathPattern() {
        return absolutePathPattern;
    }
    
    public long getMaxPathLength() {
        return maxPathLength;
    }
    
    public boolean isWindows() {
        return !posix;
    }
    
    public boolean isPosix() {
        return posix;
    }
    
    public boolean isMac() {
        return !isWindows() && !caseSensitive;
    }
    
    public boolean allowSymlink() {
        //windows can support symbolic links
        //if the user is running with special
        //permissions
        return !isWindows();
    }

    public boolean getRecordStackTracesOnOpen() {
        return recordStackTracesOnOpen;
    }

}
