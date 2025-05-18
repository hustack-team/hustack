package com.hust.baseweb.utils;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public class PdfUtils {

    public static byte[] exportPdf(Collection<?> collection, String reportPath, Map<String, Object> parameters) {
        try {
            InputStream is = new ClassPathResource(reportPath).getInputStream();
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(is);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(collection);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

}
