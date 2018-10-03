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
package io.personium.core.rs.cell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.lock.AccountLockManager;

/**
 * A utility class that defines methods that are generic to use in resource classes.
 */
public class AuthResourceUtils {
    static Logger log = LoggerFactory.getLogger(AuthResourceUtils.class);

    /**
     * constructor.
     */
    protected AuthResourceUtils() {
    }

    /**
     * JS source loading utility.
     * @param fileName JS source file name
     * @return String JS source
     */
    public static String getJavascript(String fileName) {

        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        String rtnstr = null;
        try {
            in = PersoniumUnitConfig.class.getClassLoader().getResourceAsStream(fileName);
            /* Character code specification */
            isr = new InputStreamReader(in, "UTF-8");
            reader = new BufferedReader(isr);
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = reader.readLine()) != null) {
                buf.append(str);
                buf.append("\n");
            }
            rtnstr = buf.toString();
        } catch (UnsupportedEncodingException e) {
            //TODO Automatically generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            //TODO Automatically generated catch block
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return rtnstr;
    }

    /**
     * Check whether the target of the transcell token and cell are equal.
     * @param cell target cell
     * @param tcToken Transcel token
     * @return boolean Returns true if the target of the token is cell
     * @throws MalformedURLException MalformedURLException
     */
    protected static boolean checkTargetUrl(Cell cell, TransCellAccessToken tcToken) throws MalformedURLException {
        URL targetUrl = new URL(tcToken.getTarget());
        URL myUrl = new URL(cell.getUrl());
        String pT = targetUrl.getPath();
        String pM = myUrl.getPath();

        if (!pT.endsWith("/")) {
            pT = pT + "/";
        }
        if (!pM.endsWith("/")) {
            pM = pM + "/";
        }
        if (!targetUrl.getAuthority().equals(myUrl.getAuthority()) || !pM.equals(pT)) {
            return false;
        }

        return true;

    }

    /**
     * Process to check if an Account lock exists.
     * @param accountId account ID
     * Returns true if @return lock exists
     */
    public static Boolean isLockedAccount(String accountId) {
        return AccountLockManager.hasLockObject(accountId);
    }

    /**
     * Register Account lock.
     * @param accountId account ID
     */
    public static void registAccountLock(String accountId) {
        AccountLockManager.registAccountLockObjct(accountId);
    }
}
