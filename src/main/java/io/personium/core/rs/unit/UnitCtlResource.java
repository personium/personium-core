/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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
package io.personium.core.rs.unit;

import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALUNIT;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.NotImplementedException;
import org.odata4j.core.OEntityKey;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.common.file.FileDataAccessException;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.eventlog.EventUtils;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellSnapshotCellCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.UnitUserLockManager;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.ODataResource;
import io.personium.core.utils.UriUtils;

/**
 * Jax-RS Resource handling Personium Unit Level Api.
 */
public class UnitCtlResource extends ODataResource {

    static Logger log = LoggerFactory.getLogger(UnitCtlResource.class);

    /**
     * Cache to use the search result of Cell at beforeDelete with afterDelete.
     */
    Cell cell;

    /**
     * constructor.
     * @param accessContext AccessContext
     */
    public UnitCtlResource(AccessContext accessContext) {
        super(accessContext, SCHEME_LOCALUNIT + ":/__ctl/",
                ModelFactory.ODataCtl.unitCtl(accessContext));
        checkReferenceMode(accessContext);
    }

    private void checkReferenceMode(AccessContext accessContext) {
        String unitUserName = accessContext.getSubject();
        String unitPrefix = PersoniumUnitConfig.getEsUnitPrefix();
        if (unitUserName == null) {
            unitUserName = "anon";
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(unitUserName);
        }
        if (UnitUserLockManager.hasLockObject(unitPrefix + "_" + unitUserName)) {
            throw PersoniumCoreException.Server.SERVICE_MENTENANCE_RESTORE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAccessContext(Privilege privilege) {
        AccessContext ac = this.getAccessContext();
        // Accept if UnitMaster, UnitAdmin, UnitUser, UnitLocal.
        if (AccessContext.TYPE_UNIT_MASTER.equals(ac.getType())
                || AccessContext.TYPE_UNIT_ADMIN.equals(ac.getType())
                || AccessContext.TYPE_UNIT_USER.equals(ac.getType())
                || AccessContext.TYPE_UNIT_LOCAL.equals(ac.getType())) {
            return;
        } else if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
            ac.throwInvalidTokenException(getAcceptableAuthScheme());
        } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(), getAcceptableAuthScheme());
        } else if (AccessContext.TYPE_PASSWORD_CHANGE.equals(ac.getType())) {
            throw PersoniumCoreAuthzException.ACCESS_WITH_PASSWORD_CHANGE_ACCESS_TOKEN.realm(
                    ac.getRealm(), getAcceptableAuthScheme());
        }
        throw PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;
    }

    /**
     * Obtain Auth Scheme that can be used for authentication.
     * Autret Scheme that can be used for @return authentication
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return AcceptableAuthScheme.BEARER;
    }

    @Override
    public boolean hasPrivilege(Privilege privilege) {
        return false;
    }

    @Override
    public void checkSchemaAuth(AccessContext ac) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeCreate(OEntityWrapper oEntityWrapper) {
        if (AccessContext.TYPE_UNIT_USER.equals(this.getAccessContext().getType())
                || AccessContext.TYPE_UNIT_LOCAL.equals(this.getAccessContext().getType())) {
            // If there is a Subject value in UnitUserToken, set that value to Owner.
            String subject = this.getAccessContext().getSubject();
            if (subject != null) {
                String owner = UriUtils.convertSchemeFromHttpToLocalUnit(subject);
                oEntityWrapper.put("Owner", owner);
            }
        }
    }

    /**
     * Check processing required when updating.
     * @param oEntityWrapper OEntityWrapper
     * @param oEntityKey The entityKey to be updated
     */
    @Override
    public void beforeUpdate(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        String entitySetName = oEntityWrapper.getEntitySet().getName();
        EntityResponse er = this.getODataProducer()
                .getEntity(entitySetName, oEntityKey, new EntityQueryInfo.Builder().build());
        OEntityWrapper oew = (OEntityWrapper) er.getEntity();
        //Determining accessibility for each entity
        this.checkAccessContextPerEntity(this.getAccessContext(), oew);
    }

    @Override
    public void beforeDelete(final String entitySetName, final OEntityKey oEntityKey) {
        EntityResponse er = this.getODataProducer()
                .getEntity(entitySetName, oEntityKey, new EntityQueryInfo.Builder().build());
        OEntityWrapper oew = (OEntityWrapper) er.getEntity();

        //Determining accessibility for each entity
        this.checkAccessContextPerEntity(this.getAccessContext(), oew);
        if (Cell.EDM_TYPE_NAME.equals(entitySetName)) {
            String cellId = oew.getUuid();
            cell = ModelFactory.cellFromId(cellId);
            //409 error if Cell is not empty
            if (!cell.isEmpty()) {
                throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
            }
        }
    }

    @Override
    public void afterDelete(final String entitySetName, final OEntityKey oEntityKey) {
        if (Cell.EDM_TYPE_NAME.equals(entitySetName)) {
            //Delete event log if it exists under Cell
            String owner = cell.getOwnerNormalized();
            try {
                EventUtils.deleteEventLog(this.cell.getId(), owner);
            } catch (FileDataAccessException e) {
                log.warn("Failed to delete eventlog. CellName=[" + this.cell.getName() + "] owner=[" + owner + "] "
                        + e.getMessage());
            }

            // delete CellSnapshot
            CellSnapshotCellCmp snapshotCmp = ModelFactory.cellSnapshotCellCmp(cell);
            snapshotCmp.delete(null, false);

            // delete Main Box DavCmp
            Box box = new Box(this.cell, null);
            BoxCmp boxCmp = ModelFactory.boxCmp(box);
            boxCmp.delete(null, false);

            // delete cell DavCmp
            CellCmp cellCmp = ModelFactory.cellCmp(this.cell);
            cellCmp.delete(null, false);
        }
    }

    /**
     * Corresponds to the service metadata request.
     * @return JAX-RS response object
     */
    @GET
    @Path("{first: \\$}metadata")
    public Response getMetadata() {
        return super.doGetMetadata();
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    @Path("{first: \\$}metadata")
    public Response optionsMetadata() {
        return super.doGetOptionsMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAccessContextPerEntity(AccessContext ac, OEntityWrapper oew) {
        Map<String, Object> meta = oew.getMetadata();
        String owner = UriUtils.convertSchemeFromLocalUnitToHttp((String) meta.get("Owner"));

        // In case of master token, no check is required.
        if (AccessContext.TYPE_UNIT_MASTER.equals(ac.getType())
                || AccessContext.TYPE_UNIT_ADMIN.equals(ac.getType())) {
            return;
        }

        // UnitUserToken, UnitLocalUnitUserToken Operation is allowed only for the same cell owner.
        // If owner is empty only allow master token.
        if (owner == null || !owner.equals(ac.getSubject())) {
            throw PersoniumCoreException.Auth.NOT_YOURS;
        }
    }

    @Override
    public Privilege getNecessaryReadPrivilege(String entitySetNameStr) {
        //Returns null because it is a specification that can not set Privilege for Unit level. There is no way to call this function in the first place.
        return null;
    }

    @Override
    public Privilege getNecessaryWritePrivilege(String entitySetNameStr) {
        //Returns null because it is a specification that can not set Privilege for Unit level. There is no way to call this function in the first place.
        return null;
    }

    @Override
    public Privilege getNecessaryOptionsPrivilege() {
        //Returns null because it is a specification that can not set Privilege for Unit level. There is no way to call this function in the first place.
        return null;
    }

    @Override
    public void setBasicAuthenticateEnableInBatchRequest(AccessContext ac) {
        //The Unit level API does not support batch requests, so we do not do anything here
    }

    /**
     * Not Implemented. <br />
     * Currently unimplemented because it is only necessary for $ batch access control <br />
     * Returns whether the access context has permission to $ batch.
     * @param ac access context
     * @return true: The access context has permission to $ batch
     */
    @Override
    public boolean hasPrivilegeForBatch(AccessContext ac) {
        throw new NotImplementedException();
    }

}
