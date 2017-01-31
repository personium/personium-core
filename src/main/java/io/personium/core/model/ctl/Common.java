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

import java.util.ArrayList;
import java.util.List;

import org.odata4j.core.PrefixedNamespace;
import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

import io.personium.common.utils.PersoniumCoreUtils;

/**
 * Edm 定義体で共通的に使う定数群を定義.
 */
public class Common {

    private Common() {
    }

    /**
     * UnitCtlの名前空間名.
     */
    public static final String EDM_NS_UNIT_CTL = "UnitCtl";
    /**
     * CellCtlの名前空間名.
     */
    public static final String EDM_NS_CELL_CTL = "CellCtl";
    /**
     * ODataSvcSchemaの名前空間名.
     */
    public static final String EDM_NS_ODATA_SVC_SCHEMA = "ODataSvcSchema";
    /**
     * Schema プロパティの定義体.
     */
    public static final EdmProperty.Builder P_SCHEMA = EdmProperty.newBuilder("Schema").setType(EdmSimpleType.STRING)
            .setNullable(true).setDefaultValue("null");
    /**
     * _Box.Nameプロパティの定義体.
     */
    public static final EdmProperty.Builder P_BOX_NAME = EdmProperty.newBuilder("_Box.Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(true);
    /**
     * id プロパティの定義体.
     */
    public static final EdmProperty.Builder P_ID = EdmProperty.newBuilder("__id")
            .setType(EdmSimpleType.STRING).setDefaultValue("UUID()");
    /** 拡張スキーマFormat定義. */
    public static final String DC_FORMAT = "Format";
    /** 拡張スキーマFormat正規表現定義. */
    public static final String DC_FORMAT_PATTERN_REGEX = "regEx";
    /** 拡張スキーマFormat定義. */
    public static final String DC_FORMAT_PATTERN_URI = "uri";
    /** 拡張スキーマFormat定義. */
    public static final String DC_FORMAT_PATTERN_SCHEMA_URI = "schema-uri";
    /** 拡張スキーマFormat定義. */
    public static final String DC_FORMAT_PATTERN_CELL_URL = "cell-url";
    /** 拡張スキーマFormat定義.1つ以上のスペース区切り英数字. */
    public static final String DC_FORMAT_PATTERN_USUSST = "unordered-set-of-unique-space-separated-tokens";
    /** 先頭が-,_以外で始まる半角英数大小文字,-,_が1文字から128文字. */
    public static final String PATTERN_NAME = "^[a-zA-Z0-9][a-zA-Z0-9-_]{0,127}$";
    /** 先頭が半角記号以外で始まる半角英数大小文字,半角記号(-_!#$%*+/=^`{|}~.@)が1文字から128文字. */
    public static final String PATTERN_NAME_WITH_SIGN = "^[a-zA-Z0-9][a-zA-Z0-9-_!$*=^`{|}~.@]{0,127}$";
    /** 先頭が_,:以外で始まる半角英数大小文字,-,_,+,:が1文字から128文字. */
    public static final String PATTERN_RELATION_NAME = "^[a-zA-Z0-9-\\+][a-zA-Z0-9-_\\+:]{0,127}$";
    /** multiplicityのFormat定義. */
    public static final String PATTERN_MULTIPLICITY = "0\\.\\.1|1|\\*";
    /** 先頭が-,_以外で始まる半角英数大小文字,-,_が1文字から200文字. */
    public static final String PATTERN_ID = "^[a-zA-Z0-9][a-zA-Z0-9-_:]{0,199}$";
    /** InReplyTo32文字. */
    public static final String PATTERN_IN_REPLY_TO = "^.{32}$";
    /** メッセージタイプ_messageまたはreq.relation.buildまたはreq.relation.break. */
    public static final String PATTERN_MESSAGE_TYPE = "^(message)|(req\\.relation\\.build)|(req\\.relation\\.break)$";
    /** メッセージタイトル0文字から256文字文字. */
    public static final String PATTERN_MESSAGE_TITLE = "^.{0,256}$";
    /** メッセージプライオリティ 1から5. */
    public static final String PATTERN_MESSAGE_PRIORITY = "^[1-5]$";
    /** メッセージの文字列型valueの最大長. */
    public static final int MAX_MESSAGE_BODY_LENGTH = 1024 * 64;

    /** ユーザデータのKeyのFormat定義. */
    public static final String PATTERN_USERDATA_KEY = "^[a-zA-Z0-9][a-zA-Z0-9-_]{0,127}$";
    /** ユーザデータの小数型valueのFormat定義. */
    public static final String PATTERN_DECIMAL = "^-?[0-9]{1,5}\\.[0-9]{1,5}$";
    /** ユーザデータの文字列型valueの最大長. */
    public static final int MAX_USERDATA_VALUE_LENGTH = 1024 * 50;
    /** qクエリの最大長. */
    public static final int MAX_Q_VALUE_LENGTH = 255;
    /** スキーマ定義で使用するシステム時間を指定するための予約語. */
    public static final String SYSUTCDATETIME = "SYSUTCDATETIME()";
    /** イベントの文字列型valueの最大長. */
    public static final int MAX_EVENT_VALUE_LENGTH = 1024 * 50;

    /**
     * Name項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_NAME = new ArrayList<EdmAnnotation<?>>();
    /**
     * 半角記号を許容する項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_NAME_WITH_SIGN = new ArrayList<EdmAnnotation<?>>();
    /**
     * RelationのName項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_RELATION_NAME = new ArrayList<EdmAnnotation<?>>();
    /**
     * dc:FormatのURI定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_URI = new ArrayList<EdmAnnotation<?>>();
    /**
     * dc:FormatのSchema URI定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_SCHEMA_URI = new ArrayList<EdmAnnotation<?>>();
    /**
     * dc:FormatのCell URL定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_CELL_URL = new ArrayList<EdmAnnotation<?>>();
    /**
     * Multiplicity項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_MULTIPLICITY = new ArrayList<EdmAnnotation<?>>();
    /**
     * ID項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_ID = new ArrayList<EdmAnnotation<?>>();
    /**
     * InReplyTo項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_IN_REPLY_TO = new ArrayList<EdmAnnotation<?>>();
    /**
     * MessageType項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_MESSAGE_TYPE = new ArrayList<EdmAnnotation<?>>();
    /**
     * MessageTitle項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_MESSAGE_TITLE = new ArrayList<EdmAnnotation<?>>();
    /**
     * MessagePriority項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_MESSAGE_PRIORITY = new ArrayList<EdmAnnotation<?>>();
    /**
     * AccountType項目に対するdc:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> DC_FORMAT_ACCOUNT_TYPE = new ArrayList<EdmAnnotation<?>>();

    /**
     * DC名前空間.
     */
    public static final PrefixedNamespace DC_NAMESPACE = new PrefixedNamespace(PersoniumCoreUtils.XmlConst.NS_PERSONIUM,
            PersoniumCoreUtils.XmlConst.NS_PREFIX_PERSONIUM);

    /**
     * published プロパティの定義体.
     */
    public static final EdmProperty.Builder P_PUBLISHED = EdmProperty.newBuilder("__published")
            .setType(EdmSimpleType.DATETIME).setDefaultValue(SYSUTCDATETIME).setPrecision(3);
    /**
     * updated プロパティの定義体.
     */
    public static final EdmProperty.Builder P_UPDATED = EdmProperty.newBuilder("__updated")
            .setType(EdmSimpleType.DATETIME).setDefaultValue(SYSUTCDATETIME).setPrecision(3);

    static {
        DC_FORMAT_NAME.add(createFormatNameAnnotation());
        DC_FORMAT_NAME_WITH_SIGN.add(createFormatNameWithSignAnnotation());
        DC_FORMAT_URI.add(createFormatUriAnnotation());
        DC_FORMAT_SCHEMA_URI.add(createFormatSchemaUriAnnotation());
        DC_FORMAT_CELL_URL.add(createFormatCellUrlAnnotation());
        DC_FORMAT_MULTIPLICITY.add(createFormatMultiplicityAnnotation());
        DC_FORMAT_ID.add(createFormatIdAnnotation());
        DC_FORMAT_RELATION_NAME.add(createFormatRelationNameAnnotation());
        DC_FORMAT_IN_REPLY_TO.add(createFormatInReplyToAnnotation());
        DC_FORMAT_MESSAGE_TYPE.add(createFormatMessageTypeAnnotation());
        DC_FORMAT_MESSAGE_TITLE.add(createFormatMessageTitleAnnotation());
        DC_FORMAT_MESSAGE_PRIORITY.add(createFormatMessagePriorityAnnotation());
        DC_FORMAT_ACCOUNT_TYPE.add(createFormatAccountTypeAnnotation());
    }

    /**
     * Name項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatNameAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME + "')");
    }

    /**
     * Name項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatNameWithSignAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME_WITH_SIGN + "')");
    }

    /**
     * RelationのName項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatRelationNameAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_RELATION_NAME + "')");
    }

    /**
     * dc:FormatのURIのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatUriAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_URI);
    }

    /**
     * dc:FormatのSchema URIのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatSchemaUriAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_SCHEMA_URI);
    }

    /**
     * dc:FormatのCell URLのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatCellUrlAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_CELL_URL);
    }

    /**
     * dc:FormatのMultiplicityのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatMultiplicityAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_MULTIPLICITY + "')");
    }

    /**
     * ID項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatIdAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_ID + "')");
    }

    /**
     * InReplyTo項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatInReplyToAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_IN_REPLY_TO + "')");
    }

    /**
     * MessageType項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatMessageTypeAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_MESSAGE_TYPE + "')");
    }

    /**
     * MessageTitle項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatMessageTitleAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_MESSAGE_TITLE + "')");
    }

    /**
     * Message Priority 項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatMessagePriorityAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_MESSAGE_PRIORITY + "')");
    }

    /**
     * Account Type 項目に対するdc:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    public static EdmAnnotation<?> createFormatAccountTypeAnnotation() {
        return new EdmAnnotationAttribute(
                DC_NAMESPACE.getUri(), DC_NAMESPACE.getPrefix(),
                DC_FORMAT, DC_FORMAT_PATTERN_USUSST + "('" + Account.TYPE_VALUE_BASIC + "', '"
                + Account.TYPE_VALUE_OIDC_GOOGLE + "')");
    }
}
