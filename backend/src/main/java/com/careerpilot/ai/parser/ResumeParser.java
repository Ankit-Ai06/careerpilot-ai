package com.careerpilot.ai.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class ResumeParser {

    public String extractText(MultipartFile file) throws IOException {

        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return sanitize(extractPdfText(file));
        }

        if (lowerName.endsWith(".docx")) {
            return sanitize(extractDocxText(file));
        }

        throw new IllegalArgumentException(
                "Only PDF and DOCX files are supported"
        );
    }

    private String extractPdfText(MultipartFile file)
            throws IOException {

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();

            return stripper.getText(document).trim();
        }
    }

    private String extractDocxText(MultipartFile file)
            throws IOException {

        try (XWPFDocument document =
                     new XWPFDocument(file.getInputStream())) {

            return document.getParagraphs()
                    .stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"))
                    .trim();
        }
    }

    private String sanitize(String text) {
        return text
                .replace("\u0000", "")
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .trim();
    }
}
