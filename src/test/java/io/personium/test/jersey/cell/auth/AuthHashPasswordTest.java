/**
 * Personium
 * Copyright 2019-2020 Personium Project Authors
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
package io.personium.test.jersey.cell.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumUnitConfig.Security;
import io.personium.core.auth.hash.SCryptHashPasswordImpl;
import io.personium.core.auth.hash.Sha256HashPasswordImpl;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * valid ip address range test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class})
public class AuthHashPasswordTest extends PersoniumTest {

    /** test cell name. */
    private static final String TEST_CELL = "testcellipaddress";

    /** key name. */
    private static final String KEY_NAME = "name";
    /** key password. */
    private static final String KEY_PASS = "pass";

    private static String defHashArgorithm;
    private static String defSCryptCpuCost;
    private static String defSCryptMemoryCost;
    private static String defSCryptParallelization;
    private static String defSCryptKeyLength;
    private static String defSCryptSaltLength;
    private static String defPasswordRegex;

    /**
     * Befor class.
     */
    @BeforeClass
    public static void beforClass() {
        defHashArgorithm = PersoniumUnitConfig.getAuthPasswordHashAlgorithm();
        defSCryptCpuCost = PersoniumUnitConfig.get(Security.AUTH_PASSWORD_SCRYPT_CPUCOST);
        defSCryptMemoryCost = PersoniumUnitConfig.get(Security.AUTH_PASSWORD_SCRYPT_MEMORYCOST);
        defSCryptParallelization = PersoniumUnitConfig.get(Security.AUTH_PASSWORD_SCRYPT_PARALLELIZATION);
        defSCryptKeyLength = PersoniumUnitConfig.get(Security.AUTH_PASSWORD_SCRYPT_KEYLENGTH);
        defSCryptSaltLength = PersoniumUnitConfig.get(Security.AUTH_PASSWORD_SCRYPT_SALTLENGTH);
        defPasswordRegex = PersoniumUnitConfig.getAuthPasswordRegex();
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_REGEX, "^.*[A-Za-z0-9]$");
    }

    /**
     * After class.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_HASH_ALGORITHM, defHashArgorithm);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_CPUCOST, defSCryptCpuCost);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_MEMORYCOST, defSCryptMemoryCost);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_PARALLELIZATION, defSCryptParallelization);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_KEYLENGTH, defSCryptKeyLength);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_SALTLENGTH, defSCryptSaltLength);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_REGEX, defPasswordRegex);
    }

    /**
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /**
     * constructor.
     */
    public AuthHashPasswordTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test hash password.
     */
    @Test
    public final void testHashPassword() {
        List<Map<String, String>> testAccountList = new ArrayList<>();
        try {
            // create test accounts.
            // SHA-256
            testAccountList.add(createTestAccount(
                    Sha256HashPasswordImpl.HASH_ALGORITHM_NAME, "test1", "password"));
            testAccountList.add(createTestAccount(
                    Sha256HashPasswordImpl.HASH_ALGORITHM_NAME, "test2", UserDataUtils.createString(256)));
            // SCrypt (defalut params)
            testAccountList.add(createTestAccount(
                    SCryptHashPasswordImpl.HASH_ALGORITHM_NAME, "test3", "password"));
            testAccountList.add(createTestAccount(
                    SCryptHashPasswordImpl.HASH_ALGORITHM_NAME, "test4", UserDataUtils.createString(256)));
            // SCrypt (edit params)
            changeSCryptParams("65536", "32", "2", "256", "128");
            testAccountList.add(createTestAccount(
                    SCryptHashPasswordImpl.HASH_ALGORITHM_NAME, "test5", "password"));
            testAccountList.add(createTestAccount(
                    SCryptHashPasswordImpl.HASH_ALGORITHM_NAME, "test6", UserDataUtils.createString(256)));
            changeSCryptParams(defSCryptCpuCost, defSCryptMemoryCost, defSCryptParallelization, defSCryptKeyLength,
                    defSCryptSaltLength);

            // execute authentication
            for (Map<String, String> testAccount : testAccountList) {
                // success
                String name = testAccount.get(KEY_NAME);
                String pass = testAccount.get(KEY_PASS);
                requestAuthentication(TEST_CELL, name, pass, HttpStatus.SC_OK);

                // error (Password mismatch)
                AuthTestCommon.waitForIntervalLock();
                String errorPass = pass.substring(0, pass.length() - 1);
                requestAuthentication(TEST_CELL, name, errorPass, HttpStatus.SC_BAD_REQUEST);
                AuthTestCommon.waitForIntervalLock();
                errorPass = pass + "e";
                requestAuthentication(TEST_CELL, name, errorPass, HttpStatus.SC_BAD_REQUEST);
            }
        } finally {
            // delete test account.
            for (Map<String, String> testAccount : testAccountList) {
                String name = testAccount.get(KEY_NAME);
                AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, name, -1);
            }
        }
    }

    /**
     * create test account.
     * @param hashArgorithm hash argorithm
     * @param name account name
     * @param pass password
     * @return map (key is "name" and "pass")
     */
    private Map<String, String> createTestAccount(String hashArgorithm, String name, String pass) {
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_HASH_ALGORITHM, hashArgorithm);
        AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, name, pass, HttpStatus.SC_CREATED);

        Map<String, String> map = new HashMap<>();
        map.put(KEY_NAME, name);
        map.put(KEY_PASS, pass);
        return map;
    }

    /**
     * change SCrypt params.
     * @param cc SCrypt cpu cost
     * @param mc SCrypt memory cost
     * @param p SCrypt parallelization
     * @param kLen SCrypt key length
     * @param sLen SCrypt salt length
     */
    private void changeSCryptParams(String cc, String mc, String p, String kLen, String sLen) {
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_CPUCOST, cc);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_MEMORYCOST, mc);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_PARALLELIZATION, p);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_KEYLENGTH, kLen);
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_SCRYPT_SALTLENGTH, sLen);
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param userName user name
     * @param pass password
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthentication(String cellName, String userName, String pass, int code) {
        Http request = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", cellName)
                .with("username", userName)
                .with("password", pass);
        TResponse passRes = request.returns().statusCode(code);
        return passRes;
    }
}
