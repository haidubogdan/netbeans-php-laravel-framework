/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.php.api.editor.PhpBaseElement;
import org.netbeans.modules.php.api.editor.PhpVariable;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.api.Utils;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayCreation;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayElement;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.ReturnStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.netbeans.modules.php.laravel.utils.LaravelUtils;
import org.netbeans.modules.php.spi.editor.EditorExtender;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author bogdan
 */
public class LaravelEditorExtender extends EditorExtender {

    @Override
    public List<PhpBaseElement> getElementsForCodeCompletion(FileObject fo) {
        File file = FileUtil.toFile(fo);
        if (LaravelUtils.isConfig(fo)) {
            //HashMap<String, ArrayElement>(parseConfig(file));
        }
        return Collections.emptyList();
    }

    private Map<String, ArrayElement> parseConfig(final File config) {

        FileObject fo = FileUtil.toFileObject(config);
        final Set<PhpVariable> phpVariables = Collections.synchronizedSet(new HashSet<>());
        try {
            ParserManager.parse(Collections.singleton(Source.create(fo)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    PHPParseResult parseResult = (PHPParseResult) resultIterator.getParserResult();
                    // find actions
                    Model model = parseResult.getModel();
                    ArrayVisitor arrayVisitor = new ArrayVisitor(fo, model);
                    arrayVisitor.scan(Utils.getRoot(parseResult));
                    int x = 1;
                }

            });
        } catch (ParseException ex) {
            //LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }

    private static final class ArrayVisitor extends LaravelVisitor {

        public ArrayVisitor(FileObject controller, Model model) {
            super(controller, model);
        }

        @Override
        public void visit(ReturnStatement node) {
            Expression expression = node.getExpression();
            
            if (expression instanceof ArrayCreation) {
                process((ArrayCreation) expression);
            }
        }

    }

    private abstract static class LaravelVisitor extends DefaultVisitor {

        private final Map<String, ArrayElement> configKeys = new HashMap<>();
        protected final FileObject controller;
        protected final Model model;

        protected ASTNode actionDeclaration;

        public LaravelVisitor(FileObject controller, Model model) {
            assert controller != null;
            assert model != null;
            this.controller = controller;
            this.model = model;
        }

        protected void addConfigKeys(String varName, ArrayElement value) {
            configKeys.put(varName, value);
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
                addConfigKeys(key, arrayElement);
            }
        }

        public Map<String, ArrayElement> getConfigKeys() {
            return configKeys;
        }

        public void setActionMethod(ASTNode actionDeclaration) {
            this.actionDeclaration = actionDeclaration;
        }

    }
}
