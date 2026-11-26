package com.lucasfenstra.muziekdetective.controller;

import com.lucasfenstra.muziekdetective.scraper.playlistDataScraper;
import com.lucasfenstra.muziekdetective.service.ClaudeService;
import com.lucasfenstra.muziekdetective.service.ClientIpResolver;
import com.lucasfenstra.muziekdetective.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MusicController {

    private static final Logger log = LoggerFactory.getLogger(MusicController.class);

    private final playlistDataScraper scraper;
    private final ClaudeService claudeService;
    private final RateLimiter analyzeRateLimiter;
    private final ClientIpResolver clientIpResolver;

    public MusicController(playlistDataScraper scraper, ClaudeService claudeService,
                           RateLimiter analyzeRateLimiter, ClientIpResolver clientIpResolver) {
        this.scraper = scraper;
        this.claudeService = claudeService;
        this.analyzeRateLimiter = analyzeRateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Muziek Detective - Ontdek Nieuwe Muziek");
        return "index";
    }

    @PostMapping("/analyze")
    public String analyzePlaylist(
            @RequestParam("playlistUrl") String playlistUrl,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String clientIp = clientIpResolver.resolve(request);
        if (!analyzeRateLimiter.tryAcquire(clientIp)) {
            log.info("Rate limit bereikt voor {}", clientIp);
            redirectAttributes.addFlashAttribute("error", String.format(
                    "Je hebt het maximum van %d analyses per %d minuten bereikt. Probeer het straks opnieuw.",
                    analyzeRateLimiter.getMaxRequests(), analyzeRateLimiter.getWindow().toMinutes()));
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/";
        }

        try {
            String jsonResult = scraper.scrapeSpotifyPlaylist(playlistUrl);
            String discovery = claudeService.getDiscoveryRecommendation(jsonResult);

            redirectAttributes.addFlashAttribute("discovery", discovery);
            redirectAttributes.addFlashAttribute("playlistUrl", playlistUrl);
            redirectAttributes.addFlashAttribute("success", true);
        } catch (Exception e) {
            log.error("Fout bij analyseren van {}", playlistUrl, e);
            redirectAttributes.addFlashAttribute("error", "Fout bij analyseren: " + e.getMessage());
            redirectAttributes.addFlashAttribute("success", false);
        }

        return "redirect:/";
    }
}
