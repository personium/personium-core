/**
 * Personium
 * Copyright 2018-2021 Personium Project Authors
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
package io.personium.test.jersey.box;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * GET Box root get tests.
 */
@Category({Integration.class})
@RunWith(PersoniumIntegTestRunner.class)
public class BoxRootGetTest extends PersoniumTest {

    /**
     * Constructor.
     */
    public BoxRootGetTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Normal test.
     * Get metadata json.
     */
    @Test
    public void normal_get_json() {
        TResponse res = Http.request("box/box-root-get.txt")
                .with("cell", Setup.TEST_CELL1)
                .with("box", Setup.TEST_BOX1)
                .with("token", Setup.MASTER_TOKEN_NAME)
                .with("accept", MediaType.APPLICATION_JSON)
                .returns().debug().statusCode(HttpStatus.SC_OK);

        String unitUrl = UrlUtils.unitRoot();
        boolean pathBasedEnabled = PersoniumUnitConfig.isPathBasedCellUrlEnabled();

        String cellName = Setup.TEST_CELL1;
        String cellUrl = UrlUtils.cellRoot(cellName);

        String boxName = Setup.TEST_BOX1;
        String boxSchema = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String boxStatus = "ready";
        String boxUrl = UrlUtils.boxRoot(cellName, boxName) + "/";

        JSONObject bodyJson = res.bodyAsJson();
        JSONObject unitJson = (JSONObject) bodyJson.get("unit");
        JSONObject cellJson = (JSONObject) bodyJson.get("cell");
        JSONObject boxJson = (JSONObject) bodyJson.get("box");
        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));

        assertThat(unitJson.get("url"), is(unitUrl));
        assertThat(unitJson.get("path_based_cellurl_enabled"), is(pathBasedEnabled));

        assertThat(cellJson.get("name"), is(cellName));
        assertThat(cellJson.get("url"), is(cellUrl));

        assertThat(boxJson.get("name"), is(boxName));
        assertThat(boxJson.get("url"), is(boxUrl));
        assertThat(boxJson.get("schema"), is(boxSchema));
        assertNotNull(boxJson.get("installed_at"));
        assertThat(boxJson.get("status"), is(boxStatus));
    }
}
