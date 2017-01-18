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
package com.fujitsu.dc.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreMessageUtils.Severity;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;


/**
 * URLの作成の組立を行う関数群.
 */
@RunWith(DcRunner.class)
@Category({ Unit.class })
public final class DcCoreExceptionTest {

    /**
     * 例外発生時のメッセージ生成のテスト.
     */
    @Test
    public void 例外発生時のメッセージ生成のテスト() {
        try {
            throw DcCoreException.OData.JSON_PARSE_ERROR;
        } catch (DcCoreException e) {
            Assert.assertEquals("JSON parse error.", e.getMessage());
        }
    }

    /**
     * レスポンスコードからログレベル判定処理.
     */
    @Test
    public void レスポンスコードからログレベル判定処理() {
        // 400系はINFO
        Assert.assertEquals(Severity.INFO, DcCoreException.decideSeverity(400));
        Assert.assertEquals(Severity.INFO, DcCoreException.decideSeverity(401));
        Assert.assertEquals(Severity.INFO, DcCoreException.decideSeverity(405));
        Assert.assertEquals(Severity.INFO, DcCoreException.decideSeverity(412));
        Assert.assertEquals(Severity.INFO, DcCoreException.decideSeverity(499));

        // 500系はWARN
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(500));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(502));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(505));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(512));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(599));

        // 400以下はWARN
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(399));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(302));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(300));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(201));
        Assert.assertEquals(Severity.WARN, DcCoreException.decideSeverity(200));
    }

    /**
     * メッセージコードのフォーマット異常.
     */
    @Test(expected = IllegalArgumentException.class)
    public void メッセージコードのフォーマット異常時に実行時例外が発生すること() {
        DcCoreException.create("UNKNOWN");
    }
}
