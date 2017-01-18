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
package com.fujitsu.dc.test.jersey.cell;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token.TokenParseException;
import com.fujitsu.dc.common.auth.token.UnitLocalUnitUserToken;
import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * Cell一括削除のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class CellBulkDeletionTest extends AbstractCase {

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public CellBulkDeletionTest() {
        super(new WebAppDescriptor.Builder(CellBulkDeletionTest.INIT_PARAMS).build());
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
        String cellName = "CellBulkDeletionTest";
        String boxName = "testBox";
        String davFileName = "testFile";

        // セルを作成する
        CellUtils.create(cellName, MASTER_TOKEN_NAME, -1);

        // イベントログを出力する
        Map<String, String> body = new HashMap<String, String>();
        body.put("level", "INFO");
        body.put("action", "POST");
        body.put("object", "ObjectData");
        body.put("result", "resultData");
        CellUtils.event(MASTER_TOKEN_NAME, -1, cellName, JSONObject.toJSONString(body));

        // ボックスを作成する
        BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, -1);

        // DavFileを作成する
        DavResourceUtils.createWebDavFile(cellName, MASTER_TOKEN_NAME, "box/dav-put.txt",
                "hello world!", boxName, davFileName, -1);
        // テスト用セル準備 ---------------------

        // セルの一括削除APIを実行する
        DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header("X-Dc-Recursive", "true");
        DcResponse response = request(request);

        // セル削除APIを実行して、204が返却されることを確認
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        // スレッドで非同期で削除しているため、3秒スリープする
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("");
        }
        // セルが削除されていることを確認する
        request = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        response = request(request);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時にX_Dc_Recursiveヘッダを指定しないで412が返却されること() {
        // セルを作成する
        String cellName = "CellBulkDeletionTest";
        CellUtils.create(cellName, MASTER_TOKEN_NAME, -1);

        // セルの一括削除APIを実行する
        DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse response = request(request);

        // セル削除APIを実行して、412が返却されることを確認
        try {
            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.Misc.PRECONDITION_FAILED.getCode(),
                    DcCoreException.Misc.PRECONDITION_FAILED
                            .params(DcCoreUtils.HttpHeaders.X_DC_RECURSIVE)
                            .getMessage());
        } finally {
            // セルを削除する
            request.header("X-Dc-Recursive", "true");
            request(request);
        }
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時にX_Dc_Recursiveヘッダにfalseを指定して412が返却されること() {
        // セルを作成する
        String cellName = "CellBulkDeletionTest";
        CellUtils.create(cellName, MASTER_TOKEN_NAME, -1);

        // セルの一括削除APIを実行する
        DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header("X-Dc-Recursive", "false");
        DcResponse response = request(request);

        // セル削除APIを実行して、412が返却されることを確認
        try {
            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.Misc.PRECONDITION_FAILED.getCode(),
                    DcCoreException.Misc.PRECONDITION_FAILED
                            .params(DcCoreUtils.HttpHeaders.X_DC_RECURSIVE)
                            .getMessage());
        } finally {
            // セルを削除する
            request.header("X-Dc-Recursive", "true");
            request(request);
        }
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時にユニットユーザの認証トークンを指定してセルが削除できること() {
        // セルを作成する
        String cellName = "CellBulkDeletionTest";
        // マスタートークンでX-Dc-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
        CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET, HttpStatus.SC_CREATED);

        // セルの一括削除APIを実行する（マスタートークンのヘッダ指定での降格を利用）
        DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header("X-Dc-Recursive", "true")
                .header("X-Dc-Unit-User", Setup.OWNER_VET);

        // セル削除APIを実行して、204が返却されることを確認
        DcResponse response = request(request);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
    }

    /**
     * .
     */
    @Test
    public final void セル一括削除時に異なるセルのユニットユーザの認証トークンを指定して403が返却されること() {
        // セルを作成する
        String cellName = "CellBulkDeletionTest";
        // マスタートークンでX-Dc-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
        CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET, HttpStatus.SC_CREATED);

        try {
            // セルの一括削除APIを実行する（マスタートークンのヘッダ指定での降格を利用）
            DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                    .header("X-Dc-Recursive", "true")
                    .header("X-Dc-Unit-User", Setup.OWNER_HMC);
            DcResponse response = request(request);
            assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusCode());
        } finally {
            // セルを削除する
            DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                    .header("X-Dc-Recursive", "true");
            request(request);
        }
    }

    /**
     * .
     */
    @Test
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
        String cellName = "CellBulkDeletionTest";
        try {
            // トークンの中身の取得・検証
            JSONObject json = res.bodyAsJson();
            String uluutString = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
            UnitLocalUnitUserToken uluut;
            uluut = UnitLocalUnitUserToken.parse(uluutString, UrlUtils.getHost());
            assertEquals(Setup.OWNER_VET, uluut.getSubject());
            assertEquals(UrlUtils.getHost(), uluut.getIssuer());

            // マスタートークンでX-Dc-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
            CellUtils.create(cellName, uluutString, -1);

            // セルの一括削除APIを実行する
            DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + uluutString)
                    .header("X-Dc-Recursive", "true");
            DcResponse response = request(request);
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        } catch (TokenParseException e) {
            e.printStackTrace();
        } finally {
            // セルを削除する
            DcRequest request = DcRequest.delete(UrlUtils.cellRoot(cellName));
            request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                    .header("X-Dc-Recursive", "true");
            request(request);
        }
    }

}
