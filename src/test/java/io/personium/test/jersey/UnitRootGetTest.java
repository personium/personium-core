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
package io.personium.test.jersey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * GET Unit root url tests.
 */
@Category({Integration.class})
@RunWith(PersoniumIntegTestRunner.class)
public class UnitRootGetTest extends PersoniumTest {

    /**
     * Constructor.
     */
    public UnitRootGetTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Normal test.
     * Get metadata json.
     */
    @Test
    public void normal_get_json() {
        TResponse res = Http.request("unit/unit-root-get.txt")
                .with("accept", MediaType.APPLICATION_JSON)
                .returns().debug().statusCode(HttpStatus.SC_OK);

        String unitUrl = UrlUtils.unitRoot();
        boolean pathBasedEnabled = PersoniumUnitConfig.isPathBasedCellUrlEnabled();

        JSONObject bodyJson = res.bodyAsJson();
        JSONObject unitJson = (JSONObject) bodyJson.get("unit");
        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));
        assertThat(unitJson.get("url"), is(unitUrl));
        assertThat(unitJson.get("path_based_cellurl_enabled"), is(pathBasedEnabled));
    }
}
