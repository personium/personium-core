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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.core.ODataConstants;
import org.odata4j.edm.EdmMultiplicity;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * AssociationEnd登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndCreateTest extends ODataCommon {

    private static final String ASSOCIATION_END_NAME = "product-category";

    private static final String ENTITY_TYPE_NAME = "Product";

    private static final String ASSOCIATION_END_TYPE = "ODataSvcSchema.AssociationEnd";
    /**
     * ロケーションヘッダーの期待値.
     */
    private String location;

    /**
     * コンストラクタ.
     */
    public AssociationEndCreateTest() {
        super("com.fujitsu.dc.core.rs");
        location = UrlUtils.associationEnd("testcell1", "box1", "setodata", ASSOCIATION_END_NAME, ENTITY_TYPE_NAME);
    }

    /**
     * AssociationEndを新規作成.
     */
    @Test
    public final void AssociationEndを新規作成() {
        TResponse response = Http.request("box/odatacol/schema/assocend/create.txt")
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

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Multiplicity", EdmMultiplicity.MANY.getSymbolString());
        additional.put("Name", ASSOCIATION_END_NAME);
        additional.put("_EntityType.Name", ENTITY_TYPE_NAME);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, ASSOCIATION_END_TYPE, additional);

        // AssociationEndの削除
        deleteAssociationEnd();
    }

    /**
     * AssociationEndを新規作成_存在しないCellを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndを新規作成_存在しないCellを指定した場合のテスト() {
        TResponse response = Http.request("box/odatacol/schema/assocend/create.txt")
                .with("cell", "dummyTestCell")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("multiplicity", EdmMultiplicity.MANY.getSymbolString())
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        response.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // レスポンスボディのチェック
        checkErrorResponse(response.bodyAsJson(), "PR404-DV-0003");
    }

    /**
     * AssociationEndを新規作成_存在しないBoxを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndを新規作成_存在しないBoxを指定した場合のテスト() {
        TResponse response = Http.request("box/odatacol/schema/assocend/create.txt")
                .with("cell", "testcell1")
                .with("box", "test")
                .with("collection", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("multiplicity", EdmMultiplicity.MANY.getSymbolString())
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        response.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // レスポンスボディのチェック
        checkErrorResponse(response.bodyAsJson(), "PR404-DV-0002");

    }

    /**
     * AssociationEndの新規作成_存在しないODataCollectionを指定した場合のテスト.
     */
    @Test
    public final void AssociationEndの新規作成_存在しないODataCollectionを指定した場合のテスト() {
        TResponse response = Http.request("box/odatacol/schema/assocend/create.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "test")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", ASSOCIATION_END_NAME)
                .with("multiplicity", EdmMultiplicity.MANY.getSymbolString())
                .with("entityTypeName", ENTITY_TYPE_NAME)
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        response.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    /**
     * AssociationEndを新規作成_同名のAssociationEndが登録されている場合のテスト.
     */
    @Test
    public final void AssociationEndを新規作成_同名のAssociationEndが登録されている場合のテスト() {
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

        TResponse response = Http.request("box/odatacol/schema/assocend/create.txt")
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
                .statusCode(HttpStatus.SC_CONFLICT)
                .debug();

        // レスポンスヘッダーのチェック
        // Content-Type
        response.checkHeader(ODataConstants.Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // AssociationEndの削除
        deleteAssociationEnd();
    }

    /**
     * AssociationEndの新規作成_リクエストボディに管理情報__publishedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void AssociationEndの新規作成_リクエストボディに管理情報__publishedを指定した場合400エラーとなること() {
        JSONObject body = new JSONObject();
        body.put("Name", ASSOCIATION_END_NAME);
        body.put("Multiplicity", EdmMultiplicity.MANY.getSymbolString());
        body.put("_EntityType.Name", ENTITY_TYPE_NAME);
        body.put(PUBLISHED, "/Date(0)/");

        Http.request("box/odatacol/schema/assocend/create-without-body.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * AssociationEndの新規作成_リクエストボディに管理情報__updatedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void AssociationEndの新規作成_リクエストボディに管理情報__updatedを指定した場合400エラーとなること() {
        JSONObject body = new JSONObject();
        body.put("Name", ASSOCIATION_END_NAME);
        body.put("Multiplicity", EdmMultiplicity.MANY.getSymbolString());
        body.put("_EntityType.Name", ENTITY_TYPE_NAME);
        body.put(UPDATED, "/Date(0)/");

        Http.request("box/odatacol/schema/assocend/create-without-body.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * AssociationEndの新規作成_リクエストボディに管理情報__metadataを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void AssociationEndの新規作成_リクエストボディに管理情報__metadataを指定した場合400エラーとなること() {
        JSONObject body = new JSONObject();
        body.put("Name", ASSOCIATION_END_NAME);
        body.put("Multiplicity", EdmMultiplicity.MANY.getSymbolString());
        body.put("_EntityType.Name", ENTITY_TYPE_NAME);
        body.put(METADATA, "test");

        Http.request("box/odatacol/schema/assocend/create-without-body.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * AssociationEndの新規作成時Nameに空文字を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameに空文字を指定した場合400となること() {
        String assocName = "";
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Nameにアンダーバー始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameにアンダーバー始まりの文字列を指定した場合400となること() {
        String assocName = "_" + ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Nameにハイフン始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameにハイフン始まりの文字列を指定した場合400となること() {
        String assocName = "-" + ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Nameにスラッシュを含む文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameにスラッシュを含む文字列を指定した場合400となること() {
        String assocName = "product/category";
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Nameに指定可能な文字数の最小値を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameに指定可能な文字数の最小値を指定した場合201となること() {
        String assocName = "1";
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Nameに指定可能な文字数の最大値を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameに指定可能な文字数の最大値を指定した場合201となること() {
        String assocName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Nameに指定可能な文字数の最大値をオーバー指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameに指定可能な文字数の最大値をオーバー指定した場合400となること() {
        String assocName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Nameに日本語を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Nameに日本語を指定した場合400となること() {
        String assocName = "日本語";
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Multiplicityに0..1を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの新規作成時Multiplicityに0から1を指定した場合201となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Multiplicityに1を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの新規作成時Multiplicityに1を指定した場合201となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.ONE.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Multiplicityに*を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの新規作成時Multiplicityにアスタリスクを指定した場合201となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Multiplicityに空文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Multiplicityに空文字列を指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = "";
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時Multiplicityに不正な文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時Multiplicityに不正な文字列を指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = "0";
        String entityTypeName = ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameに空文字を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名に空文字を指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "";
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameにアンダーバー始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名にアンダーバー始まりの文字列を指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "_" + ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameにハイフン始まりの文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名にハイフン始まりの文字列を指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "-" + ENTITY_TYPE_NAME;
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameにスラッシュを含む文字列を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名にスラッシュを含む文字列を指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "Product/";
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameに指定可能な文字数の最小値を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名に指定可能な文字数の最小値を指定した場合201となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "1";
        String locationHeader = null;
        String locationHeaderEntityType = null;

        try {
            TResponse resEntityType = createEntityType(entityTypeName);
            locationHeaderEntityType = resEntityType.getLocationHeader();

            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
            if (locationHeaderEntityType != null) {
                deleteOdataResource(locationHeaderEntityType);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameに指定可能な文字数の最大値を指定した場合201となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名に指定可能な文字数の最大値を指定した場合201となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String locationHeader = null;
        String locationHeaderEntityType = null;

        try {
            TResponse resEntityType = createEntityType(entityTypeName);
            locationHeaderEntityType = resEntityType.getLocationHeader();

            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
            if (locationHeaderEntityType != null) {
                deleteOdataResource(locationHeaderEntityType);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameに指定可能な文字数の最大値をオーバー指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名に指定可能な文字数の最大値をオーバー指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String locationHeader = null;
        String locationHeaderEntityType = null;

        try {
            TResponse resEntityType = createEntityType(entityTypeName);
            locationHeaderEntityType = resEntityType.getLocationHeader();

            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
            if (locationHeaderEntityType != null) {
                deleteOdataResource(locationHeaderEntityType);
            }
        }
    }

    /**
     * AssociationEndの新規作成時_EntityType.Nameに日本語を指定した場合400となること.
     */
    @Test
    public final void AssociationEndの新規作成時EntityType名に日本語を指定した場合400となること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "日本語";
        String locationHeader = null;

        try {
            TResponse res = createAssociationEnd(assocName, multiplicity, entityTypeName);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndを存在しないEntityType名を指定して作成し400が返却されること.
     */
    @Test
    public final void AssociationEndを存在しないEntityType名を指定して作成し400が返却されること() {
        String assocName = ASSOCIATION_END_NAME;
        String multiplicity = EdmMultiplicity.MANY.getSymbolString();
        String entityTypeName = "dummy";
        String locationHeader = null;

        try {
            TResponse res = Http.request("box/odatacol/schema/assocend/create.txt")
                    .with("cell", "testcell1")
                    .with("box", "box1")
                    .with("collection", "setodata")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("name", assocName)
                    .with("multiplicity", multiplicity)
                    .with("entityTypeName", entityTypeName)
                    .returns()
                    .debug();

            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);

            // メッセージチェック
            ODataCommon.checkErrorResponseBody(res,
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(entityTypeName).getMessage());

        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * AssociationEndを作成する.
     * @param entityTypeName EntityType名
     * @return レスポンス
     */
    private TResponse createEntityType(String entityTypeName) {
        return Http.request("box/entitySet-post.txt")
                .with("cellPath", "testcell1")
                .with("boxPath", "box1")
                .with("odataSvcPath", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + DcCoreConfig.getMasterToken())
                .with("Name", entityTypeName)
                .returns()
                .debug();
    }

    /**
     * AssociationEndを作成する.
     * @param assocName AssociationEndのName
     * @param multiplicity Multiplicity
     * @param entityTypeName EntityType名
     * @return レスポンス
     */
    private TResponse createAssociationEnd(String assocName, String multiplicity, String entityTypeName) {
        return Http.request("box/odatacol/schema/assocend/create.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", assocName)
                .with("multiplicity", multiplicity)
                .with("entityTypeName", entityTypeName)
                .returns()
                .debug();
    }

    /**
     * AssociationEndを削除する.
     */
    private void deleteAssociationEnd() {
        Http.request("box/odatacol/schema/assocend/delete.txt")
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
    }

}
