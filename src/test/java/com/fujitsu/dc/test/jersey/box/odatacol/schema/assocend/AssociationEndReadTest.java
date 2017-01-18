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
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * AssociationEnd登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndReadTest extends ODataCommon {

    private static final String ASSOCIATION_END_NAME = "product-category";

    private static final String ENTITY_TYPE_NAME = "Product";

    private static final String ASSOCIATION_END_TYPE = "ODataSvcSchema.AssociationEnd";

    /**
     * コンストラクタ.
     */
    public AssociationEndReadTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * EntityTypeNameにEntityType名を指定してAssociationEndを一件取得しデータを取得できること.
     */
    @Test
    public final void EntityTypeNameにEntityType名を指定してAssociationEndを一件取得しデータを取得できること() {
        String entityTypeName = ENTITY_TYPE_NAME;
        TResponse res = null;
        try {
            res = createAssociationEnd(ASSOCIATION_END_NAME, EdmMultiplicity.MANY.getSymbolString(), entityTypeName);

            TResponse response = Http.request("box/odatacol/schema/assocend/retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cell", "testcell1")
                    .with("box", "box1")
                    .with("collection", "setodata")
                    .with("name", ASSOCIATION_END_NAME)
                    .with("entityTypeName", "'" + entityTypeName + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.associationEnd("testcell1", "box1", "setodata", ASSOCIATION_END_NAME,
                    entityTypeName);
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", ASSOCIATION_END_NAME);
            // リンク情報からレスポンスボディ作成
            additional.put("_EntityType.Name", entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ASSOCIATION_END_TYPE, additional);
        } finally {
            if (res.getLocationHeader() != null) {
                deleteOdataResource(res.getLocationHeader());
            }
        }
    }

    /**
     * EntityTypeNameに存在しないEntityType名を指定してAssociationEndを一件取得し404が返却されること.
     */
    @Test
    public final void EntityTypeNameに存在しないEntityType名を指定してAssociationEndを一件取得し404が返却されること() {
        String associationName = ASSOCIATION_END_NAME;
        String entityTypeName = "dummy";
        TResponse res = null;
        try {
            res = createAssociationEnd(ASSOCIATION_END_NAME, EdmMultiplicity.MANY.getSymbolString(), entityTypeName);

            TResponse resGet = Http.request("box/odatacol/schema/assocend/retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cell", "testcell1")
                    .with("box", "box1")
                    .with("collection", "setodata")
                    .with("name", associationName)
                    .with("entityTypeName", "'" + entityTypeName + "'")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

            // メッセージチェック
            ODataCommon.checkErrorResponseBody(resGet,
                    DcCoreException.OData.NO_SUCH_ENTITY.getCode(),
                    DcCoreException.OData.NO_SUCH_ENTITY.getMessage());

        } finally {
            if (res.getLocationHeader() != null) {
                deleteOdataResource(res.getLocationHeader());
            }
        }
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

}
