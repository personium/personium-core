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

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmMultiplicity;

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

/**
 * AssociationEnd更新のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndUpdateTest extends ODataCommon {

    /**
     * コンストラクタ.
     */
    public AssociationEndUpdateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * AssociationEndの名前を変更できること.
     */
    @Test
    public final void AssociationEndの名前を変更できること() {

        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する_Nameのみ（204となること）
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1, HttpStatus.SC_NO_CONTENT);

            // 更新後のAssociationEndを取得できること
            AssociationEndUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, reName,
                    entityTypeName1, HttpStatus.SC_OK);
            // 更新前のAssociationEndを取得できないこと
            AssociationEndUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, reName,
                    associationEndName1, HttpStatus.SC_NOT_FOUND);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの多重度を変更できないこと.
     */
    @Test
    public final void AssociationEndの多重度を変更できないこと() {

        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(多重度の変更があるため400エラー)
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.MANY.getSymbolString(), entityTypeName1, HttpStatus.SC_BAD_REQUEST);
            String message = String.format("AssociationEnd 'Multiplicity' change from [%s] to [%s]",
                    EdmMultiplicity.ONE.getSymbolString(), EdmMultiplicity.MANY.getSymbolString());
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.params(message).getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの関係対象のEntityType名を変更できないこと.
     */
    @Test
    public final void AssociationEndの関係対象のEntityType名を変更できないこと() {

        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "entityTypeName2";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName2, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(関係対象のEntityType名の変更があるため400エラー)
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2, HttpStatus.SC_BAD_REQUEST);
            String message = "AssociationEnd '_EntityType.Name' change";
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.getCode(),
                    DcCoreException.OData.OPERATION_NOT_SUPPORTED.params(message).getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName2, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新リクエストのボディがNameだけであるときに更新できないこと.
     */
    @Test
    public final void AssociationEndの更新リクエストのボディがNameだけであるときに更新できないこと() {

        // 更新時の必須項目(Multiplicity,_EntityType.Name)がないためエラーとなることを確認する
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(リクエストボディに必須項目の指定がないため400エラー)
            String body = "{\"Name\":\"" + reName + "\"}";
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    body, HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Multiplicity").getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新リクエストのボディが空であるときに更新できないこと.
     */
    @Test
    public final void AssociationEndの更新リクエストのボディが空であるときに更新できないこと() {

        // 更新時の必須項目(Name,Multiplicity,_EntityType.Name)がないためエラーとなることを確認する
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(リクエストボディに必須項目の指定がないため400エラー)
            String body = "{}";
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    body, HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Name").getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新リクエストのボディにMultiplicityが存在しない場合更新できないこと.
     */
    @Test
    public final void AssociationEndの更新リクエストのボディにMultiplicityが存在しない場合更新できないこと() {

        // 更新時の必須項目(Multiplicity)がないためエラーとなることを確認する
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(リクエストボディに必須項目の指定がないため400エラー)
            String body = "{\"Name\":\"" + reName + "\",\"_EntityType.Name\":\"" + associationEndName1 + "\"}";
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    body, HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Multiplicity").getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新リクエストのボディに関係対象のEntityType名が存在しない場合更新できないこと.
     */
    @Test
    public final void AssociationEndの更新リクエストのボディに関係対象のEntityType名が存在しない場合更新できないこと() {

        // 更新時の必須項目(_EntityType.Name)がないためエラーとなることを確認する
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(リクエストボディに必須項目の指定がないため400エラー)
            String body = "{\"Name\":\"" + reName + "\","
                    + "\"Multiplicity\":\"" + EdmMultiplicity.ONE.getSymbolString() + "\"}";
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    body, HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("_EntityType.Name").getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 変名後のAssociationEndが存在する場合409になること.
     */
    @Test
    public final void 変名後のAssociationEndが存在する場合409になること() {

        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String associationEndName2 = "associationEndName2";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName2, entityTypeName1);

            // AssociationEndを更新する（変名後のAssociationEndがすでに存在するため409となること）
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    associationEndName2, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_CONFLICT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName2, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 同じ名前でAssociationEndを更新できること.
     */
    @Test
    public final void 同じ名前でAssociationEndを更新できること() {

        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndName1";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する（同じ名前で更新できること）
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新リクエストのボディに不正な項目が存在する場合更新できないこと.
     */
    @Test
    public final void AssociationEndの更新リクエストのボディに不正な項目が存在する場合更新できないこと() {

        // リクエストボディに不正な項目(Type)が指定された場合
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(リクエストボディに不正な情報が存在するため400エラー)
            String body = "{\"Name\":\"" + reName + "\",\"Multiplicity\":\"" + EdmMultiplicity.ONE.getSymbolString()
                    + "\",\"_EntityType.Name\":\"" + associationEndName1 + "\",\"Type\":\"Edm.String\"}";
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    body, HttpStatus.SC_BAD_REQUEST);
            String message = "unknown property Type for AssociationEnd";
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.FIELED_INVALID_ERROR.getCode(),
                    DcCoreException.OData.FIELED_INVALID_ERROR.params(message).getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新リクエストのボディに管理情報が存在する場合更新できないこと.
     */
    @Test
    public final void AssociationEndの更新リクエストのボディに管理情報が存在する場合更新できないこと() {

        // リクエストボディに不正な項目(管理情報)が指定された場合
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";
        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する(リクエストボディに管理情報が存在するため400エラー)
            String body = "{\"Name\":\"" + reName + "\",\"Multiplicity\":\"" + EdmMultiplicity.ONE.getSymbolString()
                    + "\",\"_EntityType.Name\":\"" + associationEndName1 + "\",\"" + PUBLISHED + "\":\"/Date(0)/\"}";
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    body, HttpStatus.SC_BAD_REQUEST);
            String message = "__published is management information name. Cannot request.";
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.FIELED_INVALID_ERROR.getCode(),
                    DcCoreException.OData.FIELED_INVALID_ERROR.params(message).getMessage());
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Nameに空文字を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Nameに空文字を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Nameにアンダーバー始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Nameにアンダーバー始まりの文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "_associationEndName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }

    }

    /**
     * AssociationEndの更新時Nameにハイフン始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Nameにハイフン始まりの文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "-associationEndName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }

    }

    /**
     * AssociationEndの更新時Nameにスラッシュを含む文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Nameにスラッシュを含む文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "association/EndName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Nameに指定可能な文字数の最小値を指定した場合204となること.
     */
    @Test
    public final void AssociationEndの更新時Nameに指定可能な文字数の最小値を指定した場合204となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }

    }

    /**
     * AssociationEndの更新時Nameに指定可能な文字数の最大値を指定した場合204となること.
     */
    @Test
    public final void AssociationEndの更新時Nameに指定可能な文字数の最大値を指定した場合204となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Nameに指定可能な文字数の最大値をオーバー指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Nameに指定可能な文字数の最大値をオーバー指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Nameに日本語を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Nameに日本語を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "日本語";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Multiplicityに0から1を指定した場合204となること.
     */
    @Test
    public final void AssociationEndの更新時Multiplicityに0から1を指定した場合204となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ZERO_TO_ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ZERO_TO_ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }

    }

    /**
     * AssociationEndの更新時Multiplicityに1を指定した場合204となること.
     */
    @Test
    public final void AssociationEndの更新時Multiplicityに1を指定した場合204となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }

    }

    /**
     * AssociationEndの更新時Multiplicityにアスタリスクを指定した場合204となること.
     */
    @Test
    public final void AssociationEndの更新時Multiplicityにアスタリスクを指定した場合204となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.MANY.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.MANY.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Multiplicityに空文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Multiplicityに空文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, "", entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Multiplicity").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時Multiplicityに不正な文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時Multiplicityに不正な文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, "0", entityTypeName1,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Multiplicity").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }

    }

    /**
     * AssociationEndの更新時_EntityType.Nameに空文字を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時EntityType名に空文字を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("_EntityType.Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時_EntityType.Nameにアンダーバー始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時EntityType名にアンダーバー始まりの文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "_entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("_EntityType.Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時_EntityType.Nameにハイフン始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時EntityType名にハイフン始まりの文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "-entityTypeName1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("_EntityType.Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時_EntityType.Nameにスラッシュを含む文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時EntityType名にスラッシュを含む文字列を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "entityType/Name1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("_EntityType.Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時_EntityType.Nameに指定可能な文字数の最小値を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの更新時EntityType名に指定可能な文字数の最小値を指定した場合201となること() {
        String entityTypeName1 = "1";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時_EntityType.Nameに指定可能な文字数の最大値を指定した場合204となること.
     */
    @Test
    public final void AssociationEndの更新時_EntityTypeNameに指定可能な文字数の最大値を指定した場合204となること() {
        String entityTypeName1 = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName1,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時_EntityType.Nameに指定可能な文字数の最大値をオーバー指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時EntityType名に指定可能な文字数の最大値をオーバー指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("_EntityType.Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndの更新時_EntityType.Nameに日本語を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの更新時EntityType名に日本語を指定した場合400となること() {
        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "日本語";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("_EntityType.Name").getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndを存在しないEntityType名を指定して作成し400が返却されること.
     */
    @Test
    public final void AssociationEndを存在しないEntityType名を指定して作成し400が返却されること() {
        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "invalidEntityTypeName";
        String associationEndName1 = "associationEndName1";
        String reName = "associationEndReName1";

        try {
            // EntityTypeを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);

            // AssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);

            // AssociationEndを更新する
            TResponse res = AssociationEndUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName1, Setup.TEST_BOX1, associationEndName1,
                    reName, EdmMultiplicity.ONE.getSymbolString(), entityTypeName2,
                    HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(
                    res,
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(entityTypeName2).getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, reName, -1);
            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
        }
    }

}
