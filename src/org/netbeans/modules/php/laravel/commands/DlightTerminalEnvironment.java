package org.netbeans.modules.php.laravel.commands;

import org.netbeans.modules.dlight.terminal.ui.RemoteInfoDialog;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;

/**
 *
 * @author bhaidu
 */
public class DlightTerminalEnvironment {

    public static ExecutionEnvironment getRemoteConfig() {
        RemoteInfoDialog cfgPanel = new RemoteInfoDialog(System.getProperty("user.name")); // NOI18N
        cfgPanel.init();
        final ExecutionEnvironment env = cfgPanel.getExecutionEnvironment();
        return env;
    }
}
