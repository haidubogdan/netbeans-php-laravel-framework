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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import static org.netbeans.modules.php.laravel.ArtisanScript.SCRIPT_NAME;
import static org.netbeans.modules.php.laravel.ArtisanScript.validate;
import org.netbeans.modules.php.laravel.api.DockerExecutable;
import org.netbeans.modules.php.laravel.api.DockerExecutable2;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Acting like a layer for multiple executable options
 *
 * @author bogdan.haidu
 */
public class ScriptExecutable {

    private File workDir = null;
    private List<String> additionalParameters = Collections.<String>emptyList();
    private Map<String, String> environmentVariables = Collections.<String, String>emptyMap();
    private String displayName = null;
    private final String scriptName;
    //TODO implement a config solution
    private final PhpModule phpModule;

    private ScriptExecutable(String scriptName, PhpModule phpModule) {
        this.scriptName = scriptName;
        this.phpModule = phpModule;
    }
    
    @CheckForNull
    public static ScriptExecutable forPhpModule(PhpModule phpModule, String scriptName) {
        return new ScriptExecutable(scriptName, phpModule);
    }

    /**
     * Set working directory for {@link #run() running} this executable.
     * <p>
     * The default value is {@code null} ("unknown" directory).
     *
     * @param workDir working directory for {@link #run() running} this
     * executable
     * @return the ScriptExecutable instance itself
     */
    public ScriptExecutable workDir(@NonNull File workDir) {
        this.workDir = workDir;
        return this;
    }

    /**
     * Set addition parameters for {@link #run() running}.
     * <p>
     * The default value is empty list (it means no additional parameters).
     *
     * @param additionalParameters addition parameters for
     * {@link #run() running}.
     * @return the ScriptExecutable instance itself
     */
    public ScriptExecutable additionalParameters(@NonNull List<String> additionalParameters) {
        this.additionalParameters = additionalParameters;
        return this;
    }

    /**
     * Set display name that is used for executable running (as a title of the
     * Output window).
     * <p>
     * The default value is {@link #getExecutable() executable} with
     * {@link #getParameters() parameters}.
     *
     * @param displayName display name that is used for executable running
     * @return the ScriptExecutable instance itself
     */
    public ScriptExecutable displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Set addition parameters for {@link #run() running}.
     * <p>
     * The default value is empty list (it means no additional parameters).
     *
     * @param environmentVariables addition parameters for
     * {@link #run() running}.
     * @return the ScriptExecutable instance itself
     */
    public ScriptExecutable environmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
        return this;
    }

    /**
     * Run this executable with the given execution descriptor.
     * <p>
     * <b>WARNING:</b> If any
     * {@link ExecutionDescriptor.InputProcessorFactory2 output processor factory}
     * should be used, use
     * {@link PhpExecutable#run(ExecutionDescriptor, ExecutionDescriptor.InputProcessorFactory2) run(ExecutionDescriptor, ExecutionDescriptor.InputProcessorFactory2)}
     * instead.
     *
     * @param executionDescriptor
     * @return task representing the actual run, value representing result of
     * the {@link Future} is exit code of the process or {@code null} if the
     * executable cannot be run
     * @see #run()
     * @see #run(ExecutionDescriptor)
     * @see ExecutionService#run()
     */
    @CheckForNull
    public Future<Integer> run(@NonNull ExecutionDescriptor executionDescriptor) {
        return run(executionDescriptor, (ExecutionDescriptor.InputProcessorFactory2) null);
    }

    /**
     * Run this executable with the given execution descriptor and optional
     * output processor factory.
     * <p>
     * @param executionDescriptor execution descriptor to be used
     * @param outProcessorFactory output processor factory to be used, can be
     * {@code null}
     * @return task representing the actual run, value representing result of
     * the {@link Future} is exit code of the process or {@code null} if the
     * executable cannot be run
     * @see #run()
     * @see #run(ExecutionDescriptor)
     * @see #run(ExecutionDescriptor,
     * ExecutionDescriptor.InputProcessorFactory2)
     * @see ExecutionService#run()
     * @since 0.28
     */
    @CheckForNull
    public Future<Integer> run(@NonNull ExecutionDescriptor executionDescriptor, @NullAllowed ExecutionDescriptor.InputProcessorFactory2 outProcessorFactory) {
        return runInternal(executionDescriptor, outProcessorFactory);
    }

    private Future<Integer> runInternal(ExecutionDescriptor executionDescriptor, ExecutionDescriptor.InputProcessorFactory2 outProcessorFactory) {
        Future<Integer> result;
        
        if (!useDocker()){
            PhpExecutable phpExecutable = new PhpExecutable(scriptName);
            result = phpExecutable.displayName(displayName)
                .workDir(workDir)
                .additionalParameters(additionalParameters)
                .environmentVariables(environmentVariables)
                .run(executionDescriptor, outProcessorFactory);
        } else {
            if (1>1) {
                DockerExecutable2 dockerExecutable = new DockerExecutable2(scriptName);
                if (environmentVariables.isEmpty()){
                    environmentVariables = new HashMap<>();
                }
                if (getDockerWorkdir() != null) {
                    environmentVariables.put("-w", getDockerWorkdir());
                }
                result = dockerExecutable.displayName(displayName)
                    .workDir(workDir)
                    .additionalParameters(additionalParameters)
                    .environmentVariables(environmentVariables)
                    .run(executionDescriptor, outProcessorFactory);
            }
            result = null;
            DockerExecutable de = new DockerExecutable();
            de.run();
        }

        return result;
    }
    
    private boolean useDocker() {
        return LaravelPreferences.getUseDocker(phpModule);
    }
    
    private String getDockerWorkdir() {
        return LaravelPreferences.getDockerWorkdir(phpModule);
    }
}
