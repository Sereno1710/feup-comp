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
import java.util.List;
import java.util.Objects;


public class Methods extends AnalysisVisitor {

    private String currentMethod;
    private List<String> imports = new ArrayList<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNC_EXPR, this::visitFuncExpr);
    }

    private Void visitImportDecl(JmmNode importDecl, SymbolTable table) {
        List<String> values = importDecl.getObjectAsList("value", String.class);
        if (!values.isEmpty()) {
            imports.add(values.get(values.size() - 1));
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    // if there's an error, return false to stop checking
    private boolean checkMethodExists(JmmNode funcExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        List<String> classNameList = funcExpr.getChild(0).getObjectAsList("className", String.class);
        String methodName = classNameList.get(classNameList.size() - 1);

        List<String> methods = table.getMethods();
        if (methods.contains(methodName)) return true;

        // if there's an extended class, assume method exists
        String extendedClass = table.getSuper();
        // if extended class is imported
        if (imports.contains(extendedClass)) return true;

        // if class is imported
        Type type = TypeUtils.getExprType(funcExpr, table);
        if (type != null) {
            if (imports.contains(TypeUtils.getExprType(funcExpr, table).getName())) return true;
        }

        // Create error report
        var message = String.format("Method '%s' is undeclared", methodName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(funcExpr),
                NodeUtils.getColumn(funcExpr),
                message,
                null)
        );
        return false;
    }

    private void checkMethodCallParameters(JmmNode funcExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // get parameters used
        List<JmmNode> parameterNodes = funcExpr.getChildren().subList(1, funcExpr.getChildren().size());

        // get method name
        String methodName;
        List<JmmNode> classChainExprs = funcExpr.getDescendants(Kind.CLASS_CHAIN_EXPR);
        if (!classChainExprs.isEmpty()) {
            List<String> classNameList = classChainExprs.get(0).getObjectAsList("className", String.class);
            methodName = classNameList.get(classNameList.size() - 1);
        } else {
            if (funcExpr.getChild(0).getKind().equals(Kind.VAR_REF_EXPR.toString())) {
                methodName = funcExpr.getChild(0).get("name");
            } else {
                methodName = funcExpr.get("name");
            }
        }

        if (!classChainExprs.isEmpty()) {
            String className = classChainExprs.get(0).getObjectAsList("className", String.class).get(0);
            // if class used is not class in the file and method isn't defined in the file
            // assume parameters are correct
            if (!Objects.equals(table.getClassName(), className) && !table.getMethods().contains(methodName)) return;
        }

        List<Symbol> symbols = table.getParameters(methodName);
        List<Type> parameterTypes = new ArrayList<>();
        for (Symbol symbol : symbols) {
            parameterTypes.add(symbol.getType());
        }

        // if the number of parameters is wrong
        if (parameterNodes.size() != parameterTypes.size()) {
            // Create error report
            var message = String.format("Method '%s' was called with the wrong number of parameters", methodName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(funcExpr),
                    NodeUtils.getColumn(funcExpr),
                    message,
                    null)
            );
            return;
        }

        int i = 0;
        while (i < parameterNodes.size()) {
            if (!TypeUtils.getExprType(parameterNodes.get(i), table).equals(parameterTypes.get(i))) {
                // Create error report
                var message = String.format("Method '%s' parameter types are incompatible", methodName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(funcExpr),
                        NodeUtils.getColumn(funcExpr),
                        message,
                        null)
                );
            }
            i++;
        }
    }

    private Void visitFuncExpr(JmmNode funcExpr, SymbolTable table) {
        if (checkMethodExists(funcExpr, table)) checkMethodCallParameters(funcExpr, table);
        return null;
    }


}
