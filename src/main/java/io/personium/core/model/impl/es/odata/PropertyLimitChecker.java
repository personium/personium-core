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
package io.personium.core.model.impl.es.odata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.core4j.Enumerable;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmDataServices.Builder;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.DcCoreConfig;
import io.personium.core.model.impl.es.doc.ComplexTypePropertyDocHandler;
import io.personium.core.model.impl.es.doc.PropertyDocHandler;

import java.util.Arrays;

/**
 * EntityType内の Property数制限数をチェックするためのメソッドを実装しているクラス.
 */
public class PropertyLimitChecker {

    static Logger log = LoggerFactory.getLogger(PropertyLimitChecker.class);

    /**
     * 制限チェックでエラーが発生した場合の通知オブジェクト.
     */
    public static class CheckError {
        String entityTypeName;
        String message;

        /**
         * コンストラクタ.
         * @param entityTypeName EntityType名
         * @param message エラー詳細
         */
        public CheckError(String entityTypeName, String message) {
            this.entityTypeName = entityTypeName;
            this.message = message;
        }

        /**
         * EntityType名を返す.
         * @return EntityType名
         */
        public String getEntityTypeName() {
            return entityTypeName;
        }

        /**
         * エラーメッセージを返す.
         * @return エラーメッセージ
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * 内部例外クラス.
     */
    @SuppressWarnings("serial")
    static class PropertyLimitException extends Exception {

        private String entityTypeName = null;

        /**
         * コンストラクタ.
         * @param message メッセージ
         */
        PropertyLimitException(String message) {
            super(message);
        }

        /**
         * コンストラクタ.
         * @param entityTypeName EntityType名
         * @param message メッセージ
         */
        PropertyLimitException(String entityTypeName, String message) {
            super(message);
            this.entityTypeName = entityTypeName;
        }

        /**
         * EntityType名を返す.
         * @return EntityType名
         */
        public String getEntityTypeName() {
            return entityTypeName;
        }

        /**
         * EntityType名を設定する.
         * @param entityTypeName EntityType名
         */
        public void setEntityTypeName(String entityTypeName) {
            this.entityTypeName = entityTypeName;
        }
    }

    /**
     * EntityTypeの階層の深さが制限を超えた場合の例外.
     */
    @SuppressWarnings("serial")
    static class EntityTypeDepthExceedException extends PropertyLimitException {
        /**
         * constructor.
         * @param message メッセージ
         */
        EntityTypeDepthExceedException(String message) {
            super(message);
        }
    }

    EdmDataServices metadata = null;
    int maxPropertyLimitInEntityType = 0;
    int maxDepth = 0;
    int[] simplePropertyLimits = null;
    int[] complexPropertyLimits = null;
    int simpleMaxForOverAllLayers = 0;
    int complexMaxForOverallLayers = 0;

    /**
     * default constructor.
     */
    public PropertyLimitChecker() {
        this.maxPropertyLimitInEntityType = DcCoreConfig.getMaxPropertyCountInEntityType();
        this.simplePropertyLimits = DcCoreConfig.getUserdataSimpleTypePropertyLimits();
        this.complexPropertyLimits = DcCoreConfig.getUserdataComplexTypePropertyLimits();
        this.maxDepth = simplePropertyLimits.length;

        int[] maxPropLimitCopy = Arrays.copyOf(simplePropertyLimits, simplePropertyLimits.length);
        Arrays.sort(maxPropLimitCopy);
        this.simpleMaxForOverAllLayers = maxPropLimitCopy[maxPropLimitCopy.length - 1];
        maxPropLimitCopy = Arrays.copyOf(complexPropertyLimits, complexPropertyLimits.length);
        Arrays.sort(maxPropLimitCopy);
        this.complexMaxForOverallLayers = maxPropLimitCopy[maxPropLimitCopy.length - 1];
    }

    /**
     * constructor.
     * @param metadata UserDataのメタデータ（プロパティ更新前)
     * @param entityTypeName 追加対象のEntityType名
     * @param dynamicPropCount 追加されるDynamicProperty数
     */
    public PropertyLimitChecker(final EdmDataServices metadata,
            final String entityTypeName, final int dynamicPropCount) {
        this();
        // 引数で受け取る metadataは更新前のものなので、追加後のメタデータを作成する必要がある。
        Builder builder = EdmDataServices.newBuilder(metadata);

        // 追加対象数分、ダミーでプロパティをスキーマ情報に追加する
        String dummyKey = "dummy" + System.currentTimeMillis();
        for (int i = 0; i < dynamicPropCount; i++) {
            org.odata4j.edm.EdmEntityType.Builder entityTypeBuilder = builder
                    .findEdmEntityType(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + entityTypeName);
            org.odata4j.edm.EdmProperty.Builder propertyBuilder =
                    EdmProperty.newBuilder(String.format("%s_%03d", dummyKey, i))
                    .setType(EdmSimpleType.getSimple("Edm.String"));
            entityTypeBuilder.addProperties(propertyBuilder);
        }
        // 変更後のメタデータを作成
        this.metadata = builder.build();
    }

    /**
     * constructor.
     * @param metadata UserDataのメタデータ（プロパティ更新前)
     * @param propHandler 追加するプロパティのハンドラ
     */
    public PropertyLimitChecker(final EdmDataServices metadata, PropertyDocHandler propHandler) {
        this();

        // 引数で受け取る metadataは更新前のものなので、追加後のメタデータを作成する必要がある。
        Builder builder = EdmDataServices.newBuilder(metadata);

        // ここで新規Property, ComplexTypeProperty等を追加
        final String dummyPropertyKey = "DUMMY" + System.currentTimeMillis();
        if (propHandler instanceof ComplexTypePropertyDocHandler) {
            // ComplexTypePropertyDocHandlerの場合の処理
            org.odata4j.edm.EdmComplexType.Builder complexTypeBuilder = builder
                    .findEdmComplexType(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + propHandler
                    .getEntityTypeName());
            Map<String, Object> staticFields = propHandler.getStaticFields();
            String typeName = (String) staticFields.get("Type");
            EdmType type = EdmSimpleType.getSimple(typeName);
            if (null != type) {
                org.odata4j.edm.EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        type);
                complexTypeBuilder.addProperties(propertyBuilder);
            } else {
                EdmComplexType complex = metadata.findEdmComplexType(UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                        + typeName);
                org.odata4j.edm.EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        complex);
                complexTypeBuilder.addProperties(propertyBuilder);
            }
        } else {
            org.odata4j.edm.EdmEntityType.Builder entityTypeBuilder = builder
                    .findEdmEntityType(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + propHandler
                    .getEntityTypeName());
            Map<String, Object> staticFields = propHandler.getStaticFields();
            String typeName = (String) staticFields.get("Type");
            EdmType type = EdmSimpleType.getSimple(typeName);
            if (null != type) {
                org.odata4j.edm.EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        type);
                entityTypeBuilder.addProperties(propertyBuilder);
            } else {
                EdmComplexType complex = metadata.findEdmComplexType(UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                        + typeName);
                org.odata4j.edm.EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        complex);
                entityTypeBuilder.addProperties(propertyBuilder);
            }
        }

        // 変更後のメタデータを作成
        this.metadata = builder.build();
    }

    /**
     * プロパティ数、階層制限値をチェックする.
     * @return エラー情報のリスト
     */
    public List<PropertyLimitChecker.CheckError> checkPropertyLimits() {

        List<PropertyLimitChecker.CheckError> result = new ArrayList<PropertyLimitChecker.CheckError>();

        if (null == metadata) {
            return result;
        }

        Iterator<EdmEntityType> iter = metadata.getEntityTypes().iterator();
        while (iter.hasNext()) {
            EdmEntityType target = iter.next();
            checkPropertyLimitsForEntityTypeInternal(result, target);
        }

        // EntityTypeに関連づいていない ComplexType内のプロパティ数の制限チェック
        Iterator<EdmComplexType> complexTypeIter = metadata.getComplexTypes().iterator();
        // 全 ComplexTypeに対してチェックする。(効率的に行うのであれば、先に EntityType側のチェックを行い、未チェックの ComplexTypeのみ実施する方法もあり)
        while (complexTypeIter.hasNext()) {
            int simplePropCount = 0;
            int complexPropCount = 0;
            EdmComplexType complexType = complexTypeIter.next();
            for (EdmProperty prop : complexType.getProperties()) {
                // 予約語プロパティの除外
                if (prop.getName().startsWith("_")) {
                    continue;
                }
                if (prop.getType().isSimple()) {
                    simplePropCount++;
                } else {
                    complexPropCount++;
                }
            }
            if (simpleMaxForOverAllLayers < simplePropCount) {
                String message = String.format(
                        "Total property[%s] count exceeds the limit[%d].", complexType.getName(),
                        simpleMaxForOverAllLayers);
                log.info(message);
                result.add(new PropertyLimitChecker.CheckError(complexType.getName(), message));
            }
            if (complexMaxForOverallLayers < complexPropCount) {
                String message = String.format(
                        "Total property[%s] count exceeds the limit[%d].", complexType.getName(),
                        complexMaxForOverallLayers);
                log.info(message);
                result.add(new PropertyLimitChecker.CheckError(complexType.getName(), message));
            }
        }
        return result;
    }

    /**
     * 指定されたエンティティタイプのプロパティ数をチェックする.
     * @param entityTypeName エンティティタイプ名
     * @return エラー情報のリスト
     */
    public List<PropertyLimitChecker.CheckError> checkPropertyLimits(String entityTypeName) {
        List<PropertyLimitChecker.CheckError> result = new ArrayList<PropertyLimitChecker.CheckError>();
        EdmEntitySet edmEntitySet = this.metadata.findEdmEntitySet(entityTypeName);
        EdmEntityType edmEntityType = edmEntitySet.getType();
        checkPropertyLimitsForEntityTypeInternal(result, edmEntityType);
        return result;
    }

    private void checkPropertyLimitsForEntityTypeInternal(
            List<PropertyLimitChecker.CheckError> result, EdmEntityType target) {
        try {
            // 予約語プロパティは内部で除外される。
            int totalProps = checkPropetyLimitPerLayer(0, target.getName(), target.getProperties());

            // 全体プロパティ数チェック
            if (maxPropertyLimitInEntityType < totalProps) {
                // 制限値を超えた。
                String message = String.format("Total property count exceeds the limit[%d].",
                        maxPropertyLimitInEntityType);
                log.info(message);
                throw new PropertyLimitException(target.getName(), message);
            }
        } catch (PropertyLimitException e) {
            if (e instanceof EntityTypeDepthExceedException) {
                e.setEntityTypeName(target.getName());
            }
            result.add(new PropertyLimitChecker.CheckError(e.getEntityTypeName(), e.getMessage()));
        }
    }

    /**
     * 階層レベルでのプロパティ制限数チェックルーチン.
     * @param depth 階層の深さ（第一階層は0, 第二階層は 1,....)
     * @param entityTypeName EntityType名
     * @param properties EntityType内のプロパティ
     * @return 対象EntityType配下の全プロパティ数
     * @throws PropertyLimitException プロパティ数の制限を超えたことを通知する例外
     */
    private int checkPropetyLimitPerLayer(int depth, String entityTypeName, Enumerable<EdmProperty> properties)
            throws PropertyLimitException {

        log.debug(String.format("Checking [%s]  Depth[%d]", entityTypeName, depth));

        if (maxDepth <= depth) {
            // 実際にはここは通らないはず。
            // ここに来る前に、Property, ComplexTypePropertyの制限値が 0となるチェックが必ず走るため。
            String message = String.format("Hiearchy depth exceeds the limit[%d].", maxDepth);
            log.info(message);
            throw new EntityTypeDepthExceedException(message);
        }

        int simplePropCount = 0;
        int complexPropCount = 0;
        for (EdmProperty prop : properties) {
            // 予約語プロパティの除外
            if (prop.getName().startsWith("_")) {
                continue;
            }
            if (prop.getType().isSimple()) {
                simplePropCount++;
            } else {
                complexPropCount++;
            }
        }

        // 階層毎のプロパティ数制限値のチェック
        int currentSimplePropLimits = simplePropertyLimits[depth];
        if (currentSimplePropLimits < 0) {
            // １階層目の特殊処理。そもそも１階層目の Limitが "*" と指定された場合 -1がsimplePropertyLimits[0]に
            // 代入されている。これをmaxPropertyLimitsInEntityTypeとして認識し、かつ現行の complexPropertyの
            // 数を勘案した制限値を計算しておく。
            currentSimplePropLimits = maxPropertyLimitInEntityType - complexPropCount;
        }

        if (currentSimplePropLimits < simplePropCount) {
            // 制限値を超えた。
            String message = String.format("Property count exceeds the limit[%d].", simplePropertyLimits[depth]);
            log.info(message);
            throw new PropertyLimitException(entityTypeName, message);
        }
        if (complexPropertyLimits[depth] < complexPropCount) {
            // 制限値を超えた。
            String message = String.format("ComplexTypeProperty count exceeds the limit[%d].",
                    complexPropertyLimits[depth]);
            log.info(message);
            throw new PropertyLimitException(entityTypeName, message);
        }

        int totalPropCountOfChildren = 0;
        depth++;
        for (EdmProperty prop : properties) {
            if (!prop.getType().isSimple()) {
                // 子階層へ処理を進める。SimpleTypeの場合は対象外
                String complexTypeName = prop.getType().getFullyQualifiedTypeName();
                EdmComplexType edmComplexType = metadata.findEdmComplexType(complexTypeName);
                if (null != edmComplexType) {
                    log.debug(String.format("  Proceed to child check [%s]  Depth[%d]",
                            edmComplexType.getName(), depth));

                    totalPropCountOfChildren += checkPropetyLimitPerLayer(depth, edmComplexType.getName(),
                            edmComplexType.getProperties());
                }
            }
        }
        // 子階層を含めた総プロパティ数を返す。
        return simplePropCount + complexPropCount + totalPropCountOfChildren;
    }

}
