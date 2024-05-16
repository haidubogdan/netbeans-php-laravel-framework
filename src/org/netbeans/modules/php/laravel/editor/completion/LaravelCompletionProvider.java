/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor.completion;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.*;
import org.netbeans.api.*;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
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

    private String methodName;

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        int offset = jtc.getCaretPosition();
        BaseDocument baseDoc = (BaseDocument) jtc.getDocument();
        int lineStart = LineDocumentUtils.getLineStart(baseDoc, offset);
        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(jtc.getDocument(), offset);

        if (tokensq == null){
            return null;
        }
        
        Token<PHPTokenId> currentToken = tokensq.token();
        int startOffset = tokensq.offset();

        if (currentToken == null) {
            return null;
        }

        PHPTokenId prevTokenId = null;

        while (tokensq.movePrevious() && tokensq.offset() >= lineStart) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }
            String text = token.text().toString();
            PHPTokenId id = token.id();

            if (prevTokenId != null && id.equals(PHPTokenId.PHP_STRING)
                    && (text.equals("config"))) {
                methodName = text;
                String quotedBladePath = currentToken.text().toString();
                int x = 1;
//                bladePath = quotedBladePath.substring(1, quotedBladePath.length() - 1);
//                return new int[]{startOffset, startOffset + currentToken.length()};
            }

            if (id.equals(PHPTokenId.PHP_TOKEN) && text.equals("(")) {
                prevTokenId = id;
            }
        }
        return null;
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return COMPLETION_QUERY_TYPE;
    }

}
