/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel;

import org.netbeans.modules.php.spi.framework.PhpModuleActionsExtender;
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import org.netbeans.modules.php.laravel.ui.actions.GenerateAppKeyAction;
import org.netbeans.modules.php.laravel.ui.actions.LaravelRunCommandAction;
import org.netbeans.modules.php.laravel.utils.LaravelUtils;
import org.netbeans.modules.php.spi.framework.actions.RunCommandAction;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdan
 */
public class LaravelPhpModuleActionsExtender extends PhpModuleActionsExtender {

    private static final List<Action> ACTIONS = Collections.<Action>singletonList(GenerateAppKeyAction.getInstance());

    @Override
    public String getMenuName() {
        //todo do use BuNdle
        return "Laravel";
    }

    @Override
    public List<? extends Action> getActions() {
        return ACTIONS;
    }

    @Override
    public boolean isViewWithAction(FileObject fo) {
        return LaravelUtils.isViewWithAction(fo);
    }

    @Override
    public RunCommandAction getRunCommandAction() {
        return LaravelRunCommandAction.getInstance();
    }

}
