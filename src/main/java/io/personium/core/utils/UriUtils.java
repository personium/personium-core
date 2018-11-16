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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /** LOCAL_BOX ADDITION. */
    public static final String SCHEME_BOX_URI = "personium-localbox:/";

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
            return localCellUrl;
        }

        String[] parts = localCellUrl.split(":/", 2);
        if (parts.length != 2) {
            return localCellUrl;
        }

        return new StringBuilder(cellUrl).append(parts[1]).toString();
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

        return new StringBuilder(SCHEME_LOCALCELL)
                .append(":/")
                .append(boxName)
                .append("/")
                .append(parts[1])
                .toString();
    }

    /**
     * "https://{cellname}.{domain}/..." to "https://{domain}/{cellname}/...".
     * @param sourceUrl Source url
     * @return Converted url
     * @throws URISyntaxException Source url is not URI
     */
    public static String convertFqdnBaseToPathBase(String sourceUrl) throws URISyntaxException {
        String convertedUrl = sourceUrl;
        URI uri = new URI(sourceUrl);

        String cellName = uri.getHost().split("\\.")[0];
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append("/").append(cellName).append(uri.getPath());

        String domain = uri.getHost().replaceFirst(cellName + "\\.", "");

        UriBuilder uriBuilder = UriBuilder.fromUri(uri);
        uriBuilder.host(domain);
        uriBuilder.replacePath(pathBuilder.toString());
        convertedUrl = uriBuilder.build().toString();
        return convertedUrl;
    }

    static Logger log = LoggerFactory.getLogger(UriUtils.class);

    /**
     * "https://{domain}/{cellname}/..." to "https://{cellname}.{domain}/...".
     * @param sourceUrl Source url
     * @return Converted url
     * @throws URISyntaxException Source url is not URI
     */
    public static String convertPathBaseToFqdnBase(String sourceUrl) throws URISyntaxException {
        String convertedUrl = sourceUrl;
        URI uri = new URI(sourceUrl);

        String domain = uri.getHost();
        String cellName = uri.getPath().split("/")[1];
        StringBuilder hostBuilder = new StringBuilder();
        hostBuilder.append(cellName).append(".").append(domain);
        StringBuilder cellPathBuilder = new StringBuilder();
        cellPathBuilder.append("/").append(cellName);
        String path = uri.getPath().replaceFirst(cellPathBuilder.toString(), "");

        UriBuilder uriBuilder = UriBuilder.fromUri(uri);
        uriBuilder.host(hostBuilder.toString());
        uriBuilder.replacePath(path);
        convertedUrl = uriBuilder.build().toString();
        return convertedUrl;
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
        //Cut out the character before the character whose specified character was found by the specified number from the end
        return StringUtils.substringBeforeLast(cellUrl, list[list.length - index]);
    }

    /**
     * Creates and returns a UriInfo object with an arbitrary BaseUri.
     * @param uriInfo UriInfo
     * @param baseLevelsAbove How many layers above BaseUri from RequestUri
     * @return UriInfo
     */
    public static UriInfo createUriInfo(final UriInfo uriInfo, final int baseLevelsAbove) {
        PersoniumUriInfo ret = new PersoniumUriInfo(uriInfo, baseLevelsAbove, null);
        return ret;
    }

    /**
     * Creates and returns a UriInfo object with an arbitrary BaseUri.
     * @param uriInfo UriInfo
     * @param baseLevelsAbove How many layers above BaseUri from RequestUri
     * @param add Additional path information
     * @return UriInfo
     */
    public static UriInfo createUriInfo(final UriInfo uriInfo, final int baseLevelsAbove, final String add) {
        PersoniumUriInfo ret = new PersoniumUriInfo(uriInfo, baseLevelsAbove, add);
        return ret;
    }

    /**
     * Wrapper of UriInfo that behaves as UriInfo with BaseUri (root) on the specified hierarchical level.
     */
    public static final class PersoniumUriInfo implements UriInfo {
        UriBuilder baseUriBuilder;
        UriInfo core;

        /**
         * Constructor.
         * @param uriInfo UriInfo
         * @param baseLevelsAbove How many hierarchical paths to route to
         * @param add Additional path information
         */
        public PersoniumUriInfo(final UriInfo uriInfo, final int baseLevelsAbove, final String add) {
            this.core = uriInfo;
            String reqUrl = uriInfo.getRequestUri().toASCIIString();
            if (reqUrl.endsWith("/")) {
                reqUrl = reqUrl.substring(0, reqUrl.length() - 1);
            }
            String[] urlSplitted = reqUrl.split("/");
            urlSplitted = (String[]) ArrayUtils.subarray(urlSplitted, 0, urlSplitted.length - baseLevelsAbove);
            reqUrl = StringUtils.join(urlSplitted, "/") + "/";
            if (add != null && add.length() != 0) {
                reqUrl = reqUrl + add + "/";
            }
            this.baseUriBuilder = UriBuilder.fromUri(reqUrl);
        }

        @Override
        public String getPath() {
            return this.getPath(true);
        }

        @Override
        public String getPath(final boolean decode) {
            String sReq = null;
            String sBas = null;
            if (decode) {
                sReq = this.getRequestUri().toString();
                sBas = this.getBaseUri().toString();
            } else {
                sReq = this.getRequestUri().toASCIIString();
                sBas = this.getBaseUri().toASCIIString();
            }
            return sReq.substring(sBas.length());
        }

        @Override
        public List<PathSegment> getPathSegments() {
            return this.core.getPathSegments();
        }

        @Override
        public List<PathSegment> getPathSegments(final boolean decode) {
            return this.core.getPathSegments(decode);
        }

        @Override
        public URI getRequestUri() {
            return this.core.getRequestUri();
        }

        @Override
        public UriBuilder getRequestUriBuilder() {
            return this.core.getRequestUriBuilder();
        }

        @Override
        public URI getAbsolutePath() {
            return this.core.getAbsolutePath();
        }

        @Override
        public UriBuilder getAbsolutePathBuilder() {
            return this.core.getAbsolutePathBuilder();
        }

        @Override
        public URI getBaseUri() {
            return this.baseUriBuilder.build();
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            return this.baseUriBuilder;
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters() {
            return this.core.getPathParameters();
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(final boolean decode) {
            return this.core.getPathParameters(decode);
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            return this.core.getQueryParameters();
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(final boolean decode) {
            return this.core.getQueryParameters(decode);
        }

        @Override
        public List<String> getMatchedURIs() {
            return this.core.getMatchedURIs();
        }

        @Override
        public List<String> getMatchedURIs(final boolean decode) {
            return this.core.getMatchedURIs(decode);
        }

        @Override
        public List<Object> getMatchedResources() {
            return this.core.getMatchedResources();
        }

        @Override
        public URI resolve(URI uri) {
            return this.core.resolve(uri);
        }

        @Override
        public URI relativize(URI uri) {
            return this.core.relativize(uri);
        }
    }
}
