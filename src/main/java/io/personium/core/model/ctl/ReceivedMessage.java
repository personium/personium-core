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
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

/**
 * ReceivedMessageのEdm 定義体.
 */
public class ReceivedMessage {
    /**
     * コンストラクタ.
     */
    protected ReceivedMessage() {
    }

    /**
     * Edm EntityType名.
     */
    public static final String EDM_TYPE_NAME = "ReceivedMessage";

    /**
     * AccountとのNavigationProperty名.
     */
    public static final String EDM_NPNAME_FOR_ACCOUNT = "_AccountRead";

    /** Type message. */
    public static final String TYPE_MESSAGE = "message";
    /** Type register relation. */
    public static final String TYPE_REQ_RELATION_BUILD = "req.relation.build";
    /** Type delete relation. */
    public static final String TYPE_REQ_RELATION_BREAK = "req.relation.break";
    /** Type register relation role. */
    public static final String TYPE_REQ_ROLE_GRANT = "req.role.grant";
    /** Type delete relation role. */
    public static final String TYPE_REQ_ROLE_REVOKE = "req.role.revoke";

    /** ステータス 未読. */
    public static final String STATUS_UNREAD = "unread";
    /** ステータス 既読. */
    public static final String STATUS_READ = "read";
    /** ステータス 未承認. */
    public static final String STATUS_NONE = "none";
    /** ステータス 承認. */
    public static final String STATUS_APPROVED = "approved";
    /** ステータス 拒否. */
    public static final String STATUS_REJECTED = "rejected";

    /** メッセージコマンド. */
    public static final String MESSAGE_COMMAND = "Command";

    /**
     * __idプロパティの定義体.
     */
    public static final EdmProperty.Builder P_ID = EdmProperty.newBuilder("__id")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_IN_REPLY_TO);
    /**
     * _Box.Nameプロパティの定義体.
     */
    public static final EdmProperty.Builder P_BOX_NAME = EdmProperty.newBuilder("_Box.Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_NAME);
    /**
     * InReplyToプロパティの定義体.
     */
    public static final EdmProperty.Builder P_IN_REPLY_TO = EdmProperty.newBuilder("InReplyTo")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_IN_REPLY_TO);
    /**
     * Fromプロパティの定義体.
     */
    public static final EdmProperty.Builder P_FROM = EdmProperty.newBuilder("From")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_URI);
    /**
     * MulticastToプロパティの定義体.
     */
    public static final EdmProperty.Builder P_MULTICAST_TO = EdmProperty.newBuilder("MulticastTo")
            .setType(EdmSimpleType.STRING)
            .setNullable(true);
    /**
     * Typeプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TYPE = EdmProperty.newBuilder("Type")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_MESSAGE_TYPE);
    /**
     * Titleプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TITLE = EdmProperty.newBuilder("Title")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_MESSAGE_TITLE);
    /**
     * Bodyプロパティの定義体.
     */
    public static final EdmProperty.Builder P_BODY = EdmProperty.newBuilder("Body")
            .setType(EdmSimpleType.STRING)
            .setNullable(false);
    /**
     * Priorityプロパティの定義体.
     */
    public static final EdmProperty.Builder P_PRIORITY = EdmProperty.newBuilder("Priority")
            .setType(EdmSimpleType.INT32)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_MESSAGE_PRIORITY);
    /**
     * Statusプロパティの定義体.
     */
    public static final EdmProperty.Builder P_STATUS = EdmProperty.newBuilder("Status")
            .setType(EdmSimpleType.STRING)
            .setNullable(false);
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
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(P_ID, P_BOX_NAME, P_IN_REPLY_TO, P_FROM, P_MULTICAST_TO, P_TYPE, P_TITLE, P_BODY,
                            P_PRIORITY, P_STATUS, P_REQUEST_RELATION, P_REQUEST_RELATION_TARGET,
                            Common.P_PUBLISHED, Common.P_UPDATED).toList()).addKeys(Common.P_ID.getName());
}
