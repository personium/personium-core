/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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

import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;

import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;

/**
 * Action for relay action.
 */
public class RelayAction extends PostAction {
    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public RelayAction(Cell cell, ActionInfo ai) {
        super(cell, ai);
    }

    @Override
    protected String getRequestUrl() {
        return service;
    }

    @Override
    protected void setHeaders(HttpMessage req, PersoniumEvent event) {
        if (cell == null || req == null || event.getSubject() == null) {
            return;
        }

        String accessToken;
        String targetCell = getTargetCellUrl();
        if (cell.getUrl().equals(targetCell)) {
            // local access token
            String subject = event.getSubject();
            if (subject.startsWith(cell.getUrl())) {
                String[] parts = subject.split(Pattern.quote("#"));
                if (parts.length == 2) {
                    subject = parts[1];
                } else {
                    subject = null;
                }
                // AccountAccessToken
                AccountAccessToken token = new AccountAccessToken(
                    new Date().getTime(),
                    AccountAccessToken.ACCESS_TOKEN_EXPIRES_HOUR * AccountAccessToken.MILLISECS_IN_AN_HOUR,
                    cell.getUrl(),
                    subject,
                    event.getSchema()
                );
                accessToken = token.toTokenString();
            } else {
                // CellLocalAccessToken
                CellLocalAccessToken token = new CellLocalAccessToken(
                    new Date().getTime(),
                    CellLocalAccessToken.ACCESS_TOKEN_EXPIRES_HOUR * CellLocalAccessToken.MILLISECS_IN_AN_HOUR,
                    cell.getUrl(),
                    subject,
                    getRoleList(event),
                    event.getSchema()
                );
                accessToken = token.toTokenString();
            }
        } else {
            // create transcell token
            TransCellAccessToken token = new TransCellAccessToken(
                UUID.randomUUID().toString(),
                new Date().getTime(),
                TransCellAccessToken.LIFESPAN,
                cell.getUrl(),
                event.getSubject(),
                getTargetCellUrl(), // targetUrl
                getRoleList(event),
                event.getSchema() // schema
            );
            accessToken = token.toTokenString();
        }
        req.addHeader(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX + accessToken);
    }

    private static final int SPLIT_NUM = 7;
    private static final int BOXCOLSVC_SPLIT_NUM = 3;

    /**
     * Get target cell from targetUrl.
     * @return cell url
     */
    protected String getTargetCellUrl() {
        String[] parts = service.split(Pattern.quote("/"));
        if (parts.length < SPLIT_NUM) {
            return null;
        }
        String target = parts[0];
        for (int i = 1; i < parts.length - BOXCOLSVC_SPLIT_NUM; i++) {
            target += "/" + parts[i];
        }
        return target + "/";
    }
}
