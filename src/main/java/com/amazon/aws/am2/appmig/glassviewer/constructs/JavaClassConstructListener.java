package com.amazon.aws.am2.appmig.glassviewer.constructs;

import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.ArrayList;
import java.util.List;

public class JavaClassConstructListener extends Java8ParserBaseListener {

    private ClassConstruct classConstruct = new ClassConstruct();
    List<String> annotations = new ArrayList<String>();

    // It is harder to identify the inner class since the structure of outer class and inner class is same.
    // One way to dertermine is ->
    // while traversing the AST the outerClass is traversed first and then the inner classes.
    boolean isInnerClass = false;

    public ClassConstruct getClassConstruct() {
        return classConstruct;
    }

    @Override public void enterPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {
        classConstruct.setPackageName(ctx.packageName().getText());
    }

    @Override public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
        if(!isInnerClass) {
            isInnerClass = true;
        } else {
            classConstruct.addInnerClass(ctx.Identifier().getText());
            return;
        }

        classConstruct.setName(ctx.Identifier().getText());

        if(ctx.classModifier() != null) {
            ctx.classModifier().forEach(c -> {
                if ("public".equals(c.getText())) {
                    classConstruct.setPublic(true);
                } else if ("abstract".equals(c.getText())) {
                    classConstruct.setAbstract(true);
                } else if ("default".equals(c.getText())) {
                    classConstruct.setDefault(true);
                } else if ("final".equals(c.getText())) {
                    classConstruct.setFinal(true);
                } else if (c.annotation() != null && !c.annotation().isEmpty()) {
                    annotations.add(c.annotation().getText());
                    classConstruct.setAnnotations(annotations);
                }
            });
        }
    }
}
