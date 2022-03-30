/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
    static class Category {
        /** Cell category. */
        static final String CELL       = "cell";
        /** Cell control object category. */
        static final String CELLCTL    = "cellctl";
        /** Box category. */
        static final String BOX        = "box";
        /** OData category. */
        static final String ODATA      = "odata";
        /** Webdav Collection category. */
        static final String WEBDAVCOL  = "webdavcol";
        /** OData Collection category. */
        static final String ODATACOL   = "odatacol";
        /** Service Collection category. */
        static final String SERVICECOL = "servicecol";
        /** Stream Collection category. */
        static final String STREAMCOL  = "streamcol";
        /** Webdav File category. */
        static final String WEBDAV     = "davfile";
        /** Message category. */
        static final String MESSAGE    = "message";
        /** Service category. */
        static final String SERVICE    = "service";
        /** Box Install category. */
        static final String BI         = "boxinstall";

        /** Timer category. */
        static final String TIMER      = "timer";
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

        /** Mkcol operation. */
        public static final String MKCOL     = "mkcol";
        /** Acl operation. */
        public static final String ACL       = "acl";
        /** Propfind operation. */
        public static final String PROPFIND  = "propfind";
        /** Proppatch operation. */
        public static final String PROPPATCH = "proppatch";

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

        /** Oneshot timer. */
        public static final String ONESHOT = "oneshot";
        /** Periodic timer. */
        public static final String PERIODIC = "periodic";
    }

    /** Separator string. */
    public static final String SEPARATOR = ".";

    /**
     * Get Type of cell operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String cell(String op) {
        return new StringBuilder(Category.CELL)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of cellctl basic operations.
     * @param entityName entitySetName
     * @param op Operation string
     * @return Type string
     */
    public static final String cellctl(String entityName, String op) {
        return new StringBuilder(Category.CELLCTL)
                .append(SEPARATOR)
                .append(entityName)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of box operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String box(String op) {
        return new StringBuilder(Category.BOX)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of WebDav Collection operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String webdavcol(String op) {
        return new StringBuilder(Category.WEBDAVCOL)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of OData Collection operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String odatacol(String op) {
        return new StringBuilder(Category.ODATACOL)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of Service Collection operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String servicecol(String op) {
        return new StringBuilder(Category.SERVICECOL)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of Stream Collection operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String streamcol(String op) {
        return new StringBuilder(Category.STREAMCOL)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of cellctl link operations.
     * @param src entitySetName to be linked
     * @param target entitySetName to link
     * @param op Operation string
     * @return Type string
     */
    public static final String cellctlLink(String src, String target, String op) {
        if (!Operation.LIST.equals(op) && src.compareTo(target) > 0) {
            // e.g. cellctl.Rule.links.Box.create -> cellct.Box.links.Rule.create
            return new StringBuilder(Category.CELLCTL)
                    .append(SEPARATOR)
                    .append(target)
                    .append(SEPARATOR)
                    .append(Operation.LINK)
                    .append(SEPARATOR)
                    .append(src)
                    .append(SEPARATOR)
                    .append(op)
                    .toString();
        } else {
            return new StringBuilder(Category.CELLCTL)
                    .append(SEPARATOR)
                    .append(src)
                    .append(SEPARATOR)
                    .append(Operation.LINK)
                    .append(SEPARATOR)
                    .append(target)
                    .append(SEPARATOR)
                    .append(op)
                    .toString();
        }
    }

    /**
     * Get Type of cellctl navprop operations.
     * @param src entitySetName
     * @param target entitySetName
     * @param op Operation string
     * @return Type string
     */
    public static final String cellctlNavProp(String src, String target, String op) {
        return new StringBuilder(Category.CELLCTL)
                .append(SEPARATOR)
                .append(src)
                .append(SEPARATOR)
                .append(Operation.NAVPROP)
                .append(SEPARATOR)
                .append(target)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of odata basic operations.
     * @param entityName entitySetName
     * @param op Operation string
     * @return Type string
     */
    public static final String odata(String entityName, String op) {
        return new StringBuilder(Category.ODATA)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of odata link operations.
     * @param src entitySetName to be linked
     * @param target entitySetName to link
     * @param op Operation string
     * @return Type string
     */
    public static final String odataLink(String src, String target, String op) {
        return new StringBuilder(Category.ODATA)
                .append(SEPARATOR)
                .append(Operation.LINK)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of odata navprop operations.
     * @param src entitySetName
     * @param target entitySetName
     * @param op Operation string
     * @return Type string
     */
    public static final String odataNavProp(String src, String target, String op) {
        return new StringBuilder(Category.ODATA)
                .append(SEPARATOR)
                .append(Operation.NAVPROP)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of odata schema basic operations.
     * @param entityName entitySetName
     * @param op Operation string
     * @return Type string
     */
    public static final String odataSchema(String entityName, String op) {
        return new StringBuilder(Category.ODATA)
                .append(SEPARATOR)
                .append(entityName)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of odata scheam link operations.
     * @param src entitySetName to be linked
     * @param target entitySetName to link
     * @param op Operation string
     * @return Type string
     */
    public static final String odataSchemaLink(String src, String target, String op) {
        if (!Operation.LIST.equals(op) && src.compareTo(target) > 0) {
            // e.g. odata.Property.links.EntityType.create -> odata.EntityType.links.Property.create
            return new StringBuilder(Category.ODATA)
                    .append(SEPARATOR)
                    .append(target)
                    .append(SEPARATOR)
                    .append(Operation.LINK)
                    .append(SEPARATOR)
                    .append(src)
                    .append(SEPARATOR)
                    .append(op)
                    .toString();
        } else {
            return new StringBuilder(Category.ODATA)
                    .append(SEPARATOR)
                    .append(src)
                    .append(SEPARATOR)
                    .append(Operation.LINK)
                    .append(SEPARATOR)
                    .append(target)
                    .append(SEPARATOR)
                    .append(op)
                    .toString();
        }
    }

    /**
     * Get Type of odata schema navprop operations.
     * @param src entitySetName
     * @param target entitySetName
     * @param op Operation string
     * @return Type string
     */
    public static final String odataSchemaNavProp(String src, String target, String op) {
        return new StringBuilder(Category.ODATA)
                .append(SEPARATOR)
                .append(src)
                .append(SEPARATOR)
                .append(Operation.NAVPROP)
                .append(SEPARATOR)
                .append(target)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of webdav operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String webdav(String op) {
        return new StringBuilder(Category.WEBDAV)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of message operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String message(String op) {
        return new StringBuilder(Category.MESSAGE)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of service operations.
     * @param op Operation string
     * @return Type string
     */
    public static final String service(String op) {
        return new StringBuilder(Category.SERVICE)
                .append(SEPARATOR)
                .append(op)
                .toString();
    }

    /**
     * Get Type of boxinstall operations.
     * @return Type string
     */
    public static final String boxinstall() {
        return new StringBuilder(Category.BI)
                .toString();
    }

    /**
     * Get Type of oneshot timer.
     * @return Type string
     */
    public static final String timerOneshot() {
        return Category.TIMER + SEPARATOR + Operation.ONESHOT;
    }

    /**
     * Get Type of periodic timer.
     * @return Type string
     */
    public static final String timerPeriodic() {
        return Category.TIMER + SEPARATOR + Operation.PERIODIC;
    }
}
