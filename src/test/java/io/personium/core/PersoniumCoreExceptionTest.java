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
package io.personium.core;

import io.personium.plugin.base.PluginMessageUtils.Severity;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;


/**
 * URLの作成の組立を行う関数群.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class })
public final class PersoniumCoreExceptionTest {

    /**
     * 例外発生時のメッセージ生成のテスト.
     */
    @Test
    public void 例外発生時のメッセージ生成のテスト() {
        try {
            throw PersoniumCoreException.OData.JSON_PARSE_ERROR;
        } catch (PersoniumCoreException e) {
            Assert.assertEquals("JSON parse error.", e.getMessage());
        }
    }

    /**
     * レスポンスコードからログレベル判定処理.
     */
    @Test
    public void レスポンスコードからログレベル判定処理() {
        // 400系はINFO
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(400));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(401));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(405));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(412));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(499));

        // 500系はWARN
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(500));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(502));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(505));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(512));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(599));

        // 400以下はWARN
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(399));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(302));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(300));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(201));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(200));
    }

    /**
     * メッセージコードのフォーマット異常.
     */
    @Test(expected = IllegalArgumentException.class)
    public void メッセージコードのフォーマット異常時に実行時例外が発生すること() {
        PersoniumCoreException.create("UNKNOWN");
    }
}
