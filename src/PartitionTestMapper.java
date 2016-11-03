

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

public class PartitionTestMapper extends Mapper<LongWritable, Text, LongWritable, Text>{
	
	private MultipleOutputs<LongWritable, Text> multipleOutputs;
	
	@Override
	protected void setup(Mapper<LongWritable, Text, LongWritable, Text>.Context context) throws IOException,
			InterruptedException {
		multipleOutputs = new MultipleOutputs<LongWritable, Text>(context);
	}
	@Override
	protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, LongWritable, Text>.Context context)
			throws IOException, InterruptedException {
		
		try{
		String keyString = value.toString().split("\t")[0];
		String data = value.toString().split("\t")[1];
		String[] splits = data.split("\\|");
		String date = splits[1].split(" ")[0];
		int flow = Integer.valueOf(splits[3]);
		int flag = Integer.valueOf(splits[4]);
		Double avgFlow = Double.valueOf(splits[7]);
		long lineNumber = Long.valueOf(splits[6]);
		String errorCode = splits[5];
		
		if(flag == 0 && errorCode != "stationary_vehicle"){
			flow = (int)(Math.ceil(avgFlow));
		}
		if(flag == 1){
			double percentDev = Math.abs(avgFlow - flow)/avgFlow;
			if(percentDev > 0.7){
				flag = 0;
				flow = (int)Math.ceil(avgFlow);
				errorCode = "SD_violation";
			}
		}
		StringBuilder sb = new StringBuilder();
		//String  subKey = keyString.substring(0, keyString.length()-2 );
		String fileName = "cleaning_subm_"+ keyString + "_NIST6.txt";
		sb.append(flag).append("\t").append(flow).append("\t").append(errorCode).append("\t").append(fileName).append("\t").append(date);
		//multipleOutputs.write(new LongWritable(Long.valueOf(lineNumber)), new Text(sb.toString()), fileName);
		context.write(new LongWritable(Long.valueOf(lineNumber)), new Text(sb.toString()));
		}catch(Exception e){
			e.printStackTrace();
		}
		
 	}
	
	@Override
	protected void cleanup(Mapper<LongWritable, Text, LongWritable, Text>.Context context) throws IOException,
			InterruptedException {
		
		multipleOutputs.close();
	}

}
