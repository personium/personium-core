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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmMultiplicity;

import com.fujitsu.dc.core.DcCoreConfig;
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

/**
 * AssociationEndの$ink削除のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndDeleteLinkTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public AssociationEndDeleteLinkTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * AssociationEndのlinkを削除してレスポンスコードが204であること.
     */
    @Test
    public final void AssociationEndのlinkを削除してレスポンスコードが204であること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";
        String key = "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'";
        String navKey = "Name='" + linkName + "',_EntityType.Name='" + linkEntityTypeName + "'";

        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            createLink(entityTypeName, linkEntityTypeName, name, linkName);

            TResponse response = Http.request("box/associationEnd-deleteLink.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("key", key)
                    .with("navKey", navKey)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

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
     * AssociationEndに存在しないEntityType名を指定してlinkを削除してレスポンスコードが404であること.
     */
    @Test
    public final void AssociationEndに存在しないEntityType名を指定してlinkを削除してレスポンスコードが404であること() {
        String dummyEntity = "dummy";
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";
        String key = "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'";
        String navKey = "Name='" + linkName + "',_EntityType.Name='" + linkEntityTypeName + "'";
        String dummyKey = "Name='" + name + "',_EntityType.Name='" + dummyEntity + "'";

        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            createLink(entityTypeName, linkEntityTypeName, name, linkName);

            // リンク元に存在しないEntityType名を指定
            Http.request("box/associationEnd-deleteLink.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("key", dummyKey)
                    .with("navKey", navKey)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

            // リンク先に存在しないEntityType名を指定
            Http.request("box/associationEnd-deleteLink.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("key", key)
                    .with("navKey", dummyKey)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

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
     * AssociationEndとEntityTypeのLink削除で400になること.
     */
    @Test
    public final void AssociationEndとEntityTypeのLink削除で400になること() {
        String entityTypeName = "Product";
        String name = "AssoEnd";
        String key = "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'";

        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);

            // AssociationとEntityTypeの$links削除(400)
            Http.request("box/odatacol/schema/delete-boxlevel-link.txt")
                    .with("method", HttpMethod.DELETE)
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", "AssociationEnd")
                    .with("id", key)
                    .with("navProp", "_EntityType")
                    .with("navKey", "'" + entityTypeName + "'")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("ifMatch", "*")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            // EntityTypeとAssociationの$links削除(400)
            Http.request("box/odatacol/schema/delete-boxlevel-link.txt")
                    .with("method", HttpMethod.DELETE)
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", "EntityType")
                    .with("id", "'" + entityTypeName + "'")
                    .with("navProp", "_AssociationEnd")
                    .with("navKey", key)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("ifMatch", "*")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, name, entityTypeName));
        }
    }

    /**
     * 関連付けのないN対NのAssociationEndを$linksで削除すると404が返却されること.
     */
    @Test
    public final void 関連付けのないN対NのAssociationEndを$linksで削除すると404が返却されること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();

        // 事前データの準備
        try {
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity01", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity02", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity03", HttpStatus.SC_CREATED);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, multiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc01", "assocTestEntity01");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, multiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc02", "assocTestEntity02");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, multiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc03", "assocTestEntity03");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box, col, "assocTestEntity01",
                    "assocTestEntity02", "assoc01",
                    "assoc02", HttpStatus.SC_NO_CONTENT);

            // 関連付けのないAssociationEndを$linksで削除して404が返却されること
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity03',Name='assoc03'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, HttpStatus.SC_NOT_FOUND);
        } finally {
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity02',Name='assoc02'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity01", box, "assoc01",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity02", box, "assoc02",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity03", box, "assoc03",
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity01", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity02", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity03", box, cell,
                    -1);

        }
    }

    /**
     * 関連付けのない0_1対0_1のAssociationEndを$linksで削除すると404が返却されること.
     */
    @Test
    public final void 関連付けのない0_1対0_1のAssociationEndを$linksで削除すると404が返却されること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String multiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();

        // 事前データの準備
        try {
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity01", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity02", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity03", HttpStatus.SC_CREATED);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, multiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc01", "assocTestEntity01");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, multiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc02", "assocTestEntity02");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, multiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc03", "assocTestEntity03");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box, col, "assocTestEntity01",
                    "assocTestEntity02",
                    "assoc01",
                    "assoc02", HttpStatus.SC_NO_CONTENT);

            // 関連付けのないAssociationEndを$linksで削除して404が返却されること
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity03',Name='assoc03'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, HttpStatus.SC_NOT_FOUND);
        } finally {
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity02',Name='assoc02'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity01", box, "assoc01",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity02", box, "assoc02",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity03", box, "assoc03",
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity01", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity02", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity03", box, cell,
                    -1);

        }
    }

    /**
     * 関連付けのない0_1対NのAssociationEndを$linksで削除すると404が返却されること.
     */
    @Test
    public final void 関連付けのない0_1対NのAssociationEndを$linksで削除すると404が返却されること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String oneZeroMultiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();
        String nMultiplicity = EdmMultiplicity.MANY.getSymbolString();

        // 事前データの準備
        try {
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity01", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity02", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity03", HttpStatus.SC_CREATED);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, oneZeroMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc01", "assocTestEntity01");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, nMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc02", "assocTestEntity02");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, nMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc03", "assocTestEntity03");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box, col, "assocTestEntity01",
                    "assocTestEntity02",
                    "assoc01",
                    "assoc02", HttpStatus.SC_NO_CONTENT);

            // 関連付けのないAssociationEndを$linksで削除して404が返却されること
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity03',Name='assoc03'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, HttpStatus.SC_NOT_FOUND);
        } finally {
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity02',Name='assoc02'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity01", box, "assoc01",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity02", box, "assoc02",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity03", box, "assoc03",
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity01", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity02", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity03", box, cell,
                    -1);

        }
    }

    /**
     * 関連付けのないN対1のAssociationEndを$linksで削除すると404が返却されること.
     */
    @Test
    public final void 関連付けのないN対0_1のAssociationEndを$linksで削除すると404が返却されること() {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String oneZeroMultiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();
        String nMultiplicity = EdmMultiplicity.MANY.getSymbolString();

        // 事前データの準備
        try {
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity01", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity02", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity03", HttpStatus.SC_CREATED);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, nMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc01", "assocTestEntity01");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, oneZeroMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc02", "assocTestEntity02");
            AssociationEndUtils.create(MASTER_TOKEN_NAME, oneZeroMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                    "assoc03", "assocTestEntity03");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box, col, "assocTestEntity01",
                    "assocTestEntity02",
                    "assoc01",
                    "assoc02", HttpStatus.SC_NO_CONTENT);

            // 関連付けのないAssociationEndを$linksで削除して404が返却されること
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity03',Name='assoc03'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, HttpStatus.SC_NOT_FOUND);
        } finally {
            String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
            String navKey = "_EntityType.Name='assocTestEntity02',Name='assoc02'";
            AssociationEndUtils.deleteLink(cell, col, box, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity01", box, "assoc01",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity02", box, "assoc02",
                    -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity03", box, "assoc03",
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity01", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity02", box, cell,
                    -1);
            EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity03", box, cell,
                    -1);

        }
    }

    private TResponse createLink(String entityTypeName, String linkEntityTypeName, String name, String linkName) {
        return Http.request("box/associationEnd-createLink.txt")
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
