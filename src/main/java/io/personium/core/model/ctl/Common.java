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

import io.personium.common.utils.CommonUtils;

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
    /** Regular expression in cell name. */
    private static final String REGEX_CELL_NAME = "[a-z0-9][a-z0-9-]{0,127}";
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
    /** Regular expression in single ip address. */
    private static final String REGEX_SINGLE_IP_ADDRESS =
            "(([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])";
    /** Regular expression in single ip address range. */
    private static final String REGEX_SINGLE_IP_ADDRESS_RANGE =
            REGEX_SINGLE_IP_ADDRESS + "(/[1-9]|/[1-2][0-9]|/3[0-2])?";

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
    /** Pattern cell name. */
    public static final String PATTERN_CELL_NAME = "^" + REGEX_CELL_NAME + "$";
    /** Pattern relation name. */
    public static final String PATTERN_RELATION_NAME = "^" + REGEX_RELATION_NAME + "$";
    /** String containing "__relation/__/".<br>
     * Explanation of applicable group.<br>
     * $1:SchemaURL
     * $2:RelationName
     */
    public static final String PATTERN_RELATION_CLASS_URL = "(^.+)__relation/__/(" + REGEX_RELATION_NAME + ")/?$"; // CHECKSTYLE IGNORE - To maintain readability
    /** String containing "__role/__/".<br>
     * Explanation of applicable group.<br>
     * $1:SchemaURL
     * $2:RoleName
     */
    public static final String PATTERN_ROLE_CLASS_URL = "(^.+)__role/__/(" + REGEX_NAME + ")/?$";
    /**
     * Pattern cell path using personium-localunit "/$1/".<br>
     * Explanation of applicable group.<br>
     * $1:CellName
     */
    public static final String PATTERN_CELL_LOCALUNIT_PATH = "/(" + REGEX_NAME + ")/$";
    /** Pattern multiplicity. */
    public static final String PATTERN_MULTIPLICITY = "0\\.\\.1|1|\\*";
    /** Pattern id. */
    public static final String PATTERN_ID = "^" + REGEX_ID + "$";
    /** Pattern userdata key. */
    public static final String PATTERN_USERDATA_KEY = "^" + REGEX_USERDATA_KEY + "$";
    /** Pattern decimal. */
    public static final String PATTERN_DECIMAL = "^" + REGEX_DECIMAL + "$";
    /** Pattern single ip address. */
    public static final String PATTERN_SINGLE_IP_ADDRESS = "^" + REGEX_SINGLE_IP_ADDRESS + "$";
    /** Pattern single ip address range. */
    public static final String PATTERN_SINGLE_IP_ADDRESS_RANGE = "^" + REGEX_SINGLE_IP_ADDRESS_RANGE + "$";

    /** Max length of userdata. */
    public static final int MAX_USERDATA_VALUE_LENGTH = 1024 * 50;
    /** Max length of query. */
    public static final int MAX_Q_VALUE_LENGTH = 255;

    /** Reserved word for setting uuid. */
    public static final String UUID = "UUID()";
    /** Reserved word for setting system time. */
    public static final String SYSUTCDATETIME = "SYSUTCDATETIME()";

    /**
     * Definition of p: Format for Name field.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_NAME = new ArrayList<EdmAnnotation<?>>();
    /**
     * Definition of p: Format for items that allow single-byte symbols.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_NAME_WITH_SIGN = new ArrayList<EdmAnnotation<?>>();
    /**
     * Definition of p: Format for Cell Name field.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_CELL_NAME = new ArrayList<EdmAnnotation<?>>();
    /**
     * Definition of p: Format for Relation Name field.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_RELATION_NAME = new ArrayList<EdmAnnotation<?>>();
    /**
     * URI definition of p: Format.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_URI = new ArrayList<EdmAnnotation<?>>();
    /**
     * Cell URL definition of p: Format.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_CELL_URL = new ArrayList<EdmAnnotation<?>>();
    /**
     * Definition of p: Format for Multiplicity item.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_MULTIPLICITY = new ArrayList<EdmAnnotation<?>>();
    /**
     * Definition of p: Format for ID item.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_ID = new ArrayList<EdmAnnotation<?>>();
    /**
     * Definition of p: Format for AccountType item.
     */
    public static final List<EdmAnnotation<?>> P_FORMAT_ACCOUNT_TYPE = new ArrayList<EdmAnnotation<?>>();

    /**
     * DC namespace.
     */
    public static final PrefixedNamespace P_NAMESPACE = new PrefixedNamespace(CommonUtils.XmlConst.NS_PERSONIUM,
            CommonUtils.XmlConst.NS_PREFIX_PERSONIUM);

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

    /**
     * Url property.
     */
    public static final EdmProperty.Builder P_URL = EdmProperty.newBuilder("Url")
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_CELL_URL)
            .setType(EdmSimpleType.STRING);

    static {
        P_FORMAT_NAME.add(createFormatNameAnnotation());
        P_FORMAT_NAME_WITH_SIGN.add(createFormatNameWithSignAnnotation());
        P_FORMAT_CELL_NAME.add(createFormatCellNameAnnotation());
        P_FORMAT_URI.add(createFormatUriAnnotation());
        P_FORMAT_CELL_URL.add(createFormatCellUrlAnnotation());
        P_FORMAT_MULTIPLICITY.add(createFormatMultiplicityAnnotation());
        P_FORMAT_ID.add(createFormatIdAnnotation());
        P_FORMAT_RELATION_NAME.add(createFormatRelationNameAnnotation());
        P_FORMAT_ACCOUNT_TYPE.add(createFormatAccountTypeAnnotation());
    }

    /**
     * Return p: Format Annotation for Name item.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatNameAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME + "')");
    }

    /**
     * Return p: Format Annotation for Name item.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatNameWithSignAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME_WITH_SIGN + "')");
    }

    /**
     * Return p: Format Annotation for Cell Name item.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatCellNameAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_CELL_NAME + "')");
    }

    /**
     * Return p: Format Annotation for Relation Name item.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatRelationNameAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_RELATION_NAME + "')");
    }

    /**
     * Return Annotation of URI of p: Format.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatUriAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_URI);
    }

    /**
     * Return Annotation of Cell URL of p: Format.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatCellUrlAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_CELL_URL);
    }

    /**
     * Return Annotation of Multiplicity of p: Format.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatMultiplicityAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_MULTIPLICITY + "')");
    }

    /**
     * Return p: Format Annotation for ID item.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatIdAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_ID + "')");
    }

    /**
     * Return p: Format Annotation for Account Type item.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatAccountTypeAnnotation() {
        return new EdmAnnotationAttribute(
                P_NAMESPACE.getUri(), P_NAMESPACE.getPrefix(),
                P_FORMAT, P_FORMAT_PATTERN_USUSST);
    }
}
