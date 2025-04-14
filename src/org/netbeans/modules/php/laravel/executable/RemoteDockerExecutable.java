package org.netbeans.modules.php.laravel.executable;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.modules.dlight.terminal.action.TerminalSupportImpl;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.HostInfo;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.nativeexecution.api.execution.NativeExecutionDescriptor;
import org.netbeans.modules.nativeexecution.api.execution.NativeExecutionService;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils.ExitStatus;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.modules.php.laravel.commands.ExecutableService.CommandLineProcessor;
import org.netbeans.modules.terminal.api.ui.IOTerm;
import org.netbeans.api.extexecution.base.ProcessBuilder;
import org.netbeans.modules.terminal.support.TerminalPinSupport;
import org.netbeans.modules.terminal.support.TerminalPinSupport.TerminalCreationDetails;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOContainer;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;
import org.netbeans.lib.terminalemulator.Term;
import org.netbeans.modules.nativeexecution.api.util.HostInfoUtils;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.laravel.ui.options.LaravelOptionsPanelController;
import org.openide.filesystems.FileUtil;

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
        final IOProvider ioProvider = IOProvider.get("Terminal"); // NOI18N
        final AtomicReference<InputOutput> ioRef = new AtomicReference<>();
        // Create a tab in EDT right after we call the method, don't let this 
        // work to be done in RP in asynchronous manner. We need this to
        // save tab order 
        final TerminalContainerTopComponent instance = TerminalContainerTopComponent.findInstance();
        instance.open();
        instance.requestActive();
        final IOContainer ioContainer = instance.getIOContainer();
        InputOutput io = ioProvider.getIO("Terminal 1", null, ioContainer);
        ioRef.set(io);
        final AtomicBoolean destroyed = new AtomicBoolean(false);

        final Runnable runnable2 = new Runnable() {
            private final Runnable delegate = () -> {
                if (SwingUtilities.isEventDispatchThread()) {
                    ioContainer.requestActive();
                } else {
                    doWork();
                }
            };

            @SuppressWarnings("PackageVisibleField")
            RequestProcessor.Task task = RP.create(delegate);

            private final OutputListener retryLink = new OutputListener() {
                @Override
                public void outputLineAction(OutputEvent ev) {
                    task.schedule(0);
                }

                @Override
                public void outputLineSelected(OutputEvent oe) {
                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
                }

                @Override
                public void outputLineCleared(OutputEvent oe) {
                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
                }
            };

            @Override
            public void run() {
                delegate.run();
            }

            private void doWork() {
                boolean verbose = env.isRemote(); // can use silentMode instead
                OutputWriter out = ioRef.get().getOut();

                if (!ConnectionManager.getInstance().isConnectedTo(env)) {
                    try {
                        if (verbose) {
                            out.println("No connection");
                        }
                        ConnectionManager.getInstance().connectTo(env);
                    } catch (IOException ex) {
                        if (!destroyed.get()) {
                            Throwable cause = ex.getCause();
                            String error = cause == null ? ex.getMessage() : cause.getMessage();
                            String msg = "TerminalAction.FailedToStart.text"; // NOI18N
                            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE));
                        }
                        return;
                    } catch (CancellationException ex) {
                        if (verbose) {
                            try {
                                out.print("LOG_Canceled");
                                out.println("LOG_Retry", retryLink);
                            } catch (IOException ignored) {
                            }
                        }
                        return;
                    } catch (ConnectionManager.CancellationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }

                final HostInfo hostInfo;
                String expandedDir = null;
                try {
                    // There is still a chance of env being disconnected
                    // (exception supressed in FetchHostInfoTask.compute)
                    if (!ConnectionManager.getInstance().isConnectedTo(env)) {
                        return;
                    }
                } catch (CancellationException ex) {
                    Exceptions.printStackTrace(ex);
                    return;
                }

                if (verbose) {
                    try {
                        // Erase "log" in case we successfully connected to host
                        out.reset();
                    } catch (IOException ex) {
                        // never thrown from TermOutputWriter
                    }
                }

                //start reverse
                try {
                    final Term term = IOTerm.term(ioRef.get());
                    term.setEmulation("xterm"); // NOI18N

                    CustomProcessBuilder npb = CustomProcessBuilder.newProcessBuilder(env);

                    // clear env modified by NB. Let it be initialized by started shell process
                    npb.getEnvironment().put("LD_LIBRARY_PATH", "");// NOI18N
                    npb.getEnvironment().put("DYLD_LIBRARY_PATH", "");// NOI18N
                    hostInfo = HostInfoUtils.getHostInfo(env);
                    if (hostInfo.getOSFamily() == HostInfo.OSFamily.WINDOWS) {
                        // /etc/profile changes directory to ${HOME} if this
                        // variable is not set.
                        npb.getEnvironment().put("CHERE_INVOKING", "1");// NOI18N
                    }

                    final TerminalPinSupport support = TerminalPinSupport.getDefault();
                    String envId = ExecutionEnvironmentFactory.toUniqueID(env);

                    //to check ate
                    //npb.addNativeProcessListener(new NativeProcessListener(ioRef.get(), destroyed));
                    String shell = hostInfo.getLoginShell();
                    if (expandedDir != null) {
                        npb.setWorkingDirectory(expandedDir);
                    }
//                            npb.setWorkingDirectory("${HOME}");
                    npb.setExecutable(shell);
                    if (shell.endsWith("bash") || shell.endsWith("bash.exe")) { // NOI18N
                        npb.setArguments("--login"); // NOI18N
                    }
                    //skip
                    CustomExecutionDescriptor descr;
                    descr = new CustomExecutionDescriptor().controllable(true).frontWindow(true).inputVisible(true).inputOutput(ioRef.get());
                    descr.postExecution(() -> {
                        ioRef.get().closeInputOutput();
                        support.close(term);
                    });
                    CustomExecutionService es = CustomExecutionService.newService(npb, descr, "Terminal Emulator"); // NOI18N
                    Future<Integer> result = es.run();
                    // ask terminal to become active
                    SwingUtilities.invokeLater(this);

                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ConnectionManager.CancellationException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {

                }
            }
        };
        RP.post(runnable2);

        if (1 == 1) {
            return;
        }

        final Runnable runnable = new Runnable() {
            private final Runnable delegate = () -> {
                if (SwingUtilities.isEventDispatchThread()) {
                    ioContainer.requestActive();
                } else {
                    doWork();
                }
            };

            @SuppressWarnings("PackageVisibleField")
            RequestProcessor.Task task = RP.create(delegate);

            private final OutputListener retryLink = new OutputListener() {
                @Override
                public void outputLineAction(OutputEvent ev) {
                    task.schedule(0);
                }

                @Override
                public void outputLineSelected(OutputEvent oe) {
                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
                }

                @Override
                public void outputLineCleared(OutputEvent oe) {
                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
                }
            };

            @Override
            public void run() {
                delegate.run();
            }

            private void doWork() {
                boolean verbose = env.isRemote(); // can use silentMode instead
                OutputWriter out = ioRef.get().getOut();

                long id = TerminalPinSupport.getDefault().createPinDetails(TerminalCreationDetails.create(IOTerm.term(ioRef.get()), 0, env.getDisplayName(), false));

                if (!ConnectionManager.getInstance().isConnectedTo(env)) {
                    try {
                        if (verbose) {
                            out.println("No connection");
                        }
                        ConnectionManager.getInstance().connectTo(env);
                    } catch (IOException ex) {
                        if (!destroyed.get()) {
                            Throwable cause = ex.getCause();
                            String error = cause == null ? ex.getMessage() : cause.getMessage();
                            String msg = "TerminalAction.FailedToStart.text"; // NOI18N
                            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE));
                        }
                        return;
                    } catch (CancellationException ex) {
                        if (verbose) {
                            try {
                                out.print("LOG_Canceled");
                                out.println("LOG_Retry", retryLink);
                            } catch (IOException ignored) {
                            }
                        }
                        return;
                    } catch (ConnectionManager.CancellationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }

                final HostInfo hostInfo;
                String expandedDir = null;
                try {
                    // There is still a chance of env being disconnected
                    // (exception supressed in FetchHostInfoTask.compute)
                    if (!ConnectionManager.getInstance().isConnectedTo(env)) {
                        return;
                    }
                } catch (CancellationException ex) {
                    Exceptions.printStackTrace(ex);
                    return;
                }

                if (verbose) {
                    try {
                        // Erase "log" in case we successfully connected to host
                        out.reset();
                    } catch (IOException ex) {
                        // never thrown from TermOutputWriter
                    }
                }

                try {
                    final Term term = IOTerm.term(ioRef.get());
                    // TODO: this is a temporary solution.

                    // Right now xterm emulation is not fully supported. (NB7.4)
                    // Still it has a very desired functionality - is recognises
                    // \ESC]%d;%sBEL escape sequences.
                    // Although \ESC]0;%sBEL is not implemented yet and window title
                    // is not set, it, at least, can skip the whole %s.
                    // This makes command prompt look better when this sequence is used
                    // in PS1 (ex. cygwin set this by default).
                    //
                    term.setEmulation("xterm"); // NOI18N

                    NativeProcessBuilder npb = NativeProcessBuilder.newProcessBuilder(env);
                    // clear env modified by NB. Let it be initialized by started shell process
                    npb.getEnvironment().put("LD_LIBRARY_PATH", "");// NOI18N
                    npb.getEnvironment().put("DYLD_LIBRARY_PATH", "");// NOI18N
                    hostInfo = HostInfoUtils.getHostInfo(env);
                    if (hostInfo.getOSFamily() == HostInfo.OSFamily.WINDOWS) {
                        // /etc/profile changes directory to ${HOME} if this
                        // variable is not set.
                        npb.getEnvironment().put("CHERE_INVOKING", "1");// NOI18N
                    }

                    final TerminalPinSupport support = TerminalPinSupport.getDefault();
                    String envId = ExecutionEnvironmentFactory.toUniqueID(env);

                    //to check ate
                    //npb.addNativeProcessListener(new NativeProcessListener(ioRef.get(), destroyed));
                    String shell = hostInfo.getLoginShell();
                    if (expandedDir != null) {
                        npb.setWorkingDirectory(expandedDir);
                    }
//                            npb.setWorkingDirectory("${HOME}");
                    npb.setExecutable(shell);
                    if (shell.endsWith("bash") || shell.endsWith("bash.exe")) { // NOI18N
                        npb.setArguments("--login"); // NOI18N
                    }

                    NativeExecutionDescriptor descr;
                    descr = new NativeExecutionDescriptor().controllable(true).frontWindow(true).inputVisible(true).inputOutput(ioRef.get());
                    descr.postExecution(() -> {
                        ioRef.get().closeInputOutput();
                        support.close(term);
                    });
                    NativeExecutionService es = NativeExecutionService.newService(npb, descr, "Terminal Emulator"); // NOI18N
                    Future<Integer> result = es.run();

                    try {
                        // if terminal can not be started then ExecutionException should be thrown
                        // wait one second to see if terminal can not be started. otherwise it's OK to exit by TimeOut

                        // IG: I've increased the timeout from 1 to 10 seconds.
                        // On slow hosts 1 sec was not enougth to get an error code from the pty
                        // No work is done after this call, so this change should be safe.
                        Integer rc = result.get(10, TimeUnit.SECONDS);
                        if (rc != 0) {
                            Logger.getLogger(TerminalSupportImpl.class.getName())
                                    .log(Level.INFO, "{0}{1}", new Object[]{"LOG_ReturnCode", rc});
                        }
                    } catch (TimeoutException ex) {
                        // we should be there
                    } catch (InterruptedException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (ExecutionException ex) {
                        if (!destroyed.get()) {
                            Throwable cause = ex.getCause();
                            String error = cause == null ? ex.getMessage() : cause.getMessage();
                            String msg = "TerminalAction.FailedToStart.text"; // NOI18N
                            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE));
                        }
                    }
                } catch (java.util.concurrent.CancellationException ex) { // VK: don't quite understand who can throw it?
                    Exceptions.printStackTrace(ex);
                    reportInIO(ioRef.get(), ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ConnectionManager.CancellationException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            private void reportInIO(InputOutput io, Exception ex) {
                if (io != null && ex != null) {
                    io.getErr().print(ex.getLocalizedMessage());
                }
            }
        };
        RP.post(runnable);
        if (1 == 1) {
            return;
        }

        String fullCommand = StringUtils.implode(params, " ");
        String[] args = new String[]{"sudo", "su"};

        ExitStatus result = ProcessUtils.execute(env, MAIN_SCRIPT, args);

        if (output == null && !result.getErrorLines().isEmpty()) {
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

    private void outputErrorMessage(String message) {
        Runnable awtTask = new Runnable() {
            @Override
            public void run() {
                TerminalComponent errorOutput = TerminalComponent.getInstance(phpModule);

                if (!errorOutput.isOpened()) {
                    errorOutput.open();
                    errorOutput.requestActive();
                }
                InputOutput io = errorOutput.getIo();
                io.getOut().println(message);
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
        if (env == null || env.getHost() == null || env.getUser() == null) {
            outputErrorMessage("Error! Remote terminal not configured.");
            return;
        }
        String[] args = new String[]{DOCKER_EXEC, dockerCommand.dockerContainer,
            dockerCommand.bashPath, "-c", dockerCommand.command + " --ansi"};

        ExitStatus result = ProcessUtils.execute(env, "docker", args);

        if (!result.getErrorLines().isEmpty() && !result.getOutputString().contains("Laravel Framework")) {
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
