package org.netbeans.modules.php.laravel.executable;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.SwingUtilities;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils.ExitStatus;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.modules.php.laravel.commands.ExecutableService.CommandLineProcessor;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.windows.InputOutput;

/**
 *
 * @author bhaidu
 */
public class RemoteDockerExecutable {

    private static final RequestProcessor RP = new RequestProcessor(RemoteDockerExecutable.class);
    public static final String MAIN_SCRIPT = "docker"; // NOI18N
    public static final String DOCKER_EXEC = "exec";
    private final ExecutionEnvironment env;
    private final DockerCommand dockerCommand;
    private TerminalComponent output = null;
    private PhpModule phpModule = null;
    private CommandLineProcessor lineProcessor = null;

    public RemoteDockerExecutable(ExecutionEnvironment env, DockerCommand dockerCommand,
            PhpModule phpModule) {
        this.env = env;
        this.dockerCommand = dockerCommand;
        this.phpModule = phpModule;
    }

    public RemoteDockerExecutable setCommandLineProcessor(CommandLineProcessor lineProcessor) {
        this.lineProcessor = lineProcessor;
        return this;
    }

    public RemoteDockerExecutable setTerminalOutput(TerminalComponent output) {
        this.output = output;
        return this;
    }

    public void extractCommands() {
        RP.post(new Runnable() {
            @Override
            public void run() {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getCommands();
                    }
                });
            }
        });
    }

    private void getCommands() {
        assert EventQueue.isDispatchThread();

        RP.post(new Runnable() {
            @Override
            public void run() {
                runGetCommands(env);
            }
        });
    }

    public void executeArtisanCommand(List<String> params) {
        RequestProcessor.Task commandTask = RP.create(new Runnable() {
            @Override
            public void run() {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        executeCommand(params);
                    }
                });
            }
        });
        commandTask.run();
    }

    private void executeCommand(List<String> params) {
        String fullCommand = StringUtils.implode(params, " ");
        String[] args = new String[]{DOCKER_EXEC, dockerCommand.dockerContainer,
            dockerCommand.bashPath, "-c", dockerCommand.command + " --ansi " + fullCommand};

        ExitStatus result = ProcessUtils.execute(env, MAIN_SCRIPT, args);

        if (!result.getErrorLines().isEmpty()) {
            outputErrorMessage(result);
            return;
        }

        if (output == null) {
            return;
        }

        Runnable awtTask = new Runnable() {
            @Override
            public void run() {
                if (!output.isOpened()) {
                    output.open();
                    output.requestActive();
                }

                InputOutput io = output.getIo();
                try {
                    io.getOut().reset();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                String commandPreview = MAIN_SCRIPT + " " + DOCKER_EXEC + dockerCommand.dockerContainer
                        + " " + dockerCommand.bashPath + " -c " + dockerCommand.command + " " + fullCommand;

                io.getOut().println(TerminalComponent.colorize(commandPreview));

                for (String line : result.getOutputLines()) {
                    io.getOut().println(line);
                }
            }
        };
        runTask(awtTask);
    }

    private void outputErrorMessage(ExitStatus result) {
        Runnable awtTask = new Runnable() {
            @Override
            public void run() {
                TerminalComponent errorOutput = TerminalComponent.getInstance(phpModule);

                if (!errorOutput.isOpened()) {
                    errorOutput.open();
                    errorOutput.requestActive();
                }
                InputOutput io = errorOutput.getIo();
                for (String line : result.getErrorLines()) {
                    io.getOut().println(line);
                }
            }
        };
        runTask(awtTask);
    }

    private void runTask(Runnable awtTask) {
        if (SwingUtilities.isEventDispatchThread()) {
            awtTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(awtTask);
            } catch (InterruptedException | InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    public void runGetCommands(ExecutionEnvironment env) {
        String[] args = new String[]{DOCKER_EXEC, dockerCommand.dockerContainer,
            dockerCommand.bashPath, "-c", dockerCommand.command + " --ansi"};

        ExitStatus result = ProcessUtils.execute(env, "docker", args);

        if (!result.getErrorLines().isEmpty()) {
            outputErrorMessage(result);
            return;
        }

        Runnable awtTask = new Runnable() {
            @Override
            public void run() {
                for (String ansiLine : result.getOutputLines()) {
                    String line = ansiLine.replaceAll("\u001B\\[[;\\d]*m", "");
                    lineProcessor.processLine(line);
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            awtTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(awtTask);
            } catch (InterruptedException | InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    public static class DockerCommand {

        public final String dockerContainer;
        public final String bashPath;
        public final String command;

        public DockerCommand(String dockerContainer, String bashPath, String command) {
            this.dockerContainer = dockerContainer;
            this.bashPath = bashPath;
            this.command = command;
        }
    }

}
