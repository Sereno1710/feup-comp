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

import java.util.Objects;

public class BinaryExprTypes extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Get members of expression
        var expressions = binaryExpr.getChildren();

        // boolean that tells us whether the operation is valid
        boolean valid = true;
        // last variable and its type
        Pair<String, Type> lastVariable = new Pair<>("", null);
        String variableIfTheresError = "";
        for (var expr : expressions) {
            String name;
            Type type;
            if (expr.hasAttribute("name")) {
                name = expr.get("name");
            } else {
                name = expr.get("value");
            }

            if (expr.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
                type = TypeUtils.getExprType(expr, table);
            } else {
                if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
                    type = new Type(TypeUtils.getIntTypeName(), false);
                } else if (expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                    type = new Type(TypeUtils.getBooleanTypeName(), false);
                } else {
                    type = null;
                }
            }
            // if expression is array, operation is invalid
            if (Objects.requireNonNull(type).isArray()) {
                // Create error report
                var message = String.format("Invalid operation: variable '%s' is an array.", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );

                return null;
            }
            // different types in the expressions, will create error report
            if (!Objects.equals(lastVariable.a, "") && !Objects.equals(type, lastVariable.b)) {
                valid = false;
                variableIfTheresError = name;
                break;
            }
            lastVariable = new Pair<>(name, type);
        }
        // if all types are the same, the operation is valid
        if (valid) return null;

        // Create error report
        var message = String.format("Invalid operation: '%s' and '%s' have different types.", lastVariable.a, variableIfTheresError);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryExpr),
                NodeUtils.getColumn(binaryExpr),
                message,
                null)
        );

        return null;
    }


}
