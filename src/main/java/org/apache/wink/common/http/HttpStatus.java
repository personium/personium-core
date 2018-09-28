/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *  
 *******************************************************************************/

package org.apache.wink.common.http;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP status codes and a helper methods.
 */
public class HttpStatus implements Cloneable {

    private static Map<Integer, HttpStatus> valuesByInt                     =
                                                                                new HashMap<Integer, HttpStatus>();

    private static final String             SL_11_START                     = "HTTP/1.1 "; //$NON-NLS-1$

    public static final HttpStatus          CONTINUE                        =
                                                                                new HttpStatus(
                                                                                               100,
                                                                                               "Continue", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          SWITCHING_PROTOCOLS             =
                                                                                new HttpStatus(
                                                                                               101,
                                                                                               "Switching Protocols", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          PROCESSING                      =
                                                                                new HttpStatus(
                                                                                               102,
                                                                                               "Processing", //$NON-NLS-1$
                                                                                               true);

    public static final HttpStatus          OK                              =
                                                                                new HttpStatus(
                                                                                               200,
                                                                                               "OK", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          CREATED                         =
                                                                                new HttpStatus(
                                                                                               201,
                                                                                               "Created", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          ACCEPTED                        =
                                                                                new HttpStatus(
                                                                                               202,
                                                                                               "Accepted", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          NON_AUTHORITATIVE_INFORMATION   =
                                                                                new HttpStatus(
                                                                                               203,
                                                                                               "Non Authoritative Information", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          NO_CONTENT                      =
                                                                                new HttpStatus(
                                                                                               204,
                                                                                               "No Content", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          RESET_CONTENT                   =
                                                                                new HttpStatus(
                                                                                               205,
                                                                                               "Reset Content", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          PARTIAL_CONTENT                 =
                                                                                new HttpStatus(
                                                                                               206,
                                                                                               "Partial Content", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          MULTI_STATUS                    =
                                                                                new HttpStatus(
                                                                                               207,
                                                                                               "Multi-Status", //$NON-NLS-1$
                                                                                               true);

    public static final HttpStatus          MULTIPLE_CHOICES                =
                                                                                new HttpStatus(
                                                                                               300,
                                                                                               "Multiple Choices", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          MOVED_PERMANENTLY               =
                                                                                new HttpStatus(
                                                                                               301,
                                                                                               "Moved Permanently", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          FOUND                           =
                                                                                new HttpStatus(
                                                                                               302,
                                                                                               "Found", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          SEE_OTHER                       =
                                                                                new HttpStatus(
                                                                                               303,
                                                                                               "See Other", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          NOT_MODIFIED                    =
                                                                                new HttpStatus(
                                                                                               304,
                                                                                               "Not Modified", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          USE_PROXY                       =
                                                                                new HttpStatus(
                                                                                               305,
                                                                                               "Use Proxy", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          TEMPORARY_REDIRECT              =
                                                                                new HttpStatus(
                                                                                               307,
                                                                                               "Temporary Redirect", //$NON-NLS-1$
                                                                                               true);

    public static final HttpStatus          BAD_REQUEST                     =
                                                                                new HttpStatus(
                                                                                               400,
                                                                                               "Bad Request", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          UNAUTHORIZED                    =
                                                                                new HttpStatus(
                                                                                               401,
                                                                                               "Unauthorized", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          PAYMENT_REQUIRED                =
                                                                                new HttpStatus(
                                                                                               402,
                                                                                               "Payment Required", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          FORBIDDEN                       =
                                                                                new HttpStatus(
                                                                                               403,
                                                                                               "Forbidden", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          NOT_FOUND                       =
                                                                                new HttpStatus(
                                                                                               404,
                                                                                               "Not Found", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          METHOD_NOT_ALLOWED              =
                                                                                new HttpStatus(
                                                                                               405,
                                                                                               "Method Not Allowed", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          NOT_ACCEPTABLE                  =
                                                                                new HttpStatus(
                                                                                               406,
                                                                                               "Not Acceptable", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          PROXY_AUTHENTICATION_REQUIRED   =
                                                                                new HttpStatus(
                                                                                               407,
                                                                                               "Proxy Authentication Required", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          REQUEST_TIMEOUT                 =
                                                                                new HttpStatus(
                                                                                               408,
                                                                                               "Request Timeout", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          CONFLICT                        =
                                                                                new HttpStatus(
                                                                                               409,
                                                                                               "Conflict", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          GONE                            =
                                                                                new HttpStatus(
                                                                                               410,
                                                                                               "Gone", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          LENGTH_REQUIRED                 =
                                                                                new HttpStatus(
                                                                                               411,
                                                                                               "Length Required", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          PRECONDITION_FAILED             =
                                                                                new HttpStatus(
                                                                                               412,
                                                                                               "Precondition Failed", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          REQUEST_TOO_LONG                =
                                                                                new HttpStatus(
                                                                                               413,
                                                                                               "Request Too Long", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          REQUEST_URI_TOO_LONG            =
                                                                                new HttpStatus(
                                                                                               414,
                                                                                               "Request-URI Too Long", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          UNSUPPORTED_MEDIA_TYPE          =
                                                                                new HttpStatus(
                                                                                               415,
                                                                                               "Unsupported Media Type", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          REQUESTED_RANGE_NOT_SATISFIABLE =
                                                                                new HttpStatus(
                                                                                               416,
                                                                                               "Requested Range Not Satisfiable", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          EXPECTATION_FAILED              =
                                                                                new HttpStatus(
                                                                                               417,
                                                                                               "Expectation Failed", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          INSUFFICIENT_SPACE_ON_RESOURCE  =
                                                                                new HttpStatus(
                                                                                               419,
                                                                                               "Insufficient Space On Resource", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          METHOD_FAILURE                  =
                                                                                new HttpStatus(
                                                                                               420,
                                                                                               "Method Failure", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          UNPROCESSABLE_ENTITY            =
                                                                                new HttpStatus(
                                                                                               422,
                                                                                               "Unprocessable Entity", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          LOCKED                          =
                                                                                new HttpStatus(
                                                                                               423,
                                                                                               "Locked", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          FAILED_DEPENDENCY               =
                                                                                new HttpStatus(
                                                                                               424,
                                                                                               "Failed Dependency", //$NON-NLS-1$
                                                                                               true);

    public static final HttpStatus          INTERNAL_SERVER_ERROR           =
                                                                                new HttpStatus(
                                                                                               500,
                                                                                               "Internal Server Error", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          NOT_IMPLEMENTED                 =
                                                                                new HttpStatus(
                                                                                               501,
                                                                                               "Not Implemented", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          BAD_GATEWAY                     =
                                                                                new HttpStatus(
                                                                                               502,
                                                                                               "Bad Gateway", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          SERVICE_UNAVAILABLE             =
                                                                                new HttpStatus(
                                                                                               503,
                                                                                               "Service Unavailable", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          GATEWAY_TIMEOUT                 =
                                                                                new HttpStatus(
                                                                                               504,
                                                                                               "Gateway Timeout", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          HTTP_VERSION_NOT_SUPPORTED      =
                                                                                new HttpStatus(
                                                                                               505,
                                                                                               "Http Version Not Supported", //$NON-NLS-1$
                                                                                               true);
    public static final HttpStatus          INSUFFICIENT_STORAGE            =
                                                                                new HttpStatus(
                                                                                               507,
                                                                                               "Insufficient Storage", //$NON-NLS-1$
                                                                                               true);

    private int                             code;
    private String                          message;

    private HttpStatus(int code, String message, boolean register) {
        this.code = code;
        this.message = message;
        if (register) {
            valuesByInt.put(code, this);
        }
    }

    /**
     * @param code status code
     * @param message status message
     * @deprecated
     */
    protected HttpStatus(int code, String message) {
        this(code, message, false);
    }

    /**
     * Gets HttpStatus for given status code.
     * 
     * @param code status code
     * @return HttpStatus
     */
    public static HttpStatus valueOf(int code) {
        return valuesByInt.get(code);
    }

    /**
     * Gets HttpStatus for given status line.
     * 
     * @param statusLine status line
     * @return HttpStatus
     */
    public static HttpStatus valueOfStatusLine(String statusLine) {

        if (!statusLine.startsWith(SL_11_START)) {
            throw new IllegalArgumentException(statusLine);
        }
        int code =
            Integer.parseInt(statusLine.substring(SL_11_START.length(), SL_11_START.length() + 3));
        return valuesByInt.get(code);
    }

    /**
     * Gets the status code.
     * 
     * @return status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the status message.
     * 
     * @return status message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the status line like <code>HTTP/1.1 404 Not Found</code>.
     * 
     * @return the status line
     */
    public String getStatusLine() {

        StringBuilder builder = new StringBuilder(SL_11_START.length() + 4 + message.length());
        builder.append(SL_11_START).append(code).append(' ').append(message);
        return builder.toString();
    }

    /**
     * Is this an error status.
     * 
     * @return <code>true</code> if this status has code greater or equal to
     *         400.
     */
    public boolean isError() {
        return code >= 400;
    }

    /**
     * Duplicates this status with another status message.
     * 
     * @param message new status message
     * @return new HttpStatus
     */
    public HttpStatus duplicate(String message) {
        HttpStatus clone = clone();
        clone.message = message;
        return clone;
    }

    @Override
    public HttpStatus clone() {
        try {
            return (HttpStatus)super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // can't happen
        }
    }

    @Override
    public String toString() {
        return code + " " + message; //$NON-NLS-1$
    }

    @Override
    public boolean equals(Object httpStatus) {
        return httpStatus instanceof HttpStatus && ((HttpStatus)httpStatus).getCode() == getCode();

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
