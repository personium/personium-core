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
import javax.websocket.OnMessage;
import javax.websocket.OnError;
import javax.websocket.OnClose;
import javax.websocket.Session;
import javax.websocket.PongMessage;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static io.personium.common.auth.token.AbstractOAuth2Token.MILLISECS_IN_A_SEC;
import static io.personium.core.auth.OAuth2Helper.Key.EXPIRES_IN;


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
    private static final String AUTHORIZED_TIME = "authorized_time";
    private static final String HEART_BEAT = "heart_beat";
    private static final String PING_COUNT = "ping_count";
    private static final byte[] PING_DATA = new byte[]{1, 2, 3};
    private static final int PING_MAX = 10;
    private static final int HEART_BEAT_TIME = 60000;
    private static final String RESPONSE = "response";
    private static final String RESPONSE_SUCCESS = "success";
    private static final String RESPONSE_ERROR = "error";
    private static final String REASON = "reason";
    private static final String REASON_FORMAT_ERROR = "format error";
    private static final String REASON_UNAUTHORIZED = "unauthorized";
    private static final String REASON_SUBSCRIBE_NOT_FOUND = "subscriptions not found";
    private static final String REASON_INVALID_STATE_TYPE =
            "invalid state type. allowed status type: [all, subscriptions]";
    private static final String STATE_TYPE_ALL = "all";
    private static final String STATE_TYPE_SUBSCRIBE = "subscriptions";
    private static final int EXPIRES_IN_SECONDS = 3600;

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
        Map<String, Object> userProperties = session.getUserProperties();

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
                userProperties.put(CELL_ID, cellId);
                userProperties.put(RULES, new ArrayList<RuleInfo>());
                userProperties.put(PING_COUNT, 0);

                cellSessionMap.put(cellId, sessionList);
            } else {
                log.warn("Connect cell name is not exist. : " + cellName);
            }

            // heartbeat (send ping).
            Timer heartBeatInterval = new Timer();
            heartBeatInterval.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        synchronized (lockObj) {
                            int sendPingCount = (int) userProperties.get(PING_COUNT);
                            // session closes if it is not received pong message PING_MAX times from the client.
                            if (sendPingCount > PING_MAX) {
                                closeSession(session);
                            } else {
                                sendPingCount++;
                                userProperties.put(PING_COUNT, sendPingCount);
                                session.getBasicRemote().sendPing(ByteBuffer.wrap(PING_DATA));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            },  0, HEART_BEAT_TIME);
            userProperties.put(HEART_BEAT, heartBeatInterval);
        }
    }

    /**
     * This callback method is called when a client disconnects.
     * Delete data about disconnected session in cellSessionIdMap, sessionMap.
     * @param session WebSocket session
     */
    @OnClose
    public void onClose(Session session) {
        log.debug("ws: onClose: " + session.getId());
        removeSessionInfo(session);
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
        log.debug("ws: onMessage[" + cellId + "]" + "[" + session.getId() + "] " + text);
        JSONObject result = new JSONObject();
        long currentTime = System.currentTimeMillis() / MILLISECS_IN_A_SEC;
        result.put("timestamp", currentTime);

        JSONParser parser = new JSONParser();
        try {
            JSONObject json = (JSONObject) parser.parse(text);

            // token must be sent by client before communication
            String receivedAccessToken = (String) json.get("access_token");
            if (receivedAccessToken != null) {
                if (checkPrivilege(receivedAccessToken, cellId)) {
                    log.debug("ws: set access_token");
                    userProperties.put(ACCESS_TOKEN, receivedAccessToken);
                    userProperties.put(AUTHORIZED_TIME, new Date());
                    // ack
                    result.put(RESPONSE, RESPONSE_SUCCESS);
                    result.put(EXPIRES_IN, EXPIRES_IN_SECONDS);
                    sendText(session, result.toJSONString());
                } else {
                    log.debug("ws: invalid access_token");
                    closeSession(session);
                }
                return;
            }

            String accessToken = (String) userProperties.get(ACCESS_TOKEN);
            if (accessToken == null) {
                result.put(RESPONSE, RESPONSE_ERROR);
                result.put(REASON, REASON_UNAUTHORIZED);
                sendText(session, result.toJSONString());
                return;
            }

            Date authorizedDate = (Date) userProperties.get(AUTHORIZED_TIME);
            if (isExpired(authorizedDate)) {
                log.debug("ws: token expired. session close.: " + cellId);
                closeSession(session);
                return;
            }

            // subscribe type is used for filtering of events
            JSONObject subscribeInfo = (JSONObject) json.get("subscribe");
            if (subscribeInfo != null) {
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
                        // ack
                        result.put(RESPONSE, RESPONSE_SUCCESS);
                    }
                } else {
                    result.put(RESPONSE, RESPONSE_ERROR);
                    result.put(REASON, REASON_FORMAT_ERROR);
                }
                sendText(session, result.toJSONString());
                return;
            }

            // delete subscribe rule in ruleList
            JSONObject unsubscribeInfo = (JSONObject) json.get("unsubscribe");
            if (unsubscribeInfo != null) {
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
                            // ack
                            result.put(RESPONSE, RESPONSE_SUCCESS);
                        } else {
                            result.put(RESPONSE, RESPONSE_ERROR);
                            result.put(REASON, REASON_SUBSCRIBE_NOT_FOUND);
                        }
                    }
                } else {
                    result.put(RESPONSE, RESPONSE_ERROR);
                    result.put(REASON, REASON_FORMAT_ERROR);
                }
                sendText(session, result.toJSONString());
                return;
            }

            // websocket state.
            String state = (String) json.get("state");
            if (state != null) {
                log.debug("ws: state: " + state);
                if (state.equals(STATE_TYPE_ALL)
                        ||  state.equals(STATE_TYPE_SUBSCRIBE)) {
                    List<JSONObject> ruleJsonList = new ArrayList<>();
                    synchronized (lockObj) {
                        List<RuleInfo> ruleList = (List<RuleInfo>) userProperties.get(RULES);
                        for (RuleInfo rule : ruleList) {
                            JSONObject ruleJson = new JSONObject();
                            ruleJson.put("type", rule.type);
                            ruleJson.put("object", rule.object);
                            ruleJsonList.add(ruleJson);
                        }
                    }
                    if (state.equals(STATE_TYPE_SUBSCRIBE)) {
                        result.put(RESPONSE, RESPONSE_SUCCESS);
                        result.put("subscriptions", ruleJsonList);
                    } else if (state.equals(STATE_TYPE_ALL)) {
                        result.put(RESPONSE, RESPONSE_SUCCESS);
                        result.put("subscriptions", ruleJsonList);
                        Date now = new Date();
                        long expireTime = authorizedDate.getTime() + EXPIRES_IN_SECONDS * MILLISECS_IN_A_SEC;
                        long expireLimit = (expireTime - now.getTime()) / MILLISECS_IN_A_SEC;
                        result.put("expires_in", expireLimit);
                        String cellName = null;
                        Cell cell = ModelFactory.cell(cellId, null);
                        if (cell != null) {
                            cellName = cell.getName();
                        }
                        result.put("cell", cellName);
                    }
                } else {
                    result.put(RESPONSE, RESPONSE_ERROR);
                    result.put(REASON, REASON_INVALID_STATE_TYPE);
                }
                sendText(session, result.toJSONString());
            }
        } catch (Exception e) {
            log.debug("Invalid message: " + text);
            // e.printStackTrace();
        }
    }

    /**
     * On returning pong message.
     * @param pongMessage pong
     * @param session session
     */
    @OnMessage
    public void onMessage(PongMessage pongMessage, Session session) {
        synchronized (lockObj) {
            int sendPingCount = (int) session.getUserProperties().get(PING_COUNT);
            // log.debug("ws: receive pong message: " + session.getId() + ":" + sendPingCount);
            sendPingCount--;
            session.getUserProperties().put(PING_COUNT, sendPingCount);
        }
    }

    /**
     * Error occurred.
     * @param session session info
     * @param e Throwable Error Information
     */
    @OnError
    public void onError(Session session, Throwable e) {
        log.error("ws: onError: " + e.getMessage());
        // e.printStackTrace();
        closeSession(session);
    }

    /**
     * Session close.
     * @param session disconnected session
     */
    private static void closeSession(Session session) {
        if (session != null && session.isOpen()) {
            synchronized (lockObj) {
                log.debug("ws: closeSession: " + session.getId());
                removeSessionInfo(session);
                try {
                    session.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Remove session info.
     * @param session disconnected session
     */
    private static void removeSessionInfo(Session session) {
        synchronized (lockObj) {
            Map<String, Object> userProperties = session.getUserProperties();
            String cellId = (String) userProperties.get(CELL_ID);

            log.debug("ws: removeSessionInfo: " + cellId);

            if (cellId != null) {
                Timer heartBeatInterval = (Timer) userProperties.get(HEART_BEAT);
                if (heartBeatInterval != null) {
                    heartBeatInterval.cancel();
                }

                userProperties.remove(CELL_ID);
                userProperties.remove(ACCESS_TOKEN);
                userProperties.remove(RULES);
                userProperties.remove(HEART_BEAT);
                userProperties.remove(AUTHORIZED_TIME);
                userProperties.remove(PING_COUNT);

                List<Session> sessionList = cellSessionMap.get(cellId);
                sessionList.remove(session);

                if (sessionList.size() == 0) {
                    cellSessionMap.remove(cellId);
                }

            }
        }
    }

    /**
     * send text message to client with session.
     * @param session send-to websocket session
     * @param message sent text
     */
    private static void sendText(Session session, String message) {
        if (session.isOpen()) {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            try {
                List<Session> sessionList = cellSessionMap.get(cellId);
                List<Session> expiredSessionList = new ArrayList<>();
                if (sessionList != null) {
                    for (Session session : sessionList) {
                        if (!session.isOpen()) {
                            expiredSessionList.add(session);
                            continue;
                        }
                        Map<String, Object> userProperties = session.getUserProperties();
                        String accessToken = (String) userProperties.get(ACCESS_TOKEN);
                        if (accessToken == null) {
                            continue;
                        }
                        Date authorizedDate = (Date) userProperties.get(AUTHORIZED_TIME);
                        if (isExpired(authorizedDate)) {
                            log.debug("ws: token expired. " + cellId);
                            expiredSessionList.add(session);
                            continue;
                        }
                        if (!isExistMatchedRule(session, event)) {
                            continue;
                        }
                        String sendMessage = toJSON(event).toJSONString();
                        sendText(session, sendMessage);
                        log.debug("ws: sent!: [" + cellId + "][" + session.getId() + "] " + sendMessage);
                    }
                    for (Session disconSession : expiredSessionList) {
                        log.debug("ws: session close : ", disconSession.getId());
                        closeSession(disconSession);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                        && (event.getType().startsWith(rule.type) || rule.type.equals("*"))
                        && rule.object != null
                        && event.getObject() != null
                        && (event.getObject().startsWith(rule.object) || rule.object.equals("*"))) {
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


    /**
     * check if session access_token is expired.
     * @param authorizedDate authorized date
     * @return boolean
     */
    private static boolean isExpired(Date authorizedDate) {
        long now = new Date().getTime();
        long expiresLimit = authorizedDate.getTime() + EXPIRES_IN_SECONDS * MILLISECS_IN_A_SEC;
        if (now > expiresLimit) {
            return true;
        }
        return false;
    }

}
