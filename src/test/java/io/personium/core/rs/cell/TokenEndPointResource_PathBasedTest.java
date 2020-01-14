/**
 * Personium
 * Copyright 2014-2019 Personium Project
 *  - FUJITSU LIMITED
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Unit;

/**
 * TokenEndPointResource unit test class.
 */
@Category({Unit.class })
public class TokenEndPointResource_PathBasedTest extends TokenEndPointResourceTest {
    static Logger log = LoggerFactory.getLogger(TokenEndPointResource_PathBasedTest.class);


    @BeforeClass
    public static void beforeClass() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.Security.TOKEN_SECRET_KEY, "0123456789abcdef");
        // path-based cell Url.
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        PersoniumCoreApplication.loadConfig();
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
        PersoniumCoreApplication.loadPlugins();
        CELL_URL = UriUtils.convertSchemeFromLocalUnitToHttp(CELL_URL_LOCALUNIT);
        SCHEMA_URL = UriUtils.convertSchemeFromLocalUnitToHttp(SCHEMA_URL_LOCALUNIT);
    }

    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.reload();
    }
}
