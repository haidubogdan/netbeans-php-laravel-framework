package org.netbeans.modules.php.laravel.project;

import javax.swing.text.Document;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.spi.project.ui.support.ProjectConvertors;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdan
 */
public class ProjectUtils {

    public static Project get(Document doc) {
        FileObject file = NbEditorUtilities.getFileObject(doc);
        Project projectOwner = ProjectConvertors.getNonConvertorOwner(file);
        return projectOwner;
    }

    public static Project get(FileObject fo) {
        Project projectOwner = ProjectConvertors.getNonConvertorOwner(fo);
        return projectOwner;
    }

    public static boolean isInLaravelModule(Document doc) {
        PhpModule module = getPhpModule(doc);

        if (module == null) {
            return false;
        }

        return isInLaravelModule(module);
    }

    public static boolean isInLaravelModule(PhpModule module) {
        return LaravelPreferences.fromPhpModule(module).isEnabled();
    }

    public static PhpModule getPhpModule(Document doc) {
        Project pr = get(doc);
        if (pr == null) {
            return null;
        }
        return pr.getLookup().lookup(PhpModule.class);
    }

    public static PhpModule getPhpModule(FileObject fo) {
        Project pr = get(fo);
        if (pr == null) {
            return null;
        }
        return pr.getLookup().lookup(PhpModule.class);
    }

    public static boolean isRouteFile(FileObject file) {
        switch (file.getNameExt()) {
            case "web.php": // NOI18N 
            case "auth.php": // NOI18N 
            case "api.php": // NOI18N 
                return true;
        }

        return false;
    }
}
