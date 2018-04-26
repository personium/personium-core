/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
package io.personium.core.rs.odata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.utils.ODataUtils;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for AbstractODataResource.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AbstractODataResourceTest.class, ODataUtils.class })
@Category({ Unit.class })
public class AbstractODataResourceTest {

    /** Target class of unit test. */
    private AbstractODataResource abstractODataResource;

    /**
     * Before.
     */
    @Before
    public void befor() {
        abstractODataResource = mock(AbstractODataResource.class, Mockito.CALLS_REAL_METHODS);
    }

    /**
     * Double型の値の有効範囲チェックのテスト.
     * @throws Exception Exception
     */
    @Test
    public void Double型の値の有効範囲チェック() throws Exception {
        checkValidateDynamicProperty("負の最小値（-1.79e308d）の場合に例外がスローされないこと", -1.79e308d, true);
        checkValidateDynamicProperty("負の最大値（-2.23e-308d）の場合に例外がスローされないこと", -2.23e-308d, true);
        checkValidateDynamicProperty("正の最小値（2.23e-308d）の場合に例外がスローされないこと", 2.23e-308d, true);
        checkValidateDynamicProperty("正の最大値（1.79e308d）の場合に例外がスローされないこと", 1.79e308d, true);
        checkValidateDynamicProperty(
                "負の最小値より小さい値（-1.791e308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", -1.791e308d, false);
        checkValidateDynamicProperty("負の最小値より大きい値（-1.789e308d）の場合に例外がスローされないこと", -1.789e308d, true);
        checkValidateDynamicProperty("負の最大値より小さい値（-2.231e-308d）の場合に例外がスローされないこと", -2.231e-308d, true);
        checkValidateDynamicProperty(
                "負の最大値より大きい値（-2.229e-308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", -2.229e-308d, false);
        checkValidateDynamicProperty(
                "正の最小値より小さい値（2.229e-308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", 2.229e-308d, false);
        checkValidateDynamicProperty("正の最小値より大きい値（2.231e-308d）の場合に例外がスローされないこと", 2.231e-308d, true);
        checkValidateDynamicProperty("正の最大値より小さい値（1.789e308d）の場合に例外がスローされないこと", 1.789e308d, true);
        checkValidateDynamicProperty("正の最大値より大きい値（1.791e308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", 1.791e308d, false);
        checkValidateDynamicProperty("0の場合にtrueが返却されること", 0d, true);
    }

    /**
     * Double型の値の有効範囲チェック.
     * @param testComment テスト内容
     * @param inputValue バリデート対象の入力値
     * @param expectedReturnValue 期待する返却値
     * @throws Exception Exception
     */
    private void checkValidateDynamicProperty(String testComment, double inputValue, boolean expectedReturnValue)
            throws Exception {
        ODataEntityResource resource = new ODataEntityResource();
        Method method = AbstractODataResource.class.getDeclaredMethod("validateDynamicProperty",
                new Class[] {OProperty.class});
        method.setAccessible(true);
        OProperty<Double> property = OProperties.double_("testKey", inputValue);
        boolean valildResult = true;
        try {
            method.invoke(resource, property);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof PersoniumCoreException) {
                PersoniumCoreException e = (PersoniumCoreException) ex.getCause();
                if (PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode().equals(e.getCode())) {
                    valildResult = false;
                } else {
                    fail(testComment + ": 期待したエラーコードではない. 例外コード:[" + e.getCode() + "]");
                }
            }
        }
        assertEquals(testComment, expectedReturnValue, valildResult);
    }

    /**
     * Test validatePropertyRegEx().
     * Normal test.
     * isValidRegEx return true;
     * @throws Exception Unexpected error.
     */
    @Test
    public void validatePropertyRegEx_Normal_check_result_is_true() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String propName = "CellName";
        OProperty<?> op = OProperties.string("CellName", "testCell");
        String pFormat = Common.P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME + "')";

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.mockStatic(ODataUtils.class);
        PowerMockito.doReturn(true).when(ODataUtils.class, "validateRegEx", "testCell", Common.PATTERN_NAME);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            abstractODataResource.validatePropertyRegEx(propName, op, pFormat);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validatePropertyRegEx().
     * Error test.
     * isValidRegEx return false;
     * @throws Exception Unexpected error.
     */
    @Test
    public void validatePropertyRegEx_Error_check_result_is_false() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String propName = "CellName";
        OProperty<?> op = OProperties.string("CellName", "_testCell");
        String pFormat = Common.P_FORMAT_PATTERN_REGEX + "('" + Common.PATTERN_NAME + "')";

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.mockStatic(ODataUtils.class);
        PowerMockito.doReturn(false).when(ODataUtils.class, "validateRegEx", "_testCell", Common.PATTERN_NAME);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            abstractODataResource.validatePropertyRegEx(propName, op, pFormat);
            fail("Not Exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

}
