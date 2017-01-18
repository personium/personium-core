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
package com.fujitsu.dc.test.jersey.cell.ctl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * パッケージ配下のテストケースを全て実行するためのテストスイート.
 */
@RunWith(Suite.class)
@SuiteClasses({
        AccountCreateTest.class,
        AccountRoleLinkListTest.class,
        AccountRoleLinkTest.class,
        AccountTest.class,
        AccountUpdateTest.class,
        AccountListTest.class,
        AccountViaNPTest.class,
        BoxCrudTest.class,
        BoxRelationLinkTest.class,
        BoxRoleLinkTest.class,
        EventTest.class,
        ExtCellCreateTest.class,
        ExtCellDeleteTest.class,
        ExtCellLinkTest.class,
        ExtCellReadTest.class,
        ExtCellUpdateTest.class,
        ExtRoleCreateTest.class,
        ExtRoleDeleteTest.class,
        ExtRoleLinkTest.class,
        ExtRoleListTest.class,
        ExtRoleReadTest.class,
        ExtRoleUpdateTest.class,
        RelationCreateTest.class,
        RelationDeleteTest.class,
        RelationReadTest.class,
        RelationUpdateTest.class,
        RoleCreateTest.class,
        RoleDeleteTest.class,
        RoleListTest.class,
        RoleReadTest.class,
        RoleResourceTest.class,
        RoleUpdateTest.class,
        RoleViaNPTest.class,
        BoxRoleViaNPTest.class
})
public class AllTests {
}
