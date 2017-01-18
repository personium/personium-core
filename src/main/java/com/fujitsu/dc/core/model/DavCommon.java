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
package com.fujitsu.dc.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dav関連で共通的に使う定数群を定義.
 */
public class DavCommon {

    private DavCommon() {
    }

    /** リソース最小長. */
    private static final int MIN_RESOURCE_LENGTH = 1;
    /** リソース最大長. */
    private static final int MAX_RESOURCE_LENGTH = 256;

    /** Depthヘッダのデフォルト値. */
    public static final String DEPTH_INFINITY = "infinity";

    /** Overwriteヘッダの上書きを許可する場合の値. */
    public static final String OVERWRITE_TRUE = "T";
    /** Overwriteヘッダの上書きを許可しない場合の値. */
    public static final String OVERWRITE_FALSE = "F";

    /**
     * 不正な名前のチェック.
     * @param name チェック対象のリソース名
     * @return true:正常、false:不正
     */
    public static final boolean isValidResourceName(String name) {
        // TODO Common.PATTERN_NAMEの正規表現ではないが正しいチェック方法なのか？API仕様書の内容とは合っていない
        if (name.length() >= MIN_RESOURCE_LENGTH
                && name.length() < MAX_RESOURCE_LENGTH) {
            String regex = "[\\\\/:*?\"<>| ]";
            Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(name);
            if (m.find()) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

}
