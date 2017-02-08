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
package io.personium.core.model.impl.es.ads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.dbcp.DelegatingPreparedStatement;
import org.apache.commons.lang.CharEncoding;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.DavNode;
import io.personium.core.model.impl.es.doc.CellDocHandler;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.LinkDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;

/**
 * JDBCタイプのADS ( Authentic Data Store ).
 */
public class JdbcAds implements Ads {
    private static DataSource ds;
    Map<String, IndexPeer> peersMap = new HashMap<String, IndexPeer>();

    static Logger log = LoggerFactory.getLogger(JdbcAds.class);
    static final String SCHEMA_NAME_REPLACING_KEY = "##schema##";
    static final boolean AUTO_COMMIT = true;

    static final String MANAGEMENT_DB_NAME = "pcs_management";

    /**
     * コンストラクタ.
     * @throws AdsConnectionException ADS接続失敗
     */
    public JdbcAds() throws AdsConnectionException {

        try {
            if (ds == null) {
                Properties p = PersoniumUnitConfig.getEsAdsDbcpProps();
                ds = BasicDataSourceFactory.createDataSource(p);
            }
        } catch (Exception e) {
            log.info("Failed to create instance of Ads.");
            throw new AdsConnectionException(e);
        }
    }

    /**
     * Adsの接続確認を行う.
     * @throws AdsException 処理失敗時発生
     */
    public void checkConnection() throws AdsException {
        try {
            // 接続確認を行う
            ds.getConnection().close();
        } catch (SQLException e) {
            // 接続失敗時は例外を出力する
            log.info("Failed to connect Ads.");
            throw new AdsException(e);
        }
    }

    @Override
    public void createEntity(String index, EntitySetDocHandler edh) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.createEntity(edh);
    }

    @Override
    public void updateEntity(String index, EntitySetDocHandler edh) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.updateEntity(edh);
    }

    @Override
    public void deleteEntity(String index, String id) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.deleteEntity(id);
    }

    /**
     * Entity Document一括生成に伴い、Adsの対応レコード一括生成を行う.
     * @param index index
     * @param bulkRequestList 一括生成データ
     * @throws AdsException 処理失敗時発生
     */
    @Override
    public void bulkEntity(String index, List<EntitySetDocHandler> bulkRequestList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.bulkEntity(bulkRequestList);
    }

    /**
     * Entity Document一括更新に伴い、Adsの対応レコード一括更新を行う.
     * @param index index
     * @param bulkRequestList 一括更新データ
     * @throws AdsException 処理失敗時発生
     */
    @Override
    public void bulkUpdateEntity(String index, List<EntitySetDocHandler> bulkRequestList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.bulkUpdateEntityLink(bulkRequestList);
    }

    /**
     * Dav Document一括更新に伴い、Adsの対応レコード一括更新を行う.
     * @param index index
     * @param bulkRequestList 一括更新データ
     * @throws AdsException 処理失敗時発生
     */
    @Override
    public void bulkUpdateDav(String index, List<DavNode> bulkRequestList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.bulkUpdateDav(bulkRequestList);
    }

    @Override
    public void createCell(String index, EntitySetDocHandler docHandler) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.createCell(docHandler);
    }

    @Override
    public void updateCell(String index, EntitySetDocHandler docHandler) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.updateCell(docHandler);
    }

    @Override
    public void deleteCell(String index, String id) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.deleteCell(id);
    }

    @Override
    public void createLink(String index, LinkDocHandler ldh) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.createLink(ldh);
    }

    @Override
    public void updateLink(String index, LinkDocHandler ldh) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.updateLink(ldh);
    }

    @Override
    public void deleteLink(String index, String id) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.deleteLink(id);
    }

    @Override
    public void createDavNode(String index, DavNode davNode) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.createDavNode(davNode);
    }

    @Override
    public void updateDavNode(String index, DavNode davNode) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.updateDavNode(davNode);
    }

    @Override
    public void deleteDavNode(String index, String id) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.deleteDavNode(id);
    }

    @Override
    public long countEntity(String index) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.countEntity();
    }

    @Override
    public List<JSONObject> getEntityList(String index, long offset, long size) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.getEntityList(offset, size);
    }

    @Override
    public long countCell(String index) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.countCell();
    }

    @Override
    public List<JSONObject> getCellList(String index, long offset, long size) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.getCellList(offset, size);
    }

    @Override
    public long countLink(String index) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.countLink();
    }

    @Override
    public List<JSONObject> searchEntityList(String index, List<String> idList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.searchEntityList(idList);
    }

    @Override
    public List<JSONObject> searchCellList(String index, List<String> idList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.searchCellList(idList);
    }

    @Override
    public List<JSONObject> searchLinkList(String index, List<String> idList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.searchLinkList(idList);
    }

    @Override
    public List<JSONObject> searchDavNodeList(String index, List<String> idList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.searchDavNodeList(idList);
    }

    /**
     * Link Document生成に伴い、Adsの一括登録用のレコード生成を行う.
     * @param index index
     * @param bulkRequestList 一括登録するLinkDocHandlerのリスト
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    @Override
    public void bulkCreateLink(String index, List<LinkDocHandler> bulkRequestList) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.bulkCreateLink(bulkRequestList);
    }

    @Override
    public List<JSONObject> getLinkList(String index, long offset, long size) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.getLinkList(offset, size);
    }

    @Override
    public long countDavNode(String index) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.countDavNode();
    }

    @Override
    public List<JSONObject> getDavNodeList(String index, long offset, long size) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        return ip.getDavNodeList(offset, size);
    }

    @Override
    public void deleteCellResourceFromEntity(String index, String cellId) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.deleteCellResourceFromEntity(cellId);
    }

    @Override
    public void deleteCellResourceFromDavNode(String index, String cellId) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.deleteCellResourceFromDavNode(cellId);
    }

    @Override
    public void deleteCellResourceFromLink(String index, String cellId) throws AdsException {
        IndexPeer ip = this.getIndexPeer(index);
        ip.deleteCellResourceFromLink(cellId);
    }

    @Override
    public void insertCellDeleteRecord(String dbName, String cellId) throws AdsException {
        IndexPeer ip = this.getIndexPeer(MANAGEMENT_DB_NAME);
        ip.insertCellDeleteRecord(dbName, cellId);
    }

    @Override
    public void createIndex(String index) throws AdsException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = ds.getConnection();
            stmt = con.createStatement();
            int res = stmt.executeUpdate(Sql.createSchema.replace(SCHEMA_NAME_REPLACING_KEY, index));
            log.debug("create schema:" + res);
            res = stmt.executeUpdate(Sql.createTableEntity.replace(SCHEMA_NAME_REPLACING_KEY, index));
            log.debug("create table entity:" + res);
            res = stmt.executeUpdate(Sql.createTableLink.replace(SCHEMA_NAME_REPLACING_KEY, index));
            log.debug("create table Link:" + res);
            res = stmt.executeUpdate(Sql.createTableDavNode.replace(SCHEMA_NAME_REPLACING_KEY, index));
            log.debug("create table DavNode:" + res);
            res = stmt.executeUpdate(Sql.createTableCell.replace(SCHEMA_NAME_REPLACING_KEY, index));
            log.debug("create table cell:" + res);
        } catch (SQLException e) {
            throw new AdsException(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                throw new AdsException(e);
            }
        }
    }

    /**
     * 管理用DBを作成する.
     * @throws AdsException 管理用DB作成に失敗
     */
    public void createManagementDatabase() throws AdsException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = ds.getConnection();
            stmt = con.createStatement();
            stmt.executeUpdate(Sql.createSchema.replace(SCHEMA_NAME_REPLACING_KEY, MANAGEMENT_DB_NAME));
            stmt.executeUpdate(
                    Sql.createManagementTableCellDelete.replace(SCHEMA_NAME_REPLACING_KEY, MANAGEMENT_DB_NAME));
        } catch (SQLException e) {
            throw new AdsException(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                throw new AdsException(e);
            }
        }
    }

    @Override
    public void deleteIndex(String index) throws AdsException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = ds.getConnection();
            stmt = con.createStatement();
            int res = stmt.executeUpdate(Sql.dropSchema.replace(SCHEMA_NAME_REPLACING_KEY, index));
            log.debug("delete schema:" + res);
            stmt.close();
            con.close();
        } catch (SQLException e) {
            throw new AdsException(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                throw new AdsException(e);
            }
        }
    }

    IndexPeer getIndexPeer(final String index) {
        IndexPeer ip = this.peersMap.get(index);
        if (ip != null) {
            return ip;
        }
        ip = new IndexPeer(ds, index);
        this.peersMap.put(index, ip);
        return ip;
    }

    /**
     * Indexに対応するオブジェクト.
     */
    static class IndexPeer {
        DataSource ds;
        String index;
        String sqlEntityInsert;
        String sqlEntityUpdate;
        String sqlEntityDelete;
        String sqlEntityBulkInsert;
        String sqlCellInsert;
        String sqlCellUpdate;
        String sqlCellDelete;
        String sqlLinkInsert;
        String sqlLinkUpdate;
        String sqlLinkDelete;
        String sqlDavNodeInsert;
        String sqlDavNodeUpdate;
        String sqlDavNodeDelete;
        String sqlDavBulkInsert;
        String sqlDeleteCellResourceFromEntity;
        String sqlDeleteCellResourceFromDavNode;
        String sqlDeleteCellResourceFromLink;
        String sqlCountCellResourceFromEntity;
        String sqlCountCellResourceFromDavNode;
        String sqlCountCellResourceFromLink;
        String sqlEntitySelect;
        String sqlEntityCount;
        String sqlCellSelect;
        String sqlCellCount;
        String sqlLinkSelect;
        String sqlLinkCount;
        String sqlLinkBulkInsert;
        String sqlDavNodeSelect;
        String sqlDavNodeCount;
        String sqlEntitySearch;
        String sqlCellSearch;
        String sqlLinkSearch;
        String sqlDavNodeSearch;

        // 管理DB用SQL群
        String sqlCellDeleteInsert;

        static final StatementHandler NOP_STATEMENT_HANDLER = new StatementHandler() {
            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                writeLog(stmt);
            }
        };
        static final QueryResultHandler QUERY_RESULT_HANDLER_FOR_COUNT = new QueryResultHandler() {
            @Override
            public Object handle(ResultSet resultSet) throws AdsException {
                try {
                    if (!resultSet.next()) {
                        throw new AdsException("No row was returened while 1 row is expected to be returned.");
                    }
                    return resultSet.getLong(1);
                } catch (SQLException e) {
                    throw new AdsException(e);
                }
            }

            @Override
            public Object handleQueryIsEmpty() throws AdsException {
                throw new AdsException("No row was returened while 1 row is expected to be returned.");
            }
        };

        IndexPeer(DataSource ds, String index) {
            this.ds = ds;
            this.index = index;
            this.sqlEntityInsert = Sql.insertEntity.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlEntityUpdate = Sql.updateEntity.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlEntityDelete = Sql.deleteEntity.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlEntityBulkInsert = Sql.bulkInsertEntity.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCellInsert = Sql.insertCell.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCellUpdate = Sql.updateCell.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCellDelete = Sql.deleteCell.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlLinkInsert = Sql.insertLink.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlLinkBulkInsert = Sql.bulkInsertLink.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlLinkUpdate = Sql.updateLink.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlLinkDelete = Sql.deleteLink.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDavNodeInsert = Sql.insertDavNode.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDavNodeUpdate = Sql.updateDavNode.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDavNodeDelete = Sql.deleteDavNode.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDavBulkInsert = Sql.bulkInsertDav.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDeleteCellResourceFromEntity = Sql.deleteCellResourceFromEntity
                    .replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDeleteCellResourceFromDavNode = Sql.deleteCellResourceFromDavNode
                    .replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDeleteCellResourceFromLink = Sql.deleteCellResourceFromLink
                    .replace(SCHEMA_NAME_REPLACING_KEY, this.index);

            this.sqlCountCellResourceFromEntity = Sql.countCellResourceFromEntity
                    .replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCountCellResourceFromDavNode = Sql.countCellResourceFromDavNode
                    .replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCountCellResourceFromLink = Sql.countCellResourceFromLink
                    .replace(SCHEMA_NAME_REPLACING_KEY, this.index);

            this.sqlEntitySelect = Sql.selectEntity.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlEntityCount = Sql.countEntity.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCellSelect = Sql.selectCell.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCellCount = Sql.countCell.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlLinkSelect = Sql.selectLink.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlLinkCount = Sql.countLink.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDavNodeSelect = Sql.selectDav.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDavNodeCount = Sql.countDav.replace(SCHEMA_NAME_REPLACING_KEY, this.index);

            this.sqlEntitySearch = Sql.searchEntity.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlCellSearch = Sql.searchCell.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlLinkSearch = Sql.searchLink.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
            this.sqlDavNodeSearch = Sql.searchDav.replace(SCHEMA_NAME_REPLACING_KEY, this.index);

            // 管理DB用SQL群
            this.sqlCellDeleteInsert = Sql.insertCellDelete.replace(SCHEMA_NAME_REPLACING_KEY, this.index);
        }

        void createEntity(final EntitySetDocHandler oedh) throws AdsException {
            this.executeUpdateSql(this.sqlEntityInsert, new StatementHandlerForEntity(oedh));
        }

        void updateEntity(final EntitySetDocHandler oedh) throws AdsException {
            this.executeUpdateSql(this.sqlEntityUpdate, new StatementHandlerForEntity(oedh));
        }

        void deleteEntity(final String id) throws AdsException {
            this.executeUpdateSql(this.sqlEntityDelete, new StatementHandler() {
                @Override
                public void handle(PreparedStatement stmt) throws SQLException {
                    stmt.setString(1, id);
                    writeLog(stmt);
                }
            });
        }

        void bulkEntity(final List<EntitySetDocHandler> bulkRequestList) throws AdsException {
            // 一括登録のSQLを生成する
            StringBuilder sql = new StringBuilder(this.sqlEntityBulkInsert);
            for (int i = 0; i < bulkRequestList.size(); i++) {
                sql.append("(?,?,?,?,?,?,?,?,?,?,?,?)");
                if (i != bulkRequestList.size() - 1) {
                    sql.append(",");
                }
            }
            this.executeUpdateSql(sql.toString(), new StatementHandlerForBulkEntity(bulkRequestList),
                    bulkRequestList.size());

        }

        void bulkUpdateEntityLink(final List<EntitySetDocHandler> bulkRequestList) throws AdsException {
            // 一括更新のSQLを生成する
            StringBuilder sql = new StringBuilder(this.sqlEntityBulkInsert);
            for (int i = 0; i < bulkRequestList.size(); i++) {
                sql.append("(?,?,?,?,?,?,?,?,?,?,?,?)");
                if (i != bulkRequestList.size() - 1) {
                    sql.append(",");
                }
            }

            sql.append(" on duplicate key update ");
            sql.append("links=values(links)");

            int expectedUpdateCount = bulkRequestList.size() * 2;
            this.executeUpdateSql(sql.toString(), new StatementHandlerForBulkEntity(bulkRequestList),
                    expectedUpdateCount);

        }

        void bulkUpdateDav(List<DavNode> bulkRequestList) throws AdsException {
            // 一括更新のSQLを生成する
            StringBuilder sql = new StringBuilder(this.sqlDavBulkInsert);
            for (int i = 0; i < bulkRequestList.size(); i++) {
                sql.append("(?,?,?,?,?,?,?,?,?,?,?)");
                if (i != bulkRequestList.size() - 1) {
                    sql.append(",");
                }
            }

            sql.append(" on duplicate key update ");
            sql.append("cell_id=values(cell_id)");
            sql.append(",box_id=values(box_id)");
            sql.append(",parent_id=values(parent_id)");
            sql.append(",children=values(children)");
            sql.append(",node_type=values(node_type)");
            sql.append(",acl=values(acl)");
            sql.append(",properties=values(properties)");
            sql.append(",file=values(file)");
            sql.append(",published=values(published)");
            sql.append(",updated=values(updated)");
            sql.append(",id=values(id)");

            int expectedUpdateCount = bulkRequestList.size() * 2;
            this.executeUpdateSql(sql.toString(), new StatementHandlerForBulkDav(bulkRequestList),
                    expectedUpdateCount);

        }

        void bulkCreateLink(final List<LinkDocHandler> bulkRequestList) throws AdsException {
            // 一括登録のSQLを生成する
            StringBuilder sql = new StringBuilder(this.sqlLinkBulkInsert);
            for (int i = 0; i < bulkRequestList.size(); i++) {
                sql.append("(?,?,?,?,?,?,?,?,?,?)");
                if (i != bulkRequestList.size() - 1) {
                    sql.append(",");
                }
            }
            this.executeUpdateSql(sql.toString(), new StatementHandlerForBulkLink(bulkRequestList),
                    bulkRequestList.size());

        }

        void createCell(final EntitySetDocHandler docHandler) throws AdsException {
            this.executeUpdateSql(this.sqlCellInsert, new StatementHandlerForCell(docHandler));
        }

        void updateCell(final EntitySetDocHandler docHandler) throws AdsException {
            this.executeUpdateSql(this.sqlCellUpdate, new StatementHandlerForCell(docHandler));
        }

        void deleteCell(final String id) throws AdsException {
            this.executeUpdateSql(this.sqlCellDelete, new StatementHandler() {
                @Override
                public void handle(PreparedStatement stmt) throws SQLException {
                    stmt.setString(1, id);
                    writeLog(stmt);
                }
            });
        }

        void createLink(final LinkDocHandler ldh) throws AdsException {
            this.executeUpdateSql(this.sqlLinkInsert, new StatementHandlerForLink(ldh));
        }

        void updateLink(final LinkDocHandler ldh) throws AdsException {
            this.executeUpdateSql(this.sqlLinkUpdate, new StatementHandlerForLink(ldh));
        }

        void deleteLink(final String id) throws AdsException {
            this.executeUpdateSql(this.sqlLinkDelete, new StatementHandler() {
                @Override
                public void handle(PreparedStatement stmt) throws SQLException {
                    stmt.setString(1, id);
                    writeLog(stmt);
                }
            });
        }

        void createDavNode(final DavNode davNode) throws AdsException {
            this.executeUpdateSql(this.sqlDavNodeInsert, new StatementHandlerForDavNode(davNode));
        }

        void updateDavNode(final DavNode davNode) throws AdsException {
            this.executeUpdateSql(this.sqlDavNodeUpdate, new StatementHandlerForDavNode(davNode));
        }

        void deleteDavNode(final String id) throws AdsException {
            this.executeUpdateSql(this.sqlDavNodeDelete, new StatementHandler() {
                @Override
                public void handle(PreparedStatement stmt) throws SQLException {
                    stmt.setString(1, id);
                    writeLog(stmt);
                }
            });
        }

        void deleteCellResourceFromEntity(final String cellId) throws AdsException {
            this.executeDeleteByQuerySql(this.sqlDeleteCellResourceFromEntity,
                    this.sqlCountCellResourceFromEntity, new StatementHandler() {
                        @Override
                        public void handle(PreparedStatement stmt) throws SQLException {
                            stmt.setString(1, cellId);
                            writeLog(stmt);
                        }
                    });
        }

        void deleteCellResourceFromDavNode(final String cellId) throws AdsException {
            this.executeDeleteByQuerySql(this.sqlDeleteCellResourceFromDavNode,
                    this.sqlCountCellResourceFromDavNode, new StatementHandler() {
                        @Override
                        public void handle(PreparedStatement stmt) throws SQLException {
                            stmt.setString(1, cellId);
                            writeLog(stmt);
                        }
                    });
        }

        void deleteCellResourceFromLink(final String cellId) throws AdsException {
            this.executeDeleteByQuerySql(this.sqlDeleteCellResourceFromLink,
                    this.sqlCountCellResourceFromLink, new StatementHandler() {
                        @Override
                        public void handle(PreparedStatement stmt) throws SQLException {
                            stmt.setString(1, cellId);
                            writeLog(stmt);
                        }
                    });
        }

        void insertCellDeleteRecord(String dbName, String cellId) throws AdsException {
            String[] tableNames = {"ENTITY", "LINK", "DAV_NODE" };
            for (String tableName : tableNames) {
                this.executeUpdateSql(this.sqlCellDeleteInsert,
                        new StatementHandlerForCellDelete(dbName, tableName, cellId));
            }
        }

        Long countEntity() throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlEntityCount,
                    NOP_STATEMENT_HANDLER,
                    QUERY_RESULT_HANDLER_FOR_COUNT);
            return (Long) ret;
        }

        Long countCell() throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlCellCount,
                    NOP_STATEMENT_HANDLER,
                    QUERY_RESULT_HANDLER_FOR_COUNT);
            return (Long) ret;
        }

        Long countLink() throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlLinkCount,
                    NOP_STATEMENT_HANDLER,
                    QUERY_RESULT_HANDLER_FOR_COUNT);
            return (Long) ret;
        }

        Long countDavNode() throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlDavNodeCount,
                    NOP_STATEMENT_HANDLER,
                    QUERY_RESULT_HANDLER_FOR_COUNT);
            return (Long) ret;
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> getEntityList(final long offset, final long size) throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlEntitySelect,
                    new StatementHandler() {
                        @Override
                        public void handle(PreparedStatement stmt) throws SQLException {
                            stmt.setLong(1, offset);
                            stmt.setLong(2, size);
                        }
                    },
                    createEntityQueryResultHandler());
            return (List<JSONObject>) ret;
        }

        private QueryResultHandler createEntityQueryResultHandler() {
            return new QueryResultHandler() {
                @Override
                public Object handle(ResultSet resultSet) throws AdsException {
                    try {
                        List<JSONObject> ret = new ArrayList<JSONObject>();
                        while (resultSet.next()) {
                            ret.add(getEntityJsonObject(resultSet));
                        }
                        return ret;
                    } catch (SQLException e) {
                        throw new AdsException(e);
                    }
                }

                @Override
                public Object handleQueryIsEmpty() throws AdsException {
                    return new ArrayList<JSONObject>();
                }
            };
        }

        @SuppressWarnings("unchecked")
        private JSONObject getEntityJsonObject(ResultSet resultSet)
                throws SQLException {
            JSONObject json = new JSONObject();
            json.put("type", resultSet.getString(Sql.IDX_ENTITY_TYPE));
            json.put("id", resultSet.getString(Sql.NUMCOLS_ENTITY));
            JSONObject source = new JSONObject();
            json.put("source", source);
            source.put(OEntityDocHandler.KEY_CELL_ID,
                    resultSet.getString(Sql.IDX_ENTITY_CELL_ID));
            source.put(OEntityDocHandler.KEY_BOX_ID,
                    resultSet.getString(Sql.IDX_ENTITY_BOX_ID));
            source.put(OEntityDocHandler.KEY_NODE_ID,
                    resultSet.getString(Sql.IDX_ENTITY_NODE_ID));
            source.put(OEntityDocHandler.KEY_ENTITY_ID,
                    resultSet.getString(Sql.IDX_ENTITY_ENTITY_ID));
            source.put(OEntityDocHandler.KEY_STATIC_FIELDS,
                    resultSet.getString(Sql.IDX_ENTITY_DECLARED_PROPS));
            source.put(OEntityDocHandler.KEY_DYNAMIC_FIELDS,
                    resultSet.getString(Sql.IDX_ENTITY_DYNAMIC_PROPS));
            source.put(OEntityDocHandler.KEY_HIDDEN_FIELDS,
                    resultSet.getString(Sql.IDX_ENTITY_HIDDEN_FIELDS));
            source.put(OEntityDocHandler.KEY_LINK,
                    resultSet.getString(Sql.IDX_ENTITY_LINKS));
            source.put(OEntityDocHandler.KEY_PUBLISHED,
                    resultSet.getString(Sql.IDX_ENTITY_PUBLISHED));
            source.put(OEntityDocHandler.KEY_UPDATED,
                    resultSet.getString(Sql.IDX_ENTITY_UPDATED));
            return json;
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> getCellList(final long offset, final long size) throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlCellSelect,
                    new StatementHandler() {
                        @Override
                        public void handle(PreparedStatement stmt) throws SQLException {
                            stmt.setLong(1, offset);
                            stmt.setLong(2, size);
                        }
                    },
                    createCellQueryResultHandler());
            return (List<JSONObject>) ret;
        }

        private QueryResultHandler createCellQueryResultHandler() {
            return new QueryResultHandler() {
                @Override
                public Object handle(ResultSet resultSet) throws AdsException {
                    try {
                        List<JSONObject> ret = new ArrayList<JSONObject>();
                        while (resultSet.next()) {
                            ret.add(getCellJsonObject(resultSet));
                        }
                        return ret;
                    } catch (SQLException e) {
                        throw new AdsException(e);
                    }
                }

                @Override
                public Object handleQueryIsEmpty() throws AdsException {
                    return new ArrayList<JSONObject>();
                }
            };
        }

        @SuppressWarnings("unchecked")
        private JSONObject getCellJsonObject(ResultSet resultSet) throws SQLException {
            JSONObject json = new JSONObject();
            json.put("type", resultSet.getString(Sql.IDX_CELL_TYPE));
            json.put("id", resultSet.getString(Sql.NUMCOLS_CELL));
            JSONObject source = new JSONObject();
            json.put("source", source);
            source.put(CellDocHandler.KEY_CELL_ID,
                    resultSet.getString(Sql.IDX_CELL_CELL_ID));
            source.put(CellDocHandler.KEY_BOX_ID,
                    resultSet.getString(Sql.IDX_CELL_BOX_ID));
            source.put(CellDocHandler.KEY_NODE_ID,
                    resultSet.getString(Sql.IDX_CELL_NODE_ID));
            source.put(CellDocHandler.KEY_STATIC_FIELDS,
                    resultSet.getString(Sql.IDX_CELL_DECLARED_PROPS));
            source.put(CellDocHandler.KEY_DYNAMIC_FIELDS,
                    resultSet.getString(Sql.IDX_CELL_DYNAMIC_PROPS));
            source.put(CellDocHandler.KEY_HIDDEN_FIELDS,
                    resultSet.getString(Sql.IDX_CELL_HIDDEN_FIELDS));
            source.put(CellDocHandler.KEY_LINK,
                    resultSet.getString(Sql.IDX_CELL_LINKS));
            source.put(CellDocHandler.KEY_ACL_FIELDS,
                    resultSet.getString(Sql.IDX_CELL_ACL_FIELDS));
            source.put(CellDocHandler.KEY_PUBLISHED,
                    resultSet.getString(Sql.IDX_CELL_PUBLISHED));
            source.put(CellDocHandler.KEY_UPDATED,
                    resultSet.getString(Sql.IDX_CELL_UPDATED));
            return json;
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> getLinkList(final long offset, final long size) throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlLinkSelect,
                    new StatementHandler() {
                        @Override
                        public void handle(PreparedStatement stmt) throws SQLException {
                            stmt.setLong(1, offset);
                            stmt.setLong(2, size);
                            writeLog(stmt);
                        }
                    },
                    createLinkQueryResultHandler());
            return (List<JSONObject>) ret;
        }

        private QueryResultHandler createLinkQueryResultHandler() {
            return new QueryResultHandler() {
                @Override
                public Object handle(ResultSet resultSet) throws AdsException {
                    try {
                        List<JSONObject> ret = new ArrayList<JSONObject>();
                        while (resultSet.next()) {
                            ret.add(getLinkJsonObject(resultSet));
                        }
                        return ret;
                    } catch (SQLException e) {
                        throw new AdsException(e);
                    }
                }

                @Override
                public Object handleQueryIsEmpty() throws AdsException {
                    return new ArrayList<JSONObject>();
                }
            };
        }

        @SuppressWarnings("unchecked")
        private JSONObject getLinkJsonObject(ResultSet resultSet) throws SQLException {
            JSONObject json = new JSONObject();
            json.put("type", "link");
            json.put("id", resultSet.getString(Sql.NUMCOLS_LINK));
            JSONObject source = new JSONObject();
            json.put("source", source);
            // // LINK 操作 SQLの 項目数
            // static final int NUMCOLS_LINK = 10;
            // // 各項目の順序
            // static final int IDX_LINK_CELL_ID = 1;
            // static final int IDX_LINK_BOX_ID = 2;
            // static final int IDX_LINK_NODE_ID = 3;
            // static final int IDX_LINK_ENT1_TYPE = 4;
            // static final int IDX_LINK_ENT1_ID = 5;
            // static final int IDX_LINK_ENT2_TYPE = 6;
            // static final int IDX_LINK_ENT2_ID = 7;
            // static final int IDX_LINK_PUBLISHED = 8;
            // static final int IDX_LINK_UPDATED = 9;
            //
            source.put(LinkDocHandler.KEY_CELL_ID,
                    resultSet.getString(Sql.IDX_LINK_CELL_ID));
            source.put(LinkDocHandler.KEY_BOX_ID,
                    resultSet.getString(Sql.IDX_LINK_BOX_ID));
            source.put(LinkDocHandler.KEY_NODE_ID,
                    resultSet.getString(Sql.IDX_LINK_NODE_ID));
            source.put(LinkDocHandler.KEY_ENT1_TYPE,
                    resultSet.getString(Sql.IDX_LINK_ENT1_TYPE));
            source.put(LinkDocHandler.KEY_ENT1_ID,
                    resultSet.getString(Sql.IDX_LINK_ENT1_ID));
            source.put(LinkDocHandler.KEY_ENT2_TYPE,
                    resultSet.getString(Sql.IDX_LINK_ENT2_TYPE));
            source.put(LinkDocHandler.KEY_ENT2_ID,
                    resultSet.getString(Sql.IDX_LINK_ENT2_ID));
            source.put(OEntityDocHandler.KEY_PUBLISHED,
                    resultSet.getString(Sql.IDX_LINK_PUBLISHED));
            source.put(OEntityDocHandler.KEY_UPDATED,
                    resultSet.getString(Sql.IDX_LINK_UPDATED));
            return json;
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> getDavNodeList(final long offset, final long size) throws AdsException {
            Object ret = this.executeQuerySql(
                    this.sqlDavNodeSelect,
                    new StatementHandler() {
                        @Override
                        public void handle(PreparedStatement stmt) throws SQLException {
                            stmt.setLong(1, offset);
                            stmt.setLong(2, size);
                            writeLog(stmt);
                        }
                    },
                    createDavNodeQueryResultHandler()
                    );
            return (List<JSONObject>) ret;
        }

        private QueryResultHandler createDavNodeQueryResultHandler() {
            return new QueryResultHandler() {
                @Override
                public Object handle(ResultSet resultSet) throws AdsException {
                    try {
                        List<JSONObject> ret = new ArrayList<JSONObject>();
                        while (resultSet.next()) {
                            ret.add(getDavNodeJsonObject(resultSet));
                        }
                        return ret;
                    } catch (SQLException e) {
                        throw new AdsException(e);
                    }
                }

                @Override
                public Object handleQueryIsEmpty() throws AdsException {
                    return new ArrayList<JSONObject>();
                }
            };
        }

        @SuppressWarnings("unchecked")
        private JSONObject getDavNodeJsonObject(ResultSet resultSet)
                throws SQLException {
            JSONObject json = new JSONObject();
            json.put("type", "dav");
            json.put("id", resultSet.getString(Sql.NUMCOLS_DAVNODE));
            JSONObject source = new JSONObject();
            json.put("source", source);

            // // DAV_NODE 操作 SQLの 項目数
            // static final int NUMCOLS_DAVNODE = 10;
            // // 各項目の順序
            // static final int IDX_DAVNODE_CELL_ID = 1;
            // static final int IDX_DAVNODE_BOX_ID = 2;
            // static final int IDX_DAVNODE_PARENT = 3;
            // static final int IDX_DAVNODE_CHILDREN = 4;
            // static final int IDX_DAVNODE_NODE_TYPE = 5;
            // static final int IDX_DAVNODE_ACL = 6;
            // static final int IDX_DAVNODE_PROPERTIES = 7;
            // static final int IDX_DAVNODE_PUBLISHED = 8;
            // static final int IDX_DAVNODE_UPDATED = 9;
            source.put(DavNode.KEY_CELL_ID,
                    resultSet.getString(Sql.IDX_DAVNODE_CELL_ID));
            source.put(DavNode.KEY_BOX_ID,
                    resultSet.getString(Sql.IDX_DAVNODE_BOX_ID));
            source.put(DavNode.KEY_PARENT,
                    resultSet.getString(Sql.IDX_DAVNODE_PARENT));
            source.put(DavNode.KEY_CHILDREN,
                    resultSet.getString(Sql.IDX_DAVNODE_CHILDREN));
            source.put(DavNode.KEY_NODE_TYPE,
                    resultSet.getString(Sql.IDX_DAVNODE_NODE_TYPE));
            source.put(DavNode.KEY_ACL,
                    resultSet.getString(Sql.IDX_DAVNODE_ACL));
            source.put(DavNode.KEY_PROPS,
                    resultSet.getString(Sql.IDX_DAVNODE_PROPERTIES));
            source.put(DavNode.KEY_FILE,
                    resultSet.getString(Sql.IDX_DAVNODE_FILE));
            source.put(DavNode.KEY_PUBLISHED,
                    resultSet.getString(Sql.IDX_DAVNODE_PUBLISHED));
            source.put(DavNode.KEY_UPDATED,
                    resultSet.getString(Sql.IDX_DAVNODE_UPDATED));
            return json;
        }

        private String createSearchSqlQueryString(final String sql, final List<String> idList) {
            // uuidリストを使用した検索用SQLを作成する。
            StringBuilder sqlbuf = new StringBuilder(sql + "(");
            for (int i = 0; i < idList.size(); i++) {
                sqlbuf.append("?");
                if (i != idList.size() - 1) {
                    sqlbuf.append(",");
                }
            }
            sqlbuf.append(")");
            return sqlbuf.toString();
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> searchEntityList(final List<String> idList) throws AdsException {
            String sql = createSearchSqlQueryString(this.sqlEntitySearch, idList);
            Object ret = this.executeQuerySql(sql, new StatementHandlerForSearch(idList),
                    createEntityQueryResultHandler());
            return (List<JSONObject>) ret;
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> searchCellList(final List<String> idList) throws AdsException {
            String sql = createSearchSqlQueryString(this.sqlCellSearch, idList);
            Object ret = this.executeQuerySql(sql, new StatementHandlerForSearch(idList),
                    createCellQueryResultHandler());
            return (List<JSONObject>) ret;
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> searchLinkList(final List<String> idList) throws AdsException {
            String sql = createSearchSqlQueryString(this.sqlLinkSearch, idList);
            Object ret = this.executeQuerySql(sql, new StatementHandlerForSearch(idList),
                    new QueryResultHandler() {
                        @Override
                        public Object handle(ResultSet resultSet) throws AdsException {
                            try {
                                List<JSONObject> ret = new ArrayList<JSONObject>();
                                while (resultSet.next()) {
                                    ret.add(getLinkJsonObject(resultSet));
                                }
                                return ret;
                            } catch (SQLException e) {
                                throw new AdsException(e);
                            }
                        }

                        @Override
                        public Object handleQueryIsEmpty() throws AdsException {
                            return new ArrayList<JSONObject>();
                        }
                    });
            return (List<JSONObject>) ret;
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> searchDavNodeList(final List<String> idList) throws AdsException {
            String sql = createSearchSqlQueryString(this.sqlDavNodeSearch, idList);
            Object ret = this.executeQuerySql(sql, new StatementHandlerForSearch(idList),
                    createDavNodeQueryResultHandler());
            return (List<JSONObject>) ret;
        }

        Connection getConnection() throws AdsException {
            try {
                // 接続する。
                Connection con = ds.getConnection();
                // DBCP経由だと以下のようなデータベースの切り替えは うまくいかない模様。
                // con.setSchema(index);
                // なのでDBの選択は個別のSQLに埋め込む設計としている。
                return con;
            } catch (SQLException e) {
                PersoniumCoreLog.Server.RDB_CONNECT_FAIL.params(e.getMessage()).reason(e).writeLog();
                throw new AdsException(e);
            }
        }

        Object executeQuerySql(String sql, StatementHandler sp,
                QueryResultHandler queryResultHandler) throws AdsException {
            log.debug(sql);
            Connection con = this.getConnection();
            PreparedStatement stmt = null;
            try {
                stmt = con.prepareStatement(sql);
                sp.handle(stmt);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    return queryResultHandler.handleQueryIsEmpty();
                }
                rs.beforeFirst();
                return queryResultHandler.handle(rs);
            } catch (SQLException e) {
                PersoniumCoreLog.Server.EXECUTE_QUERY_SQL_FAIL.params(e.getMessage()).reason(e).writeLog();
                throw new AdsException(e);
            } finally {
                try {
                    stmt.close();
                    con.close();
                } catch (SQLException e) {
                    PersoniumCoreLog.Server.RDB_DISCONNECT_FAIL.params(e.getMessage()).reason(e).writeLog();
                    throw new AdsException(e);
                }
            }
        }

        void executeDeleteByQuerySql(String deleteSql,
                String countSql,
                StatementHandler sp) throws AdsException {
            Connection con = this.getConnection();
            PreparedStatement stmt = null;
            try {
                stmt = con.prepareStatement(deleteSql);
                sp.handle(stmt);
                PersoniumCoreLog.Server.JDBC_EXEC_SQL.params(
                        ((DelegatingPreparedStatement) stmt).getDelegate().toString()).writeLog();
                stmt.executeUpdate();
                if (!AUTO_COMMIT) {
                    con.commit();
                }
            } catch (SQLException e) {
                throw new AdsException(e);
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    PersoniumCoreLog.Server.RDB_DISCONNECT_FAIL.params(e.getMessage()).reason(e).writeLog();
                    throw new AdsException(e);
                }
            }

            try {
                // 件数の確認
                stmt = con.prepareStatement(countSql);
                sp.handle(stmt);
                ResultSet resultSet = stmt.executeQuery(); // NOPMD - PMDのバグのためチェックから除外
                long count = 0;
                if (resultSet.next()) {
                    count = resultSet.getLong(1);
                } else {
                    throw new AdsException("No row was returened while 1 row is expected to be returned.");
                }
                if (count != 0) {
                    throw new AdsException(String.format(
                            "[%d] rows have been affected while 0 row is expected to be affected.", count));
                }
            } catch (SQLException e) {
                throw new AdsException(e);
            } finally {
                try {
                    stmt.close();
                    con.close();
                } catch (SQLException e) {
                    PersoniumCoreLog.Server.RDB_DISCONNECT_FAIL.params(e.getMessage()).reason(e).writeLog();
                    throw new AdsException(e);
                }
            }
        }

        void executeUpdateSql(String sql, StatementHandler sp) throws AdsException {
            executeUpdateSql(sql, sp, 1);
        }

        void executeUpdateSql(String sql, StatementHandler sp, int expectedCount) throws AdsException {
            Connection con = this.getConnection();
            PreparedStatement stmt = null;
            try {
                stmt = con.prepareStatement(sql);
                sp.handle(stmt);
                int count = stmt.executeUpdate();
                if (!AUTO_COMMIT) {
                    con.commit();
                }
                if (count != expectedCount) {
                    throw new AdsException("["
                            + count + "] rows have been affected while " + expectedCount
                            + " row is expected to be affected.");
                }
            } catch (SQLException e) {
                PersoniumCoreLog.Server.JDBC_EXEC_SQL.params(
                        ((DelegatingPreparedStatement) stmt).getDelegate().toString()).writeLog();
                throw new AdsException(e);
            } finally {
                try {
                    stmt.close();
                    con.close();
                } catch (SQLException e) {
                    PersoniumCoreLog.Server.RDB_DISCONNECT_FAIL.params(e.getMessage()).reason(e).writeLog();
                    throw new AdsException(e);
                }
            }
        }

        /**
         * SQLのPreparedStatementを受け取ってプレースホルダに値を埋め込むHandler.
         */
        abstract static class StatementHandler {
            abstract void handle(PreparedStatement stmt) throws SQLException;

            void writeLog(PreparedStatement stmt) {
                PersoniumCoreLog.Server.JDBC_EXEC_SQL.params(
                        ((DelegatingPreparedStatement) stmt).getDelegate().toString()).writeLog();
            }
        }

        /**
         * Entityを扱うためのStatementHandler.
         */
        static class StatementHandlerForEntity extends StatementHandler {
            EntitySetDocHandler oedh;

            StatementHandlerForEntity(EntitySetDocHandler oedh) {
                this.oedh = oedh;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                stmt.setString(Sql.IDX_ENTITY_TYPE, oedh.getType());
                stmt.setString(Sql.IDX_ENTITY_CELL_ID, oedh.getCellId());
                stmt.setString(Sql.IDX_ENTITY_BOX_ID, oedh.getBoxId());
                stmt.setString(Sql.IDX_ENTITY_NODE_ID, oedh.getNodeId());
                stmt.setString(Sql.IDX_ENTITY_ENTITY_ID, oedh.getEntityTypeId());
                stmt.setString(Sql.IDX_ENTITY_DECLARED_PROPS, this.oedh.getStaticFieldsString());
                stmt.setString(Sql.IDX_ENTITY_DYNAMIC_PROPS, this.oedh.getDynamicFieldsString());
                stmt.setString(Sql.IDX_ENTITY_HIDDEN_FIELDS, this.oedh.getHiddenFieldsString());
                stmt.setString(Sql.IDX_ENTITY_LINKS, this.oedh.getManyToOnelinkIdString());
                stmt.setLong(Sql.IDX_ENTITY_PUBLISHED, oedh.getPublished());
                stmt.setLong(Sql.IDX_ENTITY_UPDATED, oedh.getUpdated());
                stmt.setString(Sql.NUMCOLS_ENTITY, oedh.getId());
                if (UserDataODataProducer.USER_ODATA_NAMESPACE.equals(oedh.getType())) {
                    PersoniumCoreLog.Server.JDBC_USER_ODATA_SQL.params(oedh.getUnitUserName(), "ENTITY", oedh.getId(),
                            oedh.getType(), oedh.getCellId(), oedh.getBoxId(), oedh.getNodeId(),
                            oedh.getEntityTypeId()).writeLog();
                } else {
                    writeLog(stmt);
                }
            }
        }

        /**
         * BulkEntityを扱うためのStatementHandler.
         */
        static class StatementHandlerForBulkEntity extends StatementHandler {
            List<EntitySetDocHandler> bulkRequestList;

            StatementHandlerForBulkEntity(List<EntitySetDocHandler> bulkRequestList) {
                this.bulkRequestList = bulkRequestList;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                int index = 1;
                for (EntitySetDocHandler docHandler : bulkRequestList) {
                    stmt.setString(index++, docHandler.getType());
                    stmt.setString(index++, docHandler.getCellId());
                    stmt.setString(index++, docHandler.getBoxId());
                    stmt.setString(index++, docHandler.getNodeId());
                    stmt.setString(index++, docHandler.getEntityTypeId());
                    stmt.setString(index++, docHandler.getStaticFieldsString());
                    stmt.setString(index++, docHandler.getDynamicFieldsString());
                    stmt.setString(index++, docHandler.getHiddenFieldsString());
                    stmt.setString(index++, docHandler.getManyToOnelinkIdString());
                    stmt.setLong(index++, docHandler.getPublished());
                    stmt.setLong(index++, docHandler.getUpdated());
                    stmt.setString(index++, docHandler.getId());
                }
            }
        }

        /**
         * BulkLinkを扱うためのStatementHandler.
         */
        static class StatementHandlerForBulkLink extends StatementHandler {
            List<LinkDocHandler> bulkRequestList;

            StatementHandlerForBulkLink(List<LinkDocHandler> bulkRequestList) {
                this.bulkRequestList = bulkRequestList;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                int index = 1;
                for (LinkDocHandler docHandler : bulkRequestList) {
                    stmt.setString(index++, docHandler.getCellId());
                    stmt.setString(index++, docHandler.getBoxId());
                    stmt.setString(index++, docHandler.getNodeId());
                    stmt.setString(index++, docHandler.getEnt1Type());
                    stmt.setString(index++, docHandler.getEnt1Key());
                    stmt.setString(index++, docHandler.getEnt2Type());
                    stmt.setString(index++, docHandler.getEnt2Key());
                    stmt.setLong(index++, docHandler.getPublished());
                    stmt.setLong(index++, docHandler.getUpdated());
                    stmt.setString(index++, docHandler.getId());
                }
            }
        }

        /**
         * CellEntityを扱うためのStatementHandler.
         */
        static class StatementHandlerForCell extends StatementHandler {
            EntitySetDocHandler docHandler;

            StatementHandlerForCell(EntitySetDocHandler docHandler) {
                this.docHandler = docHandler;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                stmt.setString(Sql.IDX_CELL_TYPE, docHandler.getType());
                stmt.setString(Sql.IDX_CELL_CELL_ID, docHandler.getCellId());
                stmt.setString(Sql.IDX_CELL_BOX_ID, docHandler.getBoxId());
                stmt.setString(Sql.IDX_CELL_NODE_ID, docHandler.getNodeId());
                stmt.setString(Sql.IDX_CELL_DECLARED_PROPS, docHandler.getStaticFieldsString());
                stmt.setString(Sql.IDX_CELL_DYNAMIC_PROPS, docHandler.getDynamicFieldsString());
                stmt.setString(Sql.IDX_CELL_HIDDEN_FIELDS, docHandler.getHiddenFieldsString());
                stmt.setString(Sql.IDX_CELL_LINKS, docHandler.getManyToOnelinkIdString());
                stmt.setString(Sql.IDX_CELL_ACL_FIELDS, JSONObject.toJSONString(docHandler.getAclFields()));
                stmt.setLong(Sql.IDX_CELL_PUBLISHED, docHandler.getPublished());
                stmt.setLong(Sql.IDX_CELL_UPDATED, docHandler.getUpdated());
                stmt.setString(Sql.NUMCOLS_CELL, docHandler.getId());
                writeLog(stmt);
            }
        }

        /**
         * Linkを扱うためのStatementHandler.
         */
        static class StatementHandlerForLink extends StatementHandler {
            LinkDocHandler ldh;

            StatementHandlerForLink(LinkDocHandler ldh) {
                this.ldh = ldh;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                stmt.setString(Sql.IDX_LINK_CELL_ID, ldh.getCellId());
                stmt.setString(Sql.IDX_LINK_BOX_ID, ldh.getBoxId());
                stmt.setString(Sql.IDX_LINK_NODE_ID, ldh.getNodeId());
                stmt.setString(Sql.IDX_LINK_ENT1_TYPE, ldh.getEnt1Type());
                stmt.setString(Sql.IDX_LINK_ENT1_ID, ldh.getEnt1Key());
                stmt.setString(Sql.IDX_LINK_ENT2_TYPE, ldh.getEnt2Type());
                stmt.setString(Sql.IDX_LINK_ENT2_ID, ldh.getEnt2Key());
                stmt.setLong(Sql.IDX_LINK_PUBLISHED, ldh.getPublished());
                stmt.setLong(Sql.IDX_LINK_UPDATED, ldh.getUpdated());
                stmt.setString(Sql.NUMCOLS_LINK, ldh.getId());
                writeLog(stmt);
            }
        }

        /**
         * DavNodeを扱うためのStatementHandler.
         */
        static class StatementHandlerForDavNode extends StatementHandler {
            DavNode davNode;

            StatementHandlerForDavNode(DavNode davNode) {
                this.davNode = davNode;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                stmt.setString(Sql.IDX_DAVNODE_CELL_ID, davNode.getCellId());
                stmt.setString(Sql.IDX_DAVNODE_BOX_ID, davNode.getBoxId());
                stmt.setString(Sql.IDX_DAVNODE_PARENT, davNode.getParentId());
                stmt.setString(Sql.IDX_DAVNODE_CHILDREN, JSONObject.toJSONString(davNode.getChildren()));
                stmt.setString(Sql.IDX_DAVNODE_NODE_TYPE, davNode.getNodeType());
                stmt.setString(Sql.IDX_DAVNODE_ACL, JSONObject.toJSONString(davNode.getAcl()));
                stmt.setString(Sql.IDX_DAVNODE_PROPERTIES, JSONObject.toJSONString(davNode.getProperties()));
                String file = null;
                if (davNode.getFile() != null) {
                    file = JSONObject.toJSONString(davNode.getFile());
                }
                stmt.setString(Sql.IDX_DAVNODE_FILE, file);
                stmt.setLong(Sql.IDX_DAVNODE_PUBLISHED, davNode.getPublished());
                stmt.setLong(Sql.IDX_DAVNODE_UPDATED, davNode.getUpdated());
                stmt.setString(Sql.NUMCOLS_DAVNODE, davNode.getId());
                writeLog(stmt);
            }
        }

        /**
         * BulkDavを扱うためのStatementHandler.
         */
        static class StatementHandlerForBulkDav extends StatementHandler {
            List<DavNode> bulkRequestList;

            StatementHandlerForBulkDav(List<DavNode> bulkRequestList) {
                this.bulkRequestList = bulkRequestList;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                int index = 1;
                for (DavNode docHandler : bulkRequestList) {
                    stmt.setString(index++, docHandler.getCellId());
                    stmt.setString(index++, docHandler.getBoxId());
                    stmt.setString(index++, docHandler.getParentId());
                    stmt.setString(index++, JSONObject.toJSONString(docHandler.getChildren()));
                    stmt.setString(index++, docHandler.getNodeType());
                    stmt.setString(index++, JSONObject.toJSONString(docHandler.getAcl()));
                    stmt.setString(index++, JSONObject.toJSONString(docHandler.getProperties()));
                    String file = null;
                    if (docHandler.getFile() != null) {
                        file = JSONObject.toJSONString(docHandler.getFile());
                    }
                    stmt.setString(index++, file);
                    stmt.setLong(index++, docHandler.getPublished());
                    stmt.setLong(index++, docHandler.getUpdated());
                    stmt.setString(index++, docHandler.getId());
                }
            }
        }

        /**
         * セル削除管理を扱うためのStatementHandler.
         */
        static class StatementHandlerForCellDelete extends StatementHandler {
            String dbName;
            String tableName;
            String cellId;

            StatementHandlerForCellDelete(String dbName, String tableName, String cellId) {
                this.dbName = dbName;
                this.tableName = tableName;
                this.cellId = cellId;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                stmt.setString(Sql.IDX_CELL_DELETE_DB_NAME, dbName);
                stmt.setString(Sql.IDX_CELL_DELETE_TABLE_NAME, tableName);
                stmt.setString(Sql.IDX_CELL_DELETE_CELL_ID, cellId);
                writeLog(stmt);
            }
        }

        /**
         * Entityテーブルを検索するためのStatementHandler.
         */
        static class StatementHandlerForSearch extends StatementHandler {
            List<String> idList;

            StatementHandlerForSearch(List<String> idList) {
                this.idList = idList;
            }

            @Override
            public void handle(PreparedStatement stmt) throws SQLException {
                int index = 1;
                for (String uuid : idList) {
                    stmt.setString(index++, uuid);
                }
            }
        }

        /**
         * PreStatementに.
         */
        interface QueryResultHandler {
            Object handleQueryIsEmpty() throws AdsException;

            Object handle(ResultSet resultSet) throws AdsException;
        }
    }

    /**
     * 使用するSQLのテンプレートをStaticで保持しておくクラス.
     */
    static class Sql {
        static String createSchema =
                PersoniumCoreUtils.readStringResource("es/ads/ddl-createschema.sql", CharEncoding.UTF_8);
        static String dropSchema =
                PersoniumCoreUtils.readStringResource("es/ads/ddl-dropschema.sql", CharEncoding.UTF_8);
        static String createTableEntity =
                PersoniumCoreUtils.readStringResource("es/ads/ddl-createtable-entity.sql", CharEncoding.UTF_8);
        static String createTableLink =
                PersoniumCoreUtils.readStringResource("es/ads/ddl-createtable-link.sql", CharEncoding.UTF_8);
        static String createTableDavNode =
                PersoniumCoreUtils.readStringResource("es/ads/ddl-createtable-davnode.sql", CharEncoding.UTF_8);
        static String createTableCell =
                PersoniumCoreUtils.readStringResource("es/ads/ddl-createtable-cell.sql", CharEncoding.UTF_8);
        static String createManagementTableCellDelete =
                PersoniumCoreUtils.readStringResource("es/ads/ddl-create-management-table-cell-delete.sql",
                        CharEncoding.UTF_8);

        static String insertEntity =
                PersoniumCoreUtils.readStringResource("es/ads/entity-insert.sql", CharEncoding.UTF_8);
        static String updateEntity =
                PersoniumCoreUtils.readStringResource("es/ads/entity-update.sql", CharEncoding.UTF_8);
        static String deleteEntity =
                PersoniumCoreUtils.readStringResource("es/ads/entity-delete.sql", CharEncoding.UTF_8);
        static String bulkInsertEntity =
                PersoniumCoreUtils.readStringResource("es/ads/entity-bulk-insert.sql", CharEncoding.UTF_8);

        static String insertCell =
                PersoniumCoreUtils.readStringResource("es/ads/cell-insert.sql", CharEncoding.UTF_8);
        static String updateCell =
                PersoniumCoreUtils.readStringResource("es/ads/cell-update.sql", CharEncoding.UTF_8);
        static String deleteCell =
                PersoniumCoreUtils.readStringResource("es/ads/cell-delete.sql", CharEncoding.UTF_8);

        static String insertLink =
                PersoniumCoreUtils.readStringResource("es/ads/link-insert.sql", CharEncoding.UTF_8);
        static String updateLink =
                PersoniumCoreUtils.readStringResource("es/ads/link-update.sql", CharEncoding.UTF_8);
        static String deleteLink =
                PersoniumCoreUtils.readStringResource("es/ads/link-delete.sql", CharEncoding.UTF_8);
        static String bulkInsertLink =
                PersoniumCoreUtils.readStringResource("es/ads/link-bulk-insert.sql", CharEncoding.UTF_8);

        static String insertDavNode =
                PersoniumCoreUtils.readStringResource("es/ads/dav-insert.sql", CharEncoding.UTF_8);
        static String updateDavNode =
                PersoniumCoreUtils.readStringResource("es/ads/dav-update.sql", CharEncoding.UTF_8);
        static String deleteDavNode =
                PersoniumCoreUtils.readStringResource("es/ads/dav-delete.sql", CharEncoding.UTF_8);
        static String bulkInsertDav =
                PersoniumCoreUtils.readStringResource("es/ads/dav-bulk-insert.sql", CharEncoding.UTF_8);

        static String deleteCellResourceFromEntity =
                PersoniumCoreUtils.readStringResource("es/ads/delete-cellresource-from-entity.sql", CharEncoding.UTF_8);
        static String deleteCellResourceFromDavNode =
                PersoniumCoreUtils.readStringResource("es/ads/delete-cellresource-from-davnode.sql",
                        CharEncoding.UTF_8);
        static String deleteCellResourceFromLink =
                PersoniumCoreUtils.readStringResource("es/ads/delete-cellresource-from-link.sql", CharEncoding.UTF_8);

        static String countCellResourceFromEntity =
                PersoniumCoreUtils.readStringResource("es/ads/count-cellresource-from-entity.sql", CharEncoding.UTF_8);
        static String countCellResourceFromDavNode =
                PersoniumCoreUtils.readStringResource("es/ads/count-cellresource-from-davnode.sql", CharEncoding.UTF_8);
        static String countCellResourceFromLink =
                PersoniumCoreUtils.readStringResource("es/ads/count-cellresource-from-link.sql", CharEncoding.UTF_8);

        static String selectEntity =
                PersoniumCoreUtils.readStringResource("es/ads/entity-select.sql", CharEncoding.UTF_8);
        static String countEntity =
                PersoniumCoreUtils.readStringResource("es/ads/entity-count.sql", CharEncoding.UTF_8);
        static String selectCell =
                PersoniumCoreUtils.readStringResource("es/ads/cell-select.sql", CharEncoding.UTF_8);
        static String countCell =
                PersoniumCoreUtils.readStringResource("es/ads/cell-count.sql", CharEncoding.UTF_8);
        static String selectLink =
                PersoniumCoreUtils.readStringResource("es/ads/link-select.sql", CharEncoding.UTF_8);
        static String countLink =
                PersoniumCoreUtils.readStringResource("es/ads/link-count.sql", CharEncoding.UTF_8);
        static String selectDav =
                PersoniumCoreUtils.readStringResource("es/ads/dav-select.sql", CharEncoding.UTF_8);
        static String countDav =
                PersoniumCoreUtils.readStringResource("es/ads/dav-count.sql", CharEncoding.UTF_8);

        static String searchEntity =
                PersoniumCoreUtils.readStringResource("es/ads/entity-search.sql", CharEncoding.UTF_8);
        static String searchCell =
                PersoniumCoreUtils.readStringResource("es/ads/cell-search.sql", CharEncoding.UTF_8);
        static String searchLink =
                PersoniumCoreUtils.readStringResource("es/ads/link-search.sql", CharEncoding.UTF_8);
        static String searchDav =
                PersoniumCoreUtils.readStringResource("es/ads/dav-search.sql", CharEncoding.UTF_8);

        // Cell管理用SQL群
        static String insertCellDelete =
                PersoniumCoreUtils.readStringResource("es/ads/celldelete-insert.sql", CharEncoding.UTF_8);

        // ENTITY 操作 SQLの 項目数
        static final int NUMCOLS_ENTITY = 12;
        // 各項目の順序
        static final int IDX_ENTITY_TYPE = 1;
        static final int IDX_ENTITY_CELL_ID = 2;
        static final int IDX_ENTITY_BOX_ID = 3;
        static final int IDX_ENTITY_NODE_ID = 4;
        static final int IDX_ENTITY_ENTITY_ID = 5;
        static final int IDX_ENTITY_DECLARED_PROPS = 6;
        static final int IDX_ENTITY_DYNAMIC_PROPS = 7;
        static final int IDX_ENTITY_HIDDEN_FIELDS = 8;
        static final int IDX_ENTITY_LINKS = 9;
        static final int IDX_ENTITY_PUBLISHED = 10;
        static final int IDX_ENTITY_UPDATED = 11;

        // LINK 操作 SQLの 項目数
        static final int NUMCOLS_LINK = 10;
        // 各項目の順序
        static final int IDX_LINK_CELL_ID = 1;
        static final int IDX_LINK_BOX_ID = 2;
        static final int IDX_LINK_NODE_ID = 3;
        static final int IDX_LINK_ENT1_TYPE = 4;
        static final int IDX_LINK_ENT1_ID = 5;
        static final int IDX_LINK_ENT2_TYPE = 6;
        static final int IDX_LINK_ENT2_ID = 7;
        static final int IDX_LINK_PUBLISHED = 8;
        static final int IDX_LINK_UPDATED = 9;

        // DAV_NODE 操作 SQLの 項目数
        static final int NUMCOLS_DAVNODE = 11;
        // 各項目の順序
        static final int IDX_DAVNODE_CELL_ID = 1;
        static final int IDX_DAVNODE_BOX_ID = 2;
        static final int IDX_DAVNODE_PARENT = 3;
        static final int IDX_DAVNODE_CHILDREN = 4;
        static final int IDX_DAVNODE_NODE_TYPE = 5;
        static final int IDX_DAVNODE_ACL = 6;
        static final int IDX_DAVNODE_PROPERTIES = 7;
        static final int IDX_DAVNODE_FILE = 8;
        static final int IDX_DAVNODE_PUBLISHED = 9;
        static final int IDX_DAVNODE_UPDATED = 10;

        // ENTITY 操作 SQLの 項目数
        static final int NUMCOLS_CELL = 12;
        // 各項目の順序
        static final int IDX_CELL_TYPE = 1;
        static final int IDX_CELL_CELL_ID = 2;
        static final int IDX_CELL_BOX_ID = 3;
        static final int IDX_CELL_NODE_ID = 4;
        static final int IDX_CELL_DECLARED_PROPS = 5;
        static final int IDX_CELL_DYNAMIC_PROPS = 6;
        static final int IDX_CELL_HIDDEN_FIELDS = 7;
        static final int IDX_CELL_LINKS = 8;
        static final int IDX_CELL_ACL_FIELDS = 9;
        static final int IDX_CELL_PUBLISHED = 10;
        static final int IDX_CELL_UPDATED = 11;

        // CELL_DELETE 操作 SQLの 項目
        static final int IDX_CELL_DELETE_DB_NAME = 1;
        static final int IDX_CELL_DELETE_TABLE_NAME = 2;
        static final int IDX_CELL_DELETE_CELL_ID = 3;
    }

}
