package org.netbeans.modules.php.laravel;

import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.ui.wizards.NewProjectConfigurationPanel;
import org.netbeans.modules.php.spi.framework.PhpModuleExtender;
import org.openide.util.HelpCtx;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdans
 */
public class LaravelPhpModuleExtender extends PhpModuleExtender {

    private NewProjectConfigurationPanel panel = null;

    @Override
    public Set<FileObject> extend(PhpModule phpModule) throws ExtendingException {
        Set<FileObject> files = new HashSet<>();
        LaravelPhpFrameworkProvider.getInstance().getFrameworkCommandSupport(phpModule).refreshFrameworkCommandsLater(null);
        
        //not very relevant without an installer
        FileObject index = LaravelPhpFrameworkProvider.locate(phpModule, "public/index.php", true);
        if (index != null) {
            files.add(index);
        }
        return files;
    }

    private synchronized NewProjectConfigurationPanel getPanel() {
        if (panel == null) {
            panel = new NewProjectConfigurationPanel();
        }
        return panel;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        getPanel().addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        getPanel().removeChangeListener(listener);
    }

    @Override
    public JComponent getComponent() {
        return getPanel();
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public boolean isValid() {
        return getErrorMessage() == null;
    }

    @Override
    public String getErrorMessage() {
        return getPanel().getErrorMessage();
    }

    @Override
    public String getWarningMessage() {
        return null;
    }

}
