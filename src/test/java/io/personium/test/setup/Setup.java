/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
package io.personium.test.setup;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.jersey.box.odatacol.UserDataListWithNPTest;
import io.personium.test.jersey.box.odatacol.schema.property.PropertyUtils;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.RuleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * テスト環境の構築.
 */
@RunWith(PersoniumIntegTestRunner.class)
public class Setup extends AbstractCase {

    /** 作成するテスト環境情報を格納する。複数のセル環境を作ることを想定しているためListで用意. */
    List<Config> confs = new ArrayList<Config>();
    List<Config> eventLogConfs = new ArrayList<Config>();

    static final int NUM_ACCOUNTS = 31;
    static final int NUM_APP_AUTH_ACCOUNTS = 2;
    static final int NUM_ROLES = NUM_ACCOUNTS;
    static final int NUM_USERDATA = 10;
    /** テストセル１の名前. */
    public static final String TEST_CELL1 = "testcell1";
    /** テストセル2の名前. */
    public static final String TEST_CELL2 = "testcell2";

    /** テストSchemaセル1の名前. */
    public static final String TEST_CELL_SCHEMA1 = "schema1";
    /** テストSchemaセル2の名前. */
    public static final String TEST_CELL_SCHEMA2 = "schema2";

    /** EventLog用テストセルの名前. */
    public static final String TEST_CELL_EVENTLOG = "eventlogtestcell";

    /** Basic認証用のテストセルの名前. */
    public static final String TEST_CELL_BASIC = "basictestsetupcell";

    /** OpenID Connect認証用のテストセルの名前. */
    public static final String TEST_CELL_OIDC = "oidctestsetupcell";

    /** $filterテスト用セル名. */
    public static final String TEST_CELL_FILTER = "filtertypevalidatetest";

    /** テストセル１>テストボックス１の名前. */
    public static final String TEST_BOX1 = "box1";
    /** テストセル１>テストボックス１の名前. */
    public static final String TEST_BOX2 = "box2";

    /** EngineServiceCollection Name. */
    public static final String TEST_ENGINE_SERVICE = "service_relay";

    /** テストセル１>テストボックス1>Odataコレクションの名前. */
    public static final String TEST_ODATA = "setodata";

    /** テストセル１>テストボックス1>検索テスト用Odataコレクションの名前. */
    public static final String SEARCH_ODATA = "searchodata";

    /** テストセル１>テストボックス1>検索テスト用Odataコレクション内のEntityType一覧. */
    public static final String[] SEARCH_ENTITY_TYPES = new String[] {"string", "stringList", "int", "intList",
            "single", "singleList", "boolean", "booleanList", "datetime" };

    /** テストセル１>テストボックス1>Odataコレクション>EntityTypeの名前. */
    public static final String TEST_ENTITYTYPE_M1 = "entityMulti_1";
    /** テストセル１>テストボックス1>Odataコレクション>EntityTypeの名前. */
    public static final String TEST_ENTITYTYPE_MN = "entityMulti_N";
    /** テストセル１>テストボックス1>Odataコレクション>EntityTypeの名前. */
    public static final String TEST_ENTITYTYPE_MDP = "entityMaxDynamicProp";

    /** セル同士の関係名. */
    public static final String CELL_RELATION = "cellrelation";

    /** VETオーナー. */
    public static final String OWNER_VET = "https://example.com/test#vet";
    /** HMCオーナー. */
    public static final String OWNER_HMC = "https://example.com/test#hmc";
    /** EVTオーナー. */
    // eventLogTestCell 用のオーナー
    public static final String OWNER_EVT = "https://example.com/test#evt";

    static final double DECIMAL = 0.1;

    static final String TEST_RULE_NAME = "rule1";
    static final long WAIT_TIME_FOR_EVENT = 3000; // msec
    static final long WAIT_TIME_FOR_BULK_DELETE = 1000L;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public Setup() {
        super(new PersoniumCoreApplication());
        // アカウント生成
        List<AccountConfig> accounts = new ArrayList<AccountConfig>();
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            AccountConfig account = new AccountConfig();
            account.accountName = "account" + i;
            account.accountPass = "password" + i;
            accounts.add(account);
        }

        // BOX2個 1つ目にはサービス・Odata・WebDAVコレクションを作成
        BoxConfig box1 = new BoxConfig();
        box1.boxName = "box1";
        box1.boxSchema = UrlUtils.cellRoot("schema1");
        SvcCollectionConfig svcColl = new SvcCollectionConfig();
        OdataSvcCollectionConfig odataColl = new OdataSvcCollectionConfig();
        DavSvcCollectionConfig davColl = new DavSvcCollectionConfig();
        box1.davCol.add(davColl);
        box1.odataCol.add(odataColl);
        box1.svcCol.add(svcColl);
        svcColl.patch = new ProppatchConfig();

        // box2つ目
        BoxConfig box2 = new BoxConfig();
        box2.boxName = "box2";
        box2.boxSchema = null;

        Config conf1 = new Config();
        conf1.cellName = "testcell1";
        conf1.owner = OWNER_VET;
        conf1.account = accounts;
        conf1.extRole = settingExtRole(TEST_CELL2);
        conf1.role = settingRole(conf1.account, conf1.extRole);
        conf1.rule = settingRule();
        conf1.box.add(box1);
        conf1.box.add(box2);
        confs.add(conf1);

        Config conf2 = new Config();
        conf2.cellName = "testcell2";
        conf2.owner = OWNER_HMC;
        conf2.account = accounts;
        conf2.extRole = settingExtRole(TEST_CELL1);
        conf2.role = settingRole(conf2.account, conf2.extRole);
        conf2.box.add(box1);
        conf2.box.add(box2);
        confs.add(conf2);

        // ExtCellの登録
        conf1.extCellUrl.add(UrlUtils.cellRoot(TEST_CELL2));
        conf2.extCellUrl.add(UrlUtils.cellRoot(TEST_CELL1));

        // 関係
        RelationConfig relation1 = new RelationConfig();
        relation1.name = CELL_RELATION;
        relation1.boxName = null;
        relation1.linkExtCell.add(UrlUtils.cellRoot(conf2.cellName));
        conf1.relation.add(relation1);

        RelationConfig relation2 = new RelationConfig();
        relation2.name = CELL_RELATION;
        relation2.boxName = null;
        relation2.linkExtCell.add(UrlUtils.cellRoot(conf1.cellName));
        conf2.relation.add(relation2);

        // アプリ認証セルアカウント2件
        List<AccountConfig> appAuthAccounts = new ArrayList<AccountConfig>();
        for (int i = 0; i < NUM_APP_AUTH_ACCOUNTS; i++) {
            AccountConfig account = new AccountConfig();
            account.accountName = "account" + i;
            account.accountPass = "password" + i;
            appAuthAccounts.add(account);
        }

        // アプリ認証セル1
        Config appAuthConf1 = new Config();
        appAuthConf1.cellName = TEST_CELL_SCHEMA1;
        appAuthConf1.owner = OWNER_VET;
        appAuthConf1.account = appAuthAccounts;
        confs.add(appAuthConf1);

        // アプリ認証セルのアカウントにconfidentialRole結びつけ（account1 が confidentialRoleを持つ）
        RoleConfig role = new RoleConfig();
        role.roleName = OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME;
        appAuthConf1.role.add(role);
        role.linkAccounts.add(appAuthAccounts.get(1));

        // アプリ認証セル2
        Config appAuthConf2 = new Config();
        appAuthConf2.cellName = TEST_CELL_SCHEMA2;
        appAuthConf2.owner = OWNER_HMC;
        appAuthConf2.account = appAuthAccounts;
        confs.add(appAuthConf2);

        // $filterテスト用セル（他のテストに影響がでるので匿名ユーザに作成）
        Config filterConf = new Config();
        filterConf.cellName = TEST_CELL_FILTER;
        filterConf.owner = null;
        confs.add(filterConf);

        // EventLog用セル（セル名のみ）
        Config eventLogConf = new Config();
        eventLogConf.cellName = TEST_CELL_EVENTLOG;
        eventLogConf.owner = OWNER_EVT;
        eventLogConf.rule = settingRule();
        eventLogConfs.add(eventLogConf);

        // Basic認証テスト用のセル
        BoxConfig basicTestBox = new BoxConfig();
        basicTestBox.boxName = "box1";
        basicTestBox.boxSchema = null;
        svcColl = new SvcCollectionConfig();
        odataColl = new OdataSvcCollectionConfig();
        davColl = new DavSvcCollectionConfig();
        basicTestBox.davCol.add(davColl);
        basicTestBox.odataCol.add(odataColl);
        basicTestBox.svcCol.add(svcColl);
        svcColl.patch = new ProppatchConfig();

        Config basicTestCellConf = new Config();
        basicTestCellConf.cellName = TEST_CELL_BASIC;
        basicTestCellConf.owner = null;
        basicTestCellConf.account = accounts;
        basicTestCellConf.extRole = new ArrayList<ExtRoleConfig>();
        basicTestCellConf.role = settingRole(basicTestCellConf.account, basicTestCellConf.extRole);
        basicTestCellConf.box.add(basicTestBox);
        confs.add(basicTestCellConf);

        // TODO OIDC用のTestCell作成
        // IDToken取得機構を作るときに対応予定
    }

    /**
     * テスト環境構築. テストで共通で使用するセル等を作成する。すでに環境があったら削除してから作成するので環境をリセットするときにも利用する。
     * 単独でJUnitのテストとして起動して使ってください。
     */
    @Test
    public void reset() {
        LockManager.deleteAllLocks();
        for (Config conf : confs) {
            this.delete(conf);
            this.create(conf);
        }
    }

    /**
     * EventLog用テスト環境を構築する.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void resetEventLog() throws InterruptedException {
        LockManager.deleteAllLocks();
        for (Config conf : eventLogConfs) {
            this.delete(conf);
            this.createCell(conf);
            for (RuleConfig rule : conf.rule) {
                RuleUtils.create(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, rule.toJson(), HttpStatus.SC_CREATED);
            }
            Thread.sleep(WAIT_TIME_FOR_EVENT);
            this.createEventLog(conf);
            Thread.sleep(WAIT_TIME_FOR_EVENT);
        }
    }

    /**
     * テスト環境削除. テストで共通で使用するセル等を削除する。 単独でJUnitのテストとして起動して使ってください。
     */
    @Test
    public void destroy() {
        for (Config conf : confs) {
            this.delete(conf);
        }
    }

    /**
     * Roleの設定を行う.
     * @param ExtRoleConfig
     *            ExtRoleの設定
     */
    private List<RoleConfig> settingRole(List<AccountConfig> accounts, List<ExtRoleConfig> extRoles) {
        List<RoleConfig> roles = new ArrayList<RoleConfig>();
        for (int i = 0; i < NUM_ROLES; i++) {
            // ロール作成
            RoleConfig role = new RoleConfig();
            role.roleName = "role" + i;
            role.linkAccounts.add(accounts.get(i));
            if (extRoles != null && extRoles.size() > 0) {
                role.linkExtRole.add(extRoles.get(i));
            }
            roles.add(role);
        }
        return roles;
    }

    /**
     * ExtRoleの設定を行う.
     * @param extCell
     *            ExtRoleの向け先のCellURL
     */
    private List<ExtRoleConfig> settingExtRole(String extCell) {
        List<ExtRoleConfig> extRoles = new ArrayList<ExtRoleConfig>();
        for (int i = 0; i < NUM_ROLES; i++) {
            // ExtRole作成
            ExtRoleConfig extRole = new ExtRoleConfig();
            extRole.extRole = UrlUtils.roleResource(extCell, Box.MAIN_BOX_NAME, "role" + i);
            extRole.relationName = CELL_RELATION;
            extRole.relationBoxName = null;
            extRoles.add(extRole);
        }
        return extRoles;
    }

    private List<RuleConfig> settingRule() {
        List<RuleConfig> rules = new ArrayList<RuleConfig>();
        RuleConfig rule = new RuleConfig();
        rule.name = TEST_RULE_NAME;
        rule.external = true;
        rule.action = "log";
        rules.add(rule);
        return rules;
    }

    /**
     * テスト環境構築.
     * @param conf
     *            設定情報
     */
    private void create(Config conf) {
        // Cell作成
        createCell(conf);

        // ExtCell作成
        createExtCell(conf);

        // Relation作成
        for (RelationConfig relation : conf.relation) {
            RelationUtils.create(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, relation.toJson(),
                    HttpStatus.SC_CREATED);
            // RelationとExtCellの$link
            for (String extCell : relation.linkExtCell) {
                LinksUtils.createLinksExtCell(conf.cellName, CommonUtils.encodeUrlComp(extCell),
                        Relation.EDM_TYPE_NAME, relation.name, null,
                        AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            }
        }

        // アカウント作成
        for (AccountConfig account : conf.account) {
            this.createAccount(conf.cellName, account.accountName, account.accountPass);
        }

        // ExtRole作成
        for (ExtRoleConfig extRole : conf.extRole) {
            ExtRoleUtils.create(AbstractCase.MASTER_TOKEN_NAME, conf.cellName, extRole.toJson(), HttpStatus.SC_CREATED);
        }

        // Role作成
        for (RoleConfig role : conf.role) {
            TResponse res = createRole(conf.cellName, role.roleName);
            String roleUrl = res.getLocationHeader();
            // Role紐付け作成
            if (role.linkAccounts != null) {
                for (AccountConfig account : role.linkAccounts) {
                    this.linkAccountAndRole(conf.cellName, account.accountName, roleUrl);
                }
            }
            // ExtRoleとRoleの紐付け
            if (role.linkExtRole != null) {
                for (ExtRoleConfig extRole : role.linkExtRole) {
                    LinksUtils.createLinksExtRole(conf.cellName, CommonUtils.encodeUrlComp(extRole.extRole),
                            extRole.relationName, extRole.relationBoxName, Role.EDM_TYPE_NAME, role.roleName, null,
                            AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
                }
            }
            if ("testcell2".equals(conf.cellName)) {
                // ExtCellとロールの結びつけ
                // testcell2のtestxell1向けのExtCellにrole2（readができるロール）を結びつけてやる
                this.linkExtCelltoRole(CommonUtils.encodeUrlComp(UrlUtils.cellRoot("testcell1")), conf.cellName,
                        roleUrl);
            }
        }

        // Box作成
        for (BoxConfig box : conf.box) {
            this.createBox(conf.cellName, box.boxName, box.boxSchema);
            // box1にのみACL設定
            if ("box1".equals(box.boxName)) {
                // BoxレベルACLテスト用
                DavResourceUtils.setACL(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, "",
                        "box/acl-setscheme-none-schema-level.txt", Setup.TEST_BOX1, "");
            }
        }

        // Rule
        for (RuleConfig rule : conf.rule) {
            RuleUtils.create(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, rule.toJson(), HttpStatus.SC_CREATED);
        }

        if ("testcell1".equals(conf.cellName) || "testcell2".equals(conf.cellName)) {
            createServiceCollection(conf.cellName, TEST_BOX1, "setservice");
            createOdataCollection(conf.cellName, TEST_BOX1, TEST_ODATA);
            createOdataCollection(conf.cellName, TEST_BOX1, SEARCH_ODATA);

            DavResourceUtils.setACL(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, TEST_ODATA,
                    "box/acl-setscheme-none-schema-level.txt", Setup.TEST_BOX1, "");
            createWebdavCollection(conf.cellName, TEST_BOX1, "setdavcol");
            DavResourceUtils.setACL(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, "setdavcol",
                    "box/acl-setscheme-none-schema-level.txt", Setup.TEST_BOX1, "");
            this.createPatch2("setservice", conf.cellName);
            createServiceCollection(conf.cellName, TEST_BOX1, TEST_ENGINE_SERVICE);
            DavResourceUtils.setACL(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    TEST_ENGINE_SERVICE, "box/acl-setscheme-none-schema-level.txt", Setup.TEST_BOX1, "");

            // テストコレクションのエンティティ作成
            createTestCollectionSchema(conf.cellName);
            createMaxPropTestCollectionSchema(conf.cellName);
            createTestCollectionSchemaToSearchOdata(conf.cellName);

            // コレクションへのリソースPUT
            this.resourcesPut("setdavcol/dav.txt", conf.cellName);

            // ユーザーデータの登録
            createUserDatas(conf.cellName);
            createUserDatasToSearchOdata(conf.cellName);

            // NPテスト用のODATACollectionを作成する
            createNPTestCollectionSchema(conf);

            // ComplexType NPテスト用のスキーマを作成する
            createComplexTypeNPSchema(conf);
        }

        if (TEST_CELL_BASIC.equals(conf.cellName)) {
            createServiceCollection(conf.cellName, TEST_BOX1, "setservice");
            createOdataCollection(conf.cellName, TEST_BOX1, TEST_ODATA);

            DavResourceUtils.setACL(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, TEST_ODATA,
                    "box/acl-setscheme-none-schema-level.txt", Setup.TEST_BOX1, "");
            createWebdavCollection(conf.cellName, TEST_BOX1, "setdavcol");
            DavResourceUtils.setACL(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, "setdavcol",
                    "box/acl-setscheme-none-schema-level.txt", Setup.TEST_BOX1, "");
            this.createPatch2("setservice", conf.cellName);
            createServiceCollection(conf.cellName, TEST_BOX1, TEST_ENGINE_SERVICE);
            DavResourceUtils.setACL(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    TEST_ENGINE_SERVICE, "box/acl-setscheme-none-schema-level.txt", Setup.TEST_BOX1, "");

            // テストコレクションのエンティティ作成
            createTestCollectionSchema(conf.cellName);

            // コレクションへのリソースPUT
            this.resourcesPut("setdavcol/dav.txt", conf.cellName);
        }

        // アカウントの一覧取得用のベースデータを作成する
        createAccountforList(conf.cellName);

        // $filterテスト用のデータ作成
        setupFilterTestCell(conf);
    }

    /**
     * 最大プロパティ数テストコレクションの作成.
     * @param cellName
     *            セル名
     */
    public static void createMaxPropTestCollectionSchema(String cellName) {
        // EntityTypeを登録
        entityTypePost(TEST_ODATA, TEST_BOX1, TEST_ENTITYTYPE_M1, cellName);
        entityTypePost(TEST_ODATA, TEST_BOX1, TEST_ENTITYTYPE_MN, cellName);
        entityTypePost(TEST_ODATA, TEST_BOX1, TEST_ENTITYTYPE_MDP, cellName);

        // AssociationEndのlink作成
        associationEndPost(TEST_ODATA, TEST_ENTITYTYPE_M1, "Multi_1_to_MaxDynamicProp", "0..1", cellName);
        associationEndPost(TEST_ODATA, TEST_ENTITYTYPE_MN, "Multi_N_to_MaxDynamicProp", "\\*", cellName);
        associationEndPost(TEST_ODATA, TEST_ENTITYTYPE_MDP, "MaxDynamicProp_to_Multi_1", "\\*", cellName);
        associationEndPost(TEST_ODATA, TEST_ENTITYTYPE_MDP, "MaxDynamicProp_to_Multi_N", "\\*", cellName);

        createAssociationEndLink(TEST_ODATA, new String[] {TEST_ENTITYTYPE_M1, TEST_ENTITYTYPE_MDP},
                new String[] {"Multi_1_to_MaxDynamicProp", "MaxDynamicProp_to_Multi_1"}, cellName);
        createAssociationEndLink(TEST_ODATA, new String[] {TEST_ENTITYTYPE_MN, TEST_ENTITYTYPE_MDP},
                new String[] {"Multi_N_to_MaxDynamicProp", "MaxDynamicProp_to_Multi_N"}, cellName);

    }

    /**
     * テストコレクションの作成.
     * @param cellName
     *            セル名
     */
    public static void createTestCollectionSchema(String cellName) {
        // EntityTypeを登録
        entityTypePost(TEST_ODATA, TEST_BOX1, "Sales", cellName);
        entityTypePost(TEST_ODATA, TEST_BOX1, "SalesDetail", cellName);
        entityTypePost(TEST_ODATA, TEST_BOX1, "Product", cellName);
        entityTypePost(TEST_ODATA, TEST_BOX1, "Category", cellName);
        entityTypePost(TEST_ODATA, TEST_BOX1, "Supplier", cellName);
        entityTypePost(TEST_ODATA, TEST_BOX1, "Price", cellName);

        // AssociationEndを登録
        associationEndPost(TEST_ODATA, "Sales", "sales2salesDetail", "1", cellName);
        associationEndPost(TEST_ODATA, "SalesDetail", "salesDetail2sales", "\\*", cellName);
        associationEndPost(TEST_ODATA, "Product", "product2Sales", "\\*", cellName);
        associationEndPost(TEST_ODATA, "Sales", "sales2product", "\\*", cellName);
        associationEndPost(TEST_ODATA, "Product", "product2category", "1", cellName);
        associationEndPost(TEST_ODATA, "Category", "category2product", "1", cellName);
        associationEndPost(TEST_ODATA, "Supplier", "supplier2product", "0..1", cellName);
        associationEndPost(TEST_ODATA, "Product", "product2supplier", "1", cellName);
        associationEndPost(TEST_ODATA, "Sales", "sales2supplier", "0..1", cellName);
        associationEndPost(TEST_ODATA, "Supplier", "supplier2sales", "\\*", cellName);
        associationEndPost(TEST_ODATA, "Price", "price2sales", "0..1", cellName);
        associationEndPost(TEST_ODATA, "Sales", "sales2price", "0..1", cellName);

        createAssociationEndLink(TEST_ODATA, new String[] {"SalesDetail", "Sales" },
                new String[] {"salesDetail2sales", "sales2salesDetail" }, cellName);
        createAssociationEndLink(TEST_ODATA, new String[] {"Product", "Sales" },
                new String[] {"product2Sales", "sales2product" }, cellName);
        createAssociationEndLink(TEST_ODATA, new String[] {"Supplier", "Product" },
                new String[] {"supplier2product", "product2supplier" }, cellName);
        createAssociationEndLink(TEST_ODATA, new String[] {"Sales", "Supplier" },
                new String[] {"sales2supplier", "supplier2sales" }, cellName);
        createAssociationEndLink(TEST_ODATA, new String[] {"Price", "Sales" },
                new String[] {"price2sales", "sales2price" }, cellName);

    }

    /**
     * テストコレクションの作成.
     * @param cellName
     *            セル名
     */
    public static void createTestCollectionSchemaToSearchOdata(String cellName) {
        // EntityTypeを登録
        for (String entityType : SEARCH_ENTITY_TYPES) {
            entityTypePost(SEARCH_ODATA, TEST_BOX1, entityType, cellName);
        }
        entityTypePost(SEARCH_ODATA, TEST_BOX1, "dynamic", cellName);

        // Propertyを登録
        final int count = 5;
        for (int i = 0; i < count; i++) {
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "string",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, null, false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "stringList",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, "List", false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "int",
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, null, false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "intList",
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "List", false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "single",
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), true, null, null, false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "singleList",
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), true, null, "List", false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "boolean",
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), true, null, null, false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "booleanList",
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), true, null, "List", false, null);
            UserDataUtils.createProperty(cellName, TEST_BOX1, SEARCH_ODATA, "property" + i, "datetime",
                    EdmSimpleType.DATETIME.getFullyQualifiedTypeName(), true, null, null, false, null);
        }
    }

    /**
     * 以下のようなODataスキーマを作成. A(0..1) - B(0..1) | C(1) - A(0..1) | D(*) - A(0..1)
     * A(0..1) - C(1) | B(1) - C(1) | D(*) - B(1) A(0..1) - D(*) | B(1) - D(*) |
     * C(*) - D(*)
     * @param conf
     *            設定ファイル
     */
    public void createNPTestCollectionSchema(Config conf) {
        createOdataCollection(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION);

        // EntitySetを登録する
        entityTypePost(UserDataListWithNPTest.ODATA_COLLECTION, TEST_BOX1, UserDataListWithNPTest.ENTITY_TYPE_A,
                conf.cellName);
        entityTypePost(UserDataListWithNPTest.ODATA_COLLECTION, TEST_BOX1, UserDataListWithNPTest.ENTITY_TYPE_B,
                conf.cellName);
        entityTypePost(UserDataListWithNPTest.ODATA_COLLECTION, TEST_BOX1, UserDataListWithNPTest.ENTITY_TYPE_C,
                conf.cellName);
        entityTypePost(UserDataListWithNPTest.ODATA_COLLECTION, TEST_BOX1, UserDataListWithNPTest.ENTITY_TYPE_D,
                conf.cellName);

        // AssociationEndを登録する
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AB_A, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AC_A, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AD_A, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_AB_B, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_AC_C, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_AD_D, UserDataListWithNPTest.MULTI_AST, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BC_B, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_BC_C, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BD_B, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_BD_D, UserDataListWithNPTest.MULTI_AST, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_CD_C, UserDataListWithNPTest.MULTI_AST, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_CD_D, UserDataListWithNPTest.MULTI_AST, conf.cellName);

        // AssociationEndの関係を結ぶ
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_A, UserDataListWithNPTest.ENTITY_TYPE_B },
                new String[] {UserDataListWithNPTest.ASSOC_AB_A, UserDataListWithNPTest.ASSOC_AB_B }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_A, UserDataListWithNPTest.ENTITY_TYPE_C },
                new String[] {UserDataListWithNPTest.ASSOC_AC_A, UserDataListWithNPTest.ASSOC_AC_C }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_A, UserDataListWithNPTest.ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_AD_A, UserDataListWithNPTest.ASSOC_AD_D }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_B, UserDataListWithNPTest.ENTITY_TYPE_C },
                new String[] {UserDataListWithNPTest.ASSOC_BC_B, UserDataListWithNPTest.ASSOC_BC_C }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_B, UserDataListWithNPTest.ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_BD_B, UserDataListWithNPTest.ASSOC_BD_D }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_C, UserDataListWithNPTest.ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_CD_C, UserDataListWithNPTest.ASSOC_CD_D }, conf.cellName);
    }

    /**
     * 以下のようなODataスキーマを作成. A(0..1) - B(0..1) | C(1) - A(0..1) | D(*) - A(0..1)
     * A(0..1) - C(1) | B(1) - C(1) | D(*) - B(1) A(0..1) - D(*) | B(1) - D(*) |
     * C(*) - D(*)
     * @param conf
     *            設定ファイル
     */
    public static void createComplexTypeNPSchema(Config conf) {
        // ComplexTypeスキーマを登録する
        createComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_A, "complexType1stA", "etStrProp",
                "etComplexProp", "ct1stStrProp");
        createComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_B, "complexType1stB", "etStrProp",
                "etComplexProp", "ct1stStrProp");
        createComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_C, "complexType1stC", "etStrProp",
                "etComplexProp", "ct1stStrProp");
        createComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_D, "complexType1stD", "etStrProp",
                "etComplexProp", "ct1stStrProp");

        // AssociationEndを登録する
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AB_A, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AC_A, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AD_A, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_AB_B, UserDataListWithNPTest.MULTI_ZERO_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_AC_C, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_AD_D, UserDataListWithNPTest.MULTI_AST, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BC_B, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_BC_C, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BD_B, UserDataListWithNPTest.MULTI_ONE, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_BD_D, UserDataListWithNPTest.MULTI_AST, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_CD_C, UserDataListWithNPTest.MULTI_AST, conf.cellName);
        associationEndPost(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_CD_D, UserDataListWithNPTest.MULTI_AST, conf.cellName);

        // AssociationEndの関係を結ぶ
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_A, UserDataListWithNPTest.CT_ENTITY_TYPE_B },
                new String[] {UserDataListWithNPTest.ASSOC_AB_A, UserDataListWithNPTest.ASSOC_AB_B }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_A, UserDataListWithNPTest.CT_ENTITY_TYPE_C },
                new String[] {UserDataListWithNPTest.ASSOC_AC_A, UserDataListWithNPTest.ASSOC_AC_C }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_A, UserDataListWithNPTest.CT_ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_AD_A, UserDataListWithNPTest.ASSOC_AD_D }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_B, UserDataListWithNPTest.CT_ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_BD_B, UserDataListWithNPTest.ASSOC_BD_D }, conf.cellName);
        createAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_C, UserDataListWithNPTest.CT_ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_CD_C, UserDataListWithNPTest.ASSOC_CD_D }, conf.cellName);
    }

    /**
     * 指定したCellにユーザーデータを作成する.
      * @param cellName
     *            セル名
     */
    @SuppressWarnings("unchecked")
    public static void createUserDatas(String cellName) {
        JSONObject body;
        String userDataId;
        // ユーザーデータの登録(10個)
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue" + i);
            body.put("sample", "sample" + i);
            body.put("test", "test" + i);
            body.put("number", i);
            body.put("decimal", i + DECIMAL);
            createUserData(cellName, "box1", TEST_ODATA, "SalesDetail", body);
        }

        // userdata001のdynamicPropertyのみ違うデータ
        userDataId = "userdata001_dynamicProperty2";
        body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue2");
        body.put("sample", "sample1");
        body.put("test", "test1");
        body.put("decimal", 1 + DECIMAL);
        body.put("truth", true);
        createUserData(cellName, "box1", TEST_ODATA, "SalesDetail", body);

        // userdata001のsampleのみ違うデータ
        userDataId = "userdata001_sample2";
        body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue1");
        body.put("sample", "sample2");
        body.put("test", "test1");
        body.put("number", null);
        body.put("decimal", 1 + DECIMAL);
        body.put("truth", false);
        createUserData(cellName, "box1", TEST_ODATA, "SalesDetail", body);

        // userdata001のtestのみ違うデータ
        userDataId = "userdata001_test2";
        body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue1");
        body.put("sample", "sample1");
        body.put("test", "test2");
        body.put("number", 1);
        body.put("decimal", 1 + DECIMAL);
        createUserData(cellName, "box1", TEST_ODATA, "SalesDetail", body);

        // 部分一致検索用データの登録
        body = new JSONObject();
        userDataId = "userdata100";
        body.put("__id", userDataId);
        body.put("japanese", "部分一致検索テスト");
        body.put("english", "Search substringof Test");
        body.put("test", "atest");
        body.put("number", 1);
        body.put("decimal", 1 + DECIMAL);
        createUserDataWithDcClient(cellName, "box1", TEST_ODATA, "SalesDetail", body);

        body = new JSONObject();
        userDataId = "userdata101";
        body.put("__id", userDataId);
        body.put("japanese", "部分一致検索漢字のテスト");
        body.put("english", "Test Substringof Search value");
        body.put("test", "btest");
        body.put("number", 1);
        body.put("decimal", 1 + DECIMAL);
        createUserDataWithDcClient(cellName, "box1", TEST_ODATA, "SalesDetail", body);

        body = new JSONObject();
        userDataId = "userdata102";
        body.put("__id", userDataId);
        body.put("japanese", "部分一致けんさくのテスト");
        body.put("english", "test substringof search");
        body.put("test", "ctest");
        body.put("number", 1);
        body.put("decimal", 1 + DECIMAL);
        createUserDataWithDcClient(cellName, "box1", TEST_ODATA, "SalesDetail", body);

        body = new JSONObject();
        userDataId = "userdata001";
        body.put("__id", userDataId);
        int maxPropNum = PersoniumUnitConfig.getMaxPropertyCountInEntityType();
        for (int i = 0; i < maxPropNum; i++) {
            body.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }
        createUserDataWithDcClient(cellName, "box1", TEST_ODATA, TEST_ENTITYTYPE_MDP, body);
        deleteUserData(cellName, "box1", TEST_ODATA, TEST_ENTITYTYPE_MDP, userDataId);
    }

    /**
     * 指定したCellに検索用のユーザーデータを作成する.
      * @param cellName
     *            セル名
     */
    @SuppressWarnings("unchecked")
    public static void createUserDatasToSearchOdata(String cellName) {
        JSONObject body;
        String userDataId;

        // 文字列エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", "");
            body.put("property3", "Value" + System.currentTimeMillis());
            if (i % 2 == 0) {
                body.put("property4", "Value1");
            } else {
                body.put("property4", "Value2");
            }
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "string", body);
        }

        // 文字列配列エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", Arrays.asList());
            body.put("property3", Arrays.asList("Value" + System.currentTimeMillis()));
            body.put("property4",
                    Arrays.asList("Value" + System.currentTimeMillis(), "Value" + System.currentTimeMillis()));
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "stringList", body);
        }

        // 整数値エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", 0);
            body.put("property3", i + 1);
            body.put("property4", i - 1);
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "int", body);
        }

        // 整数値配列エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", Arrays.asList());
            body.put("property3", Arrays.asList(i));
            body.put("property4", Arrays.asList(i - 1, i + 1));
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "intList", body);
        }

        // 小数値エンティティに登録
        final float num = 10.5F;
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", 0.0F);
            body.put("property3", (i + 1) * num);
            body.put("property4", (i - 1) * num);
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "single", body);
        }

        // 小数値配列エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", Arrays.asList());
            body.put("property3", Arrays.asList(i * num));
            body.put("property4", Arrays.asList((i - 1) * num, (i + 1) * num));
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "singleList", body);
        }

        // 真偽値エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", true);
            if (i % 2 == 0) {
                body.put("property3", false);
            } else {
                body.put("property3", true);
            }
            body.put("property4", true);
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "boolean", body);
        }

        // 真偽値配列エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", Arrays.asList());
            body.put("property3", Arrays.asList(false));
            body.put("property4", Arrays.asList(false, true));
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "booleanList", body);
        }

        // 日付型エンティティに登録
        for (int i = 0; i < NUM_USERDATA; i++) {
            body = new JSONObject();
            userDataId = "userdata00" + i;
            body.put("__id", userDataId);
            body.put("property1", null);
            body.put("property2", String.format("/Date(1398991538%03d)/", i));
            body.put("property3", "/Date(1398991538550)/");
            body.put("property4", "/Date(1398991538551)/");
            createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "datetime", body);
        }

        // 動的プロパティ用エンティティに登録
        body = new JSONObject();
        userDataId = "userdata000";
        body.put("__id", userDataId);
        body.put("property1", "Value2");
        body.put("property2", 0.0);
        createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "dynamic", body);

        body = new JSONObject();
        userDataId = "userdata001";
        body.put("__id", userDataId);
        body.put("property1", "Value1");
        body.put("property2", 2);
        createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "dynamic", body);

        body = new JSONObject();
        userDataId = "userdata002";
        body.put("__id", userDataId);
        body.put("property1", "Value1");
        body.put("property2", num);
        createUserData(cellName, TEST_BOX1, SEARCH_ODATA, "dynamic", body);
    }

    /**
     * ユーザーデータを１件作成する.
     * @param cellName
     *            セル名
     * @param boxName
     *            ボックス名
     * @param colName
     *            コレクション名
     * @param entityTypeName
     *            エンティティ名
     * @param body
     *            リクエストボディ
     * @return ユーザーデータ作成時のレスポンスオブジェクト
     */
    private static PersoniumResponse createUserDataWithDcClient(String cellName, String boxName, String colName,
            String entityTypeName, JSONObject body) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // リクエストを実行する
        try {
            res = rest.post(UrlUtils.userData(cellName, boxName, colName, entityTypeName), body.toJSONString(),
                    requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        return res;
    }

    /**
     * ユーザーデータを１件作成する.
     * @param cellName
     *            セル名
     * @param boxName
     *            ボックス名
     * @param colName
     *            コレクション名
     * @param entityTypeName
     *            エンティティ名
     * @param body
     *            リクエストボディ
     */
    private static void createUserData(String cellName, String boxName, String colName, String entityTypeName,
            JSONObject body) {
        Http.request("box/odatacol/create.txt").with("cell", cellName).with("box", boxName).with("collection", colName)
                .with("entityType", entityTypeName).with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + PersoniumUnitConfig.getMasterToken())
                .with("body", body.toJSONString()).returns()
                .debug();
    }

    /**
     * 指定したCellのユーザーデータを削除する.
     * @param cellName
     *            セル名
     */
    public static void deleteUserDatas(String cellName) {
        String userDataId;
        for (int i = 0; i < NUM_USERDATA; i++) {
            userDataId = "userdata00" + i;
            deleteUserData(cellName, "box1", TEST_ODATA, "SalesDetail", userDataId);
        }
        userDataId = "userdata001_dynamicProperty2";
        deleteUserData(cellName, "box1", TEST_ODATA, "SalesDetail", userDataId);
        userDataId = "userdata001_sample2";
        deleteUserData(cellName, "box1", TEST_ODATA, "SalesDetail", userDataId);
        userDataId = "userdata001_test2";
        deleteUserData(cellName, "box1", TEST_ODATA, "SalesDetail", userDataId);
        userDataId = "userdata100";
        deleteUserData(cellName, "box1", TEST_ODATA, "SalesDetail", userDataId);
        userDataId = "userdata101";
        deleteUserData(cellName, "box1", TEST_ODATA, "SalesDetail", userDataId);
        userDataId = "userdata102";
        deleteUserData(cellName, "box1", TEST_ODATA, "SalesDetail", userDataId);

    }

    /**
     * ユーザーデータを１件削除する.
     * @param cellName
     *            セル名
     * @param boxName
     *            ボックス名
     * @param colName
     *            コレクション名
     * @param entityTypeName
     *            エンティティ名
     * @param userDataId
     *            削除対象ID
     */
    private static void deleteUserData(String cellName, String boxName, String colName, String entityTypeName,
            String userDataId) {
        // リクエスト実行
        Http.request("box/odatacol/delete.txt").with("cell", cellName).with("box", boxName).with("collection", colName)
                .with("entityType", entityTypeName).with("id", userDataId)
                .with("token", PersoniumUnitConfig.getMasterToken()).with("ifMatch", "*").returns();
    }

    /**
     * コレクションへのリソースPUT.
     * @param path
     * @param name
     * @return TResponse
     */
    private void resourcesPut(final String path, final String cellPath) {
        Http.request("box/dav-put.txt").with("cellPath", cellPath).with("path", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("box", "box1").with("contentType", "text/plan")
                .with("source", "hoge").returns();
    }

    /**
     * EntityTypeのPOST.
     * @param path
     *            コレクションパス
     * @param boxName
     *            ボックスパス
     * @param name
     *            EntitySet名
     * @param cellPath
     *            セル名
     * @return レスポンス
     */
    public static TResponse entityTypePost(final String path, final String boxName, final String name,
            final String cellPath) {
        TResponse tresponse = Http.request("box/entitySet-post.txt").with("cellPath", cellPath).with("boxPath", boxName)
                .with("odataSvcPath", path).with("token", "Bearer " + AbstractCase.MASTER_TOKEN_NAME)
                .with("accept", "application/xml").with("Name", name).returns();
        return tresponse;
    }

    /**
     * EntityTypeのDELETE.
     * @param path
     *            コレクションパス
     * @param name
     *            EntitySet名
     * @param cellPath
     *            セル名
     * @param boxPath
     *            ボックス名
     * @return レスポンス
     */
    public static TResponse entityTypeDelete(final String path, final String name, final String cellPath,
            final String boxPath) {

        TResponse tresponse = Http.request("box/entitySet-delete.txt").with("cellPath", cellPath)
                .with("boxPath", boxPath).with("odataSvcPath", path).with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("accept", "application/xml").with("Name", name).returns();
        return tresponse;
    }

    /**
     * すべてのEntityTypeのDELETE.
      * @param path
     *            コレクションパス
     * @param cellPath
     *            セル名
     * @return レスポンス
     */
    public static TResponse allEntityTypeDelete(final String path, final String cellPath) {

        // EntityType全件取得
        TResponse res = Http.request("box/entitySet-query.txt").with("cellPath", cellPath).with("odataSvcPath", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("accept", MediaType.APPLICATION_JSON).returns();

        JSONObject d = (JSONObject) res.bodyAsJson().get("d");
        if (d != null) {
            JSONArray results = (JSONArray) d.get("results");
            for (Object result : results) {
                JSONObject account = (JSONObject) result;
                String entityName = (String) account.get("Name");
                entityTypeDelete(path, entityName, cellPath, TEST_BOX1);
            }
        }
        return res;
    }

    /**
     * AssociationEndの登録.
      * @param path
     *            コレクションパス
     * @param entityTypeName
     *            エンティティタイプ名
     * @param name
     *            AssociationEnd名
     * @param multiplicity
     *            多重度
     * @param cellPath
     *            セル名
     * @return レスポンス
     */
    public static TResponse associationEndPost(final String path, final String entityTypeName, final String name,
            final String multiplicity, final String cellPath) {
        final String boxName = "box1";
        TResponse tresponse = Http.request("box/associationEnd-post.txt").with("cell", cellPath).with("box", boxName)
                .with("odataSvcPath", path).with("entityTypeName", entityTypeName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("accept", "application/json").with("Name", name)
                .with("Multiplicity", multiplicity).returns();
        return tresponse;
    }

    /**
     * AssociationEndの削除.
      * @param path
     *            コレクションパス
     * @param entityTypeName
     *            エンティティタイプ名
     * @param name
     *            AssociationEnd名
     * @param cellPath
     *            セル名
     * @return レスポンス
     */
    public static TResponse associationEndDelete(final String path, final String entityTypeName, final String name,
            final String cellPath) {
        final String boxName = "box1";
        TResponse tresponse = Http.request("box/associationEnd-delete.txt").with("cell", cellPath).with("box", boxName)
                .with("odataSvcPath", path).with("entityTypeName", entityTypeName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("accept", "application/json").with("name", name)
                .with("ifMatch", "*").returns();
        return tresponse;
    }

    /**
     * AssociationEndのlink作成.
      * @param path
     *            コレクションパス
     * @param entityTypeName
     *            エンティティタイプ
     * @param name
     *            AssociationEnd名
     * @param cellPath
     *            セルY
     * @return レスポンス
     */
    public static TResponse createAssociationEndLink(final String path, final String[] entityTypeName,
            final String[] name, final String cellPath) {
        final String boxName = "box1";
        TResponse tresponse = Http.request("box/associationEnd-createLink.txt")
                .with("baseUrl", UrlUtils.cellRoot(cellPath)).with("cell", cellPath).with("box", boxName)
                .with("odataSvcPath", path).with("entityTypeName", entityTypeName[0])
                .with("linkEntityTypeName", entityTypeName[1]).with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("accept", "application/json").with("name", name[0]).with("linkName", name[1]).returns();
        return tresponse;
    }

    /**
     * AssociationEndのlink削除.
      * @param path
     *            コレクションパス
     * @param entityTypeName
     *            エンティティタイプ
     * @param name
     *            AssociationEnd名
     * @param cellPath
     *            セル
     * @param boxName
     *            ボックス名
     * @return レスポンス
     */
    public static TResponse deleteAssociationEndLink(final String path, final String[] entityTypeName,
            final String[] name, final String cellPath, final String boxName) {
        String key = "Name='" + name[0] + "',_EntityType.Name='" + entityTypeName[0] + "'";
        String navKey = "Name='" + name[1] + "',_EntityType.Name='" + entityTypeName[1] + "'";

        TResponse tresponse = Http.request("box/associationEnd-deleteLink.txt").with("cell", cellPath)
                .with("box", boxName).with("odataSvcPath", path).with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("key", key).with("navKey", navKey).returns().debug();
        return tresponse;
    }

    /**
     * Cell作成.
      * @param cellName
     *            Cell名
     * @return Cell作成時のレスポンスオブジェクト
     */
    @SuppressWarnings("unchecked")
    final PersoniumResponse createCell(final Config config) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        // Owner指定があればセット
        String owner = config.owner;
        if (owner != null) {
            requestheaders.put(CommonUtils.HttpHeaders.X_PERSONIUM_UNIT_USER, owner);
        }

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", config.cellName);
        String data = requestBody.toJSONString();

        // リクエスト
        try {
            res = rest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME), data, requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        return res;
    }

    /**
     * ExtCellの作成.
      * @param conf
     *            設定情報
     */
    private void createExtCell(Config conf) {

        for (String url : conf.extCellUrl) {
            Http.request("cell/extCell-create.txt").with("cellPath", conf.cellName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME).with("accept", "application/xml").with("url", url)
                    .returns();
        }
    }

    /**
     * テスト環境の削除.
      * @param conf
     *            設定情報
     */
    private void delete(Config conf) {
        // セル削除
        cellBulkDeletion(conf.cellName);
    }

    /**
     * セル一括削除.
      * @param cellName
     *            セル名
     */
    public static void cellBulkDeletion(String cellName) {
        // セルの一括削除APIを実行する
        PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).header("X-Personium-Recursive", "true");
        request(request);
        // Sleep 1 second for asynchronous processing.
        try {
            Thread.sleep(WAIT_TIME_FOR_BULK_DELETE);
        } catch (InterruptedException e) {
            System.out.println("");
        }
    }

    /**
     * 最大プロパティ数テスト用コレクションのスキーマ削除.
      * @param cellName
     *            セル名
     */
    public static void deleteMaxPropTestCollectionSchema(String cellName) {
        // AssociationEndの$links削除
        deleteAssociationEndLink(TEST_ODATA, new String[] {TEST_ENTITYTYPE_M1, TEST_ENTITYTYPE_MDP },
                new String[] {"Multi_1_to_MaxDynamicProp", "MaxDynamicProp_to_Multi_1" }, cellName, TEST_BOX1);
        deleteAssociationEndLink(TEST_ODATA, new String[] {TEST_ENTITYTYPE_MN, TEST_ENTITYTYPE_MDP },
                new String[] {"Multi_N_to_MaxDynamicProp", "MaxDynamicProp_to_Multi_N" }, cellName, TEST_BOX1);

        // AssociationEndの削除
        associationEndDelete(TEST_ODATA, TEST_ENTITYTYPE_M1, "Multi_1_to_MaxDynamicProp", cellName);
        associationEndDelete(TEST_ODATA, TEST_ENTITYTYPE_MN, "Multi_N_to_MaxDynamicProp", cellName);
        associationEndDelete(TEST_ODATA, TEST_ENTITYTYPE_MDP, "MaxDynamicProp_to_Multi_1", cellName);
        associationEndDelete(TEST_ODATA, TEST_ENTITYTYPE_MDP, "MaxDynamicProp_to_Multi_N", cellName);

        entityTypeDelete(TEST_ODATA, TEST_ENTITYTYPE_MDP, cellName, TEST_BOX1);
        entityTypeDelete(TEST_ODATA, TEST_ENTITYTYPE_MN, cellName, TEST_BOX1);
        entityTypeDelete(TEST_ODATA, TEST_ENTITYTYPE_M1, cellName, TEST_BOX1);

    }

    /**
     * テスト用コレクションのスキーマ削除.
      * @param cellName
     *            セル名
     */
    public static void deleteTestCollectionSchema(String cellName) {
        deleteAssociationEndLink(TEST_ODATA, new String[] {"SalesDetail", "Sales" },
                new String[] {"salesDetail2sales", "sales2salesDetail" }, cellName, TEST_BOX1);

        deleteAssociationEndLink(TEST_ODATA, new String[] {"Product", "Sales" },
                new String[] {"product2Sales", "sales2product" }, cellName, TEST_BOX1);

        deleteAssociationEndLink(TEST_ODATA, new String[] {"Supplier", "Product" },
                new String[] {"supplier2product", "product2supplier" }, cellName, TEST_BOX1);

        deleteAssociationEndLink(TEST_ODATA, new String[] {"Sales", "Supplier" },
                new String[] {"sales2supplier", "supplier2sales" }, cellName, TEST_BOX1);

        deleteAssociationEndLink(TEST_ODATA, new String[] {"Price", "Sales" },
                new String[] {"price2sales", "sales2price" }, cellName, TEST_BOX1);

        associationEndDelete(TEST_ODATA, "Sales", "sales2salesDetail", cellName);
        associationEndDelete(TEST_ODATA, "SalesDetail", "salesDetail2sales", cellName);
        associationEndDelete(TEST_ODATA, "Product", "product2Sales", cellName);
        associationEndDelete(TEST_ODATA, "Sales", "sales2product", cellName);
        associationEndDelete(TEST_ODATA, "Product", "product2category", cellName);
        associationEndDelete(TEST_ODATA, "Category", "category2product", cellName);
        associationEndDelete(TEST_ODATA, "Supplier", "supplier2product", cellName);
        associationEndDelete(TEST_ODATA, "Product", "product2supplier", cellName);
        associationEndDelete(TEST_ODATA, "Sales", "sales2supplier", cellName);
        associationEndDelete(TEST_ODATA, "Supplier", "supplier2sales", cellName);
        associationEndDelete(TEST_ODATA, "Sales", "sales2price", cellName);
        associationEndDelete(TEST_ODATA, "Price", "price2sales", cellName);

        entityTypeDelete(TEST_ODATA, "Category", cellName, TEST_BOX1);
        entityTypeDelete(TEST_ODATA, "Price", cellName, TEST_BOX1);
        entityTypeDelete(TEST_ODATA, "Product", cellName, TEST_BOX1);
        entityTypeDelete(TEST_ODATA, "Sales", cellName, TEST_BOX1);
        entityTypeDelete(TEST_ODATA, "SalesDetail", cellName, TEST_BOX1);
        entityTypeDelete(TEST_ODATA, "Supplier", cellName, TEST_BOX1);
    }

    /**
     * NPテスト用ODataスキーマを削除.
      * @param conf
     *            設定ファイル
     */
    public static void deleteComplexTypeNPSchema(Config conf) {
        // AssociationEndの$linksを削除
        String key = "Name='" + UserDataListWithNPTest.ASSOC_AB_A + "',_EntityType.Name='"
                + UserDataListWithNPTest.CT_ENTITY_TYPE_A + "'";
        String navKey = "Name='" + UserDataListWithNPTest.ASSOC_AB_B + "',_EntityType.Name='"
                + UserDataListWithNPTest.CT_ENTITY_TYPE_B + "'";
        AssociationEndUtils.deleteLink(conf.cellName, UserDataListWithNPTest.ODATA_COLLECTION, TEST_BOX1, key, navKey,
                -1);

        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_A, UserDataListWithNPTest.CT_ENTITY_TYPE_B },
                new String[] {UserDataListWithNPTest.ASSOC_AB_A, UserDataListWithNPTest.ASSOC_AB_B }, conf.cellName,
                TEST_BOX1);
        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_A, UserDataListWithNPTest.CT_ENTITY_TYPE_C },
                new String[] {UserDataListWithNPTest.ASSOC_AC_A, UserDataListWithNPTest.ASSOC_AC_C }, conf.cellName,
                TEST_BOX1);
        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_A, UserDataListWithNPTest.CT_ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_AD_A, UserDataListWithNPTest.ASSOC_AD_D }, conf.cellName,
                TEST_BOX1);
        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_B, UserDataListWithNPTest.CT_ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_BD_B, UserDataListWithNPTest.ASSOC_BD_D }, conf.cellName,
                TEST_BOX1);
        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.CT_ENTITY_TYPE_C, UserDataListWithNPTest.CT_ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_CD_C, UserDataListWithNPTest.ASSOC_CD_D }, conf.cellName,
                TEST_BOX1);

        // AssociationEndを削除する
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AB_A, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AC_A, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AD_A, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_AB_B, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_AC_C, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_AD_D, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BC_B, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_BC_C, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BD_B, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_BD_D, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_CD_C, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_CD_D, conf.cellName);

        // ComplexTypeスキーマを削除する
        deleteComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_A, "complexType1stA", "etStrProp",
                "etComplexProp", "ct1stStrProp");
        deleteComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_B, "complexType1stB", "etStrProp",
                "etComplexProp", "ct1stStrProp");
        deleteComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_C, "complexType1stC", "etStrProp",
                "etComplexProp", "ct1stStrProp");
        deleteComplexTypeSchema(conf, UserDataListWithNPTest.CT_ENTITY_TYPE_D, "complexType1stD", "etStrProp",
                "etComplexProp", "ct1stStrProp");

        // EntitySetを削除する
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_A,
                conf.cellName, TEST_BOX1);
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_B,
                conf.cellName, TEST_BOX1);
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_C,
                conf.cellName, TEST_BOX1);
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.CT_ENTITY_TYPE_D,
                conf.cellName, TEST_BOX1);
    }

    /**
     * NPテスト用のODATACollectionを削除する.
      * @param conf
     *            設定ファイル
     */
    public static void deleteNPTestCollection(Config conf) {

        // AssociationEndの$linksを削除
        String key = "Name='" + UserDataListWithNPTest.ASSOC_AB_A + "',_EntityType.Name='"
                + UserDataListWithNPTest.ENTITY_TYPE_A + "'";
        String navKey = "Name='" + UserDataListWithNPTest.ASSOC_AB_B + "',_EntityType.Name='"
                + UserDataListWithNPTest.ENTITY_TYPE_B + "'";
        AssociationEndUtils.deleteLink(conf.cellName, UserDataListWithNPTest.ODATA_COLLECTION, TEST_BOX1, key, navKey,
                -1);
        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_A, UserDataListWithNPTest.ENTITY_TYPE_B },
                new String[] {UserDataListWithNPTest.ASSOC_AB_A, UserDataListWithNPTest.ASSOC_AB_B }, conf.cellName,
                TEST_BOX1);

        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_A, UserDataListWithNPTest.ENTITY_TYPE_C },
                new String[] {UserDataListWithNPTest.ASSOC_AC_A, UserDataListWithNPTest.ASSOC_AC_C }, conf.cellName,
                TEST_BOX1);

        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_A, UserDataListWithNPTest.ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_AD_A, UserDataListWithNPTest.ASSOC_AD_D }, conf.cellName,
                TEST_BOX1);

        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_B, UserDataListWithNPTest.ENTITY_TYPE_C },
                new String[] {UserDataListWithNPTest.ASSOC_BC_B, UserDataListWithNPTest.ASSOC_BC_C }, conf.cellName,
                TEST_BOX1);

        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_B, UserDataListWithNPTest.ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_BD_B, UserDataListWithNPTest.ASSOC_BD_D }, conf.cellName,
                TEST_BOX1);

        deleteAssociationEndLink(UserDataListWithNPTest.ODATA_COLLECTION,
                new String[] {UserDataListWithNPTest.ENTITY_TYPE_C, UserDataListWithNPTest.ENTITY_TYPE_D },
                new String[] {UserDataListWithNPTest.ASSOC_CD_C, UserDataListWithNPTest.ASSOC_CD_D }, conf.cellName,
                TEST_BOX1);

        // AssociationEndを削除する
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AB_A, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AC_A, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_A,
                UserDataListWithNPTest.ASSOC_AD_A, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_AB_B, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_AC_C, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_AD_D, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BC_B, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_BC_C, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_B,
                UserDataListWithNPTest.ASSOC_BD_B, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_BD_D, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_C,
                UserDataListWithNPTest.ASSOC_CD_C, conf.cellName);
        associationEndDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_D,
                UserDataListWithNPTest.ASSOC_CD_D, conf.cellName);

        // EntitySetを削除する
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_A, conf.cellName,
                TEST_BOX1);
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_B, conf.cellName,
                TEST_BOX1);
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_C, conf.cellName,
                TEST_BOX1);
        entityTypeDelete(UserDataListWithNPTest.ODATA_COLLECTION, UserDataListWithNPTest.ENTITY_TYPE_D, conf.cellName,
                TEST_BOX1);

        // ODATACollectionを削除する
        deleteCol(UserDataListWithNPTest.ODATA_COLLECTION, conf.cellName);
    }

    /**
     * コレクションの削除.
      * @param path
     *            コレクションパス
     * @param cellPath
     *            セル名
     */
    public static void deleteCol(final String path, final String cellPath) {
        Http.request("box/delete-col.txt").with("cellPath", cellPath).with("box", "box1").with("path", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).returns();
    }

    /**
     * アカウントの一覧取得用のベースデータ作成.。
     * @param cellName
     */
    private void createAccountforList(String cellName) {
        final String namePrefix = "AccountListUser";
        final int accountNum = 10;
        final int noTimeAccountNum = 5;
        String roleName = "accountListViaNPRoleName";

        // Accountに紐づくRoleを作成する
        RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, HttpStatus.SC_CREATED);

        // 1st block.
        // single IP address as "IPAddressRange"
        for (int i = 1; i <= accountNum; i++) {
            String userName = String.format("%s_%03d", namePrefix, i);
            String body = "{\"Name\":\"" + userName + "\", \"IPAddressRange\":\"192.1." + i + ".1\"}";
            AccountUtils.createViaNPNonCredential(cellName, AbstractCase.MASTER_TOKEN_NAME, "Role", roleName, body,
                    HttpStatus.SC_CREATED);
        }

        // 2nd block.
        // mulutiple IP address as "IPAddressRange"
        for (int i = accountNum + 1; i <= accountNum * 2; i++) {
            String userName = String.format("%s_%03d", namePrefix, i);
            String body = "{\"Name\":\"" + userName + "\", \"IPAddressRange\":\"192.1." + i + ".0/24,192.2.2.254\"}";
            AccountUtils.createViaNPNonCredential(cellName, AbstractCase.MASTER_TOKEN_NAME, "Role", roleName, body,
                    HttpStatus.SC_CREATED);
        }

        // Account without "IPAddressRange"
        for (int i = accountNum * 2 + 1; i <= accountNum * 2 + noTimeAccountNum; i++) {
            String userName = String.format("%s_%03d", namePrefix, i);
            String body = "{\"Name\":\"" + userName + "\"}";
            AccountUtils.createViaNPNonCredential(cellName, AbstractCase.MASTER_TOKEN_NAME, "Role", roleName, body,
                    HttpStatus.SC_CREATED);
        }
    }

    /**
     * PROPPATCH作成.
      * @param path
     *            パス名
     * @return boxURL
     */
    private TResponse createPatch2(final String path, final String cellPath) {
        TResponse tresponseWebDav = null;
        tresponseWebDav = Http.request("box/proppatch-class.txt").with("path", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("cellPath", cellPath).with("name1", "Test User1")
                .with("src1", "hoge").with("name2", "Test User2").with("src2", "fuga").with("name3", "Test User3")
                .with("src3", "boy").returns();
        tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);
        return tresponseWebDav;
    }

    /**
     * ExtCellとロールの結びつけ.
      * @param cellName
     *            結びつけるセル名
     * @param cellPath
     *            リクエストを投げるセル
     * @param roleUrl
     *            結びつけるロールURL
     * @return
     */
    private TResponse linkExtCelltoRole(final String cellName, final String cellPath, final String roleUrl) {
        TResponse tresponseWebDav = null;
        // DAVコレクションの作成
        tresponseWebDav = Http.request("cell/link-extCell-role.txt").with("cellPath", cellPath)
                .with("cellName", cellName).with("token", AbstractCase.MASTER_TOKEN_NAME).with("roleUrl", roleUrl)
                .returns();
        tresponseWebDav.statusCode(HttpStatus.SC_NO_CONTENT);
        return tresponseWebDav;
    }

    /**
     * 環境構築設定管理.
     */
    private class Config {
        private String cellName = "";
        private List<String> extCellUrl = new ArrayList<String>();
        private String owner = null;

        private List<AccountConfig> account = new ArrayList<AccountConfig>();

        private List<RoleConfig> role = new ArrayList<RoleConfig>();

        private List<BoxConfig> box = new ArrayList<BoxConfig>();

        private List<RelationConfig> relation = new ArrayList<RelationConfig>();

        private List<ExtRoleConfig> extRole = new ArrayList<ExtRoleConfig>();

        private List<RuleConfig> rule = new ArrayList<RuleConfig>();
    }

    /**
     * アカウント設定管理.
     */
    private class AccountConfig {
        private String accountName = "";
        private String accountPass = "";
    }

    /**
     * ロール設定管理.
     */
    private class RoleConfig {
        private String roleName = "";
        private List<AccountConfig> linkAccounts = new ArrayList<AccountConfig>();
        private List<ExtRoleConfig> linkExtRole = new ArrayList<ExtRoleConfig>();
    }

    /**
     * BOX設定管理.
     */
    private class BoxConfig {
        private String boxName = "";
        private String boxSchema = "";
        private List<DavSvcCollectionConfig> davCol = new ArrayList<DavSvcCollectionConfig>();
        private List<OdataSvcCollectionConfig> odataCol = new ArrayList<OdataSvcCollectionConfig>();
        private List<SvcCollectionConfig> svcCol = new ArrayList<SvcCollectionConfig>();
    }

    /**
     * サービスコレクション設定管理.
     */
    @SuppressWarnings("unused")
    private class SvcCollectionConfig {
        private String colName = "";
        private ProppatchConfig patch = null;
    }

    /**
     * Davコレクション設定管理.
     */
    @SuppressWarnings("unused")
    private class DavSvcCollectionConfig {
        private String colName = "";
        private ProppatchConfig patch = null;
    }

    /**
     * Odataコレクション設定管理.
     */
    @SuppressWarnings("unused")
    private class OdataSvcCollectionConfig {
        private String colName = "";
        private ProppatchConfig patch = null;
    }

    /**
     * PROPPATCH設定管理.
     */
    @SuppressWarnings("unused")
    private class ProppatchConfig {
        private String patchName = "";
    }

    /**
     * 関係設定管理.
     */
    private class RelationConfig {
        private String name = "";
        private String boxName = "";
        private List<String> linkExtCell = new ArrayList<String>();

        @SuppressWarnings("unchecked")
        public JSONObject toJson() {
            JSONObject relationJson = new JSONObject();
            relationJson.put("Name", name);
            relationJson.put("_Box.Name", boxName);
            return relationJson;
        }
    }

    /**
     * ExtRole設定管理.
     */
    private class ExtRoleConfig {
        private String extRole = "";
        private String relationName = "";
        private String relationBoxName = "";

        @SuppressWarnings("unchecked")
        public JSONObject toJson() {
            JSONObject extRoleJson = new JSONObject();
            extRoleJson.put("ExtRole", extRole);
            extRoleJson.put("_Relation.Name", relationName);
            extRoleJson.put("_Relation._Box.Name", relationBoxName);
            return extRoleJson;
        }
    }

    /**
     * Rule config.
     */
    private class RuleConfig {
        private String name = null;
        private String boxName = null;
        private Boolean external = false;
        private String action = null;

        @SuppressWarnings("unchecked")
        public JSONObject toJson() {
            JSONObject ruleJson = new JSONObject();
            ruleJson.put("Name", name);
            ruleJson.put("_Box.Name", boxName);
            ruleJson.put("EventExternal", external);
            ruleJson.put("Action", action);
            return ruleJson;
        }
    }

    /**
     * ComplexTypeスキーマを作成する.
      * @param conf
     *            設定ファイル
     * @param entityTypeName
     *            エンティティタイプ名
     * @param complexTypeName
     *            コンプレックスタイプ名
     * @param propertyName
     *            プロパティ名
     * @param complexTypePropertyName
     *            コンプレックスタイププロパティ名
     * @param innnerComplexTypePropertyName
     *            インナーコンプレックスタイププロパティ名
     */
    protected static void createComplexTypeSchema(Config conf, String entityTypeName, String complexTypeName,
            String propertyName, String complexTypePropertyName, String innnerComplexTypePropertyName) {
        // EntityType作成
        EntityTypeUtils.create(conf.cellName, AbstractCase.MASTER_TOKEN_NAME, UserDataListWithNPTest.ODATA_COLLECTION,
                entityTypeName, HttpStatus.SC_CREATED);

        // ComplexType作成
        UserDataUtils.createComplexType(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION,
                complexTypeName);

        // Property作成
        UserDataUtils.createProperty(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION, propertyName,
                entityTypeName, EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null, false, null);
        UserDataUtils.createProperty(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION,
                complexTypePropertyName, entityTypeName, complexTypeName, false, null, null, false, null);

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION,
                innnerComplexTypePropertyName, complexTypeName, EdmSimpleType.STRING.getFullyQualifiedTypeName(), false,
                null, null);
    }

    /**
     * ComplexTypeスキーマを削除する.
      * @param conf
     *            設定ファイル
     * @param entityTypeName
     *            エンティティタイプ名
     * @param complexTypeName
     *            コンプレックスタイプ名
     * @param propertyName
     *            プロパティ名
     * @param complexTypePropertyName
     *            コンプレックスタイププロパティ名
     * @param innnerComplexTypePropertyName
     *            インナーコンプレックスタイププロパティ名
     */
    public static void deleteComplexTypeSchema(Config conf, String entityTypeName, String complexTypeName,
            String propertyName, String complexTypePropertyName, String innnerComplexTypePropertyName) {
        String ctlocationUrl = UrlUtils.complexType(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION,
                complexTypeName);
        String propStrlocationUrl = UrlUtils.property(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION,
                propertyName, entityTypeName);
        String propCtlocationUrl = UrlUtils.property(conf.cellName, TEST_BOX1, UserDataListWithNPTest.ODATA_COLLECTION,
                complexTypePropertyName, entityTypeName);
        String ctplocationUrl = UrlUtils.complexTypeProperty(conf.cellName, TEST_BOX1,
                UserDataListWithNPTest.ODATA_COLLECTION, innnerComplexTypePropertyName, complexTypeName);

        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(propStrlocationUrl);
        ODataCommon.deleteOdataResource(propCtlocationUrl);
        // 作成したComplexTypePropertyを削除
        ODataCommon.deleteOdataResource(ctplocationUrl);
        // 作成したComplexTypeを削除
        ODataCommon.deleteOdataResource(ctlocationUrl);
    }

    /**
     * EventLogテスト用セルに対してアーカイブログを作成する. <br />
     * 3KByte × 36,000リクエスト = 108,000KByte のデータを投入してアーカイブファイルを１つ作成する
      * @param conf
     *            設定情報
     */
    public void createEventLog(Config conf) {
        final int itemCodeNum = 1024;
        final int loopCount = 36000;
        String jsonBase = "{"
                + "\\\"Type\\\":\\\"%1$s\\\",\\\"Object\\\":\\\"%1$s\\\",\\\"Info\\\":\\\"%1$s\\\"}";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < itemCodeNum; i++) {
            buf.append("a");
        }
        String jsonBody = String.format(jsonBase, buf.toString());
        for (int i = 0; i < loopCount; i++) {
            CellUtils.event(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, conf.cellName, jsonBody);
        }
    }

    /**
     * $filterテスト用セルのスキーマとユーザODataの作成.
      * @param conf
     *            セルのコンテキスト情報
     */
    private void setupFilterTestCell(Config conf) {
        if (TEST_CELL_FILTER.equals(conf.cellName)) {
            String token = AbstractCase.MASTER_TOKEN_NAME;
            String box = "box";
            String col = "odata";
            BoxUtils.create(TEST_CELL_FILTER, "box", token);
            DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, TEST_CELL_FILTER, box, col);
            // 型チェック用
            EntityTypeUtils.create(TEST_CELL_FILTER, token, box, col, "entity", HttpStatus.SC_CREATED);
            createFilterTestProperty("entity", "string", "Edm.String", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("entity", "int32", "Edm.Int32", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("entity", "single", "Edm.Single", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("entity", "double", "Edm.Double", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("entity", "boolean", "Edm.Boolean", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("entity", "datetime", "Edm.DateTime", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("entity", "int32_list", "Edm.Int32", "List", HttpStatus.SC_CREATED);
            createFilterTypeValidateTestUserData();
            // 全般テスト用
            EntityTypeUtils.create(TEST_CELL_FILTER, token, box, col, "filterlist", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "string", "Edm.String", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "int32", "Edm.Int32", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "single", "Edm.Single", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "double", "Edm.Double", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "boolean", "Edm.Boolean", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "datetime", "Edm.DateTime", "None", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "string_list", "Edm.String", "List", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "int32_list", "Edm.Int32", "List", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "single_list", "Edm.Single", "List", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "double_list", "Edm.Double", "List", HttpStatus.SC_CREATED);
            createFilterTestProperty("filterlist", "boolean_list", "Edm.Boolean", "List", HttpStatus.SC_CREATED);
            createFilterTestUserData("filterlist");
        }
    }

    /**
     * $filterテスト用セル配下のEntityTypeに全データ分のPropertyを登録する.
      * @param entity
     *            EntityType名
     * @param name
     *            プロパティ名
     * @param type
     *            プロパティのデータ型(ex. "Edm.Single")
     * @param kind
     *            CollectionKindの値
     * @param code
     *            期待するHTTPステータスコード
     */
    private void createFilterTestProperty(String entity, String name, String type, String kind, int code) {
        PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, TEST_CELL_FILTER, "box", "odata", entity, name, type,
                true, null, kind, false, null, code);
    }

    /**
     * $filter型チェックテスト用EntityTypeにユーザODataを登録する.
     */
    private void createFilterTypeValidateTestUserData() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        try {
            String bodyStr1 = "{\"__id\":\"id1\","
                    + "\"string\":\"string_data\",\"int32\":1111,\"single\":1111.11,\"double\":1111.1111111,"
                    + "\"boolean\":true,\"datetime\":\"/Date(1420589956172)/\","
                    + "\"d_string\":\"string_value\",\"d_int32\":2222,\"d_single\":2222.22,\"d_double\":2222.2222222,"
                    + "\"d_boolean\":true,\"d_datetime\":\"/Date(1410689956172)/\"," + "\"int32_list\":[1,2,3,4,5]}";
            JSONObject body1 = (JSONObject) new JSONParser().parse(new StringReader(bodyStr1));
            UserDataUtils.create(token, HttpStatus.SC_CREATED, body1, TEST_CELL_FILTER, "box", "odata", "entity");

            String bodyStr2 = "{\"__id\":\"id2\","
                    + "\"string\":\"string data\",\"int32\":1111,\"single\":1111.0,\"double\":1111.0,"
                    + "\"boolean\":false,\"datetime\":\"/Date(1420589956173)/\","
                    + "\"d_string\":\"string value\",\"d_int32\":2222,\"d_single\":2222.0,\"d_double\":2222.0,"
                    + "\"d_boolean\":false,\"d_datetime\":\"/Date(1410689956172)/\"," + "\"int32_list\":[3,4,5,6,7]}";
            JSONObject body2 = (JSONObject) new JSONParser().parse(new StringReader(bodyStr2));
            UserDataUtils.create(token, HttpStatus.SC_CREATED, body2, TEST_CELL_FILTER, "box", "odata", "entity");
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /**
     * $filter型チェックテスト用EntityTypeにユーザODataを登録する.
      * @param entity
     *            EntityType名
     */
    private void createFilterTestUserData(String entity) {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final int userDataNum = 10;
        final int intData = 1111;
        final BigDecimal singleData = new BigDecimal(1111.11);
        final BigDecimal doubleData = new BigDecimal(1111111.1111111d);
        final long dateTimeData = 1410689956172L;
        final int dateTimeOffset = 1000000;
        try {
            for (int i = 0; i < userDataNum; i++) {
                StringBuilder sbuf = new StringBuilder();
                sbuf.append(String.format("{\"__id\":\"id_%04d\",", i));
                sbuf.append(String.format("\"string\":\"string %s\",", String.format("%04d", i)));
                sbuf.append(String.format("\"int32\":%d,", intData * i));
                sbuf.append(String.format("\"single\":%f,", singleData.multiply(new BigDecimal(i))));
                sbuf.append(String.format("\"double\":%f,", doubleData.multiply(new BigDecimal(i))));
                sbuf.append(String.format("\"boolean\":%b,", i % 2 == 0));
                sbuf.append(String.format("\"datetime\":\"/Date(%d)/\",", dateTimeData + i * dateTimeOffset));
                sbuf.append(String.format("\"string_list\":%s,", getStringArray(i)));
                sbuf.append(String.format("\"int32_list\":%s,", getIntgerArray(i)));
                sbuf.append(String.format("\"single_list\":%s,", getFloatArray(i)));
                sbuf.append(String.format("\"double_list\":%s,", getDoubleArray(i)));
                sbuf.append(String.format("\"boolean_list\":%s,", getBooleanArray(i)));
                sbuf.append(String.format("}", i % 2 == 0));
                JSONObject body = (JSONObject) new JSONParser().parse(new StringReader(sbuf.toString()));
                UserDataUtils.create(token, HttpStatus.SC_CREATED, body, TEST_CELL_FILTER, "box", "odata", entity);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /**
     * $filterテスト用の配列データ(Edm.String)の文字列表現を生成する.
      * @param index
     *            配列の要素数
     * @return 配列データの文字列表現
     */
    private String getStringArray(int index) {
        StringBuilder sbuf = new StringBuilder("[");
        for (int i = 0; i < index; i++) {
            if (i == 0) {
                sbuf.append(String.format("\"array %04d\"", i));
            } else {
                sbuf.append(String.format(",\"array %04d\"", i));
            }
        }
        sbuf.append("]");
        return sbuf.toString();
    }

    /**
     * $filterテスト用の配列データ(Edm.Int32)の文字列表現を生成する.
      * @param index
     *            配列の要素数
     * @return 配列データの文字列表現
     */
    private String getIntgerArray(int index) {
        final int intData = 1111;
        StringBuilder sbuf = new StringBuilder("[");
        for (int i = 0; i < index; i++) {
            if (i == 0) {
                sbuf.append(String.format("%d", intData * i));
            } else {
                sbuf.append(String.format(",%d", intData * i));
            }
        }
        sbuf.append("]");
        return sbuf.toString();
    }

    /**
     * $filterテスト用の配列データ(Edm.Single)の文字列表現を生成する.
      * @param index
     *            配列の要素数
     * @return 配列データの文字列表現
     */
    private String getFloatArray(int index) {
        final BigDecimal singleData = new BigDecimal(1111.11);
        StringBuilder sbuf = new StringBuilder("[");
        for (int i = 0; i < index; i++) {
            if (i == 0) {
                sbuf.append(String.format("%f", singleData.multiply(new BigDecimal(i))));
            } else {
                sbuf.append(String.format(",%f", singleData.multiply(new BigDecimal(i))));
            }
        }
        sbuf.append("]");
        return sbuf.toString();
    }

    /**
     * $filterテスト用の配列データ(Edm.Double)の文字列表現を生成する.
     * @param index
     *            配列の要素数
     * @return 配列データの文字列表現
     */
    private String getDoubleArray(int index) {
        final BigDecimal doubleData = new BigDecimal(1111111.1111111d);
        StringBuilder sbuf = new StringBuilder("[");
        for (int i = 0; i < index; i++) {
            if (i == 0) {
                sbuf.append(String.format("%f", doubleData.multiply(new BigDecimal(i))));
            } else {
                sbuf.append(String.format(",%f", doubleData.multiply(new BigDecimal(i))));
            }
        }
        sbuf.append("]");
        return sbuf.toString();
    }

    /**
     * $filterテスト用の配列データ(Edm.Boolean)の文字列表現を生成する.
     * @param index
     *            配列の要素数
     * @return 配列データの文字列表現
     */
    private String getBooleanArray(int index) {
        boolean value = (index % 2 == 0);
        StringBuilder sbuf = new StringBuilder("[");
        for (int i = 0; i < index; i++) {
            if (i == 0) {
                sbuf.append(String.format("%b", value));
            } else {
                sbuf.append(String.format(",%b", value));
            }
        }
        sbuf.append("]");
        return sbuf.toString();
    }
}
