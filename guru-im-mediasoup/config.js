module.exports = {
    // HTTP API 端口
    http: {
        port: process.env.MEDIA_PORT || 3000,
        host: process.env.MEDIA_HOST || '0.0.0.0'
    },

    // mediasoup 配置
    mediasoup: {
        numWorkers: parseInt(process.env.MEDIASOUP_NUM_WORKERS) || 1,
        workerSettings: {
            logLevel: process.env.MEDIASOUP_LOG_LEVEL || 'warn',
            logTags: ['info', 'ice', 'dtls', 'rtp', 'srtp', 'rtcp'],
            rtcMinPort: parseInt(process.env.MEDIASOUP_MIN_PORT) || 40000,
            rtcMaxPort: parseInt(process.env.MEDIASOUP_MAX_PORT) || 40100
        },

        routerOptions: {
            mediaCodecs: [
                {
                    kind: 'audio',
                    mimeType: 'audio/opus',
                    clockRate: 48000,
                    channels: 2
                },
                {
                    kind: 'audio',
                    mimeType: 'audio/PCMU',
                    clockRate: 8000,
                    channels: 1
                },
                {
                    kind: 'audio',
                    mimeType: 'audio/PCMA',
                    clockRate: 8000,
                    channels: 1
                },
                {
                    kind: 'video',
                    mimeType: 'video/VP8',
                    clockRate: 90000,
                    parameters: {
                        'x-google-start-bitrate': 1000
                    }
                },
                {
                    kind: 'video',
                    mimeType: 'video/H264',
                    clockRate: 90000,
                    parameters: {
                        'packetization-mode': 1,
                        'profile-level-id': '42e01f',
                        'level-asymmetry-allowed': 1
                    }
                }
            ]
        },

        webRtcTransportOptions: {
            listenIps: [
                {
                    ip: '0.0.0.0',
                    announcedIp: process.env.ANNOUNCED_IP || '127.0.0.1'
                }
            ],
            enableUdp: true,
            enableTcp: true,
            preferUdp: true,
            initialAvailableOutgoingBitrate: 1000000,
            minimumAvailableOutgoingBitrate: 600000,
            maxIncomingBitrate: 1500000,
            // 配置 ICE 服务器（包括 Coturn）
            iceServers: [
                {
                    urls: [
                        'stun:stun.l.google.com:19302',
                        'stun:stun1.l.google.com:19302'
                    ]
                },
                {
                    urls: [
                        `turn:${process.env.TURN_SERVER || '192.168.2.130'}:${process.env.TURN_PORT || '3478'}`,
                        `turn:${process.env.TURN_SERVER || '192.168.2.130'}:${process.env.TURN_PORT || '3478'}?transport=tcp`,
                        `turns:${process.env.TURN_SERVER || '192.168.2.130'}:${process.env.TURN_PORT || '5349'}`
                    ],
                    username: process.env.TURN_USERNAME || 'coturn',
                    credential: process.env.TURN_PASSWORD || 'coturn'
                }
            ]
        }
    }
};