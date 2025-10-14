```text
src/main/java/com/guru/im/
├── video/
│   ├── VideoCallManager.java          # 音视频通话管理器
│   ├── VideoCallWindow.java           # 视频通话窗口
│   ├── AudioCallWindow.java           # 语音通话窗口  
│   ├── ConferenceWindow.java          # 会议窗口
│   ├── IncomingCallWindow.java        # 来电窗口
│   └── bridge/
│       ├── JCEFBridgeHandler.java     # JCEF桥接处理器
│       ├── BrowserMessageHandler.java # 浏览器消息处理器接口
│       └── MessageConverter.java      # 消息转换器
├── signaling/
│   ├── manager/
│   │   ├── SignalingManager.java      # 信令管理器
│   │   ├── SessionManager.java        # 会话管理器
│   │   └── ConferenceManager.java     # 会议管理器
│   └── handler/
│       ├── SignalingMessageHandler.java
│       └── 各种信令处理器...
```