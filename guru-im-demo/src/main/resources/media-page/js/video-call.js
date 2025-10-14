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

            // 初始化设备管理器
            await this.deviceManager.initialize();

            // 检查音视频设备
            if (!this.deviceManager.hasAudioDevice()) {
                throw new Error('未找到可用的麦克风设备');
            }

            if (!this.deviceManager.hasVideoDevice()) {
                throw new Error('未找到可用的摄像头设备');
            }

            Logger.info('音视频设备初始化完成');

        } catch (error) {
            Logger.error('设备初始化失败:', error);
            await this.signalingHandler.devicesFailed({
                sessionId: this.initData.sessionId,
                error: error.message
            });
            throw error;
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
            document.getElementById('callStatus').textContent = status;

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

            // 发起呼叫
            await this.startCall();

            // 获取音视频流并显示本地视频
            await this.getMediaStreams();
            await this.showLocalVideo();

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
            // 获取音频设备
            const audioDevice = document.getElementById('audioDeviceSelect').value;
            const audioConstraints = audioDevice ? { deviceId: audioDevice } : {};

            // 获取视频设备
            const videoDevice = document.getElementById('videoDeviceSelect').value;
            const videoConstraints = videoDevice ? { deviceId: videoDevice } : {};

            // 获取音频流
            const audioResult = await this.deviceManager.getAudioStream(audioConstraints);
            this.audioStream = audioResult.stream;

            // 获取视频流
            const videoResult = await this.deviceManager.getVideoStream(videoConstraints);
            this.videoStream = videoResult.stream;

            // 合并流用于本地显示
            this.localStream = new MediaStream([
                ...this.audioStream.getAudioTracks(),
                ...this.videoStream.getVideoTracks()
            ]);

            Logger.info('音视频流获取成功');

        } catch (error) {
            Logger.error('获取音视频流失败:', error);
            throw error;
        }
    }

    // 显示本地视频
    async showLocalVideo() {
        try {
            if (this.localVideo && this.localStream) {
                this.localVideo.srcObject = this.localStream;

                // 隐藏本地视频占位符
                document.getElementById('localVideoPlaceholder').classList.add('hidden');

                Logger.info('本地视频显示成功');
            }
        } catch (error) {
            Logger.error('显示本地视频失败:', error);
            throw error;
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

            // 生产音视频
            this.audioProducer = await this.mediasoupClient.produceAudio(this.audioStream);
            this.videoProducer = await this.mediasoupClient.produceVideo(this.videoStream);

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
            document.getElementById('callStatus').textContent = '通话中';

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
            const remoteAudioStream = new MediaStream([track]);

            // 使用远程视频元素播放音频
            if (this.remoteVideo) {
                this.remoteVideo.srcObject = remoteAudioStream;
            }

            Logger.info('远程音频播放开始');

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
                targetUserId: this.initData.targetUserId
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
                targetUserId: this.initData.targetUserId
            });
            this.endCall('视频通话已结束');

        } catch (error) {
            Logger.error('挂断视频通话失败:', error);
        }
    }

    // 结束通话
    endCall(message) {
        this.stopAllTimeouts();
        this.stopCallTimer();
        this.stopAllSounds();
        this.cleanupMediaStreams();

        if (this.isCallActive) {
            this.showNotification(message);
        }

        // 通知Java端通话结束
        if (window.JCefClient) {
            window.JCefClient.callEnded(JSON.stringify({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                duration: this.getCallDuration()
            }));
        }

        // 关闭窗口
        setTimeout(() => {
            if (window.JCefClient) {
                window.JCefClient.closeWindow();
            }
        }, 2000);
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

    // 开始接听超时
    startAnswerTimeout() {
        setTimeout(() => {
            if (!this.isCallActive) {
                Logger.warn('接听超时，自动拒绝视频通话');
                this.rejectCall();
            }
        }, this.answerTimeoutDuration);
    }

    // 开始呼叫超时
    startCallTimeout() {
        setTimeout(() => {
            if (!this.isCallActive) {
                Logger.warn('呼叫超时，自动取消视频通话');
                this.cancelCall();
            }
        }, this.callTimeoutDuration);
    }

    // 停止所有超时
    stopAllTimeouts() {
        // 这里可以添加具体的超时清理逻辑
    }

    // 停止所有音效
    stopAllSounds() {
        this.stopRingtone();
        this.stopCallTone();
    }

    // 清理媒体流
    cleanupMediaStreams() {
        if (this.audioStream) {
            this.audioStream.getTracks().forEach(track => track.stop());
        }
        if (this.videoStream) {
            this.videoStream.getTracks().forEach(track => track.stop());
        }
        if (this.localStream) {
            this.localStream.getTracks().forEach(track => track.stop());
        }
    }

    // 获取通话时长
    getCallDuration() {
        if (!this.callStartTime) return 0;
        const endTime = new Date();
        return Math.floor((endTime - this.callStartTime) / 1000);
    }

    // 显示通知
    showNotification(message) {
        // 实现通知显示逻辑
        Logger.info('通知:', message);
    }

    // 显示错误
    showError(message) {
        // 实现错误显示逻辑
        Logger.error('错误:', message);
    }
}


// 启动应用
document.addEventListener('DOMContentLoaded', () => {
    window.videoCallApp = new VideoCallApp();
});