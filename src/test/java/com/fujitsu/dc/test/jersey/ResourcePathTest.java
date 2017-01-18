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
package com.fujitsu.dc.test.jersey;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * リソースパスのテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ResourcePathTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public ResourcePathTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * CellルートパスのCell名に空白が指定された場合に404エラーが返却されること.
     */
    @Test
    public final void CellルートパスのCell名に空白が指定された場合に404エラーが返却されること() {
        String url = UrlUtils.cellRoot("cell%20test");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.Dav.CELL_NOT_FOUND.getCode(), code);
        assertEquals(DcCoreException.Dav.CELL_NOT_FOUND.getMessage(), message);
    }

    /**
     * CellルートパスのCell名に改行コードが指定された場合に404エラーが返却されること.
     */
    @Test
    public final void CellルートパスのCell名に改行コードが指定された場合に404エラーが返却されること() {
        String url = UrlUtils.cellRoot("cell%0atest");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.Dav.CELL_NOT_FOUND.getCode(), code);
        assertEquals(DcCoreException.Dav.CELL_NOT_FOUND.getMessage(), message);
    }

    /**
     * BoxルートパスのBox名に空白が指定された場合に404エラーが返却されること.
     */
    @Test
    public final void BoxルートパスのBox名に空白が指定された場合に404エラーが返却されること() {
        String url = UrlUtils.boxRoot(Setup.TEST_CELL1, "box%20test");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.Dav.BOX_NOT_FOUND.getCode(), code);
        String expectedUrl = UrlUtils.boxRoot(Setup.TEST_CELL1, "box test");
        assertEquals(DcCoreException.Dav.BOX_NOT_FOUND.params(expectedUrl).getMessage(), message);
    }

    /**
     * BoxルートパスのBox名に改行コードが指定された場合に404エラーが返却されること.
     */
    @Test
    public final void BoxルートパスのBox名に改行コードが指定された場合に404エラーが返却されること() {
        String url = UrlUtils.boxRoot(Setup.TEST_CELL1, "box%0atest");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.Dav.BOX_NOT_FOUND.getCode(), code);
        String expectedUrl = UrlUtils.boxRoot(Setup.TEST_CELL1, "box\ntest");
        assertEquals(DcCoreException.Dav.BOX_NOT_FOUND.params(expectedUrl).getMessage(), message);
    }

    /**
     * ユニット制御API一件取得の検索値に空白が指定された場合に404エラーが返却されること.
     */
    @Test
    public final void ユニット制御API一件取得の検索値に空白が指定された場合に404エラーが返却されること() {
        String url = UrlUtils.unitCtl("Cell", "cell%20test");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getCode(), code);
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getMessage(), message);
    }

    /**
     * ユニット制御API一件取得の検索値に改行コードが指定された場合に404エラーが返却されること.
     */
    @Test
    public final void ユニット制御API一件取得の検索値に改行コードが指定された場合に404エラーが返却されること() {
        String url = UrlUtils.unitCtl("Cell", "cell%0atest");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getCode(), code);
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getMessage(), message);
    }

    /**
     * セル制御API一件取得の検索値に空白が指定された場合に404エラーが返却されること.
     */
    @Test
    public final void セル制御API一件取得の検索値に空白が指定された場合に404エラーが返却されること() {
        String url = UrlUtils.cellCtl(Setup.TEST_CELL1, "Box", "box%20test");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getCode(), code);
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getMessage(), message);
    }

    /**
     * セル制御API一件取得の検索値に改行コードが指定された場合に404エラーが返却されること.
     */
    @Test
    public final void セル制御API一件取得の検索値に改行コードが指定された場合に404エラーが返却されること() {
        String url = UrlUtils.cellCtl(Setup.TEST_CELL1, "Box", "box%0atest");
        DcResponse res = ODataCommon.getOdataResource(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        JSONObject resBody = res.bodyAsJson();
        String message = (String) ((JSONObject) resBody.get("message")).get("value");
        String code = (String) resBody.get("code");
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getCode(), code);
        assertEquals(DcCoreException.OData.NO_SUCH_ENTITY.getMessage(), message);
    }
}
