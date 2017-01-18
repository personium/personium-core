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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.model.ctl.Relation;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * Relationの一件取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RelationReadTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testRelationName = "testrelation";
    private static final String RELATION_TYPE = "CellCtl.Relation";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RelationReadTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * BoxNameにBox名を指定してRelationを一件取得しデータを取得できること.
     */
    @Test
    public void BoxNameにBox名を指定してRelationを一件取得しデータを取得できること() {
        String boxname = "box1";
        try {
            CellCtlUtils.createRelation(cellName, testRelationName, boxname);

            TResponse response = Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1",
                    Relation.EDM_TYPE_NAME,
                    "Name='" + testRelationName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRelationName);
            // リンク情報からレスポンスボディ作成
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName, boxname);
        }
    }

    /**
     * BoxNameにnullを指定してRelationを一件取得しデータを取得できること.
     */
    @Test
    public void BoxNameにnullを指定してRelationを一件取得しデータを取得できること() {
        try {
            CellCtlUtils.createRelation(cellName, testRelationName);

            TResponse response = Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "null")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1",
                    Relation.EDM_TYPE_NAME,
                    "Name='" + testRelationName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRelationName);
            // リンク情報からレスポンスボディ作成
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName);
        }
    }

    /**
     * BoxNameを省略してRelationを一件取得した場合データが取得できること.
     */
    @Test
    public void BoxNameを省略してRelationを一件取得した場合データが取得できること() {
        TResponse resCreateRelation = null;
        try {
            // _Box.Name指定なしでRelation作成
            resCreateRelation = CellCtlUtils.createRelation(cellName, testRelationName);

            // _Box.Name指定なしでRelation取得
            TResponse response = Http.request("relation-retrieve-without-boxname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1",
                    Relation.EDM_TYPE_NAME,
                    "Name='" + testRelationName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRelationName);
            // リンク情報からレスポンスボディ作成
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

        } finally {
            if (resCreateRelation.getLocationHeader() != null) {
                deleteOdataResource(resCreateRelation.getLocationHeader());
            }
        }
    }

    /**
     * BoxNameを指定してRelationを登録し、BoxNameを省略してRelationを一件取得した場合データが取得できないこと.
     */
    @Test
    public void BoxNameを指定してRelationを登録しBoxNameを省略してRelationを一件取得した場合データが取得できないこと() {
        TResponse resCreateRelation = null;
        String boxname = "box1";
        try {
            // _Box.Name指定ありでRelation作成
            resCreateRelation = CellCtlUtils.createRelation(cellName, testRelationName, boxname);

            // _Box.Name指定なしでRelation取得
            Http.request("relation-retrieve-without-boxname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            if (resCreateRelation.getLocationHeader() != null) {
                deleteOdataResource(resCreateRelation.getLocationHeader());
            }
        }
    }

    /**
     * BoxNameを指定せずRelationを登録し、存在しないBoxNameを指定してRelationを一件取得した場合404が返却されること.
     */
    @Test
    public void BoxNameを指定せずRelationを登録し存在しないBoxNameを指定してRelationを一件取得した場合404が返却されること() {
        TResponse resCreateRelation = null;
        String boxname = "dummy";
        try {
            // _Box.Name指定ありでRelation作成
            resCreateRelation = CellCtlUtils.createRelation(cellName, testRelationName);

            // _Box.Name指定なしでRelation取得
            Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            if (resCreateRelation.getLocationHeader() != null) {
                deleteOdataResource(resCreateRelation.getLocationHeader());
            }
        }
    }

}
