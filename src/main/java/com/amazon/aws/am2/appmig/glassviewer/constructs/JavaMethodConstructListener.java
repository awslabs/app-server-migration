package com.amazon.aws.am2.appmig.glassviewer.constructs;

import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class JavaMethodConstructListener extends Java8ParserBaseListener {

    private final List<MethodConstruct> methodConstructList = new ArrayList<>();

    public List<MethodConstruct> getMethodConstructList() {
        return methodConstructList;
    }

    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        String name = ctx.methodHeader().methodDeclarator().Identifier().getText();
        String returnType = ctx.methodHeader().result().getText();

        // modifiers
        boolean isPublic = hasPublicModifier(ctx.methodModifier());
        boolean isProtected = hasProtectedModifier(ctx.methodModifier());
        boolean isPrivate = hasPrivateModifier(ctx.methodModifier());
        boolean isAbstract = hasAbstractModifier(ctx.methodModifier());
        boolean isStatic = hasStaticModifier(ctx.methodModifier());

        List<String> annotations = ctx.methodModifier()
                .stream()
                .filter(m -> m.annotation() != null)
                .map(m -> m.annotation().getText())
                .collect(Collectors.toList());

        List<String> parameterTypes = new ArrayList<>();
        if (ctx.methodHeader().methodDeclarator().formalParameterList() != null) {
            if (ctx.methodHeader().methodDeclarator().formalParameterList().formalParameters() != null) {
                ctx.methodHeader().methodDeclarator().formalParameterList().formalParameters().formalParameter()
                        .forEach(f -> parameterTypes.add(f.getText()));
            }
            parameterTypes.add(ctx.methodHeader().methodDeclarator().formalParameterList().lastFormalParameter().getText());
        }

        List<String> exceptionTypes = new ArrayList<>();
        if (ctx.methodHeader().throws_() != null) {
            ctx.methodHeader().throws_().exceptionTypeList().exceptionType()
                    .forEach(e -> exceptionTypes.add(e.getText()));
        }

        // local variables
        if (ctx.methodBody().block() != null && ctx.methodBody().block().blockStatements() != null) {
            List<VariableConstruct> localVariables = listLocalVariables(ctx.methodBody().block().blockStatements());

            MethodConstruct m = new MethodConstruct.MethodBuilder()
                    .name(name)
                    .returnType(returnType)
                    .annotations(annotations)
                    .parameterTypes(parameterTypes)
                    .exceptionTypes(exceptionTypes)
                    .isPublic(isPublic)
                    .isPrivate(isPrivate)
                    .isProtected(isProtected)
                    .isAbstract(isAbstract)
                    .isStatic(isStatic)
                    .startLine(ctx.start.getLine())
                    .endLine(ctx.stop.getLine())
                    .localVariablesAndTypeMap(localVariables)
                    .build();
            methodConstructList.add(m);
        }
    }

    private boolean hasPublicModifier(List<Java8Parser.MethodModifierContext> methodModifiers) {
        return hasModifier(methodModifiers, JAVA_KEYWORD_PUBLIC);
    }

    private boolean hasPrivateModifier(List<Java8Parser.MethodModifierContext> methodModifiers) {
        return hasModifier(methodModifiers, JAVA_KEYWORD_PRIVATE);
    }

    private boolean hasProtectedModifier(List<Java8Parser.MethodModifierContext> methodModifiers) {
        return hasModifier(methodModifiers, JAVA_KEYWORD_PROTECTED);
    }

    private boolean hasAbstractModifier(List<Java8Parser.MethodModifierContext> methodModifiers) {
        return hasModifier(methodModifiers, JAVA_KEYWORD_ABSTRACT);
    }

    private boolean hasStaticModifier(List<Java8Parser.MethodModifierContext> methodModifiers) {
        return hasModifier(methodModifiers, JAVA_KEYWORD_STATIC);
    }

    private boolean hasModifier(List<Java8Parser.MethodModifierContext> methodModifiers, String modifier) {
        if (methodModifiers != null) {
            for (Java8Parser.MethodModifierContext mm : methodModifiers) {
                if (modifier.equals(mm.getText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<VariableConstruct> listLocalVariables(Java8Parser.BlockStatementsContext blockStatements) {
        List<VariableConstruct> variableLst = new ArrayList<>();
        if (blockStatements != null) {
            blockStatements.blockStatement().forEach(b -> {
                if (b.localVariableDeclarationStatement() != null) {
                    String variable = b.localVariableDeclarationStatement()
                            .localVariableDeclaration()
                            .variableDeclaratorList()
                            .variableDeclarator(0) // get first variable
                            .variableDeclaratorId().getText();
                    String type = b.localVariableDeclarationStatement().localVariableDeclaration().unannType().getText();
                    VariableConstruct variableConstruct = new VariableConstruct();
                    if (b.localVariableDeclarationStatement().localVariableDeclaration().variableDeclaratorList().variableDeclarator().size() == 1) {
                        String value = (b.localVariableDeclarationStatement().localVariableDeclaration().variableDeclaratorList().variableDeclarator().get(0).children.size() == 3)
                                ? b.localVariableDeclarationStatement().localVariableDeclaration().variableDeclaratorList().variableDeclarator().get(0).children.get(2).
                                getText() : "";
                        variableConstruct.setValue(value);
                    }
                    variableConstruct.setName(variable);
                    variableConstruct.setVariableType(type);
                    variableLst.add(variableConstruct);
                }
            });
        }
        return variableLst;
    }
}
