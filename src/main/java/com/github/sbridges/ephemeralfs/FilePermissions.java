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

    private final EnumSet<Permissions> permissions;

    public static FilePermissions createDefaultFile() {
        return new FilePermissions(EnumSet.of(Permissions.READ,
                Permissions.WRITE));
    }

    public static FilePermissions createDefaultDirectory() {
        return new FilePermissions(EnumSet.of(Permissions.READ,
                Permissions.WRITE, Permissions.EXECUTE));
    }

    public FilePermissions(boolean isDirectory, FileAttribute<?>... attributes) {
        this(convertFromNioFileAttributes(isDirectory, attributes));
    }

    private FilePermissions(EnumSet<Permissions> permissions) {
        this.permissions = permissions;
    }

    public void copyFrom(FilePermissions other) {
        this.permissions.clear();
        this.permissions.addAll(other.permissions);
    }

    public boolean canRead() {
        return permissions.contains(Permissions.READ);
    }

    public boolean canWrite() {
        return permissions.contains(Permissions.WRITE);
    }

    public boolean canExecute() {
        return permissions.contains(Permissions.EXECUTE);
    }

    private static EnumSet<Permissions> convertFromNioFileAttributes(
            boolean isDirectory, FileAttribute<?>... attributes) {

        if (attributes != null) {
            for (FileAttribute<?> attr : attributes) {
                if (attr.name().equals("posix:permissions")) {
                    Collection<PosixFilePermission> callerPerms = (Collection<PosixFilePermission>) attr
                            .value();
                    EnumSet<Permissions> answer = EnumSet
                            .noneOf(Permissions.class);

                    if (callerPerms.contains(PosixFilePermission.OWNER_READ)) {
                        answer.add(Permissions.READ);
                    }
                    if (callerPerms.contains(PosixFilePermission.OWNER_WRITE)) {
                        answer.add(Permissions.WRITE);
                    }
                    if (callerPerms.contains(PosixFilePermission.OWNER_EXECUTE)) {
                        answer.add(Permissions.EXECUTE);
                    }
                    return answer;
                }
            }
        }
        if (isDirectory) {
            return EnumSet.of(Permissions.READ, Permissions.WRITE,
                    Permissions.EXECUTE);
        } else {
            return EnumSet.of(Permissions.READ, Permissions.WRITE);
        }
    }

    public Set<PosixFilePermission> toPosixFilePermissions() {
        EnumSet<PosixFilePermission> answer = EnumSet
                .noneOf(PosixFilePermission.class);
        if (permissions.contains(Permissions.READ)) {
            answer.add(PosixFilePermission.OWNER_READ);
            answer.add(PosixFilePermission.GROUP_READ);
            answer.add(PosixFilePermission.OTHERS_READ);
        }
        if (permissions.contains(Permissions.WRITE)) {
            answer.add(PosixFilePermission.OWNER_WRITE);
            answer.add(PosixFilePermission.GROUP_WRITE);
            answer.add(PosixFilePermission.OTHERS_WRITE);
        }
        if (permissions.contains(Permissions.EXECUTE)) {
            answer.add(PosixFilePermission.OWNER_EXECUTE);
            answer.add(PosixFilePermission.GROUP_EXECUTE);
            answer.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return answer;
    }

    public void setPermissions(Set<PosixFilePermission> perms) {
        permissions.clear();
        if(perms.contains(PosixFilePermission.OWNER_READ)) {
            permissions.add(Permissions.READ);
        }
        if(perms.contains(PosixFilePermission.OWNER_WRITE)) {
            permissions.add(Permissions.WRITE);
        }
        if(perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
            permissions.add(Permissions.EXECUTE);
        }
        
    }
}
