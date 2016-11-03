

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class CalculateLaneAvgSpeedMapper extends Mapper<LongWritable, Text, Text, Text> {

	Map<Integer, Inventory> inventory;
	long lineNumber = 0;

	@Override
	protected void setup(Mapper<LongWritable, Text, Text, Text>.Context context) throws IOException,
			InterruptedException {
		//inventory = Utils.getInventoryData();
	}

	@Override
	protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
			throws IOException, InterruptedException {

		lineNumber ++;
		if (value.toString().contains("lane_id")) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		try {

			String[] splits = value.toString().split("(?x),(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
			String lane_id = splits[0];
			String time = splits[1];
			int speed = Double.valueOf(splits[2]).intValue();
			int flow = Double.valueOf(splits[3]).intValue();
			int occupancy = Double.valueOf(splits[4]).intValue();
			int quality = Double.valueOf(splits[5]).intValue();
			
			int flag = 1;
			String error = "None";
			
			if(flow < 0 || flow > Utils.MAX_FLOW){
				flag = 0;
				flow = 0;
				error = "Insane_value";
			}
			if(occupancy == 100  && flow != 1){
				flag = 0;
				flow = 1;
				speed = 0;
				error = "stationary_vehicle";
			}
			if(quality != 0){
				flag = 0;
				error = "quality";
			}
			if(occupancy > 0 && flow == 0){
				flag = 0;
				error = "inconsistent_with_occupancy";
			}
			String[] dateSplit = time.split(" ");
			String date = dateSplit[0];
			Text key1 = new Text(lane_id + "-" +  date);
			sb.append(lane_id).append("|");
			sb.append(time).append("|");
			sb.append(speed).append("|");
			sb.append(flow).append("|");
			sb.append(flag).append("|");
			sb.append(error).append("|").append(lineNumber);
			
			context.write(key1, new Text(sb.toString()));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(sb.toString());
		}
	}
}
