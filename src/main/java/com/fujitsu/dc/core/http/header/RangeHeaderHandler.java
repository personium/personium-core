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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rangeリクエストヘッダを処理する. RangeHeaderHandler.parse()でRangeヘッダフィールドの値をパースして利用する。
 * 返却値はisValid()で有効なヘッダ指定かチェックして利用する。無効で合った場合ヘッダ指定を無視すべき。
 * また有効であった場合もでisSatisfiable()でRangeの開始指定が正常なbyte-range-specが存在しない場合は416レスポンスを返却すべき.
 * 処理すべきbyte-range-specが存在した場合、getByteRangeSpecCount()で有効なbyte-range-specの数を取得し、
 * getFirstBytePos()、getLastBytePos()、getContentLength()、makeContentRangeHeaderField()でレスポンス生成に必要な情報を得る。
 */
public class RangeHeaderHandler {

    // 用語
    /**
     * bytes-unit.
     */
    public static final String BYTES_UNIT = "bytes";

    // RFCに定義は無いが、apacheが上限13個までの実装になっていたので合わせた
    static final int BYTE_RANGE_SPEC_MAX = 13;
    // Rangeヘッダフィールドの文字列
    private String rangeHeaderField = "";
    // range-byte-specを管理
    private List<ByteRangeSpec> byteRangeSpecList = new ArrayList<ByteRangeSpec>();;

    // Rangeヘッダの有効無効を管理
    private boolean valid = false;

    /**
     * コンストラクタ.
     */
    private RangeHeaderHandler(String rangeHeader) {
        this.rangeHeaderField = rangeHeader;
    }

    /**
     * Rangeヘッダの値と対象ファイルのサイズを渡しパースして、本クラスのオブジェクトを生成.
     * @param rangeHeader Rangeヘッダの値（ex. bytes=500-600,601-999）
     * @param entitySize Range指定対象のファイルサイズ
     * @return 本クラスのオブジェクト
     */
    public static final RangeHeaderHandler parse(final String rangeHeader, final long entitySize) {
        RangeHeaderHandler range = new RangeHeaderHandler(rangeHeader);

        // Range ヘッダ指定なければ無視
        if (rangeHeader == null) {
            return range;
        }

        // byte-range-set 部分の抽出
        String regexByteRangesSpecifier = "^bytes\\s*=\\s*(.+)$";
        Pattern pByteRangesSpecifier = Pattern.compile(regexByteRangesSpecifier);
        Matcher mByteRangesSpecifier = pByteRangesSpecifier.matcher(rangeHeader);
        if (!mByteRangesSpecifier.matches()) {
            return range;
        }

        // byte-range-spec が複数ある可能性があるのでパース
        String[] byteRangeSpecArray = mByteRangesSpecifier.group(1).split(",");

        // byte-range-spec が上限を超えている場合はRangeヘッダ無効
        if (byteRangeSpecArray.length > BYTE_RANGE_SPEC_MAX) {
            return range;
        }

        // 各byte-range-specのパース処理
        List<ByteRangeSpec> byteRangeSpecList = new ArrayList<ByteRangeSpec>();
        for (String byteRangeSpec : byteRangeSpecArray) {
            ByteRangeSpec brs = ByteRangeSpec.parse(byteRangeSpec, entitySize);
            if (brs == null) {
                return range;
            }
            if (!brs.isInEntitySize()) {

                continue;
            }
            byteRangeSpecList.add(brs);
        }
        range.setValid();
        range.setByteRangeSpec(byteRangeSpecList);
        return range;
    }

    /**
     * Rangeヘッダフィールドの文字列を返す.
     * @return Rangeヘッダフィールド
     */
    public String getRangeHeaderField() {
        return this.rangeHeaderField;
    }

    /**
     * Rangeヘッダが有効かどうかを返す. 有効なRange指定がなかった場合falseを返す。
     * @return true 有効
     */
    public boolean isValid() {
        return this.valid;
    }
    private void setValid() {
        this.valid = true;
    }

    /**
     * Rangeヘッダで指定されているbyte-range-specの数を返す.
     * @return Rangeの数
     */
    public int getByteRangeSpecCount() {
        return this.byteRangeSpecList.size();
    }

    /**
     * ファイルの範囲内におさまっているbyte-range-specが存在するかチェック. RFC曰くファイルのbyte-range-specが一つでもファイル内に収まっているものがあれば206で返す.
     * @return bool
     */
    public boolean isSatisfiable() {
        if (this.byteRangeSpecList.size() > 0) {
            return true;
        }
        return false;
    }

    private void setByteRangeSpec(final List<ByteRangeSpec> brs) {
        this.byteRangeSpecList = brs;
    }

    /**
     * 有効なByteRangeSpecのリストを返却.
     * @return 有効なByteRangeSpecのリスト
     */
    public List<ByteRangeSpec> getByteRangeSpecList() {
        return this.byteRangeSpecList;
    }
}
