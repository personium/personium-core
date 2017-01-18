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
package com.fujitsu.dc.test.jersey.cell.auth;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcException;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.bar.BarInstallTestUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.SentMessageUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * Basic認証のCellレベルのリソースに対するテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BasicAuthCellLevelTest extends JerseyTest {

    private String cellName = Setup.TEST_CELL_BASIC;
    private String userName = "account4";
    private String password = "password4";
    private String authorization = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, password).getBytes());

    /**
     * コンストラクタ.
     */
    public BasicAuthCellLevelTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテスト終了後に呼び出される.
     */
    @After
    public void after() {
        // CellのACLをデフォルトに戻す
        CellUtils.setAclDefault(cellName, AbstractCase.MASTER_TOKEN_NAME);
    }

    /**
     * Basic認証_認証APIの操作_正常系.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証_認証APIの操作_正常系() throws DcException {
        String authTargetCell = Setup.TEST_CELL1;
        String authSchemaCell = Setup.TEST_CELL_SCHEMA1;
        String authSchemaAccount = "account0";
        String authSchemaPassword = "password0";

        // __auth(スキーマ認証)
        DcResponse dcRes = CellUtils.schemaAuthenticateWithBasic(
                authTargetCell, "account4", "password4",
                authSchemaCell, authSchemaAccount, authSchemaPassword);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        // __authz
        HashMap<String, String> authorizationHeader = new HashMap<String, String>();
        authorizationHeader.put(HttpHeaders.AUTHORIZATION, authorization);
        dcRes = CellUtils.implicitflowAuthenticate(authTargetCell, authSchemaCell, authSchemaAccount,
                authSchemaPassword, "__/redirect.html", ImplicitFlowTest.DEFAULT_STATE, authorizationHeader);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_MOVED_TEMPORARILY);
        assertThat(dcRes.getFirstHeader(HttpHeaders.LOCATION)).contains("access_token");

    }

    /**
     * Basic認証_認証APIの操作_異常系.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証_認証APIの操作_異常系() throws DcException {
        String authTargetCell = Setup.TEST_CELL1;
        String authSchemaCell = Setup.TEST_CELL_SCHEMA1;
        String authSchemaAccount = "account0";
        String authSchemaPassword = "password0";

        // __auth(スキーマ認証)
        // 認証失敗時：400が返却され、WWW-Authenticateヘッダー(Auth Scheme: Basic)が付与される。
        DcResponse dcRes = CellUtils.schemaAuthenticateWithBasic(
                authTargetCell, "account4", "invlid_password",
                authSchemaCell, authSchemaAccount, authSchemaPassword);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);
        AuthTestCommon.waitForAccountLock();

        // __authz
        // 認証失敗時：200でHTMLが返却される。この際、HTML中にメッセージが付与される。
        // また、WWW-Authenticateヘッダーは付与されない。
        HashMap<String, String> authorizationHeader = new HashMap<String, String>();
        authorizationHeader.put(HttpHeaders.AUTHORIZATION, authorization);
        dcRes = CellUtils.implicitflowAuthenticate(authTargetCell, authSchemaCell, authSchemaAccount,
                "invalid_password", "__/redirect.html", ImplicitFlowTest.DEFAULT_STATE, authorizationHeader);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", dcRes.getFirstHeader(HttpHeaders.CONTENT_TYPE));
        ImplicitFlowTest.checkHtmlBody(dcRes, "PS-AU-0004", authTargetCell);
        AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);
        AuthTestCommon.waitForAccountLock();
    }

    /**
     * Basic認証ーBoxURL取得の操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーBoxURL取得の操作() throws DcException {
        String schemaCell = Setup.TEST_CELL_SCHEMA1;
        String schemaBox = "schemaBox";

        try {
            // 事前準備
            BoxUtils.createWithSchema(cellName, schemaBox, AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.cellRoot(schemaCell));

            // 401エラーとなること
            DcResponse dcRes = CellUtils.getBoxUrl(cellName, schemaCell, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            DavResourceUtils.setACLPrivilegeAllForAllUser(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    cellName + "/" + schemaBox, "");

            dcRes = CellUtils.getBoxUrl(cellName, schemaCell, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);
        } finally {
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, schemaBox, -1);
        }
    }

    /**
     * Basic認証ーBoxインストールの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーBoxインストールの操作() throws DcException {
        String boxName = "installBox";

        String location = null;
        try {
            // 401エラーとなること
            TResponse res = CellUtils.boxInstall(cellName, boxName, authorization).statusCode(
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            res = CellUtils.boxInstall(cellName, "installBox", authorization).statusCode(HttpStatus.SC_ACCEPTED);
            AuthTestCommon.checkAuthenticateHeaderNotExists(res);

            location = res.getHeader(HttpHeaders.LOCATION);
        } finally {
            if (location != null) {
                BarInstallTestUtils.waitBoxInstallCompleted(location);
            }
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, boxName, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * Basic認証ー__mypasswordの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ー__mypasswordの操作() throws DcException {
        try {
            // 401エラーとなること
            DcResponse dcRes = CellUtils.changePassword(cellName, "newPassword", authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合も401エラーとなること
            setAclPriviriegeAllPrincipalAll(cellName);

            dcRes = CellUtils.changePassword(cellName, "newPassword", authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);
        } finally {
            CellUtils.changePassword(cellName, AbstractCase.BEARER_MASTER_TOKEN, password);
        }
    }

    /**
     * Basic認証ーaclの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーaclの操作() throws DcException {
        // 401エラーとなること
        TResponse res = CellUtils.setAclPriviriegeAllPrincipalAll(cellName, authorization).statusCode(
                HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        res = CellUtils.setAclPriviriegeAllPrincipalAll(cellName, authorization).statusCode(
                HttpStatus.SC_OK);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
    }

    /**
     * Basic認証ーpropfindの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーpropfindの操作() throws DcException {
        // 401エラーとなること
        TResponse res = CellUtils.propfindWithAnyAuthSchema(cellName, authorization, "1", HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        res = CellUtils.propfindWithAnyAuthSchema(cellName, authorization, "1", HttpStatus.SC_MULTI_STATUS);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
    }

    /**
     * Basic認証ーproppatchの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーproppatchの操作() throws DcException {
        // 401エラーとなること
        TResponse res = CellUtils.proppatchWithAnyAuthSchema(cellName, authorization, HttpStatus.SC_UNAUTHORIZED,
                "hoge", "huga");
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合であっても401となること
        // この場合、通常は無条件でアクセスを許可するところだが、このリソースではBasic認証は許容していないため、認証情報がない状態となる。
        // このリソースは、UnitUser以上の権限がないとアクセスできないが、前述のとおり認証情報がないため、401エラーとなる。
        setAclPriviriegeAllPrincipalAll(cellName);

        res = CellUtils.proppatchWithAnyAuthSchema(cellName, authorization, HttpStatus.SC_UNAUTHORIZED, "hoge", "huga");
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);
    }

    /**
     * Basic認証ー__messageの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ー__messageの操作() throws DcException {
        String title = "BasicAuthCellLevelTest";
        String messageBody = "BasicAuthCellLevelTest000000000000";
        String targetCell = Setup.TEST_CELL1;
        String messageId = null;
        try {
            // 401エラーとなること
            TResponse res = SentMessageUtils.sentWithAnyAuthSchema(authorization, cellName,
                    new SentMessageUtils.Request(targetCell), HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            SentMessageUtils.Request request = new SentMessageUtils.Request(targetCell)
                    .title(title).body(messageBody);
            res = SentMessageUtils.sentWithAnyAuthSchema(authorization, cellName,
                    request, HttpStatus.SC_CREATED);
            AuthTestCommon.checkAuthenticateHeaderNotExists(res);

            messageId = SentMessageUtils.getMessageId(res);
        } finally {
            SentMessageUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1, messageId);
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(cellName), "message", title,
                    messageBody);
        }
    }

    /**
     * Basic認証ー__eventの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ー__eventの操作() throws DcException {
        // 401エラーとなること
        TResponse res = CellUtils.eventWithAnyAuthSchema(authorization, HttpStatus.SC_UNAUTHORIZED, cellName, "INFO",
                "authSchema", "/cell/app", "success");
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        res = CellUtils.eventWithAnyAuthSchema(authorization, HttpStatus.SC_OK, cellName, "INFO",
                "authSchema", "/cell/app", "success");
        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
    }

    /**
     * Basic認証ーボックスの__eventの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーボックスの__eventの操作() throws DcException {
        String boxName = "boxName";

        try {
            // 事前準備
            BoxUtils.create(cellName, boxName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // 401エラーとなること
            DcResponse dcRes = CellUtils.eventUnderBox(authorization, cellName, boxName,
                    "info", "Action", null, null);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            dcRes = CellUtils.eventUnderBox(authorization, cellName, boxName,
                    "info", "Action", null, null);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);
        } finally {
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, boxName, -1);
        }
    }

    /**
     * Basic認証ー__logの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ー__logの操作() throws DcException {
        // 401エラーとなること
        // ログ取得
        DcResponse dcRes = CellUtils.getCurrentLogWithAnyAuth(cellName, "default.log", authorization);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

        // ログPROPFIND
        TResponse res = CellUtils.propfindArchiveLogDir(cellName, authorization, "1", HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        // ログ取得
        dcRes = CellUtils.getCurrentLogWithAnyAuth(cellName, "default.log", authorization);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);

        // ログPROPFIND
        res = CellUtils.propfindArchiveLogDir(cellName, authorization, "1", HttpStatus.SC_MULTI_STATUS);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
    }

    /**
     * Basic認証ーOPTIONSの操作.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーOPTIONSの操作() throws DcException {
        // 401エラーとなること
        TResponse res = ResourceUtils.requestUtilWithAuthSchema("OPTIONS", authorization, "/" + cellName,
                HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        res = ResourceUtils.requestUtilWithAuthSchema("OPTIONS", authorization, "/" + cellName,
                HttpStatus.SC_OK);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
    }

    /**
     * Basic認証ーCell再帰削除.
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell再帰削除() throws DcException {
        String testCell = "BasicTestCellForBulkDeletion";
        try {
            // 事前準備
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, testCell);
            CellUtils.create(testCell, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // 401エラーとなること
            DcResponse dcRes = CellUtils.bulkDeletion(authorization, testCell);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, testCell);

            // ACL all-all の場合も401エラーとなること
            // この場合、通常は無条件でアクセスを許可するところだが、このリソースではBasic認証は許容していないため、認証情報がない状態となる。
            // このリソースは、UnitUser以上の権限がないとアクセスできないが、前述のとおり認証情報がないため、401エラーとなる。
            setAclPriviriegeAllPrincipalAll(testCell);
            dcRes = CellUtils.bulkDeletion(authorization, testCell);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, testCell);
        } finally {
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, testCell);
        }

    }

    /**
     * Basic認証ーCell制御オブジェクトの作成. <br />
     * （共通ロジックのためBoxのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトの作成() throws DcException {
        String testBox = "BasicTestBox";
        try {
            // 401エラーとなること
            DcResponse dcRes = BoxUtils.createWithAuthSchema(cellName, testBox, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            dcRes = BoxUtils.createWithAuthSchema(cellName, testBox, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);

        } finally {
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, testBox, -1);
        }
    }

    /**
     * Basic認証ーCell制御オブジェクトの取得. <br />
     * （共通ロジックのためBoxのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトの取得() throws DcException {
        String testBox = Setup.TEST_BOX1;

        // 401エラーとなること
        DcResponse dcRes = BoxUtils.getWithAuthSchema(cellName, testBox, authorization);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        dcRes = BoxUtils.getWithAuthSchema(cellName, testBox, authorization);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);
    }

    /**
     * Basic認証ーCell制御オブジェクトの一覧取得. <br />
     * （共通ロジックのためBoxのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトの一覧取得() throws DcException {
        // 401エラーとなること
        DcResponse dcRes = BoxUtils.listWithAuthSchema(cellName, authorization);
        AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        dcRes = BoxUtils.listWithAuthSchema(cellName, authorization);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);
    }

    /**
     * Basic認証ーCell制御オブジェクトの更新. <br />
     * （共通ロジックのためBoxのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトの更新() throws DcException {
        String testBox = "BasicTestBox";
        String newBox = "BasicTestBoxNew";
        try {
            // 事前準備
            BoxUtils.create(cellName, testBox, AbstractCase.MASTER_TOKEN_NAME);

            // 401エラーとなること
            DcResponse dcRes = BoxUtils.updateWithAuthSchema(cellName, testBox, newBox, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            dcRes = BoxUtils.updateWithAuthSchema(cellName, testBox, newBox, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
            AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);

        } finally {
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, testBox, -1);
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, newBox, -1);
        }
    }

    /**
     * Basic認証ーCell制御オブジェクトの削除. <br />
     * （共通ロジックのためBoxのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトの削除() throws DcException {
        String testBox = "BasicTestBox";
        try {
            // 事前準備
            BoxUtils.create(cellName, testBox, AbstractCase.MASTER_TOKEN_NAME);

            // 401エラーとなること
            DcResponse dcRes = BoxUtils.deleteWithAuthSchema(cellName, testBox, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(dcRes, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            dcRes = BoxUtils.deleteWithAuthSchema(cellName, testBox, authorization);
            assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
            AuthTestCommon.checkAuthenticateHeaderNotExists(dcRes);

        } finally {
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, testBox, -1);
        }
    }

    /**
     * Basic認証ーCell制御オブジェクトのNP登録. <br />
     * （共通ロジックのためBox -> Roleのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトのNP登録() throws DcException {
        String testBox = Setup.TEST_BOX1;
        String testRole = "BasicTestRole";
        try {
            // 401エラーとなること
            TResponse res = RoleUtils.createViaNPWithAuthSchema(cellName, authorization, "Box", testBox, testRole,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            res = RoleUtils.createViaNPWithAuthSchema(cellName, authorization, "Box", testBox, testRole,
                    HttpStatus.SC_CREATED);
            AuthTestCommon.checkAuthenticateHeaderNotExists(res);

        } finally {
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, testBox, testRole, -1);
        }
    }

    /**
     * Basic認証ーCell制御オブジェクトのNP一覧. <br />
     * （共通ロジックのためBox -> Roleのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトのNP一覧() throws DcException {
        String testBox = Setup.TEST_BOX1;

        // 401エラーとなること
        TResponse res = RoleUtils.listViaNPWithAuthSchema(cellName, authorization, "Box", testBox,
                HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        res = RoleUtils.listViaNPWithAuthSchema(cellName, authorization, "Box", testBox,
                HttpStatus.SC_OK);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
    }

    /**
     * Basic認証ーCell制御オブジェクトのlinks登録. <br />
     * （共通ロジックのためBox -> Roleのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトのlinks登録() throws DcException {
        String testBox = Setup.TEST_BOX1;
        String testRole = "BasicTestRole";
        try {
            // 事前準備
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, testRole, HttpStatus.SC_CREATED);

            // 401エラーとなること
            TResponse res = RoleUtils.createLinkWithAuthSchema(cellName, testRole, "Box", testBox, authorization,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            res = RoleUtils.createLinkWithAuthSchema(cellName, testRole, "Box", testBox, authorization,
                    HttpStatus.SC_NO_CONTENT);
            AuthTestCommon.checkAuthenticateHeaderNotExists(res);

        } finally {
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, null, testRole, -1);
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, testBox, testRole, -1);
        }
    }

    /**
     * Basic認証ーCell制御オブジェクトのlinks一覧. <br />
     * （共通ロジックのためBox -> Roleのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトのlinks一覧() throws DcException {
        String testBox = Setup.TEST_BOX1;
        // 401エラーとなること
        TResponse res = RoleUtils.listLinkWithAuthSchema(cellName, authorization, "Box", testBox,
                HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

        // ACL all-all の場合正常終了すること
        setAclPriviriegeAllPrincipalAll(cellName);

        res = RoleUtils.listLinkWithAuthSchema(cellName, authorization, "Box", testBox, HttpStatus.SC_OK);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
    }

    /**
     * Basic認証ーCell制御オブジェクトのlinks削除. <br />
     * （共通ロジックのためBox -> Roleのみ実施）
     * @throws DcException リクエスト失敗
     */
    @Test
    public final void Basic認証ーCell制御オブジェクトのlinks削除() throws DcException {
        String testBox = Setup.TEST_BOX1;
        String testRole = "BasicTestRole";
        String testRoleKey = RoleUtils.keyString(testRole, testBox);
        try {
            // 事前準備
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, testBox, testRole, HttpStatus.SC_CREATED);

            // 401エラーとなること
            TResponse res = RoleUtils.deleteLinkWithAuthSchema(cellName, testRoleKey, "Box", testBox, authorization,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL all-all の場合正常終了すること
            setAclPriviriegeAllPrincipalAll(cellName);

            res = RoleUtils.deleteLinkWithAuthSchema(cellName, testRoleKey, "Box", testBox, authorization,
                    HttpStatus.SC_NO_CONTENT);
            AuthTestCommon.checkAuthenticateHeaderNotExists(res);

        } finally {
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, null, testRole, -1);
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, testBox, testRole, -1);
        }
    }

    /**
     * CellのACL(all-all)を設定する.
     * @param cell Cell名
     */
    private void setAclPriviriegeAllPrincipalAll(String cell) {
        CellUtils.setAclPriviriegeAllPrincipalAll(cell, AbstractCase.BEARER_MASTER_TOKEN);
    }

}
