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
 * This code is based on FormatParserFactory.java of odata4j-core, and some modifications
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

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.odata4j.edm.EdmDataServices;
import org.odata4j.format.Entry;
import org.odata4j.format.FormatType;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.SingleLink;
import org.odata4j.format.SingleLinks;
import org.odata4j.format.json.JsonCollectionFormatWriter;
import org.odata4j.format.json.JsonComplexObjectFormatWriter;
import org.odata4j.format.json.JsonPropertyFormatWriter;
import org.odata4j.format.json.JsonRequestEntryFormatWriter;
import org.odata4j.format.json.JsonServiceDocumentFormatWriter;
import org.odata4j.format.xml.AtomEntryFormatWriter;
import org.odata4j.format.xml.AtomFeedFormatWriter;
import org.odata4j.format.xml.AtomRequestEntryFormatWriter;
import org.odata4j.format.xml.AtomServiceDocumentFormatWriter;
import org.odata4j.format.xml.AtomSingleLinkFormatWriter;
import org.odata4j.format.xml.AtomSingleLinksFormatWriter;
import org.odata4j.format.xml.XmlPropertyFormatWriter;
import org.odata4j.producer.CollectionResponse;
import org.odata4j.producer.ComplexObjectResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.PropertyResponse;
import org.odata4j.producer.exceptions.NotImplementedException;

/**
 * Wrapper class for FormatWriterFactory.
 */
public class PersoniumFormatWriterFactory {

    /**
     * interface.
     */
    private interface FormatWriters {

        FormatWriter<EdmDataServices> getServiceDocumentFormatWriter();

        FormatWriter<EntitiesResponse> getFeedFormatWriter();

        FormatWriter<EntityResponse> getEntryFormatWriter();

        FormatWriter<PropertyResponse> getPropertyFormatWriter();

        FormatWriter<Entry> getRequestEntryFormatWriter();

        FormatWriter<SingleLink> getSingleLinkFormatWriter();

        FormatWriter<SingleLinks> getSingleLinksFormatWriter();

        FormatWriter<ComplexObjectResponse> getComplexObjectFormatWriter();

        FormatWriter<CollectionResponse<?>> getCollectionFormatWriter();
    }

    private PersoniumFormatWriterFactory() {
    }

    /**
     * Getter of format writer.
     * @param <T> type
     * @param targetType Type of format writer
     * @param acceptTypes accept type
     * @param format Format
     * @param callback callback
     * @return format writer
     */
    @SuppressWarnings("unchecked")
    public static <T> FormatWriter<T> getFormatWriter(Class<T> targetType,
            List<MediaType> acceptTypes,
            String format,
            String callback) {

        FormatType type = null;

        // if format is explicitly specified, use that
        if (format != null) {
            type = FormatType.parse(format);
        }

        // if header accepts json, use that
        if (type == null && acceptTypes != null) {
            for (MediaType acceptType : acceptTypes) {
                if (acceptType.equals(MediaType.APPLICATION_JSON_TYPE)) {
                    type = FormatType.JSON;
                    break;
                }
            }
        }

        // else default to atom
        if (type == null) {
            type = FormatType.ATOM;
        }

        FormatWriters formatWriters = null;
        if (type.equals(FormatType.JSON)) {
            formatWriters = new JsonWriters(callback);
        } else {
            formatWriters = new AtomWriters();
        }

        if (targetType.equals(EdmDataServices.class)) {
            return (FormatWriter<T>) formatWriters.getServiceDocumentFormatWriter();
        }

        if (targetType.equals(EntitiesResponse.class)) {
            return (FormatWriter<T>) formatWriters.getFeedFormatWriter();
        }

        if (targetType.equals(EntityResponse.class)) {
            return (FormatWriter<T>) formatWriters.getEntryFormatWriter();
        }

        if (targetType.equals(PropertyResponse.class)) {
            return (FormatWriter<T>) formatWriters.getPropertyFormatWriter();
        }

        if (Entry.class.isAssignableFrom(targetType)) {
            return (FormatWriter<T>) formatWriters.getRequestEntryFormatWriter();
        }

        if (SingleLink.class.isAssignableFrom(targetType)) {
            return (FormatWriter<T>) formatWriters.getSingleLinkFormatWriter();
        }

        if (SingleLinks.class.isAssignableFrom(targetType)) {
            return (FormatWriter<T>) formatWriters.getSingleLinksFormatWriter();
        }

        if (targetType.equals(ComplexObjectResponse.class)) {
            return (FormatWriter<T>) formatWriters.getComplexObjectFormatWriter();
        }

        if (targetType.equals(CollectionResponse.class)) {
            return (FormatWriter<T>) formatWriters.getCollectionFormatWriter();
        }

        throw new IllegalArgumentException("Unable to locate format writer for " + targetType.getName()
                + " and format " + type);

    }

    /**
     * Format writer for JSON.
     */
    public static class JsonWriters implements FormatWriters {

        private final String callback;

        /**
         * constructor.
         * @param callback callback
         */
        public JsonWriters(String callback) {
            this.callback = callback;
        }

        @Override
        public FormatWriter<EdmDataServices> getServiceDocumentFormatWriter() {
            return new JsonServiceDocumentFormatWriter(callback);
        }

        @Override
        public FormatWriter<EntitiesResponse> getFeedFormatWriter() {
            return new PersoniumJsonFeedFormatWriter(callback);
        }

        @Override
        public FormatWriter<EntityResponse> getEntryFormatWriter() {
            return new PersoniumJsonEntryFormatWriter(callback);
        }

        @Override
        public FormatWriter<PropertyResponse> getPropertyFormatWriter() {
            return new JsonPropertyFormatWriter(callback);
        }

        @Override
        public FormatWriter<Entry> getRequestEntryFormatWriter() {
            return new JsonRequestEntryFormatWriter(callback);
        }

        @Override
        public FormatWriter<SingleLink> getSingleLinkFormatWriter() {
            return new PersoniumJsonSingleLinkFormatWriter(callback);
        }

        @Override
        public FormatWriter<SingleLinks> getSingleLinksFormatWriter() {
            return new PersoniumJsonSingleLinksFormatWriter(callback);
        }

        @Override
        public FormatWriter<ComplexObjectResponse> getComplexObjectFormatWriter() {
            return new JsonComplexObjectFormatWriter(callback);
        }

        @Override
        public FormatWriter<CollectionResponse<?>> getCollectionFormatWriter() {
            return new JsonCollectionFormatWriter(callback);
        }
    }

    /**
     * Format writer for ATOM.
     */
    public static class AtomWriters implements FormatWriters {

        @Override
        public FormatWriter<EdmDataServices> getServiceDocumentFormatWriter() {
            return new AtomServiceDocumentFormatWriter();
        }

        @Override
        public FormatWriter<EntitiesResponse> getFeedFormatWriter() {
            return new AtomFeedFormatWriter();
        }

        @Override
        public FormatWriter<EntityResponse> getEntryFormatWriter() {
            return new AtomEntryFormatWriter();
        }

        @Override
        public FormatWriter<PropertyResponse> getPropertyFormatWriter() {
            return new XmlPropertyFormatWriter();
        }

        @Override
        public FormatWriter<Entry> getRequestEntryFormatWriter() {
            return new AtomRequestEntryFormatWriter();
        }

        @Override
        public FormatWriter<SingleLink> getSingleLinkFormatWriter() {
            return new AtomSingleLinkFormatWriter();
        }

        @Override
        public FormatWriter<SingleLinks> getSingleLinksFormatWriter() {
            return new AtomSingleLinksFormatWriter();
        }

        @Override
        public FormatWriter<ComplexObjectResponse> getComplexObjectFormatWriter() {
            throw new NotImplementedException();
        }

        @Override
        public FormatWriter<CollectionResponse<?>> getCollectionFormatWriter() {
            throw new NotImplementedException();
        }

    }

}
