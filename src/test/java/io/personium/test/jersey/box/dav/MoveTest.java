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

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
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
    public void Move_ShouldWork_InSubdomainBasedMode() throws Exception {
        String cellUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/").toHttp();
        log.info("" + cellUrl);

        String orignPath = "box1/origin.txt";
        putFileOnTestCell(orignPath, "testFile".getBytes("utf-8"), ContentType.TEXT_PLAIN.getMimeType());
        
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
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
            try (CloseableHttpResponse resp = client.execute(req) ) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            };
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }
    
}

