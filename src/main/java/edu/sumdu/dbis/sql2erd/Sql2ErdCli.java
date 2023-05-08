package edu.sumdu.dbis.sql2erd;

import edu.sumdu.dbis.sql2erd.controller.FileController;
import edu.sumdu.dbis.sql2erd.controller.LiveController;
import edu.sumdu.dbis.sql2erd.model.Table;
import edu.sumdu.dbis.sql2erd.view.RecordView;
import guru.nidi.graphviz.engine.Format;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sql2erd.jar", mixinStandardHelpOptions = true, version = "1.0")
public class Sql2ErdCli implements Callable<Integer> {
    @CommandLine.ArgGroup(multiplicity = "1")
    private InputOption inputOption;
    @CommandLine.Option(names = {"-o", "--output"}, required = true, description = "Output file name")
    private File outputFile;

    @CommandLine.Option(names = {"-f", "--format"}, required = true, description = "Output format: ${COMPLETION-CANDIDATES}")
    private Format outputFormat;

    @CommandLine.Option(names = {"-n", "--notation"}, defaultValue = "CROWS_FOOT", description = "ERD notation: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    private RecordView.Notation erdNotation;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Sql2ErdCli()).setUsageHelpAutoWidth(true).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        List<Table> tables;
        if (inputOption.inputFile != null) {
            tables = FileController.input(inputOption.inputFile);
        } else {
            tables = LiveController.input(inputOption.urlOption.databaseUrl,
                    inputOption.urlOption.databaseCatalog,
                    inputOption.urlOption.databaseSchema);
        }
        RecordView.render(tables, outputFile, outputFormat, erdNotation);
        return 0;
    }

    static class InputOption {
        @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
        private UrlOption urlOption;
        @CommandLine.Option(names = {"-i", "--input"}, paramLabel = "<inputFile.sql>", description = "Input SQL file")
        private File inputFile;

        static class UrlOption {
            @CommandLine.Option(names = {"-u", "--url"}, required = true, paramLabel = "<url>", description = "Database URL")
            private String databaseUrl;
            @CommandLine.Option(names = {"-c", "--catalog"}, paramLabel = "<catalogName>", description = "Catalog(database) name")
            private String databaseCatalog;
            @CommandLine.Option(names = {"-s", "--schema"}, paramLabel = "<schemaName>", description = "Schema(namespace) name")
            private String databaseSchema;
        }
    }
}