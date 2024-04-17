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
    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");


        var name= assignStmt.get("name");
        Type type= TypeUtils.getExprType(assignStmt,table);
        var assigns = assignStmt.getChildren();

        boolean valid = true;

        for(var assign: assigns){
            Type a_type= TypeUtils.getExprType(assign,table);
            if(!a_type.equals(type)){

                if (assign.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
                    a_type = new Type(TypeUtils.getIntTypeName(), false);
                } else if (assign.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                    a_type = new Type(TypeUtils.getBooleanTypeName(), false);
                } else if (assign.getKind().equals(Kind.IMPORT_DECL.toString())){

                    continue;
                }
                var message = String.format("Invalid operation: variable '%s' is not the of the type '%s'.", name, a_type.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null)
                );
                return null;
            }
        }



        return null;
    }
}
