package gov.ameslab.cydime.cc;

import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class DoubleStatReducer extends Reducer<Text, DoubleWritable, Text, Text> {

	private Text mResult = new Text("");
	
	@Override
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
		int count = 0;
		double sum = 0.0;
		double sumsq = 0.0;
		double max = 0.0;
		for (DoubleWritable v : values) {
			double value = v.get();
			count++;
			sum += value;
			sumsq += value * value;
			if (value > max) {
				max = value;
			}
		}
		
		double avg = 0.0;
		double var = 0.0;
		if (count > 0) {
			avg = sum / count;
			var = sumsq / count - avg * avg;
		}
		
		mResult.set(count + "\t" + avg + "\t" + var + "\t" + max);
		context.write(key, mResult);
	}
	
}
