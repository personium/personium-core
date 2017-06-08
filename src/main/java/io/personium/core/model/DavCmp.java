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

package io.personium.core.model;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.Propertyupdate;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.odata.PersoniumODataProducer;


/**
 * An delegate object interface for JaxRS Resources handling DAV related persistence.
 */
public interface DavCmp {
    /**
     * Type representing non-existing path.
     */
    String TYPE_NULL = "null";
    /**
     * Type representing a plain WebDAV collection.
     */
    String TYPE_COL_WEBDAV = "col.webdav";
    /**
     * Type representing a WebDAV Collection extended as ODataSvc.
     */
    String TYPE_COL_ODATA = "col.odata";
    /**
     * Type representing a WebDAV Collection extended as Box.
     */
    String TYPE_COL_BOX = "col.box";
    /**
     * Type representing a WebDAV Collection extended as Engine Service.
     */
    String TYPE_COL_SVC = "col.svc";
    /**
     * Type representing a WebDAV file.
     */
    String TYPE_DAV_FILE = "dav.file";
    /**
     * Type representing Cell.
     */
    String TYPE_CELL = "Cell";

    /**
     * source path for Engine service.
     */
    String SERVICE_SRC_COLLECTION = "__src";

    /**
     * DavNodeがDB上に存在するかどうか.
     * @return 存在する場合はtrue
     */
    boolean exists();

    /**
     * Davの管理データ情報を最新化する.
     */
    void load();

    /**
     * Davの管理データ情報を最新化する.<br />
     * 管理データが存在しない場合はエラーとする.
     */
    void loadAndCheckDavInconsistency();

    /**
     * @return acl
     */
    Acl getAcl();

    /**
     * @return properties
     */
    Map<String, String> getProperties();

    /**
     * @return Cell
     */
    Cell getCell();

    /**
     * @return Box
     */
    Box getBox();

    /**
     * @return Update time stamp
     */
    Long getUpdated();

    /**
     * @return Create time stamp
     */
    Long getPublished();

    /**
     * @return Content Length
     */
    Long getContentLength();

    /**
     * @return Content type
     */
    String getContentType();

    /**
     * @return Encryption type
     */
    String getEncryptionType();

    /**
     * @return true if Cell Level
     */
    boolean isCellLevel();

    /**
     * スキーマ認証レベル設定のgetter.
     * @return スキーマ認証レベル
     */
    String getConfidentialLevel();

    /**
     * ユニット昇格許可ユーザ設定取得のgetter.
     * @return ユニット昇格許可ユーザ設定
     */
    List<String> getOwnerRepresentativeAccounts();

    /**
     * 指定した名前の子パスを担当する部品を返す.
     * @param name 子供パスのパスコンポーネント名
     * @return 子パスを担当する部品
     */
    DavCmp getChild(String name);

    /**
     * @return chilrdren DavCmp in the form of Map<path, DavCmp>
     */
    Map<String, DavCmp> getChildren();

    /**
     * 親パスを担当する部品を返す.
     * @return 親パスを担当する部品
     */
    DavCmp getParent();

    /**
     * 子供パスの部品の数を返す.
     * @return 子供パスの部品の数
     */
    int getChildrenCount();

    /**
     * タイプ文字列を返す.
     * @return タイプ文字列
     */
    String getType();

    /**
     * このオブジェクトが担当するパス文字列を返す.
     * @return このオブジェクトが担当するパス文字列
     */
    String getName();


    /**
     * このオブジェクトのnodeIdを返す.
     * @return nodeId
     */
    String getId();

    /**
     * 配下にデータがない場合はtrueを返す.
     * @return 配下にデータがない場合はtrue.
     */
    boolean isEmpty();

    /**
     * 配下にあるデータをすべて削除する.
     */
    void makeEmpty();

    /**
     * MKCOLメソッドの処理.
     * @param type タイプ
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder mkcol(String type);

    /**
     * ACLメソッドの処理.
     * @param reader Reader
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder acl(Reader reader);

    /**
     * PUTメソッドによるファイルの更新処理.
     * @param contentType Content-Typeヘッダ
     * @param inputStream リクエストボディ
     * @param etag Etag
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder putForUpdate(String contentType, InputStream inputStream, String etag);

    /**
     * PUTメソッドによるファイルの作成処理.
     * @param contentType Content-Typeヘッダ
     * @param inputStream リクエストボディ
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder putForCreate(String contentType, InputStream inputStream);

    /**
     * 子リソースとの紐づける.
     * @param name 子リソースのパスコンポーネント名
     * @param nodeId 子リソースのノードID
     * @param asof 更新時刻として残すべき時刻
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder linkChild(String name, String nodeId, Long asof);

    /**
     * 子リソースとの紐づきを削除する.
     * @param name 子リソース名
     * @param asof 削除時刻として残すべき時刻
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder unlinkChild(String name, Long asof);

    /**
     * process PROPPATCH method.
     * @param propUpdate PROPPATCH要求オブジェクト
     * @param url URL
     * @return 応答オブジェクト
     */
    Multistatus proppatch(Propertyupdate propUpdate, String url);

    /**
     * process DELETE method.
     * @param ifMatch If-Matchヘッダ
     * @param recursive set true to process recursively
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder delete(String ifMatch, boolean recursive);

    /**
     * process GET method.
     * @param rangeHeaderField Rangeヘッダ
     * @return JAX-RS ResponseBuilder
     */
    ResponseBuilder get(String rangeHeaderField);

    /**
     * データ操作用ODataProducerを返します.
     * @return ODataProducer
     */
    PersoniumODataProducer getODataProducer();

    /**
     * スキーマ操作用ODataProducerを返します.
     * @param cell Cell
     * @return ODataProducer
     */
    PersoniumODataProducer getSchemaODataProducer(Cell cell);

    /**
     * @return ETag String.
     */
    String getEtag();


    /**
     * MOVE処理を行う.
     * @param etag ETag value
     * @param overwrite 移動先のリソースを上書きするかどうか
     * @param davDestination 移動先の階層情報
     * @return ResponseBuilder レスポンス
     */
    ResponseBuilder move(String etag, String overwrite, DavDestination davDestination);

    /**
     * このDavNodeリソースのURLを返します.
     * @return URL文字列
     */
    String getUrl();

    /**
     * リソースに合わせてNotFoundの例外を返却する. <br />
     * リソースによってメッセージがことなるため、各リソースのクラスはこのメソッドをオーバーライドしてメッセージを定義すること。 <br />
     * メッセージの付加情報は、ここでは設定せずに呼び出し元で設定すること。
     * @return NotFound例外
     */
    PersoniumCoreException getNotFoundException();
}
