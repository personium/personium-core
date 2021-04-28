/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
 * - FUJITSU LIMITED
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
 * A utility class for creating unique names between threads / processes / servers.
 */
public class UniqueNameComposer {

    private static final String FORMAT = "%s_%s_%s";

    private UniqueNameComposer() {
    }

    /**
     * Create a unique name through thread / process / server.
     * @param prefix Any string
     * @return unique name
     */
    public static String compose(String prefix) {
        String macAddress = getMacAddress();
        UUID uuid = UUID.randomUUID();
        return String.format(FORMAT, prefix, macAddress, uuid.toString());

    }

    /**
     * Returns a character string based on the Mac address of the first network interface of the running server.
     * If the Mac address can not be acquired, it returns a character string obtained by converting millisecond of the current date into hexadecimal as an alternative.
     * @return A string representation of the Mac address. However, delimiters are not included between bytes.
     */
    public static String getMacAddress() {
        //Get the MAC address of the first NIC.
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
            //Since nic can not be detected, it returns an alternate character string.
            return Long.toHexString(System.currentTimeMillis());
        } catch (SocketException e) {
            //Since it can not be acquired by error, it returns an alternate character string.
            return Long.toHexString(System.currentTimeMillis());
        }
    }

}
