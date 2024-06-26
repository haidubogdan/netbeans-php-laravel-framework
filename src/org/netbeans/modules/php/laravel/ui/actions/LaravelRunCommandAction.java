/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.ui.actions;

import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.LaravelPhpFrameworkProvider;
import org.netbeans.modules.php.spi.framework.actions.RunCommandAction;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author bogdan
 */
public final class LaravelRunCommandAction extends RunCommandAction {

    private static final LaravelRunCommandAction INSTANCE = new LaravelRunCommandAction();

    private LaravelRunCommandAction() {
    }

    public static LaravelRunCommandAction getInstance() {
        return INSTANCE;
    }

    @Override
    public void actionPerformed(PhpModule phpModule) {
        if (!LaravelPhpFrameworkProvider.getInstance().isInPhpModule(phpModule)) {
            return;
        }

        LaravelPhpFrameworkProvider.getInstance().getFrameworkCommandSupport(phpModule).openPanel();
    }

    @Messages({
        "# {0} - command name",
        "LaravelRunCommandAction.name=Laravel: {0}",
    })
    @Override
    protected String getFullName() {
        return Bundle.LaravelRunCommandAction_name(getPureName());
    }
}
