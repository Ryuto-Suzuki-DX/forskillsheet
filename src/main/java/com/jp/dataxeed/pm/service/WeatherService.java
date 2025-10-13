package com.jp.dataxeed.pm.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jp.dataxeed.pm.dto.Forecast;
import com.jp.dataxeed.pm.dto.WeatherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Forecast> getWeather() {

        // 今回は大阪に限定
        String url = "https://weather.tsukumijima.net/api/forecast/city/270000";

        // APIからJSONを取得
        String response = restTemplate.getForObject(url, String.class);

        System.out.println("APIレスポンス内容:");
        System.out.println(response);

        // JSON → Javaオブジェクトに変換
        ObjectMapper mapper = new ObjectMapper();
        WeatherResponse weather = null;
        try {
            weather = mapper.readValue(response, WeatherResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 今日(0)と明日(1)だけ取り出す
        if (weather != null && weather.getForecasts() != null) {
            return weather.getForecasts().subList(0, Math.min(2, weather.getForecasts().size()));
        } else {
            return Collections.emptyList();
        }
    }
}
