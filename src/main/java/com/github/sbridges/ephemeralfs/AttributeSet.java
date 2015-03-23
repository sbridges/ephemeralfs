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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AttributeSet {

    private final Map<String, Attribute> attributes;
    private final String name;
   
    public static final AttributeSet BASIC = new AttributeSet("basic", null,
            Attribute.BASIC_CREATION_TIME,
            Attribute.BASIC_FILE_KEY,
            Attribute.BASIC_IS_DIRECTORY,
            Attribute.BASIC_IS_OTHER,
            Attribute.BASIC_IS_REGULAR_FILE,
            Attribute.BASIC_IS_SYMBOLIC_LINK,
            Attribute.BASIC_LAST_ACCESS_TIME,
            Attribute.BASIC_LAST_MODIFIED_TIME,
            Attribute.BASIC_SIZE
            );
    
    public static final AttributeSet DOS = new AttributeSet("dos", BASIC,
           Attribute.DOS_ARCHIVE,
           Attribute.DOS_IS_HIDDEN,
           Attribute.DOS_IS_READ_ONLY,
           Attribute.DOS_SYSTEM
            );
    
    public static final AttributeSet OWNER = new AttributeSet("owner", BASIC,
            Attribute.OWNER_OWNER
             );
    
    
    public static final AttributeSet POSIX = new AttributeSet("posix", BASIC,
           Attribute.POSIX_GROUP,
           Attribute.OWNER_OWNER,
           Attribute.POSIX_PERMISSIONS
            );
    
    public static final AttributeSet UNIX = new AttributeSet("unix", POSIX,
            Attribute.UNIX_CTIME,
            Attribute.UNIX_GID,
            Attribute.UNIX_INO,
            Attribute.UNIX_MODE,
            Attribute.UNIX_NLINK,
            Attribute.UNIX_RDEV,
            Attribute.UNIX_UID
             );
    
    private AttributeSet(String name, AttributeSet parent, Attribute... attributes) {
        this.name = name;
        Map<String, Attribute> attributeMap = new HashMap<>();
        if(parent != null) {
            attributeMap.putAll(parent.attributes);
        }
        for(Attribute attribute : attributes) {
            if(attributeMap.put(attribute.getName(), attribute) != null) {
                throw new IllegalStateException("dupe:" + attribute.getName());
            }
        }
        this.attributes = Collections.unmodifiableMap(attributeMap);
    }

    public String getName() {
        return name;
    }
    
    public boolean contains(String name) {
        return attributes.containsKey(name);
    }
    
    public Attribute get(String name) {
        return attributes.get(name);
    }

    public Collection<Attribute> getAll() {
        return attributes.values();
    }
    

}
