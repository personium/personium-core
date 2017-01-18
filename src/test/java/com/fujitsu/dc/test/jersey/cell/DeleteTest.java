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
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRestAdapter;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.ExtCellUtils;
import com.fujitsu.dc.test.utils.ExtRoleUtils;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.SentMessageUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * JerseyTestFrameworkを利用したユニットテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class DeleteTest extends ODataCommon {
    /** テスト用Cellの情報. */
    private TestCellInfo cellInfo = new TestCellInfo();

    /** テスト用Cellの情報を保持する. */
    private class TestCellInfo {
        /** テストCell名 . */
        private String cellName = null;
        /** テストCell ID. */
        private String cellId = null;
        /** テストCell レスポンス. */
        private DcResponse resCell;
        /** テストBox名 . */
        private String boxName = null;

        void initial() {
            cellId = null;
            cellName = null;
            resCell = null;
            boxName = null;
        }
    }

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public DeleteTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    private void cellCreate() {
        // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
        this.cellInfo.cellName = "cell_name" + Long.toString(Calendar.getInstance().getTimeInMillis());

        // Cellを作成
        this.cellInfo.resCell = createCell(cellInfo.cellName);

        // LOCATIONヘッダを取得
        Header[] resHeaders = this.cellInfo.resCell.getResponseHeaders(HttpHeaders.LOCATION);

        // 作成したCellのIDを抽出
        this.cellInfo.cellId = resHeaders[0].getValue().split("'")[1];

        return;
    }

    private void boxCreate() {
        // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
        this.cellInfo.boxName = "box_name" + Long.toString(Calendar.getInstance().getTimeInMillis());

        // Boxを作成
        createBox(this.cellInfo.cellName, this.cellInfo.boxName);

        return;
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @Before
    public final void initCell() {
        cellCreate();
    }

    /**
     * testの後に実行する.
     */
    @After
    public final void afterCell() {
        DcResponse res = null;
        if (this.cellInfo.cellId != null) {
            if (this.cellInfo.boxName != null) {
                // テストBox 削除
                res = restDelete(UrlUtils.cellCtl(this.cellInfo.cellName, Box.EDM_TYPE_NAME, this.cellInfo.boxName));
            }
            // テストCell 削除
            res = restDelete(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId));
            // レスポンスコードのチェック
            if ((res.getStatusCode() == HttpStatus.SC_NO_CONTENT)
                    || (res.getStatusCode() == HttpStatus.SC_OK)) {
                this.cellInfo.initial();
            }
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        }
    }

    /**
     * cell_Id ： 正常ID.
     */
    @Test
    public final void Cell削除の正常系のテスト() {
        // Cellを削除
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        requestheaders.put(HttpHeaders.IF_MATCH, "*");
        try {
            // リクエスト
            // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
            res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // レスポンスコードのチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        this.cellInfo.initial();
    }

    /**
     * Cell名にクオート無しの数値型式名を指定した場合400エラーとなること.
     */
    @Test
    public final void Cell名にクオート無しの数値型式名を指定した場合400エラーとなること() {
        // $format なし
        // Acceptヘッダ なし
        String cellName = "123456";
        String url = getUrlWithOutQuote(cellName, null);
        DcResponse res = this.restDelete(url);

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        checkErrorResponse(res.bodyAsJson(),
                DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
    }

    /**
     * cell_Id ： 存在しないCell_Id.
     */
    @Test
    public final void Cell削除の存在しないCellId指定のテスト() {
        // Cellを削除
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        requestheaders.put(HttpHeaders.IF_MATCH, "*");
        try {
            // リクエスト
            // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
            res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, "Illegal" + this.cellInfo.cellId), requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // レスポンスコードのチェック
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        // レスポンスヘッダ Content-Typeのチェック
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        // レスポンスボディのパース
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * リクエストクエリ ： $select=name.
     */
    @Test
    public final void Cellの削除のQuery無視のテスト() {
        // CellのURL文字列を生成
        // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
        StringBuilder cellUrl =
                new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId));
        cellUrl.append("?$select=name");

        // Cellを削除
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        requestheaders.put(HttpHeaders.IF_MATCH, "*");
        try {
            // リクエスト
            res = rest.del(cellUrl.toString(), requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // レスポンスコードのチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        this.cellInfo.initial();
    }

    /**
     * リクエストヘッダ－Authorization ： 不正文字列.
     */
    @Test
    public final void Cell削除の不正Authorizationのテスト() {
        // Cellを削除
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Illegal Authorization");
        requestheaders.put(HttpHeaders.IF_MATCH, "*");
        try {
            // リクエスト
            // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
            res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // レスポンスコードのチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
    }

    /**
     * CellにBoxが存在する.
     */
    @Test
    public final void Cell削除のBoxが存在する時のテスト() {
        // Box作成
        this.boxCreate();

        // Cellを削除
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        requestheaders.put(HttpHeaders.IF_MATCH, "*");
        try {
            // リクエスト
            // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
            res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // レスポンスコードのチェック
        assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
    }

    /**
     * CellにAccountが存在する.
     */
    @Test
    public final void Cell削除のAccountが存在する時のテスト() {

        String userName = "hogehuga";
        String pass = "password";

        try {
            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, this.cellInfo.cellName,
                    userName, pass, HttpStatus.SC_CREATED);

            // Cellを削除
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestheaders.put(HttpHeaders.IF_MATCH, "*");
            try {
                // リクエスト
                // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
                res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            // レスポンスコードのチェック
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());

            // Account削除
            AccountUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME,
                    userName, HttpStatus.SC_NO_CONTENT);

            // Cellを削除（Account削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, HttpStatus.SC_NO_CONTENT);

        } finally {
            // Cell作成
            CellUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, -1);
            // Account削除
            AccountUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME, userName, -1);
        }
    }

    /**
     * Cell削除のMessageが存在する時のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell削除のMessageが存在する時のテスト() {

        // 送信先CellUrl
        String receivedCell = "receivedCell";

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(receivedCell));
        body.put("ToRelation", null);
        body.put("Type", "message");
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);
        TResponse response = null;

        try {
            createCell(receivedCell);
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, this.cellInfo.cellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // Cellを削除（SentMessage未削除のため、削除不可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, HttpStatus.SC_CONFLICT);

            // Cellを削除（ReceivedMessage未削除のため、削除不可能）
            CellUtils.delete(MASTER_TOKEN_NAME, receivedCell, HttpStatus.SC_CONFLICT);

            deleteOdataResource(response.getLocationHeader());
            // 自動生成された受信メッセージの削除
            MessageSentTest.deleteReceivedMessage(receivedCell, UrlUtils.cellRoot(this.cellInfo.cellName), "message",
                    "title", "body");

            // Cellを削除（SentMessage削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, HttpStatus.SC_NO_CONTENT);
            // Cellを削除（ReceivedMessage削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, receivedCell, HttpStatus.SC_NO_CONTENT);

        } finally {
            // Cellを削除（SentMessage削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, -1);
            // Cellを削除（ReceivedMessage削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, receivedCell, -1);
            // Cell作成
            CellUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * CellにRoleが存在する.
     */
    @Test
    public final void Cell削除のRoleが存在する時のテスト() {

        String roleName = "rolehoge";

        try {
            // Role作成
            RoleUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, null, roleName, HttpStatus.SC_CREATED);

            // Cellを削除
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestheaders.put(HttpHeaders.IF_MATCH, "*");
            try {
                // リクエスト
                // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
                res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            // レスポンスコードのチェック
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());

            // Role削除
            RoleUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME, null,
                    roleName, HttpStatus.SC_NO_CONTENT);

            // Cellを削除（Role削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellId, HttpStatus.SC_NO_CONTENT);

        } finally {
            // Cell作成
            CellUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, -1);
            // Role削除
            RoleUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME, null,
                    roleName, -1);
        }
    }

    /**
     * CellにExtCellが存在する.
     */
    @Test
    public final void Cell削除のExtCellが存在する時のテスト() {

        String extCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);

        try {
            // ExtCell作成
            ExtCellUtils.create(MASTER_TOKEN_NAME, this.cellInfo.cellName, extCellUrl,
                    HttpStatus.SC_CREATED);

            // Cellを削除
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestheaders.put(HttpHeaders.IF_MATCH, "*");
            try {
                // リクエスト
                // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
                res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            // レスポンスコードのチェック
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());

            // ExtCell削除
            ExtCellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, extCellUrl,
                    HttpStatus.SC_NO_CONTENT);

            // Cellを削除（Role削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellId, HttpStatus.SC_NO_CONTENT);

        } finally {
            // Cell作成
            CellUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, -1);
            // ExtCell削除
            ExtCellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, extCellUrl, -1);
        }
    }

    /**
     * CellにExtRoleが存在する.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell削除のExtRoleが存在する時のテスト() {

        String extRoleName = UrlUtils.roleResource(this.cellInfo.cellName, "__", "extRolehoge");
        JSONObject extRoleBody = new JSONObject();
        extRoleBody.put("ExtRole", extRoleName);
        extRoleBody.put("_Relation.Name", "relation");
        extRoleBody.put("_Relation._Box.Name", null);

        JSONObject relationBody = new JSONObject();
        relationBody.put("Name", "relation");
        relationBody.put("_Box.Name", null);

        try {
            // Relation作成
            RelationUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME,
                    relationBody, HttpStatus.SC_CREATED);

            // ExtRole作成
            ExtRoleUtils.create(MASTER_TOKEN_NAME, this.cellInfo.cellName, extRoleBody,
                    HttpStatus.SC_CREATED);

            // Cellを削除
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestheaders.put(HttpHeaders.IF_MATCH, "*");
            try {
                // リクエスト
                // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
                res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            // レスポンスコードのチェック
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());

            // ExtRole削除
            ExtRoleUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, extRoleName,
                    "'relation'", "null", HttpStatus.SC_NO_CONTENT);

            // Relation削除
            RelationUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME, "relation",
                    null, HttpStatus.SC_NO_CONTENT);

            // Cellを削除（Role削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellId, HttpStatus.SC_NO_CONTENT);

        } finally {
            // Cell作成
            CellUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, -1);
            // ExtRole削除
            ExtRoleUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellName, extRoleName,
                    "'relation'", "null", -1);
            // Relation削除
            RelationUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME, "relation", null, -1);
        }
    }

    /**
     * CellにRelationが存在する.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell削除のRelationが存在する時のテスト() {

        String relationName = "relationhoge";
        JSONObject body = new JSONObject();
        body.put("Name", relationName);
        body.put("_Box.Name", null);
        try {
            // ExtRole作成
            RelationUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, body, HttpStatus.SC_CREATED);

            // Cellを削除
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestheaders.put(HttpHeaders.IF_MATCH, "*");
            try {
                // リクエスト
                // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
                res = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, this.cellInfo.cellId), requestheaders);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            // レスポンスコードのチェック
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());

            // Relation削除
            RelationUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME, relationName, null,
                    HttpStatus.SC_NO_CONTENT);

            // Cellを削除（Role削除済のため、削除可能）
            CellUtils.delete(MASTER_TOKEN_NAME, this.cellInfo.cellId, HttpStatus.SC_NO_CONTENT);

        } finally {
            // Cell作成
            CellUtils.create(this.cellInfo.cellName, MASTER_TOKEN_NAME, -1);
            // Relation削除
            RelationUtils.delete(this.cellInfo.cellName, MASTER_TOKEN_NAME, relationName, null, -1);
        }
    }
}
