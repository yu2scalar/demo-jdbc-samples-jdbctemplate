package com.example.demo_jdbc_samples.repository;

import com.example.demo_jdbc_samples.config.SqlDialect;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.OrderingStatements;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * ordering.statements with JdbcTemplate (primary key order_id, line_no). Runs in the
 * caller's transaction (TransactionRunner). Same operations and error codes as the
 * ScalarDB version: get of a missing row 9700, update / delete of a missing row 9100.
 */
@Repository
public class OrderingStatementsRepository {
    private static final String TABLE = OrderingStatements.NAMESPACE + "." + OrderingStatements.TABLE;
    private static final String COLUMNS = "order_id, line_no, item_id, qty, unit_price";
    private static final RowMapper<OrderingStatements> ROW_MAPPER = (rs, rowNum) -> OrderingStatements.builder()
        .orderId(rs.getString(OrderingStatements.ORDER_ID))
        .lineNo(rs.getInt(OrderingStatements.LINE_NO))
        .itemId(rs.getInt(OrderingStatements.ITEM_ID))
        .qty(rs.getInt(OrderingStatements.QTY))
        .unitPrice(rs.getLong(OrderingStatements.UNIT_PRICE))
        .build();

    private final JdbcTemplate jdbcTemplate;
    private final String upsertSql;

    private int scanLimit = 100; // Default scan limit

    public OrderingStatementsRepository(JdbcTemplate jdbcTemplate, SqlDialect dialect) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSql = dialect.upsert(TABLE, List.of(OrderingStatements.ORDER_ID, OrderingStatements.LINE_NO),
            List.of(OrderingStatements.ITEM_ID, OrderingStatements.QTY, OrderingStatements.UNIT_PRICE));
    }

    public void setScanLimit(int scanLimit) {
        this.scanLimit = scanLimit;
    }

    public int getScanLimit() {
        return scanLimit;
    }

    // Get Record by Primary Key
    public OrderingStatements getOrderingStatements(OrderingStatements statements) {
        List<OrderingStatements> result = jdbcTemplate.query(
            "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE order_id = ? AND line_no = ?",
            ROW_MAPPER, statements.getOrderId(), statements.getLineNo());
        if (result.isEmpty()) {
            throw new CustomException("No record found in OrderingStatements", 9700);
        }
        return result.get(0);
    }

    // Insert Record (a duplicate key fails)
    public OrderingStatements insertOrderingStatements(OrderingStatements statements) {
        jdbcTemplate.update("INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
            statements.getOrderId(), statements.getLineNo(), statements.getItemId(), statements.getQty(), statements.getUnitPrice());
        return statements;
    }

    // Update Record (if exists)
    public OrderingStatements updateOrderingStatements(OrderingStatements statements) {
        int rows = jdbcTemplate.update("UPDATE " + TABLE + " SET item_id = ?, qty = ?, unit_price = ? WHERE order_id = ? AND line_no = ?",
            statements.getItemId(), statements.getQty(), statements.getUnitPrice(), statements.getOrderId(), statements.getLineNo());
        if (rows == 0) {
            throw new CustomException("Update condition not met: no record in OrderingStatements", 9100);
        }
        return statements;
    }

    // Upsert Record
    public OrderingStatements upsertOrderingStatements(OrderingStatements statements) {
        jdbcTemplate.update(upsertSql,
            statements.getOrderId(), statements.getLineNo(), statements.getItemId(), statements.getQty(), statements.getUnitPrice());
        return statements;
    }

    // Delete Record (if exists)
    public void deleteOrderingStatements(OrderingStatements statements) {
        int rows = jdbcTemplate.update("DELETE FROM " + TABLE + " WHERE order_id = ? AND line_no = ?",
            statements.getOrderId(), statements.getLineNo());
        if (rows == 0) {
            throw new CustomException("Delete condition not met: no record in OrderingStatements", 9100);
        }
    }

    // Scan All Records (up to scanLimit, no order)
    public List<OrderingStatements> getOrderingStatementsListAll() {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + TABLE + " FETCH FIRST ? ROWS ONLY",
            ROW_MAPPER, scanLimit);
    }

    // Scan Records by Partition Key (order_id), in line_no order as the ScalarDB clustering key
    public List<OrderingStatements> getOrderingStatementsListByPk(OrderingStatements statements) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE order_id = ? ORDER BY line_no FETCH FIRST ? ROWS ONLY",
            ROW_MAPPER, statements.getOrderId(), scanLimit);
    }

    public void truncate() {
        jdbcTemplate.execute("TRUNCATE TABLE " + TABLE);
    }
}
