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
package io.personium.core.model.file;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * スレッド/プロセス/サーバ間を通じてユニークな名前を作成するためのユーティリティクラス.
 */
public class UniqueNameComposer {

    private static final String FORMAT = "%s_%s_%s";

    private UniqueNameComposer() {
    }

    /**
     * スレッド/プロセス/サーバ間を通じてユニークな名前を作成する.
     * @param prefix 任意の文字列
     * @return ユニーク名
     */
    public static String compose(String prefix) {
        String macAddress = getMacAddress();
        UUID uuid = UUID.randomUUID();
        return String.format(FORMAT, prefix, macAddress, uuid.toString());

    }

    /**
     * 実行中サーバの最初のネットワークインタフェースの Macアドレスを基にした文字列を返す。
     * Macアドレスが取得できない場合、代替として現在日付の millisecondを 16進変換した文字列を返す。
     * @return Macアドレスの文字列表現。ただしバイト間にデリミタは含まない。
     */
    public static String getMacAddress() {
        // 最初の NIC の MACアドレスを取得する。
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface nic = networkInterfaces.nextElement();
                byte[] hardwareAddress = nic.getHardwareAddress();
                StringBuilder buffer = new StringBuilder();
                if (null != hardwareAddress) {
                    for (byte b : hardwareAddress) {
                        buffer.append(String.format("%02X", b));
                    }
                    return buffer.toString();
                }
            }
            // nicが検出できないため、代替文字列を返す。
            return Long.toHexString(System.currentTimeMillis());
        } catch (SocketException e) {
            // エラーで取得できないため、代替文字列を返す。
            return Long.toHexString(System.currentTimeMillis());
        }
    }

}
