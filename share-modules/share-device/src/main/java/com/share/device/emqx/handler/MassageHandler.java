package com.share.device.emqx.handler;

import cn.hutool.json.JSONObject;

public interface MassageHandler {

    /**
     * 策略接口
     * @param message
     */
    void handleMessage(JSONObject message);
}