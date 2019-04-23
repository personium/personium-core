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
package io.personium.core.model;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.UriUtils;

/**
 * A class that performs processing except delegation of processing from JaxRS Resource object excluding Dav related persistence.
 */
public class CellRsCmp extends DavRsCmp {

    /** Name of property in which the URL of the relay destination is described. */
    private static final String RELAY_HTML_URL = "relayhtmlurl";
    /** Name of property in which the URL of the authorization html. */
    private static final String AUTHORIZATION_HTML_URL = "authorizationhtmlurl";
    /** Name of property in which the URL of the authorization html. */
    private static final String AUTHORIZATION_PASSWORD_CHANGE_HTML_URL = "authorizationpasswordchangehtmlurl";
    /** Name of property in accounts not recording authentication history. */
    private static final String ACCOUNTS_NOT_RECORDING_AUTH_HISTORY = "accountsnotrecordingauthhistory";

    Cell cell;
    AccessContext accessContext;
    String requestKey;
    String eventId;
    String ruleChain;
    String via;

    /**
     * Constructor.
     * @param davCmp DavCmp
     * @param cell Cell
     * @param accessContext AccessContext
     */
    public CellRsCmp(final DavCmp davCmp, final Cell cell, final AccessContext accessContext) {
        this(davCmp, cell, accessContext, null, null, null, null);
    }

    /**
     * Constructor.
     * @param davCmp DavCmp
     * @param cell Cell
     * @param accessContext AccessContext
     * @param requestKey X-Personium-RequestKey Header
     * @param eventId X-Personium-EventId Header
     * @param ruleChain X-Personium-RuleChain Header
     * @param via X-Personium-Via Header
     */
    public CellRsCmp(final DavCmp davCmp, final Cell cell, final AccessContext accessContext,
            final String requestKey, final String eventId, final String ruleChain, final String via) {
        super(null, davCmp);
        this.cell = cell;
        this.accessContext = accessContext;
        this.requestKey = requestKey;
        this.eventId = eventId;
        this.ruleChain = ruleChain;
        this.via = via;
    }

    /**
     * Returns the URL of this resource.
     * @return URL string
     */
    public String getUrl() {
        return this.cell.getUrl();
    }

    /**
     * Returns the Cell to which the resource belongs.
     * @return Cell object
     */
    public Cell getCell() {
        return this.cell;
    }

    /**
     * Returns the Box to which the resource belongs.
     * @return Box object
     */
    public Box getBox() {
        return null;
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.accessContext;
    }

    /**
     * Check ACL information and judge whether access is possible.
     * @param ac access context
     * @param privilege Privilege of ACL (read or write)
     * @return boolean
     */
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {

        //If davCmp does not exist (resource that does not exist is specified) skip ACL check for that resource
        if (this.davCmp != null
                && this.getAccessContext().requirePrivilege(this.davCmp.getAcl(), privilege, this.getCell().getUrl())) {
            return true;
        }
        return false;
    }

    /**
     * Performs access control.
     * @param ac Access context
     * @param privilege Required privilege
     */
    public void checkAccessContext(AccessContext ac, Privilege privilege) {
        // Check UnitUser token.
        if (ac.isUnitUserToken(privilege)) {
            return;
        }

        //Check if basic authentication can be done
        this.accessContext.updateBasicAuthenticationStateForResource(null);

        //Access right check
        if (!this.hasPrivilege(ac, privilege)) {
            //Check the validity of the token
            //Even if the token is INVALID, if the ACL setting and Privilege is set to all, it is necessary to permit access, so check at this timing
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(getAcceptableAuthScheme());
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(
                        ac.getRealm(), getAcceptableAuthScheme());
            }
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * Get cell info (name, url).
     * @return cell info
     */
    @SuppressWarnings("unchecked")
    public JSONObject getCellMetadataJson() {
        JSONObject responseJson = new JSONObject();
        JSONObject cellMetadataJson = new JSONObject();
        cellMetadataJson.put("name", cell.getName());
        cellMetadataJson.put("url", cell.getUrl());
        responseJson.put("cell", cellMetadataJson);
        return responseJson;
    }

    /**
     * Request http get to RelayHtmlUrl.
     * @return Http response
     */
    public HttpResponse requestGetRelayHtml() {
        // Get relayhtmlurl property.
        String relayHtmlUrl;
        try {
            relayHtmlUrl = getDavCmp().getProperty(RELAY_HTML_URL, "urn:x-personium:xmlns");
        } catch (IOException | SAXException e1) {
            throw PersoniumCoreException.UI.PROPERTY_NOT_URL.params(RELAY_HTML_URL);
        }

        if (StringUtils.isEmpty(relayHtmlUrl)) {
            relayHtmlUrl = PersoniumUnitConfig.getRelayhtmlurlDefault();
        }

        // Convert personium-localunit and personium-localcell.
        relayHtmlUrl = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), relayHtmlUrl);
        relayHtmlUrl = UriUtils.convertSchemeFromLocalCellToHttp(cell.getUrl(), relayHtmlUrl);

        // Validate relayHtmlUrl.
        validateRequestHtmlUrl(relayHtmlUrl, RELAY_HTML_URL);

        // GET html.
        return requestGetHtml(relayHtmlUrl);
    }

    /**
     * Request http get to AuthorizationHtmlUrl.
     * @return Http response
     */
    public HttpResponse requestGetAuthorizationHtml() {
        // Get authorizationhtmlurl property.
        String authorizationHtmlUrl;
        try {
            authorizationHtmlUrl = getDavCmp().getProperty(AUTHORIZATION_HTML_URL, "urn:x-personium:xmlns");
        } catch (IOException | SAXException e1) {
            throw PersoniumCoreException.UI.PROPERTY_NOT_URL.params(AUTHORIZATION_HTML_URL);
        }

        if (StringUtils.isEmpty(authorizationHtmlUrl)) {
            authorizationHtmlUrl = PersoniumUnitConfig.getAuthorizationhtmlurlDefault();
        }

        // Convert personium-localunit and personium-localcell.
        authorizationHtmlUrl = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), authorizationHtmlUrl);
        authorizationHtmlUrl = UriUtils.convertSchemeFromLocalCellToHttp(cell.getUrl(), authorizationHtmlUrl);

        // Validate relayHtmlUrl.
        validateRequestHtmlUrl(authorizationHtmlUrl, AUTHORIZATION_HTML_URL);

        // GET html.
        return requestGetHtml(authorizationHtmlUrl);
    }

    /**
     * Request http get to AuthorizationPasswordChangeHtmlUrl.
     * @return Http response
     */
    public HttpResponse requestGetAuthorizationPasswordChangeHtml() {
        // Get authorizationhtmlurl property.
        String authorizationPasswordHtmlUrl;
        try {
            authorizationPasswordHtmlUrl = getDavCmp().getProperty(
                    AUTHORIZATION_PASSWORD_CHANGE_HTML_URL, "urn:x-personium:xmlns");
        } catch (IOException | SAXException e1) {
            throw PersoniumCoreException.UI.PROPERTY_NOT_URL.params(AUTHORIZATION_PASSWORD_CHANGE_HTML_URL);
        }

        if (StringUtils.isEmpty(authorizationPasswordHtmlUrl)) {
            authorizationPasswordHtmlUrl = PersoniumUnitConfig.getAuthorizationPasswordChangeHtmlUrlDefault();
        }

        // Convert personium-localunit and personium-localcell.
        authorizationPasswordHtmlUrl = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(),
                authorizationPasswordHtmlUrl);
        authorizationPasswordHtmlUrl = UriUtils.convertSchemeFromLocalCellToHttp(cell.getUrl(),
                authorizationPasswordHtmlUrl);

        // Validate relayHtmlUrl.
        validateRequestHtmlUrl(authorizationPasswordHtmlUrl, AUTHORIZATION_PASSWORD_CHANGE_HTML_URL);

        // GET html.
        return requestGetHtml(authorizationPasswordHtmlUrl);
    }

    /**
     * Request http get to HtmlUrl.
     * @param requestUrl request url
     * @return Http response
     */
    private HttpResponse requestGetHtml(String requestUrl) {
        HttpGet req = new HttpGet(requestUrl);
        // set headers
        req.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);

        // GET html.
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
        HttpResponse res;
        try {
            res = client.execute(req);
        } catch (ClientProtocolException e) {
            throw PersoniumCoreException.UI.INVALID_HTTP_RESPONSE.params(requestUrl).reason(e);
        } catch (IOException e) {
            throw PersoniumCoreException.UI.CONNECTION_FAILED.params(requestUrl).reason(e);
        }

        // Check response media type.
        Header contentType = res.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        if (!MediaType.TEXT_HTML.equals(contentType.getValue())) {
            throw PersoniumCoreException.NetWork.UNEXPECTED_RESPONSE.params(requestUrl, MediaType.TEXT_HTML);
        }

        return res;
    }

    /**
     * Validate htmlurl.
     * @param requestUrl request url
     * @param propertyName request url property name
     */
    private void validateRequestHtmlUrl(String requestUrl, String propertyName) {
        if (StringUtils.isEmpty(requestUrl)) {
            throw PersoniumCoreException.UI.NOT_CONFIGURED_PROPERTY.params(propertyName);
        }
        try {
            URI uri = new URI(requestUrl);
            String scheme = uri.getScheme();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw PersoniumCoreException.UI.PROPERTY_NOT_URL.params(propertyName);
            }
        } catch (URISyntaxException e) {
            throw PersoniumCoreException.UI.PROPERTY_NOT_URL.params(propertyName);
        }
    }


    /**
     * Request get not to recording authentication history accounts.
     * @return Http response
     */
    public List<String> requestGetAccountsNotRecordingAuthHistory() {
        // Get not to recording auth accounts.
        String accountsStr;
        try {
            accountsStr = getDavCmp().getProperty(
                    ACCOUNTS_NOT_RECORDING_AUTH_HISTORY, "urn:x-personium:xmlns");
        } catch (IOException | SAXException e1) {
            throw PersoniumCoreException.UI.PROPERTY_SETTINGS_ERROR.params(ACCOUNTS_NOT_RECORDING_AUTH_HISTORY);
        }
        if (StringUtils.isEmpty(accountsStr)) {
            return null;
        }
        String[] accounts = accountsStr.split(",");
        return Arrays.asList(accounts);
    }

    /**
     * Obtain Auth Scheme that can be used for authentication.
     * Autret Scheme that can be used for @return authentication
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return AcceptableAuthScheme.BEARER;
    }

    /**
     * Get RequestKey.
     * @return RequestKey string
     */
    @Override
    public String getRequestKey() {
        return this.requestKey;
    }

    /**
     * Get EventId.
     * @return EventId string
     */
    @Override
    public String getEventId() {
        return this.eventId;
    }

    /**
     * Get RuleChain.
     * @return RuleChain string
     */
    @Override
    public String getRuleChain() {
        return this.ruleChain;
    }

    /**
     * Get Via.
     * @return Via string
     */
    @Override
    public String getVia() {
        return this.via;
    }

}
