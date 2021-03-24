/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test.engine;

import org.apache.http.HttpStatus;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.utils.Http;

/**
 * サービス実行のリレーテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Integration.class, Regression.class })
public class ServiceRelayTestStatic extends ServiceRelayTestBase {

    /**
     * {@inheritDoc}
     */
    @Override
    void configureService() {
        // PropPatch サービス設定の登録
        Http.request("box/proppatch-set-service.txt")
                .with("path", "service_relay")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("name", "relay")
                .with("src", "relay.js")
                .returns()
                .statusCode(HttpStatus.SC_MULTI_STATUS);

        // WebDAV サービスリソースの登録
        Http.request("box/dav-put.txt")
                .with("cellPath", "testcell1")
                .with("path", "service_relay/__src/relay.js")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("box", "box1")
                .with("contentType", "text/javascript")
                .with("source", SOURCE)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void deconfigureService() {
        // WebDAV サービスリソースの削除
        Http.request("box/dav-delete.txt")
                .with("cellPath", "testcell1")
                .with("path", "service_relay/__src/relay.js")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("box", "box1")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getPathToExecute() {
        return "service_relay/relay";
    }
}
