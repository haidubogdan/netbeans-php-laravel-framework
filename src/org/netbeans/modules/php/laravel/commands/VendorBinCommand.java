package org.netbeans.modules.php.laravel.commands;

import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;

/**
 *
 * @author bhaidu
 */
public class VendorBinCommand extends FrameworkCommand {
    public static final String VENDOR_COMMAND = "./vendor/bin/"; // NOI18N

    public VendorBinCommand(String command, String description, String displayName) {
        super(command, description, displayName);
    }

    @Override
    protected String getHelpInternal() {
        return ""; // NOI18N
    }
}
