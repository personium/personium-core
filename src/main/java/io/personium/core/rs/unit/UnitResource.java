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
package io.personium.core.rs.unit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.CharEncoding;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.rs.StatusResource;
import io.personium.core.rs.cell.CellResource;
import io.personium.core.utils.ResourceUtils;

/**
 * Jax-RS Resource handling Personium Unit Level API.
 */
public class UnitResource {

    /** Cookie : p_cookie. */
    private String cookieAuthValue;
    /** Query : p_cookie_peer. */
    private String cookiePeer;
    /** Header : Authorization. */
    private String headerAuthz;
    /** Header : Host. */
    private String headerHost;
    /** Header : X-Personium-UnitUser. */
    private String headerPersoniumUnitUser;
//    /** Request uri host. */
//    private String requestURIHost;
    /** UriInfo. */
    private UriInfo uriInfo;
    /** Request uri base. */
    private String requestBaseUri;

    /**
     * Constructor.
     * @param cookieAuthValue Cookie : p_cookie
     * @param cookiePeer Query : p_cookie_peer
     * @param headerAuthz Header : Authorization
     * @param headerHost Header : Host
     * @param headerPersoniumUnitUser Header : X-Personium-UnitUser
     * @param uriInfo UriInfo
     * @param requestBaseUri Request uri base
     */
    public UnitResource(String cookieAuthValue, String cookiePeer, String headerAuthz, String headerHost,
            String headerPersoniumUnitUser, UriInfo uriInfo) {
        this.cookieAuthValue = cookieAuthValue;
        this.cookiePeer = cookiePeer;
        this.headerAuthz = headerAuthz;
        this.headerHost = headerHost;
        this.headerPersoniumUnitUser = headerPersoniumUnitUser;
//        this.requestURIHost = requestURIHost;
        this.uriInfo = uriInfo;
        this.requestBaseUri = uriInfo.getBaseUri().toString();
    }

    /**
     * @param xPersoniumRequestKey X-Personium-RequestKey header
     * @param xPersoniumEventId X-Personium-EventId header
     * @param xPersoniumRuleChain X-Personium-RuleChain header
     * @param xPersoniumVia X-Personium-Via header
     * @param httpServletRequest HttpServletRequest
     * @param cellName CellName
     * @return CellResource
     */
    @Path("{cellName}")
    public final Object cell(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) final String xPersoniumRequestKey,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_EVENTID) final String xPersoniumEventId,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RULECHAIN) final String xPersoniumRuleChain,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VIA) final String xPersoniumVia,
            @Context HttpServletRequest httpServletRequest,
            @PathParam("cellName") String cellName) {
        Cell cell = ModelFactory.cellFromName(cellName);
        AccessContext ac = AccessContext.create(headerAuthz,
                uriInfo, cookiePeer, cookieAuthValue, cell, requestBaseUri,
                headerHost, headerPersoniumUnitUser);
        if (cell == null) {
            throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
        }

        CellLockManager.STATUS cellStatus = CellLockManager.getCellStatus(cell.getId());
        if (CellLockManager.STATUS.BULK_DELETION.equals(cellStatus)) {
            throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
        }

        CellLockManager.incrementReferenceCount(cell.getId());
        httpServletRequest.setAttribute("cellId", cell.getId());
        if (xPersoniumRequestKey != null) {
            ResourceUtils.validateXPersoniumRequestKey(xPersoniumRequestKey);
        }
        return new CellResource(ac, xPersoniumRequestKey,
                xPersoniumEventId, xPersoniumRuleChain, xPersoniumVia, httpServletRequest);
    }

    /**
     * @return UnitCtlResource
     */
    @Path("__ctl")
    public final UnitCtlResource ctl() {
        AccessContext ac = AccessContext.create(headerAuthz,
                uriInfo, cookiePeer, cookieAuthValue, null, requestBaseUri,
                headerHost, headerPersoniumUnitUser);
        return new UnitCtlResource(ac);
    }

    /**
     * @return UnitCtlResource
     */
    @Path("__status")
    public final StatusResource status() {
        return new StatusResource();
    }

    static final String CROSSDOMAIN_XML = PersoniumCoreUtils.readStringResource("crossdomain.xml", CharEncoding.UTF_8);

    /**
     * Crossdomain.xmlを返します。
     * @return Crossdomain.xmlの文字列.
     */
    @Path("crossdomain.xml")
    @Produces(MediaType.APPLICATION_XML)
    @GET
    public final String crosdomainXml() {
        return CROSSDOMAIN_XML;
    }
}
