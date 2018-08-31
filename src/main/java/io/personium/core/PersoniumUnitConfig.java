/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.utils.UriUtils;

/**
 *A class that holds configuration information, from which you can access the contents of personium-unit-config.properties on the classpath.
 */
public class PersoniumUnitConfig {
    private static final int DEFAULT_BATCH_TIMEOUT = 270000;
    private static final int DEFAULT_BATCH_SLEEP_INTERVAL = 1000;
    private static final int DEFAULT_BATCH_SLEEP = 50;

    /** personium-unit-config.properties setting file passkey.*/
    static final String KEY_CONFIG_FILE = "io.personium.configurationFile";

    /** Prefix of the property key used in this application.*/
    static final String KEY_ROOT = "io.personium.core.";

    /** Key for setting the Core version.*/
    public static final String CORE_VERSION = KEY_ROOT + "version";

    /** Thread pool num io cell key. */
    public static final String THREAD_POOL_NUM_IO_CELL = KEY_ROOT + "thread.pool.num.io.cell";
    /** Thread pool num io box key. */
    public static final String THREAD_POOL_NUM_IO_BOX = KEY_ROOT + "thread.pool.num.io.box";
    /** Thread pool num misc key. */
    public static final String THREAD_POOL_NUM_MISC = KEY_ROOT + "thread.pool.num.misc";

    /** Key of master token setting.*/
    public static final String MASTER_TOKEN = KEY_ROOT + "masterToken";

    /** Unit User name to be certified as a user token issuer.*/
    public static final String UNIT_USER_ISSUERS = KEY_ROOT + "unitUser.issuers";

    /** Scheme setting key for unit.*/
    public static final String UNIT_SCHEME = KEY_ROOT + "unitScheme";

    /** Port number key for UnitUrl. */
    public static final String UNIT_PORT = KEY_ROOT + "unitPort";

    /** Path key for UnitUrl. */
    public static final String UNIT_PATH = KEY_ROOT + "unitPath";

    /** Plugin path setting key.*/
    public static final String PLUGIN_PATH = KEY_ROOT + "plugin.path";

    /**
     * Cell configurations.
     */
    public static final class Cell {
        /** Default value of relayhtmlurl. */
        public static final String RELAYHTMLURL_DEFAULT = KEY_ROOT + "cell.relayhtmlurl.default";
        /** Default value of authorizationhtmlurl. */
        public static final String AUTHORIZATIONHTMLURL_DEFAULT = KEY_ROOT + "cell.authorizationhtmlurl.default";
    }

    /**
     *Setting around OData.
     */
    public static final class OData {
        /** Maximum number of requests when doing $ batch processing.*/
        public static final String BATCH_BULK_REQUEST_MAX_SIZE = KEY_ROOT + "odata.batch.bulkRequestMaxSize";

        /** Timeout time for $ batch processing.*/
        public static final String BATCH_REQUEST_TIMEOUT_IN_MILLIS = KEY_ROOT + "odata.batch.timeoutInMillis";

        /** Sleep time for $ batch processing.*/
        public static final String BATCH_SLEEP_IN_MILLIS = KEY_ROOT + "odata.batch.sleepInMillis";

        /** Sleep interval of $ batch processing.*/
        public static final String BATCH_SLEEP_INTERVAL_IN_MILLIS = KEY_ROOT + "odata.batch.sleepIntervalInMillis";

        /** N: The maximum number of N links that $ links can create.*/
        public static final String NN_LINKS_MAX_NUM = KEY_ROOT + "odata.links.NtoN.maxnum";

        /** $ top Maximum number when specifying $ expand.*/
        public static final String EXPAND_TOP_MAXNUM = KEY_ROOT + "odata.query.expand.top.maxnum";

        /** Maximum expanded number of $ expand (when list is acquired).*/
        public static final String EXPAND_LIST_MAXNUM = KEY_ROOT + "odata.expand.list.maxnum";

        /** Maximum expanded number of $ expand (when acquiring one case).*/
        public static final String EXPAND_RETRIEVE_MAXNUM = KEY_ROOT + "odata.expand.retrieve.maxnum";

        /** Maximum number of $ top.*/
        public static final String TOP_MAX_NUM = KEY_ROOT + "odata.query.top.maxnum";

        /** Maximum number of $ skip.*/
        public static final String SKIP_MAX_NUM = KEY_ROOT + "odata.query.skip.maxnum";

        /** Number of default returns at the time of list acquisition.*/
        public static final String TOP_DEFAULT = KEY_ROOT + "odata.query.top.default";

        /** Maximum number of properties of $ expand (when list is acquired).*/
        public static final String EXPAND_PROPERTY_MAX_NUM_LIST = KEY_ROOT + "odata.query.expand.property.maxnum.list";

        /** Maximum number of properties of $ expand (when acquiring one case).*/
        public static final String EXPAND_PROPERTY_MAX_NUM_RETRIEVE = KEY_ROOT
                + "odata.query.expand.property.maxnum.retrieve";
    }

    /**
     *Setting around Account.
     */
    public static final class Account {
        /** Whether or not to update the last login time of Account at the time of successful password authentication (true: Update (default) false: Do not update).*/
        public static final String ACCOUNT_LAST_AUTHENTICATED_ENABLED = KEY_ROOT + "account.lastauthenticated.enabled";
    }

    /**
     *Setting around Dav.
     */
    public static final class Dav {
        /** Maximum number of child elements of the collection.*/
        public static final String COLLECTION_CHILDRESOURCE_MAX_NUM = KEY_ROOT + "dav.childresource.maxnum";

        /** The maximum number of depths in the collection's hierarchy.*/
        public static final String COLLECTION_DEPTH_MAX_NUM = KEY_ROOT + "dav.depth.maxnum";
    }

    /**
     *Setting around Security.
     */
    public static final class Security {
        /** The secret key used when encrypting the token.*/
        public static final String TOKEN_SECRET_KEY = KEY_ROOT + "security.secret16";

        /** The secret key used when encrypting the token.*/
        public static final String AUTH_PASSWORD_SALT = KEY_ROOT + "security.auth.password.salt";

        /** Encrypt the DAV file (true: enabled false: disabled (default)). */
        public static final String DAV_ENCRYPT_ENABLED = KEY_ROOT + "security.dav.encrypt.enabled";
    }

    /**
     *Lock setting.
     */
    public static final class Lock {
        /** Type of Lock Tolerance: memcached*/
        public static final String TYPE = KEY_ROOT + "lock.type";

        /** Account lock expiration date (s).*/
        public static final String ACCOUNTLOCK_LIFETIME = KEY_ROOT + "lock.accountlock.time";

        /** Number of retries at lock acquisition.*/
        public static final String RETRY_TIMES = KEY_ROOT + "lock.retry.times";

        /** Interval at lock retry retry.*/
        public static final String RETRY_INTERVAL = KEY_ROOT + "lock.retry.interval";

        /** Number of retries at cell lock acquisition.*/
        public static final String CELL_RETRY_TIMES = KEY_ROOT + "lock.cell.retry.times";

        /** Interval at cell lock acquisition retry.*/
        public static final String CELL_RETRY_INTERVAL = KEY_ROOT + "lock.cell.retry.interval";

        /** The memcached host name to hold lock on memcached.*/
        public static final String MEMCACHED_HOST = KEY_ROOT + "lock.memcached.host";

        /** The memcached port number for keeping locks on memcached.*/
        public static final String MEMCACHED_PORT = KEY_ROOT + "lock.memcached.port";

        /** Lock memcached operation Timeout value (ms).*/
        public static final String MEMCACHED_OPTIMEOUT = KEY_ROOT + "lock.memcached.opTimeout";
    }

    /**
     *Elastic Search related settings.
     */
    public static final class ES {
        /** Elastic Search host configuration property key.*/
        public static final String HOSTS = KEY_ROOT + "es.hosts";

        /** Elastic Search Property key for cluster name setting.*/
        public static final String CLUSTERNAME = KEY_ROOT + "es.cluster.name";

        /** When using Elastic Search, the prefix setting property key corresponding to the UNIT name used for DB naming used at DB creation.*/
        public static final String UNIT_PREFIX = KEY_ROOT + "es.unitPrefix";

        /** Property key of search result output upper limit setting of Elastic Search.*/
        public static final String TOP_NUM = KEY_ROOT + "es.topnum";

        /** Number of retries when an error occurs.*/
        public static final String RETRY_TIMES = KEY_ROOT + "es.retryTimes";

        /** Retry interval at error occurrence.*/
        public static final String RETRY_INTERVAL = KEY_ROOT + "es.retryInterval";
    }

    /**
     *Setting of BinaryData.
     */
    public static final class BinaryData {
        /** Setting whether to delete physically when deleting files (true: physical deletion, false: logical deletion).*/
        public static final String PHYSICAL_DELETE_MODE = KEY_ROOT + "binaryData.physical.delete.mode";

        /** Whether to enable fsync when writing to a file (true: valid false: disabled (default)).*/
        public static final String FSYNC_ENABLED = KEY_ROOT + "binaryData.fsync.enabled";

        /** Maximum number of retries at the time of reading / writing of Dav file, hard link creation / file name modification.*/
        public static final String MAX_RETRY_COUNT = KEY_ROOT + "binaryData.dav.retry.count";

        /** Retry interval (msec) at the time of reading / writing Dav file, hard link creation / file name modification.*/
        public static final String RETRY_INTERVAL = KEY_ROOT + "binaryData.dav.retry.interval";
    }

    /**
     *Blob setting.
     */
    public static final class BlobStore {
        /** Property key of root (URL, PATH) setting to store blob data when using Elastic Search.*/
        public static final String ROOT = KEY_ROOT + "blobStore.root";
    }

    /**
     *Number of properties in user data, hierarchy limit setting.
     */
    public static final class UserDataProperties {
        /** Maximum number limit of EntityType.*/
        public static final String MAX_ENTITY_TYPES = KEY_ROOT + "box.odata.schema.MaxEntityTypes";

        /** Maximum number of properties contained in EntityType.*/
        public static final String MAX_PROPERTY_COUNT_IN_ENTITY = KEY_ROOT + "box.odata.schema.MaxProperties";

        /** The limit number of SimpleType of each hierarchy (eg 400, 100, 20, 0).*/
        public static final String SIMPLE_TYPE_PROPERTY_LIMITS =
                KEY_ROOT + "box.odata.schema.property.LayerLimits.SimpleType";

        /** The limit number of ComplexType of each hierarchy (eg 20, 20, 50, 0).*/
        public static final String COMPLEX_TYPE_PROPERTY_LIMITS =
                KEY_ROOT + "box.odata.schema.property.LayerLimits.ComplexType";
    }

    /**
     *Event setting.
     */
    public static final class Event {
        /** Storage directory of the latest event log file.*/
        public static final String EVENT_LOG_CURRENT_DIR = KEY_ROOT + "event.log.current.dir";
        /** Maximum event hop count. */
        public static final String EVENT_HOP_MAXNUM = KEY_ROOT + "event.hop.maxnum";
    }

    /**
     *Setting around Cache.
     */
    public static final class Cache {
        /** Type of Cache Tolerance: none / memcached*/
        public static final String TYPE = KEY_ROOT + "cache.type";

        /** Whether cell caching is enabled Tolerance: true / false*/
        public static final String CELL_CACHE_ENABLED = KEY_ROOT + "cache.cell.enabled";

        /** Whether box caching is enabled Tolerance: true / false*/
        public static final String BOX_CACHE_ENABLED = KEY_ROOT + "cache.box.enabled";

        /** Whether schema caching is enabled Tolerance: true / false*/
        public static final String SCHEMA_CACHE_ENABLED = KEY_ROOT + "cache.schema.enabled";

        /** memcached host name.*/
        public static final String MEMCACHED_HOST = KEY_ROOT + "cache.memcached.host";

        /** memcached port number.*/
        public static final String MEMCACHED_PORT = KEY_ROOT + "cache.memcached.port";

        /** memcached operation Timeout value (ms).*/
        public static final String MEMCACHED_OPTIMEOUT = KEY_ROOT + "cache.memcached.opTimeout";

        /** Cache expiration date.*/
        public static final String MEMCACHED_EXPIRES_IN = KEY_ROOT + "cache.memcached.expiresin";
    }

    /**
     *Setting around Engine.
     */
    public static final class Engine {
        /** The Host key of the Engine.*/
        public static final String HOST = KEY_ROOT + "engine.host";

        /** Port of the Engine.*/
        public static final String PORT = KEY_ROOT + "engine.port";

        /** Path key of Engine.*/
        public static final String PATH = KEY_ROOT + "engine.path";
    }

    /**
     *Setting around X509.
     */
    public static final class X509 {
        /** X509 Property key of the path setting where the root certificate is placed.*/
        public static final String ROOT_CRT = KEY_ROOT + "x509.root";

        /** X509 Property key of path setting where certificate is placed.*/
        public static final String CRT = KEY_ROOT + "x509.crt";

        /** X509 Property key of the path setting where the private key is placed.*/
        public static final String KEY = KEY_ROOT + "x509.key";
    }

    /**
     *bar file export / install related settings.
     */
    public static final class BAR {
        /** bar Property key of the maximum file size (MB) of the file.*/
        public static final String BAR_FILE_MAX_SIZE = KEY_ROOT + "bar.file.maxSize";

        /** bar Property key of the maximum file size (MB) entry in the file.*/
        public static final String BAR_ENTRY_MAX_SIZE = KEY_ROOT + "bar.entry.maxSize";

        /** A property key of a size to return a response when linking user data.*/
        public static final String BAR_USERDATA_LINKS_OUTPUT_STREAM_SIZE = KEY_ROOT
                + "bar.userdata.linksOutputStreamSize";

        /** Property key of collective number of registered user data.*/
        public static final String BAR_USERDATA_BULK_SIZE = KEY_ROOT + "bar.userdata.bulkSize";

        /** Temporary storage directory for bar files. */
        public static final String BAR_TMP_DIR = KEY_ROOT + "bar.tmp.dir";

        /** Store in memcached bar The validity period (in seconds) of the installation processing status.*/
        public static final String BAR_PROGRESS_EXPIRE_IN_SEC = KEY_ROOT + "bar.progress.expireInSec";
    }

    /**
     * cell snapshot configurations.
     */
    public static final class CellSnapshot {
        /** Root directory path to store the cell export file. */
        public static final String ROOT = KEY_ROOT + "cellSnapshot.root";
    }

    /**
     * EventBus configurations.
     */
    public static final class EventBus {
        /** message queue implementation to use. */
        public static final String MQ = KEY_ROOT + "eventbus.mq";

        /** ActiveMQ broker url. */
        public static final String ACTIVEMQ_BROKER_URL = KEY_ROOT + "eventbus.activemq.brokerUrl";

        /** Kafka servers. */
        public static final String KAFKA_SERVERS = KEY_ROOT + "eventbus.kafka.bootstrap.servers";

        /** queue name of EventBus. */
        public static final String QUEUE = KEY_ROOT + "eventbus.queue";

        /** topic name for all event. */
        public static final String TOPIC_ALL = KEY_ROOT + "eventbus.topic.all";

        /** topic name for rule event. */
        public static final String TOPIC_RULE = KEY_ROOT + "eventbus.topic.rule";

        /** Number of threads to process event. */
        public static final String EVENTPROC_THREAD_NUM = KEY_ROOT + "eventbus.eventProcessing.thread.num";
    }

    /**
     * rule configurations.
     */
    public static final class Rule {
        /** Number of threads to manage timer event. */
        public static final String TIMEREVENT_THREAD_NUM = KEY_ROOT + "rule.timerEvent.thread.num";
    }

    static {
        //Forcibly load various message output classes
        PersoniumCoreLog.loadConfig();
        PersoniumCoreException.loadConfig();
        PersoniumCoreAuthnException.loadConfig();
    }

    /** singleton. */
    private static PersoniumUnitConfig singleton = new PersoniumUnitConfig();

    // static Logger log = LoggerFactory.getLogger(PersoniumCoreConfig.class);

    /** Property entity that stores the setting value.*/
    private final Properties props = new Properties();

    /** Property entity that stores setting values ​​to be overridden.*/
    private final Properties propsOverride = new Properties();

    /**
     *A protected constructor.
     */
    protected PersoniumUnitConfig() {
        this.doReload();
    }

    /**
     *Reload the settings.
     */
    private synchronized void doReload() {
        Logger log = LoggerFactory.getLogger(PersoniumUnitConfig.class);
        Properties properties = getUnitConfigDefaultProperties();
        Properties propertiesOverride = getPersoniumConfigProperties();
        //When reading succeeds, replace with member variable
        if (!properties.isEmpty()) {
            this.props.clear();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    continue;
                }
                this.props.setProperty((String) entry.getKey(), (String) entry.getValue());
            }
        }
        if (!propertiesOverride.isEmpty()) {
            this.propsOverride.clear();
            for (Map.Entry<Object, Object> entry : propertiesOverride.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    continue;
                }
                this.propsOverride.setProperty((String) entry.getKey(), (String) entry.getValue());
            }
        }
        for (Object keyObj : propsOverride.keySet()) {
            String key = (String) keyObj;
            String value = this.propsOverride.getProperty(key);
            if (value == null) {
                continue;
            }
            log.debug("Overriding Config " + key + "=" + value);
            this.props.setProperty(key, value);
        }
    }

    private static boolean isSpaceSeparatedValueIncluded(String spaceSeparatedValue, String testValue, String unitUrl) {
        if (testValue == null || spaceSeparatedValue == null) {
            return false;
        }
        String[] values = spaceSeparatedValue.split(" ");
        for (String val : values) {
            // Correspondence when "localunit" is set for issuers.
            String convertedValue = UriUtils.convertSchemeFromLocalUnitToHttp(unitUrl, val);
            if (testValue.equals(convertedValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     *Read the personium-unit-config-default.properties file.
     * @return personium-unit-config-default.properties
     */
    protected Properties getUnitConfigDefaultProperties() {
        Properties properties = new Properties();
        InputStream is = PersoniumUnitConfig.class.getClassLoader().getResourceAsStream(
                "personium-unit-config-default.properties");
        try {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("failed to load config!", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("failed to close config stream", e);
            }
        }
        return properties;
    }

    /**
     *Read the personium-unit-config.properties file.
     * @return personium-unit-config.properties
     */
    protected Properties getPersoniumConfigProperties() {
        Logger log = LoggerFactory.getLogger(PersoniumUnitConfig.class);
        Properties propertiesOverride = new Properties();
        String configFilePath = System.getProperty(KEY_CONFIG_FILE);
        InputStream is = getConfigFileInputStream(configFilePath);
        try {
            if (is != null) {
                propertiesOverride.load(is);
            } else {
                log.debug("[personium-unit-config.properties] file not found on the classpath. using default config.");
            }
        } catch (IOException e) {
            log.debug("IO Exception when loading [personium-unit-config.properties] file.");
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                log.debug("IO Exception when closing [personium-unit-config.properties] file.");
            }
        }
        return propertiesOverride;
    }

    /**
     *Get personium-unit-config.properties in InputStream format.
     *@ param configFilePath configuration file path
     * @return personium-unit-config.properties
     */
    protected InputStream getConfigFileInputStream(String configFilePath) {
        Logger log = LoggerFactory.getLogger(PersoniumUnitConfig.class);
        InputStream configFileInputStream = null;
        if (configFilePath == null) {
            configFileInputStream = PersoniumUnitConfig.class.getClassLoader().getResourceAsStream(
                    "personium-unit-config.properties");
            return configFileInputStream;
        }

        try {
            //Read the configuration file from the specified path
            File configFile = new File(configFilePath);
            configFileInputStream = new FileInputStream(configFile);
            log.info("personium-unit-config.properties from system properties.");
        } catch (FileNotFoundException e) {
            //If there is no file in the specified path, read the file on the class path
            configFileInputStream = PersoniumUnitConfig.class.getClassLoader().getResourceAsStream(
                    "personium-unit-config.properties");
            log.info("personium-unit-config.properties from class path.");
        }
        return configFileInputStream;
    }

    /**
     *Acquire setting value.
     *@ param key
     *@return setting value
     */
    private String doGet(final String key) {
        return props.getProperty(key);
    }

    /**
     *Setting value setting.
     *@ param key
     *@ param value value
     */
    private void doSet(final String key, final String value) {
        props.setProperty(key, value);
    }

    /**
     *Get all the properties.
     *@return property list object
     */
    public static Properties getProperties() {
        return singleton.props;
    }

    /**
     *Key Specify the character string to acquire setting information.
     *@ param key setting key
     *@return setting value
     */
    public static String get(final String key) {
        return singleton.doGet(key);
    }

    /**
     *Key Specify the character string and change the setting information.
     *@ param key setting key
     *@ param value value
     */
    public static void set(final String key, final String value) {
        singleton.doSet(key, value);
    }

    /**
     *Get the value of Core Version.
     *@return Core Version value
     */
    public static String getCoreVersion() {
        return get(CORE_VERSION);
    }

    /**
     * Get thread pool num for cell io.
     * @return thread pool num
     */
    public static int getThreadPoolNumForCellIO() {
        return Integer.parseInt(get(THREAD_POOL_NUM_IO_CELL));
    }

    /**
     * Get thread pool num for box io.
     * @return thread pool num
     */
    public static int getThreadPoolNumForBoxIO() {
        return Integer.parseInt(get(THREAD_POOL_NUM_IO_BOX));
    }

    /**
     * Get thread pool num for misc.
     * @return thread pool num
     */
    public static int getThreadPoolNumForMisc() {
        return Integer.parseInt(get(THREAD_POOL_NUM_MISC));
    }

    /**
     *Get the unit master token value.
     *@ return Master token value
     */
    public static String getMasterToken() {
        return get(MASTER_TOKEN);
    }

    /**
     *@return unit Host name to be certified as a user token issuer.
     */
    public static String getUnitUserIssuers() {
        return get(UNIT_USER_ISSUERS);
    }

    /**
     *@return scheme setting key for unit.
     */
    public static String getUnitScheme() {
        return get(UNIT_SCHEME);
    }

    /**
     * Get port number for Unit.
     * @return port
     */
    public static int getUnitPort() {
        int port;
        try {
            port = Integer.parseInt(get(UNIT_PORT));
        } catch (NumberFormatException e) {
            port = -1;
        }
        return port;
    }

    /**
     * Get path for Unit.
     * @return path
     */
    public static String getUnitPath() {
        return get(UNIT_PATH);
    }

    /**
     *@return plugin's path setup key.
     */
    public static String getPluginPath() {
        return get(PLUGIN_PATH);
    }

    /**
     * Get base url of this unit.
     * @return base url
     */
    public static String getBaseUrl() {
        String path = getUnitPath();
        if (path != null) {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
        } else {
            path = "";
        }

        UriBuilder uriBuilder = UriBuilder
                .fromPath(path)
                .scheme(getUnitScheme())
                .host(PersoniumCoreUtils.getFQDN())
                .port(getUnitPort());

        return uriBuilder.build().toString() + "/";
    }

    /**
     * Get default value of relayhtmlurl.
     * @return Default value of relayhtmlurl
     */
    public static String getRelayhtmlurlDefault() {
        return get(Cell.RELAYHTMLURL_DEFAULT);
    }

    /**
     * Get default value of authorizationhtmlurl.
     * @return Default value of authorizationhtmlurl
     */
    public static String getAuthorizationhtmlurlDefault() {
        return get(Cell.AUTHORIZATIONHTMLURL_DEFAULT);
    }

    /**
     *@return Maximum number of requests when doing $ batch processing.
     */
    public static String getOdataBatchBulkRequestMaxSize() {

        String cnt = get(OData.BATCH_BULK_REQUEST_MAX_SIZE);
        if (cnt == null) {
            cnt = "1000";
        }
        return cnt;
    }

    /**
     *@return $ batch processing timeout time (ms)
     */
    public static long getOdataBatchRequestTimeoutInMillis() {

        String mSecInStr = get(OData.BATCH_REQUEST_TIMEOUT_IN_MILLIS);
        if (null != mSecInStr && !mSecInStr.isEmpty()) {
            return Long.parseLong(mSecInStr);
        }
        return DEFAULT_BATCH_TIMEOUT;
    }

    /**
     *@return $ sleep time for batch processing (ms)
     */
    public static long getOdataBatchSleepInMillis() {

        String mSecInStr = get(OData.BATCH_SLEEP_IN_MILLIS);
        if (null != mSecInStr && !mSecInStr.isEmpty()) {
            return Long.parseLong(mSecInStr);
        }
        return DEFAULT_BATCH_SLEEP;
    }

    /**
     *@return $ sleep interval of batch processing (ms)
     */
    public static long getOdataBatchSleepIntervalInMillis() {

        String mSecInStr = get(OData.BATCH_SLEEP_INTERVAL_IN_MILLIS);
        if (null != mSecInStr && !mSecInStr.isEmpty()) {
            return Long.parseLong(mSecInStr);
        }
        return DEFAULT_BATCH_SLEEP_INTERVAL;
    }

    /**
     *Get the maximum limit number of child elements of the collection.
     *@return Maximum number of child elements of collection
     */
    public static int getMaxChildResourceCount() {
        return Integer.parseInt(get(Dav.COLLECTION_CHILDRESOURCE_MAX_NUM));
    }

    /**
     *Get the maximum limit number of collection hierarchy.
     *@return Maximum number of hierarchies in collection
     */
    public static int getMaxCollectionDepth() {
        return Integer.parseInt(get(Dav.COLLECTION_DEPTH_MAX_NUM));
    }

    /**
     *Returns whether or not fsync is valid when writing a file.
     *@return true if it is valid
     */
    public static boolean getFsyncEnabled() {
        return Boolean.parseBoolean(get(BinaryData.FSYNC_ENABLED));
    }

    /**
     *@return N: Get the maximum number of links that $ links can create.
     */
    public static int getLinksNtoNMaxSize() {
        return Integer.parseInt(get(OData.NN_LINKS_MAX_NUM));
    }

    /**
     *@return $ expand Maximum number of times specified by specifying expand.
     */
    public static int getTopQueryMaxSizeWithExpand() {
        return Integer.parseInt(get(OData.EXPAND_TOP_MAXNUM));
    }

    /**
     *@return Max expanded number of $ expand (when getting list).
     */
    public static int getMaxExpandSizeForList() {
        return Integer.parseInt(get(OData.EXPAND_LIST_MAXNUM));
    }

    /**
     *@return Max expanded number of $ expand (when acquiring one case).
     */
    public static int getMaxExpandSizeForRetrive() {
        return Integer.parseInt(get(OData.EXPAND_RETRIEVE_MAXNUM));
    }

    /**
     *@return The maximum value that can be specified for $ top.
     */
    public static int getTopQueryMaxSize() {
        return Integer.parseInt(get(OData.TOP_MAX_NUM));
    }

    /**
     *@return The maximum value that can be specified for $ skip.
     */
    public static int getSkipQueryMaxSize() {
        return Integer.parseInt(get(OData.SKIP_MAX_NUM));
    }

    /**
     *@return Default number of return cases when obtaining list.
     */
    public static int getTopQueryDefaultSize() {
        return Integer.parseInt(get(OData.TOP_DEFAULT));
    }

    /**
     *@return The maximum number of properties of $ expand (when listing).
     */
    public static int getExpandPropertyMaxSizeForList() {
        return Integer.parseInt(get(OData.EXPAND_PROPERTY_MAX_NUM_LIST));
    }

    /**
     *@return The maximum number of properties of $ expand (when acquiring one item).
     */
    public static int getExpandPropertyMaxSizeForRetrieve() {
        return Integer.parseInt(get(OData.EXPAND_PROPERTY_MAX_NUM_RETRIEVE));
    }

    /**
     *@return Lock type.
     */
    public static String getLockType() {
        return get(Lock.TYPE);
    }

    /**
     *@return Account lock expiration date (s).
     */
    public static String getAccountLockLifetime() {
        return get(Lock.ACCOUNTLOCK_LIFETIME);
    }

    /**
     *@return The number of retries at lock acquisition.
     */
    public static String getLockRetryTimes() {
        return get(Lock.RETRY_TIMES);
    }

    /**
     *@return Interval at lock retry retry.
     */
    public static String getLockRetryInterval() {
        return get(Lock.RETRY_INTERVAL);
    }

    /**
     *@return Number of retries when acquiring cell lock.
     */
    public static int getCellLockRetryTimes() {
        return Integer.parseInt(get(Lock.CELL_RETRY_TIMES));
    }

    /**
     *@return Interval at cell retry acquisition retry.
     */
    public static long getCellLockRetryInterval() {
        return Long.parseLong(get(Lock.CELL_RETRY_INTERVAL));
    }

    /**
     *@return memcached host name to hold lock on memcached.
     */
    public static String getLockMemcachedHost() {
        return get(Lock.MEMCACHED_HOST);
    }

    /**
     *@return memcached port number for keeping locks on memcached.
     */
    public static String getLockMemcachedPort() {
        return get(Lock.MEMCACHED_PORT);
    }

    /**
     *@return memcached operation for locking timeout value (ms).
     */
    public static long getLockMemcachedOpTimeout() {
        return Long.parseLong(get(Lock.MEMCACHED_OPTIMEOUT));
    }

    /**
     *@return Storage directory of the latest event log file.
     */
    public static String getEventLogCurrentDir() {
        return get(Event.EVENT_LOG_CURRENT_DIR);
    }

    /**
     * Get event hop maximum number.
     * @return event hop maximum number
     */
    public static int getMaxEventHop() {
        return Integer.parseInt(get(Event.EVENT_HOP_MAXNUM));
    }

    /**
     *Gets the setting value of ElasticSearch's host name.
     *@return setting value
     */
    public static String getEsHosts() {
        return get(ES.HOSTS);
    }

    /**
     *Retrieve ElasticSearch's cluster name setting value.
     *@return setting value
     */
    public static String getEsClusterName() {
        return get(ES.CLUSTERNAME);
    }

    /**
     *Get the index name prefix on Elastic Search of Unit that this application is in charge, for example setting u 0, use the name of u 0 _ addwo as the management information index.
     *@return setting value
     */
    public static String getEsUnitPrefix() {
        return get(ES.UNIT_PREFIX);
    }

    /**
     *Get the set value of search result output upper limit of @ return Es.
     */
    public static int getEsTopNum() {
        return Integer.parseInt(get(ES.TOP_NUM));
    }

    /**
     *@ return blob The route (URL, PATH) to store the data.
     */
    public static String getBlobStoreRoot() {
        return get(BlobStore.ROOT);
    }

    /**
     * Get root directory path to store cell snapshot file.
     * @return root directory path to store cell snapshot file
     */
    public static String getCellSnapshotRoot() {
        return get(CellSnapshot.ROOT);
    }

    /**
     *Binary data (Dav / Eventlog) Setting whether to delete physically when deleting.
     *@return true: physical delete, false: logical delete
     */
    public static boolean getPhysicalDeleteMode() {
        return Boolean.parseBoolean(get(BinaryData.PHYSICAL_DELETE_MODE));
    }

    /**
     *Number of retries when creating / renaming / deleting hard links of Dav files.
     *@return retry count
     */
    public static int getDavFileOperationRetryCount() {
        return Integer.parseInt(get(BinaryData.MAX_RETRY_COUNT));
    }

    /**
     *Retry interval (msec) for hard link creation / renaming / deletion of Dav file.
     *@return Retry interval (msec)
     */
    public static long getDavFileOperationRetryInterval() {
        return Long.parseLong(get(BinaryData.RETRY_INTERVAL));
    }

    /**
     *The number of retries when an error occurred in @return ES.
     */
    public static String getESRetryTimes() {
        return get(ES.RETRY_TIMES);
    }

    /**
     *Retry interval (milliseconds) at error occurrence in @return ES.
     */
    public static String getESRetryInterval() {
        return get(ES.RETRY_INTERVAL);
    }

    /**
     * Get max size of bar file.
     * @return max size of bar file
     */
    public static long getBarEntryMaxSize() {
        return Long.parseLong(get(BAR.BAR_ENTRY_MAX_SIZE));
    }

    /**
     * Get temp dir path for barinstall.
     * @return temp dir path
     */
    public static String getBarInstallTempDir() {
        return get(BAR.BAR_TMP_DIR) + "/install";
    }

    /**
     * Get temp dir path for barexport.
     * @return temp dir path
     */
    public static String getBarExportTempDir() {
        return get(BAR.BAR_TMP_DIR) + "/export";
    }

    /**
     *@return bar The expiration date (s) of the asynchronous processing status of the installation.
     */
    public static String getBarInstallProgressLifeTimeExpireInSec() {
        return get(BAR.BAR_PROGRESS_EXPIRE_IN_SEC);
    }

    /**
     *@return Cache type.
     */
    public static String getCacheType() {
        return get(Cache.TYPE);
    }

    /**
     *@return memcached host name.
     */
    public static String getCacheMemcachedHost() {
        return get(Cache.MEMCACHED_HOST);
    }

    /**
     *@return memcached port number.
     */
    public static String getCacheMemcachedPort() {
        return get(Cache.MEMCACHED_PORT);
    }

    /**
     *@ reccache memcached operation timeout value (ms).
     */
    public static long getCacheMemcachedOpTimeout() {
        return Long.parseLong(get(Cache.MEMCACHED_OPTIMEOUT));
    }

    /**
     *Returns whether or not the cache of Cell is valid.
     *@return true if it is valid.
     */
    public static boolean isCellCacheEnabled() {
        return Boolean.parseBoolean(get(Cache.CELL_CACHE_ENABLED));
    }

    /**
     *Returns whether or not the Box's cache is valid.
     *@return true if it is valid.
     */
    public static boolean isBoxCacheEnabled() {
        return Boolean.parseBoolean(get(Cache.BOX_CACHE_ENABLED));
    }

    /**
     *Returns whether the schema cache is valid or not.
     *@return true if it is valid.
     */
    public static boolean isSchemaCacheEnabled() {
        return Boolean.parseBoolean(get(Cache.SCHEMA_CACHE_ENABLED));
    }

    /**
     *@return memcached Cache expiration date.
     */
    public static int getCacheMemcachedExpiresIn() {
        return Integer.parseInt(get(Cache.MEMCACHED_EXPIRES_IN));
    }

    /**
     *Get the host name setting value of Enine.
     *@return setting value
     */
    public static String getEngineHost() {
        return get(Engine.HOST);
    }

    /**
     *Get the set value of Enine 's port.
     *@return setting value
     */
    public static int getEnginePort() {
        return Integer.parseInt(get(Engine.PORT));
    }

    /**
     *Get Enine's path setting value.
     *@return setting value
     */
    public static String getEnginePath() {
        return get(Engine.PATH);
    }

    /**
     *Get the set value of the path of the X509 secret key file of this UNIT.
     *@return setting value
     */
    public static String getX509PrivateKey() {
        return get(X509.KEY);
    }

    /**
     *Get the array of the set value of the path of the X509 root certificate file of this UNIT.
     *@return setting value
     */
    public static String[] getX509RootCertificate() {
        String[] x509RootCertificate = null;
        String value = get(X509.ROOT_CRT);
        if (value != null) {
            x509RootCertificate = value.split(" ");
        }
        return x509RootCertificate;
    }

    /**
     *Get the setting value of the path of the X509 certificate file of this UNIT.
     *@return setting value
     */
    public static String getX509Certificate() {
        return get(X509.CRT);
    }

    /**
     *The secret key setting used when encrypting the token.
     *@return setting value
     */
    public static String getTokenSecretKey() {
        return get(Security.TOKEN_SECRET_KEY);
    }

    /**
     *The salt value of the password to use for authentication.
     *@return salt value of password
     */
    public static String getAuthPasswordSalt() {
        return get(Security.AUTH_PASSWORD_SALT);
    }

    /**
     * Encrypt the DAV file.
     * @return true: enabled false: disabled
     */
    public static boolean isDavEncryptEnabled() {
        return Boolean.parseBoolean(get(Security.DAV_ENCRYPT_ENABLED));
    }

    /**
     * Get message queue implementation of EventBus.
     * @return message queue
     */
    public static String getEventBusMQ() {
        return get(EventBus.MQ);
    }

    /**
     * Get broker url of setting for activemq.
     * @return broker url
     */
    public static String getEventBusActiveMQBrokerUrl() {
        return get(EventBus.ACTIVEMQ_BROKER_URL);
    }

    /**
     * Get servers of setting for kafka.
     * @return comma-separated servers
     */
    public static String getEventBusKafkaServers() {
        return get(EventBus.KAFKA_SERVERS);
    }

    /**
     * Get queue name of EventBus.
     * @return queue name
     */
    public static String getEventBusQueueName() {
        return get(EventBus.QUEUE);
    }

    /**
     * Get topic name for all event.
     * @return topic name
     */
    public static String getEventBusTopicName() {
        return get(EventBus.TOPIC_ALL);
    }

    /**
     * Get topic name for rule event.
     * @return topic name
     */
    public static String getEventBusRuleTopicName() {
        return get(EventBus.TOPIC_RULE);
    }

    /**
     * Get thread number for eventprocessing.
     * @return thread num
     */
    public static int getEventProcThreadNum() {
        return Integer.parseInt(get(EventBus.EVENTPROC_THREAD_NUM));
    }

    /**
     * Get thread number of timer event.
     * @return thread num
     */
    public static int getTimerEventThreadNum() {
        return Integer.parseInt(get(Rule.TIMEREVENT_THREAD_NUM));
    }

    /**
     *Reload the configuration information.
     */
    public static void reload() {
        singleton.doReload();
    }

    /**
     *Returns whether the execution environment is https or not.
     *@return boolean For https: true
     */
    public static boolean isHttps() {
        return PersoniumUnitConfig.getUnitScheme().equals("https");
    }

    /**
     * Check whether the specified URL is included in the host name certified as UnitUserToken Issuer.
     * @param url Target URL
     * @param unitUrl Unit URL
     * @return Included:true
     */
    public static boolean checkUnitUserIssuers(String url, String unitUrl) {
        return isSpaceSeparatedValueIncluded(getUnitUserIssuers(), url, unitUrl);
    }

    /**
     *Get the maximum limit number of EntityType.
     *@return Maximum number of EntityType
     */
    public static int getUserdataMaxEntityCount() {
        return Integer.parseInt(get(UserDataProperties.MAX_ENTITY_TYPES));
    }

    /**
     *Get the maximum limit number of properties that can be included in EntityType.
     *@return Maximum number of properties in EntityType
     */
    public static int getMaxPropertyCountInEntityType() {
        return Integer.parseInt(get(UserDataProperties.MAX_PROPERTY_COUNT_IN_ENTITY));
    }

    /**
     *Get array of limit number of SimpleType in UsetData.
     *@return List of limits on SimpleType in each hierarchy
     */
    public static int[] getUserdataSimpleTypePropertyLimits() {
        String expr = get(UserDataProperties.SIMPLE_TYPE_PROPERTY_LIMITS);
        return getPropertyLimits(expr, -1);
    }

    /**
     *Get array of limit number of ComplexType in UsetData.
     *@return List of limits on SimpleType in each hierarchy
     */
    public static int[] getUserdataComplexTypePropertyLimits() {
        int depth = getUserdataSimpleTypePropertyLimits().length;
        String expr = get(UserDataProperties.COMPLEX_TYPE_PROPERTY_LIMITS);
        return getPropertyLimits(expr, depth);
    }

    /**
     *Whether or not to update the last login time of Account when password authentication is successful ().
     *@return true: Update (default) false: Do not update
     */
    public static boolean getAccountLastAuthenticatedEnable() {
        return Boolean.parseBoolean(get(Account.ACCOUNT_LAST_AUTHENTICATED_ENABLED));
    }

    /**
     *Return a numeric list (character string) specified as a comma-separated value as an int type array.
     *If the contents of the numeric list are longer than the length specified by arrayLength, the long part is discarded. .
     *eg limitExpression: "1, 2, 3, 4", arrayLength = 2 -> int [] {1, 2}
     *If the contents of the numerical list are shorter than the length specified by arrayLength, missing parts are padded with zeros. .
     *eg limitExpression: "1, 2", arrayLength = 4 → int [] {1, 2, 0, 0}
     *@ param limitExpression Comma-separated numeric list (character string)
     *@ param arrayLength The length of the array to return. If it is less than 0, it is the same as specifying an array of the number of elements obtained from limitExpression.
     *@ return integer array
     */
    private static int[] getPropertyLimits(String limitExpression, int arrayLength) {
        if (null != limitExpression) {
            String[] values = limitExpression.split(",");
            if (arrayLength < 1) {
                arrayLength = values.length;
            }
            int[] result = new int[arrayLength];
            Arrays.fill(result, 0);
            for (int i = 0; i < values.length && i < arrayLength; i++) {
                if ("*".equals(values[i])) {
                    //-1 is a numerical value indicating unlimited
                    result[i] = -1;
                } else {
                    result[i] = Integer.parseInt(values[i]);
                    //Notice: If it is not a number, a NumberFormatException is thrown.
                }
            }
            return result;
        }
        return new int[] {};
    }
}
