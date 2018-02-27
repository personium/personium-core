/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.core.snapshot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.Charsets;

import io.personium.core.PersoniumCoreException;

/**
 * Class for Read/Write interface of snapshot file.
 */
public class SnapshotFile implements Closeable {

    /** Replaced name of MainBox directory. */
    public static final String MAIN_BOX_DIR_NAME = "__";

    /** File name : manifest. */
    private static final String MANIFEST_JSON = "00_manifest.json";
    /** Directory name : odata. */
    private static final String ODATA_DIR = "10_odata";
    /** File name : cell. */
    private static final String CELL_JSON = "00_cell.json";
    /** File name : odata. */
    private static final String DATA_PJSON = "10_data.pjson";
    /** Directory name : webdav. */
    private static final String WEBDAV_DIR = "20_webdav";

    /** Number of skipped bytes at line count. */
    private static final long SKIP_DATA_NUM = 1024L;

    /** FileSystem class of target zip file. */
    private final FileSystem fileSystem;
    /** Map that stores file path in zip file. */
    private final Map<String, Path> pathMap;

    /**
     * Constructor.
     * If want to create an instance, use newInstance method.
     * @param filePath Target snapshot file path
     */
    private SnapshotFile(Path filePath) throws IOException {
        fileSystem = toZipFileSystem(filePath);
        pathMap = createItemPathInZip(fileSystem);
        createODataDir();
        createWebDAVDir();
    }

    /**
     * Create new instance.
     * @param filePath Target snapshot file path
     * @return SnapshotFile instance.
     * @throws IOException file I/O error
     */
    public static SnapshotFile newInstance(Path filePath) throws IOException {
        return new SnapshotFile(filePath);
    }

    /**
     * Check the structure in the Snapshot file.
     * Does the required directory/file exist.
     */
    public void checkStructure() {
        for (Path path : pathMap.values()) {
            if (!Files.exists(path)) {
                throw PersoniumCoreException.Misc.NOT_FOUND_IN_SNAPSHOT.params(path.toString());
            }
        }
    }

    /**
     * Count and return data pjson line.
     * @return Total line number
     */
    public long countDataPJson() {
        Path pathInZip = pathMap.get(DATA_PJSON);
        try (BufferedReader bufReader = Files.newBufferedReader(pathInZip, Charsets.UTF_8)) {
            LineNumberReader reader = new LineNumberReader(bufReader);
            while (true) {
                long readByte = reader.skip(SKIP_DATA_NUM);
                if (readByte == 0) {
                    break;
                }
            }
            return reader.getLineNumber();
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read data pjson from snapshot file").reason(e);
        }
    }

    /**
     * Count and return webdav files.
     * @return Total line number
     */
    public long countWebDAVFile() {
        return countWebDAVFile(pathMap.get(WEBDAV_DIR));
    }

    /**
     * Returns the number of files under the directory.
     * This method arg is directory only.
     * @param dirPath Directory path
     * @return number of files
     */
    private long countWebDAVFile(Path dirPath) {
        long count = 0L;

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    count += countWebDAVFile(path);
                } else {
                    count++;
                }
            }
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read webdav dir from snapshot file").reason(e);
        }
        return count;
    }

    /**
     * Write to manifest json.
     * @param data Data to write
     */
    public void writeManifestJson(String data) {
        Path pathInZip = pathMap.get(MANIFEST_JSON);
        try (BufferedWriter writer = Files.newBufferedWriter(pathInZip, Charsets.UTF_8)) {
            writer.write(data);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add manifest to snapshot file").reason(e);
        }
    }

    /**
     * Read manifest json.
     * @return Read data
     */
    public String readCellJson() {
        Path pathInZip = pathMap.get(CELL_JSON);
        try {
            return new String(Files.readAllBytes(pathInZip), Charsets.UTF_8);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read cell json from snapshot file").reason(e);
        }
    }

    /**
     * Write to cell json.
     * @param data Data to write
     */
    public void writeCellJson(String data) {
        Path pathInZip = pathMap.get(CELL_JSON);
        try (BufferedWriter writer = Files.newBufferedWriter(pathInZip, Charsets.UTF_8)) {
            writer.write(data);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add cell json to snapshot file").reason(e);
        }
    }

    /**
     * Get and return the reader of data pjson.
     * @return reader of data pjson
     * @throws IOException file I/O error
     */
    public BufferedReader getDataPJsonReader() throws IOException {
        Path pathInZip = pathMap.get(DATA_PJSON);
        return Files.newBufferedReader(pathInZip);
    }

    /**
     * Create data pjson.
     */
    public void createDataPJson() {
        Path pathInZip = pathMap.get(DATA_PJSON);
        try {
            Files.createFile(pathInZip);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create data pjson to snapshot file").reason(e);
        }
    }

    /**
     * Write to data pjson.
     * @param data Data to write
     */
    public void writeDataPJson(String data) {
        Path pathInZip = pathMap.get(DATA_PJSON);
        try (BufferedWriter writer = Files.newBufferedWriter(pathInZip, Charsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            writer.write(data);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add data pjson to snapshot file").reason(e);
        }
    }

    /**
     * Get and return webdav directory path.
     * @return webdav directory path
     */
    public Path getWebDAVDirPath() {
        return pathMap.get(WEBDAV_DIR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    /**
     * Convert the specified Path to Zip's FileSystem.
     * @param zipFilePath Source zip file path
     * @return Zip's FileSystem
     * @throws IOException File I/O error
     */
    private FileSystem toZipFileSystem(Path zipFilePath) throws IOException {
        // Preparation to handle zip with FileSystem.
        URI uri = toZipUri(zipFilePath);
        Map<String, String> env = new HashMap<>();
        // Automatically created if zip file does not exist.
        env.put("create", "true");
        // env.put("encoding", UTF-8); default encoding is utf-8

        return FileSystems.newFileSystem(uri, env);
    }

    /**
     * Convert file Path to "URI format for handling zip".
     * @param zipFilePath zip file path
     * @return URI format for handling zip
     */
    private URI toZipUri(Path zipFilePath) {
        try {
            return new URI("jar", zipFilePath.toAbsolutePath().toUri().toString(), null);
        } catch (URISyntaxException e) {
            // Normally, exception does not occur.
            // Because it is impossible to specify the cause, it is assumed to be UnknownError.
            throw PersoniumCoreException.Server.UNKNOWN_ERROR.reason(e);
        }
    }

    /**
     * Create map that stores file path in zip file.
     * <p>
     * <pre>
     * -------------------------
     * ├─MANIFEST_JSON
     * │
     * ├─ODATA_DIR
     * │  ├─CELL_JSON
     * │  └─DATA_PJSON
     * │
     * └─WEBDAV_DIR
     * -------------------------
     * </pre>
     * @param fs FileSystem class of target zip file
     * @return Map that stores file path in zip file
     */
    private Map<String, Path> createItemPathInZip(FileSystem fs) {
        Map<String, Path> map = new HashMap<>();
        map.put(MANIFEST_JSON, fs.getPath(MANIFEST_JSON));
        map.put(ODATA_DIR, fs.getPath(ODATA_DIR));
        map.put(CELL_JSON, fs.getPath(ODATA_DIR, CELL_JSON));
        map.put(DATA_PJSON, fs.getPath(ODATA_DIR, DATA_PJSON));
        map.put(WEBDAV_DIR, fs.getPath(WEBDAV_DIR));
        return map;
    }

    /**
     * Create OData directory.
     */
    private void createODataDir() {
        if (Files.exists(pathMap.get(ODATA_DIR))) {
            return;
        }
        try {
            Files.createDirectory(pathMap.get(ODATA_DIR));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add OData dir to snapshot file").reason(e);
        }
    }

    /**
     * Create WebDAV directory.
     */
    private void createWebDAVDir() {
        if (Files.exists(pathMap.get(WEBDAV_DIR))) {
            return;
        }
        try {
            Files.createDirectory(pathMap.get(WEBDAV_DIR));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add WebDAV dir to snapshot file").reason(e);
        }
    }
}
