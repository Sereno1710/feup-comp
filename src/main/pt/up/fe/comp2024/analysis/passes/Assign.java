package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.specs.util.SpecsCheck;

public class Assign extends AnalysisVisitor {

    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT,this::visitAssignStmt);
        addVisit(Kind.METHOD_DECL,this::visitMethodDecl);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable symbolTable) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var assigns = assignStmt.getChildren();

        boolean valid = true;

        return null;
    }
}
