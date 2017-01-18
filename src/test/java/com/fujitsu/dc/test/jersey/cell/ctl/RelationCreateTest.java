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
package com.fujitsu.dc.test.jersey.cell.ctl;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * Relationの登録のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RelationCreateTest extends ODataCommon {

    private static final String RELATION_TYPE = "CellCtl.Relation";
    private static String cellName = "testcell1";
    private static String testRelationName = "testrelation";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RelationCreateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Relation新規登録のテスト Name, _Box.Name指定あり.
     */
    @Test
    public void Relation登録の正常系ボックス指定ありのテスト() {
        String boxname = "box1";
        try {
            createRelation(boxname);
        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName, boxname);
        }
    }

    /**
     * Relation新規登録のテスト _Box.Nameにnull指定.
     */
    @Test
    public void Relation登録の正常系ボックス指定なしのテスト() {
        try {
            createRelationAndCheckResponse(false);
        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName);
        }
    }

    /**
     * Relation新規登録のテスト _Box.Name指定なし.
     */
    @Test
    public void Relation登録のボックス名キー指定なしのテスト() {
        try {
            createRelationAndCheckResponse(true);
            // TODO スキーマチェック実装後は400とする
        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName);
        }
    }

    /**
     * Relation新規登録のテスト_リクエストボディに管理情報__publishedを指定した場合400エラーとなる確認.
     */
    @Test
    public void Relation新規登録のテスト_リクエストボディに管理情報__publishedを指定した場合400エラーとなる確認() {
        errCreateRelation("box1", PUBLISHED, "/Date(0)/", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Relation新規登録のテスト_リクエストボディに管理情報__updatedを指定した場合400エラーとなる確認.
     */
    @Test
    public void Relation新規登録のテスト_リクエストボディに管理情報__updatedを指定した場合400エラーとなる確認() {
        errCreateRelation("box1", UPDATED, "/Date(0)/", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Relation新規登録のテスト_リクエストボディに管理情報__metadataを指定した場合400エラーとなる確認.
     */
    @Test
    public void Relation新規登録のテスト_リクエストボディに管理情報__metadataを指定した場合400エラーとなる確認() {
        errCreateRelation("box1", METADATA, null, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Relation新規登録時Nameに空文字を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Nameに空文字を指定した場合400になること() {
        String relationName = "";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Nameにアンダーバー始まりの文字列を指定した場合400になること() {
        String relationName = "_testRelation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameにコロン始まりの文字列を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Nameにコロン始まりの文字列を指定した場合400になること() {
        String relationName = ":testRelation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameにハイフン始まりの文字列を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameにハイフン始まりの文字列を指定した場合201になること() {
        String relationName = "-testRelation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameにプラス始まりの文字列を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameにプラス始まりの文字列を指定した場合201になること() {
        String relationName = "+testRelation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに先頭＋でコロンを含む文字列を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameに先頭プラスでコロンを含む文字列を指定した場合201になること() {
        String relationName = "+:keeps";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに先頭－でコロンを含む文字列を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameに先頭マイナスでコロンを含む文字列を指定した場合201になること() {
        String relationName = "-:keeps";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに+を含む文字列を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameにプラスを含む文字列を指定した場合201になること() {
        String relationName = "test+Relation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに-を含む文字列を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameにマイナスを含む文字列を指定した場合201になること() {
        String relationName = "test-Relation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに_を含む文字列を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameにアンダーバーを含む文字列を指定した場合201になること() {
        String relationName = "test_Relation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameにスラッシュを含む文字列を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Nameにスラッシュを含む文字列を指定した場合400になること() {
        String relationName = "test/Relation";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに__ctlを指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Nameに__ctlを指定した場合400になること() {
        String relationName = "__ctl";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに指定可能な文字数の最小値を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameに指定可能な文字数の最小値を指定した場合201になること() {
        String relationName = "1";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに指定可能な文字数の最大値を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Nameに指定可能な文字数の最大値を指定した場合201になること() {
        String relationName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに指定可能な文字数の最大値をオーバー指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Nameに指定可能な文字数の最大値をオーバー指定した場合400になること() {
        String relationName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時Nameに日本語を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Nameに日本語を指定した場合400になること() {
        String relationName = "日本語";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameに空文字を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Box名に空文字を指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Box名にアンダーバー始まりの文字列を指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "_boxname";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameにハイフン始まりの文字列を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Box名にハイフン始まりの文字列を指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "-boxname";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameにスラッシュを含む文字列を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Box名にスラッシュを含む文字列を指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "box/name";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameに__ctlを指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Box名に__ctlを指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "__ctl";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameに指定可能な文字数の最小値を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Box名に指定可能な文字数の最小値を指定した場合201になること() {
        String relationName = testRelationName;
        String boxname = "1";
        String locationHeader = null;
        String locationHeaderBox = null;

        try {
            // Box作成
            DcResponse resBox = createBox(boxname);
            locationHeaderBox = resBox.getFirstHeader(HttpHeaders.LOCATION);

            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
            if (locationHeaderBox != null) {
                CellCtlUtils.deleteOdataResource(locationHeaderBox);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameに指定可能な文字数の最大値を指定した場合201になること.
     */
    @Test
    public void Relation新規登録時Box名に指定可能な文字数の最大値を指定した場合201になること() {
        String relationName = testRelationName;
        String boxname = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String locationHeader = null;
        String locationHeaderBox = null;

        try {
            // Box作成
            DcResponse resBox = createBox(boxname);
            locationHeaderBox = resBox.getFirstHeader(HttpHeaders.LOCATION);

            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
            if (locationHeaderBox != null) {
                CellCtlUtils.deleteOdataResource(locationHeaderBox);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameに指定可能な文字数の最大値をオーバー指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Box名に指定可能な文字数の最大値をオーバー指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String locationHeader = null;
        String locationHeaderBox = null;

        try {
            // Box作成
            DcResponse resBox = createBox(boxname);
            locationHeaderBox = resBox.getFirstHeader(HttpHeaders.LOCATION);

            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
            if (locationHeaderBox != null) {
                CellCtlUtils.deleteOdataResource(locationHeaderBox);
            }
        }
    }

    /**
     * Relation新規登録時_Box.Nameに日本語を指定した場合400になること.
     */
    @Test
    public void Relation新規登録時Box名に日本語を指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "日本語";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Relation新規登録時に_Box.Nameに存在しないBox名を指定した場合400になること.
     */
    @Test
    public final void Relation新規登録時に存在しないBox名を指定した場合400になること() {
        String relationName = testRelationName;
        String boxname = "dummy";
        String locationHeader = null;

        try {
            TResponse res = createRelation(relationName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);

            // メッセージ確認
            ODataCommon.checkErrorResponseBody(res,
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(boxname).getMessage());
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * BoxからNP経由でRelationを作成した場合201になること.
     */
    @Test
    public final void BoxからNP経由でRelationを作成した場合201になること() {
        String relationName = "npRelation";

        try {
            RelationUtils.createViaNP(Setup.TEST_CELL1, Setup.MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    relationName, HttpStatus.SC_CREATED);
        } finally {
           RelationUtils.delete(Setup.TEST_CELL1, Setup.MASTER_TOKEN_NAME, relationName, Setup.TEST_BOX1, -1);
        }
    }


    /**
     * 指定されたボックス名にリンクされたRelationを作成する.
     * @param boxname
     */
    @SuppressWarnings("unchecked")
    private void createRelation(String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", testRelationName);
        body.put("_Box.Name", boxname);

        TResponse response = RelationUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                body, HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Relation",
                "Name='" + testRelationName + "',_Box.Name='" + boxname + "'");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRelationName);
        additional.put("_Box.Name", boxname);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

    }

    /**
     * 指定されたボックス名にリンクされたRelationを作成する(エラー).
     * @param boxname
     * @param errKey エラーキー
     * @param errValue エラー値
     * @param errSC 期待するエラーステータスコード
     */
    @SuppressWarnings("unchecked")
    private void errCreateRelation(String boxname, String errKey, String errValue, int errSC) {
        JSONObject body = new JSONObject();
        body.put("Name", testRelationName);
        body.put("_Box.Name", boxname);
        body.put(errKey, errValue);

        RelationUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                body, errSC);
    }

    /**
     * 指定されたボックス名にリンクされたRelationを作成する.
     * @param relationName
     * @param boxname
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    private TResponse createRelation(String relationName, String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", relationName);
        body.put("_Box.Name", boxname);

        return Http.request("relation-create.txt")
                .with("token", "Bearer " + AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .debug();
    }

    /**
     * ボックス名にリンクされていないRelationを作成する.
     * @param boxNameEmpty _Box.Nameを指定しない
     */
    @SuppressWarnings("unchecked")
    private void createRelationAndCheckResponse(boolean boxNameEmpty) {
        JSONObject body = new JSONObject();
        body.put("Name", testRelationName);
        if (!boxNameEmpty) {
            body.put("_Box.Name", null);
        }

        TResponse response = RelationUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                body, HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Relation", "Name='"
                + testRelationName + "',_Box.Name=null");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRelationName);
        additional.put("_Box.Name", null);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

    }

    private DcResponse createBox(String boxname) {
        DcRequest req = DcRequest.post(UrlUtils.cellCtl(cellName, Box.EDM_TYPE_NAME));
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", boxname);
        return request(req);
    }
}
