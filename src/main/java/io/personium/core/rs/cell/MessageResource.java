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

import java.io.Reader;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.SentMessagePort;
import io.personium.core.odata.DcODataProducer;
import io.personium.core.rs.odata.ODataCtlResource;

/**
 * JAX-RS Resource handling DC Message Level Api. /__messageというパスにきたときの処理.
 */
public final class MessageResource extends ODataCtlResource {
    static Logger log = LoggerFactory.getLogger(MessageResource.class);

    DavRsCmp davRsCmp;
    private AccessContext accessContext;

    /**
     * constructor.
     * @param accessContext AccessContext
     * @param davRsCmp davRsCmp
     */
    public MessageResource(final AccessContext accessContext, DavRsCmp davRsCmp) {
        this.accessContext = accessContext;
        this.davRsCmp = davRsCmp;
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.accessContext;
    }

    /**
     * メッセージ送信API.
     * @param version PCSバージョン
     * @param uriInfo UriInfo
     * @param reader リクエストボディ
     * @return レスポンス
     */
    @POST
    @Path("send")
    public Response messages(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VERSION) final String version,
            @Context final UriInfo uriInfo,
            final Reader reader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.accessContext, CellPrivilege.MESSAGE);

        // データ登録
        DcODataProducer producer = ModelFactory.ODataCtl.cellCtl(this.accessContext.getCell());
        MessageODataResource moResource = new MessageODataResource(this, producer, SentMessagePort.EDM_TYPE_NAME);
        moResource.setVersion(version);
        Response respose = moResource.createMessage(uriInfo, reader);
        return respose;
    }

    /**
     * メッセージ受信API.
     * @param uriInfo UriInfo
     * @param reader リクエストボディ
     * @return レスポンス
     */
    @POST
    @Path("port")
    public Response messagesPort(
            @Context final UriInfo uriInfo,
            final Reader reader) {
        // アクセス制御
        this.accessContext.checkCellIssueToken(this.davRsCmp.getAcceptableAuthScheme());

        // 受信メッセージの登録
        DcODataProducer producer = ModelFactory.ODataCtl.cellCtl(this.accessContext.getCell());
        MessageODataResource moResource = new MessageODataResource(this, producer, ReceivedMessagePort.EDM_TYPE_NAME);
        Response respose = moResource.createMessage(uriInfo, reader);
        return respose;
    }

    /**
     * メッセージ承認API.
     * @param key メッセージId
     * @param reader リクエストボディ
     * @return レスポンス
     */
    @POST
    @Path("received/{key}")
    public Response messagesApprove(@PathParam("key") final String key,
            final Reader reader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.accessContext, CellPrivilege.MESSAGE);

        // 受信メッセージの承認
        DcODataProducer producer = ModelFactory.ODataCtl.cellCtl(this.accessContext.getCell());
        MessageODataResource moResource = new MessageODataResource(this, producer, ReceivedMessagePort.EDM_TYPE_NAME);
        Response respose = moResource.changeMessageStatus(reader, key);
        return respose;
    }
}
