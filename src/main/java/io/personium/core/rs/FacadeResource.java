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
 * すべてのリクエストの入り口となるJax-RS Resource.
 * ここでURL情報の取得、Authorizationヘッダの取得を行い、
 * これら情報はサブリソースに渡してゆく.
 */
@Path("")
public class FacadeResource {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(FacadeResource.class);

    /** クッキー認証の際、クッキー内に埋め込まれている情報のキー. */
    public static final String P_COOKIE_KEY = "p_cookie";
    /** クッキー認証の際、クエリパラメタに指定されるキー. */
    public static final String COOKIE_PEER_QUERY_KEY = "p_cookie_peer";

//    /**
//     * @param cookieAuthValue クッキー内の p_cookieキーに指定された値
//     * @param cookiePeer p_cookie_peerクエリに指定された値
//     * @param authzHeaderValue Authorization ヘッダ
//     * @param host Host ヘッダ
//     * @param uriInfo UriInfo
//     * @param xPersoniumUnitUser X-Personium-UnitUser header
//     * @param xPersoniumRequestKey X-Personium-RequestKey header
//     * @param xPersoniumEventId X-Personium-EventId header
//     * @param xPersoniumRuleChain X-Personium-RuleChain header
//     * @param xPersoniumVia X-Personium-Via header
//     * @param httpServletRequest HttpServletRequest
//     * @return CellResourceオブジェクトまたはResponseオブジェクト
//     */
//    @Path("{path1}")
//    public final Object facade(
//            @CookieParam(P_COOKIE_KEY) final String cookieAuthValue,
//            @QueryParam(COOKIE_PEER_QUERY_KEY) final String cookiePeer,
//            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeaderValue,
//            @HeaderParam(HttpHeaders.HOST) final String host,
//            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_UNIT_USER) final String xPersoniumUnitUser,
//            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) final String xPersoniumRequestKey,
//            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_EVENTID) final String xPersoniumEventId,
//            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RULECHAIN) final String xPersoniumRuleChain,
//            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VIA) final String xPersoniumVia,
//            @Context final UriInfo uriInfo,
//            @Context HttpServletRequest httpServletRequest) {
//        Cell cell = ModelFactory.cell(uriInfo);
//        AccessContext ac = AccessContext.create(authzHeaderValue,
//                uriInfo, cookiePeer, cookieAuthValue, cell, uriInfo.getBaseUri().toString(),
//                host, xPersoniumUnitUser);
//        if (cell == null) {
//            throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
//        }
//
//        CellLockManager.STATUS cellStatus = CellLockManager.getCellStatus(cell.getId());
//        if (CellLockManager.STATUS.BULK_DELETION.equals(cellStatus)) {
//            throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
//        }
//
//        CellLockManager.incrementReferenceCount(cell.getId());
//        httpServletRequest.setAttribute("cellId", cell.getId());
//        if (xPersoniumRequestKey != null) {
//            ResourceUtils.validateXPersoniumRequestKey(xPersoniumRequestKey);
//        }
//        return new CellResource(ac, xPersoniumRequestKey,
//                xPersoniumEventId, xPersoniumRuleChain, xPersoniumVia, httpServletRequest);
//    }

    /**
     * @param cookieAuthValue クッキー内の p_cookieキーに指定された値
     * @param cookiePeer p_cookie_peerクエリに指定された値
     * @param headerAuthz Authorization ヘッダ
     * @param headerHost Host ヘッダ
     * @param uriInfo UriInfo
     * @param headerPersoniumUnitUser X-Personium-UnitUser header
     * @param headerPersoniumRequestKey X-Personium-RequestKey header
     * @param headerPersoniumEventId X-Personium-EventId header
     * @param headerPersoniumRuleChain X-Personium-RuleChain header
     * @param headerPersoniumVia X-Personium-Via header
     * @param httpServletRequest HttpServletRequest
     * @return CellResourceオブジェクトまたはResponseオブジェクト
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
        String accessUrl = uriInfo.getBaseUri().toString();
        String configUrl = PersoniumUnitConfig.getBaseUrl();
        if (!accessUrl.contains(".")) {
            return new UnitResource(cookieAuthValue, cookiePeer, headerAuthz, headerHost,
                    headerPersoniumUnitUser, uriInfo);
        }
        // {CellName}.{FQDN}アクセスの場合のみCellResourceに処理を渡す
//        String requestURIHost = uriInfo.getBaseUri().getHost();
        String cellName = headerHost.split("\\.")[0];
        // CellName部分を除いたURLがConfigと一致するか
        String escapeUrl = accessUrl.replaceFirst(cellName + "\\.", "");
        if (configUrl.equals(escapeUrl)) {
            Cell cell = ModelFactory.cellFromName(cellName);
            AccessContext ac = AccessContext.create(headerAuthz, uriInfo, cookiePeer, cookieAuthValue, cell,
                    uriInfo.getBaseUri().toString(), headerHost, headerPersoniumUnitUser);
            if (cell == null) {
                throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
            }

            CellLockManager.STATUS cellStatus = CellLockManager.getCellStatus(cell.getId());
            if (CellLockManager.STATUS.BULK_DELETION.equals(cellStatus)) {
                throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
            }

            CellLockManager.incrementReferenceCount(cell.getId());
            httpServletRequest.setAttribute("cellId", cell.getId());
            if (headerPersoniumRequestKey != null) {
                ResourceUtils.validateXPersoniumRequestKey(headerPersoniumRequestKey);
            }
            return new CellResource(ac, headerPersoniumRequestKey,
                    headerPersoniumEventId, headerPersoniumRuleChain, headerPersoniumVia, httpServletRequest);
        } else {
            return new UnitResource(cookieAuthValue, cookiePeer, headerAuthz, headerHost,
                    headerPersoniumUnitUser, uriInfo);
        }
    }

//    /**
//     * @param cookieAuthValue クッキー内の p_cookieキーに指定された値
//     * @param cookiePeer p_cookie_peerクエリに指定された値
//     * @param authzHeaderValue Authorization ヘッダ
//     * @param host Host ヘッダ
//     * @param xPersoniumUnitUser ヘッダ
//     * @param uriInfo UriInfo
//     * @return UnitCtlResourceオブジェクト
//     */
//    @Path("__ctl")
//    public final UnitCtlResource ctl(
//            @CookieParam(P_COOKIE_KEY) final String cookieAuthValue,
//            @QueryParam(COOKIE_PEER_QUERY_KEY) final String cookiePeer,
//            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeaderValue,
//            @HeaderParam(HttpHeaders.HOST) final String host,
//            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_UNIT_USER) final String xPersoniumUnitUser,
//            @Context final UriInfo uriInfo) {
//        AccessContext ac = AccessContext.create(authzHeaderValue,
//                uriInfo, cookiePeer, cookieAuthValue, null, uriInfo.getBaseUri().toString(),
//                host, xPersoniumUnitUser);
//        return new UnitCtlResource(ac, uriInfo);
//    }
//
//    /**
//     * @param authzHeaderValue Authorization ヘッダ
//     * @param host Host ヘッダ
//     * @param xPersoniumUnitUser ヘッダ
//     * @param uriInfo UriInfo
//     * @return UnitCtlResourceオブジェクト
//     */
//    @Path("__status")
//    public final StatusResource status(
//            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeaderValue,
//            @HeaderParam(HttpHeaders.HOST) final String host,
//            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_UNIT_USER) final String xPersoniumUnitUser,
//            @Context final UriInfo uriInfo) {
//        return new StatusResource();
//    }
//
//    static final String CROSSDOMAIN_XML = PersoniumCoreUtils.readStringResource("crossdomain.xml", CharEncoding.UTF_8);
//
//    /**
//     * Crossdomain.xmlを返します。
//     * @return Crossdomain.xmlの文字列.
//     */
//    @Path("crossdomain.xml")
//    @Produces(MediaType.APPLICATION_XML)
//    @GET
//    public final String crosdomainXml() {
//        return CROSSDOMAIN_XML;
//    }
}
