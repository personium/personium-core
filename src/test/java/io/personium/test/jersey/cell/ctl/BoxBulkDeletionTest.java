/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.test.jersey.cell.ctl;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * Test class for Box recursive delete API.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BoxBulkDeletionTest extends ODataCommon {

    /**
     * Constructor.
     */
    public BoxBulkDeletionTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Normal test.
     * Box is empty.
     */
    @Test
    public void normal_box_is_empty() {
        String cellName = Setup.TEST_CELL1;
        String boxName = "BoxBulkDeletionTestBox";
        try {
            // ---------------
            // Preparation
            // ---------------
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // ---------------
            // Execution
            // ---------------
            BoxUtils.deleteRecursive(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // ---------------
            // Verification
            // ---------------
            BoxUtils.get(cellName, MASTER_TOKEN_NAME, boxName, HttpStatus.SC_NOT_FOUND);
        } finally {
            BoxUtils.deleteRecursive(cellName, boxName, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test.
     * Box is not empty.
     */
    @Test
    public void normal_box_is_not_empty() {
        String cellName = Setup.TEST_CELL1;
        String boxName = "BoxBulkDeletionTestBox";
        String roleName = "BoxBulkDeletionTestRole";
        String relationName = "BoxBulkDeletionTestRelation";
        String webDavCollectionName = "WebDAVCollection";
        String engineServiceCollectionName = "EngineServiceCollection";
        String odataCollectionName = "ODataCollection";
        String davFileName = "davFile.txt";
        String sourceFileName = "source.js";
        String entityTypeName = "entityType";
        try {
            // ---------------
            // Preparation
            // ---------------
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create box linked object.
            RoleUtils.create(cellName, MASTER_TOKEN_NAME, roleName, boxName, HttpStatus.SC_CREATED);
            RelationUtils.create(cellName, relationName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            ExtRoleUtils.create(cellName, UrlUtils.roleClassUrl("testCell", "testRole"), relationName, boxName,
                    MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create collections.
            DavResourceUtils.createWebDAVCollection(cellName, boxName, webDavCollectionName,
                    BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    cellName, boxName, engineServiceCollectionName);
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    cellName, boxName, odataCollectionName);
            // Create file.
            DavResourceUtils.createWebDAVFile(cellName, boxName, webDavCollectionName + "/" + davFileName,
                    "txt/html", MASTER_TOKEN_NAME, "test", HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollectionSource(cellName, boxName, engineServiceCollectionName,
                    sourceFileName, "text/javascript", MASTER_TOKEN_NAME, "test", HttpStatus.SC_CREATED);
            // Create OData.
            EntityTypeUtils.create(cellName, MASTER_TOKEN_NAME, boxName, odataCollectionName,
                    entityTypeName, HttpStatus.SC_CREATED);
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "1", cellName, boxName, odataCollectionName,
                    HttpStatus.SC_CREATED, "associationEnd", entityTypeName);
            UserDataUtils.createProperty(cellName, boxName, odataCollectionName, "property", entityTypeName,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, null, false, null);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, "{\"property\":\"data\"}",
                    cellName, boxName, odataCollectionName, entityTypeName);

            // ---------------
            // Execution
            // ---------------
            BoxUtils.deleteRecursive(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // ---------------
            // Verification
            // ---------------
            BoxUtils.get(cellName, MASTER_TOKEN_NAME, boxName, HttpStatus.SC_NOT_FOUND);
            RoleUtils.get(cellName, MASTER_TOKEN_NAME, roleName, boxName, HttpStatus.SC_NOT_FOUND);
            RoleUtils.get(cellName, MASTER_TOKEN_NAME, roleName, null, HttpStatus.SC_NOT_FOUND);
            RelationUtils.get(cellName, MASTER_TOKEN_NAME, relationName, boxName, HttpStatus.SC_NOT_FOUND);
            RelationUtils.get(cellName, MASTER_TOKEN_NAME, relationName, null, HttpStatus.SC_NOT_FOUND);
            ExtRoleUtils.get(MASTER_TOKEN_NAME, cellName, UrlUtils.roleClassUrl("testCell", "testRole"),
                    "'" + relationName + "'", "'" + boxName + "'", HttpStatus.SC_NOT_FOUND);
            ExtRoleUtils.get(MASTER_TOKEN_NAME, cellName, UrlUtils.roleClassUrl("testCell", "testRole"),
                    "'" + relationName + "'", "null", HttpStatus.SC_NOT_FOUND);
        } finally {
            BoxUtils.deleteRecursive(cellName, boxName, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Error test.
     * X-Personium-Recursive is false.
     */
    @Test
    public void error_recursive_header_is_false() {
        String cellName = Setup.TEST_CELL1;
        String boxName = "BoxBulkDeletionTestBox";
        try {
            // ---------------
            // Preparation
            // ---------------
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // ---------------
            // Execution
            // ---------------
            TResponse response = BoxUtils.deleteRecursive(cellName, boxName, "false",
                    MASTER_TOKEN_NAME, HttpStatus.SC_PRECONDITION_FAILED);

            // ---------------
            // Verification
            // ---------------
            BoxUtils.get(cellName, MASTER_TOKEN_NAME, boxName, HttpStatus.SC_OK);
            PersoniumCoreException expected = PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
            checkErrorResponseBody(response, expected.getCode(), expected.getMessage());
        } finally {
            BoxUtils.deleteRecursive(cellName, boxName, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Error test.
     * X-Personium-Recursive not exists.
     */
    @Test
    public void error_recursive_header_not_exists() {
        String cellName = Setup.TEST_CELL1;
        String boxName = "BoxBulkDeletionTestBox";
        try {
            // ---------------
            // Preparation
            // ---------------
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // ---------------
            // Execution
            // ---------------
            // This txt is used only for this test, so do not convert it to UtilMethod.
            TResponse response = Http.request("cell/box-bulk-delete-non-recursive-header.txt")
                    .with("cellName", cellName)
                    .with("boxName", boxName)
                    .with("token", MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED);

            // ---------------
            // Verification
            // ---------------
            BoxUtils.get(cellName, MASTER_TOKEN_NAME, boxName, HttpStatus.SC_OK);
            PersoniumCoreException expected = PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
            checkErrorResponseBody(response, expected.getCode(), expected.getMessage());
        } finally {
            BoxUtils.deleteRecursive(cellName, boxName, MASTER_TOKEN_NAME, -1);
        }
    }
}
