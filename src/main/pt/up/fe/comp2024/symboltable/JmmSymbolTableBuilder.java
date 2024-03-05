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
                .forEach(method -> map.put(method.get("name"), new Type(method.getChild(0).get("name"), method.getChild(0).hasAttribute("array"))));
        classDecl.getChildren(MAIN_METHOD).stream().forEach(main -> map.put(main.get("name"), new Type(main.get("ret"), main.hasAttribute("array"))));
        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    List<Symbol> symbols = new ArrayList<>();
                    if (method.getChild(1).getKind().equals("Params")) {
                        JmmNode params = method.getChild(1);

                        for (var param : params.getChildren()) {
                            symbols.add(new Symbol(new Type(param.getChild(0).get("name"), param.hasAttribute("array")), param.get("name")));
                        }
                    }
                    else symbols = Collections.emptyList();
                    map.put(method.get("name"),symbols);
                });

        classDecl.getChildren(MAIN_METHOD).stream().forEach(
                main -> {
                        List<Symbol> m_symbols = new ArrayList<>();
                        JmmNode m_params = main.getChild(0);
                        for (var param: m_params.getChildren()){
                            m_symbols.add(new Symbol(new Type(param.get("name"),param.hasAttribute("array")),"main"));
                        }
                        map.put(main.get("name"),m_symbols);
                }
        );
        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));
        classDecl.getChildren(MAIN_METHOD).stream().forEach(mainMethod -> map.put(mainMethod.get("name"), getLocalsList(mainMethod)));
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
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> {
                    String typeName = varDecl.getChild(0).get("name"); // Assuming type is the first child
                    boolean isArray = varDecl.getChild(0).hasAttribute("array");
                    String fieldName = varDecl.get("name");
                    return new Symbol(new Type(typeName, isArray), fieldName);
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
                    String fieldName = varDecl.get("name");
                    return new Symbol(new Type(typeName, isArray), fieldName);
                })
                .collect(Collectors.toList());
    }



}
