package com.example.mely.metals.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetalsSpotBundleResponse {
    private MetalSpotQuoteResponse gold;
    private MetalSpotQuoteResponse silver;
    private String currency;
}
