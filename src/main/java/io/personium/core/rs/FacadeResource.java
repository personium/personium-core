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
package io.personium.core.rs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.rs.cell.CellResource;
import io.personium.core.rs.unit.UnitResource;
import io.personium.core.utils.ResourceUtils;

/**
 * Jax - RS Resource which will be the entrance of all requests.
 * Here, acquisition of URL information and acquisition of Authorization header are performed,
 * We pass these information to subresources.
 */
@Path("")
public class FacadeResource {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(FacadeResource.class);

    /** For cookie authentication, the key of the information embedded in the cookie. */
    public static final String P_COOKIE_KEY = "p_cookie";
    /** The key specified in the query parameter during cookie authentication. */
    public static final String COOKIE_PEER_QUERY_KEY = "p_cookie_peer";

    /**
     * @param cookieAuthValue The value specified for the p_cookie key in the cookie
     * @param cookiePeer p_cookie_peer Value specified in the query
     * @param headerAuthz Authorization header
     * @param headerHost Host header
     * @param uriInfo UriInfo
     * @param headerPersoniumUnitUser X-Personium-UnitUser header
     * @param headerPersoniumRequestKey X-Personium-RequestKey header
     * @param headerPersoniumEventId X-Personium-EventId header
     * @param headerPersoniumRuleChain X-Personium-RuleChain header
     * @param headerPersoniumVia X-Personium-Via header
     * @param httpServletRequest HttpServletRequest
     * @return CellResource object or Response object
     */
    @Path("/")
    public Object facade(
            @CookieParam(P_COOKIE_KEY) final String cookieAuthValue,
            @QueryParam(COOKIE_PEER_QUERY_KEY) final String cookiePeer,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String headerAuthz,
            @HeaderParam(HttpHeaders.HOST) final String headerHost,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_UNIT_USER) final String headerPersoniumUnitUser,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) final String headerPersoniumRequestKey,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_EVENTID) final String headerPersoniumEventId,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RULECHAIN) final String headerPersoniumRuleChain,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VIA) final String headerPersoniumVia,
            @Context final UriInfo uriInfo,
            @Context HttpServletRequest httpServletRequest) {

        if (log.isDebugEnabled()) {
            log.debug("Call API '" + uriInfo.getAbsolutePath().toString() + "'.");
            log.debug("    p_cookie: " + cookieAuthValue);
            log.debug("    p_cookie_peer: " + cookiePeer);
            log.debug("    Authorization: " + headerAuthz);
            log.debug("    X-Personium-Unit-User: " + headerPersoniumUnitUser);
            log.debug("    X-Personium-RequestKey: " + headerPersoniumRequestKey);
            log.debug("    X-Personium-EventId: " + headerPersoniumEventId);
            log.debug("    X-Personium-RuleChain: " + headerPersoniumRuleChain);
            log.debug("    X-Personium-Via: " + headerPersoniumVia);
        }

        if (PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
            return new UnitResource(cookieAuthValue, cookiePeer, headerAuthz, headerHost,
                    headerPersoniumUnitUser, uriInfo);
        }

        String accessUrl = uriInfo.getBaseUri().toString();
        String configUrl = PersoniumUnitConfig.getBaseUrl();
        if (configUrl.equals(accessUrl)) {
            return new UnitResource(cookieAuthValue, cookiePeer, headerAuthz, headerHost,
                    headerPersoniumUnitUser, uriInfo);
        } else {
            // {CellName}.{FQDN} access
            String cellName = headerHost.split("\\.")[0];
            Cell cell = ModelFactory.cellFromName(cellName);
            if (cell == null) {
                throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
            }
            AccessContext ac = AccessContext.create(headerAuthz, uriInfo, cookiePeer, cookieAuthValue, cell,
                    uriInfo.getBaseUri().toString(), headerHost, headerPersoniumUnitUser);

            CellLockManager.STATUS cellStatus = CellLockManager.getCellStatus(cell.getId());
            if (CellLockManager.STATUS.BULK_DELETION.equals(cellStatus)) {
                throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
            }

            CellLockManager.incrementReferenceCount(cell.getId());
            httpServletRequest.setAttribute("cellId", cell.getId());
            String requestKey = ResourceUtils.validateXPersoniumRequestKey(headerPersoniumRequestKey);
            if (headerPersoniumRequestKey == null) {
                log.debug("    Create RequestKey: " + requestKey);
            }
            return new CellResource(ac, requestKey,
                    headerPersoniumEventId, headerPersoniumRuleChain, headerPersoniumVia, httpServletRequest);
        }
    }
}
