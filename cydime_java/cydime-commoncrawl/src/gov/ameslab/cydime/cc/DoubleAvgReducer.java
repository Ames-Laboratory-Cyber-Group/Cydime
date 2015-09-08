package gov.ameslab.cydime.cc;

import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class DoubleAvgReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {

	private DoubleWritable mResult = new DoubleWritable();
	
	@Override
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
		int count = 0;
		double result = 0.0;
		for (DoubleWritable v : values) {
			count++;
			result += v.get();
		}
		
		if (count > 0) {
			result /= count;
		}
		
		mResult.set(result);
		context.write(key, mResult);
	}
	
}
