package org.netbeans.modules.php.laravel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.astnodes.ArrayFileVisitor.ConfigNamespace;
import org.netbeans.modules.php.laravel.parser.ConfigurationFileParser;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.modules.php.spi.phpmodule.ImportantFilesImplementation;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

/**
 *
 * @author bogdan
 */
public final class ConfigurationFiles extends FileChangeAdapter implements ImportantFilesImplementation {

    private static final String CONFIG_DIRECTORY = "config";
    private final PhpModule phpModule;
    private final ChangeSupport changeSupport = new ChangeSupport(this);
    // @GuardedBy("this")
    private FileObject sourceDirectory = null;
    private final Map<String, Map<String, List<String>>> configurationMapping = new HashMap<>();
    private final Map<FileObject, ConfigNamespace> configFileNamespace = new HashMap<>();
    private final Map<String, FileObject> configurationFilesAlias = new HashMap<>();
    private static final Map<String, ConfigurationFiles> INSTANCES = new WeakHashMap<>();

    ConfigurationFiles(PhpModule phpModule) {
        assert phpModule != null;
        this.phpModule = phpModule;
        storeInstance();
    }

    @Override
    public Collection<FileInfo> getFiles() {
        FileObject sourceDir = getSourceDirectory();
        if (sourceDir == null) {
            // broken project
            return Collections.emptyList();
        }

        List<FileInfo> files = Collections.emptyList();
        FileObject configDir = sourceDir.getFileObject(CONFIG_DIRECTORY);
        if (configDir != null
                && configDir.isFolder()
                && configDir.isValid()) {
            for (FileObject child : configDir.getChildren()) {
                if (child.isData()) {
                    if (files.isEmpty()) {
                        files = new ArrayList<>();
                    }
                    files.add(new FileInfo(child));
                }
            }
            Collections.sort(files, FileInfo.COMPARATOR);
        }
        return files;
    }

    public void extractConfigurationMapping(boolean withOffset) {
        FileObject sourceDir = getSourceDirectory();
        if (sourceDir == null) {
            // broken project
            return;
        }
        FileObject configDir = sourceDir.getFileObject(CONFIG_DIRECTORY);

        if (configDir == null || !configDir.isFolder() || !configDir.isValid()) {
            return;
        }

        ConfigurationFileParser configParser = new ConfigurationFileParser();
        
        for (FileObject child : configDir.getChildren()) {
            if (child.isData()) {
                configurationFilesAlias.put(child.getName(), child);
                if(withOffset){
                    configFileNamespace.put(child, configParser.getConfigTreeWithOffset(child));
                } else {
                    configurationMapping.put(child.getName(), configParser.getConfigTree(child));
                }
            }
        }
    }

    public Map<String, Map<String, List<String>>> getConfigurationMapping() {
        return configurationMapping;
    }
    
    public Map<FileObject, ConfigNamespace> getConfigurationFileNamespace() {
        return configFileNamespace;
    }
    
    public Map<String, FileObject> getConfigurationFilesAlias() {
        return configurationFilesAlias;
    }


    private void addListener(File path) {
        try {
            FileUtil.addFileChangeListener(this, path);
        } catch (IllegalArgumentException ex) {
            // noop, already listening...
            assert false : path;
        }
    }

    private void storeInstance() {
        String projectPath = phpModule.getProjectDirectory().getPath();
        synchronized (INSTANCES) {
            INSTANCES.put(projectPath, this);
        }
    }

    public static ConfigurationFiles getInstance(PhpModule phpModule) {
        String projectPath = phpModule.getProjectDirectory().getPath();
        if (INSTANCES.get(projectPath) == null) {
            synchronized (INSTANCES) {
                INSTANCES.put(projectPath, new ConfigurationFiles(phpModule));
            }
        }
        return INSTANCES.get(projectPath);
    }

    @CheckForNull
    private synchronized FileObject getSourceDirectory() {
        if (sourceDirectory == null) {
            sourceDirectory = phpModule.getSourceDirectory();
            if (sourceDirectory != null) {
                File sources = FileUtil.toFile(sourceDirectory);
                addListener(new File(new File(sources, LaravelPreferences.getAppDir(phpModule)), CONFIG_DIRECTORY));
            }
        }
        return sourceDirectory;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        changeSupport.removeChangeListener(cl);
    }

}
