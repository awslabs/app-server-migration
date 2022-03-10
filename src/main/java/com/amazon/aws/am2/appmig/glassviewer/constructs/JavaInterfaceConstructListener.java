package com.amazon.aws.am2.appmig.glassviewer.constructs;

import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JavaInterfaceConstructListener extends Java8ParserBaseListener {

    private List<InterfaceConstruct> interfaceConstructs = new ArrayList<>();

    public List<InterfaceConstruct> getInterfaceConstructs() {
        return interfaceConstructs;
    }

    @Override public void enterNormalInterfaceDeclaration(Java8Parser.NormalInterfaceDeclarationContext ctx) {
        String name = ctx.Identifier().getText();

        List<String> extendsInterfaces = null;
        if(ctx.extendsInterfaces() != null) {
            extendsInterfaces = ctx.extendsInterfaces().interfaceTypeList().interfaceType()
                    .stream()
                    .map(Java8Parser.InterfaceTypeContext::getText)
                    .collect(Collectors.toList());
        }

        interfaceConstructs.add(new InterfaceConstruct.InterfaceBuilder()
                .name(name)
                .extendsInterfaces(extendsInterfaces)
                .build());
    }
}
