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
package io.personium.core.model.ctl;

import org.core4j.Enumerable;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;

/**
 * SentMessage のEdm 定義体.
 */
public class SentMessage {
    /**
     * コンストラクタ.
     */
    protected SentMessage() {
    }

    /**
     * Edm EntityType名.
     */
    public static final String EDM_TYPE_NAME = "SentMessage";
    /**
     * _Box.Nameプロパティの定義体.
     */
    public static final EdmProperty.Builder P_BOX_NAME = EdmProperty.newBuilder("_Box.Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(true);
    /**
     * InReplyToプロパティの定義体.
     */
    public static final EdmProperty.Builder P_IN_REPLY_TO = EdmProperty.newBuilder("InReplyTo")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_IN_REPLY_TO);
    /**
     * Toプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TO = EdmProperty.newBuilder("To")
            .setType(EdmSimpleType.STRING)
            .setNullable(true);
    /**
     * ToRelationプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TO_RELATION = EdmProperty.newBuilder("ToRelation")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_RELATION_NAME);
    /**
     * Typeプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TYPE = EdmProperty.newBuilder("Type")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setDefaultValue(ReceivedMessage.TYPE_MESSAGE)
            .setAnnotations(Common.P_FORMAT_MESSAGE_TYPE);
    /**
     * Titleプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TITLE = EdmProperty.newBuilder("Title")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setDefaultValue("")
            .setAnnotations(Common.P_FORMAT_MESSAGE_TITLE);
    /**
     * Bodyプロパティの定義体.
     */
    public static final EdmProperty.Builder P_BODY = EdmProperty.newBuilder("Body")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setDefaultValue("");
    /**
     * Priorityプロパティの定義体.
     */
    public static final EdmProperty.Builder P_PRIORITY = EdmProperty.newBuilder("Priority")
            .setType(EdmSimpleType.INT32)
            .setNullable(true)
            .setDefaultValue("3")
            .setAnnotations(Common.P_FORMAT_MESSAGE_PRIORITY);
    /**
     * RequestRelationプロパティの定義体.
     */
    public static final EdmProperty.Builder P_REQUEST_RELATION = EdmProperty.newBuilder("RequestRelation")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_MESSAGE_REQUEST_RELATION);
    /**
     * RequestRelationTargetプロパティの定義体.
     */
    public static final EdmProperty.Builder P_REQUEST_RELATION_TARGET = EdmProperty.newBuilder("RequestRelationTarget")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_URI);

    /**
     * Result/Toプロパティの定義体.
     */
    public static final EdmProperty.Builder P_RESULT_TO = EdmProperty.newBuilder("To")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_URI);
    /**
    * Result/Codeプロパティの定義体.
    */
   public static final EdmProperty.Builder P_RESULT_CODE = EdmProperty.newBuilder("Code")
           .setType(EdmSimpleType.STRING)
           .setNullable(true);
   /**
    * Result/Reasonプロパティの定義体.
    */
   public static final EdmProperty.Builder P_RESULT_REASON = EdmProperty.newBuilder("Reason")
           .setType(EdmSimpleType.STRING)
           .setNullable(true);

   /**
    * Resultプロパティの Builder.
    */
   public static final EdmComplexType.Builder COMPLEXTYPE_BUILDER = EdmComplexType.newBuilder()
           .setNamespace(Common.EDM_NS_CELL_CTL)
           .setName("Sent_Message_Result")
           .addProperties(Enumerable.create(P_RESULT_TO, P_RESULT_CODE, P_RESULT_REASON).toList());
    /**
     * Resultプロパティの定義体.
     */
    public static final EdmProperty.Builder P_RESULT = EdmProperty.newBuilder("Result")
            .setType(COMPLEXTYPE_BUILDER.build())
            .setCollectionKind(CollectionKind.List)
            .setNullable(true);

    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(Common.P_ID, P_BOX_NAME, P_IN_REPLY_TO, P_TO, P_TO_RELATION,
                            P_TYPE, P_TITLE, P_BODY, P_PRIORITY, P_REQUEST_RELATION, P_REQUEST_RELATION_TARGET,
                            P_RESULT, Common.P_PUBLISHED, Common.P_UPDATED).toList()).addKeys(Common.P_ID.getName());

}
