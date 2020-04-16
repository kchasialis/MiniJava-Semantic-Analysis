import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;

/**
 * When we reach identifier, we have to determine if its a valid (declared before)
 * In order to determine this, it should know :
 * 1) The class its contained (also which classes this class inheritates)
 * 2) The method of the class he is contained
 * 3) The parameters of this method
 * 4) The fields declared in this method
 * */
class Argument {
    public SimpleEntry<ClassMethodDeclaration, ClassMethodBody> currentMethod;
    public SimpleEntry<ClassIdentifier, ClassBody> currentClass;
    public boolean isVariable; //Let identifier know that its not a function call, its a variable access
    public boolean defaultBehavior; //Let identifier know that it should not do anything for type check, just accept
}

public class SemanticAnalyzer extends GJDepthFirst<String, Argument > {

    ClassDefinitions classDefinitions;

    SemanticAnalyzer(ClassDefinitions classDefinitions) { this.classDefinitions = classDefinitions; }

    public String visit(NodeToken n, Argument argu) { return n.toString(); }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, Argument argu) {

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
    /*Deal with this later.
    public String visit(MainClass n, Argument argu) {
        String _ret = null;
        n.f1.accept(this, argu);
        String ide = n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        n.f13.accept(this, argu);
        n.f14.accept(this, argu);
        n.f15.accept(this, argu);
        n.f16.accept(this, argu);
        n.f17.accept(this, argu);
        return _ret;
    }*/

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public String visit(TypeDeclaration n, Argument argu) {
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
        ClassBody classBody = this.classDefinitions.getDefinitions().get(classIdentifier);

        if (classBody == null) {
            throw new RuntimeException("This was not supposed to happen");
        }

        Argument argument = new Argument();
        argument.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, classBody);

        for (int i = 0 ; i < n.f4.size() ; i++) {
            n.f4.elementAt(i).accept(this, argument);
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

        ClassIdentifier classIdentifier = new ClassIdentifier(ide, extendsIde);
        ClassBody classBody = this.classDefinitions.getDefinitions().get(classIdentifier);

        if (classBody == null) {
            throw new RuntimeException("This was not supposed to happen");
        }

        Argument argument = new Argument();
        argument.currentClass = new SimpleEntry<ClassIdentifier, ClassBody>(classIdentifier, classBody);

        for (int i = 0 ; i < n.f6.size() ; i++) {
            n.f6.elementAt(i).accept(this, argument);
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
    public String visit(MethodDeclaration n, Argument argu) {
        argu.defaultBehavior = true;
        argu.isVariable = false;
        String returnType = n.f1.accept(this, argu);
        String methodIdentifier = n.f2.accept(this, argu);

        //Check if returnType is valid.
        String[] tokens = returnType.split(" ");
        if (tokens[0].equals("Object")) {
            if (!classDefinitions.getDefinitions().containsKey(new ClassIdentifier(tokens[1]))) {
                throw new RuntimeException("TypeError, method returning class " + tokens[1] + " which is not defined");
            }
        }

        ClassMethodDeclaration classMethodDeclaration = new ClassMethodDeclaration(methodIdentifier, returnType);
        ClassMethodBody classMethodBody = argu.currentClass.getValue().getMethods().get(classMethodDeclaration);

        argu.currentMethod = new SimpleEntry<ClassMethodDeclaration, ClassMethodBody>(classMethodDeclaration, classMethodBody);

        for (int i = 0 ; i < n.f8.size() ; i++) {
            n.f8.elementAt(i).accept(this, argu);
        }

        String expressionReturnType = n.f10.accept(this, argu);

        if (!expressionReturnType.equals(returnType)) {
            throw new RuntimeException("Invalid return type");
        }

        return null;
    }

    /**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public String visit(Statement n, Argument argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public String visit(Block n, Argument argu) {

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
    public String visit(AssignmentStatement n, Argument argu) {
        String identifierType = n.f0.accept(this, argu);

        String expressionType = n.f2.accept(this, argu);

        if (!identifierType.equals(expressionType)) {
            throw new RuntimeException("TypeError " + identifierType + " does not match " + expressionType);
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
    public String visit(ArrayAssignmentStatement n, Argument argu) {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
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
    public String visit(IfStatement n, Argument argu) {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, Argument argu) {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, Argument argu) {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */
    public String visit(Expression n, Argument argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, Argument argu) {
        String boolClauseLeft = n.f0.accept(this, argu);
        String boolClauseRight = n.f2.accept(this, argu);

        if (boolClauseLeft.equals("boolean") && boolClauseRight.equals("bolean")) {
            return "boolean";
        }
        else {
            throw new RuntimeException("Invalid types on &&");
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, Argument argu) {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1.equals("int") && expr2.equals("int")) {
            return "int";
        }
        else {
            throw new RuntimeException("Invalid types on <");
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, Argument argu) {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1.equals("int") && expr2.equals("int")) {
            return "int";
        }
        else {
            throw new RuntimeException("Invalid types on +");
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, Argument argu) {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1.equals("int") && expr2.equals("int")) {
            return "int";
        }
        else {
            throw new RuntimeException("Invalid types on -");
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, Argument argu) {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1.equals("int") && expr2.equals("int")) {
            return "int";
        }
        else {
            throw new RuntimeException("Invalid types on *");
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, Argument argu) {
        String _ret=null;
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1.endsWith("[]") && expr2.equals("int")) {
            return expr1.split("::")[0];
        }
        else {
            throw new RuntimeException("Invalid array lookup");
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, Argument argu) {
        String expr = n.f0.accept(this, argu);

        if (expr.endsWith("[]")) {
            return "int";
        }
        else {
            throw new RuntimeException("Invalid length operator on non-array object");
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
    /* Ignore this for now
    public String visit(MessageSend n, SimpleEntry<ClassIdentifier, ClassBody) argu {
        String expr = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        return _expr;
    }*/

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    /*Ignore this for now
    public String visit(ExpressionList n, SimpleEntry<ClassIdentifier, ClassBody) argu {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }*/

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    /*Ignore this for now
    public String visit(ExpressionTail n, SimpleEntry<ClassIdentifier, ClassBody) argu {
        return n.f0.accept(this, argu);
    }*/

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    /*Ignore this for now
    public String visit(ExpressionTerm n, SimpleEntry<ClassIdentifier, ClassBody) argu {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }*/

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public String visit(Clause n, Argument argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */
    public String visit(PrimaryExpression n, Argument argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, Argument argu) {
        //return n.f0.accept(this, argu);
        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, Argument argu) {
        //return n.f0.accept(this, argu);
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, Argument argu) {
//        return n.f0.accept(this, argu);
        return "boolean";
    }

    /**
     * f0 -> "this"
     */
    /*this can only be called on method, handle it later too
    public String visit(ThisExpression n, SimpleEntry<ClassIdentifier, ClassBody) argu {
        return n.f0.accept(this, argu);
    }*/

    /**
     * f0 -> BooleanArrayAllocationExpression()
     *       | IntegerArrayAllocationExpression()
     */
    public String visit(ArrayAllocationExpression n, Argument argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(BooleanArrayAllocationExpression n, Argument argu) {
        return "boolean[]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(IntegerArrayAllocationExpression n, Argument argu) {
        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, Argument argu) {
        String ide = n.f1.accept(this, argu);
        return ide + "[]";
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, Argument argu) {
        String clause = n.f1.accept(this, argu);

        if (clause.equals("boolean")) {
            return clause;
        }
        else {
            throw new RuntimeException("Invalid clause on !");
        }
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, Argument argu) { return n.f1.accept(this, argu); }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, Argument argu) {
        String ide = n.f0.accept(this, argu);

        if (argu.defaultBehavior) {
            return ide;
        }

        /*If argu.currentMethod == null, it means that we are currently visiting
            a declaration outside of a method, which means that we only need to check
            if the class of the object that we are about to declare, exists somewhere else in the code.
          Else if argu.currentMethod != null, it means that we are inside a method.
            Therefore, we should take into account the class we are in, the fields of the method we are in
            and parameters of this method to validate if an identifier is valid and return its type.z
         */

        return null;
    }
}
