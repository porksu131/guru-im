package com.guru.im.common.constant;

public enum DeliveryStatus {
    WAIT_DELIVERY(0, "未投递"),
    DELIVERED_WAIT_ARRIVE(1, "已投递，待送达"),
    DELIVERY_FAILED(2, "投递失败"),
    ARRIVED(3, "已送达"),
    ARRIVE_FAILED(3, "送达失败");

    private final int code;
    private final String desc;

    DeliveryStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}