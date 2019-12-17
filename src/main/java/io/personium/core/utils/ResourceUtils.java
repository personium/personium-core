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
package io.personium.core.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.UUIDs;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.common.utils.CommonUtils.HttpHeaders;
import io.personium.core.PersoniumCoreException;

/**
 * A class that collects resource-related utility functions.
 */
public class ResourceUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * constructor.
     */
    private ResourceUtils() {
    }

    /**
     * Get request body from Reader and make it JSONObject.
     * @param reader request body
     * @return JSONObject
     */
    public static JSONObject parseBodyAsJSON(Reader reader) {
        StringBuilder sb = new StringBuilder();
        int character;
        JSONObject body;
        try {
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
        } catch (IOException e) {
            throw PersoniumCoreException.Common.REQUEST_BODY_LOAD_FAILED.reason(e);
        }
        String bodyString = sb.toString();
        try {
            body = (JSONObject) (new JSONParser()).parse(bodyString);
        } catch (ParseException e) {
            throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(bodyString);
        }
        return body;
    }

    /**
     * Get request body from Reader and make it Map.
     * @param reader request body
     * @return Map
     */
    public static Map<String, Object> parseBodyAsJsonToMap(Reader reader) {
        Map<String, Object> body;
        try {
            body = objectMapper.readValue(reader, new TypeReference<Map<String, Object>>() { });
        } catch (JsonParseException | JsonMappingException e) {
            throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(e.getMessage());
        } catch (IOException e) {
            throw PersoniumCoreException.Common.REQUEST_BODY_LOAD_FAILED.reason(e);
        }
        return body;
    }

    /**
     * Convert JSON String to Map.
     * @param json json string
     * @return Map
     */
    public static Map<String, Object> convertToMap(String json) {
        Map<String, Object> body;
        try {
            body = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            if (json != null) {
                body = new HashMap<>();
                body.put("content", json);
            } else {
                body = null;
            }
        }
        return body;
    }

    /**
     * Convert List to JSON String.
     * @param list list of string
     * @return converted JSON string
     */
    public static String convertListToString(List<String> list) {
        try {
            List<Map<String, Object>> lm;
            lm = list.stream().map(s -> convertToMap(s))
                              .filter(map -> map != null)
                              .collect(Collectors.toList());
            return objectMapper.writeValueAsString(lm);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Convert Map to JSON String.
     * @param map Map object
     * @return converted JSON string
     */
    public static String convertMapToString(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Check from the header information whether or not the request body exists <br />
     * * In this method, the request body is not read, and the presence or absence of the request body is judged from the header information.
     * @param contentLength Value of the Content-Length header
     * @param transferEncoding Value of Transfer-Encoding header
     * @return true: request body exists false: request body does not exist
     */
    public static boolean hasApparentlyRequestBody(Long contentLength, String transferEncoding) {
        //If the following is satisfied, it is judged that the request body exists
        return ((null != contentLength && contentLength > 0L) //NOPMD -To maintain readability
                || null != transferEncoding);
    }

    static final int MAXREQUEST_KEY_LENGTH = 128;
    static final String REQEUST_KEY_DEFAULT_FORMAT = "%s_%s";
    static final Pattern REQUEST_KEY_PATTERN = Pattern.compile("[\\p{Alpha}\\p{Digit}_-]*");

    /**
     * Validate X-Personium-RequestKey Header.<br/>
     * Throw exception in case of invalid requestKey. <br/>
     * Set the default value if requestKey is null.
     * @param requestKey X-Personium-RequestKey header's string
     * @return valid requestKey
     */
    public static String validateXPersoniumRequestKey(String requestKey) {
        if (null == requestKey) {
            String uuid64 = UUIDs.randomBase64UUID();
            requestKey = String.format(REQEUST_KEY_DEFAULT_FORMAT, uuid64.substring(0, 4), uuid64.substring(4));
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
     * It creates and returns a ResponseBuilder that handles the normal response to the OPTIONS method.
     * @param allowedMethods Allowed HTTP method string.
     * @return ResponseBuilder
     */
    public static ResponseBuilder responseBuilderForOptions(String... allowedMethods) {
        StringBuilder allowedMethodsBuilder = new StringBuilder(javax.ws.rs.HttpMethod.OPTIONS);
        if (allowedMethods != null && allowedMethods.length > 0) {
            allowedMethodsBuilder.append(", ");
            allowedMethodsBuilder.append(StringUtils.join(allowedMethods, ", "));
        }
        return Response.ok().header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethodsBuilder.toString())
                .header(HttpHeaders.ALLOW, allowedMethodsBuilder.toString());
    }

    /**
     * Return ResponseBuilder with converting list object to Json string.
     * @param list list of string
     * @return ResponseBuilder
     */
    public static ResponseBuilder responseBuilderJson(List<String> list) {
        String ret = convertListToString(list);
        if (ret != null) {
            return Response.ok(ret, MediaType.APPLICATION_JSON);
        } else {
            return Response.serverError();
        }
    }

    /**
     * return ResponseBuilder with converting map object to Json string.
     * @param map map object
     * @return ResponseBuilder
     */
    public static ResponseBuilder responseBuilderJson(Map<String, Object> map) {
        String ret = convertMapToString(map);
        if (ret != null) {
            return Response.ok(ret, MediaType.APPLICATION_JSON);
        } else {
            return Response.serverError();
        }
    }
}
