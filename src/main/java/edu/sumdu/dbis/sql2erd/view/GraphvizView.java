package edu.sumdu.dbis.sql2erd.view;

import edu.sumdu.dbis.sql2erd.model.Column;
import edu.sumdu.dbis.sql2erd.model.Reference;
import edu.sumdu.dbis.sql2erd.model.Table;
import guru.nidi.graphviz.attribute.ForLink;
import guru.nidi.graphviz.attribute.MapAttributes;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.attribute.Records.rec;
import static guru.nidi.graphviz.model.Factory.*;

public class GraphvizView {
    public static void render(List<Table> tables, File outputFile, Format outputFormat, Notation erdNotation) throws IOException {
        MutableGraph mg = mutGraph().setDirected(true)
                .graphAttrs().add(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
                .nodeAttrs().add("shape", "record")
                .linkAttrs().add("dir", "both")
                .linkAttrs().add("minlen", 2);
        Map<String, MutableNode> mutableNodeMap = new HashMap<>();
        addNodes(tables, erdNotation, mutableNodeMap);
        addEdges(tables, erdNotation, mg, mutableNodeMap);
        Graphviz.useEngine(new GraphvizV8Engine());
        Graphviz.fromGraph(mg).render(outputFormat).toFile(outputFile);
    }

    private static void addNodes(List<Table> tables, Notation erdNotation, Map<String, MutableNode> mutableNodeMap) {
        for (Table table : tables) {
            MutableNode mn = mutNode(table.getName());
            mutableNodeMap.put(table.getName(), mn);
            List<String> recs = new ArrayList<>();
            recs.add(table.getName());
            for (Column column : table.getColumns()) {
                StringBuilder sb = new StringBuilder();
                if (Objects.equals(erdNotation, Notation.BARKERS)) barkersLabel(column, sb);
                else crowsFootLabel(column, sb);
                recs.add(rec(column.getName(), sb.toString()));
            }
            mn.add(Records.of(recs.toArray(new String[0])));
        }
    }

    private static void addEdges(List<Table> tables, Notation erdNotation, MutableGraph mg, Map<String, MutableNode> mutableNodeMap) {
        for (Table table : tables) {
            String tableName = table.getName();
            for (Reference reference : table.getReferences()) {
                String referencedTableName = reference.getReferencedTableName();
                MutableNode referencedNode = mutableNodeMap.get(referencedTableName);
                String fkColumnName = reference.getColumnsNames().get(0);
                String pkColumnName = reference.getReferencedColumnNames().get(0);
                Link link = between(port(fkColumnName), referencedNode.port(pkColumnName));
                MapAttributes<ForLink> linkAttributes = new MapAttributes<>();
                if (reference.getName() != null) linkAttributes.add("label", reference.getName());
                boolean cardinality = reference.getColumnsNames().stream()
                        .anyMatch(cn -> table.getColumns().stream().collect(Collectors.toMap(Column::getName, Function.identity())).get(cn).isUnique());
                boolean modality = reference.getColumnsNames().stream()
                        .allMatch(cn -> table.getColumns().stream().collect(Collectors.toMap(Column::getName, Function.identity())).get(cn).isNotNull());
                boolean identifying = reference.getColumnsNames().stream()
                        .anyMatch(cn -> table.getColumns().stream().collect(Collectors.toMap(Column::getName, Function.identity())).get(cn).isPrimaryKey());
                if (Objects.equals(erdNotation, Notation.BARKERS))
                    barkersAttrs(linkAttributes, cardinality, modality, identifying);
                else crowsFootAttrs(linkAttributes, cardinality, modality, identifying);
                link.add(linkAttributes);
                mutableNodeMap.get(tableName).addLink(link);
            }
            mutableNodeMap.get(tableName).addTo(mg);
        }
    }


    private static void crowsFootLabel(Column column, StringBuilder sb) {
        if (column.isPrimaryKey() && column.isForeignKey()) sb.append("PFK");
        else if (column.isPrimaryKey()) sb.append("PK");
        else if (column.isForeignKey()) sb.append("FK");
        sb.append(" ").append(column.getName());
        sb.append(" ").append(column.getType());
        if (column.isUnique()) sb.append(" ").append("U");
        if (column.isNotNull()) sb.append(" ").append("NN");
    }

    private static void crowsFootAttrs(MapAttributes<ForLink> linkAttributes, boolean cardinality, boolean modality, boolean fkPrimary) {
        linkAttributes.add("arrowtail", cardinality ? "teeodot" : "crowodot");
        linkAttributes.add("arrowhead", modality ? "teetee" : "teeodot");
        linkAttributes.add("style", fkPrimary ? "solid" : "dashed");
    }


    private static void barkersLabel(Column column, StringBuilder sb) {
        if (column.isUnique() || column.isPrimaryKey()) sb.append("#");
        if (column.isNotNull() || column.isPrimaryKey()) sb.append("*");
        else sb.append("O");
        sb.append(" ").append(column.getName());
        sb.append(" ").append(column.getType());
    }

    private static void barkersAttrs(MapAttributes<ForLink> linkAttributes, boolean cardinality, boolean modality, boolean fkPrimary) {
        linkAttributes.add("arrowhead", "none");
        String arrowtail = cardinality ? "none" : "crow";
        if (fkPrimary) arrowtail += "tee";
        linkAttributes.add("arrowtail", arrowtail);
        linkAttributes.add("style", modality ? "solid" : "dashed");
    }

    public enum Notation {
        CROWS_FOOT,
        BARKERS
    }
}
