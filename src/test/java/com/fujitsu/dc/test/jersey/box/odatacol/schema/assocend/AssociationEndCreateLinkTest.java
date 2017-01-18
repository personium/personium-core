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

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmMultiplicity;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * AssociationEnd登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndCreateLinkTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public AssociationEndCreateLinkTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * AssociationEndのlinkを作成してレスポンスコードが204であること.
     */
    @Test
    public final void AssociationEndのlinkを作成してレスポンスコードが204であること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";
        String key = "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'";
        String navKey = "Name='" + linkName + "',_EntityType.Name='" + linkEntityTypeName + "'";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            TResponse response = Http.request("box/associationEnd-createLink.txt")
                    .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("linkEntityTypeName", linkEntityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("linkName", linkName)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);
        } finally {
            // AssociationEndのlink解除
            AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                    Setup.TEST_BOX1, key, navKey, -1);
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndのlink作成時にuriを空文字にすると400になること.
     */
    @Test
    public final void AssociationEndのlink作成時にuriを空文字にすると400になること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            Http.request("box/associationEnd-createLinkWithBody.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("uri", "")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndのlink作成時にuriをuri形式以外にすると400になること.
     */
    @Test
    public final void AssociationEndのlink作成時にuriをuri形式以外にすると400になること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            Http.request("box/associationEnd-createLinkWithBody.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("uri", "noturi")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndのlink作成時uriの値に前丸カッコがない場合400になること.
     */
    @Test
    public final void AssociationEndのlink作成時uriの値に前丸カッコがない場合400になること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1)
                    + Setup.TEST_BOX1 + "/" + Setup.TEST_ODATA
                    + "/\\$metadata/AssociationEndName='AssoEnd',_EntityType.Name='EntityType')";
            Http.request("box/associationEnd-createLinkWithBody.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("uri", targetUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndのlink作成時uriの値に後ろ丸カッコがない場合400になること.
     */
    @Test
    public final void AssociationEndのlink作成時uriの値に後ろ丸カッコがない場合400になること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1)
                    + Setup.TEST_BOX1 + "/" + Setup.TEST_ODATA
                    + "/\\$metadata/AssociationEnd(Name='AssoEnd',_EntityType.Name='EntityType'";
            Http.request("box/associationEnd-createLinkWithBody.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("uri", targetUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndのlink作成時にuriをnullにすると400になること.
     */
    @Test
    public final void AssociationEndのlink作成時にuriをnullにすると400になること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            Http.request("box/associationEnd-createLinkWithNull.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndのURLに存在しないEntityType名を指定してlinkを作成した場合レスポンスコードが404であること.
     */
    @Test
    public final void AssociationEndのURLに存在しないEntityType名を指定してlinkを作成した場合レスポンスコードが404であること() {
        String entityTypeName = "dummy";
        String linkEntityTypeName = "Product";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        try {
            // AssociationEndの作成
            createAssociationEnd(linkName, linkEntityTypeName);

            TResponse response = Http.request("box/associationEnd-createLink.txt")
                    .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("linkEntityTypeName", linkEntityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("linkName", linkName)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .debug();

            // メッセージチェック
            ODataCommon.checkErrorResponseBody(response,
                    DcCoreException.OData.NOT_FOUND.getCode(),
                    DcCoreException.OData.NOT_FOUND.getMessage());
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndのボディに存在しないEntityType名を指定してlinkを作成した場合レスポンスコードが400であること.
     */
    @Test
    public final void AssociationEndのボディに存在しないEntityType名を指定してlinkを作成した場合レスポンスコードが400であること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "dummy";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);

            TResponse response = Http.request("box/associationEnd-createLink.txt")
                    .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("linkEntityTypeName", linkEntityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("linkName", linkName)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

            // メッセージチェック
            ODataCommon.checkErrorResponseBody(response,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri").getMessage());
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
        }
    }

    /**
     * AssociationEndの1対1link作成時400になること.
     */
    @Test
    public final void AssociationEndの1対1link作成時400になること() {

        String entityTypeName1 = "entityTypeName1";
        String entityTypeName2 = "entityTypeName2";
        String associationEndName1 = "associationEndName1";
        String associationEndName2 = "associationEndName2";
        try {
            // EntityTypeを2つ作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName1, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    entityTypeName2, HttpStatus.SC_CREATED);

            // それぞれにAssociationEndを作成する
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName1, entityTypeName1);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, EdmMultiplicity.ONE.getSymbolString(),
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    associationEndName2, entityTypeName2);

            // AssociationEnd同士を$linksする
            TResponse response = Http.request("box/associationEnd-createLink.txt")
                    .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName1)
                    .with("linkEntityTypeName", entityTypeName2)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", associationEndName1)
                    .with("linkName", associationEndName2)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

            // メッセージチェック
            ODataCommon.checkErrorResponseBody(response,
                    DcCoreException.OData.INVALID_MULTIPLICITY.getCode(),
                    DcCoreException.OData.INVALID_MULTIPLICITY.getMessage());

        } finally {
            // AssociationEnd削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName1, Setup.TEST_BOX1, associationEndName1, HttpStatus.SC_NO_CONTENT);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName2, Setup.TEST_BOX1, associationEndName2, -1);

            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName1, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, "application/xml",
                    entityTypeName2, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndのlink作成時URLのNP名とボディのエンティティ名が異なる場合に400となること.
     */
    @Test
    public final void AssociationEndのlink作成時URLのNP名とボディのエンティティ名が異なる場合に400となること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";
        String uri = null;

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            uri = UrlUtils.cellRoot(Setup.TEST_CELL1) + "box1/" + Setup.TEST_ODATA
                    + "/\\$metadata/EntityType('" + entityTypeName + "')";

            Http.request("box/associationEnd-createLinkWithBody.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .with("uri", uri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

            // $linksの一覧取得して500エラーとならないことを確認
            Http.request("box/associationEnd-listLink.txt")
                    .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndとEntityTypeのlinkを作成した場合400が返却されること.
     */
    @Test
    public final void AssociationEndとEntityTypeのlinkを作成した場合400が返却されること() {
        String entityTypeName = "Product";
        String name = "AssoEnd";
        String uri = null;

        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);

            // AssociationEnd - EntityType $links作成
            uri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/" + Setup.TEST_ODATA
                    + "/\\$metadata/EntityType('" + entityTypeName + "')";
            Http.request("box/odatacol/schema/create-boxlevel-link-WithBody.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", "AssociationEnd")
                    .with("id", "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'")
                    .with("navProp", "_EntityType")
                    .with("token", MASTER_TOKEN_NAME)
                    .with("uri", uri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

            // EntityType - AssociationEnd $links作成
            uri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/" + Setup.TEST_ODATA
                    + "/\\$metadata/AssociationEnd(Name='" + name + "',_EntityType.Name='" + entityTypeName + "')";
            Http.request("box/odatacol/schema/create-boxlevel-link-WithBody.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", "EntityType")
                    .with("id", "'" + entityTypeName + "'")
                    .with("navProp", "_AssociationEnd")
                    .with("token", MASTER_TOKEN_NAME)
                    .with("uri", uri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
        }
    }

    /**
     * AssociationEndのlink作成時既に同じ関連がある場合409となること.
     * @throws ParseException パースエラー
     */
    @Test
    public final void AssociationEndのlink作成時既に同じ関連がある場合409となること() throws ParseException {
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
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity2of1, entityType2);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity2of2, entityType2);

            // $links登録
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityType1, entityType2, assocEntity1of1, assocEntity2of1, HttpStatus.SC_NO_CONTENT);

            // $links登録（2個め）
            TResponse res = AssociationEndUtils.createLink(
                    AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, entityType2, assocEntity1of2, assocEntity2of2, HttpStatus.SC_CONFLICT);
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

    /**
     * AssociationEndのlink作成時同一EntityTypeから複数のEntityTypeに関連づけられること.
     * @throws ParseException パースエラー
     */
    @Test
    public final void AssociationEndのlink作成時同一EntityTypeから複数のEntityTypeに関連づけられること() throws ParseException {
        // 例）
        // entity1 ---------------------------------- entity2
        // associationEnd1-1 $links associationEnd2-1
        //
        // entity1 ---------------------------------- entity3
        // associationEnd1-2 $links associationEnd3-1
        //

        String entityType1 = "AssociationEndTestEntity1";
        String entityType2 = "AssociationEndTestEntity2";
        String entityType3 = "AssociationEndTestEntity3";

        String assocEntity1of1 = "AssociationEndTestEntity1-1";
        String assocEntity1of2 = "AssociationEndTestEntity1-2";
        String assocEntity2of1 = "AssociationEndTestEntity2-1";
        String assocEntity3of1 = "AssociationEndTestEntity3-1";

        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType2, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType3, HttpStatus.SC_CREATED);

            // AssociationEndの作成
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity1of1, entityType1);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity1of2, entityType1);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity2of1, entityType2);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, assocEntity3of1, entityType3);

            // $links登録
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityType1, entityType2, assocEntity1of1, assocEntity2of1, HttpStatus.SC_NO_CONTENT);

            // $links登録（2個め）
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityType1, entityType3, assocEntity1of2, assocEntity3of1, HttpStatus.SC_NO_CONTENT);

            // UserOData登録
            JSONObject body1 = (JSONObject) new JSONParser().parse("{\"__id\":\"id\", \"name\":\"pochi\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType1);

            // UserODataNP経由登録
            JSONObject body2 = (JSONObject) new JSONParser().parse("{\"__id\":\"id\", \"name\":\"tama\"}");
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME, body2, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, "id", entityType2, HttpStatus.SC_CREATED);

            // UserODataNP経由登録
            JSONObject body3 = (JSONObject) new JSONParser().parse("{\"__id\":\"id\", \"name\":\"tama\"}");
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME, body3, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, "id", entityType3, HttpStatus.SC_CREATED);

        } finally {
            // UserODataのlink削除
            UserDataUtils.deleteLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, "id", entityType2, "id", -1);
            UserDataUtils.deleteLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType1, "id", entityType3, "id", -1);

            // UserOData削除
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType1, "id");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType2, "id");
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityType3, "id");

            // AssociationEndのlink削除
            AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                    Setup.TEST_BOX1, "Name='" + assocEntity1of1 + "',_EntityType.Name='" + entityType1 + "'",
                    "Name='" + assocEntity2of1 + "',_EntityType.Name='" + entityType2 + "'", -1);
            AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                    Setup.TEST_BOX1, "Name='" + assocEntity1of2 + "',_EntityType.Name='" + entityType1 + "'",
                    "Name='" + assocEntity3of1 + "',_EntityType.Name='" + entityType3 + "'", -1);

            // AssociationEndの削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType1,
                    Setup.TEST_BOX1, assocEntity1of1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType1,
                    Setup.TEST_BOX1, assocEntity1of2, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType2,
                    Setup.TEST_BOX1, assocEntity2of1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA, entityType3,
                    Setup.TEST_BOX1, assocEntity3of1, -1);

            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON,
                    entityType1, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON,
                    entityType2, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON,
                    entityType3, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * AssociationEndを新規作成.
     */
    private void createAssociationEnd(String name, String entityTypeName) {
        Http.request("box/odatacol/schema/assocend/create.txt").with("cell", "testcell1").with("box", "box1")
                .with("collection", "setodata").with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON).with("token", DcCoreConfig.getMasterToken())
                .with("name", name).with("multiplicity", EdmMultiplicity.MANY.getSymbolString())
                .with("entityTypeName", entityTypeName).returns().statusCode(HttpStatus.SC_CREATED).debug();
    }

    /**
     * AssociationEndを削除する.
     */
    private void deleteAssociationEnd(String name, String entityTypeName) {
        Http.request("box/odatacol/schema/assocend/delete.txt").with("cell", "testcell1").with("box", "box1")
                .with("collection", "setodata").with("token", DcCoreConfig.getMasterToken()).with("name", name)
                .with("entityTypeName", entityTypeName).with("ifMatch", "*").returns()
                .statusCode(HttpStatus.SC_NO_CONTENT).debug();
    }

}
