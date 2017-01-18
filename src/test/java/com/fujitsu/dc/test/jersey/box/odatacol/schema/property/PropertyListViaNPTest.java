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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * PropertyNP経由一覧取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyListViaNPTest extends ODataCommon {

    /** Property名. */
    private static String propName = null;

    /** Property名. */
    private static final String PROPERTY_ENTITYTYPE_NAME = "Price";

    /**
     * コンストラクタ.
     */
    public PropertyListViaNPTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        propName = "p_name_" + String.valueOf(System.currentTimeMillis());
    }

    /**
     * DefaultValueに制御コードを含むPropertyをNP経由で一覧取得した場合_レスポンスボディがエスケープされて返却されること.
     */
    @Test
    public final void DefaultValueに制御コードを含むPropertyをNP経由で一覧取得した場合_レスポンスボディがエスケープされて返却されること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            PropertyUtils.createViaPropertyNP(BEARER_MASTER_TOKEN, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_ENTITYTYPE_NAME, propName,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, "\\u0000", Property.COLLECTION_KIND_NONE,
                    false, null, 201);

            TResponse response = PropertyUtils.listViaPropertyNP(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_ENTITYTYPE_NAME, 200);

            // レスポンスチェック
            String resBody = response.getBody();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyをEntityTypeからのNP経由で一覧取得してターゲット側のPropertyのみ取得できること.
     */
    @Test
    public final void PropertyをEntityTypeからのNP経由で一覧取得してターゲット側のPropertyのみ取得できること() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String entityName = "propNpfromEntity";
        final String prop1Name = "propNpProperty1";
        final String prop2Name = "propNpProperty2";
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, token, Setup.TEST_ODATA, entityName, HttpStatus.SC_CREATED);
            // Property作成（EntityTypeのNP経由）
            PropertyUtils.createViaPropertyNP(BEARER_MASTER_TOKEN, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityName, prop1Name,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, Property.COLLECTION_KIND_NONE,
                    false, null, 201);
            PropertyUtils.createViaPropertyNP(BEARER_MASTER_TOKEN, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityName, prop2Name,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, Property.COLLECTION_KIND_NONE,
                    false, null, 201);

            // Check1: EntityTypeからPropertyへの$link一覧取得
            TResponse response = PropertyUtils.listLinks(token, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityName, HttpStatus.SC_OK);
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            ArrayList<String> expects = new ArrayList<String>();
            expects.add(UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, prop1Name, entityName));
            expects.add(UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, prop2Name, entityName));
            ODataCommon.checkLinResponseBody(response.bodyAsJson(), expects);

            // Check2: EntityTypeのNP経由での一覧取得
            response = PropertyUtils.listViaPropertyNP(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityName, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(2, results.size());
            expects = new ArrayList<String>();
            expects.add("propNpProperty1");
            expects.add("propNpProperty2");
            assertEquals(2, results.size());
            for (Object item : results) {
                JSONObject body = (JSONObject) item;
                assertTrue(expects.contains(body.get("Name")));
            }
        } finally {
            // Propertyの削除
            String url = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, prop1Name, entityName);
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(url).getStatusCode());
            url = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, prop2Name, entityName);
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(url).getStatusCode());
            url = UrlUtils.entityType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityName);
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(url).getStatusCode());
        }
    }
}
