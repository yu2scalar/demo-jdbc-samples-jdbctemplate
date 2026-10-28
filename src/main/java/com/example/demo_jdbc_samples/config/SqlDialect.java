package com.example.demo_jdbc_samples.config;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The SQL that differs between PostgreSQL and Oracle. Everything else (TRUNCATE TABLE,
 * FETCH FIRST n ROWS ONLY, SELECT ... FOR UPDATE, LIKE) is written once.
 *
 * Upsert:
 * - PostgreSQL: INSERT ... ON CONFLICT DO UPDATE. MERGE is not used because a concurrent
 *   insert of the same key can make it fail with a unique violation.
 * - Oracle: MERGE INTO ... USING (SELECT ... FROM dual).
 * The bind parameters are the columns in the given order in both cases.
 */
public enum SqlDialect {
    POSTGRESQL,
    ORACLE;

    public static SqlDialect fromProductName(String productName) {
        String name = productName.toLowerCase();
        if (name.contains("postgresql")) {
            return POSTGRESQL;
        }
        if (name.contains("oracle")) {
            return ORACLE;
        }
        throw new IllegalStateException("Unsupported database: " + productName);
    }

    /** Upsert of one row: keyColumns first, then valueColumns, as bind parameters. */
    public String upsert(String table, List<String> keyColumns, List<String> valueColumns) {
        List<String> all = new java.util.ArrayList<>(keyColumns);
        all.addAll(valueColumns);
        String columns = String.join(", ", all);
        return switch (this) {
            case POSTGRESQL -> "INSERT INTO " + table + " (" + columns + ") VALUES ("
                + all.stream().map(c -> "?").collect(Collectors.joining(", ")) + ")"
                + " ON CONFLICT (" + String.join(", ", keyColumns) + ") DO UPDATE SET "
                + valueColumns.stream().map(c -> c + " = EXCLUDED." + c).collect(Collectors.joining(", "));
            case ORACLE -> "MERGE INTO " + table + " t USING (SELECT "
                + all.stream().map(c -> "? AS " + c).collect(Collectors.joining(", ")) + " FROM dual) s ON ("
                + keyColumns.stream().map(c -> "t." + c + " = s." + c).collect(Collectors.joining(" AND "))
                + ") WHEN MATCHED THEN UPDATE SET "
                + valueColumns.stream().map(c -> "t." + c + " = s." + c).collect(Collectors.joining(", "))
                + " WHEN NOT MATCHED THEN INSERT (" + columns + ") VALUES ("
                + all.stream().map(c -> "s." + c).collect(Collectors.joining(", ")) + ")";
        };
    }
}
