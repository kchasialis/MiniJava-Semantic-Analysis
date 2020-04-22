package ClassDefinitions;

import syntaxtree.*;
import visitor.GJDepthFirst;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

/**
 * Each Class Definition has:
 * 1) Class Declaration, consists of :
 * --1.1) name, extends/name
 * 2) Class Body, consists of :
 * --2.1) fields, consist of :
 * --2.1.1) type identifier
 * --2.2) methods, consists of :
 * --2.2.1) declaration (type identifier parameters)
 * --2.2.2) body (variable declarations, statements, expression)
 */


class Argument {
    public SimpleEntry<ClassIdentifier, ClassBody> currentClass;
}


public class ClassDefinitions extends GJDepthFirst<String, Argument> {

    /*Class identifier and body*/
    private Map<ClassIdentifier, ClassBody> definitions;

    public ClassDefinitions() {
        this.definitions = new HashMap<ClassIdentifier, ClassBody>();
    }

    public Map<ClassIdentifier, ClassBody> getDefinitions() {
        return this.definitions;
    }

    private void addFieldToClassBody(String varDeclaration, ClassBody classBody) {
        String[] tokens = varDeclaration.split("::");

        ClassField classField = new ClassField(tokens[1], tokens[0]);
        if (!classBody.getFields().containsKey(classField)) {
            classBody.addField(classField);
        }
        else {
            throw new RuntimeException("Redeclaration of a variable in class");
        }
    }

    private boolean isIdentical(ClassMethodDeclaration lhs, ClassMethodDeclaration rhs) {
        if (lhs.getIdentifier().equals(rhs.getIdentifier())) {
            if (lhs.getReturnType().equals(rhs.getReturnType())) {

                List<MethodParameter> lhsParameters = new ArrayList<MethodParameter>(lhs.getParameters().keySet().size());
                List<MethodParameter> rhsParameters = new ArrayList<MethodParameter>(rhs.getParameters().keySet().size());
                lhsParameters.addAll(lhs.getParameters().keySet());
                rhsParameters.addAll(rhs.getParameters().keySet());

                return lhsParameters.equals(rhsParameters);
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    private void addMethodToClassBody(ClassMethodDeclaration classMethodDeclaration, ClassMethodBody classMethodBody, Argument argu) {
        if (argu.currentClass.getKey().getExtendsClassName() != null) {
            ClassIdentifier temp = new ClassIdentifier(argu.currentClass.getKey().getExtendsClassName());

            if (definitions.get(temp).getMethods().containsKey(new ClassMethodDeclaration(classMethodDeclaration.getIdentifier(), null))) {
                Set<ClassMethodDeclaration> parentDeclarations = definitions.get(temp).getMethods().keySet();
                boolean foundIdentical = false;

                for (ClassMethodDeclaration parentDeclaration : parentDeclarations) {
                    boolean identical = isIdentical(classMethodDeclaration, parentDeclaration);
                    if (identical) {
                        foundIdentical = true;
                        break;
                    }
                }

                if (foundIdentical) {
                    argu.currentClass.getValue().addMethod(classMethodDeclaration, classMethodBody);
                }
                else {
                    throw new RuntimeException("Method " + classMethodDeclaration.getIdentifier() + " is also defined in superclass with different type / parameters");
                }
            }
            else {
                argu.currentClass.getValue().addMethod(classMethodDeclaration, classMethodBody);
            }
        }
        else {
            argu.currentClass.getValue().addMethod(classMethodDeclaration, classMethodBody);
        }
    }

    private void addParametersToClassMethodDeclaration(String parameters, ClassMethodDeclaration classMethodDeclaration) {
        String[] parameterList = parameters.split(",");

        for (int i = 0 ; i < parameterList.length ; i++) {
            String[] tokens = parameterList[i].split("::");

            MethodParameter methodParameter = new MethodParameter(tokens[1], tokens[0]);

            if (!classMethodDeclaration.getParameters().containsKey(methodParameter)) {
                classMethodDeclaration.addToParameters(methodParameter);
            }
            else {
                throw new RuntimeException("Redeclaration of a parameter in method " + classMethodDeclaration.getIdentifier());
            }
        }
    }

    private void addFieldToClassMethodBody(String varDeclaration, ClassMethodBody classMethodBody, ClassMethodDeclaration classMethodDeclaration) {
        String[] tokens = varDeclaration.split("::");

        MethodField methodField = new MethodField(tokens[1], tokens[0]);
        if (!classMethodBody.getFields().containsKey(methodField) && !classMethodDeclaration.getParameters().containsKey(methodField)) {
            classMethodBody.addField(methodField);
        }
        else {
            throw new RuntimeException("Redeclaration of a local variable in method " + classMethodDeclaration.getIdentifier());
        }
    }


    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, Argument argu) {
        n.f0.accept(this, argu);

        /*Accept all type declarations and fill class bodies*/
        for (int i = 0 ; i < n.f1.size() ; i++) {
            n.f1.elementAt(i).accept(this, argu);
        }

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n, Argument argu) {
        String _ret = null;

        argu = new Argument();

        String ide = n.f1.accept(this, argu);

        ClassBody mainClassBody = new ClassBody();

        ClassIdentifier classIdentifier = new ClassIdentifier(ide);

        if (!definitions.containsKey(classIdentifier)) {
            definitions.put(classIdentifier, mainClassBody);
        }
        else {
            throw new RuntimeException("Redefinitions of main class / method");
        }

        ClassMethodDeclaration classMethodDeclaration = new ClassMethodDeclaration("main", "void");

        ClassMethodBody classMethodBody = new ClassMethodBody();

        argu.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, mainClassBody);

        for (int i = 0 ; i < n.f14.size() ; i++) {
            String varDeclaration = n.f14.elementAt(i).accept(this, argu);
            addFieldToClassMethodBody(varDeclaration, classMethodBody, classMethodDeclaration);
        }

        /*After finishing processing the entire method body, add method to our class methods*/
        addMethodToClassBody(classMethodDeclaration, classMethodBody, argu);

        return _ret;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public String visit(TypeDeclaration n, Argument argu) {
        argu = new Argument();
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, Argument argu) {
        String ide = n.f1.accept(this, argu);

        ClassIdentifier classIdentifier = new ClassIdentifier(ide);
        ClassBody classBody = new ClassBody();

        if (!definitions.containsKey(classIdentifier)) {
            definitions.put(classIdentifier, classBody);
        }
        else {
            throw new RuntimeException("Redefinition of " + classIdentifier.getClassName());
        }

        argu.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, classBody);
        /*Accept all variable declarations */
        for (int i = 0 ; i < n.f3.size() ; i++) {
            String varDeclaration = n.f3.elementAt(i).accept(this, argu);
            addFieldToClassBody(varDeclaration, argu.currentClass.getValue());
        }

        /*Accept all method declarations */
        for (int i = 0 ; i < n.f4.size() ; i++) {
            n.f4.elementAt(i).accept(this, argu);
        }

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n, Argument argu) {
        String ide = n.f1.accept(this, argu);
        String extendsIde = n.f3.accept(this, argu);

        if (!definitions.containsKey(new ClassIdentifier(extendsIde, null))) {
            throw new RuntimeException("Cannot find extends symbol " + extendsIde);
        }

        ClassIdentifier classIdentifier = new ClassIdentifier(ide, extendsIde);
        ClassBody classBody = new ClassBody(extendsIde);

        if (!definitions.containsKey(classIdentifier)) {
            definitions.put(classIdentifier, classBody);
        }
        else {
            throw new RuntimeException("Redefinition of " + classIdentifier.getClassName() + classIdentifier.getExtendsClassName());
        }

        argu.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, classBody);

        /*Accept all variable declarations */
        for (int i = 0 ; i < n.f5.size() ; i++) {
            String varDeclaration = n.f5.elementAt(i).accept(this, argu);
            addFieldToClassBody(varDeclaration, argu.currentClass.getValue());
        }

        /*Accept all method declarations */
        for (int i = 0 ; i < n.f6.size() ; i++) {
            n.f6.elementAt(i).accept(this, argu);
        }

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, Argument argu) {
        String _ret = null;

        String type = n.f0.accept(this, argu);

        String ide = n.f1.accept(this, argu);

        return type + "::" + ide;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, Argument argu) {
        String type = n.f1.accept(this, argu);
        String ide = n.f2.accept(this, argu);

        ClassMethodDeclaration classMethodDeclaration = new ClassMethodDeclaration(ide, type);

        if (argu.currentClass.getValue().getMethods().containsKey(classMethodDeclaration)) {
            throw new RuntimeException("Redefinition of method " + classMethodDeclaration.getIdentifier() + " in class " + argu.currentClass.getKey().getClassName());
        }

        ClassMethodBody classMethodBody = new ClassMethodBody();

        String parameters;
        if (n.f4.present()) {
            parameters = n.f4.accept(this, argu);
            if (parameters == null) {
                throw new RuntimeException("Wait, what?");
            }
            addParametersToClassMethodDeclaration(parameters, classMethodDeclaration);
        }

        /*Accept all declarations of a method inside the class*/
        for (int i = 0 ; i < n.f7.size() ; i++) {
            String varDeclaration = n.f7.elementAt(i).accept(this, argu);
            addFieldToClassMethodBody(varDeclaration, classMethodBody, classMethodDeclaration);
        }

        /*After finishing processing the entire method body, add method to our class methods*/
        addMethodToClassBody(classMethodDeclaration, classMethodBody, argu);

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, Argument argu) {
        String firstParam = n.f0.accept(this, argu);
        String paramTail = n.f1.accept(this, argu);

        return paramTail == null ? firstParam : firstParam + paramTail;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, Argument argu) throws RuntimeException {
        String type = n.f0.accept(this, argu);
        String ide = n.f1.accept(this, argu);

        if (type != null && ide != null) {
            return type + "::" + ide;
        }
        else {
            throw new RuntimeException("Invalid syntax on parameter list");
        }
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public String visit(FormalParameterTail n, Argument argu) {
        String retval = null;

        if (n.f0.size() == 1) {
            return n.f0.elementAt(0).accept(this, argu);
        }

        for (int i = 0 ; i < n.f0.size() ; i++) {
            retval += n.f0.elementAt(i).accept(this, argu);
        }

        return retval;
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTerm n, Argument argu) {
        String retval;

        retval = n.f0.accept(this, argu);
        retval += n.f1.accept(this, argu);

        return retval;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public String visit(Type n, Argument argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
     */
    public String visit(ArrayType n, Argument argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, Argument argu) {
        return "boolean[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n, Argument argu) {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, Argument argu) {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, Argument argu) {
        return "int";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, Argument argu) {
        return n.f0.toString();
    }
}
