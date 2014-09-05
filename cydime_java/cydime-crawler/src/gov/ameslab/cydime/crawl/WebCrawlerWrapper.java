/*
 * Copyright (c) 2014 Iowa State University
 * All rights reserved.
 * 
 * Copyright 2014.  Iowa State University.  This software was produced under U.S.
 * Government contract DE-AC02-07CH11358 for The Ames Laboratory, which is 
 * operated by Iowa State University for the U.S. Department of Energy.  The U.S.
 * Government has the rights to use, reproduce, and distribute this software.
 * NEITHER THE GOVERNMENT NOR IOWA STATE UNIVERSITY MAKES ANY WARRANTY, EXPRESS
 * OR IMPLIED, OR ASSUMES ANY LIABILITY FOR THE USE OF THIS SOFTWARE.  If 
 * software is modified to produce derivative works, such modified software 
 * should be clearly marked, so as not to confuse it with the version available
 * from The Ames Laboratory.  Additionally, redistribution and use in source and
 * binary forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 * 
 * 1.  Redistribution of source code must retain the above copyright notice, this
 * list of conditions, and the following disclaimer.
 * 2.  Redistribution in binary form must reproduce the above copyright notice, 
 * this list of conditions, and the following disclaimer in the documentation 
 * and/or other materials provided with distribution.
 * 3.  Neither the name of Iowa State University, The Ames Laboratory, the
 * U.S. Government, nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission
 * 
 * THIS SOFTWARE IS PROVIDED BY IOWA STATE UNIVERSITY AND CONTRIBUTORS "AS IS",
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL IOWA STATE UNIVERSITY OF CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITRY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */

package gov.ameslab.cydime.crawl;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import gov.ameslab.cydime.label.DomainDatabase;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.NetUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class WebCrawlerWrapper {

	private CrawlController[] mQueues;
	
	private void run(String[] args) throws Exception {
		if (args.length == 0) {
			DomainDatabase domainDB = DomainDatabase.readCSV(Config.INSTANCE.getPath(Config.NEW_IP_DOM_FILE));
			List<String> urls = domainDB.getAllDomainSuffixes(4, 20);
			urls = filterURLs(urls);
			Collections.shuffle(urls);			
			run(urls,
				Config.INSTANCE.getPath(Config.WEB_RAW_PATH),
				Config.INSTANCE.getPath(Config.WEB_OUTPUT_PATH)
				);
			
		} else if (args.length == 1 && "-mission".equalsIgnoreCase(args[0])) {
			List<String> urls = FileUtils.readLines(new File(Config.INSTANCE.getPath(Config.MISSION_DOM_FILE)));
			run(urls,
				Config.INSTANCE.getPath(Config.MISSION_RAW_PATH),
				Config.INSTANCE.getPath(Config.MISSION_OUTPUT_PATH)
				);
			
		} else {
			System.out.println("Usage: java -cp ORCSCrawler.jar WebCrawlerWrapper [-mission]");
		}
	}

	private void run(List<String> urls, String rawPath, String outputPath) throws Exception {
		int concurrentCrawlers = Config.INSTANCE.getInt(Config.CONCURRENT_CRAWLERS);
		int crawlerThreads = Config.INSTANCE.getInt(Config.CRAWLER_THREADS);
		int maxPagesPerHost = Config.INSTANCE.getInt(Config.MAX_PAGES_PER_HOST);
		int politenessDelay = Config.INSTANCE.getInt(Config.POLITENESS_DELAY);
		
		File dir = new File(outputPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		System.out.println("Total URLs to crawl: " + urls.size());
		
		mQueues = new CrawlController[concurrentCrawlers];
		int startIndex = 0;
		for (; startIndex < mQueues.length && startIndex < urls.size(); startIndex++) {
			mQueues[startIndex] = makeCrawler(rawPath, outputPath, maxPagesPerHost, politenessDelay, startIndex, urls.get(startIndex));
			System.out.println("Starting " + startIndex + ": " + urls.get(startIndex) + " ...");
			mQueues[startIndex].startNonBlocking(BasicCrawler.class, crawlerThreads);
		}
		
		for (int finishIndex = 0; finishIndex < urls.size(); finishIndex++, startIndex++) {
			int actualIndex = finishIndex % concurrentCrawlers;
			mQueues[actualIndex].waitUntilFinish();
			FileUtils.deleteDirectory(new File(rawPath + actualIndex + "/frontier"));
			System.out.println("Finished " + finishIndex + ": " + urls.get(finishIndex));
			
			if (startIndex >= urls.size()) continue;
			mQueues[actualIndex] = makeCrawler(rawPath, outputPath, maxPagesPerHost, politenessDelay, actualIndex, urls.get(startIndex));
			System.out.println("Starting " + startIndex + ": " + urls.get(startIndex) + " ...");
			mQueues[actualIndex].startNonBlocking(BasicCrawler.class, crawlerThreads);
		}
	}

	private List<String> filterURLs(List<String> urls) {
		List<String> filtered = CUtil.makeList();
		for (String url : urls) {
			if (NetUtil.maybeOrgDomain(url)) {
				filtered.add(url);
			}
		}
		return filtered;
	}

	private static CrawlController makeCrawler(String rawPath, String outputPath, int maxPagesPerHost, int politenessDelay, int id, String url) throws Exception {
		url = "http://" + url;
		
		/*
		 * crawlStorageFolder is a folder where intermediate crawl data is
		 * stored.
		 */
		String crawlStorageFolder = rawPath + id;

		CrawlConfig config = new CrawlConfig();

		config.setCrawlStorageFolder(crawlStorageFolder);

		/*
		 * Be polite: Make sure that we don't send more than 1 request per
		 * second (1000 milliseconds between requests).
		 */
		config.setPolitenessDelay(politenessDelay);
		
		/*
		 * You can set the maximum crawl depth here. The default value is -1 for
		 * unlimited depth
		 */
		config.setMaxDepthOfCrawling(1);

		/*
		 * You can set the maximum number of pages to crawl. The default value
		 * is -1 for unlimited number of pages
		 */
		config.setMaxPagesToFetch(maxPagesPerHost);

		/*
		 * Do you need to set a proxy? If so, you can use:
		 * config.setProxyHost("proxyserver.example.com");
		 * config.setProxyPort(8080);
		 * 
		 * If your proxy also needs authentication:
		 * config.setProxyUsername(username); config.getProxyPassword(password);
		 */

		/*
		 * This config parameter can be used to set your crawl to be resumable
		 * (meaning that you can resume the crawl from a previously
		 * interrupted/crashed crawl). Note: if you enable resuming feature and
		 * want to start a fresh crawl, you need to delete the contents of
		 * rootFolder manually.
		 */
		config.setResumableCrawling(false);

		config.setIncludeHttpsPages(true);
		
		config.setFollowRedirects(true);

		config.setSocketTimeout(5000);
		config.setConnectionTimeout(5000);
		
		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		robotstxtConfig.setEnabled(false);
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
		
		controller.setCustomData(new PathConfig(url, outputPath));	
		/*
		 * For each crawl, you need to add some seed urls. These are the first
		 * URLs that are fetched and then the crawler starts following links
		 * which are found in these pages
		 */
		controller.addSeed(url);
		return controller;
	}

	public static void main(String[] args) throws Exception {
		new WebCrawlerWrapper().run(args);
	}
	
}
