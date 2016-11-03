

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;

public class DatePartitioner extends Partitioner<LongWritable, Text>{

	@Override
	public int getPartition(LongWritable key, Text value, int numPartitions) {
		
		
		String date = value.toString().split("\t")[4];
		String[] dateSplit = date.split("-");
		int year = Integer.valueOf(dateSplit[0]);
		int month = Integer.valueOf(dateSplit[1]);
		int startYear = 2006;
		int startMpnth = 9;
		int partitionNumber = (year-startYear)*12 + (month - startMpnth); 
		return partitionNumber;
		
	}

}
