/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor;

import java.util.List;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;

public class EditorUtils {

    public static TokenSequence<PHPTokenId> getTokenSequence(Document doc, int offset) {
        BaseDocument baseDoc = (BaseDocument) doc;
        TokenSequence<PHPTokenId> tokenSequence = null;
        baseDoc.readLock();
        try {
            TokenHierarchy<Document> hierarchy = TokenHierarchy.get(baseDoc);
            tokenSequence = hierarchy.tokenSequence(PHPTokenId.language());
            if (tokenSequence == null) {
                List<TokenSequence<?>> embeddedTks = hierarchy.embeddedTokenSequences(offset, false);
                for (TokenSequence t : embeddedTks) {
                    Language lang = t.language();
                    if (lang == null || lang.mimeType() == null) {
                        continue;
                    }
                    String mime = lang.mimeType();
                    if (mime.equals(FileUtils.PHP_MIME_TYPE)) {
                        tokenSequence = t;
                        break;
                    }
                }
            }
        } finally {
            baseDoc.readUnlock();
        }
        if (tokenSequence != null) {
            tokenSequence.move(offset);
            tokenSequence.moveNext();
        }
        return tokenSequence;
    }

    public static boolean isOpenParenToken(PHPTokenId id, String text) {
        return id.equals(PHPTokenId.PHP_TOKEN) && text.equals("("); // NOI18N
    }
}
