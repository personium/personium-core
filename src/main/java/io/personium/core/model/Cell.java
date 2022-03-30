/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.UriBuilder;

import org.core4j.Enumerable;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.OptionsQueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.IExtRoleContainingToken;
import io.personium.common.auth.token.Role;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AuthUtils;
import io.personium.core.auth.ScopeArbitrator;
import io.personium.core.event.EventBus;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.cache.BoxCache;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.utils.UriUtils;
import net.spy.memcached.internal.CheckedOperationTimeoutException;

/**
 * Model Class for Cell.
 */
public abstract class Cell {
    /** logger. */
    static Logger log = LoggerFactory.getLogger(Cell.class);


    /** Edm.Entity Type Name. */
    public static String EDM_TYPE_NAME = "Cell";

    /** Status normal. */
    public static String STATUS_NORMAL = "normal";
    /** Status import error. */
    public static String STATUS_IMPORT_ERROR = "import failed";

    /** Error file name. */
    public static String IMPORT_ERROR_FILE_NAME = "import.error";

    /** Definition field of Name property. */
    public static EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name").setType(EdmSimpleType.STRING)
            .setNullable(false).setAnnotations(Common.P_FORMAT_CELL_NAME);

    /** Property List. */
    public static List<EdmProperty.Builder> PROPS = Collections.unmodifiableList(Arrays.asList(
            new EdmProperty.Builder[] {P_NAME, Common.P_PUBLISHED, Common.P_UPDATED}
            ));
    /** Key List. */
    public static List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            new String[] {P_NAME.getName()}
            ));;

    /** EntityType Builder of the Cell. */
    public static EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder().setNamespace(Common.EDM_NS_UNIT_CTL)
            .setName(EDM_TYPE_NAME).addProperties(Enumerable.create(PROPS).toList()).addKeys(KEYS);

    protected String id;
    protected String name;
    protected String owner;
    protected Long published;

    /**
     * returns the Cell name.
     * @return Cell Name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the internal ID of this Cell.
     * @return internal identity string
     */
    public String getId() {
        return this.id;
    }


    /**
     * returns URL string for this cell.
     * Return PathBaseURL or FQDNBaseURL depending on property setting.
     * @return URL string
     */
    public String getUrl() {
        if (PersoniumUnitConfig.isPathBasedCellUrlEnabled()) {
            return getPathBaseUrl();
        } else {
            return getFqdnBaseUrl();
        }
    }

    /**
     * returns Cell base URL string for this cell.
     * Cell base url : "https://{cellname}.{domain}/".
     * @return Cell base URL string
     */
    public String getFqdnBaseUrl() {
        StringBuilder hostSb = new StringBuilder(this.name);
        hostSb.append(".").append(CommonUtils.getFQDN());
        return UriBuilder.fromPath("/")
            .scheme(PersoniumUnitConfig.getUnitScheme())
            .host(hostSb.toString())
            .port(PersoniumUnitConfig.getUnitPort())
            .build()
            .toString();
    }

    /**
     * returns Cell base URL string for this cell.
     * Cell base url : "https://{domain}/{cellname}/".
     * @return Cell base URL string
     */
    public String getPathBaseUrl() {
        StringBuilder sb = new StringBuilder(this.getUnitUrl());
        return sb.append(this.name).append("/").toString() ;
    }

    /**
     * returns Unit URL string for this cell.
     * @return Unit URL string
     */
    public String getUnitUrl() {
        return PersoniumUnitConfig.getBaseUrl();
    }
    /**
     * Returns the normalized URI of the owner Unit User of this Cell.
     * @return normalized owner url.
     */
    public String getOwnerNormalized() {
        return UriUtils.convertSchemeFromLocalUnitToHttp(this.owner);
    }

    /**
     * Returns the raw URI of the owner Unit User of this Cell.
     * @return raw owner url.
     */
    public String getOwnerRaw() {
        return this.owner;
    }

    /**
     * Returns the prefix without Unit User name of the Cell.
     * @return .
     */
    public abstract String getDataBundleNameWithOutPrefix();

    /**
     * Returns the Unit User name of the Cell.
     * @return Unit User name
     */
    public abstract String getDataBundleName();

    /**
     * Returns the EventBus of the Cell.
     * @return EventBus
     */
    public EventBus getEventBus() {
        return new EventBus(this);
    }

    /**
     * Return the creation time of Cell.
     * @return time stamp of this cell creation.
     */
    public long getPublished() {
        return this.published;
    }
    /**
     * Data and control objects under (Box, Account, etc.) if there is no return true..
     * The default box may be.
     * @return It is true if there is no data and control objects under
     * (Box, Account, etc.).
     */
    public boolean isEmpty() {
        CellCtlODataProducer producer = new CellCtlODataProducer(this);
        // check no box exists.
        QueryInfo queryInfo = new QueryInfo(InlineCount.ALLPAGES, null, null, null, null, null, null, null, null);
        if (producer.getEntitiesCount(Box.EDM_TYPE_NAME, queryInfo).getCount() > 0) {
            return false;
        }

        // check that Main Box is empty
        Box defaultBox = this.getBoxForName(Box.MAIN_BOX_NAME);
        BoxCmp defaultBoxCmp = ModelFactory.boxCmp(defaultBox);
        if (!defaultBoxCmp.isEmpty()) {
            return false;
        }

        // check that no Cell Control Object exists
        //In order to improve the TODO performance, change the type so as to check the value of c: (uuid of the cell) in the Type traversal
        if (producer.getEntitiesCount(Account.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(Role.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(ExtCell.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(ExtRole.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(Relation.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(SentMessage.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(ReceivedMessage.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(Rule.EDM_TYPE_NAME, queryInfo).getCount() > 0) {
            return false;
        }
        // TODO check EventLog
        return true;
    }

    /**
     * To delete all the data and control objects in the underlying
     * (Box, Account, etc.).
     */
    public abstract void makeEmpty();

    /**
     * delete this cell.
     * @param recursive set true if you want to delete recursively
     * @param unitUserName to use for deletion operation
     */
    public abstract void delete(boolean recursive, String unitUserName);

    /**
     * Specify the Box name to get the Box.
     * @param boxName Box name
     * @return Box
     */
    public Box getBoxForName(String boxName) {
        if (Box.MAIN_BOX_NAME.equals(boxName)) {
            return new Box(this, null);
        }

        //Check the format of the Box name specified in URl. In case of invalid Because none of Box exists, return null
        if (!validatePropertyRegEx(boxName, Common.PATTERN_NAME)) {
            return null;
        }
        //Attempt to acquire the cached Box.
        Box cachedBox = BoxCache.get(boxName, this);
        if (cachedBox != null) {
            return cachedBox;
        }

        Box loadedBox = null;
        try {
            ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
            EntityResponse er = op.getEntity(Box.EDM_TYPE_NAME, OEntityKey.create(boxName), null);
            loadedBox = new Box(this, er.getEntity());
            BoxCache.cache(loadedBox);
            return loadedBox;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CheckedOperationTimeoutException) {
                return loadedBox;
            } else {
                return null;
            }
        }
    }

    /**
     * Check the value of property item with regular expression.
     * @param propValue
     * Property value
     * @param dcFormat
     * Value of dcFormat
     * @return In case of format error, return false
     */
    protected static boolean validatePropertyRegEx(String propValue, String dcFormat) {
        //Perform format check
        Pattern pattern = Pattern.compile(dcFormat);
        Matcher matcher = pattern.matcher(propValue);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }

    /**
     * Specify the Box schema to get the Box.
     * @param boxSchema box schema uri
     * @return Box
     */
    public Box getBoxForSchema(String boxSchema) {
        //Retrieving the schema name list (including aliases)
        List<String> boxSchemas = UriUtils.getUrlVariations(boxSchema);

        ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
        for (int i = 0; i < boxSchemas.size(); i++) {
            BoolCommonExpression filter = OptionsQueryParser.parseFilter("Schema eq '" + boxSchemas.get(i) + "'");
            QueryInfo qi = QueryInfo.newBuilder().setFilter(filter).build();
            try {
                EntitiesResponse er = op.getEntities(Box.EDM_TYPE_NAME, qi);
                List<OEntity> entList = er.getEntities();
                if (entList.size() == 1) {
                    return new Box(this, entList.get(0));
                }
                continue;
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * @param clientId
     * @param grantType
     * @return
     */
    public ScopeArbitrator getScopeArbitrator(String clientId, String grantType) {
        Box box = this.getBoxForSchema(clientId);
        return new ScopeArbitrator(this, box, grantType);
    }
    /**
     * It gets the Accounts to specify the Account name.
     * @param username Account name
     * @return Account
     */
    public OEntityWrapper getAccount(final String username) {
        ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
        OEntityKey key = OEntityKey.create(username);
        OEntityWrapper oew = null;
        try {
            EntityResponse resp = op.getEntity("Account", key, null);
            oew = (OEntityWrapper) resp.getEntity();
        } catch (PersoniumCoreException dce) {
            log.debug(dce.getMessage());
        }
        return oew;
    }
    /**
     * @param oew account
     * @param password password
     * @return true if authentication is successful.
     */
    public boolean authenticateAccount(final OEntityWrapper oew, final String password) {
        return AuthUtils.isMatchePassword(oew, password);
    }
    /**
     * @param username access account id
     * @return List of Roles
     */
    public abstract List<Role> getRoleListForAccount(String username);

    /**
     * Returns a list of roles should be given in this cell.
     * @param token Transformer cell access token
     * @return Role List
     */
    public abstract List<Role> getRoleListHere(IExtRoleContainingToken token);

    /**
     * convert role internal id to role resource URL.
     * @param roleId internal id of a role.
     * @return URL string
     */
    public abstract String roleIdToRoleResourceUrl(String roleId);

    /**
     * convert role resource url to its internal id.
     * @param roleUrl Role Url
     * @param baseUrl Base Url
     * @return internal id of the given role
     */
    public abstract String roleResourceUrlToId(String roleUrl, String baseUrl);

    /**
     * @return Cell Level ACL
     */
    public Acl getAcl() {
        CellCmp cc = ModelFactory.cellCmp(this);
        return cc.getAcl();
    }

}
