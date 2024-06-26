package org.netbeans.modules.php.laravel.views;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import org.openide.nodes.AbstractNode;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ChangeSupport;
import org.openide.nodes.Children;
import org.openide.util.Exceptions;

/**
 *
 * @author bhaidu
 */
public class ViewsFiles {
    private static final Node iconDelegate = DataFolder.findFolder(FileUtil.getConfigRoot()).getNodeDelegate();
     
    @NodeFactory.Registration(projectType = "org-netbeans-modules-php-project", position = 400)
    public static NodeFactory forPhpProject() {
        return new BladeViewsFilesNodeFactory();
    }

    private static final class BladeViewsFilesNodeFactory implements NodeFactory {

        @Override
        public NodeList<?> createNodes(Project project) {
            assert project != null;
            FileObject source = project.getProjectDirectory();
            String rootAbsPath = FileUtil.toFile(source).getAbsolutePath() + "\\resources\\views\\";
            String rootPath = FileUtil.toFile(source).getAbsolutePath() + "\\resources\\views\\";
            return new BladeViewsNodeList(project, rootPath);
        }

    }

    private static final class BladeViewsNodeList implements NodeList<Node>, ChangeListener {

//        private final Project project;
//        private final ChangeSupport changeSupport = new ChangeSupport(this);
        private final List<FileObject> viewsFiles = new ArrayList<>();
        private final List<FileObject> viewsFolders = new ArrayList<>();
        public String rootPath;
       

        BladeViewsNodeList(Project project, String rootPath) {
            //this.project = project;
            FileObject viewsFolder = project.getProjectDirectory().getFileObject("resources/views");

            if (viewsFolder != null && viewsFolder.isValid() && viewsFolder.isFolder()) {
                extractFolderAsTemplatePath(viewsFolder);
            }
            this.rootPath = rootPath;
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
                } else {
                    
                }
            }
        }

        @Override
        public List<Node> keys() {
            List<Node> keysList = new ArrayList<>(1);
            if (!viewsFolders.isEmpty()) {
                BladeTemplateFolderNodeList folders = new BladeTemplateFolderNodeList(viewsFolders, rootPath);
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

        List<FileObject> files = new ArrayList<>();
        private String rootPath;

        BladeTemplateFolderNodeList(List<FileObject> bladeTemplatesFiles, String rootPath) {
            super(true);
            this.files = bladeTemplatesFiles;
            this.rootPath = rootPath;
        }

        @Override
        protected Node[] createNodes(FileObject file) {
            try {
                DataObject dobj = DataObject.find(file);
                FilterNode fn = new BladeFolderNode(dobj.getNodeDelegate(), file, rootPath);
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
            setDisplayName("Blade Views files");
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

        FileObject folder;
        String rootPath;
        
        BladeFolderNode(Node node, FileObject folder, String rootPath) {
            super(node);
            this.folder = folder;
            this.rootPath = rootPath;
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
            return FileUtil.toFile(folder).getAbsolutePath().replace(rootPath, "").replace("\\", ".");
        }
    }
}
