package org.netbeans.modules.php.laravel.commands;

import org.netbeans.modules.dlight.terminal.ui.RemoteInfoDialog;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;

/**
 *
 * @author bhaidu
 */
public class DlightTerminalEnvironment {

    public static ExecutionEnvironment getRemoteConfig() {
        RemoteInfoDialog cfgPanel = new RemoteInfoDialog(System.getProperty("user.name"));
        String title = "RemoteConnectionTitle";
        cfgPanel.init();
        
        //TODO use config to open / the dialog ?
//        DialogDescriptor dd = new DialogDescriptor(cfgPanel, title, // NOI18N
//                true, DialogDescriptor.OK_CANCEL_OPTION,
//                DialogDescriptor.OK_OPTION, null);
//
//        Dialog cfgDialog = DialogDisplayer.getDefault().createDialog(dd);
//
//        try {
//            cfgDialog.setVisible(true);
//        } catch (Throwable th) {
//            if (!(th.getCause() instanceof InterruptedException)) {
//                throw new RuntimeException(th);
//            }
//            dd.setValue(DialogDescriptor.CANCEL_OPTION);
//        } finally {
//            cfgDialog.dispose();
//        }
//
//        if (dd.getValue() != DialogDescriptor.OK_OPTION) {
//            return null;
//        }

        final ExecutionEnvironment env = cfgPanel.getExecutionEnvironment();
        return env;
    }
}
