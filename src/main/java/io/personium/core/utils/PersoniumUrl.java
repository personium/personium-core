/**
 * Personium
 * Copyright 2020 Personium Project Authors
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;

/**
 * A utility class for handling personium url with following schemes.
 * <ul>
 *  <li>http / https
 *  <li>ws / wss
 *  <li>personium-localunit
 *  <li>personium-localcell
 *  <li>personium-localbox
 * </ul>
 * Example:
 *  <pre>{@code
 *    String httpUrl = PersoniumUrl.create("personium-localunit:cell1:/").toHttp();
 *    String celllocalUrl = PersoniumUrl.create("https://cell1.unit.example/").toLocalcellIf();
 *    PersoniumUrl.create("https://cell1.unit.example/").isNormalized); // true
 *  }</pre>
 * @author shimono.akio
 */
public class PersoniumUrl {
    static Logger log = LoggerFactory.getLogger(PersoniumUrl.class);

    /** enum indicating the resource type of the URL */
    public static enum ResourceType {
        /** enum value indicating the resource type of this URL is cell root.  */
        CELL_ROOT,
        /** enum value indicating the resource type of this URL is box root. */
        BOX_ROOT,
        /** enum value indicating the resource type of this URL is unit root. */
        UNIT_ROOT,
        /** enum value indicating the resource type of this URL is cell level. */
        CELL_LEVEL,
        /** enum value indicating the resource type of this URL is box level. */
        BOX_LEVEL,
        /** enum value indicating the resource type of this URL is unit level. */
        UNIT_LEVEL,
        /** enum value indicating the resource type of this URL is box-bound role. */
        ROLE_BOX_BOUND,
        /** enum value indicating the resource type of this URL is box-unbound role. */
        ROLE_BOX_UNBOUND,
        /** enum value indicating the resource type of this URL is external unit. */
        EXTERNAL_UNIT
    }

    /**
     * enum indicating the Scheme type of the URL.
     */
    public static enum SchemeType {
        /** enum value indicating "http" or "https" scheme. */
        HTTP,
        /** enum value indicating "ws" or "wss" scheme. */
        WS,
        /**
         * enum indicating "personium-localunit" scheme using a single colon.
         */
        LOCAL_UNIT_SINGLE_COLON,
        /**
         * enum indicating "personium-localunit" scheme using two colons.
         */
        LOCAL_UNIT_DOUBLE_COLON,
        /**
         * enum indicating "personium-localcell" scheme.
         */
        LOCAL_CELL,
        /**
         * enum indicating "personium-localbox" scheme.
         */
        LOCAL_BOX,
        /**
         * enum indicating invalid scheme.
         */
        INVALID
    }

    /** Scheme string, "personium-localunit". */
    public static final String SCHEME_LOCALUNIT = "personium-localunit";
    /** Scheme string, "personium-localcell". */
    public static final String SCHEME_LOCALCELL = "personium-localcell";
    /** Scheme string, "personium-localbox". */
    public static final String SCHEME_LOCALBOX = "personium-localbox";

    /** Regular expression for extracting scheme part and the rest in a URL. */
    public static final String REGEX_SUBDOMAIN = "^(.+?)\\.(.*?)$";
    /** Regular expression for matching localunit scheme. */
    public static final String REGEX_LOCALUNIT = "^([a-zA-Z0-9\\-\\_]+?):(.*)$";
    /** Regular expression for extracting a directory */
    public static final String REGEX_DIR = "^\\/?(.*?)($|\\/.*$)";
    /** Regular expression for matching non-directory API endpoint. */
    public static final String REGEX_CTL = "^__[a-zA-Z]+$";
    //    public static final String REGEX_LOCALUNIT_NO_COLON = "^\\/?([^\\/]*)(\\/.*$|$)";

    //    public static final Pattern REGEX_SCHEME = Pattern.compile("^([a-z|\\-]+?):(.*)(\\??.*)($|\\#?.*)$");
    //    public static final String REGEX_HTTP_SUBDOMAIN = "^(https?|wss?):\\/\\/(.+?)\\.(.*?)(\\/.*$|$)";
    //    public static final String REGEX_HTTP_PATH_BASE = "^(https?|wss?):\\/\\/([^\\/]+?)($|\\/.*?)($|\\/.*$)";

    String givenUrl;
    URI uri;
    String scheme;
    int port;
    /**
     * Property to indicate the scheme type of this URL.
     */
    public SchemeType schemeType;
    /**
     * Property to indicate the resource type of this URL.
     */
    public ResourceType resourceType;
    /**
     * Property to indicate the cell name of this URL.
     */
    public String cellName;
    /**
     * Property to indicate the unit domain of this URL.
     * null if the resource type is EXTERNAL_UNIT
     */
    public String unitDomain;
    /**
     * Property to indicate the box name of this URL.
     * null if the resource type is CELL_LEVEL/above or EXTERNAL_UNIT
     */
    public String boxName;
    String pathUnderCell;
    String pathUnderBox;
    String pathUnderUnit;
    /**
     * Property indicating if this URL is normalized form or not.
     */
    public Boolean isNormalized;

    /**
     * Factory method to create an object of this class.
     */
    public static PersoniumUrl create(String url) {
        return new PersoniumUrl(url);
    }

    /**
     * Factory method to create an object of this class.
     */
    public static PersoniumUrl create(String url, String cellName) {
        PersoniumUrl ret = new PersoniumUrl(url);
        if (ret.cellName == null) {
            ret.cellName = cellName;
        }
        return ret;
    }

    /**
     * Factory method to create an URL with "personium-localunit" scheme.
     */
    public static PersoniumUrl localUnit(String cellName, String path) {
        return new PersoniumUrl(SCHEME_LOCALUNIT + ":" + cellName + ":" + path);
    }

    /**
     * Constructor
     * @param url
     */
    public PersoniumUrl(String url) {
        //        log.info("-----");
        log.info(url);
        if (url == null) {
            throw new IllegalArgumentException("given url is null.");
        }
        this.givenUrl = url;
        try {
            this.uri = URI.create(this.givenUrl);
            URI normalizedUri = this.uri.normalize();
            if (!this.uri.equals(normalizedUri)) {
                this.isNormalized = false;
                this.uri = normalizedUri;
            }
        } catch (IllegalArgumentException e) {
            // we allow personium-* schemes to have
            // empty scheme specific part
            String n = this.givenUrl.replaceFirst(":", ":/");
            // log.info(this.givenUrl + " >>A>> " + n);
            this.uri = URI.create(n);
            this.isNormalized = false;
        }

        this.parseSchemeType();
        this.parseResourceType();
        if (this.uri.getFragment() != null) {
            this.isNormalized = false;
        }
        if (this.isNormalized == null) {
            this.isNormalized = true;
        }
    }

    void parseSchemeType() {
        // Matcher m = REGEX_SCHEME.matcher(this.givenUrl);
        if (this.uri == null || !this.uri.isAbsolute()) {
            throw new IllegalArgumentException("relative url is not supported.");
        }
        this.scheme = this.uri.getScheme();
        switch (this.scheme) {
        case "https":
            this.schemeType = SchemeType.HTTP;
            break;
        case "http":
            this.schemeType = SchemeType.HTTP;
            break;
        case "ws":
            this.schemeType = SchemeType.WS;
            break;
        case "wss":
            this.schemeType = SchemeType.WS;
            break;
        case SCHEME_LOCALUNIT:
            String ssp = this.uri.getSchemeSpecificPart();
            Matcher localUnit = Pattern.compile(REGEX_LOCALUNIT).matcher(ssp);
            Matcher localUnitNoColon = Pattern.compile(REGEX_DIR).matcher(ssp);
            if (localUnit.matches()) {
                this.schemeType = SchemeType.LOCAL_UNIT_DOUBLE_COLON;
                this.cellName = localUnit.group(1);
                this.pathUnderCell = localUnit.group(2);
            } else if (localUnitNoColon.matches()) {
                this.schemeType = SchemeType.LOCAL_UNIT_SINGLE_COLON;
                this.pathUnderUnit = this.uri.getPath();
                if (this.pathUnderUnit == null) {
                    this.pathUnderUnit = "/";
                }
                if (!this.pathUnderUnit.startsWith("/")) {
                    this.isNormalized = false;
                    this.pathUnderUnit = "/" + this.pathUnderUnit;
                }
                if (!Pattern.matches(REGEX_CTL, localUnitNoColon.group(1))) {
                    this.cellName = localUnitNoColon.group(1);
                    this.pathUnderCell = localUnitNoColon.group(2);
                }
            }
            break;
        case SCHEME_LOCALCELL:
            this.schemeType = SchemeType.LOCAL_CELL;
            break;
        case SCHEME_LOCALBOX:
            this.schemeType = SchemeType.LOCAL_BOX;
            break;
        default:
            this.schemeType = SchemeType.INVALID;
        }
        String ssp = this.uri.getSchemeSpecificPart();
        if (this.schemeType != SchemeType.LOCAL_UNIT_DOUBLE_COLON && this.schemeType != SchemeType.INVALID) {
            if (!ssp.startsWith("/")) {
                String n = this.givenUrl.replaceFirst(":", ":/");
//                log.info(this.givenUrl + " >>B>> " + n);
                this.uri = URI.create(n);
                this.isNormalized = false;
            }
        }
    }

    void parseResourceType() {
        switch (this.schemeType) {
        case LOCAL_UNIT_SINGLE_COLON:
            this.handleLocalunitSingleColon();
            break;
        case LOCAL_UNIT_DOUBLE_COLON:
            this.handleLocalunitDoubleColon();
            break;
        case HTTP:
        case WS:
            this.handleHttpWs();
            break;
        case LOCAL_CELL:
            this.handleLocalcell();
            break;
        case LOCAL_BOX:
            this.handleLocalbox();
            break;
        default:
            this.resourceType = ResourceType.EXTERNAL_UNIT;
        }
    }

    void handleLocalunitSingleColon() {
        if ("/".equals(this.pathUnderUnit) || "".equals(this.pathUnderUnit)) {
            this.resourceType = ResourceType.UNIT_ROOT;
        } else if (this.pathUnderUnit.startsWith("/__ctl")) {
            this.resourceType = ResourceType.UNIT_LEVEL;
        } else {
            this.parsePathUnderCell();
        }
    }

    void handleLocalunitDoubleColon() {
        this.parsePathUnderCell();
    }

    void handleHttpWs() {
        String givenUrlHost = this.uri.getHost();
        String unitHost = CommonUtils.getFQDN();
        if (givenUrlHost == null) {
            throw new IllegalArgumentException("given url is invalid [" + this.givenUrl + "]");
        }
        // detect external unit
        if (PersoniumUnitConfig.isPathBasedCellUrlEnabled() && !givenUrlHost.equals(unitHost)) {
            this.resourceType = ResourceType.EXTERNAL_UNIT;
            return;
        }
        if (!givenUrlHost.equals(unitHost) && !givenUrlHost.endsWith("." + unitHost)) {
            this.resourceType = ResourceType.EXTERNAL_UNIT;
            // sub-sub domain cases are not detected here.
            return;
        }

        if (PersoniumUnitConfig.getUnitPort() != uri.getPort()) {
            this.resourceType = ResourceType.EXTERNAL_UNIT;
            return;
        }

        // only allow configured scheme
        //  i.e. http/ws or https/wss
        List<String> unitSchemes = new ArrayList<>();
        String httpScheme = PersoniumUnitConfig.getUnitScheme();
        String wsScheme;
        if ("http".equals(httpScheme)) {
            wsScheme = "ws";
        } else if ("https".equals(httpScheme)) {
            wsScheme = "wss";
        } else {
            throw new RuntimeException("Configured unit scheme [" + httpScheme + "] invalid.");
        }
        unitSchemes.add(httpScheme);
        unitSchemes.add(wsScheme);
        if (!unitSchemes.contains(uri.getScheme())) {
            this.resourceType = ResourceType.EXTERNAL_UNIT;
            return;
        }

        // url on this unit
        // Step1 populate cellName, BoxName, paths
        if (!PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
            // Subdomain-based
            String authority = this.uri.getAuthority();
            Matcher m = Pattern.compile(REGEX_SUBDOMAIN).matcher(authority);
            if (!m.matches()) {
                // Should match
                throw new RuntimeException("Unexpected Regex mismatch: [" + authority
                        + "] somehow did not match [" + REGEX_SUBDOMAIN + "]");
            }
            if (unitHost.equals(givenUrlHost)) {
                this.unitDomain = unitHost;
                this.pathUnderUnit = this.uri.getPath();
                if (StringUtils.isEmpty(this.pathUnderUnit)) {
                    this.isNormalized = false;
                    this.pathUnderUnit = "/";
                }
                if ("/".equals(this.pathUnderUnit)) {
                    this.resourceType = ResourceType.UNIT_ROOT;
                } else {
                    this.resourceType = ResourceType.UNIT_LEVEL;
                }
                return;
            }
            this.cellName = m.group(1);
            this.unitDomain = m.group(2);
            this.pathUnderCell = this.uri.getPath();
        } else {
            // Path-based
            this.unitDomain = this.uri.getAuthority();
            this.pathUnderUnit = this.uri.getPath();
            switch (this.pathUnderUnit.length()) {
            case 0:
                this.pathUnderUnit = "/";
                this.isNormalized = false;
            case 1:
                this.resourceType = ResourceType.UNIT_ROOT;
                return;
            default:
                Matcher m = Pattern.compile(REGEX_DIR).matcher(this.uri.getPath());
                if (!m.matches()) {
                    // Should match
                    throw new RuntimeException("Unexpected Regex mismatch: [" + this.uri.getPath()
                            + "] somehow did not match [" + REGEX_DIR + "]");
                }
                if (Pattern.matches(REGEX_CTL, m.group(1))) {
                    this.resourceType = ResourceType.UNIT_LEVEL;
                    return;
                } else {
                    this.cellName = m.group(1);
                    this.pathUnderCell = m.group(2);
                }
            }
        }
        //  sub-sub domain case comes here.
        if (!unitHost.equals(this.unitDomain)) {
            throw new IllegalArgumentException("Invalid Url given [" + this.givenUrl + "]");
        }
        if (StringUtils.isEmpty(this.pathUnderCell)) {
            this.isNormalized = false;
            this.pathUnderCell = "/";
        }
        this.parsePathUnderCell();
    }

    void handleLocalbox() {
        this.pathUnderBox = this.uri.getPath();
        if (this.pathUnderBox == null) {
            this.pathUnderBox = "/";
            this.isNormalized = false;
        }
        if (!this.pathUnderBox.startsWith("/")) {
            this.pathUnderBox = "/" + this.pathUnderBox;
            this.isNormalized = false;
        }
        if ("/".equals(this.pathUnderBox)) {
            this.resourceType = ResourceType.BOX_ROOT;
        } else {
            this.resourceType = ResourceType.BOX_LEVEL;
        }
    }

    void handleLocalcell() {
        this.pathUnderCell = this.uri.getPath();
        if (this.pathUnderCell == null) {
            this.pathUnderCell = "/";
            this.isNormalized = false;
        }
        if (!this.pathUnderCell.startsWith("/")) {
            this.pathUnderCell = "/" + this.pathUnderCell;
            this.isNormalized = false;
        }
        this.parsePathUnderCell();
    }

    void parsePathUnderCell() {
        // Detect Cell Root
        if ("".equals(this.pathUnderCell)) {
            this.isNormalized = false;
            this.pathUnderCell = "/";
        }
        if ("/".equals(this.pathUnderCell)) {
            this.resourceType = ResourceType.CELL_ROOT;
            return;
        }
        // parse
        Matcher bm = Pattern.compile(REGEX_DIR).matcher(this.pathUnderCell);
        if (!bm.matches()) {
            // Should match
            throw new RuntimeException("Unexpected Regex mismatch: [" + this.pathUnderCell
                    + "] somehow did not match [" + REGEX_DIR + "]");
        }
        // Detect Cell Level
        if (Pattern.matches(REGEX_CTL, bm.group(1))) {
            this.resourceType = ResourceType.CELL_LEVEL;
            return;
        }
        // Detect Box Root or Box Level
        this.boxName = bm.group(1);
        this.pathUnderBox = bm.group(2);
        if ("".equals(this.pathUnderBox) || "/".equals(this.pathUnderBox)) {
            this.resourceType = ResourceType.BOX_ROOT;
        } else {
            this.resourceType = ResourceType.BOX_LEVEL;
        }
    }

    /**
     * Add trailing slash if missing.
     * @param url
     * @return url with trailing slash.
     */
    public static String addTrailingSlashIfMissing(String url) {
        if (url == null) {
            return null;
        }
        if (!url.endsWith("/")) {
            return url + "/";
        }
        return url;
    }

    /**
     * @return an URL string representation using "personium-localunit" scheme with a single colon.
     */
    public String getLocalUnitSingleColonUrl() {
        StringBuilder sb = new StringBuilder(SCHEME_LOCALUNIT);
        sb.append(":/");
        sb.append(this.cellName);
        if (StringUtils.isEmpty(this.pathUnderCell)) {
            sb.append("/");
        } else {
            sb.append(this.pathUnderCell);
        }
        return sb.toString();
    }

    /**
     * @return an URL string representation using "personium-localunit" scheme.
     */
    public String toLocalunit() {
        if (this.resourceType == ResourceType.EXTERNAL_UNIT) {
            return this.givenUrl;
        }
        StringBuilder sb = new StringBuilder(SCHEME_LOCALUNIT);
        sb.append(":");
        if (StringUtils.isEmpty(this.cellName)) {
            throw new UnsupportedOperationException("Double colon syntax is not applicable for unit level url.");
        }
        sb.append(this.cellName);
        sb.append(":");
        if (StringUtils.isEmpty(this.pathUnderCell)) {
            sb.append("/");
        } else {
            sb.append(this.pathUnderCell);
        }
        return sb.toString();
    }

    /**
     * Try to convert to a URL string with "personium-localunit" scheme.
     * returns original url when the resourceType is EXTERNAL_UNIT or INVALID.
     * @return an URL string representation using "personium-localunit" scheme.
     */
    public String toLocalcell() {
        if (this.resourceType == ResourceType.EXTERNAL_UNIT) {
            return this.givenUrl;
        }
        StringBuilder sb = new StringBuilder(SCHEME_LOCALCELL);
        sb.append(":");
        if (this.pathUnderBox != null) {
            sb.append(this.pathUnderBox);
        } else {
            sb.append("/");
        }
        return sb.toString();
    }

    /**
     * Try to convert to a URL string with "http(s)" scheme.
     * @return an URL string with "http(s)" scheme.
     */
    public String toHttp() {
        // return as-is if EXTRENAL
        if (this.resourceType == ResourceType.EXTERNAL_UNIT) {
            return this.givenUrl;
        }
        this.unitDomain = CommonUtils.getFQDN();
        // Construct Scheme-Domain-Port part
        StringBuilder sb = new StringBuilder(PersoniumUnitConfig.getUnitScheme());
        sb.append("://");
        if (!PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
            if (this.cellName != null) {
                sb.append(this.cellName);
                sb.append(".");
            }
        }
        sb.append(this.unitDomain);
        if (PersoniumUnitConfig.getUnitPort() > 0) {
            sb.append(":" + PersoniumUnitConfig.getUnitPort());
        }
        if (this.resourceType == ResourceType.UNIT_ROOT) {
            sb.append("/");
            return this.addQueryAndFragment(sb);
        }
        if (this.resourceType == ResourceType.UNIT_LEVEL) {
            if (!this.pathUnderUnit.startsWith("/")) {
                sb.append("/");
            }
            sb.append(this.pathUnderUnit);
            return this.addQueryAndFragment(sb);
        }
        // Add Cell Path for pathBased
        if (PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
            if (this.cellName != null) {
                sb.append("/");
                sb.append(this.cellName);
            }
        }
        // Cell Level
        if (this.resourceType == ResourceType.CELL_ROOT) {
            sb.append("/");
            return this.addQueryAndFragment(sb);
        }
        if (this.resourceType == ResourceType.CELL_LEVEL) {
            sb.append(this.pathUnderCell);
            return this.addQueryAndFragment(sb);
        }
        // Add Box Path for pathBased
        sb.append("/");
        sb.append(this.boxName);
        if (this.resourceType == ResourceType.BOX_ROOT) {
            sb.append("/");
            return this.addQueryAndFragment(sb);
        }
        sb.append(this.pathUnderBox);
        return this.addQueryAndFragment(sb);
    }

    String addQueryAndFragment(StringBuilder sb) {
        if (this.uri.getQuery() != null) {
            sb.append("?");
            sb.append(this.uri.getQuery());
        }
        if (this.uri.getFragment() != null) {
            sb.append("#");
            sb.append(this.uri.getFragment());
        }
        return sb.toString();
    }

    /**
     *
     * @deprecated
     * @return
     */
    public String normalize() {
        PersoniumUrl clone = PersoniumUrl.create(this.givenUrl);

        // normalize the path part
        clone.pathUnderBox = normalizePath(this.pathUnderBox);
        clone.pathUnderCell = normalizePath(this.pathUnderCell);
        clone.pathUnderUnit = normalizePath(this.pathUnderUnit);

        // add trailing slash for unit / cell / box root
        switch (this.resourceType) {
        case BOX_ROOT:
            clone.pathUnderBox = "/";
        case CELL_ROOT:
            clone.pathUnderCell = addTrailingSlashIfMissing(clone.pathUnderCell);
        case UNIT_ROOT:
            clone.pathUnderUnit = addTrailingSlashIfMissing(clone.pathUnderUnit);
            break;
        default:
        }
        log.info(clone.toString());
        switch (this.schemeType) {
        case HTTP:
            return clone.toHttp();
        case LOCAL_UNIT_DOUBLE_COLON:
            return clone.toLocalunit();
        case LOCAL_UNIT_SINGLE_COLON:
            return clone.toLocalunit();
        case LOCAL_CELL:
            return clone.toLocalunit();
        case LOCAL_BOX:
            return clone.toLocalunit();
        default:
            return clone.toLocalunit();
        }
    }

    static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        URI uri = URI.create(path);
        return uri.normalize().toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("givenUrl: " + this.givenUrl + "\n");
        sb.append("scheme: " + this.scheme + "\n");
        sb.append("unitDomain: " + this.unitDomain + "\n");
        sb.append("cellName: " + this.cellName + "\n");
        sb.append("boxName: " + this.boxName + "\n");
        sb.append("path under unit: " + this.pathUnderUnit + "\n");
        sb.append("path under cell: " + this.pathUnderCell + "\n");
        sb.append("path under box: " + this.pathUnderBox + "\n");
        return sb.toString();
    }
}
