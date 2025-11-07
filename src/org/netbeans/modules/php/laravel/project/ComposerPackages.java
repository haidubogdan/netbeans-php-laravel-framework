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
package org.netbeans.modules.php.laravel.project;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author bogdan
 */
public class ComposerPackages {

    private final PhpModule phpModule;
    private static final Map<String, ComposerPackages> INSTANCES = new WeakHashMap<>();
    private Map<String, Object> composerJsonContent = new HashMap<>();
    private boolean composerFileFound = false;
    
    private final String LARAVEL_PACKAGE_NAME = "laravel/framework"; //NOI18N

    private ComposerPackages(PhpModule phpModule) {
        this.phpModule = phpModule;
        this.extractPackageInfo();
    }

    private void extractPackageInfo() {
        FileObject sourceDir = phpModule.getSourceDirectory();

        if (sourceDir == null) {
            return;
        }
        FileObject composerJsonFile = sourceDir.getFileObject("composer.json"); //NOI18N

        if (composerJsonFile == null) {
            return;
        }

        JSONParser parser = new JSONParser();

        try {
            composerJsonContent = (Map<String, Object>) parser.parse(new FileReader(composerJsonFile.getPath()));
            composerFileFound = true;
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException | ParseException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public Map<String, Object> getcomposerJsonContent() {
        return composerJsonContent;
    }

    public String getLaravelVersion() {
        if (composerJsonContent == null) {
            return null;
        }
        Map<String, Object> require = (Map<String, Object>) composerJsonContent.get("require");

        if (require == null) {
            return null;
        }

        String laravelVersion = (String) require.get(LARAVEL_PACKAGE_NAME);
        return laravelVersion;
    }

    public static ComposerPackages fromPhpModule(PhpModule phpModule) {
        return new ComposerPackages(phpModule);
    }

    public static ComposerPackages getInstance(PhpModule phpModule) {
        String projectPath = phpModule.getProjectDirectory().getPath();

        synchronized (INSTANCES) {
            ComposerPackages composerPackage = INSTANCES.get(projectPath);
            if (composerPackage == null) {
                composerPackage = fromPhpModule(phpModule);
                INSTANCES.put(projectPath, composerPackage);
            }
            return composerPackage;
        }
    }

    public static ComposerPackages getInstance(String projectPath) {
        ComposerPackages composerPackage = INSTANCES.get(projectPath);
        return composerPackage;
    }

    public boolean composerFileFound() {
        return composerFileFound;
    }

    public boolean hasPhpModule() {
        return phpModule != null;
    }
}
