/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import io.personium.test.categories.Integration;
import junit.framework.TestSuite;

/**
 *  Unit Test Suite.
 */
@RunWith(Categories.class)
@SuiteClasses({
    io.personium.core.AllTests.class,
    io.personium.core.model.AllTests.class,
    io.personium.core.auth.AllTests.class,
    io.personium.core.bar.AllTests.class,
    io.personium.core.odata.AllTests.class,
    io.personium.core.rs.AllTests.class,
    io.personium.core.rs.odata.AllTests.class,
    io.personium.core.utils.AllTests.class,
    io.personium.core.model.file.AllTests.class,
    io.personium.core.model.impl.es.AllTests.class,
    io.personium.core.model.impl.es.doc.AllTests.class,
    io.personium.core.model.impl.es.odata.AllTests.class,
    io.personium.core.model.impl.es.accessor.AllTests.class,
    io.personium.core.model.lock.AllTests.class
})
@IncludeCategory(Integration.class)
public class UnitTests extends TestSuite {
}
