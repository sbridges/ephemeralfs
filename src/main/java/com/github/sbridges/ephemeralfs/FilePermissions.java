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

import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

class FilePermissions {

    private final EnumSet<PosixFilePermission> permissions;

    public static FilePermissions createDefaultFile() {
        return new FilePermissions(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.OTHERS_READ
                
                ));
    }

    public static FilePermissions createDefaultDirectory() {
        return new FilePermissions(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
                
                ));
    }
    
    public static FilePermissions createDefaultSymlink() {
        return new FilePermissions(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE,
                PosixFilePermission.OTHERS_EXECUTE
                
                ));
    }

    public FilePermissions(boolean isDirectory, FileAttribute<?>... attributes) {
        this(convertFromNioFileAttributes(isDirectory, attributes));
    }

    private FilePermissions(EnumSet<PosixFilePermission> permissions) {
        this.permissions = EnumSet.copyOf(permissions);
    }

    public void copyFrom(FilePermissions other) {
        this.permissions.clear();
        this.permissions.addAll(other.permissions);
    }

    public boolean canRead() {
        return permissions.contains(PosixFilePermission.OWNER_READ) ||
                permissions.contains(PosixFilePermission.GROUP_READ)||
                permissions.contains(PosixFilePermission.OTHERS_READ);
    }

    public boolean canWrite() {
        return permissions.contains(PosixFilePermission.OWNER_WRITE) ||
                permissions.contains(PosixFilePermission.GROUP_WRITE)||
                permissions.contains(PosixFilePermission.OTHERS_WRITE);

    }

    public boolean canExecute() {
        return permissions.contains(PosixFilePermission.OWNER_EXECUTE) ||
                permissions.contains(PosixFilePermission.GROUP_EXECUTE)||
                permissions.contains(PosixFilePermission.OTHERS_EXECUTE);

    }

    private static EnumSet<PosixFilePermission> convertFromNioFileAttributes(
            boolean isDirectory, FileAttribute<?>... attributes) {
        
        if (attributes != null) {
            for (FileAttribute<?> attr : attributes) {
                if (attr.name().equals("posix:permissions")) {
                    Collection<PosixFilePermission> callerPerms = 
                            (Collection<PosixFilePermission>) attr.value();
                    return EnumSet.copyOf(callerPerms);
                }
            }
        }
        if (isDirectory) {
            return createDefaultDirectory().permissions;
        } else {
            return createDefaultFile().permissions;
        }
    }

    public Set<PosixFilePermission> toPosixFilePermissions() {
        EnumSet<PosixFilePermission> answer = EnumSet
                .noneOf(PosixFilePermission.class);
        answer.addAll(permissions);
        return answer;
    }

    public void setPermissions(Set<PosixFilePermission> perms) {
        permissions.clear();
        permissions.addAll(perms);
        
    }
}
