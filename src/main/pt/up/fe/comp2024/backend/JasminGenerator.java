package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
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
        code.append(".class ").append(className).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
        code.append(".super java/lang/Object").append(NL);
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
            if(field.getFieldAccessModifier()== AccessModifier.PRIVATE){
                code.append(".field ").append("private ").append(final_f).append(static_f).append(field.getFieldName()).append(" ");
            }
            else if(field.getFieldAccessModifier() == AccessModifier.PROTECTED){
                code.append(".field ").append("protected ").append(final_f).append(static_f).append(field.getFieldName()).append(" ");
            }
            else code.append(".field ").append("public ").append(final_f).append(static_f).append(field.getFieldName()).append(" ");
            code.append(transformType(field.getFieldType())).append(NL);
        }
        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);

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
        if(method.isFinalMethod()){
            final_m="final ";
        }

        code.append("\n.").append(method.isFinalMethod() ? "final method" : "method ").append(modifier).append(static_m).append(final_m).append(methodName).append("(").append(params).append(")").append(transformType(methodType)).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
            if(inst.getInstType() == InstructionType.CALL && !(((CallInstruction) inst).getReturnType().getTypeOfElement() == ElementType.VOID))
                code.append("pop").append(NL);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String transformType(Type methodType) {
        var code= new StringBuilder();
        if (methodType.getTypeOfElement() == ElementType.OBJECTREF){
            code.append("L");
            if(methodType.getTypeOfElement() == ElementType.THIS){
                code.append(ollirResult.getOllirClass().getClassName()).append(";");
                return code.toString();
            }
            else {
                for (String importedClass : ollirResult.getOllirClass().getImports()) {
                    if (importedClass.endsWith(((ClassType) methodType).getName())) {
                        return (((ClassType) methodType).getName()).replace("\\.","/");
                    }
                }
            }


        } else if(methodType.getTypeOfElement() == ElementType.ARRAYREF){
            return "[" + transformType(((ArrayType) methodType).getElementType());
        }
        return transformString(methodType.toString());
    }

    private String transformString(String string) {
        switch (string){
            case "INT32": return "I";
            case "BOOLEAN": return "Z";
            case "STRING": return "Ljava/lang/String;";
            case "VOID": return "V";
            default: return "";
        }
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
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

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
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
        else if (literal.getType().getTypeOfElement().toString().equals("STRING") || literal.getType().getTypeOfElement().toString().equals("OBJECTREF") || literal.getType().getTypeOfElement().toString().equals("ARRAYREF"))
            code.append("ldc ").append(literal.getLiteral()).append(NL);
        return code.toString();
    }

    private String generateOperand(Operand operand) {

        if(operand.getType().getTypeOfElement().toString().equals("THIS")) return "aload 0"+ NL;
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
                for (Element elem : callInstruction.getArguments())
                    code.append(generators.apply(elem));
                methodName = getImportedClassName(((Operand) callInstruction.getCaller()).getName());
                code.append("new ").append(methodName).append(NL);
                code.append("dup").append(NL);
            }
            case "invokespecial" -> {
                code.append(generators.apply(first)).append(NL);
                if(a.getTypeOfElement() == ElementType.THIS)
                    methodName = ollirResult.getOllirClass().getSuperClass();
                else {
                    methodName = getImportedClassName(((ClassType) a).getName());
                }
                var param="";
                for (var arg : callInstruction.getArguments()) {
                    param+=transformType(arg.getType());
                }
                code.append(generateOperand((Operand) callInstruction.getOperands().get(0)));
                code.append("invokespecial ").append(methodName).append("/<init>").append("(").append(param).append(")").append(transformType(callInstruction.getReturnType())).append(NL);
                code.append("pop").append(NL);
            }
            case "invokevirtual" -> {
                code.append(generators.apply(first)).append(NL);
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
                code.append(generators.apply(first)).append(NL);
                LiteralElement second = (LiteralElement) callInstruction.getOperands().get(1);
                var parameters = new StringBuilder();
                for (var op : callInstruction.getArguments()) {
                    code.append(generators.apply(op));
                }
                for (var param : callInstruction.getArguments()) {
                    parameters.append(transformType(param.getType()));
                }

                code.append("invokestatic ");
                code.append(getImportedClassName(generators.apply(callInstruction.getCaller())));
                code.append("/").append(second.getLiteral().replace("\"", ""))
                        .append("(")
                        .append(parameters)
                        .append(")")
                        .append(transformType(callInstruction.getReturnType()))
                        .append(NL);
            }
            default ->{
                throw new NotImplementedException("Not supported: " + callInstruction.getInvocationType());
            }
        }
        return code.toString();
    }
    private String getImportedClassName(String className) {

        if (className.equals("this"))
            return ollirResult.getOllirClass().getClassName();

        for (String imported : this.classUnit.getImports()) {
            if (imported.endsWith(className)) {
                return imported.replace("\\.","/");
            }
        }

        return className;
    }
}
