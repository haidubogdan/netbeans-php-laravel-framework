package org.netbeans.modules.php.laravel.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.api.Utils;
import org.netbeans.modules.php.laravel.astnodes.ArrayTreeVisitor;
import org.netbeans.modules.php.laravel.astnodes.ArrayFileVisitor;
import org.netbeans.modules.php.laravel.astnodes.ArrayFileVisitor.ConfigNamespace;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdan
 */
public class ConfigurationFileParser {
    
    public Map<String, List<String>> getConfigTree(FileObject file) {
        final Map<String, List<String>> configKeys = new HashMap<>();
        try {
            ParserManager.parse(Collections.singleton(Source.create(file)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    PHPParseResult parseResult = (PHPParseResult) resultIterator.getParserResult();
                    // find actions
                    Model model = parseResult.getModel();
                    ArrayTreeVisitor arrayVisitor = new ArrayTreeVisitor(file, model, false);
                    arrayVisitor.scan(Utils.getRoot(parseResult));
                    configKeys.putAll(arrayVisitor.getConfigNamespaceString());
                }

            });
        } catch (ParseException ex) {
            //LOGGER.log(Level.WARNING, null, ex);
        }
        return configKeys;
    }
    
    public ConfigNamespace getConfigTreeWithOffset(FileObject file) {
        ConfigNamespace root = new ConfigNamespace(file.getName(), 0);
        try {
            ParserManager.parse(Collections.singleton(Source.create(file)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    PHPParseResult parseResult = (PHPParseResult) resultIterator.getParserResult();
                    // find actions
                    Model model = parseResult.getModel();
                    ArrayTreeVisitor arrayVisitor = new ArrayTreeVisitor(file, model, true);
                    arrayVisitor.scan(Utils.getRoot(parseResult));
                    root.children.addAll(arrayVisitor.getConfigNamespace().children);
                }

            });
        } catch (ParseException ex) {
            //LOGGER.log(Level.WARNING, null, ex);
        }
        return root;
    }

}
