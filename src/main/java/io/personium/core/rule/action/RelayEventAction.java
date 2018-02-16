/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;

import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;

/**
 * Action for relay.event action.
 */
public class RelayEventAction extends RelayAction {
    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public RelayEventAction(Cell cell, ActionInfo ai) {
        super(cell, ai);
        setSvcName("relayevent");
    }

    @Override
    protected void setHeaders(HttpMessage req, PersoniumEvent event) {
        super.setHeaders(req, event);

        // create permitted role list
        List<Role> roleList = new ArrayList<Role>();
        String roles = event.getRoles();
        if (roles != null) {
            String[] parts = roles.split(",");
            for (int i = 0; i < parts.length; i++) {
                try {
                    URL url = new URL(parts[i]);
                    Role role = new Role(url);
                    roleList.add(role);
                } catch (MalformedURLException e) {
                    // do nothing because of error
                    return;
                }
            }
        }
        // create transcell token
        TransCellAccessToken token = new TransCellAccessToken(
            UUID.randomUUID().toString(),
            new Date().getTime(),
            TransCellAccessToken.LIFESPAN,
            cell.getUrl(),
            event.getSubject(),
            service, // targetUrl
            roleList,
            event.getSchema() // schema
        );
        String accessToken = token.toTokenString();
        req.addHeader(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX + accessToken);
    }

    @Override
    protected String getVia(PersoniumEvent event) {
        // set X-Personium-Via header
        String via = event.getVia();
        if (via == null) {
            via = cell.getUrl();
        } else {
            via = via + "," + cell.getUrl();
        }
        return via;
    }
}

