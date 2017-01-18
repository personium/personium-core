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

import static org.junit.Assert.assertTrue;

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
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * $bacth認証のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthBatchTest extends JerseyTest {

    static final String TEST_CELL1 = Setup.TEST_CELL1;
    static final String TEST_CELL2 = Setup.TEST_CELL2;
    static final String TEST_APP_CELL1 = "schema1";
    static final String BOX_NAME = "box1";
    static final String COL_NAME = "setodata";
    static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    static final String ACL_AUTH_TEST_FILE = "box/acl-authtest.txt";

    /**
     * $batchのバウンダリー.
     */
    public static final String BOUNDARY = "batch_XAmu9BiJJLBa20sRWIq74jp2UlNAVueztqu";
    private static final String START_BOUNDARY = "--" + BOUNDARY + "\n";
    private static final String END_BOUNDARY = "--" + BOUNDARY + "--";
    /**
     * $batchのリクエストボディ.
     */
    public static final String TEST_BODY = START_BOUNDARY + retrievePostBody("Supplier", "testAutuBatch1")
            + START_BOUNDARY + retrieveListBody("Supplier")
            + START_BOUNDARY + retrieveGetBody("Supplier('testAutuBatch1')")
            + START_BOUNDARY + retrievePutBody("Supplier('testAutuBatch1')")
            + START_BOUNDARY + retrieveDeleteBody("Supplier('testAutuBatch1')")
            + END_BOUNDARY;

    /**
     * 認証トークン配列番号.
     */
    static final int NO_PRIVILEGE = 0;
    static final int READ = 1;
    static final int WRITE = 2;
    static final int READ_WRITE = 3;
    static final int EXEC = 4;

    /**
     * コンストラクタ.
     */
    public AuthBatchTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテストの後に実行する処理.
     */
    @After
    public void after() {
        // Setupで設定したACLに戻す
        DavResourceUtils.setACL(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK, COL_NAME, ACL_AUTH_TEST_FILE, BOX_NAME, "");
    }

    /**
     * 正しい認証情報を使用してすべてのユーザがアクセス可能なコレクションに対して$batchをした場合処理が受付けられること.
     * batchの実行順
     * １．POST（登録）
     * ２．GET（一覧取得）
     * ３．GET（取得）
     * ４．PUT（更新）
     * ５．DELETE（削除）
     */
    @Test
    public final void 正しい認証情報を使用してすべてのユーザがアクセス可能なコレクションに対して$batchをした場合処理が受付けられること() {

        // 認証トークン取得
        String[] tokens = accountAuth();
        // ※本テストではACLをSetupのデフォルト値から変更しているため、実際はREAD_WRITE権限は持っていない。
        String token = tokens[READ_WRITE];

        // ACL設定
        String path = String.format("%s/%s/%s", TEST_CELL1, BOX_NAME, COL_NAME);
        DavResourceUtils.setACLPrivilegeAllForAllUser(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK, path, "none");

        // READとWRITE→全てOK
        TResponse res = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                token, HttpStatus.SC_ACCEPTED);
        // 期待するレスポンスコード
        int[] expectedCodes = new int[] {HttpStatus.SC_CREATED,
                HttpStatus.SC_OK,
                HttpStatus.SC_OK,
                HttpStatus.SC_NO_CONTENT,
                HttpStatus.SC_NO_CONTENT };
        // レスポンスボディのチェック（ステータス）
        checkBatchResponseBody(res, expectedCodes);

    }

    /**
     * 不正な認証情報を使用してすべてのユーザがアクセス可能なコレクションに対して$batchをした場合処理が受付けられること.
     * batchの実行順
     * １．POST（登録）
     * ２．GET（一覧取得）
     * ３．GET（取得）
     * ４．PUT（更新）
     * ５．DELETE（削除）
     */
    @Test
    public final void 不正な認証情報を使用してすべてのユーザがアクセス可能なコレクションに対して$batchをした場合処理が受付けられること() {

        // 認証トークン取得
        String invalidToken = "invalid token";

        // ACL設定
        String path = String.format("%s/%s/%s", TEST_CELL1, BOX_NAME, COL_NAME);
        DavResourceUtils.setACLPrivilegeAllForAllUser(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK, path, "none");

        // READとWRITE→全てOK
        TResponse res = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                invalidToken, HttpStatus.SC_ACCEPTED);
        // 期待するレスポンスコード
        int[] expectedCodes = new int[] {HttpStatus.SC_CREATED,
                HttpStatus.SC_OK,
                HttpStatus.SC_OK,
                HttpStatus.SC_NO_CONTENT,
                HttpStatus.SC_NO_CONTENT };
        // レスポンスボディのチェック（ステータス）
        checkBatchResponseBody(res, expectedCodes);

    }

    /**
     * 不正な認証情報を使用してすべてのユーザがread可能なコレクションに対して$batchをした場合処理が受付けられること.
     * batchの実行順
     * １．POST（登録）
     * ２．GET（一覧取得）
     * ３．GET（取得）
     * ４．PUT（更新）
     * ５．DELETE（削除）
     */
    @Test
    public final void 不正な認証情報を使用してすべてのユーザがread可能なコレクションに対して$batchをした場合処理が受付けられること() {

        // 認証トークン取得
        String invalidToken = "invalid token";

        // ACL設定
        String path = String.format("%s/%s/%s", TEST_CELL1, BOX_NAME, COL_NAME);
        DavResourceUtils.setACLPrincipalAll(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK,
                path, "<D:read />", "");

        // READ→OK WRITE→403
        TResponse res = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                invalidToken, HttpStatus.SC_ACCEPTED);
        // 期待するレスポンスコード
        int[] expectedCodes = new int[] {HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_OK,
                HttpStatus.SC_NOT_FOUND,
                HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_FORBIDDEN };
        // レスポンスボディのチェック（ステータス）
        checkBatchResponseBody(res, expectedCodes);

    }

    /**
     * 不正な認証情報を使用してすべてのユーザがwrite可能なコレクションに対して$batchをした場合処理が受付けられること.
     * batchの実行順
     * １．POST（登録）
     * ２．GET（一覧取得）
     * ３．GET（取得）
     * ４．PUT（更新）
     * ５．DELETE（削除）
     */
    @Test
    public final void 不正な認証情報を使用してすべてのユーザがwrite可能なコレクションに対して$batchをした場合処理が受付けられること() {
        // 認証トークン取得
        String invalidToken = "invalid token";

        // ACL設定
        String path = String.format("%s/%s/%s", TEST_CELL1, BOX_NAME, COL_NAME);
        DavResourceUtils.setACLPrincipalAll(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK,
                path, "<D:write />", "");

        // READ→OK WRITE→403
        TResponse res = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                invalidToken, HttpStatus.SC_ACCEPTED);
        // 期待するレスポンスコード
        int[] expectedCodes = new int[] {HttpStatus.SC_CREATED,
                HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_NO_CONTENT,
                HttpStatus.SC_NO_CONTENT };
        // レスポンスボディのチェック（ステータス）
        checkBatchResponseBody(res, expectedCodes);
    }

    /**
     * 正しい認証情報を使用してread_write権限があるコレクションに対して$batchをした場合処理が受付けられること.
     * batchの実行順
     * １．POST（登録）
     * ２．GET（一覧取得）
     * ３．GET（取得）
     * ４．PUT（更新）
     * ５．DELETE（削除）
     */
    @Test
    public final void 正しい認証情報を使用してread_write権限があるコレクションに対して$batchをした場合処理が受付けられること() {

        // 認証トークン取得
        String[] tokens = accountAuth();

        // ACL設定
        DavResourceUtils.setACL(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK, COL_NAME, ACL_AUTH_TEST_FILE, BOX_NAME, "");

        // READとWRITE→全てOK
        TResponse res = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                tokens[READ_WRITE], HttpStatus.SC_ACCEPTED);
        // 期待するレスポンスコード
        int[] expectedCodes = new int[] {HttpStatus.SC_CREATED,
                HttpStatus.SC_OK,
                HttpStatus.SC_OK,
                HttpStatus.SC_NO_CONTENT,
                HttpStatus.SC_NO_CONTENT };
        // レスポンスボディのチェック（ステータス）
        checkBatchResponseBody(res, expectedCodes);

    }

    /**
     * 不正な認証情報を使用してすべてのユーザがアクセス可能ではないコレクションに対して$batchをした場合401エラーとなること(all-all以外).
     * batchの実行順
     * １．POST（登録）
     * ２．GET（一覧取得）
     * ３．GET（取得）
     * ４．PUT（更新）
     * ５．DELETE（削除）
     */
    @Test
    public final void 不正な認証情報を使用してすべてのユーザがアクセス可能ではないコレクションに対して$batchをした場合401エラーとなること() {

        // 認証トークン取得
        String invalidToken = "invalid token";

        // ACL設定
        DavResourceUtils.setACL(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK, COL_NAME, ACL_AUTH_TEST_FILE, BOX_NAME, "");

        // READとWRITE→全てOK
        TResponse res = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                invalidToken, HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);
    }

    /**
     * Boxレベル$batchでのACLアクセス制御の確認.
     * batchの実行順
     * １．POST（登録）
     * ２．GET（一覧取得）
     * ３．GET（取得）
     * ４．PUT（更新）
     * ５．DELETE（削除）
     */
    @Test
    public final void 正しい認証情報を使用して権限が無いコレクションに対して$batchをした場合403エラーとなること() {
        // 認証トークン取得
        String[] tokens = accountAuth();

        // ACL設定
        DavResourceUtils.setACL(TEST_CELL1, MASTER_TOKEN, HttpStatus.SC_OK, COL_NAME, ACL_AUTH_TEST_FILE, BOX_NAME, "");

        // Privilege設定なし→$batchリクエストが403
        TResponse res1 = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                tokens[NO_PRIVILEGE], HttpStatus.SC_FORBIDDEN);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res1);

        // READのみ→POST/PUT/DELETEが403
        // READの確認のため１件登録
        String body2 = START_BOUNDARY + retrievePostBody("Supplier", "testAutuBatch1")
                + END_BOUNDARY;
        UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, body2, MASTER_TOKEN, -1);

        TResponse res2 = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                tokens[READ], -1);
        // 期待するレスポンスコード
        int[] expectedCodes2 = new int[] {HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_OK,
                HttpStatus.SC_OK,
                HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_FORBIDDEN };
        // レスポンスボディのチェック（ステータス）
        checkBatchResponseBody(res2, expectedCodes2);
        // テスト用の１件削除
        String body3 = START_BOUNDARY + retrieveDeleteBody("Supplier('testAutuBatch1')")
                + END_BOUNDARY;
        UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, body3, MASTER_TOKEN, -1);

        // WRITEのみ→GETが403
        TResponse res3 = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                tokens[WRITE], HttpStatus.SC_ACCEPTED);
        // 期待するレスポンスコード
        int[] expectedCodes3 = new int[] {HttpStatus.SC_CREATED,
                HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_FORBIDDEN,
                HttpStatus.SC_NO_CONTENT,
                HttpStatus.SC_NO_CONTENT };
        // レスポンスボディのチェック（ステータス）
        checkBatchResponseBody(res3, expectedCodes3);

        // $batchで受け付けない権限のみ→$batchリクエストが403
        TResponse res4 = UserDataUtils.batch(TEST_CELL1, BOX_NAME, COL_NAME, BOUNDARY, TEST_BODY,
                tokens[EXEC], HttpStatus.SC_FORBIDDEN);
        AuthTestCommon.checkAuthenticateHeaderNotExists(res4);

    }

    /**
     * レスポンスボディのチェック.
     * @param res TResponse
     * @param expectedResCodes 期待するレスポンスコード
     */
    private void checkBatchResponseBody(TResponse res, int[] expectedResCodes) {
        String[] arrResBody = res.getBody().split("\n");
        int i = 0;
        for (String bodyLine : arrResBody) {
            if (bodyLine.startsWith("HTTP/1.1")) {
                assertTrue("expected :" + expectedResCodes[i] + " but was :" + bodyLine,
                        bodyLine.indexOf(String.valueOf(expectedResCodes[i])) > -1);
                i++;
            }
        }

    }

    private static String retrieveGetBody(String path) {
        return "Content-Type: application/http\n"
                + "Content-Transfer-Encoding:binary\n\n"
                + "GET " + path + "\n"
                + "Host: host\n\n";
    }

    private static String retrievePostBody(String path, String id) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{\"__id\":\"" + id + "\",\"Name\":\"testName\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    private static String retrieveDeleteBody(String path) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_ADUsdsfNmrFSDsd3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBz\n"
                + "Content-Length: 995\n\n"
                + "--changeset_ADUsdsfNmrFSDsd3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBz\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "DELETE " + path + " HTTP/1.1\n"
                + "Host: \n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n\n"
                + "--changeset_ADUsdsfNmrFSDsd3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBz--\n\n";
    }

    private static String retrieveListBody(String path) {
        return "Content-Type: application/http\n"
                + "Content-Transfer-Encoding:binary\n\n"
                + "GET " + path + "\n"
                + "Host: host\n\n";
    }

    private static String retrievePutBody(String path) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "PUT " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n"
                + "Content-Length: 38\n\n"
                + "{\"Name\":\"testNameUpdated\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    private String[] accountAuth() {
        String[] result = new String[5];
        // account1 アクセス権無し
        result[NO_PRIVILEGE] = ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account1", "password1");
        // account2 読み込みのみ
        result[READ] = ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account2", "password2");
        // account3 書き込みのみ
        result[WRITE] = ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account3", "password3");
        // account4 読み書き
        result[READ_WRITE] = ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account4", "password4");
        // account5 サービスの実行
        result[EXEC] = ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account5", "password5");

        return result;
    }
}
