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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Calendar;

import javax.ws.rs.core.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Cellの作成のIT.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ReadListTest extends ODataCommon {

    /** 作成するセルの個数のデフォルト値. */
    private static final int DEF_CELL_NUM = 3;

    /** テストに使用するCellのId. */
    private ArrayList<String> cellIdList = new ArrayList<String>();

    /** テストに使用するCell名のプレフィックス. */
    private static final String CELL_NAME_PREFIX = "cellname";

    /** テストに使用するCell名のリスト. */
    private String[] cellNameList;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ReadListTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        // cellIdListのクリア
        cellIdList.clear();

    }

    private void cellCreate(int num) {

        cellNameList = new String[num];

        for (int i = 0; i < num; i++) {
            // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
            cellNameList[i] = CELL_NAME_PREFIX + i + Long.toString(Calendar.getInstance().getTimeInMillis());

            // Cellを作成
            DcResponse res;
            res = createCell(cellNameList[i]);

            // Cell作成のレスポンスチェック
            // 201になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
            // LOCATIONヘッダを取得
            Header[] resHeaders = res.getResponseHeaders(HttpHeaders.LOCATION);
            // レスポンスヘッダにLOCATIONが含まれているかチェック
            assertNotNull(resHeaders);
            // LOCATIONヘッダが複数存在する場合もNGとする
            assertEquals(1, resHeaders.length);
            // 作成したCellのIDを抽出
            this.cellIdList.add(resHeaders[0].getValue().split("'")[1]);
        }
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
        this.cellDeleteList(cellIdList);
    }

    /**
     * Cellの一覧取得の正常系のテスト.
     */
    @Test
    public final void Cellの一覧取得の正常系のテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        req.header("Accept", MediaType.APPLICATION_JSON);
        this.cellListNormal(req);
    }

    /**
     * Cellの一覧取得のAcceptヘッダ無しのテスト.
     */
    @Test
    public final void Cellの一覧取得のAcceptヘッダ無しのテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        this.cellListNormalXml(req);
    }

    /**
     * Cellの一覧取得でAcceptヘッダにATOM_XMLを指定した場合XML形式で返却されること.
     * Acceptにatom/xmlを指定する
     */
    @Test
    public final void Cellの一覧取得でAcceptヘッダにATOM_XMLを指定した場合XML形式で返却されること() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        // 制限によりATOM_XMLを指定してもjson形式で返却される.
        req.header("Accept", MediaType.APPLICATION_ATOM_XML);
        this.cellListNormalXml(req);
    }

    /**
     * Cellの一覧取得のAcceptを不正値指定するテスト.
     * Acceptにatom/xmlを指定する
     */
    @Test
    public final void Cellの一覧取得のAcceptを不正値指定するテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        req.header("Accept", MediaType.APPLICATION_OCTET_STREAM);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse res = request(req);

        // 未対応のAcceptを指定した場合はUnsupportedMediaType
        assertEquals(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, res.getStatusCode());
    }

    /**
     * Cellの一覧取得のQueryを無視するのテスト.
     */
    @Test
    public final void Cellの一覧取得のQueryを無視するのテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        req.header("Accept", MediaType.APPLICATION_JSON);
        // 制限としているQueryを指定しても無視される事
        req.query("$skiptoken=13S35K");
        this.cellListNormal(req);
    }

    /**
     * Cellの一覧取得の不正なQueryを無視するのテスト.
     */
    @Test
    public final void Cellの一覧取得の不正なQueryを無視するのテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        req.header("Accept", MediaType.APPLICATION_JSON);
        // 不正なQueryを指定しても無視される事
        req.query("query=test");
        this.cellListNormal(req);
    }

    /**
     * Cellの一覧取得の不正なメソッドPUTのテスト.
     */
    @Test
    public final void Cellの一覧取得の不正なメソッドPUTのテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.put(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        this.cellErrorInvalidMethod(req);
    }

    /**
     * Cellの一覧取得のAuthorizationヘッダ無しのテスト.
     */
    @Test
    public final void Cellの一覧取得のAuthorizationヘッダ無しのテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        this.cellErrorAuthNone(req);
    }

    /**
     * Cellの一覧取得のAuthorizationヘッダが不正なパターンのテスト.
     */
    @Test
    public final void Cellの一覧取得のAuthorizationヘッダが不正なパターンのテスト() {
        cellCreate(DEF_CELL_NUM);
        DcRequest req = DcRequest.get(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        this.cellErrorAuthInvalid(req);
    }
}
