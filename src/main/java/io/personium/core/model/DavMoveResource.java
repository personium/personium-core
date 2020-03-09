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
package io.personium.core.model;

import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.PersoniumUrl;

/**
 * A class that performs processing relating to Move of Dav after receiving processing delegation from the JaxRS Resource object.
 */
public class DavMoveResource extends DavRsCmp {

    private String depth = null;
    private String overwrite = null;
    private String destination = null;
    private String ifMatch = null;

    /**
     * constructor.
     * @param parent parent resource
     * @param davCmp Parts responsible for processing dependent on backend implementation
     * @param headers Request header information
     */
    public DavMoveResource(DavRsCmp parent, DavCmp davCmp, HttpHeaders headers) {
        super(parent, davCmp);
        //From the request header, obtain information necessary for movement
        depth = getFirstHeader(headers, org.apache.http.HttpHeaders.DEPTH, DavCommon.DEPTH_INFINITY);
        overwrite = getFirstHeader(headers, org.apache.http.HttpHeaders.OVERWRITE, DavCommon.OVERWRITE_FALSE);
        destination = getFirstHeader(headers, org.apache.http.HttpHeaders.DESTINATION);
        ifMatch = getFirstHeader(headers, HttpHeaders.IF_MATCH, "*");
    }

    /**
     * Processing of the MOVE method.
     * @return JAX-RS response object
     */
    public Response doMove() {

        //Request header validation
        validateHeaders();

        //Acquire the destination Box information
        BoxRsCmp boxRsCmp = getBoxRsCmp();

        //Generate destination information
        DavDestination davDestination;
        try {
            davDestination = new DavDestination(destination, this.getAccessContext().getBaseUri(), boxRsCmp);
        } catch (URISyntaxException e) {
            //Not in URI format
            throw PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(org.apache.http.HttpHeaders.DESTINATION,
                    destination);
        }

        //Update / delete data
        ResponseBuilder response = this.davCmp.move(this.ifMatch, this.overwrite, davDestination);

        return response.build();
    }

    private BoxRsCmp getBoxRsCmp() {
        //Acquire the resource information of the topmost Box
        DavRsCmp davRsCmp = this;
        for (int i = 0; i <= PersoniumUnitConfig.getMaxCollectionDepth(); i++) {
            DavRsCmp parent = davRsCmp.getParent();
            if (null == parent) {
                break;
            }
            //End if Box can not be taken
            if (null == parent.getBox()) {
                break;
            }
            davRsCmp = parent;
        }

        //Treat the results as far as the top as Box
        BoxRsCmp boxRsCmp = (BoxRsCmp) davRsCmp;
        return boxRsCmp;
    }

    /**
     * Validate the header of the Move method <br />
     * If validation fails, throw an exception.
     * @param headers header information
     */
    void validateHeaders() {
        //Depth header
        if (!DavCommon.DEPTH_INFINITY.equalsIgnoreCase(depth)) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
        }

        //Overwrite header
        if (!DavCommon.OVERWRITE_FALSE.equalsIgnoreCase(overwrite)
                && !DavCommon.OVERWRITE_TRUE.equalsIgnoreCase(overwrite)) {
            throw PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    org.apache.http.HttpHeaders.OVERWRITE, overwrite);
        }

        //Destination header
        if (destination == null || destination.length() <= 0) {
            throw PersoniumCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST
                    .params(org.apache.http.HttpHeaders.DESTINATION);
        }

        if (this.getUrl().equals(destination)) {
            //If the source and the destination are the same, an error is assumed
            throw PersoniumCoreException.Dav.DESTINATION_EQUALS_SOURCE_URL.params(destination);
        }

        PersoniumUrl destUrl = PersoniumUrl.create(this.destination);
        PersoniumUrl currentUrl = PersoniumUrl.create(this.getUrl());
        
        if (!destUrl.isOnSameBox(currentUrl)) {
            //If the schema and the host are different from the source and the destination, an error is assumed
            throw PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    org.apache.http.HttpHeaders.DESTINATION, destination);
        }
    }

    /**
     * Get the header of the specified key from the header information <br />
     * If it does not exist, return null.
     * @param headers header information
     * @param key The key of the header to be acquired
     * @return Header of the specified key
     */
    private String getFirstHeader(HttpHeaders headers, String key) {
        return this.getFirstHeader(headers, key, null);
    }

    /**
     * Get the header of the specified key from the header information <br />
     * If it does not exist, return null.
     * @param headers header information
     * @param key The key of the header to be acquired
     * @param defaultValue Default value if header does not exist
     * @return Header of the specified key
     */
    private String getFirstHeader(HttpHeaders headers, String key, String defaultValue) {
        List<String> header = headers.getRequestHeader(key);
        if (header != null && header.size() > 0) {
            return header.get(0);
        }
        return defaultValue;
    }

}
