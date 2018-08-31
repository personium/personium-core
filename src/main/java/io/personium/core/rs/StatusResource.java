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
package io.personium.core.rs;

import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsClient;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.EsModel;

/**
 *JAX-RS Resource class corresponding to StatusResource.
 */
public class StatusResource {
    static Logger log = LoggerFactory.getLogger(StatusResource.class);
    /** The property key to retrieve the request destination URL.*/
    public static final String PROP_TARGET_URL = "io.personium.test.target";

    /**
     *Processing on the GET method.
     * @return JAS-RS Response
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces("application/json")
    public Response get() {
        StringBuilder sb = new StringBuilder();

        //Property list
        Properties props = PersoniumUnitConfig.getProperties();
        JSONObject responseJson = new JSONObject();
        JSONObject propertiesJson = new JSONObject();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            propertiesJson.put(key, value);
        }
        responseJson.put("properties", propertiesJson);

        //Create / delete Cell
        //responseJson.put("service", checkServiceStatus());

        // ElasticSearch Health
        EsClient client = EsModel.client();
        JSONObject esJson = new JSONObject();
        esJson.put("health", client.checkHealth());
        responseJson.put("ElasticSearch", esJson);

        sb.append(responseJson.toJSONString());
        return Response.status(HttpStatus.SC_OK).entity(sb.toString()).build();
    }

    /**
     *Processing for the POST method.
     * @return JAS-RS Response
     */
    @POST
    public Response post() {
        //Property Reload
        PersoniumUnitConfig.reload();
        return Response.status(HttpStatus.SC_NO_CONTENT).build();
    }

}
