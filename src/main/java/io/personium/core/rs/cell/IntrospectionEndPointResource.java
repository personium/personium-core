/**
 * Personium
 * Copyright 2018-2021 Personium Project Authors
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
package io.personium.core.rs.cell;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.ResidentLocalAccessToken;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource class for OAuth 2.0 Token Introspection.
 */
public class IntrospectionEndPointResource {

    static Logger logger = LoggerFactory.getLogger(IntrospectionEndPointResource.class);

    private static final String PARAM_TOKEN = "token";

    private static final String RESP_ACTIVE = "active";
    private static final String RESP_CLIENT_ID = "client_id";
    private static final String RESP_EXPIRATION_TIME = "exp";
    private static final String RESP_ISSUEDAT = "iat";
    private static final String RESP_SUBJECT = "sub";
    private static final String RESP_AUDIENCE = "aud";
    private static final String RESP_ISSUER = "iss";

    private static final String RESP_EXT_ROLES = "p_roles";

    private final Cell cell;
    private final DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param cell  Cell
     * @param davRsCmp davRsCmp
     */
    public IntrospectionEndPointResource(final Cell cell, final DavRsCmp davRsCmp) {
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }

    /**
     * OAuth2.0 Introspection Endpoint.
     * @param uriInfo  URI information
     * @param authzHeader Authorization Header
     * @param host Host Header
     * @param formParams Body parameters
     * @return JAX-RS Response Object
     */
    @POST
    public final Response introspect(@Context final UriInfo uriInfo,
                                     @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader,
                                     @HeaderParam(HttpHeaders.HOST) final String host,
                                     MultivaluedMap<String, String> formParams) {

        AccessContext accessContext = this.davRsCmp.getAccessContext();
        if (AccessContext.TYPE_ANONYMOUS.equals(accessContext.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(cell.getUrl(), AcceptableAuthScheme.ALL);
        }

        String schema;

        if (AccessContext.TYPE_INVALID.equals(accessContext.getType())) {
            String[] idpw = CommonUtils.parseBasicAuthzHeader(authzHeader);
            if (idpw != null) {
                String username = PersoniumUnitConfig.getIntrospectUsername();
                String password = PersoniumUnitConfig.getIntrospectPassword();
                if (idpw[0].equals(username) && idpw[1].equals(password)) {
                    schema = null;
                } else if (TokenEndPointResource.clientAuth(idpw[0], idpw[1], null, cell.getUrl()) != null) {
                    schema = idpw[0];
                } else {
                    // no privilege
                    throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
                }
            } else {
                // no privilege
                throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            }
        } else {
            if (accessContext.isUnitUserToken()) {
                schema = null;
            } else {
                schema = accessContext.getSchema();
                if (schema == null || schema.isEmpty()) {
                    // no privilege
                    throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
                }
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put(RESP_ACTIVE, false);

        if (formParams == null) {
            return ResourceUtils.responseBuilderJson(map).build();
        }
        String token = formParams.getFirst(PARAM_TOKEN);

        try {
            AbstractOAuth2Token tk = AbstractOAuth2Token.parse(token, this.cell.getUrl(), host);
            if (!tk.isExpired() && (schema == null || schema != null && schema.equals(tk.getSchema()))) {
                String issuer = tk.getIssuer();
                int expirationTime = tk.getIssuedAt() + tk.expiresIn();
                if (tk instanceof ResidentLocalAccessToken
                    || tk instanceof ResidentRefreshToken) {
                    if (issuer.equals(this.cell.getUrl())) {
                        map.put(RESP_ACTIVE, true);
                        map.put(RESP_CLIENT_ID, tk.getSchema());
                        map.put(RESP_EXPIRATION_TIME, expirationTime);
                        map.put(RESP_ISSUEDAT, tk.getIssuedAt());
                        map.put(RESP_SUBJECT, issuer + "#" + tk.getSubject());
                        map.put(RESP_ISSUER, issuer);
                        map.put(RESP_EXT_ROLES,
                                tk.getRoleList().stream().map(role -> role.toRoleInstanceURL()).collect(Collectors.toList()));
                    }
                } else if (tk instanceof VisitorLocalAccessToken
                           || tk instanceof VisitorRefreshToken
                           || tk instanceof TransCellAccessToken) {
                    IAccessToken iat = (IAccessToken) tk;
                    String audience = iat.getTarget();
                    if (issuer.equals(this.cell.getUrl())
                        || this.cell.getUrl().equals(audience)) {
                        map.put(RESP_ACTIVE, true);
                        map.put(RESP_CLIENT_ID, tk.getSchema());
                        map.put(RESP_EXPIRATION_TIME, expirationTime);
                        map.put(RESP_ISSUEDAT, tk.getIssuedAt());
                        map.put(RESP_SUBJECT, tk.getSubject());
                        map.put(RESP_AUDIENCE, audience);
                        map.put(RESP_ISSUER, issuer);
                        map.put(RESP_EXT_ROLES,
                                tk.getRoleList().stream().map(role -> role.toRoleInstanceURL()).collect(Collectors.toList()));
                    }
                }
            }
        } catch (Exception e) {
            // inactive token
            logger.debug("token parse error");
        }

        return ResourceUtils.responseBuilderJson(map).build();
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // Access Control
        this.davRsCmp.checkAccessContext(CellPrivilege.AUTH_READ);
        return ResourceUtils.responseBuilderForOptions(HttpMethod.POST)
                            .build();
    }

}
