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
package com.fujitsu.dc.test.utils;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Ace;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Acl;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Grant;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Principal;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Privilege;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * WebDAV Httpリクエストドキュメントを利用するユーティリティ.
 */
public class DavResourceUtils {

    private static final String ACL_AUTH_TEST_SETTING_FILE = "box/acl-authtest.txt";

    private DavResourceUtils() {
    }

    /**
     * コレクション作成及びACL設定(deleteやACLのテスト用).
     * @param path 対象のコレクションのパス
     * @param cellName セル名
     */
    public static void createWebDavCollection(String path, String cellName) {
        // コレクション作成
        createWebDavCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, path);
        // ACL設定
        setACL(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, path, ACL_AUTH_TEST_SETTING_FILE,
                Setup.TEST_BOX1, OAuth2Helper.SchemaLevel.NONE);
    }

    /**
     * MKCOLを実行し、レスポンスコードをチェックする.
     * @param token トークン(認証スキーマ付き)
     * @param code 期待するレスポンスコード
     * @param cell セル名
     * @param box ボックス名
     * @param path 作成するコレクションのパス
     * @return レスポンス
     */
    public static TResponse createWebDavCollectionWithAnyAuthSchema(String token,
            int code,
            String cell,
            String box,
            String path) {
        TResponse res = Http.request("box/mkcol-normal-anyAuthSchema.txt").with("cellPath", "testcell1")
                .with("cellPath", cell).with("box", box).with("path", path).with("token", token).returns();
        res.statusCode(code);
        return res;
    }

    /**
     * MKCOLを実行し、レスポンスコードをチェックする.
     * @param token Bearerトークン
     * @param code 期待するレスポンスコード
     * @param cell セル名
     * @param box ボックス名
     * @param path 作成するコレクションのパス
     * @return レスポンス
     */
    public static TResponse createWebDavCollection(String token,
            int code,
            String cell,
            String box,
            String path) {
        TResponse res = Http.request("box/mkcol-normal-anyAuthSchema.txt").with("cellPath", "testcell1")
                .with("cellPath", cell).with("box", box).with("path", path).with("token", "Bearer " + token).returns();
        res.statusCode(code);
        return res;
    }

    /**
     * MKCOLを実行し、レスポンスコードをチェックする(testcell1, box1固定).
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 作成するコレクションのパス(/testcell1/box1/ 以降を指定する)
     * @return レスポンス
     */
    @Deprecated
    public static TResponse createWebDavCollection(String token, int code, String path) {
        TResponse res = Http.request("box/mkcol-normal.txt").with("cellPath", "testcell1").with("path", path)
                .with("token", token).returns();
        res.statusCode(code);
        return res;
    }

    /**
     * コレクションを作成するユーティリティー.
     * @param fileName ファイル名
     * @param cellName セル
     * @param path パス
     * @param token トークン
     * @param code コード
     * @return レスポンス
     */
    @Deprecated
    public static TResponse createWebDavCollection(String fileName,
            String cellName, String path, String token, int code) {
        TResponse res = Http.request(fileName).with("cellPath", cellName).with("path", path).with("token", token)
                .returns().statusCode(code);
        return res;
    }

    /**
     * MKCOLを実行し、作成に成功していれば削除する.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 対象のコレクションのパス
     * @param cellName セル名
     */
    public static void createWebDavCollectionWithDelete(String token, int code, String path, String cellName) {
        TResponse res = createWebDavCollection(token, code, path);
        if (res.getStatusCode() == HttpStatus.SC_CREATED) {
            // コレクションの削除
            ResourceUtils.delete("box/delete-col.txt", cellName, AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_NO_CONTENT, path);
        }
    }

    /**
     * MKCOLを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 作成するコレクションのパス
     * @return レスポンス
     */
    public static TResponse createServiceCollection(String token, int code, String path) {
        TResponse res = Http.request("box/mkcol-service.txt").with("cellPath", "testcell1").with("path", path)
                .with("token", token).returns();
        res.statusCode(code);
        return res;
    }

    /**
     * MKCOLを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param cell 作成するセル名
     * @param box 作成するセル名
     * @param col 作成するサービスコレクション名
     * @return レスポンス
     */
    public static TResponse createServiceCollection(String token, int code, String cell, String box, String col) {
        TResponse res = Http.request("box/mkcol-service-fullpath.txt")
                .with("cell", cell).with("box", box).with("col", col)
                .with("token", token).returns().debug();
        res.statusCode(code);
        return res;
    }

    /**
     * サービス登録を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param cell セル名
     * @param box ボックス名
     * @param path サービスコレクションのパス
     * @param svcFileName サービスファイル名
     * @param svcName サービス名
     * @return レスポンス
     */
    public static TResponse setServiceProppatch(String token,
            int code,
            String cell,
            String box,
            String path,
            String svcFileName,
            String svcName) {
        return Http.request("box/proppatch-set-service-path.txt")
                .with("cell", cell)
                .with("box", box)
                .with("path", path)
                .with("token", token)
                .with("name", svcName)
                .with("src", svcFileName)
                .returns()
                .statusCode(code);
    }

    /**
     * MKCOLを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param cellPath セルのパス
     * @param boxPath ボックスのパス
     * @param colPath 作成するコレクションのパス
     * @return レスポンス
     */
    public static TResponse createODataCollection(String token, int code,
            String cellPath, String boxPath, String colPath) {
        TResponse res = Http.request("box/mkcol-odata.txt").with("cellPath", cellPath).with("boxPath", boxPath)
                .with("path", colPath).with("token", token).returns();
        res.statusCode(code);
        return res;
    }

    /**
     * コレクションを削除するユーティリティ.
     * @param cellName セル名
     * @param box ボックス名
     * @param path パス
     * @param token トークン
     * @param code コード
     * @return レスポンス
     */
    public static TResponse deleteCollection(String cellName, String box, String path, String token, int code) {
        TResponse res = Http.request("box/delete-box-col.txt").with("cellPath", cellName).with("box", box)
                .with("path", path).with("token", token).returns().statusCode(code);
        return res;
    }

    /**
     * コレクションを削除するユーティリティ.
     * @param cellName セル名
     * @param box ボックス名
     * @param path パス
     * @param token トークン(認証スキーマ付き)
     * @param code コード
     * @return レスポンス
     */
    public static TResponse deleteCollectionWithAnyAuthSchema(String cellName,
            String box,
            String path,
            String token,
            int code) {
        TResponse res = Http.request("box/delete-box-col-anyAuthSchema.txt").with("cellPath", cellName)
                .with("box", box).with("path", path).with("token", token).returns().statusCode(code);
        return res;
    }

    /**
     * ファイル指定でPROPPATCHを設定するユティリティー.
     * @param url URL
     * @param token 認証トークン
     * @param file リクエストファイル
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse setProppatch(String url, String token, String file, int code) {
        // PROPPATCH設定実行
        TResponse tresponse = Http.request(file).with("path", url).with("token", token).returns();
        tresponse.statusCode(code);
        return tresponse;
    }

    /**
     * PROPPATCHを設定するユティリティー.
     * @param url URL
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param values 設定値
     * @return レスポンス
     */
    public static TResponse setProppatch(String url, String token, int code, String... values) {
        // PROPPATCH設定実行
        TResponse tresponse = Http.request("cell/proppatch-set.txt").with("path", url).with("token", token)
                .with("author1", values[0]).with("hoge", values[1]).returns();
        tresponse.statusCode(code);
        return tresponse;
    }

    /**
     * PROPPATCHを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param cell セル名
     * @param box ボックス名
     * @param path コレクションのパス
     * @return レスポンス
     */
    public static TResponse setProppatch(String token, int code, String cell, String box, String path) {
        return Http.request("box/proppatch-set.txt").with("cell", cell).with("box", box).with("path", path)
                .with("token", token).with("author1", "Test User1")
                .with("hoge", "hoge").returns().debug().statusCode(code);
    }

    /**
     * PROPPATCHを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path コレクションのパス
     */
    public static void setProppatch(String token, int code, String path) {
        setProppatch(token, code, "testcell1", "box1", path);
    }

    /**
     * PROPPATCHを設定するユティリティー.
     * @param url URL
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param propKey propを指すキー値
     * @param values 設定値
     * @return レスポンス
     */
    public static TResponse setProppatchSetPropKey(String url, String token,
            int code, String propKey, String... values) {
        // PROPPATCH設定実行
        // TResponse tresponse = Http.request("cell/proppatch-set.txt")
        TResponse tresponse = Http.request("cell/proppatch-set-setPropKey.txt").with("path", url).with("token", token)
                .with("propkey", propKey).with("author1", values[0]).with("hoge", values[1]).returns();
        tresponse.statusCode(code);
        return tresponse;
    }

    /**
     * PROPPATCHを初期化するユティリティー.
     * @param url URL
     * @param token 認証トークン
     * @return レスポンス
     */
    public static TResponse resetProppatch(String url, String token) {
        // プロパティの削除
        TResponse tresponse = Http.request("cell/proppatch-remove.txt").with("path", url).with("token", token)
                .returns();
        tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);
        return tresponse;
    }

    /**
     * PROPPATCHを初期化するユティリティー.
     * @param url URL
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param propKey propを指すキー値
     * @return レスポンス
     */
    public static TResponse resetProppatchSetPropKey(String url, String token, int code, String propKey) {
        // プロパティの削除
        TResponse tresponse = Http.request("cell/proppatch-remove-setPropKey.txt").with("path", url)
                .with("token", token).with("propkey", propKey).returns();
        tresponse.statusCode(code);
        return tresponse;
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 対象のコレクションのパス
     * @param settingFile ACLリクエストファイル
     * @param role ACLに設定するPrincipal（Role）
     * @param privilege ACLに設定する権限
     * @param level スキーマ認証level
     * @return レスポンス
     */
    public static TResponse setACL(String cell,
            String token,
            int code,
            String path,
            String settingFile,
            String role,
            String privilege,
            String level) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(settingFile).with("cellPath", cell).with("colname", path).with("token", token)
                .with("role", role).with("privilege", privilege)
                .with("roleBaseUrl", UrlUtils.roleResource(cell, null, "")).with("level", level).returns()
                .statusCode(code);
        return tresponseWebDav;
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 対象のコレクションのパス(cellから指定)
     * @param requireSchemaAuthz スキーマ認証level
     * @return レスポンス
     */
    public static TResponse setACLPrivilegeAllForAllUser(String cell,
            String token,
            int code,
            String path,
            String requireSchemaAuthz) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request("box/acl-setting-all.txt")
                .with("colname", path)
                .with("token", token)
                .with("privilege", "<D:all/>")
                .with("roleBaseUrl", UrlUtils.roleResource(cell, null, ""))
                .with("level", requireSchemaAuthz).returns()
                .statusCode(code);
        return tresponseWebDav;
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 対象のコレクションのパス(cellから指定)
     * @param privilege 許可する権限
     * @param requireSchemaAuthz スキーマ認証level
     * @return レスポンス
     */
    public static TResponse setACLPrincipalAll(String cell,
            String token,
            int code,
            String path,
            String privilege,
            String requireSchemaAuthz) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request("box/acl-setting-all.txt")
                .with("colname", path)
                .with("token", token)
                .with("privilege", privilege)
                .with("roleBaseUrl", UrlUtils.roleResource(cell, null, ""))
                .with("level", requireSchemaAuthz).returns()
                .statusCode(code);
        return tresponseWebDav;
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 対象のコレクションのパス
     * @param settingFile ACLリクエストファイル
     * @param boxName box名
     * @param level スキーマ認証level
     * @return レスポンス
     */
    public static TResponse setACL(String cell,
            String token,
            int code,
            String path,
            String settingFile,
            String boxName,
            String level) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(settingFile).with("cellPath", cell).with("colname", path)
                .with("roleBaseUrl", UrlUtils.roleResource(cell, null, "")).with("box", boxName).with("token", token)
                .with("level", level).returns().statusCode(code);
        return tresponseWebDav;
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param box 対象のボックスパス
     * @param colname 対象のコレクションのパス
     * @param settingFile ACLリクエストファイル
     * @param roleBaseUrl ACLに設定するPrincipal（Role）
     * @param privilege ACLに設定する権限
     * @param level スキーマ認証level
     * @return レスポンス
     */
    @Deprecated
    public static TResponse setACLwithRoleBaseUrl(String cell,
            String token,
            int code,
            String box,
            String colname,
            String settingFile,
            String roleBaseUrl,
            String privilege,
            String level) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(settingFile).with("cellPath", cell).with("box", box).with("colname", colname)
                .with("token", token).with("privilege", privilege).with("roleBaseUrl", roleBaseUrl)
                .with("level", level).returns().statusCode(code);
        return tresponseWebDav;
    }

    /**
     * ACL設定.
     * @param token トークン
     * @param cell セル名
     * @param code 期待するレスポンスコード
     * @param box 対象のボックスパス
     * @param colname 対象のコレクションのパス
     * @param requireSchemaAuthz スキーマ認証level
     * @param roleBaseUrl ACLに設定するPrincipal（Role）
     * @param privilege ACLに設定する権限
     * @return レスポンス
     */
    public static TResponse setACLwithRoleBaseUrl(
            String token,
            String cell,
            String box,
            String colname,
            String requireSchemaAuthz,
            String roleBaseUrl,
            String privilege,
            int code) {
        return Http.request("box/acl-setting-baseurl.txt")
                .with("cellPath", cell)
                .with("box", box)
                .with("colname", colname)
                .with("token", token)
                .with("level", requireSchemaAuthz)
                .with("privilege", privilege)
                .with("roleBaseUrl", roleBaseUrl)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param box 対象のボックスパス
     * @param colname 対象のコレクションのパス
     * @param role ロール名
     * @param settingFile ACLリクエストファイル
     * @param roleBaseUrl ACLに設定するPrincipal（Role）
     * @param privilege ACLに設定する権限
     * @param level スキーマ認証level
     * @return レスポンス
     */
    public static TResponse setACLwithRoleBaseUrl(String cell,
            String token,
            int code,
            String box,
            String colname,
            String role,
            String settingFile,
            String roleBaseUrl,
            String privilege,
            String level) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(settingFile).with("cellPath", cell).with("box", box).with("colname", colname)
                .with("role", role).with("token", token).with("privilege", privilege).with("roleBaseUrl", roleBaseUrl)
                .with("level", level).returns().statusCode(code);
        return tresponseWebDav;
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param box 対象のボックスパス
     * @param path 対象のコレクションのパス
     * @param settingFile ACLリクエストファイル
     * @param role1 ACLに設定するPrincipal（Role）
     * @param role2 ACLに設定するPrincipal（Role）
     * @param roleLinkToBox ロールに紐付くボックス名
     * @param privilege1 ACLに設定する権限
     * @param privilege2 ACLに設定する権限
     * @param level スキーマ認証level
     * @return レスポンス
     */
    public static TResponse setACLwithBox(String cell,
            String token,
            int code,
            String box,
            String path,
            String settingFile,
            String role1,
            String role2,
            String roleLinkToBox,
            String privilege1,
            String privilege2,
            String level) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(settingFile).with("cellPath", cell).with("box", box).with("colname", path)
                .with("token", token).with("role1", role1).with("role2", role2).with("privilege1", privilege1)
                .with("privilege2", privilege2).with("roleBaseUrl", UrlUtils.roleResource(cell, roleLinkToBox, ""))
                .with("level", level).returns().statusCode(code);
        return tresponseWebDav;
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param box 対象のボックスパス
     * @param path 対象のコレクションのパス
     * @param settingFile ACLリクエストファイル
     * @param role ACLに設定するPrincipal（Role）
     * @param roleLinkToBox ロールに紐付くボックス名
     * @param privilege ACLに設定する権限
     * @param level スキーマ認証level
     * @return レスポンス
     */
    public static TResponse setACLwithBox(String cell,
            String token,
            int code,
            String box,
            String path,
            String settingFile,
            String role,
            String roleLinkToBox,
            String privilege,
            String level) {

        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(settingFile).with("cellPath", cell).with("box", box).with("colname", path)
                .with("token", token).with("role", role).with("privilege", privilege)
                .with("roleBaseUrl", UrlUtils.roleResource(cell, roleLinkToBox, "")).with("level", level).returns()
                .statusCode(code);
        return tresponseWebDav;
    }

    /**
     * BoxレベルACLを設定する.
     * <p>
     * 使用例：
     * <pre>
     * Acl acl = new Acl();
     * acl.getAce().add(DavResourceUtils.createAce(false, roleRead, &quot;read&quot;));
     * acl.getAce().add(DavResourceUtils.createAce(false, roleWrite, &quot;write&quot;));
     * acl.setXmlbase(String.format(&quot;%s/%s/__role/%s/&quot;,
     *         UrlUtils.getBaseUrl(), CELL_NAME, Box.DEFAULT_BOX_NAME));
     * DavResourceUtils.setAcl(MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, acl, HttpStatus.SC_OK);
     * </pre>
     * </p>
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param acl 設定するACLオブジェクト
     * @param code 期待するステータスコード
     * @return レスポンス
     * @throws JAXBException ACLの解析に失敗したとき
     */
    public static TResponse setAcl(String token, String cell, String box, String col, Acl acl, int code)
            throws JAXBException {
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(Acl.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(acl, writer);
        return Http.request("box/acl-setting-none-body.txt")
                .with("cell", cell)
                .with("box", box)
                .with("colname", col)
                .with("token", token)
                .with("body", writer.toString())
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * BoxレベルACLに設定するACEを設定する.
     * @param isPrincipalAll プリンシパルとして all を指定するかどうかを示すフラグ
     * @param role プリンシパルとして all を指定しない場合に参照するロールのURL（相対パス or 絶対URI)
     * @param privilege 設定する権限
     * @return 生成したAceオブジェクト
     */
    public static Ace createAce(boolean isPrincipalAll, String role, String privilege) {

        Principal principal = new Principal();
        if (isPrincipalAll) {
            principal.setAll("");
        } else {
            principal.setHref(role);
        }
        Grant grant = new Grant();
        grant.getPrivilege().add(createPrivilege(privilege));

        Ace ace = new Ace();
        ace.setPrincipal(principal);
        ace.setGrant(grant);
        return ace;
    }

    /**
     * BoxレベルACLに設定するACEを設定する.
     * @param isPrincipalAll プリンシパルとして all を指定するかどうかを示すフラグ
     * @param role プリンシパルとして all を指定しない場合に参照するロールのURL（相対パス or 絶対URI)
     * @param privileges 設定する権限
     * @return 生成したAceオブジェクト
     */
    public static Ace createAce(boolean isPrincipalAll, String role, List<String> privileges) {

        Principal principal = new Principal();
        principal.setHref(role);
        Grant grant = new Grant();
        for (String privilege : privileges) {
            grant.getPrivilege().add(createPrivilege(privilege));
        }

        Ace ace = new Ace();
        ace.setPrincipal(principal);
        ace.setGrant(grant);
        return ace;
    }

    /**
     * BoxレベルACLの権限を作成する.
     * @param type 設定する権限
     * @return 生成したPrivilegeオブジェクト
     */
    public static Privilege createPrivilege(String type) {
        Privilege privilege = new Privilege();
        if ("read".equals(type)) {
            privilege.setRead("");
        } else if ("write".equals(type)) {
            privilege.setWrite("");
        } else if ("alter-schema".equals(type)) {
            privilege.setAlterSchema("");
        } else if ("read-properties".equals(type)) {
            privilege.setReadProperties("");
        } else if ("write-properties".equals(type)) {
            privilege.setWriteProperties("");
        } else if ("read-acl".equals(type)) {
            privilege.setReadAcl("");
        } else if ("write-acl".equals(type)) {
            privilege.setWriteAcl("");
        } else if ("bind".equals(type)) {
            privilege.setBind("");
        } else if ("unbind".equals(type)) {
            privilege.setUnbind("");
        } else if ("exec".equals(type)) {
            privilege.setExec("");
        } else if ("all".equals(type)) {
            privilege.setAll("");
        }
        return privilege;
    }

    /**
     * PROPFINDを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param cell Cell名
     * @param path Box以降のパス
     * @param depth Depthヘッダの値
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse propfind(
            String token,
            String cell,
            String path,
            String depth,
            int code) {
        TResponse res = Http.request("box/propfind-box-allprop.txt")
                .with("cellPath", cell)
                .with("path", path)
                .with("depth", depth)
                .with("token", token)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * PROPFINDを実行し、レスポンスコードをチェックする.
     * @param fileName ファイル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path パス名
     * @return レスポンス
     */
    @Deprecated
    public static TResponse propfind(String fileName, String token, int code, String path) {
        TResponse res = Http.request(fileName).with("cellPath", Setup.TEST_CELL1).with("path", path).with("depth", "0")
                .with("token", token).returns().statusCode(code);
        return res;
    }

    /**
     * PROPFINDを実行し、レスポンスコードをチェックする.
     * @param reqFile リクエスト用ファイル名
     * @param token トークン
     * @param cellName セル名
     * @param path パス名
     * @param depth 取得階層 ( 0 or 1 )
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    @Deprecated
    public static TResponse propfind(String reqFile, String token, String cellName, String path, int depth, int code) {
        TResponse res = Http.request(reqFile)
                .with("token", token)
                .with("cellPath", cellName)
                .with("path", path)
                .with("depth", String.valueOf(depth))
                .returns().statusCode(code);
        return res;
    }

    /**
     * WebDAV取得リクエストを生成.
     * @param cell セル名
     * @param token トークン
     * @param boxName box名
     * @param path リクエストパス（Box以下）
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse getWebDav(String cell,
            String token,
            String boxName,
            String path,
            int code) {
        return Http.request("box/dav-get.txt")
                .with("cellPath", cell)
                .with("token", token)
                .with("box", boxName)
                .with("path", path)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * File取得リクエストを生成.
     * @param cell セル名
     * @param token トークン
     * @param fileName ファイル名
     * @param boxName box名
     * @param path リクエストパス（Box以下）
     * @param code レスポンスコード
     * @return レスポンス
     */
    @Deprecated
    public static TResponse getWebDavFile(String cell,
            String token,
            String fileName,
            String boxName,
            String path,
            int code) {
        TResponse res = Http.request(fileName).with("cellPath", cell).with("token", token).with("box", boxName)
                .with("path", path).returns().statusCode(code);
        return res;
    }

    /**
     * File作成リクエストを生成.
     * @param cell セル名
     * @param token トークン
     * @param fileName リソースファイル名
     * @param fileBody リクエストボディ
     * @param boxName box名
     * @param path リクエストパス（Box以下）
     * @param code ステータスコード
     * @return リクエストオブジェクト
     */
    @Deprecated
    public static TResponse createWebDavFile(String cell,
            String token,
            String fileName,
            String fileBody,
            String boxName,
            String path,
            int code) {
        TResponse res = Http.request(fileName).with("cellPath", cell).with("token", token).with("box", boxName)
                .with("path", path).with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN).with("source", fileBody)
                .returns().statusCode(code);
        return res;
    }

    /**
     * File作成リクエストを生成.
     * @param token トークン
     * @param cell セル名
     * @param path リクエストパス（Boxから指定）
     * @param fileBody リクエストボディ
     * @param contentType リクエストボディのMimeタイプ
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse createWebDavFile(String token,
            String cell,
            String path,
            String fileBody,
            String contentType,
            int code) {
        return Http.request("box/dav-put-with-body.txt")
                .with("cellPath", cell)
                .with("token", token)
                .with("path", path)
                .with("contentType", contentType)
                .with("source", fileBody)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * File作成リクエストを生成(認証ヘッダ無し).
     * @param cell セル名
     * @param fileName リソースファイル名
     * @param fileBody リクエストボディ
     * @param boxName box名
     * @param path リクエストパス（Box以下）
     * @param code ステータスコード
     * @return リクエストオブジェクト
     */
    public static TResponse createWebDavFileNoAuthHeader(String cell,
            String fileName,
            String fileBody,
            String boxName,
            String path,
            int code) {
        TResponse res = Http.request(fileName).with("cellPath", cell).with("box", boxName).with("path", path)
                .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN).with("source", fileBody).returns()
                .statusCode(code);
        return res;
    }

    /**
     * WebDAV移動リクエストを生成.
     * @param token トークン
     * @param cell セル名
     * @param path リクエストパス（Boxから指定）
     * @param destination 移動先のパス（URL形式）
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse moveWebDav(String token,
            String cell,
            String path,
            String destination,
            int code) {
        return Http.request("box/dav-move.txt")
                .with("cellPath", cell)
                .with("authorization", "Bearer " + token)
                .with("path", path)
                .with("destination", destination)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * WebDAV移動リクエストを生成.
     * @param token トークン
     * @param cell セル名
     * @param path リクエストパス（Boxから指定）
     * @param destination 移動先のパス（URL形式）
     * @param ifMatch IfMatch
     * @param overWrite 上書き指定
     * @param depth depth
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse moveWebDav(String token,
            String cell,
            String path,
            String destination,
            String ifMatch,
            String overWrite,
            String depth,
            int code) {
        return Http.request("box/dav-move-with-header.txt")
                .with("cellPath", cell)
                .with("authorization", "Bearer " + token)
                .with("path", path)
                .with("destination", destination)
                .with("ifMatch", ifMatch)
                .with("overWrite", overWrite)
                .with("depth", depth)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * WebDAV移動リクエストを生成.
     * @param authorization Authorizationヘッダの値
     * @param cell セル名
     * @param path リクエストパス（Boxから指定）
     * @param destination 移動先のパス（URL形式）
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse moveWebDavWithAnyAuthSchema(String authorization,
            String cell,
            String path,
            String destination,
            int code) {
        return Http.request("box/dav-move.txt")
                .with("cellPath", cell)
                .with("authorization", authorization)
                .with("path", path)
                .with("destination", destination)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * File削除リクエストを生成.
     * @param fileName リソースファイル名
     * @param cell セル名
     * @param token トークン
     * @param path パス
     * @param code レスポンスコード
     * @param boxName box名
     * @return レスポンス
     */
    @Deprecated
    public static TResponse deleteWebDavFile(String fileName,
            String cell,
            String token,
            String path,
            int code,
            String boxName) {
        TResponse res = Http.request(fileName).with("cellPath", cell).with("token", token).with("box", boxName)
                .with("path", path).returns().statusCode(code);
        return res;
    }

    /**
     * File削除リクエストを生成.
     * @param cell Cell名
     * @param token トークン
     * @param boxName Box名
     * @param path ファイルパス
     * @return レスポンス
     */
    public static TResponse deleteWebDavFile(String cell, String token, String boxName, String path) {
        return Http.request("box/dav-delete-ifmatch.txt").with("cellPath", cell).with("etag", "*").with("token", token)
                .with("box", boxName).with("path", path).returns();
    }

    /**
     * XMLから指定したタグ名の要素を返す.
     * @param doc ドキュメント
     * @param tagName タグ名
     * @return result 要素の値
     */
    public static String getXmlNodeValue(final Document doc, final String tagName) {
        String result = null;
        Element root = doc.getDocumentElement();
        // rootタグ名の確認
        assertEquals("multistatus", root.getTagName());
        NodeList nl1 = root.getElementsByTagName(tagName);
        if (nl1 == null) {
            return null;
        }
        for (int i = 0; i < nl1.getLength(); i++) {
            result = (nl1.item(i)).getFirstChild().getNodeValue();
        }
        return result;
    }

    /**
     * PROPFINDの結果にODataコレクションが含まれるかどうかをチェックする. <br />
     * dc:odataタグが含まれていれば OData コレクションが含まれものとする.
     * @param res PROPFINDレスポンス
     */
    public static void assertIsODataCol(TResponse res) {
        Document propfind = res.bodyAsXml();
        NodeList list;
        list = propfind.getElementsByTagName("dc:odata");
        assertThat(list.getLength()).isGreaterThanOrEqualTo(1);
    }

    /**
     * PROPFINDの結果にServiceコレクションが含まれるかどうかをチェックする. <br />
     * dc:pathタグが含まれていれば Service コレクションが含まれものとする.
     * @param res PROPFINDレスポンス
     */
    public static void assertIsServiceCol(TResponse res) {
        Document propfind = res.bodyAsXml();
        NodeList list;
        list = propfind.getElementsByTagName("dc:path");
        assertThat(list.getLength()).isGreaterThanOrEqualTo(1);
    }

    /**
     * PROPFINDの結果にhref タグで指定されたURLが含まれていることをチェックする.
     * @param expectedUrl 期待するURL
     * @param res PROPFINDレスポンス
     */
    public static void assertContainsHrefUrl(
            final String expectedUrl,
            TResponse res) {
        boolean isMatch = containsUrl(expectedUrl, res);
        assertEquals(true, isMatch);
    }

    /**
     * PROPFINDの結果にhref タグで指定されたURLが含まれていないことをチェックする.
     * @param expectedUrl 期待するURL
     * @param res PROPFINDレスポンス
     */
    public static void assertNotContainsHrefUrl(
            final String expectedUrl,
            TResponse res) {
        boolean isMatch = containsUrl(expectedUrl, res);
        assertEquals(false, isMatch);
    }

    private static boolean containsUrl(final String expectedUrl, TResponse res) {
        Document propfind = res.bodyAsXml();
        NodeList list;
        list = propfind.getElementsByTagName("href");
        int index = 0;
        boolean isMatch = false;
        for (index = 0; index < list.getLength(); index++) {
            org.w3c.dom.Node node = list.item(index);
            NodeList children = node.getChildNodes();
            assertEquals(1, children.getLength());
            Text item = (Text) children.item(0);
            if (expectedUrl.equals(item.getNodeValue())) {
                isMatch = true;
            }
        }
        return isMatch;
    }

    /**
     * XMLのレスポンスに期待するノード情報と同じ情報が含まれるかどうかをチェックする.
     * @param res PROPFINDレスポンス
     * @param tagName チェック対象となるタグ名
     * @param expectedNode 期待するノード情報
     */
    public static void assertEqualsNodeInResXml(TResponse res, String tagName, Node expectedNode) {
        Document propfind = res.bodyAsXml();
        NodeList list;
        list = propfind.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node item = list.item(i);
            if (item.isEqualNode(expectedNode)) {
                return;
            }
        }
        // 指定されたタグが含まれない場合はテスト失敗とする
        fail();
    }

    /**
     * XMLのレスポンスに指定したタグが含まれるかどうかをチェックする.
     * @param res PROPFINDレスポンス
     * @param tagName チェック対象となるタグ名
     */
    public static void assertContainsNodeInResXml(TResponse res, String tagName) {
        Document propfind = res.bodyAsXml();
        NodeList list;
        list = propfind.getElementsByTagName(tagName);
        assertThat(list.getLength()).isGreaterThan(0);
    }

    /**
     * XMLのレスポンスに指定したタグが含まれないかどうかをチェックする.
     * @param res PROPFINDレスポンス
     * @param tagName チェック対象となるタグ名
     */
    public static void assertNotContainsNodeInResXml(TResponse res, String tagName) {
        Document propfind = res.bodyAsXml();
        NodeList list;
        list = propfind.getElementsByTagName(tagName);
        assertThat(list.getLength()).isEqualTo(0);
    }

}
