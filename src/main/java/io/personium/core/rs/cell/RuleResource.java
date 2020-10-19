/**
 * Personium
 * Copyright 2017-2020 Personium Project Authors
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

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Response;

import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.rule.RuleManager;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource for Rule Endpoint.
 */
public class RuleResource {

    Cell cell;
    AccessContext accessContext;
    CellRsCmp cellRsCmp;

    /**
     * constructor.
     * @param cell Cell
     * @param accessContext AccessContext
     * @param cellRsCmp CellRsCmp
     */
    public RuleResource(final Cell cell, final AccessContext accessContext, final CellRsCmp cellRsCmp) {
        this.cell = cell;
        this.accessContext = accessContext;
        this.cellRsCmp = cellRsCmp;
    }

    /**
     * Return list of rule.
     * @return JAX-RS Response Object
     */
    @GET
    public final Response list() {
        // access control
        this.cellRsCmp.checkAccessContext(CellPrivilege.RULE_READ);

        RuleManager rman = RuleManager.getInstance();
        Map<String, Object> map = rman.getRules(this.cell);

        return ResourceUtils.responseBuilderJson(map).build();
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // Access Control
        this.cellRsCmp.checkAccessContext(CellPrivilege.RULE_READ);
        return ResourceUtils.responseBuilderForOptions(HttpMethod.GET)
                            .build();
    }

}
