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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.api.executable.PhpExecutableValidator;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.laravel.commands.ArtisanCommand;
import org.netbeans.modules.php.laravel.project.ComposerPackages;
import org.netbeans.modules.php.laravel.ui.options.LaravelOptionsPanelController;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.InputOutput;

/**
 *
 * @author bogdan
 */
public class ArtisanScript {

    public static final String SCRIPT_NAME = "artisan"; // NOI18N
    public static final List<String> DEFAULT_PARAMS = Collections.singletonList("--ansi"); // NOI18N

    public static final String OPTIONS_ID = "Laravel"; // NOI18N
    public static final String OPTIONS_SUB_PATH = UiUtils.FRAMEWORKS_AND_TOOLS_SUB_PATH + "/" + OPTIONS_ID; // NOI18N
    private static final String LIST_COMMAND = "list"; // NOI18N
    private static final String TAB_SPACE = "  "; // NOI18N

    private final String artisanPath;

    private ArtisanScript(String artisanPath) {
        this.artisanPath = artisanPath;
    }

    public static ArtisanScript forPhpModule(PhpModule phpModule, boolean warn) {

        FileObject artisanFo = phpModule.getProjectDirectory().getFileObject(SCRIPT_NAME);
        assert artisanFo != null;
        String artisanPath = FileUtil.toFile(artisanFo).getAbsolutePath();
        String error = validate(artisanPath);

        if (error != null && warn) {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(
                    error,
                    NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(message);
        }

        return new ArtisanScript(artisanPath);
    }

    public List<FrameworkCommand> getCommands(PhpModule phpModule) {
        List<String> listCommandSg = Collections.singletonList(LIST_COMMAND);
        CommandsLineProcessor lineProcessor = new CommandsLineProcessor(phpModule);
        List<FrameworkCommand> freshCommands;
        Future<Integer> task = createExecutable(phpModule)
                .displayName(getDisplayName(phpModule))
                .workDir(FileUtil.toFile(phpModule.getSourceDirectory()))
                .additionalParameters(listCommandSg)
                .run(getSilentDescriptor(), getOutProcessorFactory(lineProcessor));
        try {
            if (task != null && task.get().intValue() == 0) {
                freshCommands = lineProcessor.getCommands();
                if (!freshCommands.isEmpty()) {
                    return freshCommands;
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
//            LOGGER.log(Level.INFO, null, ex);
        }
        // error => rerun with output window
        runCommand(phpModule, listCommandSg, null);

        return null;
    }

    public ScriptExecutable createExecutable(PhpModule phpModule) {
        return ScriptExecutable.forPhpModule(phpModule, artisanPath);
    }

    public static String validate(String command) {
        return PhpExecutableValidator.validateCommand(command, "Laravel artisan"); // NOI18N
    }

    public void runCommand(PhpModule phpModule, List<String> parameters, Runnable postExecution) {
        createExecutable(phpModule)
                .displayName(getDisplayName(phpModule))
                .workDir(FileUtil.toFile(phpModule.getSourceDirectory()))
                .additionalParameters(getAllParameters(parameters))
                .run(getDescriptor(postExecution));
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

    /**
     * @return full IDE options Laravel path
     */
    public static String getOptionsPath() {
        return UiUtils.FRAMEWORKS_AND_TOOLS_OPTIONS_PATH + "/" + getOptionsSubPath(); // NOI18N
    }

    /**
     * @return IDE options Laravel subpath
     */
    public static String getOptionsSubPath() {
        return OPTIONS_SUB_PATH;
    }

    private ExecutionDescriptor getSilentDescriptor() {
        return new ExecutionDescriptor()
                .inputOutput(InputOutput.NULL);
    }

    private ExecutionDescriptor.InputProcessorFactory2 getOutProcessorFactory(final LineProcessor lineProcessor) {
        return new ExecutionDescriptor.InputProcessorFactory2() {
            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                return InputProcessors.ansiStripping(InputProcessors.bridge(lineProcessor));
            }
        };
    }

    static final class CommandsLineProcessor implements LineProcessor {

        private final PhpModule phpModule;
        private boolean startProcessing = false;

        // @GuardedBy(commands)
        private final List<FrameworkCommand> commands = new LinkedList<>();

        public CommandsLineProcessor(PhpModule phpModule) {
            this.phpModule = phpModule;
        }

        @Override
        public void processLine(String line) {
            if (startProcessing) {
                if (line.startsWith(TAB_SPACE)) {
                    String trimedLine = line.trim();
                    int endBoundry = trimedLine.indexOf(" "); // NOI18N
                    String commandInfo = trimedLine.substring(0, endBoundry);

                    String comment = "";// NOI18N
                    if (line.length() > endBoundry + 1) {
                        comment = line.substring(endBoundry + 2).trim();
                    }
                    synchronized (commands) {
                        commands.add(new ArtisanCommand(phpModule, commandInfo,
                                comment, commandInfo));
                    }
                }
            }
            if (line.contains("Available commands:")) {
                startProcessing = true;
            }
        }

        public List<FrameworkCommand> getCommands() {
            List<FrameworkCommand> copy;
            synchronized (commands) {
                copy = new ArrayList<>(commands);
            }
            return copy;
        }

        @Override
        public void close() {
        }

        @Override
        public void reset() {
        }

    }

}
