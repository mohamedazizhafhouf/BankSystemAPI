package com.example.mely.stocks.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfDocumentText {
    private String documentName;
    private String extractedText;
    private int pageCount;
    private int characterCount;
}
