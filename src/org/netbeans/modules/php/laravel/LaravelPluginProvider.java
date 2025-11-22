package org.netbeans.modules.php.laravel;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.project.Project;
import static org.netbeans.modules.php.laravel.PhpNbConsts.NB_PHP_PROJECT_TYPE;
import static org.netbeans.modules.php.laravel.project.ComposerPackages.COMPOSER_FILENAME;
import org.netbeans.modules.web.clientproject.api.json.JsonFile;
import org.netbeans.spi.project.LookupProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public class LaravelPluginProvider {

    private final JsonFile composerJson;

    public static String[] POPULAR_PLUGINS = new String[]{
        "inertiajs/inertia-laravel",}; // NOI18N 

    public LaravelPluginProvider(FileObject projectDir) {
        this.composerJson = new JsonFile(COMPOSER_FILENAME, projectDir, JsonFile.WatchedFields.create()
                .add("REQUIRE", "require"));
    }

    @CheckForNull
    public Map<String, Object> getContent() {
        return composerJson.getContent();
    }

    public void addPropertyChangeListener(PropertyChangeListener packageJsonListener) {
        composerJson.addPropertyChangeListener(packageJsonListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener packageJsonListener) {
        composerJson.removePropertyChangeListener(packageJsonListener);
    }

    @CheckForNull
    public <T> T getContentValue(Class<T> valueType, String... fieldHierarchy) {
        return composerJson.getContentValue(valueType, fieldHierarchy);
    }

    public void refresh() {
        composerJson.refresh();
    }

    public Map<String, String> getPopularPackages() {
        Map<String, String> popularPackages = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<Object, Object> require = getContentValue(Map.class, "require"); // NOI18N

        if (require == null) {
            return popularPackages;
        }

        for (Map.Entry<Object, Object> packageEntry : require.entrySet()) {
            String packageName = (String) packageEntry.getKey();
            if (Arrays.asList(POPULAR_PLUGINS).contains(packageName)) {
                popularPackages.put(packageName, (String) packageEntry.getValue());
            }
        }

        return popularPackages;
    }

    public static final class Plugins {

        public final Map<String, String> plugins = new ConcurrentHashMap<>();
    }

    @LookupProvider.Registration(projectType = NB_PHP_PROJECT_TYPE)
    public static LookupProvider createJavaBaseProvider() {
        return new LookupProvider() {
            @Override
            public Lookup createAdditionalLookup(Lookup baseContext) {
                Project project = baseContext.lookup(Project.class);
                return Lookups.fixed(
                        new LaravelPluginProvider(project.getProjectDirectory())
                );
            }
        };
    }
}
