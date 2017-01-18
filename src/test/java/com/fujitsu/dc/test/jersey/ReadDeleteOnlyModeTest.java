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
package com.fujitsu.dc.test.jersey;

import static com.fujitsu.dc.test.utils.BatchUtils.END_BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.START_BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveChangeSetResErrorBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveDeleteBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveDeleteResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveGetBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveGetResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveLinksPostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveListBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveListResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePutBody;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.lock.LockManager;
import com.fujitsu.dc.core.utils.MemcachedClient;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * ReadDeleteOnlyModeに関するテスト.
 */
@RunWith(DcRunner.class)
@Category({Integration.class })
public class ReadDeleteOnlyModeTest extends JerseyTest {

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("javax.ws.rs.Application",
                "com.fujitsu.dc.core.rs.DcCoreApplication");
        INIT_PARAMS.put("com.sun.jersey.config.feature.DisableWADL",
                "true");
    }

    /**
     * コンストラクタ.
     */
    public ReadDeleteOnlyModeTest() {
        super(new WebAppDescriptor.Builder(ReadDeleteOnlyModeTest.INIT_PARAMS).build());
    }

    /**
     * ReadDeleteOnlyモード時にUserODataの登録が503となること.
     * @throws Exception .
     */
    // siteで実行するとテスト順序により、エラーとなるためIgnoreとする
    @Ignore
    @Test
    public void ReadDeleteOnlyモード時にPOSTメソッドが503となること() throws Exception {
        // InProcessの場合はテスト不可のため終了する
        if (LockManager.TYPE_IN_PROCESS.equals(DcCoreConfig.getLockType())) {
            return;
        }

        // ReadDeleteOnlyモードを設定する
        String lockValue = "{\"status\":{\"volumeStatus\":[{\"status\":\"OK\",\"volume\":\"_ads\","
                + "\"allocatedDiskSize\":13738862182,\"usedDiskSize\":3725590528,\"volumeDiskSize\":68694310912},"
                + "{\"status\":\"OK\",\"volume\":\"_log\",\"allocatedDiskSize\":2113090355,\"usedDiskSize\":1092616192,"
                + "\"volumeDiskSize\":10565451776},{\"status\":\"FULL\",\"volume\":\"elasticsearch1\","
                + "\"allocatedDiskSize\":13739701043,\"usedDiskSize\":18747490304,\"volumeDiskSize\":68698505216},"
                + "{\"status\":\"OK\",\"volume\":\"dav\",\"allocatedDiskSize\":23250842419,\"usedDiskSize\":6122848256,"
                + "\"volumeDiskSize\":116254212096}],\"systemStatus\":\"FULL\"}}";
        MemcachedClient.getLockClient().add("PcsReadDeleteMode", lockValue);

        // ReadDeleteOnlyモード時にUserODataの登録を行い、503が返却されることを確認する
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_SERVICE_UNAVAILABLE,
                "{\"__id\":\"test\"}", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales");

        // ReadDeleteOnlyモードを解除する
        MemcachedClient.getLockClient().delete("PcsReadDeleteMode");
    }

    /**
     * ReadDeleteOnlyモード時に$batchを実行した場合_登録系リクエストが503となること.
     * @throws Exception .
     */
    // siteで実行するとテスト順序により、エラーとなるためIgnoreとする
    @Ignore
    @Test
    public void ReadDeleteOnlyモード時に$batchを実行した場合_登録系リクエストが503となること() throws Exception {
        // InProcessの場合はテスト不可のため終了する
        if (LockManager.TYPE_IN_PROCESS.equals(DcCoreConfig.getLockType())) {
            return;
        }

        try {
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    "{\"__id\":\"RDOnlyMode\"}",
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales");

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    "{\"__id\":\"RDOnlyMode\"}",
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "SalesDetail");

            // ReadDeleteOnlyモードを設定する
            String lockValue = "{\"status\":{\"volumeStatus\":[{\"status\":\"OK\",\"volume\":\"_ads\","
                    + "\"allocatedDiskSize\":13738862182,\"usedDiskSize\":3725590528,"
                    + "\"volumeDiskSize\":68694310912},{\"status\":\"OK\",\"volume\":\"_log\","
                    + "\"allocatedDiskSize\":2113090355,\"usedDiskSize\":1092616192,"
                    + "\"volumeDiskSize\":10565451776},{\"status\":\"FULL\",\"volume\":\"elasticsearch1\","
                    + "\"allocatedDiskSize\":13739701043,\"usedDiskSize\":18747490304,\"volumeDiskSize\":68698505216},"
                    + "{\"status\":\"OK\",\"volume\":\"dav\",\"allocatedDiskSize\":23250842419,"
                    + "\"usedDiskSize\":6122848256,\"volumeDiskSize\":116254212096}],\"systemStatus\":\"FULL\"}}";
            MemcachedClient.getLockClient().add("PcsReadDeleteMode", lockValue);

            // ReadDeleteOnlyモード時に$batchリクエスト
            String boundary = "batch_XAmu9BiJJLBa20sRWIq74jp2UlNAVueztqu";
            String body = START_BOUNDARY + retrieveListBody("Sales")
                    + START_BOUNDARY + retrieveGetBody("Sales('RDOnlyMode')")
                    + START_BOUNDARY + retrievePostBody("Sales", "RDOnlyModePost")
                    + START_BOUNDARY + retrievePutBody("Sales('RDOnlyMode')")
                    + START_BOUNDARY + retrievePostBody("Sales('RDOnlyMode')/_SalesDetail", "RDOnlyModeNp")
                    + START_BOUNDARY + retrieveLinksPostBody(
                            HttpMethod.POST,
                            "Sales('RDOnlyMode')/\\$links/_SalesDetail",
                            "{\"uri\":\""
                                    + UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                                            "SalesDetail", "RDOnlyMode") + "\"}")
                    + START_BOUNDARY + retrieveDeleteBody("Sales('RDOnlyMode')")
                    + END_BOUNDARY;

            TResponse response = UserDataUtils.batch(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    boundary, body, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_ACCEPTED);

            String expectedBody = START_BOUNDARY + retrieveListResBody()
                    + START_BOUNDARY + retrieveGetResBody("Sales", "RDOnlyMode")
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_SERVICE_UNAVAILABLE)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_SERVICE_UNAVAILABLE)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_SERVICE_UNAVAILABLE)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_SERVICE_UNAVAILABLE)
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + END_BOUNDARY;

            checkBatchResponseBody(response, expectedBody);

            // $batch内の削除リクエストが処理されていることを確認する
            UserDataUtils.get(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales", "RDOnlyMode", HttpStatus.SC_NOT_FOUND);

            // $batch内の登録リクエストが処理されていないことを確認する
            UserDataUtils.get(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales", "RDOnlyModePost", HttpStatus.SC_NOT_FOUND);
            UserDataUtils.get(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, "SalesDetail", "RDOnlyModeNp", HttpStatus.SC_NOT_FOUND);

        } finally {
            // ReadDeleteOnlyモードを解除する
            MemcachedClient.getLockClient().delete("PcsReadDeleteMode");

            UserDataUtils.deleteLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", "RDOnlyMode", "SalesDetail", "RDOnlyMode", -1);
            UserDataUtils.deleteLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", "RDOnlyMode", "SalesDetail", "RDOnlyModeNp", -1);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales", "RDOnlyMode");
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Sales", "RDOnlyModePost");
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "SalesDetail", "RDOnlyMode");
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "SalesDetail", "RDOnlyModeNp");
        }
    }

    /**
     * レスポンスボディのチェック.
     * @param res TResponse
     * @param expectedResBody 期待するレスポンスボディ
     */
    void checkBatchResponseBody(TResponse res, String expectedResBody) {
        String[] arrResBody = res.getBody().split("\n");
        String[] arrExpResBody = expectedResBody.split("\n");

        for (int i = 0; i < arrResBody.length; i++) {
            Pattern p = Pattern.compile(arrExpResBody[i]);
            Matcher m = p.matcher(arrResBody[i]);
            assertTrue("expected " + arrExpResBody[i] + " but was " + arrResBody[i], m.matches());
        }

        assertFalse(arrResBody.length < arrExpResBody.length);

    }
}
