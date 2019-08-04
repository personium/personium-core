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

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.BoxUrlRsCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.UriUtils;

/**
 * JOX-RS Resource for obtaining Box URL.
 */
public class BoxUrlResource {

    /** Box url json key name. */
    private static final String BOX_URL_KEY_NAME = "Url";

    private AccessContext accessContext = null;
    private CellRsCmp cellRsCmp;

    /**
     * constructor.
     * @param cellRsCmp DavRsCmp
     */
    public BoxUrlResource(final CellRsCmp cellRsCmp) {
        this.cellRsCmp = cellRsCmp;
        this.accessContext = this.cellRsCmp.getAccessContext();
    }

    /**
     * Box URL acquisition end point.
     * @param querySchema Schema URL of the Box to be acquired
     * @return BoxUrlResource object
     */
    @SuppressWarnings("unchecked")
    @GET
    public final Response boxUrl(@QueryParam("schema") final String querySchema) {

        String schema = querySchema;
        if (schema == null) {
            //If the schema parameter does not exist, the schema information is acquired from the authentication token
            schema = this.accessContext.getSchema();

            //If the schema of the token is ConfidentialClient, remove the # c and get the box
            if (schema != null && schema.endsWith(OAuth2Helper.Key.CONFIDENTIAL_MARKER)) {
                schema = schema.replaceAll(OAuth2Helper.Key.CONFIDENTIAL_MARKER, "");
            }
        } else {
            //If there is a query specification, check schema
            if (!ODataUtils.isValidSchemaUri(querySchema)) {
                throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("schema", querySchema);
            }
        }

        Box box = null;
        if (schema == null || schema.length() == 0) {
            box = this.cellRsCmp.getBox();
        } else {
            schema = UriUtils.resolveLocalUnit(schema);
            //Acquire Box from schema information
            box = this.cellRsCmp.getCell().getBoxForSchema(schema);
        }

        //If there is no Box also return authorization error
        if (box == null) {
            //Check if basic authentication is permitted
            this.accessContext.updateBasicAuthenticationStateForResource(null);
            if (AccessContext.TYPE_INVALID.equals(accessContext.getType())) {
                accessContext.throwInvalidTokenException(this.cellRsCmp.getAcceptableAuthScheme());
            }
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }

        // TODO Should the order of box acquisition and check processing be reversed?
        // Only when it is necessary to acquire ACL of box, obtain box and check.
        //Validity check of the authentication token (such as tokens that have expired)
        DavCmp davCmp = ModelFactory.boxCmp(box);
        DavRsCmp boxUrlRsCmp = new BoxUrlRsCmp(this.cellRsCmp, davCmp, this.accessContext, box);
        boxUrlRsCmp.checkAccessContext(this.accessContext, BoxPrivilege.READ);

        // Response body
        JSONObject responseBody = new JSONObject();
        responseBody.put(BOX_URL_KEY_NAME, box.getUrl());

        //Return response
        return Response.status(HttpStatus.SC_OK)
                .header(PersoniumCoreUtils.HttpHeaders.ACCESS_CONTROLE_EXPOSE_HEADERS, HttpHeaders.LOCATION)
                .header(HttpHeaders.LOCATION, box.getUrl())
                .entity(responseBody.toJSONString())
                .build();
    }

}
