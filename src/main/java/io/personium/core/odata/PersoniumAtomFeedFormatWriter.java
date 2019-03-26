/**
 * personium.io
 * Modifications copyright 2019 FUJITSU LIMITED
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
 * This code is based on OptionsQueryParser.java of odata4j-core, and some modifications
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

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.odata4j.core.OAtomEntity;
import org.odata4j.core.OAtomStreamEntity;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.format.xml.AtomFeedFormatWriter;
import org.odata4j.internal.InternalUtil;
import org.odata4j.stax2.QName2;
import org.odata4j.stax2.XMLWriter2;

import io.personium.core.rs.odata.AbstractODataResource;

/**
 * Wrapper class of AtomFeedFormatWriter.
 */
public class PersoniumAtomFeedFormatWriter extends AtomFeedFormatWriter {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String writeEntry(XMLWriter2 writer, OEntity oe,
            List<OProperty<?>> entityProperties, List<OLink> entityLinks,
            String baseUri, String updated,
            EdmEntitySet ees, boolean isResponse) {

        String relid = null;
        String absid = null;
        if (isResponse) {
            relid = getEntityRelId(oe);
            absid = baseUri + relid;
            writeElement(writer, "id", absid);
        }

        OAtomEntity oae = getAtomInfo(oe);

        writeElement(writer, "title", oae.getAtomEntityTitle(), "type", "text");
        String summary = oae.getAtomEntitySummary();
        if (summary != null) {
            writeElement(writer, "summary", summary, "type", "text");
        }

        LocalDateTime updatedTime = oae.getAtomEntityUpdated();
        if (updatedTime != null) {
            updated = InternalUtil.toString(updatedTime.toDateTime(DateTimeZone.UTC));
        }
        writeElement(writer, "updated", updated);

        writer.startElement("author");
        writeElement(writer, "name", oae.getAtomEntityAuthor());
        writer.endElement("author");

        if (isResponse) {
            writeElement(writer, "link", null, "rel", "edit", "title", ees.getType().getName(), "href", relid);
        }

        if (entityLinks != null) {
            if (isResponse) {
                // the producer has populated the link collection, we just what he gave us.
                for (OLink link : entityLinks) {
                    String rel = related + link.getTitle();
                    String type = (link.isCollection())
                            ? atom_feed_content_type // CHECKSTYLE IGNORE : Copy of logic from inheritance source
                            : atom_entry_content_type;
                    String href = relid + "/" + link.getTitle();
                    if (link.isInline()) {
                        writer.startElement("link");
                        writer.writeAttribute("rel", rel);
                        writer.writeAttribute("type", type);
                        writer.writeAttribute("title", link.getTitle());
                        writer.writeAttribute("href", href);
                        // write the inlined entities inside the link element
                        writeLinkInline(writer, link,
                                href, baseUri, updated, isResponse);
                        writer.endElement("link");
                    } else {
                        // deferred link.
                        writeElement(writer, "link", null,
                                "rel", rel,
                                "type", type,
                                "title", link.getTitle(),
                                "href", href);
                    }
                }
            } else {
                // for requests we include only the provided links
                // Note: It seems that OLinks for responses are only built using the
                // title and OLinks for requests have the additional info in them
                // alread.  I'm leaving that inconsistency in place for now but this
                // else and its preceding if could probably be unified.
                for (OLink olink : entityLinks) {
                    String type = olink.isCollection()
                            ? atom_feed_content_type // CHECKSTYLE IGNORE : Copy of logic from inheritance source
                            : atom_entry_content_type;

                    writer.startElement("link");
                    writer.writeAttribute("rel", olink.getRelation());
                    writer.writeAttribute("type", type);
                    writer.writeAttribute("title", olink.getTitle());
                    writer.writeAttribute("href", olink.getHref());
                    if (olink.isInline()) {
                        // write the inlined entities inside the link element
                        writeLinkInline(writer, olink, olink.getHref(),
                                baseUri, updated, isResponse);
                    }
                    writer.endElement("link");
                }
            }
        } // else entityLinks null

        writeElement(writer, "category", null,
                // oe is null for creates
                "term",
                oe == null ? ees.getType().getFullyQualifiedTypeName() : oe.getEntityType().getFullyQualifiedTypeName(), // CHECKSTYLE IGNORE : Copy of logic from inheritance source
                "scheme", scheme);

        boolean hasStream = false;
        if (oe != null) {
            OAtomStreamEntity stream = oe.findExtension(OAtomStreamEntity.class);
            if (stream != null) {
                hasStream = true;
                writer.startElement("content");
                writer.writeAttribute("type", stream.getAtomEntityType());
                writer.writeAttribute("src", baseUri + stream.getAtomEntitySource());
                writer.endElement("content");
            }
        }

        if (!hasStream) {
            writer.startElement("content");
            writer.writeAttribute("type", MediaType.APPLICATION_XML);
        }

        writer.startElement(new QName2(m, "properties", "m"));
        writeProperties(writer, entityProperties);
        writer.endElement("properties");

        if (!hasStream) {
            writer.endElement("content");
        }
        return absid;

    }

    private OAtomEntity getAtomInfo(OEntity oe) {
        if (oe != null) {
            OAtomEntity atomEntity = oe.findExtension(OAtomEntity.class);
            if (atomEntity != null) {
                return atomEntity;
            }
        }
        return new OAtomEntity() {
            @Override
            public String getAtomEntityTitle() {
                return null;
            }

            @Override
            public String getAtomEntitySummary() {
                return null;
            }

            @Override
            public String getAtomEntityAuthor() {
                return null;
            }

            @Override
            public LocalDateTime getAtomEntityUpdated() {
                return null;
            }
        };
    }

    /**
     * Return EntityRelId.
     * @param oe OEntity
     * @return EntityRelId
     */
    public String getEntityRelId(OEntity oe) {
        return getEntityRelId(oe.getEntitySet(), oe.getEntityKey());
    }

    /**
     * Return EntityRelId.
     * @param entitySet entity set
     * @param entityKey entity key
     * @return EntityRelId
     */
    public String getEntityRelId(EdmEntitySet entitySet, OEntityKey entityKey) {
        OEntityKey convertedKey = AbstractODataResource.convertToUrlEncodeKey(entitySet, entityKey);
        String key = AbstractODataResource.replaceDummyKeyToNull(convertedKey.toKeyString());
        return entitySet.getName() + key;
    }

}
