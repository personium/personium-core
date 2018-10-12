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
package io.personium.test.jersey.cell.auth;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Role;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.box.acl.jaxb.Acl;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * Basic認証のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthBasicTest extends PersoniumTest {

    static final String TEST_CELL1 = Setup.TEST_CELL1;
    // スキーマ情報なしBox作成用のCell
    static final String MY_CELL = "basicauthenticationcell";
    static final String TEST_BOX1 = Setup.TEST_BOX1;
    static final String TEST_ODATA = Setup.TEST_ODATA;
    static final String TEST_ACCOUNT = "account4";
    static final String TEST_ACCOUNT_PASSWORD = "password4";
    static final String TEST_ROLE = "role4";

    /**
     * コンストラクタ.
     */
    public AuthBasicTest() {
        // セル再帰的削除を使用するための準備
        super(new PersoniumCoreApplication());
    }

    /**
     * Basic認証で正しい認証情報を指定した場合ユーザODataの操作ができること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void Basic認証で正しい認証情報を指定した場合ユーザODataの操作ができること() throws JAXBException {
        String entityType = "basicTestEntity";
        String userId = "id0001";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();
            // EntityType作成
            EntityTypeUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String body = String.format("{\"__id\":\"%s\"}", userId);
            UserDataUtils.createWithBasic(TEST_ACCOUNT, TEST_ACCOUNT_PASSWORD, HttpStatus.SC_CREATED, body, MY_CELL,
                    TEST_BOX1,
                    TEST_ODATA, entityType);
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * Basic認証で誤ったパスワードを指定した場合ユーザODataの操作で401が返却されること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void Basic認証で誤ったパスワードを指定した場合ユーザODataの操作で401が返却されること() throws JAXBException {
        String entityType = "basicTestEntity";
        String userId = "id0001";
        String invalidPassword = "passssword4";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();
            // EntityType作成
            EntityTypeUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String body = String.format("{\"__id\":\"%s\"}", userId);
            TResponse res = UserDataUtils.createWithBasic(TEST_ACCOUNT, invalidPassword,
                    HttpStatus.SC_UNAUTHORIZED, body,
                    MY_CELL, TEST_BOX1, TEST_ODATA, entityType);
            // WWW-Authenticateヘッダチェック
            String expectedBearer = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            String expectedBasic = String.format("Basic realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(2, headers.size());
            assertThat(headers).contains(expectedBearer);
            assertThat(headers).contains(expectedBasic);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(res, PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED.getCode(),
                    PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED.getMessage());
            AuthTestCommon.waitForAccountLock();
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * Basic認証で誤ったパスワードを指定した場合Accountがロックされること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    // Accountロックのタイミングによってテストが成功したりしなかったりするためIgnoreCaseとする
    @Ignore
    public final void Basic認証で誤ったパスワードを指定した場合Accountがロックされること() throws JAXBException {
        String invalidPassword = "passwowwwwwrd4";

        String entityType = "basicTestEntity";
        String userId = "id0001";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();
            // EntityType作成
            EntityTypeUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String body = String.format("{\"__id\":\"%s\"}", userId);
            TResponse res = UserDataUtils.createWithBasic(TEST_ACCOUNT, invalidPassword, HttpStatus.SC_UNAUTHORIZED,
                    body,
                    MY_CELL, TEST_BOX1, TEST_ODATA, entityType);
            // WWW-Authenticateヘッダチェック
            String expectedBearer = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            String expectedBasic = String.format("Basic realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(2, headers.size());
            assertThat(headers).contains(expectedBearer);
            assertThat(headers).contains(expectedBasic);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(res, PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED.getCode(),
                    PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED.getMessage());

            // Accountロック後のBasic認証(正しい認証情報)で401エラーとなること
            res = UserDataUtils.createWithBasic(TEST_ACCOUNT, TEST_ACCOUNT_PASSWORD, HttpStatus.SC_UNAUTHORIZED, body,
                    MY_CELL, TEST_BOX1, TEST_ODATA, entityType);
            // WWW-Authenticateヘッダチェック
            headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(2, headers.size());
            assertThat(headers).contains(expectedBearer);
            assertThat(headers).contains(expectedBasic);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(res,
                    PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED_IN_ACCOUNT_LOCK.getCode(),
                    PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED_IN_ACCOUNT_LOCK.getMessage());

            // Accountロック解除後のBasic認証(正しい認証情報)で作成できること
            AuthTestCommon.waitForAccountLock();
            res = UserDataUtils.createWithBasic(TEST_ACCOUNT, TEST_ACCOUNT_PASSWORD, HttpStatus.SC_CREATED, body,
                    MY_CELL, TEST_BOX1, TEST_ODATA, entityType);
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * Basic認証で存在しないアカウントを指定した場合ユーザODataの操作で401が返却されること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void Basic認証で存在しないアカウントを指定した場合ユーザODataの操作で401が返却されること() throws JAXBException {
        String invalidAccount = "invalidAccount";

        String entityType = "basicTestEntity";
        String userId = "id0001";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();
            // EntityType作成
            EntityTypeUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String body = String.format("{\"__id\":\"%s\"}", userId);
            TResponse res = UserDataUtils.createWithBasic(invalidAccount, TEST_ACCOUNT_PASSWORD,
                    HttpStatus.SC_UNAUTHORIZED, body,
                    MY_CELL, TEST_BOX1, TEST_ODATA, entityType);
            // WWW-Authenticateヘッダチェック
            String expectedBearer = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            String expectedBasic = String.format("Basic realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(2, headers.size());
            assertThat(headers).contains(expectedBearer);
            assertThat(headers).contains(expectedBasic);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(res, PersoniumCoreAuthzException.BASIC_AUTH_FORMAT_ERROR.getCode(),
                    PersoniumCoreAuthzException.BASIC_AUTH_FORMAT_ERROR.getMessage());
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * Basic認証で認証情報の書式が不正な場合ユーザODataの操作で401が返却されること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void Basic認証で認証情報の書式が不正な場合ユーザODataの操作で401が返却されること() throws JAXBException {
        String entityType = "basicTestEntity";
        String userId = "id0001";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();
            // EntityType作成
            EntityTypeUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String credentials = TEST_ACCOUNT + ":" + TEST_ACCOUNT_PASSWORD;
            String body = String.format("{\"__id\":\"%s\"}", userId);
            TResponse res = Http.request("box/odatacol/create.txt")
                    .with("cell", MY_CELL)
                    .with("box", TEST_BOX1)
                    .with("collection", TEST_ODATA)
                    .with("entityType", entityType)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("token", "Basic " + credentials)
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
                    .debug();
            // WWW-Authenticateヘッダチェック
            String expectedBearer = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            String expectedBasic = String.format("Basic realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(2, headers.size());
            assertThat(headers).contains(expectedBearer);
            assertThat(headers).contains(expectedBasic);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(res, PersoniumCoreAuthzException.BASIC_AUTH_FORMAT_ERROR.getCode(),
                    PersoniumCoreAuthzException.BASIC_AUTH_FORMAT_ERROR.getMessage());
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * Basic認証で正しい認証情報を指定した場合EntityTypeの操作ができること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void Basic認証で正しい認証情報を指定した場合EntityTypeの操作ができること() throws JAXBException {
        String entityType = "basicTestEntity";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();

            // Basic認証での操作
            // EntityType作成
            EntityTypeUtils.createWithBasic(MY_CELL, TEST_ACCOUNT, TEST_ACCOUNT_PASSWORD, TEST_BOX1, TEST_ODATA,
                    entityType,
                    HttpStatus.SC_CREATED);
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * Basic認証でセルレベルの操作をする場合401が返却されること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Basic認証でセルレベルの操作をする場合401が返却されること() throws JAXBException {
        String relationName = "basicTestRelation";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();

            // Relation作成
            JSONObject body = new JSONObject();
            body.put("Name", relationName);
            TResponse res = RelationUtils.createWithBasic(MY_CELL, TEST_ACCOUNT, TEST_ACCOUNT_PASSWORD, body,
                    HttpStatus.SC_UNAUTHORIZED);
            // WWW-Authenticateヘッダチェック
            String expected = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(MY_CELL));
            List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(1, headers.size());
            assertThat(headers).contains(expected);
            ODataCommon.checkErrorResponseBody(res, PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.getCode(),
                    PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.getMessage());
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * スキーマ有のBox配下のユーザODataの操作でBasic認証した場合401が返却されること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void スキーマ有のBox配下のユーザODataの操作でBasic認証した場合401が返却されること() throws JAXBException {
        String entityType = "basicTestEntity";
        String userId = "id0001";

        try {
            // 事前準備
            // 本テストはreset()で作成するスキーマ有Box(ACL設定済み)を使用するため、EntityTypeの作成が必要
            // EntityType作成
            EntityTypeUtils.create(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String body = String.format("{\"__id\":\"%s\"}", userId);
            TResponse res = UserDataUtils.createWithBasic(TEST_ACCOUNT, TEST_ACCOUNT_PASSWORD,
                    HttpStatus.SC_UNAUTHORIZED, body,
                    TEST_CELL1, TEST_BOX1, TEST_ODATA, entityType);
            // WWW-Authenticateヘッダチェック
            String expected = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(TEST_CELL1));
            List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(1, headers.size());
            assertThat(headers).contains(expected);
            ODataCommon.checkErrorResponseBody(res, PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.getCode(),
                    PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.getMessage());
            AuthTestCommon.waitForAccountLock();
        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, TEST_CELL1, TEST_BOX1, TEST_ODATA, entityType,
                    userId);
            EntityTypeUtils.delete(TEST_ODATA, AbstractCase.MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityType,
                    TEST_BOX1, TEST_CELL1, -1);
        }
    }

    /**
     * Basic認証で誤ったパスワードを指定した場合権限にALLを指定しているユーザODataの操作を行えること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void Basic認証で誤ったパスワードを指定した場合権限にALLを指定しているユーザODataの操作を行えること() throws JAXBException {
        String entityType = "basicTestEntity";
        String userId = "id0001";
        String invalidPassword = "passssword4";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();
            // ACL権限の変更
            DavResourceUtils.setACLPrivilegeAllForAllUser(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    MY_CELL + "/" + TEST_BOX1, "");

            // EntityType作成
            EntityTypeUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String body = String.format("{\"__id\":\"%s\"}", userId);
            UserDataUtils.createWithBasic(TEST_ACCOUNT, invalidPassword, HttpStatus.SC_CREATED, body,
                    MY_CELL, TEST_BOX1, TEST_ODATA, entityType);
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * Basic認証ですべてのユーザがアクセス可能なスキーマ付きBox配下のユーザODataの操作を行えること.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    @Test
    public final void Basic認証ですべてのユーザがアクセス可能なスキーマ付きBox配下のユーザODataの操作を行えること() throws JAXBException {
        String entityType = "basicTestEntity";
        String userId = "id0001";
        String password = "password4";

        try {
            // 事前準備
            Setup.cellBulkDeletion(MY_CELL);
            createBaseData();
            // スキーマ設定(Box更新)
            BoxUtils.update(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, "*", TEST_BOX1,
                    UrlUtils.cellRoot(MY_CELL), HttpStatus.SC_NO_CONTENT);
            // ACL権限の変更
            DavResourceUtils.setACLPrivilegeAllForAllUser(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    MY_CELL + "/" + TEST_BOX1, "");
            // EntityType作成
            EntityTypeUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX1, TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // Basic認証での操作
            // ユーザOData作成
            String body = String.format("{\"__id\":\"%s\"}", userId);
            UserDataUtils.createWithBasic(TEST_ACCOUNT, password, HttpStatus.SC_CREATED, body,
                    MY_CELL, TEST_BOX1, TEST_ODATA, entityType);
        } finally {
            // Cellを再帰的削除
            Setup.cellBulkDeletion(MY_CELL);
        }
    }

    /**
     * 本クラスのテストメソッドで必要な基礎データ登録と認証関係の操作を行う.
     * @throws JAXBException リクエスト用ACLのパースエラー
     */
    private void createBaseData() throws JAXBException {
        // 基礎データ
        CellUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.create(MY_CELL, TEST_BOX1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, MY_CELL,
                TEST_BOX1, TEST_ODATA);
        // 認証に必要な情報
        RoleUtils.create(MY_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_ROLE, TEST_BOX1, HttpStatus.SC_CREATED);
        AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, MY_CELL, TEST_ACCOUNT, TEST_ACCOUNT_PASSWORD,
                HttpStatus.SC_CREATED);
        LinksUtils.createLinks(MY_CELL, Account.EDM_TYPE_NAME, TEST_ACCOUNT, null,
                Role.EDM_TYPE_NAME, TEST_ROLE, TEST_BOX1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

        Acl acl = new Acl();
        List<String> privileges = new ArrayList<String>();
        privileges.add("read");
        privileges.add("write");
        privileges.add("alter-schema");
        acl.getAce().add(DavResourceUtils.createAce(false, TEST_ROLE, privileges));
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), MY_CELL, TEST_BOX1));
        DavResourceUtils.setAcl(AbstractCase.MASTER_TOKEN_NAME, MY_CELL, TEST_BOX1, "", acl, HttpStatus.SC_OK);
    }

}
