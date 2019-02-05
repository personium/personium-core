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
package io.personium.core.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.net.util.SubnetUtils;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.plugin.PluginInfo;
import io.personium.core.plugin.PluginManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.plugin.base.auth.AuthConst;
import io.personium.plugin.base.auth.AuthPlugin;

/**
 * Authentication related utilities.
 */
public final class AuthUtils {
    private static final String MD_ALGORITHM = "SHA-256";
    /** Password minimum length.*/
    private static final int MIN_PASSWORD_LENGTH = 6;
    /** Password maximum length.*/
    private static final int MAX_PASSWORD_LENGTH = 32;

    private AuthUtils() {
    }

    /**
     * Hash string of password string.
     * @param passwd raw password string
     * @return hashed password string
     */
    public static String hashPassword(final String passwd) {
        if (passwd == null) {
            return null;
        }

        // DC0 Ruby Code
        // Digest::SHA256.hexdigest(pw + "Password hash salt value")
        String str2hash = passwd + PersoniumUnitConfig.getAuthPasswordSalt();
        try {
            MessageDigest md = MessageDigest.getInstance(MD_ALGORITHM);
            byte[] digestBytes = md.digest(str2hash.getBytes(CharEncoding.UTF_8));
            //Although its data efficiency is better, this implementation is made for compatibility with DC 0.
            return PersoniumCoreUtils.byteArray2HexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform hashing of the password.
     * Validate the password before hashing.
     * @param pCredHeader pCredHeader
     * @param entitySetName entitySetName
     * @return hashing password
     */
    public static String hashPassword(String pCredHeader, String entitySetName) {
        if (!Account.EDM_TYPE_NAME.equals(entitySetName)) {
            return null;
        }
        validatePassword(pCredHeader);
        String hPassStr = AuthUtils.hashPassword(pCredHeader);
        return hPassStr;
    }

    /**
     * Validate account type.
     * @param oEntityWrapper EntityWrapper
     * @param entitySetName entitySetName
     */
    public static void validateAccountType(OEntityWrapper oEntityWrapper, String entitySetName) {
        if (!Account.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }

        // List of allowed types.
        List<String> allowedTypeList = new ArrayList<String>();
        allowedTypeList.add(Account.TYPE_VALUE_BASIC);
        // Does it match the AccountType added in AuthPlugin.
        PluginManager pluginManager = PersoniumCoreApplication.getPluginManager();
        List<PluginInfo> pluginInfoList = pluginManager.getPluginsByType(AuthConst.PLUGIN_TYPE);
        for (PluginInfo pluginInfo : pluginInfoList) {
            AuthPlugin plugin = (AuthPlugin) pluginInfo.getObj();
            allowedTypeList.add(plugin.getAccountType());
        }

        // Check if all specified types match the allowed types.
        String type = (String) oEntityWrapper.getProperty(Account.P_TYPE.getName()).getValue();
        String[] accountTypes = type.split(" ");
        if (accountTypes == null || accountTypes.length <= 0) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Account.P_TYPE.getName());
        }
        for (String accountType : accountTypes) {
            if (!allowedTypeList.contains(accountType)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Account.P_TYPE.getName());
            }
            // Remove to avoid duplication.
            allowedTypeList.remove(accountType);
        }
    }

    /**
     * getAccountType.
     * @param oew oew
     * @return List<String>
     */
    public static List<String> getAccountType(OEntityWrapper oew) {
        String typeStr =  (String) oew.getProperty(Account.P_TYPE.getName()).getValue();
        String[] typeAry = typeStr.split(" ");
        return Arrays.asList(typeAry);
    }

    /**
     * isAccountTypeBasic.
     * @param oew oew
     * @return List<String>
    */
    public static boolean isAccountTypeBasic(OEntityWrapper oew) {
        return getAccountType(oew).contains(Account.TYPE_VALUE_BASIC);
    }

    /**
     * Validate password.
     * @param pCredHeader pCredHeader
     */
    private static void validatePassword(String pCredHeader) {
        if (pCredHeader == null) {
            return;
        }
        if (pCredHeader.length() >= MIN_PASSWORD_LENGTH
                && pCredHeader.length() <= MAX_PASSWORD_LENGTH) {
            String regex = "^[a-zA-Z0-9-_]{0,}$";
            Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(pCredHeader);
            if (!m.find()) {
                throw PersoniumCoreException.Auth.PASSWORD_INVALID;
            }
        } else {
            throw PersoniumCoreException.Auth.PASSWORD_INVALID;
        }
    }

    /**
     * Validate account ip address range.
     * @param oEntityWrapper EntityWrapper
     * @param entitySetName entitySetName
     */
    public static void validateAccountIPAddressRange(OEntityWrapper oEntityWrapper, String entitySetName) {
        if (!Account.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }

        // Check IP address range.
        List<String> ipAddressRangeList = getIPAddressRangeList(oEntityWrapper);
        if (ipAddressRangeList == null || ipAddressRangeList.isEmpty()) {
            return;
        }
        Pattern pattern = Pattern.compile(Common.PATTERN_SINGLE_IP_ADDRESS_RANGE);
        for (String address : ipAddressRangeList) {
            Matcher matcher = pattern.matcher(address);
            if (!matcher.matches()) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                        .params(Account.P_IP_ADDRESS_RANGE.getName());
            }
        }
    }

    /**
     * Check is Valid IP address.
     * @param oew oew
     * @param requestIPAddress ip address of request
     * @return boolean Returns false if authentication failure.
     */
    public static Boolean isValidIPAddress(OEntityWrapper oew, String requestIPAddress) {
        // When IPAddrsesRange of Account is not set, all IP addresses are permitted.
        List<String> ipAddressRangeList = getIPAddressRangeList(oew);
        if (ipAddressRangeList == null) {
            return true;
        }

        // If the IP address of the client is unknown, it is an error.
        if (requestIPAddress == null || requestIPAddress.isEmpty()) {
            return false;
        }
        // If the IP address of the client is an illegal format, it is an error
        String clientIPAddress = requestIPAddress.split(",")[0].trim();
        Pattern pattern = Pattern.compile(Common.PATTERN_SINGLE_IP_ADDRESS);
        Matcher matcher = pattern.matcher(clientIPAddress);
        if (!matcher.matches()) {
            return false;
        }

        // Check if the IP address of the client is included in "IPAddressRange".
        for (String ipAddressRange : ipAddressRangeList) {
            if (ipAddressRange.contains("/")) {
                SubnetUtils subnet = new SubnetUtils(ipAddressRange);
                SubnetUtils.SubnetInfo subnetInfo = subnet.getInfo();
                int address = subnetInfo.asInteger(clientIPAddress);
                int low = subnetInfo.asInteger(subnetInfo.getLowAddress());
                int high = subnetInfo.asInteger(subnetInfo.getHighAddress());
                if (low <= address && address <= high) {
                    return true;
                }
            } else {
                if (ipAddressRange.equals(clientIPAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * get ip address renge list .
     * @param oew oew
     * @return List<String>
     */
    private static List<String> getIPAddressRangeList(OEntityWrapper oew) {
        String addrStr =  (String) oew.getProperty(Account.P_IP_ADDRESS_RANGE.getName()).getValue();
        if (addrStr == null || addrStr.isEmpty()) {
            return null;
        }
        String[] addrAry = addrStr.split(",");
        return Arrays.asList(addrAry);
    }
}
