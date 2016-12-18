package main;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class CacheRepository {

	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl("jdbc:postgresql://localhost:5432/database");
		dataSource.setUsername("justas");
		dataSource.setPassword("slaptazodis");
		return dataSource;
	}

	private DataSource dataSource = dataSource();
	private JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

	public String find(String key) {
		String sql = "SELECT * FROM CACHE WHERE KEY = ?";
		List<Cache> rows = jdbcTemplate.query(sql, new Object[] { key }, new CacheRowMapper());

		if (rows.isEmpty())
			return null;

		return rows.iterator().next().getValue();
	}

	public void insert(String key, String value) {
		String SQL = "insert into CACHE (KEY, VALUE) values (?, ?)";
		jdbcTemplate.update(SQL, new Object[] { key, value });
	}
}
