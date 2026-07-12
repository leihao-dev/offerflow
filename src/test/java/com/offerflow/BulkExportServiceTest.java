package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.BulkExportService;
import com.offerflow.service.JobApplicationService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class BulkExportServiceTest {

    @Autowired
    private BulkExportService bulkExportService;

    @Autowired
    private JobApplicationService applicationService;

    @Test
    void exportAllCreatesZipWithMarkdownEntries() throws Exception {
        applicationService.create(sampleApp("公司A", "岗位A"));
        applicationService.create(sampleApp("公司B", "岗位B"));

        byte[] zip = bulkExportService.exportAllAsZip();

        int entries = 0;
        boolean foundMarkdown = false;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entry.getName().endsWith(".md")) {
                    foundMarkdown = true;
                    String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    assertTrue(content.contains("# "));
                }
            }
        }
        assertTrue(entries >= 2);
        assertTrue(foundMarkdown);
    }

    private ApplicationForm sampleApp(String company, String position) {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName(company);
        form.setPositionTitle(position);
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.now());
        return form;
    }
}
