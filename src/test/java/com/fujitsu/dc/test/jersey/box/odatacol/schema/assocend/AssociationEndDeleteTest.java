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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.edm.EdmMultiplicity;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * AssociationEnd登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndDeleteTest extends AbstractCase {

    private static final String ASSOCIATION_END_NAME = "product-category";

    private static final String ENTITY_TYPE_NAME = "Product";

    /**
     * コンストラクタ.
     */
    public AssociationEndDeleteTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * AssociationEndの削除のテスト.
     */
    @Test
    public final void AssociationEndの削除のテスト() {
        // AssociationEnd作成
        createAssociationEnd();

        // AssociationEndの削除
        TResponse res = Http.request("box/odatacol/schema/assocend/delete.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .debug();

        // レスポンスヘッダーのチェック
        // DataServiceVersion
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
    }

    /**
     * AssociationEndの削除_存在しないCellを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndの削除_存在しないCellを指定した場合のテスト() {
        // AssociationEndの削除
        TResponse res = Http.request("box/odatacol/schema/assocend/delete.txt")
                .with("cell", "dummyTestCell")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        res.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // レスポンスボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR404-DV-0003");
    }

    /**
     * AssociationEndの削除_存在しないBoxを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndの削除_存在しないBoxを指定した場合のテスト() {
        // AssociationEndの削除
        TResponse res = Http.request("box/odatacol/schema/assocend/delete.txt")
                .with("cell", "testcell1")
                .with("box", "test")
                .with("collection", "setodata")
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        res.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // レスポンスボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR404-DV-0002");
    }

    /**
     * AssociationEndの削除_存在しないODataCollectionを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndの削除_存在しないODataCollectionを指定した場合のテスト() {
        // AssociationEndの削除
        TResponse res = Http.request("box/odatacol/schema/assocend/delete.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "test")
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        res.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    /**
     * AssociationEndの削除_存在しないAssociationEndを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndの削除_存在しないアソシエーション名を指定した場合のテスト() {
        // AssociationEndの削除
        TResponse res = Http.request("box/odatacol/schema/assocend/delete.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", "test")
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        res.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // レスポンスボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * AssociationEndの削除_存在しないAssociationEndを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndの削除_存在しない関係対象のEntityType名を指定した場合のテスト() {
        // AssociationEndの削除
        TResponse res = Http.request("box/odatacol/schema/assocend/delete.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("entityTypeName", "test")
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        res.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // レスポンスボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * AssociationEndを作成する.
     */
    private void createAssociationEnd() {
        Http.request("box/odatacol/schema/assocend/create.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("multiplicity", EdmMultiplicity.MANY.getSymbolString())
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .returns()
                .statusCode(HttpStatus.SC_CREATED)
                .debug();
    }
}
