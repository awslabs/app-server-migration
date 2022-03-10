package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.ArrayList;
import java.util.List;

import src.main.resources.Java8Parser;
import src.main.resources.Java8ParserBaseListener;

public class JavaStaticBlockConstructListener extends Java8ParserBaseListener {

    private List<ConstructStaticBlockMetaData> staticBlockMetaDataList = new ArrayList<>();

    public List<ConstructStaticBlockMetaData> getStaticBlockMetadataList() {
        return staticBlockMetaDataList;
    }

    @Override public void enterStaticInitializer(Java8Parser.StaticInitializerContext ctx) {
    	ConstructStaticBlockMetaData construct = new ConstructStaticBlockMetaData();
        construct.setStartsAt(ctx.block().getStart().getLine());
        construct.setEndsAt(ctx.block().getStop().getLine());
        staticBlockMetaDataList.add(construct);
    }
}
