package cshen.data.collection;

import java.util.*;
import java.util.Date;
import java.util.Map.*;
import java.sql.*;

import org.apache.log4j.Logger;

import cshen.data.collection.ManagedFeeds.FeedMeta;
import de.nava.informa.core.ItemIF;

public class DBPersistentManagedFeeds implements PersistentManagedFeeds {
	Logger log = Logger.getLogger(DBPersistentManagedFeeds.class);
	
	static public void main(String[] args) throws Exception {
		PersistentManagedFeeds db = new DBPersistentManagedFeeds();
		db.loadFeeds();
	}
	
	static private Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection("jdbc:postgresql://onsite.bizrecovery.org:5432/SilverCrab", "bcin", "k9k12ms4");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	public DBPersistentManagedFeeds() {
		
	}
	
	public Map<String, FeedMeta> loadFeeds() throws Exception {
		Map<String, FeedMeta> feeds = new HashMap<String, FeedMeta>();
		Connection conn = getConnection();
		if (conn == null)
			return null;
		
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM feeds");
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				int id = rs.getInt("feed_id");
				String url = rs.getString("feed_url");
				Timestamp lastpub = rs.getTimestamp("last_pub_date");
				Timestamp lastcheck = rs.getTimestamp("last_check_date");
				FeedMeta meta = new FeedMeta(id, lastpub != null ? new Date(lastpub.getTime()) : null, lastcheck != null ? new Date(lastcheck.getTime()) : null);
				feeds.put(url, meta);
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return feeds;
	}
	
	
	public boolean saveFeeds(ManagedFeeds mf) {
		Connection conn = getConnection();
		if (conn == null)
			return false;
		
		boolean su = false;
		try {
			for(String feed : mf.getFeedList()) {
				FeedMeta meta = mf.getMeta(feed);
				if (meta != null) {
					PreparedStatement ps = conn.prepareStatement("Update feeds SET last_pub_date=?, last_check_date=? WHERE feed_id=?");
					ps.setTimestamp(1, meta.getLastPubDate() == null ? null : new Timestamp(meta.getLastPubDate().getTime()));
					ps.setTimestamp(2, meta.getLastCheckDate() == null ? null : new Timestamp(meta.getLastCheckDate().getTime()));
					ps.setInt(3, meta.getId());
					ps.executeUpdate();
				}
			}
			
			for(Entry<String, List<ItemIF>> entry : mf.getNewItemsOfFeeds().entrySet()) {
				String feed = entry.getKey();
				FeedMeta meta = mf.getMeta(feed);
				List<ItemIF> items = entry.getValue();
				for(ItemIF item : items) {
					String url = "";
					if (item.getGuid() != null)
						url = item.getGuid().getLocation();
					if (url.equals("") || !url.startsWith("http")) 
						url = item.getLink().toString();
					
					PreparedStatement ps = conn.prepareStatement("INSERT INTO feed_items (feed_id, url, title, pub_date, description) VALUES (?,?,?,?,?)");
					ps.setInt(1, meta.getId());
					ps.setString(2, url);
					ps.setString(3, item.getTitle());
					ps.setTimestamp(4, new Timestamp(item.getDate().getTime()));
					ps.setString(5, item.getDescription());
					ps.executeUpdate();
				}
			}
			
			su = true;
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return su;
		
	}
	
}
