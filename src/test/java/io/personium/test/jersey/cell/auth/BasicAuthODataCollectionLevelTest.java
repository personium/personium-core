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

import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.box.odatacol.batch.AbstractUserDataBatchTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.BatchUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * Basic認証のBoxレベルのリソースに対するテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BasicAuthODataCollectionLevelTest extends PersoniumTest {

    private String cellName = Setup.TEST_CELL_BASIC;
    private String boxName = Setup.TEST_BOX1;
    private String colName = Setup.TEST_ODATA;
    private String srcEntityTypeName = "Sales";
    private String targetEntityTypeName = "SalesDetail";
    private String srcUserId = "srcId";
    private String targetUserId = "targetId";

    private String userName = "account4";
    private String password = "password4";
    private String invalidPassword = "invalidPassword";
    private String token = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, password).getBytes());
    private String invalidToken = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, invalidPassword).getBytes());
    private String invalidScheme = "invalidToken";

    /**
     * コンストラクタ.
     */
    public BasicAuthODataCollectionLevelTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Basic認証ースキーマ情報を持たないBox配下のODataコレクションレベルAPIの操作.
     */
    @Test
    public final void Basic認証ースキーマ情報を持たないBox配下のODataコレクションレベルAPIの操作() {
        // ユーザOData単体の操作
        userData();

        // ユーザODataの$linksの操作
        userDataLink();

        // ユーザODataのNavProp経由での操作
        userODataNavProp();
    }

    /**
     * Basic認証ー$batchでのUserODataの操作.
     * @throws PersoniumException リクエスト失敗
     */
    @Test
    public final void Basic認証ー$batchでのUserODataの操作() throws PersoniumException {
        // ※$batchは通常のODataの操作とは別ルートでの処理になっているため、テストも分離。
        try {
            // $batchのボディ(ユーザOData 作成 - 取得 - 削除)を作成
            String body = BatchUtils.START_BOUNDARY + BatchUtils.retrievePostBody(srcEntityTypeName, srcUserId)
                    + BatchUtils.START_BOUNDARY
                    + BatchUtils.retrieveGetBody(srcEntityTypeName + "('" + srcUserId + "')")
                    + BatchUtils.START_BOUNDARY
                    + BatchUtils.retrieveDeleteBody(srcEntityTypeName + "('" + srcUserId + "')")
                    + BatchUtils.END_BOUNDARY;

            // $batch - スキーマなしのBox配下に対してBasic認証ができること
            TResponse res = BatchUtils.batchRequestAnyAuthScheme(token, cellName, boxName, colName,
                    BatchUtils.BOUNDARY,
                    body,
                    HttpStatus.SC_ACCEPTED);
            // レスポンスボディのチェック(すべてのリクエストが成功していること)
            String expectedSuccessBody = BatchUtils.START_BOUNDARY
                    + BatchUtils.retrievePostResBody(cellName, boxName, colName, srcEntityTypeName, srcUserId)
                    + BatchUtils.START_BOUNDARY + BatchUtils.retrieveGetResBody(srcEntityTypeName, srcUserId)
                    + BatchUtils.START_BOUNDARY + BatchUtils.retrieveDeleteResBody() + BatchUtils.END_BOUNDARY;
            AbstractUserDataBatchTest.checkBatchResponseBody(res, expectedSuccessBody);

            // $batch - スキーマなしのBox配下に対してBasic認証(パスワード誤り)のとき認証エラーとなること
            res = BatchUtils.batchRequestAnyAuthScheme(invalidToken, cellName, boxName, colName, BatchUtils.BOUNDARY,
                    body, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // $batch - スキーマなしのBox配下に対して認証スキーマが誤っているとき認証エラーとなること
            res = BatchUtils.batchRequestAnyAuthScheme(invalidScheme, cellName, boxName, colName, BatchUtils.BOUNDARY,
                    body, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);

            // $batch - スキーマなしのBox配下に対して権限が無いユーザを指定したとき403エラーとなること
            String nonePrivilegeToken = "Basic "
                    + Base64.encodeBase64String("account1:password1".getBytes());
            res = BatchUtils.batchRequestAnyAuthScheme(nonePrivilegeToken, cellName, boxName, colName,
                    BatchUtils.BOUNDARY, body, HttpStatus.SC_FORBIDDEN);
            AuthTestCommon.checkAuthenticateHeaderNotExists(res);

            // スキーマ設定(Box更新)
            BoxUtils.update(cellName, AbstractCase.MASTER_TOKEN_NAME, boxName, "*", boxName,
                    UrlUtils.cellRoot(cellName), HttpStatus.SC_NO_CONTENT);

            // $batch - スキーマありのBox配下に対してBasic認証ができないこと
            res = BatchUtils.batchRequestAnyAuthScheme(token, cellName, boxName, colName, BatchUtils.BOUNDARY,
                    body, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, cellName);

            // ACL権限の変更
            DavResourceUtils.setACLPrivilegeAllForAllUser(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    cellName + "/" + boxName + "/" + colName, "");

            // $batch - スキーマあり+ACLの権限がALL:ALLのBox配下に対してBasic認証ができること
            res = BatchUtils.batchRequestAnyAuthScheme(token, cellName, boxName, colName, BatchUtils.BOUNDARY,
                    body, HttpStatus.SC_ACCEPTED);
            // レスポンスボディのチェック(すべてのリクエストが成功していること)
            AbstractUserDataBatchTest.checkBatchResponseBody(res, expectedSuccessBody);
        } finally {
            // スキーマ削除(Box更新)
            BoxUtils.updateWithAuthSchema(cellName, boxName, boxName, AbstractCase.BEARER_MASTER_TOKEN);

            // ACL設定を元に戻す(Setup.create()で設定しているのもと同じ内容を設定)
            DavResourceUtils.setACL(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, colName,
                    "box/acl-authtest.txt", boxName, "");
        }
    }

    @SuppressWarnings("unchecked")
    private void userODataNavProp() {
        JSONObject jsonBody = new JSONObject();
        try {
            // NavProのsrc側となるユーザODataを作成
            jsonBody.put("__id", srcUserId);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, jsonBody, cellName, boxName,
                    colName, srcEntityTypeName);

            // NavPro経由登録(Basic認証-成功)
            jsonBody.put("__id", targetUserId);
            UserDataUtils.createViaNPAnyAuthSchema(token, jsonBody, cellName, boxName, colName, srcEntityTypeName,
                    srcUserId, targetEntityTypeName, HttpStatus.SC_CREATED);
            // NavPro経由登録(Basic認証-失敗)
            TResponse res = UserDataUtils.createViaNPAnyAuthSchema(invalidToken, jsonBody, cellName, boxName, colName,
                    srcEntityTypeName, srcUserId, targetEntityTypeName, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // NavPro経由一覧
            UserDataUtils.listViaNPAnyAuthSchema(token, cellName, boxName, colName, srcEntityTypeName, srcUserId,
                    targetEntityTypeName, "", HttpStatus.SC_OK);
            res = UserDataUtils.listViaNPAnyAuthSchema(invalidToken, cellName, boxName, colName, srcEntityTypeName,
                    srcUserId, targetEntityTypeName, "", HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();
        } finally {
            // ユーザODataの削除
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, targetEntityTypeName, targetUserId);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, srcEntityTypeName, srcUserId);
        }
    }

    /**
     * Basic認証ーユーザOData単体での操作.
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    private void userData() {
        String propertyValue = "proeprty";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("__id", srcUserId);
        jsonBody.put("property", propertyValue);
        String stringBody = jsonBody.toJSONString();

        try {
            // ユーザOData作成(Basic認証-成功)
            UserDataUtils.createWithBasic(userName, password, HttpStatus.SC_CREATED,
                    stringBody, cellName, boxName, colName, srcEntityTypeName);
            // ユーザOData作成(Basic認証-失敗)
            TResponse res = UserDataUtils.createWithBasic(userName + "invalid", password, HttpStatus.SC_UNAUTHORIZED,
                    stringBody, cellName, boxName, colName, srcEntityTypeName);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // ユーザOData取得(Basic認証-成功)
            UserDataUtils.getWithQueryAnyAuthSchema(cellName, token, boxName, colName, srcEntityTypeName, "",
                    srcUserId,
                    HttpStatus.SC_OK);
            // ユーザOData取得(Basic認証-失敗)
            res = UserDataUtils.getWithQueryAnyAuthSchema(cellName, invalidToken, boxName, colName, srcEntityTypeName,
                    "", srcUserId, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // ユーザOData更新(Basic認証-成功)
            UserDataUtils.updateAnyAuthSchema(token, HttpStatus.SC_NO_CONTENT, jsonBody, cellName, boxName, colName,
                    srcEntityTypeName, srcUserId, "*");
            // ユーザOData更新(Basic認証-失敗)
            res = UserDataUtils.updateAnyAuthSchema(invalidToken, HttpStatus.SC_UNAUTHORIZED, jsonBody, cellName,
                    boxName, colName, srcEntityTypeName, srcUserId, "*");
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // ユーザOData部分更新(Basic認証-成功)
            UserDataUtils.mergeAnyAuthSchema(token, HttpStatus.SC_NO_CONTENT, jsonBody, cellName, boxName, colName,
                    srcEntityTypeName, srcUserId, "*");
            // ユーザOData部分更新(Basic認証-失敗)
            res = UserDataUtils.mergeAnyAuthSchema(invalidToken, HttpStatus.SC_UNAUTHORIZED, jsonBody, cellName,
                    boxName, colName, srcEntityTypeName, srcUserId, "*");
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // ユーザOData一覧取得(Basic認証-成功)
            UserDataUtils.listAnyAuthSchema(cellName, boxName, colName, srcEntityTypeName, "", token, HttpStatus.SC_OK);
            // ユーザOData一覧取得(Basic認証-失敗)
            res = UserDataUtils.listAnyAuthSchema(cellName, boxName, colName, srcEntityTypeName, "", invalidToken,
                    HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // ユーザOData削除(Basic認証-成功)
            UserDataUtils.deleteAnyAuthSchema(token, HttpStatus.SC_NO_CONTENT, cellName, boxName, colName,
                    srcEntityTypeName, srcUserId);
            // ユーザOData削除(Basic認証-失敗)
            res = UserDataUtils.deleteAnyAuthSchema(invalidToken, HttpStatus.SC_UNAUTHORIZED, cellName, boxName,
                    colName, srcEntityTypeName, srcUserId);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();
        } finally {
            // ユーザOData削除
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, srcEntityTypeName,
                    srcUserId);
        }
    }

    /**
     * Basic認証ーユーザOData$linksの操作.
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    private void userDataLink() {
        JSONObject jsonBody = new JSONObject();
        try {
            // $links対象のユーザデータ作成
            jsonBody.put("__id", srcUserId);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, jsonBody,
                    cellName, boxName, colName, srcEntityTypeName);
            jsonBody.put("__id", targetUserId);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, jsonBody,
                    cellName, boxName, colName, targetEntityTypeName);

            // $links登録(Basic認証-成功)
            UserDataUtils.createLinkAnyAuthSchema(token, cellName, boxName, colName, srcEntityTypeName, srcUserId,
                    targetEntityTypeName, targetUserId, HttpStatus.SC_NO_CONTENT);
            // $links登録(Basic認証-失敗)
            TResponse res = UserDataUtils.createLinkAnyAuthSchema(invalidToken, cellName, boxName, colName,
                    srcEntityTypeName, srcUserId,
                    targetEntityTypeName, targetUserId, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // $links一覧(Basic認証-成功)
            UserDataUtils.listLinkAnyAuthSchema(token, cellName, boxName, colName, srcEntityTypeName, srcUserId,
                    targetEntityTypeName, HttpStatus.SC_OK);
            // $links一覧(Basic認証-失敗)
            res = UserDataUtils.listLinkAnyAuthSchema(invalidToken, cellName, boxName, colName,
                    srcEntityTypeName, srcUserId, targetEntityTypeName, HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // $links削除(Basic認証-成功)
            UserDataUtils.deleteLinksAnyAuthSchema(token, cellName, boxName, colName, srcEntityTypeName, srcUserId,
                    targetEntityTypeName, targetUserId, HttpStatus.SC_NO_CONTENT);
            // $links削除(Basic認証-失敗)
            res = UserDataUtils.deleteLinksAnyAuthSchema(invalidToken, cellName, boxName, colName, srcEntityTypeName,
                    srcUserId, targetEntityTypeName, targetUserId, HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();
        } finally {
            // ユーザODataの削除
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, targetEntityTypeName, targetUserId);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, srcEntityTypeName, srcUserId);
        }
    }

    private void checkAuthenticateHeaderForSchemalessBoxLevel(TResponse res, String expectedCellName) {
        // WWW-Authenticateヘッダチェック
        String bearer = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(expectedCellName));
        String basic = String.format("Basic realm=\"%s\"", UrlUtils.cellRoot(expectedCellName));
        List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
        assertEquals(2, headers.size());
        assertThat(headers).contains(bearer);
        assertThat(headers).contains(basic);
    }

}
