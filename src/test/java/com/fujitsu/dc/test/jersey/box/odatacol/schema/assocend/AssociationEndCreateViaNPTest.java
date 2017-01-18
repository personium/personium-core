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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.assocend;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * AssociationEnd NP経由登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndCreateViaNPTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public AssociationEndCreateViaNPTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * EntityTypeからNP経由でAssociationEndを登録できること.
     * @throws ParseException パースエラー
     */
    @Test
    public final void EntityTypeからNP経由でAssociationEndを登録できること() throws ParseException {
        String entityType1 = "AssociationEndTestEntity1";

        String assocEntity1of1 = "AssociationEndTestEntity1-1";

        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, HttpStatus.SC_CREATED);

            // AssociationEnd NP経由登録
            AssociationEndUtils.createViaEntityTypeNP(
                    MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    assocEntity1of1, "*", entityType1, HttpStatus.SC_CREATED);

            // AssociationEndの EntityTypeからのNP経由一覧取得
            TResponse res = AssociationEndUtils.listViaAssociationEndNP(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, "EntityType", entityType1, HttpStatus.SC_OK);
            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
            JSONObject body = (JSONObject) results.get(0);
            assertEquals(assocEntity1of1, body.get("Name"));

        } finally {
            // AssociationEndの削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType1,
                    Setup.TEST_BOX1, assocEntity1of1, -1);

            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON,
                    entityType1, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
        }
    }


    /**
     * AssociationEndのNP経由登録時既に同じ関連がある場合409となること.
     * @throws ParseException パースエラー
     */
    @Test
    public final void AssociationEndのNP経由登録時既に同じ関連がある場合409となること() throws ParseException {
        // 例）
        // entity1 ---------------------------------- entity2
        // associationEnd1-1 $links associationEnd2-1
        //
        // entity1 ---------------------------------- entity2
        // associationEnd1-2 $links associationEnd2-2
        //

        String entityType1 = "AssociationEndTestEntity1";
        String entityType2 = "AssociationEndTestEntity2";

        String assocEntity1of1 = "AssociationEndTestEntity1-1";
        String assocEntity1of2 = "AssociationEndTestEntity1-2";
        String assocEntity2of1 = "AssociationEndTestEntity2-1";
        String assocEntity2of2 = "AssociationEndTestEntity2-2";

        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType2, HttpStatus.SC_CREATED);

            // AssociationEndの作成
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity1of1, entityType1);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity1of2, entityType1);

            // AssociationEnd NP経由登録
            AssociationEndUtils.createViaNP(
                    MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    assocEntity1of1, entityType1,
                    assocEntity2of1, "*", entityType2, HttpStatus.SC_CREATED);

            // AssociationEnd NP経由登録
            TResponse res = AssociationEndUtils.createViaNP(
                    MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    assocEntity1of2, entityType1,
                    assocEntity2of2, "*", entityType2, HttpStatus.SC_CONFLICT);
            ODataCommon.checkErrorResponseBody(res,
                    DcCoreException.OData.CONFLICT_DUPLICATED_ENTITY_RELATION.getCode(),
                    DcCoreException.OData.CONFLICT_DUPLICATED_ENTITY_RELATION.getMessage());

            // UserOData登録
            JSONObject body1 = (JSONObject) new JSONParser().parse("{\"__id\":\"id\", \"name\":\"pochi\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType1);

            // UserODataNP経由登録
            JSONObject body2 = (JSONObject) new JSONParser().parse("{\"__id\":\"id\", \"name\":\"tama\"}");
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME, body2, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, "id", entityType2, HttpStatus.SC_CREATED);

        } finally {
            // UserODataのlink削除
            UserDataUtils.deleteLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, "id", entityType2, "id", -1);

            // UserOData削除
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType1, "id");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType2, "id");

            // AssociationEndのlink削除
            AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                    Setup.TEST_BOX1, "Name='" + assocEntity1of1 + "',_EntityType.Name='" + entityType1 + "'",
                    "Name='" + assocEntity2of1 + "',_EntityType.Name='" + entityType2 + "'", -1);
            AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                    Setup.TEST_BOX1, "Name='" + assocEntity1of2 + "',_EntityType.Name='" + entityType1 + "'",
                    "Name='" + assocEntity2of2 + "',_EntityType.Name='" + entityType2 + "'", -1);

            // AssociationEndの削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType1,
                    Setup.TEST_BOX1, assocEntity1of1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType1,
                    Setup.TEST_BOX1, assocEntity1of2, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType2,
                    Setup.TEST_BOX1, assocEntity2of1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType2,
                    Setup.TEST_BOX1, assocEntity2of2, -1);

            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON,
                    entityType1, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON,
                    entityType2, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
        }
    }
}
