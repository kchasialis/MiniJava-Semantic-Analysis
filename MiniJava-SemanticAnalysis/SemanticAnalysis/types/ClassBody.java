package types;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import visitors.ClassDefinitions;
import static java.util.Objects.requireNonNull;

public class ClassBody {
    /*Some brilliant guys in Java decided it was a good idea not to provide Set interface with a get() method
      So, we have to do something like this...*/
    private Map<ClassField,ClassField> fields;
    private Map<ClassMethodDeclaration, ClassMethodBody> methods;
    private List<SimpleEntry<ClassField, Integer>> fieldOffsets;
    private List<SimpleEntry<String, Integer>> methodOffsets;
    private final Optional<String> extendsClassName;
    private final Map<String, Integer> sizes;
    private Integer currentFieldOffset;
    private Integer currentMethodOffset;

    public ClassBody() {
        this.methods = new HashMap<ClassMethodDeclaration, ClassMethodBody>();
        this.fields = new HashMap<ClassField, ClassField>();
        this.fieldOffsets = new ArrayList<SimpleEntry<ClassField, Integer>>();
        this.methodOffsets = new ArrayList<SimpleEntry<String, Integer>>();
        this.extendsClassName = Optional.empty();
        this.sizes = new HashMap<String, Integer>() {{
           put("int", 4);
           put("boolean", 1);
           put("int[]", 8);
           put("boolean[]", 8);
        }};
        this.currentFieldOffset = 0;
        this.currentMethodOffset = 0;
    }

    public ClassBody(String extendsClassName, ClassDefinitions classDefinitions) {
        this.methods = new HashMap<ClassMethodDeclaration, ClassMethodBody>();
        this.fields = new HashMap<ClassField, ClassField>();
        this.fieldOffsets = new ArrayList<SimpleEntry<ClassField, Integer>>();
        this.methodOffsets = new ArrayList<SimpleEntry<String, Integer>>();
        this.extendsClassName = Optional.of(requireNonNull(extendsClassName, "Extends Class Name should be not null"));
        this.sizes = new HashMap<String, Integer>() {{
            put("int", 4);
            put("boolean", 1);
            put("int[]", 8);
            put("boolean[]", 8);
        }};
        this.currentFieldOffset = getStartingOffsetOfField(this, classDefinitions);
        this.currentMethodOffset = getStartingOffsetOfMethod(this, classDefinitions);
    }

    public String getExtendsClassName() {
        return this.extendsClassName.orElse(null);
    }

    public Map<ClassField,ClassField> getFields() {
        return this.fields;
    }

    public Map<ClassMethodDeclaration, ClassMethodBody> getMethods() {
        return this.methods;
    }

    private Integer getStartingOffsetOfField(ClassBody classBody, ClassDefinitions classDefinitions) {
        if (classBody.getExtendsClassName() != null) {
            ClassBody baseClassBody = classDefinitions.getDefinitions().get(new ClassIdentifier(classBody.getExtendsClassName()));
            List<SimpleEntry<ClassField, Integer>> tempFieldOffsets = baseClassBody.fieldOffsets;
            if (tempFieldOffsets.size() > 0) {
                Integer lastFieldSize = sizeOf(tempFieldOffsets.get(tempFieldOffsets.size() - 1).getKey().getType());
                return lastFieldSize + tempFieldOffsets.get(tempFieldOffsets.size() - 1).getValue();
            }
            else if (baseClassBody.getExtendsClassName() != null) {
                return getStartingOffsetOfField(baseClassBody, classDefinitions);
            }
            else {
                return 0;
            }
        }
        else {
            return 0;
        }
    }

    private Integer getStartingOffsetOfMethod(ClassBody classBody, ClassDefinitions classDefinitions) {
        if (classBody.getExtendsClassName() != null) {
            ClassBody baseClassBody = classDefinitions.getDefinitions().get(new ClassIdentifier(classBody.getExtendsClassName()));
            List<SimpleEntry<String, Integer>> tempMethodOffsets = baseClassBody.methodOffsets;
            if (tempMethodOffsets.size() > 0) {
                return tempMethodOffsets.get(tempMethodOffsets.size() - 1).getValue() + 8;
            }
            else if (baseClassBody.getExtendsClassName() != null) {
                return getStartingOffsetOfMethod(baseClassBody, classDefinitions);
            }
            else {
                return 0;
            }
        }
        else {
            return 0;
        }
    }

    private Integer sizeOf(String type) {
        Integer retval = this.sizes.get(type);
        //If retval == null it means its an instance of an object
        if (retval == null) {
            return 8;
        }
        return retval;
    }

    public List<SimpleEntry<ClassField, Integer>> getFieldOffsets() {
        return this.fieldOffsets;
    }

    public List<SimpleEntry<String, Integer>> getMethodOffsets() {
        return this.methodOffsets;
    }

    public void addFieldOffset(ClassField field) {
        this.fieldOffsets.add(new SimpleEntry<ClassField, Integer>(field, this.currentFieldOffset));
        this.currentFieldOffset += sizeOf(field.getType());
    }

    public void addMethodOffset(ClassMethodDeclaration classMethodDeclaration) {
        String identifier = classMethodDeclaration.getIdentifier();
        this.methodOffsets.add(new SimpleEntry<String, Integer>(identifier, this.currentMethodOffset));
        this.currentMethodOffset += 8;
    }

    public void addField(ClassField field) {
        this.fields.put(field, field);
    }

    public void addMethod(ClassMethodDeclaration classMethodDeclaration, ClassMethodBody classMethodBody) {
        this.methods.put(classMethodDeclaration, classMethodBody);
    }
}