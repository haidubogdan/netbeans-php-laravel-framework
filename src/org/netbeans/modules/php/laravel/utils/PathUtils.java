/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.ProjectConvertors;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdan
 */
public class PathUtils {

    public static final String LARAVEL_VIEW_PATH = "resources/views"; //NOI18N
    public static final String BLADE_EXT = ".blade.php"; //NOI18N
    public static final String SLASH = "/"; //NOI18N
    public static final String DOT = "."; //NOI18N

    /**
     * first we need to extract the root folder of view after we apply a generic
     * path sanitize for blade paths (ex "my.path" -> "my/path.blade.php")
     *
     * @param viewDir
     * @param viewPath
     * @return List
     */
    public static List<FileObject> findFileObjectsForBladeViewPath(FileObject viewDir, String viewPath) {
        List<FileObject> fileViewAssociationList = new ArrayList<>();

        String sanitizedBladePath = viewPathToFilePath(viewPath); //NOI18N

        FileObject includedFile = viewDir.getFileObject(sanitizedBladePath, true);

        if (includedFile != null && includedFile.isValid()) {
            fileViewAssociationList.add(includedFile);
        }

        return fileViewAssociationList;
    }

    public static FileObject findFileObjectForBladeViewPath(FileObject viewDir, String viewPath) {
        FileObject res = null;

        String sanitizedBladePath = viewPathToFilePath(viewPath);

        FileObject includedFile = viewDir.getFileObject(sanitizedBladePath, true);

        if (includedFile != null && includedFile.isValid()) {
            return includedFile;
        }

        return res;
    }

    public static List<FileObject> getParentChildrenFromPrefixPath(FileObject viewDir,
            String prefixViewPath) {
        List<FileObject> list = new ArrayList<>();

        String unixPath = prefixViewPath.replace(DOT, SLASH);
        int relativeSlash;

        //fix issues with lastIndexOf search
        relativeSlash = unixPath.lastIndexOf(SLASH);

        FileObject relativeViewRoot = null;

        if (relativeSlash > 0) {
            //filter only relative folders

            relativeViewRoot = viewDir.getFileObject(unixPath.substring(0, relativeSlash));

            if (!relativeViewRoot.isValid()) {
                relativeViewRoot = null;
            }

        }

        String relativePrefixToCompare;

        if (relativeSlash > 0) {
            //extract the path name prefix
            relativePrefixToCompare = unixPath.substring(relativeSlash + 1, unixPath.length());
        } else {
            //root reference
            relativePrefixToCompare = unixPath;
        }

        if (unixPath.endsWith(SLASH)) {
            //add children

            list.addAll(Arrays.asList(viewDir.getChildren()));

            if (relativeViewRoot != null) {
                list.addAll(Arrays.asList(relativeViewRoot.getChildren()));
            }
        } else {
            //filter by filename in relative context

            for (FileObject file : viewDir.getChildren()) {
                String filePath = file.getPath().replace(viewDir.getPath() + SLASH, "");
                if (filePath.startsWith(relativePrefixToCompare)) {
                    list.add(file);
                }
            }

            if (relativeViewRoot != null) {
                for (FileObject file : relativeViewRoot.getChildren()) {
                    if (file.getName().startsWith(relativePrefixToCompare)) {
                        list.add(file);
                    }
                }
            }
        }

        return list;
    }

    public static String toBladeViewPath(FileObject file, FileObject viewDir) {
        String path = null;

        String filePath = file.getPath();

        if (viewDir != null) {
            //belongs to the default folder
            String viewFolderPath = viewDir.getPath();
            if (filePath.startsWith(viewFolderPath)) {
                String bladePath = PathUtils.toBladeViewPath(filePath.replace(viewFolderPath, ""));
                //starting slash
                if (bladePath.startsWith(".")) {
                    bladePath = bladePath.substring(1, bladePath.length());
                }
                return bladePath;
            }
        }

        return path;
    }

    public static String getRelativeProjectPath(FileObject currentFile) {
        Project projectOwner = ProjectConvertors.getNonConvertorOwner(currentFile);
        if (projectOwner == null) {
            return "";
        }

        String dirPath = projectOwner.getProjectDirectory().getPath();
        String relativePath = currentFile.getPath().replace(dirPath, "");

        //only if we found the relative project path
        if (currentFile.getPath().length() > relativePath.length()) {
            return relativePath;
        }

        return "";
    }

    public static String toBladeViewPath(String filePath) {
        return filePath.replace(BLADE_EXT, "").replace(SLASH, DOT);
    }

    public static String viewPathToFilePath(String viewPath) {
        return viewPath.replace(DOT, SLASH) + BLADE_EXT;
    }

    public static HashSet<FileObject> getDefaultRoots(Project project) {
        HashSet<FileObject> defaultList = new HashSet<>();
        FileObject defaultViewsRoot = project.getProjectDirectory().getFileObject(LARAVEL_VIEW_PATH);

        if (defaultViewsRoot != null && defaultViewsRoot.isValid()) {
            defaultList.add(defaultViewsRoot);
        }

        return defaultList;
    }
}
