package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;
import src.main.resources.Java8Parser;
import src.main.resources.Java8Parser.FieldModifierContext;
import src.main.resources.Java8Parser.UnannPrimitiveTypeContext;
import src.main.resources.Java8Parser.UnannReferenceTypeContext;
import src.main.resources.Java8Parser.VariableDeclaratorContext;
import src.main.resources.Java8Parser.VariableDeclaratorListContext;
import src.main.resources.Java8ParserBaseListener;

public class JavaClassVariableConstructListener extends Java8ParserBaseListener {

    private final List<VariableConstruct> variableConstructList = new ArrayList<>();

    public List<VariableConstruct> getClassVariableConstructList() {
        return variableConstructList;
    }

    @Override
    public void enterFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {
        VariableConstruct construct;
        VariableDeclaratorListContext variableDeclaratorList = ctx.variableDeclaratorList();
        List<FieldModifierContext> fieldModifier = ctx.fieldModifier();
        List<String> modifiers = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        String variableType = null;
        for (FieldModifierContext fieldModifierContext : fieldModifier) {
            modifiers.add(fieldModifierContext.getText());
            if (fieldModifierContext.annotation() != null) {
                annotations.add(fieldModifierContext.annotation().getText());
            }
        }
        UnannPrimitiveTypeContext unannPrimitiveType = ctx.unannType().unannPrimitiveType();
        UnannReferenceTypeContext unannReferenceType = ctx.unannType().unannReferenceType();
        int startsAt = ctx.start.getLine();
        int endsAt = ctx.stop.getLine();
        if (unannPrimitiveType != null) {
            variableType = unannPrimitiveType.getText();
        } else if (unannReferenceType != null) {
            variableType = unannReferenceType.getText();
        }
        for (VariableDeclaratorContext variable : variableDeclaratorList.variableDeclarator()) {
            construct = new VariableConstruct();
            if (variable.variableDeclaratorId() == null) {
                continue;
            }
            TerminalNode identifier = variable.variableDeclaratorId().Identifier();
            // The null check is required in some corner cases, where the assignment of the variable cannot be processed.
            // Like the assignment is in a different language which the encoding does not support
            construct.setName((identifier != null) ? identifier.getText() : "");
            if (variable.children.size() == 3) {
                construct.setValue(variable.children.get(2).getText());
            }
            construct.setVariableAnnotations(annotations);
            construct.setVariableModifiers(modifiers);
            construct.setVariableType(variableType);
            construct.getMetaData().setStartsAt(startsAt);
            construct.getMetaData().setEndsAt(endsAt);
            variableConstructList.add(construct);
        }
    }
}
