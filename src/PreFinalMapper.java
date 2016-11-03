import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.Mapper;


public class PreFinalMapper extends Mapper<LongWritable, Text, LongWritable, Text>{
	
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
		String  subKey = keyString.substring(2);
		String fileName = "cleaning_subm_"+ subKey + "_NIST6.txt";
		sb.append(flag).append("\t").append(flow).append("\t").append(errorCode).append("\t").append(fileName);
		multipleOutputs.write(new LongWritable(Long.valueOf(lineNumber)), new Text(sb.toString()), fileName);
		
		}catch(Exception e){
			System.out.println("");
		}
		
 	}
	
	@Override
	protected void cleanup(Mapper<LongWritable, Text, LongWritable, Text>.Context context) throws IOException,
			InterruptedException {
		
		multipleOutputs.close();
	}
}
