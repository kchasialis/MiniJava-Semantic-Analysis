import syntaxtree.Goal;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ClassDefinitions.*;
import SemanticAnalyzer.*;

class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Driver <inputFile>");
            System.exit(-1);
        }
        FileInputStream fis = null;
        for (int i = 0 ; i < args.length ; i++) {
            try {
                fis = new FileInputStream(args[i]);
                MiniJavaParser mjparser = new MiniJavaParser(fis);
                Goal root = mjparser.Goal();
                System.err.println("Program parsed successfully");

                ClassDefinitions classDefs = new ClassDefinitions();
                root.accept(classDefs, null);

                SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(classDefs);
                boolean failed = classDefs.getErrorMessages().size() > 0;

                try {
                    root.accept(semanticAnalyzer, null);
                } catch (RuntimeException re) {
                    classDefs.getErrorMessages().add(re.getMessage());
                    failed = true;
                }

                if (!failed) {
                    System.err.println("Semantic analysis passed successfully");

                } else {
                    printErrors(classDefs.getErrorMessages());
                    System.err.println("Semantic analysis failed");
                }

                System.out.println();
                classDefs.printOffsets();
            } catch (ParseException ex) {
                System.err.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
         }
    }

    public static void printErrors(List<String> errorMessages) {
        for (int i = 0 ; i < errorMessages.size(); i++) {
            System.err.println(errorMessages.get(i));
        }
    }
}