package org.netbeans.modules.php.laravel.astnodes;

import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayCreation;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.ReturnStatement;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bogdan
 */
public final class ArrayConfigVisitor extends ArrayFileVisitor {

    public ArrayConfigVisitor(FileObject controller, Model model, boolean withOffset) {
        super(controller, model, withOffset);
    }

    @Override
    public void visit(ReturnStatement node) {
        Expression expression = node.getExpression();

        if (expression instanceof ArrayCreation) {
            if (withOffset){
                processWithOffset((ArrayCreation) expression);
            } else {
                process((ArrayCreation) expression);
            }
        }
    }

}
