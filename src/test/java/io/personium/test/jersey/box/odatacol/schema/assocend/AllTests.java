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
package io.personium.test.jersey.box.odatacol.schema.assocend;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * パッケージ配下のテストケースを全て実行するためのテストスイート.
 */
@RunWith(Suite.class)
@SuiteClasses({
    AssociationEndCreateLinkTest.class,
    AssociationEndCreateTest.class,
    AssociationEndDeleteLinkTest.class,
    AssociationEndDeleteTest.class,
    AssociationEndListLinkTest.class,
    AssociationEndReadLinkTest.class,
    AssociationEndReadTest.class,
    AssociationEndCreateViaNPTest.class,
    AssociationEndUpdateTest.class
    })
public class AllTests {
}
