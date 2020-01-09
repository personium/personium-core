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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.GrantCode;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.core.PersoniumCoreAuthnException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ctl.Account;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Unit;

/**
 * TokenEndPointResource unit test class.
 */
@Category({Unit.class })
public class TokenEndPointResourceTest {
    static Logger log = LoggerFactory.getLogger(TokenEndPointResourceTest.class);

    /** Target class of unit test. */
    private TokenEndPointResource tokenEndPointResource;
    private Cell mockCell;
    private CellRsCmp mockCellRsCmp;
    private UriInfo mockUriInfo;
    private String xForwadedFor = "1.2.3.4";
    public static final String ACCOUNT_NAME = "username";
    private Role role1;

    @BeforeClass
    public static void beforeClass() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.Security.TOKEN_SECRET_KEY, "0123456789abcdef");
        // This test class can run both in path-based and subdomain-based cell Url.
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        //PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
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
        String cellUrl = "personium-localunit:testcell:/";
        cellUrl = UriUtils.convertSchemeFromLocalUnitToHttp(cellUrl);

        String appCellUrl = "personium-localunit:appcell:/";
        appCellUrl = UriUtils.convertSchemeFromLocalUnitToHttp(appCellUrl);

        // prepare a mock CellRsCmp
        this.mockCellRsCmp = mock(CellRsCmp.class);
        doReturn(null).when(this.mockCellRsCmp).getAccountsNotRecordingAuthHistory();
        doReturn(false).when(this.mockCellRsCmp).isRecordingAuthHistory(null, ACCOUNT_NAME);

        // prepare a mock Cell
        this.mockCell = Mockito.spy(Cell.class);
        doReturn(unitUrl).when(this.mockCell).getUnitUrl();
        doReturn(cellUrl).when(this.mockCell).getUrl();
        Box mockBox = new Box(this.mockCell, "box1", appCellUrl, "dummyboxid", new Date().getTime());

        List<Role> roleList = new ArrayList<>();
        this.role1 = new Role("MyBoardViewer", mockBox.getName(), mockBox.getSchema(), this.mockCell.getUrl());
        roleList.add(this.role1);
        doReturn(roleList).when(this.mockCell).getRoleListForAccount(ACCOUNT_NAME);
        doReturn(roleList).when(this.mockCell).getRoleListHere(Mockito.any());
        doReturn(mockBox).when(this.mockCell).getBoxForSchema(anyString());
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
                return OEntityKey.create(Account.P_NAME.getName(), ACCOUNT_NAME);
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
        doReturn(oew).when(this.mockCell).getAccount(ACCOUNT_NAME);
        doReturn(true).when(this.mockCell).authenticateAccount(oew, "password");

        this.tokenEndPointResource = new TokenEndPointResource(mockCell, this.mockCellRsCmp);
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
        ResidentRefreshToken refreshToken = new ResidentRefreshToken(this.mockCell.getUrl(),
                ACCOUNT_NAME, schema, scopes);

        // --------------------
        // Call target method
        // --------------------
        Response actual = tokenEndPointResource.receiveRefresh(target, owner, schema, refreshToken.toTokenString(),
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);

        // --------------------
        // Confirm result
        // --------------------
        // Status Code should be 200
        assertEquals(Response.ok().build().getStatus(), actual.getStatus());
        JsonObject json = Json.createReader(new ByteArrayInputStream(((String) actual.getEntity()).getBytes()))
        .readObject();
        String atStr = json.getString(OAuth2Helper.Key.ACCESS_TOKEN);
        String rtStr = json.getString(OAuth2Helper.Key.REFRESH_TOKEN);

        AbstractOAuth2Token at = AbstractOAuth2Token.parse(atStr,  this.mockCell.getUrl(),  this.mockCell.getUnitUrl());
        AbstractOAuth2Token rt = AbstractOAuth2Token.parse(rtStr,  this.mockCell.getUrl(),  this.mockCell.getUnitUrl());
        assertTrue(at instanceof TransCellAccessToken);
        assertTrue(rt instanceof ResidentRefreshToken);

        TransCellAccessToken tcat = (TransCellAccessToken)at;
        assertEquals(this.mockCell.getUrl(), tcat.getIssuer());
        assertEquals(schema, tcat.getSchema());
        assertEquals(target, tcat.getTarget());
        assertEquals(this.mockCell.getUrl() + "#" + ACCOUNT_NAME, tcat.getSubject());

        for (Role r : tcat.getRoleList()) {
            log.info(r.toRoleClassURL());
        }
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
                this.mockCell.getUrl(), ACCOUNT_NAME, schema, scopes);

        try {
            // --------------------
            // Call the target method
            // --------------------
            tokenEndPointResource.receiveRefresh(target, owner, schema, refreshToken.toTokenString(),
                    AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                    AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);
            fail("Should throw exception");
        } catch (PersoniumCoreAuthnException pcae ) {
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
        roleList.add(this.role1);

        VisitorRefreshToken refreshToken = new VisitorRefreshToken(UUID.randomUUID().toString(),
                new Date().getTime(),
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
                this.mockCell.getUrl(),
                this.mockCell.getUrl() + "#" + ACCOUNT_NAME,
                this.mockCell.getUrl(),
                roleList,
                schema, scopes);

        // --------------------
        // Call target method
        // --------------------
        Response actual = tokenEndPointResource.receiveRefresh(target, owner, schema, refreshToken.toTokenString(),
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);

        // --------------------
        // Confirm result
        // --------------------
        // Status Code should be 200
        assertEquals(Response.ok().build().getStatus(), actual.getStatus());

        // parse the response
        JsonObject json = Json.createReader(new ByteArrayInputStream(((String) actual.getEntity()).getBytes()))
        .readObject();
        String atStr = json.getString(OAuth2Helper.Key.ACCESS_TOKEN);
        String rtStr = json.getString(OAuth2Helper.Key.REFRESH_TOKEN);

        AbstractOAuth2Token at = AbstractOAuth2Token.parse(atStr,  this.mockCell.getUrl(),  this.mockCell.getUnitUrl());
        AbstractOAuth2Token rt = AbstractOAuth2Token.parse(rtStr,  this.mockCell.getUrl(),  this.mockCell.getUnitUrl());
        assertTrue(at instanceof TransCellAccessToken);
        assertTrue(rt instanceof VisitorRefreshToken);

        TransCellAccessToken tcat = (TransCellAccessToken)at;
        assertEquals(this.mockCell.getUrl(), tcat.getIssuer());
        assertEquals(schema, tcat.getSchema());
        assertEquals(target, tcat.getTarget());
        assertEquals(this.mockCell.getUrl() + "#" + ACCOUNT_NAME, tcat.getSubject());

        for (Role r : tcat.getRoleList()) {
            log.info(r.toRoleClassURL());
        }
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

        try {
            // --------------------
            // Call target method
            // --------------------
            tokenEndPointResource.receiveRefresh(target, owner, schema,
                    refreshToken.toTokenString(),
                    AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                    AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);
            // Should throw Exception
            fail("Should throw exception");
        } catch (PersoniumCoreAuthnException pcae) {
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
        JsonObject j = Json
                .createReader(new ByteArrayInputStream(res.getEntity().toString().getBytes(Charsets.UTF8_CHARSET)))
                .readObject();
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

        // PowerMockito.doReturn(cellUrl).when(tokenEndPointResource, "getIssuerUrl");
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
        String clientId = "personium-localunit:appcell:/";
        clientId = UriUtils.convertSchemeFromLocalUnitToHttp(clientId);

        TransCellAccessToken appAuthToken = new TransCellAccessToken(clientId, clientId + "#app",
                this.mockCell.getUrl(), new ArrayList<Role>(), "", new String[0]);
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
        String clientId = "personium-localunit:appcell:/";
        clientId = UriUtils.convertSchemeFromLocalUnitToHttp(clientId);

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
        String clientId = "personium-localunit:appcell:/";
        clientId = UriUtils.convertSchemeFromLocalUnitToHttp(clientId);

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
        JsonObject json = Json.createReader(new ByteArrayInputStream(((String) res.getEntity()).getBytes()))
                .readObject();
        String atStr = json.getString("access_token");
        IAccessToken at = (IAccessToken) AbstractOAuth2Token.parse(atStr, this.mockCell.getUrl(),
                this.mockCell.getUnitUrl());
        assertEquals(clientId + "#c", at.getSchema());
    }

    @Test
    public void token_TransCellAccessToken_ShouldHave_RolesInRoleClassUrl() throws Exception {
        // Role r1 = new Role("MyBoardViewer", "box1", this.mockCell.getUnitUrl() + "appcell/", this.mockCell.getUnitUrl());

        // Prepare form contents
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", OAuth2Helper.GrantType.PASSWORD);
        formParams.add("username", "username");
        formParams.add("password", "password");
        formParams.add("p_target", this.mockCell.getUnitUrl() + "friend/");
        formParams.add("scope", "root");

        Response res = tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
        assertEquals(200, res.getStatus());
        JsonObject json = Json.createReader(new ByteArrayInputStream(((String) res.getEntity()).getBytes()))
                .readObject();
        String atStr = json.getString("access_token");
        TransCellAccessToken tcat = TransCellAccessToken.parse(atStr);
        for (Role role : tcat.getRoleList()) {
            log.info(role.toRoleClassURL());
        }
        assertEquals(this.role1.toRoleClassURL(), tcat.getRoleList().get(0).toRoleClassURL());
    }
    @Test
    public void token_TransCellAccessToken_VisitorRefreshToken_ShouldHave_SameRoles_And_VisitorAccessToken_ShouldHave_ProperRoles() throws Exception {
        // prepare App Auth Token
        String clientId = "personium-localunit:appcell:/";
        clientId = UriUtils.convertSchemeFromLocalUnitToHttp(clientId);

        List<Role> roleList = new ArrayList<Role>();
        roleList.add(new Role("confidentialClient", null, null, clientId));
        TransCellAccessToken appAuthToken = new TransCellAccessToken(clientId, clientId + "#app",
                this.mockCell.getUrl(), roleList, "", new String[0]);

        // prepare TCAT
        String issuerCellUrl = this.mockCell.getUnitUrl() + "issuerCell/";
        roleList = new ArrayList<Role>();
        Role role2 = new Role("MyBoardEditor", "mb", clientId, issuerCellUrl);
        Role role3 = new Role("MyBoardOwner", "mb", clientId, issuerCellUrl);
        roleList.add(role2);
        roleList.add(role3);
        TransCellAccessToken transCellAccessToken = new TransCellAccessToken(issuerCellUrl, issuerCellUrl + "#me",
                this.mockCell.getUrl(), roleList, "", new String[0]);
        log.info(transCellAccessToken.toSamlString());

        // Prepare form contents
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", OAuth2Helper.GrantType.SAML2_BEARER);
        formParams.add("assertion", transCellAccessToken.toTokenString());
        formParams.add("client_id", clientId);
        formParams.add("client_secret", appAuthToken.toTokenString());
        formParams.add("scope", "root");

        // Should Succeed and issue tokens
        Response res = tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
        assertEquals(200, res.getStatus());
        JsonObject json = Json.createReader(new ByteArrayInputStream(((String) res.getEntity()).getBytes()))
                .readObject();

        // VisitorLocalAccessToken should have role1 in the form of RoleInstance URL
        String accTokenStr = json.getString("access_token");
        VisitorLocalAccessToken vlat = (VisitorLocalAccessToken) AbstractOAuth2Token.parse(accTokenStr, this.mockCell.getUrl(),
                this.mockCell.getUnitUrl());
        for (Role role : vlat.getRoleList()) {
            log.info(role.toRoleInstanceURL());
        }
        assertEquals(this.role1.toRoleInstanceURL(),
                vlat.getRoleList().get(0).toRoleInstanceURL());

        // VisitorRefreshToken should have the same roles (role2,3) in the forms of Role class URL
        String refTokenStr = json.getString("refresh_token");
        VisitorRefreshToken vrt  = (VisitorRefreshToken) AbstractOAuth2Token.parse(refTokenStr, this.mockCell.getUrl(),
                this.mockCell.getUnitUrl());
        for (Role role : vrt.getRoleList()) {
            log.info(role.toRoleClassURL());
        }
        assertEquals(role2.toRoleClassURL(),
                vrt.getRoleList().get(0).toRoleClassURL());
        assertEquals(role3.toRoleClassURL(),
                vrt.getRoleList().get(1).toRoleClassURL());
    }
    @Test
    public void token_VisitorRefreshToken_VisitorRefreshToken_ShouldHave_SameRoles_And_VisitorAccessToken_ShouldHave_ProperRoles() throws Exception {
        // prepare App Auth Token
        String clientId = "personium-localunit:appcell:/";
        clientId = UriUtils.convertSchemeFromLocalUnitToHttp(clientId);
        List<Role> roleList = new ArrayList<Role>();
        roleList.add(new Role("confidentialClient", null, null, clientId));
        TransCellAccessToken appAuthToken = new TransCellAccessToken(clientId, clientId + "#app",
                this.mockCell.getUrl(), roleList, "", new String[0]);

        // prepare VisitorRefreshToken
        String issuerCellUrl = this.mockCell.getUnitUrl() + "issuerCell/";
        roleList = new ArrayList<Role>();
        Role role2 = new Role("MyBoardEditor", "mb", clientId, issuerCellUrl);
        Role role3 = new Role("MyBoardOwner", "mb", clientId, issuerCellUrl);
        roleList.add(role2);
        roleList.add(role3);
        VisitorRefreshToken vrt = new VisitorRefreshToken(
                UUID.randomUUID().toString(), new Date().getTime(),
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
                this.mockCell.getUrl(), issuerCellUrl + "#me",
                issuerCellUrl, roleList, clientId + "#c", new String[]{"root"});

        // Prepare form contents
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", OAuth2Helper.GrantType.REFRESH_TOKEN);
        formParams.add("refresh_token", vrt.toTokenString());
        formParams.add("client_id", clientId);
        formParams.add("client_secret", appAuthToken.toTokenString());
        formParams.add("scope", "root");

        // Should Succeed and issue tokens
        Response res = tokenEndPointResource.token(this.mockUriInfo, null, formParams, xForwadedFor);
        assertEquals(200, res.getStatus());
        JsonObject json = Json.createReader(new ByteArrayInputStream(((String) res.getEntity()).getBytes()))
                .readObject();

        // VisitorLocalAccessToken should have role1 in the form of RoleInstance URL
        String accTokenStr = json.getString("access_token");
        VisitorLocalAccessToken vlat = (VisitorLocalAccessToken) AbstractOAuth2Token.parse(accTokenStr, this.mockCell.getUrl(),
                this.mockCell.getUnitUrl());
        for (Role role : vlat.getRoleList()) {
            log.info(role.toRoleInstanceURL());
        }
        assertEquals(this.role1.toRoleInstanceURL(),
                vlat.getRoleList().get(0).toRoleInstanceURL());

        // VisitorRefreshToken should have the same roles (role2,3) in the forms of Role class URL
        String refTokenStr = json.getString("refresh_token");
        vrt = (VisitorRefreshToken) AbstractOAuth2Token.parse(refTokenStr, this.mockCell.getUrl(),
                this.mockCell.getUnitUrl());
        for (Role role : vrt.getRoleList()) {
            log.info(role.toRoleClassURL());
        }
        assertEquals(role2.toRoleClassURL(),
                vrt.getRoleList().get(0).toRoleClassURL());
        assertEquals(role3.toRoleClassURL(),
                vrt.getRoleList().get(1).toRoleClassURL());
    }

}
