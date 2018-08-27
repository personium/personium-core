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
package io.personium.test.jersey.cell.ctl;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.utils.Http;

/**
 * RoleResourceのテスト.
 * setupパッケージでのRoleの準備があることが前提
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class RoleResourceTest extends PersoniumTest {

    private static String cellName = "testcell1";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleResourceTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
    }

    /**
     * RoleリソースへのGET.
     */
    @Test
    public final void getBoxRole() {
        Http.request("cell/rrs-get-box-role.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
        .with("cellPath", cellName)
        .with("box", "box1")
        .with("role", "role1")
        .returns()
        .statusCode(HttpStatus.SC_OK);

    }

    /**
     * RoleリソースルートへのPROPFIND.
     */
    @Test
    public final void getRoot() {

        // アカウント・ロールの紐付けC
        Http.request("cell/rrs-get-root.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .returns()
                .statusCode(HttpStatus.SC_OK);

    }
}
