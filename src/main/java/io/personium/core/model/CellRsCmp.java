/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core.model;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.wink.webdav.model.Propfind;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.UriUtils;

/**
 * A class that performs processing except delegation of processing from JaxRS Resource object excluding Dav related persistence.
 */
public class CellRsCmp extends DavRsCmp {
    /** Logger. */
    static Logger log = LoggerFactory.getLogger(CellRsCmp.class);

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
    @Override
    public AccessContext getAccessContext() {
        return this.accessContext;
    }

    /**
     * Check ACL information and judge whether access is possible.
     * @param ac access context
     * @param privilege Privilege of ACL (read or write)
     * @return boolean
     */
    @Override
    public boolean hasSubjectPrivilege(Privilege privilege) {

        // If davCmp does not exist (resource that does not exist is specified)
        // skip ACL check for that resource
        if (this.davCmp != null
                && this.getAccessContext().hasSubjectPrivilegeForAcl(this.davCmp.getAcl(), privilege)) {
            return true;
        }
        return false;
    }

    /**
     * Performs access control.
     * @param ac Access context
     * @param privilege Required privilege
     */
    @Override
    public void checkAccessContext(Privilege privilege) {
        AccessContext ac = this.getAccessContext();
        // If UnitUser token, then OK.
        if (ac.isUnitUserToken(privilege)) {
            return;
        }

        //Check if basic authentication can be done
        this.accessContext.updateBasicAuthenticationStateForResource(null);

        //Access right check
        if (!this.hasSubjectPrivilege(privilege)) {
            //Check the validity of the token
            // Even if the token is INVALID, if the ACL setting and Privilege is set to all,
            // it is necessary to permit access, so check at this timing
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(getAcceptableAuthScheme());
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(
                        ac.getRealm(), getAcceptableAuthScheme());
            }
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }

        if (privilege instanceof CellPrivilege
                && !this.accessContext.hasScopeCellPrivilege((CellPrivilege)privilege)) {
            throw PersoniumCoreException.Auth.INSUFFICIENT_SCOPE.params(privilege.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected List<org.apache.wink.webdav.model.Response> createChildrenDavResponseList(String reqUri,
            Propfind propfind, boolean canAclRead) {
        // Resources directly below Cell are not displayed.
        return new ArrayList<>();
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
        relayHtmlUrl = UriUtils.convertSchemeFromLocalUnitToHttp(relayHtmlUrl);
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
        authorizationHtmlUrl = UriUtils.convertSchemeFromLocalUnitToHttp(authorizationHtmlUrl);
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
        authorizationPasswordHtmlUrl = UriUtils.convertSchemeFromLocalUnitToHttp(authorizationPasswordHtmlUrl);
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
        CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
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
     * Request get accounts not to recording authentication history.
     * @return Http response
     */
    public List<String> getAccountsNotRecordingAuthHistory() {
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
     * Check if the target account records authentication history.
     * @param accountId account ID
     * @param accountName account name
     * @return "true" is records authentication history
     */
    public boolean isRecordingAuthHistory(String accountId, String accountName) {
        if (StringUtils.isEmpty(accountId) || StringUtils.isEmpty(accountName)) {
            return false;
        }
        List<String> ineligibleAccountList = this.getAccountsNotRecordingAuthHistory();
        if (ineligibleAccountList == null) {
            return true;
        }
        return !ineligibleAccountList.contains(accountName);
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
