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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListFilterTypeValidateTest extends AbstractUserDataTest {

    private static final String TOKEN = DcCoreConfig.getMasterToken();
    private static final String CELL = "filterTypeValidateTest";
    private static final String BOX = "box";
    private static final String COL = "odata";
    private static final String ENTITY = "entity";
    private static final String NAMESPACE = "UserData." + ENTITY;

    /**
     * コンストラクタ.
     */
    public UserDataListFilterTypeValidateTest() {
        super();
    }

    /**
     * Edm_String型の$filter_eq検索の検証.
     */
    @Test
    public final void Edm_String型の$filter_eq検索の検証() {

        // Edm.Stringの検索条件：文字列： "string data"
        String query = "?\\$filter=string+eq+%27string%20data%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Stringの検索条件： 文字列： ""
        query = "?\\$filter=string+eq+%27%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Stringの検索条件： null
        query = "?\\$filter=string+eq+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);

        // Edm.Stringの検索条件： 整数： 1111
        query = "?\\$filter=string+eq+1111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 整数： 1111 eq以外の演算子(gt)
        query = "?\\$filter=string+gt+1111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 整数： 1111 startswith()
        query = "?\\$filter=startswith%28string%2C1111%29";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 整数： 1111 startswith()
        query = "?\\$filter=substringof%281111%2Cstring%29";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=string+eq+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=string+eq+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 真偽値： true
        query = "?\\$filter=string+eq+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
    }

    /**
     * Edm_String型の$filter_ne検索の検証.
     */
    @Test
    public final void Edm_String型の$filter_ne検索の検証() {

        // Edm.Stringの検索条件：文字列： "string data"
        String query = "?\\$filter=string+ne+%27string%20data%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Stringの検索条件： 文字列： ""
        query = "?\\$filter=string+ne+%27%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Stringの検索条件： null
        query = "?\\$filter=string+ne+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.Stringの検索条件： 整数： 1111
        query = "?\\$filter=string+ne+1111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=string+ne+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=string+ne+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
        // Edm.Stringの検索条件： 真偽値： true
        query = "?\\$filter=string+ne+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("string").getMessage());
    }

    /**
     * Edm_Int32型の$filter_eq検索の検証.
     */
    @Test
    public final void Edm_Int32型の$filter_eq検索の検証() {

        // Edm.Int32の検索条件：整数： 1111
        String query = "?\\$filter=int32+eq+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Int32の検索条件： LONG： 1111L
        query = "?\\$filter=int32+eq+1111L&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Int32の検索条件： null
        query = "?\\$filter=int32+eq+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);

        // Edm.Int32の検索条件： 文字列： "1111"
        query = "?\\$filter=int32+eq+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 文字列： "1111" eq以外の演算子(ge)
        query = "?\\$filter=int32+ge+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 文字列： "aaaa"
        query = "?\\$filter=int32+eq+%27aaaa%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=int32+eq+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=int32+eq+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 真偽値： true
        query = "?\\$filter=int32+eq+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());

        // 値の範囲チェック
        // Edm.Int32の検索条件： 範囲内(最大値 2147483647)
        query = "?\\$filter=int32+eq+2147483647&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Int32の検索条件： 範囲内(最小値 -2147483648)
        query = "?\\$filter=int32+eq+-2147483648&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Int32の検索条件： 範囲外(最大値 2147483647 + 1)： 2147483648
        query = "?\\$filter=int32+eq+2147483648&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("int32").getMessage());
        // Edm.Int32の検索条件： 範囲外(最小値 -2147483648 - 1)： -2147483649
        query = "?\\$filter=int32+eq+-2147483649&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("int32").getMessage());

    }

    /**
     * Edm_Int32型の$filter_ne検索の検証.
     */
    @Test
    public final void Edm_Int32型の$filter_ne検索の検証() {

        // Edm.Int32の検索条件：整数： 1111
        String query = "?\\$filter=int32+ne+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Int32の検索条件： LONG： 1111L
        query = "?\\$filter=int32+ne+1111L&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Int32の検索条件： null
        query = "?\\$filter=int32+ne+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.Int32の検索条件： 文字列： "1111"
        query = "?\\$filter=int32+ne+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 文字列： "aaaa"
        query = "?\\$filter=int32+ne+%27aaaa%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=int32+ne+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=int32+ne+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());
        // Edm.Int32の検索条件： 真偽値： true
        query = "?\\$filter=int32+ne+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32").getMessage());

        // 値の範囲チェック
        // Edm.Int32の検索条件： 範囲内(最大値 2147483647)
        query = "?\\$filter=int32+ne+2147483647&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Int32の検索条件： 範囲内(最小値 -2147483648)
        query = "?\\$filter=int32+ne+-2147483648&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Int32の検索条件： 範囲外(最大値 2147483647 + 1)： 2147483648
        query = "?\\$filter=int32+ne+2147483648&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("int32").getMessage());
        // Edm.Int32の検索条件： 範囲外(最小値 -2147483648 - 1)： -2147483649
        query = "?\\$filter=int32+ne+-2147483649&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("int32").getMessage());

    }

    /**
     * Edm_Single型の$filter_eq検索の検証.
     */
    @Test
    public final void Edm_Single型の$filter_eq検索の検証() {

        // Edm.Singleの検索条件：整数： 1111
        String query = "?\\$filter=single+eq+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Singleの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=single+eq+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Singleの検索条件： 実数値（倍精度）： 1111.11111
        query = "?\\$filter=single+eq+1111.11111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Singleの検索条件： null
        query = "?\\$filter=single+eq+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);

        // Edm.Singleの検索条件： 文字列： "1111"
        query = "?\\$filter=single+eq+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： 文字列： "1111" eq以外の演算子(lt)
        query = "?\\$filter=single+lt+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： 文字列： "1111.11"
        query = "?\\$filter=single+eq+%271111.11%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： 文字列： "1111.1a"
        query = "?\\$filter=single+eq+%271111.1a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： 真偽値： true
        query = "?\\$filter=single+eq+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： Single： 1111.11f
        query = "?\\$filter=single+eq+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： Decimal： 1111.11m
        query = "?\\$filter=single+eq+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());

        // 値の範囲チェック
        // Edm.Singleの検索条件： 整数部分5桁 小数部分 5桁： 11111.11111
        query = "?\\$filter=single+eq+11111.11111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Singleの検索条件： 整数部分6桁 小数部分 5桁： 111111.11111
        query = "?\\$filter=single+eq+111111.11111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("single").getMessage());
        // Edm.Singleの検索条件： 整数部分5桁 小数部分 6桁： 11111.111111
        query = "?\\$filter=single+eq+11111.111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("single").getMessage());
        // Edm.Singleの検索条件： 整数部分6桁 小数部分 6桁： 111111.111111
        query = "?\\$filter=single+eq+111111.111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("single").getMessage());
    }

    /**
     * Edm_Single型の$filter_ne検索の検証.
     */
    @Test
    public final void Edm_Single型の$filter_ne検索の検証() {

        // Edm.Singleの検索条件：整数： 1111
        String query = "?\\$filter=single+ne+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Singleの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=single+ne+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Singleの検索条件： 実数値（倍精度）： 1111.11111
        query = "?\\$filter=single+ne+1111.11111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Singleの検索条件： null
        query = "?\\$filter=single+ne+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.Singleの検索条件： 文字列： "1111"
        query = "?\\$filter=single+ne+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： 文字列： "1111.11"
        query = "?\\$filter=single+ne+%271111.11%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： 文字列： "1111.1a"
        query = "?\\$filter=single+ne+%271111.1a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： 真偽値： true
        query = "?\\$filter=single+ne+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： Single： 1111.11f
        query = "?\\$filter=single+ne+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());
        // Edm.Singleの検索条件： Decimal： 1111.11m
        query = "?\\$filter=single+ne+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("single").getMessage());

        // 値の範囲チェック
        // Edm.Singleの検索条件： 整数部分5桁 小数部分 5桁： 11111.11111
        query = "?\\$filter=single+ne+11111.11111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Singleの検索条件： 整数部分6桁 小数部分 5桁： 111111.11111
        query = "?\\$filter=single+ne+111111.11111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("single").getMessage());
        // Edm.Singleの検索条件： 整数部分5桁 小数部分 6桁： 11111.111111
        query = "?\\$filter=single+ne+11111.111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("single").getMessage());
        // Edm.Singleの検索条件： 整数部分6桁 小数部分 6桁： 111111.111111
        query = "?\\$filter=single+ne+111111.111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("single").getMessage());
    }

    /**
     * Edm_Double型の$filter_eq検索の検証.
     */
    @Test
    public final void Edm_Double型の$filter_eq検索の検証() {
        // Edm.Doubleの検索条件：整数： 1111
        String query = "?\\$filter=double+eq+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Doubleの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=double+eq+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Doubleの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=double+eq+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Doubleの検索条件： null
        query = "?\\$filter=double+eq+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);

        // Edm.Doubleの検索条件： 文字列： "1111"
        query = "?\\$filter=double+eq+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： 文字列： "1111" eq以外の演算子(le)
        query = "?\\$filter=double+le+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： 文字列： "1111.1111111"
        query = "?\\$filter=double+eq+%271111.1111111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： 文字列： "1111.111111a"
        query = "?\\$filter=double+eq+%271111.111111a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： 真偽値： true
        query = "?\\$filter=double+eq+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： Single： 1111.11f
        query = "?\\$filter=double+eq+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： Decimal： 1111.11m
        query = "?\\$filter=double+eq+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());

        // 値の範囲チェック
        // 現状、指数表記での$filter検索はできない(制限事項)ため、チェックせず。
    }

    /**
     * Edm_Double型の$filter_ne検索の検証.
     */
    @Test
    public final void Edm_Double型の$filter_ne検索の検証() {
        // Edm.Doubleの検索条件：整数： 1111
        String query = "?\\$filter=double+ne+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Doubleの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=double+ne+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Doubleの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=double+ne+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Doubleの検索条件： null
        query = "?\\$filter=double+ne+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.Doubleの検索条件： 文字列： "1111"
        query = "?\\$filter=double+ne+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： 文字列： "1111.1111111"
        query = "?\\$filter=double+ne+%271111.1111111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： 文字列： "1111.111111a"
        query = "?\\$filter=double+ne+%271111.111111a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： 真偽値： true
        query = "?\\$filter=double+ne+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： Single： 1111.11f
        query = "?\\$filter=double+ne+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());
        // Edm.Doubleの検索条件： Decimal： 1111.11m
        query = "?\\$filter=double+ne+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("double").getMessage());

        // 値の範囲チェック
        // 現状、指数表記での$filter検索はできない(制限事項)ため、チェックせず。
    }


    /**
     * Edm_Boolean型の$filter_eq検索の検証.
     */
    @Test
    public final void Edm_Boolean型の$filter_eq検索の検証() {
        // Edm.Booleanの検索条件： 真偽値： true
        String query = "?\\$filter=boolean+eq+true&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Booleanの検索条件： null
        query = "?\\$filter=boolean+eq+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);

        // Edm.Booleanの検索条件： 文字列： "true"
        query = "?\\$filter=boolean+eq+%27true%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件：整数： 1111
        query = "?\\$filter=boolean+eq+1111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=boolean+eq+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=boolean+eq+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： Single： 1111.11f
        query = "?\\$filter=boolean+eq+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： Decimal： 1111.11m
        query = "?\\$filter=boolean+eq+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
    }

    /**
     * Edm_Boolean型の$filter_ne検索の検証.
     */
    @Test
    public final void Edm_Boolean型の$filter_ne検索の検証() {
        // Edm.Booleanの検索条件： 真偽値： true
        String query = "?\\$filter=boolean+ne+true&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Booleanの検索条件： null
        query = "?\\$filter=boolean+ne+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.Booleanの検索条件： 文字列： "true"
        query = "?\\$filter=boolean+ne+%27true%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件：整数： 1111
        query = "?\\$filter=boolean+ne+1111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=boolean+ne+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=boolean+ne+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： Single： 1111.11f
        query = "?\\$filter=boolean+ne+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
        // Edm.Booleanの検索条件： Decimal： 1111.11m
        query = "?\\$filter=boolean+ne+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("boolean").getMessage());
    }

    /**
     * Edm_DateTime型の$filter_eq検索の検証.
     */
    @Test
    public final void Edm_DateTime型の$filter_eq検索の検証() {
        // Edm.DateTimeの検索条件：整数： 1111
        String query = "?\\$filter=datetime+eq+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.DateTimeの検索条件：整数(LONG)： 1420589956172
        query = "?\\$filter=datetime+eq+1420589956172&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.DateTimeの検索条件：null
        query = "?\\$filter=datetime+eq+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);

        // Edm.DateTimeの検索条件：文字列： "1420589956172"
        query = "?\\$filter=datetime+eq+%271420589956172%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 文字列： "/Date(1410689956172)/"
        query = "?\\$filter=datetime+eq+%27%2FDate%281420589956172%29%2F%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=datetime+eq+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=datetime+eq+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 真偽値： true
        query = "?\\$filter=datetime+eq+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： Single： 1111.11f
        query = "?\\$filter=datetime+eq+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： Decimal： 1111.11m
        query = "?\\$filter=datetime+eq+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());

        // 値の範囲チェック
        // Edm.DateTimeの検索条件：最大値： 253402300799999
        query = "?\\$filter=datetime+eq+253402300799999&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.DateTimeの検索条件：最小値： -6847804800000
        query = "?\\$filter=datetime+eq+-6847804800000&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.DateTimeの検索条件：最大値(253402300799999) + 1： 253402300800000
        query = "?\\$filter=datetime+eq+253402300800000&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("datetime").getMessage());
        // Edm.DateTimeの検索条件：最小値(-6847804800000) + 1： -6847804800001
        query = "?\\$filter=datetime+eq+-6847804800001&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("datetime").getMessage());
    }

    /**
     * Edm_DateTime型の$filter_ne検索の検証.
     */
    @Test
    public final void Edm_DateTime型の$filter_ne検索の検証() {
        // Edm.DateTimeの検索条件：整数： 1111
        String query = "?\\$filter=datetime+ne+1111&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.DateTimeの検索条件：整数(LONG)： 1420589956172
        query = "?\\$filter=datetime+ne+1420589956172&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.DateTimeの検索条件：null
        query = "?\\$filter=datetime+ne+null&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.DateTimeの検索条件：文字列： "1420589956172"
        query = "?\\$filter=datetime+ne+%271420589956172%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 文字列： "/Date(1410689956172)/"
        query = "?\\$filter=datetime+ne+%27%2FDate%281420589956172%29%2F%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=datetime+ne+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 実数値（倍精度）： 1111.1111111
        query = "?\\$filter=datetime+ne+1111.1111111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： 真偽値： true
        query = "?\\$filter=datetime+ne+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： Single： 1111.11f
        query = "?\\$filter=datetime+ne+1111.11f&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());
        // Edm.DateTimeの検索条件： Decimal： 1111.11m
        query = "?\\$filter=datetime+ne+1111.11m&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("datetime").getMessage());

        // 値の範囲チェック
        // Edm.DateTimeの検索条件：最大値： 253402300799999
        query = "?\\$filter=datetime+ne+253402300799999&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.DateTimeの検索条件：最小値： -6847804800000
        query = "?\\$filter=datetime+ne+-6847804800000&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.DateTimeの検索条件：最大値(253402300799999) + 1： 253402300800000
        query = "?\\$filter=datetime+ne+253402300800000&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("datetime").getMessage());
        // Edm.DateTimeの検索条件：最小値(-6847804800000) + 1： -6847804800001
        query = "?\\$filter=datetime+ne+-6847804800001&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.getCode(),
                DcCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params("datetime").getMessage());
    }

    /**
     * 動的プロパティの$filter_eq検索の検証.
     */
    @Test
    public final void 動的プロパティの$filter_eq検索の検証() {
        // Edm.String
        // Edm.Stringの検索条件：文字列： "string value"
        String query = "?\\$filter=d_string+eq+%27string%20value%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Stringの検索条件： 整数： 1111
        query = "?\\$filter=d_string+eq+1111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_string").getMessage());

        // Edm.Int32
        // Edm.Int32の検索条件：整数： 2222
        query = "?\\$filter=d_int32+eq+2222&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.Int32の検索条件： 文字列： "222a"
        query = "?\\$filter=d_int32+eq+%27222a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_int32").getMessage());

        // Edm.Single(動的プロパティでは、Double型として登録される）
        // Edm.Singleの検索条件：整数： 2222
        query = "?\\$filter=d_single+eq+2222&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Singleの検索条件： 文字列： "2222.22"
        query = "?\\$filter=d_single+eq+%272222.22%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_single").getMessage());
        // Edm.Singleの検索条件： 文字列： "2222.2a"
        query = "?\\$filter=d_single+eq+%272222.2a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_single").getMessage());

        // Edm.Double
        // Edm.Doubleの検索条件：整数： 2222
        query = "?\\$filter=d_double+eq+2222&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Doubleの検索条件： 文字列： "2222.2222222"
        query = "?\\$filter=d_double+eq+%272222.2222222%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_double").getMessage());
        // Edm.Doubleの検索条件： 文字列： "2222.222222a"
        query = "?\\$filter=d_double+eq+%272222.222222a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_double").getMessage());

        // Edm.Boolean
        // Edm.Booleanの検索条件： 真偽値： false
        query = "?\\$filter=d_boolean+eq+false&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Booleanの検索条件： 文字列： "false"
        query = "?\\$filter=d_boolean+eq+%27false%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_boolean").getMessage());

        // Edm.DateTime(動的プロパティは未サポートのため、Edm.String型として登録される）
        // Edm.DateTimeの検索条件：整数(LONG)： 1410689956172
        query = "?\\$filter=d_datetime+eq+1410689956172&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_datetime").getMessage());
        // Edm.DateTimeの検索条件： 文字列： "/Date(1410689956172)/"
        query = "?\\$filter=d_datetime+eq+%27%2FDate%281410689956172%29%2F%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // Edm.DateTimeの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=d_datetime+eq+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_datetime").getMessage());
    }

    /**
     * 動的プロパティの$filter_ne検索の検証.
     */
    @Test
    public final void 動的プロパティの$filter_ne検索の検証() {
        // Edm.String
        // Edm.Stringの検索条件：文字列： "string value"
        String query = "?\\$filter=d_string+ne+%27string%20value%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Stringの検索条件： 整数： 1111
        query = "?\\$filter=d_string+ne+1111&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_string").getMessage());

        // Edm.Int32
        // Edm.Int32の検索条件：整数： 2222
        query = "?\\$filter=d_int32+ne+2222&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.Int32の検索条件： 文字列： "222a"
        query = "?\\$filter=d_int32+ne+%27222a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_int32").getMessage());

        // Edm.Single(動的プロパティでは、Double型として登録される）
        // Edm.Singleの検索条件：整数： 2222
        query = "?\\$filter=d_single+ne+2222&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Singleの検索条件： 文字列： "2222.22"
        query = "?\\$filter=d_single+ne+%272222.22%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_single").getMessage());
        // Edm.Singleの検索条件： 文字列： "2222.2a"
        query = "?\\$filter=d_single+ne+%272222.2a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_single").getMessage());

        // Edm.Double
        // Edm.Doubleの検索条件：整数： 2222
        query = "?\\$filter=d_double+ne+2222&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Doubleの検索条件： 文字列： "2222.2222222"
        query = "?\\$filter=d_double+ne+%272222.2222222%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_double").getMessage());
        // Edm.Doubleの検索条件： 文字列： "2222.222222a"
        query = "?\\$filter=d_double+ne+%272222.222222a%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_double").getMessage());

        // Edm.Boolean
        // Edm.Booleanの検索条件： 真偽値： false
        query = "?\\$filter=d_boolean+ne+false&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // Edm.Booleanの検索条件： 文字列： "false"
        query = "?\\$filter=d_boolean+ne+%27false%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_boolean").getMessage());

        // Edm.DateTime(動的プロパティは未サポートのため、Edm.String型として登録される）
        // Edm.DateTimeの検索条件：整数(LONG)： 1410689956172
        query = "?\\$filter=d_datetime+ne+1410689956172&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_datetime").getMessage());
        // Edm.DateTimeの検索条件： 文字列： "/Date(1410689956172)/"
        query = "?\\$filter=d_datetime+ne+%27%2FDate%281410689956172%29%2F%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // Edm.DateTimeの検索条件： 実数値（単精度）： 1111.11
        query = "?\\$filter=d_datetime+ne+1111.11&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("d_datetime").getMessage());
    }

    /**
     * Edm_Boolean型では$filterの検索の比較演算子を指定するとパースエラーとなること.
     */
    @Test
    public final void Edm_Boolean型では$filterの検索の比較演算子を指定するとパースエラーとなること() {
        // Edm.Booleanの検索の比較演算子： lt
        String query = "?\\$filter=boolean+lt+true&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.FILTER_PARSE_ERROR.getCode(),
                DcCoreException.OData.FILTER_PARSE_ERROR.getMessage());

        // Edm.Booleanの検索の比較演算子： le
        query = "?\\$filter=boolean+le+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.FILTER_PARSE_ERROR.getCode(),
                DcCoreException.OData.FILTER_PARSE_ERROR.getMessage());

        // Edm.Booleanの検索の比較演算子： ge
        query = "?\\$filter=boolean+ge+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.FILTER_PARSE_ERROR.getCode(),
                DcCoreException.OData.FILTER_PARSE_ERROR.getMessage());

        // Edm.Booleanの検索の比較演算子： gt
        query = "?\\$filter=boolean+gt+true&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.FILTER_PARSE_ERROR.getCode(),
                DcCoreException.OData.FILTER_PARSE_ERROR.getMessage());
    }

    /**
     * Edm_Int32型の配列に対する$filter検索の検証.
     */
    @Test
    public final void Edm_Int32型の配列に対する$filter検索の検証() {
        // Edm.Int32配列の検索の比較演算子： eq、検索条件： 文字列："a"
        String query = "?\\$filter=int32_list+eq+%27a%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32_list").getMessage());
        // Edm.Int32配列の検索の比較演算子： eq、検索条件： 整数： 数値：3
        query = "?\\$filter=int32_list+eq+3&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.Int32配列の検索の比較演算子： ne、検索条件： 文字列："1111"
        query = "?\\$filter=int32_list+ne+%271111%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32_list").getMessage());
        // Edm.Int32配列の検索の比較演算子： ne、検索条件： 整数： 数値：6
        query = "?\\$filter=int32_list+ne+6&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);

        // Edm.Int32配列の検索の比較演算子： gt、検索条件： 文字列："b"
        query = "?\\$filter=int32_list+gt+%27b%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32_list").getMessage());
        // Edm.Int32配列の検索の比較演算子： gt、検索条件： 整数：5
        query = "?\\$filter=int32_list+gt+5&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);

        // Edm.Int32配列の検索の比較演算子： ge、検索条件： 文字列："c"
        query = "?\\$filter=int32_list+ge+%27c%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32_list").getMessage());
        // Edm.Int32配列の検索の比較演算子： ge、検索条件： 整数：5
        query = "?\\$filter=int32_list+ge+5&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);

        // Edm.Int32配列の検索の比較演算子： lt、検索条件： 文字列："d"
        query = "?\\$filter=int32_list+lt+%27d%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32_list").getMessage());
        // Edm.Int32配列の検索の比較演算子： lt、検索条件： 整数：3
        query = "?\\$filter=int32_list+lt+3&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);

        // Edm.Int32配列の検索の比較演算子： le、検索条件： 文字列："e"
        query = "?\\$filter=int32_list+le+%27e%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("int32_list").getMessage());
        // Edm.Int32配列の検索の比較演算子： le、検索条件： 整数：3
        query = "?\\$filter=int32_list+le+3&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
    }

    /**
     * 管理データ型の$filter_eq検索の検証.
     */
    @Test
    public final void 管理データ型の$filter_eq検索の検証() {
        // __idが検索できること
        String query = "?\\$filter=__id+eq+%27id1%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // __publishedが検索できること
        query = "?\\$filter=__published+le+1420038000795&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // __updatedが検索できること
        query = "?\\$filter=__updated+lt+1420038000795&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // __metadataを検索しようとすると400エラーとなること
        query = "?\\$filter=__metadata+eq+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__metadata").getMessage());
        // NTKPを検索しようとすると400エラーとなること
        query = "?\\$filter=_SalesDetail+eq+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales", query, TOKEN,
                HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("_SalesDetail").getMessage());
        // 予約語ではない__始まりのプロパティを検索しようとすると400エラーとなること
        query = "?\\$filter=__invalid+eq+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__invalid").getMessage());
    }

    /**
     * 管理データ型の$filter_ne検索の検証.
     */
    @Test
    public final void 管理データ型の$filter_ne検索の検証() {
        // __idが検索できること
        String query = "?\\$filter=__id+ne+%27id1%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 1);
        // __publishedが検索できること
        query = "?\\$filter=__published+ne+1420038000795&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // __updatedが検索できること
        query = "?\\$filter=__updated+ne+1420038000795&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // __metadataを検索しようとすると400エラーとなること
        query = "?\\$filter=__metadata+ne+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__metadata").getMessage());
        // NTKPを検索しようとすると400エラーとなること
        query = "?\\$filter=_SalesDetail+ne+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales", query, TOKEN,
                HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("_SalesDetail").getMessage());
        // 予約語ではない__始まりのプロパティを検索しようとすると400エラーとなること
        query = "?\\$filter=__invalid+ne+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__invalid").getMessage());
    }

    /**
     * 管理用プロパティがオペランドのプロパティ名として指定できないこと.
     */
    @Test
    public final void 管理用プロパティがオペランドのプロパティ名として指定できないこと() {
        // gt
        String query = "?\\$filter=__metadata+gt+%27invalidKey%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__metadata").getMessage());
        // ge
        query = "?\\$filter=__metadata+ge+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__metadata").getMessage());
        // lt
        query = "?\\$filter=__metadata+lt+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        // le
        query = "?\\$filter=__metadata+le+%27invalidKey%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__metadata").getMessage());
        // startswith
        query = "?\\$filter=startswith%28__metadata%2c%27invalidKey%27%29&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__metadata").getMessage());
        // substringof
        query = "?\\$filter=substringof%28%27invalidKey%27%2c__metadata%29&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_BAD_REQUEST);
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.UNKNOWN_QUERY_KEY.getCode(),
                DcCoreException.OData.UNKNOWN_QUERY_KEY.params("__metadata").getMessage());
    }

    /**
     * オペランドに指定した検索条件の値が空文字の場合は検索ヒットしないこと.
     */
    @Test
    public final void オペランドに指定した検索条件の値が空文字の場合は検索ヒットしないこと() {
        // eq
        String query = "?\\$filter=string+eq+%27%27&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
        // ne
        query = "?\\$filter=string+ne+%27%27&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // startswith: 空文字を指定した場合、V1.3.21では該当プロパティを含むユーザODataを全件取得する。
        query = "?\\$filter=startswith%28string%2c%27%27%29&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 2);
        // substringof: 空文字を指定した場合、V1.3.21では検索ヒットしない。
        query = "?\\$filter=substringof%28%27%27%2cstring%29&\\$inlinecount=allpages";
        res = UserDataUtils.list(CELL, BOX, COL, ENTITY, query, TOKEN, HttpStatus.SC_OK);
        ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, NAMESPACE, null, 0);
    }
}
