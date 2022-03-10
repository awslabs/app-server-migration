package com.amazon.aws.am2.appmig.glassviewer;

import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        IJavaGlassViewer viewer = new JavaGlassViewer();
        viewer.setBasePackage("com.amazon.aws.am2.glassviewer");
        viewer.view("src/main/java/com/amazon/aws/am2/glassviewer/JavaGlassViewer.java");
        Map<Integer, String> map = viewer.searchReferences("org.slf4j");
        map.forEach((x, y) -> System.out.println(x + "\t" + y));
        viewer.cleanup();
    }
}
