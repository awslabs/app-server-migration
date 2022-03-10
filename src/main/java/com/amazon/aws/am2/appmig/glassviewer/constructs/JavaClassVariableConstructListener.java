package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.ArrayList;
import java.util.List;

import src.main.resources.Java8Parser;
import src.main.resources.Java8Parser.FieldModifierContext;
import src.main.resources.Java8Parser.UnannPrimitiveTypeContext;
import src.main.resources.Java8Parser.UnannReferenceTypeContext;
import src.main.resources.Java8Parser.VariableDeclaratorContext;
import src.main.resources.Java8Parser.VariableDeclaratorListContext;
import src.main.resources.Java8ParserBaseListener;

public class JavaClassVariableConstructListener extends Java8ParserBaseListener {

	private final List<ClassVariableConstruct> classVariableConstructList = new ArrayList<>();

	public List<ClassVariableConstruct> getClassVariableConstructList() {
		return classVariableConstructList;
	}

	@Override
	public void enterFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {
		ClassVariableConstruct construct = null;
		VariableDeclaratorListContext variableDeclaratorList = ctx.variableDeclaratorList();
		List<FieldModifierContext> fieldModifier = ctx.fieldModifier();
		List<String> modifiers = new ArrayList<String>();
		List<String> annotations = new ArrayList<String>();
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
		for (VariableDeclaratorContext vari : variableDeclaratorList.variableDeclarator()) {
			construct = new ClassVariableConstruct();
			construct.setName(vari.variableDeclaratorId().Identifier().toString());
			construct.setVariableAnnotations(annotations);
			construct.setVariableModifiers(modifiers);
			construct.setVariableType(variableType);
            construct.getMetaData().setStartsAt(startsAt);
            construct.getMetaData().setEndsAt(endsAt);
			classVariableConstructList.add(construct);
		}

	}
}
