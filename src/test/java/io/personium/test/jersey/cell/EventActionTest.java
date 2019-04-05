/**
 * personium.io
 * Copyright 2019 FUJITSU LIMITED
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
package io.personium.test.jersey.cell;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RuleUtils;
import io.personium.test.utils.TResponse;

/**
 * Event action test.
 */
@RunWith(PersoniumIntegTestRunner.class)
public class EventActionTest extends PersoniumTest {

    private Log log = LogFactory.getLog(EventActionTest.class);

    /** Time to wait for action execution. */
    private static final long SLEEP_MILLES = 5000;
    /** Name of engine script. */
    private static final String SERVICE_SOURCE_NAME = "service.js";
    /** Name of engine service. */
    private static final String SERVICE_NAME = "service";
    /** Source of engine script. */
    private static final String SOURCE
    = "function(request){"
    + "  return {"
    + "    status: 200,"
    + "    headers: {'Content-Type':'application/json'},"
    + "    body: ['Hello World.']"
    + " };"
    + "}";
    /** Name of rule. */
    private static final String RULE_NAME = "ExecEventTestRule";
    /** Type of rule. */
    private static final String RULE_TYPE = "ExecEventTestType";

    /**
     * Constructor.
     */
    public EventActionTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Before.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        // Put engine script.
        DavResourceUtils.createServiceCollectionSource(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ENGINE_SERVICE, SERVICE_SOURCE_NAME, "text/javascript",
                AbstractCase.MASTER_TOKEN_NAME, SOURCE, HttpStatus.SC_CREATED);

        // Register engine service.
        DavResourceUtils.setServiceProppatch(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ENGINE_SERVICE,
                AbstractCase.MASTER_TOKEN_NAME, SERVICE_NAME, SERVICE_SOURCE_NAME, HttpStatus.SC_MULTI_STATUS);

        // Create rule.
        String serviceUrl =
                "personium-localcell:/" + Setup.TEST_BOX1 + "/" + Setup.TEST_ENGINE_SERVICE + "/" + SERVICE_NAME;
        JSONObject ruleJson = new JSONObject();
        ruleJson.put("Name", RULE_NAME);
        ruleJson.put("EventType", RULE_TYPE);
        ruleJson.put("EventExternal", "true");
        ruleJson.put("Action", "exec");
        ruleJson.put("TargetUrl", serviceUrl);
        RuleUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, ruleJson, HttpStatus.SC_CREATED);
    }

    /**
     * After.
     */
    @After
    public void after() {
        // Clear cell level acl.
        CellUtils.setAclDefault(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME);

        // Delete rule.
        RuleUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, RULE_NAME, null, -1);

        // UnRegister engine service.
        DavResourceUtils.removeServiceProppatch(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ENGINE_SERVICE,
                AbstractCase.MASTER_TOKEN_NAME, SERVICE_NAME, -1);

        // Delete engine script.
        DavResourceUtils.deleteServiceCollectionSource(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ENGINE_SERVICE, SERVICE_SOURCE_NAME,
                AbstractCase.MASTER_TOKEN_NAME, -1);
        //TODO ログ削除が実装されたらログを削除する
    }

    /**
     * Test of exec action.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void event_exec() {
        JSONObject eventJson = new JSONObject();
        eventJson.put("Type", RULE_TYPE);
        eventJson.put("Object", "TestObject");
        eventJson.put("Info", "TestInfo");

        // No exec privilege
        CellUtils.setAclSingle(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "role4",
                "<D:event/>", HttpStatus.SC_OK);
        String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account4", "password4");
        CellUtils.event(token, HttpStatus.SC_OK, Setup.TEST_CELL1, eventJson.toJSONString());
        waitForInterval();
        checkEventLogStatus(HttpStatus.SC_FORBIDDEN);

        // exec privilege
        CellUtils.setAclSingle(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "role5",
                "<D:event/>", HttpStatus.SC_OK);
        token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account5", "password5");
        CellUtils.event(token, HttpStatus.SC_OK, Setup.TEST_CELL1, eventJson.toJSONString());
        waitForInterval();
        checkEventLogStatus(HttpStatus.SC_OK);
    }

    /**
     * Check eventlog status.
     * @param expectedStatus Expected status of the last log line
     */
    private void checkEventLogStatus(int expectedStatus) {
        TResponse logResponse = CellUtils.getLog(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                Setup.TEST_CELL1, "current", "default.log");
        log.debug(logResponse.getBody());
        String[] logs = logResponse.getBody().split(",");
        int status = Integer.parseInt(logs[logs.length - 1].replaceAll("[^0-9]", ""));
        assertThat(status, is(expectedStatus));
    }

    /**
     * wait for interval.
     */
    private void waitForInterval() {
        try {
            Thread.sleep(PersoniumUnitConfig.getAccountValidAuthnInterval() * SLEEP_MILLES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
