import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class Utils {
	
	public static final int MAX_FLOW = 80;
	public static final int MAX_SPEED = 80;
	public static final int LOOKBACK_MINUTES = 10;
	public static  String inputBucket = ""; 

	public static Map<Integer, Inventory> getInventoryData() {
		// The name of the file to open.
		String fileName = inputBucket + "/core/lane_measurements/detector_lane_inventory_clean.csv";

		Map<Integer, Inventory> inventory = new HashMap<Integer, Inventory>();

		// This will reference one line at a time
		String line = null;
		BufferedReader bufferedReader = null;

		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(fileName);

			// Always wrap FileReader in BufferedReader.
			bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				String[] splits = line.split("(?x),(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
				if (splits[0].contains("lane_id")) {
					continue;
				}
				Inventory i = new Inventory();
				i.lane_id = Integer.valueOf(splits[0] == null ? "0" : splits[0]);
				i.zone_id = Integer.valueOf(splits[1] == null ? "0" : splits[1]);
				i.lane_number = Integer.valueOf(splits[2] == null ? "0" : splits[2]);
				i.road = splits[5] == null ? "" : splits[5].replaceAll("^\"|\"$", "");
				i.direction = splits[6] == null ? "" : splits[6].replaceAll("^\"|\"$", "");
				i.location_description = splits[7] == null ? "" : splits[7].replaceAll("^\"|\"$", "");
				i.latitude = splits[11] == null ? "" : splits[11];
				i.longitutde = splits[12] == null ? "" : splits[12];
				i.default_speed = Integer.valueOf(splits[14] == null || splits[14].length() == 0 ? "60" : splits[14]);
				i.interval = Integer.valueOf(splits.length != 16 && splits[15] == null ? "0" : splits[15]);

				inventory.put(Integer.valueOf(splits[0]), i);
			}
			bufferedReader.close();

		} catch (Exception ex) {
			System.out.println(line);
			ex.printStackTrace();

		}
		return inventory;

	}
}
