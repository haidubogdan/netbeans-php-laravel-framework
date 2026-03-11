package org.netbeans.modules.php.laravel.commands;

import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;

/**
 *
 * @author bhaidu
 */
public class ArtisanCommand extends FrameworkCommand {
    public static final String ARTISAN_COMMAND = "artisan"; // NOI18N

    public ArtisanCommand(String command, String description, String displayName) {
        super(command, description, displayName);
    }

    @Override
    protected String getHelpInternal() {
        return ""; // NOI18N
    }

    @Override
    public String getPreview() {
        return ARTISAN_COMMAND + " " + super.getPreview(); // NOI18N
    }
}
