import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class JobMapper extends Mapper<LongWritable, Text, LongWritable, DoubleWritable> {
	long lineNumber = 0;
	private LongWritable outKey;
	private DoubleWritable outValue = new DoubleWritable();

	// @Override
	// protected void setup(Mapper<LongWritable, Text, Text, Text>.Context
	// context) throws IOException,
	// InterruptedException {
	// inventory = Utils.getInventoryData();
	// }

	public static String getHTML(String urlToRead) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		return result.toString();
	}

	// =================================================
	// Get references
	public static String get_refs(String nodes, boolean begining, String[] gs, String geoL) {
		String q = Pattern.quote("<nd ref=\"") + "(.*?)" + Pattern.quote("\"/>");
		Pattern pattern = Pattern.compile(q);
		Matcher matcher = pattern.matcher(nodes);
		ArrayList<String> refs = new ArrayList<String>();
		while (matcher.find()) {
			refs.add(matcher.group(1));
		}
		int count = 0;
		String geo = "";
		String ref = "";
		if (begining) {
			while (count < refs.size()) {
				ref = refs.get(count++);
				String check = getLatAndLon(ref,geoL, gs);
				if (check.contains("error"))
					continue;
				else{
					geo = check;
					break;
				}
			}
			if (geo.length() == 0)
				geo = "error";
		} else {
			count = refs.size() - 1;
			while (count < refs.size()) {
				ref = refs.get(count--);
				String check = getLatAndLon(ref,geoL, gs);
				if (check.contains("error"))
					continue;
				else{
					geo = check;
					break;
				}
			}
			if (geo.length() == 0)
				geo = "error";
		}
		return geo;
	}

	// ==========================================
	// Get lane numbers
	public static String get_lanes(String tags) {
		String q = Pattern.quote("<tag k=\"lanes\" v=\"") + "(.*?)" + Pattern.quote("\"/>");
		Pattern pattern = Pattern.compile(q);
		Matcher matcher = pattern.matcher(tags);
		String ln = "";
		if (matcher.find()) {
			ln = matcher.group(1);
		} else {
			ln = "Error"; // most of the major roads all labeled
							// but some of the smaller roads are not label
		}
		return ln;
	}

	// ==================================================
	// Get latitude and longitude
	public static String getLatAndLon(String node, String nodes, String[] gs) {
		int ways_index = nodes.indexOf(node);
		String nds = nodes.substring(ways_index - 1);
		// Get latitude and longitude
		Pattern lat_pattern = Pattern.compile("lat=\"(.*?)\"");
		Pattern lon_pattern = Pattern.compile("lon=\"(.*?)\"");
		Matcher lan_matcher = lat_pattern.matcher(nds);
		Matcher lon_matcher = lon_pattern.matcher(nds);
		// Get geo points
		String geo = "";
		if (lan_matcher.find() && lon_matcher.find()) {
			geo = lan_matcher.group(1);	
			geo = geo.concat(" " + lon_matcher.group(1));
			// System.out.println(geo);
			double lan = Double.parseDouble(lan_matcher.group(1));
			double lon = Double.parseDouble(lon_matcher.group(1));
			double st_lan = Double.parseDouble(gs[0]);
			double st_lon = Double.parseDouble(gs[1]);
			double nd_lan = Double.parseDouble(gs[2]);
			double nd_lon = Double.parseDouble(gs[3]);
			// Making sure we are whitin the bbox
			if (lan < st_lan || lan > nd_lan || lon < st_lon || lon > nd_lon) {
				return "error";
			}
		}

		return geo;
	}

	// =====================================================
	// Get Distance
	public static String get_Distance(String stlat, String stlon, String ndlat, String ndlon) throws Exception {
		String api = getHTML("http://www.yournavigation.org/api/1.0/gosmore.php?format=kml&" + "flat=" + stlat
				+ "&flon=" + stlon + "&tlat=" + ndlat + "&tlon=" + ndlon + "&v=motorcar&layer=mapnik");
		Pattern dist_pattern = Pattern.compile("<distance>(.*?)</distance>");
		Matcher matcher = dist_pattern.matcher(api);
		String dist = "";
		if (matcher.find()) {
			dist = matcher.group(1);
		}
		// Convert Km to Miles
		final double MILES_PER_KILOMETER = 0.621;
		double kilometers = Double.parseDouble(dist);
		// ... Computation
		double miles = kilometers * MILES_PER_KILOMETER;
		dist = String.valueOf(miles);
		return dist;
	}

	public static String pattern_match(String xml, String[] gs) throws Exception {
		String dist_lanes = "";
		int ways_index = xml.indexOf("<way");
		String nds = xml.substring(0, ways_index - 1);
		Pattern ways_pattern = Pattern.compile("<way id=\"\\d*\">\\s*(.*?) </way>");
		Matcher matcher = ways_pattern.matcher(xml);
		while (matcher.find()) {
			String between_tags = matcher.group(1); // Since (.*?) is capturing
													// group 1
			// Printing
			// System.out.println(between_tags);
			// Split the nodes and the tags with info
			int tag_index = between_tags.indexOf("<tag");
			String nd = between_tags.substring(0, tag_index - 1);
			String tags_info = between_tags.substring(tag_index);
			// Get lane number
			String lanes = get_lanes(tags_info);
			if (lanes.contains("Error"))
				continue; // IF it contains error then is not a major road
			// Get all refs numbers
			String point1 = get_refs(nd, true,gs,nds);
			if (point1.contains("error"))
				continue;
			String point2 = get_refs(nd, false,gs,nds);
			if (point2.contains("error"))
               continue;
			// Get Distance
			int geo_index1 = point1.indexOf(" ");
			int geo_index2 = point2.indexOf(" ");
			String distance = get_Distance(point1.substring(0, geo_index1), point1.substring(geo_index1 + 1),
					point2.substring(0, geo_index2), point2.substring(geo_index2 + 1));
			dist_lanes = dist_lanes.concat(lanes).concat(" ").concat(distance).concat("!!!");

		}
		return dist_lanes;
	}

	// =========================================
	// Date Range
	private static int month(String date_s, String date_d) {
		// String date = "2015-03-01T00:00:00 2015-03-31T23:59:59";
		// Date in yy-mm-dd
		String[] dt = date_s.split("-");
		// Parse out the year and month
		int month = Integer.parseInt(dt[1]);
		return month;
	}

	// ==========================================
	// Making Feature List
	private static double[][] get_feature(String feature, int month) {
		String[] list = feature.split("!!!");
		double[][] featureList = new double[list.length][13];
		int i = 0;
		while (i != list.length) {
			String[] lt = list[i].split(" ");
			double lane = Double.parseDouble(lt[0]);
			double dist = Double.parseDouble(lt[1]);
			featureList[i][0] = lane;
			featureList[i][1] = dist;
			featureList[i][month + 1] = 1;
			++i;
		}

		return featureList;
	}

	// ============================================
	// Matrix Multuplication
	public static double[] multiply(double[][] A, double[] x) {
		int m = A.length;
		int n = A[0].length;
		if (x.length != n)
			throw new RuntimeException("Illegal matrix dimensions.");
		double[] y = new double[m];
		for (int i = 0; i < m; i++)
			for (int j = 0; j < n; j++)
				y[i] += A[i][j] * x[j];
		return y;
	}

	// ====================================
	// MAPPING
	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		// "38.7873 -77.1532 38.7978923322 -77.147105071 2015-03-01T00:00:00
		// 2015-03-31T23:59:59"
		// Separating terms
		String line = value.toString();
		String[] parsing = line.split("\t");
		lineNumber++;
		try {
			String st_lat = parsing[0];
			String st_lon = parsing[1];
			String nd_lat = parsing[2];
			String nd_lon = parsing[3];
			String date_s = parsing[4];
			String date_d = parsing[5];
			// 1) Get Api Info
			// 2) Parse to get lanes and distance
			String feature = pattern_match(getHTML("http://overpass-api.de/api/xapi?way[highway=*][bbox=" + st_lon + ","
					+ st_lat + "," + nd_lon + "," + nd_lat + "]"), parsing);

			// 3) Get Date
//			int month = month(date_s, date_d);
//			// Creating the feature List
//			double[][] featureList = get_feature(feature, month);
//			// Beta Values
//			double[] betas = { 12.903152499015576, -0.33060162248576064, -6.7152794381636571, -7.2697394072987684,
//					-6.5048300158489338, -6.6397038868589977, -3.0860297527109419, -3.2805488690538898,
//					0.89459414970823348, 0.77352606327589446, -0.13463818707038167, 6.0092785675944764,
//					0.66947539244336429 };
//			// Print Matrix
////			for (int row = 0; row < featureList.length; row++) {
////				for (int column = 0; column < featureList[row].length; column++) {
////					System.out.print(featureList[row][column] + "||");
////				}
////				System.out.println();
////			}
//			// Matrix Multiplication
//			double[] matrix = multiply(featureList, betas);
//	
//			//Writting to the conxtext
//      		outKey.set(lineNumber);
//			for (int row = 0; row < matrix.length; row++) {
//				//System.out.print(matrix[row] + "||");
//				outValue.set(matrix[row]);
//				context.write(outKey, outValue);
//			}
//			//System.out.println();
			//context.write(new Text(feature), new DoubleWritable(1));
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

}
