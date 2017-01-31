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

import java.util.Collections;
import java.util.List;

import org.core4j.Enumerable;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

import io.personium.common.auth.token.IExtRoleContainingToken;
import io.personium.common.auth.token.Role;
import io.personium.core.event.EventBus;
import io.personium.core.model.ctl.Common;
import io.personium.core.odata.OEntityWrapper;

import java.util.Arrays;

/**
 * Model Class for Cell.
 */
public interface Cell {

    /**
     * returns Cell name.
     * @return Cell name
     */
    String getName();

    /**
     * returns internal ID string.
     * @return internal ID string
     */
    String getId();

    /**
     * returns URL string for this cell.
     * @return URL string
     */
    String getUrl();

    /**
     * returns Unit URL string for this cell.
     * @return Unit URL string
     */
    String getUnitUrl();

    /**
     * It gets the URI of the Cell of the Owner Unit User.
     * @return Cell name
     */
    String getOwner();

    /**
     * It gets the prefix without Unit User name of the Cell.
     * @return .
     */
    String getDataBundleNameWithOutPrefix();

    /**
     * It gets the Unit User name of the Cell.
     * @return Unit User name
     */
    String getDataBundleName();

    /**
     * It gets the EventBus of the Cell.
     * @return EventBus
     */
    EventBus getEventBus();

    /**
     * It gets the Cell of creation time.
     * @return time stamp of this cell creation.
     */
    long getPublished();

    /**
     * Data and control objects under (Box, Account, etc.) if there is no return true..
     * The default box may be.
     * @return It is true if there is no data and control objects under
     * (Box, Account, etc.).
     */
    boolean isEmpty();

    /**
     * To delete all the data and control objects in the underlying
     * (Box, Account, etc.).
     */
    void makeEmpty();

    /**
     * delete this cell.
     * @param recursive set true if you want to delete recursively
     * @param unitUserName to use for deletion operation
     */
    void delete(boolean recursive, String unitUserName);

    /**
     * Specify the Box name to get the Box.
     * @param boxName Box name
     * @return Box
     */
    Box getBoxForName(String boxName);

    /**
     * Specify the Box schema to get the Box.
     * @param boxSchema box schema uri
     * @return Box
     */
    Box getBoxForSchema(String boxSchema);

    /**
     * It gets the Accounts to specify the Account name.
     * @param username Account name
     * @return Account
     */
    OEntityWrapper getAccount(final String username);

    /**
     * @param oew account
     * @param password password
     * @return true if authentication is successful.
     */
    boolean authenticateAccount(final OEntityWrapper oew, String password);

    // public abstract void createAccount(String username, String schema) throws Cell.ManipulationException;
    // public abstract void createConnector(String name, String schema) throws Cell.ManipulationException;
    /**
     * @param username access account id
     * @return List of Roles
     */
    List<Role> getRoleListForAccount(String username);

    /**
     * Returns a list of roles should be given in this cell.
     * @param token Transformer cell access token
     * @return Role List
     */
    List<Role> getRoleListHere(IExtRoleContainingToken token);

    /**
     * convert role internal id to role resource URL.
     * @param roleId internal id of a role.
     * @return URL string
     */
    String roleIdToRoleResourceUrl(String roleId);

    /**
     * convert role resource url to its internal id.
     * @param roleUrl Role Url
     * @param baseUrl Base Url
     * @return internal id of the given role
     */
    String roleResourceUrlToId(String roleUrl, String baseUrl);

    /**
     * Edm.Entity Type Name.
     */
    String EDM_TYPE_NAME = "Cell";

    /**
     * Name Definition of property.
     */
    EdmProperty.Builder P_PATH_NAME = EdmProperty.newBuilder("Name")
            .setNullable(false)
            .setAnnotations(Common.DC_FORMAT_NAME)
            .setType(EdmSimpleType.STRING);

    /**
     * Property List.
     */
    List<EdmProperty.Builder> PROPS = Collections.unmodifiableList(Arrays.asList(
            new EdmProperty.Builder[] {
                    P_PATH_NAME, Common.P_PUBLISHED, Common.P_UPDATED}
            ));
    /**
     * Key List.
     */
    List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            new String[] {P_PATH_NAME.getName()}
            ));;

    /**
     * EntityType Builder of the Cell.
     */
    EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder().setNamespace(Common.EDM_NS_UNIT_CTL)
            .setName(EDM_TYPE_NAME).addProperties(Enumerable.create(PROPS).toList()).addKeys(KEYS);

}
