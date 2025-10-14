class MediasoupClient extends EventEmitter {
    constructor(signalingHandler) {
        super();
        this.device = null;
        this.sendTransport = null;
        this.recvTransport = null;
        this.producers = new Map();
        this.consumers = new Map();
        this.isConnected = false;
        this.signalingHandler = signalingHandler;

        this._loadMediasoupClient();
    }

    _loadMediasoupClient() {
        if (typeof mediasoupClient !== 'undefined') {
            this._onLibraryLoaded();
        } else {
            // 监听库加载完成
            window.addEventListener('mediasoupClientLoaded', () => {
                this._onLibraryLoaded();
            });
        }
    }

    _onLibraryLoaded() {
        Logger.info('mediasoup-client库加载完成');
        this.emit('libraryLoaded');
    }

    async initializeDevice(routerRtpCapabilities) {
        try {
            Logger.info('初始化mediasoup设备...');

            Logger.info('开始加载mediasoup设备...');
            Logger.info('服务端RTP能力类型:', typeof routerRtpCapabilities);
            Logger.info('服务端RTP能力键名:', Object.keys(routerRtpCapabilities || {}));

            this.device = new mediasoupClient.Device();
            await this.device.load({routerRtpCapabilities});

            Logger.info('设备加载完成:', {
                设备对象: !!this.device,
                RTP能力: this.device.rtpCapabilities,
                是否可以加载: this.device.loaded
            });

            Logger.info('mediasoup设备初始化成功');
            return this.device;

        } catch (error) {
            Logger.error('mediasoup设备初始化失败:', error);
            throw new Error(`设备初始化失败: ${error.message}`);
        }
    }

    async createSendTransport(transportOptions) {
        try {
            Logger.info('创建发送传输...');
            this.sendTransport = this.device.createSendTransport(transportOptions);

            // 设置传输事件监听
            this._setupTransportEvents(this.sendTransport, 'send');

            Logger.info('发送传输创建成功:', this.sendTransport.id);
            return this.sendTransport;

        } catch (error) {
            Logger.error('创建发送传输失败:', error);
            throw error;
        }
    }

    async createRecvTransport(transportOptions) {
        try {
            Logger.info('创建接收传输...');

            this.recvTransport = this.device.createRecvTransport(transportOptions);

            // 设置传输事件监听
            this._setupTransportEvents(this.recvTransport, 'recv');

            Logger.info('接收传输创建成功:', this.recvTransport.id);
            return this.recvTransport;

        } catch (error) {
            Logger.error('创建接收传输失败:', error);
            throw error;
        }
    }

    _setupTransportEvents(transport, direction) {
        // 连接事件
        transport.on('connect', async ({dtlsParameters}, callback, errback) => {
            try {
                Logger.info(`传输连接: ${transport.id}`);

                // 通过信令连接传输
                await this.signalingHandler.connectTransport(transport.id, dtlsParameters);
                callback();

            } catch (error) {
                Logger.error(`传输连接失败: ${error.message}`);
                errback(error);
            }
        });

        if (direction === 'send') {
            // 生产事件
            transport.on('produce', async ({kind, rtpParameters}, callback, errback) => {
                try {
                    Logger.info(`开始生产: ${kind}`);

                    // 通过信令创建生产者
                    const produceRes = await this.signalingHandler.produce(
                        transport.id,
                        kind,
                        rtpParameters
                    );

                    const producerData = JSON.parse(produceRes).produceResponse;

                    callback({id: producerData.producerId});
                    Logger.info(`生产者创建成功: ${producerData.producerId}`);

                } catch (error) {
                    Logger.error(`生产失败: ${error.message}`);
                    errback(error);
                }
            });
        }

        // 连接状态事件
        transport.on('connectionstatechange', (state) => {
            Logger.info(`传输连接状态变更: ${state}`);
            this.emit('transportStateChange', {transportId: transport.id, state});

            if (state === 'connected') {
                this.isConnected = true;
                this.emit('connected');
            } else if (state === 'failed' || state === 'disconnected') {
                this.isConnected = false;
                this.emit('disconnected');
            }
        });
    }

    async produceAudio(audioStream) {
        return this._produceTrack('audio', audioStream.getAudioTracks()[0]);
    }

    async produceVideo(videoStream) {
        return this._produceTrack('video', videoStream.getVideoTracks()[0]);
    }

    async _produceTrack(kind, track) {
        try {
            if (!this.sendTransport) {
                throw new Error('发送传输未创建');
            }

            Logger.info(`生产${kind}轨道...`);

            const params = {
                track
                // ,
                // encodings: kind === 'video' ? [
                //     {scalabilityMode: 'S3T3'}
                // ] : undefined,
                // codecOptions: kind === 'video' ? {
                //     videoGoogleStartBitrate: 1000
                // } : undefined
            };

            const producer = await this.sendTransport.produce(params);

            // 监听生产者事件
            producer.on('trackended', () => {
                Logger.info(`生产者轨道结束: ${producer.id}`);
                this.producers.delete(producer.id);
                this.emit('producerClosed', producer.id);
            });

            producer.on('transportclose', () => {
                Logger.info(`生产者传输关闭: ${producer.id}`);
                this.producers.delete(producer.id);
            });

            this.producers.set(producer.id, producer);
            Logger.info(`${kind}生产者创建成功:`, producer.id);

            return producer;

        } catch (error) {
            Logger.error(`生产${kind}轨道失败:`, error);
            throw error;
        }
    }

    async consume(producerId) {
        try {
            if (!this.recvTransport) {
                throw new Error('接收传输未创建');
            }

            Logger.info(`开始消费生产者: ${producerId}`);

            // 通过信令创建消费者
            const consumerOptionsRes = await this.signalingHandler.consume(
                this.recvTransport.id,
                producerId,
                this.getRtpCapabilities()
            );

            const consumeResponse = JSON.parse(consumerOptionsRes).consumeResponse;

            // 构建 mediasoup 客户端期望的消费者选项格式
            const consumerOptions = {
                id: consumeResponse.consumerId,
                producerId: consumeResponse.producerId,
                kind: consumeResponse.kind,
                rtpParameters: JSON.parse(consumeResponse.rtpParameters),
                type: consumeResponse.type
            };

            const consumer = await this.recvTransport.consume(consumerOptions);

            // 监听消费者事件
            consumer.on('trackended', () => {
                Logger.info(`消费者轨道结束: ${consumer.id}`);
                this.consumers.delete(consumer.id);
                this.emit('consumerClosed', consumer.id);
            });

            consumer.on('transportclose', () => {
                Logger.info(`消费者传输关闭: ${consumer.id}`);
                this.consumers.delete(consumer.id);
            });

            this.consumers.set(consumer.id, consumer);
            Logger.info(`消费者创建成功:`, consumer.id);

            // 恢复消费者
            await this.signalingHandler.resumeConsumer(consumer.id);

            return consumer;

        } catch (error) {
            Logger.error(`消费失败:`, error);
            throw error;
        }
    }

    async closeProducer(producerId) {
        const producer = this.producers.get(producerId);
        if (producer) {
            producer.close();
            this.producers.delete(producerId);
            Logger.info(`生产者已关闭: ${producerId}`);
        }
    }

    async closeConsumer(consumerId) {
        const consumer = this.consumers.get(consumerId);
        if (consumer) {
            consumer.close();
            this.consumers.delete(consumerId);
            Logger.info(`消费者已关闭: ${consumerId}`);
        }
    }

    async closeAll() {
        // 关闭所有生产者
        this.producers.forEach(producer => {
            producer.close();
        });
        this.producers.clear();

        // 关闭所有消费者
        this.consumers.forEach(consumer => {
            consumer.close();
        });
        this.consumers.clear();

        // 关闭传输
        if (this.sendTransport) {
            this.sendTransport.close();
            this.sendTransport = null;
        }

        if (this.recvTransport) {
            this.recvTransport.close();
            this.recvTransport = null;
        }

        this.isConnected = false;
        Logger.info('mediasoup客户端已关闭');
    }

    getProducers() {
        return Array.from(this.producers.values());
    }

    getConsumers() {
        return Array.from(this.consumers.values());
    }

    canProduce(kind) {
        return this.device && this.device.canProduce(kind);
    }

    getRtpCapabilities() {
        return this.device ? this.device.rtpCapabilities : null;
    }

    destroy() {
        this.closeAll();
        this.device = null;
        Logger.info('mediasoup客户端已销毁');
    }
}

// 导出到全局
window.MediasoupClient = MediasoupClient;