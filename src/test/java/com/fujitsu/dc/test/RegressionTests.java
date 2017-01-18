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
package com.fujitsu.dc.test;

import junit.framework.TestSuite;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.fujitsu.dc.test.categories.Regression;

/**
 * 結合テスト用テストスイート.
 */
@RunWith(Categories.class)
@SuiteClasses({
        com.fujitsu.dc.core.AllTests.class,
        com.fujitsu.dc.core.model.file.AllTests.class,
        com.fujitsu.dc.core.model.impl.es.AllTests.class,
        com.fujitsu.dc.core.model.impl.es.accessor.AllTests.class,
        com.fujitsu.dc.core.model.impl.es.ads.AllTests.class,
        com.fujitsu.dc.core.model.lock.AllTests.class,
        com.fujitsu.dc.core.rs.AllTests.class,
        com.fujitsu.dc.test.jersey.AllTests.class,
        com.fujitsu.dc.test.jersey.bar.AllTests.class,
        com.fujitsu.dc.test.jersey.box.AllTests.class,
        com.fujitsu.dc.test.jersey.box.odatacol.AllTests.class,
        com.fujitsu.dc.test.jersey.box.odatacol.schema.assocend.AllTests.class,
        com.fujitsu.dc.test.jersey.box.odatacol.schema.complextype.AllTests.class,
        com.fujitsu.dc.test.jersey.box.odatacol.schema.complextypeproperty.AllTests.class,
        com.fujitsu.dc.test.jersey.box.odatacol.schema.entitytype.AllTests.class,
        com.fujitsu.dc.test.jersey.box.odatacol.schema.property.AllTests.class,
        com.fujitsu.dc.test.jersey.cell.AllTests.class,
        com.fujitsu.dc.test.jersey.cell.auth.AllTests.class,
        com.fujitsu.dc.test.jersey.cell.auth.token.AllTests.class,
        com.fujitsu.dc.test.jersey.cell.ctl.AllTests.class,
        com.fujitsu.dc.test.jersey.concurrent.AllTests.class,
        com.fujitsu.dc.test.performance.box.odatacol.AllTests.class,
        com.fujitsu.dc.test.unit.core.AllTests.class,
        com.fujitsu.dc.test.unit.core.auth.AllTests.class,
        com.fujitsu.dc.test.unit.core.bar.AllTests.class,
        com.fujitsu.dc.test.unit.core.jersey.filter.AllTests.class,
        com.fujitsu.dc.test.unit.core.model.AllTests.class,
        com.fujitsu.dc.test.unit.core.model.impl.es.AllTests.class,
        com.fujitsu.dc.test.unit.core.model.impl.es.doc.AllTests.class,
        com.fujitsu.dc.test.unit.core.model.impl.es.odata.AllTests.class,
        com.fujitsu.dc.test.unit.core.rs.odata.AllTests.class,
        com.fujitsu.dc.test.unit.core.rs.odata.validate.AllTests.class
})
@IncludeCategory(Regression.class)
public class RegressionTests extends TestSuite {
}
