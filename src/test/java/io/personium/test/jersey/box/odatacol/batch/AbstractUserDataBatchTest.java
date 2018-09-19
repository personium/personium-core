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
package io.personium.test.jersey.box.odatacol.batch;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Application;

import io.personium.test.jersey.box.odatacol.AbstractUserDataTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.BatchUtils;
import io.personium.test.utils.TResponse;

/**
 * UserData $batchテスト用の抽象クラス.
 */
public abstract class AbstractUserDataBatchTest extends AbstractUserDataTest {
    String cellName = Setup.TEST_CELL1;
    String boxName = Setup.TEST_BOX1;
    String colName = Setup.TEST_ODATA;

    /**
     * constructor.
     */
    public AbstractUserDataBatchTest(Application application) {
        super(application);
    }

    /**
     * check response body.
     * @param res TResponse
     * @param expectedResBody Expected response body
     */
    public static void checkBatchResponseBody(TResponse res, String expectedResBody) {
        String[] arrResBody = res.getBody().split("\n");
        String[] arrExpResBody = expectedResBody.split("\n");

        ODataBatchResponseParser parser = new ODataBatchResponseParser();

        List<ODataResponse> odResEx = parser.parse(expectedResBody, arrExpResBody[0]);
        List<ODataResponse> odResAc = parser.parse(res.getBody(), arrResBody[0]);

        // check if # parts equals
        assertTrue("inconsistent #Parts. #expected="
            + odResEx.size()
            + ", while #actual=" + odResAc.size(),  odResAc.size() == odResEx.size());

        for (int i = 0; i < odResEx.size(); i++) {
            ODataResponse resEx = odResEx.get(i);
            ODataResponse resAc = odResAc.get(i);
            // should be same status code
            org.junit.Assert.assertEquals(resEx.getStatusCode(), resAc.getStatusCode());
            org.junit.Assert.assertEquals(resEx.bodyAsString(), resAc.bodyAsString());

            for (String headerKey : resEx.getHeaders().keySet()) {
                String hValueEx = resEx.getHeader(headerKey);
                String hValueAc = resAc.getHeader(headerKey);
                Pattern p = Pattern.compile(hValueEx);
                Matcher m = p.matcher(hValueAc);
                assertTrue("Header " + headerKey
                    + " should match.\n\n Expected\n" + hValueEx + "\nActual\n" + hValueAc,
                    hValueEx.equals(hValueAc) || m.matches());
            }
        }
        // assertFalse("res body shorter than expected", arrResBody.length < arrExpResBody.length);

    }

    String retrievePostResBodyToSetODataCol(String entitySetName, String id) {
        return BatchUtils.retrievePostResBody(cellName, boxName, colName, entitySetName, id, true);
    }

    String retrievePostResBodyToSetODataCol(String entitySetName, String id, boolean isTerminal) {
        return BatchUtils.retrievePostResBody(cellName, boxName, colName, entitySetName, id, isTerminal);
    }

}
