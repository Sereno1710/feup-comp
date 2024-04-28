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

        List<String> imports = new ArrayList<>();
        List<JmmNode> importDecls = root.getChildren(IMPORT_DECL);
        importDecls.forEach(importDecl -> {
            imports.add(String.join(".", importDecl.getObjectAsList("value", String.class)));
        });

        JmmNode classDecl = root.getChild(root.getNumChildren() - 1);

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
                .forEach(method -> {
                    Type type = new Type(method.getChild(0).get("name"), method.getChild(0).hasAttribute("array"));
                    if (method.getChild(0).hasAttribute("vargs")) type.putObject("vargs", true);
                    map.put(method.get("name"), type);
                });
        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    List<Symbol> symbols = new ArrayList<>();
                    if (method.getChildren().size() > 1){
                        if (method.getChild(1).getKind().equals("Params")) {
                            JmmNode params = method.getChild(1);

                            for (var param : params.getChildren()) {
                                Type type = new Type(param.getChild(0).get("name"), param.hasAttribute("array"));
                                if (param.getChild(0).hasAttribute("vargs")) type.putObject("vargs", true);
                                symbols.add(new Symbol(type, param.get("name")));
                            }
                        }
                        else symbols = Collections.emptyList();
                        map.put(method.get("name"),symbols);
                    }
                    else if (method.getChildren().size() == 1){
                        if (method.getChild(0).getKind().equals("Params")) {
                            JmmNode params = method.getChild(1);

                            for (var param : params.getChildren()) {
                                Type type = new Type(param.getChild(0).get("name"), param.hasAttribute("array"));
                                if (param.getChild(0).hasAttribute("vargs")) type.putObject("vargs", true);
                                symbols.add(new Symbol(type, param.get("name")));
                            }
                        }
                        else symbols = Collections.emptyList();
                        map.put(method.get("name"),symbols);
                    }
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


        return methods;
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> {
                    String typeName = varDecl.getChild(0).get("name"); // Assuming type is the first child
                    boolean isArray = varDecl.getChild(0).hasAttribute("array");
                    Type type = new Type(typeName, isArray);
                    if (varDecl.getChild(0).hasAttribute("vargs")) type.putObject("vargs", true);
                    String fieldName = varDecl.get("name");
                    return new Symbol(type, fieldName);
                })
                .collect(Collectors.toList());
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
                    Type type = new Type(typeName, isArray);
                    if (varDecl.getChild(0).hasAttribute("vargs")) type.putObject("vargs", true);
                    String fieldName = varDecl.get("name");
                    return new Symbol(type, fieldName);
                })
                .collect(Collectors.toList());
    }



}
