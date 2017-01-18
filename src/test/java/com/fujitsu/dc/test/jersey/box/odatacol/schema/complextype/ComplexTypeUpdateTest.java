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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.complextype;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.complextypeproperty.ComplexTypePropertyUtils;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ComplexType更新のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypeUpdateTest extends ODataCommon {

    /**
     * コンストラクタ.
     */
    public ComplexTypeUpdateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * 同一のComplexTypeを複数回変名できること.
     */
    @Test
    public final void 同一のComplexTypeを複数回変名できること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "testComplexTypeRename";
        String complexTypeRename2 = "testComplexTypeRename2";
        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);
            // 変名（1回目）
            ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, complexTypeRename, HttpStatus.SC_NO_CONTENT);
            // 変名（2回目）
            ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, complexTypeRename2, HttpStatus.SC_NO_CONTENT);
            // 変名後のComplexTypeを取得できること
            TResponse res = ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    complexTypeRename2, HttpStatus.SC_OK);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypeRename2);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypeUtils.NAMESPACE, additional);
            // 変更前の名前でComplexTypeを取得できないこと
            ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, HttpStatus.SC_NOT_FOUND);
            // ComplexTypeの一覧取得で変名後のComplexTypeが取得できること
            res = ComplexTypeUtils.list(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "?\\$filter=Name+eq+'" + complexTypeRename2 + "'", HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypeRename2);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, ComplexTypeUtils.NAMESPACE, additional);
            // 最初の名前でComplexTypeを作成できること
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);
            // ComplexTypeを取得できること
            res = ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", complexTypeName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypeUtils.NAMESPACE, additional);
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, -1);
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename2, -1);
        }
    }

    /**
     * Propertyから参照されているComplexTypeのNameを変更した場合に409になること.
     */
    @Test
    public final void Propertyから参照されているComplexTypeのNameを変更した場合に409になること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "testComplexTypeRename";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);
            // Propertyの作成
            PropertyUtils.create(BEARER_MASTER_TOKEN, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    Setup.TEST_ENTITYTYPE_M1, "testProperty",
                    complexTypeName, true, null, Property.COLLECTION_KIND_NONE, false, null, HttpStatus.SC_CREATED);
            // 変名
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_CONFLICT);
            res.checkErrorResponse(DcCoreException.OData.CONFLICT_HAS_RELATED.getCode(),
                    DcCoreException.OData.CONFLICT_HAS_RELATED.getMessage());
        } finally {
            PropertyUtils.delete(BEARER_MASTER_TOKEN, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    Setup.TEST_ENTITYTYPE_M1, "testProperty", -1);
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, -1);
        }
    }

    /**
     * 二階層目のComplexTypePropertyから参照されているComplexTypeのNameを変更した場合に409になること.
     */
    @Test
    public final void 二階層目のComplexTypePropertyから参照されているComplexTypeのNameを変更した場合に409になること() {
        String complex1st = "1stComplex";
        String complex2nd = "2ndComplex";
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String btoken = AbstractCase.BEARER_MASTER_TOKEN;

        try {
            EntityTypeUtils.create(cell, token, box, col, "testEntity", HttpStatus.SC_CREATED);
            ComplexTypeUtils.create(cell, box, col, complex1st, HttpStatus.SC_CREATED);
            ComplexTypeUtils.create(cell, box, col, complex2nd, HttpStatus.SC_CREATED);
            PropertyUtils.create(btoken, cell, box, col, "testEntity", "property",
                    complex1st, true, null, "None", false, null, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cell, box, col, "1stProp", complex1st, complex2nd, HttpStatus.SC_CREATED);

            // 変名
            TResponse res = ComplexTypeUtils.update(token, cell, box, col,
                    complex2nd, complex2nd, HttpStatus.SC_CONFLICT);
            res.checkErrorResponse(DcCoreException.OData.CONFLICT_HAS_RELATED.getCode(),
                    DcCoreException.OData.CONFLICT_HAS_RELATED.getMessage());
        } finally {
            ComplexTypePropertyUtils.delete(cell, box, col, "1stProp", complex1st, -1);
            PropertyUtils.delete(btoken, cell, box, col, "testEntity", "property", -1);
            ComplexTypeUtils.delete(token, cell, box, col, complex1st, -1);
            ComplexTypeUtils.delete(token, cell, box, col, complex2nd, -1);
            EntityTypeUtils.delete(col, token, MediaType.APPLICATION_JSON, "testEntity", cell, -1);
        }
    }

    /**
     * 最下層のComplexTypePropertyから参照されているComplexTypeのNameを変更した場合に409になること.
     */
    @Test
    public final void 最下層のComplexTypePropertyから参照されているComplexTypeのNameを変更した場合に409になること() {
        String complex1st = "1stComplex";
        String complex2nd = "2ndComplex";
        String complex3rd = "3rdComplex";
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String btoken = AbstractCase.BEARER_MASTER_TOKEN;

        try {
            EntityTypeUtils.create(cell, token, box, col, "testEntity", HttpStatus.SC_CREATED);
            ComplexTypeUtils.create(cell, box, col, complex1st, HttpStatus.SC_CREATED);
            ComplexTypeUtils.create(cell, box, col, complex2nd, HttpStatus.SC_CREATED);
            ComplexTypeUtils.create(cell, box, col, complex3rd, HttpStatus.SC_CREATED);
            PropertyUtils.create(btoken, cell, box, col, "testEntity", "property", complex1st,
                    true, null, "None", false, null, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cell, box, col, "1stProp", complex1st, complex2nd, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cell, box, col, "2ndProp", complex2nd, complex3rd, HttpStatus.SC_CREATED);
            ComplexTypePropertyUtils.create(cell, box, col, "3rdProp", complex3rd, "Edm.String", HttpStatus.SC_CREATED);

            // 変名
            TResponse res = ComplexTypeUtils.update(token, cell, box, col,
                    complex3rd, complex3rd, HttpStatus.SC_CONFLICT);
            res.checkErrorResponse(DcCoreException.OData.CONFLICT_HAS_RELATED.getCode(),
                    DcCoreException.OData.CONFLICT_HAS_RELATED.getMessage());
        } finally {
            ComplexTypePropertyUtils.delete(cell, box, col, "3rdProp", complex3rd, -1);
            ComplexTypePropertyUtils.delete(cell, box, col, "2ndProp", complex2nd, -1);
            ComplexTypePropertyUtils.delete(cell, box, col, "1stProp", complex1st, -1);
            PropertyUtils.delete(btoken, cell, box, col, "testEntity", "property", -1);
            ComplexTypeUtils.delete(token, cell, box, col, complex3rd, -1);
            ComplexTypeUtils.delete(token, cell, box, col, complex2nd, -1);
            ComplexTypeUtils.delete(token, cell, box, col, complex1st, -1);
            EntityTypeUtils.delete(col, token, MediaType.APPLICATION_JSON, "testEntity", cell, -1);
        }
    }

    /**
     * 同じ名前でComplexType名を変更できること.
     */
    @Test
    public final void 同じ名前でComplexType名を変更できること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "testComplexType";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);
            // 変名
            ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, complexTypeRename, HttpStatus.SC_NO_CONTENT);
            // 変名後のComplexTypeを取得できること
            TResponse res = ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    complexTypeRename, HttpStatus.SC_OK);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypeRename);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypeUtils.NAMESPACE, additional);
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, -1);
        }
    }

    /**
     * 変名後のComplexTypeが存在する場合409になること.
     */
    @Test
    public final void 変名後のComplexTypeが存在する場合409になること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "testComplexType2";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeRename,
                    HttpStatus.SC_CREATED);
            // 変名
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_CONFLICT);
            res.checkErrorResponse(DcCoreException.OData.ENTITY_ALREADY_EXISTS.getCode(),
                    DcCoreException.OData.ENTITY_ALREADY_EXISTS.getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameが指定されていない場合400になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameが指定されていない場合400になること() {
        String complexTypeName = "testComplexType";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);
            // ComplexTypeの更新(400になること)
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, new JSONObject(), HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Name").getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
        }
    }

    /**
     * 存在しないComplexTypeを指定して更新した場合404になること.
     */
    @Test
    public final void 存在しないComplexTypeを指定して更新した場合404になること() {
        String complexTypeName = "testComplexType";
        // ComplexTypeの更新
        TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, complexTypeName, complexTypeName, HttpStatus.SC_NOT_FOUND);
        res.checkErrorResponse(DcCoreException.OData.NO_SUCH_ENTITY.getCode(),
                DcCoreException.OData.NO_SUCH_ENTITY.getMessage());
    }

    /**
     * ComplexTypeの更新時Nameにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameにアンダーバー始まりの文字列を指定した場合400になること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "_testComplexType";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // ComplexTypeの更新
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameにスラッシュを含む文字列を指定した場合400になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameにスラッシュを含む文字列を指定した場合400になること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "test/ComplexType";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // ComplexTypeの更新
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameにハイフン始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameにハイフン始まりの文字列を指定した場合400になること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "-testComplexType";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // ComplexTypeの更新
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameに空文字を指定した場合400になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameに空文字を指定した場合400になること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // ComplexTypeの更新
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameに指定可能な文字数の最小値を指定した場合204になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameに指定可能な文字数の最小値を指定した場合204になること() {
        String complexTypeName = "testComplexType";
        String complexTypeRename = "1";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // 変名
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_NO_CONTENT);

            // 変名後のComplexTypeを取得できること
            res = ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    complexTypeRename, HttpStatus.SC_OK);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypeRename);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypeUtils.NAMESPACE, additional);
            // 変更前の名前でComplexTypeを取得できないこと
            ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, HttpStatus.SC_NOT_FOUND);

        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameに指定可能な文字数の最大値をオーバーした場合400になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameに指定可能な文字数の最大値をオーバーした場合400になること() {
        String complexTypeName = "testDataEntityName";
        String complexTypeRename = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // ComplexTypeの更新
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameに指定可能な文字数の最大値を指定した場合204になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameに指定可能な文字数の最大値を指定した場合204になること() {
        String complexTypeName = "testDataEntityName";
        String complexTypeRename = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";

        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // 変名
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_NO_CONTENT);

            // 変名後のComplexTypeを取得できること
            res = ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    complexTypeRename, HttpStatus.SC_OK);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", complexTypeRename);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, ComplexTypeUtils.NAMESPACE, additional);
            // 変更前の名前でComplexTypeを取得できないこと
            ComplexTypeUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, HttpStatus.SC_NOT_FOUND);
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeRename, -1);
        }
    }

    /**
     * ComplexTypeの更新時Nameに日本語を指定した場合400になること.
     */
    @Test
    public final void ComplexTypeの更新時Nameに日本語を指定した場合400になること() {
        String complexTypeName = "testDataEntityName";
        String complexTypeRename = "日本語";
        try {
            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName,
                    HttpStatus.SC_CREATED);

            // ComplexTypeの更新
            TResponse res = ComplexTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypeName, complexTypeRename, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            ComplexTypeUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName, -1);
        }
    }
}
