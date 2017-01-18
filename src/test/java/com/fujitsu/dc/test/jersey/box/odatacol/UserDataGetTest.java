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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
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
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DaoException;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserData取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataGetTest extends AbstractUserDataTest {

    String cellName = "testcell1";
    String boxName = "box1";
    String colName = "setodata";
    String entityTypeName = "Category";
    String userDataId = "userdata001";

    /**
     * コンストラクタ.
     */
    public UserDataGetTest() {
        super();
    }

    /**
     * UserDataを新規作成して正常に取得できること.
     */
    @Test
    public final void UserDataを新規作成して正常に取得できること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("dynamicProperty", "dynamicPropertyValue");
            additional.put("secondDynamicProperty", "secondDynamicPropertyValue");
            additional.put("nullProperty", null);
            additional.put("intProperty", 123);
            additional.put("floatProperty", 123.123);
            additional.put("trueProperty", true);
            additional.put("falseProperty", false);
            additional.put("nullStringProperty", "null");
            additional.put("intStringProperty", "123");
            additional.put("floatStringProperty", "123.123");
            additional.put("trueStringProperty", "true");
            additional.put("falseStringProperty", "false");

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserData取得時にリンク情報が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserData取得時にリンク情報が取得できること() {
        String linkColName = AbstractUserDataWithNP.ODATA_COLLECTION;

        // A(0..1) - B(0..1) | A(0..1) - C(1) | A(0..1) - D(*)
        String linkEntityTypeName = AbstractUserDataWithNP.ENTITY_TYPE_A;
        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", "parent");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, linkColName, linkEntityTypeName);

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, linkColName,
                    linkEntityTypeName, "parent", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_B);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_C);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_D);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional);
        } finally {
            deleteUserData(cellName, boxName, linkColName, linkEntityTypeName,
                    "parent", DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }

        // B(0..1) - A(0..1) | B(1) - D(*)
        linkEntityTypeName = AbstractUserDataWithNP.ENTITY_TYPE_B;
        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", "parent");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, linkColName, linkEntityTypeName);

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, linkColName,
                    linkEntityTypeName, "parent", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_A);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_D);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional);
        } finally {
            deleteUserData(cellName, boxName, linkColName, linkEntityTypeName,
                    "parent", DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }

        // C(1) - A(0..1) | C(*) - D(*)
        linkEntityTypeName = AbstractUserDataWithNP.ENTITY_TYPE_C;
        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", "parent");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, linkColName, linkEntityTypeName);

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, linkColName,
                    linkEntityTypeName, "parent", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_A);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_D);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional);
        } finally {
            deleteUserData(cellName, boxName, linkColName, linkEntityTypeName,
                    "parent", DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }

        // D(*) - A(0..1) | D(*) - B(1) | D(*) - C(*)
        linkEntityTypeName = AbstractUserDataWithNP.ENTITY_TYPE_D;
        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", "parent");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, linkColName, linkEntityTypeName);

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, linkColName,
                    linkEntityTypeName, "parent", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_A);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_B);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_C);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional);
        } finally {
            deleteUserData(cellName, boxName, linkColName, linkEntityTypeName,
                    "parent", DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * UserData取得の存在しないセルを指定するテスト.
     */
    @Test
    public final void UserData取得の存在しないセルを指定するテスト() {

        // GETを実行
        getUserData("cellhoge", boxName, colName, entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData取得の存在しないBoxを指定するテスト.
     */
    @Test
    public final void UserData取得の存在しないBoxを指定するテスト() {

        // GETを実行
        getUserData(cellName, "boxhoge", colName, entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData取得の存在しないODataCollectionを指定するテスト.
     */
    @Test
    public final void UserData取得の存在しないODataCollectionを指定するテスト() {

        // GETを実行
        getUserData(cellName, boxName, "colhoge", entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData取得の存在しないEntitySetを指定するテスト.
     */
    @Test
    public final void UserData取得の存在しないEntitySetを指定するテスト() {

        // GETを実行
        getUserData(cellName, boxName, colName, "entityTypehoge",
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData取得の存在しないEntityを指定するテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserData取得の存在しないEntityを指定するテスト() {

        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");
        body.put("nullProperty", null);
        body.put("intProperty", 123);
        body.put("trueProperty", true);
        body.put("falseProperty", false);

        try {
            // リクエスト実行
            createUserData(body, HttpStatus.SC_CREATED);

            // GETを実行
            getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId + "hoge", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserData取得の無効な認証トークンを指定するテスト.
     */
    @Test
    public final void UserData取得の無効な認証トークンを指定するテスト() {

        // GETを実行
        getUserData(cellName, boxName, colName, entityTypeName,
                "userDatahaoge", "tokenhoge", HttpStatus.SC_UNAUTHORIZED);

    }

    /**
     * UserDataを$selectクエリに存在するString型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataを$selectクエリに存在するString型のプロパティ名を指定して対象のデータのみ取得できること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=dynamicProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("dynamicProperty", "dynamicPropertyValue");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在するlong型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataを$selectクエリに存在するlong型のプロパティ名を指定して対象のデータのみ取得できること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=intProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("intProperty", 123);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在するfloat型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataを$selectクエリに存在するfloat型のプロパティ名を指定して対象のデータのみ取得できること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=floatProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("floatProperty", 123.123);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在するboolean型のプロパティ名を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataを$selectクエリに存在するboolean型のプロパティ名を指定して対象のデータのみ取得できること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=falseProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("falseProperty", false);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * $selectクエリにIDを指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリにIDを指定して正常に取得できること() {

        try {
            // ユーザデータ作成
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=__id",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * $selectクエリにpublishedを指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリにpublishedを指定して正常に取得できること() {

        try {
            // ユーザデータ作成
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=__published",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * $selectクエリにupdatedを指定して正常に取得できること.
     */
    @Test
    public final void $selectクエリにupdatedを指定して正常に取得できること() {

        try {
            // ユーザデータ作成
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=__updated",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在するnullプロパティ名を指定して管理情報のみ返却されること.
     */
    @Test
    public final void UserDataを$selectクエリに存在するnullプロパティ名を指定して管理情報のみ返却されること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=nullProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在しないプロパティ名を指定して管理情のみが返却されること.
     */
    @Test
    public final void UserDataを$selectクエリに存在しないプロパティ名を指定して管理情のみが返却されること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=dummyProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在するプロパティ名を複数指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataを$selectクエリに存在するプロパティ名を複数指定して対象のデータのみ取得できること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=secondDynamicProperty,nullProperty,intProperty,floatProperty,"
                            + "trueProperty,falseProperty,nullStringProperty,intStringProperty,"
                            + "floatStringProperty,trueStringProperty,falseStringProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("secondDynamicProperty", "secondDynamicPropertyValue");
            additional.put("intProperty", 123);
            additional.put("floatProperty", 123.123);
            additional.put("trueProperty", true);
            additional.put("falseProperty", false);
            additional.put("nullStringProperty", "null");
            additional.put("intStringProperty", "123");
            additional.put("floatStringProperty", "123.123");
            additional.put("trueStringProperty", "true");
            additional.put("falseStringProperty", "false");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在するプロパティ名と存在しないプロパティ名を指定して正しい値が返却されること.
     */
    @Test
    public final void UserDataを$selectクエリに存在するプロパティ名と存在しないプロパティ名を指定して正しい値が返却されること() {
        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=dynamicProperty,noneExistProperty",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("dynamicProperty", "dynamicPropertyValue");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに存在しないプロパティ名を複数指定して管理情報のみ返却されること.
     */
    @Test
    public final void UserDataを$selectクエリに存在しないプロパティ名を複数指定して管理情報のみ返却されること() {
        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=noneExistProperty,noneExistProperty2",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを$selectクエリに*を指定してすべてのデータが取得できること.
     */
    @Test
    public final void UserDataを$selectクエリにアスタリスクを指定してすべてのデータが取得できること() {

        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=*",
                    HttpStatus.SC_OK);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("dynamicProperty", "dynamicPropertyValue");
            additional.put("secondDynamicProperty", "secondDynamicPropertyValue");
            additional.put("nullProperty", null);
            additional.put("intProperty", 123);
            additional.put("floatProperty", 123.123);
            additional.put("trueProperty", true);
            additional.put("falseProperty", false);
            additional.put("nullStringProperty", "null");
            additional.put("intStringProperty", "123");
            additional.put("floatStringProperty", "123.123");
            additional.put("trueStringProperty", "true");
            additional.put("falseStringProperty", "false");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの$selectクエリに値を指定しない場合に４００が返却されること.
     */
    @Test
    public final void UserDataの$selectクエリに値を指定しない場合に４００が返却されること() {
        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=",
                    HttpStatus.SC_BAD_REQUEST);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの$selectクエリにパースエラーとなる文字を指定した場合に４００が返却されること.
     */
    @Test
    public final void UserDataの$selectクエリにパースエラーとなる文字を指定した場合に４００が返却されること() {
        // リクエスト実行
        try {
            createUserData();

            // ユーザデータの取得
            getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "?\\$select=!",
                    HttpStatus.SC_BAD_REQUEST);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの取得で$formatにatomを指定した場合レスポンスがxml形式になること.
     */
    @Test
    public final void UserDataの取得で$formatにatomを指定した場合レスポンスがxml形式になること() {

        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, "?\\$format=atom", HttpStatus.SC_OK);

            assertEquals(MediaType.APPLICATION_ATOM_XML, response.getHeader(HttpHeaders.CONTENT_TYPE));
            response.bodyAsXml();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの取得で$formatにjsonを指定した場合レスポンスがJSON形式になること.
     */
    @Test
    public final void UserDataの取得で$formatにjsonを指定した場合レスポンスがJSON形式になること() {

        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = getUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, "?\\$format=json", HttpStatus.SC_OK);

            assertEquals(MediaType.APPLICATION_JSON, response.getHeader(HttpHeaders.CONTENT_TYPE));
            response.bodyAsJson();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの取得でAcceptヘッダにATOM_XMLを指定した場合レスポンスがxml形式になること.
     */
    @Test
    public final void UserDataの取得でAcceptヘッダにATOM_XMLを指定した場合レスポンスがxml形式になること() {

        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = Http.request("box/odatacol/get.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("accept", MediaType.APPLICATION_ATOM_XML)
                    .with("query", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            assertEquals(MediaType.APPLICATION_ATOM_XML, response.getHeader(HttpHeaders.CONTENT_TYPE));
            response.bodyAsXml();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの取得でAcceptヘッダにXMLを指定した場合レスポンスがxml形式になること.
     */
    @Test
    public final void UserDataの取得でAcceptヘッダにXMLを指定した場合レスポンスがxml形式になること() {

        try {
            createUserData();

            // ユーザデータの取得
            TResponse response = Http.request("box/odatacol/get.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("accept", MediaType.APPLICATION_XML)
                    .with("query", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            assertEquals(MediaType.APPLICATION_ATOM_XML, response.getHeader(HttpHeaders.CONTENT_TYPE));
            response.bodyAsXml();
        } finally {
            deleteUserData(userDataId);
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

            // $selectにてdynamicPropertyを指定して一件取得
            String query = "?\\$select=dynamicProperty";
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", "001", AbstractCase.MASTER_TOKEN_NAME, query, HttpStatus.SC_OK);

            // 応答の確認
            JSONObject responseJson = response.bodyAsJson();
            JSONObject result = ((JSONObject) ((JSONObject) responseJson.get("d")).get("results"));
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

            // $selectにてdynamicPropertyを指定して一件取得
            String query = "?\\$select=stringProperty";
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", "001", AbstractCase.MASTER_TOKEN_NAME, query, HttpStatus.SC_OK);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONObject result = ((JSONObject) ((JSONObject) responseJson.get("d")).get("results"));
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

            // $selectにてdynamicPropertyを指定して一件取得
            String query = "?\\$select=complexProperty";
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", "001", AbstractCase.MASTER_TOKEN_NAME, query, HttpStatus.SC_OK);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONObject result = ((JSONObject) ((JSONObject) responseJson.get("d")).get("results"));
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

            // $selectにてdynamicPropertyを指定して一件取得
            String query = "?\\$select=notExistsProperty";
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", "001", AbstractCase.MASTER_TOKEN_NAME, query, HttpStatus.SC_OK);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONObject result = ((JSONObject) ((JSONObject) responseJson.get("d")).get("results"));
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

            // $selectにてdynamicPropertyを指定して一件取得
            String query = "?\\$select=dynamicProperty,complexProperty";
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", "001", AbstractCase.MASTER_TOKEN_NAME, query, HttpStatus.SC_OK);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONObject result = ((JSONObject) ((JSONObject) responseJson.get("d")).get("results"));
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

            // $selectにてdynamicPropertyを指定して一件取得
            String query = "?\\$select=*";
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "selectTestEntity", "001", AbstractCase.MASTER_TOKEN_NAME, query, HttpStatus.SC_OK);

            // 応答の確認
            response.statusCode(HttpStatus.SC_OK);
            JSONObject responseJson = response.bodyAsJson();
            JSONObject result = ((JSONObject) ((JSONObject) responseJson.get("d")).get("results"));
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
     * 制御コードを含むUserDataの一件取得時に制御コードがエスケープされて取得できること.
     * @throws DaoException ボディのパースに失敗
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void 制御コードを含むUserDataの一件取得時に制御コードがエスケープされて取得できること() throws DaoException {

        // リクエスト実行
        try {
            // ユーザデータの作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("testField", "value_\\u0001_value"); // 0x0a(Ctl-A)
            createUserData(body, HttpStatus.SC_CREATED);

            // ユーザデータの一件取得
            String url = String.format("%s/%s/%s/%s/%s('%s')",
                    UrlUtils.getBaseUrl(), cellName, boxName, colName, entityTypeName, userDataId);
            DcRequest request = DcRequest.get(url);
            request.header("Accept", "application/json");
            request.header("Authorization", "Bearer " + AbstractCase.MASTER_TOKEN_NAME);
            DcResponse dres = ODataCommon.request(request);
            assertEquals(dres.getStatusCode(), HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            String resBody = dres.bodyAsString();
            assertTrue(resBody.contains("\\u0001"));
            assertFalse(resBody.contains("\u0001"));

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * 制御コードを含むUserDataの一覧取得時に制御コードがエスケープされて取得できること.
     * @throws DaoException ボディのパースに失敗
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void 制御コードを含むUserDataの一覧取得時に制御コードがエスケープされて取得できること() throws DaoException {

        // リクエスト実行
        try {
            // ユーザデータの作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("testField", "value_\\u0001_value"); // 0x0a(Ctl-A)
            createUserData(body, HttpStatus.SC_CREATED);

            // ユーザデータの一覧取得
            String url = String.format("%s/%s/%s/%s/%s",
                    UrlUtils.getBaseUrl(), cellName, boxName, colName, entityTypeName);
            DcRequest request = DcRequest.get(url);
            request.header("Accept", "application/json");
            request.header("Authorization", "Bearer " + AbstractCase.MASTER_TOKEN_NAME);
            DcResponse dres = ODataCommon.request(request);
            assertEquals(dres.getStatusCode(), HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            String resBody = dres.bodyAsString();
            assertTrue(resBody.contains("\\u0001"));
            assertFalse(resBody.contains("\u0001"));
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataをシングルクォート無しで取得した場合400エラーとなること.
     */
    @Test
    public final void UserDataをシングルクォート無しで取得した場合400エラーとなること() {

        String userdataKey = "123456";

        // リクエスト実行
        try {
            createUserData(userdataKey);

            // ユーザデータ更新
            String requestURL =
                    UrlUtils.userdata(cellName, boxName, colName, entityTypeName + "(" + userdataKey + ")", null);
            DcRequest req = DcRequest.get(requestURL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.IF_MATCH, "*");

            // リクエスト実行
            DcResponse res = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                    DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
        } finally {
            deleteUserData(userdataKey);
        }
    }

    private void createUserData() {
        createUserData(userDataId);
    }

    @SuppressWarnings("unchecked")
    private void createUserData(String key) {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", key);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");
        body.put("nullProperty", null);
        body.put("intProperty", 123);
        body.put("floatProperty", 123.123);
        body.put("trueProperty", true);
        body.put("falseProperty", false);
        body.put("nullStringProperty", "null");
        body.put("intStringProperty", "123");
        body.put("floatStringProperty", "123.123");
        body.put("trueStringProperty", "true");
        body.put("falseStringProperty", "false");

        createUserData(body, HttpStatus.SC_CREATED);
    }
}
