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
package io.personium.test.unit.core.bar;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.bar.BarFileReadRunner;
import io.personium.core.model.DavCmp;
import io.personium.core.model.impl.fs.DavCmpFsImpl;
import io.personium.test.categories.Unit;

/**
 * BarFileのバリデートのユニットテストクラス.
 */
@Category({Unit.class })
public class BarFileContentsTest {
    private static DavCmp newDavCmp(String name) {
        return newDavCmpFsImpl(name);
    }
    private static DavCmp newDavCmpFsImpl(String name) {
        return DavCmpFsImpl.create(name, null);
    }

    private static final String ODATA_COL_NAME = "odataCol";
    private static final String WEBDAV_COL_NAME = "davCol";
    private static final String SVC_COL_NAME = "svcCol";

    /**
     * .
     */
    private class TestBarRunner extends BarFileReadRunner {
        TestBarRunner() {
            super(null, null, null, null, null, null, null, null);
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
            return super.getEntryType(entryName, odataCols, webdavCols, serviceCols, davFiles);
        }

        /**
         * bar/90_contents/{col_name}配下のエントリが正しい定義であるかどうかを確認する.
         * @param entryName エントリ名(コレクション名)
         * @param colMap コレクションのMapオブジェクト
         * @param doneKeys 処理済みのODataコレクション用エントリリスト
         * @return 判定処理結果
         */
        protected boolean isValidODataContents(String entryName, Map<String, DavCmp> colMap,
                List<String> doneKeys) {
            return super.isValidODataContents(entryName, colMap, doneKeys);
        }

    }

    /**
     * 90_contents直下のODataコレクションの場合TYPE_ODATA_COLLECTIONが返却されること.
     */
    @Test
    public void contents直下のODataコレクションの場合TYPE_ODATA_COLLECTIONが返却されること() {

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();


        String key = "bar/90_contents/" + ODATA_COL_NAME;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + WEBDAV_COL_NAME;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + SVC_COL_NAME;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();

        String entryName = "bar/90_contents/" + ODATA_COL_NAME;

        TestBarRunner testBarRunner = new TestBarRunner();
        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(1, res);

    }

    /**
     * ODataコレクション配下のエントリの場合TYPE_ODATA_COLLECTIONが返却されること.
     */
    @Test
    public void ODataコレクション配下のエントリの場合TYPE_ODATA_COLLECTIONが返却されること() {
        String odatacolName = "odataCol";
        String webDavColName = "davCol";
        String svcColName = "svcCol";

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();

        String entryName = "bar/90_contents/odataCol/90_data/entityType/1.json";

        String key = "bar/90_contents/" + odatacolName;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + webDavColName;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + svcColName;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();

        TestBarRunner testBarRunner = new TestBarRunner();
        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(1, res);

    }

    /**
     * WebDavコレクション配下のODataコレクションの場合TYPE_ODATA_COLLECTIONが返却されること.
     */
    @Test
    public void WebDavコレクション配下のODataコレクションの場合TYPE_ODATA_COLLECTIONが返却されること() {
        String odatacolName = "odataCol";
        String webDavColName = "davCol";
        String svcColName = "svcCol";

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();


        String entryName = "bar/90_contents/davCol/" + odatacolName;

        String key = "bar/90_contents/davCol/" + odatacolName;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + webDavColName;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + svcColName;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();

        TestBarRunner testBarRunner = new TestBarRunner();
        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(1, res);

    }

    /**
     * 90_contents直下のWebDAVコレクションの場合TYPE_WEBDAV_COLLECTIONが返却されること.
     */
    @Test
    public void contents直下のODataコレクションの場合TYPE_WEBDAV_COLLECTIONが返却されること() {
        String entryName = "bar/90_contents/davCol";

        String odatacolName = "odataCol";
        String webDavColName = "davCol";
        String svcColName = "svcCol";

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();

        String key = "bar/90_contents/" + odatacolName;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + webDavColName;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + svcColName;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();

        TestBarRunner testBarRunner = new TestBarRunner();
        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(0, res);

    }

    /**
     * WebDAVコレクション配下のファイルの場合TYPE_DAV_FILEが返却されること.
     */
    @Test
    public void WebDAVコレクション配下のファイルの場合TYPE_DAV_FILEが返却されること() {
        String entryName = "bar/90_contents/davCol/hoge.jpg";

        String odatacolName = "odataCol";
        String webDavColName = "davCol";
        String svcColName = "svcCol";

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();

        String key = "bar/90_contents/" + odatacolName;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + webDavColName;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + svcColName;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();
        davFileMap.put("bar/90_contents/" + webDavColName + "/hoge.jpg", "image/jpeg");

        TestBarRunner testBarRunner = new TestBarRunner();

        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(3, res);

    }

    /**
     * 90_contents直下のServiceコレクションの場合TYPE_SERVICE_COLLECTIONが返却されること.
     */
    @Test
    public void contents直下のServiceコレクションの場合TYPE_SERVICE_COLLECTIONが返却されること() {
        String entryName = "bar/90_contents/svcCol";

        String odatacolName = "odataCol";
        String webDavColName = "davCol";
        String svcColName = "svcCol";

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();

        String key = "bar/90_contents/" + odatacolName;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + webDavColName;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + svcColName;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();

        TestBarRunner testBarRunner = new TestBarRunner();
        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(2, res);

    }

    /**
     * Serviceコレクション配下のファイルの場合TYPE_SVC_FILEが返却されること.
     */
    @Test
    public void Serviceコレクション配下のファイルの場合TYPE_SVC_FILEが返却されること() {
        String entryName = "bar/90_contents/svcCol/hoge.js";

        String odatacolName = "odataCol";
        String webDavColName = "davCol";
        String svcColName = "svcCol";

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();

        String key = "bar/90_contents/" + odatacolName;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + webDavColName;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + svcColName;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();
        davFileMap.put("bar/90_contents/svcCol/__src/hoge.js", "text/javascript");

        TestBarRunner testBarRunner = new TestBarRunner();
        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(4, res);

    }

    /**
     * 存在しないコレクションのファイルの場合TYPE_MISMATCHが返却されること.
     */
    @Test
    public void 存在しないコレクションのファイルの場合TYPE_MISMATCHが返却されること() {
        String entryName = "bar/90_contents/dummyCol";

        String odatacolName = "odataCol";
        String webDavColName = "davCol";
        String svcColName = "svcCol";

        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);
        DavCmp webDavCol = newDavCmp(WEBDAV_COL_NAME);
        DavCmp svcCol = newDavCmp(SVC_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> webDavCols = new HashMap<String, DavCmp>();
        Map<String, DavCmp> svcCols = new HashMap<String, DavCmp>();

        String key = "bar/90_contents/" + odatacolName;
        odataCols.put(key, odataCol);

        key = "bar/90_contents/" + webDavColName;
        webDavCols.put(key, webDavCol);

        key = "bar/90_contents/" + svcColName;
        svcCols.put(key, svcCol);

        Map<String, String> davFileMap = new HashMap<String, String>();

        TestBarRunner testBarRunner = new TestBarRunner();
        int res = testBarRunner.getEntryType(entryName, odataCols, webDavCols, svcCols, davFileMap);
        assertEquals(-1, res);

    }

    /**
     * ODataコレクション配下の階層が正しい場合trueが返却されること.
     */
    @Test
    public void ODataコレクション配下の階層が正しい場合trueが返却されること() {

        String key = "bar/90_contents/odataCol/";
        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<>();
        odataCols.put(key, odataCol);

        List<String> doneKeys = new ArrayList<String>();
        TestBarRunner testBarRunner = new TestBarRunner();

        String entryName = "bar/90_contents/odataCol/";
        boolean res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/odataCol/00_$metadata.xml";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/odataCol/10_odatarelations.json";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/odataCol/90_data/";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/odataCol/90_data/entity1/";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/odataCol/90_data/entity1/1.json";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);
    }

    /**
     * WebDavコレクション配下のODataコレクション配下の階層が正しい場合trueが返却されること.
     */
    @Test
    public void WebDavコレクション配下のODataコレクション配下の階層が正しい場合trueが返却されること() {

        String key = "bar/90_contents/webdavCol/odataCol/";
        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<>();
        odataCols.put(key, odataCol);

        List<String> doneKeys = new ArrayList<String>();
        TestBarRunner testBarRunner = new TestBarRunner();

        String entryName = "bar/90_contents/webdavCol/";
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/";
        boolean res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/00_$metadata.xml";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/10_odatarelations.json";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/90_data/";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/90_data/entity1/";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/90_data/entity1/1.json";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);
    }

    /**
     * ODataコレクション直下のファイル順序が不正な場合falseが返却されること.
     */
    @Test
    public void ODataコレクション直下のファイル順序が不正な場合falseが返却されること() {

        String key = "bar/90_contents/webdavCol/odataCol/";
        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<>();
        odataCols.put(key, odataCol);

        List<String> doneKeys = new ArrayList<String>();
        TestBarRunner testBarRunner = new TestBarRunner();

        String entryName = "bar/90_contents/webdavCol/";
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/";
        boolean res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(true, res);
        doneKeys.add(entryName);

        // 10_odatarelations.jsonが最初に来た場合
        entryName = "bar/90_contents/webdavCol/odataCol/10_odatarelations.json";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(false, res);

        // 90_dataが最初に来た場合
        entryName = "bar/90_contents/webdavCol/odataCol/90_data/";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(false, res);

        // ユーザデータが最初に来た場合
        entryName = "bar/90_contents/webdavCol/odataCol/90_data/entity1/";
        res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(false, res);
    }

    /**
     * ODataコレクション配下のユーザデータディレクトリが存在しない場合falseが返却されること.
     */
    @Test
    public void ODataコレクション配下のユーザデータディレクトリが存在しない場合falseが返却されること() {

        String key = "bar/90_contents/webdavCol/odataCol/";
        DavCmp odataCol = newDavCmp(ODATA_COL_NAME);

        Map<String, DavCmp> odataCols = new HashMap<>();
        odataCols.put(key, odataCol);

        List<String> doneKeys = new ArrayList<String>();
        TestBarRunner testBarRunner = new TestBarRunner();

        String entryName = "bar/90_contents/webdavCol/";
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/";
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/00_$metadata.xml";
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/10_odatarelations.json";
        doneKeys.add(entryName);

        entryName = "bar/90_contents/webdavCol/odataCol/entity1/";
        boolean res = testBarRunner.isValidODataContents(entryName, odataCols, doneKeys);
        assertEquals(false, res);
    }
}
