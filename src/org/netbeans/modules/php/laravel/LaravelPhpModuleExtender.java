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

    @Override
    public Set<FileObject> extend(PhpModule phpModule) throws ExtendingException {
        return getInitialFiles(phpModule);
    }

    private Set<FileObject> getInitialFiles(PhpModule phpModule) {
        Set<FileObject> files = new HashSet<>();
//        addSourceFile(files, phpModule, "app/config/parameters.yml"); // NOI18N
//        addSourceFile(files, phpModule, "src/AppBundle/Controller/DefaultController.php"); // NOI18N
//        addSourceFile(files, phpModule, "app/Resources/views/default/index.html.twig"); // NOI18N
        if (files.isEmpty()) {
            //addSourceFile(files, phpModule, "web/app_dev"); // NOI18N
        }
        return files;
    }

    private void addSourceFile(Set<FileObject> files, PhpModule phpModule, String relativePath) {
        FileObject sourceDirectory = phpModule.getSourceDirectory();
        if (sourceDirectory == null) {
            // broken project
            assert false : "Module extender for no sources of: " + phpModule.getName();
            return;
        }
        FileObject fileObject = sourceDirectory.getFileObject(relativePath);
        if (fileObject != null) {
            files.add(fileObject);
        }
    }

    private synchronized NewProjectConfigurationPanel getPanel() {
        if (panel == null) {
            panel = new NewProjectConfigurationPanel();
        }
        return panel;
    }

}
