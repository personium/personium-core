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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.odata4j.core.NamedValue;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OEntityKey.KeyType;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.utils.PersoniumThread;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.EventPublisher;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.Cell;
import io.personium.core.model.Box;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.es.odata.UnitCtlODataProducer;
import io.personium.core.odata.OEntityWrapper;
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
        String service;
        String boxname;
        BoxInfo box;
        String name;
    }

    static final String LOCALUNIT = UriUtils.SCHEME_LOCALUNIT + ":";
    static final String LOCALCELL = UriUtils.SCHEME_LOCALCELL + ":";
    static final String LOCALBOX = UriUtils.SCHEME_LOCALBOX + ":";

    private static RuleManager instance = null;
    private Map<String, Map<String, RuleInfo>> rules;
    private Map<String, Map<String, BoxInfo>> boxes;
    private Logger logger;

    private Object lockObj;

    private static final int EVENTRECEIVER_NUM = 2;
    private static final int RULEEVENTSUBSCRIBER_NUM = 1;
    private static final int THREAD_NUMBER = EVENTRECEIVER_NUM + RULEEVENTSUBSCRIBER_NUM;

    private ExecutorService pool;

    /**
     * Constructor.
     */
    private RuleManager() {
        rules = new HashMap<>();
        boxes = new HashMap<>();
        logger = LoggerFactory.getLogger(RuleManager.class);
        lockObj = new Object();
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
        // Load rules from DB.
        load();

        // Create ThreadPool.
        pool = Executors.newFixedThreadPool(THREAD_NUMBER);
        // Execute receiver for processing rule.
        for (int i = 0; i < EVENTRECEIVER_NUM; i++) {
            pool.execute(new EventReceiver());
        }
        // Execute receiver for managing rule.
        pool.execute(new RuleEventSubscriber());
    }

    /**
     * Finalize RuleManager.
     */
    public void finalize() {
        // Shutdown thread pool.
        try {
            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }
    }

    /**
     * Match with rule and execute the matched rule.
     * @param event target event object
     */
    public void judge(PersoniumEvent event) {
        if (event == null) {
            return;
        }

        String cellId = event.getCellId();
        if (cellId == null) {
            return;
        }

        Cell cell = ModelFactory.cell(cellId, null);
        if (cell == null) {
            return;
        }

        CellLockManager.STATUS cellStatus = CellLockManager.getCellStatus(cell.getId());
        if (CellLockManager.STATUS.BULK_DELETION.equals(cellStatus)) {
            return;
        }

        CellLockManager.incrementReferenceCount(cell.getId());

        // set event id
        String eventId = event.getEventId();
        if (eventId == null) {
            eventId = PersoniumUUID.randomUUID();
        }

        List<ActionInfo> actionList = new ArrayList<ActionInfo>();
        String ruleChain = event.getRuleChain();
        if (ruleChain == null) {
            ruleChain = "0";
        }
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
                Map<String, RuleInfo> map = rules.get(cellId);
                if (map != null) {
                    for (Map.Entry<String, RuleInfo> e : map.entrySet()) {
                        RuleInfo rule = e.getValue();
                        if (match(rule, event)) {
                            String service = rule.service;
                            // replace personium-localcell and personium-localbox
                            if (service != null) {
                                if (service.startsWith(LOCALCELL)) {
                                    service = UriUtils.convertSchemeFromLocalCellToHttp(cell.getUrl(), service);
                                } else if (service.startsWith(LOCALBOX)) {
                                    String boxName = getBoxName(rule);
                                    if (boxName != null) {
                                        service = service.replace(LOCALBOX, cell.getUrl() + rule.box.name);
                                    } else {
                                        logger.error(
                                                "ignore the Rule(%s) because _Box.Name is null.",
                                                rule.name);
                                        continue;
                                    }
                                }
                            }
                            ActionInfo ai = new ActionInfo(rule.action, service, eventId, ruleChain);
                            actionList.add(ai);
                        }
                    }
                }
            }
        }

        // convert object's scheme to http scheme
        String object = UriUtils.convertSchemeFromLocalCellToHttp(cell.getUrl(), event.getObject());
        if (object != null) {
            event.setObject(object);
        }
        // execute action
        for (ActionInfo ai : actionList) {
            ActionRunner runner = new ActionRunner(cell, ai, event);
            PersoniumThread.execute(runner);
        }

        // publish event
        publish(event);

        CellLockManager.decrementReferenceCount(cell.getId());
    }

    private void publish(PersoniumEvent event) {
        // publish all event
        EventPublisher.send(event);

        // publish event about rule
        String type = event.getType();
        if (!event.getExternal()
                && ("cellctl.Rule.create".equals(type)
                || "cellctl.Rule.update".equals(type)
                || "cellctl.Rule.patch".equals(type)
                || "cellctl.Rule.delete".equals(type)
                || "cellctl.Rule.links.Box.create".equals(type)
                || "cellctl.Rule.links.Box.delete".equals(type)
                || "cellctl.Box.links.Rule.create".equals(type)
                || "cellctl.Box.links.Rule.delete".equals(type)
                || "cellctl.Rule.navprop.Box.create".equals(type)
                || "cellctl.Box.navprop.Rule.create".equals(type)
                || "cellctl.Box.update".equals(type)
                || "cellctl.Box.merge".equals(type)
                || "cell.import".equals(type))) {
            EventPublisher.sendRuleEvent(event);
        }
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

    private String getBoxId(RuleInfo rule) {
        if (rule.box != null) {
            return rule.box.id;
        }
        return null;
    }

    private boolean match(RuleInfo rule, PersoniumEvent event) {
        if (rule == null || event == null) {
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
            if (event.getType() == null) {
                return false;
            } else if (!event.getType().startsWith(rule.type)) {
                return false;
            }
        }

        // compare schema
        String schema = getBoxSchema(rule);
        if (schema != null && !schema.equals(event.getSchema())) {
            return false;
        }

        // compare subject
        if (rule.subject != null && !rule.subject.equals(event.getSubject())) {
            return false;
        }

        // compare object
        String object = rule.object;
        String boxName = getBoxName(rule);
        if (object != null) {
            if (object.startsWith(LOCALBOX)) {
                // replace personium-localbox to personium-localcell
                object = UriUtils.convertSchemeFromLocalBoxToLocalCell(object, boxName);
                logger.debug(rule.object + " -> " + object);
            }
            if (event.getObject() == null) {
                return false;
            } else if (!event.getObject().startsWith(object)) {
                return false;
            }
        }

        // compare info
        if (rule.info != null && !rule.info.equals(event.getInfo())) {
            return false;
        }

        return true;
    }

    /**
     * Load all rules from DB.
     */
    private void load() {
        // accesscontext is set as null
        UnitCtlODataProducer odataProducer = new UnitCtlODataProducer(null);
        EdmEntitySet eSet = odataProducer.getMetadata().findEdmEntitySet(Cell.EDM_TYPE_NAME);
        List<Map<String, Object>> implicitFilters = new ArrayList<Map<String, Object>>();
        EntitySetAccessor esType = odataProducer.getAccessorForEntitySet(Cell.EDM_TYPE_NAME);
        EntitiesResponse resp = odataProducer.execEntitiesRequest(null, eSet, esType, implicitFilters);
        List<OEntity> cells = resp.getEntities();
        for (OEntity entity : cells) {
            OEntityWrapper oew = (OEntityWrapper) entity;
            OProperty<?> prop = oew.getProperty(Common.P_NAME.getName());
            logger.info("Cell=" + prop.getValue());
            List<OProperty<?>> props = oew.getProperties();
            for (OProperty<?> op : props) {
                logger.info("op: " + op.getName() + "=" + op.getValue());
            }
            Map<String, Object> map = oew.getMetadata();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                logger.info("meta: " + e.getKey() + "=" + e.getValue());
            }
            logger.info("id=" + oew.getUuid());

            // Cell object
            Cell cell = ModelFactory.cell(oew.getUuid(), null);

            // load Rules for cell
            loadRule(cell);
        }
    }

    // Load rules that belongs to cell.
    private void loadRule(Cell cell) {
        CellCtlODataProducer producer = new CellCtlODataProducer(cell);
        // query is null
        EntitiesResponse resp = producer.getEntities(Rule.EDM_TYPE_NAME, null);
        List<OEntity> ruleList = resp.getEntities();
        for (OEntity entity : ruleList) {
            registerRule(entity, cell);
        }
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
        rule.service = (String) oEntity.getProperty(Rule.P_SERVICE.getName()).getValue();
        rule.boxname = (String) oEntity.getProperty(Common.P_BOX_NAME.getName()).getValue();
        rule.name = (String) oEntity.getProperty(Rule.P_NAME.getName()).getValue();

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
            cell = ModelFactory.cell(event.getCellId(), null);
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

            String type = event.getType();
            if ("cellctl.Rule.create".equals(type)) {
                // register
                OEntityKey oEntityKey = convertFirst(event.getObject());
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if ("cellctl.Rule.update".equals(type) || "cellctl.Rule.patch".equals(type)) {
                // unregister
                OEntityKey oEntityKey = convertFirst(event.getObject());
                ret = unregisterRuleByOEntityKey(oEntityKey, cell);
                if (!ret) {
                    return ret;
                }
                // register
                OEntityKey newOEntityKey = convertFirst(event.getInfo());
                ret = registerRuleByOEntityKey(producer, newOEntityKey, cell);
            } else if ("cellctl.Rule.delete".equals(type)) {
                // unregister
                OEntityKey oEntityKey = convertFirst(event.getObject());
                ret = unregisterRuleByOEntityKey(oEntityKey, cell);
            } else if ("cellctl.Rule.links.Box.create".equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertFirst(event.getObject());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                OEntityKey boxKey = convertSecond(event.getObject());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if ("cellctl.Rule.links.Box.delete".equals(type)) {
                // delete link (unregister & register)
                OEntityKey ruleKey = convertFirst(event.getObject());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                String ruleName = getComplexKeyValue(ruleKey, Rule.P_NAME.getName());
                Map<String, Object> values = new HashMap<>();
                values.put(Common.P_BOX_NAME.getName(), AbstractODataResource.DUMMY_KEY);
                values.put(Rule.P_NAME.getName(), ruleName);
                OEntityKey oEntityKey = OEntityKey.create(values);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if ("cellctl.Box.links.Rule.create".equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertSecond(event.getObject());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                OEntityKey boxKey = convertFirst(event.getObject());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if ("cellctl.Box.links.Rule.delete".equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertSecond(event.getObject());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                String ruleName = getComplexKeyValue(ruleKey, Rule.P_NAME.getName());
                Map<String, Object> values = new HashMap<>();
                values.put(Common.P_BOX_NAME.getName(), AbstractODataResource.DUMMY_KEY);
                values.put(Rule.P_NAME.getName(), ruleName);
                OEntityKey oEntityKey = OEntityKey.create(values);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if ("cellctl.Rule.navprop.Box.create".equals(type)) {
                // link (unregister & register)
                OEntityKey ruleKey = convertFirst(event.getObject());
                ret = unregisterRuleByOEntityKey(ruleKey, cell);
                // register
                OEntityKey boxKey = convertSecond(event.getObject());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if ("cellctl.Box.navprop.Rule.create".equals(type)) {
                // register
                OEntityKey boxKey = convertFirst(event.getObject());
                OEntityKey ruleKey = convertSecond(event.getObject());
                OEntityKey oEntityKey = createOEntityKeyFromBoxAndRule(boxKey, ruleKey);
                ret = registerRuleByOEntityKey(producer, oEntityKey, cell);
            } else if ("cellctl.Box.update".equals(type) || "cellctl.Box.patch".equals(type)) {
                OEntityKey boxKey = convertFirst(event.getInfo());
                String boxName = getComplexKeyValue(boxKey, Rule.P_NAME.getName());
                Box box = cell.getBoxForName(boxName);
                if (box != null) {
                    synchronized (lockObj) {
                        Map<String, BoxInfo> bmap = boxes.get(cell.getId());
                        if (bmap != null) {
                            BoxInfo bi = bmap.get(box.getId());
                            if (bi != null) {
                                bi.name = box.getName();
                                String schema = box.getSchema();
                                schema = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), schema);
                                bi.schema = schema;
                            }
                        }
                    }
                }
                ret = true;
            } else if ("cell.import".equals(type)) {
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

    /**
     * Get key string for HashMap from ruleName and box's id.
     * @param ruleName the name of rule
     * @param boxId tha name of box
     * @return key string for HashMap
     */
    private String getRuleKey(String ruleName, String boxId) {
        String ret = ruleName;
        if (boxId != null) {
            ret += "." + boxId;
        }
        return ret;
    }

    /**
     * Unregister rules that belongs to cell.
     * @params cellId target cell id
     */
    private void deleteRule(String cellId) {
        synchronized (lockObj) {
            rules.remove(cellId);
            boxes.remove(cellId);
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
        if (rule.service != null && rule.service.startsWith(LOCALUNIT)) {
            rule.service = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), rule.service);
        }

        if (rule.action == null) {
            return false;
        }

        String cellId = cell.getId();

        // Register box as necessary.
        if (rule.boxname != null) {
            Box box = cell.getBoxForName(rule.boxname);
            if (box != null) {
                synchronized (lockObj) {
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
                        schema = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), schema);
                        bi.schema = schema;
                        bi.count = 0;
                        bmap.put(bi.id, bi);
                    }
                    bi.count++;
                    rule.box = bi;
                }
            } else {
                return false;
            }
        }

        String boxId = getBoxId(rule);
        String keyString = getRuleKey(rule.name, boxId);

        // Register rule.
        synchronized (lockObj) {
            Map<String, RuleInfo> rmap = rules.get(cellId);
            if (rmap == null) {
                rmap = new HashMap<String, RuleInfo>();
                rules.put(cellId, rmap);
            }
            rmap.put(keyString, rule);
        }

        return true;
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

        // Remove rule and box as necessary.
        synchronized (lockObj) {
            Map<String, RuleInfo> map = rules.get(cell.getId());
            if (map != null) {
                RuleInfo rule = map.remove(key);
                if (rule != null) {
                    if (rule.box != null) {
                        rule.box.count--;
                        if (rule.box.count == 0) {
                            Map<String, BoxInfo> bmap = boxes.get(cell.getId());
                            bmap.remove(rule.box.id);
                        }
                        rule.box = null;
                    }
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
     * @return JSON format string of rules managed on RuleManager
     */
    @SuppressWarnings("unchecked")
    public String getRules(Cell cell) {
        JSONObject jsonRules = new JSONObject();
        JSONArray jsonRuleArray = new JSONArray();
        JSONArray jsonBoxArray = new JSONArray();
        String cellId = cell.getId();
        logger.info("cellId is " + cellId);
        synchronized (lockObj) {
            Map<String, RuleInfo> mapRule = rules.get(cellId);
            if (mapRule != null) {
                logger.info("list is not null");
                for (RuleInfo ri : mapRule.values()) {
                    logger.info("rule");
                    JSONObject json = new JSONObject();
                    json.put(Rule.P_EXTERNAL.getName(), ri.external);
                    json.put(Rule.P_SUBJECT.getName(), ri.subject);
                    json.put(Rule.P_TYPE.getName(), ri.type);
                    json.put(Rule.P_OBJECT.getName(), ri.object);
                    json.put(Rule.P_INFO.getName(), ri.info);
                    json.put(Rule.P_ACTION.getName(), ri.action);
                    json.put(Rule.P_SERVICE.getName(), ri.service);
                    if (ri.box != null) {
                        json.put(Box.P_SCHEMA.getName(), ri.box.schema);
                        json.put(Common.P_BOX_NAME.getName(), ri.box.name);
                    }
                    json.put(Rule.P_NAME.getName(), ri.name);
                    jsonRuleArray.add(json);
                }
            }
            Map<String, BoxInfo> mapBox = boxes.get(cellId);
            if (mapBox != null) {
                for (BoxInfo bi : mapBox.values()) {
                    JSONObject json = new JSONObject();
                    json.put(Common.P_NAME.getName(), bi.name);
                    json.put(Box.P_SCHEMA.getName(), bi.schema);
                    json.put("id", bi.id);
                    jsonBoxArray.add(json);
                }
            }
        }
        jsonRules.put("rules", jsonRuleArray);
        jsonRules.put("boxes", jsonBoxArray);

        return jsonRules.toString();
    }

}

