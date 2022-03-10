package com.amazon.aws.am2.appmig.glassviewer.constructs;

import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JavaMethodSearchReferenceListener extends Java8ParserBaseListener {

	private final String importStmt;
	private final String importedSimpleClassName;
	private final Map<Integer, String> mapLineStmt = new HashMap<>();
	private final List<String> filteredClassVariables;

	public JavaMethodSearchReferenceListener(String importStmt, List<String> filteredClassVariables) {
		this.importStmt = importStmt;
		List<String> classComponentsList = Arrays.asList(importStmt.split("\\."));
		importedSimpleClassName = classComponentsList.get(classComponentsList.size() - 1);

		this.filteredClassVariables = filteredClassVariables;
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

	@Override public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
		String name = ctx.methodHeader().methodDeclarator().Identifier().getText();
		String returnType = ctx.methodHeader().result().getText();

		// checking for references in method return type
		if (isDatatypePartOfImport(returnType)) {
			mapLineStmt.put(ctx.methodHeader().start.getLine(), ctx.methodHeader().getText());
		}

		// checking for references in method formal params' type
		if (ctx.methodHeader().methodDeclarator().formalParameterList() != null) {
			if (ctx.methodHeader().methodDeclarator().formalParameterList().formalParameters() != null) {
				ctx.methodHeader().methodDeclarator().formalParameterList().formalParameters().formalParameter()
					.forEach(f -> {
						if (isDatatypePartOfImport(f.start.getText())) {
							mapLineStmt.put(f.start.getLine(), f.parent.getText());
						}
					});
			}
			Java8Parser.LastFormalParameterContext lastFormalParam = ctx.methodHeader().methodDeclarator().formalParameterList().lastFormalParameter();
			if (isDatatypePartOfImport(lastFormalParam.start.getText())) {
				if (!mapLineStmt.containsKey(lastFormalParam.start.getLine()))
					mapLineStmt.put(lastFormalParam.start.getLine(), lastFormalParam.getText());
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
						mapLineStmt.put(b.start.getLine(), b.getText());
					else if (filteredLocalVariables.stream().anyMatch(v -> isStatementPartOfFilterVariable(b.getText(), v)))
						mapLineStmt.put(b.start.getLine(), b.getText());
					else if (filteredClassVariables.stream().anyMatch(v -> isStatementPartOfFilterVariable(b.getText(), v)))
						mapLineStmt.put(b.start.getLine(), b.getText());
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
						mapLineStmt.put(b.start.getLine(), b.localVariableDeclarationStatement().getText());
						list.add(variable);
					} else if (null != b.localVariableDeclarationStatement().localVariableDeclaration().variableDeclaratorList().variableDeclarator(0).variableInitializer() &&
						isStatementPartOfImport(
							b.localVariableDeclarationStatement().localVariableDeclaration().variableDeclaratorList().variableDeclarator(0).variableInitializer().getText()))
					{
						mapLineStmt.put(b.start.getLine(), b.localVariableDeclarationStatement().getText());
						list.add(variable);
					}
				}
			});
		}
		return list;
	}
}
