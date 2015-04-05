EphemeralFs
==========

An in memory implementation of java.nio.FileSystem written in java, suitable for testing.

EphemeralFs tries to mimic the behaviour of java.nio.FileSystem for Windows, OS-X and Linux as closely as possible.

EphemeralFsFileSystemChecker allows asserting that all resources are closed, and that all file contents have been properly fsynced. 

[![Build Status](https://travis-ci.org/sbridges/ephemeralfs.png?branch=master)](https://travis-ci.org/sbridges/ephemeralfs) 

Getting Started
===============

EphemeralFs is available in maven as com.github.sbridges:ephemeralfs:1.0.1.0 :


```xml
	<dependency>
	    <groupId>com.github.sbridges</groupId>
	    <artifactId>ephemeralfs</artifactId>
	    <version>1.0.1.0</version>
	</dependency>
```

Example
=======

##Unix

```java

        FileSystem fs = EphemeralFsFileSystemBuilder
                .unixFs()
                .build();
                
        Path testDir = fs.getPath("/testDir");
        Files.createDirectory(testDir);
        Files.write(testDir.resolve("cafe"), new byte[] {'c', 'a', 'f', 'e'});
```

##Windows

```java

        FileSystem fs = EphemeralFsFileSystemBuilder
                .windowsFs()
                .build();
        
        Path testDir = fs.getPath("m:\\windwosTestDir");
        Files.createDirectory(testDir);
        Files.write(testDir.resolve("dir"), new byte[] {'d', 'o', '5'});
```


##Assertions 

```java

        FileSystem fs = EphemeralFsFileSystemBuilder
                .macFs()
                .setRecordStackTracesOnOpen(true)
                .build();
        
        Files.newOutputStream(fs.getPath("/testFile"), StandardOpenOption.CREATE);
        
        //this will throws as the stream above was not closed
        //the AssertionError will contain the stack
        //trace of where the stream was opened
        EphemeralFsFileSystemChecker.assertNoOpenResources(fs);    
```

What is supported
=================

* Basic file/directory operations such as reading, writing ,moving etc
* InputStream, OutputStream
* SeekableByteChannel, FileChannel, AsynchronousFileChannel 
* Symbolic links
* Hard links
* SecureDirectoryStream
* BasicFileAttributes, PosixFileAttributes, DosFileAttributes
* "basic", "dos", "owner", "posix", "unix" file attributes
* WatchService
* Globs
* FileLock
* Maximum file system size
* Checking file system state (all resources closed, all files fsynced)

TODO
====

* Users/Groups
* File Permissions
* Last access time
* File sizes > 2GB
* FileTypeDetector
* AclFileAttributeView


What can't be supported
=======================


<a href="http://docs.oracle.com/javase/7/docs/api/java/nio/file/Path.html#toFile()">Path#toFile</a> is specified 
to only return a File for the default provider.

<a href="http://docs.oracle.com/javase/7/docs/api/java/nio/channels/FileChannel.html#map(java.nio.channels.FileChannel.MapMode,%20long,%20long)">FileChannel#map</a> can't be implemented
since MappedByteBuffer declares all its methods final. 


