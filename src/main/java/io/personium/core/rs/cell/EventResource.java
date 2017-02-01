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
import java.util.regex.Pattern;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import org.apache.wink.webdav.WebDAVMethod.PROPFIND;
import org.apache.wink.webdav.WebDAVMethod.PROPPATCH;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.eventbus.PersoniumEventBus;
import io.personium.core.eventbus.JSONEvent;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ctl.Event;

/**
 * イベントバス用JAX-RS Resource.
 */
public class EventResource {
    Cell cell;
    AccessContext accessContext;
    DavRsCmp davRsCmp;

    static Logger log = LoggerFactory.getLogger(EventResource.class);

    static final int MAXREQUEST_KEY_LENGTH = 128;
    static final String REQEUST_KEY_DEFAULT_FORMAT = "PCS-%d";

    static final Pattern REQUEST_KEY_PATTERN = Pattern.compile("[\\p{Alpha}\\p{Digit}_-]*");

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
     * @param version X-Personium-Versionヘッダー値
     * @param requestKey X-Personium-RequestKeyヘッダー値
     * @return JAXRS Response
     */
    @POST
    public final Response receiveEvent(final Reader reader,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VERSION) final String version,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey) {

        // TODO findBugs対策↓
        log.debug(this.cell.getName());
        log.debug(this.accessContext.getBaseUri());
        log.debug(this.davRsCmp.getUrl());

        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.EVENT);

        // X-Personium-RequestKeyの解析（指定なしの場合にデフォルト値を補充）
        requestKey = validateXPersoniumRequestKey(requestKey);
        // TODO findBugs対策↓
        log.debug(requestKey);

        // リクエストボディを解析してEventオブジェクトを取得する
        JSONEvent reqBody = getRequestBody(reader);
        validateEventProperties(reqBody);

        // TODO イベントバス系のデータロック
        // TODO 新規のイベント受付かどうかをESへ検索（current/default.logのデータ検索)
        // TODO ESへCollectionとLogDavFileを登録/更新（新規：CREATE、更新：uのみPUT）
        // TODO ログ出力用のデフォルト設定情報を取得

        // ログファイル出力
        PersoniumEventBus eventBus = new PersoniumEventBus(this.cell);
        Event event = createEvent(reqBody, requestKey);
        eventBus.outputEventLog(event);

        // レスポンス返却
        return Response.ok().build();
    }

    /**
     * ログ設定更新.
     * @return レスポンス
     */
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
     * リクエストヘッダの X-Personium-RequestKey の正当性チェックを行う. <br/>
     * 不正な場合には例外を発する. <br/>
     * 未指定時には、デフォルト値を補充する.
     * @param requestKey リクエストヘッダ
     * @return 正当性チェック通過後の X-Personium-RequetKeyの値
     */
    public static String validateXPersoniumRequestKey(String requestKey) {
        if (null == requestKey) {
            requestKey = String.format(REQEUST_KEY_DEFAULT_FORMAT, System.currentTimeMillis());
        }
        if (MAXREQUEST_KEY_LENGTH < requestKey.length()) {
            throw PersoniumCoreException.Event.X_PERSONIUM_REQUESTKEY_INVALID;
        }
        if (!REQUEST_KEY_PATTERN.matcher(requestKey).matches()) {
            throw PersoniumCoreException.Event.X_PERSONIUM_REQUESTKEY_INVALID;
        }
        return requestKey;
    }

    /**
     * リクエストボディを解析してEventオブジェクトを取得する.
     * @param reader Http入力ストリーム
     * @return 解析したEventオブジェクト
     */
    protected JSONEvent getRequestBody(final Reader reader) {
        JSONEvent event = null;
        JsonParser jp = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new JsonFactory();
        try {
            jp = f.createJsonParser(reader);
            JsonToken token = jp.nextToken(); // JSONルート要素（"{"）
            if (token == JsonToken.START_OBJECT) {
                event = mapper.readValue(jp, JSONEvent.class);
            } else {
                throw PersoniumCoreException.Event.JSON_PARSE_ERROR;
            }
        } catch (IOException e) {
            throw PersoniumCoreException.Event.JSON_PARSE_ERROR;
        }
        return event;
    }

    /**
     * Event内の各プロパティ値をバリデートする.
     * @param event Eventオブジェクト
     */
    protected void validateEventProperties(final JSONEvent event) {
        Event.LEVEL level = event.getLevel();
        if (level == null) {
            throw PersoniumCoreException.Event.INPUT_REQUIRED_FIELD_MISSING.params("level");
        } else if (!JSONEvent.validateLevel(level)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("level");
        }
        String action = event.getAction();
        if (action == null) {
            throw PersoniumCoreException.Event.INPUT_REQUIRED_FIELD_MISSING.params("action");
        } else if (!JSONEvent.validateAction(action)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("action");
        }
        String object = event.getObject();
        if (object == null) {
            throw PersoniumCoreException.Event.INPUT_REQUIRED_FIELD_MISSING.params("object");
        } else if (!JSONEvent.validateObject(object)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("object");
        }
        String result = event.getResult();
        if (result == null) {
            throw PersoniumCoreException.Event.INPUT_REQUIRED_FIELD_MISSING.params("result");
        } else if (!JSONEvent.validateResult(result)) {
            throw PersoniumCoreException.Event.REQUEST_FIELD_FORMAT_ERROR.params("result");
        }
    }

    /**
     * 外部イベントログ出力用Eventオブジェクトを生成.
     * @param reqBody JSON表現リクエストボディ
     * @param requestKey RequestKeyヘッダの値
     * @return Eventオブジェクト
     */
    protected Event createEvent(JSONEvent reqBody, String requestKey) {
        Event event = new Event();
        event.setLevel(reqBody.getLevel());
        event.setAction(reqBody.getAction());
        event.setObject(reqBody.getObject());
        event.setResult(reqBody.getResult());
        event.setRequestKey(requestKey);
        event.setName("client");
        event.setSchema(accessContext.getSchema());
        event.setSubject(accessContext.getSubject());
        return event;
    }

    /**
     * 内部イベントログ出力用Eventオブジェクトを生成.
     * @param reqBody JSON表現リクエストボディ
     * @param requestKey RequestKeyヘッダの値
     * @param accessContext accessContext
     * @return Eventオブジェクト
     */
    public static Event createEvent(JSONEvent reqBody, String requestKey, AccessContext accessContext) {
        Event event = new Event();
        event.setLevel(reqBody.getLevel());
        event.setAction(reqBody.getAction());
        event.setObject(reqBody.getObject());
        event.setResult(reqBody.getResult());
        event.setRequestKey(requestKey);
        event.setName("server");
        event.setSchema(accessContext.getSchema());
        event.setSubject(accessContext.getSubject());
        return event;
    }
}
