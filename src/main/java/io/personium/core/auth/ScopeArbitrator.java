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
    public ScopeArbitrator request(String requestScopes) {
        return this.request(AbstractOAuth2Token.Scope.parse(requestScopes));
    }
    public ScopeArbitrator request(String[] requestScopes) {
        if (requestScopes != null) {
            this.requestedScopes = new HashSet<>(Arrays.asList(requestScopes));
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
        if (resolvedScope.startsWith("http://") || resolvedScope.startsWith("https://")) {
            if (isRole(resolvedScope)) {
                return true;
            }
            return false;

        }
        // Exclude invalid non-URL values;
        if (!VALID_NON_URL_SCOPES.contains(scope)) {
            return false;
        }
        // if ROPC then allow any valid scopes.
        if (this.isRopc) {
            return true;
        }

        return false;
    }
    private boolean isRole(String scope) {
        String id = this.cell.roleResourceUrlToId(scope, PersoniumUnitConfig.getBaseUrl());
        return (id != null);
    }
}
