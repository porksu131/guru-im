const mediasoup = require('mediasoup');
const config = require('./config').mediasoup;

class MediaWorker {
    constructor() {
        this.worker = null;
        this.router = null;
        this.transports = new Map();
        this.producers = new Map();
        this.consumers = new Map();
    }

    async init() {
        this.worker = await mediasoup.createWorker(config.workerSettings);

        this.worker.on('died', () => {
            console.error('Media worker died');
            process.exit(1);
        });

        this.router = await this.worker.createRouter(config.routerOptions);
        console.log(`Media worker created, router id: ${this.router.id}`);

        return this;
    }

    // 获取路由能力
    getRtpCapabilities() {
        return this.router.rtpCapabilities;
    }

    // 创建 WebRTC 传输
    async createWebRtcTransport() {
        const transport = await this.router.createWebRtcTransport(
            config.webRtcTransportOptions
        );

        this.transports.set(transport.id, transport);

        // 清理关闭的传输
        transport.on('dtlsstatechange', (dtlsState) => {
            if (dtlsState === 'closed') {
                this.transports.delete(transport.id);
            }
        });

        transport.on('close', () => {
            this.transports.delete(transport.id);
        });

        return {
            id: transport.id,
            iceParameters: transport.iceParameters,
            iceCandidates: transport.iceCandidates,
            dtlsParameters: transport.dtlsParameters
        };
    }

    // 连接传输
    async connectTransport(transportId, dtlsParameters) {
        const transport = this.transports.get(transportId);
        if (!transport) throw new Error(`Transport ${transportId} not found`);
        await transport.connect({ dtlsParameters });
    }

    // 创建生产者
    async produce(transportId, kind, rtpParameters) {
        const transport = this.transports.get(transportId);
        if (!transport) throw new Error(`Transport ${transportId} not found`);

        const producer = await transport.produce({ kind, rtpParameters });
        this.producers.set(producer.id, producer);

        producer.on('transportclose', () => {
            this.producers.delete(producer.id);
        });

        return {
            id: producer.id,
            kind: producer.kind
        };
    }

    // 创建消费者
    async consume(transportId, producerId, rtpCapabilities) {
        const transport = this.transports.get(transportId);
        if (!transport) throw new Error(`Transport ${transportId} not found`);

        const producer = this.producers.get(producerId);
        if (!producer) throw new Error(`Producer ${producerId} not found`);

        if (!this.router.canConsume({ producerId, rtpCapabilities })) {
            throw new Error('Cannot consume');
        }

        const consumer = await transport.consume({
            producerId,
            rtpCapabilities,
            paused: true
        });

        this.consumers.set(consumer.id, consumer);

        consumer.on('transportclose', () => {
            this.consumers.delete(consumer.id);
        });

        return {
            id: consumer.id,
            producerId: producerId,
            kind: consumer.kind,
            rtpParameters: consumer.rtpParameters,
            type: consumer.type
        };
    }

    // 恢复消费者
    async resumeConsumer(consumerId) {
        const consumer = this.consumers.get(consumerId);
        if (!consumer) throw new Error(`Consumer ${consumerId} not found`);
        await consumer.resume();
    }

    // 关闭传输
    async closeTransport(transportId) {
        const transport = this.transports.get(transportId);
        if (transport) {
            transport.close();
            this.transports.delete(transportId);
        }
    }

    // 关闭生产者
    async closeProducer(producerId) {
        const producer = this.producers.get(producerId);
        if (producer) {
            producer.close();
            this.producers.delete(producerId);
        }
    }

    // 获取所有生产者
    getAllProducers() {
        const producers = [];
        for (const [producerId, producer] of this.producers) {
            producers.push({
                producerId,
                kind: producer.kind
            });
        }
        return producers;
    }

    // 根据ID获取生产者
    getProducer(producerId) {
        return this.producers.get(producerId);
    }
}

module.exports = MediaWorker;