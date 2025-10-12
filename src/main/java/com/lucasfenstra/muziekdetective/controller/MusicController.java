package com.lucasfenstra.muziekdetective.controller;

import com.lucasfenstra.muziekdetective.scraper.playlistDataScraper;
import com.lucasfenstra.muziekdetective.service.SimpleOllamaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class MusicController {

    private final Map<String, String> progressUpdates = new ConcurrentHashMap<>();

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Muziek Detective - Ontdek Nieuwe Muziek");
        return "index";
    }

    @PostMapping("/analyze")
    public String analyzePlaylist(
            @RequestParam("playlistUrl") String playlistUrl,
            RedirectAttributes redirectAttributes) {

        String sessionId = java.util.UUID.randomUUID().toString();
        progressUpdates.put(sessionId, "Starten...");

        try {
            progressUpdates.put(sessionId, "Initialiseren scraper...");
            playlistDataScraper scraper = new playlistDataScraper();
            SimpleOllamaService ollamaService = new SimpleOllamaService();

            progressUpdates.put(sessionId, "Scraping Spotify playlist...");
            String jsonResult = scraper.scrapeSpotifyPlaylist(playlistUrl);

            progressUpdates.put(sessionId, "Analyseren playlist met AI...");
            String discovery = ollamaService.getDiscoveryRecommendation(jsonResult);

            progressUpdates.put(sessionId, "Voltooid!");
            redirectAttributes.addFlashAttribute("discovery", discovery);
            redirectAttributes.addFlashAttribute("playlistUrl", playlistUrl);
            redirectAttributes.addFlashAttribute("success", true);

            // Cleanup
            progressUpdates.remove(sessionId);

        } catch (Exception e) {
            progressUpdates.put(sessionId, "Fout opgetreden");
            redirectAttributes.addFlashAttribute("error", "Fout bij analyseren: " + e.getMessage());
            redirectAttributes.addFlashAttribute("success", false);
            progressUpdates.remove(sessionId);
        }

        return "redirect:/";
    }

    @GetMapping("/progress")
    @ResponseBody
    public Map<String, String> getProgress(@RequestParam String sessionId) {
        Map<String, String> response = new HashMap<>();
        String progress = progressUpdates.get(sessionId);
        if (progress != null) {
            response.put("progress", progress);
            response.put("status", "running");
        } else {
            response.put("status", "completed");
        }
        return response;
    }
}