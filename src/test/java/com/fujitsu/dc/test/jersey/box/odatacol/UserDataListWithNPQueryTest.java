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
package com.fujitsu.dc.test.jersey.box.odatacol;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataのNavigationProperty経由一覧のクエリに関するテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListWithNPQueryTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public UserDataListWithNPQueryTest() {
        super("com.fujitsu.dc.core.rs");
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

        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getCode(),
                DcCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getMessage());
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
        int top = DcCoreConfig.getTopQueryMaxSizeWithExpand();

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
        int top = DcCoreConfig.getTopQueryMaxSizeWithExpand() + 1;

        // NP経由一覧取得($expand)
        String query = String.format("?\\$expand=_%s,_%s&\\$top=%d", expandEntity1, expandEntity2, top);
        TResponse res = UserDataUtils.listViaNP(cell, box, collection, fromEntity, fromUserDataId, toEntity, query,
                HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res,
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", String.valueOf(top)).getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", String.valueOf(top)).getMessage());
    }

}
