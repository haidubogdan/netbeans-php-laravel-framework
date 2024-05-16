package org.netbeans.modules.php.laravel.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.modules.dlight.api.terminal.TerminalSupport;
import org.netbeans.modules.dlight.terminal.ui.TerminalContainerTopComponent;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.nativeexecution.api.execution.NativeExecutionDescriptor;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.executable.RemotePhpExecutable;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommandSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOColorPrint;
import org.openide.windows.IOColors;
import org.openide.windows.IOContainer;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author bhaidu
 */
public class LaravelCommandSupport extends FrameworkCommandSupport {

    private static final RequestProcessor RP = new RequestProcessor("LaravelCommand", 1); // NOI18N
    private final String laravelPath; //??

    public LaravelCommandSupport(PhpModule phpModule) {
        super(phpModule);
        this.laravelPath = "artisan";
    }

    @Override
    public String getFrameworkName() {
        return "Laravel";
    }

    @Override
    public void runCommand(CommandDescriptor commandDescriptor, Runnable postExecution) {
        String[] commands = commandDescriptor.getFrameworkCommand().getCommands();
        String[] commandParams = commandDescriptor.getCommandParams();
        List<String> params = new ArrayList<>(commands.length + commandParams.length);
        params.addAll(Arrays.asList(commands));
        params.addAll(Arrays.asList(commandParams));
        ExecutionEnvironment env = DlightTerminalEnvironment.getRemoteConfig();
        String dockerContainer = "lmc_back_v2"; 
        String bashPath = "sh";
        String command = "php artisan";
        
        RP.post(new Runnable() {
            @Override
            public void run() {
                createRemoteExecutable(phpModule, env)
                        //.displayName(getDisplayName(phpModule))
                        //                .additionalParameters(getAllParameters(parameters))
                        .runRemoteDocker(dockerContainer, bashPath, command, getDescriptor(postExecution));
            }
        });
    }

    private ExecutionDescriptor getDescriptor(Runnable postExecution) {
        ExecutionDescriptor executionDescriptor = RemotePhpExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                //.optionsPath(SymfonyOptionsPanelController.getOptionsPath())
                .inputVisible(true);
        if (postExecution != null) {
            executionDescriptor = executionDescriptor.postExecution(postExecution);
        }
        return executionDescriptor;
    }

    private String getDisplayName(PhpModule phpModule) {
        return "Laravel CLI";
    }

    private RemotePhpExecutable createRemoteExecutable(PhpModule phpModule, ExecutionEnvironment env) {
        return new RemotePhpExecutable(laravelPath, env);
    }

    @Override
    protected String getOptionsPath() {
        return null;
    }

    @Override
    protected File getPluginsDirectory() {
        FileObject sourceDirectory = phpModule.getSourceDirectory();
        if (sourceDirectory == null) {
            // broken project
            return null;
        }
        FileObject vendor = sourceDirectory.getFileObject("vendor"); // NOI18N
        if (vendor != null && vendor.isFolder()) {
            return FileUtil.toFile(vendor);
        }
        return null;
    }

    @Override
    protected List<FrameworkCommand> getFrameworkCommandsInternal() {
        List<FrameworkCommand> commands = new ArrayList<>();
        commands.add(new ArtisanCommand(phpModule, "controller", "test c", "controller preview"));
        return commands;
    }

}
