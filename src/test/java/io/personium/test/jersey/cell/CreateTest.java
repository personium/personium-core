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

import java.util.Calendar;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.model.Cell;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Cellの作成のIT.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class CreateTest extends ODataCommon {

    private static String cellNameLower = "cellname";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public CreateTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
        // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
        cellNameLower = cellNameLower + Long.toString(Calendar.getInstance().getTimeInMillis());
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        super.setResponse(null);
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
        super.cellDelete();
    }

    /**
     * Cellの作成の正常系のテスト.
     */
    @Test
    public final void Cellの作成の正常系のテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellNormal(req, cellNameLower);
    }

    /**
     * 登録したCellを削除後すぐに再登録するテスト.
     */
    @Test
    public final void 登録したCellを削除後すぐに再登録するテスト() {
        // Cellを作成
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        PersoniumResponse res = cellNormalResponse(req, cellNameLower);
        // Cell削除
        cellDelete(res);
        // Cellを再作成
        cellNormal(req, cellNameLower);
    }

    /**
     * Cellの作成のリクエストボディが無いパターンのテスト.
     */
    @Test
    public final void Cellの作成のリクエストボディが無いパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorEmptyBody(req);
    }

    /**
     * Cellの作成のリクエストボディに不正なフィールド名を指定したパターンのテスト.
     */
    @Test
    public final void Cellの作成のリクエストボディに不正なフィールド名を指定したパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorInvalidField(req, cellNameLower);
    }

    /**
     * Cellの作成のNameが空のパターンのテスト.
     */
    @Test
    public final void Cellの作成のNameが空のパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorName0(req);
    }

    /**
     * Cellの作成のNameが1文字のパターンのテスト.
     */
    @Test
    public final void Cellの作成のNameが1文字のパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellName1(req);
    }

    /**
     * Cellの作成のNameが128文字のパターンのテスト.
     */
    @Test
    public final void Cellの作成のNameが128文字のパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellName128(req);
    }

    /**
     * Cellの作成のNameが129文字のパターンのテスト.
     */
    @Test
    public final void Cellの作成のNameが129文字のパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorName129(req);
    }

    /**
     * Cellの作成のNameが"A～Z"のパターンのテスト.
     */
    @Test
    public final void Cellの作成のNameが大文字のパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorNameUpperCase(req);
    }

    /**
     * Cellの作成のNameが半角英数と"-","_"以外のパターンのテスト.
     */
    @Test
    public final void Cellの作成のNameが不正な値のパターンのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorNameCharacter(req);
    }

    /**
     * Cellの作成のNameが[__]の場合に４００が返却されること.
     */
    @Test
    public final void Cellの作成のNameが__の場合に４００が返却されること() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorNameUnderbaer(req);
    }

    /**
     * Cellの作成のNameが[__ctl]の場合に４００が返却されること.
     */
    @Test
    public final void Cellの作成のNameが__ctlの場合に４００が返却されること() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorNameUnderbaerCtl(req);
    }

    /**
     * Cell作成のリクエストボディに管理情報[__published]が指定された場合に４００が返却されること.
     */
    @Test
    public final void Cell作成のリクエストボディに管理情報__publishedが指定された場合に４００が返却されること() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorBodyDateCtl(req, PUBLISHED);
    }

    /**
     * Cell作成のリクエストボディに管理情報[__updated]が指定された場合に４００が返却されること.
     */
    @Test
    public final void Cell作成のリクエストボディに管理情報__updatedが指定された場合に４００が返却されること() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorBodyDateCtl(req, UPDATED);
    }

    /**
     * Cell作成のリクエストボディに管理情報[__metadata]が指定された場合に４００が返却されること.
     */
    @Test
    public final void Cell作成のリクエストボディに管理情報__metadataが指定された場合に４００が返却されること() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorBodyMetadataCtl(req, METADATA);
    }

    /**
     * Cellの作成のJSONフォーマットエラーのテスト.
     */
    @Test
    public final void Cellの作成のJSONフォーマットエラーのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorInvalidJson(req, cellNameLower);
    }

    /**
     * Cellの作成のXMLフォーマットエラーのテスト.
     */
    @Test
    public final void Cellの作成のXMLフォーマットエラーのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorInvalidXml(req, cellNameLower);
    }

    /**
     * Cellの作成の認証ヘッダ無しのテスト.
     */
    @Test
    public final void Cellの作成の認証ヘッダ無しのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorAuthNone(req);
    }

    /**
     * Cellの作成の不正な認証ヘッダのテスト.
     */
    @Test
    public final void Cellの作成の不正な認証ヘッダのテスト() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorAuthInvalid(req);
    }

    /**
     * Cellの作成の不正なメソッドのテスト.
     */
    @Test
    public final void Cellの作成の不正なメソッドのテスト() {
        PersoniumRequest req = PersoniumRequest.delete(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellErrorInvalidMethod(req, cellNameLower);
    }

    /**
     * Cellの作成で小文字のCell名を登録した後に同じ小文字のCell名を登録した場合、409となることを確認するテスト.
     */
    @Test
    public final void Cellの作成で小文字のCell名を登録した後に同じ小文字のCell名を登録した場合_409となることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellConflict(req, cellNameLower);
    }

    /**
     * 小文字Cell名で作成したCellに対してCellレベルAPIが利用できること.
     */
    @Test
    public final void 小文字Cell名で作成したCellに対してCellレベルAPIが利用できること() {
        // 小文字Cell名のCellを作成
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        cellNormal(req, cellNameLower);

        // 小文字Cell名を指定してBoxの一覧取得を実行
        req = PersoniumRequest.get(UrlUtils.cellCtl(cellNameLower, "Box"));
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        PersoniumResponse response = request(req);

        // 200になることを確認
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }
}
