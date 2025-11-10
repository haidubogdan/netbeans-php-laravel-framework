package org.netbeans.modules.php.laravel.editor.completion;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.laravel.ControllersClassQuery;
import static org.netbeans.modules.php.laravel.PhpNbConsts.NB_PHP_PROJECT_TYPE;
import org.netbeans.spi.project.LookupProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author bhaidu
 */
public class LaravelCompletionSupport {

    private final FileObject projectDir;
    private final ControllersClassQuery controllerClassQuery = new ControllersClassQuery();
    private boolean scanned = false;

    public LaravelCompletionSupport(Project project) {
        this.projectDir = project.getProjectDirectory();
    }

    public synchronized void parseControllerFiles() {
        controllerClassQuery.parseControllers(projectDir);
        scanned = true;
    }

    public synchronized boolean filesScanned() {
        return scanned;
    }

    public ControllersClassQuery getControllerClassQuery() {
        return controllerClassQuery;
    }

    @LookupProvider.Registration(projectType = NB_PHP_PROJECT_TYPE)
    public static LookupProvider createJavaBaseProvider() {
        return new LookupProvider() {
            @Override
            public Lookup createAdditionalLookup(Lookup baseContext) {
                Project project = baseContext.lookup(Project.class);
                return Lookups.fixed(
                        new LaravelCompletionSupport(project)
                );
            }
        };
    }
}
