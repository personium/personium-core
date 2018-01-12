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
package io.personium.core.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.core4j.Enumerable;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * OData系ユーティリティ関数を集めたクラス.
 */
public final class ODataUtils {
    static Logger log = LoggerFactory.getLogger(ODataUtils.class);

    /** URI最大長. */
    static final int URI_MAX_LENGTH = 1024;
    /** DateTime型の最小値(1753-01-01 T00:00:00.000). */
    public static final long DATETIME_MIN = -6847804800000L;
    /** DateTime型の最大値(9999-12-31 T23:59:59.999). */
    public static final long DATETIME_MAX = 253402300799999L;

    /** ODataで定義されているEdm.Double型の正の最小値. */
    public static final double DOUBLE_POSITIVE_MIN_VALUE = 2.23e-308;
    /** ODataで定義されているEdm.Double型の正の最大値. */
    public static final double DOUBLE_POSITIVE_MAX_VALUE = 1.79e+308;

    /** ODataで定義されているEdm.Double型の負の最小値. */
    public static final double DOUBLE_NEGATIVE_MIN_VALUE = -1.79e+308;
    /** ODataで定義されているEdm.Double型の負の最大値. */
    public static final double DOUBLE_NEGATIVE_MAX_VALUE = -2.23e-308;

    private ODataUtils() {
    }

    /**
     * スキーマからCardinalityを調べるユーティリティ.
     */
    public static class Cardinality {
        /**
         * 多対多.
         */
        public static final int MANY_MANY = 4;
        /**
         * 多対一.
         */
        public static final int MANY_ONE = 3;
        /**
         * 一対多.
         */
        public static final int ONE_MANY = 2;
        /**
         * 一対一.
         */
        public static final int ONE_ONE = 1;

        /**
         * @param navProp EdmNavigationProperty
         * @return Cardinalityの定数 (MANY_MANY等）
         */
        public static int forEdmNavigationProperty(EdmNavigationProperty navProp) {
            EdmMultiplicity fromM = navProp.getFromRole().getMultiplicity();
            EdmMultiplicity toM = navProp.getToRole().getMultiplicity();
            if (EdmMultiplicity.MANY.equals(fromM)) {
                if (EdmMultiplicity.MANY.equals(toM)) {
                    // NN
                    return MANY_MANY;
                } else {
                    // N1
                    return MANY_ONE;
                }
            } else {
                if (EdmMultiplicity.MANY.equals(toM)) {
                    // 1N
                    return ONE_MANY;
                } else {
                    // 11
                    return ONE_ONE;
                }
            }
        }
    }

    /**
     * If-MatchヘッダとEtagの値が等しいかチェック.
     * @param etag Etag
     * @param oedhExisting 存在するドキュメント情報
     */
    public static void checkEtag(final String etag, EntitySetDocHandler oedhExisting) {

        if (etag == null) {
            return;
        }

        // IfMatchヘッダに「*」が指定されている場合は無条件に実行
        if ("*".equals(etag)) {
            return;
        }

        // IfMatchヘッダのEtagからバージョンとUpdatedの値を取得
        long ifMatchVersion = 0;
        long ifMatchUpdated = 0;
        Pattern pattern = Pattern.compile("(^[0-9]+)-([0-9]+)");
        Matcher m = pattern.matcher(etag);
        try {
            ifMatchVersion = Long.parseLong(m.replaceAll("$1"));
            ifMatchUpdated = Long.parseLong(m.replaceAll("$2"));
        } catch (NumberFormatException e) {
            throw PersoniumCoreException.OData.ETAG_NOT_MATCH.reason(e);
        }
        // バージョンチェック
        if (ifMatchVersion != oedhExisting.getVersion() || ifMatchUpdated != oedhExisting.getUpdated()) {
            throw PersoniumCoreException.OData.ETAG_NOT_MATCH;
        }
    }

    /**
     * 引数で与えられたMapをマージする.
     * @param baseProperty マージのベースにするプロパティ群
     * @param addProperty マージで更新するプロパティ群
     * @return マージ結果
     */
    public static Map<String, Object> getMergeFields(Map<String, Object> baseProperty,
            Map<String, Object> addProperty) {
        Map<String, Object> mergeFields = new HashMap<String, Object>();
        mergeFields.putAll(baseProperty);
        addComplexPropertyMergeFields(mergeFields, addProperty);
        return mergeFields;
    }

    @SuppressWarnings("unchecked")
    private static void addComplexPropertyMergeFields(Map<String, Object> baseProperty,
            Map<String, Object> addProperty) {
        for (Map.Entry<String, Object> property : addProperty.entrySet()) {
            String key = property.getKey();
            Object value = property.getValue();
            if (!(value instanceof Map)) {
                baseProperty.put(key, value);
            } else {
                Map<String, Object> nestMap = (Map<String, Object>) baseProperty.get(key);
                if (nestMap == null) { // MERGE前の値がnullであった場合にはMERGEしようとしている値をputできない→空のHashMapを作成
                    nestMap = new HashMap<String, Object>();
                    baseProperty.put(key, nestMap);
                }
                addComplexPropertyMergeFields(nestMap, (Map<String, Object>) value);
            }
        }
    }

    /**
     * Stringの値チェック.
     * @param value チェック対象値
     * @return boolean
     */
    public static boolean validateString(String value) {
        if (value.getBytes().length > Common.MAX_USERDATA_VALUE_LENGTH) {
            return false;
        }
        return true;
    }

    /**
     * Booleanの値チェック.
     * @param value チェック対象値
     * @return boolean
     */
    public static boolean validateBoolean(String value) {
        if (!value.equals("true") && !value.equals("false")) {
            return false;
        }
        return true;
    }

    /**
     * Int32の値チェック.
     * @param value チェック対象値
     * @return boolean
     */
    public static boolean validateInt32(String value) {
        try {
            // 整数型
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * DateTimeの値チェック.
     * @param value チェック対象値
     * @return boolean
     */
    public static boolean validateDateTime(String value) {
        // DateTimeのチェックをする
        // SYSUTCDATETIME() または、/Date(【long型】)/
        try {
            if (Common.SYSUTCDATETIME.equals(value)) {
                return true;
            }
            Pattern pattern = Pattern.compile("^/Date\\((.+)\\)/$");
            Matcher match = pattern.matcher(value);
            if (match.matches()) {
                String date = match.replaceAll("$1");
                long longDate = Long.parseLong(date);
                if (longDate < DATETIME_MIN || longDate > DATETIME_MAX) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Singleの値チェック.
     * @param value チェック対象値
     * @return boolean
     */
    public static boolean validateSingle(String value) {
        try {
            if (value.contains(".")) {
                // 小数型
                Pattern pattern = Pattern.compile(Common.PATTERN_DECIMAL);
                Matcher matcher = pattern.matcher(value);
                if (!matcher.matches()) {
                    return false;
                }
            }
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * ODataで定義されているDoubleの有効範囲値チェック.
     * 有効範囲 ± 2.23e -308 から ± 1.79e +308
     * @param value チェック対象値
     * @return boolean
     */
    public static boolean validateDouble(String value) {
        try {
            Double doubleValue = Double.parseDouble(value);
            return validateDouble(doubleValue);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * ODataで定義されているDoubleの有効範囲値チェック.
     * 有効範囲 ± 2.23e -308 から ± 1.79e +308
     * @param value チェック対象値
     * @return boolean
     */
    public static boolean validateDouble(Double value) {
        if (0 == value
                || (DOUBLE_NEGATIVE_MIN_VALUE <= value //NOPMD -To maintain readability
                && value <= DOUBLE_NEGATIVE_MAX_VALUE)
                || (DOUBLE_POSITIVE_MIN_VALUE <= value //NOPMD
                && value <= DOUBLE_POSITIVE_MAX_VALUE)) {
            return true;
        }
        return false;
    }

    /**
     * Check the value of property item with regular expression.
     * @param str Input string
     * @param pFormat regular expression format
     * @return true:OK false:NG
     */
    public static boolean validateRegEx(String str, String pFormat) {
        // Check
        Pattern pattern = Pattern.compile(pFormat);
        Matcher matcher = pattern.matcher(str);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }

    // scheme check
    private static boolean isValidUriScheme(String scheme) {
        return scheme != null
                && (scheme.equals(UriUtils.SCHEME_HTTP)
                        || scheme.equals(UriUtils.SCHEME_HTTPS)
                        || scheme.equals(UriUtils.SCHEME_URN)
                        || scheme.equals(UriUtils.SCHEME_LOCALUNIT));
    }

    private static boolean isValidUrnScheme(String scheme) {
        return scheme != null
                && scheme.equals(UriUtils.SCHEME_URN);
    }

    private static boolean isValidCellUrlScheme(String scheme) {
        return scheme != null
                && (scheme.equals(UriUtils.SCHEME_HTTP)
                        || scheme.equals(UriUtils.SCHEME_HTTPS)
                        || scheme.equals(UriUtils.SCHEME_LOCALUNIT));
    }

    private static boolean isValidSchemaUrlScheme(String scheme) {
        return isValidUrnScheme(scheme) || isValidCellUrlScheme(scheme);
    }

    private static boolean isValidLocalUnitUrlScheme(String scheme) {
        return scheme != null
                && UriUtils.SCHEME_LOCALUNIT.equals(scheme);
    }

    private static boolean isValidLocalCellUrlScheme(String scheme) {
        return scheme != null
                && UriUtils.SCHEME_LOCALCELL.equals(scheme);
    }

    private static boolean isValidLocalBoxUrlScheme(String scheme) {
        return scheme != null
                && UriUtils.SCHEME_LOCALBOX.equals(scheme);
    }

    /**
     * Check if string is valid Uri.
     * @param str Input string
     * @return true if valid
     */
    public static boolean isValidUri(String str) {
        boolean isValidLength = str.length() <= URI_MAX_LENGTH;
        URI uri;
        try {
            uri = new URI(str);
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        boolean isValidScheme = isValidUriScheme(scheme);
        return isValidLength && isValidScheme;
    }

    /**
     * Check if string is valid Urn.
     * @param str Input string
     * @return true if valid
     */
    public static boolean isValidUrn(String str) {
        boolean isValidLength = str.length() <= URI_MAX_LENGTH;
        URI uri;
        try {
            uri = new URI(str);
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        boolean isValidScheme = isValidUrnScheme(scheme);
        return isValidLength && isValidScheme;
    }

    /**
     * Check if string is valid Cell URL.
     * @param str Input string
     * @return true if valid
     */
    public static boolean isValidCellUrl(String str) {
        boolean isValidLength = str.length() <= URI_MAX_LENGTH;
        URI uri;
        try {
            uri = new URI(str);
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        boolean isValidScheme = isValidCellUrlScheme(scheme);
        boolean isNormalized = uri.normalize().toString().equals(str);
        boolean hasTrailingSlash = str.endsWith("/");
        return isValidLength && isValidScheme && isNormalized && hasTrailingSlash;
    }

    /**
     * Check if string is valid Schema URI.
     * @param str Input string
     * @return true if valid
     */
    public static boolean isValidSchemaUri(String str) {
        return isValidUrn(str) || isValidCellUrl(str);
    }

    /**
     * Check if string is valid Local Cell or Box URL.
     * @param str Input string
     * @return true if valid
     */
    public static boolean isValidLocalCellOrBoxUrl(String str) {
        boolean isValidLength = str.length() <= URI_MAX_LENGTH;
        URI uri;
        try {
            uri = new URI(str);
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        boolean isValidScheme = isValidLocalCellUrlScheme(scheme) || isValidLocalBoxUrlScheme(scheme);
        boolean isNormalized = uri.normalize().toString().equals(str);
        return isValidLength && isValidScheme && isNormalized;
    }

    private static String getPath(URI uri) {
        String path = null;
        if (uri.getPath() != null) {
            path = uri.getPath();
        }
        if (uri.getQuery() != null) {
            path += "?" + uri.getQuery();
        }

        return path;
    }

    /**
     * Check the value of property item with LocalBox URL.
     * @param str Input string
     * @param pFormat regular expression format
     * @return true:OK false:NG
     */
    public static boolean validateLocalBoxUrl(String str, String pFormat) {
        URI uri;
        try {
            uri = new URI(str);
            String scheme = uri.getScheme();
            // Scheme check
            if (!isValidLocalBoxUrlScheme(scheme)) {
                return false;
            }
            // String length check
            if (uri.toString().length() > URI_MAX_LENGTH) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        String path = getPath(uri);
        if (path == null) {
            return false;
        }
        log.debug("path is " + path);
        // Regular expression check
        return ODataUtils.validateRegEx(path, pFormat);
    }

    /**
     * Check the value of property item with LocalCell URL.
     * @param str Input string
     * @param pFormat regular expression format
     * @return true:OK false:NG
     */
    public static boolean validateLocalCellUrl(String str, String pFormat) {
        URI uri;
        try {
            uri = new URI(str);
            String scheme = uri.getScheme();
            // Scheme check
            if (!isValidLocalCellUrlScheme(scheme)) {
                return false;
            }
            // String length check
            if (uri.toString().length() > URI_MAX_LENGTH) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        String path = getPath(uri);
        if (path == null) {
            return false;
        }
        log.debug("path is " + path);
        // Regular expression check
        return ODataUtils.validateRegEx(path, pFormat);
    }

    /**
     * Check the value of property item with LocalUnit URL.
     * @param str Input string
     * @param pFormat regular expression format
     * @return true:OK false:NG
     */
    public static boolean validateLocalUnitUrl(String str, String pFormat) {
        URI uri;
        try {
            uri = new URI(str);
            String scheme = uri.getScheme();
            // Scheme check
            if (!isValidLocalUnitUrlScheme(scheme)) {
                return false;
            }
            // String length check
            if (uri.toString().length() > URI_MAX_LENGTH) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        String path = getPath(uri);
        if (path == null) {
            return false;
        }
        log.debug("path is " + path);
        // Regular expression check
        return ODataUtils.validateRegEx(path, pFormat);
    }

    /**
     * Check the value of property item with Class URL.
     * @param str Input string
     * @param pFormat regular expression format
     * @return true:OK false:NG
     */
    public static boolean validateClassUrl(String str, String pFormat) {
        URI uri;
        try {
            uri = new URI(str);
            String scheme = uri.getScheme();
            // Scheme check
            if (!isValidSchemaUrlScheme(scheme)) {
                return false;
            }
            // String length check
            if (uri.toString().length() > URI_MAX_LENGTH) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        // Regular expression check
        return ODataUtils.validateRegEx(str, pFormat);
    }

    /**
     * スキーマ定義されたプロパティ数を取得する.
     * @param metadata スキーマ情報
     * @param entitySetName 対象のエンティティセット名
     * @return プロパティ数
     */
    public static int getStaticPropertyCount(EdmDataServices metadata, String entitySetName) {
        EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(entitySetName);
        EdmEntityType edmEntityType = edmEntitySet.getType();
        return getStaticPropertyCount(metadata, edmEntityType.getProperties());
    }

    private static int getStaticPropertyCount(EdmDataServices metadata, Enumerable<EdmProperty> properties) {
        int count = 0;
        for (EdmProperty ep : properties) {
            count++;
            if (!ep.getType().isSimple()) {
                EdmComplexType edmComplexType =
                        metadata.findEdmComplexType(ep.getType().getFullyQualifiedTypeName());
                count += getStaticPropertyCount(metadata, edmComplexType.getProperties());
            }
        }
        return count;
    }

}
