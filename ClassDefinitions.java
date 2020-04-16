import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static java.util.Objects.requireNonNull;


/** TODO : Check if current state is OK */

/** TODO : REMOVE ME PLS
 * Performs a first pass gathering information of all classes declared in the input program and checking shallow semantic errors.
 * 	ERROR CHECKING
 * 	- Extends class not previously declared
 * 	- Field uniqueness
 * 	- Method uniqueness
 * 	- Class uniqueness
 * 	- Method from parent must agree on return type and parameters
 */

class VariableDeclaration {
    private final String identifier;
    private final String type;

    VariableDeclaration(String identifier, String type) {
        this.identifier = identifier;
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodField)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        MethodField rhs = (MethodField) obj;

        return this.getIdentifier().equals(rhs.getIdentifier());
    }

    @Override
    public int hashCode() {
        return this.getIdentifier().hashCode();
    }
}

class ClassField extends VariableDeclaration {

    ClassField(String identifier, String type) {
        super(identifier, type);
    }
}

class MethodParameter extends VariableDeclaration {

    MethodParameter(String identifier, String type) {
        super(identifier, type);
    }
}

class MethodField extends VariableDeclaration {

    MethodField(String identifier, String type) {
        super(identifier, type);
    }
}

/**
* A class method has a declaration and a body
* 1) Declaration contains:
* --1.1) type identifier parameters
* 2) Body contains:
* --2.1) Variable declarations (we only care for these now)
* --2.2) Statements
* --2.3) Expression
* */


class ClassMethodBody {

    private Set<MethodField> fields;

    public ClassMethodBody() {
        this.fields = new HashSet<MethodField>();
    }

    public void addField(MethodField field) { this.fields.add(field); }


    public Set<MethodField> getFields() {
        return this.fields;
    }
}

class ClassMethodDeclaration {
    private final String identifier;
    private final String typeValue;
    private Set<MethodParameter> parameters;

    ClassMethodDeclaration(String identifier, String typeValue) {
        this.identifier = identifier;
        this.typeValue = typeValue;
        this.parameters = new HashSet<MethodParameter>();
    }

    public String getReturnType() {
        return this.typeValue;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public Set<MethodParameter> getParameters() {
        return this.parameters;
    }

    public void addToParameters(MethodParameter parameter) {
        this.parameters.add(parameter);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ClassMethodDeclaration)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        ClassMethodDeclaration rhs = (ClassMethodDeclaration) obj;

        return this.identifier.equals(rhs.identifier);
    }

    public int hashCode() {
        return this.identifier.hashCode();
    }
}


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

class ClassIdentifier {
    private final String className;
    private final Optional<String> extendsClassName;

    ClassIdentifier(String className, String extendsClassName) {
        this.className = requireNonNull(className, "Class Name should not be null");
        this.extendsClassName = Optional.of(requireNonNull(extendsClassName, "Extends Class Name should be not null"));
    }

    ClassIdentifier(String className) {
        this.className = requireNonNull(className, "Class Name should not be null");
        this.extendsClassName = Optional.empty();
    }

    public String getClassName() {
        return this.className;
    }

    public String getExtendsClassName() {
        if (this.extendsClassName.isPresent()) {
            return this.extendsClassName.get();
        }
        else {
            return null;
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ClassIdentifier)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        ClassIdentifier rhs = (ClassIdentifier) obj;

        return this.className.equals(rhs.className);
    }

    public int hashCode() {
        return this.className.hashCode();
    }
}

class ClassBody {

    private Set<ClassField> fields;
    private Map<ClassMethodDeclaration, ClassMethodBody> methods;

    ClassBody() {
        this.methods = new HashMap<ClassMethodDeclaration, ClassMethodBody>();
        this.fields = new HashSet<ClassField>();
    }

    public Set<ClassField> getFields() {
        return this.fields;
    }

    public Map<ClassMethodDeclaration, ClassMethodBody> getMethods() {
        return this.methods;
    }

    public void addField(ClassField field) {
        this.fields.add(field);
    }

    public void addMethod(ClassMethodDeclaration methodDecl, ClassMethodBody methodBody) {
        this.methods.put(methodDecl, methodBody);
    }
}


public class ClassDefinitions extends GJDepthFirst<String, ClassBody> {

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
        if (!classBody.getFields().contains(classField)) {
            classBody.addField(classField);
        }
        else {
            throw new RuntimeException("Redeclaration of a variable in class");
        }
    }

    private void addParametersToClassDeclaration(String parameters, ClassMethodDeclaration classMethodDeclaration) {
        String[] parameterList = parameters.split(",");

        for (int i = 0 ; i < parameterList.length ; i++) {
            String[] tokens = parameterList[i].split("::");

            MethodParameter methodParameter = new MethodParameter(tokens[1], tokens[0]);

            if (!classMethodDeclaration.getParameters().contains(methodParameter)) {
                classMethodDeclaration.addToParameters(methodParameter);
            }
            else {
                throw new RuntimeException("Redeclaration of a parameter in method");
            }
        }
    }

    private void addFieldToClassMethodBody(String varDeclaration, ClassMethodBody classMethodBody) {
        String[] tokens = varDeclaration.split("::");

        MethodField methodField = new MethodField(tokens[1], tokens[0]);
        if (!classMethodBody.getFields().contains(methodField)) {
            classMethodBody.addField(methodField);
        }
        else {
            throw new RuntimeException("Redeclaration of a field in method");
        }
    }


    /*Visit methods being here*/

    public String visit(NodeToken n, ClassBody argu) { return n.toString(); }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, ClassBody argu) {
        String _ret = null;

        n.f0.accept(this, argu);

        /*Accept all type declarations and fill class bodies*/
        for (int i = 0 ; i < n.f1.size() ; i++) {
            n.f1.elementAt(i).accept(this, argu);
        }

        return _ret;
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
    public String visit(MainClass n, ClassBody argu) {
        String _ret = null;

        String ide = n.f1.accept(this, argu);

        ClassBody mainClassBody = new ClassBody();

        ClassIdentifier classIdentifier = new ClassIdentifier(ide);

        if (!definitions.containsKey(classIdentifier)) {
            definitions.put(classIdentifier, mainClassBody);
        }
        else {
            throw new RuntimeException("Redefinitions of main class / method");
        }

        ide = n.f11.accept(this, argu);

        ClassMethodDeclaration classMethodDeclaration = new ClassMethodDeclaration("main", "void");
        classMethodDeclaration.addToParameters(new MethodParameter(ide, "String[]"));

        ClassMethodBody classMethodBody = new ClassMethodBody();

        for (int i = 0 ; i < n.f14.size() ; i++) {
            String varDeclaration = n.f14.elementAt(i).accept(this, argu);
            addFieldToClassMethodBody(varDeclaration, classMethodBody);
        }

        /*After finishing processing the entire method body, add method to our class methods*/
        mainClassBody.getMethods().put(classMethodDeclaration, classMethodBody);

        return _ret;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public String visit(TypeDeclaration n, ClassBody argu) {
        ClassBody classBody = new ClassBody();
        return n.f0.accept(this, classBody);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, ClassBody argu) {
        String _ret = null;
        String ide = n.f1.accept(this, argu);

        ClassIdentifier classIdentifier = new ClassIdentifier(ide);

        if (!definitions.containsKey(classIdentifier)) {
            definitions.put(classIdentifier, argu);
        }
        else {
            throw new RuntimeException("Redefinition of " + classIdentifier.getClassName());
        }

        /*Accept all variable declarations */
        for (int i = 0 ; i < n.f3.size() ; i++) {
            String varDeclaration = n.f3.elementAt(i).accept(this, argu);
            addFieldToClassBody(varDeclaration, argu);
        }

        /*Accept all method declarations */
        for (int i = 0 ; i < n.f4.size() ; i++) {
            n.f4.elementAt(i).accept(this, argu);
        }

        return _ret;
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
    public String visit(ClassExtendsDeclaration n, ClassBody argu) {
        String _ret = null;
        String ide = n.f1.accept(this, argu);
        String extendsIde = n.f3.accept(this, argu);

        ClassIdentifier classIdentifier = new ClassIdentifier(ide, extendsIde);

        if (!definitions.containsKey(classIdentifier)) {
            definitions.put(classIdentifier, argu);
        }
        else {
            throw new RuntimeException("Redefinition of " + classIdentifier.getClassName() + classIdentifier.getExtendsClassName());
        }

        /*Accept all variable declarations */
        for (int i = 0 ; i < n.f5.size() ; i++) {
            String varDeclaration = n.f5.elementAt(i).accept(this, argu);
            addFieldToClassBody(varDeclaration, argu);
        }

        /*Accept all method declarations */
        for (int i = 0 ; i < n.f6.size() ; i++) {
            n.f6.elementAt(i).accept(this, argu);
        }

        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, ClassBody argu) {
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
    public String visit(MethodDeclaration n, ClassBody argu) {
        String _ret = null;

        String type = n.f1.accept(this, argu);
        String ide = n.f2.accept(this, argu);

        ClassMethodDeclaration classMethodDeclaration = new ClassMethodDeclaration(ide, type);

        if (argu.getMethods().containsKey(classMethodDeclaration)) {
            throw new RuntimeException("Redefinition of method " + classMethodDeclaration.getIdentifier() + " in class");
        }

        ClassMethodBody classMethodBody = new ClassMethodBody();

        String parameters;
        if (n.f4.present()) {
            parameters = n.f4.accept(this, argu);
            if (parameters == null) {
                System.err.println("Wait, what?");
            }
            addParametersToClassDeclaration(parameters, classMethodDeclaration);
        }

        /*Accept all declarations of a method inside the class*/
        for (int i = 0 ; i < n.f7.size() ; i++) {
            String varDeclaration = n.f7.elementAt(i).accept(this, argu);
            addFieldToClassMethodBody(varDeclaration, classMethodBody);
        }

        /*After finishing processing the entire method body, add method to our class methods*/
        argu.getMethods().put(classMethodDeclaration, classMethodBody);

        return _ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, ClassBody argu) {
        String firstParam = n.f0.accept(this, argu);
        String paramTail = n.f1.accept(this, argu);

        return paramTail == null ? firstParam : firstParam + paramTail;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, ClassBody argu) throws RuntimeException {
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
    public String visit(FormalParameterTail n, ClassBody argu) {
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
    public String visit(FormalParameterTerm n, ClassBody argu) {
        String retval = null;

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
    public String visit(Type n, ClassBody argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
     */
    public String visit(ArrayType n, ClassBody argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, ClassBody argu) {
        return "boolean[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n, ClassBody argu) {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, ClassBody argu) {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, ClassBody argu) {
        return "int";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, ClassBody argu) {
        return n.f0.toString();
    }
}
