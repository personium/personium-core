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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AuthHistoryLastFile;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.lock.AccountLockManager;
import io.personium.core.model.lock.AccountValidAuthnIntervalLockManager;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockManager;

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
     * get auth history last params.
     * @param fsPath fs path
     * @param accountId account ID
     * @return file storing the last authentication history
     */
   public static AuthHistoryLastFile getAuthHistoryLast(String fsPath, String accountId) {
       AuthHistoryLastFile last = AuthHistoryLastFile.newInstance(fsPath, accountId);
       if (last.exists()) {
           last.load();
       } else {
           last.setDefault();
       }
       return last;
   }

    /**
     * update auth history last file with authentication success.
     * @param fsPath fs path
     * @param accountId account ID
     * @param time current time (unixtime)
     */
    public static void updateAuthHistoryLastFileWithSuccess(String fsPath, String accountId, long time) {
        Lock lock = LockManager.getLock(Lock.CATEGORY_AUTH_HISTORY, null, null, accountId);
        log.debug("lock auth history. accountId:" + accountId);
        try {
            AuthHistoryLastFile last = AuthHistoryLastFile.newInstance(fsPath, accountId);
            last.setLastAuthenticated(time);
            last.setFailedCount(0);
            last.save();
        } finally {
            log.debug("unlock auth history. accountId:" + accountId);
            lock.release();
        }
    }

    /**
     * update auth history last file with authentication failed.
     * @param fsPath fs path
     * @param accountId account ID
     */
    public static void updateAuthHistoryLastFileWithFailed(String fsPath, String accountId) {
        Lock lock = LockManager.getLock(Lock.CATEGORY_AUTH_HISTORY, null, null, accountId);
        log.debug("lock auth history. accountId:" + accountId);
        try {
            AuthHistoryLastFile last = AuthHistoryLastFile.newInstance(fsPath, accountId);
            if (last.exists()) {
                last.load();
            } else {
                last.setDefault();
            }
            last.setFailedCount(last.getFailedCount() + 1);
            last.save();
        } finally {
            log.debug("unlock auth history. accountId:" + accountId);
            lock.release();
        }
    }

    /**
     * Check if the target account records authentication history.
     * @param cellRsCmp cell rs cmp
     * @param accountId account ID
     * @param accountName account name
     * @return "true" is records authentication history
     */
    public static boolean isRecordingAuthHistory(CellRsCmp cellRsCmp, String accountId, String accountName) {
        if (StringUtils.isEmpty(accountId) || StringUtils.isEmpty(accountName)) {
            return false;
        }
        List<String> ineligibleAccountList = cellRsCmp.requestGetAccountsNotRecordingAuthHistory();
        if (ineligibleAccountList == null) {
            return true;
        }
        return !ineligibleAccountList.contains(accountName);
    }

    /**
     * Process to check if an Account valid authentication interval lock exists.
     * @param accountId account ID
     * @return boolean Returns true if lock exists
     */
    public static Boolean isLockedInterval(String accountId) {
        return AccountValidAuthnIntervalLockManager.hasLockObject(accountId);
    }

    /**
     * Register Account valid authentication interval lock.
     * @param accountId account ID
     */
    public static void registIntervalLock(String accountId) {
        AccountValidAuthnIntervalLockManager.registLockObject(accountId);
    }

    /**
     * Process to check if an Account lock exists.
     * @param accountId account ID
     * @return boolean Returns true if lock exists
     */
    public static Boolean isLockedAccount(String accountId) {
        return AccountLockManager.isLockedAccount(accountId);
    }

    /**
     * Countup failed count.
     * @param accountId account ID
     */
    public static void countupFailedCount(String accountId) {
        AccountLockManager.countupFailedCount(accountId);
    }

    /**
     * release Account lock. Reset the failed count.
     * @param accountId account ID
     */
    public static void releaseAccountLock(String accountId) {
        AccountLockManager.releaseAccountLock(accountId);
    }
}
