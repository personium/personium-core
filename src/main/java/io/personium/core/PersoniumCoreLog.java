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
package io.personium.core;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.plugin.base.PluginMessageUtils.Severity;

/**
 * Log message creation class.
 */
public final class PersoniumCoreLog {

    static Logger log = LoggerFactory.getLogger(PersoniumCoreLog.class);

    /**
     * OData related.
     */
    public static class OData {
        /**
         * More than 2 pieces of data came back at places where only one case should be obtained by searching with the primary key.
         * {0}: Number of hits
         */
        public static final PersoniumCoreLog FOUND_MULTIPLE_RECORDS = create("PL-OD-0001");
        /**
         * Abnormal in bulk data registration.
         */
        public static final PersoniumCoreLog BULK_INSERT_FAIL = create("PL-OD-0002");
        /**
         * Duplicate property name was detected.
         */
        public static final PersoniumCoreLog DUPLICATED_PROPERTY_NAME = create("PL-OD-0003");
    }

    /**
     * WebDAV related.
     */
    public static class Dav {
        /**
         * When Role was not found.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog ROLE_NOT_FOUND = create("PL-DV-0001");
        /**
         * Range header specification error.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog REQUESTED_RANGE_NOT_SATISFIABLE = create("PL-DV-0002");
        /**
         * Failed skipping of file specified by Range header.
         */
        public static final PersoniumCoreLog FILE_TOO_SHORT = create("PL-DV-0003");
        /**
         * Failure to delete binary data.
         * {0}: UUID of binary data
         */
        public static final PersoniumCoreLog FILE_DELETE_FAIL = create("PL-DV-0004");
    }

    /**
     * Authentication related.
     */
    public static class Auth {
        /**
         * When failing to parse the token.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog TOKEN_PARSE_ERROR = create("PL-AU-0001");
        /**
         * Signature validation error on token.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog TOKEN_DISG_ERROR = create("PL-AU-0002");
        /**
         * Root CA certificate configuration error.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog ROOT_CA_CRT_SETTING_ERROR = create("PL-AU-0003");
        /**
         * Account does not exist when updating last login time.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog ACCOUNT_ALREADY_DELETED = create("PL-AU-0004");
        /**
         * Corresponding account existed, but it does not correspond to the specified GrantType.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog UNSUPPORTED_ACCOUNT_GRANT_TYPE = create("PL-AU-0005");
        /**
         * Authentication failed. No such account.
         * {0}: URL
         * {1}: IP address
         * {2}: User ID(username)
         */
        public static final PersoniumCoreLog AUTHN_FAILED_NO_SUCH_ACCOUNT = create("PL-AU-0006");
        /**
         * Authentication failed. Consequtive authentication trial before valid authentication interval.
         * {0}: URL
         * {1}: IP address
         * {2}: User ID(username)
         */
        public static final PersoniumCoreLog AUTHN_FAILED_BEFORE_AUTHENTICATION_INTERVAL = create("PL-AU-0007");
        /**
         * Authentication failed. Account is locked.
         * {0}: URL
         * {1}: IP address
         * {2}: User ID(username)
         */
        public static final PersoniumCoreLog AUTHN_FAILED_ACCOUNT_IS_LOCKED = create("PL-AU-0008");
        /**
         * Authentication failed. Incorrect password.
         * {0}: URL
         * {1}: IP address
         * {2}: User ID(username)
         */
        public static final PersoniumCoreLog AUTHN_FAILED_INCORRECT_PASSWORD = create("PL-AU-0009");
        /**
         * Authentication failed. Incorrect IP address.
         * {0}: URL
         * {1}: IP address
         * {2}: User ID(username)
         */
        public static final PersoniumCoreLog AUTHN_FAILED_INCORRECT_IP_ADDRESS = create("PL-AU-0010");
    }

    /**
     * OIDC authentication related.
     */
    public static class OIDC {
        /**
         * The corresponding account does not exist.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog NO_SUCH_ACCOUNT = create("PL-OI-0001");
        /**
         * Corresponding account existed, but it does not correspond to the specified GrantType.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog UNSUPPORTED_ACCOUNT_GRANT_TYPE = create("PL-OI-0002");
        /**
         * Request user and IDToken user do not match.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog INVALID_ACCOUNT = create("PL-OI-0003");
        /** .
         * Issuer is not Google
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog INVALID_ISSUER = create("PL-OI-0004");
    }

    /**
     * Server internal error.
     * Throw when a process can not be continued due to a server side failure or bug, which is to indicate the cause of the problem. Basically, when exceptions occur in category of WARN or more log output
     */
    public static class Server {
        /**
         * Failed to create master data for Ads.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog DATA_STORE_ENTITY_CREATE_FAIL = create("PL-SV-0001");
        /**
         * Failed to update master data to Ads.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog DATA_STORE_ENTITY_UPDATE_FAIL = create("PL-SV-0002");
        /**
         * Failed to delete master data to Ads.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog DATA_STORE_ENTITY_DELETE_FAIL = create("PL-SV-0003");
        /**
         * Memcached port number format error.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog MEMCACHED_PORT_FORMAT_ERROR = create("PL-SV-0004");
        /**
         * Failed to connect to memcached.
         * {0}: host name
         * {1}: Port name
         * {2}: Detailed message
         */
        public static final PersoniumCoreLog MEMCACHED_CONNECTO_FAIL = create("PL-SV-0005");
        /**
         * Cache setting to memcached failed.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog MEMCACHED_SET_FAIL = create("PL-SV-0006");
        /**
         * Clear cache to memcached failed.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog MEMCACHED_CLEAR_FAIL = create("PL-SV-0007");
        /**
         * Failed to delete memcached cache.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog MEMCACHED_DELETE_FAIL = create("PL-SV-0008");
        /**
         * Failed to create master data in Bulk to Ads.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog DATA_STORE_ENTITY_BULK_CREATE_FAIL = create("PL-SV-0009");
        /**
         * Failed to connect to RDB.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog RDB_CONNECT_FAIL = create("PL-SV-0010");
        /**
         * SQL execution error.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog EXECUTE_QUERY_SQL_FAIL = create("PL-SV-0011");
        /**
         * Failure to disconnect with RDB.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog RDB_DISCONNECT_FAIL = create("PL-SV-0012");
        /**
         * Connection to ADS failed.
         * {0}: Detailed message
         */
        public static final PersoniumCoreLog ADS_CONNECTION_ERROR = create("PL-SV-0013");
        /**
         * ElasticSearch's index does not exist.
         * {0}: index name
         */
        public static final PersoniumCoreLog ES_INDEX_NOT_EXIST = create("PL-SV-0014");
        /**
         * Failed to create Ads.
         * {0}: index name
         */
        public static final PersoniumCoreLog FAILED_TO_CREATE_ADS = create("PL-SV-0015");
        /**
         * Output the SQL statement executed to JDBC to the log.
         * {0}: Executed SQL statement
         */
        public static final PersoniumCoreLog JDBC_EXEC_SQL = create("PL-SV-0016");
        /**
         * Server startup failure.
         */
        public static final PersoniumCoreLog FAILED_TO_START_SERVER = create("PL-SV-0017");
        /**
         * Log the SQL execution of the user OData executed against JDBC.
         * {0}: DB name
         * {1}: Table name
         * {2}:id
         * {3}: Type
         * {4}: Cell id
         * {5}: Box id
         * {6}: Node id
         * {7}: id of EntityType
         */
        public static final PersoniumCoreLog JDBC_USER_ODATA_SQL = create("PL-SV-0018");
        /**
         * I set the ReferenceOnly lock.
         * {0}: Key name
         */
        public static final PersoniumCoreLog SET_REFERENCE_ONLY_LOCK = create("PL-SV-0019");
        /**
         * Failed to write repair log at Ads error.
         */
        public static final PersoniumCoreLog WRITE_ADS_FAILURE_LOG_ERROR = create("PL-SV-0020");
        /**
         * Information to write to the repair log at the time of Ads error.
         * {0}: log information
         */
        public static final PersoniumCoreLog WRITE_ADS_FAILURE_LOG_INFO = create("PL-SV-0021");
    }

    /**
     * ElasticSearch.
     */
    public static class Es {
        /**
         * Connection to ES has been completed.
         * {0}: connection node address
         */
        public static final PersoniumCoreLog CONNECTED = create("PL-ES-0001");
        /**
         * Request to ES has been completed.
         * {0}: index name
         * {1}: Type name
         * {2}: Node name
         * {3}: Request query
         * {4}: Request type to ES
         * The output order of {3} and {4} is reversed
         */
        public static final PersoniumCoreLog AFTER_REQUEST = create("PL-ES-0002");
        /**
         * Create an index.
         * {0}: index name
         */
        public static final PersoniumCoreLog CREATING_INDEX = create("PL-ES-0003");
        /**
         * The registration request to ES has been completed.
         * {0}: index name
         * {1}: Type name
         * {2}: Node name
         * {3}: Request type to ES
         * {4}: Request query
         */
        public static final PersoniumCoreLog AFTER_CREATE = create("PL-ES-0004");
        /**
         * The registration request to ES has been completed.
         * {0}: Request query
         */
        public static final PersoniumCoreLog AFTER_CREATE_BODY = create("PL-ES-0005");
    }

    /**
     * Other error.
     */
    public static class Misc {
        /**
         * Unknown error such as source which can not be reached.
         */
        public static final PersoniumCoreLog UNREACHABLE_CODE_ERROR = create("PL-MC-0001");
    }

    String message;
    String code;
    Severity severity;
    Throwable reason;

    /**
     * Force load inner class.
     * Add an inner class of error classification here if it is added.
     */
    public static void loadConfig() {
        new OData();
        new Server();
        new Dav();
        new Misc();
    }

    /**
     * constructor.
     * @param severity error level
     * @param message error message
     */
    PersoniumCoreLog(final String code,
            final Severity severity,
            final String message) {
        this.code = code;
        this.severity = severity;
        this.message = message;
    }

    /**
     * Factory method.
     * @param code error code
     * @return PersoniumCoreLog
     */
    public static PersoniumCoreLog create(String code) {
        //Acquire log level
        Severity severity = PersoniumCoreMessageUtils.getSeverity(code);
        if (severity == null) {
            //If it is omitted in the setting file, it is treated as a warning.
            severity = Severity.WARN;
        }

        //Obtaining log messages
        String message = PersoniumCoreMessageUtils.getMessage(code);

        return new PersoniumCoreLog(code, severity, message);
    }

    /**
     * Return error code.
     * @return error code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * It creates and returns a message with a parameter substitution, and the expression of {1} {2} etc. on the error message is a keyword for parameter substitution.
     * @param params Additional message
     * @return PersoniumCoreLog
     */
    public PersoniumCoreLog params(final Object... params) {
        //Replacement message creation
        String ms = MessageFormat.format(this.message, params);
        //Create a message replacement clone
        PersoniumCoreLog ret = new PersoniumCoreLog(this.code, this.severity, ms);
        return ret;
    }

    /**
     * Cause Create and return an exception added.
     * @param t cause exception
     * @return PersoniumCoreException
     */
    public PersoniumCoreLog reason(final Throwable t) {
        //Make a clone
        PersoniumCoreLog ret = new PersoniumCoreLog(this.code, this.severity, this.message);
        //Set cause Exception
        ret.reason = t;
        return ret;
    }

    /**
     * Log output.
     * When outputting the log, display the class name, method name, and the number of lines of the log output source.
     * Output example)
     * 2012-09-09 11:23:47.029 [main] [INFO ] CoreLog [io.personium.core.CoreLogTest#test:22] - JSON Parse Error.
     */
    public void writeLog() {

        StackTraceElement[] ste = new Throwable().getStackTrace();
        String logInfo = String.format("[%s] - [%s#%s:%s] - %s",
                this.code, ste[1].getClassName(), ste[1].getMethodName(), ste[1].getLineNumber(), this.message);
        switch (this.severity) {
        case INFO:
            log.info(logInfo, this.reason);
            break;
        case WARN:
            log.warn(logInfo, this.reason);
            break;
        case ERROR:
            log.error(logInfo, this.reason);
            break;
        case DEBUG:
            log.debug(logInfo, this.reason);
            break;
        default:
            log.error("Message Severity Not Defined");
        }
    }
}
