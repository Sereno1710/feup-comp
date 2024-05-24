package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Objects;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String STRING_TYPE_NAME = "String";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }
    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }
    public static String getStringTypeName() {
        return STRING_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {

        var kind = Kind.fromString(expr.getKind());



        Type type = switch (kind) {
            case PAREN_EXPR, NOT_EXPR -> getExprType(expr.getChild(0), table);
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case NEW_CLASS_EXPR -> getTypeFromString(expr.getParent().get("name"), expr, table);
            case FUNC_EXPR -> getVarExprTypeFromFuncExpr(expr, table);
            case CLASS_CHAIN_EXPR -> getVarExprTypeFromClassChain(expr, table);
            case ACC_EXPR -> getArrayAccessExprType(expr, table);
            case NEW_ARRAY -> getNewArrayType(expr);
            case ARRAY_EXPR -> getArrayExprType(expr, table);
            case OBJECT_LITERAL -> new Type(table.getClassName(), false);
            case INTEGER_LITERAL, LENGTH_EXPR -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "&&", "||", "<", ">", "<=", ">=", "!", "==", "!=" -> new Type(BOOLEAN_TYPE_NAME, false);
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
                break;
            }
            curr = curr.getParent();
        }

        List<Symbol> fields = table.getFields();
        type = lookForSymbolInList(fields, name);
        return type;
    }

    public static Type getClassFromClassChain(JmmNode expr, SymbolTable table) {
        List<String> classAndFuncNames = expr.getObjectAsList("className", String.class);
        List<String> classNames = classAndFuncNames.subList(0, classAndFuncNames.size() - 1);
        String className = String.join(".", classNames);

        if (className.contains(".")) {
            Type type = new Type(className, false);
            type.putObject("classChain", true);
            return type;
        }

        if (className.equals("this")) {
            Type type = new Type(table.getClassName(), false);
            type.putObject("this", true);
            return type;
        }

        if (className.equals(table.getClassName()))
            return new Type(table.getClassName(), false);

        if (table.getImports().stream().anyMatch(importDecl -> importDecl.equals(className))) {
            Type type = new Type(className, false);
            type.putObject("imported", true);
            return type;
        }

        JmmNode curr = expr;
        Type type;
        while (curr != null) {
            if (curr.getKind().equals(Kind.METHOD_DECL.toString())) {
                List<Symbol> params = table.getParameters(curr.get("name"));
                type = lookForSymbolInList(params, className);
                if (type != null) {
                    if (type.getName().equals(table.getClassName())) return type;
                    type.putObject("imported", true);
                    return type;
                }
                List<Symbol> locals = table.getLocalVariables(curr.get("name"));
                type = lookForSymbolInList(locals, className);
                if (type != null) {
                    if (type.getName().equals(table.getClassName())) return type;
                    type.putObject("imported", true);
                    return type;
                }
                break;
            }
            curr = curr.getParent();
        }

        List<Symbol> fields = table.getFields();
        type = lookForSymbolInList(fields, className);
        return type;
    }

    private static Type getVarExprTypeFromClassChain(JmmNode expr, SymbolTable table) {
        List<String> classAndFuncNames = expr.getObjectAsList("className", String.class);
        String methodName = classAndFuncNames.get(classAndFuncNames.size() - 1);

        return table.getReturnType(methodName);
    }

    private static Type getVarExprTypeFromFuncExpr(JmmNode expr, SymbolTable table) {
        String methodName = null;
        List<JmmNode> classChainExprs = expr.getDescendants(Kind.CLASS_CHAIN_EXPR);
        List<JmmNode> newClassExprs = expr.getDescendants(Kind.NEW_CLASS_EXPR);
        if (expr.hasAttribute("name")) methodName = expr.get("name");
        if (!classChainExprs.isEmpty()) {
            List<String> classNames = classChainExprs.get(0).getObjectAsList("className", String.class);
            methodName = methodName == null ? classNames.get(classNames.size() - 1) : methodName;
        }

        // if class is imported, assume type is okay
        Type classType;
        if (!classChainExprs.isEmpty()) classType = getClassFromClassChain(classChainExprs.get(0), table);
        else if (!newClassExprs.isEmpty())
            classType = new Type(newClassExprs.get(0).get("name"), false);
        else {
            classType = null;
        }
        if (classType != null && (classType.hasAttribute("imported") ||
                (classType.hasAttribute("classChain") && table.getImports().stream()
                        .anyMatch(importDecl -> importDecl.equals(classType.getName()))))) {
            Type type = new Type("", false);
            type.putObject("assignable", true);
            return type;
        }

        JmmNode root = expr;
        while (!root.getKind().equals(Kind.CLASS_DECL.toString())) {
            root = root.getParent();
        }
        List<JmmNode> methods = root.getChildren(Kind.METHOD_DECL);
        for (JmmNode method : methods) {
            if (Objects.equals(method.get("name"), methodName)) {
                return getTypeFromTypeString(method.getChild(0).get("name"));
            }
        }
        return null;
    }

    private static Type getArrayAccessExprType(JmmNode accExpr, SymbolTable table) {
        JmmNode arrayVarNode = accExpr.getChild(0);
        Type arrayType = getVarExprType(arrayVarNode, table);
        return new Type(arrayType.getName(), false);
    }

    private static Type getNewArrayType(JmmNode newArrayExpr) {
        return new Type(getTypeFromTypeString(newArrayExpr.getChild(0).get("name")).getName(), true);
    }

    private static Type getArrayExprType(JmmNode arrayExpr, SymbolTable table) {
        Type type = null;
        for (JmmNode expr : arrayExpr.getChildren()) {
            if (type == null) type = getExprType(expr, table);
            else if (!type.equals(getExprType(expr, table))) return null;
        }
        assert type != null;
        return new Type(type.getName(), true);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable
    (Type sourceType, Type destinationType, SymbolTable table) {
        // if types are the same, return true
        if (sourceType.equals(destinationType)) return true;

        // if source type is file class, this class needs to extend destination type
        if (sourceType.getName().equals(table.getClassName())) {
            return table.getSuper().equals(destinationType.getName());
        }
        // if destination type is file class, this class needs to extend source type
        if (destinationType.getName().equals(table.getClassName())) {
            return table.getSuper().equals(sourceType.getName());
        }

        // if one member is an imported object, assume it's assignable
        boolean object;
        switch (sourceType.getName()) {
            case INT_TYPE_NAME, BOOLEAN_TYPE_NAME, STRING_TYPE_NAME -> object = false;
            default -> object = true;
        }
        if (object) return true;
        switch (destinationType.getName()) {
            case INT_TYPE_NAME, BOOLEAN_TYPE_NAME, STRING_TYPE_NAME -> object = false;
            default -> object = true;
        }
        if (object) return true;

        // if type to the right is assignable, return true
        if (sourceType.getOptionalObject("assignable").isPresent()
                || destinationType.getOptionalObject("assignable").isPresent()) return true;

        return false;
    }

    public static Type getTypeFromTypeString(String typeString) {
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

    public static Type getTypeFromString(String var, JmmNode startingNode, SymbolTable table) {
        JmmNode curr = startingNode;
        Type type;
        while (curr != null) {
            if (curr.getKind().equals(Kind.METHOD_DECL.toString())) {
                List<Symbol> params = table.getParameters(curr.get("name"));
                type = lookForSymbolInList(params, var);
                if (type != null) return type;
                List<Symbol> locals = table.getLocalVariables(curr.get("name"));
                type = lookForSymbolInList(locals, var);
                if (type != null) return type;
                break;
            }
            curr = curr.getParent();
        }

        List<Symbol> fields = table.getFields();
        type = lookForSymbolInList(fields, var);
        return type;
    }
}
