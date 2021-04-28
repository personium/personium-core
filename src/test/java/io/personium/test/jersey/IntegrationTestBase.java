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
package io.personium.test.jersey;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.Json;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.PersoniumUrl;

@RunWith(PersoniumIntegTestRunner.class)
public class IntegrationTestBase {
    /** logger. */
    private static Logger log = LoggerFactory.getLogger(IntegrationTestBase.class);

    /**
     * Configured deployment context for the tested application.
     */
    private final DeploymentContext context;

    /**
     * The test container on which the tests would be run.
     */
    static TestContainer testContainer;
    static AtomicReference<Client> client = new AtomicReference<>(null);

    /**
     * Name of the test cell. null unless createTestCell() is called.
     * This value is used when deleteTestCell() is called.
     */
    public volatile static String testCellName = null;
    @BeforeClass
    public static void beforeClass() throws Exception {
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
        PersoniumUnitConfig.reload();
        //PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
    }
    @AfterClass
    public static void afterClass() throws Exception {
        stopJServer();
        PersoniumUnitConfig.reload();
    }
    public IntegrationTestBase() {
        this.context = DeploymentContext.newInstance(new PersoniumCoreApplication());
        //testContainerFactory = getTestContainerFactory();
        this.startJServer();
    }

//    @Before
//    public void setUp() throws Exception {
//    }
//    @After
//    public void tearDown() throws Exception{
//    }

    public void startJServer() {
        if (testContainer == null) {
            testContainer = getTestContainerFactory().create(getBaseUri(), context);
            try {
                testContainer.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Create an set new client.
            client.getAndSet(ClientBuilder.newClient(testContainer.getClientConfig()));
        }
    }
    protected static URI getBaseUri() {
        if (testContainer != null) {
            // called from outside of JerseyTest constructor
            return testContainer.getBaseUri();
        }

        // called from within JerseyTest constructor
        return UriBuilder.fromUri("http://localhost/").port(TestProperties.DEFAULT_CONTAINER_PORT).build();
    }
    public static void stopJServer() throws IOException {
        try {
            testContainer.stop();
            testContainer = null;
        } finally {
            JerseyTest.closeIfNotNull(client.getAndSet(null));
        }
    }
//  private static HttpServer server = null;

//    public static void startServer() throws IOException {
//        if (server == null) {
//            log.info("Initializing an instance of Grizzly Container");
//            //PersoniumCoreApplication.start();
//            WebappContext ctx = new WebappContext() {};
//            ctx.addContextInitParameter("jersey.config.server.provider.packages", "io.personium.core");
//            //ctx.addListener("com.package.something.AServletContextListener");
//            String unitUrl = PersoniumUnitConfig.getBaseUrl();
//            log.info("Unit Url = [" + unitUrl + "]");
//
//            server = GrizzlyWebContainerFactory.create(unitUrl, Collections.singletonMap(
//                          "jersey.config.server.provider.packages", "io.personium.core.rs"));
//            //GrizzlyHttpServerFactory.createHttpServer(URI.create(unitUrl), rc);
//            ctx.deploy(server);
//            server.start();
//            log.info("Grizzly Container started");
//        }
//    }
//    public static void stopServer() {
//        if (server != null && server.isStarted()) {
//            log.info("Shutting down the initialized Grizzly instance");
//            //PersoniumCoreApplication.stop();
//            server.shutdownNow();
//            server = null;
//        }
//    }

    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new TestContainerFactory() {

            @Override
            public TestContainer create(URI baseUri, DeploymentContext arg1) {
                return createTestContainer(baseUri);
            }

            protected TestContainer createTestContainer(URI baseUri) {
                return new TestContainer() {
                    private HttpServer server;
                    ClientConfig cc = new ClientConfig();

                    @Override
                    public ClientConfig getClientConfig() {
                        return cc;
                    }

                    @Override
                    public URI getBaseUri() {
                        return baseUri;
                    }

                    @Override
                    public void start() {
                        try {
                            this.server = GrizzlyWebContainerFactory.create(
                                    baseUri,
                                    Collections.singletonMap(
                                            "jersey.config.server.provider.packages", "io.personium.core"));
                        } catch (ProcessingException e) {
                            throw new TestContainerException(e);
                        } catch (IOException e) {
                            throw new TestContainerException(e);
                        }
                    }

                    @Override
                    public void stop() {
                        this.server.shutdownNow();
                    }
                };
            }
        };
    }

    /**
     * Create a cell for testing.
     * @return created cell name.
     */
    public static String createTestCell()  {
        long timestamp = new Date().getTime();
        testCellName = "ptest" + timestamp % 1000000;
        String uUrl = PersoniumUnitConfig.getBaseUrl();

        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost req = new HttpPost(uUrl + "__ctl/Cell");
            req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            String ent = Json.createObjectBuilder().add("Name", testCellName).build().toString();
            req.setEntity(new StringEntity(ent));
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(req)) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            }
            return testCellName;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    /**
     * Create a box on the test cell.
     * @return created box name.
     */
    public static String createBoxOnTestCell(String boxName, String boxSchema) {
        String cellUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/").toHttp();

        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost req = new HttpPost(cellUrl + "__ctl/Box");
            log.info("Cell Url = " + cellUrl);

            req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            String ent = Json.createObjectBuilder()
                    .add("Name", boxName)
                    .add("Schema", boxSchema)
                    .build().toString();
            req.setEntity(new StringEntity(ent));
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(req)) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            }
            return testCellName;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    /**
     * Delete the cell for testing.
     */
    public static void deleteTestCell() {
        String localunitCellUrl = "personium-localunit:" + testCellName + ":/";
        PersoniumUrl pUrl = PersoniumUrl.create(localunitCellUrl);
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpDelete req = new HttpDelete(pUrl.toHttp());
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            req.setHeader("X-Personium-Recursive", "true");
            client.execute(req);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    /**
     * Put a file on the test cell.
     * @return created box name.
     */
    public static String putFileOnTestCell(String path, byte[] contents, String contentType) {
        String cellUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/").toHttp();

        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPut req = new HttpPut(cellUrl + path);
            log.info("Req Url = " + req.getURI().toString());

            req.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
            req.setEntity(new ByteArrayEntity(contents));
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(req)) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            }
            return testCellName;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
