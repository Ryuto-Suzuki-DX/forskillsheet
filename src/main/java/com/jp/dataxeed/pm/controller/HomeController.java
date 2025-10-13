package com.jp.dataxeed.pm.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jp.dataxeed.pm.dto.Forecast;
import com.jp.dataxeed.pm.service.WeatherService;

@RequestMapping("/home")
@Controller
public class HomeController {

    private final WeatherService weatherService;

    public HomeController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("")
    public String home(Model model) {
        System.out.println("HomeController.home() 呼び出し");
        List<Forecast> forecasts = weatherService.getWeather();
        System.out.println("WeatherService 呼び出し完了");
        model.addAttribute("forecasts", forecasts);
        return "home/home";
    }

}
