/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test.jersey.box.odatacol.batch;

import static io.personium.test.utils.BatchUtils.BOUNDARY;
import static io.personium.test.utils.BatchUtils.END_BOUNDARY;
import static io.personium.test.utils.BatchUtils.START_BOUNDARY;
import static io.personium.test.utils.BatchUtils.retrieveChangeSetResErrorBody;
import static io.personium.test.utils.BatchUtils.retrieveDeleteBody;
import static io.personium.test.utils.BatchUtils.retrieveGetBody;
import static io.personium.test.utils.BatchUtils.retrieveLinksPostBody;
import static io.personium.test.utils.BatchUtils.retrieveLinksPostResBody;
import static io.personium.test.utils.BatchUtils.retrievePostBody;

import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * UserData$batchでの$linkAPIテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchLinkTest extends AbstractUserDataBatchTest {

    /**
     * コンストラクタ.
     */
    public UserDataBatchLinkTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * $batchでユーザデータを$links登録できること.
     */
    @Test
    public final void $batchでユーザデータを$links登録できること() {
        try {
            String path = "Sales('srcKey')/\\$links/_Supplier";
            String linksBody = "{\"uri\":\""
                    + UrlUtils.userdata(cellName, boxName, colName, "Supplier", "tgtKey")
                    + "\"}";

            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody("Supplier", "tgtKey")
                    + START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, linksBody)
                    + END_BOUNDARY;

            // $batchリクエスト
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "tgtKey", true)
                    + START_BOUNDARY + retrieveLinksPostResBody()
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // $links一覧取得
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Supplier");

            // レスポンスボディのチェック
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "tgtKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "tgtKey", "Supplier", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "tgtKey", PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", PersoniumUnitConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのユーザデータ$links登録でエラーが発生した場合に適切なメッセージが返却されること.
     */
    @Test
    public final void $batchのユーザデータ$links登録でエラーが発生した場合に適切なメッセージが返却されること() {
        try {
            String path = "Sales('srcKey')/\\$links/_Supplier";
            String linksBody = "{\"uri\":\""
                    + UrlUtils.userdata(cellName, boxName, colName, "Supplier", "tgtKey")
                    + "\"}";

            String body = START_BOUNDARY
                    + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY
                    + retrievePostBody("Supplier", "tgtKey")
                    // $links元に存在しないEntitySet名を指定
                    + START_BOUNDARY
                    + retrieveLinksPostBody(HttpMethod.POST, "DummyEntitySet('srcKey')/\\$links/_Supplier",
                            linksBody)
                    // $links元に存在しないEntityキーを指定
                    + START_BOUNDARY
                    + retrieveLinksPostBody(HttpMethod.POST, "Sales('dummyKey')/\\$links/_Supplier", linksBody)
                    // $links先に存在しないEntitySet名を指定
                    + START_BOUNDARY
                    + retrieveLinksPostBody(HttpMethod.POST, "Sales('srcKey')/\\$links/_DummyEntitySet", linksBody)
                    // リクエストボディに空文字を指定
                    + START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, "")
                    // リクエストボディにuri以外のキーを指定
                    + START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, "{\"key\":\""
                            + UrlUtils.userdata(cellName, boxName, colName, "Supplier", "tgtKey")
                            + "\"}")
                    // リクエストボディの値が空文字
                    + START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, "{\"uri\":\"\"}")
                    // リクエストボディがurl形式でない
                    + START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, "{\"uri\":\"tgtKey\"}")
                    // リクエストボディで指定したEntitySetが存在しない
                    + START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, "{\"uri\":\""
                            + UrlUtils.userdata(cellName, boxName, colName, "DummyEntitySet", "tgtKey")
                            + "\"}")
                    // リクエストボディで指定したEntityキーが存在しない
                    + START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, "{\"uri\":\""
                            + UrlUtils.userdata(cellName, boxName, colName, "Supplier", "dummyKey")
                            + "\"}")
                    + END_BOUNDARY;

            // $batchリクエスト
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Supplier", "tgtKey", true)
                    // $links元に存在しないEntitySet名を指定
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_NOT_FOUND,
                            PersoniumCoreException.OData.NO_SUCH_ENTITY_SET.getCode(),
                            PersoniumCoreException.OData.NO_SUCH_ENTITY_SET.getMessage())
                    // $links元に存在しないEntityキーを指定
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_NOT_FOUND,
                            PersoniumCoreException.OData.NOT_FOUND.getCode(),
                            PersoniumCoreException.OData.NOT_FOUND.getMessage())
                    // $links先に存在しないEntitySet名を指定
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_BAD_REQUEST,
                            PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getCode(),
                            PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getMessage())
                    // リクエストボディに空文字を指定
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_BAD_REQUEST,
                            PersoniumCoreException.OData.JSON_PARSE_ERROR.getCode(),
                            PersoniumCoreException.OData.JSON_PARSE_ERROR.getMessage())
                    // リクエストボディにuri以外のキーを指定
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_BAD_REQUEST,
                            PersoniumCoreException.OData.JSON_PARSE_ERROR.getCode(),
                            PersoniumCoreException.OData.JSON_PARSE_ERROR.getMessage())
                    // リクエストボディの値が空文字
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_BAD_REQUEST,
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri").getMessage())
                    // リクエストボディがurl形式でない
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_BAD_REQUEST,
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri").getMessage())
                    // リクエストボディで指定したEntitySetが存在しない
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_BAD_REQUEST,
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri").getMessage())
                    // リクエストボディで指定したEntityキーが存在しない
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_BAD_REQUEST,
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                            PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri").getMessage())
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "tgtKey", "Supplier", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "tgtKey", PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", PersoniumUnitConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのユーザデータ$linksリクエストで未サポートのメソッドを指定した場合適切なメッセージが返却されること.
     */
    @Test
    public final void $batchのユーザデータ$linksリクエストで未サポートのメソッドを指定した場合適切なメッセージが返却されること() {
        try {
            String linksBody = "{\"uri\":\""
                    + UrlUtils.userdata(cellName, boxName, colName, "Supplier", "tgtKey")
                    + "\"}";

            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody("Supplier", "tgtKey")
                    // $links の一覧取得
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey')/\\$links/_Supplier")
                    // $links の更新
                    + START_BOUNDARY
                    + retrieveLinksPostBody(HttpMethod.PUT, "Sales('srcKey')/\\$links/_Supplier('tgtKey')", linksBody)
                    // $links の削除
                    + START_BOUNDARY + retrieveDeleteBody("Sales('srcKey')/\\$links/_Supplier('tgtKey')")
                    + END_BOUNDARY;

            // $batchリクエスト
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "tgtKey", true)
                    // $links の一覧取得
                    + START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_NOT_IMPLEMENTED,
                            PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getCode(),
                            PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getMessage())
                    // $links の更新
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_NOT_IMPLEMENTED,
                            PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getCode(),
                            PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getMessage())
                    // $links の削除
                    + START_BOUNDARY + retrieveChangesetErrorResponse(HttpStatus.SC_NOT_IMPLEMENTED,
                            PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getCode(),
                            PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getMessage())
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "tgtKey", "Supplier", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "tgtKey", PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", PersoniumUnitConfig.getMasterToken(), -1);
        }
    }

    /**
     * .
     */
    @Test
    public final void $batchで存在しないエンティティタイプのデータを削除した場合404レスポンスが返却されること() {
        String body = START_BOUNDARY + retrieveDeleteBody("notExists('testBatch1')")
                + END_BOUNDARY;

        // リクエスト実行
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                + END_BOUNDARY;
        checkBatchResponseBody(response, expectedBody);
    }

    private String retrieveQueryOperationResErrorBody(int statusCode, String code, String message) {
        // TODO ボディのURIのチェック
        return "Content-Type: application/http\n\n"
                + "HTTP/1.1 " + Integer.toString(statusCode) + " \n"
                + "Content-Type: application/json\n\n"
                + "\\{\"code\":\"" + code + "\",\"message\":\\{\"lang\":\"en\",\"value\":\"" + Pattern.quote(message)
                + ".*\n\n";
    }

    private String retrieveChangesetErrorResponse(int statusCode, String code, String message) {
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 " + String.valueOf(statusCode) + " \n"
                + "Content-Type: application/json\n\n"
                + "\\{\"code\":\"" + code + "\",\"message\":\\{\"lang\":\"en\",\"value\":\"" + Pattern.quote(message)
                + ".*\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

}
