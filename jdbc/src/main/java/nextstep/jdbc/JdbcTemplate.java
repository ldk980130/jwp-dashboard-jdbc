package nextstep.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcTemplate {

	private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

	private final DataSource dataSource;

	public JdbcTemplate(final DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public SqlBuilder createQuery(final String sql) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dataSource.getConnection();
			pstmt = conn.prepareStatement(sql);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			close(null, conn, pstmt);
			throw new DataAccessException(e);
		}
		return new SqlBuilder(conn, pstmt);
	}

	public static class SqlBuilder {

		private final Connection conn;
		private final PreparedStatement pstmt;
		private ResultSet rs;

		public SqlBuilder(final Connection conn, final PreparedStatement pstmt) {
			this.conn = conn;
			this.pstmt = pstmt;
		}

		public SqlBuilder setString(final int parameterIndex, final String parameter) {
			try {
				pstmt.setString(parameterIndex, parameter);
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
				close(rs, conn, pstmt);
				throw new DataAccessException(e);
			}
			return this;
		}

		public SqlBuilder setLong(final int parameterIndex, final Long parameter) {
			try {
				pstmt.setLong(parameterIndex, parameter);
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
				close(rs, conn, pstmt);
				throw new DataAccessException(e);
			}
			return this;
		}

		public void executeUpdate() {
			try {
				pstmt.executeUpdate();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
				throw new DataAccessException(e);
			} finally {
				close(rs, conn, pstmt);
			}
		}

		public SqlBuilder executeQuery() {
			try {
				this.rs = pstmt.executeQuery();
				return this;
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
				close(rs, conn, pstmt);
				throw new DataAccessException(e);
			}
		}

		public <T> List<T> getResultList(final RowMapper<T> rowMapper) {
			try {
				List<T> results = new ArrayList<>();
				while (rs.next()) {
					results.add(rowMapper.mapRow(rs));
				}
				return results;
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
				throw new DataAccessException(e);
			} finally {
				close(rs, conn, pstmt);
			}
		}

		public <T> T getResult(final RowMapper<T> rowMapper) {
			try {
				if (rs.next()) {
					return rowMapper.mapRow(rs);
				}
				return null;
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
				throw new DataAccessException(e);
			} finally {
				close(rs, conn, pstmt);
			}
		}
	}

	private static void close(final ResultSet rs, final Connection conn, final PreparedStatement pstmt) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			log.debug("Could not close ResultSet", e);
		}
		try {
			if (pstmt != null) {
				pstmt.close();
			}
		} catch (SQLException e) {
			log.debug("Could not close PreparedStatement", e);
		}
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			log.debug("Could not close Connection", e);
		}
	}
}
