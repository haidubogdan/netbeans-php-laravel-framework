/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel;

import java.util.EnumSet;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.modules.php.laravel.ui.customizer.LaravelCustomizerPanel;
import org.netbeans.modules.php.spi.framework.PhpModuleCustomizerExtender;
import org.openide.util.HelpCtx;
import org.netbeans.modules.php.laravel.project.ComposerPackages;

/**
 *
 * @author bogdan
 */
public class LaravelPhpModuleCustomizerExtender extends PhpModuleCustomizerExtender {

    private final PhpModule phpModule;
    private ComposerPackages composerPackages;

    // @GuardedBy(EDT)
    private LaravelCustomizerPanel component;
    
    LaravelPhpModuleCustomizerExtender(PhpModule phpModule) {
        this.phpModule = phpModule;
        composerPackages = ComposerPackages.fromPhpModule(phpModule);
    }

    @Override
    public String getDisplayName() {
        return "Laravel";
    }

    private LaravelCustomizerPanel getPanel() {
        if (component == null) {
            component = new LaravelCustomizerPanel(phpModule.getSourceDirectory());
            component.setLaravelVersion(composerPackages.getLaravelVersion());
            component.initModuleValues(phpModule);
        }
        return component;
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
        return composerPackages != null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public EnumSet<Change> save(PhpModule pm) {
        if (component == null){
            return null;
        }

        component.saveChanges(pm);
        
        return null;
    }

}
