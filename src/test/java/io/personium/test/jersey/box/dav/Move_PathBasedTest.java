/**
 * Personium
 * Copyright 2020-2022 Personium Project Authors
 * - Akio Shimono
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
package io.personium.test.jersey.box.dav;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import io.personium.core.PersoniumUnitConfig;


public class Move_PathBasedTest extends MoveTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
    }

    @Before
    public void setUp() throws Exception {
        createTestCell();
        createBoxOnTestCell("box1", "https://app1.unit.example/");
    }
    @AfterClass
    public static void tearDown() throws Exception {
        deleteTestCell();
    }
}

