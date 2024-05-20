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
    private static String rootPath;
     
    @NodeFactory.Registration(projectType = "org-netbeans-modules-php-project", position = 400)
    public static NodeFactory forPhpProject() {
        return new BladeViewsFilesNodeFactory();
    }

    private static final class BladeViewsFilesNodeFactory implements NodeFactory {

        @Override
        public NodeList<?> createNodes(Project project) {
            assert project != null;
            FileObject source = project.getProjectDirectory();
            rootPath = source.getPath() + "/resources/views/";
            return new BladeViewsNodeList(project);
        }

    }

    private static final class BladeViewsNodeList implements NodeList<Node>, ChangeListener {

//        private final Project project;
//        private final ChangeSupport changeSupport = new ChangeSupport(this);
        private final List<FileObject> viewsFiles = new ArrayList<>();
        private final List<FileObject> viewsFolders = new ArrayList<>();
       

        BladeViewsNodeList(Project project) {
            //this.project = project;
            FileObject viewsFolder = project.getProjectDirectory().getFileObject("resources/views");

            if (viewsFolder != null && viewsFolder.isValid() && viewsFolder.isFolder()) {
                extractFolderAsTemplatePath(viewsFolder);
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
                } else {
                    
                }
            }
        }

        @Override
        public List<Node> keys() {
            List<Node> keysList = new ArrayList<>(1);
            if (!viewsFolders.isEmpty()) {
                BladeTemplateFolderNodeList folders = new BladeTemplateFolderNodeList(viewsFolders);
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

        BladeTemplateFolderNodeList(List<FileObject> mdFiles) {
            super(true);
            this.files = mdFiles;
        }

        @Override
        protected Node[] createNodes(FileObject file) {
            try {
                DataObject dobj = DataObject.find(file);
                FilterNode fn = new BladeFolderNode(dobj.getNodeDelegate(), file);
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

    private static final class MarkdownFilesNodeList extends Children.Keys<FileObject> {

        List<FileObject> files = new ArrayList<>();

        MarkdownFilesNodeList(List<FileObject> mdFiles) {
            super(true);
            this.files = mdFiles;
        }

        @Override
        protected Node[] createNodes(FileObject file) {
            try {
                DataObject dobj = DataObject.find(file);
                FilterNode fn = new FilterNode(dobj.getNodeDelegate(), Children.LEAF);
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
        
        BladeFolderNode(Node node, FileObject folder) {
            super(node);
            this.folder = folder;
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
            return folder.getPath().replace(rootPath, "").replace("/", ".").replace("\\", ".");
        }
    }
}
