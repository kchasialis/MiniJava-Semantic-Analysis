package SemanticAnalyzer;

import syntaxtree.*;
import visitor.GJDepthFirst;
import ClassDefinitions.*;

import java.lang.reflect.Method;
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
    public Map<MethodParameter, MethodParameter> currentParameters;
    public Iterator<Map.Entry<MethodParameter, MethodParameter>> currentIterator;
    public int currentParameter;
    public boolean performCheck; //Let identifier know that it should not do anything for type check, just accept
}

/*This object type represents the current type we are currently working on*/
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

    /**
     * This function creates the Set of classes that our object has a 'is-a' relationship.
     * For example.
     * class A {} ... class B extends A {} ... class C extends B{} ...
     * Then C is a custom type (primitiveType is null) and its Set (SimpleEntry<String, Set<String>>) representing 'is-a' relationship is the following:
     * Set = {C, B, A};
     */
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
        returnObject.customType.getValue().add(objectName);

        fillCustomObject(new ClassIdentifier(objectName), classDefinitions, returnObject);

        return returnObject;
    }

    private boolean isDerivedOf(ObjectType rhsObject) {
        return this.customType.getValue().contains(rhsObject.customType.getKey());
    }

    /**
     * IMPORTANT!!!
     * This wont work properly if the derived class is NOT the rhs object
     **/
    public boolean equals(ObjectType rhsObject) {

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
        if (this.primitiveType == null) {
            return false;
        }
        return this.primitiveType.equals(primitiveType);
    }

}

public class SemanticAnalyzer extends GJDepthFirst<ObjectType, Argument> {

    private ClassDefinitions classDefinitions;
    private int currentLine;
    private int currentColumn;

    public SemanticAnalyzer(ClassDefinitions classDefinitions) {
        this.classDefinitions = classDefinitions;
        this.currentLine = 1;
        this.currentColumn = 1;
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
        argu.currentMethod = null;
        ObjectType returnType = n.f1.accept(this, argu);
        argu.performCheck = false;
        ObjectType methodIdentifier = n.f2.accept(this, argu);

        argu.performCheck = true;
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

        if (!returnType.equals(expressionReturnType)) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot convert " + expressionReturnType.getType() + " to " + returnType.getType() + " on return expression");
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

        this.currentLine = n.f2.beginLine;
        this.currentColumn = n.f2.beginColumn;

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

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (!identifierType.equals(expressionType)) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, incompatible types, cannot convert " + expressionType.getType() + " to " + identifierType.getType());
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

        if (arrayType.isPrimitive) {
            if (!isArray(arrayType.primitiveType)) {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, " + arrayType.primitiveType + " is not an array");
            }

            ObjectType accessExpressionType = n.f2.accept(this, argu);
            ObjectType assignmentExpressionType = n.f5.accept(this, argu);

            this.currentLine = n.f4.beginLine;
            this.currentColumn = n.f4.beginColumn;

            if (!assignmentExpressionType.isPrimitive) {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot assign " + assignmentExpressionType.getType() + " object to " + arrayType.getType());
            }

            if (!arrayType.primitiveType.substring(0, arrayType.primitiveType.length() - 2).equals(assignmentExpressionType.primitiveType)) {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot assign " + assignmentExpressionType.primitiveType + " to " + arrayType.primitiveType.substring(0, arrayType.primitiveType.length() - 2));
            }
            if (!accessExpressionType.equals("int")) {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, index of array access should be int");
            }

        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, " + arrayType.customType.getKey() + " is not an array");
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

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (!exprType.equals("boolean")) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, non-boolean type on if statement");
        }

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

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (!exprType.equals("boolean")) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, non-boolean type on while statement");
        }

        n.f4.accept(this, argu);

        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public ObjectType visit(PrintStatement n, Argument argu) {
        ObjectType exprType = n.f2.accept(this, argu);

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (!exprType.equals(new ObjectType("int"))) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot convert " + exprType.getType() + " to int at print statement");
        }

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

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (boolClauseLeft.equals("boolean") && boolClauseRight.equals("boolean")) {
            return new ObjectType("boolean");
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid types on binary operator && (" + boolClauseLeft.getType() + " and " + boolClauseRight.getType() + ")");
        }
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public ObjectType visit(BracketExpression n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public ObjectType visit(CompareExpression n, Argument argu) {
        ObjectType exprType1 = n.f0.accept(this, argu);
        ObjectType exprType2 = n.f2.accept(this, argu);

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (exprType1.equals("int") && exprType2.equals("int")) {
            return new ObjectType("boolean");
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid types on binary operator < (" + exprType1.getType() + " and " + exprType2.getType() + ")");
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

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (exprType1.equals("int") && exprType2.equals("int")) {
            return new ObjectType("int");
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid types on binary operator + (" + exprType1.getType() + " and " + exprType2.getType() + ")");
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

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (exprType2.equals("int") && exprType2.equals("int")) {
            return new ObjectType("int");
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid types on binary operator - (" + exprType1.getType() + " and " + exprType2.getType() + ")");
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

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (exprType1.equals("int") && exprType2.equals("int")) {
            return new ObjectType("int");
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid types on binary operator * (" + exprType1.getType() + " and " + exprType2.getType() + ")");
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

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (arrayType.isPrimitive) {
            if (isArray(arrayType.primitiveType) && exprType.equals("int")) {
                return new ObjectType(arrayType.primitiveType.substring(0, arrayType.primitiveType.length() - 2));
            }
            else if (!isArray(arrayType.primitiveType)) {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid array lookup, "  + arrayType.getType() + " is not an array");
            }
            else {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid array lookup, cannot convert " + exprType.getType() + " to " + arrayType.primitiveType.substring(0, arrayType.primitiveType.length() - 2));
            }
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid array lookup, " + arrayType.customType.getKey() + " is not an array");
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public ObjectType visit(ArrayLength n, Argument argu) {
        ObjectType arrayType = n.f0.accept(this, argu);

        this.currentLine = n.f1.beginLine;
        this.currentColumn = n.f1.beginColumn;

        if (arrayType.isPrimitive) {
            if (isArray(arrayType.primitiveType)) {
                return new ObjectType("int");
            } else {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid length operator on non-array object");
            }
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, " + arrayType.customType.getKey() + " is not an array");
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

        if (object.isPrimitive) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot perform MessageSend on primitive type");
        }

        argu.performCheck = false;
        ObjectType method = n.f2.accept(this, argu);

        ClassMethodDeclaration classMethodDeclaration = containsMethod(method.identifier, object.getType(), classDefinitions);

        if (classMethodDeclaration == null) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Cannot find symbol " + method.identifier);
        }

        argu.performCheck = true;

        /*We can have nested message sends.
          This means that is we call this function recursively it will set different parameters every time
          We use this tmp variable to keep our parameters from the previous call and restore them before returning
         */
        Map<MethodParameter, MethodParameter> tempParameters = argu.currentParameters;
        Iterator<Map.Entry<MethodParameter, MethodParameter>> tempIterator = argu.currentIterator;
        int tempCurrentParameter = argu.currentParameter;

        argu.currentParameters = null;
        argu.currentIterator = null;

        if (n.f4.present()) {
            argu.currentParameter = 0;
            argu.currentIterator = classMethodDeclaration.getParameters().entrySet().iterator();
            argu.currentParameters = classMethodDeclaration.getParameters();

            n.f4.accept(this, argu);

            argu.currentParameters = null;
            argu.currentIterator = null;
        }

        String returnType =  classMethodDeclaration.getReturnType();
        if (isCustomType(returnType)) {
            argu.currentParameters = tempParameters;
            argu.currentIterator = tempIterator;
            argu.currentParameter = tempCurrentParameter;
            return ObjectType.createCustomObject(returnType , classDefinitions);
        }

        ObjectType objectType = new ObjectType();

        objectType.isPrimitive = true;
        objectType.primitiveType = returnType;

        argu.currentParameters = tempParameters;
        argu.currentIterator = tempIterator;
        argu.currentParameter = tempCurrentParameter;

        return objectType;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public ObjectType visit(ExpressionList n, Argument argu) {
        ObjectType objectType = n.f0.accept(this, argu);

        if (argu.currentIterator.hasNext()) {
            Map.Entry<MethodParameter, MethodParameter> currentEntry = argu.currentIterator.next();
            if (!objectType.equals(currentEntry.getKey())) {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot convert " + objectType.getType() + " to " + currentEntry.getKey().getType() + " on expression list");
            }
        }
        argu.currentParameter++;

        n.f1.accept(this, argu);

        if (argu.currentParameter != argu.currentParameters.size()) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid method call, the number of arguments given (" + argu.currentParameter + ") is less than expected (" + argu.currentParameters.size() + ")");
        }

        return null;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public ObjectType visit(ExpressionTail n, Argument argu) {

        for (int i = 0 ; i < n.f0.size() ; i++) {
            n.f0.elementAt(i).accept(this, argu);
        }

        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public ObjectType visit(ExpressionTerm n, Argument argu) {

        ObjectType objectType = n.f1.accept(this, argu);

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (argu.currentIterator.hasNext()) {
            Map.Entry<MethodParameter, MethodParameter> currentEntry = argu.currentIterator.next();
            if (!objectType.equals(currentEntry.getKey())) {
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot convert " + objectType.getType() + " to " + currentEntry.getKey().getType() + " on expression list");
            }
           argu.currentParameter++;
        }

        return null;
    }


    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public ObjectType visit(IntegerLiteral n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return new ObjectType("int");
    }

    /**
     * f0 -> "true"
     */
    public ObjectType visit(TrueLiteral n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return new ObjectType("boolean");
    }

    /**
     * f0 -> "false"
     */
    public ObjectType visit(FalseLiteral n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return new ObjectType("boolean");
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public ObjectType visit(BooleanArrayType n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return new ObjectType("boolean[]");
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public ObjectType visit(IntegerArrayType n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return new ObjectType("int[]");
    }

    /**
     * f0 -> "boolean"
     */
    public ObjectType visit(BooleanType n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return new ObjectType("boolean");
    }
    /**
     * f0 -> "int"
     */
    public ObjectType visit(IntegerType n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return new ObjectType("int");
    }


    /**
     * f0 -> "this"
     */
    public ObjectType visit(ThisExpression n, Argument argu) {
        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;
        return ObjectType.createCustomObject(argu.currentClass.getKey().getClassName(), classDefinitions);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public ObjectType visit(BooleanArrayAllocationExpression n, Argument argu) {
        ObjectType exprType = n.f3.accept(this, argu);

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (!exprType.equals("int")) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot convert " + exprType.getType() + " to int for array allocation");
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

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (!exprType.equals("int")) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot convert " + exprType.getType() + " to int for array allocation");
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
        SimpleEntry<ClassMethodDeclaration, ClassMethodBody> tmp = argu.currentMethod;
        argu.currentMethod = null;
        ObjectType ide = n.f1.accept(this, argu);
        argu.currentMethod = tmp;

        return ObjectType.createCustomObject(ide.identifier , classDefinitions);
    }


    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public ObjectType visit(NotExpression n, Argument argu) {
        ObjectType clause = n.f1.accept(this, argu);

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (clause.equals("boolean")) {
            return clause;
        }
        else {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") Invalid clause on !");
        }
    }

    /**
     * f0 -> <IDENTIFIER>
    */
    public ObjectType visit(Identifier n, Argument argu) {
        ObjectType objectType = new ObjectType();
        objectType.identifier = n.f0.toString();

        this.currentLine = n.f0.beginLine;
        this.currentColumn = n.f0.beginColumn;

        if (!argu.performCheck) {
            objectType.isPrimitive = true;
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
                throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot find symbol " + objectType.identifier + " (line " + n.f0.beginLine + ", column " + n.f0.beginColumn + ")");
            } else {
                objectType.customType = new SimpleEntry<>(objectType.identifier, null);
                return objectType;
            }
        }

        /*If we reach here, it means are accessing a variable inside a method.
          Therefore, we should return its type to the caller, after verifying this variable was declared before.*/

        /*Check method fields first (shadowing)*/
        if (argu.currentMethod.getValue() == null) {
            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot find symbol " + objectType.identifier);
        }
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
                            throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot find symbol " + objectType.identifier);
                        }
                    }
                } else {
                    type = checkCurrentClass(objectType.identifier, argu);
                    if (type == null) {
                        throw new RuntimeException("(line " + this.currentLine + ", column " + this.currentColumn + ") TypeError, cannot find symbol " + objectType.identifier);
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
