package com.amazon.aws.am2.appmig.glassviewer.constructs;

import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_PUBLIC;
import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_ABSTRACT;
import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_DEFAULT;
import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_FINAL;
import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.ArrayList;
import java.util.List;

public class JavaClassConstructListener extends Java8ParserBaseListener {

	private final ClassConstruct classConstruct = new ClassConstruct();
	List<String> annotations = new ArrayList<>();

	/**
	 * The way the inner classes are identified is based on the traversal of AST. If
	 * the method @code enterNormalClassDeclaration is visited more than once, then
	 * the second visit is to be considered as parsing of inner class if it is
	 * within the boundaries of the outer class
	 */
	boolean isInnerClass = false;

	public ClassConstruct getClassConstruct() {
		return classConstruct;
	}

	@Override
	public void enterPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {
		classConstruct.setPackageName(ctx.packageName().getText());
	}

	@Override
	public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
		if (!isInnerClass) {
			isInnerClass = true;
		} else {
			classConstruct.addInnerClass(ctx.Identifier().getText());
			return;
		}

		classConstruct.setName(ctx.Identifier().getText());

		if (ctx.classModifier() != null) {
			ctx.classModifier().forEach(c -> {
				if (JAVA_KEYWORD_PUBLIC.equals(c.getText())) {
					classConstruct.setPublic(true);
				} else if (JAVA_KEYWORD_ABSTRACT.equals(c.getText())) {
					classConstruct.setAbstract(true);
				} else if (JAVA_KEYWORD_DEFAULT.equals(c.getText())) {
					classConstruct.setDefault(true);
				} else if (JAVA_KEYWORD_FINAL.equals(c.getText())) {
					classConstruct.setFinal(true);
				} else if (c.annotation() != null && !c.annotation().isEmpty()) {
					annotations.add(c.annotation().getText());
					classConstruct.setAnnotations(annotations);
				}
			});
		}
	}
}
