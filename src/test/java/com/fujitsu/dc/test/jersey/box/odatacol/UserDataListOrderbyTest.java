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
package com.fujitsu.dc.test.jersey.box.odatacol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData一覧の$orderbyクエリのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListOrderbyTest extends AbstractUserDataTest {

    static String userDataId201 = "userdata201";
    static String userDataId202 = "userdata202";

    /**
     * コンストラクタ.
     */
    public UserDataListOrderbyTest() {
        super();
    }

    /**
     * ユーザデータの一覧を作成.
     */
    @SuppressWarnings("unchecked")
    public void createUserDataList() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId201);
        body.put("dynamicProperty1", "dynamicPropertyValue1");
        body.put("dynamicProperty2", "dynamicPropertyValue2");
        body.put("dynamicProperty3", "dynamicPropertyValue3");

        JSONObject body2 = new JSONObject();
        body2.put("__id", userDataId202);
        body2.put("dynamicProperty1", "dynamicPropertyValueA");
        body2.put("dynamicProperty2", "dynamicPropertyValueB");
        body2.put("dynamicProperty3", "dynamicPropertyValueC");

        // ユーザデータ作成
        createUserData(body, HttpStatus.SC_CREATED);
        createUserData(body2, HttpStatus.SC_CREATED);
    }

    /**
     * ユーザデータの一覧を削除.
     */
    public void deleteUserDataList() {
        deleteUserDataList(userDataId201, userDataId202);
    }

    /**
     * UserDataに$orderbyクエリにIDを指定して正常に取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリにIDを指定して正常に取得できること() {
        String entityTypeName = "Category";

        try {
            // ユーザデータ作成
            createUserDataList("userdata001", "userdata002");

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$orderby=__id")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));
            uri.add(UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));

            ODataCommon.checkCommonResponseUri(res.bodyAsJson(), uri);
        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * UserDataに$orderbyクエリにpublishedを指定して正常に取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリにpublishedを指定して正常に取得できること() {
        String entityTypeName = "Category";

        try {
            // ユーザデータ作成
            createUserDataList("userdata002", "userdata001");

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$orderby=__published")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res);

            // レスポンスボディーのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));
            uri.add(UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));

            ODataCommon.checkCommonResponseUri(res.bodyAsJson(), uri);

        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * UserDataに$orderbyクエリにupdatedを指定して正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに$orderbyクエリにupdatedを指定して正常に取得できること() {
        String entityTypeName = "Category";

        try {
            // ユーザデータ作成
            createUserDataList("userdata001", "userdata002");

            // ユーザデータを１件だけ更新
            JSONObject body = new JSONObject();
            body.put("updatedProperty", "updatedPropertyValueA");
            updateUserData(cellName, boxName, colName, entityTypeName, "userdata001", body);

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$orderby=__updated")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res);

            // レスポンスボディーのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));
            uri.add(UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));

            ODataCommon.checkCommonResponseUri(res.bodyAsJson(), uri);

        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * UserDataに$orderbyクエリをoption指定無しの場合_昇順で取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリをoption指定無しの場合_昇順で取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$orderby=test&$top=3");

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata102')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに$orderbyクエリのoptionにascを指定した場合_昇順で取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリのoptionにascを指定した場合_昇順で取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$orderby=test%20asc&$top=3");

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata102')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに$orderbyクエリのoptionにdescを指定した場合_降順で取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリのoptionにdescを指定した場合_降順で取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$orderby=dynamicProperty%20desc&$top=5");

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata009')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata008')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata007')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata005')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに$orderbyクエリを複数指定した指定した場合_指定した順に取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリを複数指定した指定した場合_指定した順に取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$orderby=test%20asc,dynamicProperty%20desc,sample%20desc&$top=5");

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata102')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata000')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_dynamicProperty2')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに$orderbyクエリに存在しないキーを指定してデフォルトのソート順で取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリに存在しないキーを指定してデフォルトのソート順で取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$orderby=noneExistProperty&$top=3");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        // デフォルトのソート順は定義しない仕様のため、ソート順の確認は行わない
        // レスポンスボディーのチェック
        // ArrayList<String> uri = new ArrayList<String>();
        // uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata000')"));
        // uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001')"));
        // uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata002')"));
        //
        // ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに$orderbyクエリに存在しないキーを降順指定してデフォルトのソート順で取得できること.
     */
    @Test
    public final void UserDataに$orderbyクエリに存在しないキーを降順指定してデフォルトのソート順で取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$orderby=noneExistProperty%20desc&$top=3");

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        // デフォルトのソート順は定義しない仕様のため、ソート順の確認は行わない
        // レスポンスボディーのチェック
        // ArrayList<String> uri = new ArrayList<String>();
        // uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata000')"));
        // uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001')"));
        // uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata002')"));
        //
        // ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに$orderbyクエリに存在するキーと存在しないキーを指定して存在するキーでソートされること.
     */
    @Test
    public final void UserDataに$orderbyクエリに存在するキーと存在しないキーを指定して存在するキーでソートされること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$orderby=test%20asc,noneExistName%20desc&$top=3");

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata102')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyにプロパティ名を文字列で指定した場合にステータスコード400が返却されること.
     */
    @Test
    public final void UserData一覧取得で$orderbyにプロパティ名を文字列で指定した場合にステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$orderby=%27test%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserData一覧取得で$orderbyにオプションを文字列で指定した場合にステータスコード400が返却されること.
     */
    @Test
    public final void UserData一覧取得で$orderbyにオプションを文字列で指定した場合にステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$orderby=test%20%27asc%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserData一覧取得で$orderbyにプロパティ名を指定しなかった場合にステータスコード400が返却されること.
     */
    @Test
    public final void UserData一覧取得で$orderbyにプロパティ名を指定しなかった場合にステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$orderby=")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserData一覧取得で$orderbyのoptionに有効値以外を指定した場合にステータスコード400が返却されること.
     */
    @Test
    public final void UserData一覧取得で$orderbyのoptionに有効値以外を指定した場合にステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$orderby=test%20test")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserData一覧取得で$orderbyのoptionに大文字ASCを指定した場合にステータスコード400が返却されること.
     */
    @Test
    public final void UserData一覧取得で$orderbyのoptionに大文字ASCを指定した場合にステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$orderby=test%20ASC")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /************************************************************************************************************/

    /**
     * UserData一覧取得で$orderbyに文字列型のデータなしスキーマプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列型のデータなしスキーマプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "string";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに文字列型のデータがnullのプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列型のデータがnullのプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "string";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに文字列型のデータが空文字のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列型のデータが空文字のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列型のEntityTypeに対して、空文字で登録されたプロパティをソート条件に指定する
        String entityType = "string";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得でnullや空文字が含まれ$orderbyに文字列型のプロパティを指定した場合にデータが昇順で取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    // 0.19.9 ではnullを含む場合のソート順が違うため、Ignoreにした
    public final void UserData一覧取得でnullや空文字が含まれ$orderbyに文字列型のプロパティを指定した場合にデータが昇順で取得できること() {
        String entityType = "entity";
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // 検索用のUserData作成
            JSONObject body = new JSONObject();
            body.put("__id", "idNull");
            body.put("property", null);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            body = new JSONObject();
            body.put("__id", "idEmpty");
            body.put("property", "");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            body = new JSONObject();
            body.put("__id", "idSmall");
            body.put("property", "pochi");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            body = new JSONObject();
            body.put("__id", "idLarge");
            body.put("property", "poshi");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            // UserData検索
            String query = "?\\$orderby=property";
            TResponse response =
                    UserDataUtils.list(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType, query,
                            MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idEmpty')"));
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idSmall')"));
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idLarge')"));
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idNull')"));
            ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);

        } finally {
            // UserData削除
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idNull");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idEmpty");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idSmall");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idLarge");

            // EntityType削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityType,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * UserData一覧取得でnullや空文字が含まれ$orderbyに文字列型のプロパティを降順で指定した場合にデータが降順で取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserData一覧取得でnullや空文字が含まれ$orderbyに文字列型のプロパティを降順で指定した場合にデータが降順で取得できること() {
        String entityType = "entity";
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    HttpStatus.SC_CREATED);

            // 検索用のUserData作成
            JSONObject body = new JSONObject();
            body.put("__id", "idNull");
            body.put("property", null);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            body = new JSONObject();
            body.put("__id", "idEmpty");
            body.put("property", "");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            body = new JSONObject();
            body.put("__id", "idSmall");
            body.put("property", "pochi");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            body = new JSONObject();
            body.put("__id", "idLarge");
            body.put("property", "poshi");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType);

            // UserData検索
            String query = "?\\$orderby=property%20desc";
            TResponse response =
                    UserDataUtils.list(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType, query,
                            MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idLarge')"));
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idSmall')"));
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idEmpty')"));
            uri.add(UrlUtils.userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType + "('idNull')"));
            ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);

        } finally {
            // UserData削除
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idNull");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idEmpty");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idSmall");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, "idLarge");

            // EntityType削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityType,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * UserData一覧取得で$orderbyに文字列型のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列型のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "string";
        String query = "?\\$orderby=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに文字列型のプロパティの降順を指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列型のプロパティの降順を指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "string";
        String query = "?\\$orderby=property3%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに文字列配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列配列型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "stringList";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに文字列配列型のデータがnullのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列配列型のデータがnullのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列配列型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "stringList";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに文字列配列型のデータが空リストのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列配列型のデータが空リストのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列配列型のEntityTypeに対して、空リストで登録されたプロパティをソート条件に指定する
        String entityType = "stringList";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに文字列配列型のプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに文字列配列型のプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 文字列配列型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "stringList";
        String query = "?\\$orderby=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /************************************************************************************************************/

    /**
     * UserData一覧取得で$orderbyに整数型のデータなしスキーマプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数型のデータなしスキーマプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "int";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに整数型のデータがnullのプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数型のデータがnullのプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "int";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに整数型のデータが0のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数型のデータが0のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数型のEntityTypeに対して、0で登録されたプロパティをソート条件に指定する
        String entityType = "int";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに整数型のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数型のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "int";
        String query = "?\\$orderby=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに整数型のプロパティの降順を指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数型のプロパティの降順を指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "int";
        String query = "?\\$orderby=property3%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに整数配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数配列型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "intList";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに整数配列型のデータがnullのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数配列型のデータがnullのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数配列型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "intList";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに整数配列型のデータが空リストのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数配列型のデータが空リストのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数配列型のEntityTypeに対して、空リストで登録されたプロパティをソート条件に指定する
        String entityType = "intList";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに整数配列型のプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに整数配列型のプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 整数配列型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "intList";
        String query = "?\\$orderby=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /************************************************************************************************************/

    /**
     * UserData一覧取得で$orderbyに小数型のデータなしスキーマプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数型のデータなしスキーマプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "single";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに小数型のデータがnullのプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数型のデータがnullのプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "single";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに小数型のデータが0のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数型のデータが0のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数型のEntityTypeに対して、0で登録されたプロパティをソート条件に指定する
        String entityType = "single";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに小数型のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数型のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "single";
        String query = "?\\$orderby=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに小数型のプロパティの降順を指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数型のプロパティの降順を指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "single";
        String query = "?\\$orderby=property3%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに小数配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数配列型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "singleList";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに小数配列型のデータがnullのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数配列型のデータがnullのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数配列型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "singleList";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに小数配列型のデータが空リストのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数配列型のデータが空リストのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数配列型のEntityTypeに対して、空リストで登録されたプロパティをソート条件に指定する
        String entityType = "singleList";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに小数配列型のプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに小数配列型のプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 小数配列型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "singleList";
        String query = "?\\$orderby=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /************************************************************************************************************/

    /**
     * UserData一覧取得で$orderbyに真偽値型のデータなしスキーマプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値型のデータなしスキーマプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "boolean";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに真偽値型のデータがnullのプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値型のデータがnullのプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "boolean";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに真偽値型のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値型のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "boolean";
        String query = "?\\$orderby=property3,__id";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに真偽値型のプロパティの降順を指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値型のプロパティの降順を指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "boolean";
        String query = "?\\$orderby=property3%20desc,__id%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに真偽値配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値配列型のデータなしスキーマプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値配列型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "booleanList";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに真偽値配列型のデータがnullのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値配列型のデータがnullのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値配列型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "booleanList";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに真偽値配列型のデータが空リストのプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値配列型のデータが空リストのプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値配列型のEntityTypeに対して、空リストで登録されたプロパティをソート条件に指定する
        String entityType = "booleanList";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /**
     * UserData一覧取得で$orderbyに真偽値配列型のプロパティを指定した場合に400エラーとなること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに真偽値配列型のプロパティを指定した場合に400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 真偽値配列型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "booleanList";
        String query = "?\\$orderby=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME,
                        HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getCode(),
                DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY.getMessage());
    }

    /************************************************************************************************************/

    /**
     * UserData一覧取得で$orderbyに日付時刻型のデータなしスキーマプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに日付時刻型のデータなしスキーマプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 日付時刻型のEntityTypeに対して、スキーマ定義はしているが、登録時にデータを指定せずに登録したプロパティをソート条件に指定する
        String entityType = "datetime";
        String query = "?\\$orderby=property0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに日付時刻型のデータがnullのプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに日付時刻型のデータがnullのプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 日付時刻型のEntityTypeに対して、nullで登録されたプロパティをソート条件に指定する
        String entityType = "datetime";
        String query = "?\\$orderby=property1";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        assertUserDataList(response);
    }

    /**
     * UserData一覧取得で$orderbyに日付時刻型のプロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに日付時刻型のプロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 日付時刻型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "datetime";
        String query = "?\\$orderby=property2";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得で$orderbyに日付時刻型のプロパティの降順を指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに日付時刻型のプロパティの降順を指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        // 日付時刻型のEntityTypeに対して、登録されたプロパティをソート条件に指定する
        String entityType = "datetime";
        String query = "?\\$orderby=property2%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserData一覧取得でfilterとselectを組みあわせてデータ取得ができること.
     */
    @Test
    public final void UserData一覧取得でfilterとselectを組みあわせてデータ取得ができること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        String entityType = "string";
        String query = "?\\$orderby=property3%20desc&\\$filter=property4%20eq%20%27Value2%27&\\$select=property3";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        JSONObject json = response.bodyAsJson();
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        ODataCommon.checkCommonResponseUri(json, uri);

        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        for (Object result : results) {
            assertTrue("property3 is not contains", ((JSONObject) result).containsKey("property3"));
            assertFalse("property2 is contains", ((JSONObject) result).containsKey("property2"));
        }
    }

    /**
     * UserData一覧取得でtopとskipを組みあわせてデータ取得ができること.
     */
    @Test
    public final void UserData一覧取得でtopとskipを組みあわせてデータ取得ができること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        String entityType = "string";
        String query = "?\\$orderby=property3%20desc&\\$top=5&\\$skip=0";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        JSONObject json = response.bodyAsJson();
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        ODataCommon.checkCommonResponseUri(json, uri);

        query = "?\\$orderby=property3%20desc&\\$top=5&\\$skip=5";
        response = UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        json = response.bodyAsJson();
        uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(json, uri);
    }

    /**
     * UserData一覧取得で$orderbyに__idを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに__idを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        String entityType = "string";
        String query = "?\\$orderby=__id%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        JSONObject json = response.bodyAsJson();
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(json, uri);
    }

    /**
     * UserData一覧取得で$orderbyに__updatedを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに__updatedを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        String entityType = "string";
        String query = "?\\$orderby=__updated%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        JSONObject json = response.bodyAsJson();
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(json, uri);
    }

    /**
     * UserData一覧取得で$orderbyに__publishedを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに__publishedを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        String entityType = "string";
        String query = "?\\$orderby=__updated%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        JSONObject json = response.bodyAsJson();
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata009')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata008')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata007')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata006')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata005')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata004')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata003')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(json, uri);
    }

    /**
     * UserData一覧取得で$orderbyに動的プロパティを指定した場合にデータが取得できること.
     */
    @Test
    public final void UserData一覧取得で$orderbyに動的プロパティを指定した場合にデータが取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.SEARCH_ODATA;

        String entityType = "dynamic";
        String query = "?\\$orderby=property1%20asc,property2%20desc";
        TResponse response =
                UserDataUtils.list(cell, box, collection, entityType, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        JSONObject json = response.bodyAsJson();
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata002')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata001')"));
        uri.add(UrlUtils.userData(cell, box, collection, entityType + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(json, uri);
    }

    private void assertUserDataList(TResponse response) {
        List<String> expectedList = Arrays.asList("userdata000", "userdata001", "userdata002", "userdata003",
                "userdata004", "userdata005", "userdata006", "userdata007", "userdata008", "userdata009");
        JSONObject json = response.bodyAsJson();
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        for (Object result : results) {
            String id = (String) ((JSONObject) result).get(Common.P_ID.getName());
            assertTrue("expected id is not contains. id:" + id, expectedList.contains(id));
        }
    }

}
