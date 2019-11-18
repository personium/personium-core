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

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.common.utils.CommonUtils;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * List external cell test.
 */
@Category({Unit.class, Integration.class, Regression.class})
public class ExtCellListTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testExtCellName = "testcell2";
    private String testExtCellUrl = UrlUtils.cellRoot(testExtCellName);
    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * Constructor.
     */
    public ExtCellListTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * test list ExtCell to json.
     */
    @Test
    public final void test_ExtCell_normal_json() {
        String expectedMetadataUri = "http://localhost:9998/testcell1/__ctl/ExtCell('"
                + CommonUtils.encodeUrlComp(testExtCellUrl) + "')";

        TResponse res = ExtCellUtils.list(token, cellName, "application/json", HttpStatus.SC_OK);
        JSONObject body = res.bodyAsJson();
        JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
        JSONObject result = (JSONObject) results.get(0);
        JSONObject metadata = (JSONObject) result.get("__metadata");
        JSONObject roleDeferred = (JSONObject) ((JSONObject) result.get("_Role")).get("__deferred");
        JSONObject relationDeferred = (JSONObject) ((JSONObject) result.get("_Relation")).get("__deferred");

        // The arguments (URL) of the ExtCell function are encoded.
        assertEquals(expectedMetadataUri, metadata.get("uri"));
        assertEquals(expectedMetadataUri + "/_Role", roleDeferred.get("uri"));
        assertEquals(expectedMetadataUri + "/_Relation", relationDeferred.get("uri"));
    }

    /**
     * test list ExtCell to xml.
     */
    @Test
    public final void test_ExtCell_normal_xml() {
        String expectedExtCellFunction = "ExtCell('" + CommonUtils.encodeUrlComp(testExtCellUrl) + "')";
        String expectedMetadataUri = "http://localhost:9998/testcell1/__ctl/" + expectedExtCellFunction;

        TResponse res = ExtCellUtils.list(token, cellName, "application/xml", HttpStatus.SC_OK);
        Document body = res.bodyAsXml();

        Element feed = (Element) body.getElementsByTagName("feed").item(0);
        Element entry = (Element) feed.getElementsByTagName("entry").item(0);
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
        assertEquals(testExtCellUrl, dUrl.getTextContent());
    }

    /**
     * test list ExtCell multiple data.
     */
    @Test
    public final void test_ExtCell_normal_multiple() {
        String extCellUrl1 = UrlUtils.cellRoot("cellHoge1");
        String extCellUrl2 = UrlUtils.cellRoot("cellHoge2");

        try {
            ExtCellUtils.create(token, cellName, extCellUrl1, HttpStatus.SC_CREATED);
            ExtCellUtils.create(token, cellName, extCellUrl2, HttpStatus.SC_CREATED);

            TResponse res = ExtCellUtils.list(token, cellName, "application/json", HttpStatus.SC_OK);
            JSONObject body = res.bodyAsJson();
            JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
            assertEquals(3, results.size());
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl1, -1);
            ExtCellUtils.delete(token, cellName, extCellUrl2, -1);
        }
    }
}
