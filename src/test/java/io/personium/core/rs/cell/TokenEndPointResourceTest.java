/**
 * Personium
 * Copyright 2014-2019 Personium Project
 *  - FUJITSU LIMITED
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
package io.personium.core.rs.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.grizzly.utils.Charsets;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmType;
import org.powermock.api.mockito.PowerMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.GrantCode;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.core.PersoniumCoreAuthnException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ctl.Account;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Unit;

/**
 * TokenEndPointResource unit test class.
 */
@Category({ Unit.class })
public class TokenEndPointResourceTest {
    static Logger log = LoggerFactory.getLogger(TokenEndPointResourceTest.class);

    /** Target class of unit test. */
    private TokenEndPointResource tokenEndPointResource;
    private Cell mockCell;
    private CellRsCmp mockCellRsCmp;
    private UriInfo mockUriInfo;
    private String xForwadedFor = "1.2.3.4";


    @BeforeClass
    public static void beforeClass() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.Security.TOKEN_SECRET_KEY, "0123456789abcdef");
        // This test class assumes path based cell Url.
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        PersoniumCoreApplication.loadConfig();
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
        PersoniumCoreApplication.loadPlugins();
    }
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.reload();
    }


    /**
     * Before.
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        String unitUrl = PersoniumUnitConfig.getBaseUrl();
        String cellUrl =  unitUrl + "testcell/";

        this.mockCellRsCmp = mock(CellRsCmp.class);
        doReturn(null).when(this.mockCellRsCmp).getAccountsNotRecordingAuthHistory();
        doReturn(false).when(this.mockCellRsCmp).isRecordingAuthHistory(null, "username");

        this.mockCell = Mockito.spy(Cell.class);
        doReturn(unitUrl).when(this.mockCell).getUnitUrl();
        doReturn(cellUrl).when(this.mockCell).getUrl();
        Map<String, String> o = new HashMap<>();
        o.put(Account.P_IP_ADDRESS_RANGE.getName(), null);
        o.put(Account.P_TYPE.getName(), Account.P_TYPE.getDefaultValue());
        OEntity oe = new OEntity() {
            EdmEntityType edmType = Account.EDM_TYPE_BUILDER.build();

            @Override
            public String getEntitySetName() {
                return Account.EDM_TYPE_NAME;
            }

            @Override
            public OEntityKey getEntityKey() {
                return OEntityKey.create(Account.P_NAME.getName(), "username");
            }

            @Override
            public List<OProperty<?>> getProperties() {
                Account.EDM_TYPE_BUILDER.build().getProperties();
                return null;
            }

            @Override
            public OProperty<?> getProperty(String propName) {
                String value = o.get(propName);
                return OProperties.string(propName, value);
            }

            @Override
            public <T> OProperty<T> getProperty(String propName, Class<T> propClass) {
                return null;
            }

            @Override
            public EdmType getType() {
                return this.edmType;
            }

            @Override
            public <TExtension extends OExtension<OEntity>> TExtension findExtension(Class<TExtension> clazz) {
                return null;
            }

            @Override
            public EdmEntitySet getEntitySet() {
                return EdmEntitySet.newBuilder().setName("Account").build();
            }

            @Override
            public EdmEntityType getEntityType() {
                return Account.EDM_TYPE_BUILDER.build();
            }

            @Override
            public List<OLink> getLinks() {
                return null;
            }

            @Override
            public <T extends OLink> T getLink(String title, Class<T> linkClass) {
                return null;
            }

        };
        OEntityWrapper oew = new OEntityWrapper(null, oe, "5678etag");
        doReturn(oew).when(this.mockCell).getAccount("username");
        doReturn(true).when(this.mockCell).authenticateAccount(oew, "password");


        this.tokenEndPointResource = PowerMockito.spy(new TokenEndPointResource(mockCell, this.mockCellRsCmp));
        this.mockUriInfo = mock(UriInfo.class);
        doReturn(new URI(cellUrl)).when(this.mockUriInfo).getBaseUri();
    }

    /**
     * Test receiveRefresh() using ResidentRefreshToken.
     * @throws Exception Unexpected error.
     */
    @Test
    public void receiveRefresh_ResidentRefreshToken_When_Valid_Succeeds() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String target = "https://personium/testcell/";
        String owner = "false";
        String schema = "https://personium/appcell/";
        String[] scopes = new String[0];

        // --------------------
        // Vaild ResidentRefreshToken
        // --------------------
        ResidentRefreshToken refreshToken = new ResidentRefreshToken(
        		this.mockCell.getUrl(), this.mockCell.getUrl() + "#me", schema, scopes);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, long.class, long.class);
        method.setAccessible(true);
        // Run method
        Response actual = (Response) method.invoke(tokenEndPointResource, target, owner, schema,
        		refreshToken.toTokenString(),
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);
	    // --------------------
	    // Confirm result
	    // --------------------
	    assertEquals(Response.ok().build().getStatus(), actual.getStatus());
    }
    /**
     * Test receiveRefresh() using ResidentRefreshToken.
     * @throws Exception Unexpected error.
     */
    @Test
    public void receiveRefresh_ResidentRefreshToken_When_Expired_Fails() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String target = "https://personium/testcell/";
        String owner = "false";
        String schema = "https://personium/appcell/";
        String[] scopes = new String[0];

        // --------------------
        // Expired ResidentRefreshToken
        // --------------------
        ResidentRefreshToken refreshToken = new ResidentRefreshToken(
                new Date().getTime() - 2 * AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
                1000,
                this.mockCell.getUrl(), this.mockCell.getUrl() + "#me", schema, scopes);

        // --------------------
        // Run method
        // --------------------
        // Access private method
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, long.class, long.class);
        method.setAccessible(true);
        // Run method
        try {
            method.invoke(tokenEndPointResource, target, owner, schema,
                refreshToken.toTokenString(),
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);
            fail("Should throw exception");
        } catch (Exception e) {
            PersoniumCoreAuthnException pcae = (PersoniumCoreAuthnException)e.getCause();
            // --------------------
            // Confirm result
            // --------------------
            assertEquals(PersoniumCoreAuthnException.TOKEN_EXPIRED.getCode(), pcae.getCode());
        }
    }

    /**
     * Test for receiveRefresh() using VisitorRefreshToken.
     * @throws Exception Unexpected error.
     */
    @Test
    public void receiveRefresh_VisitorRefreshToken_When_Valid_Succeeds() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String target = "https://personium/testcell/";
        String owner = "false";
        String schema = "https://personium/appcell/";
        String[] scopes = new String[0];

        // --------------------
        // Valid VisitorRefreshToken
        // --------------------
        List<Role> roleList = new ArrayList<Role>();
        VisitorRefreshToken refreshToken = new VisitorRefreshToken(UUID.randomUUID().toString(),
                new Date().getTime(),
                this.mockCell.getUrl(),
                this.mockCell.getUrl() + "#me",
                this.mockCell.getUrl(),
                roleList,
                schema, scopes);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, long.class, long.class);
        method.setAccessible(true);
        // Run method
        Response actual = (Response) method.invoke(tokenEndPointResource, target, owner, schema,
                refreshToken.toTokenString(),
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);
	    // --------------------
	    // Confirm result
	    // --------------------
	    assertEquals(Response.ok().build().getStatus(), actual.getStatus());
    }

    /**
     * Test for receiveRefresh() using Expired VisitorRefreshToken.
     * @throws Exception Unexpected error.
     */
    @Test
    public void receiveRefresh_VisitorRefreshToken_When_Expired_Fails() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String target = "https://personium/testcell/";
        String owner = "false";
        String schema = "https://personium/appcell/";
        String[] scopes = new String[0];

        // --------------------
        // Expired VisitorRefreshToken
        // --------------------
        List<Role> roleList = new ArrayList<Role>();
        VisitorRefreshToken refreshToken = new VisitorRefreshToken(UUID.randomUUID().toString(),
                new Date().getTime() - 2 * AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
                1000,
                this.mockCell.getUrl(),
                this.mockCell.getUrl() + "#me",
                this.mockCell.getUrl(),
                roleList,
                schema, scopes);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, long.class, long.class);
        method.setAccessible(true);
        // Run method
        try {
            method.invoke(tokenEndPointResource, target, owner, schema,
                refreshToken.toTokenString(),
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);
            // Should throw Exception
            fail("Should throw exception");
        } catch (Exception e) {
            PersoniumCoreAuthnException pcae = (PersoniumCoreAuthnException)e.getCause();
            // --------------------
            // Confirm result
            // --------------------
            assertEquals(PersoniumCoreAuthnException.TOKEN_EXPIRED.getCode(), pcae.getCode());
        }
    }

    /**
     * test for token() method with grant_type=password params setting.
     * @throws Exception
     */
    @Test
    public void token_password_When_Valid_Succeeds() throws Exception {
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "password");
        formParams.add("username", "username");
        formParams.add("password", "password");
        formParams.add("scope", "root https://personium/appcell/");

        Response res = tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
        JsonObject j = Json.createReader(new ByteArrayInputStream(res.getEntity().toString().getBytes(Charsets.UTF8_CHARSET))).readObject();
        assertEquals(200, res.getStatus());
        assertEquals("root", j.getString("scope"));
    }

    /**
     * test for token() method with invalid client_assertion_type.
     * @throws Exception
     */
    @Test
    public void token_ClientAssertionType_When_Invalid_Fails() throws Exception {
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "password");
        formParams.add("username", "username");
        formParams.add("password", "password");
        formParams.add("client_assertion_type", "invalid_client_assertion");
        formParams.add("scope", "root https://personium/appcell/");

        try {
            tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
            fail("Should throw exception");
        } catch (PersoniumCoreAuthnException e) {
            assertEquals(PersoniumCoreAuthnException.INVALID_CLIENT_ASSERTION_TYPE.getCode(), e.getCode());
        }
    }

    /**
     * test for token() method with valid client_assertion_type and null client_assertion.
     * @throws Exception
     */
    @Test
    public void token_ClientAssertion_When_Null_Fails() throws Exception {
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "password");
        formParams.add("username", "username");
        formParams.add("password", "password");
        formParams.add("client_assertion_type", OAuth2Helper.GrantType.SAML2_BEARER);
        formParams.add("scope", "root https://personium/appcell/");

        try {
            tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
        } catch (PersoniumCoreAuthnException e) {
            assertEquals(PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getCode(), e.getCode());
            return;
        }
        fail("Should throw exception");
    }
    /**
     * test for token() method with null client_assertion_type and valid client_assertion.
     * @throws Exception
     */
    @Test
    public void token_When_ClientAssertionType_IsNull_And_ClientAssertion_Valid_Then_Fails() throws Exception {

        //PowerMockito.doReturn(cellUrl).when(tokenEndPointResource, "getIssuerUrl");
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "password");
        formParams.add("username", "username");
        formParams.add("password", "password");
        formParams.add("client_assertion", "aa");
        formParams.add("scope", "root https://personium/appcell/");

        try {
            tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
            fail("Should throw exception");
        } catch (PersoniumCoreAuthnException e) {
            assertEquals(PersoniumCoreAuthnException.INVALID_CLIENT_ASSERTION_TYPE.getCode(), e.getCode());
        }
    }

    @Test
    public void token_GrantCode_When_Invalid_Fails() throws Exception {
        String clientId = this.mockCell.getUnitUrl() + "appcell/";
        TransCellAccessToken appAuthToken = new TransCellAccessToken(clientId, clientId + "#app", this.mockCell.getUrl(), new ArrayList<Role>(), "", new String[0]);

        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "authorization_code");
        formParams.add("code", "InvalidGrantCode");
        formParams.add("scope", "root");
        formParams.add("client_id", clientId);
        formParams.add("client_secret", appAuthToken.toTokenString());

        try {
            tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
            fail("Should throw exception");
        } catch (PersoniumCoreAuthnException e) {
            e.log(log);
            assertEquals(PersoniumCoreAuthnException.INVALID_GRANT_CODE.getCode(), e.getCode());
        }
    }
    @Test
    public void token_GrantCode_When_Valid_Succeeds() throws Exception {
        String clientId = this.mockCell.getUnitUrl() + "appcell/";

        TransCellAccessToken appAuthToken = new TransCellAccessToken(
            clientId, clientId + "#app", this.mockCell.getUrl(), new ArrayList<Role>(), "", new String[0]);
        GrantCode gc = new GrantCode(new Date().getTime(), 3600, this.mockCell.getUrl(), this.mockCell.getUrl() + "#me",
                null, clientId, new String[] {"root"});

        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "authorization_code");
        formParams.add("code", gc.toTokenString());
        formParams.add("client_id", clientId);
        formParams.add("client_secret", appAuthToken.toTokenString());
        formParams.add("scope", "root");

        Response res = tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
        assertEquals(200, res.getStatus());
    }
    @Test
    public void token_GrantCode_WithConfidentialMark_When_Valid_Succeeds() throws Exception {
        // Prepare App Auth Token
        String clientId = this.mockCell.getUnitUrl() + "appcell/";
        List<Role> roleList = new ArrayList<Role>();
        roleList.add(new Role("confidentialClient", null, null, clientId));
        TransCellAccessToken appAuthToken = new TransCellAccessToken(
                clientId, clientId + "#app", this.mockCell.getUrl(), roleList, "", new String[0]);
        GrantCode gc = new GrantCode(new Date().getTime(), GrantCode.CODE_EXPIRES, this.mockCell.getUrl(), this.mockCell.getUrl() + "#me",
                null, clientId, new String[] {"root"});

        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "authorization_code");
        formParams.add("code", gc.toTokenString());
        formParams.add("client_id", clientId);
        formParams.add("client_secret", appAuthToken.toTokenString());
        formParams.add("scope", "root");

        Response res = tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
        assertEquals(200, res.getStatus());
        JsonObject json = Json.createReader(new ByteArrayInputStream(((String)res.getEntity()).getBytes())).readObject();
        String atStr = json.getString("access_token");
        IAccessToken at = (IAccessToken)AbstractOAuth2Token.parse(atStr, this.mockCell.getUrl(),  this.mockCell.getUnitUrl());
        assertEquals(clientId + "#c", at.getSchema());
    }
}
