package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReportCsvCompletenessTest {
    @Test public void csvIncludesOutcomeFactsAndKeepsTableRowsAndEscaping(){
        Report report=new Report("Run report",0,"test");
        Report.Facts facts=new Report.Facts(Report.Provenance.RECORDED);
        facts.section("Result").add("Outcome",new Report.Cell("Died",CellText.EXACT),"")
                .add("Ascended",new Report.Cell("No",CellText.EXACT),"")
                .add("Note",new Report.Cell("=formula,quoted",CellText.EXACT),"a,b");
        report.add(facts);
        Report.Table table=new Report.Table("Items",Report.Provenance.RECORDED);
        table.headers.add("Item");table.rows.add(new Report.Cell[]{new Report.Cell("kept",CellText.EXACT)});report.add(table);
        String csv=ReportRenderer.csv(report);
        assertTrue(csv,csv.contains("Result,Outcome,Died,"));
        assertTrue(csv,csv.contains("Result,Ascended,No,"));
        assertTrue(csv,csv.contains("\"'=formula,quoted\""));
        assertTrue(csv,csv.contains("\"a,b\""));
        assertTrue(csv,csv.contains("\r\nkept\r\n"));
    }
    @Test public void largeReportCsvDisclosesEveryOmittedTableRowLikeOtherFormats() {
        DatasetDef d=new DatasetDef("example","Example","").add(ColumnDef.text("label","Label").shown());
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<ReportBuilder.MAX_ROWS+3;i++) {
            Row r=new Row("row-"+i,null,"run",d.size());r.finished=true;r.ruleset="rules";
            r.setText(0,"entry-"+i,Fact.PROV_RECORDED);rows.add(r);
        }
        Dataset dataset=new Dataset(d,rows,Collections.emptyList());
        QueryResult result=QueryEngine.run(dataset,Query.of(d.id),"rules");assertEquals(1003,result.rows.size());
        Report report=ReportBuilder.view(result,"Example","",0,"test");
        assertEquals(1000,report.firstTable().rows.size());assertEquals(3,report.firstTable().omitted);
        Report.Table second=new Report.Table("Second",Report.Provenance.RECORDED);
        second.headers.add("Label");second.rows.add(new Report.Cell[]{new Report.Cell("kept",CellText.EXACT)});second.omitted=2;report.add(second);
        for(String out:new String[]{ReportRenderer.markdown(report),ReportRenderer.text(report),ReportRenderer.html(report),ReportRenderer.csv(report)}) {
            assertTrue(out,out.contains("3 more entries are not listed"));assertTrue(out,out.contains("2 more entries are not listed"));
        }
        assertEquals("Report generation does not truncate the source dataset",1003,dataset.rows.size());
    }
}
