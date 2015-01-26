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
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * An in memory FileSystem, suitable for testing.<P>
 * 
 * Build with {@link EphemeralFsFileSystemBuilder}, for example,
 * 
 * <pre>
 * <code>
        FileSystem fs = EphemeralFsFileSystemBuilder
                .unixFs()
                .build();
                
        Path testDir = fs.getPath("/testDir");
        Files.createDirectory(testDir);
        Files.write(testDir.resolve("cafe"), new byte[] {'c', 'a', 'f', 'e'});
 * </code>
 * </pre>
 *
 * TODO
 * <ul>
 *    <li>Users/Groups</li>
 *    <li>File Permissions</li>
 *    <li>DosFileAttributes</li>    
 *    <li>Last access time</li>
 *    <li>Allow file sizes > Integer.MAX_VALUE</li>
 *    <li>Some methods in {@link FileStore}</li>
 *    <li>{@link java.nio.file.spi.FileSystemProvider#readAttributes(Path, Class, LinkOption...)}</li>
 *    <li>{@link java.nio.file.spi.FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}</li>
 * </ul> 
 */
public final class EphemeralFsFileSystemProvider extends FileSystemProvider {

    static final String SCHEME = "ephemeralfs";

    private static final Pattern URI_QUERY_PATTERN = Pattern
            .compile("name=([^&=]+)");

    // maps memory file system name of EphemeralFileSystem
    private static final ConcurrentHashMap<String, EphemeralFsFileSystem> fileSystems 
        = new ConcurrentHashMap<String, EphemeralFsFileSystem>();

    public EphemeralFsFileSystemProvider() {

    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    /**
     * Close all ephemeral file systems, releasing any memory the filesystems
     * may consume.<p>
     */
    static void closeAll() {
        for (EphemeralFsFileSystem fs : fileSystems.values()) {
            try {
                fs.close();
            } catch (IOException e) {
                // shouldn't happen, this is an in memory fs
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
            throws IOException {

        if (uri == null || env == null) {
            throw new NullPointerException();
        }
        String name = validateUriAndGetName(uri);
        EphemeralFsFileSystem answer = new EphemeralFsFileSystem(name, new Settings(env), this);
        if (fileSystems.putIfAbsent(name, answer) != null) {
            throw new FileSystemAlreadyExistsException(
                    "A filesystem already exists with the name:" + name);
        }
        return answer;
    }
    
    @Override
    public FileSystem getFileSystem(URI uri) {
        String name = validateUriAndGetName(uri);
        EphemeralFsFileSystem answer = fileSystems.get(name);
        if (answer != null) {
            return answer;
        }
        throw new FileSystemNotFoundException("no filesystem with name:" + name
                + " for uri:" + uri);
    }

    String validateUriAndGetName(URI uri) {
        if (uri.getScheme() == null
                || !uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("expecting scheme of:"
                    + getScheme() + " not:" + uri.getScheme() + " uri:" + uri);
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("fragment must be null");
        }
        String query = uri.getQuery();
        Matcher matcher = URI_QUERY_PATTERN.matcher(query == null ? "" : query);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "expecting a query like, ?name=filesystemName, not:"
                            + query);
        }
        return matcher.group(1);
    }

    @Override
    public Path getPath(URI uri) {
        String name = validateUriAndGetName(uri);
        String uriPath = uri.getPath();
        if (uriPath == null) {
            throw new IllegalArgumentException("path required");
        }

        EphemeralFsFileSystem fs = fileSystems.get(name);
        if (fs == null) {
            fileSystems.putIfAbsent(name, new EphemeralFsFileSystem(
                   name,
                   new Settings(EphemeralFsFileSystemBuilder.defaultFs().buildEnv()), 
                   this));
            fs = fileSystems.get(name);
        }
        
        String path;
        if(fs.getSettings().getSeperator().equals("/")) {
            path = uriPath;
        } else {
            path = uriPath.replaceAll("/", "\\\\");
            if(path.startsWith("\\")) {
                path = fs.getSettings().getRoot() + path;
            }
        }
        return fs.getPath(path).toAbsolutePath();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
            Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        return getFs(path).newByteChannel(toefsPath(path), options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir,
            Filter<? super Path> filter) throws IOException {
        return getFs(dir).newDirectoryStream(toefsPath(dir), filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs)
            throws IOException {
        getFs(dir).createDirectory((EphemeralFsPath) dir, attrs);
    }

    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
            throws IOException {
        getFs(link).createSymbolicLink(
                toefsPath(link), 
                toefsPath(target), 
                attrs);
    }
    
    @Override
    public void createLink(Path link, Path existing) throws IOException {
        getFs(link).createLink(
                toefsPath(link),
                toefsPath(existing)
                );
    }

    
    @Override
    public void delete(Path path) throws IOException {
        getFs(path).delete(toefsPath(path));

    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
            throws IOException {
        if (getFs(source) != getFs(target)) {
            throw new IllegalArgumentException(
                    "source and target have different file systems");
        }
        getFs(source).copy(toefsPath(source), toefsPath(target),
                options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
            throws IOException {
        assertSameFs(source, target);
        getFs(source).move(toefsPath(source), toefsPath(target),
                options);

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if(path.getFileSystem() != path2.getFileSystem()) {
            return false;
        }
        if(path.equals(path2)) {
            return true;
        }
        return getFs(path).isSameFile(toefsPath(path), toefsPath(path2));
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        EphemeralFsFileSystem fs = getFs(path);
        if(!fs.getSettings().isPosix()) {
            DosFileAttributes atts = readAttributes(path, DosFileAttributes.class);
            if(atts.isDirectory()) {
                return false;
            }
            return atts.isHidden();    
        }
        return path.getFileName().toString().startsWith(".");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return getFs(path).getFileStores().iterator().next();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        getFs(path).checkAccess(toefsPath(path), modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path,
            Class<V> type, LinkOption... options) {
        EphemeralFsPath efsPath = toefsPath(path);
        return efsPath.fs.getFileAttributeView(
                new EphemeralFsPathProvider.ConstefsPathProvider(efsPath), 
                type, 
                CloseChecker.ALWAYS_OPEN, 
                options);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path,
            Class<A> type, LinkOption... options) throws IOException {
        return (A) getFileAttributeView(path, viewClassFor(type), options)
                .readAttributes();
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes,
            LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value,
            LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        return getFs(link).readSymbolicLink(toefsPath(link));
    }
    
    @Override
    public FileChannel newFileChannel(Path path,
            Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        return (FileChannel) newByteChannel(path, options, attrs);
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
            Set<? extends OpenOption> options, ExecutorService executor,
            FileAttribute<?>... attrs) throws IOException {
        return getFs(path).newAsynchronousByteChannel(
                toefsPath(path), 
                options,
                executor,
                attrs);
    }

    private EphemeralFsFileSystem getFs(Path p) {
        return toefsPath(p).getFileSystem();
    }

    private EphemeralFsPath toefsPath(Path p) {
        return ((EphemeralFsPath) p);
    }

    void closing(EphemeralFsFileSystem efs) {
        fileSystems.remove(efs.getName(), efs);
    }

    private Class<? extends BasicFileAttributeView> viewClassFor(
            Class<? extends BasicFileAttributes> attributeClass) {
        if (attributeClass == BasicFileAttributes.class) {
            return BasicFileAttributeView.class;
        } else if (attributeClass == PosixFileAttributes.class) {
            return PosixFileAttributeView.class;
        } else if (attributeClass == DosFileAttributes.class) {
            return DosFileAttributeView.class;
        } else {
            throw new IllegalArgumentException("unrecognized view class:"
                    + attributeClass);
        }
    }

    private void assertSameFs(Path p1, Path p2) {
        if (p1.getFileSystem() != p2.getFileSystem()) {
            throw new IllegalArgumentException(
                    "paths have different file systems");
        }

    }
}
