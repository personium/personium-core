/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

/**
 * Base test class for Personium that extended JerseyTest.
 */
public class PersoniumTest extends JerseyTest {
//    /**
//     * Constructor.
//     * @param application jax-rs application
//     */
//    public PersoniumTest() {
//        super();
//    }
    /**
     * Constructor.
     * @param application jax-rs application
     */
    public PersoniumTest(Application application) {
        super(application);
        RuntimeDelegate.setInstance(new org.glassfish.jersey.internal.RuntimeDelegateImpl());
    }

    /**
     * 標準だとHttpServletRequestやHttpServletResponseが取れないので
     * grizzlyで代用する
     */
    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {

//        return new GrizzlyWebTestContainerFactory();

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
                        } catch (ProcessingException e) {
                            throw new TestContainerException(e);
                        } catch (IOException e) {
                            throw new TestContainerException(e);
                        }
                    }

                    @SuppressWarnings("deprecation")
                    @Override
                    public void stop() {
                        this.server.stop();
                    }

                };
            }
        };
    }

//    @Override
//    protected DeploymentContext configureDeployment() {
//        return ServletDeploymentContext.builder(configure()).build();
//    }
}
