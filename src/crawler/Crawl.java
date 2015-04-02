package crawler;

import util.Environment;
import util.Printer;
import edu.uci.ics.crawler4j.crawler.Configurable;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Crawl {
	
	private final static Printer printer = new Printer(true);
	
	public static void main(String[] args) throws Exception {
		String resources = Environment.resources();
        String crawlStorageFolder = resources + "/crawl/root";
        int numberOfCrawlers = 7;
        
//		String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:36.0) Gecko/20100101 Firefox/36.0";
        String userAgent = "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0";
        
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setUserAgentString(userAgent);
        
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        robotstxtConfig.setUserAgentName(userAgent);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        controller.addSeed("http://www.mitpressjournals.org/toc/coli/40/4");
        printer.print("Crawling ... ");
        controller.start(Crawler.class, numberOfCrawlers);
        printer.println("[x]");
    }
}
