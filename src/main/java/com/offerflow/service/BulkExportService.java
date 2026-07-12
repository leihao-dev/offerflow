package com.offerflow.service;

import com.offerflow.model.JobApplication;
import com.offerflow.repository.JobApplicationRepository;
import com.offerflow.support.ExportLimits;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BulkExportService {

    private final JobApplicationRepository applicationRepository;
    private final MarkdownExportService markdownExportService;

    public BulkExportService(
            JobApplicationRepository applicationRepository, MarkdownExportService markdownExportService) {
        this.applicationRepository = applicationRepository;
        this.markdownExportService = markdownExportService;
    }

    public long countApplications() {
        return applicationRepository.count();
    }

    public boolean exceedsBulkLimit() {
        return countApplications() > ExportLimits.MAX_BULK_EXPORT;
    }

    public byte[] exportAllAsZip() {
        List<JobApplication> apps = applicationRepository.findAllByOrderByUpdatedAtDesc();
        if (apps.size() > ExportLimits.MAX_BULK_EXPORT) {
            apps = apps.subList(0, ExportLimits.MAX_BULK_EXPORT);
        }
        Set<String> usedNames = new HashSet<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (JobApplication app : apps) {
                String entryName = uniqueEntryName(app, usedNames);
                zos.putNextEntry(new ZipEntry(entryName));
                String markdown = markdownExportService.export(app);
                zos.write(markdown.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build export zip", ex);
        }
        return baos.toByteArray();
    }

    private String uniqueEntryName(JobApplication app, Set<String> usedNames) {
        String base = markdownExportService.buildFilename(app);
        if (usedNames.add(base)) {
            return base;
        }
        String withId = base.replace(".md", "-" + app.getId() + ".md");
        usedNames.add(withId);
        return withId;
    }
}
