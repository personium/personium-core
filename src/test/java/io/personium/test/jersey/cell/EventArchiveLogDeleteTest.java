/**
 * personium.io
 * Copyright 2019 FUJITSU LIMITED
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
package io.personium.test.jersey.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.rs.cell.LogResource;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.setup.Setup;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RuleUtils;
import io.personium.test.utils.TResponse;

/**
 * delete event archive log delete test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class EventArchiveLogDeleteTest extends ODataCommon {

    private static final String TEST_CELL = "testeventlogdelete";
    private static final String DEFAULT_LOG_FORMAT = "default.log.%d";

    /**
     * constructor.
     */
    public EventArchiveLogDeleteTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * test delete event log.<br>
     * <br>
     * "Ignore" for the following reasons. If you want to check, please remove this and execute.
     * <ul>
     * <li>Need to prepare log archive in advance, it takes time.</li>
     * <li>It is necessary to create separate data to delete.</li>
     * </ul>
     * @exception Exception exception
     */
    @SuppressWarnings("unchecked")
    @Test
//    @Ignore
    public final void delete_eventLog() throws Exception {
        try {
            // create test cell and log file.
            CellUtils.create(TEST_CELL, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            JSONObject ruleJson = new JSONObject();
            ruleJson.put("Name", "rule1");
            ruleJson.put("_Box.Name", null);
            ruleJson.put("EventExternal", true);
            ruleJson.put("Action", "log");
            RuleUtils.create(TEST_CELL, AbstractCase.MASTER_TOKEN_NAME, ruleJson, HttpStatus.SC_CREATED);
            createEventLog(TEST_CELL);

            // get before log archive.
            List<String> beforeLogArchiveList = getLogArchiveList(TEST_CELL);
            assertFalse("I could not get even one log.", beforeLogArchiveList.size() == 0);

            // Delete eventlog.
            String deleteTerget = beforeLogArchiveList.get(0);
            String[] splitedHref = deleteTerget.split("log\\.");
            String archiveLogName = String.format(DEFAULT_LOG_FORMAT, Long.valueOf(splitedHref[1]));
            PersoniumResponse response = CellUtils.deleteLog(TEST_CELL, LogResource.ARCHIVE_COLLECTION, archiveLogName);
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // Check deleted event log.
            List<String> afterLogArchiveList = getLogArchiveList(TEST_CELL);
            assertEquals(afterLogArchiveList.size(), beforeLogArchiveList.size() - 1);
            assertFalse("Not deleted.", afterLogArchiveList.contains(deleteTerget));

            // invalid : current event log.
            response = CellUtils.deleteLog(TEST_CELL, LogResource.CURRENT_COLLECTION, "default.log");
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());

            // invalid : invalid collection.
            response = CellUtils.deleteLog(TEST_CELL, "invalid", "default.log");
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

            // invalid : event log file not found.
            response = CellUtils.deleteLog(TEST_CELL, LogResource.ARCHIVE_COLLECTION, "notfound");
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        } finally {
            // delete test cell.
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("");
            }
            Setup.cellBulkDeletion(TEST_CELL);
        }
    }

    /**
     * Create an archive log for a cell. <br />
      * @param cellName cell name
     */
    private void createEventLog(String cellName) {
        final int itemCodeNum = 1024;
        final int loopCount = 108000;
        String jsonBase = "{"
                + "\\\"Type\\\":\\\"%1$s\\\",\\\"Object\\\":\\\"%1$s\\\",\\\"Info\\\":\\\"%1$s\\\"}";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < itemCodeNum; i++) {
            buf.append(String.valueOf((i + 1) % 10));
        }
        String jsonBody = String.format(jsonBase, buf.toString());
        for (int i = 0; i < loopCount; i++) {
            CellUtils.event(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, jsonBody);
        }
    }

    /**
     * Get log archive list.
     * @param cellName Cell name
     * @return log archive list
     */
    private List<String> getLogArchiveList(String cellName) {
        TResponse tresponse = ResourceUtils.logCollectionPropfind(cellName, LogResource.ARCHIVE_COLLECTION, "1",
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

        Element root = tresponse.bodyAsXml().getDocumentElement();
        NodeList responseNodeList = root.getElementsByTagName("href");
        assertNotNull(responseNodeList);

        List<String> hrefList = new ArrayList<String>();
        for (int i = 1; i < responseNodeList.getLength(); i++) {
            hrefList.add(responseNodeList.item(i).getFirstChild().getNodeValue());
        }
        return hrefList;
    }
}
