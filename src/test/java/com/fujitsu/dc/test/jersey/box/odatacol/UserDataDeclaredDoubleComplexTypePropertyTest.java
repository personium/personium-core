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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.utils.ODataUtils;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
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
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataComplexType登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataDeclaredDoubleComplexTypePropertyTest extends AbstractUserDataTest {

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
    private static final String COMPLEX_TYPE_NAME = "complexTypeTest";

    /**
     * コンストラクタ.
     */
    public UserDataDeclaredDoubleComplexTypePropertyTest() {
        super();
    }

    /**
     * ComplexPropertyがEdmDoubleで数値形式のデータを登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexPropertyがEdmDoubleで数値形式のデータを登録できること() {
        String userdataId1 = "doubleTest1";
        String userdataId2 = "doubleTest2";
        String userdataId3 = "doubleTest3";
        String userdataId4 = "doubleTest4";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);

            // Int32型で登録・取得可能なこと
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, Integer.MAX_VALUE);
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            TResponse response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, String.valueOf(Integer.MAX_VALUE));

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, String.valueOf(Integer.MAX_VALUE));

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId1, listResponse, String.valueOf(Integer.MAX_VALUE));

            // Double型で登録・取得可能なこと
            complexBody = new JSONObject();
            complexBody.put(PROP_NAME, 1234567890.12345d);
            body = new JSONObject();
            body.put("__id", userdataId2);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, String.valueOf(1234567890.12345d));

            // ユーザデータ一件取得
            getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId2,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, String.valueOf(1234567890.12345d));

            // ユーザデータ一覧取得
            listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId2, listResponse, String.valueOf(1234567890.12345d));

            // Single型で登録・取得可能なこと
            complexBody = new JSONObject();
            complexBody.put(PROP_NAME, 12345.54321d);
            body = new JSONObject();
            body.put("__id", userdataId3);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, String.valueOf(12345.54321d));

            // ユーザデータ一件取得
            getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId3,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, String.valueOf(12345.54321d));

            // ユーザデータ一覧取得
            listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId3, listResponse, String.valueOf(12345.54321d));

            // Long型で登録・取得可能なこと
            complexBody = new JSONObject();
            complexBody.put(PROP_NAME, Long.MAX_VALUE);
            body = new JSONObject();
            body.put("__id", userdataId4);
            body.put(PROP_NAME, complexBody);

            double expected = Double.parseDouble(String.valueOf(Long.MAX_VALUE));
            DecimalFormat format = new DecimalFormat("#.#");
            format.setMaximumIntegerDigits(15);
            format.setMaximumFractionDigits(14);
            String fomattedValue = format.format(expected);
            String expectedString;
            if (expected != Double.parseDouble(fomattedValue)) {
                expectedString = Double.toString(expected);
            } else {
                expectedString = fomattedValue;
            }

            // ユーザデータ作成
            response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, expectedString);

            // ユーザデータ一件取得
            getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId4,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, expectedString);

            // ユーザデータ一覧取得
            listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId4, listResponse, expectedString);

        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);
            deleteUserData(userdataId2);
            deleteUserData(userdataId3);
            deleteUserData(userdataId4);

            deleteEntities();
        }

    }

    /**
     * 小数値データ登録後に整数値データが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 小数値データ登録後に整数値データが登録できること() {
        String userdataId1 = "doubleTest1";
        String userdataId2 = "doubleTest2";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);

            // Double型で登録・取得可能なこと
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, 1234567890.12345d);
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            TResponse response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, String.valueOf(1234567890.12345d));

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, String.valueOf(1234567890.12345d));

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId1, listResponse, String.valueOf(1234567890.12345d));

            // Int32型で登録・取得可能なこと
            complexBody = new JSONObject();
            complexBody.put(PROP_NAME, Integer.MAX_VALUE);
            body = new JSONObject();
            body.put("__id", userdataId2);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, String.valueOf(Integer.MAX_VALUE));

            // ユーザデータ一件取得
            getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId2,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, String.valueOf(Integer.MAX_VALUE));

            // ユーザデータ一覧取得
            listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId2, listResponse, String.valueOf(Integer.MAX_VALUE));
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);
            deleteUserData(userdataId2);

            deleteEntities();
        }

    }

    /**
     * Double型にNullが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型にNullが登録できること() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);

            // Double型で登録・取得可能なこと
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, null);
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            TResponse response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            JSONObject json = response.bodyAsJson();
            JSONObject results = getResult(json);
            Object value = ((JSONObject) results.get(PROP_NAME)).get(PROP_NAME);
            assertNull(value);

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = getResult(getJson);
            Object getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME);
            assertNull(getValue);

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject listJson = listResponse.bodyAsJson();
            getResults = getResultsFromId(userdataId1, listJson);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME);
            assertNull(getValue);
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);

            deleteEntities();
        }

    }

    /**
     * Double型に文字列のデータが登録できないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型に文字列のデータが登録できないこと() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);

            // Double型で登録・取得可能なこと
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, "DoubleTest");
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteEntities();
        }
    }

    /**
     * Double型に文字列型の数値のデータが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型に文字列型の数値のデータが登録できること() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);

            // Double型で登録・取得可能なこと
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, "12345.123456789");
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            TResponse response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, "12345.123456789");

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, "12345.123456789");

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId1, listResponse, "12345.123456789");
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);

            deleteEntities();
        }
    }

    /**
     * Double型に最小値より小さい値のデータが登録できないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型に最小値より小さい値のデータが登録できないこと() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);

            // Double型で登録できないこと
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, -1.791e308d);
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

        } finally {
            deleteEntities();
        }
    }

    /**
     * Double型のデフォルト値がデータ登録時に反映されてデータが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型のデフォルト値がデータ登録時に反映されてデータが登録できること() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), "12345.123456789");
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "dummy", COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, null);

            // Double型で登録・取得可能なこと
            JSONObject complexBody = new JSONObject();
            complexBody.put("dummy", "test");
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            TResponse response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, "12345.123456789");

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, "12345.123456789");

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId1, listResponse, "12345.123456789");
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);

            String ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "dummy",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            deleteEntities();
        }
    }

    /**
     * Double型のデフォルト値が最大値の場合にデフォルト値を反映してデータが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型のデフォルト値が最大値の場合にデフォルト値を反映してデータが登録できること() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), "1.79E308");
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "dummy", COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, null);

            // Double型で登録・取得可能なこと
            JSONObject complexBody = new JSONObject();
            complexBody.put("dummy", "test");
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            TResponse response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            validatePostResnse(response, "1.79E308");

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateGetResponse(getResponse, "1.79E308");

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            validateListResponse(userdataId1, listResponse, "1.79E308");
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);

            String ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "dummy",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            deleteEntities();
        }
    }

    /**
     * Double型配列のデータが登録できること.
     * @throws ParseException パース例外.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型配列のデータが登録できること() throws ParseException {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), "1.79E308");
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "listDoubleProperty",
                    COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "List");

            // Double型で登録・取得可能なこと
            JSONArray reqBody = (JSONArray) new JSONParser().parse("[123.456,-567.89,1,\"9.123456789\",1.79E308]");
            JSONObject complexBody = new JSONObject();
            complexBody.put("listDoubleProperty", reqBody);
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            TResponse response = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);
            JSONObject json = response.bodyAsJson();
            JSONObject results = getResult(json);
            String value = ((JSONObject) results.get(PROP_NAME)).get("listDoubleProperty").toString();
            assertEquals("[123.456,-567.89,1,9.123456789,1.79E308]", value);

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = getResult(getJson);
            String getValue = ((JSONObject) getResults.get(PROP_NAME)).get("listDoubleProperty").toString();
            assertEquals("[123.456,-567.89,1,9.123456789,1.79E308]", getValue);

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject listJson = listResponse.bodyAsJson();
            getResults = getResultsFromId(userdataId1, listJson);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("listDoubleProperty").toString();
            assertEquals("[123.456,-567.89,1,9.123456789,1.79E308]", getValue);
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);

            String ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "listDoubleProperty",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            deleteEntities();
        }
    }

    /**
     * Double型配列のデータに最大制限値より大きい値が含まれている場合に登録できないこと.
     * @throws ParseException パース例外.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型配列のデータに最大制限値より大きい値が含まれている場合に登録できないこと() throws ParseException {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), "1.79E308");
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "listDoubleProperty",
                    COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "List");

            // Double型で登録・取得可能なこと
            JSONArray reqBody = (JSONArray) new JSONParser().parse("[123.456,-567.89,1,\"9.123456789\",1.791E308]");
            JSONObject complexBody = new JSONObject();
            complexBody.put("listDoubleProperty", reqBody);
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);

            // ユーザデータ作成
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            // ユーザデータ一件取得
            getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);

            String ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "listDoubleProperty",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            deleteEntities();
        }
    }

    /**
     * ComplexPropertyがEdmDoubleで数値形式のデータを更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexPropertyがEdmDoubleで数値形式のデータを更新できること() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "DoubleUpdate", COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "SingleUpdate", COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "LongUpdate", COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, null);
            double expected = Double.parseDouble(String.valueOf(Long.MAX_VALUE));
            DecimalFormat format = new DecimalFormat("#.#");
            format.setMaximumIntegerDigits(15);
            format.setMaximumFractionDigits(14);
            String fomattedValue = format.format(expected);
            String expectedString;
            if (expected != Double.parseDouble(fomattedValue)) {
                expectedString = Double.toString(expected);
            } else {
                expectedString = fomattedValue;
            }

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            // ユーザデータ更新(Int32, Double, Single, Long)
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, Integer.MAX_VALUE);
            complexBody.put("DoubleUpdate", 1.79E308d);
            complexBody.put("SingleUpdate", 12345.12345);
            complexBody.put("LongUpdate", Long.MAX_VALUE);
            body = new JSONObject();
            body.put(PROP_NAME, complexBody);

            UserDataUtils.update(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, body, cellName, boxName,
                    COL_NAME, ENTITY_TYPE_NAME, userdataId1, "*");

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = getResult(getJson);
            String getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME).toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(String.valueOf(Integer.MAX_VALUE), getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("DoubleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("1.79E308", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("SingleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("12345.12345", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("LongUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(expectedString, getValue);

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject listJson = listResponse.bodyAsJson();
            getResults = getResultsFromId(userdataId1, listJson);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME).toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(String.valueOf(Integer.MAX_VALUE), getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("DoubleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("1.79E308", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("SingleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("12345.12345", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("LongUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(expectedString, getValue);
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);
            // 作成したComplexTypePropertyを削除
            String ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "DoubleUpdate",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            // 作成したComplexTypePropertyを削除
            ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "SingleUpdate",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            // 作成したComplexTypePropertyを削除
            ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "LongUpdate", COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);

            deleteEntities();
        }
    }

    /**
     * ComplexPropertyがEdmDoubleで数値形式のデータを部分更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexPropertyがEdmDoubleで数値形式のデータを部分更新できること() {
        String userdataId1 = "doubleTest1";

        try {
            createEntities(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "IntUpdate", COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "DoubleUpdate", COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "SingleUpdate", COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, "LongUpdate", COMPLEX_TYPE_NAME,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, null);
            double expected = Double.parseDouble(String.valueOf(Long.MAX_VALUE));
            DecimalFormat format = new DecimalFormat("#.#");
            format.setMaximumIntegerDigits(15);
            format.setMaximumFractionDigits(14);
            String fomattedValue = format.format(expected);
            String expectedString;
            if (expected != Double.parseDouble(fomattedValue)) {
                expectedString = Double.toString(expected);
            } else {
                expectedString = fomattedValue;
            }

            // ユーザデータ作成
            JSONObject complexBody = new JSONObject();
            complexBody.put(PROP_NAME, "1");
            JSONObject body = new JSONObject();
            body.put("__id", userdataId1);
            body.put(PROP_NAME, complexBody);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, COL_NAME, ENTITY_TYPE_NAME);

            // ユーザデータ部分更新(Int32, Double, Single, Long)
            complexBody = new JSONObject();
            complexBody.put(PROP_NAME, Integer.MAX_VALUE);
            complexBody.put("DoubleUpdate", 1.79E308d);
            complexBody.put("SingleUpdate", 12345.12345);
            complexBody.put("LongUpdate", Long.MAX_VALUE);
            body = new JSONObject();
            body.put(PROP_NAME, complexBody);

            UserDataUtils.merge(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, body, cellName, boxName,
                    COL_NAME, ENTITY_TYPE_NAME, userdataId1, "*");

            // ユーザデータ一件取得
            TResponse getResponse = getUserData(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, userdataId1,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject getJson = getResponse.bodyAsJson();
            JSONObject getResults = getResult(getJson);
            String getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME).toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(String.valueOf(Integer.MAX_VALUE), getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("DoubleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("1.79E308", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("SingleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("12345.12345", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("LongUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(expectedString, getValue);

            // ユーザデータ一覧取得
            TResponse listResponse = UserDataUtils.list(cellName, boxName, COL_NAME, ENTITY_TYPE_NAME, "",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            JSONObject listJson = listResponse.bodyAsJson();
            getResults = getResultsFromId(userdataId1, listJson);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME).toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(String.valueOf(Integer.MAX_VALUE), getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("DoubleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("1.79E308", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("SingleUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals("12345.12345", getValue);
            getValue = ((JSONObject) getResults.get(PROP_NAME)).get("LongUpdate").toString();
            assertTrue(ODataUtils.validateDouble(getValue));
            assertEquals(expectedString, getValue);
        } finally {
            // ユーザデータ削除
            deleteUserData(userdataId1);
            // 作成したComplexTypePropertyを削除
            String ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "DoubleUpdate",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "SingleUpdate",
                    COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "LongUpdate", COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);
            ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, "IntUpdate", COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);

            deleteEntities();
        }
    }

    private void validatePostResnse(TResponse response, String expected) {
        JSONObject json = response.bodyAsJson();
        JSONObject results = getResult(json);
        String value = ((JSONObject) results.get(PROP_NAME)).get(PROP_NAME).toString();
        assertEquals(expected, value);
    }

    private void validateListResponse(String userdataId, TResponse listResponse, String expected) {
        JSONObject listJson = listResponse.bodyAsJson();
        JSONObject getResults = getResultsFromId(userdataId, listJson);
        String getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME).toString();
        assertTrue(ODataUtils.validateDouble(getValue));
        assertEquals(expected, getValue);
    }

    private void validateGetResponse(TResponse getResponse, String expected) {
        JSONObject getJson = getResponse.bodyAsJson();
        JSONObject getResults = getResult(getJson);
        String getValue = ((JSONObject) getResults.get(PROP_NAME)).get(PROP_NAME).toString();
        assertTrue(ODataUtils.validateDouble(getValue));
        assertEquals(expected, getValue);
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
     * プロパティを作成する.
     * @param type データ型名.
     * @param defValue デフォルト値.
     */
    protected void createProperty(String type, String defValue) {
        // リクエストパラメータ設定
        String locationUrl = UrlUtils.property(cellName, boxName, COL_NAME, null, null);
        DcRequest req = DcRequest.post(locationUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, PROP_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, ENTITY_TYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, type);
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defValue);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
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
     * Propertyを削除する.
     */
    protected void deleteProperty() {
        String locationUrl = UrlUtils.property(cellName, boxName, COL_NAME, PROP_NAME,
                ENTITY_TYPE_NAME);
        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(locationUrl);
    }

    /**
     * テストに必要なEntityを作成する.
     * @param type type.
     */
    private void createEntities(String type, String defaultcomplexTypeProperty) {
        // Collection作成
        DavResourceUtils.createODataCollection(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, cellName, boxName,
                COL_NAME);

        // EntityType作成
        createEntityType(ENTITY_TYPE_NAME, COL_NAME);

        // ComplexType作成
        UserDataUtils.createComplexType(cellName, boxName, COL_NAME, COMPLEX_TYPE_NAME);

        // Property作成
        createProperty(COMPLEX_TYPE_NAME, null);

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(cellName, boxName, COL_NAME, PROP_NAME, COMPLEX_TYPE_NAME,
                type, true, defaultcomplexTypeProperty, null);
    }

    /**
     * テストに必要なEntityを削除する.
     */
    protected void deleteEntities() {
        // 作成したComplexTypePropertyを削除
        String ctplocationUrl = UrlUtils.complexTypeProperty(cellName, boxName, COL_NAME, PROP_NAME, COMPLEX_TYPE_NAME);
        ODataCommon.deleteOdataResource(ctplocationUrl);

        // 作成したPropertyを削除
        deleteProperty();

        // 作成したComplexTypeを削除
        String ctlocationUrl = UrlUtils.complexType(cellName, boxName, COL_NAME, COMPLEX_TYPE_NAME);
        ODataCommon.deleteOdataResource(ctlocationUrl);

        // エンティティタイプを削除
        EntityTypeUtils.delete(COL_NAME, MASTER_TOKEN_NAME,
                "application/json", ENTITY_TYPE_NAME, cellName, -1);

        // コレクションを削除
        DavResourceUtils.deleteCollection(cellName, boxName, COL_NAME,
                DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
    }
}
