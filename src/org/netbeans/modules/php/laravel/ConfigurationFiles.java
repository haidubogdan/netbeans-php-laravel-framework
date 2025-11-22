package org.netbeans.modules.php.laravel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.php.laravel.utils.LaravelUtils;
import org.netbeans.modules.php.spi.phpmodule.ImportantFilesImplementation;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class ConfigurationFiles extends FileChangeAdapter implements ImportantFilesImplementation {

    private static final String CONFIG_DIRECTORY = LaravelUtils.DIR_CONFIG;

    private final FileObject sourceDirectory;
    private final ChangeSupport changeSupport = new ChangeSupport(this);
    private boolean listenerAdded = false;

    ConfigurationFiles(FileObject sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    //node tree
    @Override
    public Collection<FileInfo> getFiles() {
        if (getSourceDirectory() == null) {
            // broken project
            return Collections.emptyList();
        }

        List<FileInfo> files = Collections.emptyList();
        FileObject configDir = getConfigDirectory();
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
    
    public synchronized FileObject getConfigDirectory() {
        if (sourceDirectory == null) {
            return null;
        }
        FileObject configDir = sourceDirectory.getFileObject(CONFIG_DIRECTORY);
        
        if (!listenerAdded && configDir != null) {
            File configDirFile = FileUtil.toFile(configDir);
            addListener(configDirFile);
            listenerAdded = true;
        }
        
        return configDir;
    }

    private void addListener(File path) {
        try {
            FileUtil.addFileChangeListener(this, path);
        } catch (IllegalArgumentException ex) {
        }
    }

    @CheckForNull
    private synchronized FileObject getSourceDirectory() {
        return sourceDirectory;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    private void fireChange() {
        changeSupport.fireChange();
    }
    
    @Override
    public void fileRenamed(FileRenameEvent fe) {
        fireChange();
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        fireChange();
    }

    @Override
    public void fileDataCreated(FileEvent fe) {
        fireChange();
    }

    @Override
    public void fileFolderCreated(FileEvent fe) {
        fireChange();
    }
}
