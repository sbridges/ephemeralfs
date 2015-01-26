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

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sbridges.ephemeralfs.junit.MultiFsRunner;

@RunWith(MultiFsRunner.class)
public class AsynchronousFileChannelTest {

    final Random random = new Random();
    
    Path root;
    
    ExecutorService executor = Executors.newFixedThreadPool(1);
    
    public void tearDown() {
        executor.shutdown();
    }
    
    @Test
    public void testFutureAsyncReadWrite() throws Exception {
        Path test = root.resolve("test");
        
        
        try (AsynchronousFileChannel file = AsynchronousFileChannel.open(test,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ
                )) {
            
            byte[] contents = new byte[512];
            random.nextBytes(contents);
            
            Future<Integer> wf = file.write(ByteBuffer.wrap(contents), 0);
            assertEquals((Integer) 512, wf.get());
            
            ByteBuffer read = ByteBuffer.allocate(512);
            
            Future<Integer> rf = file.read(read, 0);
            assertEquals((Integer) 512, rf.get());
            
            assertArrayEquals(contents, read.array());
        }
    }

    @Test
    public void testHandlerAsyncReadWrite() throws Exception {
        Path test = root.resolve("test");
        
        try(AsynchronousFileChannel file = AsynchronousFileChannel.open(test,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ
                )) {
            
            byte[] contents = new byte[512];
            random.nextBytes(contents);
            
            CountdownCompletionHandler<Integer> writeCompletionHandler = 
                    new CountdownCompletionHandler<>();
            
            file.write(ByteBuffer.wrap(contents), 0, writeCompletionHandler, writeCompletionHandler);
            assertEquals((Integer) 512, writeCompletionHandler.getResult());
            
            
            ByteBuffer read = ByteBuffer.allocate(512);
            
            CountdownCompletionHandler<Integer> readCompletionHandler = 
                    new CountdownCompletionHandler<>();
            
            file.read(read, 0, readCompletionHandler, readCompletionHandler);
            assertEquals((Integer) 512, readCompletionHandler.getResult());
            
            assertArrayEquals(contents, read.array());
        }
    }

    @Test
    public void testErrorGoesToHandler() throws Exception {
        Path test = root.resolve("test");
        
        AsynchronousFileChannel file = AsynchronousFileChannel.open(test,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ
                );
        
        file.close();
        
        byte[] contents = new byte[512];
        random.nextBytes(contents);
        
        CountdownCompletionHandler<Integer> writeCompletionHandler = 
                new CountdownCompletionHandler<>();
        
        file.write(ByteBuffer.wrap(contents), 0, writeCompletionHandler, writeCompletionHandler);
        
        assertTrue(writeCompletionHandler.getExc() instanceof ClosedChannelException);
        
    }

    
    class CountdownCompletionHandler<T> implements CompletionHandler<T, CountdownCompletionHandler<T>> {

        private final CountDownLatch latch = new CountDownLatch(1);
        private T result;
        private Throwable exc;
        
        Throwable getExc() throws InterruptedException {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            return exc;
        }
        
        
        T getResult() throws InterruptedException {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertNull(exc);
            return result;
        }
        
        @Override
        public void completed(T result, CountdownCompletionHandler<T> attachment) {
            this.result = result;
            assertEquals(attachment, this);
            latch.countDown();
        }

        @Override
        public void failed(Throwable exc, CountdownCompletionHandler<T> attachment) {
            this.exc = exc;
            latch.countDown();
        }
    }
}
