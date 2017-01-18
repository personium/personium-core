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
package com.fujitsu.dc.test.unit.core.model.impl.es.odata;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmComplexType.Builder;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.core.model.ctl.EntityType;
import com.fujitsu.dc.core.model.impl.es.doc.ComplexTypePropertyDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.PropertyDocHandler;
import com.fujitsu.dc.core.model.impl.es.odata.PropertyLimitChecker;
import com.fujitsu.dc.core.model.impl.es.odata.PropertyLimitChecker.CheckError;
import com.fujitsu.dc.test.categories.Unit;

/**
 * UnitCtlODataProducerユニットテストクラス.
 */
@Category({ Unit.class })
public class PropertyLimitCheckerTest {

    private static final String NS = "UserData";

    private EdmEntityType.Builder EntityTypeを作成(String entityTypeName) {
        EdmEntityType.Builder eBuilder = EdmEntityType.newBuilder();
        eBuilder.setName(entityTypeName).addKeys("root").setNamespace(NS);
        return eBuilder;
    }

    private EdmComplexType.Builder ComplexTypeを作成(String name) {
        EdmComplexType.Builder ctBuilder = EdmComplexType.newBuilder();
        ctBuilder.setName(name).setNamespace(NS);
        return ctBuilder;
    }

    private void EntityTypeにsimple型Propertyを指定数作成(int count, EdmEntityType.Builder builder) {
        for (int i = 0; i < count; i++) {
            EdmProperty.Builder pBuilder = EdmProperty.newBuilder(String.format("p_prop%03d", i))
                    .setType(EdmSimpleType.STRING);
            builder.addProperties(pBuilder);

            // 予約語プロパティ用ダミー
            pBuilder = EdmProperty.newBuilder(String.format("_reservedProp_%03d", i))
                    .setType(EdmSimpleType.STRING);
            builder.addProperties(pBuilder);
        }
    }

    private EdmComplexType.Builder EntityTypeにcomplex型Propertyを1つ作成(String complexTypeName,
            EdmEntityType.Builder builder) {
        EdmComplexType.Builder ctBuilder = EdmComplexType.newBuilder();
        ctBuilder.setName(complexTypeName).setNamespace(NS);

        EdmProperty.Builder pBuilder = EdmProperty.newBuilder(complexTypeName + "_prop")
                    .setType(ctBuilder);
        builder.addProperties(pBuilder);
        return ctBuilder;
    }

    private void ComplexTypeにsimple型Propertyを指定数作成(int count, EdmComplexType.Builder builder) {
        for (int i = 0; i < count; i++) {
            EdmProperty.Builder pBuilder = EdmProperty.newBuilder(String.format("p_prop%03d", i))
                    .setType(EdmSimpleType.STRING);
            builder.addProperties(pBuilder);

            // 予約語プロパティ用ダミー
            pBuilder = EdmProperty.newBuilder(String.format("_reservedProp_%03d", i))
                    .setType(EdmSimpleType.STRING);
            builder.addProperties(pBuilder);
        }
    }

    private EdmComplexType.Builder ComplexTypeにcomplex型Propertyを指定数作成(int count, String complexTypeName,
            EdmComplexType.Builder builder) {
        // ComplexTypeを新規に作成
        EdmComplexType.Builder ctBuilder = EdmComplexType.newBuilder();
        ctBuilder.setName(complexTypeName).setNamespace(NS);

        // ComplexTypeに加えるべき プロパティを作成
        List<EdmProperty.Builder> pBuilderList = new ArrayList<EdmProperty.Builder>();
        for (int i = 0; i < count; i++) {
            EdmProperty.Builder pBuilder = EdmProperty.newBuilder(String.format("%s_prop_%03d", complexTypeName, i))
                    .setType(ctBuilder);
            pBuilderList.add(pBuilder);

            // 予約語プロパティ用ダミー
            pBuilder = EdmProperty.newBuilder(String.format("_reservedProp_%03d", i))
                    .setType(EdmSimpleType.STRING);
            builder.addProperties(pBuilder);
        }

        builder.addProperties(pBuilderList);
        return ctBuilder;
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でProperty追加後＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        EntityTypeにsimple型Propertyを指定数作成(399, entityType);

        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でProperty追加後＿制限値を超えた場合異常を通知すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        EntityTypeにsimple型Propertyを指定数作成(400, entityType);

        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でComplex型のProperty追加後＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();
        for (int i = 0; i < 19; i++) {
            Builder cpBuilder = EntityTypeにcomplex型Propertyを1つ作成(String.format("complexType_%03d", i), entityType);
            cpBuilderList.add(cpBuilder);
        }

        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("newComplexType");
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でComplex型のProperty追加後＿制限値を超えた場合異常を通知すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();
        for (int i = 0; i < 20; i++) {
            Builder cpBuilder = EntityTypeにcomplex型Propertyを1つ作成(String.format("complexType_%03d", i), entityType);
            cpBuilderList.add(cpBuilder);
        }

        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("newComplexType");
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でSimple型Property追加後＿Simple型_Complex型合わせて＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        EntityTypeにsimple型Propertyを指定数作成(379, entityType);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();
        for (int i = 0; i < 20; i++) {
            Builder cpBuilder = EntityTypeにcomplex型Propertyを1つ作成(String.format("complexType_%03d", i), entityType);
            cpBuilderList.add(cpBuilder);
        }

        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("newComplexType");
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でSimple型Property追加後＿Simple型_Complex型合わせて＿制限値を超えた場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        EntityTypeにsimple型Propertyを指定数作成(380, entityType);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();
        for (int i = 0; i < 20; i++) {
            Builder cpBuilder = EntityTypeにcomplex型Propertyを1つ作成(String.format("complexType_%03d", i), entityType);
            cpBuilderList.add(cpBuilder);
        }

        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("newComplexType");
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でComplex型Property追加後＿Simple型_Complex型合わせて＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        EntityTypeにsimple型Propertyを指定数作成(380, entityType);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();
        for (int i = 0; i < 19; i++) {
            Builder cpBuilder = EntityTypeにcomplex型Propertyを1つ作成(String.format("complexType_%03d", i), entityType);
            cpBuilderList.add(cpBuilder);
        }

        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("newComplexType");
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層１でComplex型Property追加後＿Simple型_Complex型合わせて＿制限値を超えた場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        EntityTypeにsimple型Propertyを指定数作成(380, entityType);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();
        for (int i = 0; i < 20; i++) {
            Builder cpBuilder = EntityTypeにcomplex型Propertyを1つ作成(String.format("complexType_%03d", i), entityType);
            cpBuilderList.add(cpBuilder);
        }

        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("newComplexType");
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", testENTITY);
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層２にProperty追加後＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(49, ctBuilder);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層２にProperty追加後＿制限値を超えた場合異常を通知すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);
        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        // 階層チェック時、ComplexTypeCheck時の２つ出力される。
        assertEquals(2, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層２にComplex型のProperty追加後＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(4, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType2");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層２にComplex型のProperty追加後＿制限値を超えた場合異常を通知すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType2");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層３にProperty追加後＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(29, targetComplexTypeBuilder);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType2");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層３にProperty追加後＿制限値を超えた場合異常を通知すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(30, targetComplexTypeBuilder);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType2");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層３にComplex型のProperty追加後＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(29, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(1, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType3");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType2");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層３にComplex型のProperty追加後＿制限値を超えた場合異常を通知すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(29, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(2, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType3");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType2");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層４にProperty追加後＿制限値以下となる場合正常終了すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(29, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(2, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        ComplexTypeにsimple型Propertyを指定数作成(9, targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType3");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層４にProperty追加後＿制限値を超えた場合異常を通知すること() {
        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(29, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(2, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        ComplexTypeにsimple型Propertyを指定数作成(10, targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType3");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 階層４にComplex型のProperty追加後＿制限値を超えた場合異常を通知すること() {
        // ★ 4階層目への ComplexType追加は定義上、エラーとなるべき。

        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        cpBuilderList.add(ctBuilder);

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(29, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(1, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        // ここが4階層目
        ComplexTypeにsimple型Propertyを指定数作成(10, targetComplexTypeBuilder2);
        Builder targetComplexTypeBuilder3 = ComplexTypeにcomplex型Propertyを指定数作成(1, "newComplexType4",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder3);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "newComplexType4");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType3");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Property追加後＿全体プロパティ数と同数になった場合正常終了すること() {

        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        cpBuilderList.add(ctBuilder);

        EntityTypeにsimple型Propertyを指定数作成(83, entityType);

        EdmDataServices.Builder builder = EdmDataServices.newBuilder();

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(30, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(2, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        // ここが4階層目
        ComplexTypeにsimple型Propertyを指定数作成(10, targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", "testEntity");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Property追加後＿全体プロパティ数を超えた異常を通知すること() {

        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        cpBuilderList.add(ctBuilder);

        EntityTypeにsimple型Propertyを指定数作成(84, entityType);

        EdmDataServices.Builder builder = EdmDataServices.newBuilder();

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(30, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(2, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        // ここが4階層目
        ComplexTypeにsimple型Propertyを指定数作成(10, targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new PropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_EntityType.Name_uniqueKey", "testEntity");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(EntityType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ComplexTypeProperty追加後＿全体プロパティ数最大値と一致する場合正常終了すること() {

        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        cpBuilderList.add(ctBuilder);

        EntityTypeにsimple型Propertyを指定数作成(84, entityType);

        EdmDataServices.Builder builder = EdmDataServices.newBuilder();

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(49, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(30, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(2, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        // ここが4階層目
        ComplexTypeにsimple型Propertyを指定数作成(10, targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ComplexTypeProperty追加後＿全体プロパティ数を超えた異常を通知すること() {

        String testENTITY = "testEntity";
        org.odata4j.edm.EdmEntityType.Builder entityType = EntityTypeを作成(testENTITY);

        List<EdmComplexType.Builder> cpBuilderList = new ArrayList<EdmComplexType.Builder>();

        EdmComplexType.Builder ctBuilder = EntityTypeにcomplex型Propertyを1つ作成("newComplexType", entityType);
        cpBuilderList.add(ctBuilder);

        EntityTypeにsimple型Propertyを指定数作成(85, entityType);

        EdmDataServices.Builder builder = EdmDataServices.newBuilder();

        // ここが２階層目となる
        ComplexTypeにsimple型Propertyを指定数作成(49, ctBuilder);
        Builder targetComplexTypeBuilder = ComplexTypeにcomplex型Propertyを指定数作成(5, "newComplexType2", ctBuilder);
        cpBuilderList.add(targetComplexTypeBuilder);

        // ここが３階層目
        ComplexTypeにsimple型Propertyを指定数作成(30, targetComplexTypeBuilder);
        Builder targetComplexTypeBuilder2 = ComplexTypeにcomplex型Propertyを指定数作成(2, "newComplexType3",
                targetComplexTypeBuilder);
        cpBuilderList.add(targetComplexTypeBuilder2);

        // ここが4階層目
        ComplexTypeにsimple型Propertyを指定数作成(10, targetComplexTypeBuilder2);

        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(entityType).setNamespace(NS)
                .addComplexTypes(cpBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "newComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }


    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void EntityTypeに関連付けられていないComplexTypeにProperty追加後＿制限値_50_と同数になる場合正常終了すること() {
        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("testComplexType");
        ComplexTypeにsimple型Propertyを指定数作成(49, ctBuilder);

        List<EdmComplexType.Builder> ctBuilderList = new ArrayList<EdmComplexType.Builder>();
        ctBuilderList.add(ctBuilder);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        EdmSchema.Builder schema = EdmSchema.newBuilder().setNamespace(NS).addComplexTypes(ctBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "testComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(0, errors.size());
    }

    /**
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void EntityTypeに関連付けられていないComplexTypeにProperty追加後＿制限値_50_を超えたという異常を通知すること() {
        EdmComplexType.Builder ctBuilder = ComplexTypeを作成("testComplexType");
        ComplexTypeにsimple型Propertyを指定数作成(50, ctBuilder);

        List<EdmComplexType.Builder> ctBuilderList = new ArrayList<EdmComplexType.Builder>();
        ctBuilderList.add(ctBuilder);
        EdmDataServices.Builder builder = EdmDataServices.newBuilder();
        EdmSchema.Builder schema = EdmSchema.newBuilder().setNamespace(NS).addComplexTypes(ctBuilderList);
        EdmDataServices metadata = builder.addSchemas(schema).build();

        PropertyDocHandler handler = new ComplexTypePropertyDocHandler();
        JSONObject staticFields = new JSONObject();
        staticFields.put("Type", "Edm.String");
        handler.setStaticFields(staticFields);

        Map<String, String> entityTypeMap = new HashMap<String, String>();
        entityTypeMap.put("_ComplexType.Name_uniqueKey", "testComplexType");
        handler.setEntityTypeMap(entityTypeMap);
        handler.setEntityTypeId("_uniqueKey");
        Map<String, Object> manyToOneKindMap = new HashMap<String, Object>();
        manyToOneKindMap.put(ComplexType.EDM_TYPE_NAME, "_uniqueKey");
        handler.setManyToOnelinkId(manyToOneKindMap);

        PropertyLimitChecker checker = new PropertyLimitChecker(metadata, handler);
        List<CheckError> errors = checker.checkPropertyLimits();
        assertEquals(1, errors.size());
    }

}
