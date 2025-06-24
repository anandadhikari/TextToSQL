package com.ai.texttosql.service;

import com.ai.texttosql.model.SchemaInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchemaAnalysisService {

    @PersistenceContext
    private EntityManager entityManager;

    @Cacheable(value = "schemaCache", key = "'allTables'")
    public List<String> getAllTableNames() {
        return entityManager.createNativeQuery(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()")
                .getResultList();
    }

    @Cacheable(value = "schemaCache", key = "#tableName")
    public SchemaInfo getTableSchema(String tableName) {
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setTableName(tableName);

        List<Object[]> columns = entityManager.createNativeQuery(
                        "SELECT column_name, data_type, is_nullable, column_key, column_default, column_comment " +
                                "FROM information_schema.columns " +
                                "WHERE table_schema = DATABASE() AND table_name = :tableName")
                .setParameter("tableName", tableName)
                .getResultList();

        schemaInfo.setColumns(columns.stream()
                .map(col -> {
                    SchemaInfo.ColumnInfo columnInfo = new SchemaInfo.ColumnInfo();
                    columnInfo.setName((String) col[0]);
                    columnInfo.setType((String) col[1]);
                    columnInfo.setNullable("YES".equals(col[2]));
                    columnInfo.setPrimaryKey("PRI".equals(col[3]));
                    columnInfo.setDefaultValue(col[4] != null ? col[4].toString() : null);
                    columnInfo.setComment((String) col[5]);
                    return columnInfo;
                })
                .collect(Collectors.toList()));

        List<Object[]> foreignKeys = entityManager.createNativeQuery(
                        "SELECT column_name, referenced_table_name, referenced_column_name, constraint_name " +
                                "FROM information_schema.key_column_usage " +
                                "WHERE table_schema = DATABASE() AND table_name = :tableName " +
                                "AND referenced_table_name IS NOT NULL")
                .setParameter("tableName", tableName)
                .getResultList();

        schemaInfo.setForeignKeys(foreignKeys.stream()
                .map(fk -> {
                    SchemaInfo.ForeignKeyInfo fkInfo = new SchemaInfo.ForeignKeyInfo();
                    fkInfo.setColumnName((String) fk[0]);
                    fkInfo.setReferencedTable((String) fk[1]);
                    fkInfo.setReferencedColumn((String) fk[2]);
                    fkInfo.setConstraintName((String) fk[3]);
                    return fkInfo;
                })
                .collect(Collectors.toList()));

        List<Object[]> indexes = entityManager.createNativeQuery(
                        "SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index), non_unique " +
                                "FROM information_schema.statistics " +
                                "WHERE table_schema = DATABASE() AND table_name = :tableName " +
                                "GROUP BY index_name, non_unique")
                .setParameter("tableName", tableName)
                .getResultList();

        schemaInfo.setIndexes(indexes.stream()
                .map(idx -> {
                    SchemaInfo.IndexInfo idxInfo = new SchemaInfo.IndexInfo();
                    idxInfo.setName((String) idx[0]);
                    idxInfo.setColumns(List.of(((String) idx[1]).split(",")));
                    idxInfo.setUnique((Integer) idx[2] == 0);
                    return idxInfo;
                })
                .collect(Collectors.toList()));

        Object[] ddlRow = (Object[]) entityManager.createNativeQuery(
                        "SHOW CREATE TABLE `" + tableName + "`")
                .getSingleResult();
        schemaInfo.setDdl((String) ddlRow[1]);

        return schemaInfo;
    }

    @Cacheable(value = "schemaCache", key = "'schemaContext'")
    public String generateSchemaContext() {
        List<String> tables = getAllTableNames();
        StringBuilder context = new StringBuilder("Database Schema:\n\n");

        for (String table : tables) {
            SchemaInfo schema = getTableSchema(table);
            context.append("Table: ").append(table).append("\n");

            for (SchemaInfo.ColumnInfo column : schema.getColumns()) {
                context.append("- ").append(column.getName()).append(" ").append(column.getType());
                if (column.isPrimaryKey()) context.append(" PRIMARY KEY");
                if (!column.isNullable()) context.append(" NOT NULL");
                if (column.getDefaultValue() != null) {
                    context.append(" DEFAULT ").append(column.getDefaultValue());
                }
                context.append("\n");
            }

            if (!schema.getForeignKeys().isEmpty()) {
                for (SchemaInfo.ForeignKeyInfo fk : schema.getForeignKeys()) {
                    context.append("FK ").append(fk.getColumnName())
                            .append(" → ").append(fk.getReferencedTable())
                            .append("(").append(fk.getReferencedColumn()).append(")\n");
                }
            }

            context.append("\n");
        }

        return context.toString();
    }
}
