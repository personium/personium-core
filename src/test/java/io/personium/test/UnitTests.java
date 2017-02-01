/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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

import junit.framework.TestSuite;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import io.personium.test.categories.Integration;

/**
 * 結合テスト用テストスイート.
 */
@RunWith(Categories.class)
@SuiteClasses({
        io.personium.core.AllTests.class,
        io.personium.core.model.file.AllTests.class,
        io.personium.core.model.impl.es.AllTests.class,
        io.personium.core.model.impl.es.accessor.AllTests.class,
        io.personium.core.model.impl.es.ads.AllTests.class,
        io.personium.core.model.lock.AllTests.class,
        io.personium.core.odata.AllTests.class,
        io.personium.core.rs.odata.AllTests.class,
        io.personium.core.rs.AllTests.class,
        io.personium.core.utils.AllTests.class,
        io.personium.test.jersey.AllTests.class,
        io.personium.test.jersey.bar.AllTests.class,
        io.personium.test.jersey.box.AllTests.class,
        io.personium.test.jersey.box.odatacol.AllTests.class,
        io.personium.test.jersey.box.odatacol.schema.assocend.AllTests.class,
        io.personium.test.jersey.box.odatacol.schema.complextype.AllTests.class,
        io.personium.test.jersey.box.odatacol.schema.complextypeproperty.AllTests.class,
        io.personium.test.jersey.box.odatacol.schema.entitytype.AllTests.class,
        io.personium.test.jersey.box.odatacol.schema.property.AllTests.class,
        io.personium.test.jersey.cell.AllTests.class,
        io.personium.test.jersey.cell.auth.AllTests.class,
        io.personium.test.jersey.cell.auth.token.AllTests.class,
        io.personium.test.jersey.cell.ctl.AllTests.class,
        io.personium.test.jersey.concurrent.AllTests.class,
        io.personium.test.performance.box.odatacol.AllTests.class,
        io.personium.test.unit.core.AllTests.class,
        io.personium.test.unit.core.auth.AllTests.class,
        io.personium.test.unit.core.bar.AllTests.class,
        io.personium.test.unit.core.jersey.filter.AllTests.class,
        io.personium.test.unit.core.model.AllTests.class,
        io.personium.test.unit.core.model.impl.es.AllTests.class,
        io.personium.test.unit.core.model.impl.es.doc.AllTests.class,
        io.personium.test.unit.core.model.impl.es.odata.AllTests.class,
        io.personium.test.unit.core.rs.odata.AllTests.class,
        io.personium.test.unit.core.rs.odata.validate.AllTests.class
})
@IncludeCategory(Integration.class)
public class UnitTests extends TestSuite {
}
