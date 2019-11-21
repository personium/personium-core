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
package io.personium.core.auth;

import static io.personium.common.auth.token.AbstractOAuth2Token.MILLISECS_IN_AN_HOUR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Matchers;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;

import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Unit;
import io.personium.test.utils.UrlUtils;

/**
 * Unit test class for AccessContext.
 */
@Category({ Unit.class })
public class AccessContextTest {

    /**
     * Master Token.
     */
    public static final String MASTER_TOKEN = PersoniumUnitConfig.getMasterToken();

    /**
     * baseUrl.
     */
    public static final String BASE_URL = UrlUtils.getBaseUrl();

    /**
     * Owner.
     */
    public static final String OWNER = null;

    /**
     * トークン処理ライブラリの初期設定.
     * @throws IOException 
     * @throws InvalidNameException 
     * @throws CertificateException 
     * @throws InvalidKeySpecException 
     * @throws Exception 
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        PersoniumCoreApplication.loadConfig();
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
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
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);

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
        when(cell.getAccount(Matchers.anyString())).thenReturn(oew);
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);
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
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(false);
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
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);
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
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);
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
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);
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
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);
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
        VisitorLocalAccessToken vlat = new VisitorLocalAccessToken(new Date().getTime(), 3600, cell.getUrl(), "https://user1.unit.example/#me",
        		roleList, schema + OAuth2Helper.Key.CONFIDENTIAL_MARKER, new String[] {"root"});
        String authzHeaderValue = "Bearer " + vlat.toTokenString();
        AccessContext ac = AccessContext.create(authzHeaderValue, new TestUriInfo(), null, null, cell, BASE_URL, UrlUtils.getHost(), null);
        ac.checkSchemaMatches(box);
    }

    /**
     * ダミーの UriInfo実装.
     */
    class TestUriInfo implements UriInfo {
        @Override
        public String getPath() {
            return "/dc1-core";
        }

        @Override
        public String getPath(boolean decode) {
            return "/dc1-core";
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
