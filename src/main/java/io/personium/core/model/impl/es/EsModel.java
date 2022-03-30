/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.model.impl.es;

import java.util.Map;

import io.personium.common.es.EsClient;
import io.personium.common.es.EsClient.Event;
import io.personium.common.es.EsIndex;
import io.personium.common.es.EsRequestLogInfo;
import io.personium.common.es.EsType;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.impl.es.accessor.CellAccessor;
import io.personium.core.model.impl.es.accessor.CellDataAccessor;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataEntityAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;

/**
 * Model handling Elastic Search in this application.
 */
public class EsModel {

    private static EsClient esClient;

    static {
        //Set handler to output log after connecting to ES
        EsClient.setEventHandler(Event.connected, new EsClient.EventHandler() {
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                PersoniumCoreLog.Es.CONNECTED.params(params).writeLog();
            }
        });
        //Set handler to output log after request other than ES registration
        EsClient.setEventHandler(Event.afterRequest, new EsClient.EventHandler() {
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                PersoniumCoreLog.Es.AFTER_REQUEST.params(params).writeLog();
            }
        });
        //Set handler to output log before creating index to ES
        EsClient.setEventHandler(Event.creatingIndex, new EsClient.EventHandler() {
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                PersoniumCoreLog.Es.CREATING_INDEX.params(params).writeLog();
            }
        });
        //Set handler to output log after ES registration request
        EsClient.setEventHandler(Event.afterCreate, new EsClient.EventHandler() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                if (logInfo == null) {
                    return; //Since there is no output information, log is not output and it ends
                } else if (UserDataODataProducer.USER_ODATA_NAMESPACE.equals(logInfo.getType())) {
                    String uuid = "";
                    Map<String, Object> body = logInfo.getData();
                    if (body != null && body.containsKey("s")) {
                        Map<String, Object> staticFields = (Map<String, Object>) body.get("s");
                        if (staticFields != null && staticFields.containsKey("__id")) {
                            uuid = (String) staticFields.get("__id");
                        }
                    }
                    PersoniumCoreLog.Es.AFTER_CREATE.params(logInfo.getIndex(),
                            logInfo.getType(), logInfo.getId(), logInfo.getOpType(), uuid).writeLog();
                    PersoniumCoreLog.Es.AFTER_CREATE_BODY.params(logInfo.getDataAsString()).writeLog();

                } else {
                    PersoniumCoreLog.Es.AFTER_CREATE.params(logInfo.getIndex(), logInfo.getType(),
                            logInfo.getId(), logInfo.getOpType(), logInfo.getDataAsString()).writeLog();
                }
            }
        });

        esClient = new EsClient(PersoniumUnitConfig.getEsClusterName(), PersoniumUnitConfig.getEsHosts());
    }

    private EsModel() {
    }

    /**
     * Returns the ES client object.
     * @return client object
     */
    public static EsClient client() {
        return esClient;
    }

    /**
     * Returns an Index operation object for management.
     * @return Index object
     */
    public static EsIndex idxAdmin() {
        return esClient.idxAdmin(PersoniumUnitConfig.getEsUnitPrefix(),
                Integer.valueOf(PersoniumUnitConfig.getESRetryTimes()),
                Integer.valueOf(PersoniumUnitConfig.getESRetryInterval()));
    }

    /**
     * Returns the Index operation object for UnitUser.
     * @param userUri UnitUser name (URL)
     * @return Index object
     */
    public static EsIndex idxUser(String userUri) {
        return esClient.idxUser(PersoniumUnitConfig.getEsUnitPrefix(),
                userUri,
                Integer.valueOf(PersoniumUnitConfig.getESRetryTimes()),
                Integer.valueOf(PersoniumUnitConfig.getESRetryInterval()));
    }

    /**
     * Returns the Index operation object for UnitUser from the ES index name.
     * @param indexName Index name of ES
     * @return Index object
     */
    public static EsIndex idxUserWithUnitPrefix(String indexName) {
        return esClient.idxUser(indexName,
                Integer.valueOf(PersoniumUnitConfig.getESRetryTimes()),
                Integer.valueOf(PersoniumUnitConfig.getESRetryInterval()));
    }

    /**
     * Returns the Index operation object with the specified name.
     * @param indexName index name
     * @param typeName index type
     * @param routingId type of index
     * @param times index type
     * @param interval index type
     * @return EsType object
     */
    public static EsType type(String indexName, String typeName, String routingId, int times, int interval) {
        return esClient.type(indexName, typeName, routingId, times, interval);
    }

    /**
     * Returns Type operation object for Cell.
     * @return Type object
     */
    public static EntitySetAccessor cell() {
        return new CellAccessor(idxAdmin(), Cell.EDM_TYPE_NAME, EsIndex.CELL_ROUTING_KEY_NAME);
    }

    /**
     * Return accessor that accesses Cell entire data without limiting Type.
     * @param unitUserName Unit user name.
     * @param cellId cell id.
     * @return Cell data accessor
     */
    public static CellDataAccessor cellData(String unitUserName, String cellId) {
        return new CellDataAccessor(idxUser(unitUserName), cellId);
    }

    /**
     * Returns Type operation object for Box.
     * @param cell Cell
     * @return Type object
     */
    public static EntitySetAccessor box(final Cell cell) {
        return cell(cell, Box.EDM_TYPE_NAME);
    }

    /**
     * Unit control type type name Return Type Operation object.
     * @param type Type name
     * @param cellId cellId
     * @return Type object
     */
    public static EntitySetAccessor unitCtl(final String type, final String cellId) {
        if ("Cell".equals(type)) {
            return EsModel.cell();
        } else {
            return new ODataEntityAccessor(idxAdmin(), type, cellId);
        }
    }

    /**
     * Returns the Cell control Type operation object with the specified type name of the specified Cell.
     * @param cell Cell
     * @param type Type name
     * @return Type object
     */
    public static EntitySetAccessor cellCtl(final Cell cell, final String type) {
        return cell(cell, type);
    }

    static EntitySetAccessor cell(final Cell cell, final String type) {
        String userUri = cell.getOwnerNormalized();
        return new ODataEntityAccessor(idxUser(userUri), type, cell.getId());
    }

    /**
     * Unit Returns the operation object of link information type between control objects.
     * @param cellId cellId
     * @return Type object
     */
    public static ODataLinkAccessor unitCtlLink(String cellId) {
        return new ODataLinkAccessor(idxAdmin(), TYPE_CTL_LINK, cellId);
    }

    /**
     * Returns the operation object of link information type between Cell control objects of specified Cell.
     * @param cell Cell
     * @return Type object
     */
    public static ODataLinkAccessor cellCtlLink(final Cell cell) {
        String userUri = cell.getOwnerNormalized();
        return new ODataLinkAccessor(idxUser(userUri), TYPE_CTL_LINK, cell.getId());
    }

    /**
     * Type name to save Link information.
     */
    public static final String TYPE_CTL_LINK = "link";

    /**
     * Returns BulkDataAccessor for Cell.
     * @return Type object
     */
    public static DataSourceAccessor batch() {
        return new DataSourceAccessor(idxAdmin());
    }

    /**
     * Returns the BulkDataAccessor of the specified Cell.
     * @param cell Cell
     * @return BulkDataAccessor
     */
    public static DataSourceAccessor batch(final Cell cell) {
        return new DataSourceAccessor(idxUser(cell.getOwnerNormalized()));
    }

    /**
     * Returns the DataSourceAccessor of the specified UnitUser name.
     * @param unitUserName unit user name
     * @return DataSourceAccessor
     */
    public static DataSourceAccessor dsa(final String unitUserName) {
        return new DataSourceAccessor(idxUser(unitUserName));
    }

    /**
     * Returns the DataSourceAccessor of the specified ES index name.
     * @param indexName Index name of ES
     * @return DataSourceAccessor
     */
    public static DataSourceAccessor getDataSourceAccessorFromIndexName(final String indexName) {
        return new DataSourceAccessor(idxUserWithUnitPrefix(indexName));
    }
}
