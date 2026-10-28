package com.example.demo_jdbc_samples.repository;

import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.OrderingOrders;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * ordering.orders with JdbcTemplate. Runs in the caller's transaction
 * (TransactionRunner). Same operations and error codes as the ScalarDB version:
 * get of a missing row 9700, update / delete of a missing row 9100.
 */
@Repository
public class OrderingOrdersRepository {
    private static final String TABLE = OrderingOrders.NAMESPACE + "." + OrderingOrders.TABLE;
    private static final String COLUMNS = "order_id, customer_id, created_at, status, total_amount";
    private static final RowMapper<OrderingOrders> ROW_MAPPER = (rs, rowNum) -> OrderingOrders.builder()
        .orderId(rs.getString(OrderingOrders.ORDER_ID))
        .customerId(rs.getInt(OrderingOrders.CUSTOMER_ID))
        .createdAt(rs.getLong(OrderingOrders.CREATED_AT))
        .status(rs.getString(OrderingOrders.STATUS))
        .totalAmount(rs.getLong(OrderingOrders.TOTAL_AMOUNT))
        .build();

    private final JdbcTemplate jdbcTemplate;

    private int scanLimit = 100; // Default scan limit

    public OrderingOrdersRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setScanLimit(int scanLimit) {
        this.scanLimit = scanLimit;
    }

    public int getScanLimit() {
        return scanLimit;
    }

    // Get Record by Primary Key
    public OrderingOrders getOrderingOrders(OrderingOrders orders) {
        return getOne("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE order_id = ?", orders);
    }

    // Get Record by Primary Key and lock it until the end of the transaction
    public OrderingOrders getOrderingOrdersForUpdate(OrderingOrders orders) {
        return getOne("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE order_id = ? FOR UPDATE", orders);
    }

    private OrderingOrders getOne(String sql, OrderingOrders orders) {
        List<OrderingOrders> result = jdbcTemplate.query(sql, ROW_MAPPER, orders.getOrderId());
        if (result.isEmpty()) {
            throw new CustomException("No record found in OrderingOrders", 9700);
        }
        return result.get(0);
    }

    // Insert Record (a duplicate key fails)
    public OrderingOrders insertOrderingOrders(OrderingOrders orders) {
        jdbcTemplate.update("INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
            orders.getOrderId(), orders.getCustomerId(), orders.getCreatedAt(), orders.getStatus(), orders.getTotalAmount());
        return orders;
    }

    // Update Record (if exists)
    public OrderingOrders updateOrderingOrders(OrderingOrders orders) {
        if (updateRow(orders) == 0) {
            throw new CustomException("Update condition not met: no record in OrderingOrders", 9100);
        }
        return orders;
    }

    // Upsert Record: update, or insert if there is no such row. No ON CONFLICT / MERGE, so the
    // same SQL runs on PostgreSQL, Oracle and through ScalarDB Native JDBC (which refuses them).
    // A concurrent insert of the same key fails with a duplicate key.
    public OrderingOrders upsertOrderingOrders(OrderingOrders orders) {
        if (updateRow(orders) == 0) {
            insertOrderingOrders(orders);
        }
        return orders;
    }

    private int updateRow(OrderingOrders orders) {
        return jdbcTemplate.update("UPDATE " + TABLE + " SET customer_id = ?, created_at = ?, status = ?, total_amount = ? WHERE order_id = ?",
            orders.getCustomerId(), orders.getCreatedAt(), orders.getStatus(), orders.getTotalAmount(), orders.getOrderId());
    }

    // Delete Record (if exists)
    public void deleteOrderingOrders(OrderingOrders orders) {
        int rows = jdbcTemplate.update("DELETE FROM " + TABLE + " WHERE order_id = ?", orders.getOrderId());
        if (rows == 0) {
            throw new CustomException("Delete condition not met: no record in OrderingOrders", 9100);
        }
    }

    // Scan All Records (up to scanLimit, no order)
    public List<OrderingOrders> getOrderingOrdersListAll() {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + TABLE + " FETCH FIRST ? ROWS ONLY",
            ROW_MAPPER, scanLimit);
    }

    // Scan Records by Primary Key
    public List<OrderingOrders> getOrderingOrdersListByPk(OrderingOrders orders) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE order_id = ? FETCH FIRST ? ROWS ONLY",
            ROW_MAPPER, orders.getOrderId(), scanLimit);
    }

    // All orders of a customer (index on customer_id; no limit, as the ScalarDB version)
    public List<OrderingOrders> getOrderingOrdersListByCustomerId(int customerId) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE customer_id = ?",
            ROW_MAPPER, customerId);
    }

    public void truncate() {
        // DELETE, not TRUNCATE: ScalarDB Native JDBC refuses TRUNCATE on a ScalarDB table
        jdbcTemplate.update("DELETE FROM " + TABLE);
    }
}
