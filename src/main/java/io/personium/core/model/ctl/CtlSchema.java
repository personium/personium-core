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
 *Schema information of the control entity group.
 */
public final class CtlSchema {
    /**
     *The constructor is private.
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

    /** Pattern id. */
    private static final String PATTERN_ID = "^.{1,400}$";
    /** AnnotationFormat id. */
    private static final List<EdmAnnotation<?>> P_FORMAT_ID = createFormatId();
    /** __id property. */
    public static final EdmProperty.Builder P_ID = EdmProperty.newBuilder("__id")
            .setType(EdmSimpleType.STRING).setDefaultValue(Common.UUID)
            .setNullable(false)
            .setAnnotations(P_FORMAT_ID);

    /**
     * Create id annotation format.
     * @return id annotation format.
     */
    private static List<EdmAnnotation<?>> createFormatId() {
        List<EdmAnnotation<?>> list = new ArrayList<EdmAnnotation<?>>();
        EdmAnnotation<?> annotation = new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('" + PATTERN_ID + "')");
        list.add(annotation);
        return list;
    }

    /**
     *Creates and returns a CSDL extension annotation for a compound Unique Key constraint (CSDL extension by Personium).
     *@ param name UK name
     *@return Annotation list
     */
    public static List<EdmAnnotation<?>> createNamedUkAnnotation(final String name) {
        List<EdmAnnotation<?>> ret = new ArrayList<EdmAnnotation<?>>();
        ret.add(new EdmAnnotationAttribute(Common.P_NAMESPACE.getUri(),
                Common.P_NAMESPACE.getPrefix(),
                "Unique", name));
        return ret;
    }

    /**
     *Creates and returns an annotation of dynamic property or not.
     *@ param name Boolean value (String type)
     *@return Annotation list
     */
    public static List<EdmAnnotation<?>> createIsDecleardAnnotation(final String name) {
        List<EdmAnnotation<?>> ret = new ArrayList<EdmAnnotation<?>>();
        ret.add(new EdmAnnotationAttribute(Common.P_NAMESPACE.getUri(),
                Common.P_NAMESPACE.getPrefix(),
                "IsDeclared", name));
        return ret;
    }

    /**
     *Creates and returns the "Format" annotation.
     *@ param name Format definition
     *@return Annotation list
     */
    public static List<EdmAnnotation<?>> createFormatAnnotation(final String name) {
        List<EdmAnnotation<?>> ret = new ArrayList<EdmAnnotation<?>>();
        ret.add(new EdmAnnotationAttribute(Common.P_NAMESPACE.getUri(),
                Common.P_NAMESPACE.getPrefix(),
                "Format", name));
        return ret;
    }

    /**
     *Returns the EdmDataServices object of the UnitCtl data service.
     * @return EdmDataServices.Builder Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForUnitCtl() {
        //List of Entity Types
        EdmEntityType.Builder[] typeList = new EdmEntityType.Builder[] {Cell.EDM_TYPE_BUILDER };

        //Definition of Association
        EdmAssociation.Builder[] assocs = new EdmAssociation.Builder[] {};

        return createDataServices(Common.EDM_NS_UNIT_CTL, typeList, assocs);
    }

    /**
     *Returns the CellCtl data service's EdmDataServices object.
     * @return EdmDataServices Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForCellCtl() {
        //List of Entity Types
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

        //Definition of Association
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

                // ExtCell : Role = many : many
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
                SentMessage.COMPLEX_TYPE_RESULT,
                RequestObject.COMPLEX_TYPE_REQUEST_OBJECT
        };

        return createDataServices(Common.EDM_NS_CELL_CTL, typeList, assocs, complexList);
    }

    /**
     * Get EdmDataServices for Message.
     * @return EdmDataServicesBuilder Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForMessage() {
        // List of Entity Type
        EdmEntityType.Builder[] typeList = new EdmEntityType.Builder[] {
                ReceivedMessagePort.EDM_TYPE_BUILDER,
                SentMessagePort.EDM_TYPE_BUILDER,
                Role.EDM_TYPE_BUILDER,
                Relation.EDM_TYPE_BUILDER,
                ExtCell.EDM_TYPE_BUILDER,
                Rule.EDM_TYPE_BUILDER
        };
        // List of Association
        EdmAssociation.Builder[] assocs = new EdmAssociation.Builder[] {
                // ExtCell : Role = many : many
                associateManyMany(Common.EDM_NS_CELL_CTL,
                        ExtCell.EDM_TYPE_BUILDER, Role.EDM_TYPE_BUILDER),

                // ExtCell : Relation = many : many
                associateManyMany(Common.EDM_NS_CELL_CTL,
                        ExtCell.EDM_TYPE_BUILDER, Relation.EDM_TYPE_BUILDER)
        };
        // List of ComplexType
        EdmComplexType.Builder[] complexList = new EdmComplexType.Builder[]{
                SentMessage.COMPLEX_TYPE_RESULT,
                RequestObject.COMPLEX_TYPE_REQUEST_OBJECT
        };

        return createDataServices(Common.EDM_NS_CELL_CTL, typeList, assocs, complexList);
    }

    /** Definition of Association.*/
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

    /** List of Entity Types.*/
    private static final EdmEntityType.Builder[] SCHEMA_TYPELIST = new EdmEntityType.Builder[] {
            EntityType.EDM_TYPE_BUILDER,
            AssociationEnd.EDM_TYPE_BUILDER,
            Property.EDM_TYPE_BUILDER,
            ComplexType.EDM_TYPE_BUILDER,
            ComplexTypeProperty.EDM_TYPE_BUILDER
    };

    /**
     *Returns the EdmDataServices object of ODataSvcSchema data service.
     * @return EdmDataServices.Builder Object
     */
    public static EdmDataServices.Builder getEdmDataServicesForODataSvcSchema() {
        return createDataServices(Common.EDM_NS_ODATA_SVC_SCHEMA, SCHEMA_TYPELIST, SCHEMA_ASSOCS);
    }

    /**
     *Returns the EdmDataServices object of the UserCtl data service.
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
        //List of Entity Types
        List<EdmEntityType.Builder> typeList = new ArrayList<EdmEntityType.Builder>();
        Map<String, EdmEntityType.Builder> typeMap = new HashMap<String, EdmEntityType.Builder>();

        //Property is __id, __ published__updated
        List<EdmProperty.Builder> properties =
                Enumerable.create(P_ID,
                        Common.P_PUBLISHED,
                        Common.P_UPDATED)
                        .toList();
        for (OEntity oe : typeEntities) {
            //Get name of EntityType
            List<OProperty<?>> p = oe.getProperties();
            String entityTypeName = "";
            for (OProperty<?> op : p) {
                if (Property.P_NAME.getName().equals(op.getName())) {
                    entityTypeName = (String) op.getValue();
                    continue;
                }
            }
            List<EdmProperty.Builder> propList = getProperties(nodeId, entityTypeName, propEntities, complexEntities);

            //Create EdmEntityType based on created information
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

        //Definition of Association
        List<EdmAssociation.Builder> assocs = new ArrayList<EdmAssociation.Builder>();
        Map<String, OEntity> assocMap = new HashMap<String, OEntity>();
        List<String> assocList = new ArrayList<String>();
        for (OEntity as : assEndEntities) {
            assocMap.put(((OEntityWrapper) as).getUuid(), as);
        }
        for (OEntity as : assEndEntities) {
            //Get information on your AssociationEnd
            OEntityWrapper asWrapper = (OEntityWrapper) as;
            String selfId = asWrapper.getUuid();
            String selfAssociationEndName = (String) as
                    .getProperty(AssociationEnd.P_ASSOCIATION_NAME.getName()).getValue();
            EdmMultiplicity selfMultiplicity = EdmMultiplicity.fromSymbolString((String) as.getProperty(
                    AssociationEnd.P_MULTIPLICITY.getName()).getValue());
            //Acquire from link information
            String selfEntityTypeLinkId = asWrapper.getLinkUuid(EntityType.EDM_TYPE_NAME);
            String associationEndLinkId = asWrapper.getLinkUuid(AssociationEnd.EDM_TYPE_NAME);

            //Asso- cationEnd or Continue if EntityType is not linked
            if (associationEndLinkId == null || selfEntityTypeLinkId == null) {
                continue;
            }

            //If AssociationEnd's link has already been created as a link destination, it is excluded
            if (assocList.contains(associationEndLinkId + selfId)) {
                continue;
            }

            //Acquire the information of AssociationEnd of link destination based on associationEndLinkId
            OEntity targetAssocOentity = assocMap.get(associationEndLinkId);
            if (targetAssocOentity == null) {
                continue;
            }
            String targetAssociationEndName = (String) targetAssocOentity
                    .getProperty(AssociationEnd.P_ASSOCIATION_NAME.getName()).getValue();
            EdmMultiplicity targetMulitplicity = EdmMultiplicity.fromSymbolString((String) targetAssocOentity
                    .getProperty(AssociationEnd.P_MULTIPLICITY.getName()).getValue());
            //Acquire from link information
            Object targetEntityTypeLinkId = ((OEntityWrapper) targetAssocOentity).getLinkUuid(EntityType.EDM_TYPE_NAME);

            //Get yourself and linked EntityTypeBuilder from the EntityType ID of the link
            EdmEntityType.Builder selfEntityType = typeMap.get(selfEntityTypeLinkId);
            EdmEntityType.Builder targetEntityType = typeMap.get(targetEntityTypeLinkId);

            if (selfEntityType == null || targetEntityType == null) {
                continue;
            }
            //Set AssociationEnd
            String selfRoleName = selfEntityType.getName() + ":" + selfAssociationEndName;
            String targetRoleName = targetEntityType.getName() + ":" + targetAssociationEndName;
            assocs.add(associate(nodeId, selfEntityType, targetEntityType, selfRoleName,
                    targetRoleName, selfMultiplicity, targetMulitplicity, null, null));
            assocList.add(selfId + associationEndLinkId);
        }

        //List of ComplexType
        List<EdmComplexType.Builder> complexList = new ArrayList<EdmComplexType.Builder>();
        Map<String, EdmComplexType.Builder> complexMap = new HashMap<String, EdmComplexType.Builder>();
        //Property is __id, __ published__updated
        for (OEntity oe : complexEntities) {
            //Get the name of Property
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

            //Create EdmComplexType based on created information
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
        //EntitySet Map
        Map<String, EdmEntitySet.Builder> setMap = new HashMap<String, EdmEntitySet.Builder>();
        for (EdmEntityType.Builder type : typeList) {
            setMap.put(type.getName(), type2set(type));
        }

        //AssociationSet is defined mechanically
        EdmAssociationSet.Builder[] assocSets = assoc2set(setMap, assocs);

        //EntityContainer is defined mechanically
        EdmEntityContainer.Builder ec = EdmEntityContainer.newBuilder()
                .setName(edmNs).setIsDefault(true)
                .addEntitySets(Enumerable.create(setMap.values()).toList())
                .addAssociationSets(Enumerable.create(assocSets).toList());

        //Schema is defined mechanically
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
                                .setTypeName(type1.getName()) //For some reason I just can not use setType, setTypeName I also have to do.
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
        //When type1 and type2 are the same, that is, when self-referencing, we do not want to register the navigation property with duplex.
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
            //TODO IsKey, UniqueKey is an extension item of PCS and can not be handled by EdmProperty.
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
