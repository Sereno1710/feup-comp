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

import java.util.ArrayList;
import java.util.List;

public class Class extends AnalysisVisitor {

    private String currentMethod;
    private List<String> imports = new ArrayList<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_CHAIN_EXPR, this::visitClassChainExpr);
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

    private Void visitClassChainExpr(JmmNode classChainExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if there is an import with the same name as class used
        List<String> classAndFuncNames = classChainExpr.getObjectAsList("className", String.class);
        List<String> classNames = classAndFuncNames.subList(0, classAndFuncNames.size() - 1);
        String className = classNames.get(classNames.size() - 1);

        // if class is 'this'
        if (className.equals("this")) return null;

        // Class is imported, return
        if (imports.stream()
                .anyMatch(importDecl -> importDecl.equals(className))) {
            return null;
        }

        Type type = TypeUtils.getExprType(classChainExpr, table);
        // if var is not initiated
        if (type == null) {
            // Create error report
            var message = String.format("Class '%s' was not imported or initialized.", className);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(classChainExpr),
                    NodeUtils.getColumn(classChainExpr),
                    message,
                    null)
            );

            return null;
        }
        return null;
    }

}
