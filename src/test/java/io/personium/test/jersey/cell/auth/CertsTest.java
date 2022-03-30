/**
 * Personium
 * Copyright 2019-2022 Personium Project Authors
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
package io.personium.test.jersey.cell.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AuthzUtils;
import io.personium.test.utils.TResponse;

/**
 * Certs endpoint tests.
 */
@Category({Integration.class})
@RunWith(PersoniumIntegTestRunner.class)
public class CertsTest extends PersoniumTest {

    /**
     * Constructor.
     */
    public CertsTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Normal test.
     * Get keys.
     */
    @Test
    public void normal_get() {
        TResponse res = AuthzUtils.certsGet(Setup.TEST_CELL1, HttpStatus.SC_OK);
        JSONArray keys = (JSONArray) res.bodyAsJson().get("keys");
        JSONObject jwk = (JSONObject) keys.get(0);

        assertThat(jwk.get("kty"), is("RSA"));
        assertThat(jwk.get("alg"), is("RS256"));
        assertNotNull(jwk.get("kid"));
        assertNotNull(jwk.get("n"));
        assertThat(jwk.get("e"), is("AQAB"));
    }

    /**
     * Normal test.
     * Get keys.
     * Make sure that the same key is returned each time.
     */
    @Test
    public void normal_get_two_time() {
        // One time.
        TResponse res = AuthzUtils.certsGet(Setup.TEST_CELL1, HttpStatus.SC_OK);
        JSONArray keys1 = (JSONArray) res.bodyAsJson().get("keys");
        JSONObject jwk1 = (JSONObject) keys1.get(0);

        // Two time.
        res = AuthzUtils.certsGet(Setup.TEST_CELL1, HttpStatus.SC_OK);
        JSONArray keys2 = (JSONArray) res.bodyAsJson().get("keys");
        JSONObject jwk2 = (JSONObject) keys2.get(0);

        assertThat(jwk2.get("kty"), is(jwk1.get("kty")));
        assertThat(jwk2.get("alg"), is(jwk1.get("alg")));
        assertThat(jwk2.get("kid"), is(jwk1.get("kid")));
        assertThat(jwk2.get("n"), is(jwk1.get("n")));
        assertThat(jwk2.get("e"), is(jwk1.get("e")));
    }

    /**
     * Normal test.
     * Get keys.
     * Make sure that different keys are returned from different cells.
     */
    @Test
    public void normal_get_other_cell() {
        // Cell1.
        TResponse res = AuthzUtils.certsGet(Setup.TEST_CELL1, HttpStatus.SC_OK);
        JSONArray keys1 = (JSONArray) res.bodyAsJson().get("keys");
        JSONObject jwk1 = (JSONObject) keys1.get(0);

        // Cell2.
        res = AuthzUtils.certsGet(Setup.TEST_CELL2, HttpStatus.SC_OK);
        JSONArray keys2 = (JSONArray) res.bodyAsJson().get("keys");
        JSONObject jwk2 = (JSONObject) keys2.get(0);

        assertThat(jwk2.get("kid"), not(jwk1.get("kid")));
        assertThat(jwk2.get("n"), not(jwk1.get("n")));
    }
}
