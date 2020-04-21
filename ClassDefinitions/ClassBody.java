package ClassDefinitions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ClassBody {
    /*Some brilliant guys in Java decided it was a good idea not to provide Set interface with a get() method
      So, we have to do something like this...*/
    private Map<ClassField,ClassField> fields;
    private Map<ClassMethodDeclaration, ClassMethodBody> methods;
    private final Optional<String> extendsClassName;

    ClassBody() {
        this.methods = new HashMap<ClassMethodDeclaration, ClassMethodBody>();
        this.fields = new HashMap<ClassField, ClassField>();
        this.extendsClassName = Optional.empty();
    }

    ClassBody(String extendsClassName) {
        this.extendsClassName = Optional.of(requireNonNull(extendsClassName, "Extends Class Name should be not null"));
    }

    public String getExtendsClassName() {
        if (this.extendsClassName.isPresent()) {
            return this.extendsClassName.get();
        }
        else {
            return null;
        }
    }

    public Map<ClassField,ClassField> getFields() {
        return this.fields;
    }

    public Map<ClassMethodDeclaration, ClassMethodBody> getMethods() {
        return this.methods;
    }

    public void addField(ClassField field) {
        this.fields.put(field, field);
    }

    public void addMethod(ClassMethodDeclaration classMethodDeclaration, ClassMethodBody classMethodBody) {
        this.methods.put(classMethodDeclaration, classMethodBody);
    }
}