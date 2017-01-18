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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmMultiplicity;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * $expandクエリ指定のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataLinkDeleteTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataLinkDeleteTest() {
        super();
    }

    /**
     * ユーザデータが0_1対0_1のlinkを削除した後に取得したユーザデータの内容が正しいこと.
     */
    @Test
    public final void ユーザデータが0_1対0_1のlinkを削除した後に取得したユーザデータの内容が正しいこと() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "0..1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "0..1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // CopmlexType/Property/ComplexTypePropertyの作成
            createPropertiesForUserDataLinkDeleteTest(cell, box, col);
            // ユーザデータ（srouce）作成
            JSONObject srcBody = createUserDataBody("srcData1", "src");
            JSONObject tgtBody = createUserDataBody("tgtData1", "tgt");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody, cell, box, col, "srcEntity");
            UserDataUtils.createViaNP(token,
                    tgtBody, cell, box, col, "srcEntity", "srcData1", "tgtEntity", HttpStatus.SC_CREATED);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // 削除後の確認
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData1");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "srcEntity", "srcData1", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData1", col);
            // CopmlexType/Property/ComplexTypePropertyの削除
            deletePropertiesForUserDataLinkDeleteTest(cell, box, col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * ユーザデータが0_1対1のlinkを削除した後に取得したユーザデータの内容が正しいこと.
     */
    @Test
    public final void ユーザデータが0_1対1のlinkを削除した後に取得したユーザデータの内容が正しいこと() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "0..1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // CopmlexType/Property/ComplexTypePropertyの作成
            createPropertiesForUserDataLinkDeleteTest(cell, box, col);
            // ユーザデータ（srouce）作成
            JSONObject srcBody = createUserDataBody("srcData1", "src");
            JSONObject tgtBody = createUserDataBody("tgtData1", "tgt");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody, cell, box, col, "srcEntity");
            UserDataUtils.createViaNP(token,
                    tgtBody, cell, box, col, "srcEntity", "srcData1", "tgtEntity", HttpStatus.SC_CREATED);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // 削除後の確認
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData1");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "srcEntity", "srcData1", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData1", col);
            // CopmlexType/Property/ComplexTypePropertyの削除
            deletePropertiesForUserDataLinkDeleteTest(cell, box, col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * ユーザデータが1対0_1のlinkを削除した後に取得したユーザデータの内容が正しいこと.
     */
    @Test
    public final void ユーザデータが1対0_1のlinkを削除した後に取得したユーザデータの内容が正しいこと() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "0..1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // CopmlexType/Property/ComplexTypePropertyの作成
            createPropertiesForUserDataLinkDeleteTest(cell, box, col);
            // ユーザデータ（srouce）作成
            JSONObject srcBody = createUserDataBody("srcData1", "src");
            JSONObject tgtBody = createUserDataBody("tgtData1", "tgt");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody, cell, box, col, "srcEntity");
            UserDataUtils.createViaNP(token,
                    tgtBody, cell, box, col, "srcEntity", "srcData1", "tgtEntity", HttpStatus.SC_CREATED);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // 削除後の確認
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData1");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "srcEntity", "srcData1", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData1", col);
            // CopmlexType/Property/ComplexTypePropertyの削除
            deletePropertiesForUserDataLinkDeleteTest(cell, box, col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * ユーザデータが0_1対Nのlinkを削除した後に取得したユーザデータの内容が正しいこと.
     */
    @Test
    public final void ユーザデータが0_1対Nのlinkを削除した後に取得したユーザデータの内容が正しいこと() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "0..1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "*", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // CopmlexType/Property/ComplexTypePropertyの作成
            createPropertiesForUserDataLinkDeleteTest(cell, box, col);
            // ユーザデータ（srouce）作成
            JSONObject srcBody = createUserDataBody("srcData1", "src");
            JSONObject tgtBody1 = createUserDataBody("tgtData1", "tgt");
            JSONObject tgtBody2 = createUserDataBody("tgtData2", "tgt");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody, cell, box, col, "srcEntity");
            UserDataUtils.createViaNP(token,
                    tgtBody1, cell, box, col, "srcEntity", "srcData1", "tgtEntity", HttpStatus.SC_CREATED);
            UserDataUtils.createViaNP(token,
                    tgtBody2, cell, box, col, "srcEntity", "srcData1", "tgtEntity", HttpStatus.SC_CREATED);

            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData2", HttpStatus.SC_NO_CONTENT);
            // 削除後の確認
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData1");
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData2");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", -1);
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData2", -1);
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "srcEntity", "srcData1", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData1", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData2", col);
            // CopmlexType/Property/ComplexTypePropertyの削除
            deletePropertiesForUserDataLinkDeleteTest(cell, box, col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * ユーザデータが1対Nのlinkを削除した後に取得したユーザデータの内容が正しいこと.
     */
    @Test
    public final void ユーザデータが1対Nのlinkを削除した後に取得したユーザデータの内容が正しいこと() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "*", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // CopmlexType/Property/ComplexTypePropertyの作成
            createPropertiesForUserDataLinkDeleteTest(cell, box, col);
            // ユーザデータ（srouce）作成
            JSONObject srcBody = createUserDataBody("srcData1", "src");
            JSONObject tgtBody1 = createUserDataBody("tgtData1", "tgt");
            JSONObject tgtBody2 = createUserDataBody("tgtData2", "tgt");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody, cell, box, col, "srcEntity");
            UserDataUtils.createViaNP(token,
                    tgtBody1, cell, box, col, "srcEntity", "srcData1", "tgtEntity", HttpStatus.SC_CREATED);
            UserDataUtils.createViaNP(token,
                    tgtBody2, cell, box, col, "srcEntity", "srcData1", "tgtEntity", HttpStatus.SC_CREATED);

            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData2", HttpStatus.SC_NO_CONTENT);
            // 削除後の確認
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData1");
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData2");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", -1);
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData2", -1);
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "srcEntity", "srcData1", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData1", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData2", col);
            // CopmlexType/Property/ComplexTypePropertyの削除
            deletePropertiesForUserDataLinkDeleteTest(cell, box, col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * ユーザデータがN対0_1のlinkを削除した後に取得したユーザデータの内容が正しいこと.
     */
    @Test
    public final void ユーザデータがN対0_1のlinkを削除した後に取得したユーザデータの内容が正しいこと() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "*", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "0..1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // CopmlexType/Property/ComplexTypePropertyの作成
            createPropertiesForUserDataLinkDeleteTest(cell, box, col);
            // ユーザデータ作成
            JSONObject srcBody1 = createUserDataBody("srcData1", "src");
            JSONObject srcBody2 = createUserDataBody("srcData2", "src");
            JSONObject tgtBody1 = createUserDataBody("tgtData1", "tgt");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody1, cell, box, col, "srcEntity");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody2, cell, box, col, "srcEntity");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, tgtBody1, cell, box, col, "tgtEntity");
            // ユーザデータの$links登録
            ResourceUtils.linksUserData("srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            ResourceUtils.linksUserData("srcEntity", "srcData2", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData2", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // 削除後の確認
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData1");
            verifyUserDataLinkRemoval(cell, box, col, "srcData2", "tgtData1");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", -1);
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData2", "tgtEntity", "tgtData1", -1);
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "srcEntity", "srcData1", col);
            UserDataUtils.delete(token, -1, "srcEntity", "srcData2", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData1", col);
            // CopmlexType/Property/ComplexTypePropertyの削除
            deletePropertiesForUserDataLinkDeleteTest(cell, box, col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * ユーザデータがN対1のlinkを削除した後に取得したユーザデータの内容が正しいこと.
     */
    @Test
    public final void ユーザデータがN対1のlinkを削除した後に取得したユーザデータの内容が正しいこと() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "*", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // CopmlexType/Property/ComplexTypePropertyの作成
            createPropertiesForUserDataLinkDeleteTest(cell, box, col);
            // ユーザデータ作成
            JSONObject srcBody1 = createUserDataBody("srcData1", "src");
            JSONObject srcBody2 = createUserDataBody("srcData2", "src");
            JSONObject tgtBody1 = createUserDataBody("tgtData1", "tgt");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody1, cell, box, col, "srcEntity");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, srcBody2, cell, box, col, "srcEntity");
            UserDataUtils.create(token, HttpStatus.SC_CREATED, tgtBody1, cell, box, col, "tgtEntity");
            // ユーザデータの$links登録
            ResourceUtils.linksUserData("srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            ResourceUtils.linksUserData("srcEntity", "srcData2", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData2", "tgtEntity", "tgtData1", HttpStatus.SC_NO_CONTENT);
            // 削除後の確認
            verifyUserDataLinkRemoval(cell, box, col, "srcData1", "tgtData1");
            verifyUserDataLinkRemoval(cell, box, col, "srcData2", "tgtData1");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData1", "tgtEntity", "tgtData1", -1);
            UserDataUtils.deleteLinks(cell,
                    box, col, "srcEntity", "srcData2", "tgtEntity", "tgtData1", -1);
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "srcEntity", "srcData1", col);
            UserDataUtils.delete(token, -1, "srcEntity", "srcData2", col);
            UserDataUtils.delete(token, -1, "tgtEntity", "tgtData1", col);
            // CopmlexType/Property/ComplexTypePropertyの削除
            deletePropertiesForUserDataLinkDeleteTest(cell, box, col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * ユーザデータリンクの削除後に削除結果を確認する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     */
    private void verifyUserDataLinkRemoval(final String cell, final String box,
            final String col, final String srcUserDataId, final String tgtUserDataId) {
        // ユーザデータ間の$linksが削除されたことをNP経由のユーザデータ一覧取得で確認する
        DcResponse res = ODataCommon.getOdataResource(
                UrlUtils.userdataNP(cell, box, col, "srcEntity", srcUserDataId, "tgtEntity"));
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        JSONArray array = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
        assertEquals(0, array.size());

        // ユーザデータを取得してプロパティが消えていないことを確認する
        res = ODataCommon.getOdataResource(UrlUtils.userdata(cell, box, col, "srcEntity", srcUserDataId));
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        JSONObject body = (JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results");
        assertNotNull(body.get("srcProp1"));
        assertNotNull(body.get("srcProp2"));
        assertNotNull(body.get("srcProp3"));

        res = ODataCommon.getOdataResource(UrlUtils.userdata(cell, box, col, "tgtEntity", tgtUserDataId));
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        body = (JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results");
        assertNotNull(body.get("tgtProp1"));
        assertNotNull(body.get("tgtProp2"));
        assertNotNull(body.get("tgtProp3"));
    }

    /**
     * ユーザデータリンクの削除テストで使用するプロパティ情報の作成.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     */
    private void createPropertiesForUserDataLinkDeleteTest(final String cell,
            final String box, final String col) {
        // ComplexType作成
        UserDataUtils.createComplexType(cell, box, col, "srcComplex");
        UserDataUtils.createComplexType(cell, box, col, "tgtComplex");
        // Property(source)作成
        UserDataUtils.createProperty(cell, box, col,
                "srcProp1", "srcEntity", "Edm.String", true, null, "None", true, null);
        UserDataUtils.createProperty(cell, box, col,
                "srcProp2", "srcEntity", "srcComplex", true, null, "None", true, null);
        UserDataUtils.createProperty(cell, box, col,
                "srcProp3", "srcEntity", "Edm.Boolean", true, null, "None", true, null);
        // Property(target)作成
        UserDataUtils.createProperty(cell, box, col,
                "tgtProp1", "tgtEntity", "Edm.String", true, null, "None", true, null);
        UserDataUtils.createProperty(cell, box, col,
                "tgtProp2", "tgtEntity", "tgtComplex", true, null, "None", true, null);
        UserDataUtils.createProperty(cell, box, col,
                "tgtProp3", "tgtEntity", "Edm.Boolean", true, null, "None", true, null);
        // ComplexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(cell,
                box, col, "srcCProp1", "srcComplex", "Edm.String", true, null, "None");
        UserDataUtils.createComplexTypeProperty(cell,
                box, col, "tgtCProp1", "tgtComplex", "Edm.String", true, null, "None");
    }

    /**
     * ユーザデータリンクの削除テストで使用するプロパティ情報の削除.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     */
    private void deletePropertiesForUserDataLinkDeleteTest(final String cell,
            final String box, final String col) {
        // ComplexTypePropertyの削除
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(cell, box, col, "srcCProp1", "srcComplex"));
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(cell, box, col, "tgtCProp1", "tgtComplex"));
        // Property(source)の削除
        ODataCommon.deleteOdataResource(UrlUtils.property(cell, box, col, "srcProp1", "srcEntity"));
        ODataCommon.deleteOdataResource(UrlUtils.property(cell, box, col, "srcProp2", "srcEntity"));
        ODataCommon.deleteOdataResource(UrlUtils.property(cell, box, col, "srcProp3", "srcEntity"));
        // Property(target)の削除
        ODataCommon.deleteOdataResource(UrlUtils.property(cell, box, col, "tgtProp1", "tgtEntity"));
        ODataCommon.deleteOdataResource(UrlUtils.property(cell, box, col, "tgtProp2", "tgtEntity"));
        ODataCommon.deleteOdataResource(UrlUtils.property(cell, box, col, "tgtProp3", "tgtEntity"));
        // ComplexTypeの削除
        ODataCommon.deleteOdataResource(UrlUtils.complexType(cell, box, col, "srcComplex"));
        ODataCommon.deleteOdataResource(UrlUtils.complexType(cell, box, col, "tgtComplex"));
    }

    /**
     * ユーザデータリンク削除テスト用にユーザデータの作成用リクエストボディを生成する.
     * @param id ユーザデータの__id
     * @param prefix プロパティ名のプレフィックス
     * @return 生成したJSONObject
     * @throws IOException 文字列の読み込みに失敗
     * @throws ParseException JSONのパースに失敗
     */
    private JSONObject createUserDataBody(String id, String prefix) throws IOException, ParseException {
        String format = "{\"__id\":\"%s\",\"#Prop1\":\"prop\",\"#Prop2\":{\"#CProp1\":\"compProp\"},\"#Prop3\":true}";
        String bodyStr = String.format(format.replaceAll("#", prefix), id);
        return (JSONObject) new JSONParser().parse(new StringReader(bodyStr));
    }

    /**
     * 関連付けのないN対NのユーザODataの$linksで削除すると400が返却されること.
     * @throws ParseException JSONパースエラー
     */
    @Test
    public final void 関連付けのないN対NのユーザODataの$linksで削除すると400が返却されること()
            throws ParseException {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String srcMultiplicity = EdmMultiplicity.MANY.getSymbolString();
        String targetMultiplicity = EdmMultiplicity.MANY.getSymbolString();

        // 事前データの準備
        try {
            // リンク削除テスト用ユーザスキーマの登録
            createForDeleteLinkTest(cell, box, col, srcMultiplicity, targetMultiplicity);
            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse("{\"__id\":\"srcId\",\"name\":\"pochi01\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity01");
            body = (JSONObject) parser.parse("{\"__id\":\"targetId\",\"name\":\"pochi02\"}");
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME, body, cell, box, col,
                    "assocTestEntity01", "srcId", "assocTestEntity02",
                    HttpStatus.SC_CREATED);
            body = (JSONObject) parser.parse("{\"__id\":\"errorId\",\"name\":\"pochi03\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity03");

            // 関連付けのないユーザODataの$links削除時に400が返却されること
            // TODO 本来はリソースとして存在しない状態なので404となるべき
            UserDataUtils.deleteLinks(cell, box, col, "assocTestEntity01", "srcId", "assocTestEntity03", "errorId",
                    HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteDataForDeleteLinkTest(cell, box, col);
        }
    }

    /**
     * 関連付けのない0_1対0_1のユーザODataの$linksで削除すると400が返却されること.
     * @throws ParseException JSONパースエラー
     */
    @Test
    public final void 関連付けのない0_1対0_1のユーザODataの$linksで削除すると400が返却されること()
            throws ParseException {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String srcMultiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();
        String targetMultiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();

        // 事前データの準備
        try {
            // リンク削除テスト用ユーザスキーマの登録
            createForDeleteLinkTest(cell, box, col, srcMultiplicity, targetMultiplicity);
            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse("{\"__id\":\"srcId\",\"name\":\"pochi01\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity01");
            body = (JSONObject) parser.parse("{\"__id\":\"targetId\",\"name\":\"pochi02\"}");
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME, body, cell, box, col,
                    "assocTestEntity01", "srcId", "assocTestEntity02",
                    HttpStatus.SC_CREATED);
            body = (JSONObject) parser.parse("{\"__id\":\"errorId\",\"name\":\"pochi03\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity03");

            // 関連付けのないユーザODataの$links削除時に400が返却されること
            // TODO 本来はリソースとして存在しない状態なので404となるべき
            UserDataUtils.deleteLinks(cell, box, col, "assocTestEntity01", "srcId", "assocTestEntity03", "errorId",
                    HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteDataForDeleteLinkTest(cell, box, col);
        }
    }

    /**
     * 関連付けのない0_1対NのユーザODataの$linksで削除すると400が返却されること.
     * @throws ParseException JSONパースエラー
     */
    @Test
    public final void 関連付けのない0_1対NのユーザODataの$linksで削除すると400が返却されること()
            throws ParseException {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String srcMultiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();
        String targetMultiplicity = EdmMultiplicity.MANY.getSymbolString();

        // 事前データの準備
        try {
            // リンク削除テスト用ユーザスキーマの登録
            createForDeleteLinkTest(cell, box, col, srcMultiplicity, targetMultiplicity);
            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse("{\"__id\":\"srcId\",\"name\":\"pochi01\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity01");
            body = (JSONObject) parser.parse("{\"__id\":\"targetId\",\"name\":\"pochi02\"}");
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME, body, cell, box, col,
                    "assocTestEntity01", "srcId", "assocTestEntity02",
                    HttpStatus.SC_CREATED);
            body = (JSONObject) parser.parse("{\"__id\":\"errorId\",\"name\":\"pochi03\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity03");

            // 関連付けのないユーザODataの$links削除時に400が返却されること
            // TODO 本来はリソースとして存在しない状態なので404となるべき
            UserDataUtils.deleteLinks(cell, box, col, "assocTestEntity01", "srcId", "assocTestEntity03", "errorId",
                    HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteDataForDeleteLinkTest(cell, box, col);
        }
    }

    /**
     * 関連付けのないN対0_1のユーザODataの$linksで削除すると400が返却されること.
     * @throws ParseException JSONパースエラー
     */
    @Test
    public final void 関連付けのないN対0_1のユーザODataの$linksで削除すると400が返却されること()
            throws ParseException {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = Setup.TEST_ODATA;
        String srcMultiplicity = EdmMultiplicity.MANY.getSymbolString();
        String targetMultiplicity = EdmMultiplicity.ZERO_TO_ONE.getSymbolString();

        // 事前データの準備
        try {
            // リンク削除テスト用ユーザスキーマの登録
            createForDeleteLinkTest(cell, box, col, srcMultiplicity, targetMultiplicity);
            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse("{\"__id\":\"srcId\",\"name\":\"pochi01\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity01");
            body = (JSONObject) parser.parse("{\"__id\":\"targetId\",\"name\":\"pochi02\"}");
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME, body, cell, box, col,
                    "assocTestEntity01", "srcId", "assocTestEntity02",
                    HttpStatus.SC_CREATED);
            body = (JSONObject) parser.parse("{\"__id\":\"errorId\",\"name\":\"pochi03\"}");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cell, box, col, "assocTestEntity03");

            // 関連付けのないユーザODataの$links削除時に400が返却されること
            // TODO 本来はリソースとして存在しない状態なので404となるべき
            UserDataUtils.deleteLinks(cell, box, col, "assocTestEntity01", "srcId", "assocTestEntity03", "errorId",
                    HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteDataForDeleteLinkTest(cell, box, col);
        }
    }

    /**
     * リンク削除テスト用ユーザスキーマの登録.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param srcMultiplicity ソース側AssociationEndのMultiplicity
     * @param srcMultiplicity ターゲット側AssociationEndのMultiplicity
     */
    private void createForDeleteLinkTest(String cell, String box, String col,
            String srcMultiplicity, String targetMultiplicity) {
        EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity01", HttpStatus.SC_CREATED);
        EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity02", HttpStatus.SC_CREATED);
        EntityTypeUtils.create(cell, MASTER_TOKEN_NAME, box, col, "assocTestEntity03", HttpStatus.SC_CREATED);
        AssociationEndUtils.create(MASTER_TOKEN_NAME, srcMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                "assoc01", "assocTestEntity01");
        AssociationEndUtils.create(MASTER_TOKEN_NAME, targetMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                "assoc02", "assocTestEntity02");
        AssociationEndUtils.create(MASTER_TOKEN_NAME, targetMultiplicity, cell, box, col, HttpStatus.SC_CREATED,
                "assoc03", "assocTestEntity03");
        AssociationEndUtils.createLink(MASTER_TOKEN_NAME, cell, box, col, "assocTestEntity01",
                "assocTestEntity02",
                "assoc01", "assoc02", HttpStatus.SC_NO_CONTENT);
    }

    /**
     * リンク削除テスト用データの削除.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     */
    private void deleteDataForDeleteLinkTest(String cell, String box, String col) {
        UserDataUtils.deleteLinks(cell, box, col, "assocTestEntity01", "srcId", "assocTestEntity02", "targetId", -1);
        UserDataUtils.delete(MASTER_TOKEN_NAME, -1, cell, box, col, "assocTestEntity01", "srcId");
        UserDataUtils.delete(MASTER_TOKEN_NAME, -1, cell, box, col, "assocTestEntity02", "targetId");
        UserDataUtils.delete(MASTER_TOKEN_NAME, -1, cell, box, col, "assocTestEntity03", "errorId");
        String key = "_EntityType.Name='assocTestEntity01',Name='assoc01'";
        String navKey = "_EntityType.Name='assocTestEntity02',Name='assoc02'";
        AssociationEndUtils.deleteLink(cell, col, box, key, navKey, -1);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity01", box, "assoc01", -1);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity02", box, "assoc02", -1);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, col, "assocTestEntity03", box, "assoc03", -1);
        EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity01", box, cell,
                -1);
        EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity02", box, cell,
                -1);
        EntityTypeUtils.delete(col, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, "assocTestEntity03", box, cell,
                -1);
    }

}
