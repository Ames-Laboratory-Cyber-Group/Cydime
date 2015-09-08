package profile;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Histogram;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;

public class DocCosineMapper extends Mapper<Text, ArchiveReader, Text, DoubleWritable> {
	
	static final Logger LOG = Logger.getLogger(DocCosineMapper.class);
	
	private Text mOutKey = new Text();
	private DoubleWritable mOutVal = new DoubleWritable(0.0);
	
	private List<Histogram<String>> mBaseDocs;
	private String[] mBasePrefixes;
	
	public void setup() throws IOException, InterruptedException, URISyntaxException {
		mBaseDocs = CUtil.makeList();
		mBaseDocs.add(loadHistogramSeed("data/www.ameslab.gov"));
		mBaseDocs.add(loadHistogramSeed("data/www.anl.gov"));
		mBaseDocs.add(loadHistogramSeed("data/www.bnl.gov"));
		mBaseDocs.add(loadHistogramSeed("data/www.nrel.gov"));
		mBaseDocs.add(loadHistogramSeed("data/www.ornl.gov"));
		mBaseDocs.add(loadHistogramSeed("data/www.pnl.gov"));
		
		mBasePrefixes = new String[] {
			"M",
			"A",
			"B",
			"N",
			"O",
			"P"
		};
	}
	
	private Histogram<String> loadHistogramSeed(String s3File) throws IOException, URISyntaxException {
		String content = FileUtils.readFileToString(new File(s3File));
		return loadHistogram(content);
	}

	private Histogram<String> loadHistogram(String text) throws IOException {
		Histogram<String> hist = new Histogram<String>();
		StringTokenizer tok = new StringTokenizer(text);
		while (tok.hasMoreTokens()) {
			hist.increment(tok.nextToken().toLowerCase());
		}
		hist.cacheTwoNorm();
		return hist;
	}
	
	private String getHostname(String url) {
		int begin = url.indexOf("//");
		if (begin < 0) {
			begin = 0;
		} else {
			begin = begin + 2;
		}
		
		int end = url.indexOf("/", begin);
		if (end < 0) {
			end = url.length();
		}
		
		return url.substring(begin, end);
	}
	
	public void map(Text key, ArchiveReader value) throws IOException {
		for (ArchiveRecord r : value) {
			try {
				if (r.getHeader().getMimetype().equals("text/plain")) {
//					LOG.debug(r.getHeader().getUrl() + " -- " + r.available());
					String hostname = getHostname(r.getHeader().getUrl().toLowerCase());
					
					String content = IOUtils.toString(r);
					Histogram<String> doc = loadHistogram(content);
					String outKey = null;
					double cosine = 0.0;
					if (doc.getTwoNorm() > 0.0) {
						for (int i = 0; i < mBasePrefixes.length; i++) {
							outKey = mBasePrefixes[i] + hostname;
							cosine = Histogram.getCosine(mBaseDocs.get(i), doc);
							
							mOutKey.set(outKey);
							mOutVal.set(cosine);
						}
					}
				}
			} catch (Exception ex) {
				LOG.error("Caught Exception", ex);
			}
		}
	}

}