/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.test.jersey;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Date;

import javax.json.Json;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.model.jaxb.ObjectIo;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.PersoniumUrl;

/**
 * Base test class for Personium that extends JerseyTest.
 */
public class PersoniumTest extends JerseyTest {
    /**
     * Constructor.
     * @param application jax-rs application
     */
    public PersoniumTest(Application application) {
        super(application);
        RuntimeDelegate.setInstance(new org.glassfish.jersey.internal.RuntimeDelegateImpl());
    }

    /**
     *  Use Grizzly since the default container cannot handle HttpServletRequest and HttpServletResponse.
     */
    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {

        return new TestContainerFactory() {

            @Override
            public TestContainer create(URI baseUri, DeploymentContext arg1) {
                return createTestContainer(baseUri);
            }

            protected TestContainer createTestContainer(URI baseUri) {
                return new TestContainer() {
                    private HttpServer server;

                    @Override
                    public ClientConfig getClientConfig() {
                        return null;
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
                            afterStartingUp();
                        } catch (ProcessingException e) {
                            throw new TestContainerException(e);
                        } catch (IOException e) {
                            throw new TestContainerException(e);
                        }
                    }

                    @Override
                    public void stop() {
                        beforeShuttingDown();
                        this.server.shutdownNow();;
                    }

                };
            }
        };
    }
    
    /**
     * Name of the test cell. null unless createTestCell() is called.
     * This value is used when deleteTestCell() is called.
     */
    public volatile static String testCellName = null;

    /**
     * Create a cell for testing.
     * @return created cell name.
     */
    public static String createTestCell()  {
        long timestamp = new Date().getTime();
        testCellName = "ptest" + timestamp%1000000;
        String uUrl = PersoniumUnitConfig.getBaseUrl();
        
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost req = new HttpPost(uUrl + "__ctl/Cell");
            req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            String ent = Json.createObjectBuilder().add("Name", testCellName).build().toString();
            req.setEntity(new StringEntity(ent));
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(req) ) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            };
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
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpDelete req = new HttpDelete(pUrl.toHttp());
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            req.setHeader("X-Personium-Recursive", "true");
            client.execute(req);
            
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    /**
     * Configure an ExtCell entry on the test cell.
     * @param targetUrl Url of the entry
     */
    public static void createExtCellOnTestCell(String targetUrl) {
        PersoniumUrl pUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/__ctl/ExtCell");
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost req = new HttpPost(pUrl.toHttp());
            req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            String ent = Json.createObjectBuilder().add("Url", targetUrl).build().toString();
            req.setEntity(new StringEntity(ent));
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(req) ) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            };
         } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    /**
     * Create a role (box-unbound) on the test Cell.
     * @param roleName
     */
    public static void createRoleOnTestCell(String roleName) {
        PersoniumUrl pUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/__ctl/Role");
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost req = new HttpPost(pUrl.toHttp());
            req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            String ent = Json.createObjectBuilder().add("Name", roleName).build().toString();
            req.setEntity(new StringEntity(ent));
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(req) ) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(201, statusCode);
            };
         } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    /**
     * Configure ACL at the root level of the test cell and grant "all" privilege to a given role.
     * @param roleName
     */
    public static void grantAllPrivToRoleOnTestCell(String roleName) {
        PersoniumUrl pUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/");
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpEntityEnclosingRequestBase req = new HttpEntityEnclosingRequestBase() {
                @Override
                public String getMethod() {
                    return "ACL";
                }
            };
            req.setURI(URI.create(pUrl.toHttp()));
            req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_XML.getMimeType());
            req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
            Acl acl = new Acl();
            Ace ace1 = new Ace();
            acl.setBase(pUrl.toHttp()+ "__role/__/", false);
            ace1.setPrincipalHref(roleName);
            ace1.addGrantedPrivilege("all");
            acl.getAceList().add(ace1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectIo.marshal(acl, baos);
            baos.flush();
            String ent = baos.toString("utf-8");
            //System.out.println(ent);
            req.setEntity(new StringEntity(ent));
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            try (CloseableHttpResponse resp = client.execute(req) ) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(200, statusCode);
            };
        } catch (IOException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * link an ExtCell entry with a role entry on the test cell. 
     * @param cellUrl
     * @param roleName
     */
    public void linkRoleToExtCellOnTestCell(String cellUrl, String roleName) {
        PersoniumUrl pUrl = PersoniumUrl.create("personium-localunit:" + testCellName + ":/");
        try(CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost req = new HttpPost(pUrl.toHttp()+ "__ctl/ExtCell(%27" + CommonUtils.encodeUrlComp(cellUrl) + "%27)/$links/_Role");
            req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());
            String roleUri = pUrl.toHttp()+ "__ctl/Role('" + roleName + "')";
            String ent = Json.createObjectBuilder().add("uri", roleUri).build().toString();
            //System.out.println(ent);
            req.setEntity(new StringEntity(ent));
            try (CloseableHttpResponse resp = client.execute(req) ) {
                int statusCode = resp.getStatusLine().getStatusCode();
                assertEquals(204, statusCode);
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * A method called just before shutting down the server. 
     */
    public void beforeShuttingDown() {
    }
    /**
     * A method called right after starting up the server. 
     */
    public void afterStartingUp() {
    }
}
