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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListSelectTest extends AbstractUserDataTest {

    static String userDataId201 = "userdata201";
    static String userDataId202 = "userdata202";

    /**
     * コンストラクタ.
     */
    public UserDataListSelectTest() {
        super();
    }

    @SuppressWarnings("unchecked")
    private void createMultiUserDataList() {
        JSONObject body = new JSONObject();
        body.put("__id", userDataId201);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");
        body.put("nullProp", null);
        body.put("intProperty", 123);
        body.put("floatProperty", 123.123);
        body.put("trueProperty", true);
        body.put("falseProperty", false);
        body.put("nullStringProperty", "null");
        body.put("intStringProperty", "123");
        body.put("floatStringProperty", "123.123");
        body.put("trueStringProperty", "true");
        body.put("falseStringProperty", "false");
        body.put("onlyExistProperty201", "exist201");

        JSONObject body2 = new JSONObject();
        body2.put("__id", userDataId202);
        body2.put("dynamicProperty", "dynamicPropertyValue2");
        body2.put("secondDynamicProperty", "secondDynamicPropertyValue2");
        body2.put("nullProp", "nullString2");
        body2.put("intProperty", 1234);
        body2.put("floatProperty", 123.1234);
        body2.put("trueProperty", true);
        body2.put("falseProperty", false);
        body2.put("nullStringProperty", "null");
        body2.put("intStringProperty", "1234");
        body2.put("floatStringProperty", "123.1234");
        body2.put("trueStringProperty", "true");
        body2.put("falseStringProperty", "false");
        body.put("onlyExistProperty202", "exist202");

        // ユーザデータ作成
        createUserData(body, HttpStatus.SC_CREATED);
        createUserData(body2, HttpStatus.SC_CREATED);
    }

    /**
     * ユーザデータの一覧を削除.
     */
    public void deleteUserDataList() {
        deleteUserData(userDataId201);
        deleteUserData(userDataId202);
    }

    /**
     * $selectクエリに存在するString型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void $selectクエリに存在するString型のプロパティ名を指定して対象のデータのみ取得できること() {

        try {
            Map<String, String> etag = new HashMap<String, String>();
            // ユーザデータの作成
            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            createUserDataList(userDataId201, userDataId202, etag);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=dynamicProperty1")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("dynamicProperty1", "dynamicPropertyValue1");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("dynamicProperty1", "dynamicPropertyValueA");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);

        } finally {
            deleteUserDataList();
        }

    }

    /**
     * $selectクエリに存在するlong型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void $selectクエリに存在するlong型のプロパティ名を指定して対象のデータのみ取得できること() {

        try {
            createMultiUserDataList();

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=intProperty")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("intProperty", 123);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("intProperty", 1234);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリに存在するfloat型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void $selectクエリに存在するfloat型のプロパティ名を指定して対象のデータのみ取得できること() {

        try {
            createMultiUserDataList();

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=floatProperty")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId201 + "')"));
            uri.put(userDataId202,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("floatProperty", 123.123);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("floatProperty", 123.1234);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリに存在するboolean型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void $selectクエリに存在するboolean型のプロパティ名を指定して対象のデータのみ取得できること() {

        try {
            createMultiUserDataList();

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=trueProperty")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId201 + "')"));
            uri.put(userDataId202,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("trueProperty", true);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("trueProperty", true);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリにIDを指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリにIDを指定して正常に取得できること() {
        String entityTypeName = "Category";

        try {
            Map<String, String> etag = new HashMap<String, String>();
            // ユーザデータの作成
            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            createUserDataList("userdata001", "userdata002", etag);

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=__id")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata001", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));
            uri.put("userdata002", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("userdata001", additionalprop);
            additionalprop.put("__id", "userdata001");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put("userdata002", additionalprop2);
            additionalprop2.put("__id", "userdata002");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);
        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * $selectクエリにpublishedを指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリにpublishedを指定して正常に取得できること() {
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
                    .with("query", "?\\$select=__published")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata001", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));
            uri.put("userdata002", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("userdata001", additionalprop);
            additionalprop.put("__id", "userdata001");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put("userdata002", additionalprop2);
            additionalprop2.put("__id", "userdata002");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * $selectクエリにupdatedを指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリにupdatedを指定して正常に取得できること() {
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
                    .with("query", "?\\$select=__updated")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata001", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));
            uri.put("userdata002", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("userdata001", additionalprop);
            additionalprop.put("__id", "userdata001");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put("userdata002", additionalprop2);
            additionalprop2.put("__id", "userdata002");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * $selectクエリに__metadataを指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリに__metadataを指定して正常に取得できること() {
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
                    .with("query", "?\\$select=__metadata")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata001", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));
            uri.put("userdata002", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("userdata001", additionalprop);
            additionalprop.put("__id", "userdata001");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put("userdata002", additionalprop2);
            additionalprop2.put("__id", "userdata002");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * $selectクエリに管理情報とユーザ情報を指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリに管理情報とユーザ情報を指定して正常に取得できること() {
        String entityTypeName = "Category";

        try {
            Map<String, String> etag = new HashMap<String, String>();
            // ユーザデータの作成
            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            createUserDataList("userdata001", "userdata002", etag);

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=__id,__published,__updated,dynamicProperty1,dynamicProperty3")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata001", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata001')"));
            uri.put("userdata002", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata002')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("userdata001", additionalprop);
            additionalprop.put("__id", "userdata001");
            additionalprop.put("dynamicProperty1", "dynamicPropertyValue1");
            additionalprop.put("dynamicProperty3", "dynamicPropertyValue3");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put("userdata002", additionalprop2);
            additionalprop2.put("__id", "userdata002");
            additionalprop2.put("dynamicProperty1", "dynamicPropertyValueA");
            additionalprop2.put("dynamicProperty3", "dynamicPropertyValueC");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);
        } finally {
            deleteUserDataList("userdata001", "userdata002");
        }
    }

    /**
     * $selectで指定したプロパティが存在するデータと存在しないデータが混在する場合に存在するプロパティが取得できること.
     */
    @Test
    public final void $selectで指定したプロパティが存在するデータと存在しないデータが混在する場合に存在するプロパティが取得できること() {

        try {
            createMultiUserDataList();

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=onlyExistProperty201")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId201 + "')"));
            uri.put(userDataId202,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop1);
            additionalprop1.put("__id", userDataId201);
            additionalprop1.put("onlyExistProperty201", "exist201");
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリに存在しないプロパティ名を指定して１件も取得できないこと.
     */
    @Test
    public final void $selectクエリに存在しないプロパティ名を指定して１件も取得できないこと() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=dummyProperty")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId201 + "')"));
            uri.put(userDataId202,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop1);
            additionalprop1.put("__id", userDataId201);
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }

    }

    /**
     * $selectクエリに存在しないプロパティ名を指定した場合に管理データのみ取得できること.
     */
    @Test
    public final void $selectクエリに存在しないプロパティ名を指定した場合に管理データのみ取得できること() {

        try {
            createMultiUserDataList();

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=dummyProperty")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId201 + "')"));
            uri.put(userDataId202,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop1);
            additionalprop1.put("__id", userDataId201);
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }

    }

    /**
     * $selectクエリに存在するプロパティ名を複数指定して対象のデータのみ取得できること.
     */
    @Test
    public final void $selectクエリに存在するプロパティ名を複数指定して対象のデータのみ取得できること() {

        try {
            // リクエストボディを設定
            createMultiUserDataList();

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=dynamicProperty,secondDynamicProperty,"
                            + "nullProp,intProperty,floatProperty,trueProperty,"
                            + "falseProperty,nullStringProperty,intStringProperty,"
                            + "floatStringProperty,trueStringProperty,falseStringProperty")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("dynamicProperty", "dynamicPropertyValue");
            additionalprop.put("secondDynamicProperty", "secondDynamicPropertyValue");
            additionalprop.put("intProperty", 123);
            additionalprop.put("floatProperty", 123.123);
            additionalprop.put("trueProperty", true);
            additionalprop.put("falseProperty", false);
            additionalprop.put("nullStringProperty", "null");
            additionalprop.put("intStringProperty", "123");
            additionalprop.put("floatStringProperty", "123.123");
            additionalprop.put("trueStringProperty", "true");
            additionalprop.put("falseStringProperty", "false");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("dynamicProperty", "dynamicPropertyValue2");
            additionalprop2.put("secondDynamicProperty", "secondDynamicPropertyValue2");
            additionalprop2.put("nullProp", "nullString2");
            additionalprop2.put("intProperty", 1234);
            additionalprop2.put("floatProperty", 123.1234);
            additionalprop2.put("trueProperty", true);
            additionalprop2.put("falseProperty", false);
            additionalprop2.put("nullStringProperty", "null");
            additionalprop2.put("intStringProperty", "1234");
            additionalprop2.put("floatStringProperty", "123.1234");
            additionalprop2.put("trueStringProperty", "true");
            additionalprop2.put("falseStringProperty", "false");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }

    }

    /**
     * $selectクエリに存在するプロパティ名と存在しないプロパティ名を指定して１件も取得できないこと.
     */
    @Test
    public final void $selectクエリに存在するプロパティ名と存在しないプロパティ名を指定して１件も取得できないこと() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=dynamicProperty1,dinamicProperty2")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId201 + "')"));
            uri.put(userDataId202,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop1);
            additionalprop1.put("__id", userDataId201);
            additionalprop1.put("dynamicProperty1", "dynamicPropertyValue1");
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("dynamicProperty1", "dynamicPropertyValueA");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }

    }

    /**
     * $selectクエリに存在しないプロパティ名を複数指定して管理情報のみ取得できること.
     */
    @Test
    public final void $selectクエリに存在しないプロパティ名を複数指定して管理情報のみ取得できること() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=noneExistProperty1,noneExistProperty2")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId201 + "')"));
            uri.put(userDataId202,
                    UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop1);
            additionalprop1.put("__id", userDataId201);
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリに*を指定してすべてのデータが取得できること.
     */
    @Test
    public final void $selectクエリにアスタリスクを指定してすべてのデータが取得できること() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=*")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("dynamicProperty1", "dynamicPropertyValue1");
            additionalprop.put("dynamicProperty2", "dynamicPropertyValue2");
            additionalprop.put("dynamicProperty3", "dynamicPropertyValue3");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("dynamicProperty1", "dynamicPropertyValueA");
            additionalprop2.put("dynamicProperty2", "dynamicPropertyValueB");
            additionalprop2.put("dynamicProperty3", "dynamicPropertyValueC");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * UserDataの$selectクエリに値を指定しない場合に４００が返却されること.
     */
    @Test
    public final void UserDataの$selectクエリに値を指定しない場合に４００が返却されること() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * UserDataの$selectクエリにパースエラーとなる文字を指定した場合に４００が返却されること.
     */
    @Test
    public final void UserDataの$selectクエリにパースエラーとなる文字を指定した場合に４００が返却されること() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=!")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリと$filterのandクエリを指定して指定したデータが取得できること.
     */
    @Test
    public final void $selectクエリと$filterのandクエリを指定して指定したデータが取得できること() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            TResponse response = Http
                    .request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query",
                            "?\\$filter=dynamicProperty1+eq+%27dynamicPropertyValueA%27+and+"
                                    + "dynamicProperty2+eq+%27dynamicPropertyValueB%27"
                                    + "&\\$select=dynamicProperty1,dynamicProperty2")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("dynamicProperty1", "dynamicPropertyValueA");
            additionalprop2.put("dynamicProperty2", "dynamicPropertyValueB");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリと$filterのorクエリを指定して指定したデータが取得できること.
     */
    @Test
    public final void $selectクエリと$filterのorクエリを指定して指定したデータが取得できること() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            TResponse response = Http
                    .request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query",
                            "?\\$filter=dynamicProperty1+eq+%27dynamicPropertyValueA%27+or+"
                                    + "dynamicProperty2+eq+%27dynamicPropertyValue2%27"
                                    + "&\\$select=dynamicProperty1,dynamicProperty2")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("dynamicProperty1", "dynamicPropertyValue1");
            additionalprop.put("dynamicProperty2", "dynamicPropertyValue2");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("dynamicProperty1", "dynamicPropertyValueA");
            additionalprop2.put("dynamicProperty2", "dynamicPropertyValueB");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * dynamicPropertyのみ$selectした場合に指定したデータのみが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void dynamicPropertyのみ$selectした場合に指定したデータのみが取得できること() {
        String userdataUrl = null;
        try {
            // スキーマ定義
            UserDataComplexTypeUtils.createComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            JSONObject complexBody = new JSONObject();
            body.put("__id", "001");
            body.put("stringProperty", "staticStringProprty");
            body.put("complexProperty", complexBody);
            body.put("dynamicProperty", "dynamicStringProperty");
            complexBody.put("complexStringProperty", "complexStringProprty");
            DcResponse createResponse = createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "selectTestEntity", body);
            userdataUrl = createResponse.getFirstHeader(HttpHeaders.LOCATION);

            // $selectにてdynamicPropertyを指定して一覧取得
            String query = "?\\$select=dynamicProperty";
            TResponse response = getUserDataList(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", query);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONArray results = ((JSONArray) ((JSONObject) responseJson.get("d")).get("results"));
            assertEquals(1, results.size());
            JSONObject result = ((JSONObject) results.get(0));
            assertEquals("001", result.get("__id"));
            assertEquals("dynamicStringProperty", result.get("dynamicProperty"));
            assertFalse(result.containsKey("stringProperty"));
            assertFalse(result.containsKey("complexProperty"));

        } finally {
            if (userdataUrl != null) {
                ODataCommon.deleteOdataResource(userdataUrl);
            }
            UserDataComplexTypeUtils.deleteComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");
        }
    }

    /**
     * 定義したシンプル型Propertyのみ$selectした場合に指定したデータのみが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 定義したシンプル型Propertyのみ$selectした場合に指定したデータのみが取得できること() {
        String userdataUrl = null;
        try {
            // スキーマ定義
            UserDataComplexTypeUtils.createComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            JSONObject complexBody = new JSONObject();
            body.put("__id", "001");
            body.put("stringProperty", "staticStringProprty");
            body.put("complexProperty", complexBody);
            body.put("dynamicProperty", "dynamicStringProperty");
            complexBody.put("complexStringProperty", "complexStringProprty");
            DcResponse createResponse = createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "selectTestEntity", body);
            userdataUrl = createResponse.getFirstHeader(HttpHeaders.LOCATION);

            // $selectにてdynamicPropertyを指定して一覧取得
            String query = "?\\$select=stringProperty";
            TResponse response = getUserDataList(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", query);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONArray results = ((JSONArray) ((JSONObject) responseJson.get("d")).get("results"));
            assertEquals(1, results.size());
            JSONObject result = ((JSONObject) results.get(0));
            assertEquals("001", result.get("__id"));
            assertEquals("staticStringProprty", result.get("stringProperty"));
            assertFalse(result.containsKey("dynamicStringProperty"));
            assertFalse(result.containsKey("complexProperty"));

        } finally {
            if (userdataUrl != null) {
                ODataCommon.deleteOdataResource(userdataUrl);
            }
            UserDataComplexTypeUtils.deleteComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");
        }
    }

    /**
     * 定義したコンプレックス型Propertyのみ$selectした場合に指定したデータのみが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 定義したコンプレックス型Propertyのみ$selectした場合に指定したデータのみが取得できること() {
        String userdataUrl = null;
        try {
            // スキーマ定義
            UserDataComplexTypeUtils.createComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            JSONObject complexBody = new JSONObject();
            body.put("__id", "001");
            body.put("stringProperty", "staticStringProprty");
            body.put("complexProperty", complexBody);
            body.put("dynamicProperty", "dynamicStringProperty");
            complexBody.put("complexStringProperty", "complexStringProprty");
            DcResponse createResponse = createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "selectTestEntity", body);
            userdataUrl = createResponse.getFirstHeader(HttpHeaders.LOCATION);

            // $selectにてdynamicPropertyを指定して一覧取得
            String query = "?\\$select=complexProperty";
            TResponse response = getUserDataList(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", query);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONArray results = ((JSONArray) ((JSONObject) responseJson.get("d")).get("results"));
            assertEquals(1, results.size());
            JSONObject result = ((JSONObject) results.get(0));
            assertEquals("001", result.get("__id"));
            assertEquals(complexBody, result.get("complexProperty"));
            assertFalse(result.containsKey("dynamicStringProperty"));
            assertFalse(result.containsKey("stringProperty"));

        } finally {
            if (userdataUrl != null) {
                ODataCommon.deleteOdataResource(userdataUrl);
            }
            UserDataComplexTypeUtils.deleteComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");
        }
    }

    /**
     * 存在しないPropertyを$selectした場合に__idのみが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 存在しないPropertyを$selectした場合に__idのみが取得できること() {
        String userdataUrl = null;
        try {
            // スキーマ定義
            UserDataComplexTypeUtils.createComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            JSONObject complexBody = new JSONObject();
            body.put("__id", "001");
            body.put("stringProperty", "staticStringProprty");
            body.put("complexProperty", complexBody);
            body.put("dynamicProperty", "dynamicStringProperty");
            complexBody.put("complexStringProperty", "complexStringProprty");
            DcResponse createResponse = createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "selectTestEntity", body);
            userdataUrl = createResponse.getFirstHeader(HttpHeaders.LOCATION);

            // $selectにてdynamicPropertyを指定して一覧取得
            String query = "?\\$select=notExistsProperty";
            TResponse response = getUserDataList(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", query);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONArray results = ((JSONArray) ((JSONObject) responseJson.get("d")).get("results"));
            assertEquals(1, results.size());
            JSONObject result = ((JSONObject) results.get(0));
            assertEquals("001", result.get("__id"));
            assertFalse(result.containsKey("complexProperty"));
            assertFalse(result.containsKey("dynamicStringProperty"));
            assertFalse(result.containsKey("stringProperty"));

        } finally {
            if (userdataUrl != null) {
                ODataCommon.deleteOdataResource(userdataUrl);
            }
            UserDataComplexTypeUtils.deleteComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");
        }
    }

    /**
     * 動的・静的プロパティを併用して$selectした場合に指定したデータのみが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 動的_静的プロパティを併用して$selectした場合に指定したデータのみが取得できること() {
        String userdataUrl = null;
        try {
            // スキーマ定義
            UserDataComplexTypeUtils.createComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            JSONObject complexBody = new JSONObject();
            body.put("__id", "001");
            body.put("stringProperty", "staticStringProprty");
            body.put("complexProperty", complexBody);
            body.put("dynamicProperty", "dynamicStringProperty");
            complexBody.put("complexStringProperty", "complexStringProprty");
            DcResponse createResponse = createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "selectTestEntity", body);
            userdataUrl = createResponse.getFirstHeader(HttpHeaders.LOCATION);

            // $selectにてdynamicPropertyを指定して一覧取得
            String query = "?\\$select=dynamicProperty,complexProperty";
            TResponse response = getUserDataList(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", query);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONArray results = ((JSONArray) ((JSONObject) responseJson.get("d")).get("results"));
            assertEquals(1, results.size());
            JSONObject result = ((JSONObject) results.get(0));
            assertEquals("001", result.get("__id"));
            assertEquals(complexBody, result.get("complexProperty"));
            assertEquals("dynamicStringProperty", result.get("dynamicProperty"));
            assertFalse(result.containsKey("stringProperty"));

        } finally {
            if (userdataUrl != null) {
                ODataCommon.deleteOdataResource(userdataUrl);
            }
            UserDataComplexTypeUtils.deleteComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");
        }
    }

    /**
     * アスタリスクを$selectに指定した場合に全ての項目が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void アスタリスクを$selectに指定した場合に全ての項目が取得できること() {
        String userdataUrl = null;
        try {
            // スキーマ定義
            UserDataComplexTypeUtils.createComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            JSONObject complexBody = new JSONObject();
            body.put("__id", "001");
            body.put("stringProperty", "staticStringProprty");
            body.put("complexProperty", complexBody);
            body.put("dynamicProperty", "dynamicStringProperty");
            complexBody.put("complexStringProperty", "complexStringProprty");
            DcResponse createResponse = createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "selectTestEntity", body);
            userdataUrl = createResponse.getFirstHeader(HttpHeaders.LOCATION);

            // $selectにてdynamicPropertyを指定して一覧取得
            String query = "?\\$select=*";
            TResponse response = getUserDataList(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", query);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONArray results = ((JSONArray) ((JSONObject) responseJson.get("d")).get("results"));
            assertEquals(1, results.size());
            JSONObject result = ((JSONObject) results.get(0));
            assertEquals("001", result.get("__id"));
            assertEquals(complexBody, result.get("complexProperty"));
            assertEquals("dynamicStringProperty", result.get("dynamicProperty"));
            assertEquals("staticStringProprty", result.get("stringProperty"));

        } finally {
            if (userdataUrl != null) {
                ODataCommon.deleteOdataResource(userdataUrl);
            }
            UserDataComplexTypeUtils.deleteComplexTypeSchema("selectTestEntity", "selectTestComplex",
                    "stringProperty", "complexProperty", "complexStringProperty");
        }
    }

    /**
     * $selectクエリにハイフンを含むプロパティを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $selectクエリにハイフンを含むプロパティを指定して対象のデータのみ取得できること() {

        try {
            // ユーザデータの作成
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            body.put("__id", userDataId201);
            body.put("name", "pochi");
            body.put("na-me", "po-chi");
            body.put("na_me", "po_chi");
            createUserData(body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("__id", userDataId202);
            body2.put("name", "tama");
            body2.put("na-me", "ta-ma");
            body2.put("na_me", "ta_ma");
            createUserData(body2, HttpStatus.SC_CREATED);

            // ユーザデータの一覧取得(filterクエリに「-」指定)
            TResponse response = UserDataUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=na-me", HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("na-me", "po-chi");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("na-me", "ta-ma");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, null);

            // ユーザデータの一覧取得(filterクエリにエスケープされた「-」を指定)
            response = UserDataUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=na%2Dme", HttpStatus.SC_OK);

            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, null);
        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリにアンダーバーを含むプロパティを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $selectクエリにアンダーバーを含むプロパティを指定して対象のデータのみ取得できること() {

        try {
            // ユーザデータの作成
            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            body.put("__id", userDataId201);
            body.put("name", "pochi");
            body.put("na-me", "po-chi");
            body.put("na_me", "po_chi");
            createUserData(body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("__id", userDataId202);
            body2.put("name", "tama");
            body2.put("na-me", "ta-ma");
            body2.put("na_me", "ta_ma");
            createUserData(body2, HttpStatus.SC_CREATED);

            // ユーザデータの一覧取得
            TResponse response = UserDataUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=na_me", HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put("na_me", "po_chi");

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put("na_me", "ta_ma");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, null);

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリでプロパティをシングルクォートで囲んだときに４００が返却されること.
     */
    @Test
    public final void $selectクエリでプロパティをシングルクォートで囲んだときに４００が返却されること() {

        try {
            createUserDataList(userDataId201, userDataId202);

            // ユーザデータの一覧取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select='dynamicProperty1'")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserDataList();
        }
    }

    /**
     * $selectクエリに存在するDouble型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $selectクエリに存在するDouble型のプロパティ名を指定して対象のデータのみ取得できること() {
        String userSelectDouble = "userSelectDouble";
        String propertyDouble = "propertyDouble";

        try {
            // Proeprty登録(Edm.Double)
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // ユーザOData登録($select指定)
            Map<String, String> etag = new HashMap<String, String>();
            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            createUserDataList(userDataId201, userDataId202, etag);

            JSONObject body = new JSONObject();
            body.put("__id", userSelectDouble);
            body.put("dynamicProperty1", "dynamicPropertyValueA");
            body.put(propertyDouble, 12345.123456789);

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);
            // Etag取得
            etag.put(userSelectDouble, response.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=" + propertyDouble)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));
            uri.put(userSelectDouble, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userSelectDouble + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put(propertyDouble, null);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put(propertyDouble, null);

            Map<String, Object> additionalprop3 = new HashMap<String, Object>();
            additional.put(userSelectDouble, additionalprop3);
            additionalprop3.put("__id", userSelectDouble);
            additionalprop3.put(propertyDouble, 12345.123456789);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);

        } finally {
            deleteUserDataList();
            deleteUserData(userSelectDouble);
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble, -1);
        }
    }

    /**
     * $selectクエリに存在するDouble型配列のプロパティ名を指定して対象のデータのみ取得できること.
     * @throws ParseException パースエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $selectクエリに存在するDouble型配列のプロパティ名を指定して対象のデータのみ取得できること() throws ParseException {
        String userSelectDouble = "userSelectDouble";
        String propertyDouble = "propertyDouble";

        try {
            JSONArray doubleArray = new JSONArray();
            doubleArray.add(123.123d);
            doubleArray.add(Integer.MAX_VALUE + 1L);
            doubleArray.add(0);
            doubleArray.add(-987.987d);
            // Proeprty登録(Edm.Double)
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "List", false, null,
                    HttpStatus.SC_CREATED);
            // ユーザOData登録($select指定)
            Map<String, String> etag = new HashMap<String, String>();
            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            createUserDataList(userDataId201, userDataId202, etag);

            JSONObject body = new JSONObject();
            body.put("__id", userSelectDouble);
            body.put("dynamicProperty1", "dynamicPropertyValueA");
            body.put(propertyDouble, doubleArray);

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);
            // Etag取得
            etag.put(userSelectDouble, response.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=" + propertyDouble)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId201, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId201 + "')"));
            uri.put(userDataId202, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId202 + "')"));
            uri.put(userSelectDouble, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userSelectDouble + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId201, additionalprop);
            additionalprop.put("__id", userDataId201);
            additionalprop.put(propertyDouble, null);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId202, additionalprop2);
            additionalprop2.put("__id", userDataId202);
            additionalprop2.put(propertyDouble, null);

            Map<String, Object> additionalprop3 = new HashMap<String, Object>();
            additional.put(userSelectDouble, additionalprop3);
            additionalprop3.put("__id", userSelectDouble);
            additionalprop3.put(propertyDouble, doubleArray);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);

        } finally {
            deleteUserDataList();
            deleteUserData(userSelectDouble);
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble, -1);
        }
    }

    /**
     * プロパティをEdm_Int32からEdm_Doubleへ更新後に$selectクエリに存在するDouble型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティをEdm_Int32からEdm_Doubleへ更新後に$selectクエリに存在するDouble型のプロパティ名を指定して対象のデータのみ取得できること() {
        String userSelectDouble = "userSelectDouble";
        String propertyDouble = "propertyDouble";

        try {
            // Proeprty登録(Edm.Int32)
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // ユーザOData登録
            Map<String, String> etag = new HashMap<String, String>();

            JSONObject body = new JSONObject();
            body.put("__id", userSelectDouble);
            body.put("dynamicProperty1", "dynamicPropertyValueA");
            body.put(propertyDouble, 12345);

            // ユーザデータ登録
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);
            // Etag取得
            etag.put(userSelectDouble, response.getHeader(HttpHeaders.ETAG));

            // プロパティ更新(Edm.Double)
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyDouble,
                    entityTypeName, propertyDouble, entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(),
                    true, null, "None", false, null);

            body = new JSONObject();
            body.put("__id", userSelectDouble + 1);
            body.put("dynamicProperty1", "dynamicPropertyValueB");
            body.put(propertyDouble, 12345.123456789);

            // ユーザデータ登録(Double型)
            response = createUserData(body, HttpStatus.SC_CREATED);
            // Etag取得
            etag.put(userSelectDouble + 1, response.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得($select指定)
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=" + propertyDouble + "&\\$orderby=dynamicProperty1")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userSelectDouble, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userSelectDouble + "')"));
            uri.put(userSelectDouble + 1, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userSelectDouble + 1 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userSelectDouble, additionalprop);
            additionalprop.put("__id", userSelectDouble);
            additionalprop.put(propertyDouble, 12345);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userSelectDouble + 1, additionalprop2);
            additionalprop2.put("__id", userSelectDouble + 1);
            additionalprop2.put(propertyDouble, 12345.123456789);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);

        } finally {
            deleteUserData(userSelectDouble);
            deleteUserData(userSelectDouble + 1);
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble, -1);
        }
    }

    /**
     * プロパティをEdm_Int32からEdm_Doubleへ更新後に$selectクエリに存在するDouble型配列のプロパティ名を指定して対象のデータのみ取得できること.
     * @throws ParseException パースエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティをEdm_Int32からEdm_Doubleへ更新後に$selectクエリに存在するDouble型配列のプロパティ名を指定して対象のデータのみ取得できること()
            throws ParseException {
        String userSelectDouble = "userSelectDouble";
        String propertyDouble = "propertyDouble";

        try {
            // Proeprty登録(Edm.Double)
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "List", false, null,
                    HttpStatus.SC_CREATED);
            // ユーザOData登録
            Map<String, String> etag = new HashMap<String, String>();

            JSONArray intArray = new JSONArray();
            intArray.add(123);
            intArray.add(Integer.MAX_VALUE);
            intArray.add(0);

            JSONObject body = new JSONObject();
            body.put("__id", userSelectDouble);
            body.put("dynamicProperty1", "dynamicPropertyValueA");
            body.put(propertyDouble, intArray);

            // ユーザデータ登録(Double型)
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);
            // Etag取得
            etag.put(userSelectDouble, response.getHeader(HttpHeaders.ETAG));

            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyDouble,
                    entityTypeName,
                    propertyDouble, entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null,
                    "List", false, null);

            JSONArray doubleArray = new JSONArray();
            doubleArray.add(123.123d);
            doubleArray.add(Integer.MAX_VALUE + 1L);
            doubleArray.add(0);
            doubleArray.add(-987.987d);

            body = new JSONObject();
            body.put("__id", userSelectDouble + 1);
            body.put("dynamicProperty1", "dynamicPropertyValueB");
            body.put(propertyDouble, doubleArray);

            // ユーザデータ登録(Double型)
            response = createUserData(body, HttpStatus.SC_CREATED);
            // Etag取得
            etag.put(userSelectDouble + 1, response.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得($select指定)
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$select=" + propertyDouble)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userSelectDouble, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userSelectDouble + "')"));
            uri.put(userSelectDouble + 1, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userSelectDouble + 1 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userSelectDouble, additionalprop);
            additionalprop.put("__id", userSelectDouble);
            additionalprop.put(propertyDouble, intArray);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userSelectDouble + 1, additionalprop2);
            additionalprop2.put("__id", userSelectDouble + 1);
            additionalprop2.put(propertyDouble, doubleArray);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);

        } finally {
            deleteUserData(userSelectDouble);
            deleteUserData(userSelectDouble + 1);
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyDouble, -1);
        }
    }
}
