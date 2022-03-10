package com.amazon.aws.am2.appmig.glassviewer;

import com.amazon.aws.am2.appmig.glassviewer.constructs.*;
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB.*;
import static com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder.OP;
import static com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder.buildQuery;

/**
 * @author agoteti
 */
public class JavaGlassViewer extends AbstractJavaGlassViewer {

    private final static Logger LOGGER = LoggerFactory.getLogger(JavaGlassViewer.class);
    private ClassConstruct classConstruct;
    private List<InterfaceConstruct> interfaceConstructs;

    @Override
    public void setBasePackage(String packageName) {
        basePackage = packageName;
    }

    @Override
    public void processClasses() {
        LOGGER.debug("processing classes");
        JavaClassConstructListener listener = new JavaClassConstructListener();
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
        classConstruct = listener.getClassConstruct();
        classConstruct.setAbsoluteFilePath(filePath);
    }

    @Override
    public void processInterfaces() {
        LOGGER.debug("processing interfaces");
        JavaInterfaceConstructListener listener = new JavaInterfaceConstructListener();
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
        interfaceConstructs = listener.getInterfaceConstructs();
        for (InterfaceConstruct construct : interfaceConstructs) {
            construct.setAbsoluteFilePath(filePath);
            construct.setPackageName(classConstruct.getPackageName());
        }
    }

    @Override
    public void processMethods() {
        LOGGER.debug("processing methods");
        JavaMethodConstructListener listener = new JavaMethodConstructListener();
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
        List<MethodConstruct> methods = listener.getMethodConstructList();
        if (classConstruct.getName() != null)
            classConstruct.setMethods(methods);
        if (!interfaceConstructs.isEmpty() && interfaceConstructs.get(0).getName() != null)
            interfaceConstructs.get(0).setMethods(methods);
    }

    @Override
    public void processImports() {
        LOGGER.debug("processing imports");
        JavaImportConstructListener listener = new JavaImportConstructListener();
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
        List<ImportConstruct> imports = listener.getImportConstructList();
        if (classConstruct.getName() != null)
            classConstruct.setImports(imports);
        if (!interfaceConstructs.isEmpty() && interfaceConstructs.get(0).getName() != null)
            interfaceConstructs.get(0).setImports(imports);
    }

    @Override
    public void processClassVariables() {
        LOGGER.debug("processing class variables");
        JavaClassVariableConstructListener listener = new JavaClassVariableConstructListener();
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
        List<ClassVariableConstruct> classVariablesList = listener.getClassVariableConstructList();
        if (classConstruct.getName() != null)
            classConstruct.setClassVariables(classVariablesList);
        if (!interfaceConstructs.isEmpty() && interfaceConstructs.get(0).getName() != null)
            interfaceConstructs.get(0).setClassVariables(classVariablesList);
    }

    @Override
    public void processStaticBlocks() {
        LOGGER.debug("processing static blocks");
        JavaStaticBlockConstructListener listener = new JavaStaticBlockConstructListener();
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    }

    @Override
    public void store() {
        if (classConstruct.getName() != null) {
            IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
            String packageId = storePackage(classConstruct.getPackageName());
            String fetch = new QueryBuilder.MatchBuilder().type(CLASS_COLLECTION).where(classConstruct.getFullClassName()).build();
            String classId = db.exists(fetch);
            if (classId == null || classId.isEmpty()) {
                LOGGER.debug("Creating the node {}", classConstruct);
                classId = db.saveNode(buildQuery(classConstruct, QueryBuilder.OP.CREATE));
                //Package -> Class relation
                db.saveNode(QueryBuilder.buildRelation(PACKAGE_CLASS_EDGE, packageId, classId));
            } else {
                LOGGER.debug("Node already exists! updating the node {} ", classConstruct);
                db.saveNode(QueryBuilder.buildQuery(classConstruct, OP.UPDATE));
            }

            List<ImportConstruct> imports = classConstruct.getImports();
            for (ImportConstruct importConstruct : imports) {
                LOGGER.debug("Creating the node import {}", importConstruct);
                if (importConstruct.getPackageName().startsWith(basePackage)) {
                    storeClassRelation(importConstruct, classId);
                } else {
                    String readImportNode = QueryBuilder.buildImportNode(importConstruct, OP.READ);
                    String importId;
                    importId = db.exists(readImportNode);
                    if (importId == null) {
                        importId = db.saveNode(QueryBuilder.buildImportNode(importConstruct, OP.CREATE));
                    }
                    db.saveNode(QueryBuilder.buildRelation(CLASS_IMPORTS_EDGE, classId, importId));
                }
            }

            List<MethodConstruct> methods = classConstruct.getMethods();
            for (MethodConstruct methodConstruct : methods) {
                LOGGER.debug("Creating the node method {}", methodConstruct);
                String readMethodNode = QueryBuilder.buildMethodNode(classConstruct, methodConstruct, QueryBuilder.OP.READ);
                String methodId = db.exists(readMethodNode);
                if (methodId == null) {
                    methodId = db.saveNode(QueryBuilder.buildMethodNode(classConstruct, methodConstruct, QueryBuilder.OP.CREATE));
                }
                String res = db.saveNode(QueryBuilder.buildRelation(CLASS_METHOD_EDGE, classId, methodId));
                if (res != null) {
                    LOGGER.debug("cannot create ClassMethodRelation");
                }
            }

            List<ClassVariableConstruct> classVariables = classConstruct.getClassVariables();
            for (ClassVariableConstruct classVariableConstruct : classVariables) {
                LOGGER.debug("Creating the node classVariable {}", classVariableConstruct);
                String readClassVariableNode = QueryBuilder.buildClassVariableNode(classConstruct, classVariableConstruct, QueryBuilder.OP.READ);
                String variableId;
                variableId = db.exists(readClassVariableNode);
                if (variableId == null) {
                    variableId = db.saveNode(QueryBuilder.buildClassVariableNode(classConstruct, classVariableConstruct, QueryBuilder.OP.CREATE));
                    if (variableId == null) {
                        LOGGER.debug("could not create ClassVariableNode");
                        continue;
                    }
                }
                String res = db.saveNode(QueryBuilder.buildRelation(CLASS_VARIABLE_EDGE, classId, variableId));
                if (res == null) {
                    LOGGER.debug("could not create ClassVariableRelation");
                }
            }
        } else {
            if (interfaceConstructs != null && !interfaceConstructs.isEmpty()) {
                InterfaceConstruct interfaceConstruct = interfaceConstructs.get(0);
                IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
                String packageId = storePackage(interfaceConstruct.getPackageName());
                String fetch = new QueryBuilder.MatchBuilder().type(CLASS_COLLECTION).where(interfaceConstruct.getFullClassName()).build();
                String interfaceId = db.exists(fetch);
                if (interfaceId == null || interfaceId.isEmpty()) {
                    LOGGER.debug("Creating the node {}", interfaceConstruct);
                    interfaceId = db.saveNode(QueryBuilder.buildQuery(interfaceConstruct, OP.CREATE));
                    //Package -> Interface relation
                    db.saveNode(QueryBuilder.buildRelation(PACKAGE_CLASS_EDGE, packageId, interfaceId));
                } else {
                    LOGGER.debug("Node already exists! updating the node {} ", interfaceConstruct);
                    db.saveNode(QueryBuilder.buildQuery(interfaceConstruct, OP.UPDATE));
                }

                List<ImportConstruct> imports = interfaceConstruct.getImports();
                for (ImportConstruct importConstruct : imports) {
                    LOGGER.debug("Creating the node import {}", importConstruct);
                    if (importConstruct.getPackageName().startsWith(basePackage)) {
                        storeClassRelation(importConstruct, interfaceId);
                    } else {
                        String readImportNode = QueryBuilder.buildImportNode(importConstruct, OP.READ);
                        String importId;
                        importId = db.exists(readImportNode);
                        if (importId == null) {
                            importId = db.saveNode(QueryBuilder.buildImportNode(importConstruct, OP.CREATE));
                        }
                        db.saveNode(QueryBuilder.buildRelation(CLASS_IMPORTS_EDGE, interfaceId, importId));
                    }
                }

                List<MethodConstruct> methods = interfaceConstruct.getMethods();
                for (MethodConstruct methodConstruct : methods) {
                    LOGGER.debug("Creating the node method {}", methodConstruct);
                    String readMethodNode = QueryBuilder.buildMethodNode(interfaceConstruct, methodConstruct, QueryBuilder.OP.READ);
                    String methodId = db.exists(readMethodNode);
                    if (methodId == null) {
                        methodId = db.saveNode(QueryBuilder.buildMethodNode(interfaceConstruct, methodConstruct, QueryBuilder.OP.CREATE));
                    }
                    String res = db.saveNode(QueryBuilder.buildRelation(CLASS_METHOD_EDGE, interfaceId, methodId));
                    if (res != null) {
                        LOGGER.debug("cannot create ClassMethodRelation");
                    }
                }

                List<ClassVariableConstruct> classVariables = interfaceConstruct.getClassVariables();
                for (ClassVariableConstruct classVariableConstruct : classVariables) {
                    LOGGER.debug("Creating the node classVariable {}", classVariableConstruct);
                    String readClassVariableNode = QueryBuilder.buildClassVariableNode(interfaceConstruct, classVariableConstruct, QueryBuilder.OP.READ);
                    String variableId;
                    variableId = db.exists(readClassVariableNode);
                    if (variableId == null) {
                        variableId = db.saveNode(QueryBuilder.buildClassVariableNode(interfaceConstruct, classVariableConstruct, QueryBuilder.OP.CREATE));
                        if (variableId == null) {
                            LOGGER.debug("could not create ClassVariableNode");
                            continue;
                        }
                    }
                    String res = db.saveNode(QueryBuilder.buildRelation(CLASS_VARIABLE_EDGE, interfaceId, variableId));
                    if (res == null) {
                        LOGGER.debug("could not create ClassVariableRelation");
                    }
                }
            }
        }
    }

    public void storeClassRelation(ImportConstruct importConstruct, String classId) {
        ClassConstruct cc = new ClassConstruct();
        cc.setName(importConstruct.getClassName());
        cc.setPackageName(importConstruct.getPackageName());

        IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
        String fetch = new QueryBuilder.MatchBuilder().type(CLASS_COLLECTION).where(cc.getFullClassName()).build();
        String newClassId = db.exists(fetch);
        if (newClassId == null || newClassId.isEmpty()) {
            LOGGER.debug("Creating the node {}", cc);
            String packageId = storePackage(cc.getPackageName());
            newClassId = db.saveNode(buildQuery(cc, QueryBuilder.OP.CREATE));
            db.saveNode(QueryBuilder.buildRelation(PACKAGE_CLASS_EDGE, packageId, newClassId));
        }

        db.saveNode(QueryBuilder.buildRelation(CLASS_CLASS_EDGE, classId, newClassId));
    }

    private String storePackage(String packageName) {
        IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
        PackageConstruct pc = new PackageConstruct();
        int index = packageName.lastIndexOf('.');
        pc.setPackageName(packageName.substring(index > -1 ? index + 1 : 0));
        pc.setFullPackageName(packageName);
        String readImportNode = QueryBuilder.buildPackageNode(pc, OP.READ);
        String packageId;
        packageId = db.exists(readImportNode);
        if (packageId == null) {
            packageId = db.saveNode(QueryBuilder.buildPackageNode(pc, OP.CREATE));
            if (index > -1) {
                String rootPackageId = storePackage(packageName.substring(0, index));
                //TODO create pkg relation
                db.saveNode(QueryBuilder.buildRelation(PACKAGE_PACKAGE_EDGE, rootPackageId, packageId));
            }
        }
        return packageId;
    }

    /*
     * Checks for -
     * 1. Import statement in DB
     * 2. Class Variables in DB
     * 3. Methods declaration/local variables/statements/filteredClassVariables
     * */
    @Override
    public Map<Integer, String> searchReferences(String importStmt) throws Exception {
        Map<Integer, String> mapLineStatement = new HashMap<>();

        IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();

        if (classConstruct.getName() != null) {
            // check for import in DB for the specific Class
            List<String> matchingImports = db.existsRelation(QueryBuilder.getMatchingClassImport(classConstruct, importStmt));
            if (matchingImports == null || matchingImports.isEmpty()) {
                LOGGER.debug("Class \"{}\" does not contain import statement \"{}\"", classConstruct.getFullClassName(), importStmt);
                return mapLineStatement;
            }
            LOGGER.debug("Class \"{}\" contains import statement \"{}\" Processing further....\n", classConstruct.getFullClassName(), importStmt);
            System.out.printf(("Class \"%s\" contains import statement \"%s\" Processing..\n"), classConstruct.getFullClassName(), importStmt);

            // then check for class variables in DB
            List<String> result = db.read(QueryBuilder.getMatchingClassVariableImport(classConstruct, matchingImports));
            List<ClassVariableConstruct> classVariableConstructs = new ArrayList<>();
            result.forEach(str -> {
                JSONObject json = new JSONObject(str);
                ClassVariableConstruct cvc = new ClassVariableConstruct();
                cvc.setName(json.getString("name"));
                cvc.setVariableType(json.getString("type"));
                cvc.setVariableAnnotations(Arrays.asList(json.getString("annotations").replaceAll("^\\[|]$", "").split(",")));
                cvc.setVariableModifiers(Arrays.asList(json.getString("modifiers").replaceAll("\\[|]", "").split(",")));
                cvc.getMetaData().setStartsAt(json.getInt("startsAt"));
                cvc.getMetaData().setEndsAt(json.getInt("endsAt"));
                classVariableConstructs.add(cvc);
                mapLineStatement.put(cvc.getMetaData().getStartsAt(), String.join("", cvc.getVariableModifiers()) + " " + cvc.getVariableType() + " " + cvc.getName());
            });

            List<String> filteredClassVariables = classVariableConstructs.stream().map(ClassVariableConstruct::getName).collect(Collectors.toList());

            // then check for methods declaration/variable/statements/filteredClassVariables
            LOGGER.debug("processing methods");
            JavaMethodSearchReferenceListener listener = new JavaMethodSearchReferenceListener(importStmt, filteredClassVariables);
            ParseTreeWalker.DEFAULT.walk(listener, parseTree);

            Map<Integer, String> mapLineStmtFromMethods = listener.getMapLineStmt();
            mapLineStatement.putAll(mapLineStmtFromMethods);
        }

        if (interfaceConstructs != null && !interfaceConstructs.isEmpty()) {
            // check for import in DB for the specific Class
            InterfaceConstruct interfaceConstruct = interfaceConstructs.get(0);
            List<String> matchingImports = db.existsRelation(QueryBuilder.getMatchingClassImport(interfaceConstruct, importStmt));
            if (matchingImports == null || matchingImports.isEmpty()) {
                LOGGER.debug("Interface \"{}\" does not contain import statement \"{}\"", interfaceConstruct.getFullClassName(), importStmt);
                return mapLineStatement;
            }
            LOGGER.debug("Interface \"{}\" contains import statement \"{}\" Processing further....\n", interfaceConstruct.getFullClassName(), importStmt);
            System.out.printf(("Interface \"%s\" contains import statement \"%s\" Processing..\n"), interfaceConstruct.getFullClassName(), importStmt);

            // then check for class variables in DB
            List<String> result = db.read(QueryBuilder.getMatchingClassVariableImport(interfaceConstruct, matchingImports));
            List<ClassVariableConstruct> classVariableConstructs = new ArrayList<>();
            result.forEach(str -> {
                JSONObject json = new JSONObject(str);
                ClassVariableConstruct cvc = new ClassVariableConstruct();
                cvc.setName(json.getString("name"));
                cvc.setVariableType(json.getString("type"));
                cvc.setVariableAnnotations(Arrays.asList(json.getString("annotations").replaceAll("^\\[|]$", "").split(",")));
                cvc.setVariableModifiers(Arrays.asList(json.getString("modifiers").replaceAll("\\[|]", "").split(",")));
                cvc.getMetaData().setStartsAt(json.getInt("startsAt"));
                cvc.getMetaData().setEndsAt(json.getInt("endsAt"));
                classVariableConstructs.add(cvc);
                mapLineStatement.put(cvc.getMetaData().getStartsAt(), String.join("", cvc.getVariableModifiers()) + " " + cvc.getVariableType() + " " + cvc.getName());
            });

            List<String> filteredClassVariables = classVariableConstructs.stream().map(ClassVariableConstruct::getName).collect(Collectors.toList());

            // then check for methods declaration/variable/statements/filteredClassVariables
            LOGGER.debug("processing methods");
            JavaMethodSearchReferenceListener listener = new JavaMethodSearchReferenceListener(importStmt, filteredClassVariables);
            ParseTreeWalker.DEFAULT.walk(listener, parseTree);

            Map<Integer, String> mapLineStmtFromMethods = listener.getMapLineStmt();
            mapLineStatement.putAll(mapLineStmtFromMethods);
        }
        return mapLineStatement;
    }
}
