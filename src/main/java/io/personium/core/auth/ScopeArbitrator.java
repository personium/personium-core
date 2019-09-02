package io.personium.core.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.utils.UriUtils;

/**
 * Class for scope arbitration object.
 * Create an instance with Cell and Box information and grant_type string.
 *
 * With isROPC true:
 *   It is a cell admin mode. So any scope request will be admitted.
 *   if not request is made then default scope will be root.
 *
 * With isROPC false:
 *   Normal use cases.
 *   only scopes that are pre-granted to box will be admitted.
 *   i.e. Cell Level Privileges and Roles
 *   if no box exists then no scope will be granted.
 *
 *   not implemented yet.
 */
public class ScopeArbitrator {
    Cell cell;
    Box box;
    boolean isRopc;
    String grantType;
    Privilege unitMaxScopePrivilege;
    Set<String> requestedScopes = new HashSet<>();
    List<String> permittedScopes = new ArrayList<String>();

    static final Set<String> VALID_NON_URL_SCOPES = new HashSet<>(Arrays.asList(new String[] {
        OAuth2Helper.Scope.OPENID
    }));
    public ScopeArbitrator(Cell cell, Box box, String grantType) {
        this.cell = cell;
        this.box = box;
        this.grantType = grantType;
        String unitMaxScopeStr = null;
        if (OAuth2Helper.GrantType.PASSWORD.equals(this.grantType)) {
            unitMaxScopeStr = PersoniumUnitConfig.getTokenDefaultScopeRopc();
        } else if (OAuth2Helper.GrantType.AUTHORIZATION_CODE.equals(this.grantType)) {
            unitMaxScopeStr = PersoniumUnitConfig.getTokenDefaultScopeCode();
        } else if (OAuth2Helper.GrantType.SAML2_BEARER.equals(this.grantType)) {
            unitMaxScopeStr = PersoniumUnitConfig.getTokenDefaultScopeAssertion();
        } else {
            unitMaxScopeStr = PersoniumUnitConfig.getTokenDefaultScopeRopc();
        }
        this.unitMaxScopePrivilege = Privilege.get(CellPrivilege.class, unitMaxScopeStr);
    }
    public ScopeArbitrator requestString(String requestScopes) {
        return this.request(AbstractOAuth2Token.Scope.parse(requestScopes));
    }
    public ScopeArbitrator request(String[] requestScopes) {
        if (requestScopes != null) {
            this.requestedScopes = new HashSet<>(Arrays.asList(requestScopes));
        }
        // remove empty entry
        this.requestedScopes.remove("");
        this.arbitrate();
        return this;
    }
    private void arbitrate() {
        if (this.requestedScopes.size() == 0 && this.unitMaxScopePrivilege != null) {
            this.requestedScopes.add(this.unitMaxScopePrivilege.getName());
        }
        for (String scope : this.requestedScopes) {
            if (this.check(scope)) {
                this.permittedScopes.add(scope);
            }
       }
    }
    public String[] getResults() {
        return this.permittedScopes.toArray(new String[0]);
    }
    private boolean check(String scope) {
        //
        if (VALID_NON_URL_SCOPES.contains(scope)) {
            return true;
        }
        String resolvedScope = UriUtils.resolveLocalUnit(scope);
        // If it looks like a role because it is a http URL.
        if (resolvedScope.startsWith("http://") || resolvedScope.startsWith("https://")) {
            // check if it is really a role or not
            if (isRole(resolvedScope)) {
                return true;
            }
            return false;
        }

        // If not, it should probably be Cell Privilege.
        // make sure.
        CellPrivilege cp = Privilege.get(CellPrivilege.class, scope);
        if (cp == null) {
            return false;
        }
        // Now Cell Level privilege can come here.
        // if ROPC then allow any valid scopes.
        if (this.unitMaxScopePrivilege != null && this.unitMaxScopePrivilege.includes(cp)) {
            return true;
        }
        // if not then reject all .. (Tentatively)
        // TODO implement Box configuration to allow Cell Level privilege, and refer to that
        // setting.
        return false;
    }
    private boolean isRole(String scope) {
        String id = this.cell.roleResourceUrlToId(scope, PersoniumUnitConfig.getBaseUrl());
        return id != null;
    }
}
