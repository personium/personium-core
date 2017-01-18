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
package com.fujitsu.dc.core.http.header;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.test.categories.Unit;

/**
 * JdbcAdsユニットテストクラス.
 */
@RunWith(Enclosed.class)
@Category({ Unit.class })
public class RangeHeaderHandlerTest {
    static Logger log = LoggerFactory.getLogger(RangeHeaderHandlerTest.class);

    /**
     * デフォルトコンストラクタ.
     */
    public void RangeHeaderHandler() {

    }

    /**
     * テスト用Fixture。
     */
    static class Fixture {
        int caseNo;
        /**
         * 入力するRangeヘッダフィールド.
         */
        String rangeHeaderField;
        /**
         * 入力するコンテンツのサイズ.
         */
        long contentSize;

        /**
         * 期待するfirst-byte-posの値.
         */
        long expectedFirstBytePos;
        /**
         * 期待するlast-byte-posの値.
         */
        long expectedLastBytePos;
        /**
         * 期待するコンテンツのサイズ.
         */
        long expectedContentSize;
        /**
         * 期待するContent-Rangeヘッダの文字列.
         */
        String expectedContentRange;

        /**
         * 正常系用コンストラクタ.
         * @param caseNo テスト番号
         * @param rangeHeaderField 入力するRangeヘッダフィールド
         * @param contentSize 入力するコンテンツのサイズ
         * @param expectedFirstBytePos 期待するfirst-byte-posの値
         * @param expectedLastBytePos 期待するlast-byte-posの値
         * @param expectedContentSize 期待するコンテンツのサイズ
         * @param expectedContentRange 期待するContent-Rangeヘッダの文字列
         */
        Fixture(int caseNo,
                String rangeHeaderField,
                long contentSize,
                long expectedFirstBytePos,
                long expectedLastBytePos,
                long expectedContentSize,
                String expectedContentRange) {
            this.caseNo = caseNo;
            this.rangeHeaderField = rangeHeaderField;
            this.contentSize = contentSize;
            this.expectedFirstBytePos = expectedFirstBytePos;
            this.expectedLastBytePos = expectedLastBytePos;
            this.expectedContentSize = expectedContentSize;
            this.expectedContentRange = expectedContentRange;
        }

        /**
         * 異常系用コンストラクタ.
         * @param caseNo テスト番号
         * @param rangeHeaderField 入力するRangeヘッダフィールド
         * @param contentSize 入力するコンテンツのサイズ
         */
        Fixture(int caseNo, String rangeHeaderField, long contentSize) {
            this.caseNo = caseNo;
            this.rangeHeaderField = rangeHeaderField;
            this.contentSize = contentSize;
        }
    }

    /**
     * 正常系テスト.
     */
    @RunWith(Theories.class)
    public static class Normal206Test {

        /**
         * 正常系テストデータ. 正常なレンジヘッダフィールドが指定された場合のテスト
         * @return テストデータ
         */
        @DataPoints
        public static Fixture[] getFixture() {
            Fixture[] datas = {
                    // ※前提 first-byte-pos:a last-byte-pos:b suffix-length:c entitylength:Z
                    // a = 0 < b
                    new Fixture(1, "bytes=0-26", 100, 0, 26, 27, "bytes 0-26/100"),
                    // a ≠ 0 < b
                    new Fixture(2, "bytes=3-7", 100, 3, 7, 5, "bytes 3-7/100"),
                    // bがない場合は、entitybodyの最後までを終端として扱う
                    new Fixture(3, "bytes=10-", 100, 10, 99, 90, "bytes 10-99/100"),
                    // Z<bの場合は、entitybodyの最後までを終端として扱う
                    new Fixture(4, "bytes=10-150", 100, 10, 99, 90, "bytes 10-99/100"),
                    // c<Zの場合（開始が省略）は、ファイルの終端からc分を扱う
                    new Fixture(5, "bytes=-10", 100, 90, 99, 10, "bytes 90-99/100"),
                    // c>Zの場合(開始が省略かつファイルサイズより指定が大きい)は、ファイル全体を扱う
                    new Fixture(6, "bytes=-150", 100, 0, 99, 100, "bytes 0-99/100"),
                    // a=b場合
                    new Fixture(7, "bytes=10-10", 100, 10, 10, 1, "bytes 10-10/100"),
                    // a=b=Zの場合
                    new Fixture(8, "bytes=99-99", 100, 99, 99, 1, "bytes 99-99/100") };
            return datas;
        }

        /**
         * 正常系.
         * @param f テストデータ
         * @throws Exception Exception
         */
        @Theory
        public void 正常系206(Fixture f) throws Exception {
            // 単一バイトレンジ指定のテストなので0固定
            int byteRangeSpecIndex = 0;

            // Rangeヘッダパース
            RangeHeaderHandler range = RangeHeaderHandler.parse(f.rangeHeaderField, f.contentSize);

            // byte-range-setの数チェック
            assertEquals(1, range.getByteRangeSpecCount());

            List<ByteRangeSpec> brss = range.getByteRangeSpecList();
            ByteRangeSpec brs = brss.get(byteRangeSpecIndex);

            // 開始・終了値チェック
            assertEquals(f.expectedFirstBytePos, brs.getFirstBytePos());
            assertEquals(f.expectedLastBytePos, brs.getLastBytePos());

            // Rangeヘッダが有効であることのチェック
            assertEquals(true, range.isValid());

            // 範囲外の指定でないことをチェック
            assertEquals(true, range.isSatisfiable());

            // Content-Lengthのチェック
            assertEquals(f.expectedContentSize, brs.getContentLength());

            // Content-Rangeヘッダのチェック
            assertEquals(f.expectedContentRange, brs.makeContentRangeHeaderField());
        }
    }

    /**
     * 無視系ーヘッダ無視パターンテスト.
     */
    @RunWith(Theories.class)
    public static class IgnoreHeader200Test {

        /**
         * 無視系ーヘッダ無視パターンテストデータ. 無効なレンジヘッダフィールドの指定のテスト
         * @return テストデータ
         */
        @DataPoints
        public static Fixture[] getFixture() {
            Fixture[] datas = {
                    // ※前提 first-byte-pos:a last-byte-pos:b suffix-length:c entitybody:Z
                    // b < Z < a（開始と終端がひっくり返っている）
                    new Fixture(1, "bytes=500-10", 100),
                    // b < a < Z（開始と終端がひっくり返っている）
                    new Fixture(2, "bytes=50-10", 100),
                    // -がない場合
                    new Fixture(3, "bytes=50", 100),
                    // c = 0
                    new Fixture(4, "bytes=-0", 100),
                    // byte-unitがない場合
                    new Fixture(5, "hoge=10-20", 100),
                    // aが文字列の場合
                    new Fixture(6, "bytes=hoge-20", 100),
                    // bが文字列の場合
                    new Fixture(7, "bytes=10-hoge", 100),
                    // aが負の数の場合
                    new Fixture(8, "bytes=-10-11", 100),
                    // bが負の数の場合
                    new Fixture(8, "bytes=10--11", 100)};
            return datas;
        }

        /**
         * 異常系ーヘッダ無視パターン.
         * @param f テストデータ
         * @throws Exception Exception
         */
        @Theory
        public void 異常系ーヘッダ無視パターン(Fixture f) throws Exception {
            // Rangeヘッダパース
            RangeHeaderHandler range = RangeHeaderHandler.parse(f.rangeHeaderField, f.contentSize);

            // byte-range-setの数チェック
            assertEquals(0, range.getByteRangeSpecCount());

            // Rangeヘッダが無効であることのチェック（無効なのでRangeヘッダ無視扱い）
            assertEquals(false, range.isValid());

            // 範囲外の指定でないことをチェック
            assertEquals(false, range.isSatisfiable());
        }
    }

    /**
     * 異常系ー416レスポンスパターンテスト.
     */
    @RunWith(Theories.class)
    public static class Abnormal416Test {

        /**
         * 異常系ー416レスポンスパターンテストデータ. 開始値がエンティティサイズより大きい場合のテスト.
         * @return テストデータ
         */
        @DataPoints
        public static Fixture[] getFixture() {
            Fixture[] datas = {
            // ※前提 first-byte-pos:a last-byte-pos:b suffix-length:c entitybody:Z
            // b < Z < a
            new Fixture(1, "bytes=200-300", 100) };
            return datas;
        }

        /**
         * 異常系ー416レスポンスパターン.
         * @param f テストデータ
         * @throws Exception Exception
         */
        @Theory
        public void 異常系ー416レスポンスパターン(Fixture f) throws Exception {
            // Rangeヘッダパース
            RangeHeaderHandler range = RangeHeaderHandler.parse(f.rangeHeaderField, f.contentSize);

            // byte-range-setの数チェック
            assertEquals(0, range.getByteRangeSpecCount());

            // Rangeヘッダが有効であることのチェック
            assertEquals(true, range.isValid());

            // 範囲外の指定になることをチェック
            assertEquals(false, range.isSatisfiable());
        }
    }

    /**
     * マルチバイトレンジは以下の4つの組み合わせのテストを実施.
     * ×が含まれる場合は、ヘッダ無視
     * △は○がひとつも含まれない場合は416扱い
     * 416 Z < a           △
     * 206 a< Z < b  ○
     * 206 a < b < Z ○
     * 200 b < a     ×（ヘッダー無視）
     */
    public static class MultiByteRangeTest {
        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * n = 3 でテスト 正常系マルチバイトレンジ1 "Range:bytes=○,△,△".
         * 無効なバイトレンジスペック(×)がひとつもない場合
         * @throws Exception Exception
         */
        @Test
        public void 正常系マルチバイトレンジ1() throws Exception {
            long fileSize = 27;

            // 10-150なので無効じゃない ○
            int firstBytePos1 = 10;
            int lastBytePos1 = 150;

            // 30-5000なので無効じゃない.416扱い △
            int firstBytePos2 = 30;
            int lastBytePos2 = 5000;

            // 100-200なので無効じゃない.416扱い △
            int firstBytePos3 = 100;
            int lastBytePos3 = 200;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            long exInstanceLength = fileSize;
            int exFirstBytePos1 = firstBytePos1;
            int exLastBytePos1 = 26;
            int exByteRangeSpecCount = 1;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertNormal(exInstanceLength, exFirstBytePos1, exLastBytePos1, exByteRangeSpecCount, 0, range);
        }

        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト 正常系マルチバイトレンジ2."Range:bytes=○,△,△".
         * 無効なバイトレンジスペック(×)がひとつもない場合
         * @throws Exception Exception
         */
        @Test
        public void 正常系マルチバイトレンジ2() throws Exception {
            long fileSize = 27;

            // 10-20なので無効じゃない ○
            int firstBytePos1 = 10;
            int lastBytePos1 = 20;

            // 30-5000なので無効じゃない.416扱い △
            int firstBytePos2 = 30;
            int lastBytePos2 = 5000;

            // 100-200なので無効じゃない.416扱い △
            int firstBytePos3 = 100;
            int lastBytePos3 = 200;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            long exInstanceLength = fileSize;
            int exFirstBytePos1 = firstBytePos1;
            int exLastBytePos1 = lastBytePos1;
            int exByteRangeSpecCount = 1;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertNormal(exInstanceLength, exFirstBytePos1, exLastBytePos1, exByteRangeSpecCount, 0, range);
        }

        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト 正常系マルチバイトレンジ3."Range:bytes=○,○,△".
         * 有効なバイトレンジスペックの範囲重なりなし
         * 無効なバイトレンジスペック(×)がひとつもない場合
         * @throws Exception Exception
         */
        @Test
        public void 正常系マルチバイトレンジ3() throws Exception {
            long fileSize = 27;

            // 10-20なので無効じゃない ○
            int firstBytePos1 = 10;
            int lastBytePos1 = 20;

            // 22-24なので無効じゃない ○
            int firstBytePos2 = 22;
            int lastBytePos2 = 24;

            // 200-300なので無効じゃない.416扱い △
            int firstBytePos3 = 200;
            int lastBytePos3 = 300;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            long exInstanceLength = fileSize;
            int exFirstBytePos1 = firstBytePos1;
            int exLastBytePos1 = lastBytePos1;
            int exFirstBytePos2 = firstBytePos2;
            int exLastBytePos2 = lastBytePos2;
            int exByteRangeSpecCount = 2;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertNormal(exInstanceLength, exFirstBytePos1, exLastBytePos1, exByteRangeSpecCount, 0, range);
            assertNormal(exInstanceLength, exFirstBytePos2, exLastBytePos2, exByteRangeSpecCount, 1, range);
        }

        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト 正常系マルチバイトレンジ4."Range:bytes=○,○,△".
         * 有効なバイトレンジスペックの範囲重なりあり
         * 無効なバイトレンジスペック(×)がひとつもない場合
         * @throws Exception Exception
         */
        @Test
        public void 正常系マルチバイトレンジ4() throws Exception {
            long fileSize = 27;

            // 10-20なので無効じゃない ○
            int firstBytePos1 = 10;
            int lastBytePos1 = 20;

            // 22-24なので無効じゃない ○
            int firstBytePos2 = 15;
            int lastBytePos2 = 24;

            // 200-300なので無効じゃない.416扱い △
            int firstBytePos3 = 200;
            int lastBytePos3 = 300;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            long exInstanceLength = fileSize;
            int exFirstBytePos1 = firstBytePos1;
            int exLastBytePos1 = lastBytePos1;
            int exFirstBytePos2 = firstBytePos2;
            int exLastBytePos2 = lastBytePos2;
            int exByteRangeSpecCount = 2;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertNormal(exInstanceLength, exFirstBytePos1, exLastBytePos1, exByteRangeSpecCount, 0, range);
            assertNormal(exInstanceLength, exFirstBytePos2, exLastBytePos2, exByteRangeSpecCount, 1, range);

        }
        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト 正常系マルチバイトレンジ5."Range:bytes=○,○,○,○,○,○,○,○,○,○,○,○,○".
         * byte-range-specの個数上限でのテスト  ※上限 13個
         * 無効なバイトレンジスペック(×)がひとつもない場合
         * @throws Exception Exception
         */
        @Test
        public void 正常系マルチバイトレンジ5() throws Exception {
            long fileSize = 27;
            String rangeHeader = "bytes=1-1,2-2,3-3,4-4,5-5,6-6,7-7,8-8,9-9,10-10,11-11,12-12,13-13";

            int exByteRangeSpecCount = 13;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            // 13個あること確認
            assertEquals(exByteRangeSpecCount, range.getByteRangeSpecCount());
            // ヘッダが有効であることの確認
            assertEquals(true, range.isValid());
            // 範囲外指定がないことの確認
            assertEquals(true, range.isSatisfiable());

            // 各range-byte-specの値チェック
            int exSize = 1;
            for (ByteRangeSpec brs : range.getByteRangeSpecList()) {
                assertEquals(exSize, brs.getFirstBytePos());
                assertEquals(exSize, brs.getLastBytePos());
                // Content-Lengthは全部１ 1-1,2-2,・・・なので
                assertEquals(1, brs.getContentLength());
                assertEquals(String.format("bytes %s-%s/%s", exSize, exSize, fileSize),
                        brs.makeContentRangeHeaderField());
                exSize++;
            }
        }

        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト 無視系マルチバイトレンジ1."Range:bytes=○,△,×".
         * バイトレンジヘッダが無効の場合のテスト
         * @throws Exception Exception
         */
        @Test
        public void 無視系マルチバイトレンジ1() throws Exception {
            long fileSize = 27;

            // 10-150なので無効じゃない ○
            int firstBytePos1 = 10;
            int lastBytePos1 = 150;

            // 300-5000なので無効じゃない .416扱い△
            int firstBytePos2 = 300;
            int lastBytePos2 = 5000;

            // 200-100なので無効.×
            int firstBytePos3 = 200;
            int lastBytePos3 = 100;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            boolean exsetvalid = false;
            int exByteRangeSpecCount = 0;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertIgnore(exsetvalid, exByteRangeSpecCount, range);
        }

        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト 無視系マルチバイトレンジ2."Range:bytes=○,△,×".
         * バイトレンジヘッダが無効の場合のテスト
         * @throws Exception Exception
         */
        @Test
        public void 無視系マルチバイトレンジ2() throws Exception {
            long fileSize = 27;

            // 10-20なので無効じゃない ○
            int firstBytePos1 = 10;
            int lastBytePos1 = 150;

            // 300-5000なので無効じゃない .416扱い△
            int firstBytePos2 = 300;
            int lastBytePos2 = 5000;

            // 200-100なので無効.×
            int firstBytePos3 = 200;
            int lastBytePos3 = 100;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            boolean exsetvalid = false;
            int exByteRangeSpecCount = 0;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertIgnore(exsetvalid, exByteRangeSpecCount, range);
        }

        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト 無視系マルチバイトレンジ3."Range:bytes=△,△,×".
         * バイトレンジヘッダが無効の場合のテスト
         * @throws Exception Exception
         */
        @Test
        public void 無視系マルチバイトレンジ3() throws Exception {
            long fileSize = 27;

            // 300-350なので無効じゃない.416扱い △
            int firstBytePos1 = 300;
            int lastBytePos1 = 350;

            // 400-700なので無効じゃない .416扱い△
            int firstBytePos2 = 300;
            int lastBytePos2 = 5000;

            // 200-100なので無効.×
            int firstBytePos3 = 200;
            int lastBytePos3 = 100;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            boolean exsetvalid = false;
            int exByteRangeSpecCount = 0;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertIgnore(exsetvalid, exByteRangeSpecCount, range);
        }

        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 3 でテスト. 416系マルチバイトレンジ1."Range:bytes=△,△,△".
         * バイトレンジスペックがすべて範囲外の場合のテスト
         * @throws Exception Exception
         */
        @Test
        public void アブノーマルマルチバイトレンジ1() throws Exception {
            long fileSize = 27;

            // 300-350なので無効じゃない.416扱い △
            int firstBytePos1 = 50;
            int lastBytePos1 = 100;

            // 400-700なので無効じゃない .416扱い△
            int firstBytePos2 = 30;
            int lastBytePos2 = 5000;

            // 100-200なので無効じゃない .416扱い△
            int firstBytePos3 = 100;
            int lastBytePos3 = 200;

            String rangeHeader = String.format("bytes=%s-%s,%s-%s,%s-%s",
                    firstBytePos1, lastBytePos1, firstBytePos2, lastBytePos2, firstBytePos3, lastBytePos3);

            int exByteRangeSpecCount = 0;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            assertAbNormal(exByteRangeSpecCount, range);
        }
        /**
         * ※前提 ：n番目のfirst-byte-pos:an last-byte-pos:bn suffix-length:cn entitybody:Z
         * ※n = 14 でテスト アブノーマルマルチレンジ2."Range:bytes=○,○,○,○,○,○,○,○,○,○,○,○,○,○".
         * byte-range-specの個数上限でのテスト  ※上限 13個
         * 無効なバイトレンジスペック(×)がひとつもない場合
         * @throws Exception Exception
         */
        @Test
        public void アブノーマルマルチレンジ2() throws Exception {
            long fileSize = 27;
            String rangeHeader = "bytes=1-1,2-2,3-3,4-4,5-5,6-6,7-7,8-8,9-9,10-10,11-11,12-12,13-13,14-14";

            int exByteRangeSpecCount = 0;

            RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeader, fileSize);

            // 0個あること確認
            assertEquals(exByteRangeSpecCount, range.getByteRangeSpecCount());
            // ヘッダが有効であることの確認
            assertEquals(false, range.isValid());
        }

        /**
         * 正常系テストの共通チェック処理.
         * @param exInstanceLength instance-length期待値
         * @param exFirstBytePos first-byte-posno期待値
         * @param exLastBytePos last-byte-pos期待値
         * @param exByteRangeSpecCount byte-range-specの数の期待値
         * @param byteRangeSpecIndex チェックするbyte-range-specの配列での位置
         * @param range
         */
        private void assertNormal(
                long exInstanceLength,
                int exFirstBytePos,
                int exLastBytePos,
                int exByteRangeSpecCount,
                int byteRangeSpecIndex,
                RangeHeaderHandler range) {


            // byte-range-setの数チェック
            assertEquals(exByteRangeSpecCount, range.getByteRangeSpecCount());

            List<ByteRangeSpec> brss = range.getByteRangeSpecList();
            ByteRangeSpec brs = brss.get(byteRangeSpecIndex);

            // 開始・終了値チェック
            assertEquals(exFirstBytePos, brs.getFirstBytePos());
            assertEquals(exLastBytePos, brs.getLastBytePos());

            // Rangeヘッダが有効であることのチェック
            assertEquals(true, range.isValid());

            // 範囲外の指定でないことをチェック
            assertEquals(true, range.isSatisfiable());

            // Content-Rangeの生成
            String contentRangeHeader = String.format("bytes %s-%s/%s",
                    exFirstBytePos, exLastBytePos, exInstanceLength);
            assertEquals(contentRangeHeader, brs.makeContentRangeHeaderField());
        }

        /**
         * 無視系テストの共通チェック処理.
         * @param exrangevalid   Rangeヘッダが有効であるかの期待値
         * @param range
         */
        private void assertIgnore(
                boolean exrangevalid,
                int exByteRangeSpecCount,
                RangeHeaderHandler range) {

            // byte-range-setの数チェック
            assertEquals(exByteRangeSpecCount, range.getByteRangeSpecCount());

            // Rangeヘッダが有効であることのチェック
            assertEquals(exrangevalid, range.isValid());
        }
        /**
         * 416系テストの共通チェック処理.
         * @param exsatifiable   range-haeder-specがすべて範囲外である場合はfalse
         * @param exByteRangeSpecCount
         * @param range
         */
        private void assertAbNormal(
                int exByteRangeSpecCount,
                RangeHeaderHandler range) {

            // byte-range-setの数チェック
            assertEquals(exByteRangeSpecCount, range.getByteRangeSpecCount());

            // Rangeヘッダが有効であることのチェック
            assertEquals(true, range.isValid());

            // byte-range-specがすべて範囲外であることのチェック
            assertEquals(false, range.isSatisfiable());

        }

    }
}
