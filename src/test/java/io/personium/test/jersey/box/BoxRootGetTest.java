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

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * GET Box root get tests.
 */
@Category({Integration.class})
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

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String cellUrl = UrlUtils.cellRoot(cellName);
        String boxSchema = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String boxStatus = "ready";
        String boxUrl = UrlUtils.boxRoot(cellName, boxName) + "/";

        JSONObject bodyJson = res.bodyAsJson();
        JSONObject boxJson = (JSONObject) bodyJson.get("box");
        JSONObject cellJson = (JSONObject) bodyJson.get("cell");
        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));

        assertThat(boxJson.get("name"), is(boxName));
        assertThat(boxJson.get("url"), is(boxUrl));
        assertThat(boxJson.get("schema"), is(boxSchema));
        assertNotNull(boxJson.get("installed_at"));
        assertThat(boxJson.get("status"), is(boxStatus));

        assertThat(cellJson.get("name"), is(cellName));
        assertThat(cellJson.get("url"), is(cellUrl));
    }
}
