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
package com.fujitsu.dc.test.unit.core.model.impl.es.doc;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;

import com.fujitsu.dc.common.es.response.DcGetResponse;
import com.fujitsu.dc.common.es.response.DcSearchHit;
import com.fujitsu.dc.core.model.ctl.CtlSchema;
import com.fujitsu.dc.core.model.impl.es.doc.OEntityDocHandler;
import com.fujitsu.dc.core.odata.OEntityWrapper;
import com.fujitsu.dc.test.categories.Unit;

/**
 * OEntityDocHandlerのユニットテスト.
 */
@Category({Unit.class })
public class OEntityDocHandlerTest {

    static final Long VERSION_0 = 2L;
    static final String ID_0 = "uuid00123";
    static final Date DATE_0 = new Date();
    static final Date DATE_1 = new Date();
    static final String ENT_SET_NAME_ACCOUNT = "Account";
    static final String ACCOUNT_P_NAME = "Name";
    static final String ACCOUNT_H_HC = "HashedCredential";
    static final String ACCOUNT_NAME_0 = "account1";
    static final String ACCOUNT_HC_0 = "hhss";

    /**
     * GetResponseを引数にとるコンストラクタのテスト.
     */
    @Test
    public void testOEntityDocHandlerGetResponse() {
        Map<String, Object> src = this.createMockSrc();

        // Mockの作成
        DcGetResponse gRes = Mockito.mock(DcGetResponse.class);
        Mockito.when(gRes.id()).thenReturn(ID_0);
        Mockito.when(gRes.getId()).thenReturn(ID_0);
        Mockito.when(gRes.version()).thenReturn(VERSION_0);
        Mockito.when(gRes.getVersion()).thenReturn(VERSION_0);
        Mockito.when(gRes.getSource()).thenReturn(src);
        Mockito.when(gRes.sourceAsMap()).thenReturn(src);

        // テスト
        OEntityDocHandler oedh = new OEntityDocHandler(gRes);
        assertEquals(VERSION_0, oedh.getVersion());
        assertEquals(ID_0, oedh.getId());
    }

    /**
     * SearchHitを引数にとるコンストラクタのテスト.
     */
    @Test
    public void testOEntityDocHandlerSearchHit() {
        Map<String, Object> src = this.createMockSrc();

        // Mockの作成
        DcSearchHit hit = Mockito.mock(DcSearchHit.class);
        Mockito.when(hit.id()).thenReturn(ID_0);
        Mockito.when(hit.getId()).thenReturn(ID_0);
        Mockito.when(hit.version()).thenReturn(VERSION_0);
        Mockito.when(hit.getVersion()).thenReturn(VERSION_0);
        Mockito.when(hit.getSource()).thenReturn(src);
        Mockito.when(hit.sourceAsMap()).thenReturn(src);

        // テスト
        OEntityDocHandler oedh = new OEntityDocHandler(hit);
        assertEquals(VERSION_0, oedh.getVersion());
        assertEquals(ID_0, oedh.getId());
    }

    /**
     * Accountスキーマに従ったSrcを作成して返す.
     * @return
     */
    Map<String, Object> createMockSrc() {
        Map<String, Object> src = new HashMap<String, Object>();

        src.put("u", DATE_1.getTime());
        src.put("p", DATE_0.getTime());
        Map<String, Object> dyn = new HashMap<String, Object>();
        Map<String, Object> stat = new HashMap<String, Object>();
        Map<String, Object> hidden = new HashMap<String, Object>();

        stat.put(ACCOUNT_P_NAME, ACCOUNT_NAME_0);
        hidden.put(ACCOUNT_H_HC, ACCOUNT_HC_0);
        dyn.put("dynkey1", "dynval1");
        src.put("d", dyn);
        src.put("s", stat);
        src.put("h", hidden);
        return src;
    }

    OEntityDocHandler createTestHandler() {
        Map<String, Object> src = this.createMockSrc();

        // Mockの作成
        DcGetResponse gRes = Mockito.mock(DcGetResponse.class);
        Mockito.when(gRes.id()).thenReturn(ID_0);
        Mockito.when(gRes.getId()).thenReturn(ID_0);
        Mockito.when(gRes.version()).thenReturn(VERSION_0);
        Mockito.when(gRes.getVersion()).thenReturn(VERSION_0);
        Mockito.when(gRes.getSource()).thenReturn(src);
        Mockito.when(gRes.sourceAsMap()).thenReturn(src);
        // Handlerの作成
        return new OEntityDocHandler(gRes);
    }

    /**
     * createOEntityメソッドのテスト.
     */
    @Test
    public void testCreateOEntity() {
        // Handlerの作成
        OEntityDocHandler oedh = this.createTestHandler();

        // AccountのEntitySetをもとに
        EdmDataServices.Builder ds = CtlSchema.getEdmDataServicesForCellCtl();
        EdmEntitySet entitySet = ds.build().findEdmEntitySet(ENT_SET_NAME_ACCOUNT);

        // 被テストメソッドであるcreateOEntityを走行させる。
        OEntityWrapper oew = oedh.createOEntity(entitySet);

        // 作成されたOEntityWrapper上で

        // Entityセットが正しくセットされていることの確認
        assertEquals(ENT_SET_NAME_ACCOUNT, oew.getEntitySetName());

        // Account.name が設定された通りになっていることを確認
        assertEquals(ACCOUNT_NAME_0, (String) oew.getProperty(ACCOUNT_P_NAME).getValue());

        // Hidden項目である HashedCredential が設定された通りになっていることを確認
        assertEquals(ACCOUNT_HC_0, oew.get(ACCOUNT_H_HC));

        // ETagが仕様通り取れることの確認
        assertEquals(oedh.getVersion() + "-" + oedh.getUpdated(), oew.getEtag());

    }

}
