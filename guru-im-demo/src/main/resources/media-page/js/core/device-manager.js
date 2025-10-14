// resources/media-page/js/device-manager.js
class DeviceManager extends EventEmitter {
    constructor() {
        super();
        this.devices = {
            audioinput: [],
            videoinput: [],
            audiooutput: []
        };
        this.selectedDevices = {
            audioinput: null,
            videoinput: null,
            audiooutput: null
        };
        this.streams = new Map();
        this.isInitialized = false;
        this.hasCameraPermission = false;
        this.hasMicrophonePermission = false;
    }

    async initialize() {
        try {
            Logger.info('开始初始化设备...');

            // 请求设备权限
            await this.requestPermissions();

            // 枚举设备
            await this.enumerateDevices();

            // 选择默认设备
            await this.selectDefaultDevices();

            this.isInitialized = true;
            Logger.info('设备初始化完成', {
                麦克风: this.devices.audioinput.length,
                摄像头: this.devices.videoinput.length,
                扬声器: this.devices.audiooutput.length
            });

            this.emit('devicesReady', this.devices);

        } catch (error) {
            Logger.error('设备初始化失败:', error);
            this.emit('devicesFailed', error.message);
            throw error;
        }
    }

    async requestPermissions() {
        try {
            // 首先只请求音频权限，这是必需的
            Logger.info('请求麦克风权限...');
            const audioStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true
                },
                video: false
            });

            // 立即停止流，我们只需要权限
            this._stopStream(audioStream);
            this.hasMicrophonePermission = true;
            Logger.info('麦克风权限获取成功');

            // 然后尝试请求视频权限，但不强制
            try {
                Logger.info('尝试请求摄像头权限...');
                const videoStream = await navigator.mediaDevices.getUserMedia({
                    audio: false,
                    video: {
                        width: { ideal: 640 },
                        height: { ideal: 480 }
                    }
                });
                this._stopStream(videoStream);
                this.hasCameraPermission = true;
                Logger.info('摄像头权限获取成功');
            } catch (videoError) {
                Logger.warn('摄像头权限获取失败，将继续使用音频模式:', videoError.message);
                this.hasCameraPermission = false;
                // 不抛出错误，允许继续使用音频
            }

            Logger.info('设备权限获取完成', {
                麦克风权限: this.hasMicrophonePermission,
                摄像头权限: this.hasCameraPermission
            });

        } catch (error) {
            // 如果音频权限也失败，才抛出错误
            if (!this.hasMicrophonePermission) {
                throw new Error(`设备权限获取失败: ${error.message}`);
            }
        }
    }

    async enumerateDevices() {
        try {
            const deviceList = await navigator.mediaDevices.enumerateDevices();

            this.devices = {
                audioinput: [],
                videoinput: [],
                audiooutput: []
            };

            deviceList.forEach(device => {
                if (this.devices[device.kind]) {
                    this.devices[device.kind].push({
                        deviceId: device.deviceId,
                        label: device.label || `未知${this.getDeviceTypeName(device.kind)}`,
                        kind: device.kind
                    });
                }
            });

            Logger.info('设备枚举完成:', {
                音频输入: this.devices.audioinput.length,
                视频输入: this.devices.videoinput.length,
                音频输出: this.devices.audiooutput.length
            });

        } catch (error) {
            throw new Error(`设备枚举失败: ${error.message}`);
        }
    }

    async selectDefaultDevices() {
        // 选择默认音频设备
        if (this.devices.audioinput.length > 0) {
            this.selectedDevices.audioinput = this.devices.audioinput[0].deviceId;
        } else {
            throw new Error('未找到可用的麦克风设备');
        }

        // 选择默认视频设备（如果有）
        if (this.devices.videoinput.length > 0) {
            this.selectedDevices.videoinput = this.devices.videoinput[0].deviceId;
        } else {
            Logger.warn('未找到可用的摄像头设备');
        }

        // 选择默认音频输出设备
        if (this.devices.audiooutput.length > 0) {
            this.selectedDevices.audiooutput = this.devices.audiooutput[0].deviceId;
        }

        Logger.info('默认设备选择完成:', this.selectedDevices);
    }

    async getAudioStream(constraints = {}) {
        return this._getStream('audioinput', {
            audio: {
                deviceId: constraints.deviceId ? { exact: constraints.deviceId } : undefined,
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true,
                ...constraints.audio
            },
            video: false
        });
    }

    async getVideoStream(constraints = {}) {
        if (!this.hasCameraPermission) {
            throw new Error('没有摄像头权限或未找到摄像头设备');
        }

        return this._getStream('videoinput', {
            video: {
                deviceId: constraints.deviceId ? { exact: constraints.deviceId } : undefined,
                width: { ideal: 1280 },
                height: { ideal: 720 },
                frameRate: { ideal: 30 },
                ...constraints.video
            },
            audio: false
        });
    }

    async getDisplayStream() {
        try {
            if (!navigator.mediaDevices.getDisplayMedia) {
                throw new Error('屏幕共享不支持');
            }

            // 使用最简参数
            const stream = await navigator.mediaDevices.getDisplayMedia({
                video: { displaySurface: 'window' },   // 让用户选择共享目标
                audio: true    // 让用户选择音频源
            });

            // 检查是否获得了视频轨道
            if (stream.getVideoTracks().length === 0) {
                throw new Error('未选择共享目标');
            }

            const streamId = Utils.generateId();
            this.streams.set(streamId, stream);

            // 监听停止共享事件
            stream.getVideoTracks()[0].addEventListener('ended', () => {
                Logger.info('屏幕共享已停止');
                this.emit('screenShareEnded');
                this._stopStream(stream);
                this.streams.delete(streamId);
            });

            Logger.info('屏幕共享流获取成功', {
                videoTracks: stream.getVideoTracks().length,
                audioTracks: stream.getAudioTracks().length
            });

            return stream;

        } catch (error) {
            Logger.error('屏幕共享失败:', error);
            throw new Error(`屏幕共享失败: ${error.message}`);
        }
    }

    async _getStream(deviceType, constraints) {
        try {
            if (!this.isInitialized) {
                throw new Error('设备管理器未初始化');
            }

            if (deviceType === 'videoinput' && this.devices.videoinput.length === 0) {
                throw new Error('未找到可用的摄像头设备');
            }

            if (deviceType === 'audioinput' && this.devices.audioinput.length === 0) {
                throw new Error('未找到可用的麦克风设备');
            }

            const stream = await navigator.mediaDevices.getUserMedia(constraints);
            const streamId = Utils.generateId();
            this.streams.set(streamId, stream);

            Logger.info(`${this.getDeviceTypeName(deviceType)}流获取成功`);
            return { stream, streamId };

        } catch (error) {
            Logger.error('获取媒体流失败:', error);
            throw new Error(`获取${this.getDeviceTypeName(deviceType)}流失败: ${error.message}`);
        }
    }

    stopStream(streamId) {
        const stream = this.streams.get(streamId);
        if (stream) {
            this._stopStream(stream);
            this.streams.delete(streamId);
        }
    }

    _stopStream(stream) {
        if (stream) {
            stream.getTracks().forEach(track => {
                track.stop();
            });
        }
    }

    stopAllStreams() {
        this.streams.forEach((stream, streamId) => {
            this._stopStream(stream);
        });
        this.streams.clear();
        Logger.info('所有媒体流已停止');
    }

    async switchAudioDevice(deviceId) {
        try {
            this.selectedDevices.audioinput = deviceId;
            this.emit('audioDeviceChanged', deviceId);
            Logger.info('音频设备已切换:', deviceId);
        } catch (error) {
            Logger.error('切换音频设备失败:', error);
            throw error;
        }
    }

    async switchVideoDevice(deviceId) {
        try {
            this.selectedDevices.videoinput = deviceId;
            this.emit('videoDeviceChanged', deviceId);
            Logger.info('视频设备已切换:', deviceId);
        } catch (error) {
            Logger.error('切换视频设备失败:', error);
            throw error;
        }
    }

    getDeviceTypeName(kind) {
        const names = {
            audioinput: '麦克风',
            videoinput: '摄像头',
            audiooutput: '扬声器'
        };
        return names[kind] || kind;
    }

    getAvailableDevices() {
        return { ...this.devices };
    }

    getSelectedDevices() {
        return { ...this.selectedDevices };
    }

    hasVideoDevice() {
        return this.devices.videoinput.length > 0 && this.hasCameraPermission;
    }

    hasAudioDevice() {
        return this.devices.audioinput.length > 0 && this.hasMicrophonePermission;
    }

    // 新增方法：检查是否只有音频设备
    hasAudioOnly() {
        return this.hasAudioDevice() && !this.hasVideoDevice();
    }

    // 新增方法：获取权限状态
    getPermissionStatus() {
        return {
            hasMicrophonePermission: this.hasMicrophonePermission,
            hasCameraPermission: this.hasCameraPermission
        };
    }

    destroy() {
        this.stopAllStreams();
        this.isInitialized = false;
        Logger.info('设备管理器已销毁');
    }
}

// 导出到全局
window.DeviceManager = DeviceManager;