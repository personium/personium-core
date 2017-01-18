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
package com.fujitsu.dc.test.jersey.cell;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * パッケージ配下のテストケースを全て実行するためのテストスイート.
 */
@RunWith(Suite.class)
@SuiteClasses({
        AclTest.class,
        BoxUrlTest.class,
        CreateTest.class,
        CellBulkDeletionTest.class,
        DefaultBoxTest.class,
        DeleteTest.class,
        ErrorPageTest.class,
        EventTest.class,
        LogTest.class,
        EventArchiveLogGetTest.class,
        MessageApproveTest.class,
        MessageEscapeTest.class,
        MessageMethodNotAllowTest.class,
        MessageReceivedTest.class,
        MessageSentTest.class,
        MessageListTest.class,
        PropPatchTest.class,
        ReadListTest.class,
        ReadTest.class,
        UnitUserCellCRUDTest.class,
        UpdateTest.class
})
public class AllTests {
}
