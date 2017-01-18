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
package com.fujitsu.dc.core.utils;

import static org.junit.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Unit;

/**
 * ODataUtilsユニットテストクラス.
 */
@RunWith(Enclosed.class)
@Category({Unit.class })
public class ODataUtilsTest {

    /**
     * テスト用Fixture。
     */
    static class FixtureForDouble {
        String testComment;
        /**
         * Double型の入力値.
         */
        double inputDoubleValue;
        /**
         * String型の入力値.
         */
        String inputStringValue;
        /**
         * 期待する返却値.
         */
        boolean expectedReturnValue;

        /**
         * コンストラクタ.
         * @param testComment テスト内容
         * @param inputValue バリデート対象の入力値
         * @param expectedReturnValue 期待する返却値
         */
        FixtureForDouble(String testComment,
                double inputValue,
                boolean expectedReturnValue) {
            this.testComment = testComment;
            this.inputDoubleValue = inputValue;
            this.expectedReturnValue = expectedReturnValue;
        }

        /**
         * コンストラクタ.
         * @param testComment テスト内容
         * @param inputValue バリデート対象の入力値
         * @param expectedReturnValue 期待する返却値
         */
        FixtureForDouble(String testComment,
                String inputValue,
                boolean expectedReturnValue) {
            this.testComment = testComment;
            this.inputStringValue = inputValue;
            this.expectedReturnValue = expectedReturnValue;
        }
    }

    /**
     * 入力値をDouble型とした値の有効範囲チェックのテスト.
     * 有効範囲 ± 2.23e -308 から ± 1.79e +308
     */
    @RunWith(Theories.class)
    public static class ValidateDoubleInputDoubleTest {

        /**
         * Double型の有効値チェックテストパターンを作成.
         * @return テストパターン
         */
        @DataPoints
        public static FixtureForDouble[] getFixture() {
            FixtureForDouble[] datas = {
                    new FixtureForDouble("負の最小値（-1.79e308d）の場合にtrueが返却されること", -1.79e308d, true),
                    new FixtureForDouble("負の最大値（-2.23e-308d）の場合にtrueが返却されること", -2.23e-308d, true),
                    new FixtureForDouble("正の最小値（2.23e-308d）の場合にtrueが返却されること", 2.23e-308d, true),
                    new FixtureForDouble("正の最大値（1.79e308d）の場合にtrueが返却されること", 1.79e308d, true),
                    new FixtureForDouble("負の最小値より小さい値（-1.791e308d）の場合にfalseが返却されること", -1.791e308d, false),
                    new FixtureForDouble("負の最小値より大きい値（-1.789e308d）の場合にtrueが返却されること", -1.789e308d, true),
                    new FixtureForDouble("負の最大値より小さい値（-2.231e-308d）の場合にtrueが返却されること", -2.231e-308d, true),
                    new FixtureForDouble("負の最大値より大きい値（-2.229e-308d）の場合にfalseが返却されること", -2.229e-308d, false),
                    new FixtureForDouble("正の最小値より小さい値（2.229e-308d）の場合にfalseが返却されること", 2.229e-308d, false),
                    new FixtureForDouble("正の最小値より大きい値（2.231e-308d）の場合にtrueが返却されること", 2.231e-308d, true),
                    new FixtureForDouble("正の最大値より小さい値（1.789e308d）の場合にtrueが返却されること", 1.789e308d, true),
                    new FixtureForDouble("正の最大値より大きい値（1.791e308d）の場合にfalseが返却されること", 1.791e308d, false),
                    new FixtureForDouble("0dの場合にtrueが返却されること", 0d, true),
            };
            return datas;
        }

        /**
         * Double型の値の有効範囲チェックのテスト.
         * @param f テストパターン
         * @throws Exception Exception
         */
        @Theory
        public void Double型の値の有効範囲チェック(FixtureForDouble f) throws Exception {
            boolean result = ODataUtils.validateDouble(f.inputDoubleValue);
            assertEquals(f.testComment, f.expectedReturnValue, result);
        }
    }

    /**
     * 入力値を文字列としたDouble型の値の有効範囲チェックのテスト.
     * 有効範囲 ± 2.23e -308 から ± 1.79e +308
     */
    @RunWith(Theories.class)
    public static class ValidateDoubleInputStringTest {

        /**
         * Double型の有効値チェックテストパターンを作成.
         * @return テストパターン
         */
        @DataPoints
        public static FixtureForDouble[] getFixture() {
            FixtureForDouble[] datas = {
                    new FixtureForDouble("文字列形式で負の最小値（-1.79e308）の場合にtrueが返却されること", "-1.79e308", true),
                    new FixtureForDouble("文字列形式で負の最大値（-2.23e-308）の場合にtrueが返却されること", "-2.23e-308", true),
                    new FixtureForDouble("文字列形式で正の最小値（2.23e-308）の場合にtrueが返却されること", "2.23e-308", true),
                    new FixtureForDouble("文字列形式で正の最大値（1.79e308）の場合にtrueが返却されること", "1.79e308", true),
                    new FixtureForDouble("文字列形式で負の最小値より小さい値（-1.791e308）の場合にfalseが返却されること", "-1.791e308", false),
                    new FixtureForDouble("文字列形式で負の最小値より大きい値（-1.789e308）の場合にtrueが返却されること", "-1.789e308", true),
                    new FixtureForDouble("文字列形式で負の最大値より小さい値（-2.231e-308）の場合にtrueが返却されること", "-2.231e-308", true),
                    new FixtureForDouble("文字列形式で負の最大値より大きい値（-2.229e-308）の場合にfalseが返却されること", "-2.229e-308", false),
                    new FixtureForDouble("文字列形式で正の最小値より小さい値（2.229e-308）の場合にfalseが返却されること", "2.229e-308", false),
                    new FixtureForDouble("文字列形式で正の最小値より大きい値（2.231e-308）の場合にtrueが返却されること", "2.231e-308", true),
                    new FixtureForDouble("文字列形式で正の最大値より小さい値（1.789e308）の場合にtrueが返却されること", "1.789e308", true),
                    new FixtureForDouble("文字列形式で正の最大値より大きい値（1.791e308）の場合にfalseが返却されること", "1.791e308", false),
                    new FixtureForDouble("文字列形式で0の場合にtrueが返却されること", "0", true),
                    new FixtureForDouble("文字列形式で負の最小値（-1.79e308d）の場合にtrueが返却されること", "-1.79e308d", true),
                    new FixtureForDouble("文字列形式で負の最大値（-2.23e-308d）の場合にtrueが返却されること", "-2.23e-308d", true),
                    new FixtureForDouble("文字列形式で正の最小値（2.23e-308d）の場合にtrueが返却されること", "2.23e-308d", true),
                    new FixtureForDouble("文字列形式で正の最大値（1.79e308d）の場合にtrueが返却されること", "1.79e308d", true),
                    new FixtureForDouble("文字列形式で負の最小値より小さい値（-1.791e308d）の場合にfalseが返却されること", "-1.791e308d", false),
                    new FixtureForDouble("文字列形式で負の最小値より大きい値（-1.789e308d）の場合にtrueが返却されること", "-1.789e308d", true),
                    new FixtureForDouble("文字列形式で負の最大値より小さい値（-2.231e-308d）の場合にtrueが返却されること", "-2.231e-308d", true),
                    new FixtureForDouble("文字列形式で負の最大値より大きい値（-2.229e-308d）の場合にfalseが返却されること", "-2.229e-308d", false),
                    new FixtureForDouble("文字列形式で正の最小値より小さい値（2.229e-308d）の場合にfalseが返却されること", "2.229e-308d", false),
                    new FixtureForDouble("文字列形式で正の最小値より大きい値（2.231e-308d）の場合にtrueが返却されること", "2.231e-308d", true),
                    new FixtureForDouble("文字列形式で正の最大値より小さい値（1.789e308d）の場合にtrueが返却されること", "1.789e308d", true),
                    new FixtureForDouble("文字列形式で正の最大値より大きい値（1.791e308d）の場合にfalseが返却されること", "1.791e308d", false),
                    new FixtureForDouble("文字列形式で0dの場合にtrueが返却されること", "0d", true),
                    new FixtureForDouble("文字列形式で負の最小値（-1.79e308D）の場合にtrueが返却されること", "-1.79e308D", true),
                    new FixtureForDouble("文字列形式で負の最大値（-2.23e-308D）の場合にtrueが返却されること", "-2.23e-308D", true),
                    new FixtureForDouble("文字列形式で正の最小値（2.23e-308D）の場合にtrueが返却されること", "2.23e-308D", true),
                    new FixtureForDouble("文字列形式で正の最大値（1.79e308D）の場合にtrueが返却されること", "1.79e308D", true),
                    new FixtureForDouble("文字列形式で負の最小値より小さい値（-1.791e308D）の場合にfalseが返却されること", "-1.791e308D", false),
                    new FixtureForDouble("文字列形式で負の最小値より大きい値（-1.789e308D）の場合にtrueが返却されること", "-1.789e308D", true),
                    new FixtureForDouble("文字列形式で負の最大値より小さい値（-2.231e-308D）の場合にtrueが返却されること", "-2.231e-308D", true),
                    new FixtureForDouble("文字列形式で負の最大値より大きい値（-2.229e-308D）の場合にfalseが返却されること", "-2.229e-308D", false),
                    new FixtureForDouble("文字列形式で正の最小値より小さい値（2.229e-308D）の場合にfalseが返却されること", "2.229e-308D", false),
                    new FixtureForDouble("文字列形式で正の最小値より大きい値（2.231e-308D）の場合にtrueが返却されること", "2.231e-308D", true),
                    new FixtureForDouble("文字列形式で正の最大値より小さい値（1.789e308D）の場合にtrueが返却されること", "1.789e308D", true),
                    new FixtureForDouble("文字列形式で正の最大値より大きい値（1.791e308D）の場合にfalseが返却されること", "1.791e308D", false),
                    new FixtureForDouble("文字列形式で0Dの場合にtrueが返却されること", "0D", true),
                    new FixtureForDouble("文字列の場合にfalseが返却されること", "parseError", false)
            };
            return datas;
        }

        /**
         * Double型の値の有効範囲チェックのテスト.
         * @param f テストパターン
         * @throws Exception Exception
         */
        @Theory
        public void Double型の値の有効範囲チェック(FixtureForDouble f) throws Exception {
            boolean result = ODataUtils.validateDouble(f.inputStringValue);
            assertEquals(f.testComment, f.expectedReturnValue, result);
        }
    }
}
