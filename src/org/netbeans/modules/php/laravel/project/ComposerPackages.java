/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.project;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author bogdan
 */
public class ComposerPackages {

    private final PhpModule phpModule;
    private static final Map<String, ComposerPackages> INSTANCES = new WeakHashMap<>();
    private Map<String, Object> composerJsonContent = new HashMap<>();
    private boolean composerFileFound = false;

    private ComposerPackages(PhpModule phpModule) {
        this.phpModule = phpModule;
        this.extractPackageInfo();
    }

    private void extractPackageInfo() {
        FileObject sourceDir = phpModule.getSourceDirectory();

        if (sourceDir == null) {
            return;
        }
        FileObject composerJsonFile = sourceDir.getFileObject("composer.json");

        if (composerJsonFile == null) {
            return;
        }

        JSONParser parser = new JSONParser();

        try {
            composerJsonContent = (Map<String, Object>) parser.parse(new FileReader(composerJsonFile.getPath()));
            composerFileFound = true;
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException | ParseException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public Map<String, Object> getcomposerJsonContent() {
        return composerJsonContent;
    }

    public String getLaravelVersion() {
        if (composerJsonContent == null) {
            return null;
        }
        Map<String, Object> require = (Map<String, Object>) composerJsonContent.get("require");

        if (require == null) {
            return null;
        }

        String laravelVersion = (String) require.get("laravel/framework");
        return laravelVersion;
    }

    public static ComposerPackages fromPhpModule(PhpModule phpModule) {
        return new ComposerPackages(phpModule);
    }

    public static ComposerPackages getInstance(PhpModule phpModule) {
        String projectPath = phpModule.getProjectDirectory().getPath();

        synchronized (INSTANCES) {
            ComposerPackages composerPackage = INSTANCES.get(projectPath);
            if (composerPackage == null) {
                composerPackage = fromPhpModule(phpModule);
                INSTANCES.put(projectPath, composerPackage);
            }
            return composerPackage;
        }
    }

    public static ComposerPackages getInstance(String projectPath) {
        ComposerPackages composerPackage = INSTANCES.get(projectPath);
        return composerPackage;
    }

    public boolean composerFileFound() {
        return composerFileFound;
    }

    public boolean hasPhpModule() {
        return phpModule != null;
    }
}
