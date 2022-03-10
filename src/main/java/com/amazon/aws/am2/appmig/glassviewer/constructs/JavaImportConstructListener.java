package com.amazon.aws.am2.appmig.glassviewer.constructs;

import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.ArrayList;
import java.util.List;

public class JavaImportConstructListener extends Java8ParserBaseListener {

    private List<ImportConstruct> importConstructList = new ArrayList<>();

    public List<ImportConstruct> getImportConstructList() {
        return importConstructList;
    }

    @Override public void enterSingleTypeImportDeclaration(Java8Parser.SingleTypeImportDeclarationContext ctx) {
        ImportConstruct ic = new ImportConstruct()
            .setClassName(ctx.typeName().Identifier().getText())
            .setPackageName(ctx.typeName().packageOrTypeName() != null ?
                    ctx.typeName().packageOrTypeName().getText() : "")
            .setStartAt(ctx.start.getLine());

        importConstructList.add(ic);
    }
}
