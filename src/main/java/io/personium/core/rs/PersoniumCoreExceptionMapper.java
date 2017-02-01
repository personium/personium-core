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

import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreMessageUtils.Severity;
import io.personium.core.exceptions.ODataErrorMessage;

/**
 * 本アプリケーションの例外マッパー. ログ出力と適切な形でのエラー応答出力を行う。
 */
@Provider
public final class PersoniumCoreExceptionMapper implements ExceptionMapper<Exception> {
    static final int ERROR_ID_ROOT = 100000;
    static Logger log = LoggerFactory.getLogger(PersoniumCoreExceptionMapper.class);

    @Override
    public Response toResponse(final Exception exception) {
        // PersoniumCoreException ならば
        if (exception instanceof PersoniumCoreException) {
            return this.handlePersoniumCoreException((PersoniumCoreException) exception);
        }
        // JaxRS例外 ならば
        if (exception instanceof WebApplicationException) {
            return this.handleWebApplicationException((WebApplicationException) exception);
        }
        /*
         * PersoniumCoreException以外の例外の扱い。ただし、WebApplicationExceptionやそのサブクラスを投げられると、
         * JAX-RS層で処理されてしまい、ここには来ない模様。（Jerseyではそうなっている）
         */
        // ログ出力
        // Unknown Exceptionはいろいろなケースで発生するため、乱数のIDをつけてログが一意になるようにする。
        String id = Math.abs(UUID.randomUUID().getMostSignificantBits() % ERROR_ID_ROOT) + "";
        StackTraceElement[] ste = exception.getStackTrace();
        StringBuilder sb = new StringBuilder("[PR500-SV-9999] - Unknown Exception [" + id + "] ");
        sb.append(exception.getMessage());
        if (ste != null && ste.length > 0) {
            sb.append(" @ ");
            sb.append(ste[0].getClassName() + "#" + ste[0].getMethodName() + ": " + ste[0].getLineNumber());
        }

        log.error(sb.toString(), exception);
        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .entity(new ODataErrorMessage("PR500-SV-9999", "en_US", "Unknown Exception [" + id + "]")).build();
    }

    /*
     * PersoniumCoreExceptionの扱い。
     */
    private Response handlePersoniumCoreException(final PersoniumCoreException pce) {
        Severity sv = pce.getSeverity();
        String code = pce.getCode();
        String message = pce.getMessage();
        Response res = pce.createResponse();
        String format = String.format("[%s] - %s", code, message);
        Throwable cause = pce.getCause();
        // ログ出力
        switch (sv) {
        case INFO:
            log.info(format, cause);
            break;
        case WARN:
            log.warn(format, cause);
            break;
        case ERROR:
            log.error(format, cause);
            break;
        default:
            log.error("Exception Severity Not Defined", pce);
        }
        return res;
    }

    /*
     * PersoniumCoreExceptionの扱い。
     */
    private Response handleWebApplicationException(final WebApplicationException webappException) {
        Response res = webappException.getResponse();
        if (HttpStatus.SC_METHOD_NOT_ALLOWED == res.getStatus()) {
            return this.handlePersoniumCoreException(PersoniumCoreException.Misc.METHOD_NOT_ALLOWED);
        }
        return res;
    }
}

