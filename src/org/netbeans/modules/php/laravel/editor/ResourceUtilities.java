package org.netbeans.modules.php.laravel.editor;

import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;

public class ResourceUtilities {

    public static final String ICON_BASE = "org/netbeans/modules/php/laravel/resources/"; //NOI18N
    public static final String FILE_ICON = ICON_BASE + "icons/file.png"; //NOI18N
    public static final String FOLDER_ICON = "org/openide/loaders/defaultFolder.gif"; //NOI18N

    public static ImageIcon loadResourceIcon(String path) {
        return ImageUtilities.loadImageIcon(ICON_BASE + path, false);
    }

    public static ImageIcon loadFileResourceIcon(boolean isFolder) {
        String path = isFolder ? FOLDER_ICON : FILE_ICON; //NOI18N
        return loadResourceIcon(path);
    }
}
