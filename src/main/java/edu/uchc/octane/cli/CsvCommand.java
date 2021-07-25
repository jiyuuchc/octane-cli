package edu.uchc.octane.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.json.JSONException;

import edu.uchc.octane.core.datasource.OctaneDataFile;

public class CsvCommand {

	static Options options;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("hie");

		options.getOption("i").setDescription("import from csv");
		options.getOption("e").setDescription("export to csv");
		options.getOption("h").setDescription("print this message");

		return options;
	}

	public static void printHelp() {
		String syntax = "octane csv [options] -i/e input_file <output_file>";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void printParameters() {
	}

	public static void importAndConvert(List<String> args) throws IOException {
		System.out.println("Octane: import...");
        String csvFilepath = args.get(0);

        File csvFile = new File(csvFilepath);
        // long startTime = System.currentTimeMillis();
        System.out.println("Reading CSV file: " + csvFilepath);

        OctaneDataFile dataset = OctaneDataFile.importFromCSV(csvFile);
        
        System.out.println("Output file: " + args.get(1));
        dataset.writeToFile(args.get(1));
 	}

	public static void readAndExport(List<String> args) throws IOException, ClassNotFoundException {
        System.out.println("Octane: export...");
        
        System.out.println("Reading octane data: " + args.get(0));
        OctaneDataFile data = OctaneDataFile.readFromFile(args.get(0));
        
        String csvFilepath = args.get(1);
        
        System.out.println("Writing to CSV file: " + csvFilepath);
        
        File csvFile = new File(csvFilepath);
        data.exportToCSV(csvFile);
	}

	public static void run(String [] args) throws JSONException, IOException {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}

			List<String> remainings = cmd.getArgList();
			
			if (cmd.hasOption("i")) {
			    if (remainings.size() == 1) {
			        String outputFile = remainings.get(0) + ".octane";
			        remainings.add(outputFile);
			    }

			    if (remainings.size() != 2) {
			        printHelp();
			        return;
			    }

			    importAndConvert(remainings);

			} else if (cmd.hasOption("e")){
                if (remainings.size() == 1) {
                    String outputFile = remainings.get(0) + ".csv";
                    remainings.add(outputFile);
                }

                if (remainings.size() != 2) {
                    printHelp();
                    return;
                }
                
                readAndExport(remainings);
			} else {
			    printHelp();
			    return;
			}

		} catch (ParseException | ClassCastException | ClassNotFoundException e) {
		    System.out.println(e.getLocalizedMessage());
			printHelp();
			return;
		}
	}
}
