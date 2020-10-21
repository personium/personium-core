/**
 * Personium
 * Copyright 2018-2020 Personium Project Authors
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
package io.personium.core.rs.box;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.CharEncoding;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.stream.StreamFactory;

/**
 * JAX-RS resource responsible for StreamTopicResource.
 */
public class StreamTopicResource extends StreamResource {

    private static final String PROP_ELEMENT_TOPICS = "topics";
    private static final String PROP_ELEMENT_TOPIC = "topic";

    private DavCmp davCmp;

    /**
     * constructor.
     * @param parent DavRsCmp
     * @param path Path name
     */
    public StreamTopicResource(final DavRsCmp parent, final String path) {
        super(parent, path);
        this.davCmp = parent.getDavCmp();
    }

    @Override
    protected List<String> getResources() {
        List<String> topics = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            String prop = this.davCmp.getPropertyAsRawString(PROP_ELEMENT_TOPICS,
                                                             CommonUtils.XmlConst.NS_PERSONIUM);
            if (prop == null) {
                return topics;
            }
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(prop.getBytes(CharEncoding.UTF_8));
            Document doc = builder.parse(is);
            Element element = doc.getDocumentElement();
            NodeList nl = element.getElementsByTagNameNS(CommonUtils.XmlConst.NS_PERSONIUM,
                                                         PROP_ELEMENT_TOPIC);
            for (int i = 0; i < nl.getLength(); i++) {
                topics.add(nl.item(i).getTextContent());
            }
        } catch (Exception e) {
            throw PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(e);
        }
        return topics;
    }

    @Override
    protected Response receive(String dest) {
        throw PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
    }

    @Override
    protected Response send(String dest, String cellUrl, String data) {
        return StreamFactory.createDataPublisher()
                            .map(sender -> {
                                 sender.open(dest);
                                 sender.publish(cellUrl, data);
                                 sender.close();
                                 return Response.ok().build();
                             })
                            .orElse(Response.status(Response.Status.FORBIDDEN)
                                            .entity("stream is disabled.")
                                            .build());
    }

}
