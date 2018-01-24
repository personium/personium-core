/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.SentMessagePort;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.odata.ODataCtlResource;
import io.personium.core.rs.odata.ODataReceivedMessageResource;
import io.personium.core.rs.odata.ODataSentMessageResource;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource handling DC Message Level Api. /__messageというパスにきたときの処理.
 */
public class MessageResource extends ODataCtlResource {
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
     * @param requestKey X-Personium-RequestKey Header
     * @param uriInfo UriInfo
     * @param reader リクエストボディ
     * @return レスポンス
     */
    @WriteAPI
    @POST
    @Path("send")
    public Response messages(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VERSION) final String version,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            @Context final UriInfo uriInfo,
            final Reader reader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.accessContext, CellPrivilege.MESSAGE);

        // データ登録
        PersoniumODataProducer producer = ModelFactory.ODataCtl.message(this.accessContext.getCell(), this.davRsCmp);
        ODataSentMessageResource resource = new ODataSentMessageResource(
                this, requestKey, producer, SentMessagePort.EDM_TYPE_NAME, version);
        Response respose = resource.createMessage(uriInfo, reader);
        return respose;
    }

    /**
     * メッセージ受信API.
     * @param requestKey X-Personium-RequestKey Header
     * @param uriInfo UriInfo
     * @param reader リクエストボディ
     * @return レスポンス
     */
    @WriteAPI
    @POST
    @Path("port")
    public Response messagesPort(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            @Context final UriInfo uriInfo,
            final Reader reader) {
        // アクセス制御
        this.accessContext.checkCellIssueToken(this.davRsCmp.getAcceptableAuthScheme());

        // 受信メッセージの登録
        PersoniumODataProducer producer = ModelFactory.ODataCtl.message(this.accessContext.getCell(), this.davRsCmp);
        ODataReceivedMessageResource resource = new ODataReceivedMessageResource(
                this, requestKey, producer, ReceivedMessagePort.EDM_TYPE_NAME);
        Response respose = resource.createMessage(uriInfo, reader);
        return respose;
    }

    /**
     * メッセージ承認API.
     * @param key メッセージId
     * @param requestKey X-Personium-RequestKey Header
     * @param reader リクエストボディ
     * @return レスポンス
     */
    @WriteAPI
    @POST
    @Path("received/{key}")
    public Response messagesApprove(@PathParam("key") final String key,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            final Reader reader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.accessContext, CellPrivilege.MESSAGE);

        // 受信メッセージの承認
        PersoniumODataProducer producer = ModelFactory.ODataCtl.message(this.accessContext.getCell(), this.davRsCmp);
        ODataReceivedMessageResource resource = new ODataReceivedMessageResource(
                this, requestKey, producer, ReceivedMessagePort.EDM_TYPE_NAME);
        Response respose = resource.changeMessageStatus(reader, key);
        return respose;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(String entitySetName, String object, String info, String reqKey, String op) {
        String schema = this.accessContext.getSchema();
        String subject = this.accessContext.getSubject();
        String type = PersoniumEventType.Category.MESSAGE + PersoniumEventType.SEPALATOR + op;
        String requestKey = ResourceUtils.validateXPersoniumRequestKey(reqKey);
        PersoniumEvent ev = new PersoniumEvent(schema, subject, type, object, info, requestKey);
        EventBus eventBus = this.accessContext.getCell().getEventBus();
        eventBus.post(ev);
    }
}
