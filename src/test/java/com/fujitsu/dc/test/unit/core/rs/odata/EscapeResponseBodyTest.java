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
package com.fujitsu.dc.test.unit.core.rs.odata;

import static org.junit.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.rs.odata.ODataEntityResource;
import com.fujitsu.dc.test.categories.Unit;

/**
 */
@Category({Unit.class })
@RunWith(Enclosed.class)
public class EscapeResponseBodyTest {

    /**
     * テスト用Fixture.
     */
    static class Fixture {
        String testComment;
        /**
         * 入力値.
         */
        String inputValue;
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
                String inputValue,
                String expectedReturnValue) {
            this.testComment = testComment;
            this.inputValue = inputValue;
            this.expectedReturnValue = expectedReturnValue;
        }
    }

    /**
     * エスケープ処理のテスト.
     */
    @RunWith(Theories.class)
    public static class TheoriesTest {

        /**
         * 制御コードのエスケープテストパターンを作成.
         * @return テストパターン
         */
        @DataPoints
        public static Fixture[] getFixture() {
            Fixture[] datas = {
                    new Fixture("制御コードが\u0000の場合変換されること", "\u0000", "\\u0000"),
                    new Fixture("制御コードが\u0001の場合変換されること", "\u0001", "\\u0001"),
                    new Fixture("制御コードが\u0002の場合変換されること", "\u0002", "\\u0002"),
                    new Fixture("制御コードが\u0003の場合変換されること", "\u0003", "\\u0003"),
                    new Fixture("制御コードが\u0004の場合変換されること", "\u0004", "\\u0004"),
                    new Fixture("制御コードが\u0005の場合変換されること", "\u0005", "\\u0005"),
                    new Fixture("制御コードが\u0006の場合変換されること", "\u0006", "\\u0006"),
                    new Fixture("制御コードが\u0007の場合変換されること", "\u0007", "\\u0007"),
                    new Fixture("制御コードが\u0008の場合変換されること", "\u0008", "\\u0008"),
                    new Fixture("制御コードが\u0009の場合変換されること", "\u0009", "\\u0009"),
                    new Fixture("制御コードが\u000Bの場合変換されること", "\u000B", "\\u000B"),
                    new Fixture("制御コードが\u000Cの場合変換されること", "\u000C", "\\u000C"),
                    new Fixture("制御コードが\u000Eの場合変換されること", "\u000E", "\\u000E"),
                    new Fixture("制御コードが\u000Fの場合変換されること", "\u000F", "\\u000F"),
                    new Fixture("制御コードが\u0010の場合変換されること", "\u0010", "\\u0010"),
                    new Fixture("制御コードが\u0011の場合変換されること", "\u0011", "\\u0011"),
                    new Fixture("制御コードが\u0012の場合変換されること", "\u0012", "\\u0012"),
                    new Fixture("制御コードが\u0013の場合変換されること", "\u0013", "\\u0013"),
                    new Fixture("制御コードが\u0014の場合変換されること", "\u0014", "\\u0014"),
                    new Fixture("制御コードが\u0015の場合変換されること", "\u0015", "\\u0015"),
                    new Fixture("制御コードが\u0016の場合変換されること", "\u0016", "\\u0016"),
                    new Fixture("制御コードが\u0017の場合変換されること", "\u0017", "\\u0017"),
                    new Fixture("制御コードが\u0018の場合変換されること", "\u0018", "\\u0018"),
                    new Fixture("制御コードが\u0019の場合変換されること", "\u0019", "\\u0019"),
                    new Fixture("制御コードが\u001Aの場合変換されること", "\u001A", "\\u001A"),
                    new Fixture("制御コードが\u001Bの場合変換されること", "\u001B", "\\u001B"),
                    new Fixture("制御コードが\u001Cの場合変換されること", "\u001C", "\\u001C"),
                    new Fixture("制御コードが\u001Dの場合変換されること", "\u001D", "\\u001D"),
                    new Fixture("制御コードが\u001Eの場合変換されること", "\u001E", "\\u001E"),
                    new Fixture("制御コードが\u001Fの場合変換されること", "\u001F", "\\u001F"),
                    new Fixture("制御コードが\u007Fの場合変換されること", "\u007F", "\\u007F"),
                    new Fixture("制御コードが\u0021の場合変換されないこと", "\u0021", "\u0021"),
                    new Fixture("制御コードが\u0031の場合変換されないこと", "\u0031", "\u0031"),
                    new Fixture("制御コードが\u0041の場合変換されないこと", "\u0041", "\u0041"),
                    new Fixture("末尾に制御コードが存在する場合変換されること", "test\u0000", "test\\u0000"),
                    new Fixture("制御コードが複数存在する場合変換されること", "\u0000test\u0001test", "\\u0000test\\u0001test"),
                    new Fixture("エスケープされた制御コードが存在する場合変換されないこと", "\\u0000", "\\u0000"),
            };
            return datas;
        }

        /**
         * 制御コードのエスケープのテスト.
         * @param f テストパターン
         * @throws Exception Exception
         */
        @Theory
        public void 制御コードのエスケープのテスト(Fixture f) throws Exception {
            ODataEntityResource resource = new ODataEntityResource();
            String actual = resource.escapeResponsebody(f.inputValue);
            assertEquals(f.testComment, f.expectedReturnValue, actual);
        }
    }
}
