package com.amazon.aws.am2.appmig.glassviewer.constructs;

import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_PUBLIC;
import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_ABSTRACT;
import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_DEFAULT;
import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_KEYWORD_FINAL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.ArrayList;
import java.util.List;

public class JavaClassConstructListener extends Java8ParserBaseListener {

	private final static Logger LOGGER = LoggerFactory.getLogger(JavaClassConstructListener.class);
	private final ClassConstruct classConstruct = new ClassConstruct();
	private final List<AnnotationConstruct> annotations = new ArrayList<>();

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
		if (ctx.Identifier() == null) {
			LOGGER.error("Unable to process normal class declaration {}", ctx);
			return;
		}
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
					AnnotationConstruct annotation = new AnnotationConstruct();
					annotation.getMetaData().setStartsAt(c.annotation().getStart().getLine());
					annotation.getMetaData().setEndsAt(c.annotation().getStop().getLine());
					annotation.setName(c.annotation().getText().trim());
					annotations.add(annotation);
					classConstruct.setAnnotations(annotations);
				}
			});
		}
	}

	@Override
	public void exitClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
		classConstruct.setLOC(ctx.stop.getLine());
	}

	@Override
	public void enterAnnotationTypeDeclaration(Java8Parser.AnnotationTypeDeclarationContext ctx) {
		ctx.getText();
	}
}
