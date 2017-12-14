/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.core.event;

/**
 * Type definition of Event.
 */
public class PersoniumEventType {
    private PersoniumEventType() {
    }

    /** Category. */
    public static class Category {
        /** Cell category. */
        public static final String CELL    = "cell";
        /** Cell control object category. */
        public static final String CELLCTL = "cellctl";
        /** OData category. */
        public static final String ODATA   = "odata";
        /** Webdav category. */
        public static final String WEBDAV  = "davfile";
        /** Message category. */
        public static final String MESSAGE = "message";
        /** Service category. */
        public static final String SERVICE = "service";
    }

    /** Operation. */
    public static class Operation {
        /** Create operation. */
        public static final String CREATE  = "create";
        /** List operation. */
        public static final String LIST    = "list";
        /** Get operation. */
        public static final String GET     = "get";
        /** Update operation. */
        public static final String UPDATE  = "update";
        /** Merge operation. */
        public static final String MERGE   = "patch";
        /** Delete operation. */
        public static final String DELETE  = "delete";

        /** Link operation. */
        public static final String LINK    = "links";
        /** Navigation property operation. */
        public static final String NAVPROP = "navprop";

        /** Send operation for Message category. */
        public static final String SEND    = "send";
        /** Receive operation. */
        public static final String RECEIVE = "receive";
        /** Unread operation. */
        public static final String UNREAD  = "unread";
        /** Read operation. */
        public static final String READ    = "read";
        /** Approve operation. */
        public static final String APPROVE = "approve";
        /** Reject operation. */
        public static final String REJECT  = "reject";

        /** Exec operation for Service category. */
        public static final String EXEC    = "exec";

        /** Import operation for Cell category. */
        public static final String IMPORT  = "import";
        /** Export operation fora Cell category. */
        public static final String EXPORT  = "export";
    }

    /** Separator string. */
    public static final String SEPALATOR = ".";
}
