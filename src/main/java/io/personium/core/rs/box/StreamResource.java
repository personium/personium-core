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
package io.personium.core.rs.box;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS resource responsible for StreamResource.
 */
public abstract class StreamResource {

    private static Logger logger = LoggerFactory.getLogger(StreamResource.class);

    DavRsCmp davRsCmp;
    final String type;

    /**
     * constructor.
     * @param parent DavRsCmp
     * @param type type of destination
     */
    public StreamResource(final DavRsCmp parent, final String type) {
        this.davRsCmp = parent;
        this.type = type;
    }

    /**
     * GET method.
     * @param name destination name
     * @return JAX_RS Response
     */
    @Path("{name}")
    @GET
    public Response get(@PathParam("name") String name) {
        return receiveCommon(name);
    }

    /**
     * POST method.
     * @param name destination name
     * @param is Request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{name}")
    @POST
    public Response post(@PathParam("name") String name,
                         final InputStream is) {
        return sendCommon(name, is);
    }

    /**
     * PUT method.
     * @param name destination name
     * @param is Request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{name}")
    @PUT
    public Response put(@PathParam("name") String name,
                        final InputStream is) {
        return sendCommon(name, is);
    }

    /**
     * PUT method.
     * This method is needed to access url with key in relay.data action of rule.
     * key parameter is ignored.
     * @param name destination name
     * @param key key string
     * @param is Request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{name}({key})")
    @PUT
    public Response put(@PathParam("name") final String name,
                        @PathParam("key") final String key,
                        final InputStream is) {
        return sendCommon(name, is);
    }

    /**
     * OPTIONS method.
     * @param name destination name
     * @return JAX-RS Response
     */
    @Path("{name}")
    @OPTIONS
    public Response options(@PathParam("name") String name) {
        // resource exist?
        checkExistence(name);

        List<String> allow = new ArrayList<>();

        try {
            this.davRsCmp.checkAccessContext(BoxPrivilege.STREAM_SEND);
            allow.add(HttpMethod.POST);
            allow.add(HttpMethod.PUT);
        } catch (Exception e) {
            logger.debug("no privilege for send");
        }

        try {
            this.davRsCmp.checkAccessContext(BoxPrivilege.STREAM_RECEIVE);
            allow.add(HttpMethod.GET);
        } catch (Exception e) {
            logger.debug("no privilege for receive");
        }

        return ResourceUtils.responseBuilderForOptions(allow.toArray(new String[allow.size()]))
                            .build();
    }

    /**
     * Get list of resource.
     */
    abstract List<String> getResources();

    /**
     * Receive data from dest.
     * @param dest destination name
     * @return JAX-RS Response
     */
    abstract Response receive(String dest);

    /**
     * Send data to dest.
     * @param dest destination name
     * @param cellUrl url of source cell
     * @param data data string
     * @return JAX-RS Response
     */
    abstract Response send(String dest, String cellUrl, String data);

    private String getUrl(String name) {
        return this.davRsCmp.getUrl() + "/" + this.type + "/" + name;
    }

    /**
     * Create destination from name.
     * @param name destination name
     * @return string of destination
     */
    private String createDestination(String name) {
        // convert to localunit url
        String localunit = UriUtils.convertSchemeFromHttpToLocalUnit(getUrl(name));
        try {
            URI uri = new URI(localunit);
            return Stream.of(uri.getPath().split(Pattern.quote("/")))
                         .filter(s -> !s.isEmpty())
                         .collect(Collectors.joining("."));
        } catch (Exception e) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(getUrl(name));
        }
    }

    /**
     * Check the existence of destination in property.
     * @param name destination name
     */
    private void checkExistence(final String name) {
        List<String> resources = getResources();
        if (resources.indexOf(name) == -1) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(getUrl(name));
        }
    }

    /**
     * receive data.
     * @param name resource name
     * @return JAX-RS Response
     */
    private Response receiveCommon(String name) {
        // access control
        this.davRsCmp.checkAccessContext(BoxPrivilege.STREAM_RECEIVE);

        // resource exist?
        checkExistence(name);

        // construct destination
        String dest = createDestination(name);

        return receive(dest);
    }

    /**
     * send to data bus.
     * @param name resource name
     * @param is Request body
     * @return JAX-RS Response
     */
    private Response sendCommon(String name, InputStream is) {
        // access control
        this.davRsCmp.checkAccessContext(BoxPrivilege.STREAM_SEND);

        // resource exist?
        checkExistence(name);

        // convert is to string
        String data;
        try {
            data = IOUtils.toString(is, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw PersoniumCoreException.Common.REQUEST_BODY_LOAD_FAILED.reason(e);
        }
        // construct destination
        String dest = createDestination(name);
        // get cell url
        String cellUrl;
        String via = this.davRsCmp.getVia();
        if (via != null) {
            List<String> cellList;
            cellList = Stream.of(via.split(Pattern.quote(",")))
                             .collect(Collectors.toList());
            cellUrl = cellList.get(0);
        } else {
            // subject of token
            String subject = this.davRsCmp.getAccessContext().getSubject();
            if (subject != null) {
                int index = subject.lastIndexOf('#');
                if (index != -1) {
                    cellUrl = subject.substring(0, index);
                } else {
                    cellUrl = subject;
                }
            } else {
                cellUrl = this.davRsCmp.getCell().getUrl();
            }
        }

        return send(dest, cellUrl, data);
    }

}
