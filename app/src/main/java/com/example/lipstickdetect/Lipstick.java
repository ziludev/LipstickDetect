package com.example.lipstickdetect;

import java.util.ArrayList;

public class Lipstick {
    // 品牌
    public ArrayList<BrandsInfo> brands;
    // 品牌信息类
    public static class BrandsInfo {
        // 品牌名字
        public String name;
        // 品牌系列
        public ArrayList<SeriesInfo> series;
        // 系列信息
        public static class SeriesInfo {
            // 系列名称
            public String name;
            // 种类
            public ArrayList<LipstickInfo> lipsticks;
            // 种类信息
            public static class LipstickInfo {
                // 颜色
                public String color;
                // 色号
                public String id;
                // 名字
                public String name;
            }
        }
    }
}
