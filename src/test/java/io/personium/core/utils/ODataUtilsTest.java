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
package io.personium.core.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.model.ctl.Common;
import io.personium.test.categories.Unit;

/**
 * ODataUtilsユニットテストクラス.
 */
@Category({Unit.class })
public class ODataUtilsTest {

    /**
     * 入力値をDouble型とした値の有効範囲チェックのテスト.
     * 有効範囲 ± 2.23e -308 から ± 1.79e +308
     * @throws Exception Exception
     */
    @Test
    public void 入力値をDouble型とした値の有効範囲チェック() throws Exception {
        checkValidateDouble("負の最小値（-1.79e308d）の場合にtrueが返却されること", -1.79e308d, true);
        checkValidateDouble("負の最大値（-2.23e-308d）の場合にtrueが返却されること", -2.23e-308d, true);
        checkValidateDouble("正の最小値（2.23e-308d）の場合にtrueが返却されること", 2.23e-308d, true);
        checkValidateDouble("正の最大値（1.79e308d）の場合にtrueが返却されること", 1.79e308d, true);
        checkValidateDouble("負の最小値より小さい値（-1.791e308d）の場合にfalseが返却されること", -1.791e308d, false);
        checkValidateDouble("負の最小値より大きい値（-1.789e308d）の場合にtrueが返却されること", -1.789e308d, true);
        checkValidateDouble("負の最大値より小さい値（-2.231e-308d）の場合にtrueが返却されること", -2.231e-308d, true);
        checkValidateDouble("負の最大値より大きい値（-2.229e-308d）の場合にfalseが返却されること", -2.229e-308d, false);
        checkValidateDouble("正の最小値より小さい値（2.229e-308d）の場合にfalseが返却されること", 2.229e-308d, false);
        checkValidateDouble("正の最小値より大きい値（2.231e-308d）の場合にtrueが返却されること", 2.231e-308d, true);
        checkValidateDouble("正の最大値より小さい値（1.789e308d）の場合にtrueが返却されること", 1.789e308d, true);
        checkValidateDouble("正の最大値より大きい値（1.791e308d）の場合にfalseが返却されること", 1.791e308d, false);
        checkValidateDouble("0dの場合にtrueが返却されること", 0d, true);
    }

    /**
     * 入力値を文字列としたDouble型の値の有効範囲チェックのテスト.
     * 有効範囲 ± 2.23e -308 から ± 1.79e +308
     * @throws Exception Exception
     */
    @Test
    public void 入力値を文字列としたDouble型の値の有効範囲チェック() throws Exception {
        checkValidateDouble("文字列形式で負の最小値（-1.79e308）の場合にtrueが返却されること", "-1.79e308", true);
        checkValidateDouble("文字列形式で負の最大値（-2.23e-308）の場合にtrueが返却されること", "-2.23e-308", true);
        checkValidateDouble("文字列形式で正の最小値（2.23e-308）の場合にtrueが返却されること", "2.23e-308", true);
        checkValidateDouble("文字列形式で正の最大値（1.79e308）の場合にtrueが返却されること", "1.79e308", true);
        checkValidateDouble("文字列形式で負の最小値より小さい値（-1.791e308）の場合にfalseが返却されること", "-1.791e308", false);
        checkValidateDouble("文字列形式で負の最小値より大きい値（-1.789e308）の場合にtrueが返却されること", "-1.789e308", true);
        checkValidateDouble("文字列形式で負の最大値より小さい値（-2.231e-308）の場合にtrueが返却されること", "-2.231e-308", true);
        checkValidateDouble("文字列形式で負の最大値より大きい値（-2.229e-308）の場合にfalseが返却されること", "-2.229e-308", false);
        checkValidateDouble("文字列形式で正の最小値より小さい値（2.229e-308）の場合にfalseが返却されること", "2.229e-308", false);
        checkValidateDouble("文字列形式で正の最小値より大きい値（2.231e-308）の場合にtrueが返却されること", "2.231e-308", true);
        checkValidateDouble("文字列形式で正の最大値より小さい値（1.789e308）の場合にtrueが返却されること", "1.789e308", true);
        checkValidateDouble("文字列形式で正の最大値より大きい値（1.791e308）の場合にfalseが返却されること", "1.791e308", false);
        checkValidateDouble("文字列形式で0の場合にtrueが返却されること", "0", true);
        checkValidateDouble("文字列形式で負の最小値（-1.79e308d）の場合にtrueが返却されること", "-1.79e308d", true);
        checkValidateDouble("文字列形式で負の最大値（-2.23e-308d）の場合にtrueが返却されること", "-2.23e-308d", true);
        checkValidateDouble("文字列形式で正の最小値（2.23e-308d）の場合にtrueが返却されること", "2.23e-308d", true);
        checkValidateDouble("文字列形式で正の最大値（1.79e308d）の場合にtrueが返却されること", "1.79e308d", true);
        checkValidateDouble("文字列形式で負の最小値より小さい値（-1.791e308d）の場合にfalseが返却されること", "-1.791e308d", false);
        checkValidateDouble("文字列形式で負の最小値より大きい値（-1.789e308d）の場合にtrueが返却されること", "-1.789e308d", true);
        checkValidateDouble("文字列形式で負の最大値より小さい値（-2.231e-308d）の場合にtrueが返却されること", "-2.231e-308d", true);
        checkValidateDouble("文字列形式で負の最大値より大きい値（-2.229e-308d）の場合にfalseが返却されること", "-2.229e-308d", false);
        checkValidateDouble("文字列形式で正の最小値より小さい値（2.229e-308d）の場合にfalseが返却されること", "2.229e-308d", false);
        checkValidateDouble("文字列形式で正の最小値より大きい値（2.231e-308d）の場合にtrueが返却されること", "2.231e-308d", true);
        checkValidateDouble("文字列形式で正の最大値より小さい値（1.789e308d）の場合にtrueが返却されること", "1.789e308d", true);
        checkValidateDouble("文字列形式で正の最大値より大きい値（1.791e308d）の場合にfalseが返却されること", "1.791e308d", false);
        checkValidateDouble("文字列形式で0dの場合にtrueが返却されること", "0d", true);
        checkValidateDouble("文字列形式で負の最小値（-1.79e308D）の場合にtrueが返却されること", "-1.79e308D", true);
        checkValidateDouble("文字列形式で負の最大値（-2.23e-308D）の場合にtrueが返却されること", "-2.23e-308D", true);
        checkValidateDouble("文字列形式で正の最小値（2.23e-308D）の場合にtrueが返却されること", "2.23e-308D", true);
        checkValidateDouble("文字列形式で正の最大値（1.79e308D）の場合にtrueが返却されること", "1.79e308D", true);
        checkValidateDouble("文字列形式で負の最小値より小さい値（-1.791e308D）の場合にfalseが返却されること", "-1.791e308D", false);
        checkValidateDouble("文字列形式で負の最小値より大きい値（-1.789e308D）の場合にtrueが返却されること", "-1.789e308D", true);
        checkValidateDouble("文字列形式で負の最大値より小さい値（-2.231e-308D）の場合にtrueが返却されること", "-2.231e-308D", true);
        checkValidateDouble("文字列形式で負の最大値より大きい値（-2.229e-308D）の場合にfalseが返却されること", "-2.229e-308D", false);
        checkValidateDouble("文字列形式で正の最小値より小さい値（2.229e-308D）の場合にfalseが返却されること", "2.229e-308D", false);
        checkValidateDouble("文字列形式で正の最小値より大きい値（2.231e-308D）の場合にtrueが返却されること", "2.231e-308D", true);
        checkValidateDouble("文字列形式で正の最大値より小さい値（1.789e308D）の場合にtrueが返却されること", "1.789e308D", true);
        checkValidateDouble("文字列形式で正の最大値より大きい値（1.791e308D）の場合にfalseが返却されること", "1.791e308D", false);
        checkValidateDouble("文字列形式で0Dの場合にtrueが返却されること", "0D", true);
        checkValidateDouble("文字列の場合にfalseが返却されること", "parseError", false);
    }

    /**
     * Double型の値の有効範囲チェックのテスト.
     * @param testComment テスト内容
     * @param inputDoubleValue バリデート対象の入力値
     * @param expectedReturnValue 期待する返却値
     * @throws Exception Exception
     */
    public void checkValidateDouble(String testComment,
            double inputDoubleValue,
            boolean expectedReturnValue) throws Exception {
        boolean result = ODataUtils.validateDouble(inputDoubleValue);
        assertEquals(testComment, expectedReturnValue, result);
    }

    /**
     * Double型の値の有効範囲チェックのテスト.
     * @param testComment テスト内容
     * @param inputStringValue バリデート対象の入力値
     * @param expectedReturnValue 期待する返却値
     * @throws Exception Exception
     */
    public void checkValidateDouble(String testComment,
            String inputStringValue,
            boolean expectedReturnValue) throws Exception {
        boolean result = ODataUtils.validateDouble(inputStringValue);
        assertEquals(testComment, expectedReturnValue, result);
    }

    /**
     * Test validateRegEx().
     * Normal test.
     * Format match.
     */
    @Test
    public void validateRegEx_Normal_match() {
        String str = "123test-_";
        String pFormat = Common.PATTERN_NAME;
        assertThat(ODataUtils.validateRegEx(str, pFormat), is(true));
    }

    /**
     * Test validateRegEx().
     * Normal test.
     * Format not match.
     */
    @Test
    public void validateRegEx_Normal_not_match() {
        String str = "-_123test";
        String pFormat = Common.PATTERN_NAME;
        assertThat(ODataUtils.validateRegEx(str, pFormat), is(false));
    }

    /**
     * Test validateClassUrl().
     * Normal test.
     * Scheme is null.
     */
    @Test
    public void validateClassUrl_Normal_scheme_is_null() {
        String str = "./dummyBox/dummyRelation";
        String pFormat = Common.PATTERN_RELATION_CLASS_URL;
        assertThat(ODataUtils.validateClassUrl(str, pFormat), is(false));
    }

    /**
     * Test validateClassUrl().
     * Normal test.
     * Scheme is not allowed format.
     */
    @Test
    public void validateClassUrl_Normal_scheme_is_not_allowed_format() {
        String str = "file://dummyFile";
        String pFormat = Common.PATTERN_RELATION_CLASS_URL;
        assertThat(ODataUtils.validateClassUrl(str, pFormat), is(false));
    }

    /**
     * Test validateClassUrl().
     * Normal test.
     * String length exceeded max.
     */
    @Test
    public void validateClassUrl_Normal_str_length_exceeded_max() {
        StringBuilder builder = new StringBuilder("http://");
        int schemeLength = builder.length();
        for (int i = 0; i < (ODataUtils.URI_MAX_LENGTH + 1 - schemeLength); i++) {
            builder.append("a");
        }
        String str = new String(builder);
        String pFormat = Common.PATTERN_RELATION_CLASS_URL;
        assertThat(ODataUtils.validateClassUrl(str, pFormat), is(false));
    }

    /**
     * Test validateClassUrl().
     * Normal test.
     * Not in URI format.
     */
    @Test
    public void validateClassUrl_Normal_not_in_uri_format() {
        String str = "\\ %";
        String pFormat = Common.PATTERN_RELATION_CLASS_URL;
        assertThat(ODataUtils.validateClassUrl(str, pFormat), is(false));
    }

    /**
     * Test validateClassUrl().
     * Normal test.
     * Not match regular expression.
     */
    @Test
    public void validateClassUrl_Normal_not_match_regular_expression() {
        String str = "http://personium/dummyCell/__relation/dummyBox/dummyRelation";
        String pFormat = Common.PATTERN_RELATION_CLASS_URL;
        assertThat(ODataUtils.validateClassUrl(str, pFormat), is(false));
    }

    /**
     * Test validateClassUrl().
     * Normal test.
     * Match regular expression.
     */
    @Test
    public void validateClassUrl_Normal_match_regular_expression() {
        String str = "http://personium/appCell/__relation/__/dummyRelation";
        String pFormat = Common.PATTERN_RELATION_CLASS_URL;
        assertThat(ODataUtils.validateClassUrl(str, pFormat), is(true));
    }

}
