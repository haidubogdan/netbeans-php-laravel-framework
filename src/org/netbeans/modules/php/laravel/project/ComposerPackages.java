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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import static org.netbeans.modules.php.laravel.PhpNbConsts.NB_PHP_PROJECT_TYPE;
import org.netbeans.spi.project.LookupProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public class ComposerPackages {

    private final FileObject projectDir;
    private Map<String, Object> composerJsonContent = new HashMap<>();
    
    private final String LARAVEL_PACKAGE_NAME = "laravel/framework"; //NOI18N
    public static final String COMPOSER_FILENAME = "composer.json"; //NOI18N
    private String laravelVersion;

    private ComposerPackages(FileObject projectDir) {
        this.projectDir = projectDir;
        this.extractPackageInfo();
    }

    private void extractPackageInfo() {
        if (projectDir == null) {
            return;
        }

        FileObject composerJsonFile = projectDir.getFileObject(COMPOSER_FILENAME); //NOI18N

        if (composerJsonFile == null) {
            return;
        }

        JSONParser parser = new JSONParser();

        try {
            composerJsonContent = (Map<String, Object>) parser.parse(new FileReader(composerJsonFile.getPath()));

            if (composerJsonContent != null) {
                extractLaravelVersion();
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException | ParseException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public Map<String, Object> getcomposerJsonContent() {
        return composerJsonContent;
    }

    private String extractLaravelVersion() {
        if (composerJsonContent == null) {
            return null;
        }
        Map<String, Object> require = (Map<String, Object>) composerJsonContent.get("require");

        if (require == null) {
            return null;
        }

        laravelVersion = (String) require.get(LARAVEL_PACKAGE_NAME);
        return laravelVersion;
    }

    public String getLaravelVersion() {
        return laravelVersion;
    }
    
    public static ComposerPackages fromProjectDir(FileObject projectDir) {
        return new ComposerPackages(projectDir);
    }

    public static ComposerPackages loadFromPhpModule(PhpModule phpModule) {
        Project project = phpModule.getLookup().lookup(Project.class);
        assert project != null;
        return project.getLookup().lookup(ComposerPackages.class);
    }

    @LookupProvider.Registration(projectType = NB_PHP_PROJECT_TYPE)
    public static LookupProvider createJavaBaseProvider() {
        return new LookupProvider() {
            @Override
            public Lookup createAdditionalLookup(Lookup baseContext) {
                Project project = baseContext.lookup(Project.class);
                return Lookups.fixed(
                        fromProjectDir(project.getProjectDirectory())
                );
            }
        };
    }
}
