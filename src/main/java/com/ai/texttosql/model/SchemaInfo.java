package com.ai.texttosql.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SchemaInfo {
    private String tableName;
    private List<ColumnInfo> columns;
    private List<ForeignKeyInfo> foreignKeys;
    private List<IndexInfo> indexes;
    private String ddl;

    @Data
    public static class ColumnInfo {
        private String name;
        private String type;
        private boolean nullable;
        private boolean primaryKey;
        private String defaultValue;
        private String comment;
    }

    @Data
    public static class ForeignKeyInfo {
        private String columnName;
        private String referencedTable;
        private String referencedColumn;
        private String constraintName;
    }

    @Data
    public static class IndexInfo {
        private String name;
        private List<String> columns;
        private boolean unique;
    }
}