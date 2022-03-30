/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.model;


import java.util.List;

import org.core4j.Enumerable;
import org.joda.time.LocalDateTime;
import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.odata.OEntityWrapper;


/**
 * Model class for personium Box.
 */
public class Box {
    private Cell cell;
    private String schema;
    private String name;
    private String id;
    private long published;

    // Schema information

    /**
     * Edm.Entity Type name.
     */
    public static final String EDM_TYPE_NAME = "Box";

    /** Extended schema Format schema-uri. */
    public static final String P_FORMAT_PATTERN_SCHEMA_URI = "schema-uri";

    /**
     * Create the Annotation for Schema URI.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatSchemaUriAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, P_FORMAT_PATTERN_SCHEMA_URI);
    }

    /**
     * To get the Annotation for Schema.
     * @param name UK Name
     * @return Annotation List
     */
    private static List<EdmAnnotation<?>> createSchemaAnnotation(final String name) {
        List<EdmAnnotation<?>> schemaAnnotation = CtlSchema.createNamedUkAnnotation(name);
        schemaAnnotation.add(createFormatSchemaUriAnnotation());
        return schemaAnnotation;
    }

    /**
     * Schema Definition of property.
     */
    public static final EdmProperty.Builder P_SCHEMA = EdmProperty.newBuilder("Schema").setType(EdmSimpleType.STRING)
            .setAnnotations(createSchemaAnnotation("uk_box_schema"))
            .setNullable(true).setDefaultValue("null");

    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL).setName(EDM_TYPE_NAME)
            .addProperties(Enumerable.create(Common.P_NAME, P_SCHEMA,
                    Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(Common.P_NAME.getName());

    /**
     * main box name.
     */
    public static final String MAIN_BOX_NAME = "__";

    /**
     * Constructor.
     * @param cell cell object
     * @param entity OEntity object
     */
    public Box(final Cell cell, final OEntity entity) {
        this.cell = cell;
        if (entity == null) {
            // Process for the MAIN BOX
            this.name = Box.MAIN_BOX_NAME;
            // Schema URL of MAIN BOX is the URL of its own cell
            this.schema = cell.getUrl();
            // Internal ID of MAIN BOX will be together with the ID of the cell.
            this.id = cell.getId();
            return;
        }
        this.name = (String) entity.getProperty(Common.P_NAME.getName()).getValue();
        this.schema = (String) entity.getProperty(P_SCHEMA.getName()).getValue();
        if (entity instanceof OEntityWrapper) {
            OEntityWrapper oew = (OEntityWrapper) entity;
            this.id = oew.getUuid();
        }
        LocalDateTime dateTime = (LocalDateTime) entity.getProperty(Common.P_PUBLISHED.getName()).getValue();
        this.published = dateTime.toDateTime().getMillis();
    }

    /**
     * Constructor.
     * @param cell Cell object
     * @param name Box name
     * @param schema Box schema
     * @param id Box Internal ID
     * @param published Date and time of creation
     */
    public Box(final Cell cell, final String name,
            final String schema, final String id, final Long published) {
        this.cell = cell;
        this.name = name;
        this.schema = schema;
        this.id = id;
        this.published = published;
    }

    /**
     * It returns the Cell that this Box belongs.
     * @return Cell
     */
    public Cell getCell() {
        return this.cell;
    }

    /**
     * It returns the path name of this Box.
     * @return Path name
     */
    public String getName() {
        return this.name;
    }

    /**
     * It returns the Schema URL of this Box.
     * @return Schema URL String
     */
    public String getSchema() {
        return this.schema;
    }

    /**
     * It returns the internal ID for the management of this Box.
     * @return ID String
     */
    public String getId() {
        return this.id;
    }

    /**
     * Set the cell object.
     * @param cell the cell to set
     */
    public void setCell(Cell cell) {
        this.cell = cell;
    }

    /**
     * Set the schema.
     * @param schema the schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Set the name.
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the id.
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get value of the published.
     * @return the published
     */
    public long getPublished() {
        return published;
    }

    /**
     * Get box url.
     * @return box url
     */
    public String getUrl() {
        return getCell().getUrl() + getName() + "/";
    }

}
