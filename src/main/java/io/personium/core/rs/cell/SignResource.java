/**
 * Personium
 * Copyright 2018-2021 Personium Project Authors
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

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;

import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.CellKeyPair;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS resource. Endpoint that handles signing contents
 */
public class SignResource {
    static final String MEDIATYPE_JOSE = "application/jose";

    static Logger log = org.slf4j.LoggerFactory.getLogger(CellResource.class);

    /** cell resource information. */
    private CellRsCmp cellRsCmp;

    /** Target cell information. */
    private CellCmp cellCmp;

    /**
     * Constructor.
     * @param cellRsCmp CellCmp
     */
    public SignResource(CellRsCmp cellRsCmp, CellCmp cellCmp) {
        this.cellRsCmp = cellRsCmp;
        this.cellCmp = cellCmp;
    }

    /**
     * Generating JWS from received InputStream
     * @param contentType
     * @param accept
     * @param inputStream
     * @return
     */
    @POST
    @Produces(MEDIATYPE_JOSE)
    public Response post(final InputStream inputStream) {
        // Access Control
        this.cellRsCmp.checkAccessContext(CellPrivilege.SIGN);

        String result = signJWS(inputStream);

        return Response.ok(result, MEDIATYPE_JOSE).build();
    }

    /**
     * Generate JWS from stream
     * @param inputStream payload data stream
     * @return Signed JWS
     * @throws IOException Exception
     */
    public String signJWS(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("given inputStream is null");
        }

        try {
            // TODO: check the content size
            return signJWS(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate JWS from bytes
     * @param plainJwsPayload payload data bytes
     * @return Signed JWS
     */
    public String signJWS(byte[] plainJwsPayload) {
        if (plainJwsPayload == null) {
            throw new IllegalArgumentException("given plainJwsPayload is null");
        }
        CellKeyPair cellKeyPair = cellCmp.getCellKeys().getCellKeyPairs();
        PrivateKey privKey = cellKeyPair.getPrivateKey();

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayloadBytes(plainJwsPayload);

        jws.setKey(privKey);
        jws.setKeyIdHeaderValue(cellKeyPair.getKeyId());

        // Currently support only RS256
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_PSS_USING_SHA256);

        String result = null;
        try {
            result = jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new RuntimeException("An exception is thrown in generating JWS", e);
        }
        return result;
    }

    /**
     * Processing of the OPTIONS method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        return ResourceUtils.responseBuilderForOptions(HttpMethod.POST).build();
    }
}
