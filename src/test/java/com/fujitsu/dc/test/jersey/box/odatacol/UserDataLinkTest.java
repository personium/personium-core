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
package com.fujitsu.dc.test.jersey.box.odatacol;

import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * $expandクエリ指定のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataLinkTest extends AbstractUserDataTest {

    private String toEntityTypeName = "toEntity";
    private String fromEntityTypeName = "fromEntity";
    private String toUserDataId = "toEntitySet";
    private String fromUserDataId = "fromEntitySet";
    private String toUserDataId2 = "toEntitySet2";
    private String fromUserDataId2 = "fromEntitySet2";

    /**
     * コンストラクタ.
     */
    public UserDataLinkTest() {
        super();
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndが0対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndが0対0() {
        String srcEntityType = "Sales";
        String targetEntityType = "Price";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                   Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndが0対1.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndが0対1() {
        String srcEntityType = "Supplier";
        String targetEntityType = "Product";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndが0対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndが0対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "Supplier";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(作成できることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId + 1));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId + 1, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndが1対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndが1対0() {
        String srcEntityType = "Product";
        String targetEntityType = "Supplier";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                   Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndが1対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "SalesDetail";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(作成できることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                   Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                   Setup.TEST_ODATA, targetEntityType, targetUserDataId + 1));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId + 1, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndがアスタ対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndがアスタ対0() {
        String srcEntityType = "Supplier";
        String targetEntityType = "Sales";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndがアスタ対1.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndがアスタ対1() {
        String srcEntityType = "SalesDetail";
        String targetEntityType = "Sales";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを作成して一覧取得できること_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを作成して一覧取得できること_AssociationEndがアスタ対アスタ() {
        String srcEntityType = "Product";
        String targetEntityType = "Sales";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);
            body.put("__id", targetUserDataId + 1);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $link登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $link登録(409になることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_CONFLICT);

            // 別のIDで$link登録(作成できることの確認)
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId + 1,
                    HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId + 1));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId + 1, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId + 1, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを11件作成して一覧取得できること_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを11件作成して一覧取得できること_AssociationEndがアスタ対アスタ() {
        String srcEntityType = "Product";
        String targetEntityType = "Sales";
        String srcUserDataId = "srcId";

        JSONObject body = new JSONObject();
        ArrayList<String> expectedUriList = new ArrayList<String>();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);

            for (int i = 0; i < 11; i++) {
                String targetUserDataId = "targetId" + i;
                body.put("__id", targetUserDataId);
                createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        targetEntityType);
                // $link登録
                ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                        HttpStatus.SC_NO_CONTENT);
                // 一覧取得のExpectedリストを作成
                expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                       Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            }

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            for (int i = 0; i < 11; i++) {
                String targetUserDataId = "targetId" + i;
                ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                        Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);
                deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            }
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを11件作成して一覧取得できること_AssociationEndが１対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを11件作成して一覧取得できること_AssociationEndが１対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "SalesDetail";
        String srcUserDataId = "srcId";

        JSONObject body = new JSONObject();
        ArrayList<String> expectedUriList = new ArrayList<String>();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);

            for (int i = 0; i < 11; i++) {
                String targetUserDataId = "targetId" + i;
                body.put("__id", targetUserDataId);
                createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        targetEntityType);
                // $link登録
                ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                        HttpStatus.SC_NO_CONTENT);
                // 一覧取得のExpectedリストを作成
                expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                       Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            }

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            for (int i = 0; i < 11; i++) {
                String targetUserDataId = "targetId" + i;
                ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                        Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);
                deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            }
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを11件作成して一覧取得できること_AssociationEndが0対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを11件作成して一覧取得できること_AssociationEndが0対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "Supplier";
        String srcUserDataId = "srcId";

        JSONObject body = new JSONObject();
        ArrayList<String> expectedUriList = new ArrayList<String>();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);

            for (int i = 0; i < 11; i++) {
                String targetUserDataId = "targetId" + i;
                body.put("__id", targetUserDataId);
                createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        targetEntityType);
                // $link登録
                ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                        HttpStatus.SC_NO_CONTENT);
                // 一覧取得のExpectedリストを作成
                expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                        Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            }

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            for (int i = 0; i < 11; i++) {
                String targetUserDataId = "targetId" + i;
                ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                        Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);
                deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            }
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndが0対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndが0対0() {
        String srcEntityType = "Sales";
        String targetEntityType = "Price";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndが0対1.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndが0対1() {
        String srcEntityType = "Supplier";
        String targetEntityType = "Product";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndが0対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndが0対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "Supplier";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndが1対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndが1対0() {
        String srcEntityType = "Product";
        String targetEntityType = "Supplier";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndが1対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "SalesDetail";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndがアスタ対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndがアスタ対0() {
        String srcEntityType = "Supplier";
        String targetEntityType = "Sales";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndがアスタ対1.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndがアスタ対1() {
        String srcEntityType = "SalesDetail";
        String targetEntityType = "Sales";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlinkを削除できること_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlinkを削除できること_AssociationEndがアスタ対アスタ() {
        String srcEntityType = "Product";
        String targetEntityType = "Sales";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links削除
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できること_AssociationEndが0対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できること_AssociationEndが0対0() {
        String srcEntityType = "Sales";
        String targetEntityType = "Price";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できること_AssociationEndが0対1.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できること_AssociationEndが0対1() {
        String srcEntityType = "Supplier";
        String targetEntityType = "Product";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できること_AssociationEndが0対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できること_AssociationEndが0対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "Supplier";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できること_AssociationEndが1対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できること_AssociationEndが1対0() {
        String srcEntityType = "Product";
        String targetEntityType = "Supplier";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できること_AssociationEndが1対アスタ() {
        String srcEntityType = "Sales";
        String targetEntityType = "SalesDetail";
        String srcUserDataId = "srcId";
        String targetUserDataId = "targetId";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できないこと_AssociationEndがアスタ対0.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できないこと_AssociationEndがアスタ対0() {
        String srcEntityType = "Supplier";
        String targetEntityType = "Sales";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できないこと_AssociationEndがアスタ対1.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できないこと_AssociationEndがアスタ対1() {
        String srcEntityType = "SalesDetail";
        String targetEntityType = "Sales";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * link済みのユーザデータを削除できないこと_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void link済みのユーザデータを削除できないこと_AssociationEndがアスタ対アスタ() {
        String srcEntityType = "Product";
        String targetEntityType = "Sales";
        String srcUserDataId = "src-Id";
        String targetUserDataId = "target-Id";

        JSONObject body = new JSONObject();
        try {
            body.put("__id", srcUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType);
            body.put("__id", targetUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType);

            // $links登録
            ResourceUtils.linksUserData(srcEntityType, srcUserDataId, targetEntityType, targetUserDataId,
                    HttpStatus.SC_NO_CONTENT);

            // $links先のユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_CONFLICT);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", srcEntityType + "('" + srcUserDataId + "')")
                    .with("trgPath", targetEntityType)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, targetEntityType, targetUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks(srcUserDataId, targetUserDataId, targetEntityType, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, srcEntityType, -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    srcEntityType, srcUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    targetEntityType, targetUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で正しく取得できること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で正しく取得できること_AssociationEndが1対アスタ() {
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail");
            // $link
            linkUserData("Sales", toUserDataId, "SalesDetail", fromUserDataId);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Sales" + "('" + toUserDataId + "')")
                    .with("trgPath", "SalesDetail")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                     Setup.TEST_ODATA, "SalesDetail", fromUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            // $link
            deleteUserDataLinks("Sales", toUserDataId, "SalesDetail", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail", fromUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", toUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で正しく取得できること_AssociationEndがアスタ対アスタ.
     */
    @Test
    public final void ユーザデータのlink一覧取得で正しく取得できること_AssociationEndがアスタ対アスタ() {

        try {
            // データ作成
            createData();

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", toEntityTypeName + "('" + toUserDataId + "')")
                    .with("trgPath", navPropName)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                     Setup.TEST_ODATA, fromEntityTypeName, fromUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * ユーザデータのlink一覧取得で正しく取得できること_AssociationEndがアスタ対1.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で正しく取得できること_AssociationEndがアスタ対1() {
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail");
            // $link
            linkUserData("Sales", toUserDataId, "SalesDetail", fromUserDataId);

            // $links一覧取得
            TResponse resList = Http.request("box/odatacol/list-link.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "SalesDetail" + "('" + fromUserDataId + "')")
                    .with("trgPath", "Sales")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                     Setup.TEST_ODATA, "Sales", toUserDataId));
            // レスポンスボディのチェック
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            // $link
            deleteUserDataLinks("Sales", toUserDataId, "SalesDetail", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail", fromUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", toUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * データ作成処理.
     */
    @SuppressWarnings("unchecked")
    public final void createData() {
        navPropName = fromEntityTypeName;
        JSONObject body = new JSONObject();
        JSONObject linkBody = new JSONObject();

        // エンティティタイプを作成
        EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                Setup.TEST_ODATA, toEntityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                Setup.TEST_ODATA, navPropName, HttpStatus.SC_CREATED);

        // AssociationEndを作成
        AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED, "AssociationEnd", toEntityTypeName);
        AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED, "LinkAssociationEnd", navPropName);

        // AssociationEndを関連付け
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, toEntityTypeName, navPropName, "AssociationEnd", "LinkAssociationEnd",
                HttpStatus.SC_NO_CONTENT);

        // ユーザデータを作成
        body.put("__id", toUserDataId);
        linkBody.put("__id", fromUserDataId);
        createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName);
        createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName);

        body.put("__id", toUserDataId2);
        linkBody.put("__id", fromUserDataId2);
        createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName);
        createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName);

        // ユーザデータ-ユーザデータの$links作成
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + navPropName + "('" + fromUserDataId + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntityTypeName + "('" + toUserDataId + "')")
                .with("trgPath", navPropName)
                .with("token", DcCoreConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + navPropName + "('" + fromUserDataId2 + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntityTypeName + "('" + toUserDataId2 + "')")
                .with("trgPath", navPropName)
                .with("token", DcCoreConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    private void linkUserData(String toEntity, String toUserId,
             String fromEntity, String fromUserId) {

        // ユーザデータ-ユーザデータの$links作成
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + fromEntity + "('" + fromUserId + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntity + "('" + toUserDataId + "')")
                .with("trgPath", fromEntity)
                .with("token", DcCoreConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * データ削除処理.
     */
    private void deleteData() {
        entityTypeName = toEntityTypeName;

        // ユーザデータ-ユーザデータの$links削除
        deleteUserDataLinks(toUserDataId, fromUserDataId);
        deleteUserDataLinks(toUserDataId2, fromUserDataId2);

        // ユーザデータを削除
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName, toUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName, fromUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName, toUserDataId2, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName, fromUserDataId2, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

        // AssociationEndLinkを削除
        AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                Setup.TEST_BOX1, "Name='AssociationEnd',_EntityType.Name='" + toEntityTypeName + "'",
                "Name='LinkAssociationEnd',_EntityType.Name='" + navPropName + "'", HttpStatus.SC_NO_CONTENT);

        // AssociationEndを削除
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                Setup.TEST_ODATA, toEntityTypeName, Setup.TEST_BOX1, "AssociationEnd", HttpStatus.SC_NO_CONTENT);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                Setup.TEST_ODATA, navPropName, Setup.TEST_BOX1, "LinkAssociationEnd", HttpStatus.SC_NO_CONTENT);

        // エンティティタイプを削除
        EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                "application/json", toEntityTypeName, Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                "application/json", navPropName, Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);
    }

    private void deleteUserDataLinks(String srcEntityTypeName, String userDataId, String trgEntityTypeName,
            String navPropId) {
        // リクエスト実行
        Http.request("box/odatacol/delete-link.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", srcEntityTypeName)
                .with("id", userDataId)
                .with("navProp", "_" + trgEntityTypeName)
                .with("navKey", navPropId)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns();
    }
}
