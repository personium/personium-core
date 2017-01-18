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
package com.fujitsu.dc.core.rs.odata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Unit;

/**
 * AbstractODataResourceユニットテストクラス.
 */
@RunWith(Enclosed.class)
@Category({Unit.class })
public class AbstractODataResourceTest {

    /**
     * テスト用Fixture。
     */
    static class Fixture {
        String testComment;
        /**
         * 入力値.
         */
        double inputValue;
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
        Fixture(String testComment,
                double inputValue,
                boolean expectedReturnValue) {
            this.testComment = testComment;
            this.inputValue = inputValue;
            this.expectedReturnValue = expectedReturnValue;
        }
    }

    /**
     * Double型の値の有効範囲チェックのテスト.
     * 有効範囲 ± 2.23e -308 から ± 1.79e +308
     */
    @RunWith(Theories.class)
    public static class ValidateDoubleTest {

        /**
         * Double型の有効値チェックテストパターンを作成.
         * @return テストパターン
         */
        @DataPoints
        public static Fixture[] getFixture() {
            Fixture[] datas = {
                    new Fixture("負の最小値（-1.79e308d）の場合に例外がスローされないこと", -1.79e308d, true),
                    new Fixture("負の最大値（-2.23e-308d）の場合に例外がスローされないこと", -2.23e-308d, true),
                    new Fixture("正の最小値（2.23e-308d）の場合に例外がスローされないこと", 2.23e-308d, true),
                    new Fixture("正の最大値（1.79e308d）の場合に例外がスローされないこと", 1.79e308d, true),
                    new Fixture("負の最小値より小さい値（-1.791e308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", -1.791e308d, false),
                    new Fixture("負の最小値より大きい値（-1.789e308d）の場合に例外がスローされないこと", -1.789e308d, true),
                    new Fixture("負の最大値より小さい値（-2.231e-308d）の場合に例外がスローされないこと", -2.231e-308d, true),
                    new Fixture("負の最大値より大きい値（-2.229e-308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", -2.229e-308d, false),
                    new Fixture("正の最小値より小さい値（2.229e-308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", 2.229e-308d, false),
                    new Fixture("正の最小値より大きい値（2.231e-308d）の場合に例外がスローされないこと", 2.231e-308d, true),
                    new Fixture("正の最大値より小さい値（1.789e308d）の場合に例外がスローされないこと", 1.789e308d, true),
                    new Fixture("正の最大値より大きい値（1.791e308d）の場合に例外コード[PR400-OD-0006]の例外がスローされること", 1.791e308d, false),
                    new Fixture("0の場合にtrueが返却されること", 0d, true)
            };
            return datas;
        }

        /**
         * Double型の値の有効範囲チェックのテスト.
         * @param f テストパターン
         * @throws Exception Exception
         */
        @Theory
        public void Double型の値の有効範囲チェック(Fixture f) throws Exception {
            ODataEntityResource resource = new ODataEntityResource();
            Method method = AbstractODataResource.class.getDeclaredMethod("validateDynamicProperty",
                    new Class[] {OProperty.class});
            method.setAccessible(true);
            OProperty<Double> property = OProperties.double_("testKey", f.inputValue);
            boolean valildResult = true;
            try {
                method.invoke(resource, property);
            } catch (InvocationTargetException ex) {
                if (ex.getCause() instanceof DcCoreException) {
                    DcCoreException e = (DcCoreException) ex.getCause();
                    if (DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode().equals(e.getCode())) {
                        valildResult = false;
                    } else {
                        fail(f.testComment + ": 期待したエラーコードではない. 例外コード:[" + e.getCode() + "]");
                    }
                }
            }
            assertEquals(f.testComment, f.expectedReturnValue, valildResult);
        }
    }
}
