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
package com.fujitsu.dc.core.rs.cell;

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

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.auth.CellPrivilege;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.core.model.DavRsCmp;
import com.fujitsu.dc.core.model.ModelFactory;

/**
 * ロールエンドポイント用JAX-RS Resource.
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
     * Roleリソースのルート.
     * Boxの一覧を返す。
     * @param authzHeader Authorization ヘッダ
     * @return JAX-RS Response Object
     */
    @Path("")
    @GET
    public final Response list(
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.AUTH_READ);
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
     * Box単位のRoleリソースのルート.
     * Boxに紐付いたロール一覧を返す。
     * Box名として__を指定されたときは、Cellレベルのロールとみなす。
     * @param boxName boxName
     * @param authzHeader authzHeader
     * @return JAXRS Response
     */
    @Path("{box}")
    @GET
    public final Response cellRole(
            @PathParam("box") String boxName,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.AUTH_READ);
        // BoxパスがCell Levelであれば、Cell レベルロールを検索して一覧で返す。
        if (BOX_PATH_CELL_LEVEL.equals(boxName)) {
            // TODO Bodyの生成
//            EntitiesResponse er = this.op.getEntities(Role.EDM_TYPE_NAME, null);
            return Response.ok().entity(boxName).build();
        }
        try {
//            EntityResponse boxEr = op.getEntity(Box.EDM_TYPE_NAME, OEntityKey.create(boxName), null);
//            EntitiesResponse rolesEr = (EntitiesResponse) op.getNavProperty(Role.EDM_TYPE_NAME,
//                    OEntityKey.create(boxName),
//                    "_role",  null);
            // TODO Bodyの生成
            return Response.ok().entity(boxName).build();
        } catch (DcCoreException dce) {
            if (DcCoreException.OData.NO_SUCH_ENTITY == dce) {
                throw DcCoreException.Dav.BOX_NOT_FOUND;
            }
            throw dce;
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
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.AUTH_READ);
        // BoxパスがCell Levelであれば、Cell レベルロールという扱い。
        if (BOX_PATH_CELL_LEVEL.equals(boxName)) {
            // TODO Bodyの生成
//            EntitiesResponse er = this.op.getEntities(Role.EDM_TYPE_NAME, null);
            return Response.ok().entity(boxName).build();
        }
        return Response.ok().entity(boxName + role).build();
    }

}
