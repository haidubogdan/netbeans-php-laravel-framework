/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor.completion;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.*;
import org.netbeans.api.*;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.laravel.editor.EditorUtils;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

/**
 *
 * @author bogdan
 */
@MimeRegistration(mimeType = FileUtils.PHP_MIME_TYPE, service = CompletionProvider.class)
public class LaravelCompletionProvider implements CompletionProvider {

    private int argCount;

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        TokenSequence<PHPTokenId> ts = EditorUtils.getTokenSequence(jtc.getDocument(), jtc.getCaretPosition());
         final String methodName = getMethodName(ts);
//        Method method = Method.Factory.create(methodName, phpModule, fo);
//        if (method == null) {
        return null;
//        }
        //return new AsyncCompletionTask(new AsyncCompletionQueryImpl(method, argCount), jtc);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return 0;
    }

    //might use a small parser
    private String getMethodName(TokenSequence<PHPTokenId> ts) {
        argCount = 1;
        while (ts.movePrevious()) {
            Token<PHPTokenId> token = ts.token();
            String text = token.text().toString();
            PHPTokenId id = token.id();
            if (id == PHPTokenId.PHP_SEMICOLON) {
                break;
            }

            // TODO must improve
            // if argument is array or method, it's not correct.
            if (text.contains(",") && id != PHPTokenId.PHP_STRING) { // NOI18N
                argCount++;
            }
            int y = 1;
//            if (Method.METHODS.contains(text) && id == PHPTokenId.PHP_STRING) {
//                return text;
//            }
        }
        return null;
    }
}
