import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Job3Mapper extends Mapper<LongWritable, Text, LongWritable, Text> {
	private Text outVal = new Text();
	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException{
		
		String[] valueString = value.toString().split("\\|");
		// ----------------
		String line = valueString[7];
		String flag = valueString[5];
		String flow = valueString[3];
		String avgflow = valueString[8];

		
		StringBuilder st = new StringBuilder();
		st.append(flag).append("|");
		st.append(avgflow).append("|");
		st.append(valueString[6]);
		LongWritable outKey = new LongWritable(Long.valueOf(line));
		outVal.set(st.toString());
		context.write(outKey, outVal);
	}
}