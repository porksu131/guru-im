package com.guru.im.offline.controller;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.offline.service.OfflineEventServiceImpl;
import com.guru.im.protocol.model.SyncEventRequest;
import com.guru.im.protocol.model.SyncEventResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/offline")
public class OfflineEventController {

    private static final Logger log = LoggerFactory.getLogger(OfflineEventController.class);
    @Autowired
    private OfflineEventServiceImpl offlineEventService;

    /**
     * 同步离线事件接口
     */
    @PostMapping("/sync")
    public ResponseResult<SyncEventResponse> syncEvents(@RequestBody SyncEventRequest request) {
        try {
            if (StringUtils.isEmpty(request.getDeviceId())) {
                throw new IllegalArgumentException("Device ID is required");
            }
            if (request.getLastSequence() < 0) {
                throw new IllegalArgumentException("Last sequence must be non-negative");
            }
            SyncEventResponse response = offlineEventService.syncEvents(request);
            return ResponseResult.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sync request: {}", e.getMessage());
            return ResponseResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("Sync events error", e);
            return ResponseResult.fail("Internal server error");
        }
    }
}