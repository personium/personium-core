package io.personium.core.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.utils.UriUtils;

public class ScopeArbitrator {
    Cell cell;
    Box box;
    boolean isRopc;
    String[] requestedScopes = new String[0];
    List<String> permittedScopes = new ArrayList<String>();

    static final String[] VALID_NON_URL_SCOPES = new String[] {
            "root",
            OAuth2Helper.Scope.OPENID
    };
    public ScopeArbitrator(Cell cell, Box box, boolean ropc) {
        this.cell = cell;
        this.box = box;
        this.isRopc = ropc;
    }
    public void request(String requestScopes) {
        this.requestedScopes = AbstractOAuth2Token.Scope.parse(requestScopes);
        arbitrate();
    }
    public ScopeArbitrator request(String[] requestScopes) {
        if (requestScopes != null) {
            this.requestedScopes = requestScopes;
        }
        arbitrate();
        return this;
    }
    private void arbitrate() {
        for (int i = 0 ; i < this.requestedScopes.length ; i++) {
            if (this.check(requestedScopes[i])) {
                this.permittedScopes.add(this.requestedScopes[i]);
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
        if (Arrays.binarySearch(VALID_NON_URL_SCOPES, scope) < 0) {
            return false;
        }
        // if ROPC then allow any valid scopes.
        if (this.isRopc) {
            return true;
        }

        return false;
    }
    private boolean isRole(String scope) {
        String id = this.box.getCell().roleResourceUrlToId(scope, PersoniumUnitConfig.getBaseUrl());
        return (id != null);
    }
}
