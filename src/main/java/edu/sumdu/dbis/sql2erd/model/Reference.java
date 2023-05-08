package edu.sumdu.dbis.sql2erd.model;


import java.util.ArrayList;
import java.util.List;

public class Reference {
    private String name;
    private List<String> columnsNames;
    private String referencedTableName;
    private List<String> referencedColumnNames;

    public Reference() {
        this.columnsNames = new ArrayList<>();
        this.referencedColumnNames = new ArrayList<>();
    }

    public Reference(List<String> columnsNames, String referencedTableName, List<String> referencedColumnNames) {
        this.columnsNames = List.copyOf(columnsNames);
        this.referencedTableName = referencedTableName;
        this.referencedColumnNames = List.copyOf(referencedColumnNames);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getColumnsNames() {
        return columnsNames;
    }

    public void setColumnsNames(List<String> columnsNames) {
        this.columnsNames = columnsNames;
    }

    public void addColumnName(String columnName) {
        this.columnsNames.add(columnName);
    }

    public String getReferencedTableName() {
        return referencedTableName;
    }

    public void setReferencedTableName(String referencedTableName) {
        this.referencedTableName = referencedTableName;
    }

    public List<String> getReferencedColumnNames() {
        return referencedColumnNames;
    }

    public void setReferencedColumnNames(List<String> referencedColumnNames) {
        this.referencedColumnNames = referencedColumnNames;
    }

    public void addReferencedColumnName(String referencedColumnName) {
        this.referencedColumnNames.add(referencedColumnName);
    }
}
