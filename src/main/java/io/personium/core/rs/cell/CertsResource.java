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
package io.personium.core.rs.cell;

import java.security.interfaces.RSAPublicKey;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

import io.personium.core.model.CellCmp;
import io.personium.core.model.impl.fs.CellKeysFile;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS resource.
 * Endpoint that handles the public key of OpenID Connect.
 */
public class CertsResource {

    /** Import target cell information. */
    private CellCmp cellCmp;

    /**
     * Constructor.
     * @param cellCmp CellCmp
     */
    public CertsResource(CellCmp cellCmp) {
        this.cellCmp = cellCmp;
    }

    /**
     * Processing of the GET method.
     * Return public key information set in Cell.
     * @return public key information (JWK)
     */
    @GET
    public Response get() {
        CellKeysFile cellKeysFile = cellCmp.getCellKeys().getCellKeysFile();
        RSAPublicKey publicKey = (RSAPublicKey) cellKeysFile.getPublicKey();
        JsonWebKey jwk = JwkUtils.fromRSAPublicKey(
                publicKey, AlgorithmUtils.RS_SHA_256_ALGO, cellKeysFile.getKeyId());
        JsonWebKeys jwks = new JsonWebKeys(jwk);
        return Response.ok(JwkUtils.jwkSetToJson(jwks), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Processing of the OPTIONS method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET
                ).build();
    }
}
