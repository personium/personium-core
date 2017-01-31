/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpHeaders;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.DcCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.rs.cell.CellResource;
import io.personium.core.rs.unit.UnitCtlResource;

/**
 * すべてのリクエストの入り口となるJax-RS Resource.
 * ここでURL情報の取得、Authorizationヘッダの取得を行い、
 * これら情報はサブリソースに渡してゆく.
 */
@Path("")
public class FacadeResource {

    /**
     * クッキー認証の際、クッキー内に埋め込まれている情報のキー.
     */
    public static final String DC_COOKIE_KEY = "dc_cookie";
    /**
     * クッキー認証の際、クエリパラメタに指定されるキー.
     */
    public static final String COOKIE_PEER_QUERY_KEY = "dc_cookie_peer";

    /**
     * @param cookieAuthValue クッキー内の dc_cookieキーに指定された値
     * @param cookiePeer dc_cookie_peerクエリに指定された値
     * @param authzHeaderValue Authorization ヘッダ
     * @param host Host ヘッダ
     * @param uriInfo UriInfo
     * @param xDcUnitUser X-Dc-UnitUserヘッダ
     * @param httpServletRequest HttpServletRequest
     * @return CellResourceオブジェクトまたはResponseオブジェクト
     */
    @Path("{path1}")
    public final Object facade(
            @CookieParam(DC_COOKIE_KEY) final String cookieAuthValue,
            @QueryParam(COOKIE_PEER_QUERY_KEY) final String cookiePeer,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeaderValue,
            @HeaderParam(HttpHeaders.HOST) final String host,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_UNIT_USER) final String xDcUnitUser,
            @Context final UriInfo uriInfo,
            @Context HttpServletRequest httpServletRequest) {
        Cell cell = ModelFactory.cell(uriInfo);
        AccessContext ac = AccessContext.create(authzHeaderValue,
                uriInfo, cookiePeer, cookieAuthValue, cell, uriInfo.getBaseUri().toString(),
                host, xDcUnitUser);
        if (cell == null) {
            throw DcCoreException.Dav.CELL_NOT_FOUND;
        }

        long cellStatus = CellLockManager.getCellStatus(cell.getId());
        if (cellStatus == CellLockManager.CELL_STATUS_BULK_DELETION) {
            throw DcCoreException.Dav.CELL_NOT_FOUND;
        }

        CellLockManager.incrementReferenceCount(cell.getId());
        httpServletRequest.setAttribute("cellId", cell.getId());
        return new CellResource(ac);
    }

    /**
     * @param cookieAuthValue クッキー内の dc_cookieキーに指定された値
     * @param cookiePeer dc_cookie_peerクエリに指定された値
     * @param authzHeaderValue Authorization ヘッダ
     * @param host Host ヘッダ
     * @param xDcUnitUser ヘッダ
     * @param uriInfo UriInfo
     * @return UnitCtlResourceオブジェクト
     */
    @Path("__ctl")
    public final UnitCtlResource ctl(
            @CookieParam(DC_COOKIE_KEY) final String cookieAuthValue,
            @QueryParam(COOKIE_PEER_QUERY_KEY) final String cookiePeer,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeaderValue,
            @HeaderParam(HttpHeaders.HOST) final String host,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_UNIT_USER) final String xDcUnitUser,
            @Context final UriInfo uriInfo) {
        AccessContext ac = AccessContext.create(authzHeaderValue,
                uriInfo, cookiePeer, cookieAuthValue, null, uriInfo.getBaseUri().toString(),
                host, xDcUnitUser);
        return new UnitCtlResource(ac, uriInfo);
    }

    /**
     * @param authzHeaderValue Authorization ヘッダ
     * @param host Host ヘッダ
     * @param xDcUnitUser ヘッダ
     * @param uriInfo UriInfo
     * @return UnitCtlResourceオブジェクト
     */
    @Path("__status")
    public final StatusResource status(
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeaderValue,
            @HeaderParam(HttpHeaders.HOST) final String host,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_UNIT_USER) final String xDcUnitUser,
            @Context final UriInfo uriInfo) {
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
