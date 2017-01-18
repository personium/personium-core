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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

import java.util.Arrays;

/**
 * UserData一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListTest extends AbstractUserDataTest {

    static String userDataId201 = "userdata201";
    static String userDataId202 = "userdata202";

    /**
     * コンストラクタ.
     */
    public UserDataListTest() {
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
     * UserDataの一覧を正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧を正常に取得できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
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

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            String location = UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')");
            ODataCommon.checkCommonResponseHeader(response);

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
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), location, nameSpace, additional, etagList);
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataの一覧取得時にリンク情報が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧取得時にリンク情報が取得できること() {
        String linkColName = AbstractUserDataWithNP.ODATA_COLLECTION;

        // A(0..1) - B(0..1) | A(0..1) - C(1) | A(0..1) - D(*)
        String linkEntityTypeName = AbstractUserDataWithNP.ENTITY_TYPE_A;
        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", "parent");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, linkColName, linkEntityTypeName);

            // ユーザデータの取得
            TResponse response = getUserDataList(cellName, boxName, linkColName, linkEntityTypeName);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_B);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_C);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_D);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, additional);
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
            TResponse response = getUserDataList(cellName, boxName, linkColName, linkEntityTypeName);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_A);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_D);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, additional);
        } finally {
            deleteUserData(cellName, boxName, linkColName, linkEntityTypeName,
                    "parent", DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }

        // C(1) - A(0..1) |C(*) - D(*)
        linkEntityTypeName = AbstractUserDataWithNP.ENTITY_TYPE_C;
        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", "parent");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, linkColName, linkEntityTypeName);

            // ユーザデータの取得
            TResponse response = getUserDataList(cellName, boxName, linkColName, linkEntityTypeName);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_A);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_D);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, additional);
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
            TResponse response = getUserDataList(cellName, boxName, linkColName, linkEntityTypeName);

            // レスポンスボディーのチェック
            ArrayList<String> links = new ArrayList<String>();
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_A);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_B);
            links.add(AbstractUserDataWithNP.ENTITY_TYPE_C);
            Map<String, Object> additional = getLinkCheckData(linkColName, linkEntityTypeName, links);

            String nameSpace = getNameSpace(linkEntityTypeName, linkColName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, additional);
        } finally {
            deleteUserData(cellName, boxName, linkColName, linkEntityTypeName,
                    "parent", DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * UserDataが存在しないとき一覧を正常に取得できること.
     */
    @Test
    public final void UserDataが存在しないとき一覧取得して結果が0件となること() {

        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        String nameSpace = getNameSpace(entityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);

    }

    /**
     * UserDataの一覧を正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを複数登録し一覧を正常に取得できること() {
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
            TResponse respons1 = createUserData(body, HttpStatus.SC_CREATED);
            TResponse respons2 = createUserData(body2, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons1.getHeader(HttpHeaders.ETAG));
            etagList.put("userdata002", respons2.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
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
            uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')"));
            uri.put(userDataId2, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId2 + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop);
            additional.put(userDataId2, additionalprop2);
            additionalprop.put("dynamicProperty", "dynamicPropertyValue");
            additionalprop.put("__id", userDataId);
            additionalprop2.put("dynamicProperty", "dynamicPropertyValue2");
            additionalprop2.put("__id", userDataId2);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon
                    .checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etagList);
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            deleteUserData(userDataId2);
        }

    }

    /**
     * UserData一覧取得で$inlinecount指定なしの場合に__countが返却されないこと.
     */
    @Test
    public final void UserData一覧取得で$inlinecount指定なしの場合に__countが返却されないこと() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName, "");

        // レスポンスボディーのチェック.__countが存在しないことを確認する
        ODataCommon.checkResponseBodyCount(response.bodyAsJson(), ODataCommon.COUNT_NONE);
    }

    /**
     * UserData一覧取得で$inlinecountにallpagesを指定した場合に__countが返却されること.
     */
    @Test
    public final void UserData一覧取得で$inlinecountにallpagesを指定した場合に__countが返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$inlinecount=allpages");

        // レスポンスボディーのチェック.__countが存在すること(初期データ16件)を確認する
        ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 16);
    }

    /**
     * UserData一覧取得で$inlinecountにnoneを指定した場合に__countが返却されないこと.
     */
    @Test
    public final void UserData一覧取得で$inlinecountにnoneを指定した場合に__countが返却されないこと() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName,
                boxName,
                colName,
                sdEntityTypeName,
                "?$inlinecount=none");

        // レスポンスボディーのチェック.__countが存在しないことを確認する
        ODataCommon.checkResponseBodyCount(response.bodyAsJson(), ODataCommon.COUNT_NONE);
    }

    /**
     * UserData一覧取得で$inlinecountに値を指定しない場合にステータスコード400が返却されること.
     */
    @Test
    public final void UserData一覧取得で$inlinecountに値を指定しない場合にステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$inlinecount=")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserData一覧取得で$inlinecountに有効値以外の値を指定した場合にステータスコード400が返却されること.
     */
    @Test
    public final void UserData一覧取得で$inlinecountに有効値以外の値を指定した場合にステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$inlinecount=xxx")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに$top指定無しの場合デフォルト件数取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに$top指定無しの場合デフォルト件数取得できること() {
        entityTypeName = "Category";

        try {
            // ユーザデータ作成
            for (int i = 0; i < 26; i++) {
                JSONObject body = new JSONObject();
                body.put("__id", "UserData" + i);
                body.put("dynamicProperty", "dynamicProperty" + String.format("%02d", i));

                createUserData(body, HttpStatus.SC_CREATED);
            }

            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$orderby=dynamicProperty")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            ArrayList<String> uri = new ArrayList<String>();
            for (int i = 0; i < 25; i++) {
                uri.add(UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('UserData" + i + "')"));
            }

            ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
        } finally {
            for (int i = 0; i < 26; i++) {
                deleteUserData("UserData" + i);
            }
        }
    }

    /**
     * UserDataの一覧取得で$formatにatomを指定した場合レスポンスがxml形式になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧取得で$formatにatomを指定した場合レスポンスがxml形式になること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$format=atom")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            String resContentType = response.getHeader(HttpHeaders.CONTENT_TYPE);
            assertEquals(MediaType.APPLICATION_ATOM_XML, resContentType.split(";")[0]);
            response.bodyAsXml();
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataの一覧取得で$formatにjsonを指定した場合レスポンスがJSON形式になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧取得で$formatにjsonを指定した場合レスポンスがJSON形式になること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$format=json")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            String resContentType = response.getHeader(HttpHeaders.CONTENT_TYPE);
            assertEquals(MediaType.APPLICATION_JSON, resContentType.split(";")[0]);
            response.bodyAsJson();
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataの一覧取得でAcceptヘッダにATOM_XMLを指定した場合レスポンスがxml形式になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧取得でAcceptヘッダにATOM_XMLを指定した場合レスポンスがxml形式になること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_ATOM_XML)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            String resContentType = response.getHeader(HttpHeaders.CONTENT_TYPE);
            assertEquals(MediaType.APPLICATION_ATOM_XML, resContentType.split(";")[0]);
            response.bodyAsXml();
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの一覧取得でAcceptヘッダにXMLを指定した場合レスポンスがxml形式になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧取得でAcceptヘッダにXMLを指定した場合レスポンスがxml形式になること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_XML)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            String resContentType = response.getHeader(HttpHeaders.CONTENT_TYPE);
            assertEquals(MediaType.APPLICATION_ATOM_XML, resContentType.split(";")[0]);
            response.bodyAsXml();
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの一覧でDouble配列型プロパティにnullを含むデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧でDouble配列型プロパティにnullを含むデータを取得できること() {
        String propName = "arrayDoubleTypeProperty";
        String userDataId = "userdata001";
        // リクエスト実行
        try {
            // Edm.Double配列のプロパティを作成
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(),
                    true, null, "List", false, null, HttpStatus.SC_CREATED);
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            JSONArray arrayBody = new JSONArray();
            arrayBody.addAll(Arrays.asList(new Double[] {1.1, null, -1.2 }));
            body.put("__id", userDataId);
            body.put(propName, arrayBody);
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop);
            additionalprop.put("__id", userDataId);
            additionalprop.put(propName, arrayBody);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon
                    .checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etagList);

        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            // プロパティ削除
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName, -1);
        }
    }

    /**
     * UserDataの一覧でInt32配列型プロパティにnullを含むデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧でInt32配列型プロパティにnullを含むデータを取得できること() {
        String propName = "arrayIntTypeProperty";
        String userDataId = "userdata001";
        // リクエスト実行
        try {
            // Edm.Double配列のプロパティを作成
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(),
                    true, null, "List", false, null, HttpStatus.SC_CREATED);
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            JSONArray arrayBody = new JSONArray();
            arrayBody.addAll(Arrays.asList(new Integer[] {1, null, -1 }));
            body.put("__id", userDataId);
            body.put(propName, arrayBody);
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop);
            additionalprop.put("__id", userDataId);
            additionalprop.put(propName, arrayBody);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon
                    .checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etagList);

        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            // プロパティ削除
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName, -1);
        }
    }

    /**
     * UserDataの一覧でBoolean配列型プロパティにnullを含むデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧でBoolean配列型プロパティにnullを含むデータを取得できること() {
        String propName = "arrayIntTypeProperty";
        String userDataId = "userdata001";
        // リクエスト実行
        try {
            // Edm.Double配列のプロパティを作成
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(),
                    true, null, "List", false, null, HttpStatus.SC_CREATED);
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            JSONArray arrayBody = new JSONArray();
            arrayBody.addAll(Arrays.asList(new Boolean[] {true, null, false }));
            body.put("__id", userDataId);
            body.put(propName, arrayBody);
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop);
            additionalprop.put("__id", userDataId);
            additionalprop.put(propName, arrayBody);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon
                    .checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etagList);

        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            // プロパティ削除
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName, -1);
        }
    }

    /**
     * UserDataの一覧で文字列配列型プロパティにnullを含むデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの一覧で文字列配列型プロパティにnullを含むデータを取得できること() {
        String propName = "arrayIntTypeProperty";
        String userDataId = "userdata001";
        // リクエスト実行
        try {
            // Edm.Double配列のプロパティを作成
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(),
                    true, null, "List", false, null, HttpStatus.SC_CREATED);
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            JSONArray arrayBody = new JSONArray();
            arrayBody.addAll(Arrays.asList(new String[] {"abc", null, "xyz", "null" }));
            body.put("__id", userDataId);
            body.put(propName, arrayBody);
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop);
            additionalprop.put("__id", userDataId);
            additionalprop.put(propName, arrayBody);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon
                    .checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etagList);

        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            // プロパティ削除
            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propName, -1);
        }
    }
}
