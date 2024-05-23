package pt.up.fe.comp2024.analysis.passes;

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

public class Assignment extends AnalysisVisitor {

    private String currentMethod;
    private JmmNode currentMethodNode;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL,this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT,this::visitAssignStmt);
        addVisit(Kind.ASSIGN_STMT_ARRAY,this::visitAssignStmtArray);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        currentMethodNode = method;
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        String varName = assignStmt.get("name");

        boolean declared = false;
        // Var is a local variable
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varName))) {
            declared = true;
        }

        // Var is a parameter
        if (!declared && table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varName))) {
            declared = true;
        }

        // Var is a field and method is not static
        if (!declared && table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varName)) &&
                !Boolean.parseBoolean(currentMethodNode.get("isStatic"))) {
            declared = true;
        }

        // if variable is not declared
        if (!declared) {
            // Create error report
            var message = String.format("Variable '%s' does not exist.", varName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null)
            );

            return null;
        }

        Type left = TypeUtils.getTypeFromString(varName, assignStmt, table);
        Type right = TypeUtils.getExprType(assignStmt.getChild(0), table);

        if (left != null && right != null
                && TypeUtils.areTypesAssignable(left, right, table)) return null;

        var message = String.format("Variable '%s' is assigned to invalid value.", varName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(assignStmt),
                NodeUtils.getColumn(assignStmt),
                message,
                null)
        );
        return null;
    }

    private Void visitAssignStmtArray(JmmNode assignStmtArray, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        String varName = assignStmtArray.get("name");

        boolean declared = false;
        // Var is a local variable
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varName))) {
            declared = true;
        }

        // Var is a parameter
        if (!declared && table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varName))) {
            declared = true;
        }

        // Var is a field and method is not static
        if (!declared && table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varName)) &&
                !Boolean.parseBoolean(currentMethodNode.get("isStatic"))) {
            declared = true;
        }

        // if variable is not declared
        if (!declared) {
            // Create error report
            var message = String.format("Variable '%s' does not exist.", varName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmtArray),
                    NodeUtils.getColumn(assignStmtArray),
                    message,
                    null)
            );

            return null;
        }

        Type left = TypeUtils.getExprType(assignStmtArray.getChild(0), table);
        Type right = TypeUtils.getExprType(assignStmtArray.getChild(1), table);

        if (left != null && right != null
                && TypeUtils.areTypesAssignable(left, right, table)) return null;

        var message = String.format("Variable '%s' is assigned to invalid value.", varName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(assignStmtArray),
                NodeUtils.getColumn(assignStmtArray),
                message,
                null)
        );
        return null;
    }
}
