/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

/**
 * .
 */
public class SingleJUnitTestRunner {
    private SingleJUnitTestRunner() {
    }

    /**
     * .
     * @param args .
     * @throws ClassNotFoundException .
     */
    public static void main(String... args) throws ClassNotFoundException {
        int retCode = 0;
        String resultMessage = "SUCCESS";
        String[] classAndMethod = args[0].split("#");
        Request request = Request.method(Class.forName(classAndMethod[0]),
                classAndMethod[1]);

        Result result = new JUnitCore().run(request);
        if (!result.wasSuccessful()) {
            retCode = 1;
            resultMessage = "FAILURE";
        }
        System.out.println(resultMessage);
        System.exit(retCode);
    }
}
