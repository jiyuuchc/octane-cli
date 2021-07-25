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
import edu.uchc.octane.core.localizationimage.LocalizationImage;
import edu.uchc.octane.core.tracking.ConnectionOptimizer;
import edu.uchc.octane.core.tracking.MinSumDistance;
import edu.uchc.octane.core.tracking.TrackingDataFile;
import edu.uchc.octane.core.tracking.TrivialConnecter;

public class TrackingCommand {
	static Options options;
	static double trackingDistance;
	static long blinkings;
	static boolean doMerge;
	static boolean isNetworkedTracking;
	static LocalizationImage locData;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("ht%nmb%");

		options.getOption("h").setDescription("print this message");
		options.getOption("t").setDescription("maximum tracking distance");
		options.getOption("m").setDescription("merge trajecories");
		options.getOption("n").setDescription("perform multi-particle tracking optimization");
		options.getOption("b").setDescription("maximum blinking frames");
		return options;
	}

	public static void printHelp() {
		String syntax = "octane track [options] data_file <output_file>";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void printParameters() {
		System.out.println("Tracking distance : " + trackingDistance);
		System.out.println("Blinking : " + blinkings);
		System.out.println("Merge trajectories : " + (doMerge?"yes":"no"));
		System.out.println("Networked tracking : " + (isNetworkedTracking?"yes":"no"));
	}

	public static void tracking(List<String> args) throws IOException, ClassNotFoundException {
		System.out.println("Octane: Tracking ...");
		printParameters();

		System.out.println("Loading File : " + args.get(0));
		locData = new LocalizationImage(OctaneDataFile.readFromFile(args.get(0)));
        System.out.println("Load File: done");
		
        ConnectionOptimizer optimizer;
        if (! isNetworkedTracking) {
            optimizer = new TrivialConnecter(trackingDistance);
        } else {
            optimizer = new MinSumDistance(trackingDistance);
        }
		TrackingDataFile tracker = new TrackingDataFile(optimizer, (int) blinkings);

		OctaneDataFile trackedData = tracker.processLocalizations(locData, doMerge);
        System.out.println("Output file: " + args.get(1));
        trackedData.writeToFile(args.get(1));

	}

	public static void run(String [] args) throws IOException, ClassNotFoundException {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}
			trackingDistance = CommandUtils.getParsedDouble(cmd, "t", 60.0);
			blinkings = CommandUtils.getParsedLong(cmd, "b", 1);
			doMerge = cmd.hasOption("m");
			isNetworkedTracking  = cmd.hasOption("n");
			        
			List<String> remainings = cmd.getArgList();
			if (remainings.size() == 1) {
				String outputFile = remainings.get(0) + ".t";
				remainings.add(outputFile);
			}

			if (remainings.size() != 2) {
				printHelp();
				return;
			}

			tracking(remainings);

		} catch (ParseException | ClassCastException e) {
			printHelp();
			return;
		}
	}

}
