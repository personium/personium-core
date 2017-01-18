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
package com.fujitsu.dc.core.bar;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.wink.webdav.WebDAVMethod;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Propstat;
import org.apache.wink.webdav.model.Resourcetype;
import org.apache.wink.webdav.model.Response;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;
import org.json.simple.JSONObject;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmStructuralType;
import org.odata4j.producer.EntityResponse;
import org.odata4j.stax2.XMLEventReader2;
import org.odata4j.stax2.XMLFactoryProvider2;
import org.odata4j.stax2.XMLInputFactory2;
import org.odata4j.stax2.staximpl.StaxXMLFactoryProvider2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.DcCoreMessageUtils;
import com.fujitsu.dc.core.bar.jackson.JSONExtRoles;
import com.fujitsu.dc.core.bar.jackson.JSONLinks;
import com.fujitsu.dc.core.bar.jackson.JSONManifest;
import com.fujitsu.dc.core.bar.jackson.JSONMappedObject;
import com.fujitsu.dc.core.bar.jackson.JSONRelations;
import com.fujitsu.dc.core.bar.jackson.JSONRoles;
import com.fujitsu.dc.core.bar.jackson.JSONUserDataLinks;
import com.fujitsu.dc.core.eventbus.DcEventBus;
import com.fujitsu.dc.core.eventbus.JSONEvent;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.core.model.BoxCmp;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.core.model.DavCmp;
import com.fujitsu.dc.core.model.DavCommon;
import com.fujitsu.dc.core.model.ModelFactory;
import com.fujitsu.dc.core.model.ctl.AssociationEnd;
import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.core.model.ctl.ComplexTypeProperty;
import com.fujitsu.dc.core.model.ctl.CtlSchema;
import com.fujitsu.dc.core.model.ctl.EntityType;
import com.fujitsu.dc.core.model.ctl.Event;
import com.fujitsu.dc.core.model.ctl.Event.LEVEL;
import com.fujitsu.dc.core.model.ctl.ExtRole;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.core.model.ctl.Relation;
import com.fujitsu.dc.core.model.ctl.Role;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.odata.UserDataODataProducer;
import com.fujitsu.dc.core.model.impl.es.odata.UserSchemaODataProducer;
import com.fujitsu.dc.core.model.progress.Progress;
import com.fujitsu.dc.core.model.progress.ProgressInfo;
import com.fujitsu.dc.core.model.progress.ProgressManager;
import com.fujitsu.dc.core.odata.DcEdmxFormatParser;
import com.fujitsu.dc.core.odata.DcODataProducer;
import com.fujitsu.dc.core.odata.OEntityWrapper;
import com.fujitsu.dc.core.rs.cell.EventResource;
import com.fujitsu.dc.core.rs.odata.BulkRequest;
import com.fujitsu.dc.core.rs.odata.ODataEntitiesResource;
import com.fujitsu.dc.core.rs.odata.ODataEntityResource;
import com.fujitsu.dc.core.rs.odata.ODataResource;

/**
 * Httpリクエストボディからbarファイルを読み込むためのクラス.
 */
public class BarFileReadRunner implements Runnable {

    private static final String CODE_BAR_INSTALL_FAILED = "PL-BI-0001";

    private static final String CODE_BAR_INSTALL_COMPLETED = "PL-BI-0000";

    private static final String CODE_BAR_INSTALL_STARTED = "PL-BI-1001";

    /**
     * ログ用オブジェクト.
     */
    static Logger log = LoggerFactory.getLogger(BarFileReadRunner.class);

    static final long MB = 1024 * 1024;
    private static final int TYPE_WEBDAV_COLLECTION = 0;
    private static final int TYPE_ODATA_COLLECTION = 1;
    private static final int TYPE_SERVICE_COLLECTION = 2;
    private static final int TYPE_DAV_FILE = 3;
    private static final int TYPE_SVC_FILE = 4;
    private static final int TYPE_MISMATCH = -1;

    private Map<String, Boolean> barFileOrder;
    private File barFile;
    private ZipArchiveInputStream zipArchiveInputStream;
    private final String boxName;
    private final ODataEntityResource odataEntityResource;
    private final DcODataProducer odataProducer;
    private final String entitySetName;
    private final UriInfo uriInfo;
    private final String requestKey;

    static final String ROOT_DIR = "bar/";
    static final String META_DIR = "bar/00_meta/";
    static final String CONTENTS_DIR_NAME = "90_contents";
    static final String CONTENTS_DIR = ROOT_DIR + CONTENTS_DIR_NAME + "/";
    static final String MANIFEST_JSON = "00_manifest.json";
    static final String RELATION_JSON = "10_relations.json";
    static final String ROLE_JSON = "20_roles.json";
    static final String EXTROLE_JSON = "30_extroles.json";
    static final String LINKS_JSON = "70_$links.json";
    static final String ROOTPROPS_XML = "90_rootprops.xml";
    static final String METADATA_XML = "00_$metadata.xml";
    static final String USERDATA_LINKS_JSON = "10_odatarelations.json";
    static final String USERDATA_DIR_NAME = "90_data";

    private static final String DCBOX_NO_SLUSH = "dcbox:";
    private static final String DCBOX = "dcbox:/";

    private Cell cell;
    private Box box;
    private String schemaUrl; // ACL名前空間チェック用
    private BoxCmp boxCmp;
    private Map<String, DavCmp> davCmpMap;
    private Map<String, String> davFileMap = new HashMap<String, String>();
    private long linksOutputStreamSize = Long.parseLong(DcCoreConfig
            .get(DcCoreConfig.BAR.BAR_USERDATA_LINKS_OUTPUT_STREAM_SIZE));
    private long bulkSize = Long.parseLong(DcCoreConfig
            .get(DcCoreConfig.BAR.BAR_USERDATA_BULK_SIZE));
    private Event event;
    private DcEventBus eventBus;
    private BarInstallProgressInfo progressInfo;

    /**
     * コンストラクタ.
     * @param barFile bar file object
     * @param cell Install target Cell
     * @param boxName Install target Box Name
     * @param odataEntityResource JAX-RS resource
     * @param producer ODataProducer
     * @param entitySetName entitySetName(=box name)
     * @param uriInfo uriInfo
     * @param requestKey イベントログに出力するRequestKeyフィールドの値
     */
    public BarFileReadRunner(
            File barFile,
            Cell cell,
            String boxName,
            ODataEntityResource odataEntityResource,
            DcODataProducer producer,
            String entitySetName,
            UriInfo uriInfo,
            String requestKey) {
        this.barFile = barFile;
        this.boxName = boxName;
        this.odataEntityResource = odataEntityResource;
        this.odataProducer = producer;
        this.entitySetName = entitySetName;
        this.uriInfo = uriInfo;
        this.cell = cell;
        this.box = null;
        this.boxCmp = null;
        this.davCmpMap = new HashMap<String, DavCmp>();
        this.requestKey = requestKey;
        setupBarFileOrder();
    }

    /**
     * barファイル読み込み処理.
     */
    public void run() {
        boolean isSuccess = true;

        String path = "/" + this.cell.getName() + "/" + boxName + "/";
        log.debug("install target: " + path);

        try {
            List<String> doneKeys = new ArrayList<String>();

            try {
                this.zipArchiveInputStream = new ZipArchiveInputStream(new FileInputStream(barFile));
            } catch (IOException e) {
                throw DcCoreException.Server.FILE_SYSTEM_ERROR.params(e.getMessage());
            }
            // ルートディレクトリ("bar/")の存在チェック
            if (!isRootDir()) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", ROOT_DIR, message);
                isSuccess = false;
                return;
            }

            // 00_metaの存在チェック
            if (!isMetadataDir()) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", META_DIR, message);
                isSuccess = false;
                return;
            }

            // 00_metaの読み込み
            ZipArchiveEntry zae = null;
            try {
                long maxBarEntryFileSize = getMaxBarEntryFileSize();
                Set<String> keyList = barFileOrder.keySet();

                while ((zae = this.zipArchiveInputStream.getNextZipEntry()) != null) {
                    String entryName = zae.getName();
                    log.debug("Entry Name: " + entryName);
                    log.debug("Entry Size: " + zae.getSize());
                    log.debug("Entry Compressed Size: " + zae.getCompressedSize());
                    if (!zae.isDirectory()) {
                        this.progressInfo.addDelta(1L);
                    }

                    // barファイル内エントリの解析＆データ登録
                    isSuccess = createMetadata(zae, entryName, maxBarEntryFileSize, keyList, doneKeys);
                    if (!isSuccess) {
                        break;
                    }
                    // 90_contentsを検出した場合、コレクション定義の有無をチェック
                    if (isContentsDir(zae)) {
                        if (davCmpMap.isEmpty()) {
                            writeOutputStream(true, "PL-BI-1004", zae.getName());
                            isSuccess = false;
                        } else {
                            writeOutputStream(false, "PL-BI-1003", zae.getName());
                        }
                        doneKeys.add(zae.getName());
                        break;
                    }
                }
            } catch (IOException ex) {
                isSuccess = false;
                log.info("IOException: " + ex.getMessage(), ex.fillInStackTrace());
            }

            // 90_contents(ユーザデータ)の読み込み
            if (isSuccess && isContentsDir(zae)) {
                isSuccess = createContents();
            }

            // 必須データを全て処理したかどうかをチェックする
            // （既にエラーを検出している場合はスキップする）
            if (isSuccess) {
                Set<String> filenameList = barFileOrder.keySet();
                for (String filename : filenameList) {
                    Boolean isNecessary = barFileOrder.get(filename);
                    if (isNecessary && !doneKeys.contains(filename)) {
                        String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                        writeOutputStream(true, "PL-BI-1004", filename, message);
                        isSuccess = false;
                    }
                }
            }
        } catch (Throwable ex) {
            isSuccess = false;
            String message = getErrorMessage(ex);
            log.info("Exception: " + message, ex.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1005", "", message);
        } finally {
            if (isSuccess) {
                writeOutputStream(false, CODE_BAR_INSTALL_COMPLETED, this.cell.getUrl() + boxName, "");
                this.progressInfo.setStatus(ProgressInfo.STATUS.COMPLETED);
            } else {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(false, CODE_BAR_INSTALL_FAILED, this.cell.getUrl() + boxName, message);
                this.progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            }
            this.progressInfo.setEndTime();
            writeToProgressCache(true);
            IOUtils.closeQuietly(this.zipArchiveInputStream);
            if (this.barFile.exists() && !this.barFile.delete()) {
                log.warn("Failed to remove bar file. [" + this.barFile.getAbsolutePath() + "].");
            }
        }
    }

    /**
     * barインストール処理状況の内部イベント出力用の設定を行う.
     */
    private void setEventBus() {
        eventBus = new DcEventBus(this.cell);
        JSONEvent reqBody = new JSONEvent();
        reqBody.setAction(WebDAVMethod.MKCOL.toString());
        reqBody.setLevel(LEVEL.INFO);
        reqBody.setObject(cell.getUrl() + boxName);
        reqBody.setResult("");

        // TODO Boxのスキーマとサブジェクトのログは内部イベントの正式対応時に実装する
        this.event = EventResource.createEvent(reqBody, this.requestKey, odataEntityResource.getAccessContext());
    }

    /**
     * 例外オブジェクトからメッセージを取得する.
     * @param ex 例外オブジェクト
     * @return メッセージ
     */
    private String getErrorMessage(Throwable ex) {
        String message = ex.getMessage();
        // メッセージがない場合例外クラス名を返却する
        if (message == null) {
            message = "throwed " + ex.getClass().getCanonicalName();
        }
        return message;
    }

    /**
     * Zipアーカイブから取得したエントリを"bar/"ディレクトリかどうかを返す.
     * @return "bar/"である場合はtrue
     */
    private boolean isRootDir() {
        return isMatchEntryName(ROOT_DIR);
    }

    /**
     * Zipアーカイブから取得したエントリを"bar/00_meta"ディレクトリかどうかを返す.
     * @return "bar/00_meta"である場合はtrue
     */
    private boolean isMetadataDir() {
        return isMatchEntryName(META_DIR);
    }

    /**
     * Zipアーカイブから取得したエントリを"bar/90_contents"ディレクトリかどうかを返す.
     * @param zae ZipArchiveEntryオブジェクト
     * @return "bar/90_contents"である場合はtrue
     */
    private boolean isContentsDir(ZipArchiveEntry zae) {
        boolean ret = false;
        if (zae == null) {
            ret = isMatchEntryName(CONTENTS_DIR);
        } else {
            ret = zae.getName().equals(CONTENTS_DIR);
        }
        return ret;
    }

    /**
     * Zipアーカイブから取得したエントリ名が指定した文字列と一致するかどうかを返す.
     * @param name 比較対象の文字列
     * @return 一致する場合はtrue
     */
    private boolean isMatchEntryName(String name) {
        boolean ret = false;
        try {
            ZipArchiveEntry zae = this.zipArchiveInputStream.getNextZipEntry();
            if (zae != null) {
                ret = zae.getName().equals(name);
            }
        } catch (IOException ex) {
            log.info("bar file entry was not read.");
            ret = false;
        }
        return ret;
    }

    /**
     * プロパティファイルからBARファイル内の最大ファイルサイズ(MB)を取得する。
     * @return com.fujitsu.dc.core.bar.entry.maxSize
     */
    private long getMaxBarEntryFileSize() {
        long maxBarFileSize;
        try {
            maxBarFileSize = Long.parseLong(DcCoreConfig
                    .get(DcCoreConfig.BAR.BAR_ENTRY_MAX_SIZE));
        } catch (NumberFormatException ne) {
            log.info("NumberFormatException" + DcCoreConfig
                    .get(DcCoreConfig.BAR.BAR_ENTRY_MAX_SIZE));
            throw DcCoreException.Server.UNKNOWN_ERROR;
        }
        return maxBarFileSize;
    }

    /**
     * barファイル内のメタデータを1件読み込み、登録する.
     * @param zae ZipArchiveEntry
     * @param entryName barファイル内エントリ名
     * @param maxSize エントリの最大ファイルサイズ(MB)
     * @param keyList 定義ファイル一覧
     * @param doneKeys 実行＆実行済定義ファイル
     * @return boolean 処理成功可否
     */
    protected boolean createMetadata(
            ZipArchiveEntry zae,
            String entryName,
            long maxSize,
            Set<String> keyList,
            List<String> doneKeys) {
        if (!isValidFileStructure(zae, entryName, maxSize, doneKeys)) {
            return false;
        }

        if (getFileExtension(entryName).equals(".xml")) {
            // XMLファイルの場合
            String boxUrl = this.box.getCell().getUrl() + this.box.getName();
            if (!registXmlEntry(entryName, this.zipArchiveInputStream, boxUrl)) {
                doneKeys.add(entryName);
                return false;
            }
            writeOutputStream(false, "PL-BI-1003", entryName);
            doneKeys.add(entryName);
            return true;
        }

        if (getFileExtension(entryName).equals(".json")) {
            // JSONファイルの場合
            if (!registJsonEntry(entryName, this.zipArchiveInputStream)) {
                return false;
            }
            writeOutputStream(false, "PL-BI-1003", entryName);
            doneKeys.add(entryName);
            return true;
        }

        if (entryName.endsWith("/")) {
            return true;
        }
        return false;
    }

    /**
     * barファイル内のコンテンツデータ(bar/90_contents)を1件読み込み、登録する.
     * @return boolean 処理成功可否
     */
    protected boolean createContents() {
        boolean isSuccess = true;
        // CollectionタイプごとのMapを作成しておく
        Map<String, DavCmp> odataCols = getCollections(DavCmp.TYPE_COL_ODATA);
        Map<String, DavCmp> webdavCols = getCollections(DavCmp.TYPE_COL_WEBDAV);
        Map<String, DavCmp> serviceCols = getCollections(DavCmp.TYPE_COL_SVC);

        DavCmp davCmp = null;
        List<String> doneKeys = new ArrayList<String>();
        try {
            ZipArchiveEntry zae = null;
            String currentPath = null;
            int userDataCount = 0;
            List<JSONMappedObject> userDataLinks = new ArrayList<JSONMappedObject>();
            LinkedHashMap<String, BulkRequest> bulkRequests = new LinkedHashMap<String, BulkRequest>();
            Map<String, String> fileNameMap = new HashMap<String, String>();
            DcODataProducer producer = null;

            while ((zae = this.zipArchiveInputStream.getNextZipEntry()) != null) {
                String entryName = zae.getName();
                log.debug("Entry Name: " + entryName);
                log.debug("Entry Size: " + zae.getSize());
                log.debug("Entry Compressed Size: " + zae.getCompressedSize());
                if (!zae.isDirectory()) {
                    this.progressInfo.addDelta(1L);
                }
                writeOutputStream(false, CODE_BAR_INSTALL_STARTED, entryName);

                // ODataCollectionからDav/ServiceCollection/別ODataCollectionのリソースに対する処理に変わった際に
                // ユーザデータの登録やリンクの登録をする必要があれば、処理を実行する
                if (currentPath != null && !entryName.startsWith(currentPath)) {
                    if (!execBulkRequest(davCmp.getCell().getId(), bulkRequests, fileNameMap, producer)) {
                        return false;
                    }
                    if (!createUserdataLinks(producer, userDataLinks)) {
                        return false;
                    }
                    userDataLinks = new ArrayList<JSONMappedObject>();
                    currentPath = null;
                }
                int entryType = getEntryType(entryName, odataCols, webdavCols, serviceCols, this.davFileMap);
                switch (entryType) {
                case TYPE_ODATA_COLLECTION:
                    // ODataコレクションの登録
                    if (!odataCols.isEmpty()) {
                        if (!isValidODataContents(entryName, odataCols, doneKeys)) {
                            return false;
                        }
                        Pattern formatPattern = Pattern.compile(CONTENTS_DIR + ".+/90_data/");
                        Matcher formatMatcher = formatPattern.matcher(entryName);
                        if (formatMatcher.matches()) {
                            currentPath = entryName;
                        }
                        Pattern userodataDirPattern = Pattern.compile(CONTENTS_DIR + ".+/90_data/.+");
                        Matcher userodataDirMatcher = userodataDirPattern.matcher(entryName);

                        if (getFileExtension(entryName).equals(".xml")) {
                            // 00_$metadata.xmlの解析・ユーザスキーマ登録
                            davCmp = getCollection(entryName, odataCols);
                            // ODataのコレクションが切り替わった場合にプロデューサーを更新する
                            producer = davCmp.getODataProducer();
                            if (!registUserSchema(entryName, this.zipArchiveInputStream, davCmp)) {
                                doneKeys.add(entryName);
                                return false;
                            }
                            writeOutputStream(false, "PL-BI-1003", entryName);
                            doneKeys.add(entryName);
                            continue;
                        } else if (entryName.endsWith(USERDATA_LINKS_JSON)) {
                            userDataLinks = registJsonLinksUserdata(entryName, this.zipArchiveInputStream);
                            if (userDataLinks == null) {
                                doneKeys.add(entryName);
                                return false;
                            }
                            writeOutputStream(false, "PL-BI-1003", entryName);
                            doneKeys.add(entryName);
                            continue;
                        } else if (userodataDirMatcher.matches() && getFileExtension(entryName).equals(".json")) {
                            userDataCount++;
                            if (!setBulkRequests(entryName, producer, bulkRequests, fileNameMap)) {
                                return false;
                            }
                            doneKeys.add(entryName);

                            if ((userDataCount % bulkSize) == 0
                                    && !execBulkRequest(davCmp.getCell().getId(),
                                            bulkRequests, fileNameMap, producer)) {
                                return false;
                            }
                            continue;
                        } else if (!entryName.endsWith("/")) {
                            // xml,jsonファイル以外のファイルがあった場合はエラーを返却する
                            String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                            log.info(message + " [" + entryName + "]");
                            writeOutputStream(true, "PL-BI-1004", entryName, message);
                            return false;
                        }
                    }
                    break;

                case TYPE_DAV_FILE:
                    // WebDAVコレクションの登録
                    // bar/90_contents/{davcol_name}配下のエントリを1つずつ登録する
                    if (!registWebDavFile(entryName, this.zipArchiveInputStream, webdavCols)) {
                        return false;
                    }
                    break;

                case TYPE_SVC_FILE:
                    // Serviceコレクションの登録
                    if (!installSvcCollection(webdavCols, entryName)) {
                        return false;
                    }
                    break;

                case TYPE_MISMATCH:
                    // ODataコレクション配下ではなく、かつ、rootpropsに定義されていないエントリ
                    String message = DcCoreMessageUtils.getMessage("PL-BI-2006");
                    log.info(message + " [" + entryName + "]");
                    writeOutputStream(true, "PL-BI-1004", entryName, message);
                    return false;

                default:
                    break;
                }
                writeOutputStream(false, "PL-BI-1003", entryName);
                doneKeys.add(entryName);
            }

            // ODataCollectionのリソースに対する処理に終わった際に、ユーザデータの登録やリンクの登録をする必要があれば実行する
            if (currentPath != null) {
                if (!execBulkRequest(davCmp.getCell().getId(), bulkRequests, fileNameMap, producer)) {
                    return false;
                }
                if (!createUserdataLinks(producer, userDataLinks)) {
                    return false;
                }
                userDataLinks = null;
            }
        } catch (IOException ex) {
            isSuccess = false;
            log.info("IOException: " + ex.getMessage(), ex.fillInStackTrace());
            String message = DcCoreMessageUtils.getMessage("PL-BI-2000");
            writeOutputStream(true, CODE_BAR_INSTALL_FAILED, "", message);

        }
        // 必須データ（bar/90_contents/{odatacol_name}/00_$metadata.xml)の確認
        isSuccess = checkNecessaryFile(isSuccess, odataCols, doneKeys);
        return isSuccess;
    }

    private boolean checkNecessaryFile(boolean isSuccess, Map<String, DavCmp> odataCols, List<String> doneKeys) {
        Set<String> colList = odataCols.keySet();
        for (String colName : colList) {
            String filename = colName + METADATA_XML;
            if (!doneKeys.contains(filename)) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", filename, message);
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    private boolean installSvcCollection(Map<String, DavCmp> webdavCols, String entryName) {
        // bar/90_contents/{svccol_name}配下のエントリを1つずつWebDAV/サービスとして登録する
        // {serviceCollection}/{scriptName}を{serviceCollection}/__src/{scriptName}に変換
        int lastSlashIndex = entryName.lastIndexOf("/");
        StringBuilder serviceSrcName = new StringBuilder();
        serviceSrcName.append(entryName.substring(0, lastSlashIndex));
        serviceSrcName.append("/__src");
        serviceSrcName.append(entryName.substring(lastSlashIndex));

        if (!registWebDavFile(serviceSrcName.toString(), this.zipArchiveInputStream, webdavCols)) {
            return false;
        }
        return true;
    }

    private boolean setBulkRequests(String entryName,
            DcODataProducer producer,
            LinkedHashMap<String, BulkRequest> bulkRequests,
            Map<String, String> fileNameMap) {
        BulkRequest bulkRequest = new BulkRequest();
        String key = DcUUID.randomUUID();
        try {
            // entityType名を取得する
            String entityTypeName = getEntityTypeName(entryName);
            if (producer.getMetadata().findEdmEntitySet(entityTypeName) == null) {
                throw DcCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            // ZipArchiveImputStreamからユーザデータのJSONをStringReader形式で取得する
            StringReader stringReader = getStringReaderFromZais();

            // リクエストボディを生成する
            ODataResource odataResource = odataEntityResource.getOdataResource();
            ODataEntitiesResource resource = new ODataEntitiesResource(odataResource, entityTypeName);
            OEntity oEntity = resource.getOEntityWrapper(stringReader, odataResource, producer.getMetadata());

            UserDataODataProducer userDataProducer = (UserDataODataProducer) producer;
            EntitySetDocHandler docHandler = producer.getEntitySetDocHandler(entityTypeName, oEntity);
            String docType = UserDataODataProducer.USER_ODATA_NAMESPACE;
            docHandler.setType(docType);
            docHandler.setEntityTypeId(userDataProducer.getEntityTypeId(oEntity.getEntitySetName()));

            odataEntityResource.setOdataProducer(userDataProducer);

            // データ内でのID競合チェック
            // TODO 複合主キー対応、ユニークキーのチェック、NTKP対応
            key = oEntity.getEntitySetName() + ":" + (String) docHandler.getStaticFields().get("__id");

            if (bulkRequests.containsKey(key)) {
                throw DcCoreException.OData.ENTITY_ALREADY_EXISTS;
            }

            // ID指定がない場合はUUIDを払い出す
            if (docHandler.getId() == null) {
                docHandler.setId(DcUUID.randomUUID());
            }
            bulkRequest.setEntitySetName(entityTypeName);
            bulkRequest.setDocHandler(docHandler);
        } catch (Exception e) {
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info(entryName + " : " + e.getMessage());
            bulkRequest.setError(e);
            return false;
        }
        bulkRequests.put(key, bulkRequest);
        fileNameMap.put(key, entryName);
        return true;
    }

    private StringReader getStringReaderFromZais() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(this.zipArchiveInputStream, "UTF-8"));
        StringBuffer buf = new StringBuffer();
        String str = null;
        while ((str = bufferedReader.readLine()) != null) {
            buf.append(str);
        }
        StringReader stringReader = new StringReader(buf.toString());
        return stringReader;
    }

    private String getEntityTypeName(String entryName) {
        String[] hierarchy = entryName.split("/");
        int size = hierarchy.length;
        String entityTypeName = hierarchy[size - 2];
        return entityTypeName;
    }

    private boolean execBulkRequest(String cellId, LinkedHashMap<String, BulkRequest> bulkRequests,
            Map<String, String> fileNameMap,
            DcODataProducer producer) {
        // バルクで一括登録を実行
        producer.bulkCreateEntity(producer.getMetadata(), bulkRequests, cellId);

        // レスポンスのチェック
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            // エラーが発生していた場合はエラーのレスポンスを返却する
            if (request.getValue().getError() != null) {
                if (request.getValue().getError() instanceof DcCoreException) {
                    DcCoreException e = ((DcCoreException) request.getValue().getError());
                    writeOutputStream(true, "PL-BI-1004", fileNameMap.get(request.getKey()), e.getMessage());
                    log.info("DcCoreException: " + e.getMessage());
                } else {
                    Exception e = request.getValue().getError();
                    String message = DcCoreMessageUtils.getMessage("PL-BI-2003");
                    writeOutputStream(true, "PL-BI-1004", fileNameMap.get(request.getKey()), message);
                    log.info("Regist Entity Error: " + e.toString());
                    log.info("Regist Entity Error: " + e.getClass().getName());
                    log.info("Regist Entity Error: " + e);
                }
                return false;
            }
            writeOutputStream(false, "PL-BI-1003", fileNameMap.get(request.getKey()));
        }

        bulkRequests.clear();
        fileNameMap.clear();
        return true;
    }

    /**
     * barファイルの90_contents配下のエントリのタイプを取得する.
     * @param entryName barファイルのエントリ名
     * @param odataCols ODataコレクションの一覧
     * @param webdavCols WebDAVコレクションの一覧
     * @param serviceCols サービスコレクションの一覧
     * @param davFiles WebDAVファイルの一覧
     * @return エントリのタイプ
     */
    protected int getEntryType(String entryName,
            Map<String, DavCmp> odataCols,
            Map<String, DavCmp> webdavCols,
            Map<String, DavCmp> serviceCols,
            Map<String, String> davFiles) {

        if (odataCols.containsKey(entryName)) {
            return TYPE_ODATA_COLLECTION;
        } else if (webdavCols.containsKey(entryName)) {
            return TYPE_WEBDAV_COLLECTION;
        } else if (serviceCols.containsKey(entryName)) {
            return TYPE_SERVICE_COLLECTION;
        } else if (davFiles.containsKey(entryName)) {
            return TYPE_DAV_FILE;
        }

        for (Entry<String, DavCmp> entry : odataCols.entrySet()) {
            String odataColPath = entry.getKey();
            if (entryName.startsWith(odataColPath)) {
                return TYPE_ODATA_COLLECTION;
            }
        }

        for (Entry<String, DavCmp> entry : serviceCols.entrySet()) {
            String serviceColPath = entry.getKey();
            if (entryName.startsWith(serviceColPath)) {
                return TYPE_SVC_FILE;
            }
        }

        return TYPE_MISMATCH;
    }

    /**
     * WebDAVファイルの登録を行う.
     * @param entryName barファイルのエントリ名
     * @param inputStream データ
     * @param webdavCols WebDAVコレクション一覧
     * @return true: 登録成功、false:登録失敗
     */
    protected boolean registWebDavFile(String entryName, InputStream inputStream,
            Map<String, DavCmp> webdavCols) {

        // 登録先のファイルパス・コレクション名を取得
        String filePath = entryName.replaceAll(CONTENTS_DIR, "");
        String colPath = entryName.substring(0, entryName.lastIndexOf("/") + 1);

        // DavCmp作成
        DavCmp parentCmp = webdavCols.get(colPath);

        // 親コレクション内のコレクション・ファイル数のチェック
        int maxChildResource = DcCoreConfig.getMaxChildResourceCount();
        if (parentCmp.getChildrenCount() >= maxChildResource) {
            // コレクション内に作成可能なコレクション・ファイル数の制限を超えたため、エラーとする
            String message = DcCoreMessageUtils.getMessage("PR400-DV-0007");
            log.info(message);
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        // 新しいノードを作成

        // 親ノードにポインタを追加
        String fileName = "";
        fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

        // 実装依存排除
        DavCmp fileCmp = parentCmp.getChild(fileName);

        // Content-Typeのチェック
        String contentType = null;
        try {
            contentType = this.davFileMap.get(entryName);
            RuntimeDelegate.getInstance().createHeaderDelegate(MediaType.class).fromString(contentType);
        } catch (Exception e) {
            String message = DcCoreMessageUtils.getMessage("PL-BI-2005");
            log.info(message + ": " + e.getMessage(), e.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        // ファイル登録
        try {
            fileCmp.putForCreate(contentType, new CloseShieldInputStream(inputStream));
        } catch (Exception e) {
            String message = DcCoreMessageUtils.getMessage("PL-BI-2004");
            log.info(message + ": " + e.getMessage(), e.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        return true;
    }

    /**
     * ファイル名の拡張子を返す.
     * @param filename ファイル名
     */
    private String getFileExtension(String filename) {
        String extension = "";
        int idx = filename.lastIndexOf(".");
        if (idx >= 0) {
            extension = filename.substring(idx);
        }
        return extension;
    }

    /**
     * 90_rootprops_xmlを解析してCollectoin/ACL/WebDAV等の登録処理を行う.
     * @param rootPropsName 90_rootprops_xmlのbarファイル内パス名
     * @param inputStream 入力ストリーム
     * @param boxUrl boxのURL
     * @return 正常終了した場合はtrue
     */
    protected boolean registXmlEntry(String rootPropsName, InputStream inputStream, String boxUrl) {
        // XMLパーサ(StAX,SAX,DOM)にInputStreamをそのまま渡すとファイル一覧の取得処理が
        // 中断してしまうため暫定対処としてバッファに格納してからパースする
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                buf.append(str);
            }

            Multistatus multiStatus = Multistatus.unmarshal(new ByteArrayInputStream(buf.toString().getBytes()));

            // 90_rootprops.xmlの定義内容について妥当性検証を行う。
            // 事前に検証することで、ゴミデータが作られないようにする。
            if (!validateCollectionDefinitions(multiStatus, rootPropsName)) {
                return false;
            }
            for (Response response : multiStatus.getResponse()) {
                int collectionType = TYPE_WEBDAV_COLLECTION;
                boolean hasCollection = false;
                boolean isBox = false;

                List<String> hrefs = response.getHref();
                String href = hrefs.get(0);
                if (href.equals("dcbox:")) {
                    href = DCBOX;
                }
                if (href.equals(DCBOX)) {
                    isBox = true;
                }
                String collectionUrl = null;
                collectionUrl = href.replaceFirst(DCBOX, boxUrl + "/");

                List<Element> propElements = new ArrayList<Element>();
                Element aclElement = null;
                String contentType = null;
                for (Propstat propstat : response.getPropstat()) {
                    Prop prop = propstat.getProp();
                    Resourcetype resourceType = prop.getResourcetype();
                    if (resourceType != null) {
                        if (resourceType.getCollection() != null) {
                            hasCollection = true;
                        }
                        List<Element> elements = resourceType.getAny();
                        for (Element element : elements) {
                            String nodeName = element.getNodeName();
                            if (nodeName.equals("dc:odata")) {
                                collectionType = TYPE_ODATA_COLLECTION;
                            } else if (nodeName.equals("dc:service")) {
                                collectionType = TYPE_SERVICE_COLLECTION;
                            }
                        }
                    }
                    // prop配下確認
                    Getcontenttype getContentType = prop.getGetcontenttype();
                    if (getContentType != null) {
                        contentType = getContentType.getValue();
                    }

                    List<Element> pElements = prop.getAny();
                    for (Element element : pElements) {
                        String nodeName = element.getNodeName();
                        if (nodeName.equals("creationdate")
                                || nodeName.equals("getlastmodified")
                                || nodeName.equals("resourcetype")) {
                            continue;
                        }
                        if (nodeName.equals("acl")) {
                            if (!BarFileUtils.aclNameSpaceValidate(rootPropsName, element, this.schemaUrl)) {
                                String message = DcCoreMessageUtils.getMessage("PL-BI-2007");
                                log.info(message + " [" + rootPropsName + "]");
                                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                                return false;
                            }
                            aclElement = element;
                            continue;
                        }
                        propElements.add(element);
                    }
                }

                String entryName = CONTENTS_DIR + href.replaceFirst(DCBOX, "");
                if (isBox) {
                    // Boxの場合、ACL登録
                    registBoxAclAndProppatch(this.box, aclElement, propElements, collectionUrl);
                } else if (hasCollection) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    // コレクションの場合、コレクション、ACL、PROPPATH登録
                    log.info(entryName);
                    createCollection(collectionUrl, entryName, this.cell, this.box, collectionType, aclElement,
                            propElements);
                } else {
                    // WebDAVファイル
                    this.davFileMap.put(entryName, contentType);
                }
            }
        } catch (DcCoreException e) {
            log.info("DcCoreException: " + e.getMessage());
            writeOutputStream(true, "PL-BI-1004", rootPropsName, e.getMessage());
            return false;
        } catch (Exception ex) {
            String message = getErrorMessage(ex);
            log.info("XMLParseException: " + message, ex.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
            return false;
        }
        return true;
    }

    /**
     * 90_rootprops.xmlに定義されたpathの階層構造に矛盾がないことを検証する.
     * @param multiStatus 90_rootprops.xmlから読み込んだJAXBオブジェクト
     * @param rootPropsName 現在処理中のエントリ名(ログ出力用)
     * @return 矛盾がない場合はtrueを、矛盾がある場合はfalseを返す。
     */
    protected boolean validateCollectionDefinitions(Multistatus multiStatus, String rootPropsName) {

        // XML定義を読み込んで、href要素のパス定義とタイプ（ODataコレクション/WebDAVコレクション/サービスコレクション、WebDAVファイル、サービスソース)を取得する。
        Map<String, Integer> pathMap = new LinkedHashMap<String, Integer>();
        for (Response response : multiStatus.getResponse()) {
            List<String> hrefs = response.getHref();
            if (hrefs.size() != 1) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2008");
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            String href = hrefs.get(0);
            // href属性値がない場合は定義エラーとみなす。
            if (href == null || href.length() == 0) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2009");
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            // href属性値として"dcbox:/" で始まらない場合は定義エラーとみなす。
            if (!href.startsWith(DCBOX_NO_SLUSH)) {
                String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2010"), href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            // 定義されたパスの種別を選別する。不正なパス種別が指定された場合は異常終了する（ログ出力は不要）。
            int collectionType = getCollectionType(rootPropsName, response);
            switch (collectionType) {
            case TYPE_WEBDAV_COLLECTION:
            case TYPE_ODATA_COLLECTION:
            case TYPE_SERVICE_COLLECTION:
                if (href.endsWith("/")) {
                    href = href.substring(0, href.length() - 1);
                }
                break;
            case TYPE_MISMATCH:
                return false;
            default:
                break;
            }
            // パス定義が重複している場合は同じデータが登録されてしまうため定義エラーとする。
            // パス末尾の"/"指定有無の条件を無視するため、このタイミングでチェックする。
            if (pathMap.containsKey(href)) {
                String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2011"), href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            pathMap.put(href, Integer.valueOf(collectionType));
        }
        // 読み込んだパス定義をもとにCollectionパスの妥当性を検証する。
        // ・共通：Boxルートの定義は必須とする
        // ・共通： パス階層構造に矛盾がないこと
        // ・ODataコレクションの場合： コレクション配下のパス定義が存在しないこと
        // ・Serviceコレクションの場合： コレクション配下に "__src" のパス定義が存在すること
        Set<String> keySet = pathMap.keySet();
        for (Entry<String, Integer> entry : pathMap.entrySet()) {
            String href = entry.getKey();
            int currentCollectionType = entry.getValue();
            int upperPathposition = href.lastIndexOf("/");
            if (upperPathposition < 0) { // "dcbox:"のパスはチェック対象外のためスキップする
                continue;
            }
            // チェック対象の上位階層がパス情報として定義されていない場合は定義エラーとする。
            // Boxルートパスが定義されていない場合も同様に定義エラーとする。
            String upper = href.substring(0, upperPathposition);
            if (!keySet.contains(upper)) {
                String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2012"), upper);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            int upperCollectionType = pathMap.get(upper);
            String resourceName = href.substring(upperPathposition + 1, href.length());
            if (upperCollectionType == TYPE_ODATA_COLLECTION) {
                // ODataコレクション：コレクション配下にコレクション／ファイルが定義されていた場合は定義エラーとする。
                String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2013"), href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            } else if (upperCollectionType == TYPE_SERVICE_COLLECTION) {
                // Serviceコレクション：コレクション配下にコレクション／ファイルが定義されていた場合は定義エラーとする。
                // ただし、"__src"のみは例外として除外する。
                if (!("__src".equals(resourceName) && currentCollectionType == TYPE_WEBDAV_COLLECTION)) {
                    String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2014"), href);
                    writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                    return false;
                }
            } else if (upperCollectionType == TYPE_DAV_FILE) {
                // WebDAVファイル／Serviceソース配下にコレクション／ファイルが定義されていた場合は定義エラーとする。
                String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2015"), href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            // カレントがServiceコレクションの場合、直下のパスに"__src"が定義されていない場合は定義エラーとする。
            if (currentCollectionType == TYPE_SERVICE_COLLECTION) {
                String srcPath = href + "/__src";
                if (!keySet.contains(srcPath) || pathMap.get(srcPath) != TYPE_WEBDAV_COLLECTION) {
                    String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2016"), href);
                    writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                    return false;
                }
            }

            // リソース名として正しいことを確認する（コレクション／ファイルの名前フォーマットは共通）。
            if (!DavCommon.isValidResourceName(resourceName)) {
                String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2017"), resourceName);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
        }
        return true;
    }

    /**
     * 90_rootprops.xml内の各responseタグに定義されているパスのコレクション種別を取得する。
     * @param rootPropsName 現在処理中のエントリ名(ログ出力用)
     * @param response 処理対象のresponseタグ用JAXBオブジェクト
     * @return 定義内容に応じたコレクション種別の値を返す。
     *         WebDAVファイル、ServiceソースはWebDAVファイルとして返す。
     *         許可されていないコレクションの種別が定義されていた場合は未定義として返す。
     */
    private int getCollectionType(String rootPropsName, Response response) {
        // <propstat>要素の配下を辿って定義されているコレクションのタイプを取得する
        // －prop/resourcetype/collecton のDOMノードパスが存在する場合はコレクション定義とみなす
        // この際、"dc:odata" または "dc:service" のDOMノードパスが存在しない場合はWebDAVコレクション定義とみなす
        // - 上記に当てはまらない場合はWebDAvファイルまたはサービスソースとみなす
        for (Propstat propstat : response.getPropstat()) {
            Prop prop = propstat.getProp();
            Resourcetype resourceType = prop.getResourcetype();
            if (resourceType != null && resourceType.getCollection() != null) {
                List<Element> elements = resourceType.getAny();
                for (Element element : elements) {
                    String nodeName = element.getNodeName();
                    if (nodeName.equals("dc:odata")) {
                        return TYPE_ODATA_COLLECTION;
                    } else if (nodeName.equals("dc:service")) {
                        return TYPE_SERVICE_COLLECTION;
                    } else {
                        String message = MessageFormat.format(DcCoreMessageUtils.getMessage("PL-BI-2018"), nodeName);
                        writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                        return TYPE_MISMATCH;
                    }
                }
            } else {
                return TYPE_DAV_FILE;
            }
        }
        return TYPE_WEBDAV_COLLECTION;
    }

    /**
     * JSONのデータを１件処理する.
     * @param entryName 対象ファイル名
     * @param inputStream 入力ストリーム
     * @return 正常終了した場合はtrue
     */
    private boolean registJsonEntry(String entryName, InputStream inputStream) {
        JsonParser jp = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new JsonFactory();
        try {
            jp = f.createJsonParser(inputStream);
            JsonToken token = jp.nextToken(); // JSONルート要素（"{"）
            Pattern formatPattern = Pattern.compile(".*/+(.*)");
            Matcher formatMatcher = formatPattern.matcher(entryName);
            String jsonName = formatMatcher.replaceAll("$1");

            if (token == JsonToken.START_OBJECT) {
                if (jsonName.equals(RELATION_JSON) || jsonName.equals(ROLE_JSON)
                        || jsonName.equals(EXTROLE_JSON) || jsonName.equals(LINKS_JSON)) {
                    registJsonEntityData(jp, mapper, jsonName);
                } else if (jsonName.equals(MANIFEST_JSON)) {
                    manifestJsonValidate(jp, mapper); // Boxはインストールの最初に作成
                }
                log.debug(jsonName);
            } else {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
        } catch (DcCoreException e) {
            // JSONファイルのバリデートエラー
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info("DcCoreException" + e.getMessage(), e.fillInStackTrace());
            return false;
        } catch (JsonParseException e) {
            // JSONファイルの解析エラー
            String message = DcCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonParseException: " + e.getMessage(), e.fillInStackTrace());
            return false;
        } catch (JsonMappingException e) {
            // JSONファイルのデータ定義エラー
            String message = DcCoreMessageUtils.getMessage("PL-BI-2003");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonMappingException: " + e.getMessage(), e.fillInStackTrace());
            return false;
        } catch (Exception e) {
            String message = DcCoreMessageUtils.getMessage("PL-BI-2000");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("Exception: " + e.getMessage(), e.fillInStackTrace());
            return false;
        }
        return true;
    }

    /**
     * 10_odatarelations.jsonのデータを読み込みユーザデータのLink情報を生成する.
     * @param entryName 対象ファイル名
     * @param inputStream 入力ストリーム
     * @return 正常終了した場合はtrue
     */
    protected List<JSONMappedObject> registJsonLinksUserdata(String entryName, InputStream inputStream) {
        List<JSONMappedObject> userDataLinks = new ArrayList<JSONMappedObject>();
        JsonParser jp = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new JsonFactory();
        try {
            jp = f.createJsonParser(inputStream);
            JsonToken token = jp.nextToken(); // JSONルート要素（"{"）

            if (token == JsonToken.START_OBJECT) {
                token = jp.nextToken();

                // $linksのチェック
                checkMatchFieldName(jp, USERDATA_LINKS_JSON);

                token = jp.nextToken();
                // 配列でなければエラー
                if (token != JsonToken.START_ARRAY) {
                    throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(USERDATA_LINKS_JSON);
                }
                token = jp.nextToken();

                while (jp.hasCurrentToken()) {
                    if (token == JsonToken.END_ARRAY) {
                        break;
                    } else if (token != JsonToken.START_OBJECT) {
                        throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(USERDATA_LINKS_JSON);
                    }
                    userDataLinks.add(barFileJsonValidate(jp, mapper, USERDATA_LINKS_JSON));

                    token = jp.nextToken();
                }
            } else {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(USERDATA_LINKS_JSON);
            }
        } catch (JsonParseException e) {
            // JSONファイルの解析エラー
            String message = DcCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonParseException: " + e.getMessage(), e.fillInStackTrace());
            return null;
        } catch (JsonMappingException e) {
            // JSONファイルのデータ定義エラー
            String message = DcCoreMessageUtils.getMessage("PL-BI-2003");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonMappingException: " + e.getMessage(), e.fillInStackTrace());
            return null;
        } catch (DcCoreException e) {
            // JSONファイルのバリデートエラー
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info("DcCoreException" + e.getMessage(), e.fillInStackTrace());
            return null;
        } catch (IOException e) {
            String message = DcCoreMessageUtils.getMessage("PL-BI-2000");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("IOException: " + e.getMessage(), e.fillInStackTrace());
            return null;
        }
        return userDataLinks;
    }

    /**
     * メタデータディレクトリ内のファイル構成が正しいかチェックする.
     * @param zae
     * @param entryName
     * @param maxSize
     * @param doneKeys
     * @return 正しい場合trueを返却
     */
    private boolean isValidFileStructure(ZipArchiveEntry zae,
            String entryName,
            long maxSize,
            List<String> doneKeys) {
        writeOutputStream(false, CODE_BAR_INSTALL_STARTED, entryName);

        // 不正なファイルでないかをチェック
        if (!barFileOrder.containsKey(entryName)) {
            log.info("[" + entryName + "] invalid file");
            String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        // 順番が正しいかチェック
        Pattern formatPattern = Pattern.compile(".*/+([0-9][0-9])_.*");
        Matcher formatMatcher = formatPattern.matcher(entryName);
        String entryIndex = formatMatcher.replaceAll("$1");
        if (doneKeys.isEmpty()) {
            // 最初のエントリの場合は"00"であることが必須
            if (!entryIndex.equals("00")) {
                log.info("bar/00_meta/00_manifest.json is not exsist");
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        } else {
            String lastEntryName = doneKeys.get(doneKeys.size() - 1);
            formatMatcher = formatPattern.matcher(lastEntryName);
            String lastEntryIndex = formatMatcher.replaceAll("$1");

            // 前回処理したエントリのプレフィックスと比較
            if (entryIndex.compareTo(lastEntryIndex) < 0) {
                log.info("[" + entryName + "] invalid file");
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }

        // [400]barファイル/barファイル内エントリのファイルサイズが上限値を超えている
        if (zae.getSize() > (long) (maxSize * MB)) {
            log.info("Bar file entry size too large invalid file [" + entryName + "]");
            String message = DcCoreException.BarInstall.BAR_FILE_ENTRY_SIZE_TOO_LARGE
                    .params(zae.getName(), String.valueOf(zae.getSize())).getMessage();
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }
        return true;
    }

    /**
     * bar/90_contents/{OdataCol_name}配下のエントリが正しい定義であるかどうかを確認する.
     * @param entryName エントリ名(コレクション名)
     * @param colMap コレクションのMapオブジェクト
     * @param doneKeys 処理済みのODataコレクション用エントリリスト
     * @return 判定処理結果
     */
    protected boolean isValidODataContents(String entryName, Map<String, DavCmp> colMap, List<String> doneKeys) {

        String odataColPath = "";
        for (Map.Entry<String, DavCmp> entry : colMap.entrySet()) {
            if (entryName.startsWith(entry.getKey())) {
                odataColPath = entry.getKey();
                break;
            }
        }

        // ODataコレクション直下からのエントリー名
        String odataPath = entryName.replaceAll(odataColPath, "");

        // bar/90_contents/{OData_collection}直下の順序チェック
        if (USERDATA_LINKS_JSON.equals(odataPath)) {

            // 00_$metadata.xmlの処理が済んでいるかのチェック
            String meatadataPath = odataColPath + METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
            // 90_data/の処理が済んでいないかのチェック
            String userDataPath = odataColPath + USERDATA_DIR_NAME + "/";
            if (doneKeys.contains(userDataPath)) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }
        if (odataPath.startsWith(USERDATA_DIR_NAME + "/")) {
            // 00_$metadata.xmlの処理が済んでいるかのチェック
            String meatadataPath = odataColPath + METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }

        // bar/90_contents/{OData_collection}/{dirPath}/のチェック
        String dirPath = null;
        Pattern pattern = Pattern.compile("^([^/]+)/.*");
        Matcher m = pattern.matcher(odataPath);
        if (m.matches()) {
            dirPath = m.replaceAll("$1");
        }
        if (dirPath != null && !dirPath.equals(USERDATA_DIR_NAME)) {
            // bar/90_contents/{OData_collection}/{dir}/の場合はエラーとする
            String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
            log.info(message + "entryName: " + entryName);
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        // bar/90_contents/{OData_collection}/90_data/{entity}/{1.json}のチェック
        String fileName = null;
        pattern = Pattern.compile(".*/([^/]+)$");
        m = pattern.matcher(odataPath);
        if (m.matches()) {
            fileName = m.replaceAll("$1");
        }
        if (fileName != null) {
            pattern = Pattern.compile("^([0-9]+).json$");
            m = pattern.matcher(fileName);
            if (!m.matches()) {
                // bar/90_contents/{OData_collection}/{dir}/の場合はエラーとする
                String message = DcCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }

        return true;
    }

    /**
     * 10_relations.json, 20_roles.json, 30_extroles.json, 70_$links.json, 10_odatarelations.jsonのバリデートチェック.
     * @param jp Jsonパース
     * @param mapper ObjectMapper
     * @param jsonName ファイル名
     * @throws IOException IOException
     */
    protected void registJsonEntityData(JsonParser jp, ObjectMapper mapper, String jsonName) throws IOException {
        JsonToken token;
        token = jp.nextToken();

        // Relations,Roles,ExtRoles,$linksのチェック
        checkMatchFieldName(jp, jsonName);

        token = jp.nextToken();
        // 配列でなければエラー
        if (token != JsonToken.START_ARRAY) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
        token = jp.nextToken();

        while (jp.hasCurrentToken()) {
            if (token == JsonToken.END_ARRAY) {
                break;
            } else if (token != JsonToken.START_OBJECT) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }

            // 1件登録処理
            JSONMappedObject mappedObject = barFileJsonValidate(jp, mapper, jsonName);
            if (jsonName.equals(RELATION_JSON)) {
                createRelation(mappedObject.getJson());
            } else if (jsonName.equals(ROLE_JSON)) {
                createRole(mappedObject.getJson());
            } else if (jsonName.equals(EXTROLE_JSON)) {
                createExtRole(mappedObject.getJson());
            } else if (jsonName.equals(LINKS_JSON)) {
                createLinks(mappedObject, odataProducer);
            }

            token = jp.nextToken();
        }
    }

    /**
     * 必須項目のバリデートチェック.
     * @param jp Jsonパーサー
     * @param mapper ObjectMapper
     * @param jsonName ファイル名
     * @throws IOException IOException
     * @return JSONMappedObject JSONMappedオブジェクト
     */
    protected JSONMappedObject barFileJsonValidate(
            JsonParser jp, ObjectMapper mapper, String jsonName) throws IOException {
        if (jsonName.equals(EXTROLE_JSON)) {
            JSONExtRoles extRoles = mapper.readValue(jp, JSONExtRoles.class);
            if (extRoles.getExtRole() == null) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            if (extRoles.getRelationName() == null) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            return extRoles;
        } else if (jsonName.equals(ROLE_JSON)) {
            JSONRoles roles = mapper.readValue(jp, JSONRoles.class);
            if (roles.getName() == null) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            return roles;
        } else if (jsonName.equals(RELATION_JSON)) {
            JSONRelations relations = mapper.readValue(jp, JSONRelations.class);
            if (relations.getName() == null) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            return relations;
        } else if (jsonName.equals(LINKS_JSON)) {
            JSONLinks links = mapper.readValue(jp, JSONLinks.class);
            linksJsonValidate(jsonName, links);
            return links;
        } else if (jsonName.equals(USERDATA_LINKS_JSON)) {
            JSONUserDataLinks links = mapper.readValue(jp, JSONUserDataLinks.class);
            userDataLinksJsonValidate(jsonName, links);
            return links;
        }
        return null;
    }

    /**
     * 70_$links.jsonのバリデート.
     * @param jsonName JSONファイル名
     * @param links 読み込んだJSONオブジェクト
     */
    private void linksJsonValidate(String jsonName, JSONLinks links) {
        if (links.getFromType() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            if (!links.getFromType().equals(Relation.EDM_TYPE_NAME) && !links.getFromType().equals(Role.EDM_TYPE_NAME)
                    && !links.getFromType().equals(ExtRole.EDM_TYPE_NAME)) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
        }
        if (links.getFromName() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> fromNameMap = links.getFromName();
            for (Map.Entry<String, String> entry : fromNameMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
        if (links.getToType() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            if (!links.getToType().equals(Relation.EDM_TYPE_NAME)
                    && !links.getToType().equals(Role.EDM_TYPE_NAME)
                    && !links.getToType().equals(ExtRole.EDM_TYPE_NAME)) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
        }
        if (links.getToName() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> toNameMap = links.getToName();
            for (Map.Entry<String, String> entry : toNameMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
    }

    /**
     * 10_odatarelations.jsonのバリデート.
     * @param jsonName JSONファイル名
     * @param links 読み込んだJSONオブジェクト
     */
    private void userDataLinksJsonValidate(String jsonName, JSONUserDataLinks links) {
        if (links.getFromType() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
        if (links.getFromId() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> fromIdMap = links.getFromId();
            for (Map.Entry<String, String> entry : fromIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
        if (links.getToType() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
        if (links.getToId() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> toIdMap = links.getToId();
            for (Map.Entry<String, String> entry : toIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
    }

    /**
     * manifest.jsonのバリデート.
     * @param jp Jsonパーサー
     * @param mapper ObjectMapper
     * @return JSONManifestオブジェクト
     * @throws IOException データの読み込みに失敗した場合
     */
    protected JSONManifest manifestJsonValidate(JsonParser jp, ObjectMapper mapper) throws IOException {
        // TODO BARファイルのバージョンチェック
        JSONManifest manifest = null;
        try {
            manifest = mapper.readValue(jp, JSONManifest.class);
        } catch (UnrecognizedPropertyException ex) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params("manifest.json unrecognized property");
        }
        if (manifest.getBarVersion() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params("manifest.json#barVersion");
        }
        if (manifest.getBoxVersion() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params("manifest.json#boxVersion");
        }
        if (manifest.getDefaultPath() == null) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params("manifest.json#DefaultPath");
        }
        return manifest;
    }

    /**
     * フィールド名がファイルの形式に一致しているかの確認.
     * @param jp
     * @param jsonName
     * @throws IOException
     * @throws JsonParseException
     */
    private void checkMatchFieldName(JsonParser jp, String jsonName) throws IOException {
        String fieldName = jp.getCurrentName();
        if (!(fieldName.equals("Relations") && jsonName.equals(RELATION_JSON))
                && !(fieldName.equals("Roles") && jsonName.equals(ROLE_JSON))
                && !(fieldName.equals("ExtRoles") && jsonName.equals(EXTROLE_JSON))
                && !(fieldName.equals("Links") && jsonName.equals(LINKS_JSON))
                && !(fieldName.equals("Links") && jsonName.equals(USERDATA_LINKS_JSON))) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
    }

    /**
     * Httpレスポンス用メッセージの出力.
     * @param isError エラー時の場合はtrueを、それ以外はfalseを指定する.
     * @param code
     *        メッセージコード(dc-messages.propertiesに定義されたメッセージコード)
     * @param path
     *        処理対象リソースパス（ex. /bar/meta/roles.json)
     */
    private void writeOutputStream(boolean isError, String code, String path) {
        writeOutputStream(isError, code, path, "");
    }

    /**
     * barファイルインストールログ詳細の出力.
     * @param isError エラー時の場合はtrueを、それ以外はfalseを指定する.
     * @param code
     *        メッセージコード(dc-messages.propertiesに定義されたメッセージコード)
     * @param path
     *        処理対象リソースパス（ex. /bar/meta/roles.json)
     * @param detail
     *        処理失敗時の詳細情報(PL-BI-2xxx)
     */
    private void writeOutputStream(boolean isError, String code, String path, String detail) {
        String message = DcCoreMessageUtils.getMessage(code);
        if (detail == null) {
            message = message.replace("{0}", "");
        } else {
            message = message.replace("{0}", detail);
        }
        outputEventBus(isError, code, path, message);

        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
        log.info(output);
    }

    /**
     * 内部イベントとしてEventBusへインストール処理状況を出力する.
     * @param isError エラー時の場合はtrueを、それ以外はfalseを指定する.
     * @param code 処理コード（ex. PL-BI-0000）
     * @param path barファイル内のエントリパス（Edmxの場合は、ODataのパス）
     * @param message 出力用メッセージ
     */
    @SuppressWarnings("unchecked")
    private void outputEventBus(boolean isError, String code, String path, String message) {
        if (event != null) {
            event.setAction(code);
            event.setObject(path);
            event.setResult(message);
            eventBus.outputEventLog(event);
        }
        if (this.progressInfo != null && isError) {
            JSONObject messageJson = new JSONObject();
            JSONObject messageDetail = new JSONObject();
            messageJson.put("code", code);
            messageJson.put("message", messageDetail);
            messageDetail.put("lang", "en");
            messageDetail.put("value", message);
            this.progressInfo.setMessage(messageJson);
            writeToProgressCache(true);
        } else {
            writeToProgressCache(false);
        }
    }

    /**
     * キャッシュへbarインストール状況を出力する.
     * @param forceOutput 強制的に出力する場合はtrueを、それ以外はfalseを指定する
     */
    private void writeToProgressCache(boolean forceOutput) {
        if (this.progressInfo != null && this.progressInfo.isOutputEventBus() || forceOutput) {
            String key = "box-" + this.box.getId();
            Progress progress = new Progress(key, progressInfo.toString());
            ProgressManager.putProgress(key, progress);
            log.info("Progress(" + key + "): " + progressInfo.toString());
        }
    }

    private void setupBarFileOrder() {
        barFileOrder = new LinkedHashMap<String, Boolean>();
        barFileOrder.put("bar/00_meta/00_manifest.json", true);
        barFileOrder.put("bar/00_meta/10_relations.json", false);
        barFileOrder.put("bar/00_meta/20_roles.json", false);
        barFileOrder.put("bar/00_meta/30_extroles.json", false);
        barFileOrder.put("bar/00_meta/70_$links.json", false);
        barFileOrder.put("bar/00_meta/90_rootprops.xml", true);
        barFileOrder.put("bar/90_contents/", false); // dummy
    }

    private String createdBoxEtag = "";
    private String createdBoxName = "";

    /**
     * 作成したBoxのボックス名を返す.
     * @return ボックス名
     */
    public String getCreatedBoxName() {
        return createdBoxName;
    }

    /**
     * 作成したBoxのETagを返す.
     * @return ETag
     */
    public String getCreatedBoxETag() {
        return createdBoxEtag;
    }

    /**
     * Box情報をESへ登録する.
     * @param json JSONファイルから読み込んだJSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    void createBox(JSONObject json) {
        if (boxName == null || boxName.isEmpty()) {
            this.createdBoxName = (String) json.get("Name");
        } else {
            json.put("Name", boxName);
            this.createdBoxName = boxName;
        }
        StringReader stringReader = new StringReader(json.toJSONString());

        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        // Boxの登録
        odataProducer.
                createEntity(entitySetName, oew);
        this.createdBoxEtag = oew.getEtag();

        // Davの登録
        Box newBox = new Box(odataEntityResource.getAccessContext().getCell(), oew);
        this.boxCmp = ModelFactory.boxCmp(newBox);

        this.box = newBox;
        this.schemaUrl = (String) json.get("Schema");
    }

    /**
     * 10_$relations.jsonに定義されているRelation情報をESへ登録する.
     * @param json JSONファイルから読み込んだJSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createRelation(JSONObject json) {
        json.put("_Box.Name", createdBoxName);
        StringReader stringReader = new StringReader(json.toJSONString());

        odataEntityResource.setEntitySetName(Relation.EDM_TYPE_NAME);
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        // Relationの登録
        odataProducer.
                createEntity(Relation.EDM_TYPE_NAME, oew);
    }

    /**
     * 20_$roles.jsonに定義されているRole情報をESへ登録する.
     * @param json JSONファイルから読み込んだJSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createRole(JSONObject json) {
        json.put("_Box.Name", createdBoxName);
        StringReader stringReader = new StringReader(json.toJSONString());

        odataEntityResource.setEntitySetName(Role.EDM_TYPE_NAME);
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        // Roleの登録
        odataProducer.
                createEntity(Role.EDM_TYPE_NAME, oew);
    }

    /**
     * 30_$extroles.jsonに定義されているExtRole情報をESへ登録する.
     * @param json JSONファイルから読み込んだJSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createExtRole(JSONObject json) {
        String url = (String) json.get(ExtRole.EDM_TYPE_NAME);
        json.put(ExtRole.EDM_TYPE_NAME, url);
        json.put("_Relation._Box.Name", createdBoxName);
        StringReader stringReader = new StringReader(json.toJSONString());

        odataEntityResource.setEntitySetName(ExtRole.EDM_TYPE_NAME);
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        // ExtRoleの登録
        odataProducer.
                createEntity(ExtRole.EDM_TYPE_NAME, oew);
    }

    /**
     * 70_$links.jsonに定義されているリンク情報をESへ登録する.
     * @param mappedObject JSONファイルから読み込んだオブジェクト
     */
    private void createLinks(JSONMappedObject mappedObject, DcODataProducer producer) {
        Map<String, String> fromNameMap = ((JSONLinks) mappedObject).getFromName();
        String fromkey =
                BarFileUtils.getComplexKeyName(((JSONLinks) mappedObject).getFromType(), fromNameMap, this.boxName);
        OEntityKey fromOEKey = OEntityKey.parse(fromkey);
        OEntityId sourceEntity = OEntityIds.create(((JSONLinks) mappedObject).getFromType(), fromOEKey);
        String targetNavProp = ((JSONLinks) mappedObject).getNavPropToType();

        // リンク作成前処理
        odataEntityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

        Map<String, String> toNameMap = ((JSONLinks) mappedObject).getToName();
        String tokey =
                BarFileUtils.getComplexKeyName(((JSONLinks) mappedObject).getToType(), toNameMap, this.boxName);
        OEntityKey toOEKey = OEntityKey.parse(tokey);
        OEntityId newTargetEntity = OEntityIds.create(((JSONLinks) mappedObject).getToType(), toOEKey);
        // $linksの登録
        producer.createLink(sourceEntity, targetNavProp, newTargetEntity);
    }

    private boolean createUserdataLinks(DcODataProducer producer, List<JSONMappedObject> userDataLinks) {
        int linkSize = userDataLinks.size();
        int linkCount = 0;
        String message = DcCoreMessageUtils.getMessage("PL-BI-1002");
        for (JSONMappedObject json : userDataLinks) {
            linkCount++;
            if (!createUserdataLink(json, producer)) {
                return false;
            }
            if (linkCount % linksOutputStreamSize == 0) {
                writeOutputStream(false, "PL-BI-1002",
                        String.format("userDataLinks %d / %d", linkCount, linkSize), message);
            }
        }
        writeOutputStream(false, "PL-BI-1002",
                String.format("userDataLinks %d / %d", linkCount, linkSize), message);
        return true;
    }

    /**
     * 10_odatarelations.jsonに定義されているリンク情報をESへ登録する.
     * @param mappedObject JSONファイルから読み込んだオブジェクト
     */
    private boolean createUserdataLink(JSONMappedObject mappedObject, DcODataProducer producer) {
        OEntityId sourceEntity = null;
        OEntityId newTargetEntity = null;
        try {
            Map<String, String> fromId = ((JSONUserDataLinks) mappedObject).getFromId();
            String fromKey = "";
            for (Map.Entry<String, String> entry : fromId.entrySet()) {
                fromKey = String.format("('%s')", entry.getValue());
                break;
            }

            OEntityKey fromOEKey = OEntityKey.parse(fromKey);
            sourceEntity = OEntityIds.create(((JSONUserDataLinks) mappedObject).getFromType(), fromOEKey);
            String targetNavProp = ((JSONUserDataLinks) mappedObject).getNavPropToType();

            // リンク作成前処理
            odataEntityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

            Map<String, String> toId = ((JSONUserDataLinks) mappedObject).getToId();
            String toKey = "";
            for (Map.Entry<String, String> entry : toId.entrySet()) {
                toKey = String.format("('%s')", entry.getValue());
                break;
            }

            OEntityKey toOEKey = OEntityKey.parse(toKey);
            newTargetEntity = OEntityIds.create(((JSONUserDataLinks) mappedObject).getToType(), toOEKey);

            // $linksの登録
            producer.createLink(sourceEntity, targetNavProp, newTargetEntity);
        } catch (Exception e) {
            String path = "";
            String targetPath = "";
            if (sourceEntity != null) {
                path = sourceEntity.getEntitySetName() + sourceEntity.getEntityKey();
            }
            if (newTargetEntity != null) {
                targetPath = "Target Link to " + newTargetEntity.getEntitySetName() + newTargetEntity.getEntityKey();
            }
            log.info(e.getMessage() + " [" + path + "]", e.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", path, targetPath);
            return false;
        }
        return true;
    }

    private void createCollection(String collectionUrl,
            String entryName,
            Cell parentCell,
            Box parentBox,
            int collectionType,
            Element aclElement,
            List<Element> propElements) {
        int index;
        if (parentCell == null || parentBox == null) {
            return;
        }

        String type = "";
        switch (collectionType) {
        case TYPE_WEBDAV_COLLECTION:
            type = DavCmp.TYPE_COL_WEBDAV;
            break;
        case TYPE_ODATA_COLLECTION:
            type = DavCmp.TYPE_COL_ODATA;
            break;
        case TYPE_SERVICE_COLLECTION:
            type = DavCmp.TYPE_COL_SVC;
            break;
        default:
            break;
        }

        String parenEntryName = "";
        DavCmp parentCmp = null;
        String tmp = entryName.replace(CONTENTS_DIR, "/");
        String[] slash = tmp.split("/");
        if (slash.length == 2) {
            parentCmp = this.boxCmp;
        } else {
            index = entryName.lastIndexOf("/", entryName.length() - 2);
            parenEntryName = entryName.substring(0, index + 1);
            parentCmp = this.davCmpMap.get(parenEntryName);
            if (parentCmp == null) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
            } else if (parentCmp.getType().equals(DavCmp.TYPE_COL_ODATA)) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
            } else if (parentCmp.getType().equals(DavCmp.TYPE_COL_SVC)) {
                String crrName = entryName.substring(index + 1, entryName.length() - 1);
                if (!"__src".equals(crrName)) {
                    throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
                }
            }
        }
  //      String parentId = parentCmp.getId();

//        // コレクションの階層数のチェック
//        DavCmp current = parentCmp;
//
//        // currentがすでに親をさし示しているためdepthの初期値は1
//        int depth = 1;
//        int maxDepth = DcCoreConfig.getMaxCollectionDepth();
//        while (null != current.getParent()) {
//            current = (DavCmp) current.getParent();
//            depth++;
//        }
//        if (depth > maxDepth) {
//            // コレクション数の制限を超えたため、400エラーとする
//            throw DcCoreException.Dav.COLLECTION_DEPTH_ERROR;
//        }
//
//        // 親コレクション内のコレクション・ファイル数のチェック
//        int maxChildResource = DcCoreConfig.getMaxChildResourceCount();
//        if (parentCmp.getChildrenCount() >= maxChildResource) {
//            // コレクション内に作成可能なコレクション・ファイル数の制限を超えたため、400エラーとする
//            throw DcCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
//        }


//        // 親ノードにポインタを追加
        String collectionName = "";
        index = collectionUrl.lastIndexOf("/");
        collectionName = collectionUrl.substring(index + 1);

        DavCmp collectionCmp = parentCmp.getChild(collectionName);
        collectionCmp.mkcol(type);

        this.davCmpMap.put(entryName, collectionCmp);

        // ACL登録
        if (aclElement != null) {
            String baseUrl = uriInfo.getBaseUri().toASCIIString();
            Element convElement =
                    BarFileUtils.convertToRoleInstanceUrl(aclElement, baseUrl, this.cell.getName(), this.boxName);
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(DcCoreUtils.nodeToString(convElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            collectionCmp.acl(aclXml);
        }

        // PROPPATCH登録
        registProppatch(collectionCmp, propElements, collectionUrl);
    }

    /**
     * BoxにACLとPROPPATCH情報を登録.
     * @param targetBox box
     * @param aclElement ACL
     * @param propElements PROPATCHで設定する内容
     * @param boxUrl boxのURL
     */
    private void registBoxAclAndProppatch(Box targetBox, Element aclElement,
            List<Element> propElements, String boxUrl) {
        if (targetBox == null || boxCmp == null) {
            return;
        }

        // ACL登録
        if (aclElement != null) {
            String baseUrl = uriInfo.getBaseUri().toASCIIString();
            Element convElement =
                    BarFileUtils.convertToRoleInstanceUrl(aclElement, baseUrl, this.cell.getName(), this.boxName);
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(DcCoreUtils.nodeToString(convElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            boxCmp.acl(aclXml);
        }

        // PROPPATCH登録
        registProppatch(boxCmp, propElements, boxUrl);
    }

    private void registProppatch(DavCmp davCmp, List<Element> propElements, String boxUrl) {
        if (!propElements.isEmpty()) {
            Reader propXml = getProppatchXml(propElements);
            try {
                Propertyupdate propUpdate = Propertyupdate.unmarshal(propXml);
                davCmp.proppatch(propUpdate, boxUrl);
            } catch (IOException ex) {
                throw DcCoreException.Dav.XML_ERROR.reason(ex);
            }
        }
    }

    /**
     * barファイル内で定義されているコレクションのMap<key, DavCmpEsImpl>を取得する.
     * @return コレクションのMapDavCmpEsImplオブジェクト
     */
    private Map<String, DavCmp> getCollections(String colType) {
        Map<String, DavCmp> map = new HashMap<String, DavCmp>();
        Set<String> keySet = davCmpMap.keySet();
        for (String key : keySet) {
            DavCmp davCmp = davCmpMap.get(key);
            if (davCmp != null && colType.equals(davCmp.getType())) {
                map.put(key, davCmp);
            }
        }
        return map;
    }

    /**
     * barファイル内で定義されているコレクションのMap<key, DavCmpEsImpl>を取得する.
     * @param entryName エントリ名
     * @param collections コレクションのMapオブジェクト
     * @return コレクションのMapDavCmpEsImplオブジェクト
     */
    private DavCmp getCollection(String entryName, Map<String, DavCmp> collections) {
        int pos = entryName.lastIndexOf("/");
        if (pos == entryName.length() - 1) {
            return collections.get(entryName);
        }
        String colName = entryName.substring(0, pos + 1);
        return collections.get(colName);
    }

    /**
     * 00_$metadata_xmlを解析してユーザスキーマの登録処理を行う.
     * @param entryName エントリ名
     * @param inputStream 入力ストリーム
     * @param davCmp Collection操作用オブジェクト
     * @return 正常終了した場合はtrue
     */
    protected boolean registUserSchema(String entryName, InputStream inputStream, DavCmp davCmp) {
        EdmDataServices metadata = null;
        // XMLパーサ(StAX,SAX,DOM)にInputStreamをそのまま渡すとファイル一覧の取得処理が
        // 中断してしまうため暫定対処としてバッファに格納してからパースする
        try {
            InputStreamReader isr = new InputStreamReader(new CloseShieldInputStream(inputStream));
            // 00_$metadata.xmlを読み込んで、ユーザスキーマを登録する
            XMLFactoryProvider2 provider = StaxXMLFactoryProvider2.getInstance();
            XMLInputFactory2 factory = provider.newXMLInputFactory2();
            XMLEventReader2 reader = factory.createXMLEventReader(isr);
            DcEdmxFormatParser parser = new DcEdmxFormatParser();
            metadata = parser.parseMetadata(reader);
        } catch (Exception ex) {
            log.info("XMLParseException: " + ex.getMessage(), ex.fillInStackTrace());
            String message = DcCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        } catch (StackOverflowError tw) {
            // ComplexTypeの循環参照時にStackOverFlowErrorが発生する
            log.info("XMLParseException: " + tw.getMessage(), tw.fillInStackTrace());
            String message = DcCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }
        // Entity/Propertyの登録
        // Property/ComplexPropertyはデータ型としてComplexTypeを使用する場合があるため、
        // 一番最初にComplexTypeを登録してから、EntityTypeを登録する
        // DcODataProducer producer = davCmp.getODataProducer();
        try {
            createComplexTypes(metadata, davCmp);
            createEntityTypes(metadata, davCmp);
            createAssociations(metadata, davCmp);
        } catch (DcCoreException e) {
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info("DcCoreException: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.info("Regist Entity Error: " + e.getMessage(), e.fillInStackTrace());
            String message = DcCoreMessageUtils.getMessage("PL-BI-2003");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }
        return true;
    }

    /**
     * Edmxに定義されているEntityType/Propertyを登録する.
     * @param metadata Edmxのメタデータ
     * @param davCmp Collection操作用オブジェクト
     */
    @SuppressWarnings("unchecked")
    protected void createEntityTypes(EdmDataServices metadata, DavCmp davCmp) {
        // DeclaredPropertyはEntityTypeに紐付いているため、EntityTypeごとにPropertyを登録する
        Iterable<EdmEntityType> entityTypes = metadata.getEntityTypes();
        UserSchemaODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmEntityType entity : entityTypes) {
            log.debug("EntityType: " + entity.getName());
            if (producer == null) {
                producer = (UserSchemaODataProducer) davCmp.getSchemaODataProducer(this.cell);
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            Map<String, String> entityTypeIds = producer.getEntityTypeIds();
            odataEntityResource.setEntitySetName(EntityType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", entity.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                    odataEntityResource.getOdataResource(), userMetadata);
            // EntityTypeの登録
            String path = String.format("/%s/%s/%s/EntityType('%s')",
                    this.cell.getName(), this.boxName, davCmp.getName(), entity.getName());
            writeOutputStream(false, "PL-BI-1002", path);
            EntityResponse response = producer.createEntity(EntityType.EDM_TYPE_NAME, oew);
            OEntityWrapper entityResponse = (OEntityWrapper) response.getEntity();
            entityTypeIds.put(entity.getName(), entityResponse.getUuid());
        }
        // EntityTypeに紐付いているPropertyの登録
        for (EdmEntityType entity : entityTypes) {
            createProperties(entity, davCmp, producer);
        }
    }

    /**
     * Edmxに定義されているProperty/ComplexTypePropertyを登録する.
     * @param entity 登録対象のPropertyが定義されているEntityType/ComplexTypeオブジェクト
     * @param davCmp Collection操作用オブジェクト
     * @param producer ODataプロデューサー
     */
    @SuppressWarnings("unchecked")
    protected void createProperties(EdmStructuralType entity, DavCmp davCmp, DcODataProducer producer) {
        Iterable<EdmProperty> properties = entity.getDeclaredProperties();
        EdmDataServices userMetadata = null;
        String edmTypeName = Property.EDM_TYPE_NAME;
        if (entity instanceof EdmComplexType) {
            edmTypeName = ComplexTypeProperty.EDM_TYPE_NAME;
        }
        for (EdmProperty property : properties) {
            String name = property.getName();
            log.debug(edmTypeName + ": " + name);
            if (name.startsWith("_")) {
                continue;
            }
            if (userMetadata == null) {
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
                odataEntityResource.setEntitySetName(edmTypeName);
            }
            CollectionKind kind = property.getCollectionKind();
            if (kind != null && !kind.equals(CollectionKind.NONE) && !kind.equals(CollectionKind.List)) {
                throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(METADATA_XML);
            }
            JSONObject json = new JSONObject();
            json.put("Name", property.getName());
            if (entity instanceof EdmComplexType) {
                json.put("_ComplexType.Name", entity.getName());
            } else {
                json.put("_EntityType.Name", entity.getName());
                json.put("IsKey", false); // Iskey対応時に設定する必要あり
                json.put("UniqueKey", null); // UniqueKey対応時に設定する必要あり
            }
            String typeName = property.getType().getFullyQualifiedTypeName();
            if (!property.getType().isSimple() && typeName.startsWith("UserData.")) {
                typeName = typeName.replace("UserData.", "");
            }
            json.put("Type", typeName);
            json.put("Nullable", property.isNullable());
            json.put("DefaultValue", property.getDefaultValue());
            json.put("CollectionKind", property.getCollectionKind().toString());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                    odataEntityResource.getOdataResource(), userMetadata);
            // ComplexTypePropertyの登録
            producer.createEntity(edmTypeName, oew);
        }
    }

    /**
     * Edmxに定義されているAssociationEndおよびリンク情報を登録する.
     * @param metadata Edmxのメタデータ
     * @param davCmp Collection操作用オブジェクト
     */
    protected void createAssociations(EdmDataServices metadata, DavCmp davCmp) {
        Iterable<EdmAssociation> associations = metadata.getAssociations();
        DcODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmAssociation association : associations) {
            // Association情報をもとに、AssociationEndとAssociationEnd同士のリンクを登録する
            String name = association.getName();
            log.debug("Association: " + name);
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(this.cell);
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
                odataEntityResource.setEntitySetName(AssociationEnd.EDM_TYPE_NAME);
            }
            String path = String.format("/%s/%s/%s/Association('%s','%s')",
                    this.cell.getName(), this.boxName, davCmp.getName(),
                    association.getEnd1().getRole(), association.getEnd2().getRole());
            writeOutputStream(false, "PL-BI-1002", path);
            // AssociationEndの登録
            EdmAssociationEnd ae1 = association.getEnd1();
            String realRoleName1 = getRealRoleName(ae1.getRole());
            createAssociationEnd(producer, userMetadata, ae1, realRoleName1);
            EdmAssociationEnd ae2 = association.getEnd2();
            String realRoleName2 = getRealRoleName(ae2.getRole());
            createAssociationEnd(producer, userMetadata, ae2, realRoleName2);

            // AssociationEnd間の$links登録
            String fromkey = String.format("(Name='%s',_EntityType.Name='%s')", realRoleName1, ae1.getType().getName());
            OEntityKey fromOEKey = OEntityKey.parse(fromkey);
            OEntityId sourceEntity = OEntityIds.create(AssociationEnd.EDM_TYPE_NAME, fromOEKey);
            String targetNavProp = "_" + AssociationEnd.EDM_TYPE_NAME;
            odataEntityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);
            String tokey = String.format("(Name='%s',_EntityType.Name='%s')", realRoleName2, ae2.getType().getName());
            OEntityKey toOEKey = OEntityKey.parse(tokey);
            OEntityId newTargetEntity = OEntityIds.create(AssociationEnd.EDM_TYPE_NAME, toOEKey);
            producer.createLink(sourceEntity, targetNavProp, newTargetEntity);
        }
    }

    /*
     * 入力文字列("エンティティタイプ名:ロール名")をコロンで分割し、コロン以降の文字列を返す
     * 文字列中にコロンが含まれなかった場合は例外をスローする
     * @param sourceRoleName 変換元のロール名 ("エンティティタイプ名:ロール名")
     * @return 実際のロール名
     */
    private String getRealRoleName(String sourceRoleName) {
        String[] tokens = sourceRoleName.split(":");
        if (tokens.length != 2) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(sourceRoleName);
        }
        if (tokens[0].length() <= 0 || tokens[1].length() <= 0) {
            throw DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(sourceRoleName);
        }
        return tokens[1];
    }

    /**
     * 引数で渡されたAssociationEndを登録する.
     * @param producer Entity登録用DcODataProcucerオブジェクト
     * @param userMetadata ユーザ定義用スキーマオブジェクト
     * @param associationEnd 登録用AssociationEndオブジェクト
     * @param associationEndName AssociationEnd名
     */
    @SuppressWarnings("unchecked")
    protected void createAssociationEnd(DcODataProducer producer,
            EdmDataServices userMetadata, EdmAssociationEnd associationEnd, String associationEndName) {
        // AssociationEndの名前は、AssociationEndのロール名を使用する
        JSONObject json = new JSONObject();
        String entityTypeName = associationEnd.getType().getName();
        json.put(AssociationEnd.P_ASSOCIATION_NAME.getName(), associationEndName);
        json.put(AssociationEnd.P_ENTITYTYPE_NAME.getName(), entityTypeName);
        json.put(AssociationEnd.P_MULTIPLICITY.getName(), associationEnd.getMultiplicity().getSymbolString());
        StringReader stringReader = new StringReader(json.toJSONString());
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(), userMetadata);
        producer.createEntity(AssociationEnd.EDM_TYPE_NAME, oew);
    }

    /**
     * Edmxに定義されているComplexType/ComplexTypePropertyを登録する.
     * @param metadata Edmxのメタデータ
     * @param davCmp Collection操作用オブジェクト
     */
    @SuppressWarnings("unchecked")
    protected void createComplexTypes(EdmDataServices metadata, DavCmp davCmp) {
        // DeclaredPropertyはComplexTypeに紐付いているため、ComplexTypeごとにComplexTypePropertyを登録する
        Iterable<EdmComplexType> complexTypes = metadata.getComplexTypes();
        DcODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmComplexType complexType : complexTypes) {
            log.debug("ComplexType: " + complexType.getName());
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(this.cell);
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            odataEntityResource.setEntitySetName(ComplexType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", complexType.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                    odataEntityResource.getOdataResource(), userMetadata);
            // ComplexTypeの登録
            String path = String.format("/%s/%s/%s/ComplexType('%s')",
                    this.cell.getName(), this.boxName, davCmp.getName(), complexType.getName());
            writeOutputStream(false, "PL-BI-1002", path);
            producer.createEntity(ComplexType.EDM_TYPE_NAME, oew);
        }

        // ComplexTypeに紐付いているComplexTypePropertyの登録
        for (EdmComplexType complexType : complexTypes) {
            createProperties(complexType, davCmp, producer);
        }
    }

    private Reader getProppatchXml(List<Element> propElements) {
        StringBuffer sbPropXml = new StringBuffer();
        sbPropXml.append("<D:propertyupdate xmlns:D=\"DAV:\"");
        sbPropXml.append(" xmlns:dc=\"urn:x-dc1:xmlns\"");
        sbPropXml.append(" xmlns:Z=\"http://www.w3.com/standards/z39.50/\">");
        sbPropXml.append("<D:set>");
        sbPropXml.append("<D:prop>");
        for (Element element : propElements) {
            sbPropXml.append(DcCoreUtils.nodeToString(element));
        }
        sbPropXml.append("</D:prop>");
        sbPropXml.append("</D:set>");
        sbPropXml.append("</D:propertyupdate>");
        Reader propXml = new StringReader(sbPropXml.toString());
        return propXml;
    }

    /**
     * @param entryCount the entryCount to set
     */
    void setEntryCount(long entryCount) {
        this.progressInfo = new BarInstallProgressInfo(this.cell.getId(), this.box.getId(), entryCount);
    }

    /**
     * 処理が開始した情報をキャッシュに記録する.
     */
    public void writeInitProgressCache() {
        setEventBus();
        writeOutputStream(false, "PL-BI-1000", this.cell.getUrl() + boxName, "");
        writeToProgressCache(true);
    }

    /**
     * エラー情報をキャッシュに記録する.
     */
    public void writeErrorProgressCache() {
        if (this.progressInfo != null) {
            this.progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            this.progressInfo.setEndTime();
            writeToProgressCache(true);
        }
    }

}
