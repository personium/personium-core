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
package io.personium.test.jersey.cell;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Cell;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * Cell一括削除のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class CellBulkDeletionTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public CellBulkDeletionTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * 全テストの実行前処理.
     */
    @org.junit.Before
    public void Before() {
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除を実行してセルを削除できること() {
        // テスト用セル準備 ---------------------
        String cellName = "cellbulkdeletiontest";
        String boxName = "testBox";
        String davFileName = "testFile";

        // セルを作成する
        CellUtils.create(cellName, MASTER_TOKEN_NAME, -1);

        // イベントログを出力する
        String os = System.getProperty("os.name").toLowerCase();
        // Windowsはプロセスの関係でイベントログが消せない為イベントログを作成しない
        if (!os.contains("windows")) {
            Map<String, String> body = new HashMap<String, String>();
            body.put("level", "INFO");
            body.put("action", "POST");
            body.put("object", "ObjectData");
            body.put("result", "resultData");
            CellUtils.event(MASTER_TOKEN_NAME, -1, cellName, JSONObject.toJSONString(body));
        }

        // ボックスを作成する
        BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, -1);

        // DavFileを作成する
        DavResourceUtils.createWebDavFile(cellName, MASTER_TOKEN_NAME, "box/dav-put.txt",
                "hello world!", boxName, davFileName, -1);
        // テスト用セル準備 ---------------------

        // セルの一括削除APIを実行する
        PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header("X-Personium-Recursive", "true");
        PersoniumResponse response = request(request);

        // セル削除APIを実行して、204が返却されることを確認
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        // スレッドで非同期で削除しているため、3秒スリープする
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("");
        }
        // セルが削除されていることを確認する
        request = PersoniumRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        response = request(request);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時にX_PERSONIUM_Recursiveヘッダを指定しないで412が返却されること() {
        // セルを作成する
        String cellName = "cellbulkdeletiontest";
        CellUtils.create(cellName, MASTER_TOKEN_NAME, -1);

        // セルの一括削除APIを実行する
        PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        PersoniumResponse response = request(request);

        // セル削除APIを実行して、412が返却されることを確認
        try {
            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, PersoniumCoreException.Misc.PRECONDITION_FAILED.getCode(),
                    PersoniumCoreException.Misc.PRECONDITION_FAILED
                            .params(CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE)
                            .getMessage());
        } finally {
            // セルを削除する
            request.header("X-Personium-Recursive", "true");
            request(request);
        }
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時にX_PERSONIUM_Recursiveヘッダにfalseを指定して412が返却されること() {
        // セルを作成する
        String cellName = "cellbulkdeletiontest";
        CellUtils.create(cellName, MASTER_TOKEN_NAME, -1);

        // セルの一括削除APIを実行する
        PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header("X-Personium-Recursive", "false");
        PersoniumResponse response = request(request);

        // セル削除APIを実行して、412が返却されることを確認
        try {
            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, PersoniumCoreException.Misc.PRECONDITION_FAILED.getCode(),
                    PersoniumCoreException.Misc.PRECONDITION_FAILED
                            .params(CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE)
                            .getMessage());
        } finally {
            // セルを削除する
            request.header("X-Personium-Recursive", "true");
            request(request);
        }
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時にユニットユーザの認証トークンを指定してセルが削除できること() {
        // セルを作成する
        String cellName = "cellbulkdeletiontest";
        // マスタートークンでX-Personium-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
        CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET, HttpStatus.SC_CREATED);

        // セルの一括削除APIを実行する（マスタートークンのヘッダ指定での降格を利用）
        PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header("X-Personium-Recursive", "true")
                .header("X-Personium-Unit-User", Setup.OWNER_VET);

        // セル削除APIを実行して、204が返却されることを確認
        PersoniumResponse response = request(request);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        // スレッドで非同期で削除しているため、3秒スリープする
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("");
        }
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時に異なるセルのユニットユーザの認証トークンを指定して403が返却されること() {
        // セルを作成する
        String cellName = "cellbulkdeletiontest";
        // マスタートークンでX-Personium-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
        CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET, HttpStatus.SC_CREATED);

        try {
            // セルの一括削除APIを実行する（マスタートークンのヘッダ指定での降格を利用）
            PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                    .header("X-Personium-Recursive", "true")
                    .header("X-Personium-Unit-User", Setup.OWNER_HMC);
            PersoniumResponse response = request(request);
            assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusCode());
        } finally {
            // セルを削除する
            PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                    .header("X-Personium-Recursive", "true");
            request(request);
        }
    }

    /**
     * .
     */
    @Test
    @Ignore // UUT promotion setting API invalidation.
    public final void セル一括削除時にユニットローカルユニットユーザの認証トークンを指定してセルが削除できること() {
        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", -1);

        // パスワード認証でのユニット昇格
        TResponse res = Http.request("authnUnit/password-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .returns();

        // セルを作成する
        String cellName = "cellbulkdeletiontest";
        try {
            // トークンの中身の取得・検証
            JSONObject json = res.bodyAsJson();
            String uluutString = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
            UnitLocalUnitUserToken uluut;
            uluut = UnitLocalUnitUserToken.parse(uluutString, UrlUtils.getHost());
            assertEquals(Setup.OWNER_VET, uluut.getSubject());
            assertEquals(UrlUtils.getHost(), uluut.getIssuer());

            // マスタートークンでX-Personium-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
            CellUtils.create(cellName, uluutString, -1);

            // セルの一括削除APIを実行する
            PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + uluutString)
                    .header("X-Personium-Recursive", "true");
            PersoniumResponse response = request(request);
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        } catch (TokenParseException e) {
            e.printStackTrace();
        } finally {
            // セルを削除する
            PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                    .header("X-Personium-Recursive", "true");
            request(request);
        }
    }

}
