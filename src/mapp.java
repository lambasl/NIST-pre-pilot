
import java.io.IOException;



import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class mapp extends Mapper<LongWritable, Text, Text, NullWritable> {
	
	NullWritable nw = NullWritable.get();
	// ====================================
	// MAPPING
	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		// "38.7873 -77.1532 38.7978923322 -77.147105071 2015-03-01T00:00:00
		// 2015-03-31T23:59:59"
		// Separating terms
		String line = value.toString();
		String[] parsing = line.split("\t");
		try {
			String st_lat = parsing[0];
			String st_lon = parsing[1];
			String nd_lat = parsing[2];
			String nd_lon = parsing[3];
			// 1) Get Api Info
			// 2) Parse to get lanes and distance
			String kee = st_lat.concat(","+st_lon).concat(","+nd_lat).concat(","+nd_lon);
			context.write(new Text(kee), nw);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

}
