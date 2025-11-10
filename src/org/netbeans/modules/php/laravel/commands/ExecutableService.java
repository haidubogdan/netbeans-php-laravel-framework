package org.netbeans.modules.php.laravel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import static org.netbeans.modules.php.laravel.commands.ArtisanCommand.ARTISAN_COMMAND;
import org.netbeans.modules.php.api.extexecution.docker.DockerExecutable;
import org.netbeans.modules.php.laravel.PhpNbConsts;
import org.netbeans.modules.php.laravel.ui.options.LaravelOptionsPanelController;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.modules.php.laravel.project.ComposerPackages;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author bhaidu
 */
public class ExecutableService {

    public static void extractArtisanCommands(PhpModule phpModule,
            ArtisanCommandSupport artisanCommandSupport) {

        CommandLineProcessor commandsProcessor = new CommandLineProcessor(artisanCommandSupport, phpModule);
        List<String> params = new ArrayList<>();
        executeCommand(phpModule, params, commandsProcessor);
    }

    public static void executeCommand(PhpModule phpModule,
            List<String> params, LineProcessor outLineProcessor) {

        if (useDocker(phpModule)) {
            params.add(0, PhpNbConsts.PHP_COMMAND);
            params.add(1, ArtisanScript.SCRIPT_NAME);
            boolean isRemote = useRemoteConnection(phpModule);
            DockerExecutable exec = new DockerExecutable(
                    getDockerContainerName(phpModule),
                    getDockerBashPath(phpModule),
                    params,
                    isRemote
            ).displayName(getDisplayName(phpModule))
                    .containerWorkDir(getDockerWorkdir(phpModule))
                    .setUserInteractive(getDockerUseInteractive(phpModule))
                    .setUseTTY(getDockerUseTTY(phpModule))
                    .setDockerUser(getDockerUser(phpModule))
                    ;

            ExecutionDescriptor.InputProcessorFactory2 descriptor = null;

            if (outLineProcessor != null) {
                descriptor = getOutPtyProcessorFactory(outLineProcessor);
            }

            exec.run(exec.getDescriptor(null), descriptor);
        } else {
            ExecutionDescriptor executionDescriptor = PhpExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                    .optionsPath(LaravelOptionsPanelController.getOptionsPath())
                    .inputVisible(true);

            ExecutionDescriptor.InputProcessorFactory2 descriptor = null;

            if (outLineProcessor != null) {
                descriptor = getOutProcessorFactory(outLineProcessor);
            }

            createPhpExecutable(phpModule)
                    .displayName(getDisplayName(phpModule))
                    .additionalParameters(params)
                    .run(executionDescriptor, descriptor);
        }
    }

    private static String getDisplayName(PhpModule phpModule) {
        ComposerPackages composerPackages = ComposerPackages.loadFromPhpModule(phpModule);

        if (composerPackages == null || composerPackages.getLaravelVersion() == null) {
            return phpModule.getDisplayName() + " CLI"; // NOI18N
        }

        return phpModule.getDisplayName() + " " + composerPackages.getLaravelVersion() + " CLI"; // NOI18N
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

    private static String getDockerWorkdir(PhpModule phpModule) {
        return LaravelPreferences.geDockerWorkdir(phpModule);
    }

    private static String getDockerBashPath(PhpModule phpModule) {
        return LaravelPreferences.getDockerBashPath(phpModule);
    }

    private static boolean getDockerUseInteractive(PhpModule phpModule) {
        return LaravelPreferences.getDockerUseInteractive(phpModule);
    }

    private static boolean getDockerUseTTY(PhpModule phpModule) {
        return LaravelPreferences.getDockerUseTTy(phpModule);
    }
    
    private static String getDockerUser(PhpModule phpModule) {
        return LaravelPreferences.getDockerUser(phpModule);
    }
    
    private static PhpExecutable createPhpExecutable(PhpModule phpModule) {
        String absolutePath = FileUtil.toFile(phpModule.getSourceDirectory()).getAbsolutePath();
        return new PhpExecutable(absolutePath + "/" + ARTISAN_COMMAND)
                .environmentVariables(Collections.singletonMap("SHELL_INTERACTIVE", "true")) // NOI18N
                .workDir(FileUtil.toFile(phpModule.getSourceDirectory()));
    }

    private static ExecutionDescriptor.InputProcessorFactory2 getOutProcessorFactory(final LineProcessor lineProcessor) {
        return new ExecutionDescriptor.InputProcessorFactory2() {
            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                //strip ansi chars before extracting the commands
                return InputProcessors.ansiStripping(InputProcessors.bridge(lineProcessor));
            }
        };
    }

    private static ExecutionDescriptor.InputProcessorFactory2 getOutPtyProcessorFactory(final LineProcessor lineProcessor) {
        return new ExecutionDescriptor.InputProcessorFactory2() {
            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                //strip ansi chars before extracting the commands
                return InputProcessors.proxy(InputProcessors.bridge(lineProcessor));
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
            line = stripAnsiColors(line);
            if (collectCommands) {
                if (line.startsWith("  ")) {
                    String trimedLine = line.trim();
                    int endBoundry = trimedLine.indexOf(" ");
                    String commandInfo = trimedLine.substring(0, endBoundry);
                    int commandActionPos = commandInfo.indexOf(":");
                    String comment = "";
                    if (line.length() > endBoundry + 1) {
                        comment = line.substring(endBoundry + 2).trim();
                    }
                    if (commandActionPos > 0) {
                        artisanCommandSupport.addCommand(new ArtisanCommand(phpModule, commandInfo,
                                comment, commandInfo));
                    } else {
                        artisanCommandSupport.addCommand(new ArtisanCommand(phpModule, commandInfo,
                                comment, commandInfo));
                    }
                }
            }
            if (line.contains("Available commands:")) { // NOI18N
                collectCommands = true;
            }
        }

        private static String stripAnsiColors(String sequence) {
            StringBuilder sb = new StringBuilder(sequence.length());
            int index = 0;
            int max = sequence.length();
            while (index < max) {
                int nextEscape = sequence.indexOf("\033[", index); // NOI18N
                if (nextEscape == -1) {
                    nextEscape = sequence.length();
                }

                for (int n = (nextEscape == -1) ? max : nextEscape; index < n; index++) {
                    sb.append(sequence.charAt(index));
                }

                if (nextEscape != -1) {
                    for (; index < max; index++) {
                        char c = sequence.charAt(index);
                        if (c == 'm') {
                            index++;
                            break;
                        }
                    }
                }
            }

            return sb.toString();
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {

        }
    }
}
