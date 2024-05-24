package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(FUNC_EXPR, this::visitFuncExpr);
        addVisit(NEW_CLASS_EXPR, this::visitNewClassExpr);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type(TypeUtils.getBooleanTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(boolType);
        String value = node.get("value");
        if(Objects.equals(value, "true")) {
            value = "1";
        } else if(Objects.equals(value, "false")) {
            value = "0";
        }
        String code = value + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitFuncExpr(JmmNode node, Void unused) {
        var classChainExpr = node.getJmmChild(0);
        List<String> classAndFuncNames = classChainExpr.getObjectAsList("className", String.class);
        String libName = classAndFuncNames.get(0);
        String functionName = classAndFuncNames.get(1);


        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        StringBuilder funcParamsCode = new StringBuilder();

        var importedLib = false;
        for (var importLib : table.getImports()) {
            if (importLib.equals(libName)) {
                importedLib = true;
                break;
            }
        }

        for (int i = 1; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            funcParamsCode.append(", ");
            funcParamsCode.append(childCode.getCode());
            computation.append(childCode.getComputation());
        }

        if (importedLib) {
            code.append("invokestatic(");
            code.append(libName);
            code.append(", \"");
            code.append(functionName);
            code.append("\"");
            code.append(funcParamsCode);
            code.append(").V");
            code.append(END_STMT);
        } else {
            Type resType = TypeUtils.getExprType(node, table);
            String resOllirType = OptUtils.toOllirType(resType);
            Type classType = TypeUtils.getClassFromClassChain(classChainExpr, table);
            String classOllirType = OptUtils.toOllirType(classType);

            if(Objects.equals(node.getParent().getKind(), "ExprStmt")) {
                code.append("invokevirtual(");
                code.append(libName);
                code.append(classOllirType);
                code.append(", \"");
                code.append(functionName);
                code.append("\"");
                code.append(funcParamsCode);
                code.append(")");
                code.append(resOllirType);
                code.append(END_STMT);
            } else {
                String temp = OptUtils.getTemp();
                code.append(temp).append(resOllirType);

                computation.append(code).append(SPACE)
                        .append(ASSIGN).append(resOllirType).append(SPACE);

                computation.append("invokevirtual(");
                computation.append(libName);
                computation.append(classOllirType);
                computation.append(", \"");
                computation.append(functionName);
                computation.append("\"");
                computation.append(funcParamsCode);
                computation.append(")");
                computation.append(resOllirType);
                computation.append(END_STMT);
            }
        }
        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitNewClassExpr(JmmNode node, Void unused) {
        String className = node.get("name");

        String tmpClassInstance = OptUtils.getTemp();

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        code.append(tmpClassInstance).append(".").append(className);

        computation.append(code).append(SPACE).append(ASSIGN)
                .append(".").append(className).append(SPACE).append("new(").append(className).append(").")
                .append(className).append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }


    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        for (var child : node.getChildren()) {
            OllirExprResult visitResult = visit(child);
            code.append(visitResult.getCode());
            computation.append(visitResult.getComputation());
        }

        return new OllirExprResult(code.toString(), computation);
    }

}
