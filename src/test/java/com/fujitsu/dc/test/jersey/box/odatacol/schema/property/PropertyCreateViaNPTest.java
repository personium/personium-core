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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * PropertyNP経由登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyCreateViaNPTest extends ODataCommon {

    /** Property名. */
    private static String propName = null;

    /** Property名. */
    private static final String PROPERTY_ENTITYTYPE_NAME = "Price";

    /**
     * コンストラクタ.
     */
    public PropertyCreateViaNPTest() {
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
     * EntityTypeからNP経由でPropertyを登録できること.
     */
    @Test
    public final void EntityTypeからNP経由でPropertyを登録できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            TResponse response = PropertyUtils.createViaPropertyNP(BEARER_MASTER_TOKEN, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_ENTITYTYPE_NAME, propName,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, Property.COLLECTION_KIND_NONE,
                    false, null, 201);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * DefaultValueに制御コードを含むPropertyをNP経由で作成した場合_レスポンスボディがエスケープされて返却されること.
     */
    @Test
    public final void DefaultValueに制御コードを含むPropertyをNP経由で作成した場合_レスポンスボディがエスケープされて返却されること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            TResponse response = PropertyUtils.createViaPropertyNP(BEARER_MASTER_TOKEN, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_ENTITYTYPE_NAME, propName,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, "\\u0000", Property.COLLECTION_KIND_NONE,
                    false, null, 201);

            // レスポンスチェック
            String resBody = response.getBody();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }
}
