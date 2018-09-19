/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Rule;

/**
 * TimerRuleManager.
 */
class TimerRuleManager {
    /**
     * Internal class for managing rule.
     */
    class TimerRuleInfo {
        String type;
        String object;
        String info;
        String cellId;
        String boxId;
        String subject;
        long count;
    }

    /**
     * Internal class for timer list.
     */
    class TimerInfo {
        Map<String, List<TimerRuleInfo>> ruleMap;
        String mapKey;
        long interval;
        long nextTime;

        ScheduledFuture<?> handle;
    }

    private static TimerRuleManager instance = null;
    private Map<String, TimerRuleInfo> rules;
    private Map<String, TimerInfo> timerMap;

    private Object lockObj;

    private ScheduledExecutorService scheduler = null;

    /**
     * Constructor.
     */
    private TimerRuleManager() {
        rules = new HashMap<>();
        timerMap = new HashMap<>();
        lockObj = new Object();
    }

    /**
     * Return TimerRuleManager instance.
     * @return TimerRuleManager object
     */
    static TimerRuleManager getInstance() {
        if (instance == null) {
            instance = new TimerRuleManager();
            instance.initialize();
        }
        return instance;
    }

    /**
     * Initialize TimerRuleManager.
     */
    private void initialize() {
        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("timer-event-sender-%d");
        scheduler = Executors.newScheduledThreadPool(
                PersoniumUnitConfig.getTimerEventThreadNum(),
                builder.build());
    }

    /**
     * Shutdown TimerRuleManager.
     */
    void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Trigger timer.oneshot event.
     * @param key key string for HashMap
     */
    void triggerOneshot(final String key) {
        synchronized (lockObj) {
            TimerInfo timer = timerMap.get(key);
            if (timer != null) {
                send(timer);
                timerMap.remove(key);
            }
        }
    }

    /**
     * Trigger timer.periodic event.
     * @param key key string for HashMap
     */
    void triggerPeriodic(final String key) {
        synchronized (lockObj) {
            TimerInfo timer = timerMap.get(key);
            if (timer != null && !send(timer)) {
                timerMap.remove(key);
                timer.handle.cancel(true);
            }
        }
    }

    private boolean send(TimerInfo timer) {
        boolean validMap = false;

        for (Iterator<Map.Entry<String, List<TimerRuleInfo>>> iruleMap = timer.ruleMap.entrySet().iterator();
                iruleMap.hasNext();) {
            Map.Entry<String, List<TimerRuleInfo>> entry = iruleMap.next();
            String cellId = entry.getKey();
            List<TimerRuleInfo> ruleList = entry.getValue();
            Cell cell = ModelFactory.cellFromId(cellId);
            EventBus eventBus = cell.getEventBus();
            boolean validList = false;

            for (Iterator<TimerRuleInfo> i = ruleList.iterator(); i.hasNext();) {
                TimerRuleInfo rule = i.next();
                if (rule.count == 0) {
                    i.remove();
                    continue;
                }
                String schema = null;
                if (rule.boxId != null) {
                    schema = RuleManager.getInstance().getSchemaForBoxId(cellId, rule.boxId);
                }
                PersoniumEvent event = new PersoniumEvent.Builder()
                        .schema(schema)
                        .subject(rule.subject)
                        .type(rule.type)
                        .object(rule.object)
                        .info(rule.info)
                        .build();
                eventBus.post(event);
                validList = true;
            }

            if (!validList) {
                iruleMap.remove();
            }
            validMap |= validList;
        }

        return validMap;
    }

    /**
     * Get key string for HashMap from subject, type, object, info, cellId and boxId.
     */
    private String getRuleKey(String subject, String type, String object, String info, String cellId, String boxId) {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append('.');
        builder.append(object);
        builder.append('.');
        if (info != null) {
            builder.append(info);
        }
        builder.append('.');
        builder.append(cellId);
        builder.append('.');
        if (boxId != null) {
            builder.append(boxId);
        }
        builder.append('.');
        if (subject != null) {
            builder.append(subject);
        }

        return builder.toString();
    }

    private void insertTimer(final TimerInfo timer) {
        if (timer.nextTime == 0) {
            return;
        }

        final String key = timer.mapKey;

        if (timer.interval > 0) {
            timer.handle = scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    TimerRuleManager man = TimerRuleManager.getInstance();
                    man.triggerPeriodic(key);
                }
            }, timer.interval, timer.interval, TimeUnit.MILLISECONDS);
        } else {
            timer.handle = scheduler.schedule(new Runnable() {
                public void run() {
                    TimerRuleManager man = TimerRuleManager.getInstance();
                    man.triggerOneshot(key);
                }
            }, timer.nextTime - new Date().getTime(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Register timer rule.
     * @param name rule name
     * @param subject EventSubject of rule
     * @param type EventType of rule
     * @param object EventObject of rule
     * @param info EventInfo of rule
     * @param cellId cellId that the rule belongs to
     * @param boxId boxId that the rule is related to
     * @return true if registering is success, false if it fails
     */
    boolean registerRule(String name, String subject,
            String type, String object, String info, String cellId, String boxId) {
        long interval;
        long nextTime;
        String mapKey = null;

        // truncate to minutes
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Date now = Date.from(instant);

        if (PersoniumEventType.timerPeriodic().equals(type)) {
            interval = TimeUnit.MINUTES.toMillis(Long.parseLong(object));
            nextTime = now.getTime() + interval;
            mapKey = "p" + interval;
        } else if (PersoniumEventType.timerOneshot().equals(type)) {
            interval = 0;
            nextTime = Long.parseLong(object);
            Instant ins = Instant.ofEpochMilli(nextTime).truncatedTo(ChronoUnit.MINUTES);
            nextTime = Date.from(ins).getTime();
            if (nextTime < now.getTime()) {
                nextTime = 0;
            } else {
                mapKey = "o" + nextTime;
            }
        } else {
            return false;
        }

        synchronized (lockObj) {
            String keyString = getRuleKey(subject, type, object, info, cellId, boxId);
            TimerRuleInfo rule = rules.get(keyString);
            if (rule == null) {
                rule = new TimerRuleInfo();
                rule.subject = subject;
                rule.type = type;
                rule.object = object;
                rule.info = info;
                rule.cellId = cellId;
                rule.boxId = boxId;
                rule.count = 1;
                rules.put(keyString, rule);

                if (mapKey != null) {
                    TimerInfo timer = timerMap.get(mapKey);
                    if (timer != null) {
                        List<TimerRuleInfo> ruleList = timer.ruleMap.get(rule.cellId);
                        if (ruleList != null) {
                            ruleList.add(rule);
                        } else {
                            ruleList = new ArrayList<>();
                            ruleList.add(rule);
                            timer.ruleMap.put(rule.cellId, ruleList);
                        }
                    } else {
                        timer = new TimerInfo();
                        timer.ruleMap = new HashMap<>();
                        List<TimerRuleInfo> ruleList = new ArrayList<>();
                        ruleList.add(rule);
                        timer.ruleMap.put(rule.cellId, ruleList);
                        timer.interval = interval;
                        timer.nextTime = nextTime;
                        timer.mapKey = mapKey;
                        timerMap.put(mapKey, timer);
                        insertTimer(timer);
                    }
                }
            } else {
                rule.count++;
            }
        }

        return true;
    }

    /**
     * Unregister rule.
     * @param subject subject value
     * @param type type value
     * @param object object value
     * @param info info value
     * @param cellId cell id
     * @param boxId box id
     * @return true if unregistering is success, false otherwise
     */
    boolean unregisterRule(String subject, String type, String object, String info, String cellId, String boxId) {
        String key = getRuleKey(subject, type, object, info, cellId, boxId);

        // Remove rule.
        synchronized (lockObj) {
            TimerRuleInfo rule = rules.get(key);
            if (rule != null) {
                rule.count--;
                if (rule.count == 0) {
                    rules.remove(key, rule);
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Get timer list managed on TimerRuleManager.
     * @param cell target cell object
     * @return JSONArray of timer list managed on  TimerRuleManager
     */
    @SuppressWarnings("unchecked")
    JSONArray getTimerList(Cell cell) {
        String cellId = cell.getId();
        if (cellId == null) {
            return null;
        }
        JSONArray jsonTimerArray = new JSONArray();

        synchronized (lockObj) {
            for (TimerInfo ti : timerMap.values()) {
                for (Iterator<Map.Entry<String, List<TimerRuleInfo>>> iruleMap = ti.ruleMap.entrySet().iterator();
                        iruleMap.hasNext();) {
                    Map.Entry<String, List<TimerRuleInfo>> entry = iruleMap.next();
                    if (cellId.equals(entry.getKey())) {
                        for (Iterator<TimerRuleInfo> i = entry.getValue().iterator(); i.hasNext();) {
                            TimerRuleInfo rule = i.next();
                            JSONObject json = new JSONObject();
                            json.put(Rule.P_SUBJECT.getName(), rule.subject);
                            json.put(Rule.P_OBJECT.getName(), rule.object);
                            json.put(Rule.P_INFO.getName(), rule.info);
                            json.put("boxId", rule.boxId);
                            json.put("count", rule.count);
                            json.put("interval", ti.interval);
                            json.put("nextTime", ti.nextTime);
                            jsonTimerArray.add(json);
                        }
                    }
                }
            }
        }

        return jsonTimerArray;
    }

}

