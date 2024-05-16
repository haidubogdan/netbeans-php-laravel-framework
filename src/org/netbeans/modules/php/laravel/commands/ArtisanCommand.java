package org.netbeans.modules.php.laravel.commands;

import java.lang.ref.WeakReference;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;

/**
 *
 * @author bhaidu
 */
public class ArtisanCommand extends FrameworkCommand {

    private final WeakReference<PhpModule> phpModule;

    public ArtisanCommand(PhpModule phpModule, String command, String description, String displayName) {
        super(command, description, displayName);
        assert phpModule != null;

        this.phpModule = new WeakReference<>(phpModule);
    }

    @Override
    protected String getHelpInternal() {
        PhpModule module = phpModule.get();
        if (module == null) {
            return ""; // NOI18N
        }

        return ""; // NOI18N
    }

    @Override
    public String getPreview() {
        return "artisan " + super.getPreview(); // NOI18N
    }
}
