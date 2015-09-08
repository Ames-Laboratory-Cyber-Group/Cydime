package gov.ameslab.cydime.cc;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Histogram;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;

public class DocCosineMapper extends Mapper<Text, ArchiveReader, Text, DoubleWritable> {
	
	static final Logger LOG = Logger.getLogger(DocCosineMapper.class);
	
	//http://www.textfixer.com/resources/common-english-words.txt
	private static final Set<String> STOPWORDS = CUtil.asSet("a", "able", "about", "across", "after", "all", "almost", "also", "am", "among", "an", "and", "any", "are", "as", "at", "be", "because", "been", "but", "by", "can", "cannot", "could", "dear", "did", "do", "does", "either", "else", "ever", "every", "for", "from", "get", "got", "had", "has", "have", "he", "her", "hers", "him", "his", "how", "however", "i", "if", "in", "into", "is", "it", "its", "just", "least", "let", "like", "likely", "may", "me", "might", "most", "must", "my", "neither", "no", "nor", "not", "of", "off", "often", "on", "only", "or", "other", "our", "own", "rather", "said", "say", "says", "she", "should", "since", "so", "some", "than", "that", "the", "their", "them", "then", "there", "these", "they", "this", "tis", "to", "too", "twas", "us", "wants", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with", "would", "yet", "you", "your");
	
	private Text mOutKey = new Text();
	private DoubleWritable mOutVal = new DoubleWritable(0.0);
	
	private List<Histogram<String>> mBaseDocs;
	private String[] mBasePrefixes;
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		
		mBaseDocs = CUtil.makeList();
		mBaseDocs.add(loadHistogram(context, "s3n://cc.ameslab.gov/in/www.ameslab.gov"));
		mBaseDocs.add(loadHistogram(context, "s3n://cc.ameslab.gov/in/www.anl.gov"));
		mBaseDocs.add(loadHistogram(context, "s3n://cc.ameslab.gov/in/www.bnl.gov"));
		mBaseDocs.add(loadHistogram(context, "s3n://cc.ameslab.gov/in/www.nrel.gov"));
		mBaseDocs.add(loadHistogram(context, "s3n://cc.ameslab.gov/in/www.ornl.gov"));
		mBaseDocs.add(loadHistogram(context, "s3n://cc.ameslab.gov/in/www.pnl.gov"));
		
		mBasePrefixes = new String[] {
			"M",
			"A",
			"B",
			"N",
			"O",
			"P"
		};
	}
	
	private Histogram<String> loadHistogram(String text) throws IOException {
		Histogram<String> hist = new Histogram<String>();
		StringTokenizer tok = new StringTokenizer(text);
		while (tok.hasMoreTokens()) {
			String word = tok.nextToken().toLowerCase();
			if (!STOPWORDS.contains(word)) {
				hist.increment(word);
			}
		}
		hist.cacheTwoNorm();
		return hist;
	}
	
	private Histogram<String> loadHistogram(Context context, String s3File) throws IOException {
		Configuration conf = context.getConfiguration();
		Path path = new Path(s3File);
		FileSystem fs = path.getFileSystem(conf);
		String content = IOUtils.toString(fs.open(path));
		return loadHistogram(content);
	}

	private String getHostname(String url) {
		if (url.indexOf("@") >= 0) return null;
		
		int begin = url.indexOf("//");
		if (begin < 0) {
			begin = 0;
		} else {
			begin = begin + 2;
		}
		
		int end = url.length();
		int index = url.indexOf("/", begin);
		if (index >= 0 && index < end) {
			end = index;
		}
		
		index = url.indexOf("?", begin);
		if (index >= 0 && index < end) {
			end = index;
		}
		
		index = url.indexOf(":", begin);
		if (index >= 0 && index < end) {
			end = index;
		}
		
		url = url.substring(begin, end);
		while (url.endsWith(".")) {
			url = url.substring(0, url.length() - 1);
		}
		
		return url;
	}
	
	@Override
	public void map(Text key, ArchiveReader value, Context context) throws IOException {
		for (ArchiveRecord r : value) {
			try {
				if (r.getHeader().getMimetype().equals("text/plain")) {
//					LOG.debug(r.getHeader().getUrl() + " -- " + r.available());
					String hostname = getHostname(r.getHeader().getUrl().toLowerCase());
					if (hostname == null) continue;
					
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
							context.write(mOutKey, mOutVal);
						}
					}
				}
			} catch (Exception ex) {
				LOG.error("Caught Exception", ex);
			}
		}
	}

}