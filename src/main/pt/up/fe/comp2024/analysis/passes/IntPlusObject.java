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
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Objects;

public class IntPlusObject extends AnalysisVisitor {

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
        Pair<String, String> lastVariable = new Pair<>("", "");
        String variableIfTheresError = "";
        for (var expr : expressions) {
            String typeName;
            String name;
            if (expr.hasAttribute("name")) {
                name = expr.get("name");
            } else {
                name = expr.get("value");
            }
            if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
                typeName = "int";
            } else if (expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                typeName = "boolean";
            } else if (expr.getKind().equals(Kind.OBJECT_LITERAL.toString())) {
                typeName = table.getClassName();
            } else {
                /**
                 * FIX: not getting type for declared variables
                 */
                typeName = "object";
            }
            // different types in the expressions, will create error report
            if (!Objects.equals(lastVariable.a, "") && !Objects.equals(typeName, lastVariable.b)) {
                valid = false;
                variableIfTheresError = name;
                break;
            }
            lastVariable = new Pair<>(name, typeName);
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
