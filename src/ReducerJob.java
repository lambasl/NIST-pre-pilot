import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class ReducerJob extends Reducer<Text, Text, NullWritable, Text> {
	private Text outKey = new Text();
	private Text outValue = new Text();
    private NullWritable nw = NullWritable.get();

    
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
			throws IOException, InterruptedException {
		double result = 0.0;
		double intercept = -3.2593147261757949;
		for (DoubleWritable v : values) {

			result += v.get();
		}
		result += intercept;

		outValue.set(String.valueOf(result));
		outKey.set(key);
		context.write(nw, outValue);
	}

}
