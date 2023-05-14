package edu.sumdu.dbis.sql2erd.controller;

import edu.sumdu.dbis.sql2erd.model.Column;
import edu.sumdu.dbis.sql2erd.model.Reference;
import edu.sumdu.dbis.sql2erd.model.Table;

import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LiveController {
    public static List<Table> input(String url, String catalogName, String schemaName) throws SQLException {
        List<Table> tableList = new ArrayList<>();
        Connection connection = DriverManager.getConnection(url);
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(catalogName, schemaName, null, new String[]{"TABLE"});
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            Table table = new Table(tableName);
            getColumns(catalogName, schemaName, metaData, tableName, table);
            Map<String, Column> columnMap = table.getColumns().stream().collect(Collectors.toMap(Column::getName, Function.identity()));
            getUniqueConstraints(catalogName, schemaName, metaData, tableName, columnMap);
            getPrimaryKeys(catalogName, schemaName, metaData, tableName, columnMap);
            getReferences(catalogName, schemaName, metaData, tableName, table, columnMap);
            tableList.add(table);
        }
        return tableList;
    }

    private static void getColumns(String catalogName, String schemaName, DatabaseMetaData metaData, String tableName, Table table) throws SQLException {
        ResultSet columns = metaData.getColumns(catalogName, schemaName, tableName, null);
        while (columns.next()) {
            Column column;
            int colSize = columns.getInt("COLUMN_SIZE");
            if (colSize == Integer.MAX_VALUE)
                column = new Column(columns.getString("COLUMN_NAME"), columns.getString("TYPE_NAME"));
            else column = new Column(columns.getString("COLUMN_NAME"),
                    columns.getString("TYPE_NAME") + " (" + colSize + ")");
            if (columns.getString("IS_NULLABLE").equals("NO")) {
                column.setNotNull(true);
            }
            table.addColumn(column);
        }
    }

    private static void getUniqueConstraints(String catalogName, String schemaName, DatabaseMetaData metaData, String tableName, Map<String, Column> columnMap) throws SQLException {
        ResultSet indices = metaData.getIndexInfo(catalogName, schemaName, tableName, true, false);
        String prevColumnName = null;
        String currIndexName;
        String currColumnName;
        Set<String> indexHashSet = new HashSet<>();
        while (indices.next()) {
            currIndexName = indices.getString("INDEX_NAME");
            currColumnName = indices.getString("COLUMN_NAME");
            if (currIndexName != null) {
                if (!indexHashSet.contains(currIndexName)) {
                    indexHashSet.add(currIndexName);
                    columnMap.get(currColumnName).setUnique(true);
                    prevColumnName = currColumnName;
                } else columnMap.get(prevColumnName).setUnique(false);
            } else columnMap.get(currColumnName).setUnique(true);
        }
    }

    private static void getPrimaryKeys(String catalogName, String schemaName, DatabaseMetaData metaData, String tableName, Map<String, Column> columnMap) throws SQLException {
        ResultSet primaryKeys = metaData.getPrimaryKeys(catalogName, schemaName, tableName);
        while (primaryKeys.next()) {
            columnMap.get(primaryKeys.getString("COLUMN_NAME")).setPrimaryKey(true);
        }
    }

    private static void getReferences(String catalogName, String schemaName, DatabaseMetaData metaData, String tableName, Table table, Map<String, Column> columnMap) throws SQLException {
        ResultSet foreignKeys = metaData.getImportedKeys(catalogName, schemaName, tableName);
        Reference reference = null;
        String currReferencedTableName;
        String prevReferencedTableName = null;
        while (foreignKeys.next()) {
            currReferencedTableName = foreignKeys.getString("PKTABLE_NAME");
            if (currReferencedTableName.equals(prevReferencedTableName)) {
                if (foreignKeys.getShort("KEY_SEQ") == 1) {
                    table.addReference(reference);
                    reference = new Reference();
                    reference.setName(foreignKeys.getString("FK_NAME"));
                    reference.setReferencedTableName(currReferencedTableName);
                }
            } else {
                if (reference != null) {
                    table.addReference(reference);
                }
                reference = new Reference();
                reference.setName(foreignKeys.getString("FK_NAME"));
                reference.setReferencedTableName(currReferencedTableName);
            }
            columnMap.get(foreignKeys.getString("FKCOLUMN_NAME")).setForeignKey(true);
            reference.addColumnName(foreignKeys.getString("FKCOLUMN_NAME"));
            reference.addReferencedColumnName(foreignKeys.getString("PKCOLUMN_NAME"));
            prevReferencedTableName = currReferencedTableName;
        }
        if (reference != null) {
            table.addReference(reference);
        }
    }
}
