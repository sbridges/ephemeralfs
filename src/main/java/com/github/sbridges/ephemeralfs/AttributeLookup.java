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
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AttributeLookup {

    private final Map<String, AttributeSet> attributeSets;
    
    public AttributeLookup(AttributeSet... attributeSets) {
        Map<String, AttributeSet> attributeSetsMap = new HashMap<>();
        for(AttributeSet attributeSet : attributeSets) {
            if(attributeSetsMap.put(attributeSet.getName(), attributeSet) != null) {
                throw new IllegalStateException("duplicate:" + attributeSet.getName());
            }
        }
        this.attributeSets = Collections.unmodifiableMap(attributeSetsMap);
    }
    
    public List<Attribute> getMultiple(String attributes) {

        if(!attributes.matches("([a-zA-Z]+:)?[a-zA-Z*]+(,[a-zA-Z*]+)*")) {
            throw new IllegalArgumentException("invalid attribute:" + attributes);
        }

        Set<Attribute> answer = new HashSet<>();
        String view;
        String rest;
        if(attributes.contains(":")) {
            int index = attributes.indexOf(':');
            view = attributes.substring(0, index);
            rest = attributes.substring(index + 1, attributes.length());
        } else {
            view = "basic";
            rest = attributes;
        }
        
        if(!attributeSets.containsKey(view)) {
            throw new UnsupportedOperationException("View '" + view + "' not available");
        }
        AttributeSet attributeSet = attributeSets.get(view);
        
        for(String name : rest.split(",")) {
            if(name.isEmpty()) {
                continue;
            }
            
            if(name.equals("*")) {
               answer.addAll(attributeSet.getAll());
            } else {
                if(!attributeSet.contains(name)) {
                    throw new IllegalArgumentException("'" + name + "' not recognized");
                }
                answer.add(attributeSet.get(name));
            } 
        }
        return new ArrayList<>(answer);
     }
            
    
    public Attribute getSingle(String name) {
        return getSingleInternal(name).attribute;
    }
    
    private NamedAttribute getSingleInternal(String name) {
        if(!name.matches("([a-zA-Z]+:)?[a-zA-Z]+")) {
            throw new IllegalArgumentException("invalid attribute:" + name);
        }
        String view;
        String att;
        if(name.contains(":")) {
            int index = name.indexOf(':');
            view = name.substring(0, index);
            att = name.substring(index + 1, name.length());
        } else {
            view = "basic";
            att = name;
        }
        
        if(!attributeSets.containsKey(view)) {
            throw new IllegalArgumentException("View '" + view + "' not recognized");
        }
        AttributeSet attributeSet = attributeSets.get(view);
        if(!attributeSet.contains(att)) {
            throw new UnsupportedOperationException("'" + name + "' not recognized");
        }
        return new NamedAttribute(
                view + ":" + att,
                attributeSet.get(att));
    }
    
    public Set<String> getViews() {
        return Collections.unmodifiableSet(attributeSets.keySet());
    }

    public boolean supportsFileAttributeView(
            Class<? extends FileAttributeView> type) {
        
        String name = "notFound";
        if(type == BasicFileAttributeView.class) {
            name = "basic";
        } else if(type == DosFileAttributeView.class) {
            name = "dos";
        } else if(type == PosixFileAttributeView.class) {
            name = "posix";
        } else if(type == FileOwnerAttributeView.class) {
            name = "owner";
        }
        
        return  attributeSets.containsKey(name);
    }


    public void write(String attribute, FileAttributesViewBuilder builder,
            Object value) throws IOException {
       NamedAttribute namedAttribute = getSingleInternal(attribute);
       namedAttribute.attribute.write(builder, namedAttribute.name, value);
    }
    
    static class NamedAttribute {
        final String name;
        final Attribute attribute;
        
        public NamedAttribute(String name, Attribute attribute) {
            this.name = name;
            this.attribute = attribute;
        }
    }
}
