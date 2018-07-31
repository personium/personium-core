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
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.SAXException;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.UriUtils;

/**
 * JaxRS Resource オブジェクトから処理の委譲を受けてDav関連の永続化を除く処理を行うクラス.
 */
public class CellRsCmp extends DavRsCmp {

    /** Name of property in which the URL of the relay destination is described. */
    private static final String RELAY_HTML_URL = "relayhtmlurl";

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
     * このリソースのURLを返します.
     * @return URL文字列
     */
    public String getUrl() {
        return this.cell.getUrl();
    }

    /**
     * リソースが所属するCellを返す.
     * @return Cellオブジェクト
     */
    public Cell getCell() {
        return this.cell;
    }

    /**
     * リソースが所属するBoxを返す.
     * @return Boxオブジェクト
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
     * ACL情報を確認し、アクセス可能か判断する.
     * @param ac アクセスコンテキスト
     * @param privilege ACLのプリビレッジ（readとかwrite）
     * @return boolean
     */
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {

        // davCmpが無い（存在しないリソースが指定された）場合はそのリソースのACLチェック飛ばす
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

        // Basic認証できるリソースかをチェック
        this.accessContext.updateBasicAuthenticationStateForResource(null);

        // アクセス権チェック
        if (!this.hasPrivilege(ac, privilege)) {
            // トークンの有効性チェック
            // トークンがINVALIDでもACL設定でPrivilegeがallに設定されているとアクセスを許可する必要があるのでこのタイミングでチェック
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

        // Convert personium-localunit and personium-localcell.
        relayHtmlUrl = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), relayHtmlUrl);
        relayHtmlUrl = UriUtils.convertSchemeFromLocalCellToHttp(cell.getUrl(), relayHtmlUrl);

        // Validate relayHtmlUrl.
        validateRelayHtmlUrl(relayHtmlUrl);

        HttpGet req = new HttpGet(relayHtmlUrl);
        // set headers
        req.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        req.addHeader(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VERSION, PersoniumUnitConfig.getCoreVersion());
        req.addHeader(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_CELLURL, cell.getUrl());

        // GET html.
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
        HttpResponse res;
        try {
            res = client.execute(req);
        } catch (ClientProtocolException e) {
            throw PersoniumCoreException.UI.INVALID_HTTP_RESPONSE.params(relayHtmlUrl).reason(e);
        } catch (IOException e) {
            throw PersoniumCoreException.UI.CONNECTION_FAILED.params(relayHtmlUrl).reason(e);
        }

        // Check response media type.
        Header contentType = res.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        if (!MediaType.TEXT_HTML.equals(contentType.getValue())) {
            throw PersoniumCoreException.NetWork.UNEXPECTED_RESPONSE.params(relayHtmlUrl, MediaType.TEXT_HTML);
        }

        return res;
    }

    /**
     * Validate relayhtmlurl.
     * @param relayHtmlUrl target url
     */
    private void validateRelayHtmlUrl(String relayHtmlUrl) {
        if (StringUtils.isEmpty(relayHtmlUrl)) {
            throw PersoniumCoreException.UI.NOT_CONFIGURED_PROPERTY.params(RELAY_HTML_URL);
        }
        try {
            URL url = new URL(relayHtmlUrl);
            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw PersoniumCoreException.UI.PROPERTY_NOT_URL.params(RELAY_HTML_URL);
            }
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.UI.PROPERTY_NOT_URL.params(RELAY_HTML_URL);
        }
    }

    /**
     * 認証に使用できるAuth Schemeを取得する.
     * @return 認証に使用できるAuth Scheme
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
