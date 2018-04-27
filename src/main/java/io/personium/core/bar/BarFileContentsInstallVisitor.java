package io.personium.core.bar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.commons.io.Charsets;
import org.apache.wink.webdav.model.Propertyupdate;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.bar.jackson.JSONUserDataLink;
import io.personium.core.bar.jackson.JSONUserDataLinks;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Box;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumEdmxFormatParser;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataEntitiesResource;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.rs.odata.ODataResource;

public class BarFileContentsInstallVisitor implements FileVisitor<Path> {

    static Logger log = LoggerFactory.getLogger(BarFileContentsInstallVisitor.class);

    private static final String CONTENTS_DIR = BarFile.CONTENTS_DIR + "/";

    private static final int TYPE_WEBDAV_COLLECTION = 0;
    private static final int TYPE_ODATA_COLLECTION = 1;
    private static final int TYPE_SERVICE_COLLECTION = 2;
    private static final int TYPE_DAV_FILE = 3;
    private static final int TYPE_SVC_FILE = 4;

    /**  */
    private Box box;
    /** */
    private String baseUrl;
    /** Progress info. */
    private BarInstallProgressInfo progressInfo;
    /**  */
    private ODataEntityResource entityResource;
    /**  */
    private EventBus eventBus;
    /**  */
    private PersoniumEvent event;
    /**  */
    private Map<String, String> davFileContentTypeMap;
    /**  */
    private Map<String, Element> davFileAclMap;
    /**  */
    private Map<String, List<Element>> davFilePropsMap;
    /**  */
    private Map<String, DavCmp> odataCollections;
    /**  */
    private Map<String, DavCmp> webdavCollections;
    /**  */
    private Map<String, DavCmp> serviceCollections;


    private String currentPath = null;
    private int userDataCount = 0;
    private JSONUserDataLinks userDataLinks = new JSONUserDataLinks();
    private LinkedHashMap<String, BulkRequest> bulkRequests = new LinkedHashMap<String, BulkRequest>();
    private Map<String, String> fileNameMap = new HashMap<String, String>();
    private DavCmp currentDavCmp = null;
    private List<String> doneKeys = new ArrayList<String>();

    public BarFileContentsInstallVisitor(Box box,
            String baseUrl,
            BarInstallProgressInfo progressInfo,
            ODataEntityResource entityResource,
            EventBus eventBus,
            PersoniumEvent event,
            Map<String, DavCmp> davCmpMap,
            Map<String, String> davFileContentTypeMap,
            Map<String, Element> davFileAclMap,
            Map<String, List<Element>> davFilePropsMap) {
        this.box = box;
        this.baseUrl = baseUrl;
        this.progressInfo = progressInfo;
        this.entityResource = entityResource;
        this.eventBus = eventBus;
        this.event = event;
        this.davFileContentTypeMap = davFileContentTypeMap;
        this.davFileAclMap = davFileAclMap;
        this.davFilePropsMap = davFilePropsMap;
        // Create a map for each collection type.
        odataCollections = getCollections(davCmpMap, DavCmp.TYPE_COL_ODATA);
        webdavCollections = getCollections(davCmpMap, DavCmp.TYPE_COL_WEBDAV);
        // Since it may be referred to as parent, Box must be registered.
        webdavCollections.putAll(getCollections(davCmpMap, DavCmp.TYPE_COL_BOX));
        serviceCollections = getCollections(davCmpMap, DavCmp.TYPE_COL_SVC);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        createContents(dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        createContents(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.info("IOException: " + exc.getMessage(), exc.fillInStackTrace());
        throw PersoniumBarException.INSTALLATION_FAILED.path(file.toString()).detail(exc.getMessage());
//        throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    /**
     * barファイル内で定義されているコレクションのMap<key, DavCmpEsImpl>を取得する.
     * @return コレクションのMapDavCmpEsImplオブジェクト
     */
    private Map<String, DavCmp> getCollections(Map<String, DavCmp> davCmpMap, String colType) {
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
     * Read one content data (bar/90_contents) in the bar file and register it.
     * @param pathInZip File path obj in zip
     * @return boolean Processing result true:success false:failure
     */
    private void createContents(Path pathInZip) {

        try {
//            ZipArchiveEntry zae = null;

//            while ((zae = this.zipArchiveInputStream.getNextZipEntry()) != null) {
            String entryName = pathInZip.toString();
            log.debug("Entry Name: " + entryName);
            log.debug("Entry Size: " + Files.size(pathInZip));
//            log.debug("Entry Compressed Size: " + zae.getCompressedSize());
            if (!Files.isDirectory(pathInZip)) {
                this.progressInfo.addDelta(1L);
            }
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_STARTED, entryName);

            // ODataCollectionからDav/ServiceCollection/別ODataCollectionのリソースに対する処理に変わった際に
            // ユーザデータの登録やリンクの登録をする必要があれば、処理を実行する
            if (currentPath != null && !entryName.startsWith(currentPath)) {
                execBulkRequest(currentDavCmp.getCell().getId(),
                        bulkRequests, fileNameMap, currentDavCmp.getODataProducer());
                createUserdataLinks(currentDavCmp.getODataProducer(), userDataLinks);
                userDataLinks = new JSONUserDataLinks();
                currentPath = null;
            }
            int entryType = getEntryType(entryName, odataCollections,
                    webdavCollections, serviceCollections, this.davFileContentTypeMap);
            switch (entryType) {
            case TYPE_ODATA_COLLECTION:
                // ODataコレクションの登録
                if (!odataCollections.isEmpty()) {
                    isValidODataContents(entryName, odataCollections, doneKeys);

                    Pattern formatPattern = Pattern.compile(CONTENTS_DIR + ".+/90_data/");
                    Matcher formatMatcher = formatPattern.matcher(entryName);
                    if (formatMatcher.matches()) {
                        currentPath = entryName;
                    }
                    Pattern userodataDirPattern = Pattern.compile(CONTENTS_DIR + ".+/90_data/.+");
                    Matcher userodataDirMatcher = userodataDirPattern.matcher(entryName);

                    if (getFileExtension(entryName).equals(".xml")) {
                        // 00_$metadata.xmlの解析・ユーザスキーマ登録
                        currentDavCmp = getCollection(entryName, odataCollections);
                        registUserSchema(entryName, pathInZip, currentDavCmp);
                        writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, entryName);
                        doneKeys.add(entryName);
                        return;
                    } else if (entryName.endsWith(BarFile.ODATA_RELATIONS_JSON)) {
                        userDataLinks = registJsonLinksUserdata(entryName, pathInZip);
                        writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, entryName);
                        doneKeys.add(entryName);
                        return;
                    } else if (userodataDirMatcher.matches() && getFileExtension(entryName).equals(".json")) {
                        userDataCount++;
                        setBulkRequests(pathInZip, entryName, currentDavCmp.getODataProducer(),
                                bulkRequests, fileNameMap);
                        doneKeys.add(entryName);
                        long bulkSize = Long.parseLong(PersoniumUnitConfig
                                .get(PersoniumUnitConfig.BAR.BAR_USERDATA_BULK_SIZE));
                        if ((userDataCount % bulkSize) == 0) {
                            execBulkRequest(currentDavCmp.getCell().getId(),
                                    bulkRequests, fileNameMap, currentDavCmp.getODataProducer());
                        }
                        return;
                    } else if (!entryName.endsWith("/")) {
                        // xml,jsonファイル以外のファイルがあった場合はエラーを返却する
                        PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                        log.info(detail.getDetailMessage() + " [" + entryName + "]");
                        throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
                    }
                }
                break;

            case TYPE_DAV_FILE:
                // WebDAVコレクションの登録
                // bar/90_contents/{davcol_name}配下のエントリを1つずつ登録する
                registWebDavFile(entryName, pathInZip, webdavCollections);
                break;

            case TYPE_SVC_FILE:
                // Serviceコレクションの登録
                installSvcCollection(pathInZip, webdavCollections, entryName);
                break;

            default:
                break;
            }
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, entryName);
            doneKeys.add(entryName);
//            }
            // ODataCollectionのリソースに対する処理に終わった際に、ユーザデータの登録やリンクの登録をする必要があれば実行する
            if (currentPath != null) {
                execBulkRequest(currentDavCmp.getCell().getId(), bulkRequests,
                        fileNameMap, currentDavCmp.getODataProducer());
                createUserdataLinks(currentDavCmp.getODataProducer(), userDataLinks);
                userDataLinks = null;
            }
        } catch (IOException ex) {
            log.info("IOException: " + ex.getMessage(), ex.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2000");
            throw PersoniumBarException.INSTALLATION_FAILED.path("").detail(detail);
        }
        // 必須データ（bar/90_contents/{odatacol_name}/00_$metadata.xml)の確認
        checkNecessaryFile(odataCollections, doneKeys);
    }

    private void execBulkRequest(String cellId, LinkedHashMap<String, BulkRequest> bulkRequests,
            Map<String, String> fileNameMap,
            PersoniumODataProducer producer) {
        // バルクで一括登録を実行
        producer.bulkCreateEntity(producer.getMetadata(), bulkRequests, cellId);

        // レスポンスのチェック
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            // エラーが発生していた場合はエラーのレスポンスを返却する
            if (request.getValue().getError() != null) {
                if (request.getValue().getError() instanceof PersoniumCoreException) {
                    PersoniumCoreException e = ((PersoniumCoreException) request.getValue().getError());
                    log.info("PersoniumCoreException: " + e.getMessage());
                    throw PersoniumBarException.INSTALLATION_FAILED.path(
                            fileNameMap.get(request.getKey())).detail(e.getMessage());
                } else {

                    Exception e = request.getValue().getError();
                    log.info("Regist Entity Error: " + e.toString());
                    log.info("Regist Entity Error: " + e.getClass().getName());
                    log.info("Regist Entity Error: " + e);
                    PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2003");
                    throw PersoniumBarException.INSTALLATION_FAILED.path(
                            fileNameMap.get(request.getKey())).detail(detail);
                }
            }
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, fileNameMap.get(request.getKey()));
        }

        bulkRequests.clear();
        fileNameMap.clear();
    }

    private void createUserdataLinks(PersoniumODataProducer producer, JSONUserDataLinks userDataLinks) {
        int linkSize = userDataLinks.getLinks().size();
        int linkCount = 0;
        String message = PersoniumCoreMessageUtils.getMessage(BarFileUtils.CODE_INSTALL_PROCESSING);
        for (JSONUserDataLink json : userDataLinks.getLinks()) {
            linkCount++;
            createUserdataLink(json, producer);
            long linksOutputStreamSize = Long.parseLong(
                    PersoniumUnitConfig.get(PersoniumUnitConfig.BAR.BAR_USERDATA_LINKS_OUTPUT_STREAM_SIZE));
            if (linkCount % linksOutputStreamSize == 0) {
                writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING,
                        String.format("userDataLinks %d / %d", linkCount, linkSize), message);
            }
        }
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING,
                String.format("userDataLinks %d / %d", linkCount, linkSize), message);
    }

    /**
     * 10_odatarelations.jsonに定義されているリンク情報をESへ登録する.
     * @param mappedObject JSONファイルから読み込んだオブジェクト
     */
    private void createUserdataLink(JSONUserDataLink mappedObject, PersoniumODataProducer producer) {
        OEntityId sourceEntity = null;
        OEntityId newTargetEntity = null;
        try {
            Map<String, String> fromId = mappedObject.getFromId();
            String fromKey = "";
            Iterator<Entry<String, String>> fromIterator = fromId.entrySet().iterator();
            fromKey = String.format("('%s')", fromIterator.next().getValue());

            OEntityKey fromOEKey = OEntityKey.parse(fromKey);
            sourceEntity = OEntityIds.create(mappedObject.getFromType(), fromOEKey);
            String targetNavProp = mappedObject.getNavPropToType();

            // リンク作成前処理
            entityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

            Map<String, String> toId = mappedObject.getToId();
            String toKey = "";
            Iterator<Entry<String, String>> toIterator = toId.entrySet().iterator();
            toKey = String.format("('%s')", toIterator.next().getValue());

            OEntityKey toOEKey = OEntityKey.parse(toKey);
            newTargetEntity = OEntityIds.create(mappedObject.getToType(), toOEKey);

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
            throw PersoniumBarException.INSTALLATION_FAILED.path(path).detail(targetPath);
        }
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
    private int getEntryType(String entryName,
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

        // ODataコレクション配下ではなく、かつ、rootpropsに定義されていないエントリ
        PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2006");
        log.info(detail.getDetailMessage() + " [" + entryName + "]");
        throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
    }

    /**
     * bar/90_contents/{OdataCol_name}配下のエントリが正しい定義であるかどうかを確認する.
     * @param entryName エントリ名(コレクション名)
     * @param colMap コレクションのMapオブジェクト
     * @param doneKeys 処理済みのODataコレクション用エントリリスト
     */
    protected void isValidODataContents(String entryName, Map<String, DavCmp> colMap, List<String> doneKeys) {

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
        if (BarFile.ODATA_RELATIONS_JSON.equals(odataPath)) {

            // 00_$metadata.xmlの処理が済んでいるかのチェック
            String meatadataPath = odataColPath + BarFile.METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getDetailMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
            // 90_data/の処理が済んでいないかのチェック
            String userDataPath = odataColPath + BarFile.ODATA_DIR + "/";
            if (doneKeys.contains(userDataPath)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getDetailMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
        }
        if (odataPath.startsWith(BarFile.ODATA_DIR + "/")) {
            // 00_$metadata.xmlの処理が済んでいるかのチェック
            String meatadataPath = odataColPath + BarFile.METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getDetailMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
        }

        // bar/90_contents/{OData_collection}/{dirPath}/のチェック
        String dirPath = null;
        Pattern pattern = Pattern.compile("^([^/]+)/.*");
        Matcher m = pattern.matcher(odataPath);
        if (m.matches()) {
            dirPath = m.replaceAll("$1");
        }
        if (dirPath != null && !dirPath.equals(BarFile.ODATA_DIR)) {
            // bar/90_contents/{OData_collection}/{dir}/の場合はエラーとする
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
            log.info(detail.getDetailMessage() + "entryName: " + entryName);
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
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
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getDetailMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
        }
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
     * @param pathInZip 入力Path
     * @param davCmp Collection操作用オブジェクト
     */
    private void registUserSchema(String entryName, Path pathInZip, DavCmp davCmp) {
        EdmDataServices metadata = null;
        // XMLパーサ(StAX,SAX,DOM)にInputStreamをそのまま渡すとファイル一覧の取得処理が
        // 中断してしまうため暫定対処としてバッファに格納してからパースする
        try (BufferedReader bufferedReader = Files.newBufferedReader(pathInZip, Charsets.UTF_8)) {
            // 00_$metadata.xmlを読み込んで、ユーザスキーマを登録する
            XMLFactoryProvider2 provider = StaxXMLFactoryProvider2.getInstance();
            XMLInputFactory2 factory = provider.newXMLInputFactory2();
            XMLEventReader2 reader = factory.createXMLEventReader(bufferedReader);
            PersoniumEdmxFormatParser parser = new PersoniumEdmxFormatParser();
            metadata = parser.parseMetadata(reader);
        } catch (Exception | StackOverflowError ex) {
            // ComplexTypeの循環参照時にStackOverFlowErrorが発生する
            log.info("XMLParseException: " + ex.getMessage(), ex.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2002");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }
        // Entity/Propertyの登録
        // Property/ComplexPropertyはデータ型としてComplexTypeを使用する場合があるため、
        // 一番最初にComplexTypeを登録してから、EntityTypeを登録する
        // PersoniumODataProducer producer = davCmp.getODataProducer();
        try {
            createComplexTypes(metadata, davCmp);
            createEntityTypes(metadata, davCmp);
            createAssociations(metadata, davCmp);
        } catch (PersoniumCoreException e) {
            log.info("PersoniumCoreException: " + e.getMessage());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(e.getMessage());
        } catch (Exception e) {
            log.info("Regist Entity Error: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2003");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }
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
        PersoniumODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmComplexType complexType : complexTypes) {
            log.debug("ComplexType: " + complexType.getName());
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(box.getCell());
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            entityResource.setEntitySetName(ComplexType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", complexType.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), userMetadata);
            // ComplexTypeの登録
            String path = String.format("/%s/%s/%s/ComplexType('%s')",
                    box.getCell().getName(), box.getName(), davCmp.getName(), complexType.getName());
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING, path);
            producer.createEntity(ComplexType.EDM_TYPE_NAME, oew);
        }

        // ComplexTypeに紐付いているComplexTypePropertyの登録
        for (EdmComplexType complexType : complexTypes) {
            createProperties(complexType, davCmp, producer);
        }
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
                producer = (UserSchemaODataProducer) davCmp.getSchemaODataProducer(box.getCell());
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            Map<String, String> entityTypeIds = producer.getEntityTypeIds();
            entityResource.setEntitySetName(EntityType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", entity.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), userMetadata);
            // EntityTypeの登録
            String path = String.format("/%s/%s/%s/EntityType('%s')",
                    box.getCell().getName(), box.getName(), davCmp.getName(), entity.getName());
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING, path);
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
    protected void createProperties(EdmStructuralType entity, DavCmp davCmp, PersoniumODataProducer producer) {
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
                entityResource.setEntitySetName(edmTypeName);
            }
            CollectionKind kind = property.getCollectionKind();
            if (kind != null && !kind.equals(CollectionKind.NONE) && !kind.equals(CollectionKind.List)) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.METADATA_XML);
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
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), userMetadata);
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
        PersoniumODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmAssociation association : associations) {
            // Association情報をもとに、AssociationEndとAssociationEnd同士のリンクを登録する
            String name = association.getName();
            log.debug("Association: " + name);
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(box.getCell());
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
                entityResource.setEntitySetName(AssociationEnd.EDM_TYPE_NAME);
            }
            String path = String.format("/%s/%s/%s/Association('%s','%s')",
                    box.getCell().getName(), box.getName(), davCmp.getName(),
                    association.getEnd1().getRole(), association.getEnd2().getRole());
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING, path);
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
            entityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);
            String tokey = String.format("(Name='%s',_EntityType.Name='%s')", realRoleName2, ae2.getType().getName());
            OEntityKey toOEKey = OEntityKey.parse(tokey);
            OEntityId newTargetEntity = OEntityIds.create(AssociationEnd.EDM_TYPE_NAME, toOEKey);
            producer.createLink(sourceEntity, targetNavProp, newTargetEntity);
        }
    }

    /**
     * 引数で渡されたAssociationEndを登録する.
     * @param producer Entity登録用PersoniumODataProcucerオブジェクト
     * @param userMetadata ユーザ定義用スキーマオブジェクト
     * @param associationEnd 登録用AssociationEndオブジェクト
     * @param associationEndName AssociationEnd名
     */
    @SuppressWarnings("unchecked")
    protected void createAssociationEnd(PersoniumODataProducer producer,
            EdmDataServices userMetadata, EdmAssociationEnd associationEnd, String associationEndName) {
        // AssociationEndの名前は、AssociationEndのロール名を使用する
        JSONObject json = new JSONObject();
        String entityTypeName = associationEnd.getType().getName();
        json.put(AssociationEnd.P_ASSOCIATION_NAME.getName(), associationEndName);
        json.put(AssociationEnd.P_ENTITYTYPE_NAME.getName(), entityTypeName);
        json.put(AssociationEnd.P_MULTIPLICITY.getName(), associationEnd.getMultiplicity().getSymbolString());
        StringReader stringReader = new StringReader(json.toJSONString());
        OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                entityResource.getOdataResource(), userMetadata);
        producer.createEntity(AssociationEnd.EDM_TYPE_NAME, oew);
    }

    /**
     * 入力文字列("エンティティタイプ名:ロール名")をコロンで分割し、コロン以降の文字列を返す.
     * 文字列中にコロンが含まれなかった場合は例外をスローする.
     * @param sourceRoleName 変換元のロール名 ("エンティティタイプ名:ロール名")
     * @return 実際のロール名
     */
    private String getRealRoleName(String sourceRoleName) {
        String[] tokens = sourceRoleName.split(":");
        if (tokens.length != 2) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(sourceRoleName);
        }
        if (tokens[0].length() <= 0 || tokens[1].length() <= 0) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(sourceRoleName);
        }
        return tokens[1];
    }

    /**
     * 10_odatarelations.jsonのデータを読み込みユーザデータのLink情報を生成する.
     * @param entryName 対象ファイル名
     * @param pathInZip 入力Path
     * @return ユーザデータのLink情報
     */
    private JSONUserDataLinks registJsonLinksUserdata(String entryName, Path pathInZip) {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(pathInZip, Charsets.UTF_8)) {
            JSONUserDataLinks links = mapper.readValue(reader, JSONUserDataLinks.class);
            for (JSONUserDataLink userDataLink : links.getLinks()) {
                userDataLinksJsonValidate(userDataLink);
            }
            return links;
        } catch (JsonParseException e) {
            // JSONファイルの解析エラー
            log.info("JsonParseException: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2002");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        } catch (JsonMappingException e) {
            // JSONファイルのデータ定義エラー
            log.info("JsonMappingException: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2003");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        } catch (PersoniumCoreException e) {
            // JSONファイルのバリデートエラー
            log.info("PersoniumCoreException" + e.getMessage(), e.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(e.getMessage());
        } catch (IOException e) {
            log.info("IOException: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2000");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }
    }

    /**
     * 10_odatarelations.jsonのバリデート.
     * @param jsonName JSONファイル名
     * @param userDataLink 読み込んだJSONオブジェクト
     */
    private void userDataLinksJsonValidate(JSONUserDataLink userDataLink) {
        if (userDataLink.getFromType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        }
        if (userDataLink.getFromId() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        } else {
            Map<String, String> fromIdMap = userDataLink.getFromId();
            for (Map.Entry<String, String> entry : fromIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
                }
            }
        }
        if (userDataLink.getToType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        }
        if (userDataLink.getToId() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        } else {
            Map<String, String> toIdMap = userDataLink.getToId();
            for (Map.Entry<String, String> entry : toIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
                }
            }
        }
    }

    private void setBulkRequests(Path pathInZip, String entryName, PersoniumODataProducer producer,
            LinkedHashMap<String, BulkRequest> bulkRequests, Map<String, String> fileNameMap) {
        BulkRequest bulkRequest = new BulkRequest();
        String key = PersoniumUUID.randomUUID();
        try {
            // entityType名を取得する
            String entityTypeName = getEntityTypeName(entryName);
            if (producer.getMetadata().findEdmEntitySet(entityTypeName) == null) {
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            // ZipArchiveImputStreamからユーザデータのJSONをStringReader形式で取得する
            StringReader stringReader = getStringReaderFromPath(pathInZip);

            // リクエストボディを生成する
            ODataResource odataResource = entityResource.getOdataResource();
            ODataEntitiesResource resource = new ODataEntitiesResource(odataResource, entityTypeName);
            OEntity oEntity = resource.getOEntityWrapper(stringReader, odataResource, producer.getMetadata());

            UserDataODataProducer userDataProducer = (UserDataODataProducer) producer;
            EntitySetDocHandler docHandler = producer.getEntitySetDocHandler(entityTypeName, oEntity);
            String docType = UserDataODataProducer.USER_ODATA_NAMESPACE;
            docHandler.setType(docType);
            docHandler.setEntityTypeId(userDataProducer.getEntityTypeId(oEntity.getEntitySetName()));

            entityResource.setOdataProducer(userDataProducer);

            // データ内でのID競合チェック
            // TODO 複合主キー対応、ユニークキーのチェック、NTKP対応
            key = oEntity.getEntitySetName() + ":" + (String) docHandler.getStaticFields().get("__id");

            if (bulkRequests.containsKey(key)) {
                throw PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS;
            }

            // ID指定がない場合はUUIDを払い出す
            if (docHandler.getId() == null) {
                docHandler.setId(PersoniumUUID.randomUUID());
            }
            bulkRequest.setEntitySetName(entityTypeName);
            bulkRequest.setDocHandler(docHandler);
        } catch (Exception e) {
            log.info(entryName + " : " + e.getMessage());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(e.getMessage());
        }
        bulkRequests.put(key, bulkRequest);
        fileNameMap.put(key, entryName);
    }

    private StringReader getStringReaderFromPath(Path pathInZip) throws IOException {
        BufferedReader bufferedReader = Files.newBufferedReader(pathInZip, Charsets.UTF_8);

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

    /**
     * WebDAVファイルの登録を行う.
     * @param entryName barファイルのエントリ名
     * @param pathInZip Path
     * @param webdavCols WebDAVコレクション一覧
     */
    protected void registWebDavFile(String entryName, Path pathInZip, Map<String, DavCmp> webdavCols) {

        // 登録先のファイルパス・コレクション名を取得
        String filePath = entryName.replaceAll(CONTENTS_DIR, "");
        String colPath = entryName.substring(0, entryName.lastIndexOf("/") + 1);

        // DavCmp作成
        DavCmp parentCmp = webdavCols.get(colPath);

        // 親コレクション内のコレクション・ファイル数のチェック
        int maxChildResource = PersoniumUnitConfig.getMaxChildResourceCount();
        if (parentCmp.getChildrenCount() >= maxChildResource) {
            // コレクション内に作成可能なコレクション・ファイル数の制限を超えたため、エラーとする
            String message = PersoniumCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR.getMessage();
            log.info(message);
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(message);
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
            contentType = this.davFileContentTypeMap.get(entryName);
            RuntimeDelegate.getInstance().createHeaderDelegate(MediaType.class).fromString(contentType);
        } catch (Exception e) {
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2005");
            log.info(detail.getDetailMessage() + ": " + e.getMessage(), e.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }

        // ファイル登録
        try (InputStream inputStream = Files.newInputStream(pathInZip)) {
            fileCmp.putForCreate(contentType, inputStream);
        } catch (Exception e) {
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2004");
            log.info(detail.getDetailMessage() + ": " + e.getMessage(), e.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }

        // ACL登録
        Element aclElement = davFileAclMap.get(entryName);
        if (aclElement != null) {
            Element convElement =
                    BarFileUtils.convertToRoleInstanceUrl(aclElement, baseUrl, box.getCell().getName(), box.getName());
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(PersoniumCoreUtils.nodeToString(convElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            fileCmp.acl(aclXml);
        }

        // PROPPATCH登録
        registProppatch(fileCmp, davFilePropsMap.get(entryName), fileCmp.getUrl());
    }

    private void registProppatch(DavCmp davCmp, List<Element> propElements, String boxUrl) {
        if (!propElements.isEmpty()) {
            Reader propXml = getProppatchXml(propElements);
            try {
                Propertyupdate propUpdate = Propertyupdate.unmarshal(propXml);
                davCmp.proppatch(propUpdate, boxUrl);
            } catch (IOException ex) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(ex);
            }
        }
    }

    private Reader getProppatchXml(List<Element> propElements) {
        StringBuffer sbPropXml = new StringBuffer();
        sbPropXml.append("<D:propertyupdate xmlns:D=\"DAV:\"");
        sbPropXml.append(" xmlns:p=\"urn:x-personium:xmlns\"");
        sbPropXml.append(" xmlns:Z=\"http://www.w3.com/standards/z39.50/\">");
        sbPropXml.append("<D:set>");
        sbPropXml.append("<D:prop>");
        for (Element element : propElements) {
            sbPropXml.append(PersoniumCoreUtils.nodeToString(element));
        }
        sbPropXml.append("</D:prop>");
        sbPropXml.append("</D:set>");
        sbPropXml.append("</D:propertyupdate>");
        Reader propXml = new StringReader(sbPropXml.toString());
        return propXml;
    }

    private void installSvcCollection(Path pathInZip, Map<String, DavCmp> webdavCols, String entryName) {
        // bar/90_contents/{svccol_name}配下のエントリを1つずつWebDAV/サービスとして登録する
        // {serviceCollection}/{scriptName}を{serviceCollection}/__src/{scriptName}に変換
        int lastSlashIndex = entryName.lastIndexOf("/");
        StringBuilder serviceSrcName = new StringBuilder();
        serviceSrcName.append(entryName.substring(0, lastSlashIndex));
        serviceSrcName.append("/__src");
        serviceSrcName.append(entryName.substring(lastSlashIndex));

        registWebDavFile(serviceSrcName.toString(), pathInZip, webdavCols);
    }

    private void checkNecessaryFile(Map<String, DavCmp> odataCols, List<String> doneKeys) {
        Set<String> colList = odataCols.keySet();
        for (String colName : colList) {
            String filename = colName + BarFile.METADATA_XML;
            if (!doneKeys.contains(filename)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                throw PersoniumBarException.INSTALLATION_FAILED.path(filename).detail(detail);
            }
        }
    }

    /**
     * Httpレスポンス用メッセージの出力.
     * @param isError エラー時の場合はtrueを、それ以外はfalseを指定する.
     * @param code
     *        メッセージコード(personium-messages.propertiesに定義されたメッセージコード)
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
     *        メッセージコード(personium-messages.propertiesに定義されたメッセージコード)
     * @param path
     *        処理対象リソースパス（ex. /bar/meta/roles.json)
     * @param detail
     *        処理失敗時の詳細情報(PL-BI-2xxx)
     */
    private void writeOutputStream(boolean isError, String code, String path, String detail) {
        String message = PersoniumCoreMessageUtils.getMessage(code);
        if (detail == null) {
            message = message.replace("{0}", "");
        } else {
            message = message.replace("{0}", detail);
        }
        BarFileUtils.outputEventBus(event, eventBus, code, path, message);
        BarFileUtils.writeToProgress(isError, progressInfo, code, message);

        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
        log.info(output);
    }

//    /**
//     *
//     * @param exception
//     */
//    private void writeOutputStream(PersoniumBarException exception) {
//        String code = exception.getCode();
//        String path = exception.getPath();
//        String message = exception.getMessage();
//        outputEventBus(true, code, path, message);
//        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
//        log.info(output);
//    }

}
