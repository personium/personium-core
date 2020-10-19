/**
 * Personium
 * Copyright 2018-2020 Personium Project Authors
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
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * Stream Endpoint.
 * /{cell}/__topic/{box}/{path}.
 */
@ServerEndpoint(value = "/{cell}/__topic/{box}/{path}")
public class StreamPathBaseEndpoint {

    /** Stream common endpoint. */
    private StreamEndpoint streamEndpoint;

    /**
     * This callback method is called when a client connects.
     * @param cellName cellName specified in url param
     * @param boxName boxName specified in url param
     * @param path path specified in url param
     * @param session WebSocket session
     */
    @OnOpen
    public void onOpen(@PathParam("cell") String cellName,
                       @PathParam("box") String boxName,
                       @PathParam("path") String path,
                       Session session) {
        String topicName = new StringBuilder().append(cellName)
                                              .append(StreamEndpoint.SEPARATOR)
                                              .append(boxName)
                                              .append(StreamEndpoint.SEPARATOR)
                                              .append(path)
                                              .toString();
        streamEndpoint = new StreamEndpoint();
        streamEndpoint.onOpen(topicName, session);
    }

    /**
     * This callback method is called when a client disconnects.
     * @param session WebSocket session
     */
    @OnClose
    public void onClose(Session session) {
        streamEndpoint.onClose(session);
    }

    /**
     * This callback method is called when a client send message data.
     * @param text received message
     * @param session sender session
     */
    @OnMessage
    public void onMessage(String text, Session session) {
        streamEndpoint.onMessage(text, session);
    }

    /**
     * On returning pong message.
     * @param pongMessage pong
     * @param session session
     */
    @OnMessage
    public void onMessage(PongMessage pongMessage, Session session) {
        streamEndpoint.onMessage(pongMessage, session);
    }

    /**
     * Error occurred.
     * @param session session info
     * @param e Throwable Error Information
     */
    @OnError
    public void onError(Session session, Throwable e) {
        streamEndpoint.onError(session, e);
    }
}
