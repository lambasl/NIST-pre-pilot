import java.io.*;
import java.net.*;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parsing {

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
	public static String get_refs(String nodes, int pos) {
		String q = Pattern.quote("<nd ref=\"") + "(.*?)" + Pattern.quote("\"/>");
		Pattern pattern = Pattern.compile(q);
		Matcher matcher = pattern.matcher(nodes);
		ArrayList<String> refs = new ArrayList<String>();
		String references = "";
		while (matcher.find()) {
			refs.add(matcher.group(1));
		}
		if (pos < (refs.size()) - 1) {
			references = refs.get(pos);

			references = references.concat(" " + refs.get(refs.size() - 1));
		} else {
			references = "error";
		}
		return references;
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
	public static String getLatAndLon(String node, String nodes, String refs, int i) {
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
			if (lan < 39.1673 || lan > 39.1803640147 || lon < -76.7432 || lon > -76.7220137059) {
				i++;
				String ref = get_refs(refs, i);
				if (!ref.contains("error")) {
					int index = ref.indexOf(" ");
					geo = getLatAndLon(ref.substring(0, index), nodes, refs, i);
				}
				else {
					return "error";
				}
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

		return dist;
	}

	public static String pattern_match(String xml) throws Exception {
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
			// Get all refs numbers
			String refs = get_refs(nd, 0);
			// Get lane number
			String lanes = get_lanes(tags_info);
			if (lanes.contains("Error")) continue; //IF it contains error then is not a major road
			// System.out.println(refs);
			// Get the latitude and longitude
			int index = refs.indexOf(" ");
			String point1 = getLatAndLon(refs.substring(0, index), nds, nd, 0);
			String point2 = "";
			// The point is outside the bouding box
			if (!point1.contains("error")) {
				point2 = getLatAndLon(refs.substring(index + 1), nds, nd, 0);
				if(point2.contains("error")){
					continue;
				}
			} else {
				continue;
			}
			// Get Distance
			int geo_index1 = point1.indexOf(" ");
			int geo_index2 = point2.indexOf(" ");
			String distance = get_Distance(point1.substring(0, geo_index1), point1.substring(geo_index1 + 1),
					point2.substring(0, geo_index2), point2.substring(geo_index2 + 1));

			/////////////////// Printing

			dist_lanes = dist_lanes.concat(lanes).concat(" ").concat(distance).concat("!!!");

		}
		return dist_lanes;
	}

	// =========================================
	// Date Range
	private static int month(String date) {
		// String date = "2015-03-01T00:00:00 2015-03-31T23:59:59";
		// Date in yy-mm-dd
		String[] dt = date.split("-");
		// Parse out the year and month
		int month = Integer.parseInt(dt[1]);
		return month;
	}

	// ==========================================
	// Making Feature List
	private static double[][] get_feature(String feature, int month) {
		String[] list = feature.split("!!!");
		double[][] featureList = new double[list.length][14];
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

	public static void main(String[] args) throws Exception {
		// System.out.println(getHTML(args[0]));
		
		String st_lat = "39.1673";
		String st_lon = "-76.7432";
		String nd_lat = "39.1803640147";
		String nd_lon = "-76.7220137059";
		String date = "2015-03-01T00:00:00	2015-03-31T23:59:59";
		// 1) Get Api Info
		// 2) Parse to get lanes and distance
		String feature = pattern_match(getHTML("http://overpass-api.de/api/xapi?way[highway=*][bbox=" + st_lon + ","
				+ st_lat + "," + nd_lon + "," + nd_lat + "]"));
		// 3) Get Date
		int month = month(date);
		//if(!feature.contains(""))
		double[][] featureList = get_feature(feature, month);

		 for (int row = 0; row < featureList.length; row++) {
		 for (int column = 0; column < featureList[row].length; column++) {
		 System.out.print(featureList[row][column] + "||");
		 }
		 System.out.println();
		 }

		// String range = month_range(start, end);
	}
}