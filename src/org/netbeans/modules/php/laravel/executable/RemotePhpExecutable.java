package org.netbeans.modules.php.laravel.executable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.php.api.util.StringUtils;

import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 * Class usable for running any PHP executable (program or script).
 */
public class RemotePhpExecutable {

    private static final Logger LOGGER = Logger.getLogger(RemotePhpExecutable.class.getName());

    /**
     * The
     * {@link ExecutionDescriptor.InputProcessorFactory2 input processor factory}
     * that strips any
     * <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape
     * sequences</a>.
     * <p>
     * <b>In fact, it is not needed anymore since the Output window understands
     * ANSI escape sequences.</b>
     *
     * @see InputProcessors#ansiStripping(InputProcessor)
     * @since 0.28
     */
    public static final ExecutionDescriptor.InputProcessorFactory2 ANSI_STRIPPING_FACTORY = new ExecutionDescriptor.InputProcessorFactory2() {
        @Override
        public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
            return InputProcessors.ansiStripping(defaultProcessor);
        }
    };

    /**
     * This descriptor is:
     * <ul>
     * <li>{@link ExecutionDescriptor#isControllable() controllable}</li>
     * <li>{@link ExecutionDescriptor#isFrontWindow() displays the Output window}</li>
     * <li>{@link ExecutionDescriptor#isFrontWindowOnError()  displays the Output window on error (since 1.62)}</li>
     * <li>{@link ExecutionDescriptor#isInputVisible() has visible user input}</li>
     * <li>{@link ExecutionDescriptor#showProgress() shows progress}</li>
     * </ul>
     */
    public static final ExecutionDescriptor DEFAULT_EXECUTION_DESCRIPTOR = new ExecutionDescriptor()
            .controllable(true)
            .frontWindow(true)
            .frontWindowOnError(true)
            .inputVisible(true)
            .showProgress(true);

    private final String command;
    final List<String> fullCommand = new CopyOnWriteArrayList<>();

    private String displayName = null;
    private ExecutionEnvironment env;

    /**
     *
     * @param command raw command.
     */
    public RemotePhpExecutable(String command, ExecutionEnvironment env) {
        this.env = env;
        this.command = command.trim();
    }

    /**
     *
     * @return the command, in the original form (just without leading and
     * trailing whitespaces).
     */
    public String getCommand() {
        return command;
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
    public Future<Integer> runRemoteDocker(String dockerContainer,
            String bashPath, String command, ExecutionDescriptor executionDescriptor) {
        Parameters.notNull("executionDescriptor", executionDescriptor); // NOI18N

        NativeProcessBuilder processBuilder = NativeProcessBuilder.newProcessBuilder(env);
        processBuilder.setExecutable("docker").setArguments("exec", dockerContainer, "sh", "-c", command).redirectError(); // NOI18N
        if (processBuilder == null) {
            return null;
        }
        executionDescriptor = getExecutionDescriptor(executionDescriptor, null);
        return ExecutionService.newService(processBuilder, executionDescriptor, getDisplayName()).run();
    }

    private String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }
        return getDefaultDisplayName();
    }

    private String getDefaultDisplayName() {
        return "";
    }

    private ExecutionDescriptor getExecutionDescriptor(ExecutionDescriptor executionDescriptor, ExecutionDescriptor.InputProcessorFactory2 outProcessorFactory) {
        final List<ExecutionDescriptor.InputProcessorFactory2> inputProcessors = new CopyOnWriteArrayList<>();
        // colors
        ExecutionDescriptor.InputProcessorFactory2 infoOutProcessorFactory = getInfoOutputProcessorFactory();
        if (infoOutProcessorFactory != null) {
            inputProcessors.add(infoOutProcessorFactory);
        }
        // file output
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

    private ExecutionDescriptor.InputProcessorFactory2 getInfoOutputProcessorFactory() {
        return new ExecutionDescriptor.InputProcessorFactory2() {
            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                return InputProcessors.proxy(new InfoInputProcessor(defaultProcessor, fullCommand), defaultProcessor);
            }
        };
    }

    //~ Inner classes
    static final class InfoInputProcessor implements InputProcessor {

        private final InputProcessor defaultProcessor;
        private char lastChar;

        public InfoInputProcessor(InputProcessor defaultProcessor, List<String> fullCommand) {
            this.defaultProcessor = defaultProcessor;
            String infoCommand = colorize(getInfoCommand(fullCommand)) + "\n"; // NOI18N
            try {
                defaultProcessor.processInput(infoCommand.toCharArray());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, null, ex);
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

        @NbBundle.Messages("InfoInputProcessor.done=Done.")
        @Override
        public void close() throws IOException {
            StringBuilder msg = new StringBuilder(Bundle.InfoInputProcessor_done().length() + 2);
            if (!isNewLine(lastChar)) {
                msg.append("\n"); // NOI18N
            }
            msg.append(colorize(Bundle.InfoInputProcessor_done()));
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

    static final class RedirectOutputProcessor implements InputProcessor {

        private final File fileOuput;
        private final Charset outputCharset;

        private OutputStream outputStream;

        public RedirectOutputProcessor(File fileOuput, Charset outputCharset) {
            assert fileOuput != null;
            assert outputCharset != null;
            this.fileOuput = fileOuput;
            this.outputCharset = outputCharset;
        }

        @Override
        public void processInput(char[] chars) throws IOException {
            if (outputStream == null) {
                outputStream = new BufferedOutputStream(new FileOutputStream(fileOuput));
            }
            ByteBuffer byteBuffer = outputCharset.encode(CharBuffer.wrap(chars));
            byte[] bytes = byteBuffer.array();
            byte[] compactedBytes = new byte[byteBuffer.limit()];
            System.arraycopy(bytes, 0, compactedBytes, 0, compactedBytes.length);
            outputStream.write(compactedBytes);
        }

        @Override
        public void reset() {
            // noop
        }

        @Override
        public void close() throws IOException {
            if (outputStream != null) {
                outputStream.close();
            }
        }

    }

}
