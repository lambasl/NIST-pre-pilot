

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class CalculateLaneAvgSpeedReducer extends Reducer<Text, Text, Text, Text> {

	// sdf for two given formats
	SimpleDateFormat format1 = null;
	SimpleDateFormat format2 = null;

	@Override
	protected void setup(Reducer<Text, Text, Text, Text>.Context context) throws IOException,
			InterruptedException {
		format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	}

	@Override
	protected void reduce(Text key, Iterable<Text> values, Reducer<Text, Text, Text, Text>.Context context)
			throws IOException, InterruptedException {

		Iterator<Text> iter = values.iterator();
		SortedMap<Date, StringBuilder> dataMap = new TreeMap<Date, StringBuilder>();

		while (iter.hasNext()) {
			String input = iter.next().toString();
			StringBuilder sb = new StringBuilder(input);
			String[] inputSplit = input.split("\\|");
			String time = inputSplit[1];
			Date timeD = null;
			try {
				timeD = format1.parse(time);
			} catch (ParseException e) {
				try {
					timeD = format2.parse(time);
				} catch (ParseException e1) {
					System.out.println("ERROR$$$$$$$$$$ : Both parsing failed for date" + time);
				}

			}
			if (timeD != null)
				dataMap.put(timeD, sb);

		}
		Calendar cal = Calendar.getInstance(); // creates calendar
		Iterator<Date> keyIter = dataMap.keySet().iterator();
		while (keyIter.hasNext()) {
			Date keyDate = keyIter.next();
			StringBuilder valueText = dataMap.get(keyDate);

			cal.setTime(keyDate); // sets calendar time/date
			cal.add(Calendar.MINUTE, -1 * Utils.LOOKBACK_MINUTES); // subtract
																	// LOOKBACK_MINS
			Date startDate = cal.getTime();

			cal.setTime(keyDate); // sets calendar time/date
			cal.add(Calendar.MINUTE, Utils.LOOKBACK_MINUTES); // adds
																// LOOKBACK_MINS
			Date endDate = cal.getTime();

			Map<Date, StringBuilder> subMap = dataMap.subMap(startDate, endDate);
			double avgFlow = 0.0;
			int flag;
			int count = 0;
			if (subMap.size() > 0) {
				Iterator<Date> subMapIter = subMap.keySet().iterator();
				while (subMapIter.hasNext()) {
					Date nextKey = subMapIter.next();
					String nextval = subMap.get(nextKey).toString();
					//check if flag is 1 then only consider this flow value
					String[] vals = nextval.split("\\|");
					flag = Integer.parseInt(vals[4]);
					if(flag ==1){
						count ++;
						avgFlow += Integer.parseInt(vals[3]);
					}
				}
				if(count > 0){
					avgFlow = avgFlow/count;
				}
			}
			cal.setTime(keyDate);
			String yy = String.valueOf(cal.get(cal.YEAR)%2000);
			String mm = String.valueOf(cal.get(cal.MONTH) + 1);
			if(mm.length() == 1){
				mm = "0" + mm;
			}
			if(yy.length() == 1){
				yy = "0" + yy;
			}
			String keyString =   yy + "_" +mm;// java calendar stores months from 0 to 11. Weird?
			
			valueText.append("|").append(avgFlow);
			context.write(new Text(keyString), new Text(valueText.toString()));
			
		}

		
	}
}
