package com.amazon.aws.am2.appmig.glassviewer.constructs;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.*;
import java.util.regex.Pattern;

public class JavaSearchReferenceListener extends Java8ParserBaseListener {

    private final String importStmt;
    private final String importedSimpleClassName;
    private final Map<Integer, String> mapLineStmt = new HashMap<>();
    private final List<String> filteredClassVariables;
    private final List<String> matchingImports;

    public JavaSearchReferenceListener(String importStmt, List<String> filteredClassVariables, List<String> matchingImports) {
        this.importStmt = importStmt;
        List<String> classComponentsList = Arrays.asList(importStmt.split("\\."));
        importedSimpleClassName = classComponentsList.get(classComponentsList.size() - 1);

        this.filteredClassVariables = filteredClassVariables;
        this.matchingImports = matchingImports;
    }

    public Map<Integer, String> getMapLineStmt() {
        return mapLineStmt;
    }

    private boolean isDatatypePartOfImport(String datatype) {
        boolean result = false;

        if (importedSimpleClassName.equals(datatype))
            result = true;
        else if (datatype.contains(".") && importStmt.equals(datatype))
            result = true;
        else {
            for (String importName : matchingImports) {
                String importClass = importName.substring(importName.lastIndexOf(".") + 1);
                if (importClass.equals(datatype)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private boolean isStatementPartOfImport(String statement) {
        boolean result = false;

        String regex = "(.*[^a-zA-Z0-9]|)" + importedSimpleClassName + "[^a-zA-Z0-9]?.*";

        if (statement.contains(importStmt))
            result = true;
        else if (Pattern.matches(regex, statement))
            result = true;
        return result;
    }

    private boolean isStatementPartOfFilterVariable(String statement, String filterVar) {
        boolean result = false;

        String regex = "(.*[^a-zA-Z0-9]|)" + filterVar + "[^a-zA-Z0-9]?.*";

        if (Pattern.matches(regex, statement))
            result = true;
        return result;
    }

    @Override
    public void enterFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {
        Java8Parser.VariableDeclaratorListContext variableDeclaratorList = ctx.variableDeclaratorList();
        for (Java8Parser.VariableDeclaratorContext vari : variableDeclaratorList.variableDeclarator()) {
            String varName = vari.variableDeclaratorId().Identifier().toString();
            if(filteredClassVariables.contains(varName)){
                addToMap(ctx);
            }
        }
    }

    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        String returnType = ctx.methodHeader().result().getText();

        // checking for references in method return type
        if (isDatatypePartOfImport(returnType)) {
            addToMap(ctx.methodHeader());
        }

        // checking for references in method formal params' type
        if (ctx.methodHeader().methodDeclarator().formalParameterList() != null) {
            if (ctx.methodHeader().methodDeclarator().formalParameterList().formalParameters() != null) {
                ctx.methodHeader().methodDeclarator().formalParameterList().formalParameters().formalParameter()
                        .forEach(f -> {
                            if (isDatatypePartOfImport(f.start.getText())) {
                                addToMap(f);
                            }
                        });
            }
            Java8Parser.LastFormalParameterContext lastFormalParam = ctx.methodHeader().methodDeclarator().formalParameterList().lastFormalParameter();
            if (isDatatypePartOfImport(lastFormalParam.start.getText())) {
                if (!mapLineStmt.containsKey(lastFormalParam.start.getLine()))
                    addToMap(lastFormalParam);
            }
        }

        // checking for references in method local variables declaration
        List<String> filteredLocalVariables = listFilteredLocalVariables(ctx.methodBody().block().blockStatements());

        // checking for references in method statements apart from variable declaration
        Java8Parser.BlockStatementsContext blockStatements = ctx.methodBody().block().blockStatements();
        if (null != blockStatements) {
            blockStatements.blockStatement().forEach(b -> {
                if (null == b.localVariableDeclarationStatement()) {
                    if (isStatementPartOfImport(b.getText()))
                        addToMap(b);
                    else if (filteredLocalVariables.stream().anyMatch(v -> isStatementPartOfFilterVariable(b.getText(), v)))
                        addToMap(b);
                    else if (filteredClassVariables.stream().anyMatch(v -> isStatementPartOfFilterVariable(b.getText(), v)))
                        addToMap(b);
                }
            });
        }

    }

    // returns those variables whose either type OR initialization (if any) has references to import
    private List<String> listFilteredLocalVariables(Java8Parser.BlockStatementsContext blockStatements) {
        List<String> list = new ArrayList<>();
        if (null != blockStatements) {
            blockStatements.blockStatement().forEach(b -> {
                if (b.localVariableDeclarationStatement() != null) {
                    String variable = b.localVariableDeclarationStatement()
                            .localVariableDeclaration()
                            .variableDeclaratorList()
                            .variableDeclarator(0) // get first variable
                            .variableDeclaratorId().getText();
                    String type = b.localVariableDeclarationStatement().localVariableDeclaration().unannType().getText();

                    // checking for references in local variable type
                    if (isDatatypePartOfImport(type)) {
                        addToMap(b);
                        list.add(variable);
                    } else if (null != b.localVariableDeclarationStatement().localVariableDeclaration().variableDeclaratorList().variableDeclarator(0).variableInitializer() &&
                            isStatementPartOfImport(
                                    b.localVariableDeclarationStatement().localVariableDeclaration().variableDeclaratorList().variableDeclarator(0).variableInitializer().getText())) {
                        addToMap(b);
                        list.add(variable);
                    }
                }
            });
        }
        return list;
    }

    private void addToMap(ParserRuleContext ctx) {
        String text = ctx.start.getInputStream().getText(Interval.of(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        mapLineStmt.put(ctx.start.getLine(), text);
    }
}
