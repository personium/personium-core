/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.rs.odata;

import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.odata4j.core.NamedValue;
import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OEntityKey.KeyType;
import org.odata4j.core.OLink;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.format.Entry;
import org.odata4j.format.FormatParser;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.Settings;
import org.odata4j.producer.EntityResponse;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.odata.PropertyLimitChecker;
import io.personium.core.model.impl.es.odata.PropertyLimitChecker.CheckError;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumFormatParserFactory;
import io.personium.core.odata.PersoniumFormatWriterFactory;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.utils.EscapeControlCode;
import io.personium.core.utils.ODataUtils;

/**
 * Abstract class handling OData resources.
 */
public abstract class AbstractODataResource {

    /**
     * Entity set name.
     */
    private String entitySetName;

    /**
     * Dummy key name.
     */
    public static final String DUMMY_KEY = "key_dummy@";

    /**
     * ODataProducer.
     */
    private PersoniumODataProducer odataProducer;

    /** JSON of $ format.*/
    public static final String FORMAT_JSON = "json";
    /** $ format atom.*/
    public static final String FORMAT_ATOM = "atom";

    /** Time when data was stored (set "PersoniumJsonFromatParser class" when "SYSUTCDATETIME ()" is specified as the request body). */
    private long currentTimeMillis = System.currentTimeMillis();

    /**
     * Setter of entitySetName.
     * @param entitySetName Entity set name
     */
    public void setEntitySetName(String entitySetName) {
        this.entitySetName = entitySetName;
    }

    /**
     * Getter of odataProducer.
     * @return odataProducer
     */
    public PersoniumODataProducer getOdataProducer() {
        return this.odataProducer;
    }

    /**
     * Setter for odataProducer.
     * @param odataProducer odataProducer
     */
    public void setOdataProducer(PersoniumODataProducer odataProducer) {
        this.odataProducer = odataProducer;
    }

    /**
     * Getter of entitySetName.
     * @return entity set name
     */
    public String getEntitySetName() {
        return this.entitySetName;
    }

    /**
     * Determine the ContentType to return.
     * @param accept Content of the Accept header;
     *        Note that quality values like 'q=0.9' are ignored to determine.
     * @param format $ format parameter
     * @return Content-Type to return
     */
    public final MediaType decideOutputFormat(final String accept, final String format) {
        MediaType mediaType = null;
        if (format != null) {
            mediaType = decideOutputFormatFromQueryValue(format);
        } else if (!StringUtils.isEmpty(accept)) {
            mediaType = decideOutputFormatFromHeaderValues(accept);
        }
        if (mediaType == null) {
            // set default.
            mediaType = MediaType.APPLICATION_ATOM_XML_TYPE;
        }
        return mediaType;
    }

    /**
     * Determine the output format from the specification ($ format) in the query.
     * @param format Specified value of $ format
     * @return output format ("application / json" or "application / atom + xml")
     */
    private MediaType decideOutputFormatFromQueryValue(String format) {
        MediaType mediaType = null;

        if (format.equals(FORMAT_ATOM)) {
            //When the specification of $ format is atom
            mediaType = MediaType.APPLICATION_ATOM_XML_TYPE;
        } else if (format.equals(FORMAT_JSON)) {
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        } else {
            throw PersoniumCoreException.OData.FORMAT_INVALID_ERROR.params(format);
        }

        return mediaType;
    }

    /**
     * Decide the output format from specification of Accept header.
     * @param acceptHeaderValue Specified value of Accept header
     * @return output format ("application / json" or "application / atom + xml")
     */
    private MediaType decideOutputFormatFromHeaderValues(String acceptHeaderValue) {
        String[] types = Stream.of(acceptHeaderValue.split("[ \t]*,[ \t]*"))
                .map(this::truncateAfterSemicolon)
                .toArray(String[]::new);
        if (Stream.of(types).anyMatch(this::isAcceptXml)) {
            return MediaType.APPLICATION_ATOM_XML_TYPE;
        } else if (Stream.of(types).anyMatch(this::isAcceptJson)) {
            return MediaType.APPLICATION_JSON_TYPE;
        } else if (Stream.of(types).anyMatch(this::isAcceptWildcard)) {
            return MediaType.APPLICATION_ATOM_XML_TYPE;
        } else {
            throw PersoniumCoreException.OData.UNSUPPORTED_MEDIA_TYPE.params(acceptHeaderValue);
        }
    }

    /**
     * Truncate the semicolon after the input character string.
     * @param source Accept A character string obtained by dividing the specified value in the header with a comma
     * @return String up to semicolon
     */
    private String truncateAfterSemicolon(String source) {
        String[] splited = source.split("[ \t]*;", 2);
        return splited[0];
    }

    private boolean isAcceptXml(String accept) {
        return accept.equals(MediaType.APPLICATION_ATOM_XML)
                || accept.equals(MediaType.APPLICATION_XML);
    }

    private boolean isAcceptJson(String accept) {
        return accept.equals(MediaType.APPLICATION_JSON);
    }

    private boolean isAcceptWildcard(String accept) {
        return accept.equals(MediaType.WILDCARD);
    }

    /**
     * Ask Producer to create Entity.
     * @param reader request body
     * @param odataResource OData resource
     * @return EntityResponse
     */
    protected EntityResponse createEntity(final Reader reader, ODataCtlResource odataResource) {
        OEntityWrapper oew = getOEntityWrapper(reader, odataResource, null);

        //Ask Producer to create an Entity. In addition to this, we also ask for existence confirmation.
        EntityResponse res = getOdataProducer().createEntity(getEntitySetName(), oew);
        return res;
    }

    /**
     * Get the OEntityWrapper from the request body.
     * @param reader request body
     * @param odataResource OData resource
     * @param metadata schema definition
     * @return OEntityWrapper
     */
    public OEntityWrapper getOEntityWrapper(final Reader reader,
            ODataCtlResource odataResource,
            EdmDataServices metadata) {
        //Create OEntity to register
        OEntity newEnt;
        if (metadata == null) {
            newEnt = createRequestEntity(reader, null);
        } else {
            newEnt = createRequestEntity(reader, null, metadata);
        }

        //Wrapped in a trumpet. Since POST never receives ETags such as If-Match Etag is null.
        String uuid = PersoniumUUID.randomUUID();
        OEntityWrapper oew = new OEntityWrapper(uuid, newEnt, null);
        //Process of attaching meta information if necessary
        odataResource.beforeCreate(oew);

        return oew;
    }

    /**
     * Create an OEntity object from the input body.
     * Since this method can not apply the locking process, it does not check the existence of data.
     * @param reader request body
     * @param oEntityKey The entityKey to update. Specify null when creating a new one
     * @return OData entity
     */
    protected OEntity createRequestEntity(final Reader reader, OEntityKey oEntityKey) {
        //Retrieving Schema Information
        EdmDataServices metadata = this.odataProducer.getMetadata();
        return createRequestEntity(reader, oEntityKey, metadata);
    }

    /**
     * Create an OEntity object from the input body.
     * Since this method can not apply the locking process, it does not check the existence of data.
     * @param reader request body
     * @param oEntityKey The entityKey to update. Specify null when creating a new one
     * @param metadata EdmDataServices schema definition
     * @return OData entity
     */
    protected OEntity createRequestEntity(final Reader reader, OEntityKey oEntityKey, EdmDataServices metadata) {
        return createRequestEntity(
                reader,
                oEntityKey,
                metadata,
                this.entitySetName);
    }

    /**
     * Create an OEntity object from the input body.
     * Since this method can not apply the locking process, it does not check the existence of data.
     * @param reader request body
     * @param oEntityKey The entityKey to update. Specify null when creating a new one
     * @param metadata EdmDataServices schema definition
     * @param entitySetNameParam EntitySet name
     * @return OData entity
     */
    protected OEntity createRequestEntity(final Reader reader,
            OEntityKey oEntityKey,
            EdmDataServices metadata,
            String entitySetNameParam) {
        //Retrieving Schema Information
        EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(entitySetNameParam);
        EdmEntityType edmEntityType = edmEntitySet.getType();
        //Retrieve the key list defined in the schema
        List<String> keysDefined = edmEntityType.getKeys();

        //Create a temporary Ontity object for the time being from the request.
        //There is a possibility that this OEntity does not contain required items.
        OEntity reqEntity = createOEntityFromRequest(keysDefined, metadata, reader, entitySetNameParam);
        List<OLink> links = reqEntity.getLinks();

        //TODO Static schema check and default value setting.
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        List<String> schemaProps = new ArrayList<String>();

        //Primary key schema check
        if (oEntityKey != null) {
            validatePrimaryKey(oEntityKey, edmEntityType);
        }

        for (EdmProperty ep : edmEntityType.getProperties()) {
            String propName = ep.getName();
            schemaProps.add(propName);
            OProperty<?> op = null;
            try {
                //Acquire corresponding property from request OEntity
                op = reqEntity.getProperty(propName);
                //When __published, __ updated is specified in the request body 400 Return 400 error
                if (op != null && (propName.equals(Common.P_PUBLISHED.getName())
                        || propName.equals(Common.P_UPDATED.getName()))) {
                    throw PersoniumCoreException.OData.FIELED_INVALID_ERROR
                            .params(propName + " is management information name. Cannot request.");
                }

                if (ep.getType().isSimple()) {
                    //In case of simple type
                    op = getSimpleProperty(ep, propName, op);
                } else {
                    //In case of Complex type
                    op = getComplexProperty(ep, propName, op, metadata);
                }
            } catch (PersoniumCoreException e) {
                throw e;
            } catch (Exception e) {
                op = setDefaultValue(ep, propName, op, metadata);
            }

            //Since there was an input, it goes to the value check processing.
            if (op != null && op.getValue() != null) {
                if (ep.getType().isSimple()) {
                    validateProperty(ep, propName, op);
                } else {
                    validateProperty(ep, propName, op, metadata);
                }
            }

            if (op != null) {
                props.add(op);
            }
        }
        //Set the value of DynamicProperty
        int dynamicPropCount = 0;
        for (OProperty<?> property : reqEntity.getProperties()) {

            String req = property.getName();
            if (req.equals("__metadata")) {
                //When __metadata is specified in the request body 400 Return 400 error
                throw PersoniumCoreException.OData.FIELED_INVALID_ERROR.params(req
                        + " is management information name. Cannot request.");
            }
            //When a dynamicProperty that does not exist in the EntityType appears, it is counted in the number of properties in order to additionally register the schema.
            //If registered, it is treated as declaredProperty, so do not count the number of properties.
            if (!schemaProps.contains(req)) {
                //Count the number of elements in dynamicProperty
                dynamicPropCount++;
                validateDynamicProperty(property);
                props.add(property);
            } else {
                //If the defined dynamic property value specified in the request body is null, it is stored as null in the ES, so it is not omitted
                //(If omitted in the request body, omitting it as property information because it is unnecessary to store in ES)
                //However, if the property value is not null, it will be double registered, so exclude it
                if (property.getValue() == null && isRegisteredDynamicProperty(edmEntityType, req)) {
                    validateDynamicProperty(property);
                    props.add(property);
                }
            }
        }
        //Check processing for each resource
        this.collectProperties(props);
        this.validate(props);

        //Check number of elements of property
        if (dynamicPropCount > 0) {
            PropertyLimitChecker checker = new PropertyLimitChecker(metadata, entitySetNameParam, dynamicPropCount);
            List<CheckError> errors = checker.checkPropertyLimits(entitySetNameParam);
            if (errors.size() > 0) {
                throw PersoniumCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED;
            }
        }

        //Because entity is Immutable, recreate Entity object to be created
        //This OEntity is guaranteed to conform to the schema, but the key does not exist.
        OEntity newEnt = OEntities.createRequest(edmEntitySet, props, links);

        //Set key
        OEntityKey key = null;
        //Distribute processing depending on whether it is a compound key or not
        if (keysDefined.size() == 1) {
            // single-unnamed value
            String keyPropName = keysDefined.get(0);
            OProperty<?> keyProp = newEnt.getProperty(keyPropName);
            Object value = keyProp.getValue();
            if (value == null) {
                //400 error if single primary key is null
                throw PersoniumCoreException.OData.NULL_SINGLE_KEY;
            }
            key = OEntityKey.create(keyProp.getValue());
        } else {
            //Implementation of multiple-named value
            Map<String, Object> keyMap = new HashMap<String, Object>();
            for (String keyPropName : keysDefined) {
                OProperty<?> keyProp = newEnt.getProperty(keyPropName);
                Object value = keyProp.getValue();
                if (value == null) {
                    //For compound keys, allow null values
                    //If the key value is null, creation of OEntityKey fails, so set a dummy key
                    value = DUMMY_KEY;
                    props.remove(keyProp);
                    props.add(OProperties.string(keyPropName, (String) null));
                }
                keyMap.put(keyPropName, value);
            }
            key = OEntityKey.create(keyMap);
        }
        editProperty(props, key.toKeyString());
        //Create OEntity to register
        //This OEntity is guaranteed to conform to the schema, and the key is properly set.
        newEnt = OEntities.create(reqEntity.getEntitySet(), key, props, links,
                key.toKeyStringWithoutParentheses(), reqEntity.getEntitySet().getName());
        return newEnt;

    }

    /**
     * It checks whether the property specified by the argument is defined by DynamicProperty.
     * @param edmEntityType edmEntityType
     * @param propertyName property name
     * @return true:DynamicProperty, false:DeclaredProperty
     */
    protected boolean isRegisteredDynamicProperty(EdmEntityType edmEntityType, String propertyName) {
        boolean isRegisteredDynamicProperty = false;
        EdmProperty prop = edmEntityType.findDeclaredProperty(propertyName);
        NamespacedAnnotation<?> isDeclared = prop.findAnnotation(Common.P_NAMESPACE.getUri(),
                Property.P_IS_DECLARED.getName());
        //Except for Property / ComplexTypeProperty, IsDeclared is not defined and is excluded.
        if (isDeclared != null && isDeclared.getValue().equals("false")) {
            isRegisteredDynamicProperty = true;
        }
        return isRegisteredDynamicProperty;
    }

    /**
     * Primary Key Validate.
     * @param oEntityKey Requested Key information
     * @param edmEntityType Schema information for EntityType
     */
    protected void validatePrimaryKey(OEntityKey oEntityKey, EdmEntityType edmEntityType) {
        for (String key : edmEntityType.getKeys()) {
            EdmType keyEdmType = edmEntityType.findProperty(key).getType();
            if (OEntityKey.KeyType.SINGLE.equals(oEntityKey.getKeyType())) {
                //For a single primary key
                if (!(oEntityKey.asSingleValue().getClass().equals(
                        EdmSimpleType.getSimple(keyEdmType.getFullyQualifiedTypeName()).getCanonicalJavaType()))) {
                    throw PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR;
                }
            } else {
                //In case of composite primary key
                Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
                for (NamedValue<?> nv : nvSet) {
                    if (nv.getName().equals(key) && !(nv.getValue().getClass().equals(
                            EdmSimpleType.getSimple(keyEdmType.getFullyQualifiedTypeName())
                                    .getCanonicalJavaType()))) {
                        throw PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR;
                    }
                }
            }
        }
    }

    /**
     * Property operation.
     * @param props property list
     * @param value Value of key
     */
    protected void editProperty(List<OProperty<?>> props, String value) {
    }

    /**
     * Get a simple property with default value set.
     * @param ep EdmProperty
     * @param propName property name
     * @param op OProperty
     * @return Simple property with default value set
     */
    protected OProperty<?> getSimpleProperty(EdmProperty ep, String propName, OProperty<?> op) {
        //If the value is not set, the default value is set
        if (op == null || op.getValue() == null) {
            op = setDefaultValue(ep, propName, op);
        }
        return op;
    }

    /**
     * Get the Complex property with the default value set.
     * @param ep EdmProperty
     * @param propName property name
     * @param op OProperty
     * @param metadata schema information
     * @return Complex property with default value set
     */
    @SuppressWarnings("unchecked")
    protected OProperty<?> getComplexProperty(EdmProperty ep, String propName, OProperty<?> op,
            EdmDataServices metadata) {
        //Get schema information of ComplexType
        OProperty<?> newProp;
        EdmComplexType edmComplexType =
                metadata.findEdmComplexType(ep.getType().getFullyQualifiedTypeName());

        //Count the number of elements in dynamicProperty
        if (op == null || op.getValue() == null) {
            newProp = setDefaultValue(ep, propName, op, metadata);
        } else {
            if (ep.getCollectionKind().equals(CollectionKind.List)) {
                //If ComplexType is an array element, create an OCollectionBuilder
                EdmCollectionType collectionType = new EdmCollectionType(CollectionKind.List, ep.getType());
                OCollection.Builder<OObject> builder = OCollections.<OObject>newBuilder(collectionType
                        .getItemType());

                if (op.getValue() instanceof OCollection<?>) {
                    //Add ComplexType property to array
                    for (OComplexObject val : (OCollection<OComplexObject>) op.getValue()) {
                        //Obtain OProperty list of ComplexType
                        List<OProperty<?>> newComplexProperties = getComplexPropertyList(ep, propName,
                                val.getProperties(), metadata);
                        builder.add(OComplexObjects.create(edmComplexType, newComplexProperties));
                    }

                    //Set array element of ComplexType as OCollection property
                    newProp = OProperties.collection(ep.getName(), collectionType, builder.build());
                } else {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
                }
            } else {
                //If ComplexType is not an array, set it as ComplexType property
                List<OProperty<?>> newComplexProperties = getComplexPropertyList(ep, propName,
                        (List<OProperty<?>>) op.getValue(), metadata);
                newProp = OProperties.complex(propName, edmComplexType, newComplexProperties);
            }
        }
        return newProp;
    }

    /**
     * Get the Complex property list with the default value set.
     * @param ep EdmProperty
     * @param propName property name
     * @param opList OProperty list
     * @param metadata schema information
     * @return Simple property with default value set
     */
    protected List<OProperty<?>> getComplexPropertyList(EdmProperty ep, String propName, List<OProperty<?>> opList,
            EdmDataServices metadata) {
        //Get schema information of ComplexType
        EdmComplexType edmComplexType =
                metadata.findEdmComplexType(ep.getType().getFullyQualifiedTypeName());
        Map<String, OProperty<?>> complexProperties = new HashMap<String, OProperty<?>>();

        //Since it loops on the basis of the schema information to confirm the missing items, it converts OProperty list to Hash format
        for (OProperty<?> cp : opList) {
            complexProperties.put(cp.getName(), cp);
        }

        List<OProperty<?>> newComplexProperties = createNewComplexProperties(metadata, edmComplexType,
                complexProperties);

        //Return a list of ComplexType properties with default values ​​set
        return newComplexProperties;
    }

    /**
     * Refer to the ComplexType schema and set mandatory checks and default values.
     * @param metadata schema information
     * @param edmComplexType Schema information of ComplexType
     * @param complexProperties List of ComplexTypeProperty
     * @return List of ComplexType properties with default values
     */
    protected List<OProperty<?>> createNewComplexProperties(EdmDataServices metadata,
            EdmComplexType edmComplexType,
            Map<String, OProperty<?>> complexProperties) {
        List<OProperty<?>> newComplexProperties = new ArrayList<OProperty<?>>();
        for (EdmProperty ctp : edmComplexType.getProperties()) {
            //Acquire property information
            String compPropName = ctp.getName();
            OProperty<?> complexProperty = complexProperties.get(compPropName);
            if (ctp.getType().isSimple()) {
                //In case of simple type
                complexProperty = getSimpleProperty(ctp, compPropName, complexProperty);
            } else {
                //In case of Complex type
                complexProperty = getComplexProperty(ctp, compPropName, complexProperty, metadata);
            }
            if (complexProperty != null) {
                newComplexProperties.add(complexProperty);
            }
        }
        return newComplexProperties;
    }

    /**
     * Set default value.
     * @param ep EdmProperty
     * @param propName property name
     * @param op OProperty
     * @return Oproperty
     */
    protected OProperty<?> setDefaultValue(EdmProperty ep, String propName, OProperty<?> op) {
        return setDefaultValue(ep, propName, op, null);
    }

    /**
     * Set default value.
     * @param ep EdmProperty
     * @param propName property name
     * @param op OProperty
     * @param metadata EdmDataServices schema definition
     * @return Oproperty
     */
    protected OProperty<?> setDefaultValue(EdmProperty ep, String propName, OProperty<?> op, EdmDataServices metadata) {
        //Property that does not have input but is defined in the schema
        //If default values ​​are defined, add them.
        //If it is an item of ComplexType itself or an item of an array, a default value is not set
        NamespacedAnnotation<?> annotation = ep.findAnnotation(Common.P_NAMESPACE.getUri(),
                Property.P_IS_DECLARED.getName());
        if (annotation != null && !(Boolean.valueOf(annotation.getValue().toString()))) {
            return null;
        }
        if (ep.getType().isSimple() && !ep.getCollectionKind().equals(CollectionKind.List)
                && ep.getDefaultValue() != null) {
            op = generateDefautlProperty(ep);
        } else if (ep.isNullable()) {
            //If nullable is true. Property with null
            //TODO Is this OK?
            op = OProperties.null_(propName, ep.getType().getFullyQualifiedTypeName());
        } else {
            //If nullable is false. Make an error
            throw PersoniumCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(propName);
        }
        return op;
    }

    /**
     * Check the value of the property item.
     * @param ep EdmProperty
     * @param propName property name
     * @param op OProperty
     */
    protected void validateProperty(EdmProperty ep, String propName, OProperty<?> op) {
        for (NamespacedAnnotation<?> annotation : ep.getAnnotations()) {
            if (annotation.getName().equals(Common.P_FORMAT)) {
                String pFormat = annotation.getValue().toString();
                //In case of regular expression check
                if (pFormat.startsWith(Common.P_FORMAT_PATTERN_REGEX)) {
                    validatePropertyRegEx(propName, op, pFormat);
                } else if (pFormat.equals(Common.P_FORMAT_PATTERN_URI)) {
                    validatePropertyUri(propName, op);
                } else if (pFormat.startsWith(Box.P_FORMAT_PATTERN_SCHEMA_URI)) {
                    validatePropertySchemaUri(propName, op);
                } else if (pFormat.startsWith(Common.P_FORMAT_PATTERN_CELL_URL)) {
                    validatePropertyCellUrl(propName, op);
                }
            }
        }
    }

    /**
     * Check the value of the property item.
     * @param ep EdmProperty
     * @param propName property name
     * @param op OProperty
     * @param metadata schema information
     */
    @SuppressWarnings("unchecked")
    protected void validateProperty(EdmProperty ep, String propName, OProperty<?> op, EdmDataServices metadata) {
        EdmComplexType edmComplexType =
                metadata.findEdmComplexType(ep.getType().getFullyQualifiedTypeName());
        if (!ep.getCollectionKind().equals(CollectionKind.List)) {
            List<OProperty<?>> list = (List<OProperty<?>>) op.getValue();
            Map<String, OProperty<?>> complexProperties = new HashMap<>();
            for (OProperty<?> cp : list) {
                complexProperties.put(cp.getName(), cp);
            }
            for (EdmProperty ctp : edmComplexType.getProperties()) {
                String compPropName = ctp.getName();
                OProperty<?> complexProperty = complexProperties.get(compPropName);
                if (complexProperty == null || complexProperty.getValue() == null) {
                    continue;
                }
                if (ctp.getType().isSimple()) {
                    validateProperty(ctp, compPropName, complexProperty);
                } else {
                    validateProperty(ctp, compPropName, complexProperty, metadata);
                }
            }
        }
        // TODO CollectionKind
    }

    /**
     * Check processing other than p: Format.
     * @param props property list
     * @param
     */
    public void validate(List<OProperty<?>> props) {
    }

    /**
     * Get property list.
     * @param props property list
     * @param
     */
    public void collectProperties(List<OProperty<?>> props) {
    }

    /**
     * Check the value of the dynamic property item.
     * @param property OProperty
     */
    private void validateDynamicProperty(OProperty<?> property) {
        //Check key
        String key = property.getName();
        Pattern pattern = Pattern.compile(Common.PATTERN_USERDATA_KEY);
        Matcher matcher = pattern.matcher(key);
        if (!matcher.matches()) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(key);
        }

        //Since it is a dynamic property, null is allowed
        if (property.getValue() == null) {
            return;
        }

        //Check for value of type String
        EdmType type = property.getType();
        if (EdmSimpleType.STRING.equals(type)) {
            String value = property.getValue().toString();
            if (!ODataUtils.validateString(value)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(key);
            }
        }

        //Double value range check
        if (EdmSimpleType.DOUBLE.equals(type)) {
            double value = (Double) property.getValue();
            if (!ODataUtils.validateDouble(value)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(key);
            }
        }
    }

    /**
     * Check the value of property item with regular expression.
     * @param propName property name
     * @param op OProperty
     * @param pFormat pFormat value
     */
    protected void validatePropertyRegEx(String propName, OProperty<?> op, String pFormat) {
        // Extract regular expressions from('regular expression')
        Pattern formatPattern = Pattern.compile(Common.P_FORMAT_PATTERN_REGEX + "\\('(.+)'\\)");
        Matcher formatMatcher = formatPattern.matcher(pFormat);
        formatMatcher.matches();
        pFormat = formatMatcher.group(1);

        if (!ODataUtils.validateRegEx(op.getValue().toString(), pFormat)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * Check whether the value of the property item is a URI.
     * @param propName property name
     * @param op OProperty
     */
    protected void validatePropertyUri(String propName, OProperty<?> op) {
        if (!ODataUtils.isValidUri(op.getValue().toString())) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * Schema URI Format Check.
     * @param propName Property name
     * @param op OProperty
     */
    protected void validatePropertySchemaUri(String propName, OProperty<?> op) {
        if (!ODataUtils.isValidSchemaUri(op.getValue().toString())) {
            throw PersoniumCoreException.OData.SCHEMA_URI_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * Cell URL Format Check.
     * @param propName Property name
     * @param op OProperty
     */
    protected void validatePropertyCellUrl(String propName, OProperty<?> op) {
        if (!ODataUtils.isValidCellUrl(op.getValue().toString())) {
            throw PersoniumCoreException.OData.CELL_URL_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * Perform normalization of OEntityKey.
     * When toKeyString OEntityKey after normalization, if it is the same key, it becomes the same character string.
     * @param oEntityKey original OEntityKey
     * @param edmEntitySet EdmEntitySet
     * @return OEntityKey Normalized OEntityKey
     */
    public static OEntityKey normalizeOEntityKey(OEntityKey oEntityKey, EdmEntitySet edmEntitySet) {
        EdmEntityType edmEntityType = edmEntitySet.getType();
        //Retrieve the key list defined in the schema
        List<String> keysDefined = edmEntityType.getKeys();

        //Set key
        OEntityKey key = null;
        //Distribute processing depending on whether it is a compound key or not
        if (keysDefined.size() == 1) {
            key = oEntityKey;
        } else {
            Map<String, Object> keyMap = new HashMap<String, Object>();
            if (OEntityKey.KeyType.COMPLEX == oEntityKey.getKeyType()) {
                //When the input key is also compound
                Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
                //Implementation of multiple-named value
                for (String keyName : keysDefined) {
                    for (NamedValue<?> nv : nvSet) {
                        if (nv.getName().equals(keyName)) {
                            //Process of filling the corresponding key in Map
                            Object value = nv.getValue();
                            if (value == null) {
                                //For compound keys, allow null values
                                //If the key value is null, creation of OEntityKey fails, so set a dummy key
                                value = DUMMY_KEY;
                            }
                            keyMap.put(keyName, value);
                        }
                    }
                }
            } else {
                //When the input key is single
                Object keyValue = oEntityKey.asSingleValue();
                for (String keyName : keysDefined) {
                    EdmProperty eProp = edmEntityType.findProperty(keyName);
                    Object value = null;
                    if (eProp.isNullable() && eProp.getDefaultValue() == null) {
                        //Nullable item should be null
                        //If the key value is null, creation of OEntityKey fails, so set a dummy key
                        value = DUMMY_KEY;
                    } else {
                        value = keyValue;
                    }
                    keyMap.put(keyName, value);
                }
            }
            //Create OEntityKey from prepared Map
            key = OEntityKey.create(keyMap);
        }
        return key;
    }

    /**
     * Generate OEntity from the request body Reader.
     * @param keyPropNames List of names of properties for creating entity keys
     * @param reader request body
     * @return OEntity
     */
    private OEntity createOEntityFromRequest(List<String> keyPropNames,
            EdmDataServices metadata,
            Reader reader,
            String entitySetNameParam) {
        OEntityKey keyDummy = null;

        if (keyPropNames.size() == 1) {
            // single-unnamed value
            keyDummy = OEntityKey.create("");
        }
        //Implementation of TODO multiple-named value

        OEntity entity = null;

        //With ODataVersion.V2, perhaps due to a bug, JSON parsing does not work, so forcibly parse the request as V1.
        entity = convertFromString(reader, MediaType.APPLICATION_JSON_TYPE, ODataVersion.V1, metadata,
                entitySetNameParam, keyDummy);

        return entity;
    }

    /**
     * Create a response builder for POST.
     * @param ent OEntity
     * @param outputFormat Content-Type
     * @param responseStr response body
     * @param resUriInfo response UriInfo
     * @param key Entity key of the response
     * @return response builder
     */
    protected ResponseBuilder getPostResponseBuilder(
            OEntity ent,
            MediaType outputFormat,
            String responseStr,
            UriInfo resUriInfo,
            String key) {
        ResponseBuilder rb = Response.status(HttpStatus.SC_CREATED).entity(responseStr).type(outputFormat)
                .header(HttpHeaders.LOCATION, resUriInfo.getBaseUri().toASCIIString()
                        + getEntitySetName() + key)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);

        //Give ETAG to response
        if (ent instanceof OEntityWrapper) {
            OEntityWrapper oew2 = (OEntityWrapper) ent;
            String etag = oew2.getEtag();
            if (etag != null) {
                rb = rb.header(HttpHeaders.ETAG, "W/\"" + etag + "\"");
            }
        }
        return rb;
    }

    /**
     * Create a response body.
     * @param uriInfo UriInfo
     * @param resp response
     * @param format Response body format
     * @param acceptableMediaTypes List of allowed MediaTypes
     * @return response body
     */
    protected String renderEntityResponse(
            final UriInfo uriInfo,
            final EntityResponse resp,
            final String format,
            final List<MediaType> acceptableMediaTypes) {
        StringWriter w = new StringWriter();
        try {
            FormatWriter<EntityResponse> fw = PersoniumFormatWriterFactory.getFormatWriter(EntityResponse.class,
                    acceptableMediaTypes, format, null);
            // UriInfo uriInfo2 = PersoniumCoreUtils.createUriInfo(uriInfo, 1);
            fw.write(uriInfo, w, resp);
        } catch (UnsupportedOperationException e) {
            throw PersoniumCoreException.OData.FORMAT_INVALID_ERROR.params(format);
        }

        String responseStr = w.toString();

        return responseStr;

    }

    /**
     * Creates a default value instance of the OData property from the property schema information of the Entity Data Model.
     * @param ep Entity Data Model properties
     * @return Instance of OData property with default value
     */
    private OProperty<?> generateDefautlProperty(EdmProperty ep) {
        EdmType edmType = ep.getType();
        OProperty<?> op = null;

        //Get the Default value from the schema.
        String defaultValue = ep.getDefaultValue();
        String propName = ep.getName();

        //If the Default value is a specific function, generate a value.
        if (EdmSimpleType.STRING.equals(edmType)) {
            //When Type is a character string and Default value is CELLID ().
            if (defaultValue.equals(Common.UUID)) {
                //When Type is a character string and Default value is UUID ().
                String newUuid = UUID.randomUUID().toString().replaceAll("-", "");
                op = OProperties.string(propName, newUuid);
            } else if (defaultValue.equals("null")) {
                //When Type is a character string and Default value is null.
                op = OProperties.null_(propName, EdmSimpleType.STRING);
            } else {
                //When Type is a character string and the Default value is other value.
                op = OProperties.string(propName, defaultValue);
            }
        } else if (EdmSimpleType.DATETIME.equals(edmType)) {
            //Edm.DateTime Type:
            if (null == defaultValue || defaultValue.equals("null")) {
                //If defaultValue is null or "null", set it to null
                op = OProperties.null_(propName, EdmSimpleType.DATETIME);
            } else {
                //- If "\ / Date (...) \ /", set the default value
                //- For "SYSUTCDATETIME ()", set the current time to the default value
                //TODO In this implementation, it is output as Default TimeZone at Atom output
                op = OProperties.datetime(propName,
                        new Date(getTimeMillis(defaultValue)));
            }

        } else if (EdmSimpleType.SINGLE.equals(edmType)) {
            //When Type is SINGLE and there is a Default value.
            op = OProperties.single(propName, Float.valueOf(defaultValue));
        } else if (EdmSimpleType.INT64.equals(edmType)) {
            //When Type is INT64 and there is a Default value.
            op = OProperties.int64(propName, Long.valueOf(defaultValue));
        } else if (EdmSimpleType.INT32.equals(edmType)) {
            //When Type is INT32 and there is a Default value.
            op = OProperties.int32(propName, Integer.valueOf(defaultValue));
        } else if (EdmSimpleType.BOOLEAN.equals(edmType)) {
            //When Type is Boolean and there is a Default value.
            op = OProperties.boolean_(propName, Boolean.parseBoolean(defaultValue));
        } else if (EdmSimpleType.DOUBLE.equals(edmType)) {
            //When Type is Double and there is Default value.
            op = OProperties.double_(propName, Double.parseDouble(defaultValue));
        }

        return op;
    }

    private static OEntity convertFromString(final Reader body,
            final MediaType type,
            final ODataVersion version,
            final EdmDataServices metadata,
            final String entitySetName,
            final OEntityKey entityKey) {
        FormatParser<Entry> parser = PersoniumFormatParserFactory.getParser(Entry.class, type, new Settings(
                version, metadata, entitySetName, entityKey, null, false));
        Entry entry = null;
        try {
            entry = parser.parse(body);
        } catch (PersoniumCoreException e) {
            throw e;
        } catch (Exception e) {
            throw PersoniumCoreException.OData.JSON_PARSE_ERROR.reason(e);
        }
        return entry.getEntity();
    }

    /**
     * Dummy key check.
     * @param value Value to check
     * @return true: Dummy key false: other than dummy key
     */
    public static boolean isDummy(Object value) {
        boolean flag = false;
        if (value.equals(DUMMY_KEY)) {
            flag = true;
        }
        return flag;
    }

    /**
     * Returns a character string obtained by replacing the dummy key with null.
     * @param value Substitution target string
     * @return Return string
     */
    public static String replaceDummyKeyToNull(String value) {
        return value.replaceAll("'" + DUMMY_KEY + "'", "null");
    }

    /**
     * Return a character string with null replaced with a dummy key (with parentheses).
     * @param value Substitution target string
     * @return Return string
     */
    public static String replaceNullToDummyKeyWithParenthesis(String value) {
        return replaceNullToDummyKey("(" + value + ")");
    }

    /**
     * Return a character string with null replaced with a dummy key.
     * @param value Substitution target string
     * @return Return string
     */
    public static String replaceNullToDummyKey(String value) {
        Pattern pattern = Pattern.compile("=null([,|\\)])");
        Matcher m = pattern.matcher(value);
        return m.replaceAll("='" + DUMMY_KEY + "'$1");
    }

    /**
     * URL encode the uri part of EntityKey and return it.
     *   core issue #214, #486
     * @param entitySet EntitySet
     * @param entityKey EntityKey
     * @return Converted EntityKey
     */
    public static OEntityKey convertToUrlEncodeKey(EdmEntitySet entitySet, OEntityKey entityKey) {
        // all single type key including user odata should be url-encoded.
        if (KeyType.SINGLE.equals(entityKey.getKeyType())) {
            String encoded;
            try {
                encoded = URLEncoder.encode((String) entityKey.asSingleValue(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                // Usually it is impossible to go through this route.
                throw PersoniumCoreException.Server.UNKNOWN_ERROR;
            }
            return OEntityKey.create(encoded);
        } else if (ExtRole.EDM_TYPE_NAME.equals(entitySet.getName())) {
            Map<String, Object> map = new HashMap<String, Object>();
            for (NamedValue<?> namedValue : entityKey.asComplexValue()) {
                if (ExtRole.P_EXT_ROLE.getName().equals(namedValue.getName())) {
                    try {
                        map.put(namedValue.getName(), URLEncoder.encode((String) namedValue.getValue(), "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        // Usually it is impossible to go through this route.
                        throw PersoniumCoreException.Server.UNKNOWN_ERROR;
                    }
                } else {
                    map.put(namedValue.getName(), namedValue.getValue());
                }
            }
            return OEntityKey.create(map);
        }
        return entityKey;
    }

    /**
     * Get EntityType and PropertyName from NavigationTargetKeyProperty.
     * @param propertyName property name
     * @return EntityType and PropertyName
     */
    public static HashMap<String, String> convertNTKP(String propertyName) {
        HashMap<String, String> ntkp = null;
        Pattern pattern = Pattern.compile("_([^.]+)\\.(.+)");
        Matcher m = pattern.matcher(propertyName);
        if (m.matches()) {
            ntkp = new HashMap<String, String>();
            ntkp.put("entityType", m.group(1));
            ntkp.put("propName", m.group(2));
        }
        return ntkp;
    }

    /**
     * Acquires the character string specified by the argument as the value of TimeMillis.
     * @param timeStr TimeMillis string representation (ex. "/ Data (...) /", "SYSUTCDATETIME ()")
     * @return TimeMillis value
     */
    private long getTimeMillis(String timeStr) {
        long timeMillis = 0;
        if (timeStr.equals(Common.SYSUTCDATETIME)) {
            timeMillis = currentTimeMillis;
        } else {
            try {
                Pattern pattern = Pattern.compile("^/Date\\((.+)\\)/$");
                Matcher match = pattern.matcher(timeStr);
                if (match.matches()) {
                    String date = match.replaceAll("$1");
                    timeMillis = Long.parseLong(date);
                }
            } catch (NumberFormatException e) {
                throw PersoniumCoreException.OData.JSON_PARSE_ERROR.reason(e);
            }
        }
        return timeMillis;
    }

    /**
     * Escape the response body.
     * @param response Response body
     * @return escaped response body
     */
    public String escapeResponsebody(String response) {
        return EscapeControlCode.escape(response);
    }
}
