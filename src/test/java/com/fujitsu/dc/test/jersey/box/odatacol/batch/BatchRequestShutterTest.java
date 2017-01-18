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
package com.fujitsu.dc.test.jersey.box.odatacol.batch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.HttpMethod;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.rs.odata.BatchRequestShutter;
import com.fujitsu.dc.test.categories.Unit;

/**
 * BatchRequestShutterのテスト.
 */
@Category({Unit.class })
public class BatchRequestShutterTest {

    /**
     * 初期状態ではシャッターOFFであること.
     */
    @Test
    public void 初期状態ではシャッターOFFであること() {
        BatchRequestShutter shutter = new BatchRequestShutter();
        assertFalse(shutter.isShuttered());
    }

    /**
     * TooManyConcurrentが発生した場合シャッターONの状態になること.
     */
    @Test
    public void TooManyConcurrentが発生した場合シャッターONの状態になること() {
        Exception e = DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS;

        BatchRequestShutter shutter = new BatchRequestShutter();
        assertFalse(shutter.isShuttered());

        shutter.updateStatus(e);
        assertTrue(shutter.isShuttered());
    }

    /**
     * TooManyConcurrent以外の例外が発生した場合シャッターONの状態にならないこと.
     */
    @Test
    public void TooManyConcurrent以外の例外が発生した場合シャッターONの状態にならないこと() {
        Exception e = DcCoreException.Server.GET_LOCK_STATE_ERROR;

        BatchRequestShutter shutter = new BatchRequestShutter();
        assertFalse(shutter.isShuttered());

        shutter.updateStatus(e);
        assertFalse(shutter.isShuttered());
    }

    /**
     * 一度でもTooManyConcurrentが発生した場合シャッターONの状態が継続すること.
     */
    @Test
    public void 一度でもTooManyConcurrentが発生した場合シャッターONの状態が継続すること() {
        Exception e1 = DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS;
        Exception e2 = DcCoreException.OData.CONFLICT_DUPLICATED_ENTITY;

        BatchRequestShutter shutter = new BatchRequestShutter();
        assertFalse(shutter.isShuttered());

        shutter.updateStatus(e1);
        assertTrue(shutter.isShuttered());

        shutter.updateStatus(e2);
        assertTrue(shutter.isShuttered());
    }

    /**
     * シャッターOFFの場合すべてのメソッドが許可されること.
     */
    @Test
    public void シャッターOFFの場合すべてのメソッドが許可されること() {
        BatchRequestShutter shutter = new BatchRequestShutter();
        assertFalse(shutter.isShuttered());

        assertTrue(shutter.accept(HttpMethod.POST));
        assertTrue(shutter.accept(HttpMethod.GET));
        assertTrue(shutter.accept(HttpMethod.PUT));
        assertTrue(shutter.accept(HttpMethod.DELETE));
    }

    /**
     * シャッターONの場合GETメソッドのみ許可されること.
     */
    @Test
    public void シャッターONの場合GETメソッドのみ許可されること() {
        Exception e = DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS;

        BatchRequestShutter shutter = new BatchRequestShutter();
        assertFalse(shutter.isShuttered());

        shutter.updateStatus(e);

        // チェック
        assertFalse(shutter.accept(HttpMethod.POST));
        assertTrue(shutter.accept(HttpMethod.GET));
        assertFalse(shutter.accept(HttpMethod.PUT));
        assertFalse(shutter.accept(HttpMethod.DELETE));
    }

}
