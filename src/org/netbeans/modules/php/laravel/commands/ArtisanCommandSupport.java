package org.netbeans.modules.php.laravel.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;

/**
 *
 * @author bhaidu
 */
public class ArtisanCommandSupport {

    private static final Map<String, ArtisanCommandSupport> INSTANCES = new WeakHashMap<>();
    public List<FrameworkCommand> commands = new ArrayList<>();

    public static ArtisanCommandSupport getInstance(PhpModule phpModule)
    {
        String projectPath = phpModule.getProjectDirectory().getPath();

        synchronized (INSTANCES) {
            ArtisanCommandSupport artisanCommandSupport = INSTANCES.get(projectPath);
            if (artisanCommandSupport == null) {
                artisanCommandSupport = new ArtisanCommandSupport();
                INSTANCES.put(projectPath, artisanCommandSupport);
            }
            return artisanCommandSupport;
        }
    }

    private ArtisanCommandSupport() {

    }
    
    public List<FrameworkCommand> getCommands(){
        return commands;
    }
}
