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
            System.out.println("Program parsed successfully");

            ClassDefinitions classDefs = new ClassDefinitions();
            root.accept(classDefs, null);

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(classDefs, classDefs.getErrorMessages());
            root.accept(semanticAnalyzer, null);
            semanticAnalyzer.printErrors();
            System.out.println("Semantic analysis passed successfully");
            System.out.println("\nPrinting offsets\n");
            classDefs.printOffsets();
        }
        catch (ParseException ex) {
            System.err.println(ex.getMessage());
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
}