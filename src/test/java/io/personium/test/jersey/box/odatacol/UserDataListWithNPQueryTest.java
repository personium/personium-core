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
package io.personium.test.jersey.box.odatacol;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.setup.Setup;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * UserDataのNavigationProperty経由一覧のクエリに関するテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListWithNPQueryTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public UserDataListWithNPQueryTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ユーザODataのNavigationProperty経由一覧取得でexpandに最大プロパティ数を指定した場合正常に取得できること.
     */
    @Test
    public final void ユーザODataのNavigationProperty経由一覧取得でexpandに最大プロパティ数を指定した場合正常に取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.TEST_ODATA;

        String fromEntity = "SalesDetail";
        String toEntity = "Sales";
        String expandEntity1 = "Price";
        String expandEntity2 = "Product";
        String fromUserDataId = "userdata000";

        // NP経由一覧取得($expand)
        String expands = String.format("?\\$expand=_%s,_%s", expandEntity1, expandEntity2);
        UserDataUtils.listViaNP(cell, box, collection, fromEntity, fromUserDataId, toEntity, expands,
                HttpStatus.SC_OK);
    }

    /**
     * ユーザODataのNavigationProperty経由一覧取得でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザODataのNavigationProperty経由一覧取得でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.TEST_ODATA;

        String fromEntity = "SalesDetail";
        String toEntity = "Sales";
        String expandEntity1 = "Price";
        String expandEntity2 = "Product";
        String expandEntity3 = "Supplier";
        String fromUserDataId = "userdata000";

        // NP経由一覧取得($expand)
        String expands = String.format("?\\$expand=_%s,_%s,_%s", expandEntity1, expandEntity2, expandEntity3);
        TResponse res = UserDataUtils.listViaNP(cell, box, collection, fromEntity, fromUserDataId, toEntity, expands,
                HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getCode(),
                PersoniumCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getMessage());
    }

    /**
     * ユーザODataのNavigationProperty経由一覧取得でexpand指定時にtopに取得件数最大数を指定した場合正常に取得できること.
     */
    @Test
    public final void ユーザODataのNavigationProperty経由一覧取得でexpand指定時にtopに取得件数最大数を指定した場合正常に取得できること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.TEST_ODATA;

        String fromEntity = "SalesDetail";
        String toEntity = "Sales";
        String expandEntity1 = "Price";
        String expandEntity2 = "Product";
        String fromUserDataId = "userdata000";
        int top = PersoniumUnitConfig.getTopQueryMaxSizeWithExpand();

        // NP経由一覧取得($expand)
        String query = String.format("?\\$expand=_%s,_%s&\\$top=%d", expandEntity1, expandEntity2, top);
        UserDataUtils.listViaNP(cell, box, collection, fromEntity, fromUserDataId, toEntity, query,
                HttpStatus.SC_OK);
    }

    /**
     * ユーザODataのNavigationProperty経由一覧取得でexpand指定時にtopに取得件数最大数を超える値を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザODataのNavigationProperty経由一覧取得でexpand指定時にtopに取得件数最大数を超える値を指定した場合400エラーとなること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String collection = Setup.TEST_ODATA;

        String fromEntity = "SalesDetail";
        String toEntity = "Sales";
        String expandEntity1 = "Price";
        String expandEntity2 = "Product";
        String fromUserDataId = "userdata000";
        int top = PersoniumUnitConfig.getTopQueryMaxSizeWithExpand() + 1;

        // NP経由一覧取得($expand)
        String query = String.format("?\\$expand=_%s,_%s&\\$top=%d", expandEntity1, expandEntity2, top);
        TResponse res = UserDataUtils.listViaNP(cell, box, collection, fromEntity, fromUserDataId, toEntity, query,
                HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res,
                PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", String.valueOf(top)).getCode(),
                PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", String.valueOf(top)).getMessage());
    }

}
