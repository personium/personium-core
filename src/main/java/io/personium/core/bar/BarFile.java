/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
package io.personium.core.bar;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.Charsets;
import org.apache.wink.webdav.model.Multistatus;

import io.personium.core.PersoniumCoreException;

/**
 * Class for Read/Write interface of bar file.
 */
public class BarFile implements Closeable {

    /** Content-Type of bar file. */
    public static final String CONTENT_TYPE = "application/x-personium-bar+zip";

    /** Directory name : bar. */
    private static final String BAR_DIR = "bar";
    /** Directory name : meta. */
    private static final String META_DIR = "00_meta";
    /** File name : manifest. */
    private static final String MANIFEST_JSON = "00_manifest.json";
    /** File name : relations. */
    private static final String RELATIONS_JSON = "10_relations.json";
    /** File name : roles. */
    private static final String ROLES_JSON = "20_roles.json";
    /** File name : extroles. */
    private static final String EXTROLES_JSON = "30_extroles.json";
    /** File name : rules. */
    private static final String RULES_JSON = "50_rules.json";
    /** File name : $links. */
    private static final String LINKS_JSON = "70_$links.json";
    /** File name : rootprops. */
    private static final String ROOTPROPS_XML = "90_rootprops.xml";

    /** Directory name : contents. */
    private static final String CONTENTS_DIR = "90_contents";
    /** File name : metadata. */
    private static final String METADATA_XML = "00_$metadata.xml";

    /** FileSystem class of target zip(bar) file. */
    private final FileSystem fileSystem;
    /** Map that stores file path in zip file. */
    private final Map<String, Path> pathMap;

    /**
     * Constructor.
     * If want to create an instance, use newInstance method.
     * @param filePath Target snapshot file path
     */
    private BarFile(Path filePath) throws IOException {
        fileSystem = toZipFileSystem(filePath);
        pathMap = createItemPathInZip(fileSystem);
        createBarDir();
        createMetaDir();
        createContentsDir();
    }

    /**
     * Create new instance.
     * @param filePath Target bar file path
     * @return BarFile instance.
     * @throws IOException file I/O error
     */
    public static BarFile newInstance(Path filePath) throws IOException {
        return new BarFile(filePath);
    }

    /**
     * Write to rootprops xml.
     * @param multistatus multistatus xml data
     */
    public void writeRootPropsXml(Multistatus multistatus) {
        Path pathInZip = pathMap.get(ROOTPROPS_XML);
        try (BufferedWriter writer = Files.newBufferedWriter(pathInZip, Charsets.UTF_8)) {
            Multistatus.marshal(multistatus, writer);
            writer.flush();
        } catch (WebApplicationException | IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add rootprops xml to bar file").reason(e);
        }
    }

    /**
     * Create directory in contents dir.
     * @param relativePath Target dir path under the contents dir
     */
    public void createDirectoryInContentsDir(Path relativePath) {
        Path pathInZip = pathMap.get(CONTENTS_DIR).resolve(relativePath.toString());
        try {
            Files.createDirectory(pathInZip);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add dir to bar file").reason(e);
        }
    }

    /**
     * Create file in contents dir.
     * @param relativePath Target file path under the contents dir
     * @param dataStream Data stream to write to the file
     */
    public void createFileInContentsDir(Path relativePath, StreamingOutput dataStream) {
        Path pathInZip = pathMap.get(CONTENTS_DIR).resolve(relativePath.toString());
        try (OutputStream os = Files.newOutputStream(pathInZip)) {
            dataStream.write(os);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add file to bar file").reason(e);
        }
    }

    /**
     * Write to metadata xml.
     * @param relativePath Target dir path under the contents dir
     * @param data Data to write
     */
    public void writeMetadataXml(Path relativePath, String data) {
        Path pathInZip = pathMap.get(CONTENTS_DIR).resolve(relativePath.toString()).resolve(METADATA_XML);
        try (BufferedWriter writer = Files.newBufferedWriter(pathInZip, Charsets.UTF_8)) {
            writer.write(data);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add metadata xml to bar file").reason(e);
        }
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
     * BAR_DIR
     * │
     * ├─META_DIR
     * │  ├─MANIFEST_JSON
     * │  ├─RELATIONS_JSON
     * │  ├─ROLES_JSON
     * │  ├─EXTROLES_JSON
     * │  ├─RULES_JSON
     * │  ├─LINKS_JSON
     * │  └─ROOTPROPS_XML
     * │
     * └─CONTENTS_DIR
     *     ├─...
     *     ├─...
     *     └─...
     * -------------------------
     * </pre>
     * @param fs FileSystem class of target zip file
     * @return Map that stores file path in zip file
     */
    private Map<String, Path> createItemPathInZip(FileSystem fs) {
        Map<String, Path> map = new HashMap<>();
        map.put(BAR_DIR, fs.getPath(BAR_DIR));
        map.put(META_DIR, fs.getPath(BAR_DIR, META_DIR));
        map.put(MANIFEST_JSON, fs.getPath(BAR_DIR, META_DIR, MANIFEST_JSON));
        map.put(RELATIONS_JSON, fs.getPath(BAR_DIR, META_DIR, RELATIONS_JSON));
        map.put(ROLES_JSON, fs.getPath(BAR_DIR, META_DIR, ROLES_JSON));
        map.put(EXTROLES_JSON, fs.getPath(BAR_DIR, META_DIR, EXTROLES_JSON));
        map.put(RULES_JSON, fs.getPath(BAR_DIR, META_DIR, RULES_JSON));
        map.put(LINKS_JSON, fs.getPath(BAR_DIR, META_DIR, LINKS_JSON));
        map.put(ROOTPROPS_XML, fs.getPath(BAR_DIR, META_DIR, ROOTPROPS_XML));

        map.put(CONTENTS_DIR, fs.getPath(BAR_DIR, CONTENTS_DIR));
        return map;
    }

    /**
     * Create bar directory.
     */
    private void createBarDir() {
        if (Files.exists(pathMap.get(BAR_DIR))) {
            return;
        }
        try {
            Files.createDirectory(pathMap.get(BAR_DIR));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add bar dir to bar file").reason(e);
        }
    }

    /**
     * Create meta directory.
     */
    private void createMetaDir() {
        if (Files.exists(pathMap.get(META_DIR))) {
            return;
        }
        try {
            Files.createDirectory(pathMap.get(META_DIR));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add meta dir to bar file").reason(e);
        }
    }

    /**
     * Create contents directory.
     */
    private void createContentsDir() {
        if (Files.exists(pathMap.get(CONTENTS_DIR))) {
            return;
        }
        try {
            Files.createDirectory(pathMap.get(CONTENTS_DIR));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add contents dir to bar file").reason(e);
        }
    }
}
