package com.jp.dataxeed.pm.dto;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ★これを追加！
public class WeatherResponse {
    private String title;
    private List<Forecast> forecasts;
}
