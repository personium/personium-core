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
 *
 *  http / https
 *  ws / wss
 *  personium-localunit
 *  personium-localcell
 *  personium-localbox
 *
 * @author shimono.akio
 */
public class PersoniumUrl {
    static Logger log = LoggerFactory.getLogger(PersoniumUrl.class);

    public static enum ResourceType {
        CELL_ROOT,
        BOX_ROOT,
        UNIT_ROOT,
        CELL_LEVEL,
        BOX_LEVEL,
        UNIT_LEVEL,
        ROLE_BOX_BOUND,
        ROLE_BOX_UNBOUND,
        EXTERNAL_UNIT
    }
    public static enum SchemeType {
        HTTP,
        WS,
        LOCAL_UNIT_SINGLE_COLON,
        LOCAL_UNIT_DOUBLE_COLON,
        LOCAL_CELL,
        LOCAL_BOX,
        INVALID
    }
    /** Scheme string, "personium-localunit". */
    public static final String SCHEME_LOCALUNIT = "personium-localunit";
    /** Scheme string, "personium-localcell". */
    public static final String SCHEME_LOCALCELL = "personium-localcell";
    /** Scheme string, "personium-localbox". */
    public static final String SCHEME_LOCALBOX = "personium-localbox";

    /** Regular expression for extracting scheme part and the rest in a URL */
    public static final Pattern REGEX_SCHEME = Pattern.compile("^([a-z|\\-]+?):(.*)$");
    public static final String REGEX_HTTP_SUBDOMAIN = "^(https?|wss?):\\/\\/(.+?)\\.(.*?)(\\/.*$|$)";
    public static final String REGEX_HTTP_PATH_BASE = "^(https?|wss?):\\/\\/(.+?)\\/(.+?)(\\/.*$|$)";
    public static final String REGEX_DIR = "^\\/?(.*?)($|\\/.*$)";
    public static final String REGEX_CTL = "^__[a-zA-Z]+$";

    /** Regular expression for matching localunit scheme with single colon */
    public static final String REGEX_LOCALUNIT_SINGLE_COLON = "^" + SCHEME_LOCALUNIT + ":\\/?([^\\/]*)(\\/.*$|$)";

    /** Regular expression for matching localunit scheme with double colons */
    public static final String REGEX_LOCALUNIT_DOUBLE_COLONS = "^" + SCHEME_LOCALUNIT + ":(.+?):(.*)$";

    String givenUrl;
    String scheme;
    int port;
    SchemeType schemeType;
    ResourceType resourceType;
    public String cellName;
    public String unitDomain;
    public String boxName;
    String pathUnderCell;
    String pathUnderBox;
    String pathUnderUnit;
    /**
     * Constructor
     * @param url
     */
    public PersoniumUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("given url is null.");
        }
        this.givenUrl = url;
        Matcher m = REGEX_SCHEME.matcher(this.givenUrl);
        if (m.matches()) {
            this.scheme = m.group(1);
            this.parseSchemeType();
            this.parseResourceType();
        } else {
            // scheme not given
            throw new IllegalArgumentException("relative url is not supported.");
        }
    }
    void parseSchemeType() {
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
            Matcher localUnitSingleColon = Pattern.compile(REGEX_LOCALUNIT_SINGLE_COLON).matcher(this.givenUrl);
            Matcher localUnitDoubleColon = Pattern.compile(REGEX_LOCALUNIT_DOUBLE_COLONS).matcher(this.givenUrl);
            if (localUnitDoubleColon.matches()) {
                this.schemeType = SchemeType.LOCAL_UNIT_DOUBLE_COLON;
                this.cellName = localUnitDoubleColon.group(1);
                this.pathUnderCell = localUnitDoubleColon.group(2);
            } else if (localUnitSingleColon.matches()) {
                this.schemeType = SchemeType.LOCAL_UNIT_SINGLE_COLON;
                this.pathUnderUnit = this.givenUrl.replaceFirst(SCHEME_LOCALUNIT + ":", "");
                if (!this.pathUnderUnit.startsWith("/")) {
                    this.pathUnderUnit = "/" + this.pathUnderUnit;
                }
                if (!Pattern.matches(REGEX_CTL, localUnitSingleColon.group(1))) {
                    this.cellName = localUnitSingleColon.group(1);
                    this.pathUnderCell = localUnitSingleColon.group(2);
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
    }
    void parseResourceType() {
        // Unit Root
        if (PersoniumUnitConfig.getBaseUrl().equals(addTrailingSlashIfMissing(this.givenUrl))) {
            this.resourceType = ResourceType.UNIT_ROOT;
            this.unitDomain = CommonUtils.getFQDN();
            return;
        }
        // Unit Level
        if (addTrailingSlashIfMissing(this.givenUrl).startsWith(PersoniumUnitConfig.getBaseUrl() + "__ctl/")) {
            this.resourceType = ResourceType.UNIT_LEVEL;
            this.unitDomain = CommonUtils.getFQDN();
            return;
        }
        switch(this.schemeType) {
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
        if ("/".equals(this.pathUnderUnit) ||"".equals(this.pathUnderUnit)) {
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
        URI uri = URI.create(this.givenUrl);
        String urlHost = uri.getHost();
        String configHost = CommonUtils.getFQDN();
        if (urlHost == null) {
            throw new IllegalArgumentException("given url is invalid [" + this.givenUrl + "]");
        }
        // detect external unit
        if (PersoniumUnitConfig.isPathBasedCellUrlEnabled() && !urlHost.equals(configHost)) {
            this.resourceType = ResourceType.EXTERNAL_UNIT;
            return;
        }
        if (!urlHost.equals(configHost) && !urlHost.endsWith("." + configHost)) {
            this.resourceType = ResourceType.EXTERNAL_UNIT;
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
            if (configHost.equals(urlHost)) {
                this.resourceType = ResourceType.UNIT_LEVEL;
                return;
            }
            // Subdomain-based
            Matcher m = Pattern.compile(REGEX_HTTP_SUBDOMAIN).matcher(this.givenUrl);
            if (!m.matches()) {
                // Should match
                throw new RuntimeException("Unexpected Regex mismatch: [" + this.givenUrl
                        + "] somehow did not match [" + REGEX_HTTP_SUBDOMAIN+ "]");
            }
            this.cellName = m.group(2);
            this.unitDomain = m.group(3);
            this.pathUnderCell = m.group(4);

        } else {
            // Path-based
            this.unitDomain = urlHost;
            Matcher m = Pattern.compile(REGEX_HTTP_PATH_BASE).matcher(this.givenUrl);
            if (!m.matches()) {
                // Should match
                throw new RuntimeException("Unexpected Regex mismatch: [" + this.givenUrl
                        + "] somehow did not match [" + REGEX_HTTP_PATH_BASE+ "]");
            }
            this.cellName = m.group(3);
            this.unitDomain = m.group(2);
            this.pathUnderCell = m.group(4);
        }
        //  sub-sub domain case comes here.
        if (!configHost.equals(this.unitDomain)) {
            throw new IllegalArgumentException("Invalid Url given [" + this.givenUrl + "]");
        }
        this.parsePathUnderCell();

        // Step2. Determine ResourceType
        if (this.cellName == null) {
            this.resourceType = ResourceType.UNIT_LEVEL;
        }
    }
    void handleLocalbox() {
        this.pathUnderBox = this.givenUrl.replaceFirst(SCHEME_LOCALBOX + ":", "");
        if (!this.pathUnderBox.startsWith("/")) {
            this.pathUnderBox = "/" + this.pathUnderBox;
        }
        if ("/".equals(this.pathUnderBox)) {
            this.resourceType = ResourceType.BOX_ROOT;
        } else {
            this.resourceType = ResourceType.BOX_LEVEL;
        }
    }
    void handleLocalcell() {
        this.pathUnderCell = this.givenUrl.replaceFirst(SCHEME_LOCALCELL + ":", "");
        if (!this.pathUnderCell.startsWith("/")) {
            this.pathUnderCell = "/" + this.pathUnderCell;
        }
        this.parsePathUnderCell();
    }
    void parsePathUnderCell() {
        // Detect Cell Root
        if ("".equals(this.pathUnderCell) || "/".equals(this.pathUnderCell)) {
            this.resourceType = ResourceType.CELL_ROOT;
            return;
        }
        // parse
        Matcher bm = Pattern.compile(REGEX_DIR).matcher(this.pathUnderCell);
        if (!bm.matches()) {
            // Should match
            throw new RuntimeException("Unexpected Regex mismatch: [" + this.pathUnderCell
                    + "] somehow did not match [" + REGEX_DIR+ "]");
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
    public static String addTrailingSlashIfMissing(String url) {
        if (!url.endsWith("/")) {
            return url + "/";
        }
        return url;
    }

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
    public String getLocalUnitDoubleColonUrl() {
        if (this.resourceType == ResourceType.EXTERNAL_UNIT) {
            return this.givenUrl;
        }
        StringBuilder sb = new StringBuilder(SCHEME_LOCALUNIT);
        sb.append(":");
        if (this.cellName == null) {
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
    public String getLocalCellUrl() {
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
    public String getHttpUrl() {
        // return as-is if EXTRENAL
        if (this.resourceType == ResourceType.EXTERNAL_UNIT) {
            return this.givenUrl;
        }
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
            return sb.toString();
        }
        if (this.resourceType == ResourceType.UNIT_LEVEL) {
            if (!this.pathUnderUnit.startsWith("/")) {
                sb.append("/");
            }
            sb.append(this.pathUnderUnit);
            return sb.toString();
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
            return sb.toString();
        }
        if (this.resourceType == ResourceType.CELL_LEVEL) {
            sb.append(this.pathUnderCell);
            return sb.toString();
        }
        // Add Box Path for pathBased
        sb.append("/");
        sb.append(this.boxName);
        if (this.resourceType == ResourceType.BOX_ROOT) {
            sb.append("/");
            return sb.toString();
        }
        sb.append(this.pathUnderBox);
        return sb.toString();
    }
    public List<String> getVariations() {
        List<String> ret = new ArrayList<>();
        return ret;
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

