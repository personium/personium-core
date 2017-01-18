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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ByteRangeSpecを管理するための入れ物.
 */
public class ByteRangeSpec {
    private long entitySize;
    private long firstBytePos;
    private long lastBytePos;

    private ByteRangeSpec(final long firstBytePos, final long lastBytePos, final long entitySize) {
        this.entitySize = entitySize;
        this.firstBytePos = firstBytePos;
        this.lastBytePos = lastBytePos;
    }

    /**
     * byte-range-specをパースして構文上正しければ本オブジェクトを返す. 不正な場合はnullを返す.
     * @param byteRangeSpecString byte-range-specの文字列
     * @param entitySize Range対象のファイルサイズ
     * @return 本オブジェクト
     */
    static final ByteRangeSpec parse(final String byteRangeSpecString, final long entitySize) {
        String firstBytePosString;
        String lastBytePosString;
        long firstBytePosLong;
        long lastBytePosLong;

        // 開始、終端の取得
        String regexByteRangeSpec = "^([^-]*)-([^-]*)$";
        Pattern pByteRangeSpec = Pattern.compile(regexByteRangeSpec);
        Matcher mByteRangeSpec = pByteRangeSpec.matcher(byteRangeSpecString);
        if (!mByteRangeSpec.matches()) {
            return null;
        }
        firstBytePosString = mByteRangeSpec.group(1).trim();
        lastBytePosString = mByteRangeSpec.group(2).trim();

        // 開始、終端両方省略されてる場合は無効
        if (firstBytePosString.equals("") && lastBytePosString.equals("")) {
            return null;
        }
        if (lastBytePosString.equals("")) {
            // 終端が省略されている場合はファイルサイズまでを終端とする
            lastBytePosLong = entitySize - 1;
        } else {
            try {
                lastBytePosLong = Long.parseLong(lastBytePosString);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // 開始省略されてて終端の指定があったら（最後-終端）から最後まで
        if (firstBytePosString.equals("")) {
            firstBytePosLong = entitySize - lastBytePosLong;
            if (firstBytePosLong < 0) {
                firstBytePosLong = 0;
            }
            lastBytePosLong = entitySize - 1;
        } else {
            // 数値かどうかチェック
            try {
                firstBytePosLong = Long.parseLong(firstBytePosString);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Rangeの開始と終端の位置が反転してたらRangeヘッダ無効
        if (firstBytePosLong > lastBytePosLong) {
            return null;
        }

        // 終端がエンティティサイズより大きい場合は、終端の値はエンティティサイズ-1の値
        if (lastBytePosLong >= entitySize) {
            lastBytePosLong = entitySize - 1;
        }

        return new ByteRangeSpec(firstBytePosLong, lastBytePosLong, entitySize);
    }

    /**
     * Rangeの開始位置がファイルの範囲内かチェックして範囲内の場合はtrue返す.
     * @return bool
     */
    public boolean isInEntitySize() {
        if (this.getFirstBytePos() > (this.entitySize - 1)) {
            return false;
        }
        return true;
    }

    /**
     * first-byte-pos.
     * @return first-byte-pos
     */
    public long getFirstBytePos() {
        return this.firstBytePos;
    }

    /**
     * last-byte-pos.
     * @return last-byte-pos
     */
    public long getLastBytePos() {
        return this.lastBytePos;
    }

    /**
     * Rangeの指定を考慮したContentLengthの返却.
     * @return long contentLength
     */
    public long getContentLength() {
        return this.getLastBytePos() + 1 - this.getFirstBytePos();
    }

    /**
     * Rangeの値をContent-Rangeヘッダの値に整形.
     * @return Content-Rangeヘッダの値
     */
    public String makeContentRangeHeaderField() {
        // Content-Rangeヘッダのフォーマットで返す
        return String.format("bytes %s-%s/%s", this.getFirstBytePos(), this.getLastBytePos(), this.entitySize);
    }
}
