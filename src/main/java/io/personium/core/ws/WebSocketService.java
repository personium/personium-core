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

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket Endpoint.
 */
@ServerEndpoint(value = "/{cell}/__event")
public class WebSocketService {
    /**
     * Subscribed condition Info which is used for filtering events.
     */
    class RuleInfo {
        String type;
        String object;
    }

    // log
    private static Logger log = LoggerFactory.getLogger(WebSocketService.class);

    // Keys of session user properties
    private static final String CELL_ID = "cellId";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String RULES = "rules";

    // session and cell id map for send event
    private static Map<String, List<Session>> cellSessionMap = new HashMap<>(); // CellId: Session[]

    // lock object
    static Object lockObj = new Object();

    /**
     * This callback method is called when a client connects.
     * Add session info in sessionMap, cellSessionIdMap.
     * @param cellName cellName specified in url param
     * @param session WebSocket session
     */
    @OnOpen
    public void onOpen(@PathParam("cell") String cellName, Session session) {
        log.debug("ws: onOpen[" + cellName + "]: " + session.getId());

        synchronized (lockObj) {
            Cell cell = ModelFactory.cell(cellName);
            if (cell != null) {
                String cellId = cell.getId();

                List<Session> sessionList = cellSessionMap.get(cellId);
                if (sessionList == null) {
                    sessionList = new ArrayList<>();
                }
                if (!sessionList.contains(session)) {
                    sessionList.add(session);
                }
                cellSessionMap.put(cellId, sessionList);

                Map<String, Object> userProperties = session.getUserProperties();
                userProperties.put(CELL_ID, cellId);
                userProperties.put(ACCESS_TOKEN, "");
                userProperties.put(RULES, new ArrayList<RuleInfo>());
            } else {
                log.warn("Connect cell name is not exist. : " + cellName);
            }
        }
    }

    /**
     * This callback method is called when a client disconnects.
     * Delete data about disconnected session in cellSessionIdMap, sessionMap.
     * @param session WebSocket session
     */
    @OnClose
    public void onClose(Session session) {
        synchronized (lockObj) {
            Map<String, Object> userProperties = session.getUserProperties();
            String cellId = (String) userProperties.get(CELL_ID);

            log.debug("ws: onClose[" + cellId + "]: " + session.getId());

            userProperties.remove(CELL_ID);
            userProperties.remove(ACCESS_TOKEN);
            userProperties.remove(RULES);

            List<Session> sessionList = cellSessionMap.get(cellId);
            sessionList.remove(session);
        }

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
        Map<String, Object> userProperties = session.getUserProperties();
        String cellId = (String) userProperties.get(CELL_ID);
        String accessToken = (String) userProperties.get(ACCESS_TOKEN);

        log.debug("ws: onMessage[" + cellId + "]" + "[" + session.getId() + "] " + text);


        JSONParser parser = new JSONParser();
        try {
            JSONObject json = (JSONObject) parser.parse(text);

            // token must be sent by client before communication
            String receivedAccessToken = (String) json.get("access_token");
            if (receivedAccessToken != null && checkPrivilege(receivedAccessToken, cellId)) {
                log.debug("ws: set access_token");
                userProperties.put(ACCESS_TOKEN, receivedAccessToken);
            }

            // subscribe type is used for filtering of events
            JSONObject subscribeInfo = (JSONObject) json.get("subscribe");
            if (subscribeInfo != null && accessToken != null) {
                log.debug("ws: set subscribe: " + subscribeInfo);

                // want to register multi rules using Array ...
                String eventType = (String) subscribeInfo.get("Type");
                String eventObject = (String) subscribeInfo.get("Object");
                if (eventType != null && eventObject != null) {
                    synchronized (lockObj) {
                        List<RuleInfo> ruleList = (List<RuleInfo>) userProperties.get(RULES);
                        RuleInfo rule = new RuleInfo();
                        rule.type = eventType;
                        rule.object = eventObject;
                        ruleList.add(rule); // able to register same rule
                    }
                }
            }

            // delete subscribe rule in ruleList
            JSONObject unsubscribeInfo = (JSONObject) json.get("unsubscribe");
            if (unsubscribeInfo != null && accessToken != null) {
                log.debug("ws: unsubscribe: " + unsubscribeInfo);

                String eventType = (String) unsubscribeInfo.get("Type");
                String eventObject = (String) unsubscribeInfo.get("Object");
                if (eventType != null && eventObject != null) {
                    synchronized (lockObj) {
                        List<RuleInfo> ruleList = (List<RuleInfo>) userProperties.get(RULES);
                        RuleInfo targetRule = null;
                        for (RuleInfo rule : ruleList) {
                            if (rule.type.equals(eventType) && rule.object.equals(eventObject)) {
                                targetRule = rule;
                                break;
                            }
                        }
                        if (targetRule != null) {
                            ruleList.remove(targetRule);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Message is not JSON: " + text);
            // e.printStackTrace();
        }
    }

    /**
     * Error occurred.
     * @param e Throwable Error Information
     */
    @OnError
    public void onError(Throwable e) {
        log.error("ws: onError: " + e.getMessage());
        // e.printStackTrace();
    }

    /**
     * send text message to client with session.
     * @param session send-to websocket session
     * @param message sent text
     */
    private static void sendText(Session session, String message) {
        session.getAsyncRemote().sendText(message);
    }

    /**
     * This method is called by EventSubscriber.
     * @param event send event of personium to all cell session
     */
    public static void sendEvent(PersoniumEvent event) {
        String cellId = event.getCellId();
        String cellName = null;
        Cell cell = ModelFactory.cell(cellId, null);
        if (cell != null) {
            cellName = cell.getName();
        }
        log.debug("ws: sendEvent: " + cellName);

        synchronized (lockObj) { // TODO better?
            // send event data to all connecting session
            List<Session> sessionList = cellSessionMap.get(cellId);
            if (sessionList != null) {
                for (Session session : sessionList) {
                    if (isExistMatchedRule(session, event)) {
                        String sendMessage = toJSON(event).toJSONString();
                        sendText(session, sendMessage);
                        log.debug("ws: sent!: [" + cellId + "][" + session.getId() + "] " + sendMessage);
                    }
                }
            }
        }
    }

    /**
     * convert Personium Event data to json.
     * @param event
     * @return
     */
    private static JSONObject toJSON(PersoniumEvent event) {
        JSONObject json = new JSONObject();
        json.put("RequestKey", event.getRequestKey());
        json.put("External", event.getExternal());
        json.put("Schema", event.getSchema());
        json.put("Subject", event.getSubject());
        json.put("Type", event.getType());
        json.put("Object", event.getObject());
        json.put("Info", event.getInfo());
        json.put("cellId", event.getCellId());
        return json;
    }

    /**
     * if occured event match subscribed rules.
     * @param session websocket session
     * @param event personium event
     * @return result of matching
     */
    private static boolean isExistMatchedRule(Session session, PersoniumEvent event) {
        boolean result = false;
        List<RuleInfo> ruleList = (List) session.getUserProperties().get(RULES);

        if (ruleList != null && ruleList.size() > 0) {
            for (RuleInfo rule : ruleList) {
                if (rule.type != null
                        && event.getType() != null
                        && (rule.type.equals(event.getType()) || rule.type.equals("*"))
                        && rule.object != null
                        && event.getObject() != null
                        && (rule.object.equals(event.getObject()) || rule.object.equals("*"))) {
                    result = true;
                }
            }
        }
        log.debug("ws: isExistMatchedRule: sessionId= " + session.getId() + "/ result= " + result);
        return result;
    }

    /**
     * craete access context from sent accessToken and cellName.
     * @param accessToken
     * @param cellName
     * @return AccessContext
     */
    private static AccessContext createAccessContext(String accessToken, String cellName) {
        log.debug("ws: checkAccessToken: cell= " + cellName);

        AccessContext result = null;
        Cell cell = ModelFactory.cell(cellName);

        String baseUri = PersoniumUnitConfig.getBaseUrl();
        URI uri = URI.create(baseUri);
        String host = uri.getHost();

        try {
            result = AccessContext.createForWebSocket(accessToken, cell, baseUri, host);
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.debug("ws: checkAccessToken result = : " + result);
        return result;
    }

    /**
     * check privilege: if it notifies event to client.
     * @param accessToken
     * @param cellId
     * @return
     */
    private static boolean checkPrivilege(String accessToken, String cellId) {
        log.debug("ws: checkPrivilege: cell= " + cellId);

        boolean result = true;
        if (accessToken == null || cellId == null) {
            result = false;
        } else {
            Cell cell = ModelFactory.cell(cellId, null);
            AccessContext ac = createAccessContext(accessToken, cell.getName());

            try {
                CellCmp cellCmp = ModelFactory.cellCmp(cell);
                if (!cellCmp.exists()) {
                    result = false;
                }
                CellRsCmp cellRsCmp = new CellRsCmp(cellCmp, cell, ac);
                cellRsCmp.checkAccessContext(ac, CellPrivilege.EVENT_READ);
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            }
        }

        log.debug("ws: checkPrivilege result = : " + result);
        return result;
    }

}
