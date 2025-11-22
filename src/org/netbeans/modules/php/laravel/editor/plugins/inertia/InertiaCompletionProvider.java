package org.netbeans.modules.php.laravel.editor.plugins.inertia;

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
import org.netbeans.modules.php.laravel.editor.ResourceUtilities;
import static org.netbeans.modules.php.laravel.editor.plugins.inertia.InertiaPluginConstants.INERTIA_METHOD;
import static org.netbeans.modules.php.laravel.editor.plugins.inertia.InertiaPluginConstants.PAGES_RESOURCES_DIR_PATH;
import org.netbeans.modules.php.laravel.project.ProjectUtils;
import org.netbeans.modules.php.laravel.utils.LaravelUtils;
import org.netbeans.modules.php.laravel.utils.StringUtils;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.filesystems.FileObject;

@MimeRegistration(mimeType = FileUtils.PHP_MIME_TYPE, service = CompletionProvider.class)
public class InertiaCompletionProvider implements CompletionProvider {

    private String methodName;
    
    public static String[] DIRECT_FUNCTION = new String[]{
        INERTIA_METHOD,
    }; 
    
    public static String[] CLASS_METHODS = new String[]{
        "render",
    }; // NOI18N  
    @Override
    public CompletionTask createTask(int i, JTextComponent component) {
        FileObject currentFile = NbEditorUtilities.getFileObject(component.getDocument());
        
        if (currentFile == null) {
            return null;
        }
        
        PhpModule module = ProjectUtils.getPhpModule(currentFile);

        if (module == null || module.getSourceDirectory() == null) {
            return null;
        }

        if (!ProjectUtils.isInLaravelModule(module)) {
            return null;
        }
        
        String reference = getQueryString(component.getDocument(), component.getCaretPosition());
        
        if (reference != null) {
            FileObject sourceDir = module.getSourceDirectory();
            AsyncCompletionQuery completionQuery = null;
            
            switch (methodName) {
                case INERTIA_METHOD:
                case "render": // NOI18N
                    completionQuery = new InertiaViewCompletionQuery(sourceDir);
                    break;
            }
            
            if (completionQuery != null) {
                return new AsyncCompletionTask(completionQuery, component);
            }
        }
        
        return null;
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String string) {
        if (component.getDocument() == null) {
            return 0;
        }

        //debug
        return COMPLETION_QUERY_TYPE;
    }
    
    private String getQueryString(Document doc, int offset) {
        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(doc, offset);

        if (tokensq == null) {
            return null;
        }

        Token<PHPTokenId> currentToken = tokensq.token();

        if (currentToken == null) {
            return null;
        }
        
        PHPTokenId openParenToken = null;

        String quotedReference = ""; // NOI18N
        int tokenCount = 0;
        boolean lookForMethod = true;
        boolean lookForClass = false;
        while (tokensq.movePrevious() && tokenCount <= 6) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }

            String text = token.text().toString();
            PHPTokenId id = token.id();

            if (id.equals(PHPTokenId.WHITESPACE) || text.equals("[") || text.equals(",")) {
                continue;
            }

            tokenCount++;
            if (openParenToken != null && id.equals(PHPTokenId.PHP_STRING)) {
                if (lookForMethod) {
                    if ( Arrays.asList(CLASS_METHODS).indexOf(text) > -1) {
                        //TODO avoid this hidden setting
                        //use a class
                        methodName = text;
                        quotedReference = currentToken.text().toString();
                        lookForMethod = false;
                        lookForClass = true;
                    } else if (Arrays.asList(DIRECT_FUNCTION).indexOf(text) > -1) {
                        methodName = text;
                        quotedReference = currentToken.text().toString();
                        break;
                    }
                } else if (lookForClass) {
                    if (!text.equals("Inertia")) {
                        methodName = null;
                    }
                    break;
                } else {
                    break;
                }
            }

            if (EditorUtils.isOpenParenToken(id, text)) {
                openParenToken = id;
            }
        }

        if (quotedReference.length() < 2 || LaravelUtils.isVariable(quotedReference)) {
            return null;
        }
        if (StringUtils.isQuotedString(quotedReference)) {
            String reference = quotedReference.substring(1, quotedReference.length() - 1);
            return reference;
        }
        return null;
    }
    
    private class InertiaViewCompletionQuery extends AsyncCompletionQuery {

        private final FileObject sourceDir;

        public InertiaViewCompletionQuery(FileObject sourceDir) {
            this.sourceDir = sourceDir;
        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {
                if (sourceDir == null) {
                    return;
                }
                
                //TODO separate query and context info
                String query = getQueryString(doc, caretOffset);
                if (query == null) {
                    return;
                }     

                //get this from config js
                String rootPagePath = PAGES_RESOURCES_DIR_PATH;
                int lastSlashPos = query.lastIndexOf("/");
                
                FileObject folderObj = null;
                String fileName = null;
                
                if (lastSlashPos > 0) {
                    if (lastSlashPos < query.length()) {
                        String folderPath = query.substring(0, lastSlashPos);
                        folderObj = sourceDir.getFileObject(rootPagePath + folderPath);
                        fileName = query.substring(lastSlashPos + 1).toLowerCase();
                    }
                } else {
                    folderObj = sourceDir.getFileObject(rootPagePath);
                    fileName = query.toLowerCase();
                }
                
                if (fileName != null && folderObj != null && folderObj.isFolder()) {
                    int pathOffset = caretOffset - fileName.length();
                    for (FileObject file : folderObj.getChildren()) {
                        String fName = file.getName().toLowerCase();
                        if (fName.startsWith(fileName)) {
                            addViewCompletionItem(file.getName(), file, sourceDir, pathOffset, resultSet);
                        }
                    }
                }
                
            } finally {
//                long time = System.currentTimeMillis() - startTime;
                resultSet.finish();
            }
        }

        private void addViewCompletionItem(String completion, FileObject file, FileObject sourceDir,
                int caretOffset, CompletionResultSet resultSet) {
            int insertOffset = caretOffset;
            String imagePath = ResourceUtilities.ICON_BASE + "icons/file.png";//NOI18N
            if (file.isFolder()) {
                imagePath = "org/openide/loaders/defaultFolder.gif";//NOI18N
            }

            String filePath = file.getPath().replace(sourceDir.getPath(), "");

            CompletionItem item = CompletionUtilities.newCompletionItemBuilder(completion)
                    .iconResource(imagePath)
                    .startOffset(insertOffset)
                    .leftHtmlText(completion)
                    .rightHtmlText(filePath)
                    .sortPriority(1)
                    .build();
            resultSet.addItem(item);
        }
    }
    
}
