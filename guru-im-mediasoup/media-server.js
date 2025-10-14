const express = require('express');
const cors = require('cors');
const config = require('./config');
const MediaRoom = require('./media-room');
const ResponseResult = require('./response-result');

class MediaServer {
    constructor() {
        this.app = express();
        this.rooms = new Map();
        this.setupMiddleware();
        this.setupRoutes();
    }

    setupMiddleware() {
        this.app.use(cors());
        this.app.use(express.json({ limit: '10mb' }));

        // 日志中间件
        this.app.use((req, res, next) => {
            console.log(`${new Date().toISOString()} ${req.method} ${req.path}`, req.body);
            next();
        });
    }

    setupRoutes() {
        // 健康检查
        this.app.get('/health', (req, res) => {
            const result = ResponseResult.ok({
                status: 'ok',
                timestamp: new Date().toISOString(),
                rooms: this.rooms.size
            });
            res.json(result);
        });

        // 服务器信息
        this.app.get('/info', (req, res) => {
            const roomsInfo = Array.from(this.rooms.entries()).map(([id, room]) => ({
                id,
                peerCount: room.peers.size
            }));

            const result = ResponseResult.ok({
                status: 'ok',
                rooms: roomsInfo,
                totalRooms: this.rooms.size,
                totalPeers: Array.from(this.rooms.values()).reduce((sum, room) => sum + room.peers.size, 0)
            });
            res.json(result);
        });

        // 创建或加入房间
        this.app.post('/room/join', async (req, res) => {
            try {
                const { roomId, peerId } = req.body;

                if (!roomId || !peerId) {
                    const result = ResponseResult.failCode(400, 'roomId and peerId are required');
                    return res.status(400).json(result);
                }

                // 获取或创建房间
                if (!this.rooms.has(roomId)) {
                    const room = new MediaRoom(roomId);
                    await room.init();
                    this.rooms.set(roomId, room);
                    console.log(`创建媒体房间: ${roomId}`);
                }

                const room = this.rooms.get(roomId);
                room.addPeer(peerId);

                const rtpCapabilities = room.getRtpCapabilities();

                const result = ResponseResult.ok({
                    rtpCapabilities,
                    roomId,
                    peerCount: room.peers.size
                });
                res.json(result);

                console.log(`用户 ${peerId} 加入媒体房间: ${roomId}`);

            } catch (error) {
                console.error('加入房间错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 获取房间RTP能力
        this.app.post('/room/rtp-capabilities', async (req, res) => {
            try {
                const { roomId } = req.body;

                if (!roomId) {
                    const result = ResponseResult.failCode(400, 'roomId is required');
                    return res.status(400).json(result);
                }

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                const rtpCapabilities = room.getRtpCapabilities();

                const result = ResponseResult.ok({
                    rtpCapabilities,
                    roomId
                });
                res.json(result);

                console.log(`查询房间RTP能力: ${roomId}`);

            } catch (error) {
                console.error('查询RTP能力错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 创建 WebRTC 传输
        this.app.post('/room/transport/create', async (req, res) => {
            try {
                const { roomId, peerId } = req.body;

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                const transport = await room.createWebRtcTransport(peerId);

                const result = ResponseResult.ok({
                    transport
                });
                res.json(result);

                console.log(`为用户 ${peerId} 创建传输: ${transport.id}`);

            } catch (error) {
                console.error('创建传输错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 连接传输
        this.app.post('/room/transport/connect', async (req, res) => {
            try {
                const { roomId, transportId, dtlsParameters, peerId } = req.body;

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                await room.connectTransport(peerId, transportId, dtlsParameters);

                const result = ResponseResult.ok();
                res.json(result);

                console.log(`传输连接成功: ${transportId}`);

            } catch (error) {
                console.error('连接传输错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 创建生产者
        this.app.post('/room/produce', async (req, res) => {
            try {
                const { roomId, transportId, kind, rtpParameters, peerId } = req.body;

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                const resultData = await room.produce(peerId, transportId, kind, rtpParameters);

                const result = ResponseResult.ok({
                    id: resultData.id
                });
                res.json(result);

                console.log(`创建生产者: ${resultData.id} (${kind}) for user ${peerId}`);

            } catch (error) {
                console.error('创建生产者错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 创建消费者
        this.app.post('/room/consume', async (req, res) => {
            try {
                const { roomId, transportId, producerId, rtpCapabilities, peerId } = req.body;

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                const resultData = await room.consume(peerId, transportId, producerId, rtpCapabilities);

                const result = ResponseResult.ok(resultData);
                res.json(result);

                console.log(`创建消费者: ${resultData.id} for producer ${producerId}`);

            } catch (error) {
                console.error('创建消费者错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 恢复消费者
        this.app.post('/room/consumer/resume', async (req, res) => {
            try {
                const { roomId, consumerId, peerId } = req.body;

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                await room.resumeConsumer(peerId, consumerId);

                const result = ResponseResult.ok();
                res.json(result);

                console.log(`恢复消费者: ${consumerId}`);

            } catch (error) {
                console.error('恢复消费者错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 获取房间内的生产者列表
        this.app.post('/room/producers', async (req, res) => {
            try {
                const { roomId } = req.body;

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                const producers = room.getProducers();

                const result = ResponseResult.ok({
                    producers
                });
                res.json(result);

            } catch (error) {
                console.error('获取生产者列表错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 离开房间
        this.app.post('/room/leave', async (req, res) => {
            try {
                const { roomId, peerId } = req.body;

                const room = this.rooms.get(roomId);
                if (!room) {
                    const result = ResponseResult.failCode(404, 'Room not found');
                    return res.status(404).json(result);
                }

                room.removePeer(peerId);

                // 如果房间为空，清理房间
                if (room.peers.size === 0) {
                    this.rooms.delete(roomId);
                    console.log(`媒体房间 ${roomId} 已清理 (空房间)`);
                }

                const result = ResponseResult.ok();
                res.json(result);

                console.log(`用户 ${peerId} 离开媒体房间: ${roomId}`);

            } catch (error) {
                console.error('离开房间错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });

        // 房间列表
        this.app.get('/rooms', async (req, res) => {
            try {
                const roomsInfo = Array.from(this.rooms.entries()).map(([id, room]) => ({
                    id,
                    peerCount: room.peers.size,
                    producers: room.getProducers().length
                }));

                const result = ResponseResult.ok({
                    rooms: roomsInfo
                });
                res.json(result);

            } catch (error) {
                console.error('获取房间列表错误:', error);
                const result = ResponseResult.fail(error.message);
                res.status(500).json(result);
            }
        });
    }

    start() {
        this.server = this.app.listen(config.http.port, config.http.host, () => {
            console.log(`媒体服务器运行在 ${config.http.host}:${config.http.port}`);
            console.log(`健康检查: http://${config.http.host}:${config.http.port}/health`);
            console.log(`服务器信息: http://${config.http.host}:${config.http.port}/info`);
            console.log(`房间列表: http://${config.http.host}:${config.http.port}/rooms`);
        });
    }
}

// 启动服务器
const server = new MediaServer();
server.start();

// 优雅关闭
process.on('SIGTERM', () => {
    console.log('SIGTERM received, shutting down media server');
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log('SIGINT received, shutting down media server');
    process.exit(0);
});