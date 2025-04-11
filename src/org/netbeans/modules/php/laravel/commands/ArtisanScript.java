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
package org.netbeans.modules.php.laravel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.api.executable.PhpExecutableValidator;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.project.ComposerPackages;
import org.netbeans.modules.php.laravel.ui.options.LaravelOptionsPanelController;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author bogdan
 */
public class ArtisanScript {

    public static final String SCRIPT_NAME = "artisan"; // NOI18N
    private static final String SHELL_INTERACTIVE = "SHELL_INTERACTIVE"; // NOI18N
    public static final List<String> DEFAULT_PARAMS = Collections.singletonList("--ansi"); // NOI18N
    private final String artisanPath;

    private ArtisanScript(String artisanPath) {
        this.artisanPath = artisanPath;
    }

    @CheckForNull
    public static ArtisanScript forPhpModule(PhpModule phpModule, boolean warn) {

        FileObject artisanFo = phpModule.getProjectDirectory().getFileObject(SCRIPT_NAME);
        assert artisanFo != null;
        String artisanPath = FileUtil.toFile(artisanFo).getAbsolutePath();
        String error = validate(artisanPath);
        if (error == null) {
            return new ArtisanScript(artisanPath);
        }
        if (warn) {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(
                    error,
                    NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(message);
        }

        return null;
    }

    public static String validate(String command) {
        return PhpExecutableValidator.validateCommand(command, "Laravel artisan"); // NOI18N
    }

    public void runCommand(PhpModule phpModule, List<String> parameters, Runnable postExecution) {
        createExecutable(phpModule)
                .displayName(getDisplayName(phpModule))
                .additionalParameters(getAllParameters(parameters))
                .run(getDescriptor(postExecution));
    }

    private PhpExecutable createExecutable(PhpModule phpModule) {
        return new PhpExecutable(artisanPath)
                .environmentVariables(Collections.singletonMap(SHELL_INTERACTIVE, "true")) // NOI18N
                .workDir(FileUtil.toFile(phpModule.getSourceDirectory()));
    }

    private String getDisplayName(PhpModule phpModule) {
        String laravelVersion = ComposerPackages.getInstance(phpModule).getLaravelVersion();
        return phpModule.getDisplayName() + " " + laravelVersion + " CLI"; // NOI18N
    }

    private List<String> getAllParameters(List<String> params) {
        List<String> allParams = new ArrayList<>(DEFAULT_PARAMS.size() + params.size());
        allParams.addAll(DEFAULT_PARAMS);
        allParams.addAll(params);
        return allParams;
    }
    
    
    private ExecutionDescriptor getDescriptor(Runnable postExecution) {
        ExecutionDescriptor executionDescriptor = PhpExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                .optionsPath(LaravelOptionsPanelController.getOptionsPath())
                .inputVisible(true);
        if (postExecution != null) {
            executionDescriptor = executionDescriptor.postExecution(postExecution);
        }
        return executionDescriptor;
    }

}
