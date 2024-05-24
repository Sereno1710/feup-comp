package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(ASSIGN_STMT_ARRAY, this::visitAssignStmtArray);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        String name = node.get("name");
        String typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        String parent = node.getParent().getKind();

        StringBuilder code = new StringBuilder();

        if (Objects.equals(parent, "ClassDecl")) {
            code.append(".field public ");
            code.append(name);
            code.append(typeCode);
            code.append(END_STMT);
        }

        return code.toString();
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        List<String> names = node.getObjectAsList("value", String.class);

        StringBuilder code = new StringBuilder();

        code.append("import ");
        for (int i = 0; i < names.size(); i++) {
            if (i != 0) {
                code.append(".");
            }
            code.append(names.get(i));
        }
        code.append(END_STMT);

        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = node.get("name");
        var rhs = exprVisitor.visit(node.getJmmChild(0));

        StringBuilder code = new StringBuilder();

        code.append(rhs.getComputation());

        Type thisType = TypeUtils.getTypeFromString(node.get("name"), node, table);
        String typeString = OptUtils.toOllirType(thisType);

        if (Objects.equals(node.getJmmChild(0).getKind(), "NewClassExpr")) {
            code.append("invokespecial(").append(rhs.getCode()).append(", \"<init>\").V").append(END_STMT);
        }

        code.append(lhs);
        code.append(typeString);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);


        return code.toString();
    }

    private String visitAssignStmtArray(JmmNode node, Void unused) {
        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        Type thisType = TypeUtils.getTypeFromString(node.get("name"), node, table);
        String typeString = OptUtils.toOllirType(new Type(thisType.getName(), false));

        if (Objects.equals(node.getJmmChild(0).getKind(), "NewClassExpr")) {
            code.append("invokespecial(").append(rhs.getCode()).append(", \"<init>\").V").append(END_STMT);
        }

        code.append(node.get("name")).append("[").append(lhs.getCode()).append("]").append(typeString).append(SPACE).append(ASSIGN)
                .append(typeString).append(SPACE).append(rhs.getCode()).append(END_STMT);


        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitIfStmt(JmmNode node, Void unused) {
        int ifNum = OptUtils.getNextIfNum();
        var conditionLabel = "if" + ifNum;
        var endLabel = "endif" + ifNum;

        StringBuilder code = new StringBuilder();

        var condition = exprVisitor.visit(node.getJmmChild(0));
        code.append(condition.getComputation());

        code.append("if (").append(condition.getCode()).append(") goto ").append(conditionLabel).append(END_STMT);

        if (node.getChildren().size() > 2) {
            code.append(exprVisitor.visit(node.getJmmChild(2)));
        }

        code.append("goto ").append(endLabel).append(END_STMT);
        code.append(conditionLabel).append(":\n");

        code.append(exprVisitor.visit(node.getJmmChild(1)));

        code.append(endLabel).append(":\n");

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        int ifNum = OptUtils.getNextWhileNum();
        var conditionLabel = "whileCond" + ifNum;
        var loopLabel = "whileLoop" + ifNum;
        var endLabel = "whileEnd" + ifNum;

        StringBuilder code = new StringBuilder();

        code.append(conditionLabel).append(":\n");

        var condition = exprVisitor.visit(node.getJmmChild(0));
        code.append(condition.getComputation());

        code.append("if (").append(condition.getCode()).append(") goto ").append(loopLabel).append(END_STMT);
        code.append("goto ").append(endLabel).append(END_STMT);
        code.append(loopLabel).append(":\n");

        code.append(exprVisitor.visit(node.getJmmChild(1)));

        code.append("goto ").append(conditionLabel).append(END_STMT);
        code.append(endLabel).append(":\n");

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (isPublic) {
            code.append("public ");
        }

        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        boolean hasParams = true;
        if (Objects.equals(name, "main")) {
            code.append("(args.array.String)");
        } else {
            code.append("(");


            var params = node.getJmmChild(1);
            if (Objects.equals(params.getKind(), "Params")) {
                for (int i = 0; i < params.getNumChildren(); i++) {
                    if (i != 0) code.append(", ");
                    code.append(visit(params.getJmmChild(i)));
                }
            } else hasParams = false;

            code.append(")");
        }

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var afterParam = hasParams ? 2 : 1;
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            if (Objects.equals(child.getKind(), "ExprStmt")) {
                code.append(exprVisitor.visit(child).getCode());
            } else {
                code.append(visit(child));
            }


        }

        if (Objects.equals(name, "main")) {
            code.append("ret.V");
            code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        String superClass = table.getSuper();
        if (!superClass.isEmpty()) {
            code.append(" extends ").append(superClass);
        } else {
            code.append(" extends Object");
        }

        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        for (var child : node.getChildren()) {
            code.append(visit(child));

        }

        return code.toString();
    }
}
