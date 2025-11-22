package org.netbeans.modules.php.laravel.editor.completion;

import java.util.Arrays;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.laravel.editor.EditorUtils;
import org.netbeans.modules.php.laravel.project.ProjectUtils;
import org.netbeans.modules.php.laravel.utils.StringUtils;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.filesystems.FileObject;

/**
 *
 * @author bhaidu
 */
@MimeRegistration(mimeType = FileUtils.PHP_MIME_TYPE, service = CompletionProvider.class)
public class ValidationCompletionProvider implements CompletionProvider {

    public static String[] VALIDATION_METHODS = new String[]{
        "validate",
        "validated",}; // NOI18N 

    public static String[] VALIDATION_RULES = new String[]{
        "required",
        "string",
        "unique:",
        "max:",
        "min:",
        "integer",
        "lowercase",
        "uppercase",
        "confirmed",
        "email"
    };// NOI18N 
    
    @Override
    public CompletionTask createTask(int i, JTextComponent component) {
        FileObject currentFile = NbEditorUtilities.getFileObject(component.getDocument());

        if (currentFile == null) {
            return null;
        }

        //only if file is in App/Http ??
        PhpModule module = ProjectUtils.getPhpModule(currentFile);

        if (module == null || module.getSourceDirectory() == null) {
            return null;
        }

        if (!ProjectUtils.isInLaravelModule(module)) {
            return null;
        }

        String reference = getContextQueryString(component.getDocument(), component.getCaretPosition());

        if (reference != null) {
            AsyncCompletionQuery completionQuery = new RuleCompletionQuery();
            return new AsyncCompletionTask(completionQuery, component);
        }

        return null;
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String string) {
        if (component.getDocument() == null) {
            return 0;
        }
        if (!ProjectUtils.isInLaravelModule(component.getDocument())) {
            return 0;
        }
        return COMPLETION_QUERY_TYPE;
    }

    private String getContextQueryString(Document doc, int offset) {

        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(doc, offset);

        if (tokensq == null) {
            return null;
        }

        Token<PHPTokenId> currentToken = tokensq.token();

        if (currentToken == null) {
            return null;
        }

        String currentTokenText = currentToken.text().toString();

        if (!StringUtils.isQuotedString(currentTokenText)) {
            return null;
        }

        boolean lookForMethCallOperator = false;
        boolean methodCallOperatorFound = false;

        while (tokensq.movePrevious()) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }

            String text = token.text().toString();
            PHPTokenId id = token.id();

            if (text.equals(";") || text.equals("{")) {
                //end of action
                break;
            }

            if (text.equals("->")) {//PHP_OBJECT_OPERATOR
                methodCallOperatorFound = true;
                break;
            }

            if (lookForMethCallOperator && text.equals("::")) {
                methodCallOperatorFound = true;
                break;
            }

            if (id.equals(PHPTokenId.PHP_STRING)) {
                if (Arrays.asList(VALIDATION_METHODS).indexOf(text) > -1) {
                    lookForMethCallOperator = true;
                }
            }

        }

        if (lookForMethCallOperator && methodCallOperatorFound) {
            return currentTokenText;
        }

        return null;
    }

    private QueryToken getTypedString(Document doc, int offset) {
        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(doc, offset);

        if (tokensq == null) {
            return null;
        }

        Token<PHPTokenId> currentToken = tokensq.token();

        if (currentToken == null) {
            return null;
        }

        String currentTokenText = currentToken.text().toString();

        if (!StringUtils.isQuotedString(currentTokenText)) {
            return null;
        }

        currentTokenText = currentTokenText.substring(1, currentTokenText.length() - 1);
        
        return new QueryToken(currentTokenText, tokensq.offset());
    }

    private static class RuleContext {

        public enum RuleContextType {
            ARRAY,
            INLINE
        };
    }
    
    private static class QueryToken {
        
        private final String queryText;
        private final int tokenOffset;
        
        public QueryToken(String queryText, int tokenOffset) {
            this.queryText = queryText;
            this.tokenOffset = tokenOffset;
        }

        public String getQueryText() {
            return queryText;
        }
        
        public int getTokenOffset() {
            return tokenOffset;
        }
    }

    private class RuleCompletionQuery extends AsyncCompletionQuery {

        public RuleCompletionQuery() {

        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {

                QueryToken queryToken = getTypedString(doc, caretOffset);

                if (queryToken == null) {
                    return;
                }

                String ruleQuery = queryToken.getQueryText();

                if (ruleQuery.contains("|")) {
                    int offsetCaretPos = caretOffset - queryToken.getTokenOffset();
                    String ruleSnippet = ruleQuery.substring(0, offsetCaretPos);
                    int lastSeparatorPos = ruleSnippet.lastIndexOf("|");
                    String[] rulesList = ruleSnippet.split("\\|");
                    int separatorOccurences = rulesList.length;
                    if ((ruleSnippet.endsWith("|") && separatorOccurences == 1) || lastSeparatorPos == -1) {
                        if (ruleSnippet.endsWith("|")) {
                            ruleQuery = ruleSnippet.substring(0, lastSeparatorPos);
                        } else {
                            ruleQuery = ruleSnippet;
                        }

                    } else {
                        if (ruleSnippet.endsWith("|")) {
                            ruleSnippet = ruleSnippet.substring(0, lastSeparatorPos);
                            lastSeparatorPos = ruleSnippet.lastIndexOf("|");
                        }
                        ruleQuery = ruleSnippet.substring(lastSeparatorPos + 1, ruleSnippet.length());
                        int y = 1;
                    }
                }

                int insertOffset = caretOffset - ruleQuery.length();
                
                for (String rule : VALIDATION_RULES) {
                    if (rule.startsWith(ruleQuery)) {
                        
                        addMethodCompletionItem(rule, resultSet, insertOffset);
                    }
                }
                
            } finally {
                resultSet.finish();
            }
        }

        private void addMethodCompletionItem(String completionText,
                CompletionResultSet resultSet, int insertOffset) {
            CompletionItem item = CompletionUtilities.newCompletionItemBuilder(completionText)
                    .iconResource("org/netbeans/modules/websvc/saas/ui/resources/method.png")
                    .startOffset(insertOffset)
                    .leftHtmlText(completionText)
                    .rightHtmlText("laravel rule")
                    .sortPriority(1)
                    .build();
            resultSet.addItem(item);
        }
    }
}
