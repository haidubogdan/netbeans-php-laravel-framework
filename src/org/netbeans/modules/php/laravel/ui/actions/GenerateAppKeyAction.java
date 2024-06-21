/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.ui.actions;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.laravel.commands.ArtisanCommandSupport;
import org.netbeans.modules.php.laravel.commands.ExecutableService;
import org.netbeans.modules.php.spi.framework.actions.BaseAction;
import org.openide.util.NbBundle;

/**
 * @author Tomas Mysik
 */
public final class GenerateAppKeyAction extends BaseAction {
    private static final long serialVersionUID = 36068831502227572L;
    private static final GenerateAppKeyAction INSTANCE = new GenerateAppKeyAction();

    private GenerateAppKeyAction() {
    }

    public static GenerateAppKeyAction getInstance() {
        return INSTANCE;
    }

    @Override
    public void actionPerformed(PhpModule phpModule) {
        //todo add action
        ArtisanCommandSupport artisanSupport = ArtisanCommandSupport.getInstance(phpModule);
        List<String> params = new ArrayList<>();
        params.add("app:key");
        ExecutableService.executeCommand(phpModule, artisanSupport, params);
    }

    @Override
    protected String getPureName() {
        return "Generate App Key";
    }

    @Override
    protected String getFullName() {
        return "Generate App Key";
    }
}