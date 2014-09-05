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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class BasicCrawler extends WebCrawler {

	private final static Pattern FILTERS = Pattern.compile(
			".*(\\.(css|js|bmp|gif|jpe?g"
			+ "|ico|png|tiff?|mid|mp2|mp3|mp4"
			+ "|wav|avi|mov|mpeg|ram|m4v|pdf"
			+ "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

	private PathConfig mPathConfig;
	
	@Override
    public void onStart() {
		mPathConfig = (PathConfig) myController.getCustomData();		
		File dir = new File(mPathConfig.getStorePath());
		dir.mkdirs();
    }
	
	private boolean isWithinDomain(String url) {
		try {
			URL u = new URL(url);
			String host = u.getHost();
			String org = getOrgDomain(host);
			return mPathConfig.getURL().contains(org);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getOrgDomain(String domain) {
		String[] split = domain.split("\\.");
		if (split.length <= 2) {
			return domain;
		} else {
			String last2 = split[split.length - 2] + "." + split[split.length - 1];
			if (last2.length() > 6) {
				return last2;
			} else {
				return split[split.length - 3] + "." + last2;
			}
		}
	}
	
	@Override
	public boolean shouldVisit(WebURL url) {
		String href = url.getURL().toLowerCase();
		return href.length() < 200
				&& href.indexOf(".css") == -1
				&& href.indexOf(".axd") == -1
				&& href.indexOf("css_style") == -1
				&& isWithinDomain(href)
				&& !FILTERS.matcher(href).matches();
	}

	@Override
	public void visit(Page page) {
		System.out.println("Visited " + page.getWebURL().getURL());
		
		if (!(page.getParseData() instanceof HtmlParseData)) return;
		
		HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
//		String text = htmlParseData.getText();
//		String html = htmlParseData.getHtml();
		
		Document doc = Jsoup.parse(htmlParseData.getHtml());
		String text = doc.body().text();
		
		String url = page.getWebURL().getURL();
		String pageStorePath = url.substring(url.indexOf("//") + 2).replaceAll("/", "_");
		
		String file = mPathConfig.getStorePath() + "/" + pageStorePath;
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(text);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}