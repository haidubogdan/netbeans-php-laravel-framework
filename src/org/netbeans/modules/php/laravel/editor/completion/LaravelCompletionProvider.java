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
import org.netbeans.modules.php.laravel.editor.completion.LaravelCompletionItem.ConfigPath;
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

    public static String[] QUERY_METHODS = new String[]{"config", "view"};

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        PhpModule module = ProjectUtils.getPhpModule(component.getDocument());

        if (module == null) {
            return null;
        }

        if (!ProjectUtils.isInLaravelModule(module)) {
            return null;
        }

        int offset = component.getCaretPosition();
        BaseDocument baseDoc = (BaseDocument) component.getDocument();

        int lineStart = LineDocumentUtils.getLineStart(baseDoc, offset);
        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(component.getDocument(), offset);

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
            AsyncCompletionQuery completionQuery;

            switch (methodName) {
                case "config":
                    completionQuery = new ConfigurationCompletionQuery(reference, module);
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

    private class ConfigurationCompletionQuery extends AsyncCompletionQuery {

        String query;
        PhpModule module;

        public ConfigurationCompletionQuery(String query, PhpModule module) {
            this.query = query;
            this.module = module;
        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            long startTime = System.currentTimeMillis();

            try {
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
                        if (!entry.getKey().startsWith(queryConfigNamespace[0])) {
                            continue;
                        }
                        FileObject configFile = confFileAlias.get(entry.getKey());
                        String configPath = "";
                        if (configFile != null) {
                            configPath = "config/" + configFile.getNameExt();
                        }
                        if (queryConfigNamespace.length == 1 && query.endsWith(".") && !entry.getValue().isEmpty()) {
                            for (Map.Entry<String, List<String>> namespace : entry.getValue().entrySet()) {
                                String rootKey = entry.getKey() + "." + namespace.getKey();
                                addConfigCompletionItem(query, rootKey, configPath, caretOffset, resultSet);
                            }
                            //should be only a unique namespace
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
    }

    private void addConfigCompletionItem(String prefix, String value, String filePath, int caretOffset, CompletionResultSet resultSet) {

        int strOffset = value.indexOf(prefix);

        String completionValue = value.replace(prefix, "");
        int insertOffset = caretOffset;
        CompletionItem item = CompletionUtilities.newCompletionItemBuilder(completionValue)
                .iconResource(ResourceUtilities.ICON_BASE + "icons/config.png")
                .startOffset(insertOffset)
                .leftHtmlText(completionValue)
                .rightHtmlText(filePath)
                .sortPriority(1)
                .build();
        resultSet.addItem(item);
    }

}
