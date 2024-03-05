package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {
        var firstChild = root.getJmmChild(0);
        List<String> imports = new ArrayList<>();
        JmmNode classDecl;
        if (Kind.IMPORT_DECL.check(firstChild)) {
            int importCount = 0;
            JmmNode child;
            while (Kind.IMPORT_DECL.check(child = root.getJmmChild(importCount++))) {
                imports.add(child.getObjectAsList("value", String.class).toString());
            }
            classDecl = child;
        }
        else {
            classDecl = firstChild;
            SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        }

        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var sup = buildSuper(classDecl);
        var fields = buildFields(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, imports, sup, fields);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getIntTypeName(), false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();
/*
        List<JmmNode> methods = classDecl.getChildren(METHOD_DECL);
        for (JmmNode jmmNode : methods) {
            List<JmmNode> params = jmmNode.getChildren("param");
            List<Symbol> symbols = new ArrayList<>();
            for (JmmNode param : params) {
                var type = param.getChild(0);
                var name = param.getChild(1).get("name");
                symbols.add(new Symbol(new Type(type.get("name"), type.hasAttribute("array")), name));
            }
            map.put(jmmNode.get("name"), symbols);
        }
*/

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    JmmNode params = method.getChild(1);
                    List<Symbol> symbols = new ArrayList<>();
                    for (var param : params.getChildren()) {
                        symbols.add(new Symbol(new Type(param.getChild(0).get("name"), param.hasAttribute("array")), param.get("name")));
                    }
                    map.put(method.get("name"),symbols);
                });

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();

        List<String> main = classDecl.getChildren(MAIN_METHOD).stream()
                .map(mainMethod -> mainMethod.get("name"))  // Corrected the method name retrieval
                .toList();

        List<String> list = new ArrayList<>(methods);
        list.addAll(main);

        return list;
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

    private static String buildSuper(JmmNode classDecl) {
        if (classDecl.hasAttribute("sup")){
            return classDecl.get("sup");
        }
        else return "";
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> {
                    String typeName = varDecl.getChild(0).get("name"); // Assuming type is the first child
                    boolean isArray = varDecl.getChild(0).hasAttribute("array");
                    String fieldName = varDecl.get("name");
                    return new Symbol(new Type(typeName, isArray), fieldName);
                })
                .collect(Collectors.toList());
    }



}
