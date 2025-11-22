package org.netbeans.modules.php.laravel.editor.model;

import java.util.HashSet;
import java.util.Set;
import org.openide.filesystems.FileObject;

public class ControllerModel {

    private final FileObject file;
    private final Set<String> methodNames = new HashSet<>();

    private boolean isValid = false;
    
    public ControllerModel(FileObject file) {
        this.file = file;
    }

    public FileObject getFile() {
        return file;
    }
    
    public boolean isValid() {
        return isValid;
    }

    public void checkClassValidity(String className) {
        isValid = className.endsWith("Controller"); // NOI18N
    }
    
    public void addMethod(String method) {
        this.methodNames.add(method);
    }
    
    public Set<String> getMethodNames() {
        return methodNames;
    }

}
