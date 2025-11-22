/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.php.laravel.editor.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.netbeans.modules.php.laravel.editor.model.RoutesModel;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileObject;

public class RoutesConfigParser extends FileChangeAdapter {

    private static final Logger LOGGER = Logger.getLogger(RoutesConfigParser.class.getName());
    public static final String ROUTES_DIR = "routes";  // NOI18N
    private final Map<FileObject, RoutesModel> collection = new HashMap<>();

    public void parseRoutes(FileObject sourceDir) {
        if (sourceDir == null) {
            // broken project
            return;
        }

        FileObject routesDir = sourceDir.getFileObject(ROUTES_DIR);

        if (routesDir == null) {
            return;
        }

        Enumeration<? extends FileObject> children = routesDir.getChildren(true);

        while (children.hasMoreElements()) {
            FileObject file = children.nextElement();
            if (file.isFolder()) {
                continue;
            }
            parseRoutesFile(file, collection);
        }
    }

    private void parseRoutesFile(FileObject file, Map<FileObject, RoutesModel> collection) {
        try {
            ParserManager.parse(Collections.singleton(Source.create(file)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    PHPParseResult parseResult = (PHPParseResult) resultIterator.getParserResult();
                    if (parseResult != null && parseResult.getProgram() != null) {
                        RoutesModel routeModel = new RoutesModel();
                        parseResult.getProgram().accept(new RoutesModelVisitor(routeModel));
                        collection.put(file, routeModel);
                    }

                }

            });
        } catch (ParseException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }

    public Set<String> getRoutesLabel() {
        Set<String> routesLabel = new HashSet<>();
        for (RoutesModel routeModels : collection.values()) {
            for (String routelabel : routeModels.getRouteNames().keySet()) {
                routesLabel.add(routelabel);
            }
        }

        return routesLabel;
    }

    public Map<FileObject, RoutesModel> getRoutesCollection() {
        return collection;
    }

    private class RoutesModelVisitor extends DefaultVisitor {

        private final String[] visitedMethods = new String[]{"name"}; // NOI18N
        private final RoutesModel model;

        public RoutesModelVisitor(RoutesModel model) {
            this.model = model;
        }

        @Override
        public void scan(ASTNode node) {
            if (node != null) {
                super.scan(node);
            }
        }

        @Override
        public void visit(FunctionInvocation node) {
            String functionName = node.getFunctionName().toString();

            if (!Arrays.stream(visitedMethods).anyMatch(functionName::equals)) {
                //in case of callback config
                super.visit(node);
                return;
            }

            List<Expression> parameters = node.getParameters();
            Iterator<?> iter = parameters.iterator();
            Expression directiveName = (Expression) iter.next();
            if (directiveName != null && directiveName instanceof Scalar) {
                Scalar name = (Scalar) directiveName;
                String escapedRouteName = name.getStringValue().replaceAll("^[\"|\']|[\"|[\']]$", ""); // NOI18N
                OffsetRange range = new OffsetRange(directiveName.getStartOffset(), directiveName.getEndOffset());
                model.addRouteName(escapedRouteName, range);
            }
        }
    }
}
