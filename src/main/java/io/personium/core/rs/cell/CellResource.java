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
package io.personium.core.rs.cell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.wink.webdav.WebDAVMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.UnitUserLockManager;
import io.personium.core.rs.box.BoxResource;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource handling Cell Level Api.
 *  logics for the url path /{cell name}.
 */
public class CellResource {
    static Logger log = LoggerFactory.getLogger(CellResource.class);

    Cell cell;
    CellCmp cellCmp;
    CellRsCmp cellRsCmp;
    AccessContext accessContext;

    /**
     * constructor.
     * @param accessContext AccessContext
     * @param requestKey X-Personium-RequestKey header
     * @param eventId X-Personium-EventId header
     * @param ruleChain X-Personium-RuleChain header
     * @param via X-Personium-Via header
     * @param httpServletRequest HttpServletRequest
     */
    public CellResource(
            final AccessContext accessContext,
            final String requestKey,
            final String eventId,
            final String ruleChain,
            final String via,
            HttpServletRequest httpServletRequest) {
        // Cellが存在しないときは例外
        this.accessContext = accessContext;
        this.cell = this.accessContext.getCell();
        if (this.cell == null) {
            throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
        }
        this.cellCmp = ModelFactory.cellCmp(this.cell);
        if (!this.cellCmp.exists()) {
            // クリティカルなタイミングでCellが削除された場合
            throw PersoniumCoreException.Dav.CELL_NOT_FOUND;
        }

        this.cellRsCmp = new CellRsCmp(this.cellCmp, this.cell, this.accessContext,
                requestKey, eventId, ruleChain, via);
        checkReferenceMode();

        // If cell status is import failed, APIs other than import or token or BulkDeletion are not accepted.
        if (Cell.STATUS_IMPORT_ERROR.equals(cellCmp.getCellStatus())) {
            String[] paths = accessContext.getUriInfo().getPath().split("/");
            // Since the Cell name is stored at the first, check the second value.
            // ex.["testcell", "__token"]
            if (paths.length <= 1) {
                if (!HttpMethod.DELETE.equals(httpServletRequest.getMethod())) {
                    throw PersoniumCoreException.Common.CELL_STATUS_IMPORT_FAILED;
                }
            } else if (!"__import".equals(paths[1]) && !"__token".equals(paths[1])) {
                throw PersoniumCoreException.Common.CELL_STATUS_IMPORT_FAILED;
            }
        }
    }

    private void checkReferenceMode() {
        Cell cellObj = accessContext.getCell();
        String unitPrefix = PersoniumUnitConfig.getEsUnitPrefix();
        String owner = cellObj.getOwner();

        if (owner == null) {
            owner = "anon";
        } else {
            owner = IndexNameEncoder.encodeEsIndexName(owner);
        }
        if (UnitUserLockManager.hasLockObject(unitPrefix + "_" + owner)) {
            throw PersoniumCoreException.Server.SERVICE_MENTENANCE_RESTORE;
        }
    }

    /*
     * static private Cache cache = CacheManager.getInstance().getCache("box-cache"); static {
     * cache.addPropertyChangeListener(new PropertyChangeListener() {
     * @Override public void propertyChange(PropertyChangeEvent arg0) {
     * System.out.println(arg0.toString()); } }); }
     */
    /**
     * handler for GET Method.
     * @param httpHeaders Request headers
     * @return JAX-RS Response Object
     */
    @GET
    public Response getSvcDoc(@Context HttpHeaders httpHeaders) {
        if (httpHeaders.getAcceptableMediaTypes().contains(MediaType.APPLICATION_XML_TYPE)) {
            StringBuffer sb = new StringBuffer();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append("<cell xmlns=\"urn:x-personium:xmlns\">");
            sb.append("<uuid>" + this.cell.getId() + "</uuid>");
            sb.append("<ctl>" + this.cell.getUrl() + "__ctl/" + "</ctl>");
            sb.append("</cell>");
            return Response.ok().entity(sb.toString()).build();
        } else {
            HttpResponse res = cellRsCmp.requestGetRelayHtml();
            int statusCode = res.getStatusLine().getStatusCode();
            HttpEntity entity = res.getEntity();
            StreamingOutput streamingOutput = new StreamingOutput() {
                @Override
                public void write(final OutputStream os) throws IOException {
                    try (InputStream in = entity.getContent()) {
                        int chr;
                        while ((chr = in.read()) != -1) {
                            os.write(chr);
                        }
                    }
                }
            };
            return Response.status(statusCode).entity(streamingOutput).build();
        }
    }

    /**
     * handler for DELETE Method.
     * ie, Recursive Cell Deletion.
     * @param recursiveHeader X-Personium-Recursive Header
     * @return JAX-RS Response Object
     */
    @WriteAPI
    @DELETE
    public Response cellBulkDeletion(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE) final String recursiveHeader) {
        // X-Personium-Recursiveヘッダの指定が"true"でない場合はエラーとする
        if (!"true".equals(recursiveHeader)) {
            throw PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
        }
        // アクセス権限の確認を実施する
        // ユニットマスター、ユニットユーザ、ユニットローカルユニットユーザ以外は権限エラーとする
        String cellOwner = this.cell.getOwner();
        checkAccessContextForCellBulkDeletion(cellOwner);

        String cellId = this.cell.getId();
        String cellName = this.cell.getName();
        String unitUserName = this.cell.getDataBundleName();
        String cellInfoLog = String.format(" CellId:[%s], CellName:[%s], CellUnitUserName:[%s]", cellId, cellName,
                unitUserName);
        log.info("Cell Bulk Deletion." + cellInfoLog);
        this.cell.delete(true, unitUserName);

        // respond 204
        return Response.noContent().build();
    }

    /**
     * Perform authority check at the time of CellBulkDeletion execution.
     * @param cellOwner cell owner
     */
    private void checkAccessContextForCellBulkDeletion(String cellOwner) {
        // Check if basic authentication is possible.
        this.accessContext.updateBasicAuthenticationStateForResource(null);

        String accessType = this.accessContext.getType();
        // Accept if UnitMaster, UnitAdmin, UnitUser, UnitLocal.
        if (AccessContext.TYPE_UNIT_MASTER.equals(accessType)
                || AccessContext.TYPE_UNIT_ADMIN.equals(accessType)) {
            return;
        } else if (AccessContext.TYPE_UNIT_USER.equals(accessType)
                || AccessContext.TYPE_UNIT_LOCAL.equals(accessType)) {
            // For UnitUser or UnitLocal, check if the cell owner and the access subject match.
            String subject = this.accessContext.getSubject();
            if (cellOwner == null || !cellOwner.equals(subject)) {
                throw PersoniumCoreException.Auth.NOT_YOURS;
            }
        } else if (AccessContext.TYPE_INVALID.equals(accessType)) {
            this.accessContext.throwInvalidTokenException(this.cellRsCmp.getAcceptableAuthScheme());
        } else if (AccessContext.TYPE_ANONYMOUS.equals(accessType)) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(accessContext.getRealm(),
                    this.cellRsCmp.getAcceptableAuthScheme());
        } else {
            throw PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;
        }
    }

    /**
     * @param pCredHeader pCredHeader X-Personium-Credentialヘッダ
     * @return CellCtlResource
     */
    @Path("__ctl")
    public CellCtlResource ctl(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL) final String pCredHeader) {
        return new CellCtlResource(this.accessContext, pCredHeader, this.cellRsCmp);
    }

    /**
     * パスワード変更APIのエンドポイント.
     * @param pCredHeader pCredHeader
     * @return Response
     */
    @Path("__mypassword")
    public PasswordResource mypassword(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL) final String pCredHeader) {
        return new PasswordResource(this.accessContext, pCredHeader, this.cell, this.cellRsCmp);
    }

    /**
     * 認証のエンドポイント .
     * <ul>
     * <li>p_targetにURLが書いてあれば、そのCELLをTARGETのCELLとしてtransCellTokenを発行する。</li>
     * <li>scopeがなければCellLocalを発行する。</li>
     * </ul>
     * @return TokenEndPointResourceオブジェクト
     */
    @Path("__token")
    public TokenEndPointResource auth() {
        return new TokenEndPointResource(this.cell, this.cellRsCmp);
    }

    /**
     * ImplicitFlow認証のエンドポイント .
     * <ul>
     * <li>p_targetにURLが書いてあれば、そのCELLをTARGETのCELLとしてtransCellTokenを発行する。</li>
     * </ul>
     * @return AuthzEndPointResourceオブジェクト
     */
    @Path("__authz")
    public AuthzEndPointResource authz() {
        return new AuthzEndPointResource(this.cell, this.cellRsCmp);
    }

    /**
     * Htmlによるエラー応答のエンドポイント .
     * @return AuthzEndPointResourceオブジェクト
     */
    @Path("__html/error")
    public ErrorHtmlResource errorHtml() {
        return new ErrorHtmlResource();
    }

    /**
     * ロールのエンドポイント .
     * @return RoleResourceオブジェクト
     */
    @Path("__role")
    public RoleResource role() {
        return new RoleResource(this.cell, this.cellRsCmp);
    }

    /**
     * BoxURL取得のエンドポイント .
     * @return BoxUrlResourceオブジェクト
     */
    @Path("__box")
    public BoxUrlResource boxUrl() {
        return new BoxUrlResource(this.cellRsCmp);
    }

    /**
     * イベントAPIのエンドポイント.
     * @return EventResourceオブジェクト
     */
    @Path("__event")
    public EventResource event() {
        return new EventResource(this.cell, this.accessContext, this.cellRsCmp);
    }

    /**
     * ログ取り出しのエンドポイント .
     * @return JAXRS応答
     */
    @Path("__log")
    public LogResource log() {
        return new LogResource(this.cell, this.accessContext, this.cellRsCmp);
    }

    /**
     * Endpoint of Rule.
     * @return RuleResource object
     */
    @Path("__rule")
    public RuleResource rule() {
        return new RuleResource(this.cell, this.cellRsCmp);
    }

    /**
     * デフォルトボックスへのアクセス.
     * @param request HTPPサーブレットリクエスト
     * @param jaxRsRequest JAX-RS用HTTPリクエスト
     * @return BoxResource Object
     */
    @Path("__")
    public BoxResource box(@Context final HttpServletRequest request,
            @Context final Request jaxRsRequest) {
        return new BoxResource(this.cell, Box.DEFAULT_BOX_NAME, this.accessContext,
                this.cellRsCmp, request, jaxRsRequest);
    }

    /**
     * メッセージ送信のエンドポイント .
     * @return MessageResourceオブジェクト
     */
    @Path("__message")
    public MessageResource message() {
        return new MessageResource(this.accessContext, this.cellRsCmp);

    }

    /**
     * Export endpoint.
     * @return CellExportResource
     */
    @Path("__export")
    public CellExportResource export() {
        return new CellExportResource(cellRsCmp);
    }

    /**
     * Snapshot endpoint.
     * @return CellSnapshotResource
     */
    @Path("__snapshot")
    public CellSnapshotResource snapshot() {
        return new CellSnapshotResource(cellRsCmp);
    }

    /**
     * Import endpoint.
     * <p>
     * Since "import" is a reserved word of java, changed the method name to "importCell".
     * @return CellImportResource
     */
    @Path("__import")
    public CellImportResource importCell() {
        return new CellImportResource(cellRsCmp);
    }

    /**
     * 次のパスをBoxResourceへ渡すメソッド.
     * @param request HTPPサーブレットリクエスト
     * @param boxName Boxパス名
     * @param jaxRsRequest JAX-RS用HTTPリクエスト
     * @return BoxResource Object
     */
    @Path("{box: [^\\/]+}")
    public BoxResource box(
            @Context final HttpServletRequest request,
            @PathParam("box") final String boxName,
            @Context final Request jaxRsRequest) {
        return new BoxResource(this.cell, boxName, this.accessContext, this.cellRsCmp, request, jaxRsRequest);
    }

    /**
     * PROPFINDメソッドの処理.
     * @param requestBodyXml Request Body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @DefaultValue("0") @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.cellRsCmp.checkAccessContext(this.cellRsCmp.getAccessContext(), CellPrivilege.PROPFIND);
        return this.cellRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                CellPrivilege.ACL_READ);
    }

    /**
     * PROPPATCHメソッドの処理.
     * @param requestBodyXml Request Body
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        AccessContext ac = this.cellRsCmp.getAccessContext();
        // トークンの有効性チェック
        // トークンがINVALIDでもACL設定でPrivilegeがallに設定されているとアクセスを許可する必要があるのでこのタイミングでチェック
        ac.updateBasicAuthenticationStateForResource(null);
        if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
            ac.throwInvalidTokenException(this.cellRsCmp.getAcceptableAuthScheme());
        } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(),
                    this.cellRsCmp.getAcceptableAuthScheme());
        }

        // アクセス制御 CellレベルPROPPATCHはユニットユーザのみ可能とする
        if (!ac.isUnitUserToken()) {
            throw PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;
        }
        return this.cellRsCmp.doProppatch(requestBodyXml);
    }

    /**
     * ACLメソッドの処理.
     * @param reader 設定XML
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        // アクセス制御
        this.cellRsCmp.checkAccessContext(this.cellRsCmp.getAccessContext(), CellPrivilege.ACL);
        return this.cellRsCmp.doAcl(reader);
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // アクセス制御
        this.cellRsCmp.checkAccessContext(this.cellRsCmp.getAccessContext(), CellPrivilege.SOCIAL_READ);
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.POST,
                PersoniumCoreUtils.HttpMethod.PROPFIND
                ).build();
    }

}
