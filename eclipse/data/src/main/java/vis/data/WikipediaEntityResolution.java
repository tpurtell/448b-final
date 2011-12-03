package vis.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import vis.data.model.WikiPage;
import vis.data.model.WikiRedirect;
import vis.data.model.meta.WikiRedirectAccessor;
import vis.data.util.SQL;
import vis.data.util.StringArrayResultSetIterator;

public class WikipediaEntityResolution {
	public static void pages() {
		if(SQL.tableExists(WikiPage.TABLE))
			return;
		HttpClient hc = new DefaultHttpClient();
        File f = new File("extra/enwiki-latest-page.sql");
        if(!f.exists()) {
	        try {
	            HttpGet hg = new HttpGet("http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-page.sql.gz");
	            System.out.println("downloading wikipedia dump " + hg.getURI());
	            HttpResponse hr = hc.execute(hg);
	            InputStream in = hr.getEntity().getContent();
	            byte[] buffer = new byte[1024*1024];
	            if(!f.createNewFile())
	            	throw new RuntimeException("failed to open file");
	            OutputStream out = new FileOutputStream(f);
	            GZIPInputStream gzin = new GZIPInputStream(in);
	            for(;;) {
	            	int bytes_read = gzin.read(buffer);
	            	if(bytes_read == -1)
	            		break;
	            	out.write(buffer, 0, bytes_read);
	            }
	            out.close();
	        } catch (ClientProtocolException e) {
	        	throw new RuntimeException("protocol failure downloading", e);
			} catch (IOException e) {
	        	throw new RuntimeException("ioexception failure downloading", e);
			} finally {
	            hc.getConnectionManager().shutdown();
	        }
        }
        SQL.importMysqlDump(f);
	}
	public static void redirects() {
		if(SQL.tableExists(WikiRedirect.TABLE))
			return;
		HttpClient hc = new DefaultHttpClient();
        File f = new File("extra/enwiki-latest-redirect.sql");
        if(!f.exists()) {
	        try {
	            HttpGet hg = new HttpGet("http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-redirect.sql.gz");
	            System.out.println("downloading wikipedia dump " + hg.getURI());
	            HttpResponse hr = hc.execute(hg);
	            InputStream in = hr.getEntity().getContent();
	            byte[] buffer = new byte[1024*1024];
	            if(!f.createNewFile())
	            	throw new RuntimeException("failed to open file");
	            OutputStream out = new FileOutputStream(f);
	            GZIPInputStream gzin = new GZIPInputStream(in);
	            for(;;) {
	            	int bytes_read = gzin.read(buffer);
	            	if(bytes_read == -1)
	            		break;
	            	out.write(buffer, 0, bytes_read);
	            }
	            out.close();
	        } catch (ClientProtocolException e) {
	        	throw new RuntimeException("protocol failure downloading", e);
			} catch (IOException e) {
	        	throw new RuntimeException("ioexception failure downloading", e);
			} finally {
	            hc.getConnectionManager().shutdown();
	        }
	    }
        SQL.importMysqlDump(f);
	}
	public static void main(String[] args) {
		redirects();
		pages();

		try {
			WikiRedirectAccessor wra = new WikiRedirectAccessor();
			StringArrayResultSetIterator i = wra.redirectIterator();
			String redirect[];
			while((redirect = i.next()) != null) {
				System.out.println(redirect[0] + " => " + redirect[1]);
			}
			
		} catch (SQLException e) {
			throw new RuntimeException("wiki redirect processing failed");
		}
	}

}
