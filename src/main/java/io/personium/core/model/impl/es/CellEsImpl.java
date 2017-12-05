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
package io.personium.core.model.impl.es;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.OptionsQueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.IExtRoleContainingToken;
import io.personium.common.auth.token.Role;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchHits;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.AuthUtils;
import io.personium.core.event.EventBus;
import io.personium.core.event.EventUtils;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.CellSnapshotCellCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.file.BinaryDataAccessException;
import io.personium.core.model.file.BinaryDataAccessor;
import io.personium.core.model.impl.es.accessor.CellAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.BoxCache;
import io.personium.core.model.impl.es.cache.CellCache;
import io.personium.core.model.impl.es.doc.CellDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.fs.DavCmpFsImpl;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.utils.UriUtils;
import net.spy.memcached.internal.CheckedOperationTimeoutException;

/**
 * Cell object implemented using ElasticSearch.
 */
public final class CellEsImpl implements Cell {
    private String id;
    private String name;
    private String url;
    private String owner;
    private Long published;
    private Map<String, Object> json;

    /**
     * Esの検索結果出力上限.
     */
    private static final int TOP_NUM = PersoniumUnitConfig.getEsTopNum();

    /**
     * logger.
     */
    static Logger log = LoggerFactory.getLogger(CellEsImpl.class);

    /**
     * constructor.
     */
    public CellEsImpl() {

    }

    @Override
    public EventBus getEventBus() {
        return new EventBus(this);
    }

    @Override
    public boolean isEmpty() {
        CellCtlODataProducer producer = new CellCtlODataProducer(this);
        // check no box exists.
        QueryInfo queryInfo = new QueryInfo(InlineCount.ALLPAGES, null, null, null, null, null, null, null, null);
        if (producer.getEntitiesCount(Box.EDM_TYPE_NAME, queryInfo).getCount() > 0) {
            return false;
        }

        // check that Main Box is empty
        Box defaultBox = this.getBoxForName(Box.DEFAULT_BOX_NAME);
        BoxCmp defaultBoxCmp = ModelFactory.boxCmp(defaultBox);
        if (!defaultBoxCmp.isEmpty()) {
            return false;
        }

        // check that no Cell Control Object exists
        // TODO 性能を向上させるため、Type横断でc:（セルのuuid）の値を検索して、チェックするように変更する
        if (producer.getEntitiesCount(Account.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(Role.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(ExtCell.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(ExtRole.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(Relation.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(SentMessage.EDM_TYPE_NAME, queryInfo).getCount() > 0
                || producer.getEntitiesCount(ReceivedMessage.EDM_TYPE_NAME, queryInfo).getCount() > 0) {
            return false;
        }
        // TODO check EventLog
        return true;
    }

    @Override
    public Box getBoxForName(String boxName) {
        if (Box.DEFAULT_BOX_NAME.equals(boxName)) {
            return new Box(this, null);
        }

        // URlに指定されたBox名のフォーマットチェックをする。不正の場合Boxが存在しないためnullを返却する
        if (!validatePropertyRegEx(boxName, Common.PATTERN_NAME)) {
            return null;
        }
        // キャッシュされたBoxの取得を試みる。
        Box cachedBox = BoxCache.get(boxName, this);
        if (cachedBox != null) {
            return cachedBox;
        }

        Box loadedBox = null;
        try {
            ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
            EntityResponse er = op.getEntity(Box.EDM_TYPE_NAME, OEntityKey.create(boxName), null);
            loadedBox = new Box(this, er.getEntity());
            BoxCache.cache(loadedBox);
            return loadedBox;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CheckedOperationTimeoutException) {
                return loadedBox;
            } else {
                return null;
            }
        }
    }

    @Override
    public Box getBoxForSchema(String boxSchema) {
        // スキーマ名一覧の取得（別名を含む）
        List<String> boxSchemas = UriUtils.getUrlVariations(this.getUnitUrl(), boxSchema);

        ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
        for (int i = 0; i < boxSchemas.size(); i++) {
            BoolCommonExpression filter = OptionsQueryParser.parseFilter("Schema eq '" + boxSchemas.get(i) + "'");
            QueryInfo qi = QueryInfo.newBuilder().setFilter(filter).build();
            try {
                EntitiesResponse er = op.getEntities(Box.EDM_TYPE_NAME, qi);
                List<OEntity> entList = er.getEntities();
                if (entList.size() == 1) {
                    return new Box(this, entList.get(0));
                }
                continue;
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Load cell info.
     * @param uriInfo UriInfo
     * @return CellObject. If Cell does not exist, it returns null.
     */
    public static Cell load(UriInfo uriInfo) {
        URI reqUri = uriInfo.getRequestUri();
        URI baseUri = uriInfo.getBaseUri();

        String rPath = reqUri.getPath();
        String bPath = baseUri.getPath();
        rPath = rPath.substring(bPath.length());
        String[] paths = StringUtils.split(rPath, "/");

        CellEsImpl cell = (CellEsImpl) findCell("s.Name.untouched", paths[0]);
        if (cell != null) {
            cell.url = getBaseUri(uriInfo, cell.name);
            cell.owner = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), cell.owner);
        }
        return cell;
    }

    /**
     * Load cell info.
     * @param id cell id
     * @param uriInfo UriInfo
     * @return CellObject. If Cell does not exist, it returns null.
     */
    public static Cell load(String id, UriInfo uriInfo) {
        log.debug(id);
        EntitySetAccessor esCells = EsModel.cell();
        PersoniumGetResponse resp = esCells.get(id);
        if (resp.exists()) {
            CellEsImpl ret = new CellEsImpl();
            ret.setJson(resp.getSource());
            ret.id = resp.getId();
            if (uriInfo != null) {
                ret.url = getBaseUri(uriInfo, ret.name);
                ret.owner = UriUtils.convertSchemeFromLocalUnitToHttp(ret.getUnitUrl(), ret.owner);
            } else {
                ret.url = "/" + ret.name + "/";
            }
            return ret;
        } else {
            return null;
        }
    }

    /**
     * Load cell from the specified cell name.
     * However, the parameter "url" of Cell is not set.
     * @param cellName target cell name
     * @return cell
     */
    public static Cell load(String cellName) {
        return findCell("s.Name.untouched", cellName);
    }

    private static String getBaseUri(final UriInfo uriInfo, String cellName) {
        // URLを生成してSet
        StringBuilder urlSb = new StringBuilder();
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.scheme(PersoniumUnitConfig.getUnitScheme());
        urlSb.append(uriBuilder.build().toASCIIString());
        urlSb.append(cellName);
        urlSb.append("/");
        return urlSb.toString();
    }

    /**
     * ID 又はCell名Cellを検索しCellオブジェクトを返却する.
     * @param queryKey
     *            Cellを検索する際のキー(Cell名)
     * @param queryValue
     *            Cellを検索する際のキーに対する値
     * @return Cell オブジェクト 該当するCellが存在しないとき、又はqueryKeyの値が無効な場合はnull
     */
    private static Cell findCell(String queryKey, String queryValue) {
        if (!queryKey.equals("_id") && !queryKey.equals("s.Name.untouched")) {
            return null;
        }
        // URlに指定されたCell名のフォーマットチェックをする。不正の場合はCellが存在しないためnullを返却する
        if (!validatePropertyRegEx(queryValue, Common.PATTERN_NAME)) {
            return null;
        }

        EntitySetAccessor ecCells = EsModel.cell();
        CellEsImpl ret = new CellEsImpl();

        Map<String, Object> cache = CellCache.get(queryValue);
        if (cache == null) {
            Map<String, Object> source = new HashMap<String, Object>();
            Map<String, Object> filter = new HashMap<String, Object>();
            Map<String, Object> term = new HashMap<String, Object>();

            term.put(queryKey, queryValue);
            filter.put("term", term);
            source.put("query", QueryMapFactory.filteredQuery(null, filter));

            PersoniumSearchResponse resp = ecCells.search(source);
            if (resp == null || resp.getHits().getCount() == 0) {
                return null;
            }
            PersoniumSearchHit hit = resp.getHits().getAt(0);
            ret.setJson(hit.getSource());
            ret.id = hit.getId();

            cache = hit.getSource();
            cache.put("_id", hit.getId());
            try {
                CellCache.cache(queryValue, cache);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof CheckedOperationTimeoutException) {
                    // memcachedへの接続でタイムアウトした場合はログだけ出力し、続行する
                    log.info("Faild to cache Cell info.");
                } else {
                    // その他のエラーの場合、サーバエラーとする
                    throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
                }
            }
        } else {
            ret.setJson(cache);
            ret.id = (String) cache.get("_id");
        }
        return ret;
    }

    /**
     * Mapからオブジェクトのメンバを設定する.
     * @param json
     *            実はMap
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setJson(Map json) {
        this.json = json;
        if (this.json == null) {
            return;
        }
        Map<String, String> urlJson = (Map<String, String>) json.get("s");
        Map<String, String> hJson = (Map<String, String>) json.get("h");
        this.published = (Long) json.get("p");
        this.name = urlJson.get("Name");
        // TODO At this timing owner's localunit/http convert should be done.
        // It is necessary to modify the source code so that UnitUrl can be read at this timing.
        this.owner = hJson.get("Owner");
    }

    @Override
    public OEntityWrapper getAccount(final String username) {
        ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
        OEntityKey key = OEntityKey.create(username);
        OEntityWrapper oew = null;
        try {
            EntityResponse resp = op.getEntity("Account", key, null);
            oew = (OEntityWrapper) resp.getEntity();
        } catch (PersoniumCoreException dce) {
            log.debug(dce.getMessage());
        }
        return oew;
    }

    @Override
    public boolean authenticateAccount(final OEntityWrapper oew, final String password) {
        // TODO 時間をはかる攻撃（名前忘れた） に対処するため、IDがみつからなくても、無駄に処理はする。
        String cred = null;
        if (oew != null) {
            cred = (String) oew.get("HashedCredential");
        }
        String hCred = AuthUtils.hashPassword(password);
        if (hCred.equals(cred)) {
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getRoleListForAccount(final String username) {
        // Accountを取得
        EntitySetAccessor accountType = EsModel.cellCtl(this, Account.EDM_TYPE_NAME);

        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();
        filters.add(QueryMapFactory.termQuery("s.Name.untouched", username));

        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        queries.add(QueryMapFactory.termQuery("c", this.getId()));

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

        Map<String, Object> source = new HashMap<String, Object>();
        source.put("filter", QueryMapFactory.andFilter(filters));
        source.put("query", query);

        PersoniumSearchHits hits = accountType.search(source).getHits();

        if (hits.getCount() == 0) {
            return null;
        }

        PersoniumSearchHit hit = hits.getHits()[0];

        List<Role> ret = new ArrayList<Role>();
        ODataLinkAccessor links = EsModel.cellCtlLink(this);

        // アカウントに結びつくロールの検索
        List<Map<String, Object>> searchRoleQueries = new ArrayList<Map<String, Object>>();
        searchRoleQueries.add(QueryMapFactory.termQuery("t1", "Account"));
        searchRoleQueries.add(QueryMapFactory.termQuery("t2", "Role"));

        List<Map<String, Object>> searchRoleFilters = new ArrayList<Map<String, Object>>();
        searchRoleFilters.add(QueryMapFactory.termQuery("k1", hit.getId()));
        Map<String, Object> and = new HashMap<String, Object>();
        and.put("filters", searchRoleFilters);
        Map<String, Object> searchRoleFilter = new HashMap<String, Object>();
        searchRoleFilter.put("and", and);

        Map<String, Object> searchRoleSource = new HashMap<String, Object>();
        searchRoleSource.put("filter", searchRoleFilter);
        searchRoleSource.put("query",
                QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(searchRoleQueries)));

        // 検索結果件数設定
        searchRoleSource.put("size", TOP_NUM);

        PersoniumSearchResponse res = links.search(searchRoleSource);
        if (res == null) {
            return ret;
        }
        PersoniumSearchHit[] hits2 = res.getHits().getHits();
        for (PersoniumSearchHit hit2 : hits2) {
            Map<String, Object> row = hit2.getSource();
            String role = (String) row.get("k2");
            log.debug(this.id);
            EntitySetAccessor roleDao = EsModel.cellCtl(this, Role.EDM_TYPE_NAME);
            PersoniumGetResponse gRes = roleDao.get(role);
            if (gRes == null) {
                continue;
            }
            Map<String, Object> src = gRes.getSource();
            Map<String, Object> s = (Map<String, Object>) src.get("s");
            Map<String, Object> l = (Map<String, Object>) src.get("l");
            String roleName = (String) s.get(KEY_NAME);
            String boxId = (String) l.get(Box.EDM_TYPE_NAME);
            String boxName = null;
            String schema = null;
            if (boxId != null) {
                // Boxの検索
                EntitySetAccessor box = EsModel.box(this);
                PersoniumGetResponse getRes = box.get(boxId);
                if (getRes == null || !getRes.isExists()) {
                    continue;
                }
                Map<String, Object> boxsrc = getRes.getSource();
                Map<String, Object> boxs = (Map<String, Object>) boxsrc.get("s");
                boxName = (String) boxs.get(KEY_NAME);
                schema = (String) boxs.get(KEY_SCHEMA);
            }
            Role roleObj = new Role(roleName, boxName, schema);

            ret.add(roleObj);
        }
        return ret;
    }

    @Override
    public List<Role> getRoleListHere(final IExtRoleContainingToken token) {
        List<Role> ret = new ArrayList<Role>();

        // ExtCellとRoleの結びつけ設定から払い出すRoleをリストアップ
        this.addRoleListExtCelltoRole(token, ret);

        // ExtCellとRelationとRoleの結びつけから払い出すRoleをリストアップ
        // と
        // ExtCellとRelationとExtRoleとRoleの結びつけから払い出すRoleをリストアップ
        this.addRoleListExtCelltoRelationAndExtRole(token, ret);

        return ret;
    }

    /**
     * ExtCellとRoleの突き合わせを行い払い出すRoleを決める.
     * @param token
     *            トランスセルアクセストークン
     * @param roles
     *            払い出すロールのリスト。ここに追加する（破壊的メソッド）
     */
    private void addRoleListExtCelltoRole(final IExtRoleContainingToken token, List<Role> roles) {
        // ExtCell-Role結びつけに対応するRoleの取得
        String extCell = token.getExtCellUrl();
        String principal = token.getSubject();
        String principalCell;
        if (principal.contains("#")) {
            principalCell = token.getSubject().substring(0, principal.indexOf("#"));
        } else {
            principalCell = token.getSubject();
        }

        // アクセス主体がExtCellと異なる場合（2段階以上のトランスセルトークン認証）は許さない。
        if (extCell.equals(principalCell)) {
            ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
            EntitiesResponse response = null;
            // 検索結果出力件数設定
            QueryInfo qi = QueryInfo.newBuilder().setTop(TOP_NUM).setInlineCount(InlineCount.NONE).build();

            List<String> list = UriUtils.getUrlVariations(this.getUnitUrl(), extCell);
            for (int i = 0; i < list.size(); i++) {
                String extCellUrl = list.get(i);
                try {
                    // ExtCell-Roleのリンク情報取得
                    response = (EntitiesResponse) op.getNavProperty(ExtCell.EDM_TYPE_NAME,
                            OEntityKey.create(extCellUrl),
                            "_" + Role.EDM_TYPE_NAME, qi);
                } catch (PersoniumCoreException dce) {
                    if (PersoniumCoreException.OData.NO_SUCH_ENTITY != dce) {
                        throw dce;
                    }
                }
                if (response != null) {
                    break;
                }
            }
            if (response == null) {
                return;
            }

            // ExtCell-Roleのリンク情報をすべて見て今回アクセスしてきたセル向けのロールを洗い出す。
            List<OEntity> entList = response.getEntities();
            for (OEntity ent : entList) {
                OEntityWrapper entRole = (OEntityWrapper) ent;
                this.addRole(entRole.getUuid(), roles);
            }
        }
    }

    /**
     * ExtCellとRelationとRoleの結びつけから払い出すRoleをリストアップ. と
     * ExtCellとRelationとExtRoleとRoleの結びつけから払い出すRoleをリストアップ.
     * @param token
     *            トランスセルアクセストークン
     * @param roles
     *            払い出すロールのリスト。ここに追加する（破壊的メソッド）
     */
    @SuppressWarnings("unchecked")
    private void addRoleListExtCelltoRelationAndExtRole(final IExtRoleContainingToken token, List<Role> roles) {
        String extCell = token.getExtCellUrl();

        // ExtCell-Role結びつけに対応するRoleの取得
        ODataProducer op = ModelFactory.ODataCtl.cellCtl(this);
        EntitiesResponse response = null;
        // 検索結果出力件数設定
        QueryInfo qi = QueryInfo.newBuilder().setTop(TOP_NUM).setInlineCount(InlineCount.NONE).build();
        List<String> list = UriUtils.getUrlVariations(this.getUnitUrl(), extCell);
        for (int i = 0; i < list.size(); i++) {
            try {
                String extCellUrl = list.get(i);
                // ExtCell-Relationのリンク情報取得
                response = (EntitiesResponse) op.getNavProperty(ExtCell.EDM_TYPE_NAME,
                        OEntityKey.create(extCellUrl),
                        "_" + Relation.EDM_TYPE_NAME, qi);
            } catch (PersoniumCoreException dce) {
                if (PersoniumCoreException.OData.NO_SUCH_ENTITY != dce) {
                    throw dce;
                }
            }
            if (response != null) {
                break;
            }
        }
        if (response == null) {
            return;
        }

        List<OEntity> entList = response.getEntities();
        for (OEntity ent : entList) {
            OEntityWrapper entRelation = (OEntityWrapper) ent;

            // ExtCell-Relationのリンク情報をすべて見て今回アクセスしてきたセル向けのロールを洗い出す。
            PersoniumSearchResponse res = serchRoleLinks(Relation.EDM_TYPE_NAME, entRelation.getUuid());
            if (res == null) {
                continue;
            }
            this.addRoles(res.getHits().getHits(), roles);
            // ↑ ここまででExtCellとRelationとRoleの結びつけから払い出すRoleをリストアップ.は完了
            // ↓ こっからはExtCellとRelationとExtRoleとRoleの結びつけから払い出すRoleをリストアップの処理.

            // RelationからExtRoleの情報取得。
            EntitySetAccessor extRoleType = EsModel.cellCtl(this, ExtRole.EDM_TYPE_NAME);

            // Relationに結びつくExtRoleの検索
            // 現在の登録件数を取得してから一覧取得する
            Map<String, Object> source = new HashMap<String, Object>();

            // 暗黙フィルタを指定して、検索対象を検索条件の先頭に設定する（絞りこみ）
            List<Map<String, Object>> implicitFilters = QueryMapFactory.getImplicitFilters(this.id, null, null, null,
                    extRoleType.getType());
            String linksKey = OEntityDocHandler.KEY_LINK + "." + Relation.EDM_TYPE_NAME;
            implicitFilters.add(0, QueryMapFactory.termQuery(linksKey, entRelation.getUuid()));
            Map<String, Object> query = QueryMapFactory.mustQuery(implicitFilters);
            Map<String, Object> filteredQuery = QueryMapFactory.filteredQuery(null, query);
            source.put("query", filteredQuery);
            long hitNum = extRoleType.count(source);
            // ExtCellの設定が存在しないときは飛ばす
            if (hitNum == 0) {
                continue;
            }
            source.put("size", hitNum);

            PersoniumSearchHits extRoleHits = extRoleType.search(source).getHits();
            // ExtCellの設定が存在しないときは飛ばす
            // 件数取得後に削除される場合があるため、検索結果を再度確認しておく
            if (extRoleHits.getCount() == 0) {
                continue;
            }
            for (PersoniumSearchHit extRoleHit : extRoleHits.getHits()) {
                Map<String, Object> extRoleSource = extRoleHit.getSource();
                Map<String, Object> extRoleS = (Map<String, Object>) extRoleSource.get("s");
                String esExtRole = (String) extRoleS.get(ExtRole.EDM_TYPE_NAME);

                // トークンに入ってるロールと突き合わせ
                for (Role tokenRole : token.getRoleList()) {
                    if (!tokenRole.createUrl().equals(esExtRole)) {
                        continue;
                    }
                    // ExtCell-Roleのリンク情報をすべて見て今回アクセスしてきたセル向けのロールを洗い出す。
                    PersoniumSearchResponse resExtRoleToRole = serchRoleLinks(
                            ExtRole.EDM_TYPE_NAME, extRoleHit.getId());
                    if (resExtRoleToRole == null) {
                        continue;
                    }
                    this.addRoles(resExtRoleToRole.getHits().getHits(), roles);
                }
            }
        }
    }

    /**
     * Roleと他のエンティテセットのリンクテーブルから対応するデータを取得する.
     * @param searchKey
     *            検索条件のエンティティセット名
     * @param searchValue
     *            検索するuuid
     * @return 検索結果
     */
    private PersoniumSearchResponse serchRoleLinks(final String searchKey, final String searchValue) {

        ODataLinkAccessor links = EsModel.cellCtlLink(this);
        // Relationに結びつくロールの検索
        Map<String, Object> source = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();
        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();

        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        queries.add(QueryMapFactory.termQuery("t1", searchKey));
        queries.add(QueryMapFactory.termQuery("t2", "Role"));

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

        filters.add(QueryMapFactory.termQuery("k1", searchValue));
        and.put("filters", filters);
        filter.put("and", and);
        source.put("filter", filter);
        source.put("query", query);
        // 検索結果件数設定
        source.put("size", TOP_NUM);

        return links.search(source);
    }

    /**
     * Roleが含まれたSearchHitの配列からロールの値を取得する.
     * @param hits
     *            Roleを検索し結果
     * @param roles
     *            払い出すロールのリスト。ここに追加する（破壊的メソッド）
     */
    private void addRoles(PersoniumSearchHit[] hits, List<Role> roles) {
        for (PersoniumSearchHit hit : hits) {
            Map<String, Object> src = hit.getSource();
            String roleUuid = (String) src.get("k2");

            // Relation-Roleのリンク情報をすべて見て今回アクセスしてきたセル向けのロールを洗い出す。
            this.addRole(roleUuid, roles);
        }
    }

    /**
     * ロールの値を取得する.
     * @param uuid
     *            RoleのUUID
     * @param roles
     *            払い出すロールのリスト。ここに追加する（破壊的メソッド）
     */
    @SuppressWarnings("unchecked")
    private void addRole(String uuid, List<Role> roles) {
        EntitySetAccessor roleDao = EsModel.cellCtl(this, Role.EDM_TYPE_NAME);
        PersoniumGetResponse gRes = roleDao.get(uuid);
        if (gRes == null) {
            return;
        }
        Map<String, Object> src = gRes.getSource();
        Map<String, Object> s = (Map<String, Object>) src.get("s");
        Map<String, Object> l = (Map<String, Object>) src.get("l");
        String roleName = (String) s.get(KEY_NAME);
        String schema = (String) s.get(KEY_SCHEMA);
        String boxId = (String) l.get(Box.EDM_TYPE_NAME);
        String boxName = null;
        if (boxId != null) {
            // Boxの検索
            Map<String, Object> boxsrc = DavCmpFsImpl.searchBox(this, boxId);
            Map<String, Object> boxs = (Map<String, Object>) boxsrc.get("s");
            boxName = (String) boxs.get(KEY_NAME);
        }

        roles.add(new Role(roleName, boxName, schema, this.url));
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    @Override
    public String getDataBundleNameWithOutPrefix() {
        String unitUserName;
        if (this.owner == null) {
            unitUserName = AccessContext.TYPE_ANONYMOUS;
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }
        return unitUserName;
    }

    @Override
    public String getDataBundleName() {
        String unitUserName = PersoniumUnitConfig.getEsUnitPrefix() + "_" + getDataBundleNameWithOutPrefix();
        return unitUserName;
    }

    /**
     * Cell名を取得します.
     * @return Cell名
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * このCellの内部IDを返します.
     * @return 内部ID文字列
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * このCellの内部IDを設定します.
     * @param id
     *            内部ID文字列
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * このCellのURLを返します.
     * @return URL文字列
     */
    @Override
    public String getUrl() {
        return this.url;
    }

    /**
     * このCellのURLを設定します.
     * @param url
     *            URL文字列
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * このCellのUnit URLを返します.
     * @return unitUrl 文字列
     */
    @Override
    public String getUnitUrl() {
        return UriUtils.getUnitUrl(this.getUrl());
    }

    static final String KEY_NAME = "Name";
    static final String KEY_SCHEMA = "Schema";

    /**
     * プロパティ項目の値を正規表現でチェックする.
     * @param propValue
     *            プロパティ値
     * @param dcFormat
     *            dcFormatの値
     * @return フォーマットエラーの場合、falseを返却
     */
    private static boolean validatePropertyRegEx(String propValue, String dcFormat) {
        // フォーマットのチェックを行う
        Pattern pattern = Pattern.compile(dcFormat);
        Matcher matcher = pattern.matcher(propValue);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }

    /**
     * Cellの作成時間を返却する.
     * @return Cellの作成時間
     */
    public long getPublished() {
        return this.published;
    }

    @Override
    public String roleIdToRoleResourceUrl(String roleId) {
        CellCtlODataProducer ccop = new CellCtlODataProducer(this);
        OEntity oe = ccop.getEntityByInternalId(Role.EDM_TYPE_NAME, roleId);
        if (oe == null) {
            // ロールが存在しない場合、nullを返す。
            return null;
        }

        String boxName = (String) oe.getProperty("_Box.Name").getValue();
        OProperty<?> schemaProp = oe.getProperty("_Box.Schema");
        String schema = null;
        if (schemaProp != null) {
            schema = (String) schemaProp.getValue();
        }
        String roleName = (String) oe.getProperty("Name").getValue();
        Role roleObj = new Role(roleName, boxName, schema, this.getUrl());
        return roleObj.createUrl();
    }

    @Override
    public String roleResourceUrlToId(String roleUrl, String baseUrl) {
        EntitySetAccessor roleType = EsModel.cellCtl(this, Role.EDM_TYPE_NAME);

        // roleNameがURLの対応
        URL rUrl = null;
        try {
            // xml:baseの対応
            if (baseUrl != null && !"".equals(baseUrl)) {
                // URLの相対パス対応
                rUrl = new URL(new URL(baseUrl), roleUrl);
            } else {
                rUrl = new URL(roleUrl);
            }
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND.reason(e);
        }

        Role role = null;
        try {
            role = new Role(rUrl);
        } catch (MalformedURLException e) {
            log.info("Role URL:" + rUrl.toString());
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND;
        }

        // ロールリソースのセルURL部分はACL設定対象のセルURLと異なるものを指定することは許さない
        if (!(this.getUrl().equals(role.getBaseUrl()))) {
            PersoniumCoreLog.Dav.ROLE_NOT_FOUND.params("Cell different").writeLog();
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND;
        }
        // Roleの検索
        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        queries.add(QueryMapFactory.termQuery("c", this.getId()));
        queries.add(QueryMapFactory.termQuery("s." + KEY_NAME + ".untouched", role.getName()));

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();
        if (!(Box.DEFAULT_BOX_NAME.equals(role.getBoxName()))) {
            // Roleがボックスと紐付く場合に、検索クエリを追加
            Box targetBox = this.getBoxForName(role.getBoxName());
            if (targetBox == null) {
                throw PersoniumCoreException.Dav.BOX_LINKED_BY_ROLE_NOT_FOUND.params(baseUrl);
            }
            String boxId = targetBox.getId();
            filters.add(QueryMapFactory.termQuery("l." + Box.EDM_TYPE_NAME, boxId));
        } else {
            // Roleがボックスと紐付かない場合にもnull検索クエリを追加
            filters.add(QueryMapFactory.missingFilter("l." + Box.EDM_TYPE_NAME));
        }

        Map<String, Object> source = new HashMap<String, Object>();
        if (!filters.isEmpty()) {
            source.put("filter", QueryMapFactory.andFilter(filters));
        }
        source.put("query", query);
        PersoniumSearchHits hits = roleType.search(source).getHits();

        // 対象のRoleが存在しない場合はNull
        if (hits == null || hits.getCount() == 0) {
            PersoniumCoreLog.Dav.ROLE_NOT_FOUND.params("Not Hit").writeLog();
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND;
        }
        // 対象のRoleが複数件取得された場合は内部エラーとする
        if (hits.getAllPages() > 1) {
            PersoniumCoreLog.OData.FOUND_MULTIPLE_RECORDS.params(hits.getAllPages()).writeLog();
            throw PersoniumCoreException.OData.DETECTED_INTERNAL_DATA_CONFLICT;
        }

        PersoniumSearchHit hit = hits.getHits()[0];
        return hit.getId();
    }

    @Override
    public void delete(boolean recursive, String unitUserName) {
        // Cellに対するアクセス数を確認して、アクセスをロックする
        int maxLoopCount = PersoniumUnitConfig.getCellLockRetryTimes();
        long interval = PersoniumUnitConfig.getCellLockRetryInterval();
        waitCellAccessible(this.id, maxLoopCount, interval);

        CellLockManager.setCellStatus(this.id, CellLockManager.STATUS.BULK_DELETION);

        // Cellエンティティを削除する
        CellAccessor cellAccessor = (CellAccessor) EsModel.cell();
        CellDocHandler docHandler = new CellDocHandler(cellAccessor.get(this.getId()));
        try {
            cellAccessor.delete(docHandler);
            log.info("Cell Entity Deletion End.");
        } finally {
            CellCache.clear(this.getName());
            CellLockManager.setCellStatus(this.getId(), CellLockManager.STATUS.NORMAL);
        }

        // Make this cell empty asynchronously
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                makeEmpty();
            }
        });
        thread.start();

    }

    private void waitCellAccessible(String cellId, int maxLoopCount, long interval) {
        for (int loopCount = 0; loopCount < maxLoopCount; loopCount++) {
            long count = CellLockManager.getReferenceCount(cellId);
            // 自分のリクエスト分も含まれるので他のリクエストが存在する場合は１より大きくなる
            if (count <= 1) {
                return;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                throw PersoniumCoreException.Misc.CONFLICT_CELLACCESS;
            }
        }
        throw PersoniumCoreException.Misc.CONFLICT_CELLACCESS;
    }

    private static final int DAVFILE_DEFAULT_FETCH_COUNT = 1000;

    @Override
    public void makeEmpty() {
        CellAccessor cellAccessor = (CellAccessor) EsModel.cell();
        String unitUserNameWithOutPrefix = this.getDataBundleNameWithOutPrefix();
        String cellInfoLog = String.format(" CellId:[%s], CellName:[%s], CellUnitUserName:[%s]", this.getId(),
                this.getName(), this.getDataBundleName());

        // セルIDとタイプ情報をクエリに使用してWebDavファイルの管理情報一覧の件数を取得する
        long davfileCount = cellAccessor.getDavFileTotalCount(this.getId(), unitUserNameWithOutPrefix);

        // 1000件ずつ、WebDavファイルの管理情報件数まで以下を実施する
        int fetchCount = DAVFILE_DEFAULT_FETCH_COUNT;
        BinaryDataAccessor accessor = new BinaryDataAccessor(
                PersoniumUnitConfig.getBlobStoreRoot(), unitUserNameWithOutPrefix,
                PersoniumUnitConfig.getPhysicalDeleteMode(), PersoniumUnitConfig.getFsyncEnabled());
        for (int i = 0; i <= davfileCount; i += fetchCount) {
            // WebDavファイルのID一覧を取得する
            List<String> davFileIdList = cellAccessor.getDavFileIdList(this.getId(), unitUserNameWithOutPrefix,
                    fetchCount, i);
            // BinaryDataAccessorのdeleteメソッドにて「.deleted」にリネームする
            for (String davFileId : davFileIdList) {
                try {
                    accessor.delete(davFileId);
                } catch (BinaryDataAccessException e) {
                    // 削除に失敗した場合はログを出力して処理を続行する
                    log.warn(String.format("Delete DavFile Failed DavFileId:[%s].", davFileId) + cellInfoLog, e);
                }
            }
        }
        log.info("DavFile Deletion End.");

        // delete CellSnapshot
        CellSnapshotCellCmp snapshotCmp = ModelFactory.cellSnapshotCellCmp(this);
        try {
            snapshotCmp.delete(null, false);
        } catch (PersoniumCoreException e) {
            // If the deletion fails, output a log and continue processing.
            log.warn(String.format("Delete CellSnapshot Failed."), e);
        }
        log.info("CellSnapshotFile Deletion End.");

        // delete EventLog file
        try {
            EventUtils.deleteEventLog(this.getId(), this.getOwner());
            log.info("EventLog Deletion End.");
        } catch (BinaryDataAccessException e) {
            // 削除に失敗した場合はログを出力して処理を続行する
            log.warn("Delete EventLog Failed." + cellInfoLog, e);
        }

        // Cell配下のエンティティを削除する
        cellAccessor.cellBulkDeletion(this.getId(), unitUserNameWithOutPrefix);
        log.info("Cell Entity Resource Deletion End.");
    }
}
