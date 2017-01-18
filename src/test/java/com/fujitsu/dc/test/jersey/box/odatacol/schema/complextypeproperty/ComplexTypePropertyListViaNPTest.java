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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.complextypeproperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ComplexTypePropertyNP経由一覧取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypePropertyListViaNPTest extends ODataCommon {

    /** ComplexType NameKey名. */
    private static final String COMPLEX_TYPE_NAME_KEY = ComplexType.P_COMPLEXTYPE_NAME.getName().toString();

    /** ComplexTypeProperty名. */
    private static final String CT_PROPERTY_NAME = "ctp_name";

    /** ComplexType名. */
    private static final String COMPLEX_TYPE_NAME = "address";

    /** ComplexTypeリソースURL. */
    private static final String CT_LOCATION_URL =
            UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, COMPLEX_TYPE_NAME);

    /**
     * コンストラクタ.
     */
    public ComplexTypePropertyListViaNPTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        // ComplexType作成
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        // リクエスト実行
        request(req);
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
        // 作成したComplexTypeを削除
        assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(CT_LOCATION_URL).getStatusCode());
    }

    /**
     * DefaultValueに制御コードを含むComplexTypePropertyをNP経由で一覧取得した場合_レスポンスボディがエスケープされて返却されること.
     */
    @Test
    public final void DefaultValueに制御コードを含むComplexTypePropertyをNP経由で一覧取得した場合_レスポンスボディがエスケープされて返却されること() {
        String ctplocationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);

        try {
            ComplexTypePropertyUtils.createViaComplexTypePropertyNP(
                    BEARER_MASTER_TOKEN, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    COMPLEX_TYPE_NAME, CT_PROPERTY_NAME, EdmSimpleType.STRING.getFullyQualifiedTypeName(),
                    false, "\\u0000", Property.COLLECTION_KIND_NONE, 201);

            TResponse response = ComplexTypePropertyUtils.listViaComplexTypePropertyNP(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    COMPLEX_TYPE_NAME, 200);

            // レスポンスチェック
            String resBody = response.getBody();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
        }
    }
}
