package org.netbeans.modules.php.laravel.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.executable.RemotePhpExecutable;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommandSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.laravel.utils.StringUtils;

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

        if (useRemoteConnection()) {
            //REMOTE WITH docker
            ExecutionEnvironment env = DlightTerminalEnvironment.getRemoteConfig();
            String dockerContainer = getDockerContainerName();
            String bashPath = getDockerBashPath();

            String command;

            String preScript = getPreScript();

            if (preScript != null && !preScript.isEmpty()) {
                command = preScript + " " + "php artisan " + StringUtils.implode(params, " ");
            } else {
                command = "php artisan " + StringUtils.implode(params, " ");
            }

            RP.post(new Runnable() {
                @Override
                public void run() {
                    RemotePhpExecutable exec = createRemoteExecutable(phpModule, env);

                    if (useDocker()) {
                        exec.runRemoteDocker(dockerContainer, bashPath, command, getDescriptor(postExecution));
                    } else {
                        exec.runRemote(dockerContainer, bashPath, command, getDescriptor(postExecution));
                    }
                }
            });
        } else if (useDocker()) {
            throw new UnsupportedOperationException("Feature incomplete.");
        } else {
            createPhpExecutable(phpModule)
                    .displayName(getDisplayName(phpModule))
                    .additionalParameters(params)
                    .run(getDescriptor(postExecution));
        }
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

    private PhpExecutable createPhpExecutable(PhpModule phpModule) {
        String absolutePath = FileUtil.toFile(phpModule.getSourceDirectory()).getAbsolutePath();
        return new PhpExecutable(absolutePath + "/artisan") //??
                .environmentVariables(Collections.singletonMap("SHELL_INTERACTIVE", "true")) // NOI18N
                .workDir(FileUtil.toFile(phpModule.getSourceDirectory()));
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
        commands.add(new ArtisanCommand(phpModule, "", "basic command", "Artisan"));
        //temporary hardcode
        commands.add(new ArtisanCommand(phpModule, "about", "Display basic information about your application", "About"));
        return commands;
    }

    private String getPreScript() {
        return LaravelPreferences.getPreScript(phpModule);
    }

    private boolean useRemoteConnection() {
        return LaravelPreferences.getRemoteConnectionFlag(phpModule);
    }

    private boolean useDocker() {
        return LaravelPreferences.getUseDocker(phpModule);
    }

    private String getDockerContainerName() {
        return LaravelPreferences.getDockerContainerName(phpModule);
    }

    private String getDockerBashPath() {
        return LaravelPreferences.getDockerBashPath(phpModule);
    }

}
