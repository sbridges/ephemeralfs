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
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.concurrent.atomic.AtomicLong;

class FileProperties {

    private static final AtomicLong iNodeCounter = new AtomicLong();
    
    private final Long iNodeNumber =  iNodeCounter.incrementAndGet();
    private FilePermissions filePermissions;
    
    private final EphemeralFsFileTimes fileTimes = new EphemeralFsFileTimes();
    
    private boolean dosIsArchive = false;
    private boolean dosIsHidden = false;
    private boolean dosIsReadOnly = false;
    private boolean dosIsSystem = false;
    private EphemeralFsUserPrincipal owner;
    private EphemeralFsGroupPrincipal group;
    private final EphemeralFsFileSystem fs;
    
    public FileProperties(EphemeralFsFileSystem fs, FilePermissions filePermissions, boolean isFile) {
        this.fs = fs;
        this.filePermissions = filePermissions;
        try {
            this.owner = (EphemeralFsUserPrincipal) 
                    fs.getUserPrincipalLookupService()
                    .lookupPrincipalByName(EphemeralFsUserPrincipalLookupService.DEFAULT_USER);
            this.group = (EphemeralFsGroupPrincipal) 
                    fs.getUserPrincipalLookupService()
                       .lookupPrincipalByGroupName(EphemeralFsUserPrincipalLookupService.DEFAULT_GROUP);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if(isFile && fs.getSettings().isWindows()) {
            dosIsArchive = true;
        }
    }
    
    public FilePermissions getFilePermissions() {
        return filePermissions;
    }

    public void setFilePermissions(FilePermissions filePermissions) {
        this.filePermissions = filePermissions;
    }
    
    public boolean getDosIsArchive() {
        return dosIsArchive;
    }
    
    public void setDosIsArchive(boolean dosIsArchive) {
        this.dosIsArchive = dosIsArchive;
    }
    
    public boolean getDosIsHidden() {
        return dosIsHidden;
    }
    
    public void setDosIsHidden(boolean dosIsHidden) {
        this.dosIsHidden = dosIsHidden;
    }
    
    public boolean getDosIsReadOnly() {
        return dosIsReadOnly;
    }
    
    public void setDosIsReadOnly(boolean dosIsReadOnly) {
        this.dosIsReadOnly = dosIsReadOnly;
    }
    
    public boolean getDosIsSystem() {
        return dosIsSystem;
    }
    
    public void setDosIsSystem(boolean dosIsSystem) {
        this.dosIsSystem = dosIsSystem;
    }
    
    public EphemeralFsUserPrincipal getOwner() {
        return owner;
    }
    
    public void setOwner(UserPrincipal owner) throws IOException {
        if(owner != fs.getUserPrincipalLookupService().lookupPrincipalByName(owner.getName())) {
            throw new IOException("set owner using wrong fs");
        }
        this.owner = (EphemeralFsUserPrincipal) owner;
    }
    
    public GroupPrincipal getGroup() {
        return group;
    }

    public void setGroup(GroupPrincipal group) throws IOException {
        if(group != fs.getUserPrincipalLookupService().lookupPrincipalByGroupName(group.getName())) {
            throw new IOException("set group using wrong fs");
        }
        this.group = (EphemeralFsGroupPrincipal) group;
    }
    
    public static AtomicLong getInodecounter() {
        return iNodeCounter;
    }
    
    public Long getiNodeNumber() {
        return iNodeNumber;
    }
    
    public EphemeralFsFileTimes getFileTimes() {
        return fileTimes;
    }

    public EphemeralFsFileSystem getFs() {
        return fs;
    }
    
    public Long getInodeNumber() {
        return iNodeNumber;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FileProperties[iNodeNumber=");
        builder.append(iNodeNumber);
        builder.append(", filePermissions=");
        builder.append(filePermissions);
        builder.append(", fileTimes=");
        builder.append(fileTimes);
        builder.append(", dosIsArchive=");
        builder.append(dosIsArchive);
        builder.append(", dosIsHidden=");
        builder.append(dosIsHidden);
        builder.append(", dosIsReadOnly=");
        builder.append(dosIsReadOnly);
        builder.append(", dosIsSystem=");
        builder.append(dosIsSystem);
        builder.append(", owner=");
        builder.append(owner);
        builder.append(", group=");
        builder.append(group);
        builder.append(", fs=");
        builder.append(fs);
        builder.append("]");
        return builder.toString();
    }
}
