const MediaWorker = require('./media-worker');

class MediaRoom {
    constructor(roomId) {
        this.id = roomId;
        this.worker = null;
        this.peers = new Map(); // 简单的peer跟踪
    }

    async init() {
        this.worker = new MediaWorker();
        await this.worker.init();
        return this;
    }

    // 添加peer
    addPeer(peerId) {
        const peer = {
            id: peerId,
            transports: new Set(),
            producers: new Set(),
            consumers: new Set()
        };
        this.peers.set(peerId, peer);
        return peer;
    }

    getPeer(peerId) {
        return this.peers.get(peerId);
    }

    removePeer(peerId) {
        const peer = this.peers.get(peerId);
        if (peer) {
            // 清理peer的所有传输
            for (const transportId of peer.transports) {
                this.worker.closeTransport(transportId);
            }
            this.peers.delete(peerId);
        }
    }

    // 获取路由能力
    getRtpCapabilities() {
        return this.worker.getRtpCapabilities();
    }

    // 创建传输
    async createWebRtcTransport(peerId) {
        const transport = await this.worker.createWebRtcTransport();
        const peer = this.getPeer(peerId);
        if (peer) {
            peer.transports.add(transport.id);
        }
        return transport;
    }

    // 连接传输
    async connectTransport(peerId, transportId, dtlsParameters) {
        await this.worker.connectTransport(transportId, dtlsParameters);
    }

    // 创建生产者
    async produce(peerId, transportId, kind, rtpParameters) {
        const result = await this.worker.produce(transportId, kind, rtpParameters);
        const peer = this.getPeer(peerId);
        if (peer) {
            peer.producers.add(result.id);
        }
        return result;
    }

    // 创建消费者
    async consume(peerId, transportId, producerId, rtpCapabilities) {
        const result = await this.worker.consume(transportId, producerId, rtpCapabilities);
        const peer = this.getPeer(peerId);
        if (peer) {
            peer.consumers.add(result.id);
        }
        return result;
    }

    // 恢复消费者
    async resumeConsumer(peerId, consumerId) {
        await this.worker.resumeConsumer(consumerId);
    }

    // 获取房间内所有生产者
    getProducers() {
        const producers = [];
        for (const peer of this.peers.values()) {
            for (const producerId of peer.producers) {
                // 使用 worker 获取生产者信息
                const producer = this.worker.getProducer(producerId);
                if (producer) {
                    producers.push({
                        producerId: producerId,
                        peerId: peer.id,
                        kind: producer.kind
                    });
                }
            }
        }
        return producers;
    }
}

module.exports = MediaRoom;