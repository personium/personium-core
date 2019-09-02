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

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;

/**
 * Box URL Class for performing processing excluding Dav related persistence by delegating processing from resource object.
 */
public class BoxUrlRsCmp extends BoxRsCmp {

    /**
     * constructor.
     * @param cellRsCmp CellRsCmp
     * @param davCmp DavCmp
     * @param accessContext AccessContext
     * @param box box
     */
    public BoxUrlRsCmp(final CellRsCmp cellRsCmp, final DavCmp davCmp,
            final AccessContext accessContext, final Box box) {
        super(cellRsCmp, davCmp, accessContext, box);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAccessContext(Privilege privilege) {
        AcceptableAuthScheme allowedAuthScheme = getAcceptableAuthScheme();

        AccessContext ac = this.getAccessContext();

        // For unit user token, do not check
        if (ac.isUnitUserToken(privilege)) {
            return;
        }

        // For schema authenticated token of target box, do not check.
        if (ac.getSchema() != null && isMatchesBoxSchema(ac)) {
            return;
        }

        // Check auth schema.
        ac.checkSchemaAccess(this.getConfidentialLevel(), this.getBox(), allowedAuthScheme);

        // Check basic authentication.
        ac.updateBasicAuthenticationStateForResource(null);

        // Check access control.
        if (!this.hasSubjectPrivilege(privilege)) {
            // If the token is INVALID or Privilege is set to all it is necessary to grant access.
            // For this reason, check the validity of the token at this timing.
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(allowedAuthScheme);
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(), allowedAuthScheme);
            }
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * Whether the schema of the token matches the schema of the box.
     * @param ac access context
     * @return true:match  false:not match
     */
    private boolean isMatchesBoxSchema(AccessContext ac) {
        try {
            ac.checkSchemaMatches(getBox());
        } catch (PersoniumCoreException e) {
            return false;
        }
        return true;
    }

    /**
     * Obtain Auth Scheme that can be used for authentication.
     * Autret Scheme that can be used for @return authentication
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return AcceptableAuthScheme.BEARER;
    }

}
