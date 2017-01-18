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
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;

/**
 * UserData登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataCreateLinkTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataCreateLinkTest() {
        super();
    }

    /**
     * 長さ128文字のEntityTypeでユーザーデータの関連付けが作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 長さ128文字のEntityTypeでユーザーデータの関連付けが作成できること() {
        String entityTypeName127 = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567";
        entityTypeName = "a" + entityTypeName127;
        navPropName = "b" + entityTypeName127;
        String userDataId = "128EntityType";
        String linkUserDataId = "128LinkEntityType";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        JSONObject linkBody = new JSONObject();
        linkBody.put("__id", linkUserDataId);

        try {
            // 128文字のエンティティタイプを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                    Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                    Setup.TEST_ODATA, navPropName, HttpStatus.SC_CREATED);

            // AssociationEndを作成
            AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    "AssociationEnd", entityTypeName);
            AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    "LinkAssociationEnd", navPropName);

            // AssociationEndを関連付け
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityTypeName,
                    navPropName,
                    "AssociationEnd",
                    "LinkAssociationEnd", HttpStatus.SC_NO_CONTENT);

            // ユーザーデータを作成
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);
            createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, navPropName);

            // $links登録が成功することを確認する
            String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                    + Setup.TEST_ODATA + "/" + navPropName + "('" + linkUserDataId + "')";
            Http.request("link-userdata-userdata.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", entityTypeName + "('" + userDataId + "')")
                    .with("trgPath", navPropName)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("trgUserdataUrl", targetUri)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        } finally {
            // ユーザーデータLinkを削除
            deleteUserDataLinks(userDataId, linkUserDataId);

            // ユーザーデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, userDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    navPropName, linkUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

            // AssociationEndLinkを削除
            AssociationEndUtils
                    .deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                            Setup.TEST_BOX1, "Name='AssociationEnd',_EntityType.Name='" + entityTypeName + "'",
                            "Name='LinkAssociationEnd',_EntityType.Name='" + navPropName + "'",
                            HttpStatus.SC_NO_CONTENT);

            // AssociationEndを削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName, Setup.TEST_BOX1, "AssociationEnd", HttpStatus.SC_NO_CONTENT);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, navPropName, Setup.TEST_BOX1, "LinkAssociationEnd", HttpStatus.SC_NO_CONTENT);

            // エンティティタイプを削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                    "application/json", entityTypeName, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                    "application/json", navPropName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * ユーザデータ$link登録のテスト_uriに前括弧がない場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ$link登録のテスト_uriに前括弧がない場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String targetEntityType = "Product";

        // 前準備
        try {
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, cellName,
                    boxName, colName, "Category");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, colName, targetEntityType);

            String targetUri = UrlUtils.cellRoot(cellName) + boxName + "/"
                    + colName + "/" + targetEntityType + "'userdata001')";
            // $links登録(リクエストボディuriの丸かっこが後ろのみ)
            Http.request("link-userdata-userdata.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("colPath", colName)
                    .with("srcPath", "Category('userdata001')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("trgUserdataUrl", targetUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // ユーザデータの削除
            deleteUserData(userDataId);
            deleteUserData(cellName, boxName, colName, targetEntityType,
                    userDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータ$link登録のテスト_uriに後ろ括弧がない場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ$link登録のテスト_uriに後ろ括弧がない場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String targetEntityType = "Product";

        // 前準備
        try {
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, cellName,
                    boxName, colName, "Category");
            createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, colName, "Product");

            String targetUri = UrlUtils.cellRoot(cellName) + boxName + "/"
                    + colName + "/" + targetEntityType + "('userdata001'";
            // $links登録(リクエストボディuriの丸かっこが前のみ)
            Http.request("link-userdata-userdata.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("colPath", colName)
                    .with("srcPath", "Category('userdata001')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("trgUserdataUrl", targetUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            // ユーザデータの削除
            deleteUserData(userDataId);
            deleteUserData(cellName, boxName, colName, targetEntityType,
                    userDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータの$link更新で400エラーになること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータの$link更新で400エラーになること() {
        entityTypeName = "entity1";
        navPropName = "entity2";
        String userDataId = "et1Userdata";
        String linkUserDataId = "et2Userdata";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        JSONObject linkBody = new JSONObject();
        linkBody.put("__id", linkUserDataId);
        String linkPath = Setup.TEST_BOX1 + "/" + Setup.TEST_ODATA + "/"
                + entityTypeName + "\\('" + userDataId + "'\\)" + "/\\$links/_" + navPropName;
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + navPropName + "('" + linkUserDataId + "')";

        try {
            // 128文字のエンティティタイプを作成
            EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                    Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);
            EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                    Setup.TEST_ODATA, navPropName, HttpStatus.SC_CREATED);

            // AssociationEndを作成
            AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    "AssociationEnd", entityTypeName);
            AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED,
                    "LinkAssociationEnd", navPropName);

            // AssociationEndを関連付け
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityTypeName,
                    navPropName,
                    "AssociationEnd",
                    "LinkAssociationEnd", HttpStatus.SC_NO_CONTENT);

            // ユーザーデータを作成
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);
            createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, navPropName);

            // IdありRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + targetUri + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            // ユーザーデータLinkを削除
            // deleteUserDataLinks(userDataId, linkUserDataId);

            // ユーザーデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, userDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    navPropName, linkUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

            // AssociationEndLinkを削除
            AssociationEndUtils
                    .deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                            Setup.TEST_BOX1, "Name='AssociationEnd',_EntityType.Name='" + entityTypeName + "'",
                            "Name='LinkAssociationEnd',_EntityType.Name='" + navPropName + "'",
                            HttpStatus.SC_NO_CONTENT);

            // AssociationEndを削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, entityTypeName, Setup.TEST_BOX1, "AssociationEnd", HttpStatus.SC_NO_CONTENT);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    Setup.TEST_ODATA, navPropName, Setup.TEST_BOX1, "LinkAssociationEnd", HttpStatus.SC_NO_CONTENT);

            // エンティティタイプを削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                    "application/json", entityTypeName, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                    "application/json", navPropName, Setup.TEST_CELL1, -1);
        }
    }
}
