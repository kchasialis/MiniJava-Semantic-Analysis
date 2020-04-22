package SemanticAnalyzer;

import syntaxtree.*;
import visitor.GJDepthFirst;
import ClassDefinitions.*;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;


/**
 * A class method has a declaration and a body
 * 1) Declaration contains:
 * --1.1) type identifier parameters
 * 2) Body contains:
 * --2.1) Variable declarations (we only care for these now)
 * --2.2) Statements
 * --2.3) Expression
 * */

class Argument {
    public SimpleEntry<ClassMethodDeclaration, ClassMethodBody> currentMethod;
    public SimpleEntry<ClassIdentifier, ClassBody> currentClass;
    public int currentParameter;
    public List<MethodParameter> parameters;
    public boolean performCheck; //Let identifier know that it should not do anything for type check, just accept
}

class ObjectType {
    public String identifier;
    public SimpleEntry<String, Set<String> > customType;
    public String primitiveType;
    public boolean isPrimitive;

    ObjectType() {
        this.identifier = null;
        this.primitiveType = null;
        this.customType = null;
        this.isPrimitive = false;
    }

    ObjectType(String primitiveType) {
        this.isPrimitive = true;
        this.primitiveType = primitiveType;
    }

    public String getType() {
        if (this.isPrimitive) {
            return this.primitiveType;
        }
        else {
            return this.customType.getKey();
        }
    }

    private static void fillCustomObject(ClassIdentifier classIdentifier, ClassDefinitions classDefinitions, ObjectType objectType) {
        String extendsIdentifier = classDefinitions.getDefinitions().get(classIdentifier).getExtendsClassName();
        if (extendsIdentifier != null) {
            objectType.customType.getValue().add(extendsIdentifier);
            fillCustomObject(new ClassIdentifier(extendsIdentifier), classDefinitions, objectType);
        }
    }

    public static ObjectType createCustomObject(String objectName, ClassDefinitions classDefinitions) {
        ObjectType returnObject = new ObjectType();
        returnObject.isPrimitive = false;
        returnObject.customType = new SimpleEntry<String, Set<String>>(objectName, new HashSet<String>());
        fillCustomObject(new ClassIdentifier(objectName), classDefinitions, returnObject);

        return returnObject;
    }

    private boolean isDerivedOf(ObjectType rhsObject) {
        return rhsObject.customType.getValue().contains(this.customType.getKey());
    }


    /**
     * IMPORTANT!!!
     * This wont work properly if the derived class is NOT the rhs object
     **/
    public boolean equals(ObjectType rhsObject) {

        if (rhsObject == null) {
            return false;
        }

        if (!rhsObject.isPrimitive && !this.isPrimitive) {
            /*If both types are custom objects */
            return rhsObject.isDerivedOf(this);
        }
        else if (!rhsObject.isPrimitive) {
            return false;
        }
        else if (!this.isPrimitive) {
            return false;
        }
        else {
            /*If both types are primitives*/
            return rhsObject.primitiveType.equals(this.primitiveType);
        }
    }

    /**
     * "this" object must be a derived class of the method parameter
     */

    public boolean equals(MethodParameter methodParameter) {
        String type = methodParameter.getType();
        boolean isCustomObject = !type.equals("int") && !type.equals("int[]") && !type.equals("boolean") && !type.equals("boolean[]");

        if (isCustomObject && !this.isPrimitive) {
            /*If both types are custom objects check if the expression is a derived class of the declared parameter*/
            return this.customType.getValue().contains(type);
        }
        else if (isCustomObject) {
            return false;
        }
        else if (!this.isPrimitive) {
            return false;
        }
        else {
            /*If both types are primitives*/
            return type.equals(this.primitiveType);
        }
    }

    public boolean equals(String primitiveType) {
        return this.primitiveType.equals(primitiveType);
    }

}

public class SemanticAnalyzer extends GJDepthFirst<ObjectType, Argument> {

    private ClassDefinitions classDefinitions;
    private List<String> errorMessages;

    public SemanticAnalyzer(ClassDefinitions classDefinitions) {
        this.classDefinitions = classDefinitions;
        this.errorMessages = new ArrayList<String>();
    }

    public void printErrors() {
        if (this.errorMessages.size() > 0) {
            for (int i = 0; i < this.errorMessages.size(); i++) {
                System.err.println(this.errorMessages.get(i));
            }
            throw new RuntimeException("Semantic analysis failed");
        }
    }

    private boolean isArray(String array) {
        if (array == null) {
            return false;
        }

        return array.endsWith("[]");
    }


    private String checkCurrentClass(String identifier, Argument argu) {
        ClassField classField = argu.currentClass.getValue().getFields().get(new ClassField(identifier, null));
        if (classField != null) {
            return classField.getType();
        }
        else {
            return null;
        }
    }

    private String checkParents(String identifier, String extendsClassName, ClassDefinitions classDefinitions) {
        if (extendsClassName != null) {
            ClassIdentifier temp = new ClassIdentifier(extendsClassName);
            ClassField classField = classDefinitions.getDefinitions().get(temp).getFields().get(new ClassField(identifier, null));
            if (classField != null) {
                return classField.getType();
            }
            return checkParents(identifier, classDefinitions.getDefinitions().get(temp).getExtendsClassName(), classDefinitions);
        }
        return null;
    }

    private boolean isCustomType(String type) {
        return !type.equals("int") && !type.equals("int[]") && !type.equals("boolean") && !type.equals("boolean[]");
    }

    private ClassMethodDeclaration containsMethod(String methodIdentifier, String currentClassName, ClassDefinitions classDefinitions) {
        if (currentClassName != null) {
            ClassIdentifier classIdentifier = new ClassIdentifier(currentClassName);
            for (ClassMethodDeclaration classMethodDeclaration : classDefinitions.getDefinitions().get(classIdentifier).getMethods().keySet()) {
                if (classMethodDeclaration.equals(new ClassMethodDeclaration(methodIdentifier, null))) {
                    return classMethodDeclaration;
                }
            }

            if (classDefinitions.getDefinitions().get(classIdentifier).getExtendsClassName() != null) {
                return containsMethod(methodIdentifier, classDefinitions.getDefinitions().get(classIdentifier).getExtendsClassName(), classDefinitions);
            }
        }
        this.errorMessages.add("Method identifier " + methodIdentifier + " was not found");
        return null;
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public ObjectType visit(Goal n, Argument argu) {
        argu = new Argument();
        n.f0.accept(this, argu);

        for (int i = 0 ; i < n.f1.size() ; i++) {
            n.f1.elementAt(i).accept(this, null);
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
    public ObjectType visit(MainClass n, Argument argu) {
        argu.performCheck = false;
        ObjectType objectType = n.f1.accept(this, argu);

        if (objectType == null) {
            return null;
        }

        ClassIdentifier classIdentifier = new ClassIdentifier(objectType.identifier);
        ClassBody classBody = this.classDefinitions.getDefinitions().get(classIdentifier);

        if (classBody == null) {
            throw new RuntimeException("This was not supposed to happen");
        }

        argu.performCheck = true;
        argu.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, classBody);

        for (int i = 0 ; i < n.f14.size() ; i++) {
            n.f14.elementAt(i).accept(this, argu);
        }

        ClassMethodDeclaration classMethodDeclaration = new ClassMethodDeclaration("main", "void");
        for (ClassMethodDeclaration methodDeclaration : argu.currentClass.getValue().getMethods().keySet()) {
            if (methodDeclaration.equals(classMethodDeclaration)) {
                classMethodDeclaration = methodDeclaration;
            }
        }
        ClassMethodBody classMethodBody = argu.currentClass.getValue().getMethods().get(classMethodDeclaration);

        argu.currentMethod = new SimpleEntry<ClassMethodDeclaration, ClassMethodBody>(classMethodDeclaration, classMethodBody);

        for (int i = 0 ; i < n.f15.size() ; i++) {
            n.f15.elementAt(i).accept(this, argu);
        }

        return null;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public ObjectType visit(TypeDeclaration n, Argument argu) {
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
    public ObjectType visit(ClassDeclaration n, Argument argu) {
        argu.performCheck = false;
        ObjectType objectType = n.f1.accept(this, argu);

        if (objectType == null) {
            return null;
        }

        ClassIdentifier classIdentifier = new ClassIdentifier(objectType.identifier);
        ClassBody classBody = this.classDefinitions.getDefinitions().get(classIdentifier);

        if (classBody == null) {
            throw new RuntimeException("This was not supposed to happen");
        }

        argu.performCheck = true;

        argu.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, classBody);

        for (int i = 0 ; i < n.f3.size() ; i++) {
            n.f3.elementAt(i).accept(this, argu);
        }

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
    public ObjectType visit(ClassExtendsDeclaration n, Argument argu) {
        argu.performCheck = false;
        ObjectType object = n.f1.accept(this, argu);
        ObjectType extendsObject = n.f3.accept(this, argu);

        if (object == null || extendsObject == null) {
            return null;
        }

        ClassIdentifier classIdentifier = new ClassIdentifier(object.identifier, extendsObject.identifier);
        ClassBody classBody = this.classDefinitions.getDefinitions().get(classIdentifier);

        if (classBody == null) {
            throw new RuntimeException("This was not supposed to happen");
        }

        argu.performCheck = true;

        argu.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, classBody);
        argu.currentMethod = null;

        for (int i = 0 ; i < n.f5.size() ; i++) {
            n.f5.elementAt(i).accept(this, argu);
        }

        for (int i = 0 ; i < n.f6.size() ; i++) {
            n.f6.elementAt(i).accept(this, argu);
        }

        return null;
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
    public ObjectType visit(MethodDeclaration n, Argument argu) {
        argu.performCheck = true;
        ObjectType returnType = n.f1.accept(this, argu);
        argu.performCheck = false;
        ObjectType methodIdentifier = n.f2.accept(this, argu);

        if (returnType == null || methodIdentifier == null) {
            return null;
        }

        argu.performCheck = true;
        argu.currentMethod = null;
        if (n.f4.present()) {
            n.f4.accept(this, argu);
        }

        for (int i = 0 ; i < n.f7.size() ; i++) {
            n.f7.elementAt(i).accept(this, argu);
        }

        ClassMethodDeclaration classMethodDeclaration = new ClassMethodDeclaration(methodIdentifier.identifier, null);
        for (ClassMethodDeclaration methodDeclaration : argu.currentClass.getValue().getMethods().keySet()) {
            if (methodDeclaration.equals(classMethodDeclaration)) {
                classMethodDeclaration = methodDeclaration;
            }
        }
        ClassMethodBody classMethodBody = argu.currentClass.getValue().getMethods().get(classMethodDeclaration);

        argu.currentMethod = new SimpleEntry<ClassMethodDeclaration, ClassMethodBody>(classMethodDeclaration, classMethodBody);

        for (int i = 0 ; i < n.f8.size() ; i++) {
            n.f8.elementAt(i).accept(this, argu);
        }

        ObjectType expressionReturnType = n.f10.accept(this, argu);

        if (expressionReturnType == null) {
            return null;
        }

        if (!returnType.equals(expressionReturnType)) {
            this.errorMessages.add("Incompatible return type (" + expressionReturnType.getType() + " to " + returnType.getType() + ")");
        }

        return null;
    }


    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public ObjectType visit(VarDeclaration n, Argument argu) { return n.f0.accept(this, argu); }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public ObjectType visit(FormalParameter n, Argument argu) { return n.f0.accept(this, argu); }


    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public ObjectType visit(Block n, Argument argu) {

        for (int i = 0 ; i < n.f1.size() ; i++) {
            n.f1.elementAt(i).accept(this, argu);
        }

        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public ObjectType visit(AssignmentStatement n, Argument argu) {
        ObjectType identifierType = n.f0.accept(this, argu);

        ObjectType expressionType = n.f2.accept(this, argu);

        if (identifierType == null || expressionType == null) {
            return null;
        }

        System.out.println("Entering assignment statement");

        if (!identifierType.equals(expressionType)) {
            this.errorMessages.add("TypeError, incompatible types, cannot convert " + expressionType.getType() + " to " + identifierType.getType());
            return null;
        }
        else {
            System.out.println(identifierType.primitiveType + " = " + expressionType.primitiveType);
        }

        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public ObjectType visit(ArrayAssignmentStatement n, Argument argu) {
        ObjectType arrayType = n.f0.accept(this, argu);

        if (arrayType == null) {
            return null;
        }

        System.out.println("Entering array assignment statement");

        if (arrayType.isPrimitive) {
            if (!isArray(arrayType.primitiveType)) {
                this.errorMessages.add("TypeError, " + arrayType.primitiveType + " is not an array");
                return null;
            }

            ObjectType accessExpressionType = n.f2.accept(this, argu);
            ObjectType assignmentExpressionType = n.f5.accept(this, argu);

            if (!assignmentExpressionType.isPrimitive) {
                this.errorMessages.add("TypeError, cannot assign " + assignmentExpressionType.getType() + " object to " + arrayType.getType());
                return null;
            }

            if (!arrayType.primitiveType.substring(0, arrayType.primitiveType.length() - 2).equals(assignmentExpressionType.primitiveType)) {
                this.errorMessages.add("TypeError, cannot assign " + assignmentExpressionType.primitiveType + " to " + arrayType.primitiveType.substring(0, arrayType.primitiveType.length() - 2));
                return null;
            }
            if (!accessExpressionType.equals("int")) {
                this.errorMessages.add("TypeError, index of array access should be int");
                return null;
            }

            System.out.println(arrayType.getType() + " [ " + accessExpressionType.getType() + " ] = " + assignmentExpressionType.getType());
        }
        else {
            this.errorMessages.add("TypeError, " + arrayType.customType.getKey() + " is not an array");
        }

        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public ObjectType visit(IfStatement n, Argument argu) {
        ObjectType exprType = n.f2.accept(this, argu);

        if (exprType == null) {
            return null;
        }

        if (!exprType.equals("boolean")) {
            this.errorMessages.add("TypeError, non-boolean type on if statement");
            return null;
        }

        System.out.println("Entering if statement");

        System.out.println("Expression equals " + exprType.getType());

        n.f4.accept(this, argu);
        n.f6.accept(this, argu);

        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public ObjectType visit(WhileStatement n, Argument argu) {
        ObjectType exprType = n.f2.accept(this, argu);

        if (exprType == null) {
            return null;
        }

        if (!exprType.equals("boolean")) {
            this.errorMessages.add("TypeError, non-boolean type on while statement");
            return null;
        }

        System.out.println("Entering while statement");

        System.out.println("Expression equals " + exprType.getType());

        n.f4.accept(this, argu);

        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public ObjectType visit(AndExpression n, Argument argu) {
        ObjectType boolClauseLeft = n.f0.accept(this, argu);
        ObjectType boolClauseRight = n.f2.accept(this, argu);

        if (boolClauseLeft == null || boolClauseRight == null) {
            return null;
        }

        System.out.println("Entering && expression");

        if (boolClauseLeft.equals("boolean") && boolClauseRight.equals("boolean")) {
            System.out.println(boolClauseLeft.getType() + " && " + boolClauseRight.getType());
            return new ObjectType("boolean");
        }
        else {
            this.errorMessages.add("Invalid types on binary operator &&");
            return null;
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public ObjectType visit(CompareExpression n, Argument argu) {
        ObjectType exprType1 = n.f0.accept(this, argu);
        ObjectType exprType2 = n.f2.accept(this, argu);

        if (exprType1 == null || exprType2 == null) {
            return null;
        }

        System.out.println("Entering < expression");

        if (exprType1.equals("int") && exprType2.equals("int")) {
            System.out.println(exprType1.getType() + " < " + exprType2.getType());
            return new ObjectType("boolean");
        }
        else {
            this.errorMessages.add("Invalid types on binary operator <");
            return null;
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public ObjectType visit(PlusExpression n, Argument argu) {
        ObjectType exprType1 = n.f0.accept(this, argu);
        ObjectType exprType2 = n.f2.accept(this, argu);

        if (exprType1 == null || exprType2 == null) {
            return null;
        }

        System.out.println("Entering + expression");

        if (exprType1.equals("int") && exprType2.equals("int")) {
            System.out.println(exprType1.getType() + " + " + exprType2.getType());
            return new ObjectType("int");
        }
        else {
            this.errorMessages.add("Invalid types on binary operator +");
            return null;
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public ObjectType visit(MinusExpression n, Argument argu) {
        ObjectType exprType1 = n.f0.accept(this, argu);
        ObjectType exprType2 = n.f2.accept(this, argu);

        if (exprType1 == null || exprType2 == null) {
            return null;
        }

        System.out.println("Entering - expression");

        if (exprType2.equals("int") && exprType2.equals("int")) {
            System.out.println(exprType1.getType() + " - " + exprType2.getType());
            return new ObjectType("int");
        }
        else {
            this.errorMessages.add("Invalid types on binary operator -");
            return null;
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public ObjectType visit(TimesExpression n, Argument argu) {
        ObjectType exprType1 = n.f0.accept(this, argu);
        ObjectType exprType2 = n.f2.accept(this, argu);

        if (exprType1 == null || exprType2 == null) {
            return null;
        }

        System.out.println("Entering * expression");

        if (exprType1.equals("int") && exprType2.equals("int")) {
            System.out.println(exprType1.getType() + " * " + exprType2.getType());
            return new ObjectType("int");
        }
        else {
            this.errorMessages.add("Invalid types on binary operator *");
            return null;
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public ObjectType visit(ArrayLookup n, Argument argu) {
        ObjectType arrayType = n.f0.accept(this, argu);
        ObjectType exprType = n.f2.accept(this, argu);

        if (arrayType == null || exprType == null) {
            return null;
        }

        System.out.println("Entering array lookup expression");

        if (arrayType.isPrimitive) {
            if (isArray(arrayType.primitiveType) && exprType.equals("int")) {
                System.out.println(arrayType.getType() + " [ " + exprType.getType() + " ]");
                return new ObjectType(arrayType.primitiveType.substring(0, arrayType.primitiveType.length() - 2));
            } else {
                this.errorMessages.add("Invalid array lookup");
                return null;
            }
        }
        else {
            this.errorMessages.add("TypeError, " + arrayType.customType.getKey() + " is not an array");
            return null;
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public ObjectType visit(ArrayLength n, Argument argu) {
        ObjectType arrayType = n.f0.accept(this, argu);

        if (arrayType == null) {
            return null;
        }

        System.out.println("Entering array length expression");

        if (arrayType.isPrimitive) {
            if (isArray(arrayType.primitiveType)) {
                System.out.println(arrayType.getType());
                return new ObjectType("int");
            } else {
                this.errorMessages.add("Invalid length operator on non-array object");
                return null;
            }
        }
        else {
            this.errorMessages.add("TypeError, " + arrayType.customType.getKey() + " is not an array");
            return null;
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public ObjectType visit(MessageSend n, Argument argu) {
        argu.performCheck = true;
        ObjectType object = n.f0.accept(this, argu);

        if (object == null) {
            return null;
        }

        if (object.isPrimitive) {
            this.errorMessages.add("TypeError, cannot perform MessageSend on primitive type");
        }

        System.out.println("Entering MessageSend expression");

        argu.performCheck = false;
        ObjectType method = n.f2.accept(this, argu);

        if (method == null) {
            return null;
        }

        ClassMethodDeclaration classMethodDeclaration = containsMethod(method.identifier, argu.currentClass.getKey().getClassName(), classDefinitions);

        if (classMethodDeclaration == null) {
            this.errorMessages.add("Cannot find symbol " + method.identifier);
            return null;
        }

        argu.parameters = null;

        if (n.f4.present()) {
            argu.currentParameter = 0;
            argu.parameters = new ArrayList<MethodParameter>(classMethodDeclaration.getParameters().keySet());
            n.f4.accept(this, argu);
            argu.parameters = null;
        }

        String returnType = classMethodDeclaration.getReturnType();
        if (isCustomType(returnType)) {
            System.out.println("Returning " + ObjectType.createCustomObject(returnType , classDefinitions).getType() + " from MessageSend");
            return ObjectType.createCustomObject(returnType , classDefinitions);
        }

        ObjectType objectType = new ObjectType();

        objectType.isPrimitive = true;
        objectType.primitiveType = returnType;

        System.out.println("Returning " + objectType.getType() + " from MessageSend");
        return objectType;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public ObjectType visit(ExpressionList n, Argument argu) {
        ObjectType objectType = n.f0.accept(this, argu);

        if (objectType == null) {
            return null;
        }

        System.out.println("Entering expression list");
        System.out.print(objectType.getType() + ", ");

        if (!objectType.equals(argu.parameters.get(argu.currentParameter))) {
            this.errorMessages.add("Incompatible types on ExpressionList");
            return null;
        }
        argu.currentParameter++;

        n.f1.accept(this, argu);

        System.out.println();
        return null;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public ObjectType visit(ExpressionTail n, Argument argu) {

        for (int i = 0 ; i < n.f0.size() ; i++) {
            n.f0.elementAt(i).accept(this, argu).getType();
        }

        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public ObjectType visit(ExpressionTerm n, Argument argu) {

        ObjectType objectType = n.f1.accept(this, argu);

        if (objectType == null) {
            return null;
        }

        System.out.print(objectType.getType() + ", ");
        if (!objectType.equals(argu.parameters.get(argu.currentParameter))) {
            this.errorMessages.add("Incompatible types on ExpressionList");
        }
        argu.currentParameter++;

        return null;
    }


    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public ObjectType visit(IntegerLiteral n, Argument argu) { return new ObjectType("int"); }

    /**
     * f0 -> "true"
     */
    public ObjectType visit(TrueLiteral n, Argument argu) { return new ObjectType("boolean"); }

    /**
     * f0 -> "false"
     */
    public ObjectType visit(FalseLiteral n, Argument argu) { return new ObjectType("boolean"); }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public ObjectType visit(BooleanArrayType n, Argument argu) { return new ObjectType("boolean[]"); }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public ObjectType visit(IntegerArrayType n, Argument argu) { return new ObjectType("int[]"); }

    /**
     * f0 -> "boolean"
     */
    public ObjectType visit(BooleanType n, Argument argu) { return new ObjectType("boolean"); }
    /**
     * f0 -> "int"
     */
    public ObjectType visit(IntegerType n, Argument argu) { return new ObjectType("int"); }


    /**
     * f0 -> "this"
     */
    public ObjectType visit(ThisExpression n, Argument argu) { return ObjectType.createCustomObject(argu.currentClass.getKey().getClassName(), classDefinitions); }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public ObjectType visit(BooleanArrayAllocationExpression n, Argument argu) {
        ObjectType exprType = n.f3.accept(this, argu);

        if (exprType == null) {
            return null;
        }

        if (!exprType.equals("int")) {
            this.errorMessages.add("TypeError, cannot convert " + exprType.getType() + " to int for array allocation");
            return null;
        }

        return new ObjectType("boolean[]");
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public ObjectType visit(IntegerArrayAllocationExpression n, Argument argu) {
        ObjectType exprType = n.f3.accept(this, argu);

        if (exprType == null) {
            return null;
        }

        if (!exprType.equals("int")) {
            this.errorMessages.add("TypeError, cannot convert " + exprType.getType() + " to int for array allocation");
            return null;
        }

        return new ObjectType("int[]");
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public ObjectType visit(AllocationExpression n, Argument argu) {
        boolean tmp = argu.performCheck;
        argu.performCheck = false;
        ObjectType ide = n.f1.accept(this, argu);
        argu.performCheck = tmp;

        if (ide == null) {
            return null;
        }

        return ObjectType.createCustomObject(ide.identifier , classDefinitions);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public ObjectType visit(NotExpression n, Argument argu) {
        ObjectType clause = n.f1.accept(this, argu);

        if (clause == null) {
            return null;
        }

        if (clause.equals("boolean")) {
            return clause;
        }
        else {
            this.errorMessages.add("Invalid clause on !");
            return null;
        }
    }

    /**
     * f0 -> <IDENTIFIER>
    */
    public ObjectType visit(Identifier n, Argument argu) {
        ObjectType objectType = new ObjectType();
        objectType.identifier = n.f0.toString();

        if (!argu.performCheck) {
            return objectType;
        }

        /*If argu.currentMethod == null, it means that we are currently visiting
            an object(!) declaration, which means that we only need to check
            if the class of the object that we are about to declare, exists somewhere else in the code.
          Else if argu.currentMethod != null, it means that we are inside a method.
            Therefore, we have a variable access and we should take into account the class we are in, the fields of the method we are in
            and parameters of this method to validate if an identifier is valid and return its type.
         */
        if (argu.currentMethod == null) {
            if (!classDefinitions.getDefinitions().containsKey(new ClassIdentifier(objectType.identifier))) {
                this.errorMessages.add("Symbol " + objectType.identifier + " is not defined");
                return null;
            } else {
                return objectType;
            }
        }

        /*If we reach here, it means are accessing a variable inside a method.
          Therefore, we should return its type to the caller, after verifying this variable was declared before.*/

        /*Check method fields first (shadowing)*/
        String type;
        MethodField methodField = argu.currentMethod.getValue().getFields().get(new MethodField(objectType.identifier, null));
        if (methodField != null) {
            type = methodField.getType();
        } else {
            /*Check parameters of the method*/
            MethodParameter methodParameter = argu.currentMethod.getKey().getParameters().get(new MethodParameter(objectType.identifier, null));
            if (methodParameter != null) {
                type = methodParameter.getType();
            }
            /*Finally, check if current class or its super class contains this variable*/
            else {
                if (argu.currentClass.getKey().getExtendsClassName() != null) {
                    type = checkCurrentClass(objectType.identifier, argu);
                    if (type == null) {
                        type = checkParents(objectType.identifier, argu.currentClass.getKey().getExtendsClassName(), classDefinitions);
                        if (type == null) {
                            return null;
                        }
                    }
                }
                else {
                    type = checkCurrentClass(objectType.identifier, argu);
                    if (type == null) {
                        this.errorMessages.add("Variable " + objectType.identifier + " is not defined");
                        return null;
                    }
                }
            }
        }

        if (isCustomType(type)) {
            return ObjectType.createCustomObject(type , classDefinitions);
        }

        objectType.isPrimitive = true;
        objectType.primitiveType = type;

        return objectType;
    }
}
