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
package io.personium.core.rs.odata;

import java.util.Map;

/**
 * The OdataBatch class.
 */
public class BatchBodyPart {
    private String httpMethod = null;
    private Map<String, String> httpHeaders = null;
    private String body = null;
    private String uri = null;
    private String changesetStr = null;
    private Boolean bChangesetStart = false;
    private Boolean bChangesetEnd = false;
    private String uriLast = null;

    private Boolean isLinksRequest = false;

    private String sourceEntitySetName;
    private String sourceEntitySetKey;
    private String targetEntitySetName;
    private String targetEntitySetKey;

    private String targetNavigationProperty;

    private String requestQuery = null;

    BatchBodyPart(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    /**
     * Getter of HttpHeaders.
     * @return HttpHeaders
     */
    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * Getter of request body.
     * @return request body
     */
    public String getEntity() {
        return this.body;
    }

    /**
     * Request body setter.
     * @ param bodyParam request body
     */
    public void setEntity(String bodyParam) {
        this.body = bodyParam;
    }

    /**
     * Getter of uri.
     * @return uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * uri's setter.
     * @param uri uri
     */
    public void setUri(String uri) {
        this.uri = uri;

        if (this.uri.endsWith("/")) {
            //If the URL ends with "/", delete the terminating "/"
            this.uri = this.uri.substring(0, this.uri.length() - 2);
        }

        //Get last URL of URL
        int index = this.uri.lastIndexOf('/');
        this.uriLast = this.uri.substring(index + 1);
    }

    /**
     * Getter of HttpMethod.
     * @return HttpMethod
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * HttpMethod's setter.
     * @param httpMethod HttpMethod
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Get the entity set name.
     * @return entity set name
     */
    public String getEntitySetName() {
        if (hasNavigationProperty() || isLinksRequest) {
            return this.sourceEntitySetName;
        } else {
            int i = this.uriLast.indexOf('(');
            if (i != -1) {
                return this.uriLast.substring(0, i);
            } else {
                return this.uriLast;
            }
        }
    }

    /**
     * Set the entity set name.
     * @ param entitySetName Entity set name
     */
    public void setSourceEntitySetName(String entitySetName) {
        this.sourceEntitySetName = entitySetName;
    }

    /**
     * Get the entity key.
     * @return entity key
     */
    public String getEntityKeyWithParences() {
        if (hasNavigationProperty() || isLinksRequest) {
            return "('" + this.sourceEntitySetKey + "')";
        } else {
            int i = this.uriLast.indexOf('(');
            if (i > -1 && i < this.uriLast.length()) {
                return this.uriLast.substring(i);
            } else {
                return null;
            }
        }
    }

    /**
     * Get the entity key.
     * @return entity key
     */
    public String getEntityKey() {
        if (hasNavigationProperty()) {
            return "'" + this.sourceEntitySetKey + "'";
        } else {
            int i = this.uriLast.indexOf('(');
            if (i > -1 && i < this.uriLast.length()) {
                return this.uriLast.substring(i + 1, this.uriLast.length() - 1);
            } else {
                return null;
            }
        }
    }

    /**
     * Set the entity key.
     * @ param entityKey entity key
     */
    public void setSourceEntityKey(String entityKey) {
        this.sourceEntitySetKey = entityKey;
    }

    /**
     * It returns whether it is the beginning of changeset.
     * @return true: beginning of changeset
     */
    public Boolean isChangesetStart() {
        return bChangesetStart;
    }

    /**
     * The setter of the beginning of the changeset.
     * @ param flg true: beginning of changeset
     */
    public void setbChangesetStart(Boolean flg) {
        this.bChangesetStart = flg;
    }

    /**
     * Return whether it is the end of changeset.
     * @return true: end of changeset
     */
    public Boolean isChangesetEnd() {
        return bChangesetEnd;
    }

    /**
     * The terminator flag setter of changeset.
     * @ param flg true: termination of changeset
     */
    public void setChangesetEnd(Boolean flg) {
        this.bChangesetEnd = flg;
    }

    /**
     * changeset String getter.
     * @return changeset string
     */
    public String getChangesetStr() {
        return changesetStr;
    }

    /**
     * changeset String setter.
     * @ param changesetStr changeset string
     */
    public void setChangesetStr(String changesetStr) {
        this.changesetStr = changesetStr;
    }

    /**
     * Set source / target information based on the path specified when registering via NavigationProperty.
     * @param requestPath requestPath
     */
    public void setNavigationProperty(String requestPath) {
        int sourceKeyStartIndex = requestPath.indexOf("(") + 2;
        int sourceKeyEndIndex = requestPath.indexOf("'", sourceKeyStartIndex);
        this.sourceEntitySetName = requestPath.substring(0, sourceKeyStartIndex - 2);
        this.sourceEntitySetKey = requestPath.substring(sourceKeyStartIndex, sourceKeyEndIndex);
        int lastPathNameIndex = requestPath.indexOf("/") + 1;
        this.targetNavigationProperty = requestPath.substring(lastPathNameIndex, requestPath.length());
    }

    /**
     * Get the target EntitySet name to use when registering via NavigationProperty.
     * @return the targetEntitySetName
     */
    public String getTargetNavigationProperty() {
        return this.targetNavigationProperty;
    }

    /**
     * Returns whether this bulk request is about to register via NavigationProperty.
     * @return Returns true when registering via NavigationProperty, false otherwise
     */
    public boolean hasNavigationProperty() {
        return this.targetNavigationProperty != null;
    }

    /**
     * Return the URI up to the Collection.
     * URI up to @return Collection
     */
    public String getCollectionUri() {
        int index = this.uri.lastIndexOf('/');
        return this.uri.substring(0, index);
    }

    /**
     * Return whether this bulk request is about to register Links or not.
     * @return Links Return true for registration, false otherwise
     */
    public Boolean isLinksRequest() {
        return isLinksRequest;
    }

    /**
     * Sets whether this bulk request is trying to register Links.
     * @ param isLinksRequest Links true for registration, false otherwise
     */
    public void setIsLinksRequest(Boolean isLinksRequest) {
        this.isLinksRequest = isLinksRequest;
    }

    /**
     * $ links Get the EntitySet name of the destination.
     * @return $ links destination EntitySet name
     */
    public String getTargetEntitySetName() {
        return targetEntitySetName;
    }

    /**
     * $ links Set the EntitySet name of the destination.
     * @ param entitySetName $ links destination EntitySet name
     */
    public void setTargetEntitySetName(String entitySetName) {
        this.targetEntitySetName = entitySetName;
    }

    /**
     * $ links Get the Entity key of the destination.
     * @return $ links destination Entity key
     */
    public String getTargetEntityKey() {
        return targetEntitySetKey;
    }

    /**
     * $ links Set the Entity key of the destination.
     * @ param entityKey $ links destination Entity key
     */
    public void setTargetEntityKey(String entityKey) {
        this.targetEntitySetKey = entityKey;
    }

    /**
     * Get a query for bulk requests.
     * @return Query for bulk requests
     */
    public String getRequestQuery() {
        return this.requestQuery;
    }

    /**
     * Set up a bulk request query.
     * @ param query query
     */
    public void setRequestQuery(String query) {
        this.requestQuery = query;
    }

}
