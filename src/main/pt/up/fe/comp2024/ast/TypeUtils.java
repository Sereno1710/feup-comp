package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }
    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());



        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR , ASSIGN_STMT -> getVarExprType(expr, table);
            case FUNC_EXPR -> getVarExprTypeFromFuncExpr(expr, table);
            case CLASS_CHAIN_EXPR -> getVarExprTypeFromClassChain(expr, table);
            case ACC_EXPR -> getArrayAccessExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-" -> new Type(INT_TYPE_NAME, false);
            case "&&", "||", "<", ">", "<=", ">=" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type lookForSymbolInList(List<Symbol> list, String name) {
        for (Symbol symbol : list) {
            if (Objects.equals(symbol.getName(), name)) {
                return symbol.getType();
            }
        }
        return null;
    }

    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        String name = varRefExpr.get("name");

        JmmNode curr = varRefExpr;
        Type type;
        while (curr != null) {
            if (curr.getKind().equals(Kind.METHOD_DECL.toString())) {
                List<Symbol> params = table.getParameters(curr.get("name"));
                type = lookForSymbolInList(params, name);
                if (type != null) return type;
                List<Symbol> locals = table.getLocalVariables(curr.get("name"));
                type = lookForSymbolInList(locals, name);
                if (type != null) return type;
            }
            curr = curr.getParent();
        }

        List<Symbol> fields = table.getFields();
        type = lookForSymbolInList(fields, name);
        return type;
    }

    private static Type getVarExprTypeFromClassChain(JmmNode expr, SymbolTable table) {
        List<String> classAndFuncNames = expr.getObjectAsList("className", String.class);
        List<String> classNames = classAndFuncNames.subList(0, classAndFuncNames.size() - 1);
        String className = classNames.get(classNames.size() - 1);

        JmmNode curr = expr;
        Type type;
        while (curr != null) {
            if (curr.getKind().equals(Kind.METHOD_DECL.toString())) {
                List<Symbol> params = table.getParameters(curr.get("name"));
                type = lookForSymbolInList(params, className);
                if (type != null) return type;
                List<Symbol> locals = table.getLocalVariables(curr.get("name"));
                type = lookForSymbolInList(locals, className);
                if (type != null) return type;
            }
            curr = curr.getParent();
        }

        List<Symbol> fields = table.getFields();
        type = lookForSymbolInList(fields, className);
        return type;
    }

    private static Type getVarExprTypeFromFuncExpr(JmmNode expr, SymbolTable table) {
        String methodName;
        String className = "";
        if (expr.hasAttribute("name")) methodName = expr.get("name");
        else {
            List<String> classNames = expr.getChild(0).getObjectAsList("className", String.class);
            methodName = classNames.get(1);
            className = classNames.get(0);
        }

        // if method is not defined in this file, return type
        if (!table.getMethods().contains(methodName)) {
            if (!Objects.equals(className, "") && !Objects.equals(table.getClassName(), className)) {
                Type type = new Type("", false);
                type.putObject("accept", true);
                return type;
            }
        }

        JmmNode root = expr;
        while (!root.getKind().equals(Kind.CLASS_DECL.toString())) {
            root = root.getParent();
        }
        List<JmmNode> methods = root.getChildren(Kind.METHOD_DECL);
        for (JmmNode method : methods) {
            if (Objects.equals(method.get("name"), methodName)) {
                return getTypeFromString(method.getChild(0).get("name"));
            }
        }
        return null;
    }

    private static Type getArrayAccessExprType(JmmNode accExpr, SymbolTable table) {
        JmmNode arrayVarNode = accExpr.getChild(0);
        Type arrayType = getVarExprType(arrayVarNode, table);
        return new Type(arrayType.getName(), false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }

    public static Type getTypeFromString(String typeString) {
        switch (typeString) {
            case INT_TYPE_NAME -> {
                return new Type(INT_TYPE_NAME, false);
            }
            case "int[]" -> {
                return new Type(INT_TYPE_NAME, true);
            }
            case BOOLEAN_TYPE_NAME -> {
                return new Type(BOOLEAN_TYPE_NAME, false);
            }
            case "boolean[]" -> {
                return new Type(BOOLEAN_TYPE_NAME, true);
            }
            default -> {
                if (typeString.endsWith("[]")) return new Type(typeString.substring(0, typeString.length() - 2), true);
                return new Type(typeString, false);
            }
        }
    }
}
