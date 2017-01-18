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
package com.fujitsu.dc.test.unit.core.auth;

import static com.fujitsu.dc.common.auth.token.AbstractOAuth2Token.MILLISECS_IN_AN_HOUR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Matchers;

import com.fujitsu.dc.common.auth.token.CellLocalAccessToken;
import com.fujitsu.dc.common.auth.token.LocalToken;
import com.fujitsu.dc.common.auth.token.UnitLocalUnitUserToken;
import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.auth.AccessContext;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.core.odata.OEntityWrapper;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * AccessContext ユニットテストクラス.
 */
@RunWith(DcRunner.class)
@Category({ Unit.class })
public class AccessContextTest {

    /**
     * マスタートークン.
     */
    public static final String MASTER_TOKEN = DcCoreConfig.getMasterToken();

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
     */
    @BeforeClass
    public static void beforeClass() {
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
     * testCreateBasicのテスト.
     */
    @Test
    public void testCreate() {
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(null, null, null, null,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(accessContext.getType(), AccessContext.TYPE_ANONYMOUS);
    }

    /**
     * testCreateBasicのテスト.
     * TODO V1.1 Basic認証に対応後有効化する
     */
    @Test
    @Ignore
    public void testCreateBasic() {
        String auth = "Basic "
                + DcCoreUtils.encodeBase64Url("user:pass".getBytes());
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);
        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(auth,
                null, null, null, cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(accessContext.getType(), AccessContext.TYPE_BASIC);

    }

    /**
     * testCreateBasicでInvalidになるテスト.
     */
    @Test
    public void testCreateBasicINVALID() {
        String auth = "Basic "
                + DcCoreUtils.encodeBase64Url("user:pass".getBytes());
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
    public void testCreateBearerAuthzMasterToken() {
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
    public void testCreateBearerAuthzCellNullParseError() {
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
    public void testCreateBearerAuthzCellNullParamBearer() {
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
    public void testCreateBearerAuthzCellNullParamBearerSpace() {
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
        when(cell.getOwner()).thenReturn("cellOwner");

        UriInfo uriInfo =  new TestUriInfo();

        // uluut発行処理
        UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                System.currentTimeMillis(), UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getOwner(), uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort());

        String tokenString = uluut.toTokenString();
        // dc_cookie_peerとして、ランダムなUUIDを設定する
        String dcCookiePeer = UUID.randomUUID().toString();
        String cookieValue = dcCookiePeer + "\t" + tokenString;
        // ヘッダに返却するdc-cookie値は、暗号化する
        String encodedCookieValue = LocalToken.encode(cookieValue,
                UnitLocalUnitUserToken.getIvBytes(AccessContext.getCookieCryptKey(uriInfo.getBaseUri())));

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
        when(cell.getOwner()).thenReturn("cellOwner");
        when(cell.getUrl()).thenReturn(uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort());

        // Token発行処理
        CellLocalAccessToken token = new CellLocalAccessToken(
                uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort(), cell.getOwner(), null,
                uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort());

        String tokenString = token.toTokenString();
        // dc_cookie_peerとして、ランダムなUUIDを設定する
        String dcCookiePeer = UUID.randomUUID().toString();
        String cookieValue = dcCookiePeer + "\t" + tokenString;
        // ヘッダに返却するdc-cookie値は、暗号化する
        String encodedCookieValue = LocalToken.encode(cookieValue,
                UnitLocalUnitUserToken.getIvBytes(AccessContext.getCookieCryptKey(uriInfo.getBaseUri())));

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(null, uriInfo, dcCookiePeer, encodedCookieValue,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_LOCAL, accessContext.getType());
    }

    /**
     * BASIC認証AuthorizationHeaderとcookie認証情報が同時に指定された場合のAccessContext生成の正常系テスト.
     */
    @Test
    public void BASIC認証AuthorizationHeaderとcookie認証情報が同時に指定された場合のAccessContext生成の正常系テスト() {
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((OEntityWrapper) Matchers.any(), Matchers.anyString())).thenReturn(true);
        when(cell.getOwner()).thenReturn("cellOwner");

        UriInfo uriInfo =  new TestUriInfo();

        // uluut発行処理
        UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                System.currentTimeMillis(), UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getOwner(), uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort());

        String tokenString = uluut.toTokenString();
        // dc_cookie_peerとして、ランダムなUUIDを設定する
        String dcCookiePeer = UUID.randomUUID().toString();
        String cookieValue = dcCookiePeer + "\t" + tokenString;
        // ヘッダに返却するdc-cookie値は、暗号化する
        String encodedCookieValue = LocalToken.encode(cookieValue,
                UnitLocalUnitUserToken.getIvBytes(AccessContext.getCookieCryptKey(uriInfo.getBaseUri())));

        String basicAuth = "Basic "
                + DcCoreUtils.encodeBase64Url("user:pass".getBytes());

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(basicAuth, uriInfo, dcCookiePeer, encodedCookieValue,
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
        when(cell.getOwner()).thenReturn("cellOwner");

        UriInfo uriInfo =  new TestUriInfo();

        // uluut発行処理
        UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                System.currentTimeMillis(), UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getOwner(), uriInfo.getBaseUri().getHost()  + ":"  + uriInfo.getBaseUri().getPort());

        String tokenString = uluut.toTokenString();
        // dc_cookie_peerとして、ランダムなUUIDを設定する
        String dcCookiePeer = UUID.randomUUID().toString();
        String cookieValue = dcCookiePeer + "\t" + tokenString;
        // ヘッダに返却するdc-cookie値は、暗号化する
        String encodedCookieValue = LocalToken.encode(cookieValue,
                UnitLocalUnitUserToken.getIvBytes(AccessContext.getCookieCryptKey(uriInfo.getBaseUri())));

        String masterTokenAuth = "Bearer " + MASTER_TOKEN;

        // 第1引数は AuthHeader, 第2引数は UriInfo, 第3引数は cookie_peer, 第4引数は cookie内の暗号化されたトークン情報
        AccessContext accessContext = AccessContext.create(masterTokenAuth, uriInfo, dcCookiePeer, encodedCookieValue,
                cell, BASE_URL, UrlUtils.getHost(), OWNER);
        assertEquals(AccessContext.TYPE_UNIT_MASTER, accessContext.getType());
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
    }
}
