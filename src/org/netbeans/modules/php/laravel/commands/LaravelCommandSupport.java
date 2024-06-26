package org.netbeans.modules.php.laravel.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommandSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author bhaidu
 */
public class LaravelCommandSupport extends FrameworkCommandSupport {

    ArtisanCommandSupport artisanSupport;

    public LaravelCommandSupport(PhpModule phpModule) {
        super(phpModule);
        artisanSupport = ArtisanCommandSupport.getInstance(phpModule);
    }

    @Override
    public String getFrameworkName() {
        return "Laravel";
    }

    @Override
    protected List<FrameworkCommand> getFrameworkCommandsInternal() {
        List<FrameworkCommand> commands = new ArrayList<>();
        commands.add(new ArtisanCommand(phpModule, "", "about", "Artisan"));

        if (artisanSupport.getCommands().isEmpty()) {
            ExecutableService.extractArtisanCommands(phpModule, artisanSupport);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            
            if (artisanSupport.getCommands().isEmpty()) {
                return null;
            }
        }

        commands.addAll(artisanSupport.getCommands());

        return commands;
    }

    @Override
    public void runCommand(CommandDescriptor commandDescriptor, Runnable postExecution) {
        String[] commands = commandDescriptor.getFrameworkCommand().getCommands();
        String[] commandParams = commandDescriptor.getCommandParams();
        List<String> params = new ArrayList<>(commands.length + commandParams.length);
        params.addAll(Arrays.asList(commands));
        params.addAll(Arrays.asList(commandParams));

        ExecutableService.executeCommand(phpModule, artisanSupport, params);
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

}
