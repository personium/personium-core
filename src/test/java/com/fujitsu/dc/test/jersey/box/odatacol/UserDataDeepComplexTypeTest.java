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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.CompareJSON;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * UserDataComplexType複数階層のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataDeepComplexTypeTest extends JerseyTest {

    private static final String COMPLEX_TYPE_NAME_1 = "complexType1";
    private static final String LIST_COMPLEX_TYPE_NAME_1 = "listComplexType1";
    private static final String COMPLEX_TYPE_NAME_2 = "complexType2";
    private static final String LIST_COMPLEX_TYPE_NAME_2 = "listComplexType2";
    private static final String COMPLEX_TYPE_NAME_3 = "complexType3";
    private static final String LIST_COMPLEX_TYPE_NAME_3 = "listComplexType3";

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
    public UserDataDeepComplexTypeTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * 複数階層あるデータに対して1階層目のデータをMERGEして_正常に更新できること.
     */
    @Test
    public final void 複数階層あるデータに対して1階層目のデータをMERGEして_正常に更新できること() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userDataDeepComplexTypeTestCell";
        String boxName = "box";
        String odataColName = "col";
        String entityTypeName = "entity";

        try {
            createODataCollection(token, cellName, boxName, odataColName);
            createSchema(token, cellName, boxName, odataColName, entityTypeName);

            // 1階層目の文字列Propertyを指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id001");
            String bodyString = "{"
                    + "  \"p1Property\": \"p1PropertyValueUpdated\""
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT,
                    parseStringToJSONObject(bodyString),
                    cellName, boxName, odataColName,
                    entityTypeName, "id001", "*");
            // 更新確認
            TResponse response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, "id001", HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(
                    getDefaultUserDataRequestBody("id001"),
                    response.getBody());
            assertNotNull(res);
            assertEquals(1, res.size());
            assertEquals("p1PropertyValueUpdated", res.getMismatchValue("p1Property"));

            // 1階層目の文字列Property配列を指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id002");
            String bodyStringArray = "{"
                    + "  \"p1ListProperty\": ["
                    + "    \"p1ListPropertyValueUpdated1\","
                    + "    \"p1ListPropertyValueUpdated2\""
                    + "  ]"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT, parseStringToJSONObject(bodyStringArray),
                    cellName, boxName, odataColName,
                    entityTypeName, "id002", "*");
            // 更新確認
            response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, "id002", HttpStatus.SC_OK);
            res = CompareJSON.compareJSON(
                    getDefaultUserDataRequestBody("id002"),
                    response.getBody());
            assertNotNull(res);
            assertEquals(1, res.size());
            JSONArray resArray = (JSONArray) res.getMismatchValue("p1ListProperty");
            assertEquals(2, resArray.size());
            assertTrue(resArray.contains("p1ListPropertyValueUpdated1"));
            assertTrue(resArray.contains("p1ListPropertyValueUpdated2"));

        } catch (ParseException e) {
            fail(e.getMessage());
        } finally {
            // Cell一括削除
            Setup.cellBulkDeletion(cellName);
        }

    }

    /**
     * 複数階層あるデータに対して2階層目のデータをMERGEして_正常に更新できること.
     */
    @Test
    public final void 複数階層あるデータに対して2階層目のデータをMERGEして_正常に更新できること() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userDataDeepComplexTypeTestCell";
        String boxName = "box";
        String odataColName = "col";
        String entityTypeName = "entity";

        try {
            createODataCollection(token, cellName, boxName, odataColName);
            createSchema(token, cellName, boxName, odataColName, entityTypeName);

            // 2階層目の文字列Propertyを指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id001");
            String bodyString = "{"
                    + "  \"p1ComplexProperty\": {"
                    + "    \"c1Property\": \"c1PropertyValueUpdated\""
                    + "  }"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT, parseStringToJSONObject(bodyString),
                    cellName, boxName, odataColName,
                    entityTypeName, "id001", "*");
            // 更新確認
            TResponse response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, "id001", HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(
                    getDefaultUserDataRequestBody("id001"),
                    response.getBody());
            assertNotNull(res);
            assertEquals(1, res.size());
            assertEquals("c1PropertyValueUpdated", res.getMismatchValue("p1ComplexProperty.c1Property"));

            // 2階層目の文字列Property配列を指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id002");
            String bodyStringArray = "{"
                    + "  \"p1ComplexProperty\": {"
                    + "    \"c1ListProperty\": ["
                    + "      \"c1ListPropertyValueUpdated1\","
                    + "      \"c1ListPropertyValueUpdated2\""
                    + "    ]"
                    + "  }"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT, parseStringToJSONObject(bodyStringArray),
                    cellName, boxName, odataColName,
                    entityTypeName, "id002", "*");
            // 更新確認
            response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, "id002", HttpStatus.SC_OK);
            res = CompareJSON.compareJSON(
                    getDefaultUserDataRequestBody("id002"),
                    response.getBody());
            assertNotNull(res);
            assertEquals(1, res.size());
            JSONArray resArray = (JSONArray) res.getMismatchValue("p1ComplexProperty.c1ListProperty");
            assertEquals(2, resArray.size());
            assertTrue(resArray.contains("c1ListPropertyValueUpdated1"));
            assertTrue(resArray.contains("c1ListPropertyValueUpdated2"));

            // 配列型の2階層目の文字列Propertyを指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id003");
            String bodyArrayComplexString = "{"
                    + "  \"p1ComplexListProperty\": ["
                    + "    {"
                    + "      \"lc1Property\": \"lc1PropertyValueUpdated\""
                    + "   }"
                    + "  ]"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT,
                    parseStringToJSONObject(bodyArrayComplexString),
                    cellName, boxName, odataColName,
                    entityTypeName, "id003", "*");
            // ComplexType型のListのMERGEは未サポートのため、MERGE後のチェックは省略

            // 配列型の2階層目の文字列Propertyを指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id004");
            String bodyArrayComplexArrayString = "{"
                    + "  \"p1ComplexListProperty\": ["
                    + "    {"
                    + "      \"lc1ListProperty\": ["
                    + "        \"lc1ListPropertyValueUpdated1\","
                    + "        \"lc1ListPropertyValueUpdated2\""
                    + "      ]"
                    + "    }"
                    + "  ]"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT,
                    parseStringToJSONObject(bodyArrayComplexArrayString),
                    cellName, boxName, odataColName,
                    entityTypeName, "id004", "*");
            // ComplexType型のListのMERGEは未サポートのため、MERGE後のチェックは省略

        } catch (ParseException e) {
            fail(e.getMessage());
        } finally {
            // Cell一括削除
            Setup.cellBulkDeletion(cellName);
        }

    }

    /**
     * 複数階層あるデータに対して4階層目のデータをMERGEして_正常に更新できること.
     */
    @Test
    public final void 複数階層あるデータに対して4階層目のデータをMERGEして_正常に更新できること() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userDataDeepComplexTypeTestCell";
        String boxName = "box";
        String odataColName = "col";
        String entityTypeName = "entity";

        try {
            createODataCollection(token, cellName, boxName, odataColName);
            createSchema(token, cellName, boxName, odataColName, entityTypeName);

            // 4階層目の文字列Propertyを指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id001");
            String bodyString = "{"
                    + "  \"p1ComplexProperty\": {"
                    + "    \"c1ComplexProperty\": {"
                    + "      \"c2ComplexProperty\": {"
                    + "        \"c3Property\": \"c3PropertyValueUpdated\""
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT, parseStringToJSONObject(bodyString),
                    cellName, boxName, odataColName,
                    entityTypeName, "id001", "*");
            // 更新確認
            TResponse response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, "id001", HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(
                    getDefaultUserDataRequestBody("id001"),
                    response.getBody());
            assertNotNull(res);
            assertEquals(1, res.size());
            assertEquals("c3PropertyValueUpdated",
                    res.getMismatchValue("p1ComplexProperty.c1ComplexProperty.c2ComplexProperty.c3Property"));

            // 4階層目の文字列Property配列を指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id002");
            String bodyStringArray = "{"
                    + "  \"p1ComplexProperty\": {"
                    + "    \"c1ComplexProperty\": {"
                    + "      \"c2ComplexProperty\": {"
                    + "        \"c3ListProperty\": ["
                    + "          \"c3ListPropertyValueUpdated1\","
                    + "          \"c3ListPropertyValueUpdated2\""
                    + "        ]"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT, parseStringToJSONObject(bodyStringArray),
                    cellName, boxName, odataColName,
                    entityTypeName, "id002", "*");
            // 更新確認
            response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, "id002", HttpStatus.SC_OK);
            res = CompareJSON.compareJSON(
                    getDefaultUserDataRequestBody("id002"),
                    response.getBody());
            assertNotNull(res);
            assertEquals(1, res.size());
            JSONArray resArray = (JSONArray) res
                    .getMismatchValue("p1ComplexProperty.c1ComplexProperty.c2ComplexProperty.c3ListProperty");
            assertEquals(2, resArray.size());
            assertTrue(resArray.contains("c3ListPropertyValueUpdated1"));
            assertTrue(resArray.contains("c3ListPropertyValueUpdated2"));

            // 配列型の4階層目の文字列Propertyを指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id003");
            String bodyArrayComplexString = "{"
                    + "  \"p1ComplexListProperty\": ["
                    + "    {"
                    + "      \"lc1ListComplexProperty\": ["
                    + "        {"
                    + "          \"lc2ListComplexProperty\": ["
                    + "            {"
                    + "              \"lc3Property\": \"lc3PropertyValue\""
                    + "            }"
                    + "          ]"
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ]"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT,
                    parseStringToJSONObject(bodyArrayComplexString),
                    cellName, boxName, odataColName,
                    entityTypeName, "id003", "*");
            // ComplexType型のListのMERGEは未サポートのため、MERGE後のチェックは省略

            // 配列型の4階層目の文字列Propertyを指定してMERGE
            createUserData(token, cellName, boxName, odataColName, entityTypeName, "id004");
            String bodyArrayComplexArrayString = "{"
                    + "  \"p1ComplexListProperty\": ["
                    + "    {"
                    + "      \"lc1ListComplexProperty\": ["
                    + "        {"
                    + "          \"lc2ListComplexProperty\": ["
                    + "            {"
                    + "              \"lc3ListProperty\": ["
                    + "                \"lc3ListPropertyValueUpdated1\","
                    + "                \"lc3ListPropertyValueUpdated2\""
                    + "              ]"
                    + "            }"
                    + "          ]"
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ]"
                    + "}";
            UserDataUtils.merge(token, HttpStatus.SC_NO_CONTENT,
                    parseStringToJSONObject(bodyArrayComplexArrayString),
                    cellName, boxName, odataColName,
                    entityTypeName, "id004", "*");
            // ComplexType型のListのMERGEは未サポートのため、MERGE後のチェックは省略

        } catch (ParseException e) {
            fail(e.getMessage());
        } finally {
            // Cell一括削除
            Setup.cellBulkDeletion(cellName);
        }

    }

    /**
     * 複数階層あるデータに対してZeroToZeroのNP経由登録して_ソース側のデータに変更がないこと.
     */
    @Test
    public final void 複数階層あるデータに対してZeroToZeroのNP経由登録して_ソース側のデータに変更がないこと() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userDataDeepComplexTypeTestCell";
        String boxName = "box";
        String odataColName = "col";
        String entityTypeName = "entity";

        try {
            createODataCollection(token, cellName, boxName, odataColName);
            createSchema(token, cellName, boxName, odataColName, entityTypeName);

            String entityTypeNameNP = "entityNP";
            String sourceMultiplicity = "0..1";
            String targetMultiplicity = "0..1";
            createLinkedEntityType(token, cellName, boxName, odataColName, entityTypeName, entityTypeNameNP,
                    sourceMultiplicity, targetMultiplicity);

            String srcId = "id001";
            createUserData(token, cellName, boxName, odataColName, entityTypeName, srcId);

            TResponse response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);

            UserDataUtils.createViaNP(token,
                    parseStringToJSONObject("{\"__id\":\"idNp\"}"),
                    cellName, boxName, odataColName, entityTypeName, srcId,
                    entityTypeNameNP, HttpStatus.SC_CREATED);

            // ソース側のデータに変更がないこと
            TResponse modResponse = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(
                    response.getBody(),
                    modResponse.getBody());
            assertNull(res);

        } catch (ParseException e) {
            fail(e.getMessage());
        } finally {
            // Cell一括削除
            Setup.cellBulkDeletion(cellName);
        }

    }

    /**
     * 複数階層あるデータに対してZeroToManyのNP経由登録して_ソース側のデータに変更がないこと.
     */
    @Test
    public final void 複数階層あるデータに対してZeroToManyのNP経由登録して_ソース側のデータに変更がないこと() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userDataDeepComplexTypeTestCell";
        String boxName = "box";
        String odataColName = "col";
        String entityTypeName = "entity";

        try {
            createODataCollection(token, cellName, boxName, odataColName);
            createSchema(token, cellName, boxName, odataColName, entityTypeName);

            String entityTypeNameNP = "entityNP";
            String sourceMultiplicity = "0..1";
            String targetMultiplicity = "*";
            createLinkedEntityType(token, cellName, boxName, odataColName, entityTypeName, entityTypeNameNP,
                    sourceMultiplicity, targetMultiplicity);

            String srcId = "id001";
            createUserData(token, cellName, boxName, odataColName, entityTypeName, srcId);

            TResponse response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);

            UserDataUtils.createViaNP(token,
                    parseStringToJSONObject("{\"__id\":\"idNp\"}"),
                    cellName, boxName, odataColName, entityTypeName, srcId,
                    entityTypeNameNP, HttpStatus.SC_CREATED);

            // ソース側のデータに変更がないこと
            TResponse modResponse = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(
                    response.getBody(),
                    modResponse.getBody());
            assertNull(res);

        } catch (ParseException e) {
            fail(e.getMessage());
        } finally {
            // Cell一括削除
            Setup.cellBulkDeletion(cellName);
        }

    }

    /**
     * 複数階層あるデータに対してManyToZeroのNP経由登録して_ソース側のデータに変更がないこと.
     */
    @Test
    public final void 複数階層あるデータに対してManyToZeroのNP経由登録して_ソース側のデータに変更がないこと() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userDataDeepComplexTypeTestCell";
        String boxName = "box";
        String odataColName = "col";
        String entityTypeName = "entity";

        try {
            createODataCollection(token, cellName, boxName, odataColName);
            createSchema(token, cellName, boxName, odataColName, entityTypeName);

            String entityTypeNameNP = "entityNP";
            String sourceMultiplicity = "*";
            String targetMultiplicity = "0..1";
            createLinkedEntityType(token, cellName, boxName, odataColName, entityTypeName, entityTypeNameNP,
                    sourceMultiplicity, targetMultiplicity);

            String srcId = "id001";
            createUserData(token, cellName, boxName, odataColName, entityTypeName, srcId);

            TResponse response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);

            UserDataUtils.createViaNP(token,
                    parseStringToJSONObject("{\"__id\":\"idNp\"}"),
                    cellName, boxName, odataColName, entityTypeName, srcId,
                    entityTypeNameNP, HttpStatus.SC_CREATED);

            // ソース側のデータに変更がないこと
            TResponse modResponse = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(
                    response.getBody(),
                    modResponse.getBody());
            assertNull(res);

        } catch (ParseException e) {
            fail(e.getMessage());
        } finally {
            // Cell一括削除
            Setup.cellBulkDeletion(cellName);
        }

    }

    /**
     * 複数階層あるデータに対してManyToManyのNP経由登録して_ソース側のデータに変更がないこと.
     */
    @Test
    public final void 複数階層あるデータに対してManyToManyのNP経由登録して_ソース側のデータに変更がないこと() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userDataDeepComplexTypeTestCell";
        String boxName = "box";
        String odataColName = "col";
        String entityTypeName = "entity";

        try {
            createODataCollection(token, cellName, boxName, odataColName);
            createSchema(token, cellName, boxName, odataColName, entityTypeName);

            String entityTypeNameNP = "entityNP";
            String sourceMultiplicity = "*";
            String targetMultiplicity = "*";
            createLinkedEntityType(token, cellName, boxName, odataColName, entityTypeName, entityTypeNameNP,
                    sourceMultiplicity, targetMultiplicity);

            String srcId = "id001";
            createUserData(token, cellName, boxName, odataColName, entityTypeName, srcId);

            TResponse response = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);

            UserDataUtils.createViaNP(token,
                    parseStringToJSONObject("{\"__id\":\"idNp\"}"),
                    cellName, boxName, odataColName, entityTypeName, srcId,
                    entityTypeNameNP, HttpStatus.SC_CREATED);

            // ソース側のデータに変更がないこと
            TResponse modResponse = UserDataUtils.get(cellName, token, boxName, odataColName,
                    entityTypeName, srcId, HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(
                    response.getBody(),
                    modResponse.getBody());
            assertNull(res);

        } catch (ParseException e) {
            fail(e.getMessage());
        } finally {
            // Cell一括削除
            Setup.cellBulkDeletion(cellName);
        }

    }

    private void createLinkedEntityType(String token,
            String cellName,
            String boxName,
            String odataColName,
            String sourceEntityTypeName,
            String targetEntityTypeName,
            String sourceMultiplicity,
            String targetMultiplicity) {
        // EntityType作成
        EntityTypeUtils.create(cellName, token, boxName, odataColName,
                targetEntityTypeName, HttpStatus.SC_CREATED);
        // AssociationEnd作成
        AssociationEndUtils.create(token, sourceMultiplicity, cellName, boxName, odataColName,
                HttpStatus.SC_CREATED, "assoc1", sourceEntityTypeName);
        AssociationEndUtils.create(token, targetMultiplicity, cellName, boxName, odataColName,
                HttpStatus.SC_CREATED, "assoc2", targetEntityTypeName);
        // AsoociationEnd $links作成
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, odataColName,
                sourceEntityTypeName, targetEntityTypeName, "assoc1", "assoc2", HttpStatus.SC_NO_CONTENT);
    }

    private JSONObject parseStringToJSONObject(String body) {
        JSONObject bodyJSON = null;
        try {
            bodyJSON = (JSONObject) new JSONParser().parse(body);
        } catch (ParseException e) {
            fail("parse failed. [" + e.getMessage() + "]");
        }
        return bodyJSON;
    }

    private void createSchema(
            String token,
            String cellName,
            String boxName,
            String odataColName,
            String entityTypeName) {
        // EntityType作成
        EntityTypeUtils.create(cellName, token, boxName, odataColName,
                entityTypeName, HttpStatus.SC_CREATED);

        // 3階層目のComplexType作成
        UserDataUtils.createComplexType(cellName, boxName, odataColName, COMPLEX_TYPE_NAME_3);
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c3Property",
                COMPLEX_TYPE_NAME_3, "Edm.String", true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c3ListProperty",
                COMPLEX_TYPE_NAME_3, "Edm.String", true, null, "List");

        // 3階層目のComplexType(List)作成
        UserDataUtils.createComplexType(cellName, boxName, odataColName, LIST_COMPLEX_TYPE_NAME_3);
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc3Property",
                LIST_COMPLEX_TYPE_NAME_3, "Edm.String", true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc3ListProperty",
                LIST_COMPLEX_TYPE_NAME_3, "Edm.String", true, null, "List");

        // 2階層目のComplexType作成
        UserDataUtils.createComplexType(cellName, boxName, odataColName, COMPLEX_TYPE_NAME_2);
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c2Property",
                COMPLEX_TYPE_NAME_2, "Edm.String", true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c2ListProperty",
                COMPLEX_TYPE_NAME_2, "Edm.String", true, null, "List");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c2ComplexProperty",
                COMPLEX_TYPE_NAME_2, COMPLEX_TYPE_NAME_3, true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c2ListComplexProperty",
                COMPLEX_TYPE_NAME_2, LIST_COMPLEX_TYPE_NAME_3, true, null, "List");

        // 2階層目のComplexType(List)作成
        UserDataUtils.createComplexType(cellName, boxName, odataColName, LIST_COMPLEX_TYPE_NAME_2);
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc2Property",
                LIST_COMPLEX_TYPE_NAME_2, "Edm.String", true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc2ListProperty",
                LIST_COMPLEX_TYPE_NAME_2, "Edm.String", true, null, "List");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc2ComplexProperty",
                LIST_COMPLEX_TYPE_NAME_2, COMPLEX_TYPE_NAME_3, true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc2ListComplexProperty",
                LIST_COMPLEX_TYPE_NAME_2, LIST_COMPLEX_TYPE_NAME_3, true, null, "List");

        // 1階層目のComplexType作成
        UserDataUtils.createComplexType(cellName, boxName, odataColName, COMPLEX_TYPE_NAME_1);
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c1Property",
                COMPLEX_TYPE_NAME_1, "Edm.String", true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c1ListProperty",
                COMPLEX_TYPE_NAME_1, "Edm.String", true, null, "List");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c1ComplexProperty",
                COMPLEX_TYPE_NAME_1, COMPLEX_TYPE_NAME_2, true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "c1ListComplexProperty",
                COMPLEX_TYPE_NAME_1, LIST_COMPLEX_TYPE_NAME_2, true, null, "List");

        // 1階層目のComplexType(List)作成
        UserDataUtils.createComplexType(cellName, boxName, odataColName, LIST_COMPLEX_TYPE_NAME_1);
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc1Property",
                LIST_COMPLEX_TYPE_NAME_1, "Edm.String", true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc1ListProperty",
                LIST_COMPLEX_TYPE_NAME_1, "Edm.String", true, null, "List");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc1ComplexProperty",
                LIST_COMPLEX_TYPE_NAME_1, COMPLEX_TYPE_NAME_2, true, null, "None");
        UserDataUtils.createComplexTypeProperty(cellName, boxName, odataColName, "lc1ListComplexProperty",
                LIST_COMPLEX_TYPE_NAME_1, LIST_COMPLEX_TYPE_NAME_2, true, null, "List");

        // Property作成
        UserDataUtils.createProperty(cellName, boxName, odataColName, "p1Property",
                entityTypeName, "Edm.String", true, null, "None", false, null);
        UserDataUtils.createProperty(cellName, boxName, odataColName, "p1ListProperty",
                entityTypeName, "Edm.String", true, null, "List", false, null);
        UserDataUtils.createProperty(cellName, boxName, odataColName, "p1ComplexProperty",
                entityTypeName, COMPLEX_TYPE_NAME_1, true, null, "None", false, null);
        UserDataUtils.createProperty(cellName, boxName, odataColName, "p1ComplexListProperty",
                entityTypeName, LIST_COMPLEX_TYPE_NAME_1, true, null, "List", false, null);

    }

    private TResponse createUserData(String token,
            String cellName,
            String boxName,
            String odataColName,
            String entityTypeName,
            String id) {
        // ユーザOData作成
        return UserDataUtils.create(token, HttpStatus.SC_CREATED, getDefaultUserDataRequestBody(id),
                cellName, boxName, odataColName, entityTypeName);
    }

    private void createODataCollection(String token, String cellName, String boxName, String odataColName) {
        // Cell作成
        CellUtils.create(cellName, token, HttpStatus.SC_CREATED);

        // Box作成
        BoxUtils.create(cellName, boxName, token, HttpStatus.SC_CREATED);

        // Collection作成
        DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, cellName, boxName, odataColName);
    }

    private String getDefaultUserDataRequestBody(String id) {
        return "{"
                + "  \"__id\": \"" + id + "\","
                + "  \"p1Property\": \"p1PropertyValue\","
                + "  \"p1ListProperty\": ["
                + "    \"p1ListPropertyValue1\","
                + "    \"p1ListPropertyValue2\""
                + "  ],"
                + "  \"p1ComplexProperty\": {"
                + "    \"c1Property\": \"c1PropertyValue\","
                + "    \"c1ListProperty\": ["
                + "      \"c1ListPropertyValue1\","
                + "      \"c1ListPropertyValue2\""
                + "    ],"
                + "    \"c1ComplexProperty\": {"
                + "      \"c2Property\": \"c2PropertyValue\","
                + "      \"c2ListProperty\": ["
                + "        \"c2ListPropertyValue1\","
                + "        \"c2ListPropertyValue2\""
                + "      ],"
                + "      \"c2ComplexProperty\": {"
                + "        \"c3Property\": \"c3PropertyValue\","
                + "        \"c3ListProperty\": ["
                + "          \"c3ListPropertyValue1\","
                + "          \"c3ListPropertyValue2\""
                + "        ]"
                + "      },"
                + "      \"c2ListComplexProperty\": ["
                + "        {"
                + "          \"lc3Property\": \"lc3PropertyValue\","
                + "          \"lc3ListProperty\": ["
                + "            \"lc3ListPropertyValue1\","
                + "            \"lc3ListPropertyValue2\""
                + "          ]"
                + "        }"
                + "      ]"
                + "    },"
                + "    \"c1ListComplexProperty\": ["
                + "      {"
                + "        \"lc2Property\": \"lc2PropertyValue\","
                + "        \"lc2ListProperty\": ["
                + "          \"lc2ListPropertyValue1\","
                + "          \"lc2ListPropertyValue2\""
                + "        ],"
                + "        \"lc2ComplexProperty\": {"
                + "          \"c3Property\": \"c3PropertyValue\","
                + "          \"c3ListProperty\": ["
                + "            \"c3ListPropertyValue1\","
                + "            \"c3ListPropertyValue2\""
                + "          ]"
                + "        },"
                + "        \"lc2ListComplexProperty\": ["
                + "          {"
                + "            \"lc3Property\": \"lc3PropertyValue\","
                + "            \"lc3ListProperty\": ["
                + "              \"lc3ListPropertyValue1\","
                + "              \"lc3ListPropertyValue2\""
                + "            ]"
                + "          }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  },"
                + "  \"p1ComplexListProperty\": ["
                + "    {"
                + "      \"lc1Property\": \"lc1PropertyValue\","
                + "      \"lc1ListProperty\": ["
                + "        \"lc1ListPropertyValue1\","
                + "        \"lc1ListPropertyValue2\""
                + "      ],"
                + "      \"lc1ComplexProperty\": {"
                + "        \"c2Property\": \"c2PropertyValue\","
                + "        \"c2ListProperty\": ["
                + "          \"c2ListPropertyValue1\","
                + "          \"c2ListPropertyValue2\""
                + "        ],"
                + "        \"c2ComplexProperty\": {"
                + "          \"c3Property\": \"c3PropertyValue\","
                + "          \"c3ListProperty\": ["
                + "            \"c3ListPropertyValue1\","
                + "            \"c3ListPropertyValue2\""
                + "          ]"
                + "        },"
                + "        \"c2ListComplexProperty\": ["
                + "          {"
                + "            \"lc3Property\": \"lc3PropertyValue\","
                + "            \"lc3ListProperty\": ["
                + "              \"lc3ListPropertyValue1\","
                + "              \"lc3ListPropertyValue2\""
                + "            ]"
                + "          }"
                + "        ]"
                + "      },"
                + "      \"lc1ListComplexProperty\": ["
                + "        {"
                + "          \"lc2Property\": \"lc2PropertyValue\","
                + "          \"lc2ListProperty\": ["
                + "            \"lc2ListPropertyValue1\","
                + "            \"lc2ListPropertyValue2\""
                + "          ],"
                + "          \"lc2ComplexProperty\": {"
                + "            \"c3Property\": \"c3PropertyValue\","
                + "            \"c3ListProperty\": ["
                + "              \"c3ListPropertyValue1\","
                + "              \"c3ListPropertyValue2\""
                + "            ]"
                + "          },"
                + "          \"lc2ListComplexProperty\": ["
                + "            {"
                + "              \"lc3Property\": \"lc3PropertyValue\","
                + "              \"lc3ListProperty\": ["
                + "                \"lc3ListPropertyValue1\","
                + "                \"lc3ListPropertyValue2\""
                + "              ]"
                + "            }"
                + "          ]"
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}";
    }
}
