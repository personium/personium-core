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

import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;

import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.odata.PersoniumODataProducer;

/**
 * JAX-RS resource that handles password change processing in resource class.
 */
public class PasswordResource {

    String pCredHeader;
    AccessContext accessContext;
    Cell cell;

    static Logger log = LoggerFactory.getLogger(PasswordResource.class);

    private String key;
    private String keyString = null;
    private OEntityKey oEntityKey;
    private DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param accessContext accessContext
     * @param pCredHeader pCredHeader
     * @param cell cell
     * @param davRsCmp DavRsCmp
     */
    public PasswordResource(final AccessContext accessContext,
            final String pCredHeader,
            Cell cell, DavRsCmp davRsCmp) {
        this.accessContext = accessContext;
        this.pCredHeader = pCredHeader;
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }

    /**
     * Change the password.
     * @return ODataEntityResource class object
     */
    @WriteAPI
    @PUT
    public Response mypass() {
        //Access control
        this.accessContext.checkMyLocalToken(cell, this.davRsCmp.getAcceptableAuthScheme());
        //Get the Account name to change password from cell local token
        this.key = this.accessContext.getSubject();
        String[] keyName;
        keyName = this.key.split("#");
        this.keyString = "('" + keyName[1] + "')";

        try {
            this.oEntityKey = OEntityKey.parse(this.keyString);
        } catch (IllegalArgumentException e) {
            throw PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.reason(e);
        }

        //Obtain schema information of Account
        PersoniumODataProducer producer = ModelFactory.ODataCtl.cellCtl(accessContext.getCell());
        EdmEntitySet esetAccount = producer.getMetadata().getEdmEntitySet(Account.EDM_TYPE_NAME);

        //Ask Producer to change password
        producer.updatePassword(esetAccount, this.oEntityKey, this.pCredHeader);

        //Response return
        return Response.noContent()
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();
    }
}
