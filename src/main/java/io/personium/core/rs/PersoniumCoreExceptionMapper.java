/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
 * - FUJITSU LIMITED
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

import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.exceptions.ODataErrorMessage;

/**
 * Exception mapper for this application. Perform log output and error response output in an appropriate form.
 */
@Provider
public final class PersoniumCoreExceptionMapper implements ExceptionMapper<Exception> {
    static final int ERROR_ID_ROOT = 100000;
    static Logger log = LoggerFactory.getLogger(PersoniumCoreExceptionMapper.class);

    @Override
    public Response toResponse(final Exception exception) {
        //If PersoniumCoreException
        if (exception instanceof PersoniumCoreException) {
            return this.handlePersoniumCoreException((PersoniumCoreException) exception);
        }
        //If JaxRS exception
        if (exception instanceof WebApplicationException) {
            return this.handleWebApplicationException((WebApplicationException) exception);
        }
        /*
         * Handling exceptions other than PersoniumCoreException. However, when WebApplicationException or its subclass is thrown,
         * It was processed by the JAX-RS layer and it seems not to come here. (This is true in Jersey)
         */
        //Log output
        //Since Unknown Exception occurs in various cases, it makes ID of a random number so that log becomes unique.
        String id = Math.abs(UUID.randomUUID().getMostSignificantBits() % ERROR_ID_ROOT) + "";
        StackTraceElement[] ste = exception.getStackTrace();
        StringBuilder sb = new StringBuilder("[PR500-SV-9999] - Unknown Exception [" + id + "] ");
        sb.append(exception.getMessage());
        if (ste != null && ste.length > 0) {
            sb.append(" @");
            sb.append(ste[0].getClassName() + "#" + ste[0].getMethodName() + ": " + ste[0].getLineNumber());
        }

        log.error(sb.toString(), exception);
        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .entity(new ODataErrorMessage("PR500-SV-9999", "en_US", "Unknown Exception [" + id + "]")).build();
    }

    /*
     * Handling of PersoniumCoreException.
     */
    public Response handlePersoniumCoreException(final PersoniumCoreException pce) {
        pce.log(log);
        Response res = pce.createResponse();
        return res;
    }

    /*
     * Handling of PersoniumCoreException.
     */
    private Response handleWebApplicationException(final WebApplicationException webappException) {
        Response res = webappException.getResponse();
        if (HttpStatus.SC_METHOD_NOT_ALLOWED == res.getStatus()) {
            return this.handlePersoniumCoreException(PersoniumCoreException.Misc.METHOD_NOT_ALLOWED);
        } else if (HttpStatus.SC_NOT_FOUND == res.getStatus()) {
            return this.handlePersoniumCoreException(PersoniumCoreException.Misc.NOT_FOUND);
        } else if (HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE == res.getStatus()) {
            return this.handlePersoniumCoreException(PersoniumCoreException.Misc.UNSUPPORTED_MEDIA_TYPE_NO_PARAMS);
        }
        return res;
    }
}
