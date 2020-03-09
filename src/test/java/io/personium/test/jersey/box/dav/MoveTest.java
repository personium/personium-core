/**
 * Personium
 * Copyright 2020 Personium Project Authors
 *  - Akio Shimono
 *  - FUJITSU LIMITED
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
package io.personium.test.jersey.box.dav;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.PersoniumUrl;
import io.personium.test.jersey.IntegrationTestBase;


public class MoveTest extends IntegrationTestBase {
    /** logger. */
    private static Logger log = LoggerFactory.getLogger(MoveTest.class);
    @BeforeClass
    public static void beforeClass() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
    }

    @Before
    public void setUp() throws Exception {
        createTestCell();
        createBoxOnTestCell("box1", "https://app1.unit.example/");
    }
    @AfterClass
    public static void tearDown() throws Exception{
        deleteTestCell();
    }


    @Test
    public void Move_ToSiblingDestination_Should_Work() throws Exception {
        String cellUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/").toHttp();
        log.info("" + cellUrl);

        // create test file on a specific path.
        String orignPath = "box1/origin.txt";
        String destPath = "box1/destination.txt";
        String testFileContent = "testFileContent";
        putFileOnTestCell(orignPath, testFileContent.getBytes("utf-8"), ContentType.TEXT_PLAIN.toString());
        
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            // MOVE request
            HttpRequestBase req = new HttpRequestBase() {
                @Override
                public String getMethod() {
                    return "MOVE";
                }
            };
            req.setURI(URI.create(cellUrl + orignPath));
            log.info("Req Url = " + req.getURI().toString());

            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            req.setHeader(HttpHeaders.DESTINATION, cellUrl + "box1/destination.txt");

            // Issue the request
            try (CloseableHttpResponse resp = client.execute(req) ) {
                // should respond 201
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            };
            
            // Confirm that the moved resource actually exists on the destination URL.
            HttpGet getReq = new HttpGet(cellUrl + destPath);
            getReq.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(getReq) ) {
                // should respond 200
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(200, statusCode);
                String ctype = resp.getHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue();
                assertEquals(ContentType.TEXT_PLAIN.toString(), ctype);
                HttpEntity ent = resp.getEntity();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ent.writeTo(baos);
                String bodyStr = baos.toString("utf-8");
                baos.close();
                assertEquals(testFileContent, bodyStr);
            };
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}

