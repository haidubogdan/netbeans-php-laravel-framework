package org.netbeans.modules.php.laravel.project;

import javax.swing.text.Document;
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.laravel.editor.parser.ControllersClassParser;
import static org.netbeans.modules.php.laravel.PhpNbConsts.NB_PHP_PROJECT_TYPE;
import org.netbeans.modules.php.laravel.editor.parser.RoutesConfigParser;
import org.netbeans.spi.project.LookupProvider;
import org.openide.awt.NotificationDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * 
 * Controller parser trigger using ControllersClassQuery
 */
public class LaravelAppSupport {

    private final FileObject projectDir;
    private final ControllersClassParser controllerClassParser = new ControllersClassParser();
    private final RoutesConfigParser routesConfigParser = new RoutesConfigParser();
    private boolean scanned = false;

    public LaravelAppSupport(Project project) {
        this.projectDir = project.getProjectDirectory();
    }

    public synchronized void scannProject() {
        controllerClassParser.parseControllers(projectDir);
        routesConfigParser.parseRoutes(projectDir);
        scanned = true;
    }

    public synchronized boolean filesScanned() {
        return scanned;
    }

    public ControllersClassParser getControllerClassParser() {
        return controllerClassParser;
    }
    
    public RoutesConfigParser getRoutesConfigParser() {
        return routesConfigParser;
    }

    @LookupProvider.Registration(projectType = NB_PHP_PROJECT_TYPE)
    public static LookupProvider createJavaBaseProvider() {
        return new LookupProvider() {
            @Override
            public Lookup createAdditionalLookup(Lookup baseContext) {
                Project project = baseContext.lookup(Project.class);
                return Lookups.fixed(new LaravelAppSupport(project)
                );
            }
        };
    }
    
    public static LaravelAppSupport getInstance(Document doc) {
        Project project = ProjectUtils.get(doc);
        LaravelAppSupport support = project.getLookup().lookup(LaravelAppSupport.class);

        if (support == null) {
            return null;
        }

        if (!support.filesScanned()) {
            support.scannProject();
            NotificationDisplayer.getDefault().notify("Laravel: Project files parsed",
                    ImageUtilities.loadImageIcon("org/netbeans/modules/git/resources/icons/info.png", false),
                    "",
                    evt -> {
                    }
            );
        }

        return support;
    }
}
