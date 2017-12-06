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
package io.personium.test.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.odata4j.core.ODataConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * JerseyTestFrameworkを利用したユニットテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Ignore
public class AbstractCase extends JerseyTest {

    /** ログオブジェクト. */
    private Log log;

    /** マスタートークン. */
    public static final String MASTER_TOKEN_NAME = PersoniumUnitConfig.getMasterToken();
    /** マスタートークン(Bearer + MASTER_TOKEN_NAME). */
    public static final String BEARER_MASTER_TOKEN = "Bearer " + MASTER_TOKEN_NAME;
    /** クエリフォーマット xml. */
    public static final String QUERY_FORMAT_ATOM = "$format=atom";
    /** クエリフォーマット json. */
    public static final String QUERY_FORMAT_JSON = "$format=json";
    /** __published. */
    public static final String PUBLISHED = "__published";
    /** __updated. */
    public static final String UPDATED = "__updated";
    /** __metadata. */
    public static final String METADATA = "__metadata";

    /** タイプ名Cell. */
    public static final String TYPE_CELL = "UnitCtl.Cell";

    /** 128文字の文字列. */
    public static final String STRING_LENGTH_128 = "1234567890123456789012345678901234567890"
            + "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567x";

    /** 128文字の文字列. */
    public static final String STRING_LENGTH_129 = "1234567890123456789012345678901234567890"
            + "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678x";

    /** リクエストヘッダ. */
    private HashMap<String, String> headers;

    /**
     * コンストラクタ.
     */
    protected AbstractCase() {
        log.debug("AbstractCase constructer");
    }

    /**
     * コンストラクタ.
     * @param value テスト対象パッケージ文字列
     */
    public AbstractCase(final String value) {
        super(value);
        log = LogFactory.getLog(AbstractCase.class);
        log.debug("======================" + this.getClass().getName() + "======================");
    }

    /**
     * コンストラクタ.
     * @param build WebAppDescriptor
     */
    public AbstractCase(WebAppDescriptor build) {
        super(build);
        log = LogFactory.getLog(AbstractCase.class);
        log.debug("======================" + this.getClass().getName() + "======================");
    }

    /**
     * リクエストヘッダをセットする.
     * @param newHeaders 設定するヘッダのHashMapオブジェクト
     */
    public final void setHeaders(final HashMap<String, String> newHeaders) {
        this.headers = newHeaders;
    }

    /**
     * Cell作成.
     * @param cellName Cell名
     * @return Cell作成時のレスポンスオブジェクト
     */
    @SuppressWarnings("unchecked")
    public final PersoniumResponse createCell(final String cellName) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);
        String data = requestBody.toJSONString();

        // リクエスト
        try {
            res = rest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME), data, requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return res;
    }

    /**
     * Create cell.
     * @param cellName Cell name
     * @param owner Owner
     * @return API response
     */
    public TResponse createCell(String cellName, String owner) {
        if (owner != null) {
            return Http.request("cell-createWithOwner.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("owner", owner)
                    .with("cellPath", cellName)
                    .returns().debug().statusCode(HttpStatus.SC_CREATED);
        } else {
            return Http.request("cell-create.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .returns().debug().statusCode(HttpStatus.SC_CREATED);
        }
    }

    /**
     * Create box.
     * @param cellName Cell name
     * @param boxName Box name
     * @return API response
     */
    public TResponse createBox(String cellName, String boxName) {
        return createBox(cellName, boxName, null);
    }

    /**
     * Create box.
     * @param cellName Cell name
     * @param boxName Box name
     * @param boxSchema Box schema
     * @return API response
     */
    public TResponse createBox(String cellName, String boxName, String boxSchema) {
        if (boxSchema != null) {
            return Http.request("cell/box-create-with-scheme.txt")
                    .with("cellPath", cellName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("boxPath", boxName)
                    .with("schema", boxSchema)
                    .returns().debug().statusCode(HttpStatus.SC_CREATED);
        } else {
            return Http.request("cell/box-create.txt")
                    .with("cellPath", cellName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("boxPath", boxName)
                    .returns().debug().statusCode(HttpStatus.SC_CREATED);
        }
    }

    /**
     * Create account.
     * Type is basic.
     * @param cellName Cell name
     * @param accountName Account name
     * @param password Password
     * @return API response
     */
    public TResponse createAccount(String cellName, String accountName, String password) {
        return Http.request("account-create.txt")
                .with("cellPath", cellName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("password", password)
                .with("username", accountName)
                .returns().debug().statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * Create role.
     * @param cellName Cell name
     * @param roleName Role name
     * @return API response
     */
    public TResponse createRole(String cellName, String roleName) {
        return createRole(cellName, roleName, null);
    }

    /**
     * Create role.
     * @param cellName Cell name
     * @param roleName Role name
     * @param boxName Box name
     * @return API response
     */
    @SuppressWarnings("unchecked")
    public TResponse createRole(String cellName, String roleName, String boxName) {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("Name", roleName);
        jsonBody.put("_Box.Name", boxName);

        return Http.request("role-create.txt")
                .with("cellPath", cellName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("body", jsonBody.toJSONString())
                .returns().debug().statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * Link account and role.
     * @param cellName Cell name
     * @param accountName Account name
     * @param roleUrl Role url
     * @return API response
     */
    public TResponse linkAccountAndRole(String cellName, String accountName, String roleUrl) {
        return Http.request("cell/link-account-role.txt")
                .with("cellPath", cellName)
                .with("username", accountName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleUrl", roleUrl)
                .returns().debug().statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Create service collection.
     * @param cellName Cell name
     * @param boxName Box name
     * @param path Path
     * @return API response
     */
    public TResponse createServiceCollection(String cellName, String boxName, String path) {
        return Http.request("box/mkcol-service-fullpath.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("path", path)
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .returns().debug().statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * Create webdav collection.
     * @param cellName Cell name
     * @param boxName Box name
     * @param path Path
     * @return API response
     */
    public TResponse createWebdavCollection(String cellName, String boxName, String path) {
        return Http.request("box/mkcol-normal-fullpath.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("path", path)
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .returns().debug().statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * Create odata collection.
     * @param cellName Cell name
     * @param boxName Box name
     * @param path Path
     * @return API response
     */
    public TResponse createOdataCollection(String cellName, String boxName, String path) {
        return Http.request("box/mkcol-odata.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("path", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns().debug().statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * URLを指定してGETを行う汎用メンバ.
     * @param url リクエスト先のURL
     * @return レスポンスオブジェクト
     */
    public final PersoniumResponse restGet(final String url) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        if (this.headers == null) {
            // setHeadersで設定されていない場合、リクエストヘッダをセット
            this.headers = new HashMap<String, String>();
            this.headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            this.headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        }
        try {
            // リクエスト
            res = rest.getAcceptEncodingGzip(url, this.headers);
            // res = rest.get(url, headers);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return res;
    }

    /**
     * URLを指定してPOSTを行う汎用メンバ.
     * @param url リクエスト先のURL
     * @param data postデータ
     * @return レスポンスオブジェクト
     */
    public final PersoniumResponse restPost(final String url, final String data) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        try {
            // リクエスト
            res = rest.post(url, data, requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return res;
    }

    /**
     * URLを指定してPUTを行う汎用メンバ.
     * @param url リクエスト先のURL
     * @param data リクエストデータ
     * @return レスポンスオブジェクト
     */
    public final PersoniumResponse restPut(final String url, final String data) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        try {
            // リクエスト
            res = rest.put(url, requestheaders, new ByteArrayInputStream(data.getBytes()));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return res;
    }

    /**
     * URLを指定してDELETEを行う汎用メンバ.
     * @param url リクエスト先のURL
     * @return レスポンスオブジェクト
     */
    public final PersoniumResponse restDelete(final String url) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        requestheaders.put(HttpHeaders.IF_MATCH, "*");

        try {
            // リクエスト
            res = rest.del(url, requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return res;
    }

    /**
     * DcRequestオブジェクトを使用してリクエスト実行.
     * @param req リクエストパラメータ
     * @return res
     */
    public static PersoniumResponse request(PersoniumRequest req) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        String method = req.getMethod();
        try {
            // リクエスト
            if (method.equals(HttpMethod.GET)) {
                res = rest.getAcceptEncodingGzip(req.getUrl(), req.getHeaders());
            } else if (method.equals(HttpMethod.PUT)) {
                res = rest.put(req.getUrl(), req.getBody(), req.getHeaders());
            } else if (method.equals(HttpMethod.POST)) {
                res = rest.post(req.getUrl(), req.getBody(), req.getHeaders());
            } else if (method.equals(HttpMethod.DELETE)) {
                res = rest.del(req.getUrl(), req.getHeaders());
            } else {
                res = rest.request(method, req.getUrl(), req.getBody(), req.getHeaders());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return res;
    }

    /**
     * DOMから、指定した要素名のタグを取得する.
     * @param doc Documentオブジェクト
     * @param name 取得する要素名
     * @return 取得したNodeオブジェクト（存在しない場合はnull)
     */
    private Node getElementByTagName(final Document doc, final String name) {
        NodeList nl = doc.getElementsByTagName(name);
        if (nl.getLength() > 0) {
            return nl.item(0);
        } else {
            return null;
        }
    }

    /**
     * 名前空間を指定してDOMから要素を取得する.
     * @param doc Documentオブジェクト
     * @param name 取得する要素名
     * @param ns 名前空間名
     * @return 取得したNodeオブジェクト
     */
    private Node getElementByTagNameNS(final Document doc, final String name, final String ns) {
        NodeList nl = doc.getElementsByTagNameNS(ns, name);
        if (nl.getLength() > 0) {
            return nl.item(0);
        } else {
            return null;
        }
    }

    /**
     * Node内の指定した属性値を取得する.
     * @param node 対象となる要素(Nodeオブジェクト)
     * @param name 取得する属性名
     * @return 取得した属性値
     */
    private String getAttributeValue(final Node node, final String name) {
        NamedNodeMap nnm = node.getAttributes();
        Node attr = nnm.getNamedItem(name);
        if (attr != null) {
            return attr.getNodeValue();
        } else {
            return "";
        }
    }

    /**
     * 名前空間を指定してNode内の指定した属性値を取得する.
     * @param node 対象となる要素(Nodeオブジェクト)
     * @param name 取得する属性名
     * @param ns 名前空間名
     * @return 取得した属性値
     */
    private String getAttributeValueNS(final Node node, final String name, final String ns) {
        NamedNodeMap nnm = node.getAttributes();
        Node attr = nnm.getNamedItemNS(ns, name);
        if (attr != null) {
            return attr.getNodeValue();
        } else {
            return "";
        }
    }

    /**
     * CellのURL文字列を生成.
     * @param curCellId cellのid
     * @return cellを取得するURL
     */
    public String getUrl(final String curCellId) {
        return getUrl(curCellId, null);
    }

    /**
     * CellのURL文字列を生成.
     * @param curCellId cellのid
     * @param reqQuery query
     * @return cellを取得するURL
     */
    public String getUrl(final String curCellId, final String reqQuery) {
        StringBuilder url = new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        url.append("('");
        url.append(curCellId);
        url.append("')");

        if (reqQuery != null) {
            url.append("?");
            url.append(reqQuery);
        }
        return url.toString();
    }

    /**
     * CellのURL文字列を生成.
     * @param curCellId cellのid
     * @param reqQuery query
     * @return cellを取得するURL
     */
    public String getUrlWithOutQuote(final String curCellId, final String reqQuery) {
        StringBuilder url = new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        url.append("(");
        url.append(curCellId);
        url.append(")");

        if (reqQuery != null) {
            url.append("?");
            url.append(reqQuery);
        }
        return url.toString();
    }

    /**
     * 指定した文字列形式の日付が、日付形式なのかチェックする.
     * @param src チェック対象の日付文字列
     * @return true:日付形式、false：日付形式ではない
     */
    private Boolean validISO8601a(final String src) {
        // FastDateFormat fastDateFormat =
        // org.apache.commons.lang.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss'Z'"};
        try {
            org.apache.commons.lang.time.DateUtils.parseDate(src, patterns);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    /**
     * 指定した文字列形式の日付が、日付形式なのかチェックする.
     * @param src チェック対象の日付文字列
     * @return true:日付形式、false：日付形式ではない
     */
    private Boolean validISO8601b(final String src) {
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSS"};
        try {
            org.apache.commons.lang.time.DateUtils.parseDate(src, patterns);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    /**
     * 日付のフォーマットが正しいかチェックする.
     * @param date レスポンスの日付
     * @return boolean フォーマットの判定
     */
    public static Boolean validDate(final String date) {
        String dateformat = "Date\\([0-9]+\\)";
        Pattern pattern = Pattern.compile(dateformat);
        Matcher dateformatmatch = pattern.matcher(date);
        return dateformatmatch.find();
    }

    /** XMLを操作するための名前空間. */
    protected static final String MS_DS = "http://schemas.microsoft.com/ado/2007/08/dataservices";

    /** XMLを操作するための名前空間. */
    protected static final String MS_MD = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata";

    /**
     * CellのXML形式のフォーマットチェック.
     * @param doc Documentオブジェクト
     */
    public final void checkCellXML(final Document doc) {
        this.checkCellResponse(doc);
    }

    /**
     * CellのJson形式のフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     */
    public final void checkCellJson(final JSONObject doc, final String locationHeader) {
        this.checkCellResponse(doc, locationHeader);
    }

    /**
     * DomainのJson形式のフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     */
    public final void checkDomainJson(final JSONObject doc, final String locationHeader) {
        this.checkDomainResponse(doc, locationHeader);
    }

    /**
     * CellのXML形式のフォーマットチェック.
     * @param doc Documentオブジェクト
     */
    public final void checkCellResponse(final Document doc) {
        Node elm;

        // id要素
        elm = getElementByTagName(doc, "id");
        assertNotNull(elm);
        // id値が空かどうかチェック
        assertTrue(elm.getTextContent().length() > 0);

        // title要素
        elm = getElementByTagName(doc, "title");
        assertNotNull(elm);
        // type属性が"text"かチェック
        assertEquals("text", getAttributeValue(elm, "type"));

        // updated要素
        elm = getElementByTagName(doc, "updated");
        assertNotNull(elm);
        // updated要素の値が日付形式かどうかチェック
        assertTrue(validISO8601a(elm.getTextContent()));

        // Name要素の存在チェック
        elm = getElementByTagName(doc, "Name");
        assertNotNull(elm);

        // link要素
        elm = getElementByTagName(doc, "link");
        assertNotNull(elm);
        assertEquals("edit", getAttributeValue(elm, "rel"));
        assertEquals("Cell", getAttributeValue(elm, "title"));
        assertTrue(getAttributeValue(elm, "href").length() > 0);

        // category要素
        elm = getElementByTagName(doc, "category");
        assertNotNull(elm);
        assertEquals(TYPE_CELL, getAttributeValue(elm, "term"));
        assertEquals("http://schemas.microsoft.com/ado/2007/08/dataservices/scheme", getAttributeValue(elm, "scheme"));

        // content要素
        elm = getElementByTagName(doc, "content");
        assertNotNull(elm);
        assertEquals("application/xml", getAttributeValue(elm, "type"));

        // d:__id要素
        elm = getElementByTagNameNS(doc, "__id", MS_DS);
        assertNotNull(elm);
        // id値が空かどうかチェック
        assertTrue(elm.getTextContent().length() > 0);

        // d:Name要素
        elm = getElementByTagNameNS(doc, "Name", MS_DS);
        assertNotNull(elm);
        // name値が空かどうかチェック
        assertTrue(elm.getTextContent().length() > 0);

        // d:__published要素
        elm = getElementByTagNameNS(doc, "__published", MS_DS);
        assertNotNull(elm);
        // __published要素の値が日付形式かどうかチェック
        assertTrue(validISO8601b(elm.getTextContent()));
        assertEquals("Edm.DateTime", getAttributeValueNS(elm, "type", MS_MD));

        // d:__updated要素
        elm = getElementByTagNameNS(doc, "__updated", MS_DS);
        assertNotNull(elm);
        // __updated要素の値が日付形式かどうかチェック
        assertTrue(validISO8601b(elm.getTextContent()));
        assertEquals("Edm.DateTime", getAttributeValueNS(elm, "type", MS_MD));
    }

    /**
     * CellのJson形式のフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     */
    public final void checkCellResponse(final JSONObject doc, final String locationHeader) {

        JSONObject results = (JSONObject) ((JSONObject) doc.get("d")).get("results");
        validateCellResponse(results, locationHeader);
    }

    /**
     * DomainのJson形式のフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     */
    public final void checkDomainResponse(final JSONObject doc, final String locationHeader) {

        JSONObject results = (JSONObject) ((JSONObject) doc.get("d")).get("results");
        validateDomainResponse(results, locationHeader);
    }

    /**
     * CellのJson形式のリストのフォーマットチェック.
     * @param response DcResponseオブジェクト
     * @param contentType レスポンスのContentType
     */
    public final void checkCellListResponse(PersoniumResponse response, MediaType contentType) {

        // Cell作成のレスポンスチェック
        // 200になることを確認
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        // DataServiceVersionのチェック
        Header[] resDsvHeaders = response.getResponseHeaders(ODataConstants.Headers.DATA_SERVICE_VERSION);
        assertEquals(1, resDsvHeaders.length);
        assertEquals("2.0", resDsvHeaders[0].getValue());

        // ContentTypeのチェック
        Header[] resContentTypeHeaders = response.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
        assertEquals(1, resContentTypeHeaders.length);
        String value = resContentTypeHeaders[0].getValue();
        String[] values = value.split(";");
        assertEquals(contentType.toString(), values[0]);

        if (contentType == MediaType.APPLICATION_JSON_TYPE) {
            // レスポンスボディのJsonもチェックが必要
            checkCellListResponse(response.bodyAsJson());

        } else if (contentType == MediaType.APPLICATION_ATOM_XML_TYPE) {
            // TODO レスポンスボディのチェック
            fail("Not Implemented.");
            // checkCellXML(response.bodyAsXml());
        }
    }

    /**
     * DomainのJson形式のリストのフォーマットチェック.
     * @param response DcResponseオブジェクト
     * @param contentType レスポンスのContentType
     */
    public final void checkDomainListResponse(PersoniumResponse response, MediaType contentType) {

        // Cell取得のレスポンスチェック
        // 200になることを確認
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        // DataServiceVersionのチェック
        Header[] resDsvHeaders = response.getResponseHeaders(ODataConstants.Headers.DATA_SERVICE_VERSION);
        assertEquals(1, resDsvHeaders.length);
        assertEquals("2.0", resDsvHeaders[0].getValue());

        // ContentTypeのチェック
        Header[] resContentTypeHeaders = response.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
        assertEquals(1, resContentTypeHeaders.length);
        String value = resContentTypeHeaders[0].getValue();
        String[] values = value.split(";");
        assertEquals(contentType.toString(), values[0]);

        if (contentType == MediaType.APPLICATION_JSON_TYPE) {
            // レスポンスボディのJsonもチェックが必要
            checkDomainListResponse(response.bodyAsJson());

        } else if (contentType == MediaType.APPLICATION_ATOM_XML_TYPE) {
            // レスポンスボディのチェック
            fail("Not Implemented.");
            // checkCellXML(response.bodyAsXml());
        }
    }

    /**
     * CellのJson形式のリストのフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     */
    public final void checkCellListResponse(JSONObject doc) {

        JSONArray arResults = (JSONArray) ((JSONObject) doc.get("d")).get("results");

        for (Object obj : arResults) {
            JSONObject results = (JSONObject) obj;
            validateCellResponse(results, null);
        }

    }

    /**
     * DomainのJson形式のリストのフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     */
    public final void checkDomainListResponse(JSONObject doc) {

        JSONArray arResults = (JSONArray) ((JSONObject) doc.get("d")).get("results");

        for (Object obj : arResults) {
            JSONObject results = (JSONObject) obj;
            validateDomainResponse(results, null);
        }

    }

    /**
     * CellのJson形式のフォーマットチェック.
     * @param results JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     */
    public final void validateCellResponse(final JSONObject results, final String locationHeader) {
        String value;

        // d:Name要素
        value = (String) results.get("Name");
        assertNotNull(value);
        // Name値が空かどうかチェック
        assertTrue(value.length() > 0);

        // d:__published要素
        value = (String) results.get("__published");
        assertNotNull(value);
        // __published要素の値が日付形式かどうかチェック
        assertTrue(validDate(value));

        // d:__updated要素
        value = (String) results.get("__updated");
        assertNotNull(value);
        // __updated要素の値が日付形式かどうかチェック
        assertTrue(validDate(value));

        // __metadata要素
        JSONObject metadata = (JSONObject) results.get("__metadata");
        // uri要素
        value = (String) metadata.get("uri");
        assertNotNull(value);
        if (locationHeader != null) {
            // LOCATIONヘッダと等しいかチェック
            assertEquals(value, locationHeader);
        }

        // type要素
        value = (String) metadata.get("type");
        assertNotNull(value);
        assertEquals(value, TYPE_CELL);

        // etag要素
        value = (String) metadata.get("etag");
        assertNotNull(value);
        // etag値が空かどうかチェック
        assertTrue(value.length() > 0);
    }

    /**
     * DomainのJson形式のフォーマットチェック.
     * @param results JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     */
    public final void validateDomainResponse(final JSONObject results, final String locationHeader) {
        String value;

        // d:Name要素
        value = (String) results.get("Name");
        assertNotNull(value);
        // Name値が空かどうかチェック
        assertTrue(value.length() > 0);

        // d:__published要素
        value = (String) results.get("__published");
        assertNotNull(value);
        // __published要素の値が日付形式かどうかチェック
        assertTrue(validDate(value));

        // d:__updated要素
        value = (String) results.get("__updated");
        assertNotNull(value);
        // __updated要素の値が日付形式かどうかチェック
        assertTrue(validDate(value));

        // __metadata要素
        JSONObject metadata = (JSONObject) results.get("__metadata");
        // uri要素
        value = (String) metadata.get("uri");
        assertNotNull(value);
        if (locationHeader != null) {
            // LOCATIONヘッダと等しいかチェック
            assertEquals(value, locationHeader);
        }

        // type要素
        value = (String) metadata.get("type");
        assertNotNull(value);

        // etag要素
        value = (String) metadata.get("etag");
        assertNotNull(value);
        // etag値が空かどうかチェック
        assertTrue(value.length() > 0);
    }

    /**
     * CellのXML形式のフォーマットチェック.
     * @param doc Documentオブジェクト
     * @param code エラーコード
     */
    public final void checkErrorResponseXML(final Document doc, final String code) {
        this.checkErrorResponse(doc, code);
    }

    /**
     * CellのXML形式のフォーマットチェック.
     * @param doc Documentオブジェクト
     * @param code エラーコード
     */
    public final void checkErrorResponse(final Document doc, final String code) {
        Node elm;

        // code要素
        elm = getElementByTagName(doc, "code");
        assertNotNull(elm);
        // code値が引数と一致するかチェック
        assertEquals(code, elm.getTextContent());

        // message要素
        elm = getElementByTagName(doc, "message");
        assertNotNull(elm);
        // message値が引数と一致するかチェックは行わない。
        // （今後の国際化対応等を見越し、エラーコードのみのチェックとする）
    }

    /**
     * エラーレスポンスチェック.
     * @param doc JSONObjectオブジェクト
     * @param code エラーコード
     */
    public final void checkErrorResponse(JSONObject doc, String code) {
        checkErrorResponse(doc, code, null);
    }

    /**
     * エラーレスポンスチェック.
     * @param doc JSONObjectオブジェクト
     * @param code エラーコード
     * @param message エラーメッセージ
     */
    public final void checkErrorResponse(JSONObject doc, String code, String message) {

        String value;
        JSONObject error = doc;

        // code要素
        value = (String) error.get("code");
        assertNotNull(value);
        // code値が引数と一致するかチェック
        assertEquals(code, value);

        // __metadata要素
        JSONObject metadata = (JSONObject) error.get("message");
        // lang要素
        value = (String) metadata.get("lang");
        assertNotNull(value);

        // value要素
        value = (String) metadata.get("value");
        if (message == null) {
            assertNotNull(value);
        } else {
            assertEquals(message, value);
        }
    }

    /**
     * エラーレスポンス時のXML形式のフォーマットチェック.
     * @param doc Documentオブジェクト
     * @param errorCode エラーコード
     */
    public final void checkErrorXML(final Document doc, final String errorCode) {
        Node elm;

        // error要素
        elm = getElementByTagName(doc, "error");
        assertNotNull(elm);

        // code要素
        elm = getElementByTagName(doc, "code");
        assertNotNull(elm);
        assertEquals(elm.getTextContent(), errorCode);

        // message要素
        elm = getElementByTagName(doc, "message");
        assertNotNull(elm);
        assertTrue(elm.getTextContent().length() > 0);
    }

}
