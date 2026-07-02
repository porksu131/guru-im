class VideoCallApp {
    constructor() {
        this.initData = null;
        this.deviceManager = null;
        this.signalingHandler = null;
        this.mediasoupClient = null;

        // 媒体流
        this.audioStream = null;
        this.videoStream = null;
        this.localStream = null;

        // 生产者
        this.audioProducer = null;
        this.videoProducer = null;

        // 消费者
        this.audioConsumer = null;
        this.videoConsumer = null;

        // 音频元素
        this.remoteAudioElement = null;

        // 视频元素
        this.localVideo = null;
        this.remoteVideo = null;

        // 通话状态
        this.callStartTime = null;
        this.callTimer = null;
        this.isMuted = false;
        this.isVideoOff = false;
        this.isIncomingCall = false;
        this.isCallActive = false;
        this.isInitialized = false;

        // 音效
        this.ringtone = null;
        this.callTone = null;
        this.hangupTone = null;
        this.isRinging = false;
        this.isCalling = false;

        // 本地视频拖动相关
        this.isDragging = false;
        this.dragOffset = { x: 0, y: 0 };
        this.localVideoBounds = null;

        // 超时设置
        this.callTimeoutDuration = 35000;
        this.answerTimeoutDuration = 30000;

        // 初始化基础组件
        this.initializeBaseComponents();
    }

    // 初始化音效
    initializeSounds() {
        try {
            this.ringtone = document.getElementById('ringtone');
            this.callTone = document.getElementById('callTone');
            this.hangupTone = document.getElementById('hangupTone');

            if (this.ringtone) this.ringtone.volume = 0.6;
            if (this.callTone) this.callTone.volume = 0.4;
            if (this.hangupTone) this.hangupTone.volume = 0.4;

            Logger.info('音效初始化完成');
        } catch (error) {
            Logger.warn('音效初始化失败:', error);
        }
    }

    // 播放振铃音
    playRingtone() {
        try {
            if (this.ringtone && !this.isRinging) {
                this.ringtone.currentTime = 0;
                this.ringtone.play().catch(e => {
                    Logger.warn('播放振铃音失败:', e);
                });
                this.isRinging = true;
                Logger.info('开始播放振铃音');
            }
        } catch (error) {
            Logger.warn('播放振铃音出错:', error);
        }
    }

    // 停止振铃音
    stopRingtone() {
        try {
            if (this.ringtone && this.isRinging) {
                this.ringtone.pause();
                this.ringtone.currentTime = 0;
                this.isRinging = false;
                Logger.info('停止播放振铃音');
            }
        } catch (error) {
            Logger.warn('停止振铃音出错:', error);
        }
    }

    // 播放呼叫音
    playCallTone() {
        try {
            if (this.callTone && !this.isCalling) {
                this.callTone.currentTime = 0;
                this.callTone.loop = true;
                this.callTone.play().catch(e => {
                    Logger.warn('播放呼叫音失败:', e);
                });
                this.isCalling = true;
                Logger.info('开始播放呼叫音（循环）');
            }
        } catch (error) {
            Logger.warn('播放呼叫音出错:', error);
        }
    }

    // 停止呼叫音
    stopCallTone() {
        try {
            if (this.callTone && this.isCalling) {
                this.callTone.pause();
                this.callTone.currentTime = 0;
                this.isCalling = false;
                Logger.info('停止播放呼叫音');
            }
        } catch (error) {
            Logger.warn('停止呼叫音出错:', error);
        }
    }

    // 播放挂断音
    playHangupTone() {
        try {
            if (this.hangupTone) {
                this.hangupTone.currentTime = 0;
                this.hangupTone.play().catch(e => {
                    Logger.warn('播放挂断音失败:', e);
                });
                Logger.info('播放挂断音');
            }
        } catch (error) {
            Logger.warn('播放挂断音出错:', error);
        }
    }

    // 初始化基础组件
    async initializeBaseComponents() {
        try {
            Logger.info('初始化视频通话基础组件...');

            // 初始化管理器
            this.deviceManager = new DeviceManager();
            this.signalingHandler = new SignalingHandler();
            this.mediasoupClient = new MediasoupClient(this.signalingHandler);

            // 初始化音效
            this.initializeSounds();

            // 设置UI事件
            this.setupUIEvents();

            // 获取视频元素
            this.localVideo = document.getElementById('localVideo');
            this.remoteVideo = document.getElementById('remoteVideo');

            // 初始化本地视频拖动功能
            this.initializeLocalVideoDrag();

            // 显示等待初始化数据的界面
            this.showWaitingForInitData();

            Logger.info('基础组件初始化完成，等待初始化数据...');

            // 主动请求初始化数据
            await this.requestCallInitData();

        } catch (error) {
            Logger.error('基础组件初始化失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // 初始化本地视频拖动功能
    initializeLocalVideoDrag() {
        const localVideoContainer = document.getElementById('localVideoContainer');

        if (!localVideoContainer) return;

        // 鼠标按下事件
        localVideoContainer.addEventListener('mousedown', (e) => {
            // 排除调整大小区域的点击
            if (e.offsetX > localVideoContainer.offsetWidth - 20 &&
                e.offsetY > localVideoContainer.offsetHeight - 20) {
                return; // 这是调整大小区域，不进行拖动
            }

            this.startDragging(e);
        });

        // 触摸事件
        localVideoContainer.addEventListener('touchstart', (e) => {
            if (e.touches.length === 1) {
                this.startDragging(e.touches[0]);
                e.preventDefault();
            }
        });

        // 鼠标移动事件
        document.addEventListener('mousemove', (e) => {
            this.handleDragging(e);
        });

        // 触摸移动事件
        document.addEventListener('touchmove', (e) => {
            if (e.touches.length === 1) {
                this.handleDragging(e.touches[0]);
                e.preventDefault();
            }
        });

        // 结束拖动
        document.addEventListener('mouseup', () => {
            this.stopDragging();
        });

        document.addEventListener('touchend', () => {
            this.stopDragging();
        });

        // 限制本地视频在可视区域内
        this.updateLocalVideoBounds();
        window.addEventListener('resize', () => {
            this.updateLocalVideoBounds();
            this.constrainLocalVideoPosition();
        });
    }

    // 开始拖动
    startDragging(e) {
        const localVideoContainer = document.getElementById('localVideoContainer');
        if (!localVideoContainer) return;

        this.isDragging = true;
        localVideoContainer.classList.add('dragging');

        const rect = localVideoContainer.getBoundingClientRect();
        this.dragOffset.x = e.clientX - rect.left;
        this.dragOffset.y = e.clientY - rect.top;

        // 临时提升z-index确保在最上层拖动
        localVideoContainer.style.zIndex = '1000';
    }

    // 处理拖动
    handleDragging(e) {
        if (!this.isDragging) return;

        const localVideoContainer = document.getElementById('localVideoContainer');
        if (!localVideoContainer) return;

        let x = e.clientX - this.dragOffset.x;
        let y = e.clientY - this.dragOffset.y;

        // 限制在边界内
        x = Math.max(20, Math.min(x, window.innerWidth - localVideoContainer.offsetWidth - 20));
        y = Math.max(20, Math.min(y, window.innerHeight - localVideoContainer.offsetHeight - 20));

        localVideoContainer.style.left = x + 'px';
        localVideoContainer.style.top = y + 'px';
        localVideoContainer.style.right = 'auto'; // 清除right定位
    }

    // 停止拖动
    stopDragging() {
        if (!this.isDragging) return;

        this.isDragging = false;
        const localVideoContainer = document.getElementById('localVideoContainer');

        if (localVideoContainer) {
            localVideoContainer.classList.remove('dragging');
            localVideoContainer.style.zIndex = '10'; // 恢复原来的z-index

            // 确保位置在边界内
            this.constrainLocalVideoPosition();
        }
    }

    // 更新本地视频边界
    updateLocalVideoBounds() {
        this.localVideoBounds = {
            minX: 20,
            minY: 20,
            maxX: window.innerWidth - 220, // 考虑本地视频最小宽度
            maxY: window.innerHeight - 170 // 考虑本地视频最小高度
        };
    }

    // 限制本地视频位置在边界内
    constrainLocalVideoPosition() {
        const localVideoContainer = document.getElementById('localVideoContainer');
        if (!localVideoContainer || !this.localVideoBounds) return;

        const rect = localVideoContainer.getBoundingClientRect();

        let x = rect.left;
        let y = rect.top;

        x = Math.max(this.localVideoBounds.minX, Math.min(x, this.localVideoBounds.maxX));
        y = Math.max(this.localVideoBounds.minY, Math.min(y, this.localVideoBounds.maxY));

        localVideoContainer.style.left = x + 'px';
        localVideoContainer.style.top = y + 'px';
        localVideoContainer.style.right = 'auto';
    }

    // 请求初始化数据
    async requestCallInitData() {
        try {
            Logger.info('主动请求视频通话初始化数据...');
            const response = await this.signalingHandler.sendRequest('requestCallInitData');

            if (response) {
                const initData = JSON.parse(response);
                Logger.info('收到视频通话初始化数据:', initData);
                await this.handleCallInit(initData);
            } else {
                throw new Error('未收到初始化数据');
            }
        } catch (error) {
            Logger.error('请求初始化数据失败:', error);
            this.showError('获取通话数据失败: ' + error.message);
        }
    }

    // 显示等待初始化数据的界面
    showWaitingForInitData() {
        document.getElementById('loadingOverlay').classList.add('hidden');

        const waitingHtml = `
            <div id="waitingInit" style="
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                z-index: 1000;
                color: white;
                text-align: center;
            ">
                <div class="loading-spinner"></div>
                <div style="margin-top: 20px; font-size: 1.1rem;">等待视频通话初始化数据...</div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', waitingHtml);
    }

    hideWaitingForInitData() {
        const waitingElement = document.getElementById('waitingInit');
        if (waitingElement) {
            waitingElement.remove();
        }
    }

    // 初始化设备
    async initializeDevices() {
        try {
            Logger.info('初始化音视频设备...');

            if (this.deviceManager.isInitialized) {
                Logger.info('设备已初始化，跳过');
                return;
            }

            // 初始化设备管理器（内部会请求权限，视频失败只警告）
            await this.deviceManager.initialize();

            // 音频设备是必须的
            if (!this.deviceManager.hasAudioDevice()) {
                throw new Error('未找到可用的麦克风设备');
            }

            // 视频设备可选，只记录状态
            this.hasVideo = this.deviceManager.hasVideoDevice();
            if (!this.hasVideo) {
                Logger.warn('未检测到摄像头，将仅使用音频通话');
            } else {
                Logger.info('摄像头设备可用');
            }

        } catch (error) {
            // 只有音频初始化失败才真正中断
            if (!this.deviceManager || !this.deviceManager.hasAudioDevice()) {
                Logger.error('音频设备初始化失败:', error);
                await this.signalingHandler.devicesFailed({
                    sessionId: this.initData.sessionId,
                    error: error.message
                });
                throw error;
            } else {
                // 音频可用，但视频异常，降级
                Logger.warn('音频可用，但视频初始化异常，降级为音频模式');
                this.hasVideo = false;
            }
        }
    }

    // 处理通话初始化数据
    async handleCallInit(initData) {
        try {
            Logger.info('收到视频通话初始化数据，开始初始化设备...');
            this.initData = initData;
            this.isIncomingCall = initData.isIncoming;

            // 隐藏等待界面
            this.hideWaitingForInitData();

            // 初始化设备
            await this.initializeDevices();

            // 更新UI
            this.updateCallInfo();

            this.isInitialized = true;

            if (this.isIncomingCall) {
                await this.startIncomingCall();
            } else {
                await this.startOutgoingCall();
            }

        } catch (error) {
            Logger.error('处理视频通话初始化数据失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // 更新通话信息
    updateCallInfo() {
        try {
            const userName = this.initData.targetUserName;

            // 更新用户信息
            document.getElementById('remoteName').textContent = userName;
            document.getElementById('remoteAvatar').textContent = Utils.createAvatar(userName);

            // 更新状态
            const status = this.isIncomingCall ? '视频来电中...' : '视频呼叫中...';
            document.getElementById('remoteStatus').textContent = status;

            // 填充设备列表
            this.populateDeviceLists();

        } catch (error) {
            Logger.error('updateCallInfo error:', error);
        }
    }

    // 启动来电
    async startIncomingCall() {
        try {
            Logger.info('开始处理视频来电...');

            // 显示来电界面
            this.showIncomingCallUI();

            // 播放振铃音
            this.playRingtone();

            // 启动接听超时计时器
            this.startAnswerTimeout();

        } catch (error) {
            Logger.error('处理视频来电失败:', error);
            this.showError('处理来电失败: ' + error.message);
        }
    }

    // 显示来电界面
    showIncomingCallUI() {
        document.getElementById('incomingCallActions').classList.remove('hidden');
        document.getElementById('outgoingCallActions').classList.add('hidden');
        document.getElementById('controls').classList.remove('active');

        document.getElementById('acceptCallBtn').onclick = () => this.acceptCall();
        document.getElementById('rejectCallBtn').onclick = () => this.rejectCall();
    }

    // 启动去电
    async startOutgoingCall() {
        try {
            Logger.info('开始发起视频通话...');

            // 获取音视频流并显示本地视频
            await this.getMediaStreams();
            await this.showLocalVideo();

            // 发起呼叫
            await this.startCall();

            // 初始化mediasoup
            await this.initializeMediasoup();

            // 显示去电界面
            document.getElementById('outgoingCallActions').classList.remove('hidden');
            document.getElementById('incomingCallActions').classList.add('hidden');
            document.getElementById('outgoingHangupBtn').onclick = () => this.cancelCall();

            // 播放呼叫音
            this.playCallTone();

            // 启动呼叫超时计时器
            this.startCallTimeout();

        } catch (error) {
            Logger.error('发起视频通话失败:', error);
            this.showError('发起通话失败: ' + error.message);
        }
    }

    // 发起呼叫
    async startCall() {
        const callResponseStr = await this.signalingHandler.devicesReady({
            sessionId: this.initData.sessionId,
            targetUserId: this.initData.targetUserId,
            deviceId: this.initData.deviceInfo.deviceId,
            hasVideo: true
        });
        Logger.info('收到视频呼叫请求的响应:', callResponseStr);

        const callResponse = JSON.parse(callResponseStr);

        if (callResponse && callResponse.callRequestResponse) {
            if (callResponse.callRequestResponse.success) {
                this.sessionId = callResponse.callRequestResponse.sessionId;
                this.roomId = callResponse.callRequestResponse.roomId;
                Logger.info(`更新会话信息: sessionId=${this.sessionId}, roomId=${this.roomId}`);
            } else {
                throw new Error('视频呼叫请求失败: ' + (callResponse.callRequestResponse.errorMessage || '未知错误'));
            }
        } else {
            throw new Error('视频呼叫请求失败: 未知错误');
        }
    }

    // 获取音视频流
    async getMediaStreams() {
        try {
            // 获取音频（必须）
            const audioDevice = document.getElementById('audioDeviceSelect').value;
            const audioConstraints = audioDevice ? { deviceId: audioDevice } : {};
            const audioResult = await this.deviceManager.getAudioStream(audioConstraints);
            this.audioStream = audioResult.stream;

            // 尝试获取视频（非必须）
            try {
                const videoDevice = document.getElementById('videoDeviceSelect').value;
                const videoConstraints = videoDevice ? { deviceId: videoDevice } : {};
                const videoResult = await this.deviceManager.getVideoStream(videoConstraints);
                this.videoStream = videoResult.stream;
                this.hasVideo = true;
            } catch (videoError) {
                Logger.warn('获取视频流失败（可能被占用），仅使用音频:', videoError.message);
                this.videoStream = null;
                this.hasVideo = false;
                this.showNotification('本地摄像头不可用');
            }

            // 构建本地流（包含视频轨道如果有）
            const tracks = [...this.audioStream.getAudioTracks()];
            if (this.videoStream) {
                tracks.push(...this.videoStream.getVideoTracks());
            }
            this.localStream = new MediaStream(tracks);

            Logger.info('本地流获取成功，视频可用:', !!this.videoStream);

        } catch (error) {
            // 音频获取失败才抛出
            Logger.error('获取音频流失败:', error);
            throw error;
        }
    }

    // 显示本地视频
    async showLocalVideo() {
        try {
            if (this.localVideo && this.localStream) {
                this.localVideo.srcObject = this.localStream;
                const placeholder = document.getElementById('localVideoPlaceholder');
                if (!this.videoStream) {
                    placeholder.classList.remove('hidden');
                    placeholder.innerHTML = '<i class="fas fa-microphone"></i> 仅音频';
                } else {
                    placeholder.classList.add('hidden');
                }
                Logger.info('本地视频显示成功（视频:', !!this.videoStream, '）');
            }
        } catch (error) {
            Logger.error('显示本地视频失败:', error);
            // 不抛出，允许音频继续
        }
    }

    // 初始化 mediasoup
    async initializeMediasoup() {
        try {
            Logger.info('初始化mediasoup...');

            const roomId = this.roomId || `room_${this.initData.sessionId}`;

            // 获取RTP能力
            let rtpCapabilitiesRes = '{}';
            if (this.isIncomingCall) {
                rtpCapabilitiesRes = await this.signalingHandler.joinRoom(this.initData.sessionId);
            } else {
                rtpCapabilitiesRes = await this.signalingHandler.getRouterRtpCapabilities(roomId);
            }

            const rtpCapabilities = JSON.parse(JSON.parse(rtpCapabilitiesRes).rtpCapabilitiesResponse.rtpCapabilities);

            // 初始化设备
            await this.mediasoupClient.initializeDevice(rtpCapabilities.rtpCapabilities);

            if (!this.mediasoupClient.canProduce('audio') || !this.mediasoupClient.canProduce('video')) {
                throw new Error('设备不支持音视频生产，RTP能力不匹配');
            }

            // 创建发送传输
            const sendTransportOptionsRes = await this.signalingHandler.createTransport(
                roomId,
                'send',
                {}
            );
            const sendTransportOptions = JSON.parse(JSON.parse(sendTransportOptionsRes).transportCreate.transportOptions);
            await this.mediasoupClient.createSendTransport(sendTransportOptions);

            // 创建接收传输
            const recvTransportOptionsRes = await this.signalingHandler.createTransport(
                roomId,
                'recv',
                {}
            );
            const recvTransportOptions = JSON.parse(JSON.parse(recvTransportOptionsRes).transportCreate.transportOptions);
            await this.mediasoupClient.createRecvTransport(recvTransportOptions);

            // 生产音频
            this.audioProducer = await this.mediasoupClient.produceAudio(this.audioStream);

            // 生产视频（仅当有视频流）
            if (this.videoStream) {
                this.videoProducer = await this.mediasoupClient.produceVideo(this.videoStream);
            } else {
                this.videoProducer = null;
                Logger.info('无视频流，不创建视频生产者');
            }

            Logger.info('mediasoup初始化完成');

        } catch (error) {
            Logger.error('mediasoup初始化失败:', error);
            throw error;
        }
    }

    // 接听通话
    async acceptCall() {
        try {
            Logger.info('接听视频通话...');

            // 停止振铃音
            this.stopRingtone();
            this.stopAllTimeouts();

            // 隐藏接听界面
            document.getElementById('incomingCallActions').classList.add('hidden');

            // 获取音视频流并显示本地视频
            await this.getMediaStreams();
            await this.showLocalVideo();

            // 初始化mediasoup
            await this.initializeMediasoup();

            // 开始通话计时
            this.startCallTimer();

            // 更新状态
            this.isCallActive = true;
            document.getElementById('remoteStatus').textContent = '通话中';

            // 显示控制按钮
            document.getElementById('controls').classList.add('active');

            // 通知Java端通话已接受
            await this.signalingHandler.callAccepted({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId
            });

            this.roomId = `room_${this.initData.sessionId}`;

            // 消费对方的音视频
            await this.consumeRemoteMedia(this.roomId);

            Logger.info('视频通话已接听');

        } catch (error) {
            Logger.error('接听视频通话失败:', error);
            this.showError('接听失败: ' + error.message);
        }
    }

    // 消费远程媒体
    async consumeRemoteMedia(roomId) {
        try {
            Logger.info('开始消费远程音视频...');

            const producersRes = await this.signalingHandler.getRoomProducers(roomId);
            const roomProducers = JSON.parse(producersRes).roomProducersResponse;

            if (roomProducers && roomProducers.producers) {
                for (const producer of roomProducers.producers) {
                    if (producer.producerId !== this.audioProducer?.id && producer.producerId !== this.videoProducer?.id) {
                        await this.createMediaConsumer(roomId, producer.producerId, producer.kind);
                    }
                }
            }

            Logger.info('远程音视频消费完成');

        } catch (error) {
            Logger.error('消费远程音视频失败:', error);
        }
    }

    // 创建媒体消费者
    async createMediaConsumer(roomId, producerId, kind) {
        try {
            Logger.info(`创建${kind}消费者，producerId: ${producerId}`);

            const consumer = await this.mediasoupClient.consume(producerId);

            if (kind === 'audio') {
                this.audioConsumer = consumer;
                await this.playRemoteAudio(consumer.track);
            } else if (kind === 'video') {
                this.videoConsumer = consumer;
                await this.showRemoteVideo(consumer.track);
            }

            Logger.info(`${kind}消费者创建成功`);

        } catch (error) {
            Logger.error(`创建${kind}消费者失败:`, error);
        }
    }

    // 播放远程音频
    async playRemoteAudio(track) {
        try {
            if (!this.remoteAudioElement) {
                this.remoteAudioElement = document.createElement('audio');
                this.remoteAudioElement.autoplay = true;
                this.remoteAudioElement.volume = 1.0;
                this.remoteAudioElement.style.display = 'none'; // 隐藏
                document.body.appendChild(this.remoteAudioElement);
            }
            const stream = new MediaStream([track]);
            this.remoteAudioElement.srcObject = stream;
            await this.remoteAudioElement.play();
            Logger.info('远程音频播放已启动');
        } catch (error) {
            Logger.error('播放远程音频失败:', error);
        }
    }

    // 显示远程视频
    async showRemoteVideo(track) {
        try {
            const remoteVideoStream = new MediaStream([track]);

            if (this.remoteVideo) {
                this.remoteVideo.srcObject = remoteVideoStream;

                // 隐藏远程视频占位符
                document.getElementById('remoteVideoPlaceholder').classList.add('hidden');

                Logger.info('远程视频显示成功');
            }

        } catch (error) {
            Logger.error('显示远程视频失败:', error);
        }
    }

    showNotification(message) {
        // 简单 Toast 提示
        const toast = document.createElement('div');
        toast.className = 'toast-notification';
        toast.textContent = message;
        toast.style.cssText = `
        position: fixed; bottom: 80px; left: 50%; transform: translateX(-50%);
        background: rgba(0,0,0,0.8); color: white; padding: 10px 20px;
        border-radius: 8px; z-index: 9999; font-size: 14px;
        animation: fadeInUp 0.3s ease;
    `;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    }

    // 拒绝通话
    async rejectCall() {
        try {
            Logger.info('拒绝视频通话...');

            this.stopRingtone();
            this.playHangupTone();

            await this.signalingHandler.callRejected({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId
            });
            this.endCall('视频通话已拒绝');

        } catch (error) {
            Logger.error('拒绝视频通话失败:', error);
        }
    }

    // 取消通话
    async cancelCall() {
        try {
            Logger.info('取消视频通话...');

            this.stopCallTone();
            this.playHangupTone();

            await this.signalingHandler.callCancelled({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                reason: '用户主动取消'
            });
            this.endCall('视频通话已取消');

        } catch (error) {
            Logger.error('取消视频通话失败:', error);
        }
    }

    // 挂断通话
    async hangupCall() {
        try {
            Logger.info('挂断视频通话...');

            this.playHangupTone();

            await this.signalingHandler.callEnded({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                reason: '用户主动挂断'
            });
            this.endCall('视频通话已挂断');

        } catch (error) {
            Logger.error('挂断视频通话失败:', error);
        }
    }

    // 结束通话
    endCall(message) {
        Logger.info(`结束通话: ${message}`);

        // 停止所有音效
        this.stopAllSounds(); // 内部调用了 stopRingtone 和 stopCallTone

        // 停止所有超时计时器
        this.stopAllTimeouts();

        // 停止通话计时器
        this.stopCallTimer();

        // 隐藏所有操作按钮（来电/去电/控制）
        const incomingActions = document.getElementById('incomingCallActions');
        if (incomingActions) incomingActions.classList.add('hidden');

        const outgoingActions = document.getElementById('outgoingCallActions');
        if (outgoingActions) outgoingActions.classList.add('hidden');

        const controls = document.getElementById('controls');
        if (controls) controls.classList.remove('active');

        // 重置头部和计时器样式
        const header = document.getElementById('header');
        if (header) header.classList.remove('call-active');

        const timerEl = document.getElementById('timer');
        if (timerEl) timerEl.classList.remove('active');

        // 标记通话非活跃
        this.isCallActive = false;

        // 清理资源（媒体流 + mediasoup）
        this.cleanup();

        // 显示结束信息
        this.showCallEnded(message);

        // 延迟关闭窗口
        setTimeout(() => {
            this.signalingHandler.closeWindow();
        }, 2000);
    }

    // 清理所有资源（媒体流 + mediasoup）
    cleanup() {
        // 停止所有媒体流
        if (this.deviceManager) {
            this.deviceManager.stopAllStreams();
        }

        // 关闭 mediasoup 生产者/消费者和传输
        if (this.mediasoupClient) {
            // 关闭生产者
            if (this.audioProducer) {
                this.mediasoupClient.closeProducer(this.audioProducer.id);
                this.audioProducer = null;
            }
            if (this.videoProducer) {
                this.mediasoupClient.closeProducer(this.videoProducer.id);
                this.videoProducer = null;
            }
            // 关闭消费者
            if (this.audioConsumer) {
                this.mediasoupClient.closeConsumer(this.audioConsumer.id);
                this.audioConsumer = null;
            }
            if (this.videoConsumer) {
                this.mediasoupClient.closeConsumer(this.videoConsumer.id);
                this.videoConsumer = null;
            }
            // 关闭传输
            this.mediasoupClient.closeAll();
        }


        // 释放本地流引用
        this.audioStream = null;
        this.videoStream = null;
        this.localStream = null;

        Logger.info('资源清理完成');
    }

    showCallEnded(reason) {
        // 更新状态显示
        const statusEl = document.getElementById('remoteStatus');
        if (statusEl) {
            statusEl.textContent = reason;
        }
        // 隐藏计时器显示
        const timerEl = document.getElementById('timer');
        if (timerEl) {
            timerEl.classList.add('hidden');
        }
        // 隐藏控制栏
        const controls = document.getElementById('controls');
        if (controls) {
            controls.classList.remove('active');
        }
    }

    // 静音/取消静音
    async toggleMute() {
        try {
            if (this.audioProducer) {
                if (this.isMuted) {
                    await this.audioProducer.resume();
                    document.getElementById('muteBtn').classList.remove('active');
                    document.getElementById('muteBtn').querySelector('i').className = 'fas fa-microphone';
                    document.getElementById('muteBtn').querySelector('.tooltip').textContent = '静音';
                    this.isMuted = false;
                    Logger.info('取消静音');
                } else {
                    await this.audioProducer.pause();
                    document.getElementById('muteBtn').classList.add('active');
                    document.getElementById('muteBtn').querySelector('i').className = 'fas fa-microphone-slash';
                    document.getElementById('muteBtn').querySelector('.tooltip').textContent = '取消静音';
                    this.isMuted = true;
                    Logger.info('已静音');
                }
            }
        } catch (error) {
            Logger.error('切换静音状态失败:', error);
        }
    }

    // 开启/关闭视频
    async toggleVideo() {
        try {
            if (this.videoProducer) {
                if (this.isVideoOff) {
                    await this.videoProducer.resume();
                    document.getElementById('videoBtn').classList.remove('active');
                    document.getElementById('videoBtn').querySelector('i').className = 'fas fa-video';
                    document.getElementById('videoBtn').querySelector('.tooltip').textContent = '关闭视频';
                    this.isVideoOff = false;
                    Logger.info('开启视频');
                } else {
                    await this.videoProducer.pause();
                    document.getElementById('videoBtn').classList.add('active');
                    document.getElementById('videoBtn').querySelector('i').className = 'fas fa-video-slash';
                    document.getElementById('videoBtn').querySelector('.tooltip').textContent = '开启视频';
                    this.isVideoOff = true;
                    Logger.info('关闭视频');
                }
            }
        } catch (error) {
            Logger.error('切换视频状态失败:', error);
        }
    }

    // 切换摄像头
    async switchCamera() {
        try {
            Logger.info('切换摄像头...');

            // 获取所有视频设备
            const videoDevices = await this.deviceManager.getVideoDevices();
            if (videoDevices.length < 2) {
                this.showNotification('未找到其他摄像头');
                return;
            }

            // 获取当前摄像头
            const currentDevice = document.getElementById('videoDeviceSelect').value;

            // 选择下一个摄像头
            const currentIndex = videoDevices.findIndex(device => device.deviceId === currentDevice);
            const nextIndex = (currentIndex + 1) % videoDevices.length;
            const nextDevice = videoDevices[nextIndex];

            // 更新设备选择
            document.getElementById('videoDeviceSelect').value = nextDevice.deviceId;

            // 重新获取视频流
            if (this.videoStream) {
                this.videoStream.getTracks().forEach(track => track.stop());
            }

            const videoResult = await this.deviceManager.getVideoStream({ deviceId: nextDevice.deviceId });
            this.videoStream = videoResult.stream;

            // 更新本地视频流
            if (this.localVideo) {
                this.localVideo.srcObject = this.videoStream;
            }

            // 替换视频生产者
            if (this.videoProducer) {
                await this.videoProducer.replaceTrack({ track: this.videoStream.getVideoTracks()[0] });
            }

            this.showNotification(`已切换到 ${nextDevice.label}`);

        } catch (error) {
            Logger.error('切换摄像头失败:', error);
            this.showError('切换摄像头失败: ' + error.message);
        }
    }

    // 填充设备列表
    populateDeviceLists() {
        try {
            const videoDevices = this.deviceManager.getVideoDevices();
            const audioDevices = this.deviceManager.getAudioDevices();

            const videoSelect = document.getElementById('videoDeviceSelect');
            const audioSelect = document.getElementById('audioDeviceSelect');

            // 填充视频设备
            videoSelect.innerHTML = '<option value="">选择摄像头</option>';
            videoDevices.forEach(device => {
                const option = document.createElement('option');
                option.value = device.deviceId;
                option.textContent = device.label;
                videoSelect.appendChild(option);
            });

            // 填充音频设备
            audioSelect.innerHTML = '<option value="">选择麦克风</option>';
            audioDevices.forEach(device => {
                const option = document.createElement('option');
                option.value = device.deviceId;
                option.textContent = device.label;
                audioSelect.appendChild(option);
            });

            // 设置默认设备
            if (videoDevices.length > 0) {
                videoSelect.value = videoDevices[0].deviceId;
            }
            if (audioDevices.length > 0) {
                audioSelect.value = audioDevices[0].deviceId;
            }

        } catch (error) {
            Logger.error('填充设备列表失败:', error);
        }
    }

    // 设置UI事件
    setupUIEvents() {
        // 挂断按钮
        document.getElementById('hangupBtn').addEventListener('click', () => this.hangupCall());

        // 静音按钮
        document.getElementById('muteBtn').addEventListener('click', () => this.toggleMute());

        // 视频按钮
        document.getElementById('videoBtn').addEventListener('click', () => this.toggleVideo());

        // 设备按钮
        document.getElementById('deviceBtn').addEventListener('click', () => this.toggleDeviceSelector());

        // 切换摄像头按钮
        document.getElementById('switchCameraBtn').addEventListener('click', () => this.switchCamera());

        // 关闭设备选择器
        document.getElementById('closeDevices').addEventListener('click', () => this.toggleDeviceSelector());

        // 设备选择变化
        document.getElementById('videoDeviceSelect').addEventListener('change', (e) => this.onVideoDeviceChange(e.target.value));
        document.getElementById('audioDeviceSelect').addEventListener('change', (e) => this.onAudioDeviceChange(e.target.value));
    }

    // 切换设备选择器
    toggleDeviceSelector() {
        const deviceSelector = document.getElementById('deviceSelector');
        deviceSelector.classList.toggle('active');
    }

    // 视频设备变化
    async onVideoDeviceChange(deviceId) {
        try {
            if (this.videoStream) {
                this.videoStream.getTracks().forEach(track => track.stop());
            }

            const videoResult = await this.deviceManager.getVideoStream({ deviceId });
            this.videoStream = videoResult.stream;

            if (this.localVideo) {
                this.localVideo.srcObject = this.videoStream;
            }

            if (this.videoProducer) {
                await this.videoProducer.replaceTrack({ track: this.videoStream.getVideoTracks()[0] });
            }

            Logger.info('视频设备已切换');

        } catch (error) {
            Logger.error('切换视频设备失败:', error);
        }
    }

    // 音频设备变化
    async onAudioDeviceChange(deviceId) {
        try {
            if (this.audioStream) {
                this.audioStream.getTracks().forEach(track => track.stop());
            }

            const audioResult = await this.deviceManager.getAudioStream({ deviceId });
            this.audioStream = audioResult.stream;

            if (this.audioProducer) {
                await this.audioProducer.replaceTrack({ track: this.audioStream.getAudioTracks()[0] });
            }

            Logger.info('音频设备已切换');

        } catch (error) {
            Logger.error('切换音频设备失败:', error);
        }
    }

    // 开始通话计时器
    startCallTimer() {
        this.callStartTime = new Date();
        this.callTimer = setInterval(() => {
            const now = new Date();
            const duration = Math.floor((now - this.callStartTime) / 1000);
            const minutes = Math.floor(duration / 60).toString().padStart(2, '0');
            const seconds = (duration % 60).toString().padStart(2, '0');
            const callTimerElement = document.getElementById('timer');
            if (callTimerElement) {
                callTimerElement.textContent = `${minutes}:${seconds}`;
            }
        }, 1000);
    }

    // 停止通话计时器
    stopCallTimer() {
        if (this.callTimer) {
            clearInterval(this.callTimer);
            this.callTimer = null;
        }
    }

    // 启动接听超时计时器（接收方）
    startAnswerTimeout() {
        this.stopAllTimeouts(); // 先停止所有超时计时器

        Logger.info(`启动接听超时计时器: ${this.answerTimeoutDuration}ms`);

        this.answerTimeoutTimer = setTimeout(async () => {
            Logger.warn('接听超时，自动结束通话');

            // 停止振铃音
            this.stopRingtone();

            // 发送接听超时信令
            await this.signalingHandler.callTimeout({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                timeoutType: 'ANSWER_TIMEOUT',
                reason: '接听超时（30秒）'
            });

            this.endCall('接听超时');

        }, this.answerTimeoutDuration);
    }

    // 启动呼叫超时计时器（发起方）
    startCallTimeout() {
        this.stopAllTimeouts(); // 先停止所有超时计时器

        Logger.info(`启动呼叫超时计时器: ${this.callTimeoutDuration}ms`);

        this.callTimeoutTimer = setTimeout(async () => {
            Logger.warn('呼叫超时，自动结束通话');

            // 停止呼叫音
            this.stopCallTone();

            // 发送呼叫超时信令
            await this.signalingHandler.callTimeout({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                timeoutType: 'CALL_TIMEOUT',
                reason: '呼叫超时（35秒）'
            });

            this.endCall('呼叫超时，对方未接听');

        }, this.callTimeoutDuration);
    }

    // 停止所有超时
    stopAllTimeouts() {
        if (this.answerTimeoutTimer) {
            clearTimeout(this.answerTimeoutTimer);
            this.answerTimeoutTimer = null;
            Logger.info('停止接听超时计时器');
        }
        if (this.callTimeoutTimer) {
            clearTimeout(this.callTimeoutTimer);
            this.callTimeoutTimer = null;
            Logger.info('停止呼叫超时计时器');
        }
    }

    // 停止所有音效
    stopAllSounds() {
        this.stopRingtone();
        this.stopCallTone();
    }

    // 获取通话时长
    getCallDuration() {
        if (!this.callStartTime) return 0;
        const endTime = new Date();
        return Math.floor((endTime - this.callStartTime) / 1000);
    }

    // 显示错误信息
    showError(message) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.textContent = message;
        document.body.appendChild(errorDiv);

        setTimeout(() => {
            errorDiv.remove();
        }, 5000);
    }


    // 处理对方接受通话
    handleCallAccepted() {
        Logger.info('对方已接受视频通话');

        // 停止呼叫音和超时计时器
        this.stopCallTone();
        this.stopAllTimeouts();

        this.isCallActive = true;
        this.startCallTimer();

        // 更新状态
        document.getElementById('remoteStatus').textContent = '通话中';
        document.getElementById('callTimer')?.classList.add('active');

        // 隐藏去电等待界面，显示控制按钮
        document.getElementById('outgoingCallActions').classList.add('hidden');
        document.getElementById('controls').classList.add('active');

        // 消费远程媒体
        this.consumeRemoteMedia(this.roomId || `room_${this.initData.sessionId}`);
    }

    // 处理对方拒绝通话
    handleCallRejected(reason) {
        Logger.info(`通话被拒绝: ${reason}`);
        this.stopCallTone();
        this.playHangupTone();
        this.endCall(`${reason}`);
    }

    // 处理对方挂断
    handleCallHangup() {
        Logger.info('对方已挂断通话');
        this.stopCallTone();
        this.playHangupTone();
        this.endCall('对方已挂断');
    }

    // 处理对方取消
    handleCallCancel() {
        Logger.info('对方已取消通话');
        this.stopCallTone();
        this.playHangupTone();
        this.endCall('对方已取消');
    }

    // 处理超时
    handleCallTimeout() {
        Logger.warn('通话超时');
        this.stopCallTone();
        this.endCall('通话超时');
    }

}


// 启动应用
document.addEventListener('DOMContentLoaded', () => {
    window.videoCallApp = new VideoCallApp();
});

// 全局函数供 Java 端调用
window.handleCallAccepted = function () {
    if (window.videoCallApp) {
        window.videoCallApp.handleCallAccepted();
    }
};

window.handleCallRejected = function (reason) {
    if (window.videoCallApp) {
        window.videoCallApp.handleCallRejected(reason);
    }
};

window.handleCallHangup = function () {
    if (window.videoCallApp) {
        window.videoCallApp.handleCallHangup();
    }
};

window.handleCallCancel = function () {
    if (window.videoCallApp) {
        window.videoCallApp.handleCallCancel();
    }
};

window.handleCallTimeout = function () {
    if (window.videoCallApp) {
        window.videoCallApp.handleCallTimeout();
    }
};