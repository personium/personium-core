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
package com.fujitsu.dc.core.odata;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Unit;

/**
 * DcJsonWriterユニットテストクラス.
 */
@RunWith(Enclosed.class)
@Category({Unit.class })
public class DcJsonWriterTest {

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
        String expectedReturnValue;

        /**
         * コンストラクタ.
         * @param testComment テスト内容
         * @param inputValue 入力値
         * @param expectedReturnValue 期待する返却値
         */
        Fixture(String testComment,
                double inputValue,
                String expectedReturnValue) {
            this.testComment = testComment;
            this.inputValue = inputValue;
            this.expectedReturnValue = expectedReturnValue;
        }
    }

    /**
     * Double型の値の出力フォーマットテスト.
     */
    @RunWith(Theories.class)
    public static class ValidateDoubleTest {

        /**
         * Double型の出力フォーマットチェックテストパターンを作成.
         * @return テストパターン
         */
        @DataPoints
        public static Fixture[] getFixture() {
            Fixture[] datas = {
                    new Fixture("1.0が1になること", 1.0d, "1"),
                    new Fixture("1.00000が1になること", 1.000000d, "1"),
                    new Fixture("1234567が1234567になること", 1234567d, "1234567"),
                    new Fixture("12345678が12345678になること", 12345678d, "12345678"),
                    new Fixture("123456789012345が123456789012345になること", 123456789012345d, "123456789012345"),
                    new Fixture("1234567890123456が1.234567890123456E15になること",
                            1234567890123456d, "1.234567890123456E15"),
                    new Fixture("1234567890123456789が1.23456789012345677E18になること",
                            1234567890123456789d, "1.23456789012345677E18"),
                    new Fixture("0.1が0.1になること", 0.1d, "0.1"),
                    new Fixture("0.1234567890が0.123456789になること", 0.1234567890d, "0.123456789"),
                    new Fixture("0.12345678901234が0.12345678901234になること", 0.12345678901234d, "0.12345678901234"),
                    new Fixture("0.123456789012345が0.123456789012345になること", 0.123456789012345d, "0.123456789012345"),
                    new Fixture("0.12345678901234567が0.12345678901234566になること",
                            0.12345678901234567d, "0.12345678901234566"),
                    new Fixture("1e1が10になること", 1e1d, "10"),
                    new Fixture("1e6が1000000になること", 1e6d, "1000000"),
                    new Fixture("1e7が10000000になること", 1e7d, "10000000"),
                    new Fixture("1e14が100000000000000になること", 1e14d, "100000000000000"),
                    new Fixture("1e15が1e15になること", 1e15d, "1.0E15"),
                    new Fixture("1e23が9.999999999999999E22になること", 1e23d, "9.999999999999999E22"),
                    new Fixture("1e-1が0.1になること", 1e-1d, "0.1"),
                    new Fixture("1e-3が0.001になること", 1e-3d, "0.001"),
                    new Fixture("1e-4が0.0001になること", 1e-4d, "0.0001"),
                    new Fixture("1e-14が0.00000000000001になること", 1e-14d, "0.00000000000001"),
                    new Fixture("1e-15が1e-15になること", 1e-15d, "1.0E-15")
            };
            return datas;
        }

        /**
         * Double型の値の有効範囲チェックのテスト.
         * @param f テストパターン
         */
        @Theory
        public void Double型の値の有効範囲チェック(Fixture f) {
            StringWriter writer = null;
            try {
                writer = new StringWriter();
                DcJsonWriter jsonWriter = new DcJsonWriter(writer);
                jsonWriter.writeNumber(f.inputValue);
                String actual = writer.toString();
                assertEquals(f.testComment, f.expectedReturnValue, actual);
            } finally {
                IOUtils.closeQuietly(writer);
            }
        }
    }
}
