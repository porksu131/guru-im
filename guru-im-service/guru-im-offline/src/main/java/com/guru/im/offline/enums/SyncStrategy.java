package com.guru.im.offline.enums;

public enum SyncStrategy {
    FULL(0, "全量同步", 500, 100), // 阈值500条，每批100条
    INCREMENTAL(1, "增量同步", 1000, 200),
    BY_CONVERSATION(2, "按会话同步", 2000, 300),
    BY_PRIORITY(3, "按优先级同步", null, 150); // 无数量限制，按优先级过滤

    private final Integer code;
    private final String desc;
    private final Integer threshold; // 触发历史拉取的阈值
    private final Integer defaultBatchSize; // 默认批次大小

    SyncStrategy(Integer code, String desc, Integer threshold, Integer defaultBatchSize) {
        this.code = code;
        this.desc = desc;
        this.threshold = threshold;
        this.defaultBatchSize = defaultBatchSize;
    }

    public static SyncStrategy fromCode(Integer code) {
        for (SyncStrategy strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        return INCREMENTAL; // 默认增量同步
    }


    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public Integer getDefaultBatchSize() {
        return defaultBatchSize;
    }
}