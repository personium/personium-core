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
 * This code is based on JsonCollectionFormatParser.java of odata4j-core, and some modifications
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

import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OObject;
import org.odata4j.core.OSimpleObjects;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.format.FormatParser;
import org.odata4j.format.Settings;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonEvent;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonValueEvent;
import io.personium.core.utils.ODataUtils;

/**
 *OCollection's parser.
 */
public class PersoniumJsonCollectionFormatParser extends PersoniumJsonFormatParser implements
        FormatParser<OCollection<? extends OObject>> {

    private final EdmCollectionType returnType;

    private String propertyName;

    /**
     *constructor.
     *@ param collectionType Collection type
     *@ param md schema information
     *@ param name property name
     */
    public PersoniumJsonCollectionFormatParser(EdmCollectionType collectionType, EdmDataServices md, String name) {
        super(null);
        super.setMetadata(md);
        returnType = collectionType;
        propertyName = name;
    }

    @Override
    public OCollection<? extends OObject> parse(Reader reader) {
        JsonStreamReader jsr = PersoniumJsonStreamReaderFactory.createJsonStreamReader(reader);
        try {
            // parse the entry
            OCollection<? extends OObject> o = parseCollection(jsr);
            return o;
        } finally {
            jsr.close();
        }
    }

    /**
     *Perth of Collection.
     * @param jsr JsonStreamReader
     * @return OCollection
     */
    protected OCollection<? extends OObject> parseCollection(JsonStreamReader jsr) {
        ensureNext(jsr);
        OCollection.Builder<OObject> c = newCollectionBuilder();

        if (this.returnType.getItemType().isSimple()) {
            //Parse as OSimpleObject if array type is simple type
            parseCollectionOfSimple(c, jsr);
        } else {
            //If the type of the array is not a simple type, obtain a parser of the corresponding type and parse it as a ComplexObject
            EdmComplexType ct = getMetadata().findEdmComplexType(
                    this.returnType.getItemType().getFullyQualifiedTypeName());

            if (null != ct) {
                //If there is a ComplexType, execute a parse and acquire a ComplexTypeObject
                Settings s = new Settings(getVersion(), getMetadata(),
                        getEntitySetName(), getEntityKey(), null, false, ct);
                PersoniumJsonComplexObjectFormatParser cofp = new PersoniumJsonComplexObjectFormatParser(s);
                while (jsr.hasNext()) {
                    OComplexObject obj = cofp.parseSingleObject(jsr);
                    if (null != obj) {
                        c = c.add(obj);
                    } else {
                        break;
                    }
                }

            } else {
                //If ComplexType does not exist on the schema definition, it is regarded as an error
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propertyName);
            }

        }

        ensureEndArray(jsr.previousEvent());
        return c.build();
    }

    /**
     *Parsing for simple type array.
     * @param builder OCollection.Builder
     * @param jsr JsonStreamReader
     */
    protected void parseCollectionOfSimple(OCollection.Builder<OObject> builder, JsonStreamReader jsr) {
        while (jsr.hasNext()) {
            JsonEvent e = jsr.nextEvent();
            if (e.isValue()) {
                JsonValueEvent ve = e.asValue();
                String propValue = ve.getValue();
                if (propValue != null) {
                    if (this.returnType.getItemType().equals(EdmSimpleType.BOOLEAN)
                            && !ODataUtils.validateBoolean(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propertyName);
                    } else if (this.returnType.getItemType().equals(EdmSimpleType.SINGLE)
                            && !ODataUtils.validateSingle(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propertyName);
                    } else if (this.returnType.getItemType().equals(EdmSimpleType.INT32)
                            && !ODataUtils.validateInt32(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propertyName);
                    } else if (this.returnType.getItemType().equals(EdmSimpleType.DOUBLE)
                            && !ODataUtils.validateDouble(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propertyName);
                    } else if (this.returnType.getItemType().equals(EdmSimpleType.STRING)
                            && !ODataUtils.validateString(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propertyName);
                    } else if (this.returnType.getItemType().equals(EdmSimpleType.DATETIME)) {
                        if (!ODataUtils.validateDateTime(propValue)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propertyName);
                        }
                        if (Common.SYSUTCDATETIME.equals(propValue)) {
                            String crrTime = String.valueOf(getCurrentTimeMillis());
                            propValue = String.format("/Date(%s)/", crrTime);
                        }
                    }
                }
                builder.add(OSimpleObjects.parse((EdmSimpleType<?>) this.returnType.getItemType(), propValue));
            } else if (e.isEndArray()) {
                break;
            } else {
                throw new RuntimeException("invalid JSON content");
            }
        }
    }

    /**
     *Create a collection builder.
     * @return OCollection.Builder
     */
    protected OCollection.Builder<OObject> newCollectionBuilder() {
        return OCollections.<OObject>newBuilder(this.returnType.getItemType());
    }

}
