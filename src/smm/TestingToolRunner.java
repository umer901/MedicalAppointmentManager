package smm;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.io.File;
import smm.controller.ControllerInterface;
import smm.controller.Controller;
import smm.model.AppModel;
public class TestingToolRunner {

    /** The "generateTests" method should not be reimplemented nor modified.
     * It uses the Generation Testing Tool in the TestingTool folder, the feature model written in featureModelPath
     * @param testingToolFolder the name of the folder where the tool is located
     * @param featureModelPath the path to the feature model.
     * @param reference writes tests and register logs with this reference, overwrites the previous tests generated with this reference. Use a different reference to preserve previous tests and logs.
     * @return true if the generation was a success, else false.
     */
    public static boolean generateTests(String featureModelPath, String testingToolFolder, int reference, boolean usingWindows) throws Exception {
        String executablePath = testingToolFolder + "GenerationTestingTool";
        if (usingWindows) {
            executablePath += ".exe";
        }
        try {

            ProcessBuilder processBuilder = new ProcessBuilder(executablePath, featureModelPath, Integer.toString(reference), testingToolFolder);
            // Définition du répertoire de travail
            //processBuilder.directory(new File("./"));

            Process process = processBuilder.inheritIO().start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return true;
            } else {
                System.out.println("Le processus s'est terminé avec le code de sortie : " + exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Tool file was not found at specified path : " + executablePath);
            e.printStackTrace();
            return false;
        }
    }

    /** The "executeTests" method should not be reimplemented nor modified.
     * It reads the test suite at "reference" in folder "testingToolFolder", and writes the logs in this same folder and at the same reference.
     * The (de)activations and logs are requested at the "controller".
     * @param reference writes tests and register logs with this reference, overwrites the previous tests generated with this reference. Use a different reference to preserve previous tests and logs.
     * @param testingToolFolder the path to the folder where the test suites are stored
     * @param controller an instance of ControllerInterface to (de)activate features
     * @return The number of alternative paths that were executed, or 0 if there was a problem.
     */
    public static int executeTests(ControllerInterface controller, String testingToolFolder, int reference) {
        controller.disableUIView();

        // Searches for the number of alternative paths, the number in the first line of each test suite.
        String firstPath = testingToolFolder + "paths" + reference + "-0.txt";
        int numberPaths;
        try(BufferedReader br = new BufferedReader(new FileReader(firstPath))) {
            numberPaths = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        // Searches for existing logs at this reference. If they exist, wipe them.
        for(int i = 0; i<numberPaths; i++) {
            File file = new File(testingToolFolder + "logs" + reference + "-" + i + ".txt");
            if (file.exists()) {
                try {
                    FileWriter writer = new FileWriter(file);
                    writer.write(""); // Write an empty string to clear the content
                    writer.close();
                } catch (IOException e) {
                    System.err.println("An error occurred while clearing the file: " + e.getMessage());
                    return 0;
                }
            }
        }

        // Reads each alternative path and creates logs for each of them consecutively.
        for(int j = 0; j<numberPaths; j++) {
            String path = testingToolFolder + "paths" + reference + "-" + j + ".txt";
            String line;
            int stepCounter = 0;

            try(BufferedReader br = new BufferedReader(new FileReader(path))) {

                // Number of paths
                br.readLine();
                // Undetectable transition rates
                br.readLine();
                // keyword "ACTIVATION"
                br.readLine();

                String activationLine = br.readLine().trim();
                String[] activations2 = activationLine.split("-");
                if (!br.readLine().trim().equals("DEACTIVATION")) {
                    System.out.println("Irregular pattern detected in your test suite, please contact Pierre Martou (teaching assistant).");
                }
                String deactivationLine = br.readLine().trim();
                String[] deactivations2 = deactivationLine.split("-");
                controller.activate(deactivations2, activations2);

                while ((line = br.readLine()) != null) {
                    line = line.trim();

                    // New sets of features to (de)activate.
                    if (line.equals("ACTIVATION")) {
                        activationLine = br.readLine().trim();
                        String[] activations = activationLine.split("-");
                        if (!br.readLine().trim().equals("DEACTIVATION")) {
                            System.out.println("Irregular pattern detected in your test suite, please contact Pierre Martou (teaching assistant).");
                        }
                        deactivationLine = br.readLine().trim();
                        String[] deactivations = deactivationLine.split("-");

                        // Calls the controller to (de)activate the relevant features.
                        try {
                            controller.activate(deactivations, activations);
                        } catch (Exception e) {
                            System.out.println("Error detected while executing the (de)activations of features.\n"
                                    + "If you believe a specific combination of features (e.g. transition of two specific features) caused this error, don't hesitate to report it on Inginious.");
                            System.out.println("The features activated were: " + activationLine);
                            System.out.println("The features deactivated were: " + deactivationLine);
                            System.out.println("It occurred in: test reference " + reference + ", path number " + j + ", after " + stepCounter + " system verification(s).");
                            return 0;
                        }
                    // This system state will be compared to others in alternative execution paths.
                    } else if (line.equals("BREAKPOINT")) {
                        stepCounter++;
                        TestingToolRunner.writeStateToFile(controller, testingToolFolder+"logs"+reference+"-"+j+".txt");
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }
        return numberPaths;

    }

    /**
     * The "activationsAtSpecificStep" method should not be modified.
     * This method fetches the (de)activation at the specified step, in the specific suite, for testing purposes.
     *
     * @param step an integer corresponding to the step of interest
     * @param path where the execution path is stored
     * @return Discrepancies between logs if they exist, or an empty String.
     */
    public static String[][] activationsAtSpecificStep(int step, String path) {
        String line;
        int stepCounter = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            // The first six lines are: number of paths, undetectable transition rate, four lines that define the initial state.
            for(int i = 0; i<6; i++) {
                br.readLine();
            }

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (stepCounter == step) {
                    String activationLine = br.readLine().trim();
                    String[] activations = activationLine.split("-");
                    if (!br.readLine().trim().equals("DEACTIVATION")) {
                        System.out.println("Irregular pattern detected in your test suite, please contact Pierre Martou (teaching assistant).");
                    }
                    String deactivationLine = br.readLine().trim();
                    String[] deactivations = deactivationLine.split("-");
                    return new String[][]{activations, deactivations};
                }

                if (line.equals("BREAKPOINT")) {
                    stepCounter++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /** The "verifyLogs" method should not be modified, apart from the information put in "discrepancies" if you wish to change it.
     * This method reads the logs from 0 to "numberPaths", tagged with "reference", in folder "testingToolFolder".
     * @param numberPaths an integer corresponding to the number of paths
     * @param reference writes tests and register logs with this reference, overwrites the previous tests generated with this reference. Use a different reference to preserve previous tests and logs.
     * @return Discrepancies between logs if they exist, or an empty String.
     */
    public static List<String> verifyLogs(String testingToolFolder, int reference, int numberPaths) {
        List<String> discrepancies = new ArrayList<>();

        List<List<String[]>> allLogs = new ArrayList<List<String[]>>();

        // Reads all logs from files
        for (int i = 0; i<numberPaths; i++) {
            String path = testingToolFolder + "logs" + reference + "-" + i + ".txt";
            allLogs.add(readStatesFromFile(path));
        }

        // Compares each list of logs. At the same point in time, the logs should be the same.
        for (int i = 0; i<allLogs.get(0).size(); i++) {
            List<String[]> currentLogs = new ArrayList<>();
            for (int j = 0; j<allLogs.size(); j++) {
                currentLogs.add(allLogs.get(j).get(i));
            }
            // System.out.println(Arrays.toString(currentLogs.get(0)));
            String[] firstLog = currentLogs.get(0);
            for(int j=1;j<currentLogs.size();j++) {
                String[] nextLog = currentLogs.get(j);
                // System.out.println(Arrays.toString(nextLog));
                boolean discrepancy = compareLogs(firstLog, nextLog);

                if (discrepancy) {
                    String newDiscrepancy = "\n ==================================================";
                    newDiscrepancy += "\nReference "+reference+", at step " + i + ", between path 0 and alternative path "+j + ", logs are inconsistent. Logs are :\n";
                    newDiscrepancy += Arrays.toString(firstLog) + "\n VS \n" + Arrays.toString(nextLog);
                    newDiscrepancy += "\n --------------------------------------------------";
                    newDiscrepancy += "\n These logs were created after the following transition: ";

                    String[][] allActivations = activationsAtSpecificStep(i, testingToolFolder + "paths" + reference + "-0.txt");
                    if (allActivations == null) {
                        System.out.println("A problem occurred while searching for the (de)activations that caused an error in logs; please check by hand in the files.");
                    }
                    newDiscrepancy += "\n Activation of the features : " + Arrays.toString(allActivations[0]);
                    newDiscrepancy += "\n Deactivation of the features : " + Arrays.toString(allActivations[1]);
                    //newDiscrepancy += "\n (The error might come from transitions in the alternative path instead of path 0, whose (de)activations are listed here.";
                    //newDiscrepancy += "\n However, since all transitions are tested at least once in the path 0, you should see at least one other discrepancy, and this one should list the erroneous transition.) ";
                    discrepancies.add(newDiscrepancy);
                    break;
                }
            }


        }
        if (discrepancies.isEmpty()) {
            discrepancies.add("All logs are consistent between alternative execution paths. Congratulations.");
        } else {
            String lastElement = discrepancies.get(discrepancies.size() - 1);
            lastElement += "\n ==================================================";
            discrepancies.set(discrepancies.size() - 1, lastElement);
        }
        return discrepancies;
    }

    /**
     * The "readStatesFromFile" method should not be reimplemented nor modified.
     * It reads the logs from "filepath" and returns creates a list of all logs contained within.
     *
     * @param filePath is the name of the file where the logs are written.
     * @return a list of all logs contained in "filename", in order
     */
    private static List<String[]> readStatesFromFile(String filePath) {
        List<String[]> logs = new ArrayList<>();

        List<String> currentLogs = new ArrayList<>();
        String line;
        StringBuilder blockInLogs = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.equals("ENDSTATE")) {
                    logs.add(currentLogs.toArray(new String[0]));
                    currentLogs = new ArrayList<>();
                } else if (line.equals("LINEMARKER")) {
                    currentLogs.add(blockInLogs.toString());
                    blockInLogs = new StringBuilder();

                } else {
                    blockInLogs.append(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error while reading the logs from the written files.");
            e.printStackTrace();
            return null;
        }
        return logs;
    }


    /**
     * The "writeStateToFile" method should not be reimplemented nor modified.
     * It uses the "controller" to get the state as logs and writes them.
     *
     * @param filename is the name of the file where the logs are written.
     * @param controller is an instance of ControllerInterface and controls your system.
     */
    public static void writeStateToFile(ControllerInterface controller, String filename) throws IOException {
        // Call the getState function to obtain the string to write to the file
        String[] state = controller.getStateAsLog();

        // Check if the string contains the reserved keywords "ENDSTATE"
        for (String line : state) {
            if (line.contains("ENDSTATE") || line.contains("LINEMARKER")) {
                System.out.println("Error: The logs cannot contain reserved keywords (ENDSTATE or LINEMARKER), in : " + line);
                return;
            }
        }

        try {
            // Create a FileWriter with append mode to write to the end of the file
            FileWriter writer = new FileWriter(filename, true);

            for (String line : state) {
                writer.write(line+"\n");
                writer.write("LINEMARKER\n");
            }
            writer.write("ENDSTATE\n");

            // Close the FileWriter
            writer.close();
        } catch (IOException e) {
            System.out.println("Error writing to the file: " + e.getMessage());
            throw e;
        }
    }

    /** You can modify this method to omit specific lines in your logs, for example those with timestamps, to avoid false positives.
     *
     * This method is responsible for checking consistency between logs. It verifies if discrepancies are found between logs1 and logs2.
     *
     * @param logs1 First logs to be compared
     * @param logs2 Second logs to be compared
     * @return True if those logs are equivalent, else False.
     */
    public static boolean compareLogs(String[] logs1, String[] logs2) {

        Set<String> set1 = new HashSet<>(Arrays.asList(logs1));
        Set<String> set2 = new HashSet<>(Arrays.asList(logs2));

        // Add here some rules of elements to remove from those lists so they are not compared.

        // The following code can be used to show the differences between logs. You can use it elsewhere if needed.
        /*Set<String> difference1 = new HashSet<>(set1);
        difference1.removeAll(set2);

        Set<String> difference2 = new HashSet<>(set2);
        difference2.removeAll(set1);

        Set<String> differences = new HashSet<>(difference1);
        differences.addAll(difference2);*/

        // Check if sets are equal in size and content
        return !(set1.equals(set2) && logs1.length == logs2.length);
    }

    /** The "launchTestingTool" method can be modified.
     * This method generates tests, executes them through a Controller, and verifies the generated logs for consistency.
     * @param controller is an instance of ControllerInterface and controls your system.
     * @param testingToolFolder is the path to the folder containing your testing tool (GenerationTestingTool), and where your test suites and logs will be stored.
     * @param featureModelPath is the complete path to your feature model (e.g. "./features.txt" if it is in your root folder)
     */
    public static void launchTestingTool(ControllerInterface controller, String testingToolFolder, String featureModelPath, boolean skipGeneration, int reference, boolean usingWindows) throws Exception {

        // You can skip this step if your feature model did not change and you do not want to change the test suite at "reference".
        if (!skipGeneration) {
            boolean success = generateTests(featureModelPath, testingToolFolder, reference, usingWindows);
            if (!success) {
                System.out.println("Test generation failed.");
                return;
            }
        }

        // Execute the (de)activations through your Controller and writes the logs, then returns the number of alternative paths.
        int numberOfPaths = executeTests(controller, testingToolFolder, reference);

        // Checks if the logs in the alternative paths are the same.
        // You can modify the function "compareLogs" if you want to add custom rules, such as omitting timestamps in your logs.
        List<String> discrepancies = verifyLogs(testingToolFolder, reference, numberOfPaths);
        for(String line : discrepancies) {
            System.out.println(line);
        }
    }

    public static void main(String[] args) throws Exception {
        AppModel model = new AppModel();  
        ControllerInterface controller = new Controller(model);

        // Change this if you wish to preserve old tests that you reported in Inginious, for example.
        int reference = 0;

        boolean skipGeneration = false;
        boolean usingWindows = true;

        launchTestingTool(controller, "./TestingTool/", "./src/features.txt", skipGeneration, reference, usingWindows);
    }
}