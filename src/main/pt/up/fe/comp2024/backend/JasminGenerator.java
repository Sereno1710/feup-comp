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
            if(field.getFieldAccessModifier()== AccessModifier.PRIVATE){
                code.append(".field ").append("private ").append(field.getFieldName()).append(" ");
            }
            else if(field.getFieldAccessModifier() == AccessModifier.PROTECTED){
                code.append(".field ").append("protected ").append(field.getFieldName()).append(" ");
            }
            else code.append(".field ").append("public ").append(field.getFieldName()).append(" ");
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

        code.append("\n.").append(method.isFinalMethod() ? "final method" : "method ").append(modifier).append(static_m).append(methodName).append("(").append(params).append(")").append(transformType(methodType)).append(NL);

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
        } else if (methodType.toString().contains("OBJECTREF")){
            for(var imps:ollirResult.getOllirClass().getImports()){
                if(imps.endsWith("."+methodType.toString().split("\\(")[1].replace(")",""))){
                   return imps.replace(".","/") + ";";
                }
            }
            return methodType.toString().split("\\(")[1].replace(")","");

        } else if(methodType.toString().contains("STRING")){
            return "Ljava/lang/String;";
        } else if(methodType.getTypeOfElement().toString().equals("ARRAYREF")){
            return transformString(methodType.toString());
        }
        return methodType.toString();
    }

    private String transformString(String string) {
        switch (string){
            case "INT32": return "I";
            case "BOOLEAN": return "Z";
            case "STRING": return "Ljava/lang/String;";
            default:
                if(string.contains("OBJECTREF")){
                    return string.split("\\(")[1].replace(")","");
                }
                return  string;
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
        // get register

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
        var methodName = "";
        Operand first = (Operand) callInstruction.getOperands().get(0);
        if(callInstruction.getCaller().getType().getTypeOfElement().toString().equals("OBJECTREF")){
            for(var imps:ollirResult.getOllirClass().getImports()){
                if(imps.endsWith("."+callInstruction.getCaller().getType().toString().split("\\(")[1].replace(")",""))){
                    methodName = imps.replace(".","/");
                }
            }
            if(methodName.isEmpty()){
                methodName = callInstruction.getCaller().getType().toString().split("\\(")[1].replace(")","");
            }
        }
        else methodName = callInstruction.getCaller().getType().toString().split("\\(")[1].replace(")","");
        if (type.equals("NEW") ) {
            code.append("new ").append(methodName).append(NL);
            code.append("dup").append(NL);


        } else if (type.equals("invokespecial")){
            code.append(generateOperand((Operand)callInstruction.getOperands().get(0)));
            code.append("invokespecial ").append(methodName).append("/<init>()V").append(NL);
            code.append("pop").append(NL);
        }
        else if ( type.equals("invokevirtual")){
            StringBuilder parameters= new StringBuilder();

            for (Element staticElement : callInstruction.getOperands()){
                if(staticElement.toString().equals(callInstruction.getMethodName().toString())){
                    continue;
                }
                if(staticElement instanceof LiteralElement) code.append(generateLiteral((LiteralElement) staticElement));
                else if(staticElement instanceof Operand) code.append(generateOperand((Operand) staticElement));
            }

            for(var param: callInstruction.getArguments()){
                parameters.append(transformType(param.getType()));
            }

            var methodName2 = callInstruction.getMethodName().toString().split(":")[1].split("\\.")[0].replace("\"","").replace(" ","");
            code.append(type).append(" ").append(methodName).append("/").append(methodName2).append("(").append(parameters).append(")").append(transformType(callInstruction.getReturnType())).append(NL);
        } else {
            var parameters= new StringBuilder();
            for(var param: callInstruction.getArguments()){
                parameters.append(transformType(param.getType()));
            }

            for (Element staticElement : callInstruction.getOperands()){
                if(staticElement.toString().equals(callInstruction.getMethodName().toString())){
                    continue;
                }
                if(staticElement instanceof LiteralElement) code.append(generateLiteral((LiteralElement) staticElement));
                if(staticElement instanceof Operand) code.append(generateOperand((Operand) staticElement));
            }

            code.append("invokestatic ");
            if(first.getType().toString().equals("THIS")){
                code.append(currentMethod.getOllirClass().getClassName());
            }
            else {
                for(var imp: ollirResult.getOllirClass().getImports()){
                    if(imp.endsWith(first.getName())){
                        var aux= imp.replace("\\.","/");
                        code.append(aux);
                        break;
                    }
                }
            }
            LiteralElement second = (LiteralElement) callInstruction.getOperands().get(1);
            code.append("/").append(second.getLiteral().replace("\"",""))
                    .append("(")
                    .append(parameters)
                    .append(")")
                    .append(transformType(callInstruction.getReturnType()))
                    .append(NL);
            if(!callInstruction.getReturnType().toString().equals("VOID")){
                code.append("pop").append(NL);
            }
        }
        return code.toString();
    }
}
