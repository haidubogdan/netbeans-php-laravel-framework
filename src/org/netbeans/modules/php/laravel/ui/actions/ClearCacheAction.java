/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.ui.actions;

import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.spi.framework.actions.BaseAction;
import org.openide.util.NbBundle;

/**
 * @author Tomas Mysik
 */
public final class ClearCacheAction extends BaseAction {
    private static final long serialVersionUID = 36068831502227572L;
    private static final ClearCacheAction INSTANCE = new ClearCacheAction();

    private ClearCacheAction() {
    }

    public static ClearCacheAction getInstance() {
        return INSTANCE;
    }

    @Override
    public void actionPerformed(PhpModule phpModule) {
        //todo add action
    }

    @Override
    protected String getPureName() {
        return "Clear cache";
    }

    @Override
    protected String getFullName() {
        return "Clear Cache - fullname";
    }
}