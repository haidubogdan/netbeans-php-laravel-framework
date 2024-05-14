package org.netbeans.modules.php.laravel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.*;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.preferences.LaravelPreferences;
import org.netbeans.modules.php.spi.phpmodule.ImportantFilesImplementation;
import org.openide.*;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
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

    ConfigurationFiles(PhpModule phpModule) {
        assert phpModule != null;
        this.phpModule = phpModule;
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

    private void addListener(File path) {
        try {
            FileUtil.addFileChangeListener(this, path);
        } catch (IllegalArgumentException ex) {
            // noop, already listening...
            assert false : path;
        }
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
