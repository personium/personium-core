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
import io.personium.core.model.DavRsCmp;
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
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param accessContext AccessContext
     * @param pCredHeader X-Personium-Credentialヘッダ
     * @param davRsCmp davRsCmp
     */
    public CellCtlResource(final AccessContext accessContext, final String pCredHeader, DavRsCmp davRsCmp) {
        super(accessContext, UriUtils.SCHEME_LOCALCELL + ":/__ctl/", ModelFactory.ODataCtl.cellCtl(accessContext
                .getCell()));
        this.pCredHeader = pCredHeader;
        this.davRsCmp = davRsCmp;
    }

    @Override
    public void checkAccessContext(final AccessContext ac, Privilege privilege) {
        this.davRsCmp.checkAccessContext(ac, privilege);
    }

    /**
     * 認証に使用できるAuth Schemeを取得する.
     * @return 認証に使用できるAuth Scheme
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return this.davRsCmp.getAcceptableAuthScheme();
    }

    @Override
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {
        return this.davRsCmp.hasPrivilege(ac, privilege);
    }

    @Override
    public void checkSchemaAuth(AccessContext ac) {
    }

    @Override
    public void beforeCreate(final OEntityWrapper oEntityWrapper) {
        String entitySetName = oEntityWrapper.getEntitySet().getName();
        String hPassStr = AuthUtils.checkValidatePassword(pCredHeader, entitySetName);
        if (hPassStr != null) {
            oEntityWrapper.put("HashedCredential", hPassStr);
        }
    }

    @Override
    public void beforeUpdate(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        String entitySetName = oEntityWrapper.getEntitySet().getName();
        String hPassStr = AuthUtils.checkValidatePassword(pCredHeader, entitySetName);
        if (hPassStr != null) {
            oEntityWrapper.put("HashedCredential", hPassStr);
        }
    }

    /**
     * サービスメタデータリクエストに対応する.
     * @return JAX-RS 応答オブジェクト
     */
    @GET
    @Path("{first: \\$}metadata")
    public Response getMetadata() {
        return super.doGetMetadata();
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    @Path("{first: \\$}metadata")
    public Response optionsMetadata() {
        return super.doGetOptionsMetadata();
    }

    @Override
    public Privilege getNecessaryReadPrivilege(String entitySetNameStr) {
        // セルレベルはエンティティセットごとに権限が異なる
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
        // セルレベルはエンティティセットごとに権限が異なる
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
        // CellレベルAPIはバッチリクエストに対応していないため、ここでは何もしない
    }

    /**
     * Not Implemented. <br />
     * 現状、$batchのアクセス制御でのみ必要なメソッドのため未実装. <br />
     * アクセスコンテキストが$batchしてよい権限を持っているかを返す.
     * @param ac アクセスコンテキスト
     * @return true: アクセスコンテキストが$batchしてよい権限を持っている
     */
    @Override
    public boolean hasPrivilegeForBatch(AccessContext ac) {
        throw new NotImplementedException();
    }

    @Override
    public void validate(String entitySetName, List<OProperty<?>> props) {
        if (Rule.EDM_TYPE_NAME.equals(entitySetName)) {
            // get properties
            String action = null;
            String service = null;
            String object = null;
            String boxname = null;
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
                } else if (Rule.P_SERVICE.getName().equals(name) && value != null) {
                    service = value;
                } else if (Rule.P_OBJECT.getName().equals(name) && value != null) {
                    object = value;
                } else if (Common.P_BOX_NAME.getName().equals(name) && value != null) {
                    boxname = value;
                }
            }

            // action: relay or relay.event or exec -> service: not null
            if ((Rule.ACTION_RELAY.equals(action) || Rule.ACTION_RELAY_EVENT.equals(action)
                    || Rule.ACTION_EXEC.equals(action)) && service == null) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());
            }

            // boxname: not null
            if (boxname != null) {
                // object: personium-localbox:/xxx
                if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());
                }
                // action: exec -> service: personium-localbox:/xxx
                if (Rule.ACTION_EXEC.equals(action) && !service.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());
                }
            } else {
                // object: personium-localcell:/xxx
                if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());
                }
                // action: exec -> service: personium-localcell:/xxx
                if (Rule.ACTION_EXEC.equals(action) && !service.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());
                }
            }

            // action: relay -> service: personium-localunit: or http: or https:
            if (Rule.ACTION_RELAY.equals(action)
                    && !service.startsWith(UriUtils.SCHEME_HTTP)
                    && !service.startsWith(UriUtils.SCHEME_HTTPS)
                    && !service.startsWith(UriUtils.SCHEME_LOCALUNIT)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());
            }

            // action: relay.event -> service: cell url
            if (Rule.ACTION_RELAY_EVENT.equals(action)
                    && !ODataUtils.isValidCellUrl(service)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());
            }
        }
    }

    @Override
    public void postEvent(String entitySetName, String object, String info, String op) {
        String type = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                + entitySetName + PersoniumEventType.SEPALATOR + op;
        PersoniumEvent ev = new PersoniumEvent(PersoniumEvent.INTERNAL_EVENT, type, object, info, this.davRsCmp);
        EventBus eventBus = this.getAccessContext().getCell().getEventBus();
        eventBus.post(ev);
    }
}
