import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class ZoneIdAvgSpeedMapper extends Mapper<LongWritable, Text , Text, Text> {
	Map<Integer, Inventory> inventory;
	long lineNumber = 0;
	private Text outKey = new Text();
	private Text outValue = new Text();

	@Override
	protected void setup(Mapper<LongWritable, Text, Text, Text>.Context context) throws IOException,
			InterruptedException {
		inventory = Utils.getInventoryData();
	}
	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException{
		if (value.toString().contains("lane_id")) {
			return;
		}
		lineNumber++;
		StringBuilder sb = new StringBuilder();
		try {

			String[] splits = value.toString().split("(?x),(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
			String lane_id = splits[0];
			String time = splits[1];
			int speed = Integer.valueOf(splits[2]);
			int flow = Integer.valueOf(splits[3]);
			int occupancy = Integer.valueOf(splits[4]);
			int quality = Integer.valueOf(splits[5]);
			
			int flag = 1;
			String error = "None";
			
			if(flow < 0 || flow > Utils.MAX_FLOW){
				flag = 0;
				error = "Insane_value";
			}
			if(occupancy >= 98 && flow != 1 && speed <= 0){
				flag = 0;
				flow = 1;
				speed = 0;
				error = "stationary_vehicle";
			}
			if(quality != 0){
				flag = 0;
				error = "quality";
			}
			if(speed > 0 && occupancy > 0 && flow <= 0){
				flag = 0;
				error = "inconsistent_with_flow_and_speed";
			}
			int query = Integer.valueOf(lane_id);
			Inventory lane_detector = inventory.get(query);
			String direction = lane_detector.direction;
			String road = lane_detector.road;
			String lat = lane_detector.latitude;
			String lon = lane_detector.longitutde;
			int interval  = lane_detector.interval;
			lat = lat.length() > 6 ? lat.substring(0,6) : lat;
			lon = lon.length() > 7 ? lon.substring(0,7) : lon;
			StringBuilder str = new StringBuilder();
			str.append(road + "|");
			str.append(direction + "|");
			str.append(lat + "|");
			str.append(lon + "|");	
			String[] dateSplit = time.split(" ");
			String date = dateSplit[0];
			str.append(date);
			outKey.set(str.toString());
			
			sb.append(lane_id).append("|");
			sb.append(time).append("|");
			sb.append(speed).append("|");
			sb.append(flow).append("|");
			sb.append(interval).append("|");
			sb.append(flag).append("|");
			sb.append(error).append("|").append(lineNumber);
			outValue.set(sb.toString());
			
			context.write(outKey, outValue);
		} catch (Exception e) {
			System.out.println(sb.toString());
		}
	}

}
