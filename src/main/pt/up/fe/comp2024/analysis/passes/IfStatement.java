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
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class IfStatement extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitIfStmt(JmmNode ifStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // condition expression
        JmmNode condition = ifStmt.getChild(0);
        // if statement block
        JmmNode ifBlock = ifStmt.getChild(1);
        // else statement block
        JmmNode elseBlock = ifStmt.getChild(2);

        // if condition is a boolean expression, return
        if (TypeUtils.getExprType(condition, table).equals(new Type(TypeUtils.getBooleanTypeName(), false)))
            return null;

        // Create error report
        var message = "If statements require a boolean expression as the condition";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(ifStmt),
                NodeUtils.getColumn(ifStmt),
                message,
                null)
        );

        return null;
    }


}
