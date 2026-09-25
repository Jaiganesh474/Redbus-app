package com.redbus.controller;

import com.redbus.dto.CityPairDto;
import com.redbus.entity.KbDocument;
import com.redbus.repository.KbDocumentRepository;
import com.redbus.service.BusRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/seo", "/api/v1/seo"})
@RequiredArgsConstructor
public class SeoController {

    private final BusRouteService busRouteService;
    private final KbDocumentRepository kbDocumentRepository;

    @GetMapping("/routes")
    public ResponseEntity<List<CityPairDto>> getSeoRoutes() {
        return ResponseEntity.ok(busRouteService.getPopularRoutes());
    }

    @GetMapping("/sitemap-data")
    public ResponseEntity<List<String>> getSitemapPaths() {
        List<CityPairDto> pairs = busRouteService.getPopularRoutes();
        List<String> paths = pairs.stream()
                .map(p -> "/bus-tickets/" + p.getSourceCity().toLowerCase().replace(" ", "-") + "-to-" + p.getDestinationCity().toLowerCase().replace(" ", "-"))
                .collect(Collectors.toList());
        return ResponseEntity.ok(paths);
    }

    @GetMapping("/faqs")
    public ResponseEntity<List<Map<String, String>>> getPublicFaqs() {
        List<KbDocument> docs = kbDocumentRepository.findAll();
        List<Map<String, String>> faqs = docs.stream().map(d -> {
            Map<String, String> map = new HashMap<>();
            map.put("question", d.getTitle());
            map.put("answer", d.getContent());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(faqs);
    }
}
