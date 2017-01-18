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

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserData一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListFilterNoneExistKeyTest extends AbstractUserDataTest {

    static String userDataId201 = "userdata201";
    static String userDataId202 = "userdata202";

    /**
     * コンストラクタ.
     */
    public UserDataListFilterNoneExistKeyTest() {
        super();
    }

    /**
     * 登録データに存在するキーと存在しないキーでor検索を行った場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録データに存在するキーと存在しないキーでor検索を行った場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String userDataId2 = "userdata002";
        JSONObject body2 = new JSONObject();
        body2.put("__id", userDataId2);
        body2.put("dynamicProperty", "dynamicPropertyValue2");

        // リクエスト実行
        try {
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED);
            createUserData(body2, HttpStatus.SC_CREATED);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue%27"
                            + "+or+noneExistName+eq+%27noneExistValue%27")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

            // エラーコードとメッセージのチェック
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                    DcCoreException.OData.UNKNOWN_QUERY_KEY.params("noneExistName").getMessage());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            deleteUserData(userDataId2);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 登録データに存在するキーと存在しないキーでand検索を行った場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録データに存在するキーと存在しないキーでand検索を行った場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String userDataId2 = "userdata002";
        JSONObject body2 = new JSONObject();
        body2.put("__id", userDataId2);
        body2.put("dynamicProperty", "dynamicPropertyValue2");

        // リクエスト実行
        try {
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED);
            createUserData(body2, HttpStatus.SC_CREATED);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue%27"
                            + "+and+noneExistName+eq+%27noneExistValue%27"
                            + "&\\$inlinecount=allpages")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

            // エラーコードとメッセージのチェック
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                    DcCoreException.OData.UNKNOWN_QUERY_KEY.params("noneExistName").getMessage());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            deleteUserData(userDataId2);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 登録データに存在しないキーを指定して検索を行った場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録データに存在しないキーを指定して検索を行った場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String userDataId2 = "userdata002";
        JSONObject body2 = new JSONObject();
        body2.put("__id", userDataId2);
        body2.put("dynamicProperty", "dynamicPropertyValue2");

        // リクエスト実行
        try {
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED);
            createUserData(body2, HttpStatus.SC_CREATED);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$filter=noneExistName+eq+%27noneExistValue%27"
                            + "&\\$inlinecount=allpages")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

            // エラーコードとメッセージのチェック
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                    DcCoreException.OData.UNKNOWN_QUERY_KEY.params("noneExistName").getMessage());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            deleteUserData(userDataId2);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 登録データが存在しないEntityTypeに対して存在しないキーで検索を行った場合400エラーとなること.
     */
    @Test
    public final void 登録データが存在しないEntityTypeに対して存在しないキーで検索を行った場合400エラーとなること() {
        // リクエスト実行
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$filter=noneExistName+eq+%27noneExistValue%27"
                        + "&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        // エラーコードとメッセージのチェック
        ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("noneExistName").getMessage());
    }

    /**
     * 登録データが存在しないEntityTypeに対して$filterクエリを指定した場合0件HITとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録データが存在しないEntityTypeに対して$filterクエリを指定した場合0件HITとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED);
            // ユーザデータ削除(DynamicPropertyだけ残したいため)
            deleteUserData(userDataId);

            // リクエスト実行
            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$filter=dynamicProperty+eq+%27noneExistValue%27"
                        + "&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null, "__id", 0, null);
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 動的Propertyを削除する.
     * 動的Propertyは様々なデータ型で作られるため、テスト後に必ず削除しておく必要がある。
     * @param propertyName
     */
    private void deleteDynamicProperty(String propertyName) {
        String resourceUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propertyName, entityTypeName);
        ODataCommon.deleteOdataResource(resourceUrl);
    }
}
