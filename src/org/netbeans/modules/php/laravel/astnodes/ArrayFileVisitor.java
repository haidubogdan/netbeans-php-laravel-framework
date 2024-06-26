package org.netbeans.modules.php.laravel.astnodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayCreation;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayElement;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import static org.netbeans.modules.php.editor.parser.astnodes.Scalar.Type.STRING;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdan
 */
public abstract class ArrayFileVisitor extends DefaultVisitor {

    private final Map<String, List<String>> configNamespaceStringMap = new HashMap<>();
    private ConfigNamespace configNamespaceRoot;
    protected final FileObject file;
    protected final Model model;
    protected final boolean withOffset;

    protected ASTNode actionDeclaration;

    public ArrayFileVisitor(FileObject file, Model model, boolean withOffset) {
        assert file != null;
        assert model != null;
        this.file = file;
        this.model = model;
        this.withOffset = withOffset;
    }

    public ArrayFileVisitor(FileObject file, Model model) {
        this(file, model, false);
    }

    protected void process(ArrayCreation arrayCreation) {
        List<ArrayElement> elements = arrayCreation.getElements();
        for (ArrayElement arrayElement : elements) {
            if (!(arrayElement.getKey() instanceof Scalar)) {
                // not string key
                continue;
            }
            Scalar scalar = (Scalar) arrayElement.getKey();
            String stringValue = scalar.getStringValue();
            String key = stringValue.substring(1, stringValue.length() - 1);
            List<String> configKeyNamespaces = new ArrayList<>();

            if (arrayElement.getValue() instanceof ArrayCreation) {
                flattenConfigTree(key, (ArrayCreation) arrayElement.getValue(), configKeyNamespaces);
            }

            configNamespaceStringMap.put(key, configKeyNamespaces);
        }
    }
    
    protected void processWithOffset(ArrayCreation arrayCreation) {
        List<ArrayElement> elements = arrayCreation.getElements();

        configNamespaceRoot = new ConfigNamespace(this.file.getName(), 0);

        for (ArrayElement arrayElement : elements) {
            if (!(arrayElement.getKey() instanceof Scalar)) {
                // not string key
                continue;
            }
            Scalar scalar = (Scalar) arrayElement.getKey();
            String stringValue = scalar.getStringValue();
            String key = stringValue.substring(1, stringValue.length() - 1);
            
            ConfigNamespace configNamespaceNode =  new ConfigNamespace(key, scalar.getStartOffset());

            if (arrayElement.getValue() instanceof ArrayCreation) {
                processConfigTreeWithOffset((ArrayCreation) arrayElement.getValue(), configNamespaceNode);
            }
            
            configNamespaceRoot.children.add(configNamespaceNode);
        }
    }

    private void flattenConfigTree(String key, ArrayCreation value, List<String> configKeyNamespaces) {
        List<ArrayElement> elements = value.getElements();

        for (ArrayElement arrayElement : elements) {
            if (!(arrayElement.getKey() instanceof Scalar)) {
                continue;
            }
            Scalar scalarKey = (Scalar) arrayElement.getKey();
            if (!scalarKey.getScalarType().equals(STRING)) {
                continue;
            }
            String elementKey = extractValueFromScalar(scalarKey);
            String pathRoot = key + "." + elementKey;
            configKeyNamespaces.add(pathRoot);

            if (arrayElement.getValue() instanceof ArrayCreation) {
                flattenConfigTree(pathRoot, (ArrayCreation) arrayElement.getValue(), configKeyNamespaces);
            }
        }
    }
    
    private void processConfigTreeWithOffset(ArrayCreation value, ConfigNamespace configNamespace) {
        List<ArrayElement> elements = value.getElements();

        for (ArrayElement arrayElement : elements) {
            if (!(arrayElement.getKey() instanceof Scalar)) {
                continue;
            }
            Scalar scalarKey = (Scalar) arrayElement.getKey();
            if (!scalarKey.getScalarType().equals(STRING)) {
                continue;
            }
            String elementKey = extractValueFromScalar(scalarKey);
            ConfigNamespace configNamespaceNode =  new ConfigNamespace(elementKey, scalarKey.getStartOffset());
            configNamespace.children.add(configNamespaceNode);

            if (arrayElement.getValue() instanceof ArrayCreation) {
                processConfigTreeWithOffset((ArrayCreation) arrayElement.getValue(), configNamespaceNode);
            }
        }
    }

    public Map<String, List<String>> getConfigNamespaceString() {
        return configNamespaceStringMap;
    }
    
    public ConfigNamespace getConfigNamespace() {
        return configNamespaceRoot;
    }

    public String extractValueFromScalar(Scalar scalar) {
        String stringValue = scalar.getStringValue();
        return stringValue.substring(1, stringValue.length() - 1);
    }

    public void setActionMethod(ASTNode actionDeclaration) {
        this.actionDeclaration = actionDeclaration;
    }

    public static class ConfigNamespace {
        public String namespace;
        public int offset;
        public ArrayList<ConfigNamespace> children = new ArrayList<>();
        
        public ConfigNamespace(String namespace, int offset){
            this.namespace = namespace;
            this.offset = offset;
        }
    }
}
