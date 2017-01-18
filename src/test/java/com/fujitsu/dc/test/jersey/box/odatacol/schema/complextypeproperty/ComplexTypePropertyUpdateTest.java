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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.core.model.impl.es.odata.UserDataODataProducer;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.complextype.ComplexTypeUtils;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * ComplexTypeProperty更新のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypePropertyUpdateTest extends ODataCommon {

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public ComplexTypePropertyUpdateTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * ComplexTypePropertyのTypeをInt32からDoubleに変更できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeをInt32からDoubleに変更できること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";
        String userDataId = "001";
        String userDataId2 = "002";
        long int32Max = 2147483647L;
        double doubleMax = -1.79E308;

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            String body = String.format("{\"__id\":\"%s\",\"%s\":{\"%s\":%d}}",
                    userDataId, propertyName, complexTypePropertyName, int32Max);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // ComplexTypePropertyが更新されていることの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

            // ComplexTypeProperty更新前に登録していたUserODataが取得できること
            TResponse userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId, HttpStatus.SC_OK);
            JSONObject results = (JSONObject) ((JSONObject) userData.bodyAsJson().get("d")).get("results");
            JSONObject complexResults = (JSONObject) results.get(propertyName);
            assertEquals(int32Max, complexResults.get(complexTypePropertyName));

            // Double型のデータが登録できること
            body = String.format("{\"__id\":\"%s\",\"%s\":{\"%s\":%f}}",
                    userDataId2, propertyName, complexTypePropertyName, doubleMax);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);
            userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId2, HttpStatus.SC_OK);
            results = (JSONObject) ((JSONObject) userData.bodyAsJson().get("d")).get("results");
            complexResults = (JSONObject) results.get(propertyName);
            assertEquals(doubleMax, complexResults.get(complexTypePropertyName));

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyのTypeをInt32からDoubleに変更できること_Propertyが複数ある場合.
     * @throws ParseException パースエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyのTypeをInt32からDoubleに変更できること_Propertyが複数ある場合() throws ParseException {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName1 = "property1";
        String propertyName2 = "property2";
        String propertyName3 = "property3";
        String entityTypeName = "entity";
        String complexTypeName1 = "complex1";
        String complexTypeName2 = "complex2";
        String prop1ComplexTypeProperty1 = "complexTypeProperty1";
        String prop1ComplexTypeProperty2 = "complexTypeProperty2";
        String prop3ComplexTypeProperty1 = "complexTypeProperty1";
        String prop3ComplexTypeProperty2 = "complexTypeProperty2";

        String userDataId = "001";
        String userDataId2 = "002";
        double doubleMax = -1.79E308;

        String nameSpace = UserDataODataProducer.USER_ODATA_NAMESPACE + "." + entityTypeName;

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);

            // Property1
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName1, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, prop1ComplexTypeProperty1,
                    complexTypeName1, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, prop1ComplexTypeProperty2,
                    complexTypeName1, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName1,
                    complexTypeName1, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // Property2
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName2,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), true, null, Property.COLLECTION_KIND_NONE,
                    false, null, HttpStatus.SC_CREATED);

            // Property3
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName2, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, prop3ComplexTypeProperty1,
                    complexTypeName2, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, prop3ComplexTypeProperty2,
                    complexTypeName2, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName3,
                    complexTypeName2, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            String body = String
                    .format("{\"__id\":\"%s\",\"%s\": {\"%s\": true,\"%s\": true},"
                            + "\"%s\": true,\"%s\": {\"%s\": true,\"%s\": 12345}}",
                            userDataId, propertyName1, prop1ComplexTypeProperty1, prop1ComplexTypeProperty2,
                            propertyName2, propertyName3, prop3ComplexTypeProperty1, prop3ComplexTypeProperty2);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    prop3ComplexTypeProperty2, complexTypeName2,
                    prop3ComplexTypeProperty2, complexTypeName2,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // ComplexTypePropertyが更新されていることの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, prop3ComplexTypeProperty2, complexTypeName2);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, prop3ComplexTypeProperty2);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName2);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    prop3ComplexTypeProperty2, complexTypeName2);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

            // ComplexTypeProperty更新前に登録していたUserODataが取得できること
            TResponse userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId, HttpStatus.SC_OK);
            JSONObject expectedJson = (JSONObject) new JSONParser().parse(body);
            ODataCommon.checkResponseBody(userData.bodyAsJson(), null, nameSpace, expectedJson);

            // Double型のデータが登録できること
            body = String
                    .format("{\"__id\":\"%s\",\"%s\": {\"%s\": true,\"%s\": true},"
                            + "\"%s\": true,\"%s\": {\"%s\": true,\"%s\": %f}}",
                            userDataId2, propertyName1, prop1ComplexTypeProperty1, prop1ComplexTypeProperty2,
                            propertyName2, propertyName3, prop3ComplexTypeProperty1, prop3ComplexTypeProperty2,
                            doubleMax);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);
            userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId2, HttpStatus.SC_OK);
            expectedJson = (JSONObject) new JSONParser().parse(body);
            ODataCommon.checkResponseBody(userData.bodyAsJson(), null, nameSpace, expectedJson);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyのTypeをInt32からDoubleに変更できること_4階層目.
     * @throws ParseException パースエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyのTypeをInt32からDoubleに変更できること_4階層目() throws ParseException {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName1 = "property1";
        String entityTypeName = "entity";
        String complexTypeName1 = "complex1";
        String complexTypeName2 = "complex2";
        String complexTypeName3 = "complex3";
        String complexTypeProperty1 = "complexTypeProperty1";
        String complexTypeProperty2 = "complexTypeProperty2";
        String complexTypeProperty31 = "complexTypeProperty31";
        String complexTypeProperty32 = "complexTypeProperty32";

        String userDataId = "001";
        String userDataId2 = "002";
        double doubleMax = -1.79E308;

        String nameSpace = UserDataODataProducer.USER_ODATA_NAMESPACE + "." + entityTypeName;

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);

            // 4階層目
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName3, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty31,
                    complexTypeName3, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty32,
                    complexTypeName3, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);

            // 3階層目
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName2, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty2,
                    complexTypeName2, complexTypeName3, HttpStatus.SC_CREATED);

            // 2階層目
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName1, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty1,
                    complexTypeName1, complexTypeName2, HttpStatus.SC_CREATED);

            // 1階層目
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName1,
                    complexTypeName1, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            String body = String
                    .format("{\"__id\":\"%s\",\"%s\": {\"%s\":{\"%s\":{\"%s\":true,\"%s\":12345}}}}",
                            userDataId, propertyName1, complexTypeProperty1, complexTypeProperty2,
                            complexTypeProperty31, complexTypeProperty32);

            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypeProperty32, complexTypeName3,
                    complexTypeProperty32, complexTypeName3,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // ComplexTypePropertyが更新されていることの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypeProperty32, complexTypeName3);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypeProperty32);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName3);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypeProperty32, complexTypeName3);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

            // ComplexTypeProperty更新前に登録していたUserODataが取得できること
            TResponse userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId, HttpStatus.SC_OK);
            JSONObject expectedJson = (JSONObject) new JSONParser().parse(body);
            ODataCommon.checkResponseBody(userData.bodyAsJson(), null, nameSpace, expectedJson);

            // Double型のデータが登録できること
            body = String
                    .format("{\"__id\":\"%s\",\"%s\": {\"%s\":{\"%s\":{\"%s\":true,\"%s\":%f}}}}",
                            userDataId2, propertyName1, complexTypeProperty1, complexTypeProperty2,
                            complexTypeProperty31, complexTypeProperty32, doubleMax);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);
            userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId2, HttpStatus.SC_OK);
            expectedJson = (JSONObject) new JSONParser().parse(body);
            ODataCommon.checkResponseBody(userData.bodyAsJson(), null, nameSpace, expectedJson);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新前のTypeがInt32以外の場合400エラーとなること.
     */
    @Test
    public final void ComplexTypePropertyの更新前のTypeがInt32以外の場合400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.STRING.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon
                    .checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                            DcCoreException.OData.OPERATION_NOT_SUPPORTED
                                    .params("ComplexTypeProperty 'Type' change from [Edm.String] to [Edm.Double]")
                                    .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新後のTypeがDouble以外の場合400エラーとなること.
     */
    @Test
    public final void ComplexTypePropertyの更新後のTypeがDouble以外の場合400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName,
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon
                    .checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                            DcCoreException.OData.OPERATION_NOT_SUPPORTED
                                    .params("ComplexTypeProperty 'Type' change from [Edm.Int32] to [Edm.Single]")
                                    .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新でTypeプロパティを省略した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新でTypeプロパティを省略した場合400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Nullable", nullable);
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Type").getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新でNameプロパティを省略した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新でNameプロパティを省略した場合400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Nullable", nullable);
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Name").getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新でNameプロパティを変更できること.
     */
    @Test
    public final void ComplexTypePropertyの更新でNameプロパティを変更できること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";
        String complexTypePropertyReName = "updated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse res = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyReName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // 変更後のComplexPropertyを取得できること
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyReName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
            // 変更前の名前でComlexTypePropertyを取得できないこと
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
            // ComplexTypeProrpetyの一覧取得で変名後のComplexTypePropertyが取得できること
            TResponse tres = ComplexTypePropertyUtils.list(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "?\\$filter=Name+eq+'" + complexTypePropertyReName + "'", HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBodyList(tres.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);

            // 最初の名前でComlexTypePropertyを作成できること
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            // ComlexTypePropertyを取得できること
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新でNameプロパティを変更できること_4階層目.
     * @throws ParseException レスポンスボディのパースに失敗
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新でNameプロパティを変更できること_4階層目() throws ParseException {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName1 = "property1";
        String entityTypeName = "entity";
        String complexTypeName1 = "complex1";
        String complexTypeName2 = "complex2";
        String complexTypeName3 = "complex3";
        String complexTypeProperty1 = "complexTypeProperty1";
        String complexTypeProperty2 = "complexTypeProperty2";
        String complexTypeProperty3 = "complexTypeProperty31";
        String complexTypePropertyReName = "complexTypePropertyReName";

        String userDataId = "001";
        String nameSpace = UserDataODataProducer.USER_ODATA_NAMESPACE + "." + entityTypeName;

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);

            // 4階層目
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName3, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty3,
                    complexTypeName3, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);

            // 3階層目
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName2, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty2,
                    complexTypeName2, complexTypeName3, HttpStatus.SC_CREATED);

            // 2階層目
            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName1, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty1,
                    complexTypeName1, complexTypeName2, HttpStatus.SC_CREATED);

            // 1階層目
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName1,
                    complexTypeName1, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            String body = String
                    .format("{\"__id\":\"%s\",\"%s\": {\"%s\":{\"%s\":{\"%s\":true}}}}",
                            userDataId, propertyName1, complexTypeProperty1, complexTypeProperty2,
                            complexTypeProperty3);

            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // ComplexTypeProperty更新
            DcResponse res = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypeProperty3, complexTypeName3,
                    complexTypePropertyReName, complexTypeName3,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // 変更後のComplexTypePropertyを取得できること
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyReName, complexTypeName3);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
            // 変更前の名前でComlexTypePropertyを取得できないこと
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypeProperty3, complexTypeName3);
            assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
            // ComplexTypePropertyの一覧取得で変名後のComplexTypePropertyが取得できること
            TResponse tres = ComplexTypePropertyUtils.list(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "?\\$filter=Name+eq+'" + complexTypePropertyReName + "'", HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBodyList(tres.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
            // UserDataのpropertyが変更されていること
            TResponse userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId, HttpStatus.SC_OK);
            String expectedBody = String
                    .format("{\"__id\":\"%s\",\"%s\": {\"%s\":{\"%s\":{\"%s\":true}}}}",
                            userDataId, propertyName1, complexTypeProperty1, complexTypeProperty2,
                            complexTypePropertyReName);
            JSONObject expectedJson = (JSONObject) new JSONParser().parse(expectedBody);
            ODataCommon.checkResponseBody(userData.bodyAsJson(), null, nameSpace, expectedJson);

            // 最初の名前でComlexTypePropertyを作成できること
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypeProperty3,
                    complexTypeName3, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            // ComlexTypePropertyを取得できること
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypeProperty3, complexTypeName3);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypeProperty3);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新でNameプロパティを複数回変更できること.
     */
    @Test
    public final void ComplexTypePropertyの更新でNameプロパティを複数回変更できること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";
        String complexTypePropertyReName = "updated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse res = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyReName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // 変更後のComplexPropertyを取得できること
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyReName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
            // 変更前の名前でComlexTypePropertyを取得できないこと
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
            // ComplexTypeProrpetyの一覧取得で変名後のComplexTypePropertyが取得できること
            TResponse tres = ComplexTypePropertyUtils.list(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "?\\$filter=Name+eq+'" + complexTypePropertyReName + "'", HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBodyList(tres.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);

            // 最初の名前に変名できること
            // ComplexTypeProperty更新
            res = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyReName, complexTypeName,
                    complexTypePropertyName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // ComlexTypePropertyを取得できること
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新でNameプロパティを同名に変更できること.
     */
    @Test
    public final void ComplexTypePropertyの更新でNameプロパティを同名に変更できること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";
        String complexTypePropertyReName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse res = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyReName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // 変更後のComplexPropertyを取得できること
            res = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyReName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
            // ComplexTypeProrpetyの一覧取得で変名後のComplexTypePropertyが取得できること
            TResponse tres = ComplexTypePropertyUtils.list(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "?\\$filter=Name+eq+'" + complexTypePropertyReName + "'", HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypePropertyReName);
            ODataCommon.checkResponseBodyList(tres.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, additional);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * 存在しないComplexTypePropertyを変更できないこと.
     */
    @Test
    public final void 存在しないComplexTypePropertyを変更できないこと() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";
        String complexTypePropertyReName = "updated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse res = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyReName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);
            assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新で_ComplexType_Nameプロパティを省略した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新で_ComplexType_Nameプロパティを省略した場合400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("Nullable", nullable);
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("_ComplexType.Name").getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新で_ComplexType_Nameプロパティを変更しようとした場合400エラーとなること.
     */
    @Test
    public final void ComplexTypePropertyの更新で_ComplexType_Nameプロパティを変更しようとした場合400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            ComplexTypeUtils.create(cellName, boxName, colName, "updated", HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, "updated",
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(
                    response,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("ComplexTypeProperty '_ComplexType.Name' change")
                            .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyのNullableを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void ComplexTypePropertyのNullableを変更しようとした場合に400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), false, defaultValue, collectionKind);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(
                    response,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("ComplexTypeProperty 'Nullable' change from [true] to [false]")
                            .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新前のNullableがtrueでNullableを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新前のNullableがtrueでNullableを省略した場合にTypeのみ更新されること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // ComplexTypePropertyが更新されていることの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新前のNullableがfalseでNullableを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新前のNullableがfalseでNullableを省略した場合に400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = false;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(),
                    nullable, defaultValue, collectionKind);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(
                    response,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("ComplexTypeProperty 'Nullable' change from [false] to [true]")
                            .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyのDefaultValueを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void ComplexTypePropertyのDefaultValueを変更しようとした場合に400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, 12345, collectionKind);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(
                    response,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("ComplexTypeProperty 'DefaultValue' change from [null] to [12345]")
                            .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新前のDefaultValueがnullでDefaultValueを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新前のDefaultValueがnullでDefaultValueを省略した場合にTypeのみ更新されること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("Nullable", nullable);
            body.put("CollectionKind", collectionKind);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // ComplexTypePropertyが更新されていることの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新前のDefaultValueがnull以外でDefaultValueを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新前のDefaultValueがnull以外でDefaultValueを省略した場合に400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        int defaultValue = 12345;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(),
                    nullable, defaultValue, collectionKind);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("Nullable", nullable);
            body.put("CollectionKind", collectionKind);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(
                    response,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("ComplexTypeProperty 'DefaultValue' change from [12345] to [null]")
                            .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, String.valueOf(defaultValue));
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyのCollectionKindを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void ComplexTypePropertyのCollectionKindを変更しようとした場合に400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, "List");

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(
                    response,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("ComplexTypeProperty 'CollectionKind' change from [None] to [List]")
                            .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }

    }

    /**
     * ComplexTypePropertyの更新前のCollectionKindがNoneでCollectionKindを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新前のCollectionKindがNoneでCollectionKindを省略した場合にTypeのみ更新されること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName,
                    complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("Nullable", nullable);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // ComplexTypePropertyが更新されていることの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * ComplexTypePropertyの更新前のCollectionKindがListでCollectionKindを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypePropertyの更新前のCollectionKindがListでCollectionKindを省略した場合に400エラーとなること() {
        String cellName = "complexTypePropertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "property";
        String entityTypeName = "entity";
        String complexTypeName = "complex";
        String complexTypePropertyName = "complexTypePropertyToBeUpdated";

        // ComplexTypePropertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = "List";

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);

            ComplexTypeUtils.create(cellName, boxName, colName, complexTypeName, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cellName, boxName, colName, complexTypePropertyName, complexTypeName,
                    complexTypePropertyName, complexTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(),
                    nullable, defaultValue, collectionKind);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);

            // ComplexTypeProperty更新
            JSONObject body = new JSONObject();
            body.put("Name", complexTypePropertyName);
            body.put("_ComplexType.Name", complexTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("Nullable", nullable);
            DcResponse response = ComplexTypePropertyUtils.update(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(
                    response,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("ComplexTypeProperty 'CollectionKind' change from [List] to [None]")
                            .getMessage());

            // ComplexTypePropertyが更新されていないことの確認
            response = ComplexTypePropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, complexTypePropertyName, complexTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            String locationUrl = ComplexTypePropertyUtils.composeLocationUrl(cellName, boxName, colName,
                    complexTypePropertyName, complexTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }
}
