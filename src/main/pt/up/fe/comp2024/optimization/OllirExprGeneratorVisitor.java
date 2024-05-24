package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
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
        addVisit(NOT_EXPR, this::visitNotExpr);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(LENGTH_EXPR, this::visitLengthExpr);
        addVisit(ACC_EXPR, this::visitAccExpr);
        addVisit(ARRAY_EXPR, this::visitArrayExpr);

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
        if (Objects.equals(value, "true")) {
            value = "1";
        } else if (Objects.equals(value, "false")) {
            value = "0";
        }
        String code = value + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        String ollirType = ".array" + OptUtils.toOllirType(node.getJmmChild(0));

        String code = OptUtils.getTemp() + ollirType;

        StringBuilder computation = new StringBuilder();

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(ollirType).append(SPACE).append("new(array, ").append(visit(node.getJmmChild(1)).getCode())
                .append(")").append(ollirType).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitArrayExpr(JmmNode node, Void unused) {
        String ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table));
        String ollirArrayType = ".array" + ollirType;

        String code = OptUtils.getTemp() + ollirArrayType;

        StringBuilder computation = new StringBuilder();

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(ollirArrayType).append(SPACE).append("new(array, ")
                .append(node.getChildren().size()).append(".i32)").append(ollirArrayType).append(END_STMT);

        for (int i = 0; i < node.getChildren().size(); i++) {
            var arrayValue = visit(node.getJmmChild(i));

            computation.append(arrayValue.getComputation()).append(code).append("[").append(i).append(".i32]")
                    .append(ollirType).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append(arrayValue.getCode())
                    .append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitLengthExpr(JmmNode node, Void unused) {
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = OptUtils.getTemp() + ollirType;

        StringBuilder computation = new StringBuilder();

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(ollirType).append(SPACE).append("arraylength(")
                .append(visit(node.getJmmChild(0)).getCode()).append(")").append(ollirType).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitAccExpr(JmmNode node, Void unused) {
        Type type = TypeUtils.getExprType(node.getJmmChild(0), table);
        String ollirType = OptUtils.toOllirType(new Type(type.getName(), false));

        var n = visit(node.getJmmChild(1));

        String code = OptUtils.getTemp() + ollirType;

        StringBuilder computation = new StringBuilder();

        computation.append(n.getComputation()).append(code).append(SPACE)
                .append(ASSIGN).append(ollirType).append(SPACE).append(node.getJmmChild(0).get("name"))
                .append(ollirType).append(".array").append("[").append(n.getCode()).append("]").append(ollirType)
                .append(END_STMT);

        return new OllirExprResult(code, computation);
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

    private OllirExprResult visitNotExpr(JmmNode node, Void unused) {
        var child = visit(node.getJmmChild(0));

        StringBuilder computation = new StringBuilder();

        computation.append(child.getComputation());

        String code = OptUtils.getTemp() + ".bool";

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(".bool").append(SPACE)
                .append("!.bool").append(SPACE).append(child.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        var method = node.getParent();
        while (method != null) {
            if (method.getKind().equals("MethodDecl")) {
                var methodName = method.get("name");
                var paramNum = 1;
                for (Symbol param : table.getParameters(methodName)) {
                    if (Objects.equals(param.getName(), id)) {
                        String code = "$" + paramNum + "." + id + ollirType;

                        return new OllirExprResult(code);
                    }
                    paramNum++;
                }
                break;
            }
            method = method.getParent();
        }

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
            if (importLib.equals(libName) || importLib.endsWith("." + libName)) {
                importedLib = true;
                break;
            } else {
                String[] parts = importLib.split("\\.");
                for (String part : parts) {
                    if (part.equals(libName)) {
                        importedLib = true;
                        break;
                    }
                }
            }
            if (importedLib) {
                break;
            }
        }
        boolean declaredMethod = false;
        for(String method: table.getMethods()) {
            if(Objects.equals(functionName, method)) {
                declaredMethod = true;
                break;
            }
        }
        if (declaredMethod) {
            var defParams = table.getParameters(functionName);
            for (int i = 1; i < node.getNumChildren(); i++) {
                var child = node.getJmmChild(i);
                var childCode = visit(child);

                var varargsParam = i <= defParams.size() && defParams.get(i - 1).getType().hasAttribute("vargs");
                var ollirType = OptUtils.toOllirType(table.getReturnType(functionName));
                var ollirArrayType = ".array" + ollirType;

                if (varargsParam) {
                    var temp = OptUtils.getTemp() + ollirArrayType;
                    computation.append(temp).append(SPACE)
                            .append(ASSIGN).append(ollirArrayType).append(SPACE).append("new(array, ")
                            .append(visit(node.getJmmChild(1)).getCode())
                            .append(")").append(ollirArrayType).append(END_STMT);

                    for (int j = i; j < node.getNumChildren(); j++) {
                        var varargsChild = node.getJmmChild(j);
                        var varargsChildCode = visit(varargsChild);

                        computation.append(varargsChildCode.getComputation()).append(temp).append("[")
                                .append(j).append(".i32]").append(ollirType).append(SPACE)
                                .append(ASSIGN).append(ollirType).append(SPACE).append(varargsChildCode.getCode())
                                .append(END_STMT);
                    }

                    funcParamsCode.append(", ");
                    funcParamsCode.append(temp);

                    break;
                }

                funcParamsCode.append(", ");
                funcParamsCode.append(childCode.getCode());
                code.append(childCode.getComputation());
            }
        } else {
            for (int i = 1; i < node.getNumChildren(); i++) {
                var child = node.getJmmChild(i);
                var childCode = visit(child);
                funcParamsCode.append(", ");
                funcParamsCode.append(childCode.getCode());
                code.append(childCode.getComputation());
            }
        }

        if (importedLib) {
            code.append("invokestatic(");
            code.append(libName);
            code.append(", \"");
            code.append(functionName);
            code.append("\"");
            code.append(funcParamsCode);
            if (Objects.equals(node.getParent().getKind(), "AssignStmt")) {
                var parentType = TypeUtils.getTypeFromString(node.getParent().get("name"), node.getParent(), table);
                String parentTypeString = OptUtils.toOllirType(parentType);
                code.append(parentTypeString);
            } else if (Objects.equals(node.getParent().getKind(), "AssignStmt")) {

            } else {
                code.append(").V");
                code.append(END_STMT);
            }
        } else {
            Type classType = TypeUtils.getClassFromClassChain(classChainExpr, table);
            String classOllirType = OptUtils.toOllirType(classType);

            if (Objects.equals(node.getParent().getKind(), "ExprStmt")) {
                Type resType = TypeUtils.getExprType(node, table);
                String resOllirType = OptUtils.toOllirType(resType);

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
                var parent = node.getParent();
                if (Objects.equals(node.getParent().getKind(), "BinaryExpr")) {
                    while (!Objects.equals(parent.getKind(), "AssignStmt")) {
                        parent = parent.getParent();
                    }
                }
                var parentTypeString = "";
                if (Objects.equals(node.getParent().getKind(), "AccExpr")) {
                    parentTypeString = OptUtils.toOllirType(TypeUtils.getExprType(parent, table));
                } else if (Objects.equals(node.getParent().getKind(), "FuncExpr")) {
                    parentTypeString = OptUtils.toOllirType(table.getReturnType(node.getJmmChild(0).getObjectAsList("className", String.class).get(1)));
                } else {
                    parentTypeString = OptUtils.toOllirType(TypeUtils.getTypeFromString(parent.get("name"), node.getParent(), table));
                }

                String temp = OptUtils.getTemp();
                code.append(temp).append(parentTypeString);

                computation.append(code).append(SPACE)
                        .append(ASSIGN).append(parentTypeString).append(SPACE);

                computation.append("invokevirtual(");
                computation.append(libName);
                computation.append(classOllirType);
                computation.append(", \"");
                computation.append(functionName);
                computation.append("\"");
                computation.append(funcParamsCode);
                computation.append(")");
                computation.append(parentTypeString);
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
