/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor.completion;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.laravel.ConfigurationFiles;
import org.netbeans.modules.php.laravel.editor.EditorUtils;
import org.netbeans.modules.php.laravel.editor.ResourceUtilities;
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
 * @author bogdan
 */
@MimeRegistration(mimeType = FileUtils.PHP_MIME_TYPE, service = CompletionProvider.class)
public class LaravelCompletionProvider implements CompletionProvider {

    private String methodName;

    public static String[] QUERY_METHODS = new String[]{"config", "view", "make", "render"};

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        PhpModule module = ProjectUtils.getPhpModule(component.getDocument());

        if (module == null) {
            return null;
        }

        if (!ProjectUtils.isInLaravelModule(module)) {
            return null;
        }

        String reference = getQueryString(component.getDocument(), component.getCaretPosition());

        if (reference != null) {
            AsyncCompletionQuery completionQuery;

            switch (methodName) {
                case "config":
                    completionQuery = new ConfigurationCompletionQuery(module);
                    break;
                case "view":
                case "make":
                case "render":
                    completionQuery = new ViewCompletionQuery(module);
                    break;
                default:
                    return null;
            }

            return new AsyncCompletionTask(completionQuery, component);
        }
        return null;
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        if (component.getDocument() == null) {
            return 0;
        }
        if (!ProjectUtils.isInLaravelModule(component.getDocument())) {
            return 0;
        }
        return COMPLETION_QUERY_TYPE;
    }

    private String getQueryString(Document doc, int offset) {
        BaseDocument baseDoc = (BaseDocument) doc;

        int lineStart = LineDocumentUtils.getLineStart(baseDoc, offset);
        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(doc, offset);

        if (tokensq == null) {
            return null;
        }

        Token<PHPTokenId> currentToken = tokensq.token();

        if (currentToken == null) {
            return null;
        }

        PHPTokenId prevTokenId = null;

        String quotedReference = "";

        while (tokensq.movePrevious() && tokensq.offset() >= lineStart) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }
            String text = token.text().toString();
            PHPTokenId id = token.id();

            if (prevTokenId != null && id.equals(PHPTokenId.PHP_STRING)
                    && (Arrays.asList(QUERY_METHODS).indexOf(text) > -1)) {
                methodName = text;
                quotedReference = currentToken.text().toString();
                break;
            }

            if (id.equals(PHPTokenId.PHP_TOKEN) && text.equals("(")) {
                prevTokenId = id;
            }
        }

        if (quotedReference.length() < 2 || quotedReference.startsWith("$")) {
            return null;
        }
        if (StringUtils.isQuotedString(quotedReference)) {
            String reference = quotedReference.substring(1, quotedReference.length() - 1);
            return reference;
        }
        return null;
    }

    private class ConfigurationCompletionQuery extends AsyncCompletionQuery {

        PhpModule module;

        public ConfigurationCompletionQuery(PhpModule module) {
            this.module = module;
        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            long startTime = System.currentTimeMillis();

            try {
                String query = getQueryString(doc, caretOffset);
                if (query == null) {
                    return;
                }
                ConfigurationFiles confFiles = ConfigurationFiles.getInstance(module);
                if (confFiles != null) {
                    confFiles.extractConfigurationMapping(false);
                    String[] queryConfigNamespace = query.split("\\.");
                    Map<String, Map<String, List<String>>> confFileList = confFiles.getConfigurationMapping();
                    Map<String, FileObject> confFileAlias = confFiles.getConfigurationFilesAlias();
                    String filterQuery = query;
                    if (query.endsWith(".")) {
                        filterQuery = query.substring(0, query.length() - 1);
                    }
                    for (Map.Entry<String, Map<String, List<String>>> entry : confFileList.entrySet()) {
                        String fileKey = entry.getKey();
                        if (!fileKey.startsWith(queryConfigNamespace[0])) {
                            continue;
                        }
                        FileObject configFile = confFileAlias.get(entry.getKey());
                        String configPath = "";
                        if (configFile != null) {
                            configPath = "config/" + configFile.getNameExt();
                        }
                        if (queryConfigNamespace.length == 1) {
                            if (query.endsWith(".") && !entry.getValue().isEmpty()) {
                                for (Map.Entry<String, List<String>> namespace : entry.getValue().entrySet()) {
                                    String rootKey = entry.getKey() + "." + namespace.getKey();
                                    addConfigCompletionItem(query, rootKey, configPath, caretOffset, resultSet);
                                }
                                //should be only a unique namespace
                            } else {
                                addConfigCompletionItem(query, fileKey, configPath, caretOffset, resultSet);
                            }
                            break;
                        }
                        if (queryConfigNamespace.length < 2) {
                            continue;
                        }
                        for (Map.Entry<String, List<String>> namespace : entry.getValue().entrySet()) {
                            String rootKey = entry.getKey() + "." + namespace.getKey();

                            if (!rootKey.startsWith(queryConfigNamespace[0] + "." + queryConfigNamespace[1])) {
                                continue;
                            }

                            if (queryConfigNamespace.length == 2 && !query.endsWith(".")) {
                                addConfigCompletionItem(query, rootKey, configPath, caretOffset, resultSet);
                            }

                            if (query.endsWith(".") && !namespace.getValue().isEmpty()) {

                                for (String configNamespace : namespace.getValue()) {
                                    String fullConfigNamespace = entry.getKey() + "." + configNamespace;
                                    addConfigCompletionItem(query, fullConfigNamespace, configPath, caretOffset, resultSet);
                                }
                                //should be only a unique namespace
                                break;
                            }

                            for (String configNamespace : namespace.getValue()) {
                                String fullConfigNamespace = entry.getKey() + "." + configNamespace;
                                if (fullConfigNamespace.startsWith(filterQuery)) {

                                    addConfigCompletionItem(query, fullConfigNamespace, configPath, caretOffset, resultSet);
                                }
                            }
                        }
                    }
                }
            } finally {
//                long time = System.currentTimeMillis() - startTime;
                resultSet.finish();
            }
        }

        private void addConfigCompletionItem(String prefix, String value, String filePath, int caretOffset, CompletionResultSet resultSet) {
            String previewValue;
            int insertOffset = caretOffset;
            if (prefix.contains(".")) {
                previewValue = value.replace(prefix, "");
            } else {
                previewValue = value;
                insertOffset = caretOffset - prefix.length();
            }
            CompletionItem item = CompletionUtilities.newCompletionItemBuilder(previewValue)
                    .iconResource(ResourceUtilities.ICON_BASE + "icons/config.png")
                    .startOffset(insertOffset)
                    .leftHtmlText(previewValue)
                    .rightHtmlText(filePath)
                    .sortPriority(1)
                    .build();
            resultSet.addItem(item);
        }
    }

    private class ViewCompletionQuery extends AsyncCompletionQuery {

        PhpModule module;

        public ViewCompletionQuery(PhpModule module) {
            this.module = module;
        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {
                FileObject sourceDir = module.getSourceDirectory();
                if (sourceDir == null) {
                    return;
                }
                int lastDotPos;

                String query = getQueryString(doc, caretOffset);
                if (query == null) {
                    return;
                }

                if (query.endsWith(".")) {
                    lastDotPos = query.length();
                } else {
                    lastDotPos = query.lastIndexOf(".");
                }
                int pathOffset;

                if (lastDotPos > 0) {
                    int dotFix = query.endsWith(".") ? 0 : 1;
                    pathOffset = caretOffset - query.length() + lastDotPos + dotFix;
                } else {
                    pathOffset = caretOffset - query.length();
                }
                FileObject viewFolder = sourceDir.getFileObject("resources/views");
                List<FileObject> childrenFiles = PathUtils.getParentChildrenFromPrefixPath(viewFolder, query);
                for (FileObject file : childrenFiles) {
                    String pathFileName = file.getName();
                    if (!file.isFolder()) {
                        pathFileName = pathFileName.replace(".blade", "");
                    }
                    addViewCompletionItem(pathFileName, file, sourceDir, pathOffset, resultSet);
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
