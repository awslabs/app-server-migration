package com.amazon.aws.am2.appmig.glassviewer.constructs;

public class AnnotationConstruct implements JavaConstruct {

    private String name;
    private final ConstructMetaData metadata = new ConstructMetaData();

    @Override
    public JConstructType getType() {
        return JConstructType.ANNOTATION;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ConstructMetaData getMetaData() {
        return metadata;
    }

    @Override
    public String toString() {
        return "AnnotationConstruct [name=" + name + ", metadata=" + metadata + "]";
    }
}
