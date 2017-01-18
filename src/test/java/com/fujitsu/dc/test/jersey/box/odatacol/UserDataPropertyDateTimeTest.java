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
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.utils.ODataUtils;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserDataComplexType登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataPropertyDateTimeTest extends AbstractUserDataTest {

    /** ODATA Collection名. */
    protected static final String COL_NAME = "propCol";
    /** EntityType名. */
    protected static final String ENTITY_TYPE_NAME = "userDataEntityType";
    /** Property名. */
    protected static final String PROP_NAME = "propName";
    /** EntitySet名. */
    protected static final String USERDATA_ID = "userdata001";
    /** EntitySet名. */
    protected static final String USERDATA_ID2 = "userdata002";

    /**
     * コンストラクタ.
     */
    public UserDataPropertyDateTimeTest() {
        super();
    }

    /**
     * PropertyのTypeがEdmStringでユーザデータが予約語の場合予約語の内容がそのまま作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmStringでユーザデータが予約語の場合予約語の内容がそのまま作成できること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, Common.SYSUTCDATETIME);

        try {
            createEntities();
            createProperty(EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            String value = results.get(PROP_NAME).toString();
            assertEquals(Common.SYSUTCDATETIME, value);

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertTrue(ODataUtils.validateDateTime(getValue));

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }

    }

    /**
     * PropertyのTypeがEdmDateTimeでnullが許可の場合ユーザデータにnullを設定してデフォルト値で作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでnullが許可の場合ユーザデータにnullを設定してデフォルト値で作成できること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, null);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            String value = results.get(PROP_NAME).toString();
            assertTrue(ODataUtils.validateDateTime(value));

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertTrue(ODataUtils.validateDateTime(getValue));

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }

    }

    /**
     * PropertyのTypeがEdmDateTimeでnullが許可の場合ユーザデータに空文字を設定するとBadRequestになること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでnullが許可の場合ユーザデータに空文字を設定するとBadRequestになること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, "");

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_BAD_REQUEST,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            String code = DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode();
            String message = DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PROP_NAME).getMessage();
            checkErrorResponse(response.bodyAsJson(), code, message);

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでnullが許可の場合ユーザデータに予約語を大文字で設定しても作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでnullが許可の場合ユーザデータに予約語を大文字で設定しても作成できること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, Common.SYSUTCDATETIME);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            String value = results.get(PROP_NAME).toString();
            assertTrue(ODataUtils.validateDateTime(value));

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertTrue(ODataUtils.validateDateTime(getValue));
        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }

    }

    /**
     * PropertyのTypeがEdmDateTimeでユーザデータに不正な書式を指定するとBadRequestになること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでユーザデータに不正な書式を指定するとBadRequestになること() {
        final String time = "/Date(1359340262406)";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_BAD_REQUEST,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            String code = DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode();
            String message = DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PROP_NAME).getMessage();
            checkErrorResponse(response.bodyAsJson(), code, message);

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでユーザデータに正規の書式を指定すると作成されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでユーザデータに正規の書式を指定すると作成されること() {
        final String time = "/Date(1359340262406)/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);
        JSONObject body2 = new JSONObject();
        body2.put("__id", USERDATA_ID2);
        body2.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ1作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            String value = results.get(PROP_NAME).toString();
            assertEquals(time, value);

            // ユーザデータ2作成
            TResponse response2 = createUserData(body2, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json2 = response2.bodyAsJson();
            JSONObject results2 = (JSONObject) ((JSONObject) json2.get("d")).get("results");
            String value2 = results2.get(PROP_NAME).toString();
            assertEquals(time, value2);

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertTrue(ODataUtils.validateDateTime(getValue));

            // ユーザデータ一覧取得
            TResponse listResponse = getUserDataList(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            String nameSpace = getNameSpace(ENTITY_TYPE_NAME, COL_NAME);
            ODataCommon.checkResponseBodyList(listResponse.bodyAsJson(), null, nameSpace, null);

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);
            deleteUserData(USERDATA_ID2);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでユーザデータに最大値を指定すると正常終了すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでユーザデータに最大値を指定すると正常終了すること() {
        final String time = "/Date(" + ODataUtils.DATETIME_MAX +  ")/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject getJson = response.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertTrue(ODataUtils.validateDateTime(getValue));
        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでユーザデータに最小値を指定すると正常終了すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでユーザデータに最小値を指定すると正常終了すること() {
        final String time = "/Date(" + ODataUtils.DATETIME_MIN +  ")/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject getJson = response.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertTrue(ODataUtils.validateDateTime(getValue));
        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでユーザデータに最大値より大きい値を指定すると400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでユーザデータに最大値より大きい値を指定すると400エラーとなること() {
        final String time = "/Date(" + (ODataUtils.DATETIME_MAX + 1) +  ")/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_BAD_REQUEST,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            String code = json.get("code").toString();
            assertEquals("PR400-OD-0006", code);
        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでユーザデータに最小値より小さい値を指定すると400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでユーザデータに最小値より小さい値を指定すると400エラーとなること() {
        final String time = "/Date(" + (ODataUtils.DATETIME_MIN - 1) + ")/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_BAD_REQUEST,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            String code = json.get("code").toString();
            assertEquals("PR400-OD-0006", code);
        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでユーザデータにint型の範囲の値を指定しても正常終了すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeでユーザデータにint型の範囲の値を指定しても正常終了すること() {
        final String time = "/Date(922)/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName());

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject getJson = response.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertTrue(ODataUtils.validateDateTime(getValue));

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            getJson = getResponse.bodyAsJson();
            getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            getValue = (String) getResults.get(PROP_NAME);
            assertEquals("/Date(922)/", getValue);
        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeのときnullで更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void PropertyのTypeがEdmDateTimeのときnullで更新できること() {
        final String time = "/Date(1359340262406)/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName(), null);

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            String value = results.get(PROP_NAME).toString();
            assertEquals(time, value);

            body.put(PROP_NAME, null);
            // ユーザデータ更新
            updateUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID, body);

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertEquals(null, getValue);

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * 配列のPropertyのTypeがEdmDateTimeのときnullで更新できること. 配列にEdmDateTime型のPropertyを登録できないため、Ignoreとしている.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    public final void 配列のPropertyのTypeがEdmDateTimeのときnullで更新できること() {
        final String time = "/Date(1359340262406)/";
        // リクエストボディを設定
        JSONArray etListPropStr = new JSONArray();
        etListPropStr.add(time);
        etListPropStr.add(time);
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, etListPropStr);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName(), null, "List");

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            assertEquals("[/Date(1360037777872)/,/Date(1360037777872)/]", results.get(PROP_NAME).toString());

            etListPropStr.add(null);
            etListPropStr.add(null);
            etListPropStr.remove(0);
            etListPropStr.remove(0);
            body.put(PROP_NAME, etListPropStr);
            // ユーザデータ更新
            updateUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID, body);

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            assertEquals("[null,null]", getResults.get(PROP_NAME).toString());
        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * DynamicPropertyをnullにして更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void DynamicPropertyをnullにして更新できること() {
        final String time = "/Date(1359340262406)/";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);
        body.put(PROP_NAME, time);

        try {
            createEntities();
            createProperty(EdmSimpleType.DATETIME.getFullyQualifiedTypeName(), null);

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            String value = results.get(PROP_NAME).toString();
            assertEquals(time, value);

            body.put(PROP_NAME, null);
            body.put("nullprop", null);
            // ユーザデータ更新
            updateUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID, body);

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = (JSONObject) ((JSONObject) getJson.get("d")).get("results");
            String getValue = (String) getResults.get(PROP_NAME);
            assertEquals(null, getValue);

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);

            deleteProperty();
            deleteEntities();
        }
    }

    /**
     * TypeがEdmSingleのPropertyにnullで登録し取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void TypeがEdmSingleのPropertyにnullで登録し取得できること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID);

        try {
            createEntities();
            createProperty(EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), null, null);

            // ユーザデータ作成
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response, response.getLocationHeader());

            JSONObject json = response.bodyAsJson();
            JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            String value = (String) results.get(PROP_NAME);
            assertEquals(null, value);

            // ユーザデータ一件取得
            getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, USERDATA_ID, Setup.MASTER_TOKEN_NAME,
                    HttpStatus.SC_OK);
            json = response.bodyAsJson();
            results = (JSONObject) ((JSONObject) json.get("d")).get("results");
            value = (String) results.get(PROP_NAME);
            assertEquals(null, value);

        } finally {
            // ユーザデータ削除
            deleteUserData(USERDATA_ID);
            deleteProperty();
            deleteEntities();
        }

    }

    /**
     * EntityTypeを作成する.
     * @param name EntityTypeのName
     * @param odataName oadataコレクション名
     * @return レスポンス
     */
    protected TResponse createEntityType(String name, String odataName) {
        return Http.request("box/entitySet-post.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("odataSvcPath", odataName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + DcCoreConfig.getMasterToken())
                .with("Name", name)
                .returns()
                .debug();
    }

    /**
     * テストに必要なEntityを作成する.
     */
    protected void createEntities() {
        DavResourceUtils.createODataCollection(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, cellName, boxName,
                COL_NAME);
        createEntityType(ENTITY_TYPE_NAME, COL_NAME);

    }

    /**
     * プロパティを作成する.
     * @param type type.
     */
    private void createProperty(String type) {
        createProperty(type, "SYSUTCDATETIME()", null);
    }

    /**
     * プロパティを作成する.
     * @param type type.
     * @param defValue デフォルト値.
     */
    private void createProperty(String type, String defValue) {
        createProperty(type, defValue, null);
    }

    /**
     * プロパティを作成する.
     * @param type データ型名.
     * @param defValue デフォルト値.
     * @param colkindValue 配列かどうか.
     */
    private void createProperty(String type, String defValue, String colkindValue) {
        // リクエストパラメータ設定
        String locationUrl = UrlUtils.property(cellName, boxName, COL_NAME, null, null);
        DcRequest req = DcRequest.post(locationUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, PROP_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, ENTITY_TYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, type);
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defValue);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, colkindValue);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
    }

    /**
     * ユーザーデータを削除する.
     * @param userDataId 削除対象ID
     */
    protected void deleteUserData(String userDataId) {
        // リクエスト実行
        Http.request("box/odatacol/delete.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", COL_NAME)
                .with("entityType", ENTITY_TYPE_NAME)
                .with("id", userDataId)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns()
                .debug();
    }

    /**
     * テストに必要なEntityを削除する.
     */
    protected void deleteEntities() {
        deleteProperty();
        // エンティティタイプを削除
        EntityTypeUtils.delete(COL_NAME, MASTER_TOKEN_NAME,
                "application/json", ENTITY_TYPE_NAME, cellName, -1);
        // コレクションを削除
        DavResourceUtils.deleteCollection(cellName, boxName, COL_NAME, DcCoreConfig.getMasterToken(), -1);
    }

    /**
     * Propertyを削除する.
     */
    protected void deleteProperty() {
        String locationUrl = UrlUtils.property(cellName, boxName, COL_NAME, PROP_NAME,
                ENTITY_TYPE_NAME);
        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(locationUrl);
    }
}
