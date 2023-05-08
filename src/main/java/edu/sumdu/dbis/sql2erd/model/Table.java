package edu.sumdu.dbis.sql2erd.model;

import java.util.ArrayList;
import java.util.List;

public class Table {
    private final String name;
    private final List<Column> columns;
    private final List<Reference> references;

    public Table(String name) {
        this.name = name;
        this.columns = new ArrayList<>();
        this.references = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void addColumn(Column column) {
        this.columns.add(column);
    }

    public List<Reference> getReferences() {
        return references;
    }

    public void addReference(Reference reference) {
        this.references.add(reference);
    }
}
