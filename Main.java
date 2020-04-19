import syntaxtree.Goal;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import ClassDefinitions.*;
import SemanticAnalyzer.*;

class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Driver <inputFile>");
            System.exit(-1);
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(args[0]);
            MiniJavaParser mjparser = new MiniJavaParser(fis);
            Goal root = mjparser.Goal();
            System.err.println("Program passed successfully");

            ClassDefinitions classDefs = new ClassDefinitions();
            root.accept(classDefs, null);

            printDefinitions(classDefs.getDefinitions());

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(classDefs);
            root.accept(semanticAnalyzer, null);
            System.err.println("Semantic analysis passed successfully");
        }
        catch (ParseException ex) {
            System.out.println(ex.getMessage());
        }
        catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
        finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch(IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    private static void printDefinitions(Map<ClassIdentifier, ClassBody> definitions) {
        Set<ClassIdentifier> classIdentifiers = definitions.keySet();

        for (ClassIdentifier classIdentifier : classIdentifiers) {
            String extendsName = classIdentifier.getExtendsClassName();
            if (extendsName != null) {
                System.out.println("Class + " + classIdentifier.getClassName() + " extends " + extendsName);
            }
            else {
                System.out.println("Class " + classIdentifier.getClassName());

            }

            ClassBody body = definitions.get(classIdentifier);

            System.out.println("Fields of class :");
            Set<ClassField> classFields = body.getFields().keySet();

            for (ClassField field : classFields) {
                System.out.println(field.getType() + " " + field.getIdentifier() + ";");
            }

            System.out.println("Methods of class :");

            Map<ClassMethodDeclaration, ClassMethodBody> methods = body.getMethods();
            Set<ClassMethodDeclaration> classMethodDeclarations = methods.keySet();

            for (ClassMethodDeclaration classMethodDeclaration : classMethodDeclarations) {
                System.out.print(classMethodDeclaration.getReturnType() + " " + classMethodDeclaration.getIdentifier() + " (");

                Set<MethodParameter> methodParameters = classMethodDeclaration.getParameters().keySet();
                int count = 0;
                for (MethodParameter methodParameter : methodParameters) {
                    System.out.print(methodParameter.getType() + " " + methodParameter.getIdentifier());
                    count++;
                    if (count != methodParameters.size()) {
                        System.out.print(", ");
                    }
                }
                System.out.print(")");

                System.out.println();

                ClassMethodBody classMethodBody = methods.get(classMethodDeclaration);
                Set<MethodField> methodFields =  classMethodBody.getFields().keySet();

                System.out.println("Fields of method : " + classMethodDeclaration.getIdentifier());
                for (MethodField methodField : methodFields) {
                    System.out.println(methodField.getType() + " " + methodField.getIdentifier() + ";");
                }
            }
            System.out.println("\n");
        }
    }
}