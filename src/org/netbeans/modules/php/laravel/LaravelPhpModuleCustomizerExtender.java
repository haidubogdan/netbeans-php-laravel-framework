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
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author bogdan
 */
public class LaravelPhpModuleCustomizerExtender extends PhpModuleCustomizerExtender {

    private final PhpModule phpModule;
    private final ComposerPackages composerPackages;
    private final boolean isFrameworkEnabledOnProject;

    // @GuardedBy(EDT)
    private LaravelCustomizerPanel component;
    
    LaravelPhpModuleCustomizerExtender(PhpModule phpModule) {
        this.phpModule = phpModule;
        composerPackages = ComposerPackages.fromProjectDir(phpModule.getProjectDirectory());
        isFrameworkEnabledOnProject = LaravelPreferences.fromPhpModule(phpModule).hasEnabledConfigured();
    }

    @Messages("LBL_Laravel=Laravel")
    @Override
    public String getDisplayName() {
        return Bundle.LBL_Laravel();
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

    //TODO check flow
    @Override
    public EnumSet<Change> save(PhpModule pm) {
        if (component == null){
            return null;
        }

        component.saveChanges(pm);
        
        if (isFrameworkEnabledOnProject != component.isFrameworkEnabled()){
            //?? what is the purpose
            return EnumSet.of(Change.FRAMEWORK_CHANGE);
        }
              
        return null;
    }
    
    private LaravelCustomizerPanel getPanel() {
        if (component == null) {
            component = new LaravelCustomizerPanel(LaravelPreferences.fromPhpModule(phpModule));
            component.setLaravelVersion(composerPackages.getLaravelVersion());
            component.initModuleValues();
            component.initPluginInfo(phpModule.getSourceDirectory());
        }
        return component;
    }
}
