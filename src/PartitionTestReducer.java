

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

public class PartitionTestReducer extends Reducer<LongWritable, Text, NullWritable, Text>{
	
	MultipleOutputs<NullWritable, Text> multipleOutput;

	@Override
	protected void setup(Reducer<LongWritable, Text, NullWritable, Text>.Context context) throws IOException,
			InterruptedException {
		multipleOutput = new MultipleOutputs<NullWritable, Text>(context);

	}
	
	@Override
	protected void reduce(LongWritable key, Iterable<Text> values,
			Reducer<LongWritable, Text, NullWritable, Text>.Context arg2) throws IOException, InterruptedException {
		
		String[] splits = values.iterator().next().toString().split("\\t");
		multipleOutput.write(NullWritable.get(), new Text(splits[0] + "\t" + splits[1] + "\t" +  splits[2]), splits[3]);
		//arg2.write(NullWritable.get(), new Text(splits[0] + "\t" + splits[1] + "\t" +  splits[2]));

	}
	
	@Override
	protected void cleanup(Reducer<LongWritable, Text, NullWritable, Text>.Context context) throws IOException,
			InterruptedException {
		multipleOutput.close();
	}

}
