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
package io.personium.core.ws;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket Endpoint.
 * /__event.
 */
@ServerEndpoint(
        value = "/__event",
        configurator = PersoniumConfigurator.class
)
public class WebSocketFqdnBaseEndpoint {

    /** Websocket service. */
    private WebSocketService webSocketService;

    /**
     * This callback method is called when a client connects.
     * Add session info in sessionMap, cellSessionIdMap.
     * @param session WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        String cellName = (String) session.getUserProperties().get("CellName");
        webSocketService = new WebSocketService();
        webSocketService.onOpen(cellName, session);
    }

    /**
     * This callback method is called when a client disconnects.
     * Delete data about disconnected session in cellSessionIdMap, sessionMap.
     * @param session WebSocket session
     */
    @OnClose
    public void onClose(Session session) {
        webSocketService.onClose(session);
    }

    /**
     * This callback method is called when a client send message data.
     * sent text is allowed to be JSON and the following data types.
     *  * authorization {access_token: ${any}}
     *  * subscribe {subscribe: {Type: ${any}, Object: ${any}}}
     *  * unsubscribe {unsubscribe: {Type: ${any}, Object: ${any}}}
     * @param text received message
     * @param session sender session
     */
    @OnMessage
    public void onMessage(String text, Session session) {
        webSocketService.onMessage(text, session);
    }

    /**
     * On returning pong message.
     * @param pongMessage pong
     * @param session session
     */
    @OnMessage
    public void onMessage(PongMessage pongMessage, Session session) {
        webSocketService.onMessage(pongMessage, session);
    }

    /**
     * Error occurred.
     * @param session session info
     * @param e Throwable Error Information
     */
    @OnError
    public void onError(Session session, Throwable e) {
        webSocketService.onError(session, e);
    }

}
