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
package io.personium.core.rule.action;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Cell;
import io.personium.core.model.ctl.Common;
import io.personium.core.rule.ActionInfo;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.ResourceUtils;

/**
 * Action class for relay.data action.
 */
public class RelayDataAction extends HttpAction {
    static Logger logger = LoggerFactory.getLogger(RelayDataAction.class);
    static final String ERROR_CODE = Integer.toString(HttpStatus.SC_NOT_FOUND);

    // HTTP method
    private String method;
    // source url without entityid
    private String srcUrl;
    // original entityid
    private String idFrom;
    // updated entityid
    private String idTo;

    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public RelayDataAction(Cell cell, ActionInfo ai) {
        super(cell, ai);
    }

    private Optional<String> readSourceData(PersoniumEvent event) {
        HttpGet req;
        try {
            req = new HttpGet(srcUrl + putParenthesesAround(idTo));
        } catch (Exception e) {
            return Optional.empty();
        }

        // headers
        setCommonHeaders(req, event);
        // accept
        req.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        // token
        Optional<String> accessToken = new TokenBuilder().cellUrl(cell.getUrl())
                                                         .targetCellUrl(cell.getUrl())
                                                         .subject(event.getSubject().orElse(null))
                                                         .schema(event.getSchema().orElse(null))
                                                         .roleList(event.getRoleList())
                                                         .build();
        accessToken.ifPresent(token ->
                        req.addHeader(HttpHeaders.AUTHORIZATION,
                                      OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX + token));

        String result = null;
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
             CloseableHttpResponse response = client.execute(req)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                result = EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            logger.debug("read data error");
            result = null;
        }

        return Optional.ofNullable(result);
    }

    private String writeData(PersoniumEvent event, Map<String, Object> map) {
        String errMsg = "";

        // remove fragment from service
        String url = ActionUtils.getUrl(service);
        Optional<HttpEntityEnclosingRequestBase> oReq = Optional.empty();
        if (isPostMethod()) {
            try {
                oReq = Optional.ofNullable(new HttpPost(url));
            } catch (Exception e) {
                errMsg = "url is invalid";
            }
        } else if (isPutMethod()) {
            // create url
            try {
                oReq = Optional.ofNullable(new HttpPut(url + putParenthesesAround(idFrom)));
            } catch (Exception e) {
                errMsg = "url is invalid";
            }
        }

        return oReq.map(req -> {
                        map.put(Common.P_ID.getName(), idTo);
                        String data = ResourceUtils.convertMapToString(map);
                        logger.debug("writeData: " + req.getMethod() + " " + data);

                        // create payload as JSON
                        if (data != null) {
                            req.setEntity(new StringEntity(data,
                                                           ContentType.APPLICATION_JSON
                                                                      .withCharset(StandardCharsets.UTF_8)));
                        }

                        // set headers
                        setCommonHeaders(req, event);
                        Optional<String> accessToken;
                        accessToken = new TokenBuilder().cellUrl(cell.getUrl())
                                                        .targetCellUrl(ActionUtils.getCellUrl(this.service))
                                                        .subject(event.getSubject().orElse(null))
                                                        .schema(event.getSchema().orElse(null))
                                                        .roleList(event.getRoleList())
                                                        .build();
                        accessToken.ifPresent(token ->
                                        req.addHeader(HttpHeaders.AUTHORIZATION,
                                                      OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX + token));

                        String result;
                        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
                             CloseableHttpResponse response = client.execute(req)) {
                            result = Integer.toString(response.getStatusLine().getStatusCode());
                        } catch (Exception e) {
                            result = ERROR_CODE;
                        }

                        return result;
                    })
                   .orElse(ERROR_CODE + errMsg);
    }

    private boolean isPostMethod() {
        return "POST".equals(method);
    }
    private boolean isPutMethod() {
        return "PUT".equals(method);
    }

    private String removeParentheses(String id) {
        return Stream.of(id.split("[(')]"))
                     .filter(s -> !s.isEmpty())
                     .findFirst()
                     .orElse("");
    }

    private String putParenthesesAround(String id) {
        StringBuilder builder = new StringBuilder();
        builder.append("('");
        builder.append(id);
        builder.append("')");
        return builder.toString();
    }

    private boolean analyzeEvent(PersoniumEvent event) {
        // Type
        method = event.getType()
                      .map(type -> {
                           if (PersoniumEventType.odata(null, PersoniumEventType.Operation.CREATE)
                                                 .equals(type)) {
                               return "POST";
                           } else if (PersoniumEventType.odata(null, PersoniumEventType.Operation.UPDATE)
                                                        .equals(type)) {
                               return "PUT";
                           } else if (PersoniumEventType.odata(null, PersoniumEventType.Operation.MERGE)
                                                        .equals(type)) {
                               return "PUT";
                           } else {
                               return null;
                           }
                       })
                      .orElse("");

        // Object
        List<String> split = event.getObject()
                                  .map(obj -> {
                                       List<String> list = Stream.of(obj.split("[(')]"))
                                                                 .filter(s -> !s.isEmpty())
                                                                 .collect(Collectors.toList());
                                       if (list.size() == 2) {
                                           return list;
                                       } else {
                                           return null;
                                       }
                                   })
                                  .orElse(Arrays.asList("", ""));

        // Info
        String id = event.getInfo()
                         .map(info -> {
                              String[] array = info.split(",", 2);
                              if (array.length == 2) {
                                  return array[1];
                              } else {
                                  return null;
                              }
                          })
                         .orElse("");

        if (isPostMethod()) {
            idFrom = "";
            idTo = split.get(1);
        } else if (isPutMethod()) {
            idFrom = split.get(1);
            idTo = removeParentheses(id);
        } else {
            return false;
        }
        srcUrl = split.get(0);
        return true;
    }

    @Override
    public PersoniumEvent execute(PersoniumEvent event) {
        // analyze Type, Object and Info of event.
        if (!analyzeEvent(event)) {
            // create event for result of action execution
            PersoniumEvent evt = event.clone()
                                      .type(action)
                                      .object(service)
                                      .info("unsupported event")
                                      .eventId(eventId)
                                      .ruleChain(chain)
                                      .build();

            return evt;
        }

        Optional<String> sourceData = readSourceData(event);

        String result;
        result = sourceData.map(data -> {
                                // remove unnecessary key
                                try {
                                    return ActionUtils.getEntityAsMap(data);
                                } catch (Exception e) {
                                    return null;
                                }
                            })
                           .map(map -> {
                                return writeData(event, map);
                            })
                           .orElse(ERROR_CODE);

        // create event for result of action execution
        PersoniumEvent evt = event.clone()
                                  .type(action)
                                  .object(service)
                                  .info(result)
                                  .eventId(eventId)
                                  .ruleChain(chain)
                                  .build();

        return evt;
    }

    @Override
    protected Optional<String> getVia(PersoniumEvent event) {
        // for X-Personium-Via header
        return Optional.ofNullable(event.getVia().map(via -> via + "," + cell.getUrl()).orElse(cell.getUrl()));
    }

}
