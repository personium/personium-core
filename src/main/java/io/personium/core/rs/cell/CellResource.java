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
package io.personium.core.rs.cell;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.wink.webdav.WebDAVMethod;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.ACL;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.UnitUserLockManager;
import io.personium.core.rs.box.BoxResource;

/**
 * JAX-RS Resource handling Cell Level Api.
 *  logics for the url path /{cell name}.
 */
public final class CellResource {
    static Logger log = LoggerFactory.getLogger(CellResource.class);

    Cell cell;
    CellCmp cellCmp;
    CellRsCmp cellRsCmp;
    AccessContext accessContext;

    /**
     * constructor.
     * @param accessContext AccessContext
     */
    public CellResource(
            final AccessContext accessContext) {
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

        this.cellRsCmp = new CellRsCmp(this.cellCmp, this.cell, this.accessContext);
        checkReferenceMode();
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
     * @return JAX-RS Response Object
     */
    @GET
    public Response getSvcDoc() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<cell xmlns=\"urn:x-personium:xmlns\">");
        sb.append("<uuid>" + this.cell.getId() + "</uuid>");
        sb.append("<ctl>" + this.cell.getUrl() + "__ctl/" + "</ctl>");
        sb.append("</cell>");
        return Response.ok().entity(sb.toString()).build();
    }

    /**
     * handler for DELETE Method.
     * ie, Recursive Cell Deletion.
     * @param recursiveHeader X-Personium-Recursive Header
     * @return JAX-RS Response Object
     */
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



    private void checkAccessContextForCellBulkDeletion(String cellOwner) {

        // Basic認証できるかチェック
        this.accessContext.updateBasicAuthenticationStateForResource(null);

        String accessType = this.accessContext.getType();
        // ユニットマスター、ユニットユーザ、ユニットローカルユニットユーザ以外は権限エラーとする
        if (AccessContext.TYPE_INVALID.equals(accessType)) {
            this.accessContext.throwInvalidTokenException(this.cellRsCmp.getAcceptableAuthScheme());
        } else if (AccessContext.TYPE_ANONYMOUS.equals(accessType)) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(accessContext.getRealm(),
                    this.cellRsCmp.getAcceptableAuthScheme());
        } else if (!AccessContext.TYPE_UNIT_MASTER.equals(accessType)
                && !AccessContext.TYPE_UNIT_USER.equals(accessType)
                && !AccessContext.TYPE_UNIT_LOCAL.equals(accessType)) {
            throw PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;
        } else if (AccessContext.TYPE_UNIT_USER.equals(accessType)
                || AccessContext.TYPE_UNIT_LOCAL.equals(accessType)) {
            // ユニットユーザ、ユニットローカルユニットユーザの場合はセルオーナとアクセス主体が一致するかチェック
            String subject = this.accessContext.getSubject();
            if (cellOwner == null || !cellOwner.equals(subject)) {
                throw PersoniumCoreException.Auth.NOT_YOURS;
            }
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
     * イベント受付のエンドポイント .
     * @param boxName Box名
     * @param reader 入力
     * @return JAXRS応答
     */
    @POST
    @Path("__event/{boxName}")
    public Response postEvent(
            @PathParam("boxName") final String boxName,
            final Reader reader) {
        // アクセス制御
        this.cellRsCmp.checkAccessContext(this.cellRsCmp.getAccessContext(), CellPrivilege.EVENT);
        // Subjectを取得する
        String subject = this.accessContext.getSubject();
        // BoxNameからSchemaをとる
        Box box = this.cell.getBoxForName(boxName);
        String schema = null;
        if (box != null) {
            schema = box.getSchema();
        }
        // Bodyを解釈してPersoniumEventオブジェクトをつくる。
        JSONParser parser = new JSONParser();
        try {
            JSONObject evJson = (JSONObject) parser.parse(reader);
            String levelStr = (String) evJson.get("level");
            String action = (String) evJson.get("action");
            String object = (String) evJson.get("object");
            String result = (String) evJson.get("result");
            int level = PersoniumEvent.Level.INFO;
            if ("warn".equalsIgnoreCase(levelStr)) {
                level = PersoniumEvent.Level.WARN;
            } else if ("error".equalsIgnoreCase(levelStr)) {
                level = PersoniumEvent.Level.ERROR;
            }
            PersoniumEvent ev = new PersoniumEvent("client", schema, level, subject, action, object, result);

            // PersoniumEventオブジェクトをEventBusオブジェクトに流す.
            EventBus eventBus = this.cell.getEventBus();
            eventBus.post(ev);
            return Response.ok().build();
        } catch (IOException e) {
            throw PersoniumCoreException.Server.UNKNOWN_ERROR.reason(e);
        } catch (ParseException e) {
            throw PersoniumCoreException.Event.JSON_PARSE_ERROR.reason(e);
        }

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
     * デフォルトボックスへのアクセス.
     * @param request HTPPサーブレットリクエスト
     * @param jaxRsRequest JAX-RS用HTTPリクエスト
     * @return BoxResource BoxResource Object
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
     * 次のパスをBoxResourceへ渡すメソッド.
     * @param request HTPPサーブレットリクエスト
     * @param boxName Boxパス名
     * @param jaxRsRequest JAX-RS用HTTPリクエスト
     * @return BoxResource BoxResource Object
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

        return this.cellRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                CellPrivilege.PROPFIND, CellPrivilege.ACL_READ);
    }

    // TODO Interim correspondence.(For security reasons)
//    /**
//     * PROPPATCHメソッドの処理.
//     * @param requestBodyXml Request Body
//     * @return JAX-RS Response
//     */
//    @WebDAVMethod.PROPPATCH
//    public Response proppatch(final Reader requestBodyXml) {
//        AccessContext ac = this.cellRsCmp.getAccessContext();
//        // トークンの有効性チェック
//        // トークンがINVALIDでもACL設定でPrivilegeがallに設定されているとアクセスを許可する必要があるのでこのタイミングでチェック
//        ac.updateBasicAuthenticationStateForResource(null);
//        if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
//            ac.throwInvalidTokenException(this.cellRsCmp.getAcceptableAuthScheme());
//        } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
//            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(),
//                    this.cellRsCmp.getAcceptableAuthScheme());
//        }
//
//        // アクセス制御 CellレベルPROPPATCHはユニットユーザのみ可能とする
//        if (!ac.isUnitUserToken()) {
//            throw PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;
//        }
//        return this.cellRsCmp.doProppatch(requestBodyXml);
//    }

    /**
     * ACLメソッドの処理.
     * @param reader 設定XML
     * @return JAX-RS Response
     */
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
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.POST,
                PersoniumCoreUtils.HttpMethod.PROPFIND
                ).build();
    }

}
