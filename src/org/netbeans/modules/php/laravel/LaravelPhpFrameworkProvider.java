package org.netbeans.modules.php.laravel;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.modules.php.laravel.editor.LaravelEditorExtender;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.modules.php.api.framework.BadgeIcon;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.phpmodule.PhpModuleProperties;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayElement;
import org.netbeans.modules.php.laravel.commands.LaravelCommandSupport;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.modules.php.laravel.project.ComposerPackages;
import org.netbeans.modules.php.spi.editor.EditorExtender;
import org.netbeans.modules.php.spi.framework.PhpFrameworkProvider;
import org.netbeans.modules.php.spi.framework.PhpModuleActionsExtender;
import org.netbeans.modules.php.spi.framework.PhpModuleCustomizerExtender;
import org.netbeans.modules.php.spi.framework.PhpModuleExtender;
import org.netbeans.modules.php.spi.framework.PhpModuleIgnoredFilesExtender;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommandSupport;
import org.netbeans.modules.php.spi.phpmodule.ImportantFilesImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;

/**
 *
 * @author bogdan
 */
public class LaravelPhpFrameworkProvider extends PhpFrameworkProvider {

    @StaticResource
    private static final String ICON_PATH = "org/netbeans/modules/php/laravel/resources/laravel_badge.png"; // NOI18N
    private static final LaravelPhpFrameworkProvider INSTANCE = new LaravelPhpFrameworkProvider();
    private final BadgeIcon badgeIcon;
    private final Map<Integer, Boolean> inPhpModuleChecked = new HashMap<>();
    private boolean isInModule = false;
    private FileObject sourceDirectory;

    private LaravelPhpFrameworkProvider() {
        super("Laravel 9 PHP Web Framework", // NOI18N
                "Laravel 9",
                "Laravel description");
        badgeIcon = new BadgeIcon(
                ImageUtilities.loadImage(ICON_PATH),
                LaravelPhpFrameworkProvider.class.getResource("/" + ICON_PATH)); // NOI18N
    }

    @PhpFrameworkProvider.Registration(position = 203)
    public static LaravelPhpFrameworkProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public BadgeIcon getBadgeIcon() {
        return badgeIcon;
    }

    @Override
    public boolean isInPhpModule(PhpModule phpModule) {
        sourceDirectory = phpModule.getSourceDirectory();

        if (sourceDirectory == null) {
            // broken project
            return false;
        }

        int projectHash = sourceDirectory.getPath().hashCode();

        //flag to skip extra checking, when project is loaded
        if (inPhpModuleChecked.containsKey(projectHash)) {
            return inPhpModuleChecked.get(projectHash);
        }

        ComposerPackages composerPackages = ComposerPackages.fromPhpModule(phpModule);

        if (composerPackages != null) {
            String laravelVersion = composerPackages.getLaravelVersion();
            isInModule = laravelVersion != null;
        }
        inPhpModuleChecked.put(projectHash, isInModule);

        return isInModule;
    }

    //try to get the version from composer
    @Override
    public String getName(PhpModule phpModule) {

        return super.getName(phpModule);
    }

    @Override
    public PhpModuleExtender createPhpModuleExtender(PhpModule pm) {
        //not implement it
       
        return new LaravelPhpModuleExtender();
    }

    @Override
    public PhpModuleCustomizerExtender createPhpModuleCustomizerExtender(PhpModule phpModule) {
        if (isInPhpModule(phpModule)) {
            return new LaravelPhpModuleCustomizerExtender(phpModule);
        }
        return null;
    }

    @Override
    public PhpModuleProperties getPhpModuleProperties(PhpModule phpModule) {
        PhpModuleProperties properties = new PhpModuleProperties();

        if ( phpModule.getSourceDirectory() == null) {
            // broken project
            return properties;
        }

        //todo add tests
        return properties;
    }

    /**
     * for project template
     *
     * @param pm
     * @return
     */
    @Override
    public PhpModuleActionsExtender getActionsExtender(PhpModule pm) {
        return new LaravelPhpModuleActionsExtender();
    }

    @Override
    public ImportantFilesImplementation getConfigurationFiles2(PhpModule phpModule) {
        return new ConfigurationFiles(phpModule);
    }

    @Override
    public PhpModuleIgnoredFilesExtender getIgnoredFilesExtender(PhpModule pm) {
        return null;
    }

    @Override
    public EditorExtender getEditorExtender(PhpModule phpModule) {
        return new LaravelEditorExtender();
    }

    /**
     * not required for the moment
     *
     * @param pm
     * @return
     */
    @Override
    public FrameworkCommandSupport getFrameworkCommandSupport(PhpModule pm) {
        return new LaravelCommandSupport(pm);
    }

    public FileObject getSourceDirectory() {
        return sourceDirectory;
    }

}
