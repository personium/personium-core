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

import java.util.List;
import java.util.Map;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket endpoint configurator.
 */
public class PersoniumConfigurator extends ServerEndpointConfig.Configurator {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(PersoniumConfigurator.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyHandshake(ServerEndpointConfig conf, HandshakeRequest req, HandshakeResponse res) {
        Map<String, List<String>> headers = req.getHeaders();
        if (headers == null || headers.get("host") == null) {
            return;
        }
        String host = headers.get("host").get(0);
        // {CellName}.{Domain}/__event に対応する
        String cellName = host.split("\\.")[0];
        conf.getUserProperties().put("CellName", cellName);
    }
}
