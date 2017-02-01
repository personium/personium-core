/**
 * personium.io
 * Modifications copyright 2014 FUJITSU LIMITED
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
 * --------------------------------------------------
 * This code is based on EdmxFormatParser.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
 *  - Add support for IsDeclared property and Format property in parseEdmProperty().
 *  - Add support for OpenType in parseEdmEntityType().
 * --------------------------------------------------
 * The copyright and the license text of the original code is as follows:
 */
/****************************************************************************
 * Copyright (c) 2010 odata4j
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
package io.personium.core.odata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.core4j.Enumerable;
import org.core4j.Func1;
import org.core4j.Predicate1;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OPredicates;
import org.odata4j.core.PrefixedNamespace;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmAssociationSet;
import org.odata4j.edm.EdmAssociationSetEnd;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmFunctionParameter;
import org.odata4j.edm.EdmFunctionParameter.Mode;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmType;
import org.odata4j.format.xml.EdmxFormatParser;
import org.odata4j.stax2.Attribute2;
import org.odata4j.stax2.QName2;
import org.odata4j.stax2.StartElement2;
import org.odata4j.stax2.XMLEvent2;
import org.odata4j.stax2.XMLEventReader2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.CtlSchema;

/**
 * barインストール時に使用するEdmxのXML解析用パーサクラス.
 */
public class PersoniumEdmxFormatParser extends EdmxFormatParser {

    /**
     * ログ用オブジェクト.
     */
    static Logger log = LoggerFactory.getLogger(PersoniumEdmxFormatParser.class);

    private final EdmDataServices.Builder dataServices = EdmDataServices.newBuilder();

    /**
     * コンストラクタ.
     */
    public PersoniumEdmxFormatParser() {
    }

    /**
     * Edmxの解析.
     * @param reader 解析用EdmxのReaderオブジェクト
     * @return EdmDataServices 解析したEdmxのJAXBオブジェクト
     */
    @Override
    public EdmDataServices parseMetadata(XMLEventReader2 reader) {
        List<EdmSchema.Builder> schemas = new ArrayList<EdmSchema.Builder>();
        List<PrefixedNamespace> namespaces = new ArrayList<PrefixedNamespace>();
        namespaces.add(new PrefixedNamespace(Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix()));

        ODataVersion version = null;
        boolean foundDataServices = false;
        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();

            boolean shouldReturn = false;

//          if (isStartElement(event, XmlFormatParser.EDMX_EDMX)) {
//              // should extract the declared namespaces here...
//          }

            if (isStartElement(event, EDMX_DATASERVICES)) {
                foundDataServices = true;
                String str = getAttributeValueIfExists(
                        event.asStartElement(), new QName2(NS_METADATA, "DataServiceVersion"));
                if (str != null) {
                    version = ODataVersion.parse(str);
                } else {
                    version = null;
                }
            }

            if (isStartElement(event, EDM2006_SCHEMA, EDM2007_SCHEMA, EDM2008_SCHEMA, EDM2009_SCHEMA)) {
                schemas.add(parseEdmSchema(reader, event.asStartElement()));
                if (!foundDataServices) { // some dallas services have Schema as the document element!
                    shouldReturn = true;
                }
            }

            if (isEndElement(event, EDMX_DATASERVICES)) {
                shouldReturn = true;
            }

            if (shouldReturn) {
                dataServices.setVersion(version).addSchemas(schemas).addNamespaces(namespaces);
                resolve();
                return dataServices.build();
            }
        }

        throw new UnsupportedOperationException();
    }

    /**
     * resolve.
     */
    private void resolve() {

        final Map<String, EdmEntityType.Builder> allEetsByFQName =
                Enumerable.create(dataServices.getEntityTypes()).toMap(new Func1<EdmEntityType.Builder, String>() {
                    public String apply(EdmEntityType.Builder input) {
                        if (input.getFQAliasName() != null) {
                            return input.getFQAliasName();
                        } else {
                            return input.getFullyQualifiedTypeName();
                        }
                    }
                });
        final Map<String, EdmAssociation.Builder> allEasByFQName =
                Enumerable.create(dataServices.getAssociations()).toMap(new Func1<EdmAssociation.Builder, String>() {
                    public String apply(EdmAssociation.Builder input) {
                        if (input.getFQAliasName() != null) {
                            return input.getFQAliasName();
                        } else {
                            return input.getFQNamespaceName();
                        }
                    }
                });

        for (EdmSchema.Builder edmSchema : dataServices.getSchemas()) {

            // resolve associations
            for (int i = 0; i < edmSchema.getAssociations().size(); i++) {
                EdmAssociation.Builder tmpAssociation = edmSchema.getAssociations().get(i);

                tmpAssociation.getEnd1().setType(allEetsByFQName.get(tmpAssociation.getEnd1().getTypeName()));
                tmpAssociation.getEnd2().setType(allEetsByFQName.get(tmpAssociation.getEnd2().getTypeName()));
            }

            // resolve navproperties
            for (EdmEntityType.Builder eet : edmSchema.getEntityTypes()) {
                List<EdmNavigationProperty.Builder> navProps = eet.getNavigationProperties();
                for (int i = 0; i < navProps.size(); i++) {
                    final EdmNavigationProperty.Builder tmp = navProps.get(i);
                    final EdmAssociation.Builder ea = allEasByFQName.get(tmp.getRelationshipName());

                    List<EdmAssociationEnd.Builder> finalEnds = Enumerable.create(
                            tmp.getFromRoleName(), tmp.getToRoleName())
                                .select(new Func1<String, EdmAssociationEnd.Builder>() {
                                    public EdmAssociationEnd.Builder apply(String input) {
                                        if (ea.getEnd1().getRole().equals(input)) {
                                            return ea.getEnd1();
                                        }
                                        if (ea.getEnd2().getRole().equals(input)) {
                                            return ea.getEnd2();
                                        }
                                        throw new IllegalArgumentException("Invalid role name " + input);
                                    }
                            }).toList();

                    tmp.setRelationship(ea).setFromTo(finalEnds.get(0), finalEnds.get(1));
                }
            }

            // resolve entitysets
            for (EdmEntityContainer.Builder edmEntityContainer : edmSchema.getEntityContainers()) {
                for (int i = 0; i < edmEntityContainer.getEntitySets().size(); i++) {
                    final EdmEntitySet.Builder tmpEes = edmEntityContainer.getEntitySets().get(i);
                    EdmEntityType.Builder eet = allEetsByFQName.get(tmpEes.getEntityTypeName());
                    if (eet == null) {
                        throw new IllegalArgumentException("Invalid entity type " + tmpEes.getEntityTypeName());
                    }
                    edmEntityContainer.getEntitySets().set(
                            i, EdmEntitySet.newBuilder().setName(tmpEes.getName()).setEntityType(eet));
                }
            }

            // resolve associationsets
            for (final EdmEntityContainer.Builder edmEntityContainer : edmSchema.getEntityContainers()) {
                for (int i = 0; i < edmEntityContainer.getAssociationSets().size(); i++) {
                    final EdmAssociationSet.Builder tmpEas = edmEntityContainer.getAssociationSets().get(i);
                    final EdmAssociation.Builder ea = allEasByFQName.get(tmpEas.getAssociationName());

                    List<EdmAssociationSetEnd.Builder> finalEnds = Enumerable.create(tmpEas.getEnd1(), tmpEas.getEnd2())
                            .select(new Func1<EdmAssociationSetEnd.Builder, EdmAssociationSetEnd.Builder>() {
                                public EdmAssociationSetEnd.Builder apply(final EdmAssociationSetEnd.Builder input) {

                                    EdmAssociationEnd.Builder eae = null;
                                    if (ea.getEnd1().getRole().equals(input.getRoleName())) {
                                        eae = ea.getEnd1();
                                    } else  if (ea.getEnd2().getRole().equals(input.getRoleName())) {
                                        eae = ea.getEnd2();
                                    }

                                    if (eae == null) {
                                        throw new IllegalArgumentException("Invalid role name " + input.getRoleName());
                                    }

                                    EdmEntitySet.Builder ees = Enumerable.create(edmEntityContainer.getEntitySets())
                                            .first(OPredicates.nameEquals(
                                                    EdmEntitySet.Builder.class, input.getEntitySetName()));
                                    return EdmAssociationSetEnd.newBuilder().setRole(eae).setEntitySet(ees);
                                }
                            }).toList();

                    tmpEas.setAssociation(ea).setEnds(finalEnds.get(0), finalEnds.get(1));
                }
            }

            // resolve functionimports
            for (final EdmEntityContainer.Builder edmEntityContainer : edmSchema.getEntityContainers()) {
                for (int i = 0; i < edmEntityContainer.getFunctionImports().size(); i++) {
                    final EdmFunctionImport.Builder tmpEfi = edmEntityContainer.getFunctionImports().get(i);
                    EdmEntitySet.Builder ees = Enumerable.create(
                            edmEntityContainer.getEntitySets()).firstOrNull(new Predicate1<EdmEntitySet.Builder>() {
                                public boolean apply(EdmEntitySet.Builder input) {
                                    return input.getName().equals(tmpEfi.getEntitySetName());
                                }
                            });

                    EdmType.Builder<?, ?> typeBuilder = null;
                    if (tmpEfi.getReturnTypeName() != null) {
                        typeBuilder = dataServices.resolveType(tmpEfi.getReturnTypeName());
                        if (typeBuilder == null) {
                            throw new RuntimeException("Edm-type not found: " + tmpEfi.getReturnTypeName());
                        }

                        if (tmpEfi.isCollection()) {
                            typeBuilder = EdmCollectionType.newBuilder()
                                    .setKind(CollectionKind.Collection).setCollectionType(typeBuilder);
                        }
                    }

                    edmEntityContainer.getFunctionImports().set(i,
                            EdmFunctionImport.newBuilder()
                            .setName(tmpEfi.getName())
                            .setEntitySet(ees)
                            .setReturnType(typeBuilder)
                            .setHttpMethod(tmpEfi.getHttpMethod())
                            .addParameters(tmpEfi.getParameters()));
                }
            }

            // resolve type hierarchy
            for (Entry<String, EdmEntityType.Builder> entry : allEetsByFQName.entrySet()) {
                String baseTypeName = entry.getValue().getFQBaseTypeName();
                if (null != baseTypeName) {
                    EdmEntityType.Builder baseType = allEetsByFQName.get(baseTypeName);
                    if (baseType == null) {
                        throw new IllegalArgumentException("Invalid baseType: " + baseTypeName);
                    }
                    entry.getValue().setBaseType(baseType);
                }
            }
        }
    }

    /**
     * parseEdmSchema.
     * @param reader reader
     * @param schemaElement schemaElement
     * @return EdmSchema.Builder
     */
    private EdmSchema.Builder parseEdmSchema(XMLEventReader2 reader, StartElement2 schemaElement) {

        String schemaNamespace = schemaElement.getAttributeByName(new QName2("Namespace")).getValue();
        String schemaAlias = getAttributeValueIfExists(schemaElement, new QName2("Alias"));
        final List<EdmEntityType.Builder> edmEntityTypes = new ArrayList<EdmEntityType.Builder>();
        List<EdmComplexType.Builder> edmComplexTypes = new ArrayList<EdmComplexType.Builder>();
        List<EdmAssociation.Builder> edmAssociations = new ArrayList<EdmAssociation.Builder>();
        List<EdmEntityContainer.Builder> edmEntityContainers = new ArrayList<EdmEntityContainer.Builder>();

        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();

            if (isStartElement(event, EDM2006_ENTITYTYPE, EDM2007_ENTITYTYPE, EDM2008_ENTITYTYPE, EDM2009_ENTITYTYPE)) {
                EdmEntityType.Builder edmEntityType =
                        parseEdmEntityType(reader, schemaNamespace, schemaAlias, event.asStartElement());
                edmEntityTypes.add(edmEntityType);
            }
            if (isStartElement(
                    event, EDM2006_ASSOCIATION, EDM2007_ASSOCIATION, EDM2008_ASSOCIATION, EDM2009_ASSOCIATION)) {
                EdmAssociation.Builder edmAssociation =
                        parseEdmAssociation(reader, schemaNamespace, schemaAlias, event.asStartElement());
                edmAssociations.add(edmAssociation);
            }
            if (isStartElement(
                    event, EDM2006_COMPLEXTYPE, EDM2007_COMPLEXTYPE, EDM2008_COMPLEXTYPE, EDM2009_COMPLEXTYPE)) {
                EdmComplexType.Builder edmComplexType =
                        parseEdmComplexType(reader, schemaNamespace, event.asStartElement());
                edmComplexTypes.add(edmComplexType);
            }
            if (isStartElement(event, EDM2006_ENTITYCONTAINER,
                    EDM2007_ENTITYCONTAINER, EDM2008_ENTITYCONTAINER, EDM2009_ENTITYCONTAINER)) {
                // PMD指摘により、parseEdmEntityContainerの第2引数からschemaNamespaceを削除
               EdmEntityContainer.Builder edmEntityContainer =
                        parseEdmEntityContainer(reader, event.asStartElement());
                edmEntityContainers.add(edmEntityContainer);
            }
            if (isEndElement(event, schemaElement.getName())) {
                return EdmSchema.newBuilder().setNamespace(schemaNamespace).setAlias(schemaAlias)
                        .addEntityTypes(edmEntityTypes)
                        .addComplexTypes(edmComplexTypes)
                        .addAssociations(edmAssociations)
                        .addEntityContainers(edmEntityContainers);
            }
        }

        throw new UnsupportedOperationException();

    }

    /**
     * parseEdmEntityContainer.
     * @param reader reader
     * @param entityContainerElement entityContainerElement
     * @return EdmEntityContainer.Builder
     */
    private EdmEntityContainer.Builder parseEdmEntityContainer(
            XMLEventReader2 reader, StartElement2 entityContainerElement) {
        String name = entityContainerElement.getAttributeByName("Name").getValue();
        boolean isDefault = "true".equals(
                getAttributeValueIfExists(entityContainerElement, new QName2(NS_METADATA, "IsDefaultEntityContainer")));
        String lazyLoadingEnabledValue =
                getAttributeValueIfExists(entityContainerElement, new QName2(NS_EDMANNOTATION, "LazyLoadingEnabled"));
        Boolean lazyLoadingEnabled = null;
        if (lazyLoadingEnabledValue != null) {
            lazyLoadingEnabled = lazyLoadingEnabledValue.equals("true");
        }

        List<EdmEntitySet.Builder> edmEntitySets = new ArrayList<EdmEntitySet.Builder>();
        List<EdmAssociationSet.Builder> edmAssociationSets = new ArrayList<EdmAssociationSet.Builder>();
        List<EdmFunctionImport.Builder> edmFunctionImports = new ArrayList<EdmFunctionImport.Builder>();

        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();

            if (isStartElement(event, EDM2006_ENTITYSET, EDM2007_ENTITYSET, EDM2008_ENTITYSET, EDM2009_ENTITYSET)) {
                edmEntitySets.add(
                        EdmEntitySet.newBuilder().setName(getAttributeValueIfExists(event.asStartElement(), "Name"))
                        .setEntityTypeName(getAttributeValueIfExists(event.asStartElement(), "EntityType")));
            }
            // PMD指摘により、parseEdmAssociationSetの第2引数からschemaNamespaceを削除
            if (isStartElement(event,
                    EDM2006_ASSOCIATIONSET, EDM2007_ASSOCIATIONSET, EDM2008_ASSOCIATIONSET, EDM2009_ASSOCIATIONSET)) {
                edmAssociationSets.add(parseEdmAssociationSet(reader, event.asStartElement()));
            }
            // PMD指摘により、parseEdmFunctionImportの第2引数からschemaNamespaceを削除
            if (isStartElement(event,
                    EDM2006_FUNCTIONIMPORT, EDM2007_FUNCTIONIMPORT, EDM2008_FUNCTIONIMPORT, EDM2009_FUNCTIONIMPORT)) {
                edmFunctionImports.add(parseEdmFunctionImport(reader, event.asStartElement()));
            }

            if (isEndElement(event, entityContainerElement.getName())) {
                return EdmEntityContainer.newBuilder().setName(name)
                        .setIsDefault(isDefault).setLazyLoadingEnabled(lazyLoadingEnabled)
                        .addEntitySets(edmEntitySets)
                        .addAssociationSets(edmAssociationSets).addFunctionImports(edmFunctionImports);
            }
        }
        throw new UnsupportedOperationException();

    }

    /**
     * parseEdmFunctionImport.
     * @param reader reader
     * @param functionImportElement functionImportElement
     * @return EdmFunctionImport.Builder
     */
    private EdmFunctionImport.Builder parseEdmFunctionImport(
            XMLEventReader2 reader, StartElement2 functionImportElement) {
        String name = functionImportElement.getAttributeByName("Name").getValue();
        String entitySet = getAttributeValueIfExists(functionImportElement, "EntitySet");
        Attribute2 returnTypeAttr = functionImportElement.getAttributeByName("ReturnType");
        String returnType = null;
        if (returnTypeAttr != null) {
            returnType = returnTypeAttr.getValue();
        } else {
            returnType = null;
        }

        // strict parsing
        boolean isCollection = null != returnType && returnType.matches("^Collection\\(.*\\)$");
        if (isCollection) {
            final int beginIndex = 11;
            returnType = returnType.substring(beginIndex, returnType.length() - 1);
        }
        String httpMethod = getAttributeValueIfExists(functionImportElement, new QName2(NS_METADATA, "HttpMethod"));

        List<EdmFunctionParameter.Builder> parameters = new ArrayList<EdmFunctionParameter.Builder>();

        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();

            if (isStartElement(event, EDM2006_PARAMETER, EDM2007_PARAMETER, EDM2008_PARAMETER, EDM2009_PARAMETER)) {
                // Mode attribute is optional and thus can be null
                Attribute2 modeAttribute = event.asStartElement().getAttributeByName("Mode");
                Mode mode = null;
                if (modeAttribute != null) {
                    mode = Mode.valueOf(modeAttribute.getValue());
                } else {
                    mode = null;
                }
                parameters.add(EdmFunctionParameter.newBuilder()
                        .setName(event.asStartElement().getAttributeByName("Name").getValue())
                        //.setType(EdmType.get(event.asStartElement().getAttributeByName("Type").getValue()))
                        .setType(EdmType.newDeferredBuilder(
                                event.asStartElement().getAttributeByName("Type").getValue(), dataServices))
                                .setMode(mode));
            }

            if (isEndElement(event, functionImportElement.getName())) {
                return EdmFunctionImport.newBuilder().setName(name).setEntitySetName(entitySet)
                        .setReturnTypeName(returnType).setIsCollection(isCollection).setHttpMethod(httpMethod)
                        .addParameters(parameters);
            }
        }
        throw new UnsupportedOperationException();

    }

    /**
     * parseEdmAssociationSet.
     * @param reader reader
     * @param associationSetElement associationSetElement
     * @return EdmAssociationSet.Builder
     */
    private EdmAssociationSet.Builder parseEdmAssociationSet(
            XMLEventReader2 reader, StartElement2 associationSetElement) {
        String name = associationSetElement.getAttributeByName("Name").getValue();
        String associationName = associationSetElement.getAttributeByName("Association").getValue();

        List<EdmAssociationSetEnd.Builder> ends = new ArrayList<EdmAssociationSetEnd.Builder>();

        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();

            if (isStartElement(event, EDM2006_END, EDM2007_END, EDM2008_END, EDM2009_END)) {
                ends.add(EdmAssociationSetEnd.newBuilder()
                        .setRoleName(event.asStartElement().getAttributeByName("Role").getValue())
                        .setEntitySetName(event.asStartElement().getAttributeByName("EntitySet").getValue()));
            }

            if (isEndElement(event, associationSetElement.getName())) {
                return EdmAssociationSet.newBuilder().setName(name)
                        .setAssociationName(associationName).setEnds(ends.get(0), ends.get(1));
            }
        }
        throw new UnsupportedOperationException();

    }

    /**
     * parseEdmAssociation.
     * @param reader reader
     * @param schemaNamespace schemaNamespace
     * @param schemaAlias schemaAlias
     * @param associationElement associationElement
     * @return EdmAssociation.Builder
     */
    private EdmAssociation.Builder parseEdmAssociation(
            XMLEventReader2 reader, String schemaNamespace, String schemaAlias, StartElement2 associationElement) {
        String name = associationElement.getAttributeByName("Name").getValue();

        List<EdmAssociationEnd.Builder> ends = new ArrayList<EdmAssociationEnd.Builder>();

        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();

            if (isStartElement(event, EDM2006_END, EDM2007_END, EDM2008_END, EDM2009_END)) {
                ends.add(EdmAssociationEnd.newBuilder()
                        .setRole(event.asStartElement().getAttributeByName("Role").getValue())
                        .setTypeName(event.asStartElement().getAttributeByName("Type").getValue())
                        .setMultiplicity(EdmMultiplicity.fromSymbolString(
                                event.asStartElement().getAttributeByName("Multiplicity").getValue())));
            }

            if (isEndElement(event, associationElement.getName())) {
                return EdmAssociation.newBuilder().setNamespace(schemaNamespace).
                        setAlias(schemaAlias).setName(name).setEnds(ends.get(0), ends.get(1));
            }
        }
        throw new UnsupportedOperationException();

    }

    /**
     * parseEdmProperty.
     * @param event event
     * @return EdmProperty.Builder
     */
    private EdmProperty.Builder parseEdmProperty(XMLEvent2 event) {
        String propertyName = getAttributeValueIfExists(event.asStartElement(), "Name");
        String propertyType = getAttributeValueIfExists(event.asStartElement(), "Type");
        String propertyNullable = getAttributeValueIfExists(event.asStartElement(), "Nullable");
        String maxLength = getAttributeValueIfExists(event.asStartElement(), "MaxLength");
        String unicode = getAttributeValueIfExists(event.asStartElement(), "Unicode");
        String fixedLength = getAttributeValueIfExists(event.asStartElement(), "FixedLength");
        String collectionKindS = getAttributeValueIfExists(event.asStartElement(), "CollectionKind");
        CollectionKind ckind = CollectionKind.NONE;
        if (null != collectionKindS) {
            ckind = Enum.valueOf(CollectionKind.class, collectionKindS);
        }
        String defaultValue = getAttributeValueIfExists(event.asStartElement(), "DefaultValue");
        String precision = getAttributeValueIfExists(event.asStartElement(), "Precision");
        String scale = getAttributeValueIfExists(event.asStartElement(), "Scale");

        String isDeclared = getAttributeValueIfExists(
                event.asStartElement(), new QName2(Common.P_NAMESPACE.getUri(), "IsDeclared"));
        String format = getAttributeValueIfExists(
                event.asStartElement(), new QName2(Common.P_NAMESPACE.getUri(), "Format"));

        String storeGeneratedPattern = getAttributeValueIfExists(
                event.asStartElement(), new QName2(NS_EDMANNOTATION, "StoreGeneratedPattern"));

        String fcTargetPath = getAttributeValueIfExists(event.asStartElement(), M_FC_TARGETPATH);
        String fcContentKind = getAttributeValueIfExists(event.asStartElement(), M_FC_CONTENTKIND);
        String fcKeepInContent = getAttributeValueIfExists(event.asStartElement(), M_FC_KEEPINCONTENT);
        String fcEpmContentKind = getAttributeValueIfExists(event.asStartElement(), M_FC_EPMCONTENTKIND);
        String fcEpmKeepInContent = getAttributeValueIfExists(event.asStartElement(), M_FC_EPMKEEPINCONTENT);

        Integer maxLengthVal = null;
        if (maxLength != null) {
            if (maxLength.equals("Max")) {
                maxLengthVal = Integer.MAX_VALUE;
            } else {
                maxLengthVal = Integer.parseInt(maxLength);
            }
        }
        Integer precisionVal = null;
        if (precision != null) {
            precisionVal = Integer.parseInt(precision);
        }
        Integer scaleVal = null;
        if (scale != null) {
            scaleVal = Integer.parseInt(scale);
        }
        Boolean nullableVal = true;
        if (propertyNullable != null) {
            if (!"true".equals(propertyNullable) && !"false".equals(propertyNullable)) {
                String message = "Invalid Nullable value '%s'";
                throw new IllegalArgumentException(String.format(message, propertyNullable));
            }
            nullableVal = Boolean.parseBoolean(propertyNullable);
        }
        EdmProperty.Builder builder = EdmProperty.newBuilder(propertyName)
                .setType(EdmType.newDeferredBuilder(propertyType, dataServices))
                .setNullable(nullableVal)
                .setMaxLength(maxLengthVal)
                .setUnicode("false".equals(unicode))
                .setFixedLength("false".equals(fixedLength))
                .setStoreGeneratedPattern(storeGeneratedPattern)
                .setFcTargetPath(fcTargetPath)
                .setFcContentKind(fcContentKind)
                .setFcKeepInContent(fcKeepInContent)
                .setFcEpmContentKind(fcEpmContentKind)
                .setFcEpmKeepInContent(fcEpmKeepInContent)
                .setCollectionKind(ckind)
                .setDefaultValue(defaultValue)
                .setPrecision(precisionVal)
                .setScale(scaleVal);
        if (isDeclared != null) {
            builder.setAnnotations(CtlSchema.createIsDecleardAnnotation(isDeclared));
        }
        if (format != null) {
            builder.setAnnotations(CtlSchema.createFormatAnnotation(format));
        }
        return builder;
    }

    /**
     * parseEdmComplexType.
     * @param reader reader
     * @param schemaNamespace schemaNamespace
     * @param complexTypeElement complexTypeElement
     * @return EdmComplexType.Builder
     */
    private EdmComplexType.Builder parseEdmComplexType(
            XMLEventReader2 reader, String schemaNamespace, StartElement2 complexTypeElement) {
        String name = complexTypeElement.getAttributeByName("Name").getValue();
        String isAbstractS = getAttributeValueIfExists(complexTypeElement, "Abstract");
        List<EdmProperty.Builder> edmProperties = new ArrayList<EdmProperty.Builder>();

        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();

            if (isStartElement(event, EDM2006_PROPERTY, EDM2007_PROPERTY, EDM2008_PROPERTY, EDM2009_PROPERTY)) {
                edmProperties.add(parseEdmProperty(event));
            }

            if (isEndElement(event, complexTypeElement.getName())) {
                EdmComplexType.Builder complexType = EdmComplexType.newBuilder()
                        .setNamespace(schemaNamespace).setName(name).addProperties(edmProperties);
                if (isAbstractS != null) {
                    complexType.setIsAbstract("true".equals(isAbstractS));
                }
                return complexType;
            }
        }

        throw new UnsupportedOperationException();

    }

    /**
     * parseEdmEntityType.
     * @param reader reader
     * @param schemaNamespace schemaNamespace
     * @param schemaAlias schemaAlias
     * @param entityTypeElement entityTypeElement
     * @return EdmEntityType.Builder
     */
    private EdmEntityType.Builder parseEdmEntityType(
            XMLEventReader2 reader, String schemaNamespace, String schemaAlias, StartElement2 entityTypeElement) {
        String name = entityTypeElement.getAttributeByName("Name").getValue();
        String hasStreamValue = getAttributeValueIfExists(entityTypeElement, new QName2(NS_METADATA, "HasStream"));
        Boolean hasStream = null;
        if (hasStreamValue != null) {
            hasStream = hasStreamValue.equals("true");
        }
        String baseType = getAttributeValueIfExists(entityTypeElement, "BaseType");
        String openType = getAttributeValueIfExists(entityTypeElement, "OpenType");
        String isAbstractS = getAttributeValueIfExists(entityTypeElement, "Abstract");

        List<String> keys = new ArrayList<String>();
        List<EdmProperty.Builder> edmProperties = new ArrayList<EdmProperty.Builder>();
        List<EdmNavigationProperty.Builder> edmNavigationProperties = new ArrayList<EdmNavigationProperty.Builder>();

        while (reader.hasNext()) {
            XMLEvent2 event = reader.nextEvent();
            if (event.isStartElement()) {
                StartElement2 stElem = event.asStartElement();
                log.debug("tagName: " + stElem.getName());
                log.debug("Nullable(API): " + stElem.getAttributeByName("Nullable"));
            }

            if (isStartElement(event, EDM2006_PROPERTYREF,
                    EDM2007_PROPERTYREF, EDM2008_PROPERTYREF, EDM2009_PROPERTYREF)) {
                keys.add(event.asStartElement().getAttributeByName("Name").getValue());
            }

            if (isStartElement(event, EDM2006_PROPERTY, EDM2007_PROPERTY, EDM2008_PROPERTY, EDM2009_PROPERTY)) {
                edmProperties.add(parseEdmProperty(event));
            }

            if (isStartElement(event, EDM2006_NAVIGATIONPROPERTY,
                    EDM2007_NAVIGATIONPROPERTY, EDM2008_NAVIGATIONPROPERTY, EDM2009_NAVIGATIONPROPERTY)) {
                String associationName = event.asStartElement().getAttributeByName("Name").getValue();
                String relationshipName = event.asStartElement().getAttributeByName("Relationship").getValue();
                String fromRoleName = event.asStartElement().getAttributeByName("FromRole").getValue();
                String toRoleName = event.asStartElement().getAttributeByName("ToRole").getValue();

                edmNavigationProperties.add(EdmNavigationProperty.newBuilder(associationName)
                        .setRelationshipName(relationshipName).setFromToName(fromRoleName, toRoleName));

            }

            if (isEndElement(event, entityTypeElement.getName())) {
                Boolean isAbstractVal = null;
                if (isAbstractS != null) {
                    isAbstractVal = "true".equals(isAbstractS);
                }
                EdmEntityType.Builder builder = EdmEntityType.newBuilder()
                        .setNamespace(schemaNamespace)
                        .setAlias(schemaAlias)
                        .setName(name)
                        .setHasStream(hasStream)
                        .addKeys(keys)
                        .addProperties(edmProperties)
                        .addNavigationProperties(edmNavigationProperties)
                        .setBaseType(baseType)
                        .setIsAbstract(isAbstractVal);
                if (openType != null) {
                    builder.setAnnotations(CtlSchema.OPENTYPE);
                }
                return builder;
            }
        }

        throw new UnsupportedOperationException();
    }

}
