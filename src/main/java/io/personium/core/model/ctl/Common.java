/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
 * Constant values commonly used in Edm.
 */
public class Common {

    /** Constructor. */
    private Common() {
    }

    /** Regular expression in generic name. */
    private static final String REGEX_NAME = "[a-zA-Z0-9][a-zA-Z0-9-_]{0,127}";
    /** Regular expression in name with sign. */
    private static final String REGEX_NAME_WITH_SIGN = "[a-zA-Z0-9][a-zA-Z0-9-_!$*=^`{|}~.@]{0,127}";
    /** Regular expression in snapshot file name. */
    private static final String REGEX_SNAPSHOT_NAME = "[a-zA-Z0-9-_]{1,192}";
    /** Regular expression in relation name. */
    private static final String REGEX_RELATION_NAME = "[a-zA-Z0-9-\\+][a-zA-Z0-9-_\\+:]{0,127}";
    /** Regular expression in id. */
    private static final String REGEX_ID = "[a-zA-Z0-9][a-zA-Z0-9-_:]{0,199}";
    /** Regular expression in userdata key. */
    private static final String REGEX_USERDATA_KEY = "[a-zA-Z0-9][a-zA-Z0-9-_]{0,127}";
    /** Regular exporession in decimal. */
    private static final String REGEX_DECIMAL = "-?[0-9]{1,5}\\.[0-9]{1,5}";

    /**
     * Namespace of UnitCtl.
     */
    public static final String EDM_NS_UNIT_CTL = "UnitCtl";
    /**
     * Namespace of CellCtl.
     */
    public static final String EDM_NS_CELL_CTL = "CellCtl";
    /**
     * Namespace of ODataSvcSchema.
     */
    public static final String EDM_NS_ODATA_SVC_SCHEMA = "ODataSvcSchema";

    /** Extended schema Format. */
    public static final String P_FORMAT = "Format";
    /** Extended schema Format regEx. */
    public static final String P_FORMAT_PATTERN_REGEX = "regEx";
    /** Extended schema Format uri. */
    public static final String P_FORMAT_PATTERN_URI = "uri";
    /** Extended schema Format cell-url. */
    public static final String P_FORMAT_PATTERN_CELL_URL = "cell-url";
    /** Extended schema Format USUSST. */
    public static final String P_FORMAT_PATTERN_USUSST = "unordered-set-of-unique-space-separated-tokens";

    /** Pattern generic name. */
    public static final String PATTERN_NAME = "^" + REGEX_NAME + "$";
    /** Pattern snapshot name. */
    public static final String PATTERN_SNAPSHOT_NAME = "^" + REGEX_SNAPSHOT_NAME + "$";
    /** Pattern name with sign. */
    public static final String PATTERN_NAME_WITH_SIGN = "^" + REGEX_NAME_WITH_SIGN + "$";
    /** Pattern relation name. */
    public static final String PATTERN_RELATION_NAME = "^" + REGEX_RELATION_NAME + "$";
    /**
     * Pattern relation class path "/$1/__relation/__/$2".<br>
     * Explanation of applicable group.<br>
     * $1:CellName
     * $2:RelationName
     */
    public static final String PATTERN_RELATION_CLASS_PATH = "/(" + REGEX_NAME + ")/__relation/__/(" + REGEX_RELATION_NAME + ")/?$"; // CHECKSTYLE IGNORE - To maintain readability
    /**
     * Pattern relation class url "$1/$2/__relation/__/$3".<br>
     * Explanation of applicable group.<br>
     * $1:BaseURL
     * $2:CellName
     * $3:RelationName
     */
    public static final String PATTERN_RELATION_CLASS_URL = "(^.+)" + PATTERN_RELATION_CLASS_PATH;
    /**
     * Pattern role class path "/$1/__role/__/$2".<br>
     * Explanation of applicable group.<br>
     * $1:CellName
     * $2:RoleName
     */
    public static final String PATTERN_ROLE_CLASS_PATH = "/(" + REGEX_NAME + ")/__role/__/(" + REGEX_NAME + ")/?$";
    /**
     * Pattern role class url "$1/$2/__role/__/$3".<br>
     * Explanation of applicable group.<br>
     * $1:BaseURL
     * $2:CellName
     * $3:RoleName
     */
    public static final String PATTERN_ROLE_CLASS_URL = "(^.+)" + PATTERN_ROLE_CLASS_PATH;
    /**
     * Pattern service path using personium-localbox "/$1/$2".<br>
     * Explanation of applicable group.<br>
     * $1:CollectionName
     * $2:ServiceName
     */
    public static final String PATTERN_SERVICE_LOCALBOX_PATH = "/(" + REGEX_NAME + ")/(" + REGEX_NAME + ")$";
    /**
     * Pattern service path using personium-localcell "/$1/$2/$3".<br>
     * Explanation of applicable group.<br>
     * $1:BoxName
     * $2:CollectionName
     * $3:ServiceName
     */
    public static final String PATTERN_SERVICE_LOCALCELL_PATH = "/(" + REGEX_NAME + "|__)" + PATTERN_SERVICE_LOCALBOX_PATH; // CHECKSTYLE IGNORE - To maintain readability
    /**
     * Pattern service path "/$1/$2/$3/$4".<br>
     * Explanation of applicable group.<br>
     * $1:CellName
     * $2:BoxName
     * $3:CollectionName
     * $4:ServiceName
     */
    public static final String PATTERN_SERVICE_PATH = "/(" + REGEX_NAME + ")" + PATTERN_SERVICE_LOCALCELL_PATH;
    /** Pattern multiplicity. */
    public static final String PATTERN_MULTIPLICITY = "0\\.\\.1|1|\\*";
    /** Pattern id. */
    public static final String PATTERN_ID = "^" + REGEX_ID + "$";
    /** Pattern userdata key. */
    public static final String PATTERN_USERDATA_KEY = "^" + REGEX_USERDATA_KEY + "$";
    /** Pattern decimal. */
    public static final String PATTERN_DECIMAL = "^" + REGEX_DECIMAL + "$";

    /** Max length of userdata. */
    public static final int MAX_USERDATA_VALUE_LENGTH = 1024 * 50;
    /** Max length of query. */
    public static final int MAX_Q_VALUE_LENGTH = 255;

    /** Reserved word for setting uuid. */
    public static final String UUID = "UUID()";
    /** Reserved word for setting system time. */
    public static final String SYSUTCDATETIME = "SYSUTCDATETIME()";

    /**
     * Name項目に対するp:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_NAME = new ArrayList<EdmAnnotation<?>>();
    /**
     * 半角記号を許容する項目に対するp:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_NAME_WITH_SIGN = new ArrayList<EdmAnnotation<?>>();
    /**
     * RelationのName項目に対するp:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_RELATION_NAME = new ArrayList<EdmAnnotation<?>>();
    /**
     * p:FormatのURI定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_URI = new ArrayList<EdmAnnotation<?>>();
    /**
     * p:FormatのCell URL定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_CELL_URL = new ArrayList<EdmAnnotation<?>>();
    /**
     * Multiplicity項目に対するp:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_MULTIPLICITY = new ArrayList<EdmAnnotation<?>>();
    /**
     * ID項目に対するp:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_ID = new ArrayList<EdmAnnotation<?>>();
    /**
     * AccountType項目に対するp:Formatの定義.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_ACCOUNT_TYPE = new ArrayList<EdmAnnotation<?>>();

    /**
     * DC名前空間.
     */
    public static final PrefixedNamespace P_NAMESPACE = new PrefixedNamespace(PersoniumCoreUtils.XmlConst.NS_PERSONIUM,
            PersoniumCoreUtils.XmlConst.NS_PREFIX_PERSONIUM);

    /**
     * Name property.
     */
    public static final EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(P_FORMAT_NAME);
    /**
     * _Box.Name property.
     */
    public static final EdmProperty.Builder P_BOX_NAME = EdmProperty.newBuilder("_Box.Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_NAME);
    /**
     * __id property.
     */
    public static final EdmProperty.Builder P_ID = EdmProperty.newBuilder("__id")
            .setType(EdmSimpleType.STRING).setDefaultValue(UUID);
    /**
     * __published property.
     */
    public static final EdmProperty.Builder P_PUBLISHED = EdmProperty.newBuilder("__published")
            .setType(EdmSimpleType.DATETIME).setDefaultValue(SYSUTCDATETIME).setPrecision(3);
    /**
     * __updated property.
     */
    public static final EdmProperty.Builder P_UPDATED = EdmProperty.newBuilder("__updated")
            .setType(EdmSimpleType.DATETIME).setDefaultValue(SYSUTCDATETIME).setPrecision(3);

    static {
        P_FORMAT_NAME.add(createFormatNameAnnotation());
        P_FORMAT_NAME_WITH_SIGN.add(createFormatNameWithSignAnnotation());
        P_FORMAT_URI.add(createFormatUriAnnotation());
        P_FORMAT_CELL_URL.add(createFormatCellUrlAnnotation());
        P_FORMAT_MULTIPLICITY.add(createFormatMultiplicityAnnotation());
        P_FORMAT_ID.add(createFormatIdAnnotation());
        P_FORMAT_RELATION_NAME.add(createFormatRelationNameAnnotation());
        P_FORMAT_ACCOUNT_TYPE.add(createFormatAccountTypeAnnotation());
    }

    /**
     * Name項目に対するp:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatNameAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME + "')");
    }

    /**
     * Name項目に対するp:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatNameWithSignAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME_WITH_SIGN + "')");
    }

    /**
     * RelationのName項目に対するp:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatRelationNameAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_RELATION_NAME + "')");
    }

    /**
     * p:FormatのURIのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatUriAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_URI);
    }

    /**
     * p:FormatのCell URLのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatCellUrlAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_CELL_URL);
    }

    /**
     * p:FormatのMultiplicityのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatMultiplicityAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_MULTIPLICITY + "')");
    }

    /**
     * ID項目に対するp:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatIdAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_ID + "')");
    }

    /**
     * Account Type 項目に対するp:FormatのAnnotationを返却.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatAccountTypeAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_USUSST + "('" + Account.TYPE_VALUE_BASIC + "', '"
                + Account.TYPE_VALUE_OIDC_GOOGLE + "')");
    }
}
