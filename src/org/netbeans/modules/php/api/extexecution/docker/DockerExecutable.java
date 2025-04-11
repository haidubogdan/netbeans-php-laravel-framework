package org.netbeans.modules.php.api.extexecution.docker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.php.api.extexecution.pty4j.PtyCliProcessBuilder;
import org.netbeans.modules.php.api.util.StringUtils;
import static org.netbeans.modules.php.laravel.PhpNbConsts.LARAVEL_UI_OPTIONS_PATH;
import org.netbeans.modules.php.laravel.commands.DlightTerminalEnvironment;
import org.openide.util.BaseUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 *
 * @author bhaidu
 */
public class DockerExecutable {

    public static final String DOCKER_COMMAND = "docker";// NOI18N
    public static final String DOCKER_EXEC = "exec";// NOI18N

    public static final ExecutionDescriptor DEFAULT_EXECUTION_DESCRIPTOR = new ExecutionDescriptor()
            .controllable(true)
            .frontWindow(true)
            .frontWindowOnError(true)
            .inputVisible(true)
            .showProgress(true);

    private final String containerName;
    private final List<String> commandArguments;
    private final String bashType;

    private boolean interactive = true;
    private boolean asTerminal = true;

    private String containerWorkDir;
    private boolean noInfo = false;

    private String terminalName = "PtyTerminal";// NOI18N

    private final boolean isRemote;

    public DockerExecutable(String containerName, String bashType,
            List<String> commandArguments, boolean isRemote) {
        assert containerName != null && !containerName.isEmpty();
        this.containerName = containerName;
        this.commandArguments = commandArguments;
        this.bashType = bashType;
        this.isRemote = isRemote;
    }

    public DockerExecutable displayName(String displayName) {
        this.terminalName = displayName;
        return this;
    }

    public DockerExecutable containerWorkDir(String containerWorkDir) {
        this.containerWorkDir = containerWorkDir;
        return this;
    }

    public DockerExecutable setUserInteractive(boolean status) {
        this.interactive = status;
        return this;
    }
    
    public DockerExecutable setUseTTY(boolean status) {
        this.asTerminal = status;
        return this;
    }

    public Future<Integer> run(ExecutionDescriptor executionDescriptor, ExecutionDescriptor.InputProcessorFactory2 outProcessorFactory) {
        Parameters.notNull("executionDescriptor", executionDescriptor); // NOI18N
        Callable<Process> processBuilder = getProcessBuilder();
        executionDescriptor = getExecutionDescriptor(executionDescriptor, outProcessorFactory);

        return ExecutionService.newService(processBuilder, executionDescriptor, terminalName).run();
    }

    private ExecutionDescriptor getExecutionDescriptor(ExecutionDescriptor executionDescriptor, ExecutionDescriptor.InputProcessorFactory2 outProcessorFactory) {
        final List<ExecutionDescriptor.InputProcessorFactory2> inputProcessors = new CopyOnWriteArrayList<>();
        // colors
        ExecutionDescriptor.InputProcessorFactory2 infoOutProcessorFactory = getInfoOutputProcessorFactory();
        if (infoOutProcessorFactory != null) {
            inputProcessors.add(infoOutProcessorFactory);
        }

        if (outProcessorFactory != null) {
            inputProcessors.add(outProcessorFactory);
        }
        if (!inputProcessors.isEmpty()) {
            executionDescriptor = executionDescriptor.outProcessorFactory(new ExecutionDescriptor.InputProcessorFactory2() {
                @Override
                public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                    InputProcessor[] processors = new InputProcessor[inputProcessors.size()];
                    for (int i = 0; i < inputProcessors.size(); ++i) {
                        processors[i] = inputProcessors.get(i).newInputProcessor(defaultProcessor);
                    }
                    return InputProcessors.proxy(processors);
                }
            });
        }
        return executionDescriptor;
    }

    private Callable<Process> getProcessBuilder() {
        List<String> arguments = new ArrayList<>();
        boolean isUnix = BaseUtilities.isUnix();
        if (!isRemote && !isUnix) {
            arguments.add(DOCKER_COMMAND);
        }

        arguments.add(DOCKER_EXEC);

        if (containerWorkDir != null && !containerWorkDir.trim().isEmpty()) {
            arguments.add("-w");
            arguments.add(containerWorkDir);
        }

        //docker options
        String interactionMode = ""; // NOI18N

        if (interactive) {
            interactionMode += "i"; // NOI18N
        }
        if (asTerminal) {
            interactionMode += "t"; // NOI18N
        }

        if (interactionMode.length() > 0) {
            arguments.add("-" + interactionMode);
        }

        arguments.add(containerName);
        arguments.add(bashType);
        arguments.add("-c"); // NOI18N

        //join all the command parameters
        String execCommand = String.join(" ", commandArguments); // NOI18N

        arguments.add(execCommand);

        if (isRemote) {
            ExecutionEnvironment env = DlightTerminalEnvironment.getRemoteConfig();
            NativeProcessBuilder processBuilder = NativeProcessBuilder.newProcessBuilder(env);
            processBuilder.setExecutable(DOCKER_COMMAND);
            processBuilder.setArguments(arguments.toArray(new String[0]));
            processBuilder.setUsePty(true);
            return processBuilder;
        } else if (isUnix) {
            NativeProcessBuilder processBuilder = NativeProcessBuilder.newLocalProcessBuilder();
            processBuilder.setExecutable(DOCKER_COMMAND);
            processBuilder.setArguments(arguments.toArray(new String[0]));
            processBuilder.setUsePty(true);
            return processBuilder;
        } else {
            PtyCliProcessBuilder processBuilder = new PtyCliProcessBuilder();

            processBuilder.setArguments(arguments);

            return processBuilder;
        }
    }

    private ExecutionDescriptor.InputProcessorFactory2 getInfoOutputProcessorFactory() {
        if (noInfo) {
            // no info
            return null;
        }
        return new ExecutionDescriptor.InputProcessorFactory2() {
            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                InputProcessor[] processors = new InputProcessor[2];
                processors[0] = new InfoInputProcessor(defaultProcessor, commandArguments);
                processors[1] = new FormatterProcessor(defaultProcessor);
                return InputProcessors.proxy(processors);
            }
        };
    }

    static final class InfoInputProcessor implements InputProcessor {

        private final InputProcessor defaultProcessor;
        private char lastChar;

        public InfoInputProcessor(InputProcessor defaultProcessor, List<String> fullCommand) {
            this.defaultProcessor = defaultProcessor;
            String infoCommand = colorize(getInfoCommand(fullCommand)) + "\n"; // NOI18N
            try {
                defaultProcessor.processInput(infoCommand.toCharArray());
            } catch (IOException ex) {
                int y = 1;
            }
        }

        @Override
        public void processInput(char[] chars) throws IOException {
            if (chars.length > 0) {
                lastChar = chars[chars.length - 1];
            }
        }

        @Override
        public void reset() throws IOException {
            // noop
        }

        @NbBundle.Messages("LBL_Docker1_InfoInputProcessor.done=Done.")
        @Override
        public void close() throws IOException {
            StringBuilder msg = new StringBuilder(Bundle.LBL_Docker1_InfoInputProcessor_done().length() + 2);
            if (!isNewLine(lastChar)) {
                msg.append("\n"); // NOI18N
            }
            msg.append(colorize(Bundle.LBL_Docker1_InfoInputProcessor_done()));
            msg.append("\n"); // NOI18N
            defaultProcessor.processInput(msg.toString().toCharArray());
        }

        public static String getInfoCommand(List<String> fullCommand) {
            List<String> escapedCommand = new ArrayList<>(fullCommand.size());
            for (String command : fullCommand) {
                escapedCommand.add("\"" + command.replace("\"", "\\\"") + "\""); // NOI18N
            }
            return StringUtils.implode(escapedCommand, " "); // NOI18N
        }

        private static String colorize(String msg) {
            return "\033[1;30m" + msg + "\033[0m"; // NOI18N
        }

        private static boolean isNewLine(char ch) {
            return ch == '\n' || ch == '\r' || ch == '\u0000'; // NOI18N
        }

    }

    public ExecutionDescriptor getDescriptor(Runnable postExecution) {
        ExecutionDescriptor executionDescriptor = DEFAULT_EXECUTION_DESCRIPTOR
                .optionsPath(LARAVEL_UI_OPTIONS_PATH);
        if (postExecution != null) {
            executionDescriptor = executionDescriptor.postExecution(postExecution);
        }
        return executionDescriptor;
    }

    private static class FormatterProcessor implements InputProcessor {

        private final InputProcessor delegate;

        private boolean closed;

        public FormatterProcessor(InputProcessor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void processInput(char[] chars) throws IOException {
            if (closed) {
                throw new IllegalStateException("Already closed processor");
            }

            String sequence = new String(chars);

            sequence = stripAnsiCursorStates(sequence);
            delegate.processInput(sequence.toCharArray());
        }

        @Override
        public void reset() throws IOException {
            if (closed) {
                throw new IllegalStateException("Already closed processor");
            }

            delegate.reset();
        }

        @Override
        public void close() throws IOException {
            closed = true;

            delegate.close();
        }

        /**
         * https://gist.github.com/fnky/458719343aabd01cfb17a3a4f7296797
         *
         * @param sequence
         * @return
         */
        private static String stripAnsiCursorStates(String sequence) {
            /**
             * ESC[?25l make cursor invisible ESC[?25h make cursor visible
             */

            String escAnsiToken = "[?25";

            if (!sequence.contains(escAnsiToken)) {
                return sequence;
            }

            StringBuilder sb = new StringBuilder(sequence);
            int escOffset;
            int counter = 0;
            while ((escOffset = sb.indexOf(escAnsiToken)) > 0) {
                sb.delete(escOffset - 1, escOffset + escAnsiToken.length() + 1);
                counter++;
                if (counter > 1000) {
                    break;
                }
            }

            return sb.toString();
        }
    }
}
