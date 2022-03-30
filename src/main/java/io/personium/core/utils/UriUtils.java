/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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

import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALBOX;
import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALCELL;
import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALUNIT;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;

/**
 * Utilities for handling URIs with personium-* scheme.
 * @author fjqs, shimono
 *
 */
public class UriUtils {
    /** Scheme string, "http". */
    public static final String SCHEME_HTTP = "http";
    /** Scheme string, "https". */
    public static final String SCHEME_HTTPS = "https";
    /** Scheme string, "urn". */
    public static final String SCHEME_URN = "urn";

    /** LOCAL_CELL ADDITION. */
    public static final String SCHEME_CELL_URI = SCHEME_LOCALCELL + ":/";
    /** LOCAL_BOX ADDITION. */
    public static final String SCHEME_BOX_URI = SCHEME_LOCALBOX + ":/";


    /** Regular expression for matching localunit scheme with single colon. */
    public static final Pattern REGEX_LOCALUNIT_SINGLE_COLON
        = Pattern.compile("^" + SCHEME_LOCALUNIT + ":(.*)$");

    /** Regular expression for matching localunit scheme with double colons. */
    public static final Pattern REGEX_LOCALUNIT_DOUBLE_COLONS
        = Pattern.compile("^" + SCHEME_LOCALUNIT + ":(.+?):(.*)$");

    /** Regular expression for matching Cell URL. */
    public static final String REGEX_HTTP_SUBDOMAIN = "^(http|https):\\/\\/(.+?)\\.(.*)$";

    /** String Slash. */
    public static final String STRING_SLASH = "/";

    /**
     * constructor.
     */
    private UriUtils() {
    }

    /**
     * Get Url Variations.
     * @param url String
     * @return ArrayList<String>
     */
    public static List<String> getUrlVariations(String url) {
        List<String> variations = new ArrayList<String>();
        if (url == null) {
            return variations;
        }
        variations.add(url);
        try {
            PersoniumUrl pUrl = PersoniumUrl.create(url);
            switch (pUrl.schemeType) {
            case HTTP:
                variations.add(pUrl.toLocalunit());
                variations.add(pUrl.getLocalUnitSingleColonUrl());
                break;
            case LOCAL_UNIT_DOUBLE_COLON:
                variations.add(pUrl.toHttp());
                variations.add(pUrl.getLocalUnitSingleColonUrl());
                break;
            case LOCAL_UNIT_SINGLE_COLON:
                variations.add(pUrl.toHttp());
                variations.add(pUrl.toLocalunit());
            default:
                // no variation for other scheme
            }
        } catch (IllegalArgumentException e) {
            // No op.
            // When Given Url is invalid, PersoniumUrl will throw IllegalArgumentException.
            // Even in such case, this class should just return the original given url as
            // a single variation.
        }
        return variations;
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
     * If the he given value does not match localunit schem, the given value is returned as-is.
     * @param localUnitSchemeUrl local unit url
     * @return url string with http(s) scheme
     */
    public static String convertSchemeFromLocalUnitToHttp(String localUnitSchemeUrl) {
        if (localUnitSchemeUrl == null) {
            return null;
        }
        String unitUrl = PersoniumUnitConfig.getBaseUrl();
        Matcher localUnitDoubleColons = REGEX_LOCALUNIT_DOUBLE_COLONS.matcher(localUnitSchemeUrl);
        Matcher localUnitSingleColon = REGEX_LOCALUNIT_SINGLE_COLON.matcher(localUnitSchemeUrl);
        String pathBased = localUnitSchemeUrl;
        if (localUnitDoubleColons.matches()) {
            // when detected personium-localunit scheme with double colons
            String cellName = localUnitDoubleColons.group(1);
            String path = localUnitDoubleColons.group(2);
            StringBuilder sb = new StringBuilder(unitUrl);
            sb.append(cellName);
            if (!path.startsWith(STRING_SLASH)) {
                sb.append(STRING_SLASH);
            }
            sb.append(path);
            pathBased = sb.toString();
        } else if (localUnitSingleColon.matches()) {
            // when detected personium-localunit scheme with single colon
            String path = localUnitSingleColon.group(1);
            if (path.startsWith(STRING_SLASH) && unitUrl.endsWith(STRING_SLASH)) {
                unitUrl = unitUrl.replaceFirst("/*$", "");
            }
            StringBuilder sb = new StringBuilder(unitUrl);
            sb.append(path);
            pathBased = sb.toString();
        }
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

    /**
     * Convert scheme from http(s) to LocalUnit.
     * Convert only if the target URL matches UnitURL, otherwise just return the given value as-is.
     * @param url target url
     * @return url string with local unit scheme
     */
    public static String convertSchemeFromHttpToLocalUnit(String url) {
        if (url == null) {
            return null;
        }
        String unitUrl = PersoniumUnitConfig.getBaseUrl();
        if (PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
            // path based
            if (!url.startsWith(unitUrl)) {
                // return as-is when url is foreign
                return url;
            }
            // convert when url is localunit
            String ret = url.replaceFirst(unitUrl, SCHEME_LOCALUNIT + ":/");
            ret = ret.replaceFirst("\\:\\/(.+?)\\/", ":$1:/");
            return ret;
        } else {
            // return with single colon syntax when url is unit level.
            if (url.startsWith(unitUrl)) {
                // convert when url is localunit
                return url.replaceFirst(unitUrl, SCHEME_LOCALUNIT + ":/");
            }
            // return with double colon syntax when url is cell level.
            URI uri;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw PersoniumCoreException.Common.INVALID_URL.params(url).reason(e);
            }
            URI unitUri;
            try {
                unitUri = new URI(unitUrl);
            } catch (URISyntaxException e) {
                throw PersoniumCoreException.Common.INVALID_URL.params(unitUrl).reason(e);
            }
            if (uri.getHost() == null) {
                return url;
            }
            String host = uri.getHost();
            String cellName = host.split("\\.")[0];
            String unitDomain = host.replaceFirst(cellName + "\\.", "");
            if (uri.getHost() == null) {
                return url;
            }
            String unitHost = unitUri.getHost();
            if (!unitDomain.contentEquals(unitHost)) {
                // foreign URL
                return url;
            }
            StringBuilder sb = new StringBuilder(SCHEME_LOCALUNIT);
            sb.append(":").append(cellName).append(":");
            sb.append(uri.getPath());
            if (uri.getQuery() != null) {
                sb.append("?").append(uri.getQuery());
            }
            if (uri.getFragment() != null) {
                sb.append("#").append(uri.getFragment());
            }
            return sb.toString();
        }
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
     * Convert such URL in the format of "https://{domain}/{cellname}/..." to "https://{cellname}.{domain}/...".
     * if the givne URL is not in the above format then return the Given URL as is
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
        String path = uri.getPath();
        String[] pathAry = path.split("/");
        String cellName = null;
        String firstPath = null;

        StringBuilder hostBuilder = new StringBuilder();
        StringBuilder cellPathBuilder = new StringBuilder();
        if (pathAry.length > 1) {
            firstPath = pathAry[1];
            if (!firstPath.startsWith("__")) {
                cellName = firstPath;
            }
        }
        // Construct FQNDN
        if (cellName != null) {
            hostBuilder.append(cellName).append(".");
        }
        hostBuilder.append(domain);

        // Construct Path
        if (cellName != null) {
            cellPathBuilder.append("/").append(cellName);
            path = path.replaceFirst(cellPathBuilder.toString(), "");
        }

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

    /**
     * @param url1 String
     * @param url2 String
     * @return if urls are equal or not
     */
    public static boolean equalIgnoringPort(String url1, String url2) {

        try {
            URI u1 = new URI(url1);
            URI u2 = new URI(url2);
            if (!Objects.equals(u1.getHost(), u2.getHost())) {
                return false;
            }
            if (!Objects.equals(u1.getScheme(), u2.getScheme())) {
                return false;
            }
            if (!Objects.equals(u1.getPath(), u2.getPath())) {
                return false;
            }
            if (!Objects.equals(u1.getFragment(), u2.getFragment())) {
                return false;
            }
            if (!Objects.equals(u1.getQuery(), u2.getQuery())) {
                return false;
            }
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * @param url String
     * @return http url
     */
    public static String resolveLocalUnit(String url) {
        return UriUtils.convertSchemeFromLocalUnitToHttp(url);
    }
}
