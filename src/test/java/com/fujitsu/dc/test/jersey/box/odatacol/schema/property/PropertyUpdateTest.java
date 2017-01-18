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
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * Property更新のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyUpdateTest extends ODataCommon {

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
    public PropertyUpdateTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * PropertyのTypeをInt32からDoubleに変更できること.
     */
    @Test
    public final void PropertyのTypeをInt32からDoubleに変更できること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";
        String userDataId = "001";
        String userDataId2 = "002";
        long int32Max = 2147483647L;
        double doubleMax = -1.79E308;

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            String body = String.format("{\"__id\":\"%s\",\"%s\":%d}", userDataId, propertyName, int32Max);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // Propertyが更新されていることの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

            // Property更新前に登録していたUserODataが取得できること
            TResponse userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId, HttpStatus.SC_OK);
            JSONObject results = (JSONObject) ((JSONObject) userData.bodyAsJson().get("d")).get("results");
            assertEquals(int32Max, results.get(propertyName));

            // Double型のデータが登録できること
            body = String.format("{\"__id\":\"%s\",\"%s\":%f}", userDataId2, propertyName, doubleMax);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);
            userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId2, HttpStatus.SC_OK);
            results = (JSONObject) ((JSONObject) userData.bodyAsJson().get("d")).get("results");
            assertEquals(doubleMax, results.get(propertyName));

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * PropertyのTypeをInt32からDoubleに変更できること_Propertyが複数ある内の2番目.
     */
    @Test
    public final void PropertyのTypeをInt32からDoubleに変更できること_Propertyが複数ある内の2番目() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName1 = "property1";
        String propertyName2 = "property2";
        String propertyNameToBeUpdated = "propertyToBeUpdated";
        String entityTypeName = "entity";
        String userDataId = "001";
        String userDataId2 = "002";
        long int32Max = 2147483647L;
        double doubleMax = -1.79E308;
        String stringValue = "test";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName1,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName,
                    entityTypeName, propertyNameToBeUpdated,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName2,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            String body = String.format("{\"__id\":\"%s\",\"%s\":\"%s\",\"%s\":%d,\"%s\":\"%s\"}",
                    userDataId, propertyName1, stringValue,
                    propertyNameToBeUpdated, int32Max, propertyName2, stringValue);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyNameToBeUpdated,
                    entityTypeName, propertyNameToBeUpdated,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind, isKey, uniqueKey);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // Propertyが更新されていることの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    propertyNameToBeUpdated, entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyNameToBeUpdated);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyNameToBeUpdated,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

            // Property更新前に登録していたUserODataが取得できること
            TResponse userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId, HttpStatus.SC_OK);
            JSONObject results = (JSONObject) ((JSONObject) userData.bodyAsJson().get("d")).get("results");
            assertEquals(int32Max, results.get(propertyNameToBeUpdated));
            assertEquals(stringValue, results.get(propertyName1));
            assertEquals(stringValue, results.get(propertyName2));

            // Double型のデータが登録できること
            body = String.format("{\"__id\":\"%s\",\"%s\":\"%s\",\"%s\":%f,\"%s\":\"%s\"}",
                    userDataId2, propertyName1, stringValue,
                    propertyNameToBeUpdated, doubleMax, propertyName2, stringValue);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);
            userData = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName,
                    userDataId2, HttpStatus.SC_OK);
            results = (JSONObject) ((JSONObject) userData.bodyAsJson().get("d")).get("results");
            assertEquals(doubleMax, results.get(propertyNameToBeUpdated));
            assertEquals(stringValue, results.get(propertyName1));
            assertEquals(stringValue, results.get(propertyName2));
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のTypeがInt32以外の場合400エラーとなること.
     */
    @Test
    public final void Propertyの更新前のTypeがInt32以外の場合400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'Type' change from [Edm.String] to [Edm.Double]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新後のTypeがDouble以外の場合400エラーとなること.
     */
    @Test
    public final void Propertyの更新後のTypeがDouble以外の場合400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'Type' change from [Edm.Int32] to [Edm.Single]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新でTypeプロパティを省略した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新でTypeプロパティを省略した場合400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Type").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新でNameプロパティを省略した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新でNameプロパティを省略した場合400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Name").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新でNameプロパティを変更できること.
     * @throws ParseException JSON文字列のパースエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新でNameプロパティを変更できること() throws ParseException {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String propertyReName = "changedPropertyName";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            String body = "{\"__id\":\"id1\",\"propertyToBeUpdated\":123}";
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyReName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // 変名後のPropretyを取得できること
            response = PropertyUtils.get(MASTER_TOKEN_NAME, cellName, boxName, colName, propertyReName, entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", propertyReName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, PropertyUtils.NAMESPACE, additional);
            // 変名前の名前でPropretyを取得できないこと
            response = PropertyUtils.get(MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName, entityTypeName);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
            // Propretyの一覧取得で変名後のPropretyが取得できること
            response = PropertyUtils.list(MASTER_TOKEN_NAME, cellName, boxName, colName, "?$filter=Name+eq+'"
                    + propertyReName + "'");
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", propertyReName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, PropertyUtils.NAMESPACE,
                    additional);

            // 最初の名前でPropretyを作成できること
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            // Propretyを取得できること
            response = PropertyUtils.get(MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName, entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", propertyName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, PropertyUtils.NAMESPACE, additional);

            // UserODataを更新できること
            body = "{\"__id\":\"id1\",\"changedPropertyName\":123.321,\"propertyToBeUpdated\":\"test\"}";
            JSONObject bodyJson = (JSONObject) new JSONParser().parse(body);
            UserDataUtils.update(MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, bodyJson, cellName, boxName, colName,
                    entityTypeName, "id1", "*");
            TResponse res = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, "id1",
                    HttpStatus.SC_OK);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, "UserData." + entityTypeName, bodyJson);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新でNameプロパティを複数回変更できること.
     * @throws ParseException JSON文字列のパースエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新でNameプロパティを複数回変更できること() throws ParseException {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String propertyReName = "changedPropertyName";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            String body = "{\"__id\":\"id1\",\"propertyToBeUpdated\":123}";
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyReName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // 変名後のPropretyを取得できること
            response = PropertyUtils.get(MASTER_TOKEN_NAME, cellName, boxName, colName, propertyReName, entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", propertyReName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, PropertyUtils.NAMESPACE, additional);
            // 変名前の名前でPropretyを取得できないこと
            response = PropertyUtils.get(MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName, entityTypeName);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
            // Propretyの一覧取得で変名後のPropretyが取得できること
            response = PropertyUtils.list(MASTER_TOKEN_NAME, cellName, boxName, colName, "?$filter=Name+eq+'"
                    + propertyReName + "'");
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", propertyReName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, PropertyUtils.NAMESPACE,
                    additional);

            // 最初の名前に変名できること
            // Property更新
            response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyReName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
            // Propretyを取得できること
            response = PropertyUtils.get(MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName, entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", propertyName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, PropertyUtils.NAMESPACE, additional);

            // UserODataを更新できること
            body = "{\"__id\":\"id1\",\"propertyToBeUpdated\":123.321}";
            JSONObject bodyJson = (JSONObject) new JSONParser().parse(body);
            UserDataUtils.update(MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, bodyJson, cellName, boxName, colName,
                    entityTypeName, "id1", "*");
            TResponse res = UserDataUtils.get(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, "id1",
                    HttpStatus.SC_OK);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, "UserData." + entityTypeName, bodyJson);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * 存在しないPropretyが変更できないこと.
     */
    @Test
    public final void 存在しないPropretyが変更できないこと() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String propertyReName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyReName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新でNameプロパティを同名に変更できること.
     */
    @Test
    public final void Propertyの更新でNameプロパティを同名に変更できること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String propertyReName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyReName,
                    entityTypeName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // 変名後のPropretyを取得できること
            response = PropertyUtils.get(MASTER_TOKEN_NAME, cellName, boxName, colName, propertyReName, entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", propertyReName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, PropertyUtils.NAMESPACE, additional);
            // Propretyの一覧取得で変名後のPropretyが取得できること
            response = PropertyUtils.list(MASTER_TOKEN_NAME, cellName, boxName, colName, "?$filter=Name+eq+'"
                    + propertyReName + "'");
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            additional = new HashMap<String, Object>();
            additional.put("Name", propertyReName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, PropertyUtils.NAMESPACE,
                    additional);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }


    /**
     * Propertyの更新で_EntityType.Nameプロパティを省略した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新で_EntityType_Nameプロパティを省略した場合400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("_EntityType.Name").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新で_EntityType_Nameプロパティを変更しようとした場合400エラーとなること.
     */
    @Test
    public final void Propertyの更新で_EntityType_Nameプロパティを変更しようとした場合400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";
        String invalidEntityTypeName = "InvalidEntityType";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, MASTER_TOKEN_NAME, boxName, colName, invalidEntityTypeName,
                    HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    invalidEntityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property '_EntityType.Name' change")
                            .getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);

        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * DynamicPropertyの更新でNameプロパティを変更できないこと.
     * @throws ParseException JSON文字列のパースエラー
     */
    @Test
    public final void DynamicPropertyの更新でNameプロパティを変更できないこと() throws ParseException {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String propertyReName = "changedPropertyName";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            String body = "{\"__id\":\"id1\",\"propertyToBeUpdated\":123}";
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                    entityTypeName);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyReName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'IsDeclared' change from [false] to [true]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * PropertyのNullableを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void PropertyのNullableを変更しようとした場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), false, defaultValue,
                    collectionKind,
                    isKey, uniqueKey);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'Nullable' change from [true] to [false]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のNullableがtrueでNullableを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のNullableがtrueでNullableを省略した場合にTypeのみ更新されること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            body.put("IsKey", isKey);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // PropertyがTypeのみ更新されてることの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のNullableがfalseでNullableを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のNullableがfalseでNullableを省略した場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = false;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            body.put("IsKey", isKey);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'Nullable' change from [false] to [true]").getMessage());

            // Property更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * PropertyのDefaultValueを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void PropertyのDefaultValueを変更しようとした場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, 12345, collectionKind,
                    isKey, uniqueKey);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'DefaultValue' change from [null] to [12345]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のDefaultValueがnullでDefaultValueを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のDefaultValueがnullでDefaultValueを省略した場合にTypeのみ更新されること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("Nullable", nullable);
            body.put("CollectionKind", collectionKind);
            body.put("IsKey", isKey);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // PropertyがTypeのみ更新されてることの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のDefaultValueがnull以外でDefaultValueを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のDefaultValueがnull以外でDefaultValueを省略した場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        int defaultValue = 12345;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("Nullable", nullable);
            body.put("CollectionKind", collectionKind);
            body.put("IsKey", isKey);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'DefaultValue' change from [12345] to [null]").getMessage());

            // Property更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, String.valueOf(defaultValue));
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * PropertyのCollectionKindを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void PropertyのCollectionKindを変更しようとした場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue, "List",
                    isKey, uniqueKey);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'CollectionKind' change from [None] to [List]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のCollectionKindがNoneでCollectionKindを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のCollectionKindがNoneでCollectionKindを省略した場合にTypeのみ更新されること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("Nullable", nullable);
            body.put("IsKey", isKey);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // PropertyがTypeのみ更新されてることの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のCollectionKindがListでCollectionKindを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のCollectionKindがListでCollectionKindを省略した場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = "List";
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("Nullable", nullable);
            body.put("IsKey", isKey);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'CollectionKind' change from [List] to [None]").getMessage());

            // Property更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * PropertyのIsKeyを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void PropertyのIsKeyを変更しようとした場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    true, uniqueKey);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'IsKey' change from [false] to [true]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のIsKeyがfalseでIsKeyを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のIsKeyがfalseでIsKeyを省略した場合にTypeのみ更新されること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            body.put("Nullable", nullable);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // PropertyがTypeのみ更新されてることの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のIsKeyがtrueでIsKeyを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のIsKeyがtrueでIsKeyを省略した場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = false;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = true;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            body.put("Nullable", nullable);
            body.put("UniqueKey", uniqueKey);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'IsKey' change from [true] to [false]").getMessage());

            // Property更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * PropertyのUniqueKeyを変更しようとした場合に400エラーとなること.
     */
    @Test
    public final void PropertyのUniqueKeyを変更しようとした場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), nullable, defaultValue,
                    collectionKind,
                    isKey, "PropertyTest");

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'UniqueKey' change from [null] to [PropertyTest]").getMessage());

            // Propertyが更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のUniqueKeyがnullでUniqueKeyを省略した場合にTypeのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のUniqueKeyがnullでUniqueKeyを省略した場合にTypeのみ更新されること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = true;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = null;

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            body.put("IsKey", isKey);
            body.put("Nullable", nullable);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

            // PropertyがTypeのみ更新されてることの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

    /**
     * Propertyの更新前のUniqueKeyがnull以外でUniqueKeyを省略した場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Propertyの更新前のUniqueKeyがnull以外でUniqueKeyを省略した場合に400エラーとなること() {
        String cellName = "propertyUpdateTestCell";
        String boxName = "box";
        String colName = "collection";
        String propertyName = "propertyToBeUpdated";
        String entityTypeName = "entity";

        // Propertyの内容
        boolean nullable = false;
        String defaultValue = null;
        String collectionKind = Property.COLLECTION_KIND_NONE;
        boolean isKey = false;
        String uniqueKey = "PropertyTest";

        try {
            // 事前準備
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils
                    .create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            PropertyUtils.create(BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName, propertyName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), nullable, defaultValue, collectionKind, isKey,
                    uniqueKey, HttpStatus.SC_CREATED);

            // Property更新
            JSONObject body = new JSONObject();
            body.put("Name", propertyName);
            body.put("_EntityType.Name", entityTypeName);
            body.put("Type", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            body.put("DefaultValue", defaultValue);
            body.put("CollectionKind", collectionKind);
            body.put("IsKey", isKey);
            body.put("Nullable", nullable);

            DcResponse response = PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName, entityTypeName, body);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED
                            .params("Property 'UniqueKey' change from [PropertyTest] to [null]").getMessage());

            // Property更新されていないことの確認
            response = PropertyUtils.get(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propertyName,
                    entityTypeName);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            String locationUrl = PropertyUtils.composeLocationUrl(cellName, boxName, colName, propertyName,
                    entityTypeName);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, cellName);
        }
    }

}
