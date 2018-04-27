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
package io.personium.core.bar;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.bar.jackson.JSONManifest;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.odata.PersoniumOptionsQueryParser;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.rs.odata.ODataResource;

/**
 * barファイルインストール処理を行うクラス.
 */
public class BarFileInstaller {
    /**
     * ログ用オブジェクト.
     */
    static Logger log = LoggerFactory.getLogger(BarFileInstaller.class);

    static final long MB = 1024 * 1024;
    static final int BUF_SIZE = 1024; // for output response.

    private final Cell cell;
    private String boxName;
    private ODataEntityResource oDataEntityResource;
    private UriInfo uriInfo;

    private String barTempDir = PersoniumUnitConfig.getBarInstallTempDir();

    private JSONObject manifestJson;

    /**
     * コンストラクタ.
     * @param cell
     *            セルオブジェクト
     * @param boxName
     *            ボックス名
     * @param oDataEntityResource oDataEntityResource
     * @param uriInfo UriInfo
     */
    public BarFileInstaller(
            final Cell cell,
            final String boxName,
            final ODataEntityResource oDataEntityResource,
            final UriInfo uriInfo) {
        this.cell = cell;
        this.boxName = boxName;
        this.oDataEntityResource = oDataEntityResource;
        this.uriInfo = uriInfo;
    }

    /**
     * barファイルインストールを行うメソッド.
     * @param headers
     *            Httpヘッダーを格納したMAP
     * @param inStream
     *            Httpリクエストボディ用InputStream
     * @param requestKey イベントログに出力するRequestKeyフィールドの値
     * @return レスポンス
     */
    public Response barFileInstall(Map<String, String> headers,
            InputStream inStream, String requestKey) {

        // 事前チェック
        checkPreConditions(headers);

        // barファイルの格納
        File file = storeTemporaryBarFile(inStream);

        // bar_version : 2
        if (execVer2Process(file, requestKey)) {
            // レスポンスの返却
            ResponseBuilder res = Response.status(HttpStatus.SC_ACCEPTED);
            res.header(HttpHeaders.LOCATION, this.cell.getUrl() + boxName);
            return res.build();
        }

        BarFileReadRunner runner = null;
        try {
            // barファイルのバリデート
            long entryCount = checkBarFileContents(file);

            // BoxおよびスキーマURLの重複チェック
            checkDuplicateBoxAndSchema((String) this.manifestJson.get("Schema"));

            // Boxの作成
            // ここまでのエラーは400番台のエラーとなり、Boxは作成されないため、Boxメタデータ（キャッシュ）には書き込まずに終了する。
            runner = new BarFileReadRunner(file, this.cell, this.boxName,
                    this.oDataEntityResource, this.oDataEntityResource.getOdataProducer(),
                    Box.EDM_TYPE_NAME, this.uriInfo, requestKey);
            runner.createBox(this.manifestJson);

            // barファイル内のエントリ数を設定（この時点でProgressInfoを作成）
            runner.setEntryCount(entryCount);
            runner.writeInitProgressCache();

        } catch (PersoniumCoreException e) {
            if (null != runner) {
                runner.writeErrorProgressCache();
            }
            removeBarFile(file);
            throw e;
        } catch (Exception e) {
            if (null != runner) {
                runner.writeErrorProgressCache();
            }
            removeBarFile(file);
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        } finally {
            IOUtils.closeQuietly(inStream);
        }

        // 非同期実行
        // TODO 多重実行時を考慮してスレッドプール化するなどの対策が必要
        Thread thread = new Thread(runner);
        thread.start();

        // レスポンスの返却
        ResponseBuilder res = Response.status(HttpStatus.SC_ACCEPTED);
        res.header(HttpHeaders.LOCATION, this.cell.getUrl() + boxName);
        return res.build();
    }

    private boolean execVer2Process(File file, String requestKey) {
        long entryCount;
        String schema;
        try (BarFile barFile = BarFile.newInstance(file.toPath())) {
            if (!barFile.exists(BarFile.MANIFEST_JSON)) {
                return false;
            }
            ObjectMapper mapper = new ObjectMapper();
            JSONManifest manifest = mapper.readValue(barFile.getReader(BarFile.MANIFEST_JSON), JSONManifest.class);
            if (StringUtils.isEmpty(manifest.getBarVersion())) {
                removeBarFile(file);
                throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params("bar_version");
            }
            int barVersion = Float.valueOf(manifest.getBarVersion()).intValue();
            if (barVersion == 2) {
                checkBarFileSize(file);
                barFile.checkStructure();
                // Use FileVisitor to check process recursively.
                FileVisitor<Path> visitor = new BarFileCheckVisitor();
                try {
                    Files.walkFileTree(barFile.getRootDirPath(), visitor);
                } catch (IOException e) {
                    removeBarFile(file);
                    throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params(e.getMessage());
                }
                // BoxおよびスキーマURLの重複チェック
                checkDuplicateBoxAndSchema(manifest.getSchema());

                entryCount = ((BarFileCheckVisitor) visitor).getEntryCount();
                schema = manifest.getSchema();
            } else {
                removeBarFile(file);
                throw PersoniumCoreException.BarInstall.BAR_FILE_STRUCTURE_AND_VERSION_MISMATCH;
            }
        } catch (IOException e) {
            removeBarFile(file);
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params(e.getMessage());
        } catch (NumberFormatException e) {
            removeBarFile(file);
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params("bar_version");
        }
        // TODO コンストラクタでやってるBox作成とかはやっぱrunの中でやるべきじゃね？
        BarFileInstallRunner runner = new BarFileInstallRunner(file.toPath(), entryCount,
                boxName, schema, uriInfo, oDataEntityResource, requestKey);
        // TODO 多重実行時を考慮してスレッドプール化するなどの対策が必要
        Thread thread = new Thread(runner);
        thread.start();
        removeBarFile(file);
        return true;
    }

    private void removeBarFile(File barFile) {
        if (barFile.exists() && !barFile.delete()) {
            log.warn("Failed to remove bar file. [" + barFile.getAbsolutePath() + "].");
        }
    }

    /**
     * barインストール受付時の事前チェックを行うメソッド.
     * @param headers HTTPヘッダー
     */
    private void checkPreConditions(Map<String, String> headers) {
        // [403]アクセス制御
        AccessContext accessContext = this.oDataEntityResource.getAccessContext();
        ODataResource odataResource = this.oDataEntityResource.getOdataResource();
        odataResource.checkAccessContext(accessContext, CellPrivilege.BOX_BAR_INSTALL);

        // [400]リクエストヘッダの形式チェック
        checkHeaders(headers);

    }

    /**
     * Httpヘッダーのチェック.
     * @param headers
     *            Httpヘッダーを格納したMAP
     */
    private void checkHeaders(Map<String, String> headers) {
        // Content-Type: application/zip固定
        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        if (!"application/zip".equals(contentType)) {
            throw PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR
                    .params(HttpHeaders.CONTENT_TYPE);
        }
    }

    /**
     * システムプロパティに設定されているbarファイルの最大ファイルサイズ(MB)を取得する。
     * @return io.personium.core.bar.file.maxSize
     */
    protected long getMaxBarFileSize() {
        long maxBarFileSize;
        try {
            maxBarFileSize = Long.parseLong(PersoniumUnitConfig
                    .get(PersoniumUnitConfig.BAR.BAR_FILE_MAX_SIZE));
        } catch (NumberFormatException ne) {
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
        return maxBarFileSize;
    }

    /**
     * プロパティファイルからBARファイル内の最大ファイルサイズ(MB)を取得する。
     * @return io.personium.core.bar.entry.maxSize
     */
    protected long getMaxBarEntryFileSize() {
        long maxBarFileSize;
        try {
            maxBarFileSize = Long.parseLong(PersoniumUnitConfig
                    .get(PersoniumUnitConfig.BAR.BAR_ENTRY_MAX_SIZE));
        } catch (NumberFormatException ne) {
            log.info("NumberFormatException" + PersoniumUnitConfig
                    .get(PersoniumUnitConfig.BAR.BAR_ENTRY_MAX_SIZE));
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
        return maxBarFileSize;
    }

    /**
     * ファイルディスクリプタの同期.
     * @param fd ファイルディスクリプタ
     * @throws SyncFailedException 同期に失敗
     */
    public void sync(FileDescriptor fd) throws SyncFailedException {
        fd.sync();
    }

    /**
     * Httpリクエストボディからbarファイルを読み込み、一時領域へ格納する.
     * @param inStream Httpリクエストボディ用InputStreamオブジェクト
     * @return 一時領域に格納したbarファイルのFileオブジェクト
     */
    private File storeTemporaryBarFile(InputStream inStream) {

        // barファイル格納先のディレクトリが存在しなければ作成する。
        String unitUserName = BarFileUtils.getUnitUserName(this.cell.getOwner());
        File barFileDir = new File(new File(barTempDir, unitUserName), "bar");
        if (!barFileDir.exists() && !barFileDir.mkdirs()) {
            String message = "unable create directory: " + barFileDir.getAbsolutePath();
            throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(message);
        }

        // barファイルをNFS上に格納する。
        String prefix = this.cell.getId() + "_" + this.boxName;
        File barFile = null;
        OutputStream outStream = null;
        try {
            barFile = File.createTempFile(prefix, ".bar", barFileDir);
            barFile.deleteOnExit(); // VM異常終了時に削除する設定
            outStream = new FileOutputStream(barFile);
            IOUtils.copyLarge(inStream, outStream);
        } catch (IOException e) {
            String message = "unable save bar file: %s";
            if (barFile == null) {
                message = String.format(message, barFileDir + prefix + "XXX.bar");
            } else {
                message = String.format(message, barFile.getAbsolutePath());
            }
            throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(message);
        } finally {
            if (null != outStream && PersoniumUnitConfig.getFsyncEnabled()) {
                try {
                    sync(((FileOutputStream) outStream).getFD());
                } catch (Exception e) {
                    throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(e.getMessage());
                }
            }
            IOUtils.closeQuietly(outStream);
        }
        return barFile;
    }

    /**
     * barファイルを読み込み、バリデーションを行うメソッド.
     * <ul>
     * <li>barファイル内のエントリ数（ファイルのみ）をカウントする。</li>
     * <li>barファイル内の各エントリのファイルサイズの上限値をチェックする。</li>
     * <li>TODO barファイル内の各エントリの順序をチェックする。</li>
     * </ul>.
     * @param barFile 一時領域に保存したbarファイルのFileオブジェクト
     * @returns barファイル内のエントリ（ファイル）数
     */
    private long checkBarFileContents(File barFile) {

        // barファイルサイズチェック
        checkBarFileSize(barFile);

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(barFile, "UTF-8");
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            ZipArchiveEntry zae = null;
            long entryCount = 0;
            String entryName = null;
            try {
                long maxBarEntryFileSize = getMaxBarEntryFileSize();
                // 必須ファイルチェック用のデータをセットアップ
                Map<String, String> requiredBarFiles = setupBarFileOrder();
                while (entries.hasMoreElements()) {
                    zae = entries.nextElement();
                    entryName = zae.getName();
                    log.info("read: " + entryName);
                    if (!zae.isDirectory()) {
                        // インストール進捗率算出用の母数としてbarファイル内のファイル数をカウント
                        entryCount++;

                        // barファイル内エントリのファイルサイズチェック
                        checkBarFileEntrySize(zae, entryName, maxBarEntryFileSize);

                        // Box生成用にマニフェストファイルのみを読み込む。
                        if (zae.getName().endsWith("/" + BarFileReadRunner.MANIFEST_JSON)) {
                            checkAndReadManifest(entryName, zae, zipFile);
                        }
                    }
                    // barファイルの必須ファイルチェック（格納順はインストール時にチェックする）
                    if (!checkBarFileStructures(zae, requiredBarFiles)) {
                        throw PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.params(entryName);
                    }
                }
                if (!requiredBarFiles.isEmpty()) {
                    StringBuilder entryNames = new StringBuilder();
                    Object[] requiredFileNames = requiredBarFiles.keySet().toArray();
                    for (int i = 0; i < requiredFileNames.length; i++) {
                        if (i > 0) {
                            entryNames.append(" " + requiredFileNames[i]);
                        } else {
                            entryNames.append(requiredFileNames[i]);
                        }
                    }
                    throw PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.params(entryNames.toString());
                }
                return entryCount;
            } catch (PersoniumCoreException e) {
                throw e;
            } catch (Exception e) {
                log.info(e.getMessage(), e.fillInStackTrace());
                throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params(entryName);
            }
        } catch (FileNotFoundException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params("barFile");
        } catch (ZipException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params(e.getMessage());
        } catch (IOException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params(e.getMessage());
        } catch (PersoniumCoreException e) {
            throw e;
        } catch (RuntimeException e) {
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        } finally {
            ZipFile.closeQuietly(zipFile);
        }
    }

    private void checkAndReadManifest(String entryName, ZipArchiveEntry zae, ZipFile zipFile) throws IOException {
        InputStream inStream = zipFile.getInputStream(zae);
        try {
            JSONManifest manifest =
                    BarFileUtils.readJsonEntry(inStream, entryName, JSONManifest.class);
            if (!manifest.checkSchema()) {
                throw PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.params(entryName);
            }
            this.manifestJson = manifest.getJson();
        } finally {
            IOUtils.closeQuietly(inStream);
        }
    }

    /**
     * barファイル内エントリのファイルサイズチェック.
     * @param zae barファイル内エントリ
     * @param entryName エントリ名
     * @param maxBarEntryFileSize エントリのファイルサイズ
     */
    protected void checkBarFileEntrySize(ZipArchiveEntry zae, String entryName,
            long maxBarEntryFileSize) {
        // [400]barファイル内エントリのファイルサイズが上限値を超えている
        if (zae.getSize() > (long) (maxBarEntryFileSize * MB)) {
            String message = "Bar file entry size too large invalid file [%s: %sB]";
            log.info(String.format(message, entryName, String.valueOf(zae.getSize())));
            throw PersoniumCoreException.BarInstall.BAR_FILE_ENTRY_SIZE_TOO_LARGE
                    .params(entryName, String.valueOf(zae.getSize()));
        }
    }

    /**
     * barファイルサイズチェック.
     * @param barFile barファイル
     */
    protected void checkBarFileSize(File barFile) {
        // [400]barファイルのファイルサイズが上限値を超えている
        long maxBarFileSize = getMaxBarFileSize();
        if (barFile.length() > (long) (maxBarFileSize * MB)) {
            String message = "Bar file size too large invalid file [%sB]";
            log.info(String.format(message, String.valueOf(barFile.length())));
            throw PersoniumCoreException.BarInstall.BAR_FILE_SIZE_TOO_LARGE
                    .params(String.valueOf(barFile.length()));
        }
    }

    /**
     * barファイルの必須ファイル.
     */
    private Map<String, String> setupBarFileOrder() {
        Map<String, String> requiredBarFiles = new LinkedHashMap<String, String>();
        requiredBarFiles.put("bar/", BarFileReadRunner.ROOT_DIR);
        requiredBarFiles.put("bar/00_meta/", BarFileReadRunner.META_DIR);
        requiredBarFiles.put("bar/00_meta/00_manifest.json", BarFileReadRunner.MANIFEST_JSON);
        requiredBarFiles.put("bar/00_meta/90_rootprops.xml", BarFileReadRunner.ROOTPROPS_XML);
        return requiredBarFiles;
    }

    /**
     * barファイルの構造をチェックする.
     */
    private boolean checkBarFileStructures(ZipArchiveEntry zae, Map<String, String> requiredBarFiles)
            throws UnsupportedEncodingException, ParseException {

        String entryName = zae.getName(); // ex. "bar/00_meta/00_manifest.json"
        if (requiredBarFiles.containsKey(entryName)) {
            requiredBarFiles.remove(entryName);
        }
        return true;
    }

    /**
     * インストール先Boxが既に登録されているかどうか、マニフェストに定義されているスキーマURLが既に登録されているかどうかをチェックする.
     */
    private void checkDuplicateBoxAndSchema(String schema) {
        PersoniumODataProducer producer = oDataEntityResource.getOdataProducer();

        // [400]既に同じscheme URLが設定されたBoxが存在している
        // 同じスキーマURLを持つBoxを検索し、1件以上ヒットした場合はエラーとする。
        BoolCommonExpression filter = PersoniumOptionsQueryParser.parseFilter("Schema eq '" + schema + "'");
        QueryInfo query = new QueryInfo(null, null, null, filter, null, null, null, null, null);
        if (producer.getEntitiesCount(Box.EDM_TYPE_NAME, query).getCount() > 0) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.params(schema);
        }

        // [405]既に同名のBoxが存在している
        // Box名のみで検索を行い、スキーマ有無に係わらず検索にヒットした場合はエラーとする。
        filter = PersoniumOptionsQueryParser.parseFilter("Name eq '" + this.boxName + "'");
        query = new QueryInfo(null, null, null, filter, null, null, null, null, null);
        if (producer.getEntitiesCount(Box.EDM_TYPE_NAME, query).getCount() > 0) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_BOX_ALREADY_EXISTS.params(this.boxName);
        }

        log.info("Install target Box is not found, able to install.");
    }

    /**
     * セル名の取得.
     * @return セル名
     */
    public String getCellName() {
        return cell.getName();
    }

    /**
     * ODataProducerの取得.
     * @return ODataProducer
     */
    public PersoniumODataProducer getOdataProducer() {
        return oDataEntityResource.getOdataProducer();
    }

    /**
     * ODataEntityResourceの取得.
     * @return ODataEntityResource
     */
    public ODataEntityResource getODataEntityResource() {
        return oDataEntityResource;
    }

    /**
     * URIの取得.
     * @return UriInfo
     */
    public UriInfo getUriInfo() {
        return uriInfo;
    }

}
