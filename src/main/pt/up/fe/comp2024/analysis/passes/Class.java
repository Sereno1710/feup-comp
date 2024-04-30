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
    private final List<String> lastImports = new ArrayList<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
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
                var message = "Method main has the wrong arguments";
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

        Map<String, String> importMap = new HashMap<>();
        for (String imp : lastImports) {
            if (importMap.containsKey(imp)) {
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
            importMap.put(imp, "a");
        }

        return null;
    }

    private Void visitImportDecl(JmmNode importDecl, SymbolTable table) {
        List<String> values = importDecl.getObjectAsList("value", String.class);
        if (!values.isEmpty()) {
            lastImports.add(values.get(values.size() - 1));
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        currentMethodNode = method;

        // check duplicate fields
        List<Symbol> fields = table.getFields();
        Map<String, String> fieldMap = new HashMap<>();
        for (Symbol field : fields) {
            if (field.getType().hasAttribute("vargs")) {
                // Create error report
                var message = String.format("Field '%s' cannot be varargs.", field.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method.getParent()),
                        NodeUtils.getColumn(method.getParent()),
                        message,
                        null)
                );

                return null;
            }
            if (fieldMap.containsKey(field.getName())) {
                // Create error report
                var message = String.format("Field '%s' was already defined.", field.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method.getParent()),
                        NodeUtils.getColumn(method.getParent()),
                        message,
                        null)
                );

                return null;
            }
            fieldMap.put(field.getName(), "a");
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

        // check if parameter has the same name as a field, if function is not static
        if (!Boolean.parseBoolean(currentMethodNode.get("isStatic"))) {
            for (Symbol param : parameters) {
                if (fieldMap.containsKey(param.getName())) {
                    // Create error report
                    var message = String.format("Field '%s' was already defined.", param.getName());
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
        }

        // check if local variable has the same name as a parameter or field
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
            // if method is not static
            if (!Boolean.parseBoolean(currentMethodNode.get("isStatic")) && fieldMap.containsKey(local.getName())) {
                // Create error report
                var message = String.format("Field '%s' was already defined.", local.getName());
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

        // if method is static, can't use 'this'
        if (Boolean.parseBoolean(currentMethodNode.get("isStatic"))) {
            // Check if there is an import with the same name as class used
            List<String> classAndFuncNames = classChainExpr.getObjectAsList("className", String.class);
            List<String> classNames = classAndFuncNames.subList(0, classAndFuncNames.size() - 1);
            String name = classNames.get(classNames.size() - 1);

            // if name is 'this'
            if (name.equals("this")) {
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

        Type type = TypeUtils.getClassFromClassChain(classChainExpr, table);
        String className;
        if (type != null) {
            className = type.getName();
        } else {
            className = "";
        }

        // Class is class in file, return
        if (className.equals(table.getClassName())) return null;

        // Class is imported, return
        if (lastImports.stream()
                .anyMatch(importDecl -> importDecl.equals(className))) {
            return null;
        }

        // Create error report
        var message = String.format("Class '%s' was not imported or initialized.", className);
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
