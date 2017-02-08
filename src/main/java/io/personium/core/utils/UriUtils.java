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
package io.personium.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Scheme Utilities.
 * @author fjqs
 *
 */
public class UriUtils {
    /** PRTCOL HTTP. */
    public static final String SCHEME_HTTP = "http";
    /** PRTCOL HTTPS. */
    public static final String SCHEME_HTTPS = "https";
    /** SCHEME URN. */
    public static final String SCHEME_URN = "urn";
    /** LOCAL_UNIT. */
    public static final String SCHEME_LOCALUNIT = "personium-localunit";
    /** LOCAL_CELL. */
    public static final String SCHEME_LOCALCELL = "personium-localcell";
    /** LOCAL_BOX. */
    public static final String SCHEME_LOCALBOX = "personium-localbox";

    /** LOCAL_UNIT ADDITION. */
    public static final String SCHEME_UNIT_URI = "personium-localunit:/";

    /** SLASH. */
    public static final String STRING_SLASH = "/";

    /**
     * constructor.
     */
    private UriUtils() {
    }

    /**
     * Get Url Variations.
     * @param unitUrl String
     * @param url String
     * @return ArrayList<String>
     */
    public static List<String> getUrlVariations(String unitUrl, String url) {
        List<String> variations = new ArrayList<String>();
        variations.add(url);
        if (url != null && unitUrl != null) {
            String substitute = getUrlSubstitute(unitUrl, url);
            if (!url.equals(substitute)) {
                variations.add(substitute);
            }
        }
        return variations;
    }

    /**
     * getUrlSubstitute.
     * @param unitUrl String
     * @param url String
     * @return utl String
     */
    public static String getUrlSubstitute(String unitUrl, String url) {
        if (url != null && unitUrl != null) {
            if (url.startsWith(unitUrl)) {
                // FullHttpUrlOnThisUnit(personium-localunit:/)
                url = url.replace(unitUrl, SCHEME_UNIT_URI);
            } else {
                url = convertSchemeFromLocalUnitToHttp(unitUrl, url);
            }
        }
        return url;
    }

    /**
     * convert Scheme from LocalUnit to http.
     * @param unitUrl String
     * @param localUnitSchemeUrl String
     * @return Url with http scheme
     */
    public static String convertSchemeFromLocalUnitToHttp(String unitUrl, String localUnitSchemeUrl) {
        if ((localUnitSchemeUrl != null) && (localUnitSchemeUrl.startsWith(SCHEME_LOCALUNIT))) {
            // SchemeLocalUnit(http://host/localunit/)
            return localUnitSchemeUrl.replace(SCHEME_UNIT_URI, unitUrl);
        }
        return localUnitSchemeUrl;
    }

    /**
     * getUnitUrl.
     * @param cellUrl String
     * @return url String
     */
    public static String getUnitUrl(String cellUrl) {
        return getUnitUrl(cellUrl, 1);
    }

    /**
     * getUnitUrl.
     * @param cellUrl String
     * @param index int from last
     * @return url String
     */
    public static String getUnitUrl(String cellUrl, int index) {
        String[] list = cellUrl.split(STRING_SLASH);
        // 指定文字が最後から指定数で発見された文字より前の文字を切り出す
        return StringUtils.substringBeforeLast(cellUrl, list[list.length - index]);
    }
}
