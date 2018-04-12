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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.wink.webdav.model.Multistatus;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.format.xml.EdmxFormatWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.BoxRsCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;

/**
 * Performs bar file export processing.
 */
public class BarFileExporter {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(BarFileExporter.class);

    /** Extension of the bar file to be created. */
    private static final String BAR_FILE_EXTENSION = ".bar";

    /** Target box rscmp. */
    private BoxRsCmp boxRsCmp;

    /**
     * Constructor.
     * @param boxRsCmp Target box rscmp.
     */
    public BarFileExporter(BoxRsCmp boxRsCmp) {
        this.boxRsCmp = boxRsCmp;
    }

    /**
     * Export bar file.
     * @return JAX-RS response
     */
    public Response export() {
        // Start export.
        log.info(String.format("Start export. BoxName:%s", boxRsCmp.getBox().getName()));
        Cell cell = boxRsCmp.getCell();
        Path barDirPath = Paths.get(PersoniumUnitConfig.getBarExportTempDir(), cell.getDataBundleName());
        if (!Files.exists(barDirPath)) {
            // Create temp directory.
            try {
                Files.createDirectories(barDirPath);
            } catch (IOException e) {
                throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create bar export temp directory").reason(e);
            }
        }

        String barFileName = boxRsCmp.getBox().getName() + UUID.randomUUID().toString() + BAR_FILE_EXTENSION;
        Path barFilePath = barDirPath.resolve(barFileName);
        try (BarFile barFile = BarFile.newInstance(barFilePath)) {
            // Make the contents of the zip file.
            makeBarFile(barFile);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create bar file").reason(e);
        }
        log.info(String.format("End export. BoxName:%s", boxRsCmp.getBox().getName()));

        // Create response.
        InputStream in;
        try {
            in = new FileInputStream(barFilePath.toFile());
        } catch (FileNotFoundException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read bar file").reason(e);
        }
        StreamingOutput streaming = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try {
                    IOUtils.copy(in, output);
                } finally {
                    IOUtils.closeQuietly(in);
                    Files.delete(barFilePath);
                }
            }
        };
        Long fileSize = barFilePath.toFile().length();
        return Response.ok(streaming)
                .header(HttpHeaders.CONTENT_LENGTH, fileSize)
                .header(HttpHeaders.CONTENT_TYPE, BarFile.CONTENT_TYPE)
                .build();
    }

    /**
     * Make the contents of the zip file.
     * @param barFile bar file
     */
    private void makeBarFile(BarFile barFile) {
        addRootPropsToZip(barFile);
        log.info(String.format("Added rootprops xml."));
        addContents(barFile);
        log.info(String.format("Added contents."));
    }

    /**
     * Get rootprops data and add it to the zip file.
     * @param barFile bar file
     */
    private void addRootPropsToZip(BarFile barFile) {
        Multistatus multistatus = boxRsCmp.getRootProps();
        barFile.writeRootPropsXml(multistatus);
    }

    /**
     * Create contents file and add it to zip file.
     * @param barFile bar file
     */
    private void addContents(BarFile barFile) {
        Map<String, DavCmp> childrenMap = boxRsCmp.getDavCmp().getChildren();
        for (String childName : childrenMap.keySet()) {
            Path path = Paths.get(childName);
            DavCmp child = childrenMap.get(childName);
            // Process all contents recursively.
            addContentsRecurcive(barFile, path, child);
        }
    }

    /**
     * Recursively create and add all contents.
     * @param barFile bar file
     * @param path Relative path under the contents dir
     * @param davCmp Target contents
     */
    private void addContentsRecurcive(BarFile barFile, Path path, DavCmp davCmp) {
        String type = davCmp.getType();
        if (DavCmp.TYPE_COL_WEBDAV.equals(type)
                || DavCmp.TYPE_COL_SVC.equals(type)) {
            // Create directory.
            barFile.createDirectoryInContentsDir(path);
            Map<String, DavCmp> childrenMap = davCmp.getChildren();
            for (String childName : childrenMap.keySet()) {
                Path childPath = path.resolve(childName);
                DavCmp child = childrenMap.get(childName);
                // Process all contents recursively.
                addContentsRecurcive(barFile, childPath, child);
            }
        } else if (DavCmp.TYPE_COL_ODATA.equals(type)) {
            // Create directory.
            barFile.createDirectoryInContentsDir(path);
            EdmDataServices dataEdmDataSearvices = davCmp.getODataProducer().getMetadata();
            StringWriter writer = new StringWriter();
            EdmxFormatWriter.write(dataEdmDataSearvices, writer);
            // Write metadata xml.
            barFile.writeMetadataXml(path, writer.toString());
        } else if (DavCmp.TYPE_DAV_FILE.equals(type)) {
            // Get file data.
            Response response = davCmp.get(null).build();
            StreamingOutput stream = (StreamingOutput) response.getEntity();
            // Create file in zip.
            barFile.createFileInContentsDir(path, stream);
        }
    }
}
