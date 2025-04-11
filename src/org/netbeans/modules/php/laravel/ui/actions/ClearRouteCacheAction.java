/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.ui.actions;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.commands.ExecutableService;
import org.netbeans.modules.php.spi.framework.actions.BaseAction;
import org.openide.util.RequestProcessor;

/**
 * @author Tomas Mysik
 */
public final class ClearRouteCacheAction extends BaseAction {

    private static final String ACTION_NAME = "Clear Route Cache"; //NOI18N
    private static final String LARAVEL_COMMAND = "route:clear"; //NOI18N
    private static final long serialVersionUID = 36068831502227575L;

    private static final ClearRouteCacheAction INSTANCE = new ClearRouteCacheAction();
    private static final RequestProcessor WORKER = new RequestProcessor(ClearRouteCacheAction.class.getName(), 1, true);

    private ClearRouteCacheAction() {
    }

    public static ClearRouteCacheAction getInstance() {
        return INSTANCE;
    }

    @Override
    public void actionPerformed(PhpModule phpModule) {
        WORKER.post(new Runnable() {
            @Override
            public void run() {
                List<String> params = new ArrayList<>();
                params.add(LARAVEL_COMMAND);
                ExecutableService.executeCommand(phpModule, params, null);
            }
        });
    }

    @Override
    protected String getPureName() {
        return ACTION_NAME;
    }

    @Override
    protected String getFullName() {
        return ACTION_NAME;
    }
}
