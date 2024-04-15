package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import javax.print.DocFlavor;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.IMPORT_DECL;

public class ClassNotImported extends AnalysisVisitor {

    private String currentMethod;
    private List<String> imports;

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNC_EXPR, this::visitFuncExpr);
    }

    private Void visitImportDecl(JmmNode root, SymbolTable table) {
        List<JmmNode> importDecls = root.getChildren(IMPORT_DECL);
        importDecls.forEach(importDecl -> {
            List<String> values = importDecl.getObjectAsList("value", String.class);
            if (!values.isEmpty()) {
                imports.add(values.get(values.size() - 1));
            }
        });
        System.out.println("imports: "+imports.stream().toString());
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitFuncExpr(JmmNode funcExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if there is an import with the same name as class used
        List<String> classAndFuncNames = funcExpr.getObjectAsList("className", String.class);
        List<String> classNames = classAndFuncNames.subList(0, classAndFuncNames.size() - 1);
        String className = String.join(".", classNames);


        // Class is imported, return
        System.out.println("imports: "+table.getImports().stream().toString());
        if (table.getImports().stream()
                .anyMatch(importDecl -> importDecl.equals(classNames))) {
            return null;
        }

        // Create error report
        var message = String.format("Class '%s' was not imported.", className);
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
