/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.core.rs.odata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import org.glassfish.jersey.uri.UriComponent;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;

/**
 * BatchBodyParser class.
 */
public class BatchBodyParser {

    private String collectionUri = null;

    //Add the valid value of $ top when analyzing the request in the BoundaryParser class
    private int bulkTopCount = 0;

    /**
     * Parsing the request body of $ batch.
     * @param boundary Boundary string
     * @param reader request body
     * @param requestUriParam baseUri
     * @return BatchBodyPart list
     */
    public List<BatchBodyPart> parse(String boundary, Reader reader, String requestUriParam) {
        //If the number of TODO requests exceeds 10,000, an error occurs

        this.collectionUri = requestUriParam.split("/\\$batch")[0];

        BufferedReader br = new BufferedReader(reader);
        //Getting the body
        StringBuilder body;
        try {
            body = getBatchBody(br);
        } catch (IOException e) {
            //IOException is a serious failure
            throw PersoniumCoreException.Server.UNKNOWN_ERROR.reason(e);
        }

        if (!body.toString().startsWith("--" + boundary + "\n")) {
            //If the beginning of the request body does not begin with "- boundary character string", it is regarded as an error
            throw PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR;
        }
        if (!body.toString().trim().endsWith("--" + boundary + "--")) {
            //If the end of the request body does not end with "- boundary string -", it is regarded as an error
            throw PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR;
        }

        //Retrieve individual requests
        List<BatchBodyPart> requests = getRequests(body.toString(), boundary);
        return requests;
    }

    /**
     * Get the request body of $ batch.
     * @param br Request body
     * @return request body (StringBuilder)
     * @throws IOException
     */
    private StringBuilder getBatchBody(BufferedReader br) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while (true) {
            line = br.readLine();
            if (line == null) {
                break;
            }
            body.append(line + "\n");
        }
        return body;
    }

    /**
     * Get individual requests from $ batch 's request body and return them in the list.
     * @param body $ batch request body
     * @param boundaryStr Boundary string
     * @return BatchBodyPart list
     */
    private List<BatchBodyPart> getRequests(String body, String boundaryStr) {
        List<BatchBodyPart> requests = new ArrayList<BatchBodyPart>();
        //Split body by boundary
        String[] arrBoundary = splitBoundary(body, boundaryStr);
        for (String boundaryBody : arrBoundary) {
            if (!boundaryBody.equals("")) {
                BoundaryParser boundary = new BoundaryParser(null, boundaryStr);
                requests.addAll(boundary.parse(boundaryBody));
            }
        }
        return requests;
    }

    private String[] splitBoundary(String input, String boundaryStr) {
        return input.split("--" + boundaryStr + "\n");
    }

    /**
     * The parser of the contents of the boundary.
     */
    class BoundaryParser {

        private Map<String, String> headers = null;
        private BoundaryParser parent = null;
        private String boundaryStr = null;

        /**
         * constructor.
         * @param boundary Boundary string
         */
        BoundaryParser(BoundaryParser boundary, String boundaryStr) {
            this.parent = boundary;
            this.boundaryStr = boundaryStr;
        }

        /**
         * Analyze the inside of the boundary.
         * @param boundary Body Body part of the boundary
         * @return BatchBodyPart list
         */
        List<BatchBodyPart> parse(String boundaryBody) {

            List<BatchBodyPart> requests = new ArrayList<BatchBodyPart>();

            //Split by line
            List<String> bodyLines = Arrays.asList(boundaryBody.split("\n"));

            //Get content type
            String type = getContentType(bodyLines);
            if (type == null) {
                //There is no specification of Content-Type
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(HttpHeaders.CONTENT_TYPE);
            }

            //Get the body
            String boundaryBodyPart = getBoundaryBody(bodyLines, this.headers.size());

            if (type.equals("application/http")) {
                //Handling requests
                requests.add(getRequest(boundaryBodyPart));
            } else if (type.startsWith("multipart/mixed")) {
                //Processing changeset

                //Nesting of changeset
                if (this.parent != null) {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_CHANGESET_NEST_ERROR;
                }

                //Retrieve changeset's boundary string
                String changeset = getBoundaryStr(type);

                //Split body by boundary
                List<BatchBodyPart> changesetRequests = new ArrayList<BatchBodyPart>();
                String[] arrChangeset = splitBoundary(boundaryBodyPart, changeset);
                for (String changesetBody : arrChangeset) {
                    if (!changesetBody.equals("")) {
                        BoundaryParser changesetBoundary = new BoundaryParser(this, changeset);
                        changesetRequests.addAll(changesetBoundary.parse(changesetBody));
                    }
                }

                //changeset Setting start flag
                changesetRequests.get(0).setbChangesetStart(true);
                //changeset Set termination flag
                changesetRequests.get(changesetRequests.size() - 1).setChangesetEnd(true);

                requests.addAll(changesetRequests);
            } else {
                //Content-Type is invalid
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(HttpHeaders.CONTENT_TYPE);
            }

            return requests;

        }

        /**
         * Get the request from the boundary and return it with BatchBodyPart type.
         * @param bodyPart Body part of the boundary
         * @return BatchBodyPart
         */
        private BatchBodyPart getRequest(String bodyPart) {

            //Form of request
            // ---------
            //HTTP header
            //:
            //Blank line
            //{Method} {Relative path of request}
            //{Request header}
            //Blank line
            //{Request body}
            // ---------

            List<String> lines = Arrays.asList(bodyPart.split("\n"));
            List<String> partHeaders = lines.subList(1, lines.size());
            Map<String, String> requestHeaders = getHeaders(partHeaders);

            BatchBodyPart batchBodyPart = new BatchBodyPart(requestHeaders);
            //requestLine is "{method} {relative path of request}"
            String requestLine = lines.get(0);
            String method = getMethod(requestLine);
            batchBodyPart.setHttpMethod(method);

            String requestUri = getUri(requestLine);
            String requestPath = getPath(requestUri);
            String requestQuery = getQuery(requestUri);

            if (requestPath.indexOf("/") > 0) {
                //$ links, NP-based requests are errors as queries are not supported and there is specification
                //Allow query if TODO acquisition request limit is canceled
                if (!requestQuery.equals("")) {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                }
                Pattern linkPathPattern = Pattern
                        .compile("^([^\\(]*)\\('([^']*)'\\)\\/\\$links\\/([^\\(]*)(\\('([^')]*)'\\))?$");
                Matcher linkMatcher = linkPathPattern.matcher(requestPath);
                Pattern npPathPattern = Pattern
                        .compile("^([^\\(]*)\\('([^']*)'\\)\\/_([^\\(]*)(\\('([^')]*)'\\))?$");
                Matcher npMatcher = npPathPattern.matcher(requestPath);
                if (linkMatcher.find()) {
                    //$ links request
                    batchBodyPart.setIsLinksRequest(true);
                    batchBodyPart.setSourceEntitySetName(linkMatcher.replaceAll("$1"));
                    batchBodyPart.setSourceEntityKey(linkMatcher.replaceAll("$2"));
                    batchBodyPart.setTargetEntitySetName(linkMatcher.replaceAll("$3"));
                    batchBodyPart.setTargetEntityKey(linkMatcher.replaceAll("$5"));
                    batchBodyPart.setUri(collectionUri + "/" + requestUri);
                } else if (npMatcher.find()) {
                    //NavigationProperty Available
                    batchBodyPart.setNavigationProperty(requestPath);
                    String targetNvProName = batchBodyPart.getTargetNavigationProperty();
                    batchBodyPart.setTargetEntitySetName(npMatcher.replaceAll("$3"));
                    batchBodyPart.setUri(collectionUri + "/" + targetNvProName);
                } else {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                }
            } else {
                Pattern reqPattern = Pattern.compile("^([^\\(]*)(\\('([^']*)'\\))?$");
                Matcher reqMatcher = reqPattern.matcher(requestPath);
                if (reqMatcher.find()) {
                    String targetID = reqMatcher.replaceAll("$2");
                    //Check method and path integrity
                    if (HttpMethod.POST.equals(method)) {
                        //POST _ If an ID of the target is specified, it is an error
                        if (null != targetID && !"".equals(targetID)) {
                            throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                        }
                    } else if (!HttpMethod.GET.equals(method)
                            && (null == targetID || "".equals(targetID))) {
                        //In cases other than POST and GET _ If an ID of the target is not specified, an error is made
                        throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                    }
                    //In the case of GET _ Since there is both one acquisition and list acquisition, the presence or absence of the target ID is not checked
                } else {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                }
                batchBodyPart.setUri(collectionUri + "/" + requestPath);
            }
            batchBodyPart.setEntity(getBoundaryBody(lines, requestHeaders.size() + 1));
            batchBodyPart.setChangesetStr(getChangesetStr());

            //Ignore designation when query is specified except for GET method
            if (HttpMethod.GET.equals(method)) {
                batchBodyPart.setRequestQuery(requestQuery);
                Integer top = null;
                //In order to check the sum of the $ top specified values ​​of the $ batch request as a whole, only the value of $ top is acquired
                //We ignore the exception here as we will check the errors of the values ​​of each query at the time of sending the request
                try {
                    top = QueryParser.parseTopQuery(UriComponent.decodeQuery(requestQuery, true).getFirst("$top"));
                } catch (PersoniumCoreException e) {
                    top = null;
                }
                if (top != null) {
                    checkTopCount(top);
                }
            }

            return batchBodyPart;
        }

        private String getChangesetStr() {
            String changesetStr = null;

            if (this.parent != null) {
                //In case of changeset processing
                return this.boundaryStr;
            }

            return changesetStr;
        }

        /**
         * Set the header and return the content type.
         * @param bodyLines Boundary contents
         * @return Content type
         */
        private String getContentType(List<String> bodyLines) {
            Map<String, String> map = getHeaders(bodyLines);
            this.headers = map;
            return this.headers.get(HttpHeaders.CONTENT_TYPE);
        }

        /**
         * A header is acquired from the body part.
         * @param bodyLines body part
         * @return header
         */
        private Map<String, String> getHeaders(List<String> bodyLines) {
            Map<String, String> map = new HashMap<String, String>();
            for (String bodyLine : bodyLines) {
                if (bodyLine.equals("")) {
                    break;
                }
                String[] types = bodyLine.split("[\\s]*:[\\s]*");
                if (types.length > 1) {
                    map.put(types[0], types[1]);
                }
            }
            return map;
        }

        /**
         * Get the HTTP method.
         * @param line {method} {path} *
         * @return HTTP method string
         */
        private String getMethod(String line) {
            Pattern pattern = Pattern.compile("(^[A-Z]+)[\\s]+([^\\s]+).*");
            Matcher m = pattern.matcher(line);

            String method = m.replaceAll("$1");

            //Check if it is an acceptable method
            if (!HttpMethod.POST.equals(method) && !HttpMethod.GET.equals(method) && !HttpMethod.PUT.equals(method)
                    && !HttpMethod.DELETE.equals(method)) {
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_METHOD_ERROR.params(method);
            }
            return method;
        }

        /**
         * Get the URI path of the request.
         * @param line {method} {URI path} *
         * @return Request URI path
         */
        private String getUri(String line) {
            Pattern pattern = Pattern.compile("(^[A-Z]+)[\\s]+([^\\s]+).*");
            Matcher m = pattern.matcher(line);

            return m.replaceAll("$2");
        }

        /**
         * Get the relative path of the request.
         * @param line {method} {path} *
         * Relative path of @return request
         */
        private String getPath(String line) {
            String[] path = line.split("\\?");
            return path[0];
        }

        /**
         * Get the relative path of the request.
         * @param line {method} {path} *
         * Relative path of @return request
         */
        private String getQuery(String line) {
            String[] path = line.split("\\?", 2);
            if (path.length != 2) {
                return "";
            }
            return path[1];
        }

        /**
         * Get the body from the body part of the boundary.
         * @param bodyLines body part
         * @param headersize Header size
         * @return body
         */
        private String getBoundaryBody(List<String> bodyLines, int headersize) {
            StringBuilder boundaryBody = new StringBuilder();
            for (int i = headersize + 1; i < bodyLines.size(); i++) {
                if (bodyLines.get(i).contains("--" + this.boundaryStr + "--")) {
                    break;
                }
                boundaryBody.append(bodyLines.get(i));
                boundaryBody.append("\n");
            }
            return boundaryBody.toString();
        }

        /**
         * Get the boundary character string.
         * @param contentType Value of Content-Type
         * @return Boundary string
         */
        private String getBoundaryStr(String contentType) {
            String boundary = null;

            Pattern pattern = Pattern.compile(".+boundary=(.+)");
            Matcher m = pattern.matcher(contentType);
            //There is no boundary designation
            if (!m.matches()) {
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(HttpHeaders.CONTENT_TYPE);
            }
            boundary = m.replaceAll("$1");

            return boundary;
        }

        /**
         * Check the $ top number specified in the bulk request as a whole.
         * If the total value of $ top in the bulk request as a whole is more than 10,000 it will return 400 error
         * @param query
         */
        private void checkTopCount(int query) {
            bulkTopCount += query;
            if (bulkTopCount > PersoniumUnitConfig.getTopQueryMaxSize()) {
                throw PersoniumCoreException.OData.BATCH_TOTAL_TOP_COUNT_LIMITATION_EXCEEDED;
            }

        }
    }
}
