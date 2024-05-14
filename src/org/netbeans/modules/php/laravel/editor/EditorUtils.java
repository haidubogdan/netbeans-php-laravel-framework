/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor;

import javax.swing.text.Document;
import org.netbeans.*;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.php.*;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;

/**
 *
 * @author bogdan
 */
public class EditorUtils {

    public static TokenSequence<PHPTokenId> getTokenSequence(Document doc, int offset) {
        //DocUtils.atomicLock(doc);
        TokenSequence<PHPTokenId> tokenSequence = null;
        try {
            TokenHierarchy<Document> hierarchy = TokenHierarchy.get(doc);
            tokenSequence = hierarchy.tokenSequence(PHPTokenId.language());
        } finally {
            //DocUtils.atomicUnlock(doc);
        }
        if (tokenSequence != null) {
            tokenSequence.move(offset);
            tokenSequence.moveNext();
        }
        return tokenSequence;

    }
}
