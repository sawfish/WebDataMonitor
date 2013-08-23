package cshen.data.collection;
/**
 * 
 */


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import twitter4j.FilterQuery;
import twitter4j.RawStreamListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

public class TwitterStreamReader {
	private static Logger log = Logger.getLogger(TwitterStreamReader.class);
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			usage();
			System.exit(1);
		}
		String tweetsFilesDir = args[0];
		String localQueryFile = args.length > 1 ? args[1] : null;
		
		run(tweetsFilesDir, localQueryFile);
	}

	private static void usage() {
		System.err.println("usage TwitterStreamReader <hdfs_tweets_list_dir> [local_query_file]");
		
	}

	private static void run(final String tweetsFilesDir, String localQueryFile) throws IOException, InterruptedException {
		Configuration conf = new Configuration();
		final FileSystem hdfs = FileSystem.get(conf);
		FileSystem local = FileSystem.getLocal(conf);
		FilterQuery query = null;
		
		hdfs.mkdirs(new Path(tweetsFilesDir));
		
		if (localQueryFile != null) {
			List<String> queryPhrases = new ArrayList<String>();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					local.open(new Path(localQueryFile))));
			String line;
			while((line = br.readLine()) != null) {
				line = line.trim();
				if (line.length() != 0)
					queryPhrases.add(line);
			}
			br.close();
			if (queryPhrases.size() != 0)
				query  = new FilterQuery(0, null, queryPhrases.toArray(new String[queryPhrases.size()]));
		}
//		if (query == null) { 
//			query = new FilterQuery();
//			query.locations(new double[][] {{-166.00,70.38,-48.92,17.98}}); // candada and us.
//		}
		
		RawStreamListener listener = new RawStreamListener() {
			int count = 0;
			BufferedWriter bw = null;
			@Override
			public void onException(Exception arg0) {
				log.warn(arg0.getMessage());
			}
			
			@Override
			public void onMessage(String json) {
				try {
					if (bw == null || count == 100 * 1024) {
						if (bw != null)
							bw.close();
						bw = new BufferedWriter(new OutputStreamWriter(
								hdfs.create(new Path(tweetsFilesDir + "/"
										+ "tweets." + new SimpleDateFormat("yyMMddHHmmss").format(new Date()) + ".dat"))));
						count = 0;
					}
					json.replaceAll("\n", " ");
					bw.write(json + "\n");
					count++;
				} catch (IOException e) {
					log.warn(e.getMessage());
					log.warn("missed tweet: " + json);
				}
			}
		};
		
		TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
	    twitterStream.addListener(listener);
	    if (query != null)
	    	twitterStream.filter(query);
	    else
	    	twitterStream.sample();
	}
}
