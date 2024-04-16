package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class UndeclaredMethod extends AnalysisVisitor {

    private String currentMethod;
    private List<String> imports = new ArrayList<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNC_EXPR, this::visitFuncExpr);
    }

    private Void visitImportDecl(JmmNode importDecl, SymbolTable table) {
        List<String> values = importDecl.getObjectAsList("value", String.class);
        if (!values.isEmpty()) {
            imports.add(values.get(values.size() - 1));
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitFuncExpr(JmmNode funcExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        List<String> classNameList = funcExpr.getChild(0).getObjectAsList("className", String.class);
        String className = classNameList.get(0);
        String methodName = classNameList.get(classNameList.size() - 1);

        List<String> methods = table.getMethods();
        if (methods.contains(methodName)) return null;

        // if there's an extended class, assume method exists
        String extendedClass = table.getSuper();
        // if extended class is imported
        if (imports.contains(extendedClass)) return null;

        // if class is imported
        if (imports.contains(TypeUtils.getExprType(funcExpr, table).getName())) return null;

        // Create error report
        var message = String.format("Method '%s' is undeclared", methodName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(funcExpr),
                NodeUtils.getColumn(funcExpr),
                message,
                null)
        );
        return null;
    }


}
