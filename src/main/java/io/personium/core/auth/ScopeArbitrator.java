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
 * Create an instance with Cell and Box information and a flag whether the token
 * authentication is done via ROPC or not.
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
    Set<String> requestedScopes = new HashSet<>();
    List<String> permittedScopes = new ArrayList<String>();

    static final Set<String> VALID_NON_URL_SCOPES = new HashSet<>(Arrays.asList(new String[] {
        CellPrivilege.ROOT.getName(),
        CellPrivilege.MESSAGE.getName(),
        CellPrivilege.MESSAGE_READ.getName(),
        CellPrivilege.EVENT.getName(),
        CellPrivilege.EVENT_READ.getName(),
        CellPrivilege.ACL.getName(),
        CellPrivilege.ACL_READ.getName(),
        CellPrivilege.AUTH.getName(),
        CellPrivilege.AUTH_READ.getName(),
        CellPrivilege.SOCIAL.getName(),
        CellPrivilege.SOCIAL_READ.getName(),
        CellPrivilege.BOX.getName(),
        CellPrivilege.BOX_BAR_INSTALL.getName(),
        CellPrivilege.BOX_READ.getName(),
        CellPrivilege.LOG.getName(),
        CellPrivilege.LOG_READ.getName(),
        CellPrivilege.PROPFIND.getName(),
        CellPrivilege.RULE.getName(),
        CellPrivilege.RULE_READ.getName(),
        OAuth2Helper.Scope.OPENID
    }));
    public ScopeArbitrator(Cell cell, Box box, boolean ropc) {
        this.cell = cell;
        this.box = box;
        this.isRopc = ropc;
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
        if (this.requestedScopes.size() == 0 && this.isRopc) {
            // if ROPC and no scope requested then root will be granted.
            this.requestedScopes.add("root");
        }
        this.arbitrate();
        return this;
    }
    private void arbitrate() {
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
        if (!VALID_NON_URL_SCOPES.contains(scope)) {
            return false;
        }
        // Now Cell Level privilege can come here.
        // if ROPC then allow any valid scopes.
        if (this.isRopc) {
            return true;
        }
        // if not the reject all .. (Tentatively)
        // TODO implement Box configuration to allow Cell Level privilege, and refer to that
        // setting.
        return false;
    }
    private boolean isRole(String scope) {
        String id = this.cell.roleResourceUrlToId(scope, PersoniumUnitConfig.getBaseUrl());
        return (id != null);
    }
}
