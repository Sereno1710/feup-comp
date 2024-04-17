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


public class ReturnType extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        // if return types are correct
        if (TypeUtils.getTypeFromString(returnStmt.getParent().getChild(0).get("name"))
                .equals(TypeUtils.getExprType(returnStmt.getChild(0), table)))
            return null;

        // Create error report
        var message = String.format("Return statement has wrong type in method '%s'", returnStmt.getParent().get("name"));
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(returnStmt),
                NodeUtils.getColumn(returnStmt),
                message,
                null)
        );
        return null;
    }


}
