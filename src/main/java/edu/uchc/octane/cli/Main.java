package edu.uchc.octane.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.json.JSONException;

public class Main {

    final static Properties properties = new Properties();

    public static void printHelp() {
        System.out.println("Octane(cli) v" + properties.getProperty("version") + " - available commands");
        System.out.println("\tanalyze\t analyze raw image data for localizations");
        System.out.println("\tdrift\t correct drift in the localzation data");
        System.out.println("\ttrack\t track trajectories and merge coordinantes");
        System.out.println("\tcsv\t import from or export to CSV files");
        System.out.println();
        System.out.println("octane cmd -h for further help");
    }

    public static void main(String[] args) throws IOException, JSONException, ClassNotFoundException {

        InputStream stream = Main.class.getClassLoader().getResourceAsStream("project.properties");
        properties.load(stream);
        
        if (args == null || args.length == 0) {
            printHelp();
            return;
        }

        String cmd = args[0];
        if (args.length > 1) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (cmd.equals("about")) {
            System.out.println("Octane " + properties.getProperty("version"));
            System.out.println("Single molecule localization microscopy data analysis tools");
        } else if (cmd.equals("drift")) {
            DriftCommand.run(args);
        } else if (cmd.equals("analyze")) {
            AnalyzeCommand.run(args);
        } else if (cmd.equals("track")) {
            TrackingCommand.run(args);
        } else if (cmd.equals("csv")) {
            CsvCommand.run(args);
        } else {
            printHelp();
        }
    }
}
