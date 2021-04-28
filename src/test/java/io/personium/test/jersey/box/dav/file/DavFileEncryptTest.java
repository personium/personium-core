/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.test.jersey.box.dav.file;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.jersey.PersoniumIntegTestRunner;

/**
 * Encrypt DAV File related tests.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Integration.class, Regression.class })
public class DavFileEncryptTest extends DavFileTest {

    /** dav.encrypt.enabled in properties. */
    private static String encryptEnabled = "";

    /**
     * Befor class.
     */
    @BeforeClass
    public static void beforClass() {
        encryptEnabled = PersoniumUnitConfig.get("io.personium.core.security.dav.encrypt.enabled");
        PersoniumUnitConfig.set("io.personium.core.security.dav.encrypt.enabled", "true");
    }

    /**
     * After class.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set("io.personium.core.security.dav.encrypt.enabled",
                encryptEnabled != null ? encryptEnabled : "false"); // CHECKSTYLE IGNORE
    }
}
