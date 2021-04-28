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
package io.personium.core.rs.cell;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.odata4j.core.OEntity;
import org.odata4j.core.OProperty;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.ODataProducer;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;

/**
 * JAX-RS Resource for roll end point.
 */
public class RoleResource {

    private final ODataProducer op;
    static final String BOX_PATH_CELL_LEVEL = "__";
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param cell Cell
     * @param davRsCmp DavRsCmp
     */
    public RoleResource(final Cell cell, final DavRsCmp davRsCmp) {
        this.op = ModelFactory.ODataCtl.cellCtl(cell);
        this.davRsCmp = davRsCmp;
    }

    /**
     * Role Root of the resource.
     * Returns a list of Box.
     * @param authzHeader Authorization header
     * @return JAX-RS Response Object
     */
//    @Path("")
    @GET
    public final Response list(
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader) {
        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.AUTH_READ);
        EntitiesResponse er = op.getEntities(Box.EDM_TYPE_NAME, null);
        List<OEntity> loe = er.getEntities();
        List<String> sl = new ArrayList<String>();
        sl.add(BOX_PATH_CELL_LEVEL);
        for (OEntity oe : loe) {
            OProperty<String> nameP = oe.getProperty("Name", String.class);
            sl.add(nameP.getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (String s : sl) {
            sb.append(s + "<br/>");
        }
        return Response.ok().entity(sb.toString()).build();
    }
    /**
     * Route of Role resource in Box units.
     * Returns the role list associated with Box.
     * When __ is specified as a Box name, it is regarded as a cell level role.
     * @param boxName boxName
     * @param authzHeader authzHeader
     * @return JAXRS Response
     */
    @Path("{box}")
    @GET
    public final Response cellRole(
            @PathParam("box") String boxName,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader) {
        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.AUTH_READ);
        //If the Box path is Cell Level, search the Cell level role and return it as a list.
        if (BOX_PATH_CELL_LEVEL.equals(boxName)) {
            //Generation of TODO Body
//            EntitiesResponse er = this.op.getEntities(Role.EDM_TYPE_NAME, null);
            return Response.ok().entity(boxName).build();
        }
        try {
//            EntityResponse boxEr = op.getEntity(Box.EDM_TYPE_NAME, OEntityKey.create(boxName), null);
//            EntitiesResponse rolesEr = (EntitiesResponse) op.getNavProperty(Role.EDM_TYPE_NAME,
//                    OEntityKey.create(boxName),
//                    "_role",  null);
            //Generation of TODO Body
            return Response.ok().entity(boxName).build();
        } catch (PersoniumCoreException pce) {
            if (PersoniumCoreException.OData.NO_SUCH_ENTITY == pce) {
                throw PersoniumCoreException.Dav.BOX_NOT_FOUND;
            }
            throw pce;
        }
    }
    /**
     * @param boxName boxName
     * @param role roleName
     * @param authzHeader authzHeader
     * @return JAXRS Response
     */
    @Path("{box}/{role}")
    @GET
    public final Response boxRole(
            @PathParam("box") String boxName,
            @PathParam("role") String role,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader) {
        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.AUTH_READ);
        //If the Box pass is Cell Level, it is handled as Cell Level Roll.
        if (BOX_PATH_CELL_LEVEL.equals(boxName)) {
            //Generation of TODO Body
//            EntitiesResponse er = this.op.getEntities(Role.EDM_TYPE_NAME, null);
            return Response.ok().entity(boxName).build();
        }
        return Response.ok().entity(boxName + role).build();
    }

}
