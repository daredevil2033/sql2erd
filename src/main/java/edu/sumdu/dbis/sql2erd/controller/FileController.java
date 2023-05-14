package edu.sumdu.dbis.sql2erd.controller;

import edu.sumdu.dbis.sql2erd.model.Column;
import edu.sumdu.dbis.sql2erd.model.Reference;
import edu.sumdu.dbis.sql2erd.model.Table;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileController {
    public static List<Table> input(File input) throws IOException, JSQLParserException {
        List<Table> tables = new ArrayList<>();
        List<Statement> statements = CCJSqlParserUtil.parseStatements(Files.readString(input.toPath())).getStatements();
        for (Statement statement : statements) {
            if (statement instanceof CreateTable) {
                CreateTable createTable = (CreateTable) statement;
                Table table = new Table(createTable.getTable().getName());
                getColumns(createTable, table);
                getIndices(createTable, table);
                tables.add(table);
            }
        }
        return tables;
    }

    private static void getColumns(CreateTable createTable, Table table) {
        List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            Column column = new Column(columnDefinition.getColumnName(), columnDefinition.getColDataType().toString());
            List<String> columnSpecs = columnDefinition.getColumnSpecs();
            if (columnSpecs != null) {
                for (String columnSpec : columnSpecs) {
                    if (columnSpec.equalsIgnoreCase("PRIMARY") &&
                            columnSpecs.get(columnSpecs.indexOf(columnSpec) + 1).equalsIgnoreCase("KEY")) {
                        column.setPrimaryKey(true);
                        column.setNotNull(true);
                        column.setUnique(true);
                    } else if (columnSpec.equalsIgnoreCase("NOT") &&
                            columnSpecs.get(columnSpecs.indexOf(columnSpec) + 1).equalsIgnoreCase("NULL")) {
                        column.setNotNull(true);
                    } else if (columnSpec.equalsIgnoreCase("UNIQUE")) {
                        column.setUnique(true);
                    } else if (columnSpec.equalsIgnoreCase("REFERENCES")) {
                        column.setForeignKey(true);
                        String referencedColumnName = columnSpecs.get(columnSpecs.indexOf(columnSpec) + 2);
                        Reference reference = new Reference(Collections.singletonList(column.getName()),
                                columnSpecs.get(columnSpecs.indexOf(columnSpec) + 1),
                                Collections.singletonList(referencedColumnName.substring(1, referencedColumnName.length() - 1)));
                        if (columnSpecs.indexOf(columnSpec) - 2 >= 0) {
                            if (columnSpecs.get(columnSpecs.indexOf(columnSpec) - 2).equalsIgnoreCase("CONSTRAINT")) {
                                reference.setName(columnSpecs.get(columnSpecs.indexOf(columnSpec) - 1));
                            }
                        }
                        table.addReference(reference);
                    }
                }
            }
            table.addColumn(column);
        }
    }

    private static void getIndices(CreateTable createTable, Table table) {
        List<Index> indices = createTable.getIndexes();
        if (indices != null) {
            for (Index index : indices) {
                String indexName = index.getName();
                Map<String, Column> columnMap = table.getColumns().stream().collect(Collectors.toMap(Column::getName, Function.identity()));
                if (index instanceof ForeignKeyIndex) {
                    ForeignKeyIndex fkIndex = (ForeignKeyIndex) index;
                    for (String columnName : fkIndex.getColumnsNames()) {
                        columnMap.get(columnName).setForeignKey(true);
                    }
                    Reference reference = new Reference(fkIndex.getColumnsNames(), fkIndex.getTable().getName(), fkIndex.getReferencedColumnNames());
                    reference.setName(indexName);
                    table.addReference(reference);
                } else {
                    List<String> columnNames = index.getColumnsNames();
                    for (String columnName : columnNames) {
                        Column column = columnMap.get(columnName);
                        if (columnNames.size() == 1) {
                            if (index.getType().equalsIgnoreCase("UNIQUE")) {
                                column.setUnique(true);
                            }
                        }
                        if (index.getType().equalsIgnoreCase("PRIMARY KEY")) {
                            column.setPrimaryKey(true);
                            column.setNotNull(true);
                            if (columnNames.size() == 1) {
                                column.setUnique(true);
                            }
                        }
                    }
                }
            }
        }
    }
}
