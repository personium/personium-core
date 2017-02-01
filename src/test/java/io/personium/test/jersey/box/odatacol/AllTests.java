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
package io.personium.test.jersey.box.odatacol;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * パッケージ配下のテストケースを全て実行するためのテストスイート.
 */
@RunWith(Suite.class)
@SuiteClasses({
        UserDataComplexPropertyDateTimeTest.class,
        UserDataComplexTypeCreateTest.class,
        UserDataComplexTypeGetTest.class,
        UserDataComplexTypeListTest.class,
        UserDataComplexTypeListWithNPTest.class,
        UserDataCreateLinkTest.class,
        UserDataCreateTest.class,
        UserDataCreateWithNPTest.class,
        UserDataDeleteTest.class,
        UserDataDynamicDoublePropertyTest.class,
        UserDataDeclardDoublePropertyTest.class,
        UserDataExpandTest.class,
        UserDataFullTextSearchTest.class,
        UserDataGetTest.class,
        UserDataListFilterBooleanTest.class,
        UserDataListFilterFunctionTest.class,
        UserDataListFilterNoneExistKeyTest.class,
        UserDataListFilterTest.class,
        UserDataListFilterTypeValidateTest.class,
        UserDataListSelectTest.class,
        UserDataListTest.class,
        UserDataListWithNPTest.class,
        UserDataListWithNPDeclaredDoubleTest.class,
        UserDataMergeTest.class,
        UserDataComplexTypeMergeTest.class,
        UserDataPropertyDateTimeTest.class,
        UserDataUpdateTest.class,
        UserDataValidateTest.class,
        UserDataLinkTest.class,
        UserDataLinkDeleteTest.class,
        UserDataLinkPropertyTest.class,
        UserDataDeepComplexTypeTest.class,
        UserDataSpecificCharTest.class,
        UserDataListTopSkipTest.class,
        UserDataAssociationTest.class,
        UserDataListWithNPQueryTest.class,
        UserDataListOrderbyTest.class,
        UserDataDeclaredDoubleComplexTypePropertyTest.class,
        UserDataListDeclaredDoubleTest.class,
        UserDataWithNPDeclaredDoublePropertyTest.class
})
public class AllTests {
}
