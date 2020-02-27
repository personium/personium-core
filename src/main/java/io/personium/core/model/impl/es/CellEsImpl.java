/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 *  - FUJITSU LIMITED
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
package io.personium.core.model.impl.es;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.IExtRoleContainingToken;
import io.personium.common.auth.token.Role;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchHits;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.common.es.util.IndexNameEncoder;
import io.personium.common.file.FileDataAccessException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.eventlog.EventUtils;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellSnapshotCellCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.impl.es.accessor.CellAccessor;
import io.personium.core.model.impl.es.accessor.CellDataAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.CellCache;
import io.personium.core.model.impl.es.doc.CellDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.fs.DavCmpFsImpl;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.utils.UriUtils;
import net.spy.memcached.internal.CheckedOperationTimeoutException;

/**
 * Cell object implemented using ElasticSearch.
 */
public class CellEsImpl extends Cell {
    /** logger. */
    static Logger log = LoggerFactory.getLogger(CellEsImpl.class);

    /** Es search result output upper limit. */
    private static final int TOP_NUM = PersoniumUnitConfig.getEsTopNum();

    private Map<String, Object> json;

    /**
     * constructor.
     */
    public CellEsImpl() {
    }

    /**
     * Load cell info from id..
     * @param id cell id
     * @return CellObject. If Cell does not exist, it returns null.
     */
    public static Cell loadFromId(String id) {
        log.debug(id);
        EntitySetAccessor esCells = EsModel.cell();
        PersoniumGetResponse resp = esCells.get(id);
        if (resp.exists()) {
            CellEsImpl cell = new CellEsImpl();
            cell.setJson(resp.getSource());
            cell.id = resp.getId();
            return cell;
        } else {
            return null;
        }
    }

    /**
     * Load cell from the specified cell name.
     * However, the parameter "url" of Cell is not set.
     * @param cellName target cell name
     * @return cell
     */
    public static Cell loadFromName(String cellName) {
        CellEsImpl cell = (CellEsImpl) findCell("s.Name.untouched", cellName);
        return cell;
    }

    /**
     * Search for ID or Cell name Cell and return Cell object.
     * @param queryKey
     * Key (Cell name) for searching for Cell
     * @param queryValue
     * Value for key when searching Cell
     * @return Cell object If the corresponding Cell does not exist, or null if the value of queryKey is invalid
     */
    private static Cell findCell(String queryKey, String queryValue) {
        if (!queryKey.equals("_id") && !queryKey.equals("s.Name.untouched")) {
            return null;
        }
        //Check the format of the Cell name specified in URl. In case of invalid, return null because Cell does not exist
        if (!validatePropertyRegEx(queryValue, Common.PATTERN_NAME)) {
            return null;
        }

        EntitySetAccessor ecCells = EsModel.cell();
        CellEsImpl ret = new CellEsImpl();

        Map<String, Object> cache = CellCache.get(queryValue);
        if (cache == null) {
            Map<String, Object> source = new HashMap<String, Object>();
            Map<String, Object> filter = new HashMap<String, Object>();
            Map<String, Object> term = new HashMap<String, Object>();

            term.put(queryKey, queryValue);
            filter.put("term", term);
            source.put("query", QueryMapFactory.filteredQuery(null, filter));

            PersoniumSearchResponse resp = ecCells.search(source);
            if (resp == null || resp.getHits().getCount() == 0) {
                return null;
            }
            PersoniumSearchHit hit = resp.getHits().getAt(0);
            ret.setJson(hit.getSource());
            ret.id = hit.getId();

            cache = hit.getSource();
            cache.put("_id", hit.getId());
            try {
                CellCache.cache(queryValue, cache);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof CheckedOperationTimeoutException) {
                    //If timeout occurs due to connection to memcached, output only the log and continue
                    log.info("Faild to cache Cell info.");
                } else {
                    //In case of other errors, it is regarded as a server error
                    throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
                }
            }
        } else {
            ret.setJson(cache);
            ret.id = (String) cache.get("_id");
        }

        return ret;
    }

    @Override
    public String getDataBundleNameWithOutPrefix() {
        String unitUserName;
        if (this.owner == null) {
            unitUserName = AccessContext.TYPE_ANONYMOUS;
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }
        return unitUserName;
    }

    @Override
    public String getDataBundleName() {
        String unitUserName = PersoniumUnitConfig.getEsUnitPrefix() + "_" + getDataBundleNameWithOutPrefix();
        return unitUserName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void makeEmpty() {
        String unitUserNameWithOutPrefix = this.getDataBundleNameWithOutPrefix();
        String cellInfoLog = String.format(" CellId:[%s], CellName:[%s], CellUnitUserName:[%s]", this.getId(),
                this.getName(), this.getDataBundleName());
        //--------------------
        // WebDav file.
        //--------------------
        // Delete cell snapshot file.
        CellSnapshotCellCmp snapshotCmp = ModelFactory.cellSnapshotCellCmp(this);
        try {
            snapshotCmp.delete(null, true);
        } catch (PersoniumCoreException e) {
            // If the deletion fails, output a log and continue processing.
            log.warn("Delete CellSnapshot Failed." + cellInfoLog, e);
        }
        log.info("CellSnapshotFile Deletion End.");

        // Delete event log file.
        try {
            EventUtils.deleteEventLog(this.getId(), this.getOwnerNormalized());
        } catch (FileDataAccessException e) {
            // If the deletion fails, output a log and continue processing.
            log.warn("Delete EventLog Failed." + cellInfoLog, e);
        }
        log.info("EventLog Deletion End.");

        // Delete dav file.
        CellCmp cellCmp = ModelFactory.cellCmp(this);
        try {
            cellCmp.delete(null, true);
        } catch (PersoniumCoreException e) {
            // If the deletion fails, output a log and continue processing.
            log.warn("Delete DavFile Failed." + cellInfoLog, e);
        }
        log.info("DavFile Deletion End.");

        //--------------------
        // OData.
        //--------------------
        // Delete all entities under the cell.
        CellDataAccessor cellDataAccessor = EsModel.cellData(unitUserNameWithOutPrefix, this.getId());
        cellDataAccessor.bulkDeleteCell();
        log.info("Cell Entity Resource Deletion End.");
    }

    @Override
    public void delete(boolean recursive, String unitUserName) {
        //Check the number of accesses to Cell and lock access
        int maxLoopCount = PersoniumUnitConfig.getCellLockRetryTimes();
        long interval = PersoniumUnitConfig.getCellLockRetryInterval();
        waitCellAccessible(this.id, maxLoopCount, interval);

        CellLockManager.setCellStatus(this.id, CellLockManager.STATUS.BULK_DELETION);

        // Delete cell entity.
        CellAccessor cellAccessor = (CellAccessor) EsModel.cell();
        CellDocHandler docHandler = new CellDocHandler(cellAccessor.get(this.getId()));
        try {
            cellAccessor.delete(docHandler);
            log.info("Cell Entity Deletion End.");
        } finally {
            CellCache.clear(this.getName());
            CellLockManager.setCellStatus(this.getId(), CellLockManager.STATUS.NORMAL);
        }

        // Make this cell empty asynchronously.
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                makeEmpty();
            }
        });
        thread.start();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getRoleListForAccount(final String username) {
        //Acquire Account
        EntitySetAccessor accountType = EsModel.cellCtl(this, Account.EDM_TYPE_NAME);

        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();
        filters.add(QueryMapFactory.termQuery("s.Name.untouched", username));

        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        queries.add(QueryMapFactory.termQuery("c", this.getId()));

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

        Map<String, Object> source = new HashMap<String, Object>();
        source.put("filter", QueryMapFactory.andFilter(filters));
        source.put("query", query);

        PersoniumSearchHits hits = accountType.search(source).getHits();

        if (hits.getCount() == 0) {
            return null;
        }

        PersoniumSearchHit hit = hits.getHits()[0];

        List<Role> ret = new ArrayList<Role>();
        ODataLinkAccessor links = EsModel.cellCtlLink(this);

        //Search for roles tied to accounts
        List<Map<String, Object>> searchRoleQueries = new ArrayList<Map<String, Object>>();
        searchRoleQueries.add(QueryMapFactory.termQuery("t1", "Account"));
        searchRoleQueries.add(QueryMapFactory.termQuery("t2", "Role"));

        List<Map<String, Object>> searchRoleFilters = new ArrayList<Map<String, Object>>();
        searchRoleFilters.add(QueryMapFactory.termQuery("k1", hit.getId()));
        Map<String, Object> and = new HashMap<String, Object>();
        and.put("filters", searchRoleFilters);
        Map<String, Object> searchRoleFilter = new HashMap<String, Object>();
        searchRoleFilter.put("and", and);

        Map<String, Object> searchRoleSource = new HashMap<String, Object>();
        searchRoleSource.put("filter", searchRoleFilter);
        searchRoleSource.put("query",
                QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(searchRoleQueries)));

        //Search result count setting
        searchRoleSource.put("size", TOP_NUM);

        PersoniumSearchResponse res = links.search(searchRoleSource);
        if (res == null) {
            return ret;
        }
        PersoniumSearchHit[] hits2 = res.getHits().getHits();
        for (PersoniumSearchHit hit2 : hits2) {
            Map<String, Object> row = hit2.getSource();
            String role = (String) row.get("k2");
            log.debug(this.id);
            EntitySetAccessor roleDao = EsModel.cellCtl(this, Role.EDM_TYPE_NAME);
            PersoniumGetResponse gRes = roleDao.get(role);
            if (gRes == null) {
                continue;
            }
            Map<String, Object> src = gRes.getSource();
            Map<String, Object> s = (Map<String, Object>) src.get("s");
            Map<String, Object> l = (Map<String, Object>) src.get("l");
            String roleName = (String) s.get(Common.P_NAME.getName());
            String boxId = (String) l.get(Box.EDM_TYPE_NAME);
            String boxName = null;
            String schema = null;
            if (boxId != null) {
                //Search Box
                EntitySetAccessor box = EsModel.box(this);
                PersoniumGetResponse getRes = box.get(boxId);
                if (getRes == null || !getRes.isExists()) {
                    continue;
                }
                Map<String, Object> boxsrc = getRes.getSource();
                Map<String, Object> boxs = (Map<String, Object>) boxsrc.get("s");
                boxName = (String) boxs.get(Common.P_NAME.getName());
                schema = (String) boxs.get(Box.P_SCHEMA.getName());
            }
            Role roleObj = new Role(roleName, boxName, schema, this.getUrl());

            ret.add(roleObj);
        }
        return ret;
    }

    @Override
    public List<Role> getRoleListHere(final IExtRoleContainingToken token) {
        List<Role> ret = new ArrayList<Role>();

        //List the Role to be paid out from the association setting of ExtCell and Role
        this.addRoleListExtCelltoRole(token, ret);

        //List the Role to be paid out from the association of ExtCell, Relation and Role
        //When
        //List Role to be paid out from the association between ExtCell, Relation, ExtRole, and Role
        this.addRoleListExtCelltoRelationAndExtRole(token, ret);

        return ret;
    }

    @Override
    public String roleIdToRoleResourceUrl(String roleId) {
        CellCtlODataProducer ccop = new CellCtlODataProducer(this);
        OEntity oe = ccop.getEntityByInternalId(Role.EDM_TYPE_NAME, roleId);
        if (oe == null) {
            //If the role does not exist, it returns null.
            return null;
        }

        String boxName = (String) oe.getProperty("_Box.Name").getValue();
        OProperty<?> schemaProp = oe.getProperty("_Box.Schema");
        String schema = null;
        if (schemaProp != null) {
            schema = (String) schemaProp.getValue();
        }
        String roleName = (String) oe.getProperty("Name").getValue();
        Role roleObj = new Role(roleName, boxName, schema, this.getUrl());
        return roleObj.toRoleInstanceURL();
    }

    @Override
    public String roleResourceUrlToId(String roleUrl, String baseUrl) {
        EntitySetAccessor roleType = EsModel.cellCtl(this, Role.EDM_TYPE_NAME);

        //The roleName corresponds to the URL
        URL rUrl = null;
        try {
            //Correspondence of xml: base
            if (baseUrl != null && !"".equals(baseUrl)) {
                //URL relative path correspondence
                rUrl = new URL(new URL(baseUrl), roleUrl);
            } else {
                rUrl = new URL(roleUrl);
            }
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND.reason(e);
        }

        Role role = null;
        try {
            role = Role.createFromRoleInstanceUrl(rUrl.toExternalForm());
        } catch (MalformedURLException e) {
            log.info("Role URL:" + rUrl.toString());
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND;
        }

        //It is not permitted to designate the cell URL portion of the role resource different from the cell URL of the ACL setting target
        if (!UriUtils.equalIgnoringPort(this.getUrl(), role.getBaseUrl())) {
            PersoniumCoreLog.Dav.ROLE_NOT_FOUND.params("Cell different").writeLog();
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND;
        }
        //Search for Role
        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        queries.add(QueryMapFactory.termQuery("c", this.getId()));
        queries.add(QueryMapFactory.termQuery("s." + Common.P_NAME.getName() + ".untouched", role.getName()));

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();
        if (!(Box.MAIN_BOX_NAME.equals(role.getBoxName()))) {
            //Add search queries when Role is tied to a box
            Box targetBox = this.getBoxForName(role.getBoxName());
            if (targetBox == null) {
                throw PersoniumCoreException.Dav.BOX_LINKED_BY_ROLE_NOT_FOUND.params(baseUrl);
            }
            String boxId = targetBox.getId();
            filters.add(QueryMapFactory.termQuery("l." + Box.EDM_TYPE_NAME, boxId));
        } else {
            //Addition of null search query even when Role is not tied to a box
            filters.add(QueryMapFactory.missingFilter("l." + Box.EDM_TYPE_NAME));
        }

        Map<String, Object> source = new HashMap<String, Object>();
        if (!filters.isEmpty()) {
            source.put("filter", QueryMapFactory.andFilter(filters));
        }
        source.put("query", query);
        PersoniumSearchHits hits = roleType.search(source).getHits();

        //Null if target Role does not exist
        if (hits == null || hits.getCount() == 0) {
            PersoniumCoreLog.Dav.ROLE_NOT_FOUND.params("Not Hit").writeLog();
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND;
        }
        //If more than one target Role is acquired, set as internal error
        if (hits.getAllPages() > 1) {
            PersoniumCoreLog.OData.FOUND_MULTIPLE_RECORDS.params(hits.getAllPages()).writeLog();
            throw PersoniumCoreException.OData.DETECTED_INTERNAL_DATA_CONFLICT;
        }

        PersoniumSearchHit hit = hits.getHits()[0];
        return hit.getId();
    }

    /**
     * Set the internal ID of this Cell.
     * @param id
     * Internal ID string
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set members of objects from Map.
     * @param json
     * Actually Map
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setJson(Map json) {
        this.json = json;
        if (this.json == null) {
            return;
        }
        Map<String, String> urlJson = (Map<String, String>) json.get("s");
        Map<String, String> hJson = (Map<String, String>) json.get("h");
        this.published = (Long) json.get("p");
        this.name = urlJson.get("Name");
        // TODO At this timing owner's localunit/http convert should be done.
        // It is necessary to modify the source code so that UnitUrl can be read at this timing.
        this.owner = hJson.get("Owner");
    }

    private void waitCellAccessible(String cellId, int maxLoopCount, long interval) {
        for (int loopCount = 0; loopCount < maxLoopCount; loopCount++) {
            long count = CellLockManager.getReferenceCount(cellId);
            //Since it includes my request, it becomes larger than 1 if there are other requests
            if (count <= 1) {
                return;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                throw PersoniumCoreException.Misc.CONFLICT_CELLACCESS;
            }
        }
        throw PersoniumCoreException.Misc.CONFLICT_CELLACCESS;
    }

    /**
     * Match ExtCell and Role and decide which Role to pay out.
     * @param token
     * Transcell access token
     * @param roles
     * List of roles to be withdrawn. Add here (destructive method)
     */
    private void addRoleListExtCelltoRole(final IExtRoleContainingToken token, List<Role> roles) {
        //Acquisition of Role corresponding to ExtCell-Role binding
        String extCell = token.getIssuer();
        String principal = token.getSubject();
        String principalCell;
        if (principal.contains("#")) {
            principalCell = token.getSubject().substring(0, principal.indexOf("#"));
        } else {
            principalCell = token.getSubject();
        }

        //If the access subject is different from ExtCell (two or more levels of transcell token authentication), do not allow.
        if (extCell.equals(principalCell)) {
            ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
            EntitiesResponse response = null;
            //Number of search result output setting
            QueryInfo qi = QueryInfo.newBuilder().setTop(TOP_NUM).setInlineCount(InlineCount.NONE).build();

            List<String> list = UriUtils.getUrlVariations(extCell);
            for (int i = 0; i < list.size(); i++) {
                String extCellUrl = list.get(i);
                try {
                    //Acquire link information of ExtCell-Role
                    response = (EntitiesResponse) op.getNavProperty(ExtCell.EDM_TYPE_NAME,
                            OEntityKey.create(extCellUrl),
                            "_" + Role.EDM_TYPE_NAME, qi);
                } catch (PersoniumCoreException dce) {
                    if (!PersoniumCoreException.OData.NO_SUCH_ENTITY.getCode().equals(dce.getCode())) {
                        throw dce;
                    }
                    // Continue processing with log output only.
                    log.debug("no such entity.");
                }
                if (response != null) {
                    break;
                }
            }
            if (response == null) {
                return;
            }

            //Look at all link information of ExtCell-Role and wash out the roll for cell which has accessed this time.
            List<OEntity> entList = response.getEntities();
            for (OEntity ent : entList) {
                OEntityWrapper entRole = (OEntityWrapper) ent;
                this.addRole(entRole.getUuid(), roles);
            }
        }
    }

    /**
     * List the Role to be paid out from the association between ExtCell, Relation and Role.
     * List the Role to be paid out from the association between ExtCell, Relation, ExtRole, and Role.
     * @param token
     * Transcell access token
     * @param roles
     * List of roles to be withdrawn. Add here (destructive method)
     */
    @SuppressWarnings("unchecked")
    private void addRoleListExtCelltoRelationAndExtRole(final IExtRoleContainingToken token, List<Role> roles) {
        String extCell = token.getExtCellUrl();

        //Acquisition of Role corresponding to ExtCell-Role binding
        ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
        EntitiesResponse response = null;
        //Number of search result output setting
        QueryInfo qi = QueryInfo.newBuilder().setTop(TOP_NUM).setInlineCount(InlineCount.NONE).build();
        List<String> list = UriUtils.getUrlVariations(extCell);
        for (int i = 0; i < list.size(); i++) {
            try {
                String extCellUrl = list.get(i);
                //Acquire link information of ExtCell-Relation
                response = (EntitiesResponse) op.getNavProperty(ExtCell.EDM_TYPE_NAME,
                        OEntityKey.create(extCellUrl),
                        "_" + Relation.EDM_TYPE_NAME, qi);
            } catch (PersoniumCoreException dce) {
                if (!PersoniumCoreException.OData.NO_SUCH_ENTITY.getCode().equals(dce.getCode())) {
                    throw dce;
                }
                // Continue processing with log output only.
                log.debug("no such entity.");
            }
            if (response != null) {
                break;
            }
        }
        if (response == null) {
            return;
        }

        List<OEntity> entList = response.getEntities();
        for (OEntity ent : entList) {
            OEntityWrapper entRelation = (OEntityWrapper) ent;

            //Look at all the link information of ExtCell-Relation and wash out the roll for cell accessed this time.
            PersoniumSearchResponse res = serchRoleLinks(Relation.EDM_TYPE_NAME, entRelation.getUuid());
            if (res == null) {
                continue;
            }
            this.addRoles(res.getHits().getHits(), roles);
            //↑ List the Role to be paid out from the association between ExtCell, Relation and Role so far.
            //↓ This is the process of listing Role to be paid out from the association between ExtCell, Relation, ExtRole, and Role.

            //Retrieve ExtRole information from Relation.
            EntitySetAccessor extRoleType = EsModel.cellCtl(this, ExtRole.EDM_TYPE_NAME);

            //Search for ExtRole linked to Relation
            //Acquire a list after acquiring the number of current registrations
            Map<String, Object> source = new HashMap<String, Object>();

            //Specify an implicit filter and set the search target to the beginning of the search condition (narrow down)
            List<Map<String, Object>> implicitFilters = QueryMapFactory.getImplicitFilters(this.id, null, null, null,
                    extRoleType.getType());
            String linksKey = OEntityDocHandler.KEY_LINK + "." + Relation.EDM_TYPE_NAME;
            implicitFilters.add(0, QueryMapFactory.termQuery(linksKey, entRelation.getUuid()));
            Map<String, Object> query = QueryMapFactory.mustQuery(implicitFilters);
            Map<String, Object> filteredQuery = QueryMapFactory.filteredQuery(null, query);
            source.put("query", filteredQuery);
            long hitNum = extRoleType.count(source);
            //Skip this if ExtCell settings do not exist
            if (hitNum == 0) {
                continue;
            }
            source.put("size", hitNum);

            PersoniumSearchHits extRoleHits = extRoleType.search(source).getHits();
            //Skip this if ExtCell settings do not exist
            //Since it may be deleted after acquiring the number, check the search result again
            if (extRoleHits.getCount() == 0) {
                continue;
            }
            for (PersoniumSearchHit extRoleHit : extRoleHits.getHits()) {
                Map<String, Object> extRoleSource = extRoleHit.getSource();
                Map<String, Object> extRoleS = (Map<String, Object>) extRoleSource.get("s");
                String esExtRole = (String) extRoleS.get(ExtRole.EDM_TYPE_NAME);

                //Match with the rolls in the token
                for (Role tokenRole : token.getRoleList()) {
                    if (!tokenRole.toRoleInstanceURL().equals(esExtRole)) {
                        continue;
                    }
                    //Look at all link information of ExtCell-Role and wash out the roll for cell which has accessed this time.
                    PersoniumSearchResponse resExtRoleToRole = serchRoleLinks(
                            ExtRole.EDM_TYPE_NAME, extRoleHit.getId());
                    if (resExtRoleToRole == null) {
                        continue;
                    }
                    this.addRoles(resExtRoleToRole.getHits().getHits(), roles);
                }
            }
        }
    }

    /**
     * Acquire corresponding data from the link table of Role and another entity set.
     * @param searchKey
     * Entity set name of search condition
     * @param searchValue
     * Search uuid
     * @return Search results
     */
    private PersoniumSearchResponse serchRoleLinks(final String searchKey, final String searchValue) {

        ODataLinkAccessor links = EsModel.cellCtlLink(this);
        //Search for roles linked to Relation
        Map<String, Object> source = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();
        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();

        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        queries.add(QueryMapFactory.termQuery("t1", searchKey));
        queries.add(QueryMapFactory.termQuery("t2", "Role"));

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

        filters.add(QueryMapFactory.termQuery("k1", searchValue));
        and.put("filters", filters);
        filter.put("and", and);
        source.put("filter", filter);
        source.put("query", query);
        //Search result count setting
        source.put("size", TOP_NUM);

        return links.search(source);
    }

    /**
     * Get the value of the role from the array of SearchHit containing Role.
     * @param hits
     * Search for Role and the result
     * @param roles
     * List of roles to be withdrawn. Add here (destructive method)
     */
    private void addRoles(PersoniumSearchHit[] hits, List<Role> roles) {
        for (PersoniumSearchHit hit : hits) {
            Map<String, Object> src = hit.getSource();
            String roleUuid = (String) src.get("k2");

            //See all of the Relation-Role link information and identify the roll for the cell that is accessed this time.
            this.addRole(roleUuid, roles);
        }
    }

    /**
     * Get the value of the role.
     * @param uuid
     * Role UUID
     * @param roles
     * List of roles to be withdrawn. Add here (destructive method)
     */
    @SuppressWarnings("unchecked")
    private void addRole(String uuid, List<Role> roles) {
        EntitySetAccessor roleDao = EsModel.cellCtl(this, Role.EDM_TYPE_NAME);
        PersoniumGetResponse gRes = roleDao.get(uuid);
        if (gRes == null) {
            return;
        }
        Map<String, Object> src = gRes.getSource();
        Map<String, Object> s = (Map<String, Object>) src.get("s");
        Map<String, Object> l = (Map<String, Object>) src.get("l");
        String roleName = (String) s.get(Common.P_NAME.getName());
        String schema = (String) s.get(Box.P_SCHEMA.getName());
        String boxId = (String) l.get(Box.EDM_TYPE_NAME);
        String boxName = null;
        if (boxId != null) {
            //Search Box
            Map<String, Object> boxsrc = DavCmpFsImpl.searchBox(this, boxId);
            Map<String, Object> boxs = (Map<String, Object>) boxsrc.get("s");
            boxName = (String) boxs.get(Common.P_NAME.getName());
        }

        roles.add(new Role(roleName, boxName, schema, this.getUrl()));
    }
}
