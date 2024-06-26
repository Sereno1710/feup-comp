package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class SemanticAnalysisTest {

    @Test
    public void symbolTable() {

        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/SymbolTable.jmm"));
        System.out.println("Symbol Table:\n" + result.getSymbolTable().print());
    }

    @Test
    public void varNotDeclared() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarNotDeclared.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void classNotImported() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ClassNotImported.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void intPlusObject() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IntPlusObject.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void boolTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/BoolTimesInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayPlusInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayPlusInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayAccessOnInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayAccessOnInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayIndexNotInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayIndexNotInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assignIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignIntToBool.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assignObjectToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignObjectToBool.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void objectAssignmentFail() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentFail.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void objectAssignmentPassExtends() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentPassExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void objectAssignmentPassImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentPassImports.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void intInIfCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IntInIfCondition.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInWhileCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInWhileCondition.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void callToUndeclaredMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToUndeclaredMethod.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void callToMethodAssumedInExtends() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToMethodAssumedInExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToMethodAssumedInImport() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToMethodAssumedInImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void incompatibleArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IncompatibleArguments.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void incompatibleReturn() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IncompatibleReturn.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assumeArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssumeArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargs() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/Varargs.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsWithArray() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarargsWithArray.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarargsWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varargsWithArrayWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarargsWithArrayWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInit() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInit.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitWrong1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInitWrong1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInitWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInitWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void staticMethodWithThis() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/StaticMethodWithThis.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void staticMethodWithField() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/StaticMethodWithField.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void memberAccess() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MemberAccess.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void memberAccessOnInt() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MemberAccessOnInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void mainMethodWrong1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MainMethodWrong1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void mainMethodWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MainMethodWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void miscLengthAsName() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MiscLengthAsName.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void memberAccessWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MemberAccessWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void multipleReturns() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MultipleReturns.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void noReturns() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/NoReturns.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void mainEverywehereOk() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MainEverywhereOk.jmm"));
        System.out.println(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void mainImportOk() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MainImportOk.jmm"));
        System.out.println(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void duplicatedFieldInvalid() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedField.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void duplicatedImportInvalid() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedImport.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void fieldInStaticInvalid() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/FieldInStatic.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void shadowFieldParamAndLocalOk() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ShadowFieldParamAndLocal.jmm"));
        System.out.println(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void varEqualsThisOk() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarEqualsThis.jmm"));
        System.out.println(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void varEqualsThisExtendedOk() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarEqualsThisExtended.jmm"));
        System.out.println(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void variableUndefinedArrayInvalid() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VariableUndefinedArray.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void AssignIntToFunc() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignIntToFunc.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void MethodCallWithVar() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MethodCallWithVar.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void PriorityImportOverVar() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/PriorityImportOverVar.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ClassInitFuncCall() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ClassInitFuncCall.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void array1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/array1.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void basicfuncs() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/basicfuncs.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void basicprints() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/basicprints.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callhell() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/callhell.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void crazyobj() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/crazyobj.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void fields() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/fields.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void hard1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/hard1.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void HelloWorld() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/HelloWorld.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void if_() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/if.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ifhell() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ifhell.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void manyassign() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/manyassign.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void multarr() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/multarr.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void printarr() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/printarr.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void prop1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/prop1.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void prop2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/prop2.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void prop3() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/prop3.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void returnobj() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/returnobj.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void thisRet() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/thisRet.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void unused_const_fold_prop() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/unused_const_fold_prop.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void while_() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/while.jmm"));
        TestUtils.noErrors(result);
    }
}
