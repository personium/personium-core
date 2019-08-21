/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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
package io.personium.core.rs.cell;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.NotImplementedException;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.AuthUtils;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.ODataResource;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS Resource handling DC Cell Level Api.
 */
public final class CellCtlResource extends ODataResource {
    String pCredHeader;
    CellRsCmp cellRsCmp;

    /**
     * constructor.
     * @param accessContext AccessContext
     * @param pCredHeader X-Personium-Credential header
     * @param cellRsCmp davRsCmp
     */
    public CellCtlResource(final AccessContext accessContext, final String pCredHeader, CellRsCmp cellRsCmp) {
        super(accessContext, UriUtils.SCHEME_LOCALCELL + ":/__ctl/", ModelFactory.ODataCtl.cellCtl(accessContext
                .getCell()));
        this.pCredHeader = pCredHeader;
        this.cellRsCmp = cellRsCmp;
    }

    @Override
    public void checkAccessContext(final AccessContext ac, Privilege privilege) {
        this.cellRsCmp.checkAccessContext(ac, privilege);
    }

    /**
     * Obtain Auth Scheme that can be used for authentication.
     * @return Auth Scheme that can be used for authentication
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return this.cellRsCmp.getAcceptableAuthScheme();
    }

    @Override
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {
        return this.cellRsCmp.hasSubjectPrivilege(privilege);
    }

    @Override
    public void checkSchemaAuth(AccessContext ac) {
    }

    @Override
    public void beforeCreate(final OEntityWrapper oEntityWrapper) {
        String entitySetName = oEntityWrapper.getEntitySet().getName();
        AuthUtils.validateAccountType(oEntityWrapper, entitySetName);
        AuthUtils.validateAccountIPAddressRange(oEntityWrapper, entitySetName);
        Map<String, String> hashed = AuthUtils.hashPassword(pCredHeader, entitySetName);
        if (hashed != null) {
            for (Map.Entry<String, String> hashedEntry : hashed.entrySet()) {
                oEntityWrapper.put(hashedEntry.getKey(), hashedEntry.getValue());
            }
        }
    }

    @Override
    public void beforeUpdate(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        String entitySetName = oEntityWrapper.getEntitySet().getName();
        AuthUtils.validateAccountType(oEntityWrapper, entitySetName);
        AuthUtils.validateAccountIPAddressRange(oEntityWrapper, entitySetName);
        Map<String, String> hashed = AuthUtils.hashPassword(pCredHeader, entitySetName);
        if (hashed != null) {
            for (Map.Entry<String, String> hashedEntry : hashed.entrySet()) {
                oEntityWrapper.put(hashedEntry.getKey(), hashedEntry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeMerge(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        String entitySetName = oEntityWrapper.getEntitySet().getName();

        if (Account.EDM_TYPE_NAME.equals(entitySetName)) {
            // validate Account. (Unspecified items are not subject to validate)
            if (oEntityWrapper.hasProperty(Account.P_TYPE.getName())) {
                AuthUtils.validateAccountType(oEntityWrapper, entitySetName);
            }
            if (oEntityWrapper.hasProperty(Account.P_IP_ADDRESS_RANGE.getName())) {
                AuthUtils.validateAccountIPAddressRange(oEntityWrapper, entitySetName);
            }
            Map<String, String> hashed = AuthUtils.hashPassword(pCredHeader, entitySetName);
            if (hashed != null) {
                for (Map.Entry<String, String> hashedEntry : hashed.entrySet()) {
                    oEntityWrapper.put(hashedEntry.getKey(), hashedEntry.getValue());
                }
            }
        } else {
            beforeUpdate(oEntityWrapper, oEntityKey);
        }
    }

    /**
     * Corresponds to the service metadata request.
     * @return JAX-RS response object
     */
    @GET
    @Path("{first: \\$}metadata")
    public Response getMetadata() {
        return super.doGetMetadata();
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    @Path("{first: \\$}metadata")
    public Response optionsMetadata() {
        return super.doGetOptionsMetadata();
    }

    @Override
    public Privilege getNecessaryReadPrivilege(String entitySetNameStr) {
        //The cell level has different authority for each entity set
        if (Account.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.AUTH_READ;
        } else if (Role.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.AUTH_READ;
        } else if (ExtRole.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.AUTH_READ;
        } else if (Relation.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.SOCIAL_READ;
        } else if (ExtCell.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.SOCIAL_READ;
        } else if (Box.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.BOX_READ;
        } else if (ReceivedMessage.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.MESSAGE_READ;
        } else if (SentMessage.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.MESSAGE_READ;
        } else if (Rule.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.RULE_READ;
        }
        return null;

    }

    @Override
    public Privilege getNecessaryWritePrivilege(String entitySetNameStr) {
        //The cell level has different authority for each entity set
        if (Account.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.AUTH;
        } else if (Role.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.AUTH;
        } else if (ExtRole.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.AUTH;
        } else if (Relation.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.SOCIAL;
        } else if (ExtCell.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.SOCIAL;
        } else if (Box.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.BOX;
        } else if (ReceivedMessage.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.MESSAGE;
        } else if (SentMessage.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.MESSAGE;
        } else if (Rule.EDM_TYPE_NAME.equals(entitySetNameStr)) {
            return CellPrivilege.RULE;
        }
        return null;
    }

    @Override
    public Privilege getNecessaryOptionsPrivilege() {
        return CellPrivilege.SOCIAL_READ;
    }

    @Override
    public void setBasicAuthenticateEnableInBatchRequest(AccessContext ac) {
        //Since the Cell level API does not support batch requests, we do not do anything here
    }

    /**
     * Not Implemented. <br />
     * Currently unimplemented because it is only necessary for $ batch access control <br />
     * Returns whether the access context has permission to $ batch.
     * @param ac access context
     * @return true: The access context has permission to $ batch
     */
    @Override
    public boolean hasPrivilegeForBatch(AccessContext ac) {
        throw new NotImplementedException();
    }

    @Override
    public void validate(String entitySetName, List<OProperty<?>> props) {
        if (Rule.EDM_TYPE_NAME.equals(entitySetName)) {
            // get properties
            Boolean external = false;
            String subject = null;
            String type = null;
            String object = null;
            String info = null;
            String action = null;
            String targetUrl = null;
            Boolean boxBound = false;
            for (OProperty<?> property : props) {
                String name = property.getName();
                String value = null;
                if (property.getValue() != null) {
                    value = property.getValue().toString();
                }
                if (Rule.P_ACTION.getName().equals(name)) {
                    if (value == null) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_ACTION.getName());
                    } else {
                        action = value;
                    }
                } else if (Rule.P_EXTERNAL.getName().equals(name) && value != null) {
                    external = Boolean.valueOf(value);
                } else if (Rule.P_TARGETURL.getName().equals(name) && value != null) {
                    targetUrl = value;
                } else if (Rule.P_SUBJECT.getName().equals(name) && value != null) {
                    subject = value;
                } else if (Rule.P_TYPE.getName().equals(name) && value != null) {
                    type = value;
                } else if (Rule.P_OBJECT.getName().equals(name) && value != null) {
                    object = value;
                } else if (Rule.P_INFO.getName().equals(name) && value != null) {
                    info = value;
                } else if (Common.P_BOX_NAME.getName().equals(name) && value != null) {
                    boxBound = true;
                }
            }

            String error = validateRule(external, subject, type, object, info, action, targetUrl, boxBound);
            if (error != null) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(error);
            }
        }
    }

    /**
     * Validate Rule.
     * @param unitUrl unit base url
     * @param external value of EventExternal
     * @param subject value of EventSubject
     * @param type value of EventType
     * @param object value of EventObject
     * @param info value of EventInfo
     * @param action value of Action
     * @param targetUrl value of TargetUrl
     * @param boxBound flag of box bounded
     * @return property name of format error
     */
    public static String validateRule(Boolean external, String subject,
            String type, String object, String info, String action, String targetUrl, Boolean boxBound) {

        // check if convert scheme to localunit
        String converted = UriUtils.convertSchemeFromHttpToLocalUnit(subject);
        if (converted != null && !converted.equals(subject)) {
            return Rule.P_SUBJECT.getName();
        }
        converted = UriUtils.convertSchemeFromHttpToLocalUnit(targetUrl);
        if (converted != null && !converted.equals(targetUrl)) {
            return Rule.P_TARGETURL.getName();
        }

        return validateRule(external, type, object, info, action, targetUrl, boxBound);
    }

    /**
     * Validate Rule.
     * @param external value of EventExternal
     * @param type value of EventType
     * @param object value of EventObject
     * @param info value of EventInfo
     * @param action value of Action
     * @param targetUrl value of TargetUrl
     * @param boxBound flag of box bounded
     * @return property name of format error
     */
    public static String validateRule(
            Boolean external,
            String type, String object, String info, String action, String targetUrl, Boolean boxBound) {

        if (!validateRuleExternal(external, type)) {
            return Rule.P_EXTERNAL.getName();
        }
        if (!validateRuleType(type, action)) {
            return Rule.P_TYPE.getName();
        }
        if (!validateRuleObject(object, external, type, boxBound)) {
            return Rule.P_OBJECT.getName();
        }
        if (!validateRuleTargetUrl(targetUrl, action, boxBound)) {
            return Rule.P_TARGETURL.getName();
        }

        return null; // valid
    }

    private static boolean validateRuleExternal(Boolean external, String type) {
        // type: timer.periodic or timer.oneshot -> external: false
        if ((PersoniumEventType.timerPeriodic().equals(type) || PersoniumEventType.timerOneshot().equals(type))
            && external.booleanValue()) {
            return false;
        }

        return true;
    }

    private static boolean validateRuleType(String type, String action) {
        // action: relay.data -> type: odata.create or odata.update or odata.patch
        if (Rule.ACTION_RELAY_DATA.equals(action)
            && !(PersoniumEventType.odata("", PersoniumEventType.Operation.CREATE).equals(type)
                 || PersoniumEventType.odata("", PersoniumEventType.Operation.UPDATE).equals(type)
                 || PersoniumEventType.odata("", PersoniumEventType.Operation.MERGE).equals(type))) {
            return false;
        }
        return true;
    }

    private static boolean validateRuleObject(String object, Boolean external, String type, Boolean boxBound) {
        // type: timer.periodic or timer.oneshot -> object: decimal number
        if (PersoniumEventType.timerPeriodic().equals(type) || PersoniumEventType.timerOneshot().equals(type)) {
            // object: > 0
            if (object == null || !ODataUtils.validateTime(object)) {
                return false;
            }
        } else if (boxBound.booleanValue()) {
            // external: false -> object: personium-localbox:/xxx or personium-localcell:/__xxx
            if (!external.booleanValue() && object != null
                    && !ODataUtils.isValidLocalBoxUrl(object)
                    && !(ODataUtils.isValidLocalCellUrl(object)
                            && object.startsWith(UriUtils.SCHEME_CELL_URI + "__"))) {
                return false;
            }
        } else {
            // external: false -> object: personium-localcell:/xxx
            if (!external.booleanValue() && object != null && !ODataUtils.isValidLocalCellUrl(object)) {
                return false;
            }
        }

        return true;
    }

    private static boolean validateRuleTargetUrl(String targetUrl, String action, Boolean boxBound) {
        // action: relay or relay.event or exec or relay.data -> targetUrl: not null
        if ((Rule.ACTION_RELAY.equals(action) || Rule.ACTION_RELAY_EVENT.equals(action)
                || Rule.ACTION_EXEC.equals(action) || Rule.ACTION_RELAY_DATA.equals(action)) && targetUrl == null) {
            return false;
        }

        if (Rule.ACTION_EXEC.equals(action)) {
            if (boxBound.booleanValue()) {
                // targetUrl: personium-localbox:/xxx
                if (!ODataUtils.isValidLocalBoxUrl(targetUrl)) {
                    return false;
                }
            } else {
                // targetUrl: personium-localcell:/xxx
                if (!ODataUtils.isValidLocalCellUrl(targetUrl)) {
                    return false;
                }
            }
        } else if (Rule.ACTION_RELAY.equals(action) || Rule.ACTION_RELAY_DATA.equals(action)) {
            if (boxBound.booleanValue()) {
                // targetUrl: personium-localunit:/xxx or http://xxx or https://xxx
                //            or personium-localcell:/xxx or personium-localbox:/xxx
                if (!ODataUtils.isValidUrl(targetUrl)
                    && !ODataUtils.isValidLocalUnitUrl(targetUrl)
                    && !ODataUtils.isValidLocalCellUrl(targetUrl)
                    && !ODataUtils.isValidLocalBoxUrl(targetUrl)) {
                    return false;
                }
            } else {
                // targetUrl: personium-localunit:/xxx or http://xxx or https://xxx
                //            or personium-localcell:/xxx
                if (!ODataUtils.isValidUrl(targetUrl)
                    && !ODataUtils.isValidLocalUnitUrl(targetUrl)
                    && !ODataUtils.isValidLocalCellUrl(targetUrl)) {
                    return false;
                }
            }
        } else if (Rule.ACTION_RELAY_EVENT.equals(action)
                   && !ODataUtils.isValidCellUrl(targetUrl)
                   && !targetUrl.equals(UriUtils.SCHEME_CELL_URI)) {
            // targetUrl: cell url or personium-localcell:/
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(String entitySetName, String object, String info, String op) {
        String type = PersoniumEventType.cellctl(entitySetName, op);
        postEventInternal(type, object, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postLinkEvent(String src, String object, String info, String target, String op) {
        String type = PersoniumEventType.cellctlLink(src, target, op);
        postEventInternal(type, object, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postNavPropEvent(String src, String object, String info, String target, String op) {
        String type = PersoniumEventType.cellctlNavProp(src, target, op);
        postEventInternal(type, object, info);
    }

    private void postEventInternal(String type, String object, String info) {
        PersoniumEvent ev = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(info)
                .davRsCmp(this.cellRsCmp)
                .build();
        EventBus eventBus = this.getAccessContext().getCell().getEventBus();
        eventBus.post(ev);
    }
}
