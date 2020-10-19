/**
 * Personium
 * Copyright 2019-2020 Personium Project Authors
 *  - Akio Shimono
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.Responses;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import io.personium.common.auth.token.Role;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Box;
import io.personium.core.model.BoxRsCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.fs.DavCmpFsImplTest.MockDavCmpFsImpl;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.utils.PersoniumUrl;
import io.personium.core.utils.TestUtils;
import io.personium.core.utils.UriUtils;

/**
 * Unit tests for BarFileExporter class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelFactory.ODataCtl.class)
public class BarFileExporterTest {
    public static String CELL_URL;
    public static String BOX_SCHEMA_URL;
    public static final String BOX_NAME = "box";

    public List<ZipEntry> barZipEntryList;
    public Map<String, ZipEntry> barZipEntryMap;
    public Map<String, byte[]> barZipContentMap;
    static Logger log = LoggerFactory.getLogger(BarFileExporterTest.class);

    private BoxRsCmp boxRsCmpMock;

    
    /**
     * Set Personium Unit configuration for the testing.
     *   Unit url = https://unit.example/
     *   Path-based Cell URL = false
     */
    @BeforeClass
    public static void beforeClass() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        CommonUtils.setFQDN("unit.example");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_PORT, "");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_SCHEME, "https");
        CELL_URL = PersoniumUrl.create("personium-localunit:user1:/").toHttp();
        BOX_SCHEMA_URL = PersoniumUrl.create("personium-localunit:app1:/").toHttp();
        PersoniumUnitConfig.set(PersoniumUnitConfig.BAR.BAR_TMP_DIR, "/tmp/");
    }
    /**
     * Reset Personium Unit configuration.
     */
    @AfterClass
    public static void afterClass() throws Exception {
        PersoniumUnitConfig.reload();
    }
    public static Cell mockCell(String cellUrl) {
        return new CellEsImpl() {
            @Override
            public String getUrl() {
                return cellUrl;
            };
        };
    }
    public static BoxRsCmp mockBoxRsComp(Box box) {
        // Test Settings

        // prepare ACL
        Map<Role, List<String>> aclSettings = new HashMap<>();
        List<String> grantList = new ArrayList<>();
        grantList.add("read");
        aclSettings.put(new Role("role1", null, null, box.getCell().getUrl()), grantList);
        Acl acl1 = TestUtils.mockAcl(box, aclSettings);

        // Prepare DavCmp structure
        MockDavCmpFsImpl dcCell = new MockDavCmpFsImpl(box.getCell(), null);
        MockDavCmpFsImpl dcBox = new MockDavCmpFsImpl(box, dcCell, acl1);
        // box/col/file2.json
        // box/file1.json
        MockDavCmpFsImpl col = new MockDavCmpFsImpl("col", dcBox, acl1);
        new MockDavCmpFsImpl("file1.json", col, acl1, DavCmp.TYPE_DAV_FILE);
        new MockDavCmpFsImpl("file2.json", dcBox, null, DavCmp.TYPE_DAV_FILE);

        AccessContext ac = null;
        CellRsCmp cellCmp = new CellRsCmp(dcCell, box.getCell(), ac);
        return new BoxRsCmp(cellCmp, dcBox, ac, box);
    }


    public void readExportedBarFile(byte[] barBytes) {
        barZipEntryList = new ArrayList<>();
        barZipEntryMap = new HashMap<>();
        barZipContentMap = new HashMap<>();
        try (
            ByteArrayInputStream bais = new ByteArrayInputStream(barBytes);
            ZipInputStream zis = new ZipInputStream(bais)
        ){
            ZipEntry ent = null;
            int bufferSize = 1024;
            while((ent = zis.getNextEntry()) != null) {
                String entName = ent.getName();
                log.info(entName);

                barZipEntryList.add(ent);
                barZipEntryMap.put(entName, ent);

                byte data[] = new byte[bufferSize];
                int count = 0;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while((count = zis.read(data, 0, bufferSize)) != -1) {
                    baos.write(data,0,count);
                }

                barZipEntryMap.put(entName, ent);
                barZipContentMap.put(entName, baos.toByteArray());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(ModelFactory.ODataCtl.class);
        CellCtlODataProducer mCellCtlProducer = Mockito.mock(CellCtlODataProducer.class);
        PowerMockito.when(ModelFactory.ODataCtl.cellCtl(any())).thenReturn(mCellCtlProducer);
        EntitiesResponse res = Responses.entities(new ArrayList<>(), EdmEntitySet.newBuilder().build(), 0, "");
        Mockito.when(mCellCtlProducer.getNavProperty(anyString(), any(), anyString(), any())).thenReturn(res);
        Cell cell = mockCell(CELL_URL);
        Box box = TestUtils.mockBox(cell, BOX_NAME, BOX_SCHEMA_URL);
        this.boxRsCmpMock = mockBoxRsComp(box);
    }

    /**
     * bar file composition check.
     */
    @Test
    public void export_BarFile_ShouldInclude_NecessaryZipEntries() {
        log.info("------------");
        log.info("export_BarFile_ShouldInclude_NecessaryZipEntries");
        log.info("------------");
        BarFileExporter exporter = new BarFileExporter(this.boxRsCmpMock);
        Response res = exporter.export();

        // parse the response bar file contents
        StreamingOutput so = (StreamingOutput)res.getEntity();
        byte[] barFileBytes = TestUtils.responseToBytes(so);
        readExportedBarFile(barFileBytes);

        ZipEntry meta = barZipEntryMap.get("00_meta/");
        assertNotNull(meta);
        assertTrue(meta.isDirectory());

        ZipEntry manifest = barZipEntryMap.get("00_meta/00_manifest.json");
        assertNotNull(manifest);
        assertFalse(manifest.isDirectory());

        ZipEntry rootprops = barZipEntryMap.get("00_meta/90_rootprops.xml");
        assertNotNull(rootprops);
        assertFalse(rootprops.isDirectory());

        ZipEntry contents = barZipEntryMap.get("90_contents/");
        assertNotNull(contents);
        assertTrue(contents.isDirectory());

        ZipEntry col = barZipEntryMap.get("90_contents/col/");
        assertNotNull(col);
        assertTrue(col.isDirectory());

        ZipEntry f1 = barZipEntryMap.get("90_contents/col/file1.json");
        assertNotNull(f1);
        assertFalse(f1.isDirectory());

        ZipEntry f2 = barZipEntryMap.get("90_contents/file2.json");
        assertNotNull(f2);
        assertFalse(f2.isDirectory());

    }

    /**
     * check for 90_rootprops.xml in the exported bar file.
     * @throws Exception
     */
    @Test
    public void export_RootpropsXml_ShouldHave_ValidContents() throws Exception {
        log.info("------------");
        log.info("export_RootpropsXml_ShouldHave_ValidContents");
        log.info("------------\n");
        BarFileExporter exporter = new BarFileExporter(this.boxRsCmpMock);
        Response res = exporter.export();
        // parse the response bar file contents
        StreamingOutput so = (StreamingOutput)res.getEntity();
        byte[] barFileBytes = TestUtils.responseToBytes(so);
        readExportedBarFile(barFileBytes);

        // Check the bar file contents
        byte[] b = barZipContentMap.get("00_meta/90_rootprops.xml");
        String rootpropsXml = new String(b);
        log.info("00_meta/90_rootprops.xml\n----\n" + rootpropsXml + "\n----");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse(new InputSource(new StringReader(rootpropsXml)));
        XPath xpath = XPathFactory.newInstance().newXPath();

        // ACL base Url should be the base url of Role Class Url
        String base = xpath.evaluate("//acl[position()=1]/@base", doc);
        log.info("//acl[position()=1]/@base = " + base);
        assertEquals(BOX_SCHEMA_URL + "__role/__/", base);

        // href url should use personium-localbox: scheme
        String href = xpath.evaluate("//href[position()=1]/text()", doc);
        log.info("//href[position()=1]/text() = " + href);
        assertEquals("personium-localbox:/", href);

        // should not include inherited ace (count of inherited tag should be 0)
        String countInherited = xpath.evaluate("count(//inherited)", doc);
        log.info("count(//inherited) = " + countInherited);
        assertEquals("0", countInherited);
    }

    /**
     * check for 00_manifest.json in the exported bar file.
     * @throws Exception
     */
    @Test
    public void export_ManifestJson_ShouldHave_ValidContents() throws Exception {
        log.info("----");
        log.info("export_ManifestJson_ShouldHave_ValidContents");
        log.info("----\n");
        BarFileExporter exporter = new BarFileExporter(this.boxRsCmpMock);
        Response res = exporter.export();
        // parse the response bar file contents
        StreamingOutput so = (StreamingOutput)res.getEntity();
        byte[] barFileBytes = TestUtils.responseToBytes(so);
        readExportedBarFile(barFileBytes);

        // Check the bar file contents
        byte[] b = barZipContentMap.get("00_meta/00_manifest.json");
        String manifestJson = new String(b);
        log.info("00_meta/00_manifest.json\n----\n" + manifestJson + "\n----");

        JsonObject json = Json.createReader(new StringReader(manifestJson)).readObject();

        // Box schema URL should be in "shema" key.
        assertEquals(BOX_SCHEMA_URL, json.getString("schema"));

        // bar version should be "2".
        assertEquals("2", json.getString("bar_version"));
    }
}
