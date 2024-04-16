package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

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
        classUnit = classUnit;
        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
        code.append(".super java/lang/Object").append(NL);
        var fieldsList = ollirResult.getOllirClass().getFields();
        for(var field: fieldsList){
            var field_txt="";
            if(field.getFieldAccessModifier() != AccessModifier.DEFAULT){
                field_txt = field.getFieldAccessModifier().name().toLowerCase()+ field.getFieldName() + " ";
            }
            else field_txt= "public "+ field.getFieldName() + " ";
            code.append(".field ").append(field_txt).append(transformType(field.getFieldType())).append(NL);
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
        classUnit= null;
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
        var params="";
        for(var param: methodParameters){
            var par="";
            if(param.getType().getTypeOfElement().toString().equals("ARRAYREF")){
                    par+="["+ transformType(param.getType());

            }
            else if(param.getType().getTypeOfElement().toString().equals("INT32")){
                par+="I";
            }
            else if(param.getType().getTypeOfElement().toString().equals("BOOLEAN")) {
                par += "Z";
            }
            else{
                par += transformType(param.getType());
            }
            params+=par;
        }
        var static_m="";
        if(method.isStaticMethod()){
            static_m="static ";
        }

        code.append("\n."+(method.isFinalMethod()? "final method" :"method ")).append(modifier).append(static_m).append(methodName).append("(").append(params).append(")").append(transformType(methodType)).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String transformType(Type methodType) {
        if(methodType.toString().matches("INT32")){
            return "I";
        }
        else if(methodType.toString().matches("BOOLEAN")){
            return "Z";
        }
        else if(methodType.toString().matches("VOID")){
            return "V";
        } else if (methodType.toString().contains("STRING")){
           return "Ljava/lang/String;";
        }
        return methodType.toString();
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
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if(operand.getType().getTypeOfElement().toString().equals("INT32") | operand.getType().getTypeOfElement().toString().equals("BOOLEAN"))
            return "iload " + reg +NL;
        return "aload " + reg + NL;
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

        // Generate code for loading the object reference
        code.append(generators.apply(putField.getObject()));

        // Generate code for loading the value to be stored in the field
        code.append(generators.apply(putField.getValue()));

        // Generate putfield instruction
        var className = this.ollirResult.getOllirClass().getClassName();
        var fieldName = putField.getField().getName();
        var fieldType = transformType(putField.getField().getType());
        code.append("putfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getField) {
        var code = new StringBuilder();

        // Generate code for loading the object reference
        code.append(generators.apply(getField.getObject()));

        // Generate getfield instruction
        var className = this.ollirResult.getOllirClass().getClassName();
        var fieldName = getField.getField().getName();
        var fieldType = transformType(getField.getField().getType());
        code.append("getfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);

        // Optionally, you may need to store the retrieved value in a local variable if it's going to be used later

        return code.toString();
    }
    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        if(returnInst.getOperand() == null){
            code.append("return").append(NL);
        }
        else {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("ireturn").append(NL);
        }
        return code.toString();
    }
    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();
        var type = callInstruction.getInvocationType().toString();
        var methodName = callInstruction.getCaller().getType().toString().split("\\(")[1].replace(")","");

        if (type.equals("NEW")) {
            // For NEW invocation types, generate new and dup instructions
            code.append("new ").append(methodName).append(NL);
            code.append("dup").append(NL);

        } else {
            var methodName2 = callInstruction.getMethodName().toString().split(":")[1].split("\\.")[0].replace("\"","").replace(" ","");
            // For other invocation types, generate the appropriate instruction with the method signature
            code.append(type).append(" ").append(methodName).append("/").append(methodName2).append("()V").append(NL);
        }

        return code.toString();
    }
}
