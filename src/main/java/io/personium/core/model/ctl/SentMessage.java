/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core.model.ctl;

import org.core4j.Enumerable;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;

/**
 * Edm definition of SentMessage.
 */
public class SentMessage extends Message {
    /**
     * Constructor.
     */
    protected SentMessage() {
        super();
    }

    /**
     * Edm Entity Type name.
     */
    public static final String EDM_TYPE_NAME = "SentMessage";

    /**
     * To property.
     */
    public static final EdmProperty.Builder P_TO = EdmProperty.newBuilder("To")
            .setType(EdmSimpleType.STRING)
            .setNullable(true);
    /**
     * ToRelation property.
     */
    public static final EdmProperty.Builder P_TO_RELATION = EdmProperty.newBuilder("ToRelation")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_RELATION_NAME);

    /**
     * Result/To property.
     */
    public static final EdmProperty.Builder P_RESULT_TO = EdmProperty.newBuilder("To")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_CELL_URL);
    /**
    * Result/Code property.
    */
   public static final EdmProperty.Builder P_RESULT_CODE = EdmProperty.newBuilder("Code")
           .setType(EdmSimpleType.STRING)
           .setNullable(true);
   /**
    * Result/Reason property.
    */
   public static final EdmProperty.Builder P_RESULT_REASON = EdmProperty.newBuilder("Reason")
           .setType(EdmSimpleType.STRING)
           .setNullable(true);

   /**
    * Result ComplexType Builder.
    */
   public static final EdmComplexType.Builder COMPLEX_TYPE_RESULT = EdmComplexType.newBuilder()
           .setNamespace(Common.EDM_NS_CELL_CTL)
           .setName("Sent_Message_Result")
           .addProperties(Enumerable.create(P_RESULT_TO, P_RESULT_CODE, P_RESULT_REASON).toList());
    /**
     * Result property.
     */
    public static final EdmProperty.Builder P_RESULT = EdmProperty.newBuilder("Result")
            .setType(COMPLEX_TYPE_RESULT.build())
            .setCollectionKind(CollectionKind.List)
            .setNullable(true);

    /**
     * EntityType Builder.
     */
    static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(Common.P_ID, Common.P_BOX_NAME, P_IN_REPLY_TO, P_TO, P_TO_RELATION,
                            P_TYPE, P_TITLE, P_BODY, P_PRIORITY, P_REQUEST_OBJECTS,
                            P_RESULT, Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(Common.P_ID.getName());

}
