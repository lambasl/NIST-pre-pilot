import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class reducto extends Reducer<Text, NullWritable, Text, Text> {
	// private Text outKey = new Text();
	// private Text outValue = new Text();
	// private NullWritable nw = NullWritable.get();

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
				String check = getLatAndLon(ref, geoL, gs);
				if (check.contains("error"))
					continue;
				else {
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
				String check = getLatAndLon(ref, geoL, gs);
				if (check.contains("error"))
					continue;
				else {
					geo = check;
					break;
				}
			}
			if (geo.length() == 0)
				geo = "error";
		}
		return geo;
	}

	public static String get_Distance(String stlat, String stlon, String ndlat, String ndlon) throws Exception {
		String api = getHTML("http://www.yournavigation.org/api/1.0/gosmore.php?format=kml&" + "flat=" + stlat
				+ "&flon=" + stlon + "&tlat=" + ndlat + "&tlon=" + ndlon
				+ "&v=motorcar&layer=mapnik&instructions=0&fast=1&lang=en_US");
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

	// Get latitude and longitude
	public static String getLatAndLon(String node, String nodes, String[] gs) {
		int ways_index = nodes.indexOf(node);
		System.out.println("GetLatandLon" + ways_index);
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

	public static String pattern_match(String xml, String[] gs) throws Exception {
		String dist_lanes = "";
		int ways_index = xml.indexOf("<way");
		System.out.println("Ways_index" + ways_index);
		String nds = xml.substring(0, ways_index - 1);
		
		Pattern ways_pattern = Pattern.compile("<way id=\"\\d*\">\\s*(.*?) </way>");
		Matcher matcher = ways_pattern.matcher(xml);
		while (matcher.find()) {
			String between_tags = matcher.group(1); // Since (.*?) is capturing
													// group 1
			// Printing
			 System.out.println(between_tags);
			// Split the nodes and the tags with info
			int tag_index = between_tags.indexOf("<tag");
			System.out.println("Index: " + tag_index);
			String nd = between_tags.substring(0, tag_index - 1);
			String tags_info = between_tags.substring(tag_index);
			// Get lane number
			String lanes = get_lanes(tags_info);
			// Get all refs numbers
			String point1 = get_refs(nd, true, gs, nds);
			if (point1.contains("error"))
				continue;
			String point2 = get_refs(nd, false, gs, nds);
			if (point2.contains("error"))
				continue;
			// Get Distance
			int geo_index1 = point1.indexOf(" ");
			int geo_index2 = point2.indexOf(" ");
			System.out.println("Point1" + geo_index1);
			System.out.println("Point2" + geo_index2);
			String distance = get_Distance(point1.substring(0, geo_index1), point1.substring(geo_index1 + 1),
					point2.substring(0, geo_index2), point2.substring(geo_index2 + 1));
			dist_lanes = dist_lanes.concat(lanes).concat(" ").concat(distance).concat("!!!");

		}
		return dist_lanes;
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
		}
		return ln;
	}

	public void reduce(Text key, Iterable<NullWritable> values, Context context)
			throws IOException, InterruptedException {
		// Split key which represent the two given points
		String[] bbox = key.toString().split(",");
		String st_lat = bbox[0];
		String st_lon = bbox[1];
		String nd_lat = bbox[2];
		String nd_lon = bbox[3];

		try {
			String feature = pattern_match(
					getHTML("http://overpass-api.de/api/xapi?way[highway=motorway|motorway_link|trunk|primary][lanes=*][bbox="
							+ st_lon + "," + st_lat + "," + nd_lon + "," + nd_lat + "]"),
					bbox);

			context.write(key, new Text(feature));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
