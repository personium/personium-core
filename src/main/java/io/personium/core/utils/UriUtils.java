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

import io.personium.core.PersoniumUnitConfig;

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
    /** LOCAL_CELL ADDITION. */
    public static final String SCHEME_CELL_URI = "personium-localcell:/";
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
            if (url.startsWith(SCHEME_UNIT_URI)) {
                url = convertSchemeFromLocalUnitToHttp(unitUrl, url);
            } else {
                url = convertSchemeFromHttpToLocalUnit(unitUrl, url);
            }
        }
        return url;
    }

    /**
     * Judge whether the argument's URL is personium-localunit and return it.
     * @param targetUrl target
     * @return true:localunit false:other
     */
    public static boolean isLocalUnitUrl(String targetUrl) {
        if (targetUrl != null && targetUrl.startsWith(SCHEME_LOCALUNIT)) {
            return true;
        }
        return false;
    }

    /**
     * Convert scheme from LocalUnit to http(s).
     * @param unitUrl unit url
     * @param localUnitSchemeUrl local unit url
     * @return url string with http(s) scheme
     */
    public static String convertSchemeFromLocalUnitToHttp(String unitUrl, String localUnitSchemeUrl) {
        if (localUnitSchemeUrl != null && localUnitSchemeUrl.startsWith(SCHEME_UNIT_URI)) {
            String pathBased = localUnitSchemeUrl.replaceFirst(SCHEME_UNIT_URI, unitUrl);
            if (PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
                return pathBased;
            } else {
                try {
                    return convertPathBaseToFqdnBase(pathBased);
                } catch (URISyntaxException e) {
                    return localUnitSchemeUrl;
                }
            }
        }
        return localUnitSchemeUrl;
    }

    /**
     * Convert scheme from http(s) to LocalUnit.
     * Convert only if the target URL matches UnitURL.
     * @param unitUrl unit url
     * @param url target url
     * @return url string with local unit scheme
     */
    public static String convertSchemeFromHttpToLocalUnit(String unitUrl, String url) {
        if (url == null) {
            return url;
        }
        if (url.startsWith(unitUrl)) {
            return url.replaceFirst(unitUrl, SCHEME_UNIT_URI);
        }

        // convert to path based url
        String pathBased;
        try {
            pathBased = convertFqdnBaseToPathBase(url);
        } catch (URISyntaxException e) {
            return url;
        }

        if (pathBased != null && pathBased.startsWith(unitUrl)) {
            return pathBased.replaceFirst(unitUrl, SCHEME_UNIT_URI);
        }

        return url;
    }

    /**
     * Convert scheme from LocalCell to http(s).
     * @param cellUrl cell url string
     * @param localCellUrl local cell url
     * @return url string with http(s) scheme
     */
    public static String convertSchemeFromLocalCellToHttp(String cellUrl, String localCellUrl) {
        if (localCellUrl != null && localCellUrl.startsWith(SCHEME_CELL_URI)) {
            return localCellUrl.replaceFirst(SCHEME_CELL_URI, cellUrl);
        }
        return localCellUrl;
    }

    /**
     * Convert scheme from http(s) to LocalCell.
     * @param cellUrl cell url string
     * @param url target url string
     * @return url string with local cell scheme
     */
    public static String convertSchemeFromHttpToLocalCell(String cellUrl, String url) {
        if (url != null && url.startsWith(cellUrl)) {
            return url.replaceFirst(cellUrl, SCHEME_CELL_URI);
        }
        return url;
    }

    /**
     * Convert scheme from LocalBox to http(s).
     * @param boxUrl box url string
     * @param localBoxUrl local box url
     * @return url string with http(s) scheme
     */
    public static String convertSchemeFromLocalBoxToHttp(String boxUrl, String localBoxUrl) {
        if (localBoxUrl != null && localBoxUrl.startsWith(SCHEME_BOX_URI)) {
            return localBoxUrl.replaceFirst(SCHEME_BOX_URI, boxUrl);
        }
        return localBoxUrl;
    }

    /**
     * Convert scheme from http(s) to LocalBox.
     * Convert only if the target URL matches BoxURL.
     * @param boxUrl boxURL
     * @param url Target URL
     * @return Url with LocalBox scheme
     */
    public static String convertSchemeFromHttpToLocalBox(String boxUrl, String url) {
        if (url != null && url.startsWith(boxUrl)) {
            return url.replaceFirst(boxUrl, SCHEME_BOX_URI);
        }
        return url;
    }

    /**
     * Convert scheme from LocalBox to LocalCell.
     * @param boxUrl String
     * @param boxName String
     * @return url with localcell scheme
     */
    public static String convertSchemeFromLocalBoxToLocalCell(String boxUrl, String boxName) {
        if (boxUrl != null && boxName != null && boxUrl.startsWith(SCHEME_BOX_URI)) {
            String localCell = SCHEME_CELL_URI + boxName + "/";
            return boxUrl.replaceFirst(SCHEME_BOX_URI, localCell);
        }
        return boxUrl;
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
        if (uri.getHost() == null) {
            return convertedUrl;
        }

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
        if (uri.getHost() == null) {
            return convertedUrl;
        }

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
