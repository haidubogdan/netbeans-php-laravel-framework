/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.php.laravel;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

public class GlobalDockerPreferences {

    private static final String DOCKER_EXEC_PATH = "docker_path"; //NOI18N
    private static final String DOCKER_EXEC_DEFAULT_PATH = "docker"; //NOI18N
    private static final String DOCKER_EXEC_MACOS_DEFAULT_PATH = "/usr/local/bin/docker"; //NOI18N

    public static Preferences getPreferences() {
        return NbPreferences.forModule(GlobalDockerPreferences.class);
    }

    public static String getDockerExecPath() {
        String defaultPath = DOCKER_EXEC_DEFAULT_PATH;
        if (isMac()) {
            defaultPath = DOCKER_EXEC_MACOS_DEFAULT_PATH;
        }
        return getPreferences().get(DOCKER_EXEC_PATH, defaultPath);
    }

    public static void setDockerExecPath(String path) {
        getPreferences().put(DOCKER_EXEC_PATH, path);
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
