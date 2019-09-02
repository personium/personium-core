/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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
package io.personium.core.rule;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.UriBuilder;

import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.rest.RestStatus;
import org.odata4j.core.NamedValue;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OEntityKey.KeyType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.personium.common.es.response.EsClientException;
import io.personium.common.es.response.EsClientException.PersoniumSearchPhaseExecutionException;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.utils.PersoniumThread;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventFactory;
import io.personium.core.event.EventPublisher;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataEntityAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.es.odata.EsQueryHandler;
import io.personium.core.model.impl.es.odata.ODataQueryHandler;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.utils.UriUtils;

/**
 * RuleManager.
 */
public class RuleManager {
    /**
     * Internal class for managing box.
     */
    class BoxInfo {
        String id;
        String name;
        String schema;
        int count;
    }

    /**
     * Internal class for managing rule.
     */
    class RuleInfo {
        Boolean external;
        String subject;
        String type;
        String object;
        String info;
        String action;
        String targeturl;
        String boxname;
        BoxInfo box;
        String name;
        long hitcount;
    }

    private static Logger logger = LoggerFactory.getLogger(RuleManager.class);

    /** EventType definition for rule event. */
    private static final String RULEEVENT_RULE_CREATE =
            PersoniumEventType.cellctl(Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
    private static final String RULEEVENT_RULE_UPDATE =
            PersoniumEventType.cellctl(Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.UPDATE);
    private static final String RULEEVENT_RULE_MERGE =
            PersoniumEventType.cellctl(Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.MERGE);
    private static final String RULEEVENT_RULE_DELETE =
            PersoniumEventType.cellctl(Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.DELETE);
    private static final String RULEEVENT_RULE_LINK_BOX_CREATE =
            PersoniumEventType.cellctlLink(Rule.EDM_TYPE_NAME, Box.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
    private static final String RULEEVENT_RULE_LINK_BOX_DELETE =
            PersoniumEventType.cellctlLink(Rule.EDM_TYPE_NAME, Box.EDM_TYPE_NAME, PersoniumEventType.Operation.DELETE);
    private static final String RULEEVENT_BOX_LINK_RULE_CREATE =
            PersoniumEventType.cellctlLink(Box.EDM_TYPE_NAME, Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
    private static final String RULEEVENT_BOX_LINK_RULE_DELETE =
            PersoniumEventType.cellctlLink(Box.EDM_TYPE_NAME, Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.DELETE);
    private static final String RULEEVENT_RULE_NAVPROP_BOX_CREATE =
            PersoniumEventType.cellctlNavProp(Rule.EDM_TYPE_NAME,
                                              Box.EDM_TYPE_NAME,
                                              PersoniumEventType.Operation.CREATE);
    private static final String RULEEVENT_BOX_NAVPROP_RULE_CREATE =
            PersoniumEventType.cellctlNavProp(Box.EDM_TYPE_NAME,
                                              Rule.EDM_TYPE_NAME,
                                              PersoniumEventType.Operation.CREATE);
    private static final String RULEEVENT_BOX_UPDATE =
            PersoniumEventType.cellctl(Box.EDM_TYPE_NAME, PersoniumEventType.Operation.UPDATE);
    private static final String RULEEVENT_BOX_MERGE =
            PersoniumEventType.cellctl(Box.EDM_TYPE_NAME, PersoniumEventType.Operation.MERGE);
    private static final String RULEEVENT_BOX_RECURSIVE_DELETE =
            PersoniumEventType.box(PersoniumEventType.Operation.DELETE);
    private static final String RULEEVENT_CELL_IMPORT = PersoniumEventType.cell(PersoniumEventType.Operation.IMPORT);

    private static RuleManager instance = null;
    private Optional<TimerRuleManager> timerRuleManager = Optional.empty();
    private Map<String, Map<String, RuleInfo>> rules;
    private Map<String, Map<String, BoxInfo>> boxes;
    private Set<String> loadRuleCells;

    private Object lockObj;
    private Object boxLockObj;

    private ExecutorService pool;

    private EventPublisher ruleEventPublisher;

    /**
     * Constructor.
     */
    private RuleManager() {
        rules = new HashMap<>();
        boxes = new HashMap<>();
        loadRuleCells = new HashSet<>();
        lockObj = new Object();
        boxLockObj = new Object();
    }

    /**
     * Return RuleManager instance.
     * @return RuleManager object
     */
    public static RuleManager getInstance() {
        if (instance == null) {
            instance = new RuleManager();
            instance.initialize();
        }
        return instance;
    }

    /**
     * Initialize RuleManager and execute threads in order to receive event.
     */
    private void initialize() {
        logger.info("initialize.");
        // Initialize TimerRuleManager.
        if (PersoniumUnitConfig.getTimerEventThreadNum() > 0) {
            timerRuleManager = Optional.of(TimerRuleManager.getInstance());
        }

        // Load rules for initialize.
        loadRulesForInitialize();

        // Create ThreadPool.
        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("ruleevent-subscriber-%d");
        pool = Executors.newFixedThreadPool(1, builder.build());
        // Execute subscriber for managing rule.
        pool.execute(new RuleEventSubscribeRunner());

        // init EventPublisher
        ruleEventPublisher = EventFactory.createEventPublisher(PersoniumUnitConfig.getEventBusRuleTopicName());
    }

    /**
     * Shutdown RuleManager.
     */
    public void shutdown() {
        // close EventPublisher
        ruleEventPublisher.close();

        // Finalize TimerRuleManager.
        timerRuleManager.ifPresent(manager -> {
                             manager.shutdown();
                         });
        timerRuleManager = Optional.empty();

        // Shutdown thread pool.
        try {
            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }

        instance = null;
    }

    /**
     * If the event matches with rule, then execute action of the matched rule.
     * @param event target event object
     */
    public void judge(PersoniumEvent event) {
        if (event == null) {
            return;
        }

        String cellId = event.getCellId();

        Cell cell = ModelFactory.cellFromId(cellId);
        if (cell == null) {
            return;
        }

        CellLockManager.STATUS cellStatus = CellLockManager.getCellStatus(cell.getId());
        if (CellLockManager.STATUS.BULK_DELETION.equals(cellStatus)) {
            return;
        }

        CellLockManager.incrementReferenceCount(cell.getId());

        // set event id
        String eventId = event.getEventId().orElse(PersoniumUUID.randomUUID());

        List<ActionInfo> actionList = new ArrayList<ActionInfo>();
        String ruleChain = event.getRuleChain().orElse("0");
        try {
            int i = Integer.parseInt(ruleChain);
            i++;
            if (i > PersoniumUnitConfig.getMaxEventHop()) {
                ruleChain = null;
            } else {
                ruleChain = String.valueOf(i);
            }
        } catch (NumberFormatException e) {
            logger.info("invalid RuleChain:" + ruleChain);
            ruleChain = null;
        }
        if (ruleChain != null) {
            synchronized (lockObj) {
                if (!loadRuleCells.contains(cellId)) {
                    loadRule(cell);
                }

                Map<String, RuleInfo> map = rules.get(cellId);
                if (map != null) {
                    for (Map.Entry<String, RuleInfo> e : map.entrySet()) {
                        RuleInfo rule = e.getValue();
                        if (isMatched(rule, event)) {
                            String targetUrl = rule.targeturl;
                            // replace localcell and localbox
                            if (targetUrl != null) {
                                // get relative path to set as fragment
                                //   {Cell URL}box/col/...#box/col/...
                                String relative = null;
                                try {
                                    URI uri = new URI(targetUrl);
                                    String scheme = uri.getScheme();
                                    if (UriUtils.SCHEME_LOCALCELL.equals(scheme)) {
                                        relative = uri.getPath().substring(1);
                                        targetUrl = UriUtils.convertSchemeFromLocalCellToHttp(cell.getUrl(), targetUrl);
                                    } else if (UriUtils.SCHEME_LOCALBOX.equals(scheme)) {
                                        synchronized (boxLockObj) {
                                            String boxName = getBoxName(rule);
                                            if (boxName != null) {
                                                relative = boxName + uri.getPath();
                                                String boxUrl = cell.getUrl() + boxName + "/";
                                                targetUrl = UriUtils.convertSchemeFromLocalBoxToHttp(boxUrl, targetUrl);
                                            } else {
                                                logger.error(
                                                        "ignore the Rule(%s) because _Box.Name is null.",
                                                        rule.name);
                                                continue;
                                            }
                                        }
                                    }
                                    if (relative != null) {
                                        targetUrl += "#" + relative;
                                    }
                                } catch (Exception ex) {
                                    logger.error(
                                            "ignore the Rule(%s) because TargetUrl is invalid.",
                                            rule.name);
                                    continue;
                                }
                            }
                            logger.debug("TargetUrl:{} -> {}", rule.targeturl, targetUrl);
                            rule.hitcount++;
                            ActionInfo ai = new ActionInfo(rule.action, targetUrl, eventId, ruleChain);
                            actionList.add(ai);
                        }
                    }
                }
            }
        }

        // convert object's scheme to http scheme
        event.convertObject(cell.getUrl());

        // when the type of the event is timer.periodic or timer.oneshot
        if (PersoniumEventType.timerPeriodic().equals(event.getType())
            || PersoniumEventType.timerOneshot().equals(event.getType())) {
            // validate subject
            event.getSubject().ifPresent(subject -> {
                                   if (!subject.startsWith(cell.getUrl())) {
                                       event.resetSubject();
                                   }
                               });
        }

        // execute action
        for (ActionInfo ai : actionList) {
            ActionRunner runner = new ActionRunner(cell, ai, event);
            PersoniumThread.MISC.execute(runner);
        }

        // publish event
        publish(event);

        CellLockManager.decrementReferenceCount(cell.getId());
    }

    private void publish(PersoniumEvent event) {
        // publish event about rule
        event.getType().ifPresent(type -> {
                            if (!event.getExternal()
                                && (RULEEVENT_RULE_CREATE.equals(type)
                                    || RULEEVENT_RULE_UPDATE.equals(type)
                                    || RULEEVENT_RULE_MERGE.equals(type)
                                    || RULEEVENT_RULE_DELETE.equals(type)
                                    || RULEEVENT_RULE_LINK_BOX_CREATE.equals(type)
                                    || RULEEVENT_RULE_LINK_BOX_DELETE.equals(type)
                                    || RULEEVENT_BOX_LINK_RULE_CREATE.equals(type)
                                    || RULEEVENT_BOX_LINK_RULE_DELETE.equals(type)
                                    || RULEEVENT_RULE_NAVPROP_BOX_CREATE.equals(type)
                                    || RULEEVENT_BOX_NAVPROP_RULE_CREATE.equals(type)
                                    || RULEEVENT_BOX_UPDATE.equals(type)
                                    || RULEEVENT_BOX_MERGE.equals(type)
                                    || RULEEVENT_BOX_RECURSIVE_DELETE.equals(type)
                                    || RULEEVENT_CELL_IMPORT.equals(type))) {
                               ruleEventPublisher.publish(event);
                           }
                       });
    }

    private String getBoxName(RuleInfo rule) {
        if (rule.box != null) {
            return rule.box.name;
        }
        return null;
    }

    private String getBoxSchema(RuleInfo rule) {
        if (rule.box != null) {
            return rule.box.schema;
        }
        return null;
    }

    private boolean isMatched(RuleInfo rule, PersoniumEvent event) {
        if (rule == null) {
            return false;
        }

        // compare external
        //   external is required
        if (rule.external == null) {
            return false;
        } else if (!rule.external.equals(event.getExternal())) {
            return false;
        }

        // compare type
        if (rule.type != null) {
            // if rule.type is .xxx then backward match, otherwise forward match.
            if (rule.type.startsWith(PersoniumEventType.SEPARATOR)) {
                if (!event.getType().map(type -> type.endsWith(rule.type)).orElse(false)) {
                    return false;
                }
            } else if (!event.getType().map(type -> type.startsWith(rule.type)).orElse(false)) {
                return false;
            }
        }

        // compare schema
        synchronized (boxLockObj) {
            String schema = getBoxSchema(rule);
            if (schema != null
                && !event.getSchema().map(s -> schema.equals(s)).orElse(false)) {
                return false;
            }
        }

        // compare subject
        if (rule.subject != null
            && !event.getSubject().map(s -> rule.subject.equals(s)).orElse(false)) {
            return false;
        }

        // compare object
        String object = rule.object;
        if (object != null) {
            if (object.startsWith(UriUtils.SCHEME_BOX_URI)) {
                // replace localbox to localcell
                synchronized (boxLockObj) {
                    String boxName = getBoxName(rule);
                    object = UriUtils.convertSchemeFromLocalBoxToLocalCell(object, boxName);
                    logger.debug(rule.object + " -> " + object);
                }
            }
            final String fobj = object;
            if (!event.getObject().map(o -> o.startsWith(fobj)).orElse(false)) {
                return false;
            }
        }

        // compare info
        if (rule.info != null
            && !event.getInfo().map(i -> i.startsWith(rule.info)).orElse(false)) {
            return false;
        }

        return true;
    }

    /**
     * Load rules from DB (Only cells with "timer event").
     */
    private void loadRulesForInitialize() {
        logger.debug("  loadRulesForInitialize.");
        // Load Rule for cells with timer events.
        // (Other cells read the rule later.)
        List<String> cellIdList = this.searchCellsWithTimerEventOnly();
        logger.debug("  count cells with TimerEvent:" + cellIdList.size());
        for (String cellId : cellIdList) {
            Cell cell = ModelFactory.cellFromId(cellId);
            synchronized (lockObj) {
                if (!loadRuleCells.contains(cell.getId())) {
                    loadRule(cell);
                }
            }
        }
    }

    /**
     * Perform list retrieval.
     * @return cell list (have timerEvent cells only)
     */
    public List<String> searchCellsWithTimerEventOnly() {
        EdmDataServices metadata = CtlSchema.getEdmDataServicesForCellCtl().build();
        EdmEntitySet eSet = metadata.findEdmEntitySet(Rule.EDM_TYPE_NAME);
        EntitySetAccessor esType = new ODataEntityAccessor(EsModel.idxUser("*"), Rule.EDM_TYPE_NAME, null);

        List<Map<String, Object>> implicitFilters = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> orQueries = new ArrayList<Map<String, Object>>();
        orQueries.add(QueryMapFactory.termQuery("s.EventType.untouched", PersoniumEventType.timerPeriodic()));
        orQueries.add(QueryMapFactory.termQuery("s.EventType.untouched", PersoniumEventType.timerOneshot()));
        implicitFilters.add(QueryMapFactory.shouldQuery(orQueries));

        // Conditional search etc.
        ODataQueryHandler visitor = new EsQueryHandler(eSet.getType());
        visitor.initialize(null, implicitFilters);
        Map<String, Object> source = visitor.getSource();

        PersoniumSearchResponse res = null;
        try {
            res = esType.search(source);
        } catch (EsClientException ex) {
            if (ex.getCause() instanceof PersoniumSearchPhaseExecutionException) {
                SearchPhaseExecutionException speex = (SearchPhaseExecutionException) ex.getCause().getCause();
                if (speex.status().equals(RestStatus.BAD_REQUEST)) {
                    throw PersoniumCoreException.OData.SEARCH_QUERY_INVALID_ERROR.reason(ex);
                } else {
                    throw PersoniumCoreException.Server.DATA_STORE_SEARCH_ERROR.reason(ex);
                }
            }
        }

        List<String> cellIds = new ArrayList<>();
        if (res != null) {
            for (PersoniumSearchHit hit : res.getHits().getHits()) {
                EntitySetDocHandler oedh = new OEntityDocHandler(hit);
                oedh.getCellId();
                if (oedh.getCellId() != null && !cellIds.contains(oedh.getCellId())) {
                    cellIds.add(oedh.getCellId());
                }
            }
        }
        return cellIds;
    }

    // Load rules that belongs to cell.
    private void loadRule(Cell cell) {
        logger.info("loadRule Cell=" + cell.getName() + " id=" + cell.getId());

        CellCtlODataProducer producer = new CellCtlODataProducer(cell);
        // query is null
        EntitiesResponse resp = producer.getEntities(Rule.EDM_TYPE_NAME, null);
        List<OEntity> ruleList = resp.getEntities();
        for (OEntity entity : ruleList) {
            registerRule(entity, cell);
        }
        this.loadRuleCells.add(cell.getId());
    }

    // Convert OEntity object to RuleInfo.
    private RuleInfo createRuleInfo(final OEntity oEntity) {
        RuleInfo rule = new RuleInfo();

        rule.type = (String) oEntity.getProperty(Rule.P_TYPE.getName()).getValue();
        rule.object = (String) oEntity.getProperty(Rule.P_OBJECT.getName()).getValue();
        rule.info = (String) oEntity.getProperty(Rule.P_INFO.getName()).getValue();
        rule.subject = (String) oEntity.getProperty(Rule.P_SUBJECT.getName()).getValue();
        rule.external = (Boolean) oEntity.getProperty(Rule.P_EXTERNAL.getName()).getValue();
        rule.action = (String) oEntity.getProperty(Rule.P_ACTION.getName()).getValue();
        rule.targeturl = (String) oEntity.getProperty(Rule.P_TARGETURL.getName()).getValue();
        rule.boxname = (String) oEntity.getProperty(Common.P_BOX_NAME.getName()).getValue();
        rule.name = (String) oEntity.getProperty(Rule.P_NAME.getName()).getValue();
        rule.hitcount = 0;

        return rule;
    }

    // OEntityKey.getComplexKeyValue
    private String getComplexKeyValue(final OEntityKey oEntityKey, String key) {
        if (oEntityKey == null) {
            return null;
        }

        if (KeyType.COMPLEX.equals(oEntityKey.getKeyType())) {
            Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
            for (NamedValue<?> nv : nvSet) {
                if (nv.getName().equals(key)) {
                    return (String) nv.getValue();
                }
            }
        } else if (KeyType.SINGLE.equals(oEntityKey.getKeyType())) {
            return (String) oEntityKey.asSingleValue();
        }
        return null;
    }

    private static final int FIRST_SPLIT_NUMBER = 3;

    private OEntityKey convertFirst(String str) {
        String[] parts = str.split("[()]", FIRST_SPLIT_NUMBER);
        if (parts.length < FIRST_SPLIT_NUMBER - 1) {
            return null;
        }
        String key = parts[FIRST_SPLIT_NUMBER - 2];

        logger.info("convertFirst: key=" + key);

        String keyString = AbstractODataResource.replaceNullToDummyKeyWithParenthesis(key);
        OEntityKey oEntityKey = null;

        try {
            oEntityKey = OEntityKey.parse(keyString);
        } catch (IllegalArgumentException e) {
            logger.error("error! " + e.getMessage());
        }

        return oEntityKey;
    }

    private static final int SECOND_SPLIT_NUMBER = 5;

    private OEntityKey convertSecond(String str) {
        String[] parts = str.split("[()]", SECOND_SPLIT_NUMBER);
        logger.info("convertNp: parts " + parts.length);
        if (parts.length < SECOND_SPLIT_NUMBER - 1) {
            return null;
        }
        String key = parts[SECOND_SPLIT_NUMBER - 2];
        logger.info("converSecond: key=" + key);

        String keyString = AbstractODataResource.replaceNullToDummyKeyWithParenthesis(key);
        OEntityKey oEntityKey = null;
        try {
            oEntityKey = OEntityKey.parse(keyString);
        } catch (IllegalArgumentException e) {
            logger.error("error! " + e.getMessage());
        }

        return oEntityKey;
    }

    private OEntityKey createOEntityKeyFromBoxAndRule(OEntityKey boxKey, OEntityKey ruleKey) {
        String boxName = getComplexKeyValue(boxKey, Common.P_NAME.getName());
        String ruleName = getComplexKeyValue(ruleKey, Rule.P_NAME.getName());
        Map<String, Object> values = new HashMap<>();
        values.put(Common.P_BOX_NAME.getName(), boxName);
        values.put(Rule.P_NAME.getName(), ruleName);
        OEntityKey oEntityKey = OEntityKey.create(values);
        return oEntityKey;
    }

    private boolean registerRuleByOEntityKey(CellCtlODataProducer producer, OEntityKey oEntityKey, Cell cell) {
        EntityResponse entityResp = producer.getEntity(Rule.EDM_TYPE_NAME, oEntityKey, null);
        OEntity oEntity = entityResp.getEntity();
        return registerRule(oEntity, cell);
    }

    private boolean unregisterRuleByOEntityKey(OEntityKey oEntityKey, Cell cell) {
        String ruleName = getComplexKeyValue(oEntityKey, Rule.P_NAME.getName());
        String boxName = getComplexKeyValue(oEntityKey, Common.P_BOX_NAME.getName());
        return unregisterRule(ruleName, boxName, cell);
    }

    /**
     * Manage rule in accordance with event about rule.
     * @param event event object
     * @return true if processing is success, false if processing fails
     */
    public boolean handleRuleEvent(PersoniumEvent event) {
        boolean ret = false;

        Cell cell = null;
        try {
            cell = ModelFactory.cellFromId(event.getCellId());
        } catch (Exception e) {
            cell = null;
        }
        // delete rules of cell because the cell does not exist
        if (cell == null) {
            deleteRule(event.getCellId());
            return ret;
        }

        CellLockManager.STATUS cellStatus = CellLockManager.getCellStatus(cell.getId());
        if (CellLockManager.STATUS.BULK_DELETION.equals(cellStatus)) {
            return ret;
        }

        CellLockManager.incrementReferenceCount(cell.getId());

        CellCtlODataProducer producer = new CellCtlODataProducer(cell);

        try {
            synchronized (lockObj) {
                if (!loadRuleCells.contains(cell.getId())) {
                    loadRule(cell);
                }
            }

            String type = event.getType().get();
            if (RULEEVENT_RULE_CREATE.equals(type)) {
                // register
                OEntityKey oEntityKey = convertFirst(event.getObject().get());
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if (RULEEVENT_RULE_UPDATE.equals(type) || RULEEVENT_RULE_MERGE.equals(type)) {
                // unregister
                OEntityKey oEntityKey = convertFirst(event.getObject().get());
                ret = unregisterRuleByOEntityKey(oEntityKey, cell);
                if (!ret) {
                    return ret;
                }
                // register
                OEntityKey newOEntityKey = convertFirst(event.getInfo().get());
                ret = registerRuleByOEntityKey(producer, newOEntityKey, cell);
            } else if (RULEEVENT_RULE_DELETE.equals(type)) {
                // unregister
                OEntityKey oEntityKey = convertFirst(event.getObject().get());
                ret = unregisterRuleByOEntityKey(oEntityKey, cell);
            } else if (RULEEVENT_RULE_LINK_BOX_CREATE.equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertFirst(event.getObject().get());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                OEntityKey boxKey = convertSecond(event.getObject().get());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if (RULEEVENT_RULE_LINK_BOX_DELETE.equals(type)) {
                // delete link (unregister & register)
                OEntityKey ruleKey = convertFirst(event.getObject().get());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                String ruleName = getComplexKeyValue(ruleKey, Rule.P_NAME.getName());
                Map<String, Object> values = new HashMap<>();
                values.put(Common.P_BOX_NAME.getName(), AbstractODataResource.DUMMY_KEY);
                values.put(Rule.P_NAME.getName(), ruleName);
                OEntityKey oEntityKey = OEntityKey.create(values);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if (RULEEVENT_BOX_LINK_RULE_CREATE.equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertSecond(event.getObject().get());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                OEntityKey boxKey = convertFirst(event.getObject().get());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if (RULEEVENT_BOX_LINK_RULE_DELETE.equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertSecond(event.getObject().get());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                String ruleName = getComplexKeyValue(ruleKey, Rule.P_NAME.getName());
                Map<String, Object> values = new HashMap<>();
                values.put(Common.P_BOX_NAME.getName(), AbstractODataResource.DUMMY_KEY);
                values.put(Rule.P_NAME.getName(), ruleName);
                OEntityKey oEntityKey = OEntityKey.create(values);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if (RULEEVENT_RULE_NAVPROP_BOX_CREATE.equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertFirst(event.getObject().get());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                OEntityKey boxKey = convertSecond(event.getObject().get());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if (RULEEVENT_BOX_NAVPROP_RULE_CREATE.equals(type)) {
                // register
                OEntityKey boxKey = convertFirst(event.getObject().get());
                OEntityKey ruleKey = convertSecond(event.getObject().get());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if (RULEEVENT_BOX_UPDATE.equals(type) || RULEEVENT_BOX_MERGE.equals(type)) {
                OEntityKey boxKey = convertFirst(event.getInfo().get());
                String boxName = getComplexKeyValue(boxKey, Rule.P_NAME.getName());
                Box box = cell.getBoxForName(boxName);
                if (box != null) {
                    setBoxInfo(cell, box);
                }
                ret = true;
            } else if (RULEEVENT_BOX_RECURSIVE_DELETE.equals(type)) {
                String boxUri = event.getObject().get();
                String boxName = boxUri.substring(boxUri.lastIndexOf(UriUtils.STRING_SLASH) + 1);
                ret = unregisterRule(boxName, cell);
            } else if (RULEEVENT_CELL_IMPORT.equals(type)) {
                deleteRule(cell.getId());
                loadRule(cell);
                ret = true;
            }
        } catch (Exception e) {
            logger.error("handleRuleEvent error: " + e.getMessage(), e);
            ret = false;
        }

        CellLockManager.decrementReferenceCount(cell.getId());

        return ret;
    }

    private void setBoxInfo(Cell cell, Box box) {
        synchronized (boxLockObj) {
            Map<String, BoxInfo> bmap = boxes.get(cell.getId());
            if (bmap != null) {
                BoxInfo bi = bmap.get(box.getId());
                if (bi != null) {
                    bi.name = box.getName();
                    String schema = box.getSchema();
                    bi.schema = UriUtils.convertSchemeFromLocalUnitToHttp(schema);
                }
            }
        }
    }

    String getSchemaForBoxId(String cellId, String boxId) {
        synchronized (boxLockObj) {
            Map<String, BoxInfo> bmap = boxes.get(cellId);
            if (bmap != null) {
                BoxInfo bi = bmap.get(boxId);
                if (bi != null && bi.schema != null) {
                    return new String(bi.schema);
                }
            }
        }
        return null;
    }

    /**
     * Get key string for HashMap from ruleName and box's id.
     * @param ruleName the name of rule
     * @param boxId tha name of box
     * @return key string for HashMap
     */
    private String getRuleKey(String ruleName, String boxId) {
        StringBuilder builder = new StringBuilder(ruleName);
        if (boxId != null) {
            builder.append('.');
            builder.append(boxId);
        }
        return builder.toString();
    }

    /**
     * Unregister rules that belongs to cell.
     * @params cellId target cell id
     */
    private void deleteRule(String cellId) {
        synchronized (lockObj) {
            rules.remove(cellId);
            synchronized (boxLockObj) {
                boxes.remove(cellId);
            }
        }
    }

    private String removeFragment(String url) {
        try {
            return UriBuilder.fromUri(url)
                             .fragment(null)
                             .build()
                             .toString();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Register rule by OEntity object.
     * @param oEntity OEntity object of Rule
     * @param cell cell object that the rule belongs to
     * @return true if registering is success, false if it fails
     */
    private boolean registerRule(OEntity oEntity, Cell cell) {
        // Convert OEntity to RuleInfo.
        RuleInfo rule = createRuleInfo(oEntity);

        // Replace personium-localunit scheme to http scheme.
        rule.subject = UriUtils.convertSchemeFromLocalUnitToHttp(rule.subject);
        // Remove fragment from TargetUrl
        rule.targeturl = removeFragment(rule.targeturl);
        try {
            URI uri = new URI(rule.targeturl);
            if (UriUtils.SCHEME_LOCALUNIT.equals(uri.getScheme())) {
                String path = uri.getPath();
                List<String> list = Stream.of(path.split(Pattern.quote("/")))
                                          .filter(s -> !s.isEmpty())
                                          .collect(Collectors.toList());
                String relative = null;
                if (list.size() > 1) {
                    list.remove(0);
                    relative = list.stream().collect(Collectors.joining("/"));
                }
                String turl = UriUtils.convertSchemeFromLocalUnitToHttp(rule.targeturl);
                if (relative != null) {
                    turl += "#" + relative;
                }
                rule.targeturl = turl;
            }
        } catch (Exception e) {
            logger.debug("exception occurred");
        }

        if (rule.action == null) {
            return false;
        }

        final String cellId = cell.getId();

        String boxId = null;

        // Register box as necessary.
        if (rule.boxname != null) {
            Box box = cell.getBoxForName(rule.boxname);
            if (box != null) {
                synchronized (boxLockObj) {
                    Map<String, BoxInfo> bmap = boxes.get(cellId);
                    BoxInfo bi = null;
                    if (bmap == null) {
                        bmap = new HashMap<>();
                        boxes.put(cellId, bmap);
                    } else {
                        bi = bmap.get(box.getId());
                    }
                    if (bi == null) {
                        bi = new BoxInfo();
                        bi.id = box.getId();
                        bi.name = box.getName();
                        String schema = box.getSchema();
                        bi.schema = UriUtils.convertSchemeFromLocalUnitToHttp(schema);
                        bi.count = 0;
                        bmap.put(bi.id, bi);
                    }
                    bi.count++;
                    rule.box = bi;
                    boxId = bi.id;
                }
            } else {
                return false;
            }
        }

        String keyString = getRuleKey(rule.name, boxId);

        // Register rule.
        synchronized (lockObj) {
            Map<String, RuleInfo> rmap = rules.get(cellId);
            if (rmap == null) {
                rmap = new HashMap<String, RuleInfo>();
                rules.put(cellId, rmap);
            }
            rmap.put(keyString, rule);

            // TimerRuleManager
            final String fboxId = boxId;
            timerRuleManager.ifPresent(manager -> manager.registerRule(rule.name,
                                                                       rule.subject,
                                                                       rule.type,
                                                                       rule.object,
                                                                       rule.info,
                                                                       cellId,
                                                                       fboxId));
        }

        return true;
    }

    /**
     * Unregister rule.
     * @param boxName name of box linked with the rule
     * @param cell cell object that the rule belongs to
     * @return true if unregistering is success, false otherwise
     */
    private boolean unregisterRule(String boxName, Cell cell) {
        List<RuleInfo> ruleList = getRuleListLinkedBox(boxName, cell);
        if (ruleList.isEmpty()) {
            return true;
        }
        for (RuleInfo rule : ruleList) {
            // Get key string for HashMap from ruleName and box's id
            String key = getRuleKey(rule.name, rule.box.id);
            if (!unregisterRuleByKey(key, rule.box.id, cell)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get rule list linked box.
     * @param boxName boxName name of box linked with the rule
     * @param cell cell object that the rule belongs to
     * @return rule list
     */
    private List<RuleInfo> getRuleListLinkedBox(String boxName, Cell cell) {
        List<RuleInfo> ruleList = new ArrayList<RuleInfo>();
        Map<String, RuleInfo> map = rules.get(cell.getId());
        if (map == null || map.isEmpty()) {
            return ruleList;
        }
        for (RuleInfo rule : map.values()) {
            if (boxName.equals(rule.boxname)) {
                ruleList.add(rule);
            }
        }
        return ruleList;
    }

    /**
     * Unregister rule.
     * @param ruleName name of rule unregistered
     * @param boxName name of box linked with the rule
     * @param cell cell object that the rule belongs to
     * @return true if unregistering is success, false otherwise
     */
    private boolean unregisterRule(String ruleName, String boxName, Cell cell) {
        // Get box's id from boxName
        String boxId = null;
        if (boxName != null) {
            Box box = cell.getBoxForName(boxName);
            if (box != null) {
                boxId = box.getId();
            }
        }

        // Get key string for HashMap from ruleName and box's id
        String key = getRuleKey(ruleName, boxId);

        return unregisterRuleByKey(key, boxId, cell);
    }

    /**
     * Unregister rule.
     * @param key key is ruleName and box's id
     * @param boxId id of box linked with the rule
     * @param cell cell object that the rule belongs to
     * @return true if unregistering is success, false otherwise
     */
    private boolean unregisterRuleByKey(String key, String boxId, Cell cell) {
        // Remove rule and box as necessary.
        synchronized (lockObj) {
            Map<String, RuleInfo> map = rules.get(cell.getId());
            if (map != null) {
                RuleInfo rule = map.remove(key);
                if (rule != null) {
                    if (rule.box != null) {
                        synchronized (boxLockObj) {
                            rule.box.count--;
                            if (rule.box.count == 0) {
                                Map<String, BoxInfo> bmap = boxes.get(cell.getId());
                                bmap.remove(rule.box.id);
                            }
                        }
                        rule.box = null;
                    }

                    // TimerRuleManager
                    final String fboxId = boxId;
                    timerRuleManager.ifPresent(manager -> manager.unregisterRule(rule.subject,
                                                                                 rule.type,
                                                                                 rule.object,
                                                                                 rule.info,
                                                                                 cell.getId(),
                                                                                 fboxId));

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get rules and boxes managed on RuleManager.
     * This method is for the debug purpose.
     * @param cell target cell object
     * @return rules managed on RuleManager
     */
    public Map<String, Object> getRules(Cell cell) {
        Map<String, Object> ret = new HashMap<>();
        String cellId = cell.getId();
        logger.info("cellId is " + cellId);
        synchronized (lockObj) {
            if (!loadRuleCells.contains(cellId)) {
                loadRule(cell);
            }

            Optional<Map<String, RuleInfo>> rulesForCell = Optional.ofNullable(rules.get(cellId));
            List<Map<String, Object>> ruleList;
            ruleList = rulesForCell.map(mapRule ->
                                        mapRule.values()
                                               .stream()
                                               .map(ri -> {
                                                    Map<String, Object> m = new HashMap<>();
                                                    m.put(Rule.P_EXTERNAL.getName(), ri.external);
                                                    m.put(Rule.P_SUBJECT.getName(), ri.subject);
                                                    m.put(Rule.P_TYPE.getName(), ri.type);
                                                    m.put(Rule.P_OBJECT.getName(), ri.object);
                                                    m.put(Rule.P_INFO.getName(), ri.info);
                                                    m.put(Rule.P_ACTION.getName(), ri.action);
                                                    m.put(Rule.P_TARGETURL.getName(), ri.targeturl);
                                                    if (ri.box != null) {
                                                        m.put(Box.P_SCHEMA.getName(), ri.box.schema);
                                                        m.put(Common.P_BOX_NAME.getName(), ri.box.name);
                                                    }
                                                    m.put(Rule.P_NAME.getName(), ri.name);
                                                    m.put("HitCount", ri.hitcount);
                                                    return m;
                                                })
                                               .collect(Collectors.toList()))
                                  .orElse(new ArrayList<>());
            ret.put("rules", ruleList);

            synchronized (boxLockObj) {
                Optional<Map<String, BoxInfo>> boxesForCell = Optional.ofNullable(boxes.get(cellId));
                List<Map<String, Object>> boxList;
                boxList = boxesForCell.map(mapBox ->
                                           mapBox.values()
                                                 .stream()
                                                 .map(bi -> {
                                                      Map<String, Object> m = new HashMap<>();
                                                      m.put(Common.P_NAME.getName(), bi.name);
                                                      m.put(Box.P_SCHEMA.getName(), bi.schema);
                                                      m.put("id", bi.id);
                                                      return m;
                                                  })
                                                 .collect(Collectors.toList()))
                                      .orElse(new ArrayList<>());
                ret.put("boxes", boxList);
            }
        }
        timerRuleManager.ifPresent(manager -> ret.put("timers", manager.getTimerList(cell)));

        return ret;
    }

}

