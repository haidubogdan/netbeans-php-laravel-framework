/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.ui.actions;

import java.util.Arrays;
import org.netbeans.modules.php.spi.framework.PhpModuleActionsExtender;
import java.util.List;
import javax.swing.Action;
import org.netbeans.modules.php.laravel.utils.LaravelUtils;
import org.netbeans.modules.php.spi.framework.actions.RunCommandAction;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdan
 */
public class LaravelPhpModuleActionsExtender extends PhpModuleActionsExtender {

    private static final List<Action> ACTIONS = Arrays.<Action>asList(
            ClearCacheAction.getInstance(),
            ClearViewCacheAction.getInstance(),
            ClearRouteCacheAction.getInstance(),
            GenerateAppKeyAction.getInstance()
    );

    @Override
    public String getMenuName() {
        return "Laravel Commands"; // NOI18N
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
