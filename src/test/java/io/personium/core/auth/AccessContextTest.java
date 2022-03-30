/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.auth;

import static io.personium.common.auth.token.AbstractOAuth2Token.MILLISECS_IN_AN_HOUR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Unit;
import io.personium.test.utils.UrlUtils;

/**
 * Unit test class for AccessContext.
 */
@Category({ Unit.class })
public class AccessContextTest {
    static Logger log = LoggerFactory.getLogger(AccessContextTest.class);

    /**
     * Master Token.
     */
    public static final String MASTER_TOKEN = "MasterTokenForThisTest";

    /**
     * baseUrl.
     */
    public static final String BASE_URL = UrlUtils.getBaseUrl();

    /**
     * Owner.
     */
    public static final String OWNER = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // In order for this test to run without any configuration,
        //   set secrte16
        PersoniumUnitConfig.set(PersoniumUnitConfig.Security.TOKEN_SECRET_KEY, "0123456789abcdef");
        //   set master token
        PersoniumUnitConfig.set(PersoniumUnitConfig.MASTER_TOKEN, MASTER_TOKEN);
        PersoniumCoreApplication.loadConfig();
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        PersoniumUnitConfig.reload();
    }

    /**
     * testGetCellのテスト.
     */
    @Test
    @Ignore
    public void testGetCell() {
        fail("Not yet implemented");
    }

    /**
     * testGetTypeのテスト.
     */
    @Test
    @Ignore
    public void testGetType() {
        fail("Not yet implemented");
    }

    /**
     * testGetSubjectのテスト.
     */
    @Test
    @Ignore
    public void testGetSubject() {
        fail("Not yet implemented");
    }

    /**
     * testGetSchemaのテスト.
     */
    @Test
    @Ignore
    public void testGetSchema() {
        fail("Not yet implemented");
    }

    /**
     * testAddRoleのテスト.
     */
    @Test
    @Ignore
    public void testAddRole() {
        fail("Not yet implemented");
    }

    /**
     * testGetRoleListのテスト.
     */
    @Test
    @Ignore
    public void testGetRoleList() {
        fail("Not yet implemented");
    }

    /**
     */
    @Test
    public void create_NoAuthzHeader_ShouldReturn_TypeAnonymous() {
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) any(), anyString())).thenReturn(true);

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(null, null, null, null,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(accessContext.getType(), AccessContext.TYPE_ANONYMOUS);
    }

    @Test
    public void create_Basic_Valid_ShouldReturn_TypeBasic() {
        String auth = "Basic "
                + CommonUtils.encodeBase64Url("username:password".getBytes());
        Cell cell = (Cell) mock(Cell.class);
        List<OProperty<?>> props = new ArrayList<>();
        OEntityWrapper oew = new OEntityWrapper(
            UUID.randomUUID().toString(),
            OEntities.create(
                    EdmEntitySet.newBuilder().build(),
                    OEntityKey.create("k","dum"), props,
                    null
            ),
            null
        );
        when(cell.getAccount(anyString())).thenReturn(oew);
        when(cell.authenticateAccount((OEntityWrapper) any(), anyString())).thenReturn(true);
        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(auth,
                null, null, null, cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_BASIC, accessContext.getType());
    }

    /**
     * testCreateBasicでInvalidになるテスト.
     */
    @Test
    public void create_Basic_INVALID() {
        String auth = "Basic "
                + CommonUtils.encodeBase64Url("user:pass".getBytes());
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) any(), anyString())).thenReturn(false);
        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(auth,
                null, null, null, cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(accessContext.getType(), AccessContext.TYPE_INVALID);
    }

    /**
     * Bearer形式 マスタートークンを指定して、UNIT_MASTERのアクセスコンテキストが取得できること.
     */
    @Test
    public void create_Bearer_MasterToken_ShouldReturn_TypeUnitMaster() {
        String authzHeader = "Bearer " + MASTER_TOKEN;
        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(authzHeader,
                null, null, null, null, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_UNIT_MASTER, accessContext.getType());
    }

    /**
     * Bearer形式 Cell指定がない状態でパースエラーが発生した場合、ACCESS_INVALIDのアクセスコンテキストが取得できること.
     */
    @Test
    public void create_Bearer_CellNull_ShoudReturn_TypeInvalid() {
        // 「dGVzdA==」=>「test」のBase64化した文字列
        String authzHeader = "Bearer dGVzdA==";
        System.out.println(authzHeader);
        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(authzHeader,
                null, null, null, null, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_INVALID, accessContext.getType());
    }

    /**
     * Bearer形式 Cell指定がない状態で「Bearer」のみ指定した場合、ACCESS_INVALIDのアクセスコンテキストが取得できること.
     */
    @Test
    public void create_BearerOnly_ShouldReturn_TypeInvalid() {
        String authzHeader = "Bearer";
        System.out.println(authzHeader);
        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(authzHeader,
                null, null, null, null, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_INVALID, accessContext.getType());
    }

    /**
     * Bearer形式 Cell指定がない状態で「Bearer 」のみ指定した場合、ACCESS_INVALIDのアクセスコンテキストが取得できること.
     */
    @Test
    public void create_BearerSpace_ShouldReturn_TypeInvalid() {
        String authzHeader = "Bearer ";
        System.out.println(authzHeader);
        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(authzHeader,
                null, null, null, null, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_INVALID, accessContext.getType());
    }


    /**
     * AuthorizationHeaderなしでのULUUTのcookie認証によるAccessContext生成の正常系テスト.
     */
    @Test
    public void AuthorizationHeaderなしでのULUUTのcookie認証によるAccessContext生成の正常系テスト() {
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) any(), anyString())).thenReturn(true);
        when(cell.getOwnerNormalized()).thenReturn("cellowner");
        when(cell.getUrl()).thenReturn(UrlUtils.getBaseUrl() + "/cellowner");
        when(cell.getUnitUrl()).thenReturn(UrlUtils.getBaseUrl());

        UriInfo uriInfo =  new TestUriInfo();

        // uluut発行処理
        UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                System.currentTimeMillis(), UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getOwnerNormalized(), UrlUtils.getBaseUrl());

        // p_cookie_peerとして、ランダムなUUIDを設定する
        String dcCookiePeer = UUID.randomUUID().toString();
        // ヘッダに返却するdc-cookie値は、暗号化する
        String encodedCookieValue = uluut.getCookieString(dcCookiePeer,
                AccessContext.getCookieCryptKey(uriInfo.getBaseUri().getHost()));

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(null, uriInfo, dcCookiePeer, encodedCookieValue,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_UNIT_LOCAL, accessContext.getType());
    }

    /**
     * AuthorizationHeaderなしでのLocalTokenのcookie認証によるAccessContext生成の正常系テスト.
     */
    @Test
    public void AuthorizationHeaderなしでのLocalTokenのcookie認証によるAccessContext生成の正常系テスト() {
        UriInfo uriInfo =  new TestUriInfo();

        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) any(), anyString())).thenReturn(true);
        when(cell.getOwnerNormalized()).thenReturn("cellowner");
        when(cell.getUrl()).thenReturn(UrlUtils.getBaseUrl() + "/cellowner");
        when(cell.getUnitUrl()).thenReturn(UrlUtils.getBaseUrl());

        // Token発行処理
        VisitorLocalAccessToken token = new VisitorLocalAccessToken(
                new Date().getTime(),
                VisitorLocalAccessToken.ACCESS_TOKEN_EXPIRES_MILLISECS,
                UrlUtils.getBaseUrl() + "/cellowner",
                cell.getOwnerNormalized(),
                null,
                UrlUtils.getBaseUrl() + "/cellowner",
                new String[] {"scope"});

        // p_cookie_peerとして、ランダムなUUIDを設定する
        String dcCookiePeer = UUID.randomUUID().toString();
        // ヘッダに返却するdc-cookie値は、暗号化する
        String encodedCookieValue = token.getCookieString(dcCookiePeer,
                AccessContext.getCookieCryptKey(uriInfo.getBaseUri().getHost()));

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(null, uriInfo, dcCookiePeer, encodedCookieValue,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_VISITOR, accessContext.getType());
    }

    /**
     * BASIC認証AuthorizationHeaderとcookie認証情報が同時に指定された場合のAccessContext生成の正常系テスト.
     */
    @Test
    public void BASIC認証AuthorizationHeaderとcookie認証情報が同時に指定された場合のAccessContext生成の正常系テスト() {
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) any(), anyString())).thenReturn(true);
        when(cell.getOwnerNormalized()).thenReturn("cellowner");

        UriInfo uriInfo =  new TestUriInfo();

        // uluut発行処理
        UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                System.currentTimeMillis(), UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getOwnerNormalized(), uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort());

        // p_cookie_peerとして、ランダムなUUIDを設定する
        String pCookiePeer = UUID.randomUUID().toString();
        // ヘッダに返却するdc-cookie値は、暗号化する
        String encodedCookieValue = uluut.getCookieString(pCookiePeer,
                AccessContext.getCookieCryptKey(uriInfo.getBaseUri().getHost()));

        String basicAuth = "Basic "
                + CommonUtils.encodeBase64Url("user:pass".getBytes());

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(basicAuth, uriInfo, pCookiePeer, encodedCookieValue,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_INVALID, accessContext.getType());
    }

    /**
     * マスタトークン認証AuthorizationHeaderとcookie認証情報が同時に指定された場合のAccessContext生成の正常系テスト.
     */
    @Test
    public void マスタトークン認証AuthorizationHeaderとcookie認証情報が同時に指定された場合のAccessContext生成の正常系テスト() {
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) any(), anyString())).thenReturn(true);
        when(cell.getOwnerNormalized()).thenReturn("cellowner");

        UriInfo uriInfo =  new TestUriInfo();

        // uluut発行処理
        UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                System.currentTimeMillis(), UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getOwnerNormalized(), uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort());

        // p_cookie_peerとして、ランダムなUUIDを設定する
        String pCookiePeer = UUID.randomUUID().toString();
        // ヘッダに返却するp-cookie値は、暗号化する
        String encodedCookieValue = uluut.getCookieString(pCookiePeer,
                AccessContext.getCookieCryptKey(uriInfo.getBaseUri().getHost()));

        String masterTokenAuth = "Bearer " + MASTER_TOKEN;

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(masterTokenAuth, uriInfo, pCookiePeer, encodedCookieValue,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_UNIT_MASTER, accessContext.getType());
    }


    @Test
    public void create_Bearer_VisitorLocalAccessToken_WithSchemaWithConfidentialMarker_When_Valid_Succeeds() {
    	String schema = "https://app1.unit.example/";
        Cell cell = (Cell) mock(Cell.class);
        when(cell.getUrl()).thenReturn("https://cell1.unit.example/");

        Box box = new Box(cell, "box", schema, "abcde", new Date().getTime()-1000000);

        List<Role> roleList = new ArrayList<>();
        roleList.add(new Role("role1", "box", schema, cell.getUrl()));
        roleList.add(new Role("role2", "box", schema, cell.getUrl()));
        VisitorLocalAccessToken vlat = new VisitorLocalAccessToken(new Date().getTime(), 3600, cell.getUrl(), "https://user1.unit.example/#me",
            roleList, schema + OAuth2Helper.Key.CONFIDENTIAL_MARKER, new String[] {"root"});
        String authzHeaderValue = "Bearer " + vlat.toTokenString();
        AccessContext ac = AccessContext.create(authzHeaderValue, new TestUriInfo(), null, null, cell, BASE_URL, UrlUtils.getHost(), null);
        ac.checkSchemaMatches(box);
        assertEquals(OAuth2Helper.SchemaLevel.CONFIDENTIAL , ac.getConfidentialLevel());
    }

    @Test
    public void hasSubjectPrivilegeForAcl_ReturnTrue_For_ValidVisitorLocalAccessToken() throws Exception {
        String schema = "https://app1.unit.example/";
        Cell cell = (Cell) mock(Cell.class);
        when(cell.getUrl()).thenReturn("https://cell1.unit.example/");

        Box box = new Box(cell, "box", schema, "abcde", new Date().getTime()-1000000);

        List<Role> roleList = new ArrayList<>();

        Role role = new Role("role1", box.getName(), schema, cell.getUrl());
        roleList.add(role);
        VisitorLocalAccessToken vlat = new VisitorLocalAccessToken(new Date().getTime(), 3600, cell.getUrl(), "https://user1.unit.example/#me",
            roleList, schema + OAuth2Helper.Key.CONFIDENTIAL_MARKER, new String[] {"root"});
        String authzHeaderValue = "Bearer " + vlat.toTokenString();
        AccessContext ac = AccessContext.create(authzHeaderValue, new TestUriInfo(), null, null, cell, BASE_URL, UrlUtils.getHost(), null);

        for (Role r: ac.getRoleList()) {
            log.info(r.toRoleInstanceURL() + "  (role instance url)");
        }

        // prepare Acl with
        //  baseUrl : https://cell1.unit.example/__role/box/
        Acl acl = new Acl();
        String baseUrl = cell.getUrl() + "__role/" + box.getName() + "/";
        acl.setBase(baseUrl);
        log.info(baseUrl + "  (Base Url in ACL)");

        Ace ace = new Ace();
        ace.setPrincipalHref(role.getName());
        ace.addGrantedPrivilege(BoxPrivilege.READ.getName());
        acl.getAceList().add(ace);

        assertTrue(ac.hasSubjectPrivilegeForAcl(acl, BoxPrivilege.READ));
    }

    @Test
    public void hasSubjectPrivilegeForAcl_ReturnFalse_When_AccessToken_HasInvalidRoleUrl() throws Exception {
        String schema = "https://app1.unit.example/";
        Cell cell = (Cell) mock(Cell.class);
        when(cell.getUrl()).thenReturn("https://cell1.unit.example/");

        Box box = new Box(cell, "box", schema, "abcde", new Date().getTime()-1000000);

        List<Role> roleList = new ArrayList<>();

        // PATH BASED role instance url is given in VisitorLocalAccessToken
        /// whereas unit is configured to use SUBDOMAIN BASED cell url.
        Role brokenRole = new Role("role2", box.getName(), schema, UriUtils.convertFqdnBaseToPathBase(cell.getUrl()));
        roleList.add(brokenRole);
        VisitorLocalAccessToken vlat = new VisitorLocalAccessToken(new Date().getTime(), 3600, cell.getUrl(), "https://user1.unit.example/#me",
            roleList, schema + OAuth2Helper.Key.CONFIDENTIAL_MARKER, new String[] {"root"});
        String authzHeaderValue = "Bearer " + vlat.toTokenString();
        AccessContext ac = AccessContext.create(authzHeaderValue, new TestUriInfo(), null, null, cell, BASE_URL, UrlUtils.getHost(), null);

        for (Role r: ac.getRoleList()) {
            log.info(r.toRoleInstanceURL() + "  (role instance url)");
        }

        // prepare Acl with
        //  baseUrl : https://cell1.unit.example/__role/box/
        Acl acl1 = new Acl();
        String baseUrl = cell.getUrl() + "__role/" + box.getName() + "/";
        acl1.setBase(baseUrl);
        log.info(baseUrl + "  (Base Url in ACL)");

        Ace ace = new Ace();
        ace.setPrincipalHref(brokenRole.getName());
        ace.addGrantedPrivilege(BoxPrivilege.READ.getName());
        acl1.getAceList().add(ace);

        // should not match
        // since the Role url is in PATH_BASE cell url format,
        // whereas unit itself is configured to use
        // SUBDOMAIN_BASE cell url.
        assertFalse(ac.hasSubjectPrivilegeForAcl(acl1, BoxPrivilege.READ));
    }

    /**
     * Mock UriInfo Implementation.
     */
    class TestUriInfo implements UriInfo {
        @Override
        public String getPath() {
            return "/personium-core";
        }

        @Override
        public String getPath(boolean decode) {
            return "/personium-core";
        }

        @Override
        public List<PathSegment> getPathSegments() {
            return null;
        }

        @Override
        public List<PathSegment> getPathSegments(boolean decode) {
            return null;
        }

        @Override
        public URI getRequestUri() {
            return null;
        }

        @Override
        public UriBuilder getRequestUriBuilder() {
            return null;
        }

        @Override
        public URI getAbsolutePath() {
            return null;
        }

        @Override
        public UriBuilder getAbsolutePathBuilder() {
            return null;
        }

        @Override
        public URI getBaseUri() {
            try {
                return new URI(UrlUtils.getBaseUrl());
            } catch (URISyntaxException e) {
                fail();
                return null;
            }
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            return null;
        }

        @Override
        public List<String> getMatchedURIs() {
            return null;
        }

        @Override
        public List<String> getMatchedURIs(boolean decode) {
            return null;
        }

        @Override
        public List<Object> getMatchedResources() {
            return null;
        }

        @Override
        public URI relativize(URI uri) {
            return null;
        }

        @Override
        public URI resolve(URI uri) {
            return null;
        }
    }
}
