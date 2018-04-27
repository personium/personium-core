package io.personium.core.bar;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import org.apache.wink.webdav.WebDAVMethod;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Propstat;
import org.apache.wink.webdav.model.Resourcetype;
import org.apache.wink.webdav.model.Response;
import org.json.simple.JSONObject;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.producer.EntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.bar.jackson.IJSONMappedObjects;
import io.personium.core.bar.jackson.JSONExtRole;
import io.personium.core.bar.jackson.JSONExtRoles;
import io.personium.core.bar.jackson.JSONLink;
import io.personium.core.bar.jackson.JSONLinks;
import io.personium.core.bar.jackson.JSONRelation;
import io.personium.core.bar.jackson.JSONRelations;
import io.personium.core.bar.jackson.JSONRole;
import io.personium.core.bar.jackson.JSONRoles;
import io.personium.core.bar.jackson.JSONRule;
import io.personium.core.bar.jackson.JSONRules;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavCommon;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.utils.UriUtils;

public class BarFileInstallRunner implements Runnable {

    static Logger log = LoggerFactory.getLogger(BarFileInstallRunner.class);

    private static final String CONTENTS_DIR = BarFile.CONTENTS_DIR + "/";
    private static final String LOCALBOX_NO_SLUSH = UriUtils.SCHEME_LOCALBOX + ":";

    /**  */
    private Box box;
    /**  */
    private BoxCmp boxCmp;
    /** Bar file path. */
    private Path barFilePath;
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
    private String requestKey;

    private Map<String, DavCmp> davCmpMap;
    private Map<String, String> davFileContentTypeMap = new HashMap<String, String>();
    private Map<String, Element> davFileAclMap = new HashMap<String, Element>();
    private Map<String, List<Element>> davFilePropsMap = new HashMap<String, List<Element>>();

    public BarFileInstallRunner(Path barFilePath,
            long entryCount,
            String boxName,
            String schema,
            UriInfo uriInfo,
            ODataEntityResource entityResource,
            String requestKey) {
        this.barFilePath = barFilePath;
        this.baseUrl = uriInfo.getBaseUri().toASCIIString();
        this.entityResource = entityResource;
        this.requestKey = requestKey;

        createBox(boxName, schema);
        setEntryCount(entryCount);
        setEventBus();
        writeInitProgressCache();
    }

    /**
     * Box情報をESへ登録する.
     * @param json JSONファイルから読み込んだJSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createBox(String boxName, String schema) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("Name", boxName);
        jsonObj.put("Schema", schema);
        StringReader stringReader = new StringReader(jsonObj.toJSONString());

        OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                entityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        // Boxの登録
        EntityResponse res = entityResource.getOdataProducer().createEntity(Box.EDM_TYPE_NAME, oew);

        // Davの登録
        box = new Box(entityResource.getAccessContext().getCell(), oew);
        boxCmp = ModelFactory.boxCmp(box);

        // post event
        postCellCtlCreateEvent(res);
    }

    /**
     * @param entryCount the entryCount to set
     */
    private void setEntryCount(long entryCount) {
        progressInfo = new BarInstallProgressInfo(box.getCell().getId(), box.getId(), entryCount);
    }

    /**
     * 処理が開始した情報をキャッシュに記録する.
     */
    private void writeInitProgressCache() {
        writeOutputStream(false, "PL-BI-1000", UriUtils.SCHEME_LOCALCELL + ":/" + box.getName(), "");
        BarFileUtils.writeToProgressCache(true, progressInfo);
    }

    /**
     * barインストール処理状況の内部イベント出力用の設定を行う.
     */
    private void setEventBus() {
        // TODO Boxのスキーマとサブジェクトのログは内部イベントの正式対応時に実装する
        String type = WebDAVMethod.MKCOL.toString();
        String object = UriUtils.SCHEME_LOCALCELL + ":/" + box.getName();
        String result = "";
        event = new PersoniumEvent(type, object, result, requestKey);
        eventBus = box.getCell().getEventBus();
    }

    @Override
    public void run() {
        try (BarFile barFile = BarFile.newInstance(barFilePath)) {
            ObjectMapper mapper = new ObjectMapper();
            createCellCtlObjects(barFile, mapper, BarFile.RELATIONS_JSON, JSONRelations.class);
            createCellCtlObjects(barFile, mapper, BarFile.ROLES_JSON, JSONRoles.class);
            createCellCtlObjects(barFile, mapper, BarFile.EXTROLES_JSON, JSONExtRoles.class);
            createCellCtlObjects(barFile, mapper, BarFile.RULES_JSON, JSONRules.class);
            createCellCtlObjects(barFile, mapper, BarFile.LINKS_JSON, JSONLinks.class);
            registXmlEntry(barFile.getRootPropsXmlPathString(), barFile.getReader(BarFile.ROOTPROPS_XML));

            FileVisitor<Path> visitor = new BarFileContentsInstallVisitor(box, baseUrl, progressInfo,
                    entityResource, eventBus, event, davCmpMap, davFileContentTypeMap, davFileAclMap, davFilePropsMap);
//            try {
            Files.walkFileTree(barFile.getContentsDirPath().toAbsolutePath(), visitor);
//            } catch (IOException e) {
//                throw PersoniumCoreException.Common.FILE_IO_ERROR.params("copy webdav data from snapshot file").reason(e);
//            }
        } catch (PersoniumBarException e) {
            writeOutputStream(e);
            writeOutputStream(false, BarFileUtils.CODE_BAR_INSTALL_FAILED,
                    UriUtils.SCHEME_LOCALCELL + ":/" + box.getName(), e.getMessage());
            progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            return;
        } catch (Throwable t) {
            String message = getErrorMessage(t);
            log.info("Exception: " + message, t.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1005", "", message);
            writeOutputStream(false, BarFileUtils.CODE_BAR_INSTALL_FAILED,
                    UriUtils.SCHEME_LOCALCELL + ":/" + box.getName(), message);
            progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            return;
        } finally {
            try {
                if (Files.exists(barFilePath) && !Files.deleteIfExists(barFilePath)) {
                    log.info("Failed to remove bar file. [" + barFilePath.toAbsolutePath().toString() + "].");
                }
            } catch (IOException e) {
                log.info("Failed to remove bar file. [" + barFilePath.toAbsolutePath().toString() + "].");
            }
        }
        writeOutputStream(false, BarFileUtils.CODE_BAR_INSTALL_COMPLETED,
                UriUtils.SCHEME_LOCALCELL + ":/" + box.getName());
        progressInfo.setStatus(ProgressInfo.STATUS.COMPLETED);
        progressInfo.setEndTime();
        BarFileUtils.writeToProgressCache(true, progressInfo);
    }

    @SuppressWarnings("unchecked")
    private void createCellCtlObjects(BarFile barFile, ObjectMapper mapper, String jsonFileName, Class jsonClazz) {
        if (!barFile.exists(jsonFileName)) {
            return;
        }
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_STARTED, jsonFileName);
        try {
            IJSONMappedObjects objects =
                    (IJSONMappedObjects) mapper.readValue(barFile.getReader(jsonFileName), jsonClazz);
            if (objects.getObjectsSize() <= 0) {
                return;
            }
            if (BarFile.RELATIONS_JSON.equals(jsonFileName)) {
                createRelations((JSONRelations) objects);
            } else if (BarFile.ROLES_JSON.equals(jsonFileName)) {
                createRoles((JSONRoles) objects);
            } else if (BarFile.EXTROLES_JSON.equals(jsonFileName)) {
                createExtRoles((JSONExtRoles) objects);
            } else if (BarFile.RULES_JSON.equals(jsonFileName)) {
                createRules((JSONRules) objects);
            } else if (BarFile.LINKS_JSON.equals(jsonFileName)) {
                createLinks((JSONLinks) objects);
            }
        } catch (JsonParseException | JsonMappingException e) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonFileName);
        } catch (IOException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params(e.getMessage());
        }
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, jsonFileName);
    }

    /**
     * 10_$relations.jsonに定義されているRelation情報をESへ登録する.
     * @param jsonMapObjects JSONファイルから読み込んだJSONMapオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createRelations(JSONRelations jsonMapObjects) {
        for (JSONRelation jsonMapObject : jsonMapObjects.getRelations()) {
            JSONObject json = jsonMapObject.getJson();
            json.put("_Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            entityResource.setEntitySetName(Relation.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            // Relationの登録
            EntityResponse res = entityResource.getOdataProducer().createEntity(Relation.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * 20_$roles.jsonに定義されているRole情報をESへ登録する.
     * @param jsonMapObjects JSONファイルから読み込んだJSONMapオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createRoles(JSONRoles jsonMapObjects) {
        for (JSONRole jsonMapObject : jsonMapObjects.getRoles()) {
            JSONObject json = jsonMapObject.getJson();
            json.put("_Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            entityResource.setEntitySetName(Role.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            // Roleの登録
            EntityResponse res = entityResource.getOdataProducer().createEntity(Role.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * 30_$extroles.jsonに定義されているExtRole情報をESへ登録する.
     * @param jsonMapObjects JSONファイルから読み込んだJSONMapオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createExtRoles(JSONExtRoles jsonMapObjects) {
        for (JSONExtRole jsonMapObject : jsonMapObjects.getExtRoles()) {
            JSONObject json = jsonMapObject.getJson();
            String url = (String) json.get(ExtRole.EDM_TYPE_NAME);
            json.put(ExtRole.EDM_TYPE_NAME, url);
            json.put("_Relation._Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            entityResource.setEntitySetName(ExtRole.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            // ExtRoleの登録
            EntityResponse res = entityResource.getOdataProducer().createEntity(ExtRole.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * 50_rules.jsonに定義されているRule情報をESへ登録する.
     * @param jsonMapObjects JSONファイルから読み込んだJSONMapオブジェクト
     */
    @SuppressWarnings("unchecked")
    private void createRules(JSONRules jsonMapObjects) {
        for (JSONRule jsonMapObject : jsonMapObjects.getRules()) {
            JSONObject json = jsonMapObject.getJson();
            log.debug("createRules: " + json.toString());
            json.put("_Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            //EntityにRuleを登録して, afterCreateでRuleManagerにも登録
            entityResource.setEntitySetName(Rule.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            // Ruleの登録
            EntityResponse res = entityResource.getOdataProducer().createEntity(Rule.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * 70_$links.jsonに定義されているリンク情報をESへ登録する.
     * @param jsonMapObjects JSONファイルから読み込んだJSONMapオブジェクト
     */
    private void createLinks(JSONLinks jsonMapObjects) {
        for (JSONLink jsonMapObject : jsonMapObjects.getLinks()) {
            Map<String, String> fromNameMap = jsonMapObject.getFromName();
            String fromkey =
                    BarFileUtils.getComplexKeyName(jsonMapObject.getFromType(), fromNameMap, box.getName());
            OEntityKey fromOEKey = OEntityKey.parse(fromkey);
            OEntityId sourceEntity = OEntityIds.create(jsonMapObject.getFromType(), fromOEKey);
            String targetNavProp = jsonMapObject.getNavPropToType();

            // リンク作成前処理
            entityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

            Map<String, String> toNameMap = jsonMapObject.getToName();
            String tokey =
                    BarFileUtils.getComplexKeyName(jsonMapObject.getToType(), toNameMap, box.getName());
            OEntityKey toOEKey = OEntityKey.parse(tokey);
            OEntityId newTargetEntity = OEntityIds.create(jsonMapObject.getToType(), toOEKey);
            // $linksの登録
            entityResource.getOdataProducer().createLink(sourceEntity, targetNavProp, newTargetEntity);

            // post event
            String keyString = AbstractODataResource.replaceDummyKeyToNull(fromOEKey.toString());
            String targetKeyString = AbstractODataResource.replaceDummyKeyToNull(toOEKey.toString());
            String object = String.format("%s:/__ctl/%s%s/$links/%s%s",
                    UriUtils.SCHEME_LOCALCELL, sourceEntity.getEntitySetName(),
                    keyString, targetNavProp, targetKeyString);
            String info = "box install";
            String type = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                    + sourceEntity.getEntitySetName() + PersoniumEventType.SEPALATOR
                    + PersoniumEventType.Operation.LINK + PersoniumEventType.SEPALATOR
                    + newTargetEntity.getEntitySetName() + PersoniumEventType.SEPALATOR
                    + PersoniumEventType.Operation.CREATE;
            PersoniumEvent ev = new PersoniumEvent(type, object, info, this.requestKey);
            EventBus bus = box.getCell().getEventBus();
            bus.post(ev);
        }
    }

    /**
     * Post event to EventBus.
     * @param res
     */
    private void postCellCtlCreateEvent(EntityResponse res) {
        String name = res.getEntity().getEntitySetName();
        String keyString = AbstractODataResource.replaceDummyKeyToNull(res.getEntity().getEntityKey().toKeyString());
        String object = String.format("%s:/__ctl/%s%s", UriUtils.SCHEME_LOCALCELL, name, keyString);
        String info = "box install";
        String type = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                + name + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.CREATE;
        PersoniumEvent ev = new PersoniumEvent(type, object, info, requestKey);
        EventBus bus = box.getCell().getEventBus();
        bus.post(ev);
    }

    /**
     * 90_rootprops_xmlを解析してCollectoin/ACL/WebDAV等の登録処理を行う.
     * @param rootPropsName 90_rootprops_xmlのbarファイル内パス名
     * @param bufferedReader 入力ストリームReader
     */
    protected void registXmlEntry(String rootPropsName, BufferedReader bufferedReader) {
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_STARTED, rootPropsName);
        try {
            // XMLパーサ(StAX,SAX,DOM)にInputStreamをそのまま渡すとファイル一覧の取得処理が
            // 中断してしまうため暫定対処としてバッファに格納してからパースする
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                buf.append(str);
            }

            Multistatus multiStatus = Multistatus.unmarshal(new ByteArrayInputStream(buf.toString().getBytes()));

            // 90_rootprops.xmlの定義内容について妥当性検証を行う。
            // 事前に検証することで、ゴミデータが作られないようにする。
            validateCollectionDefinitions(multiStatus, rootPropsName);
            for (Response response : multiStatus.getResponse()) {
                String collectionType = DavCmp.TYPE_COL_WEBDAV;
                boolean hasCollection = false;
                boolean isBox = false;

                List<String> hrefs = response.getHref();
                String href = hrefs.get(0);
                if (href.equals(LOCALBOX_NO_SLUSH)) {
                    href = UriUtils.SCHEME_BOX_URI;
                }
                if (href.equals(UriUtils.SCHEME_BOX_URI)) {
                    isBox = true;
                }
                String collectionUrl = null;
                collectionUrl = href.replaceFirst(UriUtils.SCHEME_BOX_URI, box.getUrl());

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
                            if (nodeName.equals("p:odata")) {
                                collectionType = DavCmp.TYPE_COL_ODATA;
                            } else if (nodeName.equals("p:service")) {
                                collectionType = DavCmp.TYPE_COL_SVC;
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
                            if (!BarFileUtils.aclNameSpaceValidate(rootPropsName, element, box.getSchema())) {
                                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2007");
                                log.info(detail.getDetailMessage() + " [" + rootPropsName + "]");
                                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
                            }
                            aclElement = element;
                            continue;
                        }
                        propElements.add(element);
                    }
                }

                String entryName = CONTENTS_DIR + href.replaceFirst(UriUtils.SCHEME_BOX_URI, "");
                if (isBox) {
                    // For Box, collection and ACL registration.
                    davCmpMap.put(entryName, boxCmp);
                    registBoxAclAndProppatch(this.box, aclElement, propElements, collectionUrl);
                } else if (hasCollection) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    // コレクションの場合、コレクション、ACL、PROPPATH登録
                    log.info(entryName);
                    createCollection(collectionUrl, entryName, this.box.getCell(), this.box, collectionType, aclElement,
                            propElements);
                } else {
                    // WebDAVファイル
                    this.davFileContentTypeMap.put(entryName, contentType);
                    this.davFileAclMap.put(entryName, aclElement);
                    this.davFilePropsMap.put(entryName, propElements);
                }
            }
        } catch (PersoniumCoreException e) {
            log.info("PersoniumCoreException: " + e.getMessage());
            throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(e.getMessage());
        } catch (Exception ex) {
            String message = getErrorMessage(ex);
            log.info("XMLParseException: " + message, ex.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(message);
        }
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, rootPropsName);
    }

    /**
     * 90_rootprops.xmlに定義されたpathの階層構造に矛盾がないことを検証する.
     * @param multiStatus 90_rootprops.xmlから読み込んだJAXBオブジェクト
     * @param rootPropsName 現在処理中のエントリ名(ログ出力用)
     */
    private void validateCollectionDefinitions(Multistatus multiStatus, String rootPropsName) {

        // XML定義を読み込んで、href要素のパス定義とタイプ（ODataコレクション/WebDAVコレクション/サービスコレクション、WebDAVファイル、サービスソース)を取得する。
        Map<String, String> pathMap = new LinkedHashMap<String, String>();
        for (Response response : multiStatus.getResponse()) {
            List<String> hrefs = response.getHref();
            if (hrefs.size() != 1) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2008");
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            String href = hrefs.get(0);
            // href属性値がない場合は定義エラーとみなす。
            if (href == null || href.length() == 0) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2009");
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            // href属性値としてlocalboxで始まらない場合は定義エラーとみなす。
            if (!href.startsWith(LOCALBOX_NO_SLUSH)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail(
                        "PL-BI-2010", LOCALBOX_NO_SLUSH, href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            // 定義されたパスの種別を選別する。不正なパス種別が指定された場合は異常終了する（ログ出力は不要）。
            String collectionType = getCollectionType(rootPropsName, response);
            switch (collectionType) {
                case DavCmp.TYPE_COL_WEBDAV:
                case DavCmp.TYPE_COL_ODATA:
                case DavCmp.TYPE_COL_SVC:
                    if (href.endsWith("/")) {
                        href = href.substring(0, href.length() - 1);
                    }
                    break;
                default:
                    break;
            }
            // パス定義が重複している場合は同じデータが登録されてしまうため定義エラーとする。
            // パス末尾の"/"指定有無の条件を無視するため、このタイミングでチェックする。
            if (pathMap.containsKey(href)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2011", href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            pathMap.put(href, collectionType);
        }
        // 読み込んだパス定義をもとにCollectionパスの妥当性を検証する。
        // ・共通：Boxルートの定義は必須とする
        // ・共通： パス階層構造に矛盾がないこと
        // ・ODataコレクションの場合： コレクション配下のパス定義が存在しないこと
        // ・Serviceコレクションの場合： コレクション配下に "__src" のパス定義が存在すること
        Set<String> keySet = pathMap.keySet();
        for (Entry<String, String> entry : pathMap.entrySet()) {
            String href = entry.getKey();
            String currentCollectionType = entry.getValue();
            int upperPathposition = href.lastIndexOf("/");
            if (upperPathposition < 0) { // "dcbox:"のパスはチェック対象外のためスキップする
                continue;
            }
            // チェック対象の上位階層がパス情報として定義されていない場合は定義エラーとする。
            // Boxルートパスが定義されていない場合も同様に定義エラーとする。
            String upper = href.substring(0, upperPathposition);
            if (!keySet.contains(upper)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2012", upper);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            String upperCollectionType = pathMap.get(upper);
            String resourceName = href.substring(upperPathposition + 1, href.length());
            if (DavCmp.TYPE_COL_ODATA.equals(upperCollectionType)) {
                // ODataコレクション：コレクション配下にコレクション／ファイルが定義されていた場合は定義エラーとする。
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2013", href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            } else if (DavCmp.TYPE_COL_SVC.equals(upperCollectionType)) {
                // Serviceコレクション：コレクション配下にコレクション／ファイルが定義されていた場合は定義エラーとする。
                // ただし、"__src"のみは例外として除外する。
                if (!("__src".equals(resourceName) && DavCmp.TYPE_COL_WEBDAV.equals(currentCollectionType))) {
                    PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2014", href);
                    throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
                }
            } else if (DavCmp.TYPE_DAV_FILE.equals(upperCollectionType)) {
                // WebDAVファイル／Serviceソース配下にコレクション／ファイルが定義されていた場合は定義エラーとする。
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2015", href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            // カレントがServiceコレクションの場合、直下のパスに"__src"が定義されていない場合は定義エラーとする。
            if (DavCmp.TYPE_COL_SVC.equals(currentCollectionType)) {
                String srcPath = href + "/__src";
                if (!keySet.contains(srcPath) || !DavCmp.TYPE_COL_WEBDAV.equals(pathMap.get(srcPath))) {
                    PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2016", href);
                    throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
                }
            }

            // リソース名として正しいことを確認する（コレクション／ファイルの名前フォーマットは共通）。
            if (!DavCommon.isValidResourceName(resourceName)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2017", resourceName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
        }
    }

    /**
     * 90_rootprops.xml内の各responseタグに定義されているパスのコレクション種別を取得する。
     * @param rootPropsName 現在処理中のエントリ名(ログ出力用)
     * @param response 処理対象のresponseタグ用JAXBオブジェクト
     * @return 定義内容に応じたコレクション種別の値を返す。
     *         WebDAVファイル、ServiceソースはWebDAVファイルとして返す。
     *         許可されていないコレクションの種別が定義されていた場合は未定義として返す。
     */
    private String getCollectionType(String rootPropsName, Response response) {
        // <propstat>要素の配下を辿って定義されているコレクションのタイプを取得する
        // －prop/resourcetype/collecton のDOMノードパスが存在する場合はコレクション定義とみなす
        // この際、"p:odata" または "p:service" のDOMノードパスが存在しない場合はWebDAVコレクション定義とみなす
        // - 上記に当てはまらない場合はWebDAvファイルまたはサービスソースとみなす
        for (Propstat propstat : response.getPropstat()) {
            Prop prop = propstat.getProp();
            Resourcetype resourceType = prop.getResourcetype();
            if (resourceType != null && resourceType.getCollection() != null) {
                List<Element> elements = resourceType.getAny();
                for (Element element : elements) {
                    String nodeName = element.getNodeName();
                    if (nodeName.equals("p:odata")) {
                        return DavCmp.TYPE_COL_ODATA;
                    } else if (nodeName.equals("p:service")) {
                        return DavCmp.TYPE_COL_SVC;
                    } else {
                        PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2018", nodeName);
                        throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
                    }
                }
            } else {
                return DavCmp.TYPE_DAV_FILE;
            }
        }
        return DavCmp.TYPE_COL_WEBDAV;
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
            Element convElement =
                    BarFileUtils.convertToRoleInstanceUrl(aclElement, baseUrl, box.getCell().getName(), box.getName());
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(PersoniumCoreUtils.nodeToString(convElement));
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

    private void createCollection(String collectionUrl,
            String entryName,
            Cell parentCell,
            Box parentBox,
            String collectionType,
            Element aclElement,
            List<Element> propElements) {
        int index;
        if (parentCell == null || parentBox == null) {
            return;
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
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
            } else if (parentCmp.getType().equals(DavCmp.TYPE_COL_ODATA)) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
            } else if (parentCmp.getType().equals(DavCmp.TYPE_COL_SVC)) {
                String crrName = entryName.substring(index + 1, entryName.length() - 1);
                if (!"__src".equals(crrName)) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
                }
            }
        }

        String collectionName = "";
        index = collectionUrl.lastIndexOf("/");
        collectionName = collectionUrl.substring(index + 1);

        DavCmp collectionCmp = parentCmp.getChild(collectionName);
        collectionCmp.mkcol(collectionType);

        this.davCmpMap.put(entryName, collectionCmp);

        // ACL登録
        if (aclElement != null) {
            Element convElement =
                    BarFileUtils.convertToRoleInstanceUrl(aclElement, baseUrl, box.getCell().getName(), box.getName());
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(PersoniumCoreUtils.nodeToString(convElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            collectionCmp.acl(aclXml);
        }

        // PROPPATCH登録
        registProppatch(collectionCmp, propElements, collectionUrl);
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

    /**
     *
     * @param exception
     */
    private void writeOutputStream(PersoniumBarException exception) {
        String code = exception.getCode();
        String path = exception.getPath();
        String message = exception.getMessage();
        BarFileUtils.outputEventBus(event, eventBus, code, path, message);
        BarFileUtils.writeToProgress(true, progressInfo, code, message);
        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
        log.info(output);
    }

    /**
     * エラー情報をキャッシュに記録する.
     */
    public void writeErrorProgressCache() {
        if (progressInfo != null) {
            progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            progressInfo.setEndTime();
            BarFileUtils.writeToProgressCache(true, progressInfo);
        }
    }
}
