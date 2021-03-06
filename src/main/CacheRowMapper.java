package main;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class CacheRowMapper implements RowMapper {
	public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
		Cache cache = new Cache();
		cache.setKey(rs.getString("KEY"));
		cache.setValue(rs.getString("VALUE"));
		return cache;
	}
}