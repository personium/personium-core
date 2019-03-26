/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
package io.personium.test.jersey.cell.ctl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.TResponse;

/**
 * Read external cell test.
 */
@Category({Unit.class, Integration.class, Regression.class})
public class ExtCellReadTest extends ODataCommon {

    private static String cellName = "testcell1";
    private String extCellUrl = UrlUtils.cellRoot("cellHoge");
    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    private static final String EXT_CELL_TYPE = "CellCtl.ExtCell";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtCellReadTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * test get ExtCell to json.
     */
    @Test
    public final void test_ExtCell_normal_json() {
        String expectedMetadataUri = "http://localhost:9998/testcell1/__ctl/ExtCell('"
                + PersoniumCoreUtils.encodeUrlComp(extCellUrl) + "')";

        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);

            TResponse res = ExtCellUtils.get(token, cellName, extCellUrl, HttpStatus.SC_OK);
            JSONObject body = res.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) body.get("d")).get("results");
            JSONObject metadata = (JSONObject) results.get("__metadata");
            JSONObject roleDeferred = (JSONObject) ((JSONObject) results.get("_Role")).get("__deferred");
            JSONObject relationDeferred = (JSONObject) ((JSONObject) results.get("_Relation")).get("__deferred");

            // The arguments (URL) of the ExtCell function are encoded.
            assertEquals(expectedMetadataUri, metadata.get("uri"));
            assertEquals(expectedMetadataUri + "/_Role", roleDeferred.get("uri"));
            assertEquals(expectedMetadataUri + "/_Relation", relationDeferred.get("uri"));

            ODataCommon.checkResponseBody(res.bodyAsJson(), res.getLocationHeader(), EXT_CELL_TYPE, null);
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * test get ExtCell to xml.
     */
    @Test
    public final void test_ExtCell_normal_xml() {
        String expectedExtCellFunction = "ExtCell('" + PersoniumCoreUtils.encodeUrlComp(extCellUrl) + "')";
        String expectedMetadataUri = "http://localhost:9998/testcell1/__ctl/" + expectedExtCellFunction;
        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);

            TResponse res = ExtCellUtils.get(token, cellName, extCellUrl, "application/xml", HttpStatus.SC_OK);
            Document body = res.bodyAsXml();

            Element entry = (Element) body.getElementsByTagName("entry").item(0);
            Element entryId = (Element) entry.getElementsByTagName("id").item(0);
            String metadataUri = entryId.getTextContent();
            List<String> linkHrefList = new ArrayList<>();
            NodeList links = entry.getElementsByTagName("link");
            for (int i = 0; i < links.getLength(); i++) {
                Element link = (Element) links.item(i);
                linkHrefList.add(link.getAttribute("href"));
            }
            Element content = (Element) entry.getElementsByTagName("content").item(0);
            Element mProperties = (Element) content.getElementsByTagName("m:properties").item(0);
            Element dUrl = (Element) mProperties.getElementsByTagName("d:Url").item(0);

            // The arguments (URL) of the ExtCell function are encoded.
            assertEquals(expectedMetadataUri, metadataUri);
            assertEquals(3, linkHrefList.size());
            for (String linkHref : linkHrefList) {
                assertTrue(linkHref.indexOf(expectedExtCellFunction) != 1);
            }

            // "d:Url" is not subject to encoding
            assertEquals(extCellUrl, dUrl.getTextContent());
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * Urlが数字の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが数字の場合400エラーを返却すること() {
        ExtCellUtils.extCellAccess(HttpMethod.GET, cellName, "123", token, "", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Urlが真偽値の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが真偽値の場合400エラーを返却すること() {
        ExtCellUtils.extCellAccess(HttpMethod.GET, cellName, "false", token, "", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Urlがhttpの場合正常に動作すること.
     */
    @Test
    public final void Urlがhttpの場合正常に動作すること() {
        String extCellHttpUrl = "http://localhost:9998/testcell2/";
        ExtCellUtils.get(token, "testcell1", extCellHttpUrl, HttpStatus.SC_OK);
    }

    /**
     * returns_404_on_GET_with_matching_localunit_url_key_even_if_http_scheme_entity_exists.
     */
    @Test
    public final void returns_404_on_GET_with_matching_localunit_url_key_even_if_http_scheme_entity_exists() {
        String extCellUnitUrl = "personium-localunit:/testcell1/";
        ExtCellUtils.get(token, "testcell2", extCellUnitUrl, HttpStatus.SC_NOT_FOUND);
    }
}
