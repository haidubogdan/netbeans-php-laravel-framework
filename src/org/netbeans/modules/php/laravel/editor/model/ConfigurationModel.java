package org.netbeans.modules.php.laravel.editor.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.netbeans.modules.php.laravel.astnodes.ArrayFileVisitor;
import org.netbeans.modules.php.laravel.editor.parser.ConfigurationFileParser;
import org.openide.filesystems.FileObject;

public class ConfigurationModel {

    private static final Map<FileObject, ConfigurationModel> INSTANCES
            = new WeakHashMap<>();

    private final Map<String, Map<String, List<String>>> configurationMapping = new HashMap<>();
    private final Map<FileObject, ArrayFileVisitor.ConfigNamespace> configFileNamespace = new HashMap<>();
    private final Map<String, FileObject> configurationFilesAlias = new HashMap<>();

    public static ConfigurationModel getModel(FileObject configDir) {
        ConfigurationModel model = INSTANCES.get(configDir);
        if (model == null) {
            model = new ConfigurationModel(configDir);
            INSTANCES.put(configDir, model);
        }
        return model;
    }

    private ConfigurationModel(FileObject configDir) {

        ConfigurationFileParser configParser = new ConfigurationFileParser();
        
        for (FileObject child : configDir.getChildren()) {
            if (child.isData()) {
                configurationFilesAlias.put(child.getName(), child);
                configFileNamespace.put(child, configParser.getRelativeConfigTree(child));
                configurationMapping.put(child.getName(), configParser.getConfigTree(child));
            }
        }
    }
        
    public Map<String, Map<String, List<String>>> getConfigurationMapping() {
        return configurationMapping;
    }
    
    public Map<FileObject, ArrayFileVisitor.ConfigNamespace> getConfigurationFileNamespace() {
        return configFileNamespace;
    }
    
    public Map<String, FileObject> getConfigurationFilesAlias() {
        return configurationFilesAlias;
    }
}
