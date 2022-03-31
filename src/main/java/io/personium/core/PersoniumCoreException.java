/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.exceptions.ODataErrorMessage;
import io.personium.core.utils.EscapeControlCode;
import io.personium.plugin.base.PluginMessageUtils.Severity;

/**
 * Log message creation class.
 */
@SuppressWarnings("serial")
public class PersoniumCoreException extends RuntimeException {

    /**
     * OData related.
     */
    public static class OData {
        /**
         * When JSON parsing fails.
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-OD-0001");
        /**
         * Failure in parsing the query.
         */
        public static final PersoniumCoreException QUERY_PARSE_ERROR = create("PR400-OD-0002");
        /**
         * Failure parsing $ fileter.
         */
        public static final PersoniumCoreException FILTER_PARSE_ERROR = create("PR400-OD-0003");
        /**
         * Failed to parse EntityKey.
         */
        public static final PersoniumCoreException ENTITY_KEY_PARSE_ERROR = create("PR400-OD-0004");

        /**
         * The value specified for $ format is invalid.
         */
        public static final PersoniumCoreException FORMAT_INVALID_ERROR = create("PR400-OD-0005");
        /**
         * The request data format is invalid.
         * {0} Property name
         */
        public static final PersoniumCoreException REQUEST_FIELD_FORMAT_ERROR = create("PR400-OD-0006");
        /**
         * The field name of the request body is invalid.
         * {0}: Detailed message
         * Occurs when you try to update the management information and when you try to set a value that does not exist in the schema
         * Note) Since this error will manage messages by source, do not use this in the future.
         */
        public static final PersoniumCoreException FIELED_INVALID_ERROR = create("PR400-OD-0007");
        /**
         * The corresponding Association does not exist.
         */
        public static final PersoniumCoreException NO_SUCH_ASSOCIATION = create("PR400-OD-0008");
        /**
         * There is no essential item of the request body.
         * {0}: Property name
         */
        public static final PersoniumCoreException INPUT_REQUIRED_FIELD_MISSING = create("PR400-OD-0009");
        /**
         * There is no essential item of the request body.
         */
        public static final PersoniumCoreException KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED = create("PR400-OD-0010");
        /**
         * There is no Key specification of the request URL.
         */
        public static final PersoniumCoreException KEY_FOR_NAVPROP_SHOULD_BE_SPECIFIED = create("PR400-OD-0011");
        /**
         * Character type is invalid.
         * {0}: Property name
         */
        public static final PersoniumCoreException INVALID_TYPE_ERROR = create("PR400-OD-0012");
        /**
         * The value specified for $ inlinecount is invalid.
         * {0}: value specified by inlinecount
         */
        public static final PersoniumCoreException INLINECOUNT_PARSE_ERROR = create("PR400-OD-0013");
        /**
         * The specified property does not exist.
         */
        public static final PersoniumCoreException UNKNOWN_PROPERTY_APPOINTED = create("PR400-OD-0014");
        /**
         * Failure in parsing $ orderby.
         */
        public static final PersoniumCoreException ORDERBY_PARSE_ERROR = create("PR400-OD-0015");
        /**
         * A single key was specified as null.
         */
        public static final PersoniumCoreException NULL_SINGLE_KEY = create("PR400-OD-0016");
        /**
         * Failure in parsing $ select.
         */
        public static final PersoniumCoreException SELECT_PARSE_ERROR = create("PR400-OD-0017");
        /**
         * An update of AssociationEnd was requested.
         */
        public static final PersoniumCoreException NOT_PUT_ASSOCIATIONEND = create("PR400-OD-0019");
        /**
         * The type of data to be registered is different from the data type registered in elasticsearch.
         */
        public static final PersoniumCoreException SCHEMA_MISMATCH = create("PR400-OD-0020");
        /**
         * The body format of $ batch is invalid.
         * Incorrect designation of header
         * {0}: header name
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_HEADER_ERROR = create("PR400-OD-0021");
        /**
         * The body format of $ batch is invalid.
         * When nesting of changeset is specified
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_CHANGESET_NEST_ERROR = create("PR400-OD-0022");
        /**
         * When parsing $ batch's body fails.
         */
        public static final PersoniumCoreException BATCH_BODY_PARSE_ERROR = create("PR400-OD-0023");
        /**
         * The resource specified in the NTKP of the body does not exist in the update request.
         * {0}: value specified by NTKP
         */
        public static final PersoniumCoreException BODY_NTKP_NOT_FOUND_ERROR = create("PR400-OD-0024");
        /**
         * The NTKP specified by $ expand does not exist as a resource.
         * {0}: value specified by $ expand
         */
        public static final PersoniumCoreException EXPAND_NTKP_NOT_FOUND_ERROR = create("PR400-OD-0025");
        /**
         * Failed to parse $ expand.
         */
        public static final PersoniumCoreException EXPAND_PARSE_ERROR = create("PR400-OD-0026");
        /**
         * The index of another schema type has already been created.
         */
        public static final PersoniumCoreException ANOTHRE_SCHEMA_TYPE_ALREADY_EXISTS = create("PR400-OD-0027");
        /**
         * Failure parsing EntityKey of $ links.
         */
        public static final PersoniumCoreException ENTITY_KEY_LINKS_PARSE_ERROR = create("PR400-OD-0028");
        /**
         * The value specified for the query is invalid.
         */
        public static final PersoniumCoreException QUERY_INVALID_ERROR = create("PR400-OD-0029");
        /**
         * The number of requests specified by $ Batch is invalid.
         */
        public static final PersoniumCoreException TOO_MANY_REQUESTS = create("PR400-OD-0030");
        /**
         * Specify 1: 1 in $ link registration.
         */
        public static final PersoniumCoreException INVALID_MULTIPLICITY = create("PR400-OD-0031");

        /**
         * The number of hierarchies of EnitityType and the number of contained properties exceeded the limit.
         */
        public static final PersoniumCoreException ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED = create("PR400-OD-0032");

        /**
         * The limit of the number of EnitityTypes has been exceeded.
         */
        public static final PersoniumCoreException ENTITYTYPE_COUNT_LIMITATION_EXCEEDED = create("PR400-OD-0033");

        /**
         * The body format of $ batch is invalid.
         * Request path specification error
         * {0}: Request path
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_PATH_ERROR = create("PR400-OD-0034");

        /**
         * The body format of $ batch is invalid.
         * A method that can not be accepted with $ batch is specified
         * {0}: method
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_METHOD_ERROR = create("PR400-OD-0035");

        /**
         * Failure in parsing the query.
         * {0}: Failed query
         */
        public static final PersoniumCoreException QUERY_PARSE_ERROR_WITH_PARAM = create("PR400-OD-0036");

        /**
         * The sum of the values ​​of $ top specified in the entire $ batch exceeded the upper limit.
         */
        public static final PersoniumCoreException BATCH_TOTAL_TOP_COUNT_LIMITATION_EXCEEDED = create("PR400-OD-0037");

        /**
         * $ links is over the maximum number that can be created.
         */
        public static final PersoniumCoreException LINK_UPPER_LIMIT_RECORD_EXEED = create("PR400-OD-0038");

        /**
         * The sum of the values ​​of the specified $ expand exceeded the upper limit.
         */
        public static final PersoniumCoreException EXPAND_COUNT_LIMITATION_EXCEEDED = create("PR400-OD-0039");

        /**
         * An array type property was specified in the $ orderby query.
         */
        public static final PersoniumCoreException CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY = create("PR400-OD-0040");

        /**
         * The value {1} of the request header {0} is incorrect.
         */
        public static final PersoniumCoreException BAD_REQUEST_HEADER_VALUE = create("PR400-OD-0041");

        /**
         * Unsupported operation was performed Details: {0}.
         */
        public static final PersoniumCoreException OPERATION_NOT_SUPPORTED = create("PR400-OD-0042");

        /**
         * An unknown operator is specified.
         */
        public static final PersoniumCoreException UNSUPPORTED_QUERY_OPERATOR = create("PR400-OD-0043");

        /**
         * An unknown function is specified.
         */
        public static final PersoniumCoreException UNSUPPORTED_QUERY_FUNCTION = create("PR400-OD-0044");

        /**
         * When unknown property is specified.
         */
        public static final PersoniumCoreException UNKNOWN_QUERY_KEY = create("PR400-OD-0045");

        /**
         * A value of a format different from the data type of the property is specified.
         */
        public static final PersoniumCoreException OPERATOR_AND_OPERAND_TYPE_MISMATCHED = create("PR400-OD-0046");

        /**
         * A value outside the range of the property's data type is specified.
         */
        public static final PersoniumCoreException UNSUPPORTED_OPERAND_FORMAT = create("PR400-OD-0047");

        /**
         * Unable to unseal search value.
         */
        public static final PersoniumCoreException OPERATOR_AND_OPERAND_UNABLE_TO_UNESCAPE = create("PR400-OD-0048");

        /**
         * Cell URL Invalid format.
         * {0} property name
         */
        public static final PersoniumCoreException CELL_URL_FORMAT_ERROR = create("PR400-OD-0049");
        /**
         * Schema URI Invalid format.
         * {0} property name
         */
        public static final PersoniumCoreException SCHEMA_URI_FORMAT_ERROR = create("PR400-OD-0050");
        /**
         * search query invalid error.
         */
        public static final PersoniumCoreException SEARCH_QUERY_INVALID_ERROR = create("PR400-OD-0051");

        /**
         * The corresponding EntitySet does not exist.
         */
        public static final PersoniumCoreException NO_SUCH_ENTITY_SET = create("PR404-OD-0001");
        /**
         * The corresponding Entity does not exist.
         */
        public static final PersoniumCoreException NO_SUCH_ENTITY = create("PR404-OD-0002");
        /**
         * The corresponding resource does not exist.
         */
        public static final PersoniumCoreException NOT_FOUND = create("PR404-OD-0000");
        /**
         * The corresponding Navigation Property does not exist.
         */
        public static final PersoniumCoreException NOT_SUCH_NAVPROP = create("PR404-OD-0003");
        /**
         * Operation on the entity where the relevant data resides.
         */
        public static final PersoniumCoreException CONFLICT_HAS_RELATED = create("PR409-OD-0001");
        /**
         * Link already exists.
         */
        public static final PersoniumCoreException CONFLICT_LINKS = create("PR409-OD-0002");
        /**
         * Entity already exists.
         */
        public static final PersoniumCoreException ENTITY_ALREADY_EXISTS = create("PR409-OD-0003");
        /**
         * An entity with the same name already exists when deleting $ links for a compound key entity.
         */
        public static final PersoniumCoreException CONFLICT_UNLINKED_ENTITY = create("PR409-OD-0004");
        /**
         * An entity with the same name already exists when adding $ links to a single key entity.
         */
        public static final PersoniumCoreException CONFLICT_DUPLICATED_ENTITY = create("PR409-OD-0005");
        /**
         * AssociationEnd already has the same relation when registering Link.
         */
        public static final PersoniumCoreException CONFLICT_DUPLICATED_ENTITY_RELATION = create("PR409-OD-0006");
        /**
         * There is no specification of If-Match header.
         */
        public static final PersoniumCoreException HEADER_NOT_EXIST = create("PR412-OD-0001");
        /**
         * Etag of the corresponding Entity does not match.
         */
        public static final PersoniumCoreException ETAG_NOT_MATCH = create("PR412-OD-0002");
        /**
         * .
         */
        public static final PersoniumCoreException CONFLICT_NP = create("PR412-OD-0003");
        /**
         * An unsupported media type was specified.
         */
        public static final PersoniumCoreException UNSUPPORTED_MEDIA_TYPE = create("PR415-OD-0001");
        /**
         * Duplicate property name was detected.
         */
        public static final PersoniumCoreException DUPLICATED_PROPERTY_NAME = create("PR500-OD-0001");
        /**
         * Inconsistency of internal data was detected.
         */
        public static final PersoniumCoreException DETECTED_INTERNAL_DATA_CONFLICT = create("PR500-OD-0002");
    }

    /**
     * WebDAV related.
     * TODO WebDav errors are implemented according to WebDav specifications.
     */
    public static class Dav {

        /**
         * When XML parsing fails.
         */
        public static final PersoniumCoreException XML_ERROR = create("PR400-DV-0001");
        /**
         * When the content of XML is wrong.
         */
        public static final PersoniumCoreException XML_CONTENT_ERROR = create("PR400-DV-0002");
        /**
         * Depth is other than 0, 1, infinity.
         * {0}: Depth header value
         */
        public static final PersoniumCoreException INVALID_DEPTH_HEADER = create("PR400-DV-0003");
        /**
         * When ROLE does not exist.
         */
        public static final PersoniumCoreException ROLE_NOT_FOUND = create("PR400-DV-0004");
        /**
         * When there is no BOX associated with Role.
         * {0}:BOX URL
         */
        public static final PersoniumCoreException BOX_LINKED_BY_ROLE_NOT_FOUND = create("PR400-DV-0005");
        /**
         * When XML validation fails.
         */
        public static final PersoniumCoreException XML_VALIDATE_ERROR = create("PR400-DV-0006");
        /**
         * There are too many child elements of the collection.
         */
        public static final PersoniumCoreException COLLECTION_CHILDRESOURCE_ERROR = create("PR400-DV-0007");
        /**
         * The collection hierarchy is too deep.
         */
        public static final PersoniumCoreException COLLECTION_DEPTH_ERROR = create("PR400-DV-0008");
        /**
         * Invalid value is set in header.
         * {0}: Header key
         * {1}: value of header
         */
        public static final PersoniumCoreException INVALID_REQUEST_HEADER = create("PR400-DV-0009");
        /**
         * When mandatory header is not specified.
         * {0}: Header key
         */
        public static final PersoniumCoreException REQUIRED_REQUEST_HEADER_NOT_EXIST = create("PR400-DV-0010");
        /**
         * __Src is specified as the source resource.
         */
        public static final PersoniumCoreException SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_MOVE =
                create("PR400-DV-0011");
        /**
         * When an existing resource is designated as the resource of the move destination.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_OVERWRITE = create("PR400-DV-0012");
        /**
         * When the path under the OData collection is specified as the destination resource.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_ODATA_COLLECTION =
                create("PR400-DV-0013");
        /**
         * When the path under the file is specified as the destination resource.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_FILE = create("PR400-DV-0014");
        /**
         * Box can not be moved in the MOVE method.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_BOX = create("PR400-DV-0015");
        /**
         * When the path under the Service collection is specified as the destination resource.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_SERVICE_COLLECTION =
                create("PR400-DV-0016");
        /**
         * When __src is specified as the destination resource.
         */
        public static final PersoniumCoreException SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE =
                create("PR400-DV-0017");
        /**
         * The source is a collection and the service source collection is specified as the destination resource.
         */
        public static final PersoniumCoreException SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_CONTAIN_COLLECTION =
                create("PR400-DV-0018");

        /**
         * When the resource does not exist.
         */
        public static final PersoniumCoreException RESOURCE_NOT_FOUND = create("PR404-DV-0001");
        /**
         * When there is no BOX.
         * {0}: BOX name
         */
        public static final PersoniumCoreException BOX_NOT_FOUND = create("PR404-DV-0002");
        /**
         * When CELL does not exist.
         */
        public static final PersoniumCoreException CELL_NOT_FOUND = create("PR404-DV-0003");
        /**
         * When the method is not accepted.
         */
        public static final PersoniumCoreException METHOD_NOT_ALLOWED = create("PR405-DV-0001");
        /**
         * When Depth is infinity.
         */
        public static final PersoniumCoreException PROPFIND_FINITE_DEPTH = create("PR403-DV-0001");
        /**
         * Delete failed if there is a child resource at the time of collection deletion.
         */
        public static final PersoniumCoreException HAS_CHILDREN = create("PR403-DV-0003");
        /**
         * When collection file name is invalid.
         */
        public static final PersoniumCoreException RESOURCE_NAME_INVALID = create("PR403-DV-0004");
        /**
         * When source and destination are the same.
         * {0}: value of Destination header
         */
        public static final PersoniumCoreException DESTINATION_EQUALS_SOURCE_URL = create("PR403-DV-0005");

        /**
         * When the parent resource does not exist at PUT, MKCOL, MOVE of the collection file.
         */
        public static final PersoniumCoreException HAS_NOT_PARENT = create("PR409-DV-0001");
        /**
         * File already exists.
         * <p>
         * {0} : File name
         */
        public static final PersoniumCoreException FILE_ALREADY_EXISTS = create("PR409-DV-0002");
        /**
         * The Etag of the corresponding resource does not match.
         */
        public static final PersoniumCoreException ETAG_NOT_MATCH = create("PR412-DV-0001");
        /**
         * When "F" is specified in the Overwrite header but the resource of the destination already exists.
         */
        public static final PersoniumCoreException DESTINATION_ALREADY_EXISTS = create("PR412-DV-0002");
        /**
         * Range header specification error.
         */
        public static final PersoniumCoreException REQUESTED_RANGE_NOT_SATISFIABLE = create("PR416-DV-0001");
        /**
         * Detects file system inconsistency.
         */
        public static final PersoniumCoreException FS_INCONSISTENCY_FOUND = create("PR500-DV-0001");
        /**
         * When tracing from Box and searching by id, there is inconsistency in Dav data.
         */
        public static final PersoniumCoreException DAV_INCONSISTENCY_FOUND = create("PR500-DV-0002");
        /**
         * When tracing from Box and searching by id, there is inconsistency in Dav data.
         */
        public static final PersoniumCoreException DAV_UNAVAILABLE = create("PR503-DV-0001");
    }

    /**
     * Service collection error.
     */
    public static class ServiceCollection {
        /**
         * Personium-Engine connection failed.
         */
        public static final PersoniumCoreException SC_ENGINE_CONNECTION_ERROR = create("PR500-SC-0001");
        /**
         * Failure to open the file (unused _ as it refers to the implementation content, so do not use it).
         */
        public static final PersoniumCoreException SC_FILE_OPEN_ERROR = create("PR500-SC-0002");
        /**
         * Failed to close the file (unused _ as it refers to the implementation content, so do not use it).
         */
        public static final PersoniumCoreException SC_FILE_CLOSE_ERROR = create("PR500-SC-0003");

        /**
         * Failed to close the file (unused _ as it refers to the implementation content, so do not use it).
         */
        public static final PersoniumCoreException SC_IO_ERROR = create("PR500-SC-0004");
        /**
         * Other errors.
         */
        public static final PersoniumCoreException SC_UNKNOWN_ERROR = create("PR500-SC-0005");
        /**
         * Error when incorrect HTTP response is returned in service invocation.
         */
        public static final PersoniumCoreException SC_INVALID_HTTP_RESPONSE_ERROR = create("PR500-SC-0006");
    }

    /**
     * Error when calling SentMessage receive API.
     */
    public static class SentMessage {
        /**
         * The resource specified for ToRelation does not exist.
         * {0}: specified value
         */
        public static final PersoniumCoreException TO_RELATION_NOT_FOUND_ERROR = create("PR400-SM-0001");
        /**
         * When there is no ExtCell associated with the resource specified for ToRelation.
         * {0}: specified value
         */
        public static final PersoniumCoreException RELATED_EXTCELL_NOT_FOUND_ERROR = create("PR400-SM-0002");
        /**
         * When the destination URL exceeds the maximum transmission permitted number.
         */
        public static final PersoniumCoreException OVER_MAX_SENT_NUM = create("PR400-SM-0003");
        /**
         * When the Box corresponding to the schema can not be found from the schema-authenticated token.
         * {0}:Schema
         */
        public static final PersoniumCoreException BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS = create("PR400-SM-0004");

        /**
         * If the request fails.
         */
        public static final PersoniumCoreException SM_CONNECTION_ERROR = create("PR500-SM-0001");
        /**
         * When the body parsing fails.
         */
        public static final PersoniumCoreException SM_BODY_PARSE_ERROR = create("PR500-SM-0002");
    }

    /**
     * Error when ReceiveMessageAPI call.
     */
    public static class ReceivedMessage {
        /**
         * A relation already exists in relation relation registration of a message.
         */
        public static final PersoniumCoreException REQUEST_RELATION_EXISTS_ERROR = create("PR400-RM-0001");
        /**
         * When a Box corresponding to the schema can not be found.
         * {0}:Schema
         */
        public static final PersoniumCoreException BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS = create("PR400-RM-0002");
        /**
         * When the Box corresponding to the RelationClassURL can not be found.
         * {0}:RelationClassURL
         */
        public static final PersoniumCoreException BOX_THAT_MATCHES_RELATION_CLASS_URL_NOT_EXISTS = create("PR400-RM-0003"); // CHECKSTYLE IGNORE - To maintain readability

// unnecessary.
//        /**
//* Failed to parse the RequestRelation of the message.
//         */
//        public static final PersoniumCoreException REQUEST_RELATION_PARSE_ERROR = create("PR409-RM-0001");

        /**
         * There is no Relation subject to relation deletion.
         */
        public static final PersoniumCoreException REQUEST_RELATION_DOES_NOT_EXISTS = create("PR409-RM-0002");

// unnecessary.
//        /**
//* Failed to parse the RequestRelationTarget of the message.
//         */
//        public static final PersoniumCoreException REQUEST_RELATION_TARGET_PARSE_ERROR = create("PR409-RM-0003");

        /**
         * There is no ExtCell subject to relationship deletion.
         */
        public static final PersoniumCoreException REQUEST_RELATION_TARGET_DOES_NOT_EXISTS = create("PR409-RM-0004");

        /**
         * Link information of RequestRelation and RequestRelationTarget does not exist.
         */
        public static final PersoniumCoreException LINK_DOES_NOT_EXISTS = create("PR409-RM-0005");
    }

    /**
     * Server internal error.
     * Throw when a process can not be continued due to a server side failure or bug, which is to indicate the cause of the problem. Basically, when exceptions occur in category of WARN or more log output
     */
    public static class Server {
        /**
         * Unknown error.
         */
        public static final PersoniumCoreException UNKNOWN_ERROR = create("PR500-SV-0000");
        /**
         * When connection to the data store fails.
         */
        public static final PersoniumCoreException DATA_STORE_CONNECTION_ERROR = create("PR500-SV-0001");
        /**
         * Unknown data store related error.
         */
        public static final PersoniumCoreException DATA_STORE_UNKNOWN_ERROR = create("PR500-SV-0002");
        /**
         * When retrying over with a request to ES.
         */
        public static final PersoniumCoreException ES_RETRY_OVER = create("PR500-SV-0003");
        /**
         * When an error occurs in the file system.
         */
        public static final PersoniumCoreException FILE_SYSTEM_ERROR = create("PR500-SV-0004");
        /**
         * Data store search failed.
         */
        public static final PersoniumCoreException DATA_STORE_SEARCH_ERROR = create("PR500-SV-0005");
        /**
         * Data store update failed, rollback failed as well.
         */
        public static final PersoniumCoreException DATA_STORE_UPDATE_ROLLBACK_ERROR = create("PR500-SV-0006");
        /**
         * Data store update failed, rollback succeeded.
         */
        public static final PersoniumCoreException DATA_STORE_UPDATE_ERROR_ROLLBACKED = create("PR500-SV-0007");

        /**
         * When connection to memcached fails.
         */
        public static final PersoniumCoreException SERVER_CONNECTION_ERROR = create("PR503-SV-0002");
        /**
         * Memcached failed to acquire lock status.
         */
        public static final PersoniumCoreException GET_LOCK_STATE_ERROR = create("PR503-SV-0003");
        /**
         * When restoring data per unit user.
         */
        public static final PersoniumCoreException SERVICE_MENTENANCE_RESTORE = create("PR503-SV-0004");
        /**
         * ReadDeleteOnly mode state.
         */
        public static final PersoniumCoreException READ_DELETE_ONLY = create("PR503-SV-0005");
        /**
         * Failed to connect to Ads.
         */
        public static final PersoniumCoreException ADS_CONNECTION_ERROR = create("PR503-SV-0006");
    }

    /**
     * NetWork related error.
     */
    public static class NetWork {
        /**
         * NetWork related error.
         */
        public static final PersoniumCoreException NETWORK_ERROR = create("PR500-NW-0000");
        /**
         * HTTP request failed.
         */
        public static final PersoniumCoreException HTTP_REQUEST_FAILED = create("PR500-NW-0001");
        /**
         * The connection destination returns an unexpected response.
         */
        public static final PersoniumCoreException UNEXPECTED_RESPONSE = create("PR500-NW-0002");
        /**
         * The connection destination returns an unexpected value.
         */
        public static final PersoniumCoreException UNEXPECTED_VALUE = create("PR500-NW-0003");
    }

    /**
     * Authentication error.
     */
    public static class Auth {
        /**
         * Invalid password string.
         */
        public static final PersoniumCoreException PASSWORD_INVALID = create("PR400-AU-0001");
        /**
         * Request parameter is invalid.
         */
        public static final PersoniumCoreException REQUEST_PARAM_INVALID = create("PR400-AU-0002");
        /**
         * Invalid password string.
         */
        public static final PersoniumCoreException P_CREDENTIAL_REQUIRED = create("PR400-AU-0003");

        /**
         * It is not unit user access.
         */
        public static final PersoniumCoreException UNITUSER_ACCESS_REQUIRED = create("PR403-AU-0001");
        /**
         * There is no necessary authority.
         */
        public static final PersoniumCoreException NECESSARY_PRIVILEGE_LACKING = create("PR403-AU-0002");
        /**
         * It can not be accessed by the unit user specified in the authentication header.
         */
        public static final PersoniumCoreException NOT_YOURS = create("PR403-AU-0003");
        /**
         * Schema authentication is required.
         */
        public static final PersoniumCoreException SCHEMA_AUTH_REQUIRED = create("PR403-AU-0004");
        /**
         * It can not be accessed with this schema authentication.
         */
        public static final PersoniumCoreException SCHEMA_MISMATCH = create("PR403-AU-0005");
        /**
         * Schema authentication level is insufficient.
         */
        public static final PersoniumCoreException INSUFFICIENT_SCHEMA_AUTHZ_LEVEL = create("PR403-AU-0006");
        /**
         * Scope is insufficient.
         */
        public static final PersoniumCoreException INSUFFICIENT_SCOPE = create("PR403-AU-0007");

        /**
         * Error setting root CA certificate.
         */
        public static final PersoniumCoreException ROOT_CA_CRT_SETTING_ERROR = create("PR500-AN-0001");
        /**
         * Request parameter is invalid.
         */
        public static final PersoniumCoreException REQUEST_PARAM_CLIENTID_INVALID = create("PR400-AZ-0002");
        /**
         * Request parameter is invalid.
         */
        public static final PersoniumCoreException REQUEST_PARAM_REDIRECT_INVALID = create("PR400-AZ-0003");
        /**
         * When JSON parsing fails.
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-AZ-0005");
        /**
         * When JSON Encode fails.
         */
        public static final PersoniumCoreException IDTOKEN_ENCODED_INVALID = create("PR400-AZ-0006");
        /**
         * Error when "Box whose schema is the cell url specified by clientId" does not exist.
         * Because there is a possibility that it may be used illegally for confirming the existence of Box, the message is "Authorization failed".
         */
        public static final PersoniumCoreException BOX_NOT_INSTALLED = create("PR400-AZ-0007");

    }

    /**
     * Event related error.
     */
    public static class Event {
        /**
         * Failure in JSON Perth.
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-EV-0001");
        /**
         * The value of X-Personium-RequestKey is invalid.
         */
        public static final PersoniumCoreException X_PERSONIUM_REQUESTKEY_INVALID = create("PR400-EV-0002");
        /**
         * There is no essential item of the request body.
         * {0}: Property name
         */
        public static final PersoniumCoreException INPUT_REQUIRED_FIELD_MISSING = create("PR400-EV-0003");
        /**
         * The request data format is invalid.
         * {0} Property name
         */
        public static final PersoniumCoreException REQUEST_FIELD_FORMAT_ERROR = create("PR400-EV-0004");
        /**
         * When the current event log file can not be deleted.
         */
        public static final PersoniumCoreException CURRENT_FILE_CANNOT_DELETE = create("PR400-EV-0005");
        /**
         * Http response output failed.
         */
        public static final PersoniumCoreException EVENT_RESPONSE_FAILED = create("PR500-EV-0001");
        /**
         * When the compressed event log file can not be opened.
         */
        public static final PersoniumCoreException ARCHIVE_FILE_CANNOT_OPEN = create("PR500-EV-0002");
        /**
         * File delete failed.
         */
        public static final PersoniumCoreException FILE_DELETE_FAILED = create("PR500-EV-0003");

    }

    /**
     * bar File installation related error.
     */
    public static class BarInstall {
        /**
         * When the value of the request header is invalid.
         */
        public static final PersoniumCoreException REQUEST_HEADER_FORMAT_ERROR = create("PR400-BI-0001");
        /**
         * When the file size of the Bar file exceeds the upper limit value.
         */
        public static final PersoniumCoreException BAR_FILE_SIZE_TOO_LARGE = create("PR400-BI-0002");
        /**
         * When the file size of the entry in the Bar file exceeds the upper limit value.
         */
        public static final PersoniumCoreException BAR_FILE_ENTRY_SIZE_TOO_LARGE = create("PR400-BI-0003");
        /**
         * When the Box to be installed has been registered as Box Schema.
         */
        public static final PersoniumCoreException BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS = create("PR400-BI-0004");
        /**
         * When the file size of the Bar file exceeds the upper limit value.
         */
        public static final PersoniumCoreException BAR_FILE_SIZE_INVALID = create("PR400-BI-0005");
        /**
         * When JSON file format is invalid.
         */
        public static final PersoniumCoreException JSON_FILE_FORMAT_ERROR = create("PR400-BI-0006");
        /**
         * bar When the file can not be opened.
         */
        public static final PersoniumCoreException BAR_FILE_CANNOT_OPEN = create("PR400-BI-0007");
        /**
         * When the bar file can not be read.
         */
        public static final PersoniumCoreException BAR_FILE_CANNOT_READ = create("PR400-BI-0008");
        /**
         * When the structure of the bar file is incorrect.
         */
        public static final PersoniumCoreException BAR_FILE_INVALID_STRUCTURES = create("PR400-BI-0009");
        /**
         * Bar file structure and bar_version do not match.
         */
        public static final PersoniumCoreException BAR_FILE_STRUCTURE_AND_VERSION_MISMATCH = create("PR400-BI-0010");
        /**
         * When the Box to be installed is registered.
         */
        public static final PersoniumCoreException BAR_FILE_BOX_ALREADY_EXISTS = create("PR405-BI-0001");
        /**
         * Http response output failed.
         */
        public static final PersoniumCoreException BAR_FILE_RESPONSE_FAILED = create("PR500-BI-0001");
    }

    /**
     * UI.
     */
    public static class UI {
        /**
         * The corresponding property does not exist.<p>
         * Property [{0}] not configured.
         */
        public static final PersoniumCoreException NOT_CONFIGURED_PROPERTY = create("PR412-UI-0001");
        /**
         * The property being set is not http(s).<p>
         * Property settings error. [{0}] should be normalized URL with http(s), personium-localunit and personium-localcell scheme. // CHECKSTYLE IGNORE - To maintain readability
         */
        public static final PersoniumCoreException PROPERTY_NOT_URL = create("PR412-UI-0002");
        /**
         * Property settings error. [{0}] // CHECKSTYLE IGNORE - To maintain readability
         */
        public static final PersoniumCoreException PROPERTY_SETTINGS_ERROR = create("PR412-UI-0003");
        /**
         * Invalid HTTP response was returned.<p>
         * Invalid HTTP response was returned from {0}.
         */
        public static final PersoniumCoreException INVALID_HTTP_RESPONSE = create("PR500-UI-0001");
        /**
         * Connection failed.<p>
         * Could not connect to {0}.
         */
        public static final PersoniumCoreException CONNECTION_FAILED = create("PR500-UI-0002");
    }

    /**
     * Other error.
     */
    public static class Misc {
        /**
         * File or Directory does not exist in the snapshot file.
         * <p>
         * {0} : File or Directory path in zip
         */
        public static final PersoniumCoreException NOT_FOUND_IN_SNAPSHOT = create("PR400-MC-0001");
        /**
         * Path based CellUrl access is not allowed.
         */
        public static final PersoniumCoreException PATH_BASED_ACCESS_NOT_ALLOWED = create("PR400-MC-0002");
        /**
         * Version of the snapshot file is invalid.
         * <p>
         * {0} : Expected version
         * {1} : Actual version
         */
        public static final PersoniumCoreException SNAPSHOT_VERSION_INVALID = create("PR400-MC-0003");
        /**
         * The specified snapshot file is not zip file.
         */
        public static final PersoniumCoreException SNAPSHOT_IS_NOT_ZIP = create("PR400-MC-0004");
        /**
         * Unexpected URI.
         */
        public static final PersoniumCoreException NOT_FOUND = create("PR404-MC-0001");
        /**
         * When the method is not accepted.
         */
        public static final PersoniumCoreException METHOD_NOT_ALLOWED = create("PR405-MC-0001");
        /**
         * Canceled while processing in the server.
         * Used with $ batch timeout.
         */
        public static final PersoniumCoreException SERVER_REQUEST_TIMEOUT = create("PR408-MC-0001");
        /**
         * There is other access to the target cell when processing to the cell.
         */
        public static final PersoniumCoreException CONFLICT_CELLACCESS = create("PR409-MC-0001");
        /**
         * TODO Provisional.
         * Provisional check error of cell import.
         */
        public static final PersoniumCoreException EXPORT_CELL_EXISTS = create("PR409-MC-0002");
        /**
         * When the prerequisite specification of the header is not satisfied.
         */
        public static final PersoniumCoreException PRECONDITION_FAILED = create("PR412-MC-0001");
        /**
         * Unsupported media type.
         * No params.
         */
        public static final PersoniumCoreException UNSUPPORTED_MEDIA_TYPE_NO_PARAMS = create("PR415-MC-0001");

        /**
         * When the method is not implemented yet.
         */
        public static final PersoniumCoreException METHOD_NOT_IMPLEMENTED = create("PR501-MC-0001");
        /**
         * Unimplemented function.
         */
        public static final PersoniumCoreException NOT_IMPLEMENTED = create("PR501-MC-0002");
        /**
         * When there are too many concurrent requests.
         * Used with exclusive control timeout.
         */
        public static final PersoniumCoreException TOO_MANY_CONCURRENT_REQUESTS = create("PR503-SV-0001");

    }

    /**
     * Common.
     */
    public static class Common {
        /**
         * Required key missing.
         * <p>
         * {0} : Required key
         */
        public static final PersoniumCoreException REQUEST_BODY_REQUIRED_KEY_MISSING = create("PR400-CM-0001");
        /**
         * Field format error.
         * <p>
         * {0} : Field name
         * {1} : Format
         */
        public static final PersoniumCoreException REQUEST_BODY_FIELD_FORMAT_ERROR = create("PR400-CM-0002");
        /**
         * Unknown key specified.
         * <p>
         * {0} : Unknown key
         */
        public static final PersoniumCoreException REQUEST_BODY_UNKNOWN_KEY_SPECIFIED = create("PR400-CM-0003");
        /**
         * JSON parse error.
         * <p>
         * {0} : Parse string
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-CM-0004");
        /**
         * Invalid URL authority.
         * <p>
         * {0} : Given authority
         * {1} : Configured authority
         */
        public static final PersoniumCoreException INVALID_URL_AUTHORITY = create("PR400-CM-0005");
        /**
         * Requested media type is not acceptable.
         * <p>
         * {0} : Given media type
         */
        public static final PersoniumCoreException MEDIATYPE_NOT_ACCEPTABLE = create("PR406-CM-0001");
        /**
         * Executing API that is not allowed when the cell status is "import failed".
         */
        public static final PersoniumCoreException CELL_STATUS_IMPORT_FAILED = create("PR409-CM-0001");
        /**
         * Error when writing to cell is locked.
         * <p>
         * {0} : Processing that caused lock.
         */
        public static final PersoniumCoreException LOCK_WRITING_TO_CELL = create("PR409-CM-0002");
        /**
         * Failed to load the request body.
         */
        public static final PersoniumCoreException REQUEST_BODY_LOAD_FAILED = create("PR500-CM-0001");
        /**
         * File I/O error.
         * <p>
         * {0} : Overview of failed processing
         */
        public static final PersoniumCoreException FILE_IO_ERROR = create("PR500-CM-0002");

        /**
         * Unchecked Invalid URL used internally.
         * <p>
         * {0} : URL
         */
        public static final PersoniumCoreException INVALID_URL = create("PR500-CM-0003");
    }

    /**
     * Plugin error.
     */
    public static class Plugin {
        /**
         * It corresponds to the non-checked exception which came out outside without being caught inside the plug-in.
         */
        public static final PersoniumCoreException UNEXPECTED_ERROR = create("PR500-PL-0001");

    }

    String code;
    Severity severity;
    String message;
    int status;

    /**
     * Force load inner class.
     * Add an inner class of error classification here if it is added.
     */
    public static void loadConfig() {
        new OData();
        new Dav();
        new ServiceCollection();
        new Server();
        new Auth();
        new Event();
        new Misc();
        new Plugin();
    }

    /**
     * constructor.
     * @param status HTTP response status
     * @param severity error level
     * @param code error code
     * @param message error message
     */
    PersoniumCoreException(final String code,
            final Severity severity,
            final String message,
            final int status,
            final Throwable t) {
        super(t);
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.status = status;
    }

    /**
     * constructor.
     * @param code error code
     * @param severity error level
     * @param message error message
     * @param status HTTP response status
     */
    PersoniumCoreException(final String code,
            final Severity severity,
            final String message,
            final int status) {
        this(code, severity, message, status, null);
    }

    /**
     * Create a response object.
     * @return JAX-RS response object
     */
    public Response createResponse() {
        //When TODO error, specify JSON as fixed, but specify Content-Type when canceling restriction! !
        return Response.status(status)
                .entity(new ODataErrorMessage(code, message))
                .type(MediaType.valueOf(MediaType.APPLICATION_JSON))
                .build();
    }

    /**
     * Return the log level.
     * @return log level
     */
    public Severity getSeverity() {
        return this.severity;
    }

    /**
     * Return the HTTP status code.
     * @return HTTP status code
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * Return error code.
     * @return error code
     */
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    /**
     * Cause Create and return an exception added.
     * @param t cause exception
     * @return PersoniumCoreException
     */
    public PersoniumCoreException reason(final Throwable t) {
        //Make a clone
        PersoniumCoreException ret = new PersoniumCoreException(this.code, this.severity, this.message, this.status, t);
        return ret;
    }

    /**
     * It creates and returns a message with parameter substitution, and the expression such as $ 1 $ 2 on the error message is a keyword for parameter substitution.
     * @param params Additional message
     * @return PersoniumCoreMessage
     */
    public PersoniumCoreException params(final Object... params) {
        //Replacement message creation
        String ms = MessageFormat.format(this.message, params);

        //Escape processing of control code
        ms = EscapeControlCode.escape(ms);

        //Create a message replacement clone
        PersoniumCoreException ret = new PersoniumCoreException(this.code, this.severity, ms, this.status);
        return ret;
    }

    /**
     * Factory method.
     * @param code Message code
     * @return PersoniumCoreException
     */
    public static PersoniumCoreException create(String code) {
        int statusCode = parseCode(code);

        //Acquire log level
        Severity severity = PersoniumCoreMessageUtils.getSeverity(code);
        if (severity == null) {
            //If the log level is not set, it is automatically judged from the response code.
            severity = decideSeverity(statusCode);
        }

        //Obtaining log messages
        String message = PersoniumCoreMessageUtils.getMessage(code);

        return new PersoniumCoreException(code, severity, message, statusCode);
    }

    /**
     * Determination of log level from response code.
     * @param statusCode Status code
     * @return Log level determined from status code
     */
    static Severity decideSeverity(int statusCode) {
        //If setting is omitted, obtain log level from error code
        if (statusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            //In the case of 500 series, warning (500 or more are collectively warnings)
            return Severity.WARN;
        } else if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
            //Info for 400 series
            return Severity.INFO;
        } else {
            //In other cases it is impossible to think about warning.
            //When processing 200 series or 300 series with Personium Core Exception let's write the log level setting properly.
            return Severity.WARN;
        }
    }

    /**
     * The parsing of the message code.
     * @param code Message code
     * @return -1 for status codes or log messages.
     */
    static int parseCode(String code) {
        Pattern p = Pattern.compile("^PR(\\d{3})-\\w{2}-\\d{4}$");
        Matcher m = p.matcher(code);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "message code should be in \"PR000-OD-0000\" format. code=[" + code + "].");
        }
        return Integer.parseInt(m.group(1));
    }
    static Logger defaultLogger = LoggerFactory.getLogger(PersoniumCoreException.class);
    /*
     * Handling of PersoniumCoreException.
     */
    public void log(final Logger log) {
        Logger l = log;
        if (l == null) {
            l = defaultLogger;
        }
        Severity sv = this.getSeverity();
        String code = this.getCode();
        String message = String.format("[%s] - %s", code, this.getMessage());
        Throwable cause = this.getCause();
        //Log output
        switch (sv) {
        case INFO:
            if (l.isDebugEnabled()) {
                l.debug(message, this);
            } else {
                l.info(message);         // Info-level do not print stack trace
                logCauseChain(l, cause); // instead log the causing exception chain in a simple format.
            }
            break;
        case WARN:
            l.warn(message, this);
            break;
        case ERROR:
            l.error(message, this);
            break;
        default:
            l.error("Exception Severity Not Defined");
            l.error(message, this);
        }
    }
    private static void logCauseChain(final Logger log, Throwable cause) {
        if (cause == null) {
            return;
        }
        log.info("   reason = " + cause.getMessage() + " (" + cause.getClass().getCanonicalName() + ")");
        if (cause.getCause() != null) {
            logCauseChain(log, cause.getCause());
        }
    }
}

