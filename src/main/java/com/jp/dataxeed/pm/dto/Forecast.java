package com.jp.dataxeed.pm.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ← これを追加！
public class Forecast {
    private String dateLabel;
    private String date;
    private String telop;
    private Temperature temperature;
    private Image image;

    @Data
    public static class Temperature {
        private TempDetail min;
        private TempDetail max;
    }

    @Data
    public static class TempDetail {
        private String celsius;
        private String fahrenheit;
    }

    @Data
    public static class Image {
        private String title;
        private String url;
        private int width;
        private int height;
    }
}
