import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Job3Reducer extends Reducer<Text, Text, Text, Text>{
	private Text outVal = new Text();
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
		String[] tokens = key.toString().split("|");
		StringBuilder str = new StringBuilder();
		for(int i = 0 ; i < tokens.length; ++i){
			str.append(tokens[i]);
			if(i < tokens.length - 1)
				str.append("|");
		}
		outVal.set(str.toString());
		context.write(key, outVal);
	}

}

