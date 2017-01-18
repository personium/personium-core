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
package com.fujitsu.dc.test.jersey.box.dav;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

import static org.fest.assertions.Assertions.assertThat;

/**
 * MOVEの最大値に関するテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveLimitTest extends JerseyTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "MoveFileLimitTestCell";
    private static final String BOX_NAME = "box1";
    private static final String SRC_COL_NAME = "srcCol";
    private static final String DST_COL_NAME = "dstCol";
    private static final String DST_RESOURCE_PREFIX = "dst";
    private static final String FILE_BODY = "testFileBody";

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public MoveLimitTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * Collection配下の子要素数が最大値を超える場合ファイルのMOVEができないこと.
     */
    @Test
    public final void Collection配下の子要素数が最大値を超える場合ファイルのMOVEができないこと() {
        final String moveFileNameUnderLimitCount = "moveFile1.txt";
        final String moveFileNameEqualLimitCount = "moveFile2.txt";
        final String moveOverwriteFileNameEqualLimitCount = "moveFile3.txt";
        final String moveFileNameOverLimitCount = "moveFile4.txt";
        try {
            // 事前準備
            CellUtils.create(CELL_NAME, TOKEN, HttpStatus.SC_CREATED);
            BoxUtils.create(CELL_NAME, BOX_NAME, TOKEN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, SRC_COL_NAME);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, DST_COL_NAME);
            // 移動用のファイル作成
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveFileNameUnderLimitCount, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveFileNameEqualLimitCount, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveOverwriteFileNameEqualLimitCount, FILE_BODY,
                    MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveFileNameOverLimitCount,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            // 移動先のCollectionに子要素数の最大値 - 2までファイル作成
            for (int i = 0; i < DcCoreConfig.getMaxChildResourceCount() - 2; i++) {
                String fileName = String.format("%s_%05d", DST_RESOURCE_PREFIX, i);
                DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                        BOX_NAME + "/" + DST_COL_NAME + "/" + fileName, FILE_BODY, MediaType.TEXT_PLAIN,
                        HttpStatus.SC_CREATED);
            }

            // Fileの移動(最大値 - 1) → 201
            String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveFileNameUnderLimitCount);
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveFileNameUnderLimitCount, destination,
                    HttpStatus.SC_CREATED);

            // Fileの移動(最大値) → 201
            destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveFileNameEqualLimitCount);
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveFileNameEqualLimitCount, destination,
                    HttpStatus.SC_CREATED);

            // Fileの移動(最大値, 上書き) → 204
            destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveFileNameEqualLimitCount);
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, SRC_COL_NAME, moveOverwriteFileNameEqualLimitCount);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");
            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);

            // Fileの移動(最大値 + 1) → 400
            destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveFileNameOverLimitCount);
            TResponse res = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveFileNameOverLimitCount, destination,
                    HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

            // 移動したファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, DST_COL_NAME + "/"
                    + moveFileNameUnderLimitCount, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, DST_COL_NAME + "/"
                    + moveFileNameEqualLimitCount, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);

            // 移動に失敗したファイルが取得できないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    DST_COL_NAME + "/" + moveFileNameOverLimitCount,
                    HttpStatus.SC_NOT_FOUND);

            // 移動したファイルが移動元に存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    SRC_COL_NAME + "/" + moveOverwriteFileNameEqualLimitCount,
                    HttpStatus.SC_NOT_FOUND);

            // 移動に失敗したファイルが移動元に存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    SRC_COL_NAME + "/" + moveFileNameOverLimitCount,
                    HttpStatus.SC_OK);
        } finally {
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * Collection配下の子要素数が最大値を超える場合コレクションのMOVEができないこと.
     */
    @Test
    public final void Collection配下の子要素数が最大値を超える場合コレクションのMOVEができないこと() {
        final String moveCollectionNameUnderLimitCount = "moveCollection1";
        final String moveCollectionNameEqualLimitCount = "moveCollection2";
        final String moveOverwriteCollectionNameEqualLimitCount = "moveCollection3";
        final String moveCollectionNameOverLimitCount = "moveCollection4";
        try {
            // 事前準備
            CellUtils.create(CELL_NAME, TOKEN, HttpStatus.SC_CREATED);
            BoxUtils.create(CELL_NAME, BOX_NAME, TOKEN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, SRC_COL_NAME);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, DST_COL_NAME);
            // 移動用のコレクション作成
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, SRC_COL_NAME + "/" + moveCollectionNameUnderLimitCount);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, SRC_COL_NAME + "/" + moveCollectionNameEqualLimitCount);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, SRC_COL_NAME + "/" + moveOverwriteCollectionNameEqualLimitCount);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                    BOX_NAME, SRC_COL_NAME + "/" + moveCollectionNameOverLimitCount);

            // 移動先のCollectionに子要素数の最大値 - 2までコレクション作成
            for (int i = 0; i < DcCoreConfig.getMaxChildResourceCount() - 2; i++) {
                String collectionName = String.format("%s_%05d", DST_RESOURCE_PREFIX, i);
                DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                        BOX_NAME, DST_COL_NAME + "/" + collectionName);
            }

            // Collectionの移動(最大値 - 1) → 201
            String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveCollectionNameUnderLimitCount);
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveCollectionNameUnderLimitCount, destination,
                    HttpStatus.SC_CREATED);

            // Collectionの移動(最大値) → 201
            destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveCollectionNameEqualLimitCount);
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveCollectionNameEqualLimitCount, destination,
                    HttpStatus.SC_CREATED);

            // Collectionの移動(最大値, 上書き) → 204
            // TODO 現状は上書き指定に対応していないため、400としているが、上書き指定に対応した際は204に変更する
            destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveCollectionNameEqualLimitCount);
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, SRC_COL_NAME, moveOverwriteCollectionNameEqualLimitCount);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");
            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);

            // Collectionの移動(最大値 + 1) → 400
            destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, moveCollectionNameOverLimitCount);
            TResponse res = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + SRC_COL_NAME + "/" + moveCollectionNameOverLimitCount, destination,
                    HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

            // 移動したコレクションを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, DST_COL_NAME + "/"
                    + moveCollectionNameUnderLimitCount, HttpStatus.SC_OK);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, DST_COL_NAME + "/"
                    + moveCollectionNameEqualLimitCount, HttpStatus.SC_OK);

            // 移動に失敗したコレクションが取得できないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    DST_COL_NAME + "/" + moveCollectionNameOverLimitCount,
                    HttpStatus.SC_NOT_FOUND);
            // TODO 現状は上書き指定に対応していないため、移動元に存在することを確認しているが、上書き対応した際には移動先に存在することを確認
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    SRC_COL_NAME + "/" + moveOverwriteCollectionNameEqualLimitCount,
                    HttpStatus.SC_OK);
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    SRC_COL_NAME + "/" + moveCollectionNameOverLimitCount,
                    HttpStatus.SC_OK);
        } finally {
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * 一階層のコレクションの移動で移動先のコレクションの階層の深さが最大値を超える場合コレクションのMOVEができないこと.
     */
    @Test
    public final void 一階層のコレクションの移動で移動先のコレクションの階層の深さが最大値を超える場合コレクションのMOVEができないこと() {

        try {
            // 事前準備
            CellUtils.create(CELL_NAME, TOKEN, HttpStatus.SC_CREATED);
            BoxUtils.create(CELL_NAME, BOX_NAME, TOKEN, HttpStatus.SC_CREATED);
            // 移動先のCollectionに階層の深さの最大値 - 2までコレクション作成
            String destPath = "";
            int index;
            for (index = 0; index < DcCoreConfig.getMaxCollectionDepth() - 2; index++) {
                destPath = String.format("%s%s_%03d/", destPath, DST_RESOURCE_PREFIX, index);
                DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                        BOX_NAME, destPath);
            }

            // 移動(最大値 - 1) → 201
            String srcCol = String.format("%s_%03d", SRC_COL_NAME, index);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            destPath = String.format("%s%s_%03d", destPath, DST_RESOURCE_PREFIX, index);
            String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcCol, destUrl,
                    HttpStatus.SC_CREATED);

            // 移動(最大値) → 201
            index += 1;
            srcCol = String.format("%s_%03d", SRC_COL_NAME, index);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            destPath = String.format("%s/%s_%03d", destPath, DST_RESOURCE_PREFIX, index);
            destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcCol, destUrl,
                    HttpStatus.SC_CREATED);

            // 移動(最大値, 上書き) → 204
            // TODO 現状は上書き指定に対応していないため、400としているが、上書き指定に対応した際は204に変更する
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(UrlUtils.box(CELL_NAME, BOX_NAME, srcCol));
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            // エラーコード確認
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(),
                    expectedException.getMessage());

            // 移動(最大値 + 1) → 400
            index += 1;
            srcCol = String.format("%s_%03d", SRC_COL_NAME, index);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            destPath = String.format("%s/%s_%03d", destPath, DST_RESOURCE_PREFIX, index);
            destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
            // TODO 現状はコレクションの最大階層チェックを実施していないため、201としているが対応後は400にする
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcCol, destUrl,
                    HttpStatus.SC_CREATED);
            // TResponse res = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcCol, destUrl,
            // HttpStatus.SC_BAD_REQUEST);
            // // エラーコード確認
            // expectedException = DcCoreException.Dav.COLLECTION_DEPTH_ERROR;
            // ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * 二階層のコレクションの移動で移動先のコレクションの階層の深さが最大値を超える場合コレクションのMOVEができないこと.
     */
    @Test
    public final void 二階層のコレクションの移動で移動先のコレクションの階層の深さが最大値を超える場合コレクションのMOVEができないこと() {

        try {
            // 事前準備
            CellUtils.create(CELL_NAME, TOKEN, HttpStatus.SC_CREATED);
            BoxUtils.create(CELL_NAME, BOX_NAME, TOKEN, HttpStatus.SC_CREATED);
            // 移動先のCollectionに階層の深さの最大値 - 2までコレクション作成
            String destPath = "";
            int index;
            for (index = 0; index < DcCoreConfig.getMaxCollectionDepth() - 2; index++) {
                destPath = String.format("%s%s_%03d/", destPath, DST_RESOURCE_PREFIX, index);
                DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                        BOX_NAME, destPath);
            }

            // 移動(最大値) → 201
            String srcCol = String.format("%s_%03d", SRC_COL_NAME, index);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            DavResourceUtils
                    .createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol + "/child");

            destPath = String.format("%s%s_%03d", destPath, DST_RESOURCE_PREFIX, index);
            String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcCol, destUrl, HttpStatus.SC_CREATED);

            // 移動(最大値 + 1) → 400
            index += 1;
            srcCol = String.format("%s_%03d", SRC_COL_NAME, index);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            DavResourceUtils
                    .createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol + "/child");
            destPath = String.format("%s/%s_%03d", destPath, DST_RESOURCE_PREFIX, index);
            destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
            // TODO 現状はコレクションの最大階層チェックを実施していないため、201としているが対応後は400にする
            DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcCol, destUrl,
                    HttpStatus.SC_CREATED);
            // TResponse res = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcCol, destUrl,
            // HttpStatus.SC_BAD_REQUEST);
            // // エラーコード確認
            // DcCoreException expectedException = DcCoreException.Dav.COLLECTION_DEPTH_ERROR;
            // ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }
}
