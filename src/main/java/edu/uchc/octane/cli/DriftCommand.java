package edu.uchc.octane.cli;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.drift.CorrelationEstimator;

public class DriftCommand {

	static Options options;
	static long numOfKeyFrames;
	static double resolution;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("hn%r%");

		options.getOption("h").setDescription("print this message");
		options.getOption("n").setDescription("number of key frames");
		options.getOption("r").setDescription("resolution of the drift calculation");

		return options;
	}

	public static void printHelp() {
		String syntax = "octane drift [options] data_file <output_file>";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void printParameters() {
		System.out.println("Number of key frames = " + numOfKeyFrames);
		System.out.println("Resolution = " + resolution);
	}

	public static void run(String[] args) throws ClassNotFoundException, IOException {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}
			numOfKeyFrames = CommandUtils.getParsedLong(cmd, "n", 10);
			resolution = CommandUtils.getParsedDouble(cmd, "r", 16.0);

			List<String> remainings = cmd.getArgList();
			if (remainings.size() == 1) {
				String outputFile = remainings.get(0) + ".d";
				remainings.add(outputFile);
			}

			if (remainings.size() != 2) {
				printHelp();
				return;
			}

			drift(remainings);

		} catch (ParseException | ClassCastException e) {
			printHelp();
			return;
		}
	}

	public static void drift(List<String> args) throws IOException, ClassNotFoundException {

		System.out.println("Correcting drift ...");

		CorrelationEstimator corrector = new CorrelationEstimator(resolution * 100, resolution);
		System.out.println("Loading File : " + args.get(0));
		OctaneDataFile data = OctaneDataFile.readFromFile(args.get(0));
		System.out.println("Load File: done");

		printParameters();

		System.out.println("Estimating...");
		//c.estimate( d, new Rectangle(1000,1000,2048,2048), 20);
		corrector.estimateAndCorrect(data, null, (int) numOfKeyFrames);

		System.out.println("Saving results to " + args.get(1));
		data.writeToFile(args.get(1));
		System.out.println("Saving : done");
	}
}
