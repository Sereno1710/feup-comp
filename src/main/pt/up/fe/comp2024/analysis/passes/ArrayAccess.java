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

public class ArrayAccess extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ACC_EXPR, this::visitAccExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitAccExpr(JmmNode accExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        String name = accExpr.getChild(0).get("name");

        // if variable is array and , then
        if (TypeUtils.getExprType(accExpr.getChild(0), table).isArray()
                && TypeUtils.getExprType(accExpr.getChild(1), table)
                .equals(new Type(TypeUtils.getIntTypeName(), false))) return null;

        // Create error report
        var message = String.format("Invalid operation: '%s' is not an array.", name);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(accExpr),
                NodeUtils.getColumn(accExpr),
                message,
                null)
        );

        return null;
    }


}
