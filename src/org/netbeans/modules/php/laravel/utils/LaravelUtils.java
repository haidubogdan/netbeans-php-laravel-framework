/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.utils;

import java.io.File;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * todo check what is necessary
 * 
 * @author bogdan
 */
public final class LaravelUtils {

    public static final String DIR_CONFIG = "config"; // NOI18N
    public static final String DIR_VIEWS = "views"; // NOI18N
    public static final String CONFIG_METHOD = "config"; // NOI18N
    public static final String VIEW_METHOD = "view"; // NOI18N

    private LaravelUtils() {
    }

    public static boolean isView(FileObject fo) {
        File file = FileUtil.toFile(fo);
        String fileDir = file.getParentFile().getName();
        return DIR_VIEWS.equals(fileDir) && fo.getNameExt().endsWith(PathUtils.BLADE_EXT);
    }
    
    public static boolean isConfig(FileObject fo) {
        File file = FileUtil.toFile(fo);
        return DIR_CONFIG.equals(file.getParentFile().getName());
    }

    public static boolean isViewWithAction(FileObject fo) {
        return isView(fo);
    }
}
