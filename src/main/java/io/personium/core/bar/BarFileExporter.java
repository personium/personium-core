/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.wink.webdav.model.Multistatus;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.format.xml.EdmxFormatWriter;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.bar.jackson.IJSONMappedObjects;
import io.personium.core.bar.jackson.JSONManifest;
import io.personium.core.bar.jackson.JSONRelations;
import io.personium.core.bar.jackson.JSONRoles;
import io.personium.core.bar.jackson.JSONRules;
import io.personium.core.model.Box;
import io.personium.core.model.BoxRsCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.Rule;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.odata.QueryParser;

/**
 * Performs bar file export processing.
 */
public class BarFileExporter {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(BarFileExporter.class);

    /** Extension of the bar file to be created. */
    private static final String BAR_FILE_EXTENSION = ".bar";
    /** BarVersion. */
    private static final String BAR_VERSION = "2";
    /** BoxVersion. */
    private static final String BOX_VERSION = "1";
    /** Limit when retrieving OData. */
    private static final int SEARCH_LIMIT = 1000;

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
            // init
            barFile.initDirCreating();
            // Make the contents of the zip file.
            makeBarFile(barFile);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create bar file").reason(e);
        }
        log.info(String.format("End export. BoxName:%s", boxRsCmp.getBox().getName()));

        // Sync bar file.
        if (PersoniumUnitConfig.getFsyncEnabled()) {
            try (FileOutputStream fos = new FileOutputStream(barFilePath.toFile(), true)) {
                fos.getFD().sync();
            } catch (IOException e) {
                throw PersoniumCoreException.Common.FILE_IO_ERROR.params("sync failed").reason(e);
            }
        }

        // Create response.
        StreamingOutput streaming = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try {
                    Files.copy(barFilePath, output);
                } finally {
                    Files.delete(barFilePath);
                }
            }
        };
        Long fileSize;
        try {
            fileSize = Files.size(barFilePath);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read bar file").reason(e);
        }
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
        addManifestToZip(barFile);
        log.info(String.format("Added manifest json."));
        addRelationsToZip(barFile);
        log.info(String.format("Added relations json."));
        addRolesToZip(barFile);
        log.info(String.format("Added roles json."));
        addRulesToZip(barFile);
        log.info(String.format("Added rules json."));
        addRootPropsToZip(barFile);
        log.info(String.format("Added rootprops xml."));
        addContents(barFile);
        log.info(String.format("Added contents."));
    }

    /**
     * Generate manifest and add it to the zip file.
     * @param barFile bar file
     */
    private void addManifestToZip(BarFile barFile) {
        String defaultPath = boxRsCmp.getBox().getName();
        String schema = boxRsCmp.getBox().getSchema();
        JSONManifest manifest = new JSONManifest(BAR_VERSION, BOX_VERSION, defaultPath, schema);
        ObjectMapper mapper = new ObjectMapper();
        try {
            barFile.writeManifestJson(mapper.writeValueAsString(manifest));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create manifest json").reason(e);
        }
    }

    /**
     * Get relations data and add it to the zip file.
     * @param barFile bar file
     */
    private void addRelationsToZip(BarFile barFile) {
        List<OrderByExpression> orderBy = QueryParser.parseOderByQuery(Relation.P_NAME.getName());
        JSONRelations relations = searchCellCtlObjectsLinkedToBox(Relation.EDM_TYPE_NAME, orderBy, JSONRelations.class);

        if (relations.getRelations().isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            barFile.writeRelationsJson(mapper.writeValueAsString(relations));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create relations json").reason(e);
        }
    }

    /**
     * Get roles data and add it to the zip file.
     * @param barFile bar file
     */
    private void addRolesToZip(BarFile barFile) {
        List<OrderByExpression> orderBy = QueryParser.parseOderByQuery(Common.P_NAME.getName());
        JSONRoles roles = searchCellCtlObjectsLinkedToBox(Role.EDM_TYPE_NAME, orderBy, JSONRoles.class);

        if (roles.getRoles().isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            barFile.writeRolesJson(mapper.writeValueAsString(roles));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create roles json").reason(e);
        }
    }

    /**
     * Get rules data and add it to the zip file.
     * @param barFile bar file
     */
    private void addRulesToZip(BarFile barFile) {
        List<OrderByExpression> orderBy = QueryParser.parseOderByQuery(Rule.P_NAME.getName());
        JSONRules rules = searchCellCtlObjectsLinkedToBox(Rule.EDM_TYPE_NAME, orderBy, JSONRules.class);

        if (rules.getRules().isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            barFile.writeRulesJson(mapper.writeValueAsString(rules));
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create rules json").reason(e);
        }
    }

    /**
     * Search cell control objects linked to Box.
     * @param edmTypeName CellCtl object type to search for
     * @param orderBy order by
     * @param clazz Class to return. clazz needs to implements IJSONMappedObjects interface.
     * @return Instance of clazz that added search results.
     */
    private <T> T searchCellCtlObjectsLinkedToBox(String edmTypeName, List<OrderByExpression> orderBy, Class<T> clazz) {
        // Create return objects.
        IJSONMappedObjects jsonObjects;
        try {
            jsonObjects = (IJSONMappedObjects) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            // Usually, this Exception does not occur.
            // Throw unknown error for the moment.
            throw PersoniumCoreException.Server.UNKNOWN_ERROR.reason(e);
        }

        // Create query.
        InlineCount inlineCount = InlineCount.ALLPAGES;
        int top = SEARCH_LIMIT;
        int skip = 0;
        QueryInfo queryInfo = new QueryInfo(inlineCount, top, skip, null, orderBy, null, null, null, null);

        // Through "Navigation Property", get the objects associated with Box.
        OEntityKey entityKey = OEntityKey.parse("'" + boxRsCmp.getBox().getName() + "'");
        PersoniumODataProducer producer = ModelFactory.ODataCtl.cellCtl(boxRsCmp.getCell());
        while (true) {
            EntitiesResponse response = (EntitiesResponse) producer.getNavProperty(
                    Box.EDM_TYPE_NAME, entityKey, "_" + edmTypeName, queryInfo);
            jsonObjects.addObjects(response.getEntities());
            if (SEARCH_LIMIT > response.getInlineCount()) {
                break;
            }
            // If the data still exists, continue to acquire the data.
            skip += SEARCH_LIMIT;
            queryInfo = new QueryInfo(inlineCount, top, skip, null, orderBy, null, null, null, null);
        }
        return clazz.cast(jsonObjects);
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
            // Get metadata.
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
