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
package io.personium.core.rs.cell;

import java.text.MessageFormat;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.auth.OAuth2Helper.Key;

/**
 * JAX-RS resource for HTML error response.
 */
public class ErrorHtmlResource {
    static Logger log = LoggerFactory.getLogger(ErrorHtmlResource.class);

    /**
     * GET request.
     * @param host Host header
     * @param code query parameter
     * @param uriInfo context
     * @return JAX-RS Response Object
     */
    @GET
    public final Response implicitError(@HeaderParam(HttpHeaders.HOST) final String host,
            @QueryParam(Key.CODE) final String code,
            @Context final UriInfo uriInfo) {

        // エラーHTMLの返却
        ResponseBuilder rb = Response.ok().type(MediaType.TEXT_HTML);
        return rb.entity(this.htmlForCode(code))
                .header("Content-Type", "text/html; charset=UTF-8").build();
    }

    /**
     * returns HTML string for error code.
     * @param code message code
     * @return HTML string
     */
    private String htmlForCode(String code) {
        String title = PersoniumCoreMessageUtils.getMessage("PS-ER-0001");
        String msg = null;
        try {
            msg = PersoniumCoreMessageUtils.getMessage(code);
        } catch (Exception e) {
            log.info(e.getMessage());
            msg = PersoniumCoreMessageUtils.getMessage("PS-ER-0002");
        }

        String html = PersoniumCoreUtils.readStringResource("html/error.html", CharEncoding.UTF_8);
        html = MessageFormat.format(html, title, msg);
        return html;
    }


}
