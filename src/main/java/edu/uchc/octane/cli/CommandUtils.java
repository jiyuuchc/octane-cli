package edu.uchc.octane.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

public class CommandUtils {

	static public long getParsedLong(CommandLine cmd, String s, long defaultValue) throws ParseException {
		long value = defaultValue;
		if (cmd.hasOption(s)) {
			value = (long) cmd.getParsedOptionValue(s);
		}
		return value;
	}

	static public double getParsedDouble(CommandLine cmd, String s, double defaultValue) throws ParseException {
		double value = defaultValue;

		if (cmd.hasOption(s)) {
			try {
				value = (double) cmd.getParsedOptionValue(s);
			} catch (ClassCastException e) {
				value = (long) cmd.getParsedOptionValue(s);
			}
		}

		return value;
	}
}
