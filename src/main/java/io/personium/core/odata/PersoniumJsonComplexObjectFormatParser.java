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
 * This code is based on JsonComplexObjectFormatParser.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.format.FormatParser;
import org.odata4j.format.Settings;
import org.odata4j.format.json.JsonStreamReaderFactory.JsonParseException;
import org.odata4j.format.json.JsonTypeConverter;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonEvent;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader;
import io.personium.core.utils.ODataUtils;

/**
 * Parser for OComplexObjects in JSON.
 */
public class PersoniumJsonComplexObjectFormatParser extends PersoniumJsonFormatParser
        implements FormatParser<OComplexObject> {

    /**
     * constructor.
     * @param s setting information
     */
    public PersoniumJsonComplexObjectFormatParser(Settings s) {
        super(s);
        if (s == null) {
            returnType = null;
        } else {
            returnType = (EdmComplexType) s.parseType;
        }
    }

    /** Type to return.*/
    private EdmComplexType returnType = null;

    /**
     * The parse of ComplexTypeObject.
     * @param reader Perth target
     * @return OComplexObject
     */
    @Override
    public OComplexObject parse(Reader reader) {
        JsonStreamReader jsr = PersoniumJsonStreamReaderFactory.createJsonStreamReader(reader);
        try {
            OComplexObject o = parseSingleObject(jsr);
            return o;
        } finally {
            jsr.close();
        }
    }

    /**
     * The parse of ComplexTypeObject.
     * @param jsr JsonStreamReader of parse target character
     * @return OComplexObject
     */
    public OComplexObject parseSingleObject(JsonStreamReader jsr) {
        ensureNext(jsr);

        JsonEvent event = jsr.nextEvent();
        if (event.isStartObject()) {
            List<OProperty<?>> props = new ArrayList<OProperty<?>>();
            return eatProps(props, jsr);
        } else {
            // not a start object.
            return null;
        }
    }

    /**
     * The parse of ComplexTypeObject.
     * @param jsr JsonStreamReader of parse target character
     * @param startPropertyEvent JsonEvent
     * @return OComplexObject
     */
    public OComplexObject parseSingleObject(JsonStreamReader jsr, JsonEvent startPropertyEvent) {
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        addProperty(props, startPropertyEvent.asStartProperty().getName(), jsr);
        return eatProps(props, jsr);
    }

    /**
     * Add the shortcoming ComplexTypeProperty.
     * @param props Property list to be added
     * @param jsr JsonStreamReader of parse target character
     * @return OComplexObject
     */
    private OComplexObject eatProps(List<OProperty<?>> props, JsonStreamReader jsr) {

        ensureNext(jsr);
        while (jsr.hasNext()) {
            JsonEvent event = jsr.nextEvent();

            if (event.isStartProperty()) {
                addProperty(props, event.asStartProperty().getName(), jsr);
            } else if (event.isEndProperty()) {
                continue;
            } else if (event.isEndObject()) {
                break;
            } else {
                throw new JsonParseException("unexpected parse event: " + event.toString());
            }
        }
        return OComplexObjects.create(returnType, props);
    }

    /**
     * Added properties.
     * @param props Property list to be added
     * @param name Additional Property Name
     * @param jsr JsonStreamReader of parse target character
     */
    protected void addProperty(List<OProperty<?>> props, String name, JsonStreamReader jsr) {
        JsonEvent event = jsr.nextEvent();

        //Get property definition from ComplexType definition
        EdmProperty ep = returnType.findProperty(name);

        if (event.isEndProperty()) {
            if (ep == null) {
                //If the property definition does not exist on the ComplexType definition, it is regarded as an error
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
            if (ep.getType().isSimple()) {
                //Value check
                String propValue = event.asEndProperty().getValue();
                if (propValue != null) {
                    if (ep.getType().equals(EdmSimpleType.BOOLEAN)
                            && !ODataUtils.validateBoolean(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (ep.getType().equals(EdmSimpleType.SINGLE)
                            && !ODataUtils.validateSingle(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (ep.getType().equals(EdmSimpleType.INT32)
                            && !ODataUtils.validateInt32(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (ep.getType().equals(EdmSimpleType.DOUBLE)
                            && !ODataUtils.validateDouble(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (ep.getType().equals(EdmSimpleType.STRING)
                            && !ODataUtils.validateString(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (ep.getType().equals(EdmSimpleType.DATETIME)) {
                        if (!ODataUtils.validateDateTime(propValue)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                        }
                        if (Common.SYSUTCDATETIME.equals(propValue)) {
                            String crrTime = String.valueOf(getCurrentTimeMillis());
                            propValue = String.format("/Date(%s)/", crrTime);
                        }
                    }
                }

                //If it is a simple type (character string, number, etc.), it is added to the property
                props.add(JsonTypeConverter.parse(name, (EdmSimpleType<?>) ep.getType(), propValue));
            } else {
                //If it is ComplexType type, parse it again and add it to the property
                JsonObjectPropertyValue val = getValue(event, name, jsr, ep);
                if (val != null) {
                    props.add(OProperties.complex(name, (EdmComplexType) val.complexObject.getType(),
                            val.complexObject.getProperties()));
                } else {
                    //When the value of ComplexType is Null
                    EdmComplexType ct = getMetadata().findEdmComplexType(ep.getType().getFullyQualifiedTypeName());
                    props.add(OProperties.complex(name, ct, null));
                }
            }
        } else if (event.isStartObject()) {
            JsonObjectPropertyValue val = getValue(event, name, jsr, ep);
            if (val.complexObject != null) {
                //If it is ComplexType data, it is added to the property
                props.add(OProperties.complex(name, (EdmComplexType) val.complexObject.getType(),
                        val.complexObject.getProperties()));
            } else {
                //Make errors other than ComplexType data
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
        } else if (event.isStartArray()) {
            //For array objects
            JsonObjectPropertyValue val = new JsonObjectPropertyValue();

            //If the schema definition exists and CollectionKind is not None, parse it as an array
            if (null != ep && ep.getCollectionKind() != CollectionKind.NONE) {
                val.collectionType = new EdmCollectionType(ep.getCollectionKind(), ep.getType());
                PersoniumJsonCollectionFormatParser cfp = new PersoniumJsonCollectionFormatParser(val.collectionType,
                        getMetadata(), name);
                val.collection = cfp.parseCollection(jsr);
            }

            //If parsing succeeds, add it to the property
            if (val.collectionType != null && val.collection != null) {
                props.add(OProperties.collection(name, val.collectionType, val.collection));
            } else {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
        } else {
            throw new JsonParseException("expecting endproperty, got: " + event.toString());
        }
    }

    /**
     * Get the value of the JSON object.
     * @param event JsonEvent
     * @param name property name
     * @param jsr JsonStreamReader
     * @param ep Property Definition
     * @return JsonObjectPropertyValue
     */
    protected JsonObjectPropertyValue getValue(JsonEvent event,
            String name,
            JsonStreamReader jsr,
            EdmProperty ep) {
        JsonObjectPropertyValue rt = new JsonObjectPropertyValue();
        if (event.isStartObject()) {
            // ensureStartObject(value);
            event = jsr.nextEvent();
            ensureStartProperty(event);

            //Get ComplexType definition from schema definition
            EdmComplexType ct = getMetadata().findEdmComplexType(ep.getType().getFullyQualifiedTypeName());

            if (null != ct) {
                //If there is a ComplexType, execute a parse and acquire a ComplexTypeObject
                Settings s = new Settings(getVersion(), getMetadata(), null, null, null, false, ct);
                PersoniumJsonComplexObjectFormatParser cofp = new PersoniumJsonComplexObjectFormatParser(s);
                rt.complexObject = cofp.parseSingleObject(jsr, event);
            } else {
                //If ComplexType does not exist on the schema definition, it is regarded as an error
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }

            ensureEndProperty(jsr.nextEvent());
        } else if (event.isEndProperty() && event.asEndProperty().getValue() == null) {
            //When the value of ComplexType is Null
            return null;
        } else {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
        }
        return rt;
    }

}
