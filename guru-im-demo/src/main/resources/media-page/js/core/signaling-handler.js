class SignalingHandler extends EventEmitter {
    constructor() {
        super();
        this.pendingRequests = new Map();
        this.requestId = 1;
        this.sessionId = this.getValueFromURL('sessionId');
        this.userId = this.getValueFromURL('userId');
        this.callType = this.getValueFromURL('callType');
    }

    // 发送请求到Java端
    sendRequest(method, data = {}) {
        return new Promise((resolve, reject) => {
            const requestId = this.requestId++;
            const request = {
                method,
                data,
                requestId: requestId.toString(),
                sessionId: this.sessionId
            };

            this.pendingRequests.set(requestId, { resolve, reject });

            try {
                if (window.cefQuery) {
                    window.cefQuery({
                        request: JSON.stringify(request),
                        onSuccess: (response) => {
                            this._handleResponse(response, requestId);
                        },
                        onFailure: (errorCode, errorMessage) => {
                            this._handleError(requestId, `CEF错误 ${errorCode}: ${errorMessage}`);
                        }
                    });
                } else {
                    this._handleError(requestId, `发送请求失败: CEF接口不可用`);
                }
            } catch (error) {
                this._handleError(requestId, `发送请求失败: ${error.message}`);
            }
        });
    }

    _handleResponse(response, requestId) {
        try {
            const pending = this.pendingRequests.get(requestId);
            if (!pending) {
                Logger.warn('收到未知请求ID的响应:', requestId);
                return;
            }

            const result = JSON.parse(response);

            if (result.success) {
                pending.resolve(result.data || {});
            } else {
                pending.reject(new Error(result.error || '未知错误'));
            }

            this.pendingRequests.delete(requestId);

        } catch (error) {
            this._handleError(requestId, `解析响应失败: ${error.message}`);
        }
    }

    _handleError(requestId, errorMessage) {
        const pending = this.pendingRequests.get(requestId);
        if (pending) {
            pending.reject(new Error(errorMessage));
            this.pendingRequests.delete(requestId);
        }
    }

    // 设备相关信令
    async devicesReady(data) {
        return this.sendRequest('devicesReady', data);
    }

    async devicesFailed(data) {
        return this.sendRequest('devicesFailed', data);
    }

    // 通话控制信令
    async callAccepted(data) {
        return this.sendRequest('callAccepted', data);
    }

    async callRejected(data) {
        return this.sendRequest('callRejected', data);
    }

    async callCancelled(data) {
        return this.sendRequest('callCancelled', data);
    }

    async conferenceCancelled(reason) {
        return this.sendRequest('conferenceCancelled', { reason });
    }

    async callEnded(data) {
        return this.sendRequest('callEnded', data);
    }

    // mediasoup信令

    async joinRoom(sessionId) {
        return this.sendRequest('joinRoom', {sessionId});
    }

    async getRouterRtpCapabilities(roomId) {
        return this.sendRequest('getRouterRtpCapabilities', { roomId });
    }

    async createTransport(roomId, direction, options) {
        return this.sendRequest('createTransport', {
            roomId,
            direction,
            options
        });
    }

    async connectTransport(transportId, dtlsParameters) {
        return this.sendRequest('connectTransport', {
            transportId,
            dtlsParameters
        });
    }

    async produce(transportId, kind, rtpParameters) {
        return this.sendRequest('produce', {
            transportId,
            kind,
            rtpParameters
        });
    }

    async consume(transportId, producerId, rtpCapabilities) {
        return this.sendRequest('consume', {
            transportId,
            producerId,
            rtpCapabilities
        });
    }

    async resumeConsumer(consumerId) {
        return this.sendRequest('resumeConsumer', { consumerId });
    }

    async getRoomProducers(roomId) {
        return this.sendRequest('getRoomProducers', { roomId });
    }

    async callTimeout(data) {
        return await this.sendRequest('callTimeout', data);
    }

    // 窗口控制
    async closeWindow() {
        return this.sendRequest('closeWindow');
    }

    async minimizeWindow() {
        return this.sendRequest('minimizeWindow');
    }

    // 日志
    async log(level, message) {
        return this.sendRequest('log', { level, message });
    }

    destroy() {
        this.pendingRequests.clear();
        Logger.info('信令处理器已销毁');
    }

    getValueFromURL(key) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(key);
    }
}

// 导出到全局
window.SignalingHandler = SignalingHandler;