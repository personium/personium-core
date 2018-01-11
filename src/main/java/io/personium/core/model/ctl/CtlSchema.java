/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
package io.personium.core.model.ctl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.core4j.Enumerable;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmAssociationSet;
import org.odata4j.edm.EdmAssociationSetEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.producer.edm.Edm;

import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.odata.OEntityWrapper;

/**
 * 制御エンティティ群のスキーマ情報.
 */
public final class CtlSchema {
    /**
     * コンストラクタは非公開.
     */
    private CtlSchema() {
    }

    /**
     * EntityOpenType.
     */
    public static final List<EdmAnnotation<?>> OPENTYPE = new ArrayList<EdmAnnotation<?>>();
    static {
        OPENTYPE.add(new EdmAnnotationAttribute(
                null, null,
                Edm.EntityType.OpenType,
                "true"));
    }

    /**
     * 複合Uniqueキー制約（PersoniumによるCSDL拡張）のためのCSDL拡張アノテーションを作成して返します.
     * @param name UK名
     * @return Annotationのリスト
     */
    public static List<EdmAnnotation<?>> createNamedUkAnnotation(final String name) {
        List<EdmAnnotation<?>> ret = new ArrayList<EdmAnnotation<?>>();
        ret.add(new EdmAnnotationAttribute(Common.P_NAMESPACE.getUri(),
                Common.P_NAMESPACE.getPrefix(),
                "Unique", name));
        return ret;
    }

    /**
     * 動的プロパティか否かのアノテーションを作成して返します.
     * @param name 真偽値(String型)
     * @return Annotationのリスト
     */
    public static List<EdmAnnotation<?>> createIsDecleardAnnotation(final String name) {
        List<EdmAnnotation<?>> ret = new ArrayList<EdmAnnotation<?>>();
        ret.add(new EdmAnnotationAttribute(Common.P_NAMESPACE.getUri(),
                Common.P_NAMESPACE.getPrefix(),
                "IsDeclared", name));
        return ret;
    }

    /**
     * "Format"のアノテーションを作成して返します.
     * @param name フォーマット定義
     * @return Annotationのリスト
     */
    public static List<EdmAnnotation<?>> createFormatAnnotation(final String name) {
        List<EdmAnnotation<?>> ret = new ArrayList<EdmAnnotation<?>>();
        ret.add(new EdmAnnotationAttribute(Common.P_NAMESPACE.getUri(),
                Common.P_NAMESPACE.getPrefix(),
                "Format", name));
        return ret;
    }

    /**
     * CellCtlデータサービスのEdmDataServices オブジェクトを返します.
     * @return EdmDataServices Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForCellCtl() {
        // Entity Type のリスト
        EdmEntityType.Builder[] typeList = new EdmEntityType.Builder[] {
                Role.EDM_TYPE_BUILDER,
                Box.EDM_TYPE_BUILDER,
                Account.EDM_TYPE_BUILDER,
                ExtCell.EDM_TYPE_BUILDER,
                ExtRole.EDM_TYPE_BUILDER,
                Relation.EDM_TYPE_BUILDER,
                ReceivedMessage.EDM_TYPE_BUILDER,
                SentMessage.EDM_TYPE_BUILDER,
                Rule.EDM_TYPE_BUILDER};

        // Associationの定義
        EdmAssociation.Builder[] assocs = new EdmAssociation.Builder[] {
                // Box : Role = 0-1 : many
                associate(Common.EDM_NS_CELL_CTL,
                        Box.EDM_TYPE_BUILDER, Role.EDM_TYPE_BUILDER,
                        EdmMultiplicity.ZERO_TO_ONE, EdmMultiplicity.MANY),

                // Box : Relation = 0-1 : many
                associate(Common.EDM_NS_CELL_CTL,
                        Box.EDM_TYPE_BUILDER, Relation.EDM_TYPE_BUILDER,
                        EdmMultiplicity.ZERO_TO_ONE, EdmMultiplicity.MANY),
                // Account : Role = many : many
                associateManyMany(Common.EDM_NS_CELL_CTL,
                        Account.EDM_TYPE_BUILDER, Role.EDM_TYPE_BUILDER),

                // ExtCell : Relation = many : many
                associateManyMany(Common.EDM_NS_CELL_CTL,
                        ExtCell.EDM_TYPE_BUILDER, Role.EDM_TYPE_BUILDER),
                // ExtCell : Relation = many : many
                associateManyMany(Common.EDM_NS_CELL_CTL,
                        ExtCell.EDM_TYPE_BUILDER, Relation.EDM_TYPE_BUILDER),

                // ExtRole : Role = many : many
                associateManyMany(Common.EDM_NS_CELL_CTL,
                        ExtRole.EDM_TYPE_BUILDER, Role.EDM_TYPE_BUILDER),

                // ExtRole : Relation = many : 1
                associate(Common.EDM_NS_CELL_CTL,
                        ExtRole.EDM_TYPE_BUILDER, Relation.EDM_TYPE_BUILDER,
                        EdmMultiplicity.MANY, EdmMultiplicity.ONE),

                // Relation : Role = many : many
                associateManyMany(Common.EDM_NS_CELL_CTL,
                        Relation.EDM_TYPE_BUILDER, Role.EDM_TYPE_BUILDER),

                // Box : ReceivedMessage = 0-1 : many
                associate(Common.EDM_NS_CELL_CTL,
                        Box.EDM_TYPE_BUILDER, ReceivedMessage.EDM_TYPE_BUILDER,
                        EdmMultiplicity.ZERO_TO_ONE, EdmMultiplicity.MANY),

                // Box : SentMessage = 0-1 : many
                associate(Common.EDM_NS_CELL_CTL,
                        Box.EDM_TYPE_BUILDER, SentMessage.EDM_TYPE_BUILDER,
                        EdmMultiplicity.ZERO_TO_ONE, EdmMultiplicity.MANY),

                // Account : ReceivedMessage = many : many
                associate(Common.EDM_NS_CELL_CTL,
                        Account.EDM_TYPE_BUILDER, ReceivedMessage.EDM_TYPE_BUILDER,
                        null, null,
                        EdmMultiplicity.MANY, EdmMultiplicity.MANY,
                        null, null, ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE),

                // Box : Rule = 0-1 : many
                associate(Common.EDM_NS_CELL_CTL,
                        Box.EDM_TYPE_BUILDER, Rule.EDM_TYPE_BUILDER,
                        EdmMultiplicity.ZERO_TO_ONE, EdmMultiplicity.MANY),

        };
        EdmComplexType.Builder[] complexList = new EdmComplexType.Builder[]{
                SentMessage.COMPLEXTYPE_BUILDER,
                SentMessage.REQUESTRULE_BUILDER,
                ReceivedMessage.REQUESTRULE_BUILDER
        };

        return createDataServices(Common.EDM_NS_CELL_CTL, typeList, assocs, complexList);
    }

    /**
     * UnitCtlデータサービスののEdmDataServices オブジェクトを返します.
     * @return EdmDataServices.Builder Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForUnitCtl() {
        // Entity Type のリスト
        EdmEntityType.Builder[] typeList = new EdmEntityType.Builder[] {Cell.EDM_TYPE_BUILDER };

        // Associationの定義
        EdmAssociation.Builder[] assocs = new EdmAssociation.Builder[] {};
        return createDataServices(Common.EDM_NS_UNIT_CTL, typeList, assocs);
    }

    /** Associationの定義. */
    private static final EdmAssociation.Builder[] SCHEMA_ASSOCS = new EdmAssociation.Builder[] {
            associate(Common.EDM_NS_ODATA_SVC_SCHEMA,
                    EntityType.EDM_TYPE_BUILDER, AssociationEnd.EDM_TYPE_BUILDER,
                    null, null,
                    EdmMultiplicity.ONE, EdmMultiplicity.MANY, null, null),
            associate(Common.EDM_NS_ODATA_SVC_SCHEMA,
                    AssociationEnd.EDM_TYPE_BUILDER, AssociationEnd.EDM_TYPE_BUILDER,
                    null, null,
                    EdmMultiplicity.ZERO_TO_ONE, EdmMultiplicity.ZERO_TO_ONE, null, null),
            associate(Common.EDM_NS_ODATA_SVC_SCHEMA,
                    EntityType.EDM_TYPE_BUILDER, Property.EDM_TYPE_BUILDER,
                    null, null,
                    EdmMultiplicity.ONE, EdmMultiplicity.MANY, null, null),
            associate(Common.EDM_NS_ODATA_SVC_SCHEMA,
                    ComplexType.EDM_TYPE_BUILDER, ComplexTypeProperty.EDM_TYPE_BUILDER,
                    null, null,
                    EdmMultiplicity.ONE, EdmMultiplicity.MANY, null, null)
    };

    /** Entity Type のリスト. */
    private static final EdmEntityType.Builder[] SCHEMA_TYPELIST = new EdmEntityType.Builder[] {
            EntityType.EDM_TYPE_BUILDER,
            AssociationEnd.EDM_TYPE_BUILDER,
            Property.EDM_TYPE_BUILDER,
            ComplexType.EDM_TYPE_BUILDER,
            ComplexTypeProperty.EDM_TYPE_BUILDER
    };

    /**
     * ODataSvcSchemaデータサービスののEdmDataServices オブジェクトを返します.
     * @return EdmDataServices.Builder Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForODataSvcSchema() {
        return createDataServices(Common.EDM_NS_ODATA_SVC_SCHEMA, SCHEMA_TYPELIST, SCHEMA_ASSOCS);
    }

    /**
     * MessageデータのEdmDataServices オブジェクトを返します.
     * @return EdmDataServices Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForMessage() {
        // Entity Type のリスト
        EdmEntityType.Builder[] typeList = new EdmEntityType.Builder[] {
                ReceivedMessagePort.EDM_TYPE_BUILDER,
                SentMessagePort.EDM_TYPE_BUILDER,
                Role.EDM_TYPE_BUILDER,
                Relation.EDM_TYPE_BUILDER,
                ExtCell.EDM_TYPE_BUILDER,
                Rule.EDM_TYPE_BUILDER};
        EdmAssociation.Builder[] assocs = new EdmAssociation.Builder[] {};
        EdmComplexType.Builder[] complexList = new EdmComplexType.Builder[]{
                SentMessage.COMPLEXTYPE_BUILDER,
                SentMessage.REQUESTRULE_BUILDER,
                ReceivedMessage.REQUESTRULE_BUILDER
        };
        return createDataServices(Common.EDM_NS_CELL_CTL, typeList, assocs, complexList);
    }

    /**
     * id プロパティの定義体.
     */
    public static final EdmProperty.Builder P_ID = EdmProperty.newBuilder("__id")
            .setType(EdmSimpleType.STRING).setDefaultValue(Common.UUID)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_ID);

    /**
     * UserCtlデータサービスのEdmDataServices オブジェクトを返します.
     * @param nodeId nodeId
     * @param typeEntities EntityType
     * @param assEndEntities AssociationEnd
     * @param propEntities propEntities
     * @param complexEntities complexEntities
     * @param complexPropEntities complexPropEntities
     * @return EdmDataServices.Builder Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForUserData(String nodeId,
            List<OEntity> typeEntities,
            List<OEntity> assEndEntities,
            List<OEntity> propEntities,
            List<OEntity> complexEntities,
            List<OEntity> complexPropEntities) {
        // Entity Type のリスト
        List<EdmEntityType.Builder> typeList = new ArrayList<EdmEntityType.Builder>();
        Map<String, EdmEntityType.Builder> typeMap = new HashMap<String, EdmEntityType.Builder>();

        // Propertyは__id,__published__updated
        List<EdmProperty.Builder> properties =
                Enumerable.create(P_ID,
                        Common.P_PUBLISHED,
                        Common.P_UPDATED)
                        .toList();
        for (OEntity oe : typeEntities) {
            // EntityTypeの名前を取得する
            List<OProperty<?>> p = oe.getProperties();
            String entityTypeName = "";
            for (OProperty<?> op : p) {
                if (Property.P_NAME.getName().equals(op.getName())) {
                    entityTypeName = (String) op.getValue();
                    continue;
                }
            }
            List<EdmProperty.Builder> propList = getProperties(nodeId, entityTypeName, propEntities, complexEntities);

            // 作成した情報を元にEdmEntityTypeを作成
            EdmEntityType.Builder builder = EdmEntityType.newBuilder()
                    .setNamespace(nodeId)
                    .setName(entityTypeName)
                    .setAnnotations(CtlSchema.OPENTYPE)
                    .addProperties(properties)
                    .addKeys(Common.P_ID.getName())
                    .addProperties(propList);
            typeList.add(builder);
            typeMap.put(((OEntityWrapper) oe).getUuid(), builder);
        }

        // Associationの定義
        List<EdmAssociation.Builder> assocs = new ArrayList<EdmAssociation.Builder>();
        Map<String, OEntity> assocMap = new HashMap<String, OEntity>();
        List<String> assocList = new ArrayList<String>();
        for (OEntity as : assEndEntities) {
            assocMap.put(((OEntityWrapper) as).getUuid(), as);
        }
        for (OEntity as : assEndEntities) {
            // 自分のAssociationEndの情報を取得する
            OEntityWrapper asWrapper = (OEntityWrapper) as;
            String selfId = asWrapper.getUuid();
            String selfAssociationEndName = (String) as
                    .getProperty(AssociationEnd.P_ASSOCIATION_NAME.getName()).getValue();
            EdmMultiplicity selfMultiplicity = EdmMultiplicity.fromSymbolString((String) as.getProperty(
                    AssociationEnd.P_MULTIPLICITY.getName()).getValue());
            // リンク情報から取得する
            String selfEntityTypeLinkId = asWrapper.getLinkUuid(EntityType.EDM_TYPE_NAME);
            String associationEndLinkId = asWrapper.getLinkUuid(AssociationEnd.EDM_TYPE_NAME);

            // AssoicationEndまたは、EntityTypeがリンクされていない場合はコンティニュー
            if (associationEndLinkId == null || selfEntityTypeLinkId == null) {
                continue;
            }

            // 既にリンク先としてAssociationEndのリンクが作成されている場合は対象外とする
            if (assocList.contains(associationEndLinkId + selfId)) {
                continue;
            }

            // リンク先のAssociationEndの情報をassociationEndLinkIdをもとに取得する
            OEntity targetAssocOentity = assocMap.get(associationEndLinkId);
            if (targetAssocOentity == null) {
                continue;
            }
            String targetAssociationEndName = (String) targetAssocOentity
                    .getProperty(AssociationEnd.P_ASSOCIATION_NAME.getName()).getValue();
            EdmMultiplicity targetMulitplicity = EdmMultiplicity.fromSymbolString((String) targetAssocOentity
                    .getProperty(AssociationEnd.P_MULTIPLICITY.getName()).getValue());
            // リンク情報から取得する
            Object targetEntityTypeLinkId = ((OEntityWrapper) targetAssocOentity).getLinkUuid(EntityType.EDM_TYPE_NAME);

            // リンクのEntityTypeのIDから、自分とリンク先のEntityTypeBuilderを取得する
            EdmEntityType.Builder selfEntityType = typeMap.get(selfEntityTypeLinkId);
            EdmEntityType.Builder targetEntityType = typeMap.get(targetEntityTypeLinkId);

            if (selfEntityType == null || targetEntityType == null) {
                continue;
            }
            // AssociationEndを設定する
            String selfRoleName = selfEntityType.getName() + ":" + selfAssociationEndName;
            String targetRoleName = targetEntityType.getName() + ":" + targetAssociationEndName;
            assocs.add(associate(nodeId, selfEntityType, targetEntityType, selfRoleName,
                    targetRoleName, selfMultiplicity, targetMulitplicity, null, null));
            assocList.add(selfId + associationEndLinkId);
        }

        // ComplexType のリスト
        List<EdmComplexType.Builder> complexList = new ArrayList<EdmComplexType.Builder>();
        Map<String, EdmComplexType.Builder> complexMap = new HashMap<String, EdmComplexType.Builder>();
        // Propertyは__id,__published__updated
        for (OEntity oe : complexEntities) {
            // Propertyの名前を取得する
            List<OProperty<?>> p = oe.getProperties();
            String complexName = "";
            for (OProperty<?> op : p) {
                if (ComplexTypeProperty.P_NAME.getName().equals(op.getName())) {
                    complexName = (String) op.getValue();
                    continue;
                }
                if (Common.P_PUBLISHED.getName().equals(op.getName())
                        || Common.P_UPDATED.getName().equals(op.getName())
                        || P_ID.getName().equals(op.getName())) {
                    continue;
                }
            }
            List<EdmProperty.Builder> complexPropNames = getComplexTypeProperties(nodeId, complexName,
                    complexPropEntities, complexEntities);

            // 作成した情報を元にEdmComplexTypeを作成
            EdmComplexType.Builder builder = EdmComplexType.newBuilder().setNamespace(nodeId).setName(complexName)
                    .addProperties(complexPropNames);
            complexList.add(builder);
            complexMap.put(((OEntityWrapper) oe).getUuid(), builder);
        }

        List<EdmAnnotation<?>> ukNameEType = createNamedUkAnnotation("uk_name_etype");
        EdmProperty.Builder propPropertyName = Property.EDM_TYPE_BUILDER.findProperty("Name");
        propPropertyName.setAnnotations(ukNameEType);
        return CtlSchema.createDataServices(
                nodeId,
                typeList.toArray(new EdmEntityType.Builder[0]),
                assocs.toArray(new EdmAssociation.Builder[0]),
                complexList.toArray(new EdmComplexType.Builder[0]));
    }

    private static EdmDataServices.Builder createDataServices(final String edmNs,
            final EdmEntityType.Builder[] typeList,
            final EdmAssociation.Builder[] assocs) {
        return createDataServices(edmNs, typeList, assocs, null);
    }

    private static EdmDataServices.Builder createDataServices(final String edmNs,
            final EdmEntityType.Builder[] typeList,
            final EdmAssociation.Builder[] assocs,
            final EdmComplexType.Builder[] complexList) {
        // EntitySet の Map
        Map<String, EdmEntitySet.Builder> setMap = new HashMap<String, EdmEntitySet.Builder>();
        for (EdmEntityType.Builder type : typeList) {
            setMap.put(type.getName(), type2set(type));
        }

        // AssociationSetは機械的に定義
        EdmAssociationSet.Builder[] assocSets = assoc2set(setMap, assocs);

        // EntityContainerは機械的に定義
        EdmEntityContainer.Builder ec = EdmEntityContainer.newBuilder()
                .setName(edmNs).setIsDefault(true)
                .addEntitySets(Enumerable.create(setMap.values()).toList())
                .addAssociationSets(Enumerable.create(assocSets).toList());

        // Schemaは機械的に定義
        EdmSchema.Builder schema = EdmSchema.newBuilder().addEntityTypes(Enumerable.create(typeList).toList())
                .addAssociations(Enumerable.create(assocs).toList()).setNamespace(edmNs).addEntityContainers(ec);
        if (complexList != null) {
            schema.addComplexTypes(Enumerable.create(complexList).toList());
        }

        EdmDataServices.Builder ret = EdmDataServices.newBuilder()
                .addNamespaces(Enumerable.create(Common.P_NAMESPACE).toList())
                .addSchemas(schema).setVersion(ODataVersion.V2);
        return ret;
    }

    private static EdmEntitySet.Builder type2set(final EdmEntityType.Builder type) {
        return EdmEntitySet.newBuilder().setName(type.getName()).setEntityType(type);
    }

    private static EdmAssociationSetEnd.Builder assocend2assocendset(final Map<String, EdmEntitySet.Builder> setMap,
            final EdmAssociationEnd.Builder end) {
        EdmEntitySet.Builder set = setMap.get(end.getTypeName());
        return EdmAssociationSetEnd.newBuilder().setRole(end).setRoleName(end.getRole()).setEntitySet(set)
                .setEntitySetName(set.getName());
    }

    private static EdmAssociationSet.Builder assoc2set(final Map<String, EdmEntitySet.Builder> setMap,
            final EdmAssociation.Builder assoc) {
        EdmAssociationEnd.Builder e1 = assoc.getEnd1();
        EdmAssociationEnd.Builder e2 = assoc.getEnd2();
        EdmAssociationSet.Builder ret = EdmAssociationSet.newBuilder().setName(assoc.getName()).setAssociation(assoc)
                .setAssociationName(assoc.getName())
                .setEnds(assocend2assocendset(setMap, e1), assocend2assocendset(setMap, e2));
        // System.out.println(ret.getEnd1().getEntitySetName());
        // System.out.println(ret.getEnd1().getRoleName());
        // System.out.println(ret.getAssociationName());
        return ret;
    }

    private static EdmAssociationSet.Builder[] assoc2set(final Map<String, EdmEntitySet.Builder> setMap,
            final EdmAssociation.Builder[] assocs) {
        EdmAssociationSet.Builder[] ret = new EdmAssociationSet.Builder[assocs.length];
        for (int i = 0; i < assocs.length; i++) {
            ret[i] = assoc2set(setMap, assocs[i]);
        }
        return ret;
    }

    static EdmAssociation.Builder associateManyMany(
            final String namespace,
            final EdmEntityType.Builder type1,
            final EdmEntityType.Builder type2) {
        return associate(namespace, type1, type2, EdmMultiplicity.MANY, EdmMultiplicity.MANY);
    }

    static EdmAssociation.Builder associateManyMany(
            final String namespace,
            final EdmEntityType.Builder type1,
            final EdmEntityType.Builder type2,
            final List<EdmAnnotation<?>> type1NavPropAnnotations,
            final List<EdmAnnotation<?>> type2NavPropAnnotations) {
        return associate(namespace, type1, type2, null, null,
                EdmMultiplicity.MANY, EdmMultiplicity.MANY, type1NavPropAnnotations, type2NavPropAnnotations);
    }

    static EdmAssociation.Builder associate(
            final String namespace,
            final EdmEntityType.Builder type1,
            final EdmEntityType.Builder type2,
            final EdmMultiplicity type1Multiplicity,
            final EdmMultiplicity type2Multiplicity) {
        return associate(namespace, type1, type2, null, null, type1Multiplicity, type2Multiplicity, null, null);
    }

    static EdmAssociation.Builder associate(
            final String namespace,
            final EdmEntityType.Builder type1,
            final EdmEntityType.Builder type2,
            final String type1AssociationEndName,
            final String type2AssociationEndName,
            final EdmMultiplicity type1Multiplicity,
            final EdmMultiplicity type2Multiplicity,
            final List<EdmAnnotation<?>> type1NavPropAnnotations,
            final List<EdmAnnotation<?>> type2NavPropAnnotations) {
        return associate(namespace, type1, type2, type1AssociationEndName, type2AssociationEndName, type1Multiplicity,
                type2Multiplicity, null, null, null, null);
    }

    static EdmAssociation.Builder associate(
            final String namespace,
            final EdmEntityType.Builder type1,
            final EdmEntityType.Builder type2,
            final String type1AssociationEndName,
            final String type2AssociationEndName,
            final EdmMultiplicity type1Multiplicity,
            final EdmMultiplicity type2Multiplicity,
            final List<EdmAnnotation<?>> type1NavPropAnnotations,
            final List<EdmAnnotation<?>> type2NavPropAnnotations,
            final String paramAssoc1Name, final String paramAssoc2Name) {
        String type1EndRole = type1AssociationEndName;
        String type2EndRole = type2AssociationEndName;
        if (type1EndRole == null) {
            type1EndRole = type1.getName() + "-" + type2.getName();
        }
        if (type2EndRole == null) {
            type2EndRole = type2.getName() + "-" + type1.getName();
        }
        EdmAssociation.Builder ret = EdmAssociation
                .newBuilder()
                .setEnds(
                        EdmAssociationEnd.newBuilder()
                                .setType(type1)
                                .setTypeName(type1.getName()) // なぜか setTypeをしただけだと使えず、setTypeNameもしないといけない。
                                .setMultiplicity(type1Multiplicity)
                                .setRole(type1EndRole),
                        EdmAssociationEnd.newBuilder()
                                .setType(type2)
                                .setTypeName(type2.getName())
                                .setMultiplicity(type2Multiplicity)
                                .setRole(type2EndRole))
                .setName(type1.getName() + "-" + type2.getName() + "-assoc")
                .setNamespace(namespace);

        // create nav props
        String assoc1Name = paramAssoc1Name;
        String assoc2Name = paramAssoc2Name;
        if (assoc1Name == null) {
            assoc1Name = "_" + type1.getName();
        }
        if (assoc2Name == null) {
            assoc2Name = "_" + type2.getName();
        }
        EdmNavigationProperty.Builder np1 = EdmNavigationProperty.newBuilder(assoc2Name)
                .setFromTo(ret.getEnd1(), ret.getEnd2()).setRelationship(ret);
        if (type1NavPropAnnotations != null) {
            np1 = np1.setAnnotations(type1NavPropAnnotations);
        }
        EdmNavigationProperty.Builder np2 = EdmNavigationProperty.newBuilder(assoc1Name)
                .setFromTo(ret.getEnd2(), ret.getEnd1()).setRelationship(ret);
        if (type2NavPropAnnotations != null) {
            np2 = np2.setAnnotations(type2NavPropAnnotations);
        }
        type1.addNavigationProperties(np1);
        // type1とtype2が同一のとき、すなわち自己参照のときは、２重でナビゲーションプロパティを登録したくない。
        if (type1 != type2) {
            type2.addNavigationProperties(np2);
        }
        return ret;
    }

    private static List<EdmProperty.Builder> getProperties(
            String nodeId,
            String name,
            List<OEntity> propEntities,
            List<OEntity> complexEntities) {

        List<EdmProperty.Builder> list = new ArrayList<EdmProperty.Builder>();
        for (OEntity oe : propEntities) {
            if (!name.equals(oe.getProperty(Property.P_ENTITYTYPE_NAME.getName()).getValue().toString())) {
                continue;
            }
            Object propValue = oe.getProperty(Property.P_NAME.getName().toString()).getValue();
            EdmProperty.Builder property = EdmProperty.newBuilder((String) propValue);
            setTypeProperty(nodeId, Property.P_TYPE.getName().toString(), oe, complexEntities, property);
            propValue = oe.getProperty(Property.P_NULLABLE.getName().toString());
            if (propValue != null) {
                propValue = ((OProperty<?>) propValue).getValue();
                property.setNullable((Boolean) propValue);
            }

            propValue = oe.getProperty(Property.P_IS_DECLARED.getName().toString());
            if (propValue != null) {
                propValue = ((OProperty<?>) propValue).getValue();
                if (!(Boolean) propValue) {
                    List<EdmAnnotation<?>> isDecleard = createIsDecleardAnnotation(propValue.toString());
                    property.setAnnotations(isDecleard);
                }
            }

            propValue = oe.getProperty(Property.P_DEFAULT_VALUE.getName().toString()).getValue();
            property.setDefaultValue((String) propValue);
            propValue = oe.getProperty(Property.P_COLLECTION_KIND.getName().toString()).getValue();
            if (propValue == null || "None".equals(propValue)) {
                property.setCollectionKind(CollectionKind.NONE);
            } else {
                property.setCollectionKind(CollectionKind.valueOf((String) propValue));
            }
            // TODO IsKey、UniqueKeyはPCSの拡張項目のため、EdmPropertyでは扱えない。
            list.add(property);
        }
        return list;
    }

    private static List<EdmProperty.Builder> getComplexTypeProperties(
            String nodeId,
            String name,
            List<OEntity> complexPropEntities,
            List<OEntity> complexEntities) {

        List<EdmProperty.Builder> list = new ArrayList<EdmProperty.Builder>();
        for (OEntity oe : complexPropEntities) {
            if (!name.equals(
                    oe.getProperty(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName()).getValue().toString())) {
                continue;
            }
            Object propValue = oe.getProperty(ComplexTypeProperty.P_NAME.getName().toString()).getValue();
            EdmProperty.Builder property = EdmProperty.newBuilder((String) propValue);
            setTypeProperty(nodeId, ComplexTypeProperty.P_TYPE.getName().toString(), oe, complexEntities, property);
            propValue = oe.getProperty(ComplexTypeProperty.P_NULLABLE.getName().toString());
            if (propValue != null) {
                propValue = ((OProperty<?>) propValue).getValue();
                property.setNullable((Boolean) propValue);
            }
            propValue = oe.getProperty(ComplexTypeProperty.P_DEFAULT_VALUE.getName().toString()).getValue();
            property.setDefaultValue((String) propValue);
            propValue = oe.getProperty(ComplexTypeProperty.P_COLLECTION_KIND.getName().toString()).getValue();
            if (propValue == null || "None".equals(propValue)) {
                property.setCollectionKind(CollectionKind.NONE);
            } else {
                property.setCollectionKind(CollectionKind.valueOf((String) propValue));
            }
            list.add(property);
        }
        return list;
    }

    private static void setTypeProperty(String nodeId, String typeName, OEntity oe,
            List<OEntity> complexEntities,
            EdmProperty.Builder property) {
        String complexTypeName = (String) ((OProperty<?>) oe.getProperty(typeName)).getValue();
        EdmType type = EdmSimpleType.getSimple(complexTypeName);
        if (type == null) {
            if (isExistComplexTypeEntity(complexTypeName, complexEntities)) {
                property.setType(EdmComplexType.newBuilder().setNamespace(nodeId).setName(complexTypeName));
            }
        } else {
            property.setType(type);
        }
    }

    private static boolean isExistComplexTypeEntity(
            String name,
            List<OEntity> complexEntities) {
        for (OEntity oe : complexEntities) {
            if (name.equals(oe.getProperty(ComplexType.P_COMPLEXTYPE_NAME.getName()).getValue().toString())) {
                return true;
            }
        }
        return false;
    }

}
