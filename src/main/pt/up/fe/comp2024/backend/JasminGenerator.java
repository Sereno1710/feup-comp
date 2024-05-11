package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2024.analysis.passes.ReturnType;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.management.OperationsException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;
    List<Report> reports;

    String code;

    Method currentMethod;
    ClassUnit classUnit;
    private final FunctionClassMap<TreeNode, String> generators;
    private final int limits_stack = 0;
    private final int limits_locals = 0;
    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        classUnit = this.ollirResult.getOllirClass();
        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateCallInstruction);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }
        
        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();
        // generate class name
        var className = classUnit.getClassName();
        var modifier = classUnit.getClassAccessModifier() != AccessModifier.DEFAULT ?
                classUnit.getClassAccessModifier().name().toLowerCase() + " " :
                "public ";
        code.append(".class ").append(modifier).append(className).append(NL);

        code.append(".super ");
        if (classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")) {
            code.append("java/lang/Object").append(NL);

        } else {
            code.append(classUnit.getSuperClass().replace("\\.","/")).append(NL);
        }
        code.append(NL);
        var fieldsList = classUnit.getFields();
        for(var field: fieldsList){
            var final_f="";
            var static_f="";
            if(field.isFinalField()){
                final_f= "final ";
            }
            if(field.isStaticField()){
                static_f="static ";
            }
            var modifier_f = field.getFieldAccessModifier() != AccessModifier.DEFAULT ?
                    field.getFieldAccessModifier().name().toLowerCase() + " " :
                    "";
            code.append(".field ").append(modifier_f).append(final_f).append(static_f).append(field.getFieldName()).append(" ");

            code.append(transformType(field.getFieldType())).append(NL);
        }
        // generate a single constructor method
        if(classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")) {
            var defaultConstructor = """
                    ;default constructor
                    .method public <init>()V
                        aload_0
                        invokespecial java/lang/Object/<init>()V
                        return
                    .end method
                    """;
            code.append(defaultConstructor);
        }
        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }
            
            code.append(generators.apply(method));
        }
        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();
        var methodType = method.getReturnType();
        // TODO: Hardcoded param types and return type, needs to be expanded
        var methodParameters = method.getParams();
        var params=new StringBuilder();
        for(var param: methodParameters){
            params.append(transformType(param.getType()));
        }

        var static_m="";
        if(method.isStaticMethod()){
            static_m="static ";
        }
        var final_m="";

        code.append("\n.").append(method.isFinalMethod() ? "final method" : "method ").append(modifier).append(static_m).append(final_m).append(methodName).append("(").append(params).append(")").append(transformType(methodType)).append(NL);
        code.append(NL);
        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
            if(inst.getInstType() == InstructionType.CALL && !(((CallInstruction) inst).getReturnType().toString().equals("VOID")))
                code.append(TAB).append("pop").append(NL);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String transformType(Type methodType) {
        var code= new StringBuilder();
        if (methodType.getTypeOfElement() == ElementType.OBJECTREF){
            code.append("L").append(getImportedClassName(((ClassType)methodType).getName()).replace(".","/")).append(";");
            return code.toString();
        } else if(methodType.getTypeOfElement() == ElementType.ARRAYREF){
            return "[" + transformArray(((ArrayType) methodType).getElementType()) + ";";
        } else if (methodType.getTypeOfElement() == ElementType.STRING){
            return "Ljava/lang/String;";
        }
        return transformString(methodType.toString());
    }
    private String transformArray(Type methodType ) {
        var code = new StringBuilder();
        if (methodType.getTypeOfElement() == ElementType.OBJECTREF) {
            code.append(getImportedClassName(methodType.getTypeOfElement().toString()));
            return code.toString();
        }
        else if(methodType.getTypeOfElement() == ElementType.ARRAYREF){
            return "[" + transformArray(((ArrayType) methodType).getElementType());
        }
        switch (methodType.toString()){
            case "INT32": return "I";
            case "BOOLEAN": return "Z";
            case "STRING": return "Ljava/lang/String";
            case "VOID": return "V";
            default: return null;
        }
    }
    private String transformString(String string) {
        switch (string){
            case "INT32": return "I";
            case "BOOLEAN": return "Z";
            case "VOID": return "V";
            default: return null;
        }
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (lhs instanceof ArrayOperand) {
            code.append("aload ").append(currentMethod.getVarTable().get(((ArrayOperand) lhs).getName()).getVirtualReg()).append(NL);
            var temp = ((ArrayOperand) lhs).getIndexOperands().get(0);
            code.append(loadVar(temp));
            code.append(generateInstruction(assign.getRhs()));
        }
        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        if(operand.getType().getTypeOfElement().toString().equals("INT32") | operand.getType().getTypeOfElement().toString().equals("BOOLEAN")){
            code.append("istore ").append(reg).append(NL);
        }
        else code.append("astore ").append(reg).append(NL);

        return code.toString();
    }
    private String generateInstruction(Instruction inst){
        var code = new StringBuilder();
        switch (inst.getInstType()){
            case ASSIGN -> code.append(generateAssign((AssignInstruction) inst));
            case BINARYOPER -> code.append(generateBinaryOp((BinaryOpInstruction) inst));
            case CALL -> code.append(generateCallInstruction((CallInstruction) inst));
            case GETFIELD -> code.append(generateGetField((GetFieldInstruction) inst));
            case PUTFIELD -> code.append(generatePutField((PutFieldInstruction) inst));
            case RETURN -> code.append(generateReturn((ReturnInstruction) inst));
        }
        return code.toString();
    }
    private String loadVar(Element element){
        var code = new StringBuilder();
        if(element instanceof LiteralElement) code.append(generateLiteral((LiteralElement) element)).append(NL);
        else if (element instanceof  ArrayOperand) code.append(generateArrayElement((ArrayOperand) element)).append(NL);
        else if (element instanceof Operand) code.append(generateOperand((Operand) element)).append(NL);
        return code.toString();
    }
    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateArrayElement(ArrayOperand array) {
        var code = new StringBuilder();
        var reg = currentMethod.getVarTable().get(array.getName()).getVirtualReg();
        code.append(TAB).append("aload ").append(reg).append(NL);
        code.append(TAB).append(loadVar(array.getIndexOperands().get(0))).append(NL).append(TAB).append("iaload");
        return "";
    }
    private String generateLiteral(LiteralElement literal) {
        var code=new StringBuilder();
        if(literal.getType().getTypeOfElement().toString().equals("INT32")|| literal.getType().getTypeOfElement().toString().equals("BOOLEAN")){
            int value = Integer.parseInt(literal.getLiteral());

            if(value >= -1 && value <=5)  code.append("iconst_");
            else if(value >= -128 && value<=127) code.append("bipush ");
            else if(value >= -32768 && value <= 32767) code.append("sipush ");
            else {code.append("ldc ");}

            code.append(value == -1 ?  "m1" : value).append(NL);
        }
        return code.toString();
    }

    private String generateOperand(Operand operand) {

        if(operand.getType().getTypeOfElement().toString().equals("THIS")) return "aload_0"+ NL;
        else if(operand.getType().getTypeOfElement().toString().equals("INT32") | operand.getType().getTypeOfElement().toString().equals("BOOLEAN")) {
            var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
            return "iload " + reg +NL;}
        else if(operand.getType().getTypeOfElement().toString().equals("STRING") || operand.getType().getTypeOfElement().toString().equals("OBJECTREF") || operand.getType().getTypeOfElement().toString().equals("ARRAYREF")) {
            var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
            return "aload " + reg + NL;
        }
        return "";
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case MUL -> "imul";
            case DIV -> "idiv";
            case ADD -> "iadd";
            case SUB -> "isub";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }
    private String generatePutField(PutFieldInstruction putField) {
        var code = new StringBuilder();

        code.append(generators.apply(putField.getObject()));

        code.append(generators.apply(putField.getValue()));

        var className = this.ollirResult.getOllirClass().getClassName();
        var fieldName = putField.getField().getName();
        var fieldType = transformType(putField.getField().getType());
        code.append("putfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getField) {
        var code = new StringBuilder();


        code.append(generators.apply(getField.getObject()));

        var className = this.ollirResult.getOllirClass().getClassName();
        var fieldName = getField.getField().getName();
        var fieldType = transformType(getField.getField().getType());
        code.append("getfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);


        return code.toString();
    }
    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        if(returnInst.getOperand() == null){
            code.append("return").append(NL);
        }
        else if(returnInst.getReturnType().toString().equals("INT32") || returnInst.getReturnType().toString().equals("BOOLEAN")){
            code.append(generators.apply(returnInst.getOperand()));
            code.append("ireturn").append(NL);
        }
        else {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("areturn").append(NL);
        }
        return code.toString();
    }
    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();
        var type = callInstruction.getInvocationType().toString();
        var a= ((Operand) callInstruction.getCaller()).getType();
        Operand first = (Operand) callInstruction.getOperands().get(0);
        var methodName="";

        switch (type) {
            case "NEW" -> {
                if(callInstruction.getReturnType().getTypeOfElement().equals(ElementType.ARRAYREF)){
                    for(Element elem: callInstruction.getArguments()){
                        code.append(loadVar(elem));
                    }
                    code.append(TAB).append("newarray int").append(NL);
                    break;
                }
                methodName = getImportedClassName(((Operand) callInstruction.getCaller()).getName());
                code.append("new ").append(methodName).append(NL);
            }
            case "invokespecial" -> {
                code.append(generators.apply(first));
                if(a.getTypeOfElement() == ElementType.THIS)
                    methodName = ollirResult.getOllirClass().getSuperClass();
                else {
                    methodName = getImportedClassName(((ClassType) a).getName());
                }
                StringBuilder param= new StringBuilder();
                for (var arg : callInstruction.getArguments()) {
                    param.append(transformType(arg.getType()));
                }
                code.append("invokespecial ").append(methodName).append("/<init>").append("(").append(param).append(")").append(transformType(callInstruction.getReturnType())).append(NL);
            }
            case "invokevirtual" -> {
                code.append(generators.apply(first));
                LiteralElement second = (LiteralElement) callInstruction.getOperands().get(1);
                StringBuilder parameters = new StringBuilder();
                for (var op : callInstruction.getArguments()) {
                    code.append(generators.apply(op));
                }
                for (var param : callInstruction.getArguments()) {
                    parameters.append(transformType(param.getType()));
                }

                var methodName2 = second.getLiteral().replace("\"","");

                code.append(type).append(" ").append(getImportedClassName(((ClassType) first.getType()).getName())).append("/").append(methodName2).append("(").append(parameters).append(")").append(transformType(callInstruction.getReturnType())).append(NL);
            }
            case "invokestatic" -> {
                code.append(generators.apply(first));
                LiteralElement second = (LiteralElement) callInstruction.getOperands().get(1);
                var parameters = new StringBuilder();
                for (var op : callInstruction.getArguments()) {
                    code.append(generators.apply(op));
                }
                for (var param : callInstruction.getArguments()) {
                    parameters.append(transformType(param.getType()));
                }

                code.append("invokestatic ");
                code.append(getImportedClassName(((Operand) callInstruction.getCaller()).getName()));
                code.append("/").append(second.getLiteral().replace("\"", ""))
                        .append("(")
                        .append(parameters)
                        .append(")")
                        .append(transformType(callInstruction.getReturnType()))
                        .append(NL);
            }
            case "arraylength" -> {
                code.append(loadVar(callInstruction.getCaller())).append("arraylength").append(NL);
            }
            default ->{
                throw new NotImplementedException("Not supported: " + callInstruction.getInvocationType());
            }
        }
        return code.toString();
    }
    private String getImportedClassName(String className) {

        if (className.equals("this"))
            return classUnit.getClassName();

        for (String imported : this.classUnit.getImports()) {
            if (imported.endsWith(className)) {
                return imported.replace(".","/");
            }
        }

        return className;
    }
}
