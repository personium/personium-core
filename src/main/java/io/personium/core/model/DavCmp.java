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

package io.personium.core.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.Propertyupdate;
import org.xml.sax.SAXException;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.odata.PersoniumODataProducer;


/**
 * An delegate object interface for JaxRS Resources handling DAV related persistence.
 */
public interface DavCmp {
    /**
     * Type representing non-existing path.
     */
    String TYPE_NULL = "null";
    /**
     * Type representing a plain WebDAV collection.
     */
    String TYPE_COL_WEBDAV = "col.webdav";
    /**
     * Type representing a WebDAV Collection extended as ODataSvc.
     */
    String TYPE_COL_ODATA = "col.odata";
    /**
     * Type representing a WebDAV Collection extended as Box.
     */
    String TYPE_COL_BOX = "col.box";
    /**
     * Type representing a WebDAV Collection extended as Engine Service.
     */
    String TYPE_COL_SVC = "col.svc";
    /**
     * Type representing a WebDAV file.
     */
    String TYPE_DAV_FILE = "dav.file";
    /**
     * Type representing Cell.
     */
    String TYPE_CELL = "Cell";

    /**
     * source path for Engine service.
     */
    String SERVICE_SRC_COLLECTION = "__src";

    /**
     * Whether DavNode exists on the DB.
     * @return true if it exists
     */
    boolean exists();

    /**
     * Update Dav's management data information.
     */
    void load();

    /**
     * Update Dav's management data information <br />
     * If there is no management data, it is an error.
     */
    void loadAndCheckDavInconsistency();

    /**
     * @return acl
     */
    Acl getAcl();

    /**
     * Get property.
     * @param propertyName property name
     * @param propertyNamespace property namespace
     * @return property property value
     * @throws IOException If any IO errors occur
     * @throws SAXException If any parse errors occur
     */
    String getProperty(String propertyName, String propertyNamespace) throws IOException, SAXException;

    /**
     * @return properties
     */
    Map<String, String> getProperties();

    /**
     * @return Cell
     */
    Cell getCell();

    /**
     * @return Box
     */
    Box getBox();

    /**
     * @return Update time stamp
     */
    Long getUpdated();

    /**
     * @return Create time stamp
     */
    Long getPublished();

    /**
     * @return Content Length
     */
    Long getContentLength();

    /**
     * @return Content type
     */
    String getContentType();

    /**
     * @return Encryption type
     */
    String getEncryptionType();

    /**
     * @return Cell status.
     */
    String getCellStatus();

    /**
     * @return true if Cell Level
     */
    boolean isCellLevel();

    /**
     * Getter for schema authentication level setting.
     * @return schema authentication level
     */
    String getConfidentialLevel();

    /**
     * Getter for getting unit promotion permission user setting.
     * @return unit promotion permission user setting
     */
    List<String> getOwnerRepresentativeAccounts();

    /**
     * Returns the part responsible for the child path with the specified name.
     * @ param name path component path name of child path
     * @return Parts responsible for child path
     */
    DavCmp getChild(String name);

    /**
     * @return chilrdren DavCmp in the form of Map<path, DavCmp>
     */
    Map<String, DavCmp> getChildren();

    /**
     * Returns the part responsible for the parent path.
     * @return Parts responsible for parent path
     */
    DavCmp getParent();

    /**
     * Returns the number of parts of the child pass.
     * @return Number of parts of the child path
     */
    int getChildrenCount();

    /**
     * Return type string.
     * @return type string
     */
    String getType();

    /**
     * Returns the path string that this object is responsible for.
     * @return Path string responsible for this object
     */
    String getName();


    /**
     * Returns the nodeId of this object.
     * @return nodeId
     */
    String getId();

    /**
     * Returns true if there is no data below.
     * True if there is no data under @return.
     */
    boolean isEmpty();

    /**
     * Delete all data under the component, including itself.
     */
    void makeEmpty();

    /**
     * Processing of MKCOL method.
     * @ param type type
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder mkcol(String type);

    /**
     * Processing of ACL method.
     * @param reader Reader
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder acl(Reader reader);

    /**
     * File update processing by PUT method.
     * @ param contentType Content-Type header
     * @ param inputStream request body
     * @param etag Etag
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder putForUpdate(String contentType, InputStream inputStream, String etag);

    /**
     * File creation processing by PUT method.
     * @ param contentType Content-Type header
     * @ param inputStream request body
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder putForCreate(String contentType, InputStream inputStream);

    /**
     * Link with child resources.
     * @ param name Path component name of child resource Component name
     * @ param nodeId Node ID of child resource
     * @ param asof Time to keep as update time
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder linkChild(String name, String nodeId, Long asof);

    /**
     * Delete association with child resource.
     * @ param name child resource name
     * @ param asof Time to leave as deletion time
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder unlinkChild(String name, Long asof);

    /**
     * process PROPPATCH method.
     * @ param propUpdate PROPPATCH request object
     * @param url URL
     * @return response object
     */
    Multistatus proppatch(Propertyupdate propUpdate, String url);

    /**
     * process DELETE method.
     * @ param ifMatch If-Match header
     * @param recursive set true to process recursively
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder delete(String ifMatch, boolean recursive);

    /**
     * process GET method.
     * @ param rangeHeaderField Range header
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder get(String rangeHeaderField);

    /**
     * Return ODataProducer for data manipulation.
     * @return ODataProducer
     */
    PersoniumODataProducer getODataProducer();

    /**
     * Return ODataProducer for schema operation.
     * @param cell Cell
     * @return ODataProducer
     */
    PersoniumODataProducer getSchemaODataProducer(Cell cell);

    /**
     * @return ETag String.
     */
    String getEtag();


    /**
     * Move processing is performed.
     * @param etag ETag value
     * @ param overwrite Whether to overwrite the destination resource
     * @ param davDestination Hierarchy information of the destination
     * @return ResponseBuilder response
     */
    ResponseBuilder move(String etag, String overwrite, DavDestination davDestination);

    /**
     * Returns the URL of this DavNode resource.
     * @return URL string
     */
    String getUrl();

    /**
     * Return NotFound exceptions according to resources <br />
     * Since the messages depend on the resource, each resource class should override this method to define the message. <br />
     * The additional information of the message should be set by the caller without setting it here.
     * @return NotFound exception
     */
    PersoniumCoreException getNotFoundException();
}
