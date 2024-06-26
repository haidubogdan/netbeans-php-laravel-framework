package org.netbeans.modules.php.laravel.editor;

import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;

public class ResourceUtilities {

    public static final String ICON_BASE = "org/netbeans/modules/php/laravel/resources/"; //NOI18N

    public static ImageIcon loadResourceIcon(String path){
        return ImageUtilities.loadImageIcon(ICON_BASE + path, false);
    }
}