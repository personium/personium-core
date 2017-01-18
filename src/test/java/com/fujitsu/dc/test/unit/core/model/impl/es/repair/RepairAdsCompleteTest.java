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
package com.fujitsu.dc.test.unit.core.model.impl.es.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.ads.AbstractAdsWriteFailureLog;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogException;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogWriter;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.impl.es.repair.RepairAds;
import com.fujitsu.dc.test.categories.Unit;

/**
 * RepairAdsTest ユニットテストクラス.
 */
@Category({Unit.class })
public class RepairAdsCompleteTest {

    static Logger log = LoggerFactory.getLogger(RepairAdsCompleteTest.class);
    private static final String PIO_VERSION_DUMMY = "1.4.1-test";
    private static final String TEST_ADS_LOGDIR = "./testdir";

    /**
     * すべてのテスト実行前の処理.
     */
    @BeforeClass
    public static void beforeClass() {
        // 他のテスト中で作成されたADS書込み失敗ログの削除
        File logDir = new File(DcCoreConfig.getAdsWriteFailureLogDir());
        File[] files = logDir.listFiles();
        for (File file : files) {
            file.delete();
        }
    }

    /**
     * 各テストの開始前に実行される処理.
     */
    @Before
    public void before() {
        // テスト用ログディレクトリが各テストで削除しきれていない場合を考慮して、最初にディレクトリを削除しておく
        File logDir = new File(TEST_ADS_LOGDIR);
        if (logDir.exists()) {
            File[] files = logDir.listFiles();
            for (File file : files) {
                file.delete();
            }
            logDir.delete();
        }
    }

    /**
     * 出力中のADS書き込み失敗ログファイルのみが存在しない場合にリペア終了判定でfalseが返却されること.
     */
    @Test
    public void 出力中のADS書き込み失敗ログファイルのみが存在しない場合にリペア終了判定でfalseが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ROTATE, PIO_VERSION_DUMMY,
                System.currentTimeMillis());
        File rotated = new File(dir, fileName);
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            rotated.createNewFile();
            file = getAdsWriteFailureLog(writer);
            assertFalse((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != rotated) {
                rotated.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * 出力中のADS書き込み失敗ログファイルのみが存在する場合にリペア終了判定でfalseが返却されること.
     */
    @Test
    public void 出力中のADS書き込み失敗ログファイルのみが存在する場合にリペア終了判定でfalseが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            writer.openActiveFile();
            file = getAdsWriteFailureLog(writer);
            assertFalse((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * 出力中とローテートされたADS書き込み失敗ログファイルが存在する場合にリペア終了判定でfalseが返却されること.
     */
    @Test
    public void 出力中とローテートされたADS書き込み失敗ログファイルが存在する場合にリペア終了判定でfalseが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ROTATE, PIO_VERSION_DUMMY,
                System.currentTimeMillis());
        File rotated = new File(dir, fileName);
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            rotated.createNewFile();
            writer.openActiveFile();
            file = getAdsWriteFailureLog(writer);
            assertFalse((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != rotated) {
                rotated.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * 論理削除されたADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でtrueが返却されること.
     */
    @Test
    public void 論理削除されたADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でtrueが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ROTATE, PIO_VERSION_DUMMY,
                System.currentTimeMillis()) + AbstractAdsWriteFailureLog.LOGICAL_DELETED_LOGNAME_SUFFIX;
        File rotated = new File(dir, fileName);
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            rotated.createNewFile();
            file = getAdsWriteFailureLog(writer);
            assertTrue((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != rotated) {
                rotated.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * エラー用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること.
     */
    @Test
    public void エラー用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ACTIVE, PIO_VERSION_DUMMY,
                System.currentTimeMillis()) + AbstractAdsWriteFailureLog.ERROR_LOGNAME_SUFFIX;
        File rotated = new File(dir, fileName);
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            rotated.createNewFile();
            file = getAdsWriteFailureErrorLog(writer);
            assertFalse((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != rotated) {
                rotated.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * リトライ用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること.
     */
    @Test
    public void リトライ用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ACTIVE, PIO_VERSION_DUMMY,
                System.currentTimeMillis()) + AbstractAdsWriteFailureLog.RETRY_LOGNAME_SUFFIX;
        File rotated = new File(dir, fileName);
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            rotated.createNewFile();
            file = getAdsWriteFailureRetryLog(writer);
            assertFalse((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != rotated) {
                rotated.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * ローテートされたリトライ用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること.
     */
    @Test
    public void ローテートされたリトライ用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ROTATE, PIO_VERSION_DUMMY,
                System.currentTimeMillis()) + AbstractAdsWriteFailureLog.RETRY_LOGNAME_SUFFIX;
        File rotated = new File(dir, fileName);
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            rotated.createNewFile();
            file = getAdsWriteFailureRotatedRetryLog(writer);
            assertFalse((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != rotated) {
                rotated.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * ローテートされたエラー用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること.
     */
    @Test
    public void ローテートされたエラー用のADS書き込み失敗ログファイルだけが存在する場合にリペア終了判定でfalseが返却されること() {
        RepairAds repair = RepairAds.getInstance();
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance("./", PIO_VERSION_DUMMY, true);
        File dir = new File(TEST_ADS_LOGDIR);
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            baseDir.set(writer, TEST_ADS_LOGDIR);
            if (!dir.mkdir()) {
                fail("mkdir failed(environment error): " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }

        File file = null;
        String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ROTATE, PIO_VERSION_DUMMY,
                System.currentTimeMillis()) + AbstractAdsWriteFailureLog.ERROR_LOGNAME_SUFFIX;
        File rotated = new File(dir, fileName);
        try {
            Class<?> clazz = RepairAds.class;
            Field baseDir = clazz.getDeclaredField("adsLogBaseDir");
            baseDir.setAccessible(true);
            baseDir.set(repair, new File(TEST_ADS_LOGDIR));
            Field version = clazz.getDeclaredField("pcsVersion");
            version.setAccessible(true);
            version.set(repair, PIO_VERSION_DUMMY);
            Method method = clazz.getDeclaredMethod("isRepairCompleted");
            method.setAccessible(true);

            rotated.createNewFile();
            file = getAdsWriteFailureRotatedErrorLog(writer);
            assertFalse((Boolean) method.invoke(repair));
        } catch (Exception e) {
            e.printStackTrace();
            fail("check failed");
        } finally {
            try {
                writer.closeActiveFile();
            } catch (AdsWriteFailureLogException e) {
                e.printStackTrace();
            }
            if (null != file) {
                file.delete();
            }
            if (null != rotated) {
                rotated.delete();
            }
            if (null != dir) {
                dir.delete();
            }
        }
    }

    /**
     * オープン済みの出力中ADS書き込み失敗ログのパスを取得する.
     * @param writer writer
     * @return ログのパス
     */
    private File getAdsWriteFailureLog(AdsWriteFailureLogWriter writer) {
        File file = null;
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            String baseDirV = (String) baseDir.get(writer);
            clazz = writer.getClass();
            Field createdTime = clazz.getDeclaredField("createdTime");
            createdTime.setAccessible(true);
            Long createdTimeV = (Long) createdTime.get(writer);
            final String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ACTIVE, PIO_VERSION_DUMMY,
                    createdTimeV);
            file = new File(baseDirV, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }
        return file;
    }

    /**
     * 出力中のエラー用ADS書き込み失敗ログのパスを取得する.
     * @param writer writer
     * @return ログのパス
     */
    private File getAdsWriteFailureErrorLog(AdsWriteFailureLogWriter writer) {
        File file = null;
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            String baseDirV = (String) baseDir.get(writer);
            clazz = writer.getClass();
            Field createdTime = clazz.getDeclaredField("createdTime");
            createdTime.setAccessible(true);
            Long createdTimeV = (Long) createdTime.get(writer);
            final String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ACTIVE, PIO_VERSION_DUMMY,
                    createdTimeV) + AbstractAdsWriteFailureLog.ERROR_LOGNAME_SUFFIX;
            file = new File(baseDirV, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }
        return file;
    }

    /**
     * 出力中のリトライ用のADS書き込み失敗ログのパスを取得する.
     * @param writer writer
     * @return ログのパス
     */
    private File getAdsWriteFailureRetryLog(AdsWriteFailureLogWriter writer) {
        File file = null;
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            String baseDirV = (String) baseDir.get(writer);
            clazz = writer.getClass();
            Field createdTime = clazz.getDeclaredField("createdTime");
            createdTime.setAccessible(true);
            Long createdTimeV = (Long) createdTime.get(writer);
            final String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ACTIVE, PIO_VERSION_DUMMY,
                    createdTimeV) + AbstractAdsWriteFailureLog.RETRY_LOGNAME_SUFFIX;
            file = new File(baseDirV, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }
        return file;
    }

    /**
     * 出力中のエラー用ADS書き込み失敗ログのパスを取得する.
     * @param writer writer
     * @return ログのパス
     */
    private File getAdsWriteFailureRotatedErrorLog(AdsWriteFailureLogWriter writer) {
        File file = null;
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            String baseDirV = (String) baseDir.get(writer);
            clazz = writer.getClass();
            Field createdTime = clazz.getDeclaredField("createdTime");
            createdTime.setAccessible(true);
            Long createdTimeV = (Long) createdTime.get(writer);
            final String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ROTATE, PIO_VERSION_DUMMY,
                    createdTimeV) + AbstractAdsWriteFailureLog.ERROR_LOGNAME_SUFFIX;
            file = new File(baseDirV, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }
        return file;
    }

    /**
     * 出力中のリトライ用のADS書き込み失敗ログのパスを取得する.
     * @param writer writer
     * @return ログのパス
     */
    private File getAdsWriteFailureRotatedRetryLog(AdsWriteFailureLogWriter writer) {
        File file = null;
        try {
            Class<?> clazz = AbstractAdsWriteFailureLog.class;
            Field baseDir = clazz.getDeclaredField("baseDir");
            baseDir.setAccessible(true);
            String baseDirV = (String) baseDir.get(writer);
            clazz = writer.getClass();
            Field createdTime = clazz.getDeclaredField("createdTime");
            createdTime.setAccessible(true);
            Long createdTimeV = (Long) createdTime.get(writer);
            final String fileName = String.format(AbstractAdsWriteFailureLog.LOGNAME_FORMAT_ROTATE, PIO_VERSION_DUMMY,
                    createdTimeV) + AbstractAdsWriteFailureLog.RETRY_LOGNAME_SUFFIX;
            file = new File(baseDirV, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            fail("configuration failed.");
        }
        return file;
    }

}
