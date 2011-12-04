package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import vis.data.model.AutoCompleteEntry;
import vis.data.model.AutoCompleteEntry.Type;
import vis.data.model.AutoCompleteTerm;
import vis.data.util.SQL;
import vis.data.util.SQL.NullForLastRowProcessor;

public class AutoCompleteAccessor {
	PreparedStatement insert_, query_, queryLimit_, queryType_, queryTypeLimit_;
	public AutoCompleteAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public AutoCompleteAccessor(Connection conn) throws SQLException {
		String columns = AutoCompleteEntry.TERM_ID + "," + AutoCompleteTerm.TERM + "," + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.REFERENCE_ID + "," + AutoCompleteEntry.SCORE;
		query_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + 
			" JOIN " + AutoCompleteTerm.TABLE + " ON " + AutoCompleteTerm.ID + "=" + AutoCompleteEntry.TERM_ID + 
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?" +
			" ORDER BY " + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.SCORE + " DESC");
		queryLimit_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + 
			" JOIN " + AutoCompleteTerm.TABLE + " ON " + AutoCompleteTerm.ID + "=" + AutoCompleteEntry.TERM_ID + 
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?" + 
			" ORDER BY " + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.SCORE + " DESC LIMIT ?");
		queryType_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + 
			" JOIN " + AutoCompleteTerm.TABLE + " ON " + AutoCompleteTerm.ID + "=" + AutoCompleteEntry.TERM_ID + 
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?"  + " AND " + AutoCompleteEntry.TYPE + " = ?" +
			" ORDER BY " + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.SCORE + " DESC");
		queryTypeLimit_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + 
			" JOIN " + AutoCompleteTerm.TABLE + " ON " + AutoCompleteTerm.ID + "=" + AutoCompleteEntry.TERM_ID + 
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?" + " AND " + AutoCompleteEntry.TYPE + " = ?" +
			" ORDER BY " + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.SCORE + " DESC LIMIT ?");
		insert_ = conn.prepareStatement("INSERT INTO " + AutoCompleteEntry.TABLE + 
			" (" + AutoCompleteEntry.TERM_ID + "," + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.REFERENCE_ID + "," + AutoCompleteEntry.SCORE + ")" +
			" VALUES (?,?,?,?)");
	}
	public NamedAutoComplete[] lookup(String q) throws SQLException {
		query_.setString(1, q);
		return processResults(query_.executeQuery());
	}
	public NamedAutoComplete[] lookup(String q, AutoCompleteEntry.Type t) throws SQLException {
		queryType_.setString(1, q);
		queryType_.setInt(2, t.ordinal());
		return processResults(queryType_.executeQuery());
	}
	public NamedAutoComplete[] lookup(String q, int limit) throws SQLException {
		queryLimit_.setString(1, q);
		queryLimit_.setInt(2, limit);
		return processResults(queryLimit_.executeQuery());
	}
	public NamedAutoComplete[] lookup(String q, AutoCompleteEntry.Type t, int limit) throws SQLException {
		queryTypeLimit_.setString(1, q);
		queryTypeLimit_.setInt(2, t.ordinal());
		queryTypeLimit_.setInt(3, limit);
		return processResults(queryTypeLimit_.executeQuery());
	}
	public static class NamedAutoComplete extends AutoCompleteEntry {
		public String term_;
	}
	static NamedAutoComplete[] processResults(ResultSet rs) throws SQLException {
		try {
			List<NamedAutoComplete> res = new LinkedList<NamedAutoComplete>();
			while(rs.next()) {
				NamedAutoComplete ac = new NamedAutoComplete();
				ac.termId_ = rs.getInt(1);
				ac.term_ = rs.getString(2);
				ac.type_ = AutoCompleteEntry.Type.values()[rs.getInt(3)];
				ac.referenceId_ = rs.getInt(4);
				ac.score_ = rs.getInt(5);
				res.add(ac);
			}
			return res.toArray(new NamedAutoComplete[res.size()]);
		} finally {
			rs.close();
		}
	}
	public void addAutoComplete(int term_id, Type type, int ref, int score) throws SQLException {
		insert_.setInt(1, term_id);
		insert_.setInt(2, type.ordinal());
		insert_.setInt(3, ref);
		insert_.setInt(4, score);
		insert_.executeUpdate();
	}
	public void addAutoCompleteBatch(int term_id, Type type, int ref, int score) throws SQLException {
		insert_.setInt(1, term_id);
		insert_.setInt(2, type.ordinal());
		insert_.setInt(3, ref);
		insert_.setInt(4, score);
		insert_.addBatch();
	}
	public static class ResultSetIterator extends org.apache.commons.dbutils.ResultSetIterator {
		public ResultSetIterator(ResultSet rs) {
			super(rs, new NullForLastRowProcessor());
		}
		
		public NamedAutoComplete nextLemma() {
			Object fields[] = super.next();
			if(fields == null)
				return null;
			NamedAutoComplete ac = new NamedAutoComplete();
			ac.termId_ = (Integer)fields[0];
			ac.term_ = (String)fields[1];
			ac.type_ = AutoCompleteEntry.Type.values()[(Integer)fields[2]];
			ac.referenceId_ = (Integer)fields[3];
			ac.score_ = (Integer)fields[4];
			return ac;
		}
	}
	public ResultSetIterator autoCompleteIterator(String q) throws SQLException {
		int fetch_size = query_.getFetchSize();
		try {
			query_.setFetchSize(Integer.MIN_VALUE);
			query_.setString(1, q);
			return new ResultSetIterator(query_.executeQuery());
		} finally {
			query_.setFetchSize(fetch_size);
		}
	}
	public ResultSetIterator autoCompleteIterator(String q, AutoCompleteEntry.Type t) throws SQLException {
		int fetch_size = query_.getFetchSize();
		try {
			queryType_.setString(1, q);
			queryType_.setInt(2, t.ordinal());
			return new ResultSetIterator(queryType_.executeQuery());
		} finally {
			queryType_.setFetchSize(fetch_size);
		}
	}
	public ResultSetIterator autoCompleteIterator(String q, int limit) throws SQLException {
		int fetch_size = query_.getFetchSize();
		try {
			queryLimit_.setString(1, q);
			queryLimit_.setInt(2, limit);
			return new ResultSetIterator(queryLimit_.executeQuery());
		} finally {
			queryLimit_.setFetchSize(fetch_size);
		}
	}
	public ResultSetIterator autoCompleteIterator(String q, AutoCompleteEntry.Type t, int limit) throws SQLException {
		int fetch_size = query_.getFetchSize();
		try {
			queryTypeLimit_.setString(1, q);
			queryTypeLimit_.setInt(2, t.ordinal());
			queryTypeLimit_.setInt(3, limit);
			return new ResultSetIterator(queryTypeLimit_.executeQuery());
		} finally {
			queryTypeLimit_.setFetchSize(fetch_size);
		}
	}
}