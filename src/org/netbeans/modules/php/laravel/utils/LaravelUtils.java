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
    private static final String DIR_CONFIG = "config"; // NOI18N
    private static final String DIR_VIEWS = "views"; // NOI18N
    private static final String FILE_VIEW_EXTENSION = ".blade.php"; // NOI18N


    private LaravelUtils() {
    }

    public static boolean isView(FileObject fo) {
        File file = FileUtil.toFile(fo);
        return DIR_VIEWS.equals(file.getParentFile().getName()) && fo.getNameExt().endsWith(FILE_VIEW_EXTENSION);
    }
    
    public static boolean isConfig(FileObject fo) {
        File file = FileUtil.toFile(fo);
        return DIR_CONFIG.equals(file.getParentFile().getName());
    }

    public static boolean isViewWithAction(FileObject fo) {
        return isView(fo);
    }
}
