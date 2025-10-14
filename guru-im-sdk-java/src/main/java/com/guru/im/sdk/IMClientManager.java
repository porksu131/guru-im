package com.guru.im.sdk;

import com.guru.im.core.im.IMClient;
import com.guru.im.core.im.config.ImClientConfig;
import com.guru.im.protocol.model.DeviceInfo;
import com.guru.im.sdk.config.IMConfig;
import com.guru.im.sdk.event.IMEventDispatcher;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.listener.DefaultConnectionListener;
import com.guru.im.sdk.listener.DefaultMessageListener;
import com.guru.im.sdk.model.UserInfo;
import com.guru.im.sdk.service.*;

public class IMClientManager {
    private final IMClient client;
    private final IMEventDispatcher eventDispatcher = new IMEventDispatcher();
    private final ServiceFactory serviceFactory;
    private final DeviceInfo deviceInfo;
    private final UserInfo userInfo;
    private final ImClientConfig imClientConfig;

    public IMClientManager(IMConfig config, UserInfo userInfo, DeviceInfo deviceInfo) {
        this.userInfo = userInfo;
        this.deviceInfo = deviceInfo;
        this.imClientConfig = convertConfig(config);
        this.client = new IMClient(imClientConfig);
        this.serviceFactory = new ServiceFactory(this);
    }

    private ImClientConfig convertConfig(IMConfig config) {
        ImClientConfig clientConfig = IMConfig.convert(config);
        clientConfig.setToken(userInfo.getAccessToken());
        clientConfig.setMessageListener(new DefaultMessageListener(eventDispatcher));
        clientConfig.setConnectionListener(new DefaultConnectionListener(eventDispatcher));
        clientConfig.setDeviceInfo(deviceInfo);
        return clientConfig;
    }

    public void connect() {
        client.connect();
    }


    public void reconnect() {
        client.reconnect();
    }


    public void disconnect() {
        client.disconnect();
    }

    public ChatService chat() {
        return serviceFactory.getChatService();
    }

    public GroupService group() {
        return serviceFactory.getGroupService();
    }

    public UserService user() {
        return serviceFactory.getUserService();
    }

    public SignalService signal() {
        return serviceFactory.getSignalService();
    }

    public void addEventListener(IMEventListener listener) {
        eventDispatcher.addEventListener(listener);
    }

    public void removeEventListener(IMEventListener listener) {
        eventDispatcher.removeEventListener(listener);
    }


    public IMClient getClient() {
        return client;
    }

    public IMEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public ServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public ImClientConfig getImClientConfig() {
        return imClientConfig;
    }
}