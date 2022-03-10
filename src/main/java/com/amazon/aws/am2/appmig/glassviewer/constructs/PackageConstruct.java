package com.amazon.aws.am2.appmig.glassviewer.constructs;

public class PackageConstruct implements JavaConstruct {

    private String packageName;
    private String fullPackageName;

    public String getPackageName() {
        return packageName;
    }

    public PackageConstruct setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getFullPackageName() {
        return fullPackageName;
    }

    public void setFullPackageName(String fullPackageName) {
        this.fullPackageName = fullPackageName;
    }

    @Override
    public JConstructType getType() {
        return JConstructType.IMPORT;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public ConstructMetaData getMetaData() {
        return null;
    }

    @Override
    public String toString() {
        return "ImportConstruct{" +
                "packageName='" + packageName + '\'' +
                ", fullPackageName='" + fullPackageName + '\'' +
                '}';
    }
}
