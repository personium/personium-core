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
import java.util.Arrays;
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

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.doc.ComplexTypePropertyDocHandler;
import io.personium.core.model.impl.es.doc.PropertyDocHandler;

/**
 *A class that implements a method for checking the limit number of Property in EntityType.
 */
public class PropertyLimitChecker {

    static Logger log = LoggerFactory.getLogger(PropertyLimitChecker.class);

    /**
     *Notification object when an error occurs in restriction check.
     */
    public static class CheckError {
        String entityTypeName;
        String message;

        /**
         *constructor.
         *@ param entityTypeName EntityType name
         *@ param message Error Details
         */
        public CheckError(String entityTypeName, String message) {
            this.entityTypeName = entityTypeName;
            this.message = message;
        }

        /**
         *Returns the EntityType name.
         *@return EntityType name
         */
        public String getEntityTypeName() {
            return entityTypeName;
        }

        /**
         *It returns an error message.
         *@ return error message
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     *Internal exception class.
     */
    @SuppressWarnings("serial")
    static class PropertyLimitException extends Exception {

        private String entityTypeName = null;

        /**
         *constructor.
         *@ param message Message
         */
        PropertyLimitException(String message) {
            super(message);
        }

        /**
         *constructor.
         *@ param entityTypeName EntityType name
         *@ param message Message
         */
        PropertyLimitException(String entityTypeName, String message) {
            super(message);
            this.entityTypeName = entityTypeName;
        }

        /**
         *Returns the EntityType name.
         *@return EntityType name
         */
        public String getEntityTypeName() {
            return entityTypeName;
        }

        /**
         *Set the EntityType name.
         *@ param entityTypeName EntityType name
         */
        public void setEntityTypeName(String entityTypeName) {
            this.entityTypeName = entityTypeName;
        }
    }

    /**
     *Exception when the hierarchy depth of EntityType exceeds the limit.
     */
    @SuppressWarnings("serial")
    static class EntityTypeDepthExceedException extends PropertyLimitException {
        /**
         * constructor.
         *@ param message Message
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
        this.maxPropertyLimitInEntityType = PersoniumUnitConfig.getMaxPropertyCountInEntityType();
        this.simplePropertyLimits = PersoniumUnitConfig.getUserdataSimpleTypePropertyLimits();
        this.complexPropertyLimits = PersoniumUnitConfig.getUserdataComplexTypePropertyLimits();
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
     *@ param metadata UserData metadata (before property update)
     *@ param entityTypeName EntityType name to be added
     *@ param dynamicPropCount Number of DynamicProperty added
     */
    public PropertyLimitChecker(final EdmDataServices metadata,
            final String entityTypeName, final int dynamicPropCount) {
        this();
        //Since the metadata received as an argument is the one before updating, it is necessary to create the metadata after the addition.
        Builder builder = EdmDataServices.newBuilder(metadata);

        //Add properties to schema information with dummy for the number of addition targets
        String dummyKey = "dummy" + System.currentTimeMillis();
        for (int i = 0; i < dynamicPropCount; i++) {
            EdmEntityType.Builder entityTypeBuilder = builder
                    .findEdmEntityType(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + entityTypeName);
            EdmProperty.Builder propertyBuilder =
                    EdmProperty.newBuilder(String.format("%s_%03d", dummyKey, i))
                    .setType(EdmSimpleType.getSimple("Edm.String"));
            entityTypeBuilder.addProperties(propertyBuilder);
        }
        //Create changed metadata
        this.metadata = builder.build();
    }

    /**
     * constructor.
     *@ param metadata UserData metadata (before property update)
     *@ param propHandler Handler for properties to add
     */
    public PropertyLimitChecker(final EdmDataServices metadata, PropertyDocHandler propHandler) {
        this();

        //Since the metadata received as an argument is the one before updating, it is necessary to create the metadata after the addition.
        Builder builder = EdmDataServices.newBuilder(metadata);

        //Here, new Property, ComplexTypeProperty etc. are added
        final String dummyPropertyKey = "DUMMY" + System.currentTimeMillis();
        if (propHandler instanceof ComplexTypePropertyDocHandler) {
            //Processing in the case of ComplexTypePropertyDocHandler
            EdmComplexType.Builder complexTypeBuilder = builder
                    .findEdmComplexType(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + propHandler
                    .getEntityTypeName());
            Map<String, Object> staticFields = propHandler.getStaticFields();
            String typeName = (String) staticFields.get("Type");
            EdmType type = EdmSimpleType.getSimple(typeName);
            if (null != type) {
                EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        type);
                complexTypeBuilder.addProperties(propertyBuilder);
            } else {
                EdmComplexType complex = metadata.findEdmComplexType(UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                        + typeName);
                EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        complex);
                complexTypeBuilder.addProperties(propertyBuilder);
            }
        } else {
            EdmEntityType.Builder entityTypeBuilder = builder
                    .findEdmEntityType(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + propHandler
                    .getEntityTypeName());
            Map<String, Object> staticFields = propHandler.getStaticFields();
            String typeName = (String) staticFields.get("Type");
            EdmType type = EdmSimpleType.getSimple(typeName);
            if (null != type) {
                EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        type);
                entityTypeBuilder.addProperties(propertyBuilder);
            } else {
                EdmComplexType complex = metadata.findEdmComplexType(UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                        + typeName);
                EdmProperty.Builder propertyBuilder = EdmProperty.newBuilder(dummyPropertyKey).setType(
                        complex);
                entityTypeBuilder.addProperties(propertyBuilder);
            }
        }

        //Create changed metadata
        this.metadata = builder.build();
    }

    /**
     *Check the number of properties and the hierarchy limit value.
     *@return List of error information
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

        //Check limit on number of properties in ComplexType not related to EntityType
        Iterator<EdmComplexType> complexTypeIter = metadata.getComplexTypes().iterator();
        //Check against all ComplexType. (If it is done efficiently, there is also a method of checking the EntityType side first and implementing only uncompleted ComplexType)
        while (complexTypeIter.hasNext()) {
            int simplePropCount = 0;
            int complexPropCount = 0;
            EdmComplexType complexType = complexTypeIter.next();
            for (EdmProperty prop : complexType.getProperties()) {
                //Exclude reserved word properties
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
     *Check the number of properties for the specified entity type.
     *@ param entityTypeName Entity type name
     *@return List of error information
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
            //Reserved word properties are internally excluded.
            int totalProps = checkPropetyLimitPerLayer(0, target.getName(), target.getProperties());

            //Check the total number of properties
            if (maxPropertyLimitInEntityType < totalProps) {
                //The limit value has been exceeded.
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
     *Property limit check routine at hierarchical level.
     *@ param depth Depth of hierarchy (0 for the first hierarchy, 1 for the second hierarchy, ....)
     *@ param entityTypeName EntityType name
     *@ param properties Property in EntityType
     *@return Total number of properties under EntityType
     *@throws PropertyLimitException exception notifying that the number of properties limit has been exceeded
     */
    private int checkPropetyLimitPerLayer(int depth, String entityTypeName, Enumerable<EdmProperty> properties)
            throws PropertyLimitException {

        log.debug(String.format("Checking [%s]  Depth[%d]", entityTypeName, depth));

        if (maxDepth <= depth) {
            //Actually this should not pass.
            //Before coming here, the check that Property, ComplexTypeProperty's limit value becomes 0 surely runs.
            String message = String.format("Hiearchy depth exceeds the limit[%d].", maxDepth);
            log.info(message);
            throw new EntityTypeDepthExceedException(message);
        }

        int simplePropCount = 0;
        int complexPropCount = 0;
        for (EdmProperty prop : properties) {
            //Exclude reserved word properties
            if (prop.getName().startsWith("_")) {
                continue;
            }
            if (prop.getType().isSimple()) {
                simplePropCount++;
            } else {
                complexPropCount++;
            }
        }

        //Check property limit for each hierarchy
        int currentSimplePropLimits = simplePropertyLimits[depth];
        if (currentSimplePropLimits < 0) {
            //Special processing at the first level. In the first place, when Limit of the first layer is specified as "*" -1 becomes simplePropertyLimits [0]
            //It has been substituted. Recognize this as maxPropertyLimitsInEntityType, and make sure that the current complexProperty
            //Calculate the limit value taking the number into account.
            currentSimplePropLimits = maxPropertyLimitInEntityType - complexPropCount;
        }

        if (currentSimplePropLimits < simplePropCount) {
            //The limit value has been exceeded.
            String message = String.format("Property count exceeds the limit[%d].", simplePropertyLimits[depth]);
            log.info(message);
            throw new PropertyLimitException(entityTypeName, message);
        }
        if (complexPropertyLimits[depth] < complexPropCount) {
            //The limit value has been exceeded.
            String message = String.format("ComplexTypeProperty count exceeds the limit[%d].",
                    complexPropertyLimits[depth]);
            log.info(message);
            throw new PropertyLimitException(entityTypeName, message);
        }

        int totalPropCountOfChildren = 0;
        depth++;
        for (EdmProperty prop : properties) {
            if (!prop.getType().isSimple()) {
                //Advance processing to child hierarchy. Not applicable for SimpleType
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
        //Returns the total number of properties including child hierarchy.
        return simplePropCount + complexPropCount + totalPropCountOfChildren;
    }

}
