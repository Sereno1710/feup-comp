package pt.up.fe.comp2024.analysis.passes;

import org.antlr.v4.runtime.misc.Pair;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WhileStatement extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Pair<Boolean, String> allOperatorsValid(List<JmmNode> binExprs) {
        for (JmmNode expr : binExprs) {
            if (!new ArrayList<>(Arrays.asList(">", "<", "<=", ">=", "!")).contains(expr.get("op"))) {
                return new Pair<>(false, expr.get("op"));
            }
        }
        return new Pair<>(true, null);
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // condition expression
        JmmNode condition = whileStmt.getChild(0);
        // while body statement
        JmmNode bodyBlock = whileStmt.getChild(1);

        // if condition is a boolean expression, return
        if (TypeUtils.getExprType(condition, table).equals(new Type(TypeUtils.getBooleanTypeName(), false)))
            return null;

        // Create error report
        var message = "Boolean expressions require a boolean expression as the condition";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(whileStmt),
                NodeUtils.getColumn(whileStmt),
                message,
                null)
        );

        return null;

        // get binary expressions
        /*List<JmmNode> binExprs = new ArrayList<>(condition.getDescendants(Kind.BINARY_EXPR));
        if (condition.hasAttribute("op")) binExprs.add(condition);
        Pair<Boolean, String> pair = allOperatorsValid(binExprs);
        // if there's an invalid operator, add error
        if (!pair.a) {
            // Create error report
            var message = String.format("Operator '%s' is not valid for boolean expressions", pair.b);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(whileStmt),
                    NodeUtils.getColumn(whileStmt),
                    message,
                    null)
            );
            return null;
        }

        if (condition.getChildren().isEmpty()) {
            if (!TypeUtils.getExprType(condition, table).equals(new Type(TypeUtils.getBooleanTypeName(), false))) {
                String name;
                if (condition.hasAttribute("name")) {
                    name = condition.get("name");
                } else {
                    name = condition.get("value");
                }
                // Create error report
                var message = String.format("Variable '%s' is not boolean", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(whileStmt),
                        NodeUtils.getColumn(whileStmt),
                        message,
                        null)
                );
                return null;
            }
        }

        return null;*/
    }


}
