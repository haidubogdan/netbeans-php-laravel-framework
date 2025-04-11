package org.netbeans.modules.php.laravel.views;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import org.openide.nodes.AbstractNode;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import static org.netbeans.modules.php.laravel.PhpNbConsts.NB_PHP_PROJECT_TYPE;
import org.netbeans.modules.php.laravel.utils.PathUtils;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.Children;
import org.openide.util.Exceptions;

/**
 *
 * @author bhaidu
 */
public class ViewsFiles {
    private static final String VIEWS_TITLE = "Blade Views files"; //NOI18N
    private static final Node iconDelegate = DataFolder.findFolder(FileUtil.getConfigRoot()).getNodeDelegate();
     
    @NodeFactory.Registration(projectType = NB_PHP_PROJECT_TYPE, position = 400)
    public static NodeFactory forPhpProject() {
        return new BladeViewsFilesNodeFactory();
    }

    private static final class BladeViewsFilesNodeFactory implements NodeFactory {

        @Override
        public NodeList<?> createNodes(Project project) {
            assert project != null;
            FileObject source = project.getProjectDirectory();
            FileObject viewsDir = source.getFileObject(PathUtils.LARAVEL_VIEW_PATH);
            return new BladeViewsNodeList(project, viewsDir);
        }

    }

    private static final class BladeViewsNodeList implements NodeList<Node>, ChangeListener {

        private final List<FileObject> viewsFolders = new ArrayList<>();
        private final String viewsPath;

        BladeViewsNodeList(Project project, FileObject viewsDir) {
            if (viewsDir != null && viewsDir.isValid() && viewsDir.isFolder()) {
                this.viewsPath = viewsDir.getPath();
                extractFolderAsTemplatePath(viewsDir);
            } else {
                this.viewsPath = null;
            }
        }

        private void extractFolderAsTemplatePath(FileObject dir) {
            for (FileObject file : dir.getChildren()) {
                if (file.isFolder() && file.getChildren().length > 0){
                    boolean hasFile = false;
                    for (FileObject child : file.getChildren()){
                        if (!child.isFolder()){
                            hasFile = true;
                            break;
                        }
                    }
                    
                    //add only folders
                    if (hasFile){
                        viewsFolders.add(file);
                        extractFolderAsTemplatePath(file);
                    }
                }
            }
        }

        @Override
        public List<Node> keys() {
            List<Node> keysList = new ArrayList<>(1);
            if (!viewsFolders.isEmpty()) {
                BladeTemplateFolderNodeList folders = new BladeTemplateFolderNodeList(viewsFolders, viewsPath);
                folders.setKeys();
                keysList.add(new MainNode(folders));
            }
            return keysList;
        }

        @Override
        public void addNotify() {

        }

        @Override
        public void removeNotify() {

        }

        @Override
        public void addChangeListener(ChangeListener cl) {

        }

        @Override
        public void removeChangeListener(ChangeListener cl) {

        }

        @Override
        public void stateChanged(ChangeEvent e) {

        }

        @Override
        public Node node(Node key) {
            return key;
        }
    }
    
    private static final class BladeTemplateFolderNodeList extends Children.Keys<FileObject> {

        private final String viewPath;
        List<FileObject> files = new ArrayList<>();

        BladeTemplateFolderNodeList(List<FileObject> bladeTemplatesFiles, String viewPath) {
            super(true);
            this.viewPath = viewPath;
            this.files = bladeTemplatesFiles;
        }

        @Override
        protected Node[] createNodes(FileObject file) {
            try {
                DataObject dobj = DataObject.find(file);
                FilterNode fn = new BladeFolderNode(dobj.getNodeDelegate(), file, viewPath);
                return new Node[]{fn};
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }

        public void setKeys() {
            List<FileObject> keys = new ArrayList<>(files.size());
            keys.addAll(files);
            setKeys(keys);
        }
    }

    private static final class MainNode extends AbstractNode {

        MainNode(BladeTemplateFolderNodeList markdownFiles) {
            super(markdownFiles);
            setDisplayName(VIEWS_TITLE);
        }

        @Override
        public Image getIcon(int type) {
            return iconDelegate.getIcon(type);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return iconDelegate.getIcon(type);
        }
    }

    private static final class BladeFolderNode extends FilterNode {

        private final FileObject folder;
        private final String viewPath;
        
        BladeFolderNode(Node node, FileObject folder, String viewPath) {
            super(node);
            this.folder = folder;
            this.viewPath = viewPath;
        }

        @Override
        public Image getIcon(int type) {
            return iconDelegate.getIcon(type);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return iconDelegate.getIcon(type);
        }
        
        @Override
        public String getDisplayName(){
            return folder.getPath()
                    .replace(viewPath, "") //NOI18N
                    .replace(PathUtils.SLASH, PathUtils.DOT)
                    .replaceFirst("^.", ""); //NOI18N
        }
    }
}
