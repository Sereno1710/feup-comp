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

public class ExprTypes extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.NEW_CLASS_EXPR, this::visitNewClassExpr);
        addVisit(Kind.NOT_EXPR, this::visitNotExpr);
        addVisit(Kind.LENGTH_EXPR, this::visitLengthExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        String operator = binaryExpr.get("op");
        Type intOrBoolean = null;
        switch (operator) {
            case "+", "*", "-", "/", "<", ">", "<=", ">=" -> intOrBoolean = new Type(TypeUtils.getIntTypeName(), false);
            case "&&", "||", "!" -> intOrBoolean = new Type(TypeUtils.getBooleanTypeName(), false);
            case "==", "!=" -> intOrBoolean = new Type("intOrBool", false);
        }

        if (intOrBoolean == null) {
            // Create error report
            var message = String.format("Invalid operator: operator '%s' is not accepted.", operator);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null)
            );

            return null;
        }

        // if values can be int or boolean
        if (intOrBoolean.getName().equals("intOrBool")) {
            // if either value is not int or boolean
            if ((!TypeUtils.getExprType(binaryExpr.getChild(0), table)
                    .equals(new Type(TypeUtils.getIntTypeName(), false)) &&
                    !TypeUtils.getExprType(binaryExpr.getChild(0), table)
                            .equals(new Type(TypeUtils.getBooleanTypeName(), false)))
                    ||
                    (!TypeUtils.getExprType(binaryExpr.getChild(1), table)
                            .equals(new Type(TypeUtils.getIntTypeName(), false)) &&
                            !TypeUtils.getExprType(binaryExpr.getChild(1), table)
                                    .equals(new Type(TypeUtils.getBooleanTypeName(), false)))) {
                // Create error report
                var message = String.format("Invalid operation: operator '%s' requires int or boolean types.", operator);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );

                return null;
            } else return null;
        }

        if (!TypeUtils.getExprType(binaryExpr.getChild(0), table).equals(intOrBoolean) ||
                !TypeUtils.getExprType(binaryExpr.getChild(1), table).equals(intOrBoolean)) {
            // Create error report
            var message = String.format("Invalid operation: operator '%s' requires '%s' types.", operator, intOrBoolean.getName());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null)
            );

            return null;
        }

        return null;
    }

    private Void visitNewClassExpr(JmmNode newClassExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        String varName = "";
        if (newClassExpr.getParent().hasAttribute("name")) {
            varName = newClassExpr.getParent().get("name");
        }

        if (varName.isEmpty()) return null;

        // if classes match in initializations, return
        if (TypeUtils.getTypeFromString(varName, newClassExpr, table)
                .equals(TypeUtils.getTypeFromTypeString(newClassExpr.get("name"))))
            return null;

        var message = String.format("Variable '%s' has an invalid initialization.", varName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(newClassExpr),
                NodeUtils.getColumn(newClassExpr),
                message,
                null)
        );

        return null;
    }

    private Void visitNotExpr(JmmNode notExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // if type of expr is boolean, it's correct
        if (TypeUtils.getExprType(notExpr, table)
                .equals(new Type(TypeUtils.getBooleanTypeName(), false)))
            return null;

        // Create error report
        var message = "Invalid operation: '!' can only be used on boolean expressions.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(notExpr),
                NodeUtils.getColumn(notExpr),
                message,
                null)
        );

        return null;
    }

    private Void visitLengthExpr(JmmNode lengthExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // if length is used on something that is not an array, add an error
        if (!TypeUtils.getExprType(lengthExpr.getChild(0), table).isArray()) {
            // Create error report
            var message = "Invalid operation: 'length' can only be used on arrays.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(lengthExpr),
                    NodeUtils.getColumn(lengthExpr),
                    message,
                    null)
            );

            return null;
        }

        return null;
    }


}
