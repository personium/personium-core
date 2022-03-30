/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.stream.DataSubscriber;
import io.personium.core.stream.IDataListener;
import io.personium.core.stream.StreamFactory;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * Common Endpoint for Stream.
 */
public class StreamEndpoint implements IDataListener {
    // log
    private static Logger log = LoggerFactory.getLogger(StreamEndpoint.class);

    private static final byte[] PING_DATA = new byte[]{1, 2, 3};
    private static final int PING_MAX = 10;
    private static final int HEART_BEAT_TIME = 60000;
    private static final long INIT_EXPIRES_IN = 60000;

    // Keys of session user properties
    private static final String KEY_PROPERTIES_TOPIC = "topic";
    private static final String KEY_PROPERTIES_HEART_BEAT = "heart_beat";

    // JSON Keys
    private static final String KEY_JSON_ACCESS_TOKEN = "AccessToken";
    private static final String KEY_JSON_UNKNOWN = "Unknown";
    private static final String KEY_JSON_EXPIRES_IN = "ExpiresIn";
    private static final String KEY_JSON_TIMESTAMP = "Timestamp";
    private static final String KEY_JSON_RESPONSE = "Response";
    private static final String KEY_JSON_RESULT = "Result";
    private static final String KEY_JSON_REASON = "Reason";

    // Keywords used in JSON Values
    private static final String RESPONSE_SUCCESS = "Success";
    private static final String RESPONSE_ERROR = "Error";

    // Description lines in JSON Values
    private static final String REASON_INVALID_MESSAGE = "Invalid message";
    private static final String REASON_TOKEN_INACTIVE = "Token inactive";
    private static final String REASON_SESSION_EXPIRED = "Session expired";

    /** separator of topic name. */
    static final String SEPARATOR = ".";

    // session
    private Session mySession;

    // expiration time
    private AtomicLong expirationTime;

    // ping
    private AtomicInteger sendPingCount;

    // subscriber
    private Optional<DataSubscriber> subscriber;

    /**
     * for IDataListener.
     */
    @Override
    public void onMessage(String cellUrl, String data) {
        log.debug("onMessage: {}, {}", cellUrl, data);

        if (isExpired()) {
            log.debug("ws: session expired. session close.: {}" + mySession.getId());
            closeSession();
            return;
        }

        // {From:cellurl, Body:{}}
        Map<String, Object> map = new HashMap<>();
        map.put("From", cellUrl);
        map.put("Body", ResourceUtils.convertToMap(data));
        sendText(mySession, ResourceUtils.convertMapToString(map));
    }


    private void subscribe(final String topic) {
        if (subscriber.isPresent()) {
            // if present, do nothing.
            return;
        }
        subscriber = StreamFactory.createDataSubscriber();
        subscriber.ifPresent(sub -> {
                       sub.subscribe(topic);
                       sub.setListener(this);
                   });
    }

    private void unsubscribe() {
        subscriber.ifPresent(sub -> sub.unsubscribe());
        subscriber = Optional.empty();
    }

    private void closeSession() {
        if (mySession != null) {
            try {
                mySession.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This callback method is called when a client connects.
     * @param topic topic specified in url param
     * @param session WebSocket session
     */
    public void onOpen(String topic, Session session) {
        log.debug("ws: onOpen[{}]: {}", topic, session.getId());

        // initialization
        mySession = session;
        expirationTime = new AtomicLong(System.currentTimeMillis() + INIT_EXPIRES_IN);
        sendPingCount = new AtomicInteger(0);
        subscriber = Optional.empty();

        // set topic
        Map<String, Object> userProperties = session.getUserProperties();
        userProperties.put(KEY_PROPERTIES_TOPIC, topic);

        // heartbeat (send ping).
        Timer heartBeatInterval = new Timer();
        heartBeatInterval.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        },  HEART_BEAT_TIME, HEART_BEAT_TIME);
        userProperties.put(KEY_PROPERTIES_HEART_BEAT, heartBeatInterval);
    }

    private void sendPing() {
        // session closes if it is not received pong message PING_MAX times from the client.
        if (sendPingCount.getAndIncrement() > PING_MAX || isExpired()) {
            closeSession();
        } else {
            try {
                mySession.getBasicRemote().sendPing(ByteBuffer.wrap(PING_DATA));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This callback method is called when a client disconnects.
     * @param session WebSocket session
     */
    public void onClose(Session session) {
        Map<String, Object> userProperties = session.getUserProperties();
        String topic = (String) userProperties.get(KEY_PROPERTIES_TOPIC);
        log.debug("ws: onClose[{}]: {}", topic, session.getId());
        unsubscribe();
        mySession = null;

        Timer heartBeatInterval = (Timer) userProperties.get(KEY_PROPERTIES_HEART_BEAT);
        if (heartBeatInterval != null) {
            heartBeatInterval.cancel();
        }
    }

    /**
     * This callback method is called when a client send message data.
     * @param text received message
     * @param session sender session
     */
    public void onMessage(String text, Session session) {
        Map<String, Object> userProperties = session.getUserProperties();
        String topic = (String) userProperties.get(KEY_PROPERTIES_TOPIC);
        log.debug("ws: onMessage[{}][{}]: {}", topic, session.getId(), text);

        Map<String, Object> result = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        result.put(KEY_JSON_TIMESTAMP, timestamp);

        if (isExpired()) {
            log.debug("ws: session expired. session close.: {}", session.getId());

            result.put(KEY_JSON_RESPONSE, KEY_JSON_UNKNOWN);
            result.put(KEY_JSON_RESULT, RESPONSE_ERROR);
            result.put(KEY_JSON_REASON, REASON_SESSION_EXPIRED);
            sendText(session, ResourceUtils.convertMapToString(result));

            closeSession();
            return;
        }

        Map<String, Object> map = ResourceUtils.convertToMap(text);

        // token must be sent by client before communication
        String receivedAccessToken = (String) map.get(KEY_JSON_ACCESS_TOKEN);
        if (receivedAccessToken != null) {
            long exp = checkAccessToken(receivedAccessToken, topic);

            result.put(KEY_JSON_RESPONSE, KEY_JSON_ACCESS_TOKEN);

            if (exp < 0) {
                log.debug("ws: token inactive. session close.: {}", session.getId());

                result.put(KEY_JSON_RESULT, RESPONSE_ERROR);
                result.put(KEY_JSON_REASON, REASON_TOKEN_INACTIVE);
                sendText(session, ResourceUtils.convertMapToString(result));

                closeSession();
                return;
            }
            // set expiration time
            expirationTime.set(exp);

            result.put(KEY_JSON_RESULT, RESPONSE_SUCCESS);
            result.put(KEY_JSON_EXPIRES_IN, TimeUnit.MILLISECONDS.toSeconds(exp - timestamp));
            sendText(session, ResourceUtils.convertMapToString(result));

            // subscribe topic
            subscribe(topic);

        } else {
            log.debug("Invalid message: " + text);

            result.put(KEY_JSON_RESPONSE, KEY_JSON_UNKNOWN);
            result.put(KEY_JSON_RESULT, RESPONSE_ERROR);
            result.put(KEY_JSON_REASON, REASON_INVALID_MESSAGE);
            sendText(session, ResourceUtils.convertMapToString(result));

            closeSession();
        }
    }

    /**
     * On receive access token.
     * @param token token string
     * @param topic dot-separated topic string
     */
    private long checkAccessToken(String token, String topic) {
        if (checkTopic(token, topic) && checkPrivilege(token, topic)) {
            return getExpirationTime(token, topic);
        } else {
            return -1;
        }
    }

    /**
     * On returning pong message.
     * @param pongMessage pong
     * @param session session
     */
    public void onMessage(PongMessage pongMessage, Session session) {
        sendPingCount.getAndDecrement();
        // int count = sendPingCount.getAndDecrement();
        // log.debug("ws: receive pong message: {}:{}", session.getId(), count);
    }

    /**
     * Error occurred.
     * @param session session info
     * @param e Throwable Error Information
     */
    public void onError(Session session, Throwable e) {
        log.error("ws: onError: {}", e.getMessage());
        // e.printStackTrace();
        closeSession();
    }

    /**
     * send text message to client with session.
     * @param session send-to websocket session
     * @param message sent text
     */
    private void sendText(Session session, String message) {
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

    private String convertUrl(String url) {
        if (!PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
            // path based cell url -> fqdn based cell url
            try {
                return UriUtils.convertPathBaseToFqdnBase(url);
            } catch (URISyntaxException e) {
                return url;
            }
        } else {
            return url;
        }
    }

    private boolean checkTopic(String token, String topic) {
        boolean result = false;

        if (token == null || topic == null) {
            return result;
        }

        // url
        //  topic: cell.box.{dir}+.col.topic.topicname -> /cell/box/{dir}+/col
        List<String> paths = Stream.of(topic.split(Pattern.quote(SEPARATOR)))
                                   .collect(Collectors.toList());
        String topicName = paths.remove(paths.size() - 1);
        String type = paths.remove(paths.size() - 1);
        if (!"topic".equals(type)) {
            return result;
        }
        String path = paths.stream().collect(Collectors.joining("/"));

        String url = convertUrl(PersoniumUnitConfig.getBaseUrl() + path);
        HttpPropfind req;
        try {
            req = new HttpPropfind(url);
        } catch (Exception e) {
            return result;
        }

        req.addHeader(HttpHeaders.AUTHORIZATION,
                      CommonUtils.createBearerAuthzHeader(token));
        req.addHeader(HttpHeaders.DEPTH, "0");
        req.addHeader(HttpHeaders.ACCEPT, "application/xml");

        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
             CloseableHttpResponse response = client.execute(req)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MULTI_STATUS) {
                // check Content-Type: application/xml
                Header header = response.getLastHeader(HttpHeaders.CONTENT_TYPE);
                if (!"application/xml".equals(header.getValue())) {
                    return result;
                }

                // check if topic exists
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(response.getEntity().getContent());
                    Element docElem = doc.getDocumentElement();
                    NodeList propstatList = docElem.getElementsByTagName("propstat");
                    for (int i = 0; i < propstatList.getLength(); i++) {
                        Element propstatElem = (Element) propstatList.item(i);
                        // check status
                        NodeList statusList = propstatElem.getElementsByTagName("status");
                        Node statusNode = statusList.item(statusList.getLength() - 1);
                        if (!"HTTP/1.1 200 OK".equals(statusNode.getTextContent())) {
                            break;
                        }
                        // check resourcetype
                        NodeList resourcetypeList = propstatElem.getElementsByTagName("resourcetype");
                        if (resourcetypeList.getLength() != 1) {
                            break;
                        }
                        Element resourcetypeElem = (Element) resourcetypeList.item(0);
                        NodeList streamList;
                        streamList = resourcetypeElem.getElementsByTagNameNS(CommonUtils.XmlConst.NS_PERSONIUM,
                                                                             "stream");
                        if (streamList.getLength() != 1) {
                            break;
                        }
                        // check topics
                        NodeList topicsList;
                        topicsList = propstatElem.getElementsByTagNameNS(CommonUtils.XmlConst.NS_PERSONIUM,
                                                                         "topics");
                        for (int j = 0; j < topicsList.getLength(); j++) {
                            Element topicElem = (Element) topicsList.item(j);
                            NodeList topicList;
                            topicList = topicElem.getElementsByTagNameNS(CommonUtils.XmlConst.NS_PERSONIUM,
                                                                         "topic");
                            for (int k = 0; k < topicList.getLength(); k++) {
                                if (topicName.equals(topicList.item(k).getTextContent())) {
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            log.warn("exception occurred: ", e);
        }

        return result;
    }

    /**
     * class for handling PROPFIND method.
     */
    private class HttpPropfind extends HttpEntityEnclosingRequestBase {
        public static final String METHOD_NAME = "PROPFIND";

        HttpPropfind(String uri) throws IllegalArgumentException {
            try {
                URI u = new URI(uri);
                setURI(u);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }
    }

    /**
     * check if token can read stream.
     * @param token token string
     * @param topic topic specified on open
     * @return
     */
    private boolean checkPrivilege(String token, String topic) {
        boolean result = false;

        if (token == null || topic == null) {
            return result;
        }

        // topic: cell.box.{dir}+.col.topic.topicname
        String url = convertUrl(PersoniumUnitConfig.getBaseUrl() + topic.replace(SEPARATOR, "/"));
        HttpOptions req;
        try {
            req = new HttpOptions(url);
        } catch (Exception e) {
            return result;
        }

        req.addHeader(HttpHeaders.AUTHORIZATION,
                      CommonUtils.createBearerAuthzHeader(token));

        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
             CloseableHttpResponse response = client.execute(req)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Header header = response.getLastHeader(HttpHeaders.ALLOW);
                List<String> allows = Stream.of(header.getValue().split(Pattern.quote(",")))
                                            .map(s -> s.trim())
                                            .filter(s -> !s.isEmpty())
                                            .collect(Collectors.toList());
                if (allows.contains("GET")) {
                    result = true;
                }
            }
        } catch (Exception e) {
            log.warn("exception occurred: ", e);
        }

        return result;
    }

    private long getExpirationTime(String token, String topic) {
        long result = -1;

        if (token == null || topic == null) {
            return result;
        }
        Optional<String> cell = Stream.of(topic.split(Pattern.quote(SEPARATOR)))
                                      .filter(s -> !s.isEmpty())
                                      .findFirst();
        String url = cell.map(name -> PersoniumUnitConfig.getBaseUrl() + name + "/__introspect")
                         .orElse("");
        url = convertUrl(url);

        HttpPost req;
        try {
            req = new HttpPost(url);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", token));
            req.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            req.addHeader(HttpHeaders.AUTHORIZATION,
                          CommonUtils.createBasicAuthzHeader(PersoniumUnitConfig.getIntrospectUsername(),
                                                                    PersoniumUnitConfig.getIntrospectPassword()));
        } catch (Exception e) {
            return result;
        }

        req.addHeader(HttpHeaders.ACCEPT, "application/json");

        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
             CloseableHttpResponse response = client.execute(req)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // check content-type
                Header header = response.getLastHeader(HttpHeaders.CONTENT_TYPE);
                if (!"application/json".equals(header.getValue())) {
                    return result;
                }

                // get expiration time
                String res = EntityUtils.toString(response.getEntity());
                Map<String, Object> map = ResourceUtils.convertToMap(res);
                if (map != null) {
                    Object obj = map.get("active");
                    if (obj instanceof Boolean) {
                        boolean active = (boolean) obj;
                        if (active) {
                            obj = map.get("exp");
                            if (obj instanceof Integer) {
                                int exp = (int) obj;
                                result = TimeUnit.SECONDS.toMillis(exp);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("exception occurred: ", e);
        }

        return result;
    }


    /**
     * check if session is expired.
     * @return boolean
     */
    private boolean isExpired() {
        long exp = expirationTime.get();
        long now = System.currentTimeMillis();

        return now >= exp;
    }

}
