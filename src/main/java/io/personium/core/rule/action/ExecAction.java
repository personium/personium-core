/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
package io.personium.core.rule.action;

import java.util.Optional;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;

/**
 * Action for exec action.
 */
public class ExecAction extends PostAction {

    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public ExecAction(Cell cell, ActionInfo ai) {
        super(cell, ai);
    }

    @Override
    protected String getRequestUrl() {
        return ActionUtils.getUrl(this.service);
    }

    @Override
    protected void setSpecificHeaders(HttpMessage req, PersoniumEvent event) {
        if (cell == null || req == null) {
            return;
        }

        // Authorization header
        Optional<String> accessToken = new TokenBuilder().cellUrl(cell.getUrl())
                                                         .targetCellUrl(cell.getUrl())
                                                         .subject(event.getSubject().orElse(null))
                                                         .schema(event.getSchema().orElse(null))
                                                         .roleList(event.getRoleList())
                                                         .build();
        accessToken.ifPresent(token -> req.addHeader(HttpHeaders.AUTHORIZATION,
                                                     OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX + token));
    }
}

