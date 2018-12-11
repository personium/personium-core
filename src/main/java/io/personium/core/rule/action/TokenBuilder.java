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

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;

/**
 * Create token string.
 */
public class TokenBuilder {
    private String cellUrl;
    private String targetCellUrl;
    private String subject;
    private String schema;
    private List<Role> roleList;

    /**
     * Constructor.
     */
    public TokenBuilder() {
        roleList = new ArrayList<Role>();
    }

    /**
     * Set cellUrl.
     * @param url self cellUrl
     * @return TokenBuilder
     */
    public TokenBuilder cellUrl(String url) { // CHECKSTYLE IGNORE
        this.cellUrl = url;
        return this;
    }

    /**
     * Set targetCellUrl.
     * @param url target cellUrl
     * @return TokenBuilder
     */
    public TokenBuilder targetCellUrl(String url) { // CHECKSTYLE IGNORE
        this.targetCellUrl = url;
        return this;
    }

    /**
     * Set subject.
     * @param subject subject
     * @return TokenBuilder
     */
    public TokenBuilder subject(String subject) { // CHECKSTYLE IGNORE
        this.subject = subject;
        return this;
    }

    /**
     * Set schema.
     * @param schema schema
     * @return TokenBuilder
     */
    public TokenBuilder schema(String schema) { // CHECKSTYLE IGNORE
        this.schema = schema;
        return this;
    }

    /**
     * Set roleList.
     * @param roleList list of role
     * @return TokenBuilder
     */
    public TokenBuilder roleList(List<Role> roleList) { // CHECKSTYLE IGNORE
        this.roleList = roleList;
        return this;
    }

    /**
     * Build token string.
     * @return token string
     */
    public Optional<String> build() {
        String accessToken = null;

        if (subject == null || cellUrl == null || targetCellUrl == null) {
            return Optional.empty();
        }

        if (cellUrl.equals(targetCellUrl)) {
            // local access token
            if (subject.startsWith(cellUrl)) {
                String[] parts = subject.split(Pattern.quote("#"));
                if (parts.length == 2) {
                    subject = parts[1];
                } else {
                    subject = null;
                }
                // AccountAccessToken
                AccountAccessToken token =
                    new AccountAccessToken(new Date().getTime(),
                                           AccountAccessToken.ACCESS_TOKEN_EXPIRES_HOUR
                                               * AccountAccessToken.MILLISECS_IN_AN_HOUR,
                                           cellUrl,
                                           subject,
                                           schema);
                accessToken = token.toTokenString();
            } else {
                // CellLocalAccessToken
                CellLocalAccessToken token =
                    new CellLocalAccessToken(new Date().getTime(),
                                             CellLocalAccessToken.ACCESS_TOKEN_EXPIRES_HOUR
                                                 * CellLocalAccessToken.MILLISECS_IN_AN_HOUR,
                                             cellUrl,
                                             subject,
                                             roleList,
                                             schema);
                accessToken = token.toTokenString();
            }
        } else {
            // create transcell token
            TransCellAccessToken token =
                new TransCellAccessToken(UUID.randomUUID().toString(),
                                         new Date().getTime(),
                                         TransCellAccessToken.LIFESPAN,
                                         cellUrl,
                                         subject,
                                         targetCellUrl,
                                         roleList,
                                         schema);
            accessToken = token.toTokenString();
        }

        return Optional.ofNullable(accessToken);
    }

}
