/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
package io.personium.core.rule.action;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpMessage;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.ResourceUtils;

/**
 * Abstract class of Action about Post.
 */
public abstract class PostAction extends HttpAction {
    static Logger logger = LoggerFactory.getLogger(PostAction.class);

    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public PostAction(Cell cell, ActionInfo ai) {
        super(cell, ai);
    }

    @Override
    public PersoniumEvent execute(PersoniumEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Event Execute with '" + event.getType() + " '.");
            logger.debug("    External: " + event.getExternal());
            logger.debug("    Schema: " + event.getSchema());
            logger.debug("    Subject: " + event.getSubject());
            logger.debug("    Type: " + event.getType());
            logger.debug("    Object: " + event.getObject());
            logger.debug("    Info: " + event.getInfo());
            logger.debug("    RequestKey: " + event.getRequestKey());
            logger.debug("    EventId: " + event.getEventId());
            logger.debug("    RuleChain: " + event.getRuleChain());
            logger.debug("    Via: " + event.getVia());
            logger.debug("    Roles: " + event.getRoles());
            logger.debug("    CellId: " + event.getCellId());
            logger.debug("    Time: " + event.getTime());
        }

        String requestUrl = getRequestUrl();
        if (requestUrl == null) {
            return null;
        }

        HttpPost req = new HttpPost(requestUrl);

        // create payload as JSON
        Map<String, Object> map = createEvent(event);
        String body = ResourceUtils.convertMapToString(map);
        if (body != null) {
            req.setEntity(new StringEntity(
                    body,
                    ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
        }

        // set headers
        //  X-Personium-RequestKey, X-Personium-EventId, X-Personium-RuleChain, X-Personium-Via
        setCommonHeaders(req, event);

        // set specific headers in derrived class
        setSpecificHeaders(req, event);

        String result;
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
             CloseableHttpResponse objResponse = client.execute(req)) {
            logger.info(EntityUtils.toString(objResponse.getEntity()));
            result = Integer.toString(objResponse.getStatusLine().getStatusCode());
        } catch (ClientProtocolException e) {
            logger.error("Invalid Http response: " + e.getMessage(), e);
            result = "404";
        } catch (Exception e) {
            logger.error("Connection Error: " + e.getMessage(), e);
            result = "404";
        }

        // create event for result of script execution
        PersoniumEvent evt = event.clone()
                                  .type(action)
                                  .object(service)
                                  .info(result)
                                  .eventId(eventId)
                                  .ruleChain(chain)
                                  .build();

        return evt;
    }

    /**
     * Create request url in derrived class.
     * @return created request url, null if error is occurred
     */
    protected abstract String getRequestUrl();

    /**
     * Create event.
     * @param event PersoniumEvent object
     * @return Map object
     */
    protected Map<String, Object> createEvent(PersoniumEvent event) {
        Map<String, Object> map = new HashMap<>();

        map.put("External", event.getExternal());
        event.getSchema().ifPresent(schema -> map.put("Schema", schema));
        event.getSubject().ifPresent(subject -> map.put("Subject", subject));
        event.getType().ifPresent(type -> map.put("Type", type));
        event.getObject().ifPresent(object -> map.put("Object", object));
        event.getInfo().ifPresent(info -> map.put("Info", info));

        return map;
    }

    /**
     * Set specific HTTP headers in derrived class.
     * @param req Request to set headers
     * @param event PersoniumEvent object
     */
    protected abstract void setSpecificHeaders(HttpMessage req, PersoniumEvent event);

}
