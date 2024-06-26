package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Class extends AnalysisVisitor {

    private String currentMethod;
    private JmmNode currentMethodNode;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_CHAIN_EXPR, this::visitClassChainExpr);
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {
        List<JmmNode> methods = classDecl.getDescendants(Kind.METHOD_DECL);
        Map<String, String> methodMap = new HashMap<>();
        for (JmmNode method : methods) {
            // if there's a duplicate method
            if (methodMap.containsKey(method.get("name"))) {
                // Create error report
                var message = String.format("Method '%s' was already defined.", method.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );

                return null;
            }
            // if main has the wrong arguments
            if (method.get("name").equals("main") &&
                    !(method.getChild(1).getChild(0).get("name")
                            .equals(TypeUtils.getStringTypeName()) &&
                            method.getChild(1).getChild(0).hasAttribute("array"))) {
                // Create error report
                var message = "Method main has the wrong arguments.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );

                return null;
            }
            // if method that does not return void has no return
            if (!table.getReturnType(method.get("name"))
                    .equals(new Type("void", false)) &&
                    method.getChildren(Kind.RETURN_STMT).isEmpty()) {
                // Create error report
                var message = String.format("Method '%s' has no return.", method.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );

                return null;
            }

            methodMap.put(method.get("name"), "a");
        }

        // if class is imported more than once
        Map<String, String> importMap = new HashMap<>();
        for (String imp : table.getImports()) {
            for (String key : importMap.keySet()) {
                if (imp.contains(key) || key.contains(imp)) {
                    // Create error report
                    var message = String.format("Class '%s' is imported more than once.", imp);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(classDecl.getParent()),
                            NodeUtils.getColumn(classDecl.getParent()),
                            message,
                            null)
                    );

                    return null;
                }
            }
            importMap.put(imp, "a");
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        currentMethodNode = method;

        // check duplicate fields
        List<JmmNode> fieldNodes =  method.getParent().getChildren(Kind.VAR_DECL);
        List<Symbol> fields = table.getFields();
        Map<String, String> fieldMap = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getType().hasAttribute("vargs")) {
                // Create error report
                var message = String.format("Field '%s' cannot be varargs.", fields.get(i).getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(fieldNodes.get(i)),
                        NodeUtils.getColumn(fieldNodes.get(i)),
                        message,
                        null)
                );

                return null;
            }
            if (fieldMap.containsKey(fields.get(i).getName())) {
                // Create error report
                var message = String.format("Field '%s' was already defined.", fields.get(i).getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(fieldNodes.get(i)),
                        NodeUtils.getColumn(fieldNodes.get(i)),
                        message,
                        null)
                );

                return null;
            }
            fieldMap.put(fields.get(i).getName(), "a");
        }

        // check duplicate parameters
        List<Symbol> parameters = table.getParameters(currentMethod);
        Map<String, String> paramMap = new HashMap<>();
        for (Symbol param : parameters) {
            if (paramMap.containsKey(param.getName())) {
                // Create error report
                var message = String.format("Parameter '%s' was already defined.", param.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );

                return null;
            }
            paramMap.put(param.getName(), "a");
        }

        // check local variable duplicates
        List<Symbol> locals = table.getLocalVariables(currentMethod);
        Map<String, String> localsMap = new HashMap<>() {};
        for (Symbol sym : locals) {
            if (sym.getType().hasAttribute("vargs")) {
                // Create error report
                var message = String.format("Local '%s' cannot be varargs.", sym.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );

                return null;
            }
            if (localsMap.containsKey(sym.getName())) {
                // Create error report
                var message = String.format("Variable '%s' was already defined.", sym.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );

                return null;
            }
            localsMap.put(sym.getName(), "a");
        }

        // check if local variable has the same name as a parameter
        for (Symbol local : locals) {
            if (paramMap.containsKey(local.getName())) {
                // Create error report
                var message = String.format("Parameter '%s' was already defined.", local.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );

                return null;
            }
        }

        return null;
    }

    private Void visitClassChainExpr(JmmNode classChainExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        Type type = TypeUtils.getClassFromClassChain(classChainExpr, table);
        String className;
        if (type != null) {
            if (type.hasAttribute("this")) className = "this";
            else className = type.getName();
        }
        else {
            className = "";
        }

        // if method is static, can't use 'this'
        if (Boolean.parseBoolean(currentMethodNode.get("isStatic"))) {
            // if name is 'this'
            if (className.equals("this")) {
                // Create error report
                var message = String.format("Can't use 'this' in static method: '%s'", currentMethod);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(classChainExpr),
                        NodeUtils.getColumn(classChainExpr),
                        message,
                        null)
                );

                return null;
            }
        }

        // Class is class in file, return
        if (className.equals(table.getClassName()) || className.equals("this")) return null;

        // Class is imported, return
        if (type != null && type.hasAttribute("imported")) return null;

        if (type == null) {
            // Create error report
            var message = String.format("Class '%s' was not imported or initialized.", className);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(classChainExpr),
                    NodeUtils.getColumn(classChainExpr),
                    message,
                    null)
            );
        }

        return null;
    }

}
