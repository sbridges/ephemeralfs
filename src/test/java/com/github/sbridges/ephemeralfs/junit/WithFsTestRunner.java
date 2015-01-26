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

package com.github.sbridges.ephemeralfs.junit;


import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import com.github.sbridges.ephemeralfs.OSTest;

/**
 * Runs a test with a file system of a given type 
 */
class WithFsTestRunner extends BlockJUnit4ClassRunner {

    private final FsType type;
    private Runnable cleanup;
    
    public WithFsTestRunner(Class<?> klass, FsType type) throws InitializationError {
        super(klass);
        this.type = type;
    }
    
    @Override
    protected String getName() {
        return "[" + type + "]";
    }

    @Override
    protected String testName(final FrameworkMethod method) {
        return String.format("%s [%s]", method.getName(),
                type);
    }
    
    
    
    /**
     * Returns a new fixture for running a test. Default implementation executes
     * the test class's no-argument constructor (validation should have ensured
     * one exists).
     */
    @Override
    protected Object createTest() throws Exception {
        Object test = super.createTest();
        
        Field f = test.getClass().getDeclaredField("root");
        f.setAccessible(true);
        final Path root = type.createTestRoot(test.getClass());
        f.set(test, root);
        
        cleanup = new Runnable() {
            @Override
            public void run() {
                try {
                    type.cleanUpRoot(root);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        return test;
    }
    
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
    	
        Description description = describeChild(method);
        if (method.getAnnotation(IgnoreIf.class) != null) {
        	List<FsType> ignoreTypes = 
        			Arrays.asList(method.getAnnotation(IgnoreIf.class).value());
        	
        	if(ignoreTypes.isEmpty()) {
        		throw new IllegalStateException("No ignore types for:" + description);
        	}
        	if(ignoreTypes.contains(FsType.SYSTEM)) {
        		throw new IllegalStateException("can't ignore Native");
        	}
        	
        	if(ignoreTypes.contains(type) ||
        	   type == FsType.SYSTEM && ignoreTypes.contains(getNativeType())) {
        		notifier.fireTestIgnored(description);
        		return;
        	}
        }
        if (method.getAnnotation(IgnoreUnless.class) != null) {
        	List<FsType> acceptTypes = 
        			Arrays.asList(method.getAnnotation(IgnoreUnless.class).value());
        	
        	if(acceptTypes.isEmpty()) {
        		throw new IllegalStateException("No ignore types for:" + description);
        	}
        	if(acceptTypes.contains(FsType.SYSTEM)) {
        		throw new IllegalStateException("can't ignore Native");
        	}
        	
        	if( (type == FsType.SYSTEM && !acceptTypes.contains(getNativeType())) ||
        	     type != FsType.SYSTEM && !acceptTypes.contains(type)) {
        		notifier.fireTestIgnored(description);
        		return;
        	}	
        }
        if (method.getAnnotation(IgnoreIfNoSymlink.class) != null) {
        	List<FsType> ignoreTypes = Arrays.asList(FsType.WINDOWS);
        	
        	if(ignoreTypes.contains(type) ||
        	   type == FsType.SYSTEM && ignoreTypes.contains(getNativeType())) {
        		notifier.fireTestIgnored(description);
        		return;
        	}
        }
    	
        try {
            super.runChild(method, notifier);
        } finally {
            if(cleanup != null) {
            	try
            	{
            		cleanup.run();
            	} catch(Exception e) {
            		EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
            		eachNotifier.addFailure(e);
            	}
                cleanup = null;
            }
        }
    }

	public static FsType getNativeType() {
		if(OSTest.isMac()) {
			return FsType.MAC;
		}
		else if(OSTest.isUnix()) {
			return FsType.UNIX;
		} else if (OSTest.isWindows()) {
			return FsType.WINDOWS;
		} else {
			throw new IllegalStateException();
		}
	}

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        //sort methods by name by default
        List<FrameworkMethod> answer = new ArrayList<FrameworkMethod>(super.computeTestMethods());
        Collections.sort(answer, new Comparator<FrameworkMethod>() {
            @Override
            public int compare(FrameworkMethod o1, FrameworkMethod o2) {
                return o1.getName().compareTo(o2.getName());
            }
            
        });
        return answer;
    }

	
	
}
