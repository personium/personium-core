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

import java.io.Reader;

import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import org.apache.wink.webdav.WebDAVMethod.PROPFIND;
import org.apache.wink.webdav.WebDAVMethod.PROPPATCH;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * イベントバス用JAX-RS Resource.
 */
public class EventResource {
    Cell cell;
    AccessContext accessContext;
    DavRsCmp davRsCmp;

    static Logger log = LoggerFactory.getLogger(EventResource.class);

    /**
     * constructor.
     * @param cell Cell
     * @param accessContext AccessContext
     * @param davRsCmp DavRsCmp
     */
    public EventResource(final Cell cell, final AccessContext accessContext, final DavRsCmp davRsCmp) {
        this.accessContext = accessContext;
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }

    /**
     * イベントの受付.
     * @param reader リクエストボディ
     * @return JAXRS Response
     */
    @WriteAPI
    @POST
    public final Response receiveEvent(final Reader reader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.EVENT);

        // リクエストボディを解析してEventオブジェクトを取得する
        PersoniumEvent event = getRequestBody(reader);
        validateEventProperties(event);

        // post event
        EventBus eventBus = this.cell.getEventBus();
        eventBus.post(event);

        // レスポンス返却
        return Response.ok().build();
    }

    /**
     * ログ設定更新.
     * @return レスポンス
     */
    @WriteAPI
    @PROPPATCH
    public final Response updateLogSettings() {
        // TODO アクセス制御
        // this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.LOG);
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
    }

    /**
     * ログ設定取得.
     * @return レスポンス
     */
    @PROPFIND
    public final Response getLogSettings() {
        // TODO アクセス制御
        // this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.LOG_READ);
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
    }

    /**
     * リクエストボディを解析してEventオブジェクトを取得する.
     * @param reader Http入力ストリーム
     * @param requestKey
     * @return 解析したEventオブジェクト
     */
    private PersoniumEvent getRequestBody(final Reader reader) {
        JSONObject body;
        body = ResourceUtils.parseBodyAsJSON(reader);

        Object obj;

        // Type
        obj = body.get("Type");
        if (!(obj instanceof String)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("Type");
        }
        String type = (String) body.get("Type");

        // Object
        obj = body.get("Object");
        if (!(obj instanceof String)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("Object");
        }
        String object = (String) body.get("Object");

        // Info
        obj = body.get("Info");
        if (!(obj instanceof String)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("Info");
        }
        String info = (String) body.get("Info");

        PersoniumEvent ev = new PersoniumEvent(PersoniumEvent.EXTERNAL_EVENT,
                type, object, info, this.davRsCmp);

        return ev;
    }

    /**
     * Event内の各プロパティ値をバリデートする.
     * @param event Eventオブジェクト
     */
    private void validateEventProperties(final PersoniumEvent event) {
        String type = event.getType();
        if (type == null) {
            throw PersoniumCoreException.Event.INPUT_REQUIRED_FIELD_MISSING.params("Type");
        } else if (!PersoniumEvent.validateType(type)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("Type");
        }
        String object = event.getObject();
        if (object == null) {
            throw PersoniumCoreException.Event.INPUT_REQUIRED_FIELD_MISSING.params("Object");
        } else if (!PersoniumEvent.validateObject(object)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("Object");
        }
        String info = event.getInfo();
        if (info == null) {
            throw PersoniumCoreException.Event.INPUT_REQUIRED_FIELD_MISSING.params("Info");
        } else if (!PersoniumEvent.validateInfo(info)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("Info");
        }
    }

}
