package com.example.demo_jdbc_samples.repository;

import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.InventoryItems;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * inventory.items with JdbcTemplate. Runs in the caller's transaction
 * (TransactionRunner). Same operations and error codes as the ScalarDB version:
 * get of a missing row 9700, update / delete of a missing row 9100.
 */
@Repository
public class InventoryItemsRepository {
    private static final String TABLE = InventoryItems.NAMESPACE + "." + InventoryItems.TABLE;
    private static final String COLUMNS = "item_id, name, price, stock";
    private static final RowMapper<InventoryItems> ROW_MAPPER = (rs, rowNum) -> InventoryItems.builder()
        .itemId(rs.getInt(InventoryItems.ITEM_ID))
        .name(rs.getString(InventoryItems.NAME))
        .price(rs.getLong(InventoryItems.PRICE))
        .stock(rs.getInt(InventoryItems.STOCK))
        .build();

    private final JdbcTemplate jdbcTemplate;

    private int scanLimit = 100; // Default scan limit

    public InventoryItemsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setScanLimit(int scanLimit) {
        this.scanLimit = scanLimit;
    }

    public int getScanLimit() {
        return scanLimit;
    }

    // Get Record by Primary Key
    public InventoryItems getInventoryItems(InventoryItems items) {
        return getOne("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE item_id = ?", items);
    }

    // Get Record by Primary Key and lock it until the end of the transaction
    public InventoryItems getInventoryItemsForUpdate(InventoryItems items) {
        return getOne("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE item_id = ? FOR UPDATE", items);
    }

    private InventoryItems getOne(String sql, InventoryItems items) {
        List<InventoryItems> result = jdbcTemplate.query(sql, ROW_MAPPER, items.getItemId());
        if (result.isEmpty()) {
            throw new CustomException("No record found in InventoryItems", 9700);
        }
        return result.get(0);
    }

    // Insert Record (a duplicate key fails)
    public InventoryItems insertInventoryItems(InventoryItems items) {
        jdbcTemplate.update("INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (?, ?, ?, ?)",
            items.getItemId(), items.getName(), items.getPrice(), items.getStock());
        return items;
    }

    // Update Record (if exists)
    public InventoryItems updateInventoryItems(InventoryItems items) {
        if (updateRow(items) == 0) {
            throw new CustomException("Update condition not met: no record in InventoryItems", 9100);
        }
        return items;
    }

    // Upsert Record: update, or insert if there is no such row. No ON CONFLICT / MERGE, so the
    // same SQL runs on PostgreSQL, Oracle and through ScalarDB Native JDBC (which refuses them).
    // A concurrent insert of the same key fails with a duplicate key.
    public InventoryItems upsertInventoryItems(InventoryItems items) {
        if (updateRow(items) == 0) {
            insertInventoryItems(items);
        }
        return items;
    }

    private int updateRow(InventoryItems items) {
        return jdbcTemplate.update("UPDATE " + TABLE + " SET name = ?, price = ?, stock = ? WHERE item_id = ?",
            items.getName(), items.getPrice(), items.getStock(), items.getItemId());
    }

    // Delete Record (if exists)
    public void deleteInventoryItems(InventoryItems items) {
        int rows = jdbcTemplate.update("DELETE FROM " + TABLE + " WHERE item_id = ?", items.getItemId());
        if (rows == 0) {
            throw new CustomException("Delete condition not met: no record in InventoryItems", 9100);
        }
    }

    // Scan All Records (up to scanLimit, no order)
    public List<InventoryItems> getInventoryItemsListAll() {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + TABLE + " FETCH FIRST ? ROWS ONLY",
            ROW_MAPPER, scanLimit);
    }

    // Scan Records by Primary Key
    public List<InventoryItems> getInventoryItemsListByPk(InventoryItems items) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE item_id = ? FETCH FIRST ? ROWS ONLY",
            ROW_MAPPER, items.getItemId(), scanLimit);
    }

    public void truncate() {
        // DELETE, not TRUNCATE: ScalarDB Native JDBC refuses TRUNCATE on a ScalarDB table
        jdbcTemplate.update("DELETE FROM " + TABLE);
    }
}
