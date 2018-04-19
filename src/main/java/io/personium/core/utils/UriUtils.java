/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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
     * Convert scheme from LocalUnit to http.
     * @param unitUrl String
     * @param localUnitSchemeUrl String
     * @return Url with http scheme
     */
    public static String convertSchemeFromLocalUnitToHttp(String unitUrl, String localUnitSchemeUrl) {
        if (localUnitSchemeUrl != null && localUnitSchemeUrl.startsWith(SCHEME_LOCALUNIT)) {
            // SchemeLocalUnit(http://host/localunit/)
            return localUnitSchemeUrl.replace(SCHEME_UNIT_URI, unitUrl);
        }
        return localUnitSchemeUrl;
    }

    /**
     * Convert scheme from http to LocalUnit.
     * Convert only if the target URL matches UnitURL.
     * @param unitUrl UnitURL
     * @param url Target URL
     * @return Url with LocalUnit scheme
     */
    public static String convertSchemeFromHttpToLocalUnit(String unitUrl, String url) {
        if (url != null && url.startsWith(unitUrl)) {
            return url.replace(unitUrl, SCHEME_UNIT_URI);
        }
        return url;
    }

    /**
     * Convert scheme from LocalCell to http.
     * @param cellUrl String
     * @param localCellUrl String
     * @return Url with http scheme
     */
    public static String convertSchemeFromLocalCellToHttp(String cellUrl, String localCellUrl) {
        // cellUrl: http://host/cell/

        if (localCellUrl == null || !localCellUrl.startsWith(SCHEME_LOCALCELL)) {
            return null;
        }

        String[] parts = localCellUrl.split(":/", 2);
        if (parts.length != 2) {
            return null;
        }

        String retUrl = String.format("%s%s", cellUrl, parts[1]);

        return retUrl;
    }

    /**
     * Convert scheme from http to LocalCell.
     * @param cellUrl String
     * @param url String
     * @return url with personium-localcell scheme
     */
    public static String convertSchemeFromHttpToLocalCell(String cellUrl, String url) {
        if (url == null || !url.startsWith(cellUrl)) {
            return null;
        }

        String retUrl = url.replaceFirst(cellUrl, UriUtils.SCHEME_LOCALCELL + ":/");

        return retUrl;
    }

    /**
     * Convert scheme from http to LocalBox.
     * Convert only if the target URL matches BoxURL.
     * @param boxUrl boxURL
     * @param url Target URL
     * @return Url with LocalBox scheme
     */
    public static String convertSchemeFromHttpToLocalBox(String boxUrl, String url) {
        if (url != null && url.startsWith(boxUrl)) {
            return url.replaceFirst(boxUrl, UriUtils.SCHEME_LOCALBOX + ":/");
        }
        return url;
    }

    /**
     * Convert scheme from LocalBox to LocalCell.
     * @param boxUrl String
     * @param boxName String
     * @return url with personim-localcell scheme
     */
    public static String convertSchemeFromLocalBoxToLocalCell(String boxUrl, String boxName) {
        if (boxUrl == null || boxName == null) {
            return null;
        } else if (boxUrl.startsWith(SCHEME_LOCALCELL)) {
             return boxUrl;
        } else if (!boxUrl.startsWith(SCHEME_LOCALBOX)) {
            return null;
        }

        String[] parts = boxUrl.split(":/", 2);
        if (parts.length != 2) {
            return null;
        }

        String retUrl = String.format("%s:/%s/%s", SCHEME_LOCALCELL, boxName, parts[1]);

        return retUrl;
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
