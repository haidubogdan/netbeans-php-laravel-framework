package org.netbeans.modules.php.laravel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.executable.DockerExecutable;
import org.netbeans.modules.php.laravel.executable.RemoteDockerExecutable;
import org.netbeans.modules.php.laravel.executable.TerminalComponent;
import org.netbeans.modules.php.laravel.ui.options.LaravelOptionsPanelController;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.modules.php.laravel.project.ComposerPackages;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author bhaidu
 */
public class ExecutableService {

    private static final List<String> DEFAULT_PARAMS = Collections.singletonList("--ansi"); // NOI18N

    public static List<FrameworkCommand> extractArtisanCommands(PhpModule phpModule,
            ArtisanCommandSupport artisanCommandSupport) {
        List<FrameworkCommand> commands = new ArrayList<>();

        CommandLineProcessor lineProcessor = new CommandLineProcessor(artisanCommandSupport, phpModule);

        if (useRemoteConnection(phpModule)) {
            if (useDocker(phpModule)) {
                RemoteDockerExecutable dockerExec = getRemoteDockerExecutable(phpModule);
                dockerExec.setCommandLineProcessor(lineProcessor);
                dockerExec.extractCommands();
            } else {
                throw new UnsupportedOperationException("Not implemented yet");
            }
        } else if (useDocker(phpModule)) {
            DockerExecutable dockerExec = getLocalDockerExecutable(phpModule);
            dockerExec.setCommandLineProcessor(lineProcessor);
            dockerExec.extractCommands();
        } else {
            ExecutionDescriptor executionDescriptor = PhpExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                    .optionsPath(LaravelOptionsPanelController.getOptionsPath())
                    .inputVisible(true);

            createPhpExecutable(phpModule)
                    .displayName(getDisplayName(phpModule))
                    .additionalParameters(DEFAULT_PARAMS)
                    .run(executionDescriptor, getOutProcessorFactory(lineProcessor));
        }

        return commands;
    }

    public static List<FrameworkCommand> executeCommand(PhpModule phpModule,
            ArtisanCommandSupport artisanCommandSupport,
            List<String> params) {
        List<FrameworkCommand> commands = new ArrayList<>();

        if (useRemoteConnection(phpModule)) {
            if (useDocker(phpModule)) {
                TerminalComponent output = TerminalComponent.getInstance(phpModule);
                RemoteDockerExecutable dockerExec = getRemoteDockerExecutable(phpModule);
                dockerExec.setTerminalOutput(output);
                dockerExec.executeArtisanCommand(params);
            } else {
                throw new UnsupportedOperationException("Not implemented yet");
            }
        } else if (useDocker(phpModule)) {
            TerminalComponent output = TerminalComponent.getInstance(phpModule);
            DockerExecutable dockerExec = getLocalDockerExecutable(phpModule);
            dockerExec.setTerminalOutput(output);
            dockerExec.executeArtisanCommand(params);
        } else {
            ExecutionDescriptor executionDescriptor = PhpExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                    .optionsPath(LaravelOptionsPanelController.getOptionsPath())
                    .inputVisible(true);

            createPhpExecutable(phpModule)
                    .displayName(getDisplayName(phpModule))
                    .additionalParameters(params)
                    .run(executionDescriptor);
        }

        return commands;
    }

    private static String getDisplayName(PhpModule phpModule) {
        String laravelVersion = ComposerPackages.getInstance(phpModule).getLaravelVersion();
        return "Laravel " + laravelVersion + " CLI";
    }

    private static String getPreScript(PhpModule phpModule) {
        return LaravelPreferences.getPreScript(phpModule);
    }

    private static boolean useRemoteConnection(PhpModule phpModule) {
        return LaravelPreferences.getRemoteConnectionFlag(phpModule);
    }

    private static boolean useDocker(PhpModule phpModule) {
        return LaravelPreferences.getUseDocker(phpModule);
    }

    private static String getDockerContainerName(PhpModule phpModule) {
        return LaravelPreferences.getDockerContainerName(phpModule);
    }

    private static String getDockerBashPath(PhpModule phpModule) {
        return LaravelPreferences.getDockerBashPath(phpModule);
    }

    private static PhpExecutable createPhpExecutable(PhpModule phpModule) {
        String absolutePath = FileUtil.toFile(phpModule.getSourceDirectory()).getAbsolutePath();
        return new PhpExecutable(absolutePath + "/artisan") //??
                .environmentVariables(Collections.singletonMap("SHELL_INTERACTIVE", "true")) // NOI18N
                .workDir(FileUtil.toFile(phpModule.getSourceDirectory()));
    }

    private static RemoteDockerExecutable getRemoteDockerExecutable(PhpModule phpModule) {
        ExecutionEnvironment env = DlightTerminalEnvironment.getRemoteConfig();
        String dockerContainer = getDockerContainerName(phpModule);
        String bashPath = getDockerBashPath(phpModule);

        String command;

        String preScript = getPreScript(phpModule);

        if (preScript != null && !preScript.isEmpty()) {
            command = preScript + " " + "php artisan ";
        } else {
            command = "php artisan ";
        }

        return new RemoteDockerExecutable(env,
                new RemoteDockerExecutable.DockerCommand(dockerContainer, bashPath, command), phpModule);
    }

    private static DockerExecutable getLocalDockerExecutable(PhpModule phpModule) {
        String dockerContainer = getDockerContainerName(phpModule);
        String bashPath = getDockerBashPath(phpModule);

        String command;

        String preScript = getPreScript(phpModule);

        if (preScript != null && !preScript.isEmpty()) {
            command = preScript + " " + "php artisan ";
        } else {
            command = "php artisan ";
        }

        return new DockerExecutable(
                new DockerExecutable.DockerCommand(dockerContainer, bashPath, command), phpModule);
    }

    private static ExecutionDescriptor.InputProcessorFactory2 getOutProcessorFactory(final LineProcessor lineProcessor) {
        return new ExecutionDescriptor.InputProcessorFactory2() {
            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                return InputProcessors.ansiStripping(InputProcessors.bridge(lineProcessor));
            }
        };
    }

    public static class CommandLineProcessor implements LineProcessor {

        private final ArtisanCommandSupport artisanCommandSupport;
        private final PhpModule phpModule;
        private boolean collectCommands = false;

        public CommandLineProcessor(ArtisanCommandSupport artisanCommandSupport, PhpModule phpModule) {
            this.artisanCommandSupport = artisanCommandSupport;
            this.phpModule = phpModule;
        }

        @Override
        public void processLine(String line) {
            if (collectCommands) {
                if (line.startsWith("  ")) {
                    String trimedLine = line.trim();
                    int endBoundry = trimedLine.indexOf(" ");
                    String commandInfo = trimedLine.substring(0, endBoundry);
                    int commandActionPos = commandInfo.indexOf(":");
                    if (commandActionPos > 0) {
                        String comment = "";
                        if (line.length() > endBoundry + 1) {
                            comment = line.substring(endBoundry + 2).trim();
                        }

                        artisanCommandSupport.commands.add(new ArtisanCommand(phpModule, commandInfo,
                                comment, commandInfo));
                    }
                }
            }
            if (line.contains("Available commands:")) {
                collectCommands = true;
            }
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {

        }
    }
}
