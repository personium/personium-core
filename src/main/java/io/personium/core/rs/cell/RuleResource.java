/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;

import io.personium.core.rule.RuleManager;

/**
 * JAX-RS Resource for Rule Endpoint.
 */
public class RuleResource {
    Cell cell;
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param cell Cell
     * @param davRsCmp DavRsCmp
     */
    public RuleResource(final Cell cell, final DavRsCmp davRsCmp) {
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }

    /**
     * Return list of rule.
     * @return JAX-RS Response Object
     */
    @GET
    public final Response list() {
        // access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.RULE_READ);

        RuleManager rman = RuleManager.getInstance();
        String ruleList = rman.getRules(this.cell);

        return Response.ok(ruleList).build();
    }

}
