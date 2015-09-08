package profile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.archive.io.ArchiveReader;
import org.archive.io.warc.WARCReaderFactory;

public class Runner {
	
	public static void main(String args[]) throws IOException, InterruptedException, URISyntaxException {
		runProfile(3);
	}
	
	public static void runProfile(int times) throws IOException, InterruptedException, URISyntaxException {
		for (int i = 0; i < times; i++) {
			DocCosineMapper docMapper = new DocCosineMapper();
			docMapper.setup();

			String fn = "data/CC-MAIN-20140901014525-00468-ip-10-180-136-8.ec2.internal.warc.wet.gz";
			FileInputStream is = new FileInputStream(fn);
			// The file name identifies the ArchiveReader and indicates if it should be decompressed
			ArchiveReader ar = WARCReaderFactory.get(fn, is, true);

			docMapper.map(null, ar);
			
		}
	}

}
