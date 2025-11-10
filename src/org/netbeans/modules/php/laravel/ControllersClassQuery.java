package org.netbeans.modules.php.laravel;

import org.netbeans.modules.php.laravel.editor.completion.ControllerModel;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.api.Utils;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.netbeans.modules.php.laravel.astnodes.ArrayConfigVisitor;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.util.Pair;

/**
 *
 * @author bhaidu
 */
public class ControllersClassQuery extends FileChangeAdapter {

    private static final Logger LOGGER = Logger.getLogger(ControllersClassQuery.class.getName());
    public static final String CONTROLLER_PATH = "app/Http/Controllers"; // NOI18N
    //not sure about it
    private final Map<FileObject, ControllerModel> collection = new HashMap<>();
    private final Map<String, Set<String>> methodControllersReferences = new HashMap<>();

    public void parseControllers(FileObject sourceDir) {
        if (sourceDir == null) {
            // broken project
            return;
        }

        FileObject controllerDir = sourceDir.getFileObject(CONTROLLER_PATH);

        if (controllerDir == null || !controllerDir.isFolder()) {
            return;
        }

        Enumeration<? extends FileObject> children = controllerDir.getChildren(true);

        while (children.hasMoreElements()) {
            FileObject file = children.nextElement();
            if (file.isFolder()) {
                continue;
            }
            parseControllerFile(file, collection);
        }
    }

    private void parseControllerFile(FileObject file, Map<FileObject, ControllerModel> collection) {
        try {
            ParserManager.parse(Collections.singleton(Source.create(file)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    PHPParseResult parseResult = (PHPParseResult) resultIterator.getParserResult();
                    if (parseResult != null && parseResult.getProgram() != null) {
                        ControllerModel controllerModel = new ControllerModel(file);
                        parseResult.getProgram().accept(new ControllerModelVisitor(controllerModel));
                        if (controllerModel.isValid()) {
                            collection.put(file, controllerModel);
                        }
                    }

                }

            });
        } catch (ParseException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }
    
    public Map<String, Set<String>> getClassMethodReferences() {
        return methodControllersReferences;
    }
    
    public Map<String, Set<String>> findClassReferences(String method) {
        Map<String, Set<String>> filteredMethodReference = new HashMap<>();

        for (String methodName : methodControllersReferences.keySet()) {
            if (methodName.startsWith(method)) {
                Set<String> classes = methodControllersReferences.get(methodName);
                filteredMethodReference.computeIfAbsent(methodName, s -> new HashSet<>()).addAll(classes);
            }
        }

        return filteredMethodReference;
    }

    private class ControllerModelVisitor extends DefaultVisitor {

        private final ControllerModel model;
        private String className;
        private final Set<String> methods = new HashSet<>();

        public ControllerModelVisitor(ControllerModel model) {
            this.model = model;
        }

        @Override
        public void scan(ASTNode node) {
            if (node != null) {
                super.scan(node);
            }
        }

        @Override
        public void visit(ClassDeclaration node) {
            super.visit(node);
            Expression superClass = node.getSuperClass();
            if (superClass != null && node.getName().getName() != null) {
                String superClassName = sanitazeClassName(superClass.toString());
                className = node.getName().getName();
                model.checkClassValidity(superClassName);
                for (String method : methods) {
                    methodControllersReferences.computeIfAbsent(method, s -> new HashSet<>()).add(className);
                }
            }
        }

        @Override
        public void visit(MethodDeclaration node) {
            if (node.getFunction() == null) {
                return;
            }
            String functionName = node.getFunction().getFunctionName().getName();

            if (functionName == null) {
                return;
            }

            if (functionName.startsWith("__") || node.getModifier() != 1) {
                return;
            }

            model.addMethod(functionName);
            methods.add(functionName);

            if (className != null) {
                methodControllersReferences.computeIfAbsent(functionName, s -> new HashSet<>()).add(className);
            }

        }

        private String sanitazeClassName(String className) {
            return className.replace("\\", ""); // NOI18N
        }
    }

    private final class FileChangeListenerImpl extends FileChangeAdapter {

        @Override
        public void fileFolderCreated(FileEvent fe) {

        }

        @Override
        public void fileChanged(FileEvent fe) {
            processFile(fe.getFile());
        }

        @Override
        public void fileDataCreated(FileEvent fe) {

        }

        private void processFile(FileObject file) {
            assert file.isData() : file;
            //parseComponentFile(file, componentClassCollection);
        }

    }
}
