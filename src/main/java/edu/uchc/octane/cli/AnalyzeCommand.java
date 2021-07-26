package edu.uchc.octane.cli;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.math3.util.FastMath;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.fitting.Fitter;
import edu.uchc.octane.core.fitting.leastsquare.IntegratedGaussianPSF;
import edu.uchc.octane.core.fitting.leastsquare.LeastSquare;
import edu.uchc.octane.core.fitting.maximumlikelihood.ConjugateGradient;
import edu.uchc.octane.core.fitting.maximumlikelihood.SymmetricErf;
import edu.uchc.octane.core.fitting.maximumlikelihood.Simplex;
import edu.uchc.octane.core.frameanalysis.LocalMaximum;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;
import edu.uchc.octane.core.pixelimage.RectangularImage;
import edu.uchc.octane.core.utils.MMTaggedTiff;
import edu.uchc.octane.core.utils.TaggedImage;

public class AnalyzeCommand {
	static final Logger logger = LoggerFactory.getLogger(AnalyzeCommand.class);
	static Options options;
	static long windowSize = 3;
	static long thresholdIntensity = 30;
	static long backgroundIntensity = 0;
	static double cntsPerPhoton = 1.63;
	static int startingFrame = 0;
	static int endingFrame = -1;
	static double pixelSize = 65;
//	static boolean multiPeak = false;
//	static boolean asymmetric = false;
//	static boolean useLeastSquare = false;
	static String engine = "ls";
	static String [] engineList = {"ls", "cg", "simplex"};

	//static List<double[]> positions;
	//static String [] headers;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("hw%t%b%c%s%e%p%E:");

		options.getOption("h").setDescription("print this message");
		options.getOption("w").setDescription("fitting window size");
		options.getOption("t").setDescription("intensity threshold value");
		options.getOption("b").setDescription("camera offset");
		options.getOption("c").setDescription("Counts per photon");
		options.getOption("s").setDescription("starting frame");
		options.getOption("e").setDescription("ending frame");
		options.getOption("p").setDescription("pixel size");
		options.getOption("E").setDescription("Fitting engine (ls, cg or simplex)");

//		options.getOption("m").setDescription("perform multi-peak fitting");
//		options.getOption("a").setDescription("asymmetric psf fitting (for 3D)");
//		options.getOption("l").setDescription("use least square fitter (multi-peak always use least square fitter)");

		return options;
	}

	public static void printHelp() {
		String syntax = "octane analyze [options] data_file_folder <output_file>";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void printParameters() {
		System.out.println("Processing Frame : " + startingFrame + " - " + endingFrame);
		System.out.println("Background offset = " + backgroundIntensity);
		System.out.println("Cnts per photon = " + cntsPerPhoton);
		System.out.println("Threshold intensity = " + thresholdIntensity);
		System.out.println("Fitting window size = " + windowSize);
		System.out.println("Pixels size = " + pixelSize);
		System.out.println("Fitting engine: " + engine);
//		System.out.println("Multi-peak fitting: " + (multiPeak?"yes":"no"));
//		System.out.println("3D fitting: " + (asymmetric?"yes":"no"));
	}

	public static void run(String [] args) throws JSONException, IOException {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}

			windowSize = CommandUtils.getParsedLong(cmd, "w", windowSize);
			thresholdIntensity = CommandUtils.getParsedLong(cmd, "t", thresholdIntensity );
			backgroundIntensity = CommandUtils.getParsedLong(cmd, "b", backgroundIntensity  );
			cntsPerPhoton = CommandUtils.getParsedDouble(cmd, "c", cntsPerPhoton);
			startingFrame = (int) CommandUtils.getParsedLong(cmd, "s", startingFrame);
			endingFrame = (int) CommandUtils.getParsedLong(cmd, "e", endingFrame);;
			pixelSize = CommandUtils.getParsedDouble(cmd, "p", pixelSize);
			if (cmd.hasOption("E")) {
				engine = cmd.getOptionValue("E").toLowerCase();
				List<String> l = Arrays.asList(engineList);
				if (!l.contains(engine)) {
					printHelp();
					return;
				}
			}
//			multiPeak = cmd.hasOption("m");
//			asymmetric = cmd.hasOption("a");
//			useLeastSquare = cmd.hasOption("m") || cmd.hasOption("a") || cmd.hasOption("l");

			List<String> remainings = cmd.getArgList();
			if (remainings.size() == 1) {
				String outputFile = remainings.get(0).replaceAll("/+$", "") + ".octane";
				remainings.add(outputFile);
			}

			if (remainings.size() != 2) {
				printHelp();
				return;
			}

			process(remainings);

		} catch (ParseException | ClassCastException e) {
			printHelp();
			return;
		}
	}

	public static void process(List<String> args) throws JSONException, IOException {
		System.out.println("Analyze data: " + args.get(0));

		MMTaggedTiff stackReader = new MMTaggedTiff(args.get(0), false, false);
		int frames = stackReader.getSummaryMetadata().getInt("Frames");
		System.out.println("Total frames: " + frames);
		ArrayList<double[]> [] results = new ArrayList[frames];

		if (startingFrame < 0 ) {
			startingFrame = 0;
		}
		if (endingFrame >= frames || endingFrame < 0) {
			endingFrame = frames;
		}

		printParameters();

		IntStream.range(startingFrame, endingFrame).parallel().forEach( f -> {
			TaggedImage img;
			synchronized (stackReader) {
				img= stackReader.getImage(0 /*channel*/, 0 /*slice*/, f /*frame*/, 0 /*position*/);
			}

			if (img != null ) {
				try {
					results[f] = processFrame(img);
				} catch (JSONException e) {
					assert(false); //shouldn't happen
				}
				System.out.println("Processed frame " + f + ", Found " + results[f].size() + " molecules." );
			} else {
				System.out.println("Error reading frame " + f);
			}
		});
		stackReader.close();

		int cnt = 0;
		for (int f = startingFrame; f < endingFrame; f++) {
			cnt += results[f].size(); 
		}
		String [] tmpHeader = getFitter().getHeaders();
		String [] headers = Arrays.copyOf(tmpHeader, tmpHeader.length + 1);
		headers[headers.length-1] = "frame";
		double [][] data = new double[headers.length][cnt];

		int idx = 0;
		for (int f = startingFrame; f < endingFrame; f++) {
			for (double[] p : results[f]) {
				for (int h = 0; h < tmpHeader.length; h ++) {
					data[h][idx] = p[h];
				}
				data[headers.length-1][idx] = f;
				idx += 1;
			}
		}
		
		for (int h = 0; h < headers.length; h++) {
			String s = headers[h];
			if ( s.equals("x") || s.equals("y") || s.equals("z") || s.startsWith("sigma")) {
				for (idx = 0 ; idx < cnt; idx ++ ) {
					data[h][idx]= data[h][idx] * pixelSize;		
				}
			}
		}

		OctaneDataFile raw = new OctaneDataFile(data, headers);

		System.out.println("Saving to file: " + args.get(1));
		ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(args.get(1)));
		System.out.println("Output file: " + args.get(1));
		fo.writeObject(raw);
		fo.close();
	}

	static Fitter getFitter() {
		Fitter fitter;
		switch (engine) {
		case "cg": fitter = new ConjugateGradient(new SymmetricErf()); break;
		case "simplex": fitter = new Simplex(new SymmetricErf()); break;
		default: fitter = new LeastSquare(new IntegratedGaussianPSF());
		}
		return fitter;
	}
	
	static ArrayList<double[]> processFrame(TaggedImage img) throws JSONException {
		short [] iPixels = (short[]) img.pix;
		double [] pixels = new double[iPixels.length];
		for (int i = 0; i < pixels.length; i ++) {
			pixels[i] = (iPixels[i]&0xffff - backgroundIntensity) / cntsPerPhoton ;
		}

		RectangularDoubleImage data = new RectangularDoubleImage(pixels, img.tags.getInt("Width"));
		ArrayList<double[]> particles = new ArrayList();

		Fitter fitter = getFitter();
		
		LocalMaximum finder = new LocalMaximum(thresholdIntensity, 0, (int) windowSize);
		finder.processFrame(data, new LocalMaximum.CallBackFunctions() {
			@Override
			public boolean fit(RectangularImage subimg, int x, int y) {
				double [] result = fitter.fit(subimg, null);
				if (result != null) {
					if (result[0] < subimg.x0 || result[0] > subimg.x0 + subimg.width || result[1] < subimg.y0 || result[1] > subimg.y0 + subimg.height) {
						logger.warn("Discard fitting results - out of bound: " + (result[0] - windowSize - subimg.x0) + ", " + (result[1] - subimg.y0 - windowSize));
						return true;
					}
					if (result[3] < 0) {
						logger.warn("Discard fitting results - negative intensity: " + result[3]);
						return true;
					}
					result[2] = FastMath.abs(result[2]); // make sigma always positive  
					particles.add(result);
				} 
				return true;
			}
		});
		return particles;
	}
}
