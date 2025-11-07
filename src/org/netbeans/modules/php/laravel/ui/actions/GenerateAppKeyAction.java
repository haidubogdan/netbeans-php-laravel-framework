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
public final class GenerateAppKeyAction extends BaseAction {

    private static final long serialVersionUID = 36068831502227572L;
    
    private static final String ACTION_NAME = "Generate App Key"; //NOI18N
    private static final String LARAVEL_COMMAND = "key:generate"; //NOI18N

    private static final GenerateAppKeyAction INSTANCE = new GenerateAppKeyAction();
    private static final RequestProcessor WORKER = new RequestProcessor(GenerateAppKeyAction.class.getName(), 1, true);

    private GenerateAppKeyAction() {
    }

    public static GenerateAppKeyAction getInstance() {
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
