/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.CellLocalRefreshToken;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.TransCellRefreshToken;
import io.personium.core.auth.CellPrivilege;
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
     * @param host Host Header
     * @param formParams Body parameters
     * @return JAX-RS Response Object
     */
    @POST
    public final Response introspect(@Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.HOST) final String host,
            MultivaluedMap<String, String> formParams) {

        Map<String, Object> map = new HashMap<>();
        map.put(RESP_ACTIVE, false);

        if (formParams == null) {
            return ResourceUtils.responseBuilderJson(map).build();
        }
        String token = formParams.getFirst(PARAM_TOKEN);

        try {
            AbstractOAuth2Token tk = AbstractOAuth2Token.parse(token, this.cell.getUrl(), host);
            if (!tk.isExpired()) {
                String issuer = tk.getIssuer();
                int expirationTime = tk.getIssuedAt() + tk.expiresIn();
                if (tk instanceof AccountAccessToken
                    || tk instanceof CellLocalRefreshToken) {
                    if (issuer.equals(this.cell.getUrl())) {
                        map.put(RESP_ACTIVE, true);
                        map.put(RESP_CLIENT_ID, tk.getSchema());
                        map.put(RESP_EXPIRATION_TIME, expirationTime);
                        map.put(RESP_ISSUEDAT, tk.getIssuedAt());
                        map.put(RESP_SUBJECT, issuer + "#" + tk.getSubject());
                        map.put(RESP_ISSUER, issuer);
                        map.put(RESP_EXT_ROLES,
                                tk.getRoles().stream().map(role -> role.createUrl()).collect(Collectors.toList()));
                    }
                } else if (tk instanceof CellLocalAccessToken
                           || tk instanceof TransCellRefreshToken
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
                                tk.getRoles().stream().map(role -> role.createUrl()).collect(Collectors.toList()));
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
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.AUTH_READ);
        return ResourceUtils.responseBuilderForOptions(HttpMethod.POST)
                            .build();
    }

}
