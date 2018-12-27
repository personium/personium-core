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
package io.personium.core.rs.cell;

import javax.ws.rs.GET;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * JAX-RS resource.
 * Endpoint that handles the public key of OpenID Connect.
 */
public class CertsResource {

    /**
     * Constructor.
     */
    public CertsResource() {
    }

    /**
     * Processing of the GET method.
     * Return public key information set in Cell.
     * @return public key information (JWK)
     */
    @SuppressWarnings("unchecked")
    @GET
    public Response get() {
        //TODO Under development.
        // Returns json of dummy.
        JSONObject keyJson = new JSONObject();
        keyJson.put("kty", "RSA");
        keyJson.put("use", "sig");
        keyJson.put("alg", "RS256");
        keyJson.put("kid", "2dx0zf47146wbvt63o24ansk3h0du1yf");
        keyJson.put("n", "dummy_modulus");
        keyJson.put("e", "AQAB");

        JSONArray keysJsonArray = new JSONArray();
        keysJsonArray.add(keyJson);

        JSONObject responseJson = new JSONObject();
        responseJson.put("keys", keysJsonArray);

        return Response.ok(responseJson.toJSONString(), MediaType.APPLICATION_JSON).build();
    }

}
