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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

/**
 * Junit runner that runs tests with multiple file systems.<p>
 * 
 * Classes that wish to be run with this runner must have a single 
 * Path field names root.<p> 
 * 
 * @see RunWithTypes
 */
public class MultiFsRunner extends ParentRunner<Runner> {

    private final List<Runner> children;
    
    public MultiFsRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        children = new ArrayList<>();
        
        EnumSet<FsType> validTypes = EnumSet.allOf(FsType.class);
    	if(testClass.getAnnotation(RunWithTypes.class) != null ) {
    		validTypes.clear();
    		List<FsType> allowedTypes = Arrays.asList(testClass.getAnnotation(RunWithTypes.class).value());
    		validTypes.addAll(allowedTypes);
    		if(validTypes.contains(WithFsTestRunner.getNativeType())) {
    			validTypes.add(FsType.SYSTEM);
    		}
        }
    	if(testClass.getAnnotation(RunUnlessType.class) != null ) {
    		
    		List<FsType> dissallowedTypes = Arrays.asList(testClass.getAnnotation(RunUnlessType.class).value());
    		validTypes.removeAll(dissallowedTypes);
    		
    		if(dissallowedTypes.contains(WithFsTestRunner.getNativeType())) {
    			validTypes.remove(FsType.SYSTEM);
    		}
        }
        
        for(FsType type : validTypes) {
      		children.add(new WithFsTestRunner(testClass, type));	
        }
    }

    @Override
    protected List<Runner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(Runner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(Runner child, RunNotifier notifier) {
        child.run(notifier);
    }
    
    


}
