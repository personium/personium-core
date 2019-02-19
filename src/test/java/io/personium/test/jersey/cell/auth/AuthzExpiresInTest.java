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
package io.personium.test.jersey.cell.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;

/**
 * valid ip address range test for authz.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthzExpiresInTest extends PersoniumTest {

    /**
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * constructor.
     */
    public AuthzExpiresInTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Testing handlePassword.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePassword() throws Exception {
        PersoniumResponse res = requestAuthz4Password(Setup.TEST_CELL1, "account1", "password1", "5");
        Map<String, String> responseMap = parseResponse(res);
        assertThat(responseMap.get(OAuth2Helper.Key.EXPIRES_IN), is("5"));
    }

    /**
     * Testing handlePCookie.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePCookie() throws Exception {
        // TODO ★Need to test "handlePCookie"
    }

    /**
     * request authorization.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @param expiresIn accress token expires in time(s).
     * @return http response
     */
    private PersoniumResponse requestAuthz4Password(String cellName, String userName,
            String password, String expiresIn) throws PersoniumException {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String body = "response_type=token&client_id=" + clientId
                + "&redirect_uri=" + clientId + "__/redirect.html"
                + "&username=" + userName
                + "&password=" + password
                + "&state=" + ImplicitFlowTest.DEFAULT_STATE
                + "&expires_in=" + expiresIn;

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        return rest.post(UrlUtils.cellRoot(cellName) + "__authz", body, requestheaders);
    }

    /**
     * parse response.
     * @param res the personium response
     * @return parse response.
     */
    private Map<String, String> parseResponse(PersoniumResponse res) {
        String location = res.getFirstHeader(HttpHeaders.LOCATION);
        System.out.println(location);
        String[] locations = location.split("#");
        String[] responses = locations[1].split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String response : responses) {
            String[] value = response.split("=");
            map.put(value[0], value[1]);
        }

        return map;
    }
}
