class ScreenShareApp {
    constructor() {
        this.initData = null;
        this.deviceManager = null;
        this.signalingHandler = null;
        this.mediasoupClient = null;

        this.audioStream = null;
        this.screenStream = null;
        this.audioProducer = null;
        this.screenProducer = null;
        this.audioConsumer = null;
        this.audioAnalyser = null;
        this.audioContext = null;
        this.remoteAudio = null;

        this.callStartTime = null;
        this.callTimer = null;

        this.isMuted = false;
        this.isSpeakerMuted = false;
        this.volumeLevel = 1.0; // 默认扬声器音量 100%
        this.volumeControlVisible = false;


        this.isIncomingCall = false;
        this.isCallActive = false;
        this.isScreenSharing = false;
        this.isScreenPaused = false;

        this.isInitialized = false;

        // 音效控制
        this.ringtone = null;
        this.callTone = null;
        this.hangupTone = null;
        this.isRinging = false;
        this.isCalling = false;

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

    // 播放振铃音（接收方）
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

    // 播放呼叫音（发起方）
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
            Logger.info('初始化基础组件...');

            await this.monitorPermissionState();

            await this.checkJCEFPermission();

            this.deviceManager = new DeviceManager();
            this.signalingHandler = new SignalingHandler();
            this.mediasoupClient = new MediasoupClient(this.signalingHandler);

            this.initializeSounds();
            this.setupUIEvents();
            // 加载音量设置
            this.loadVolumeSettings();

            this.showWaitingForInitData();

            Logger.info('基础组件初始化完成，等待初始化数据...');

            await this.requestCallInitData();

        } catch (error) {
            Logger.error('基础组件初始化失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // 浏览器权限测试
    async checkJCEFPermission() {
        try {
            // 检测媒体支持
            Logger.info('JCEF 环境检测:', {
                userAgent: navigator.userAgent,
                mediaDevices: !!navigator.mediaDevices,
                getDisplayMedia: !!navigator.mediaDevices?.getDisplayMedia,
                getUserMedia: !!navigator.mediaDevices?.getUserMedia
            });

            // 检查权限API
            if (navigator.permissions) {
                try {
                    const displayPermission = await navigator.permissions.query({ name: 'display-capture' });
                    Logger.info('显示捕获权限状态:', displayPermission.state);
                } catch (e) {
                    Logger.warn('显示捕获权限API不支持:', e.message);
                }
            }

            // 测试媒体设备枚举
            try {
                const devices = await navigator.mediaDevices.enumerateDevices();
                Logger.info('可用媒体设备:', devices.length);
            } catch (e) {
                Logger.warn('设备枚举失败:', e.message);
            }

        } catch (error) {
            Logger.error('环境检测失败:', error);
        }

    }

    // 监控权限状态变化
    async monitorPermissionState() {
        if (navigator.permissions) {
            try {
                const permissionStatus = await navigator.permissions.query({ name: 'display-capture' });

                permissionStatus.onchange = () => {
                    Logger.info('屏幕捕获权限状态变更:', permissionStatus.state);

                    if (permissionStatus.state === 'granted') {
                        Logger.info('屏幕共享权限已授予');
                    } else if (permissionStatus.state === 'denied') {
                        Logger.warn('屏幕共享权限被拒绝');
                    }
                };

            } catch (e) {
                Logger.warn('权限状态监控不支持:', e.message);
            }
        }
    }


    // 请求初始化数据
    async requestCallInitData() {
        try {
            Logger.info('主动请求桌面共享初始化数据...');
            const response = await this.signalingHandler.sendRequest('requestCallInitData');

            if (response) {
                const initData = JSON.parse(response);
                Logger.info('收到初始化数据:', initData);
                await this.handleCallInit(initData);
            } else {
                throw new Error('未收到初始化数据');
            }
        } catch (error) {
            Logger.error('请求初始化数据失败:', error);
            this.showError('获取共享数据失败: ' + error.message);
        }
    }

    // 显示等待初始化数据的界面
    showWaitingForInitData() {
        const loadingOverlay = document.getElementById('loadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.classList.add('hidden');
        }

        const waitingHtml = `
            <div id="waitingInit" style="
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                z-index: 1000;
                color: white;
                text-align: center;
            ">
                <div class="loading-spinner"></div>
                <div style="margin-top: 20px; font-size: 1.1rem;">等待桌面共享初始化数据...</div>
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

    // 切换设备选择器
    toggleDeviceSelector() {
        const deviceSelector = document.getElementById('deviceSelector');
        if (deviceSelector) {
            if (deviceSelector.classList.contains('active')) {
                this.hideDeviceSelector();
            } else {
                this.showDeviceSelector();
            }
        }
    }

    // 显示设备选择器
    showDeviceSelector() {
        try {
            Logger.info('打开设备设置');
            const deviceSelector = document.getElementById('deviceSelector');
            if (deviceSelector) {
                deviceSelector.classList.add('active');

                if (this.deviceManager && this.deviceManager.isInitialized) {
                    this.populateAudioDevices();
                }
            }
        } catch (error) {
            Logger.error('打开设备设置失败:', error);
        }
    }

    // 隐藏设备选择器
    hideDeviceSelector() {
        try {
            Logger.info('关闭设备设置');
            const deviceSelector = document.getElementById('deviceSelector');
            if (deviceSelector) {
                deviceSelector.classList.remove('active');
            }
        } catch (error) {
            Logger.error('关闭设备设置失败:', error);
        }
    }

    // 初始化本地设备信息
    async initializeDevices() {
        try {
            await this.deviceManager.initialize();

            if (!this.deviceManager.hasAudioDevice()) {
                throw new Error('未找到可用的麦克风设备');
            }

            Logger.info('音频设备初始化完成');
        } catch (error) {
            Logger.error('设备初始化失败:', error);
            await this.signalingHandler.devicesFailed({
                sessionId: this.initData.sessionId,
                error: error.message
            });
            throw error;
        }
    }

    // 发起桌面共享
    async startScreenShare() {
        const callResponseStr = await this.signalingHandler.devicesReady({
            sessionId: this.initData.sessionId,
            targetUserId: this.initData.targetUserId,
            deviceId: this.initData.deviceInfo.deviceId
        });
        Logger.info('收到桌面共享请求的响应:', callResponseStr);

        const callResponse = JSON.parse(callResponseStr);

        if (callResponse && callResponse.callRequestResponse) {
            if (callResponse.callRequestResponse.success) {
                this.sessionId = callResponse.callRequestResponse.sessionId;
                this.roomId = callResponse.callRequestResponse.roomId;
                Logger.info(`更新会话信息: sessionId=${this.sessionId}, roomId=${this.roomId}`);
            } else {
                throw new Error('桌面共享请求失败: ' + (callResponse.callRequestResponse.errorMessage || '未知错误'));
            }
        } else {
            throw new Error('桌面共享请求失败: 未知错误');
        }
    }

    // 处理桌面共享初始化数据
    async handleCallInit(initData) {
        try {
            Logger.info('收到桌面共享初始化数据，开始初始化设备...');
            this.initData = initData;
            this.isIncomingCall = initData.isIncoming;

            this.hideWaitingForInitData();

            await this.initializeDevices();

            this.updateCallInfo();

            this.isInitialized = true;

            if (this.isIncomingCall) {
                await this.startIncomingCall();
            } else {
                await this.startOutgoingCall();
            }

        } catch (error) {
            Logger.error('处理桌面共享初始化数据失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // 更新UI信息
    updateCallInfo() {
        try {
            this.populateParticipants();
            this.populateAudioDevices();

            // 更新占位符文本
            const placeholderText = document.getElementById('screenPlaceholderText');
            if (placeholderText) {
                if (this.isIncomingCall) {
                    placeholderText.textContent = '等待对方共享屏幕...';
                } else {
                    placeholderText.textContent = '点击开始共享屏幕';
                }
            }

        } catch (error) {
            Logger.error('updateCallInfo error:', error);
        }
    }

    // 填充参与者列表
    populateParticipants() {
        try {
            const participantsList = document.getElementById('participantsList');
            if (!participantsList) return;

            participantsList.innerHTML = '';

            // 添加当前用户
            const currentUserItem = document.createElement('div');
            currentUserItem.className = 'participant-item';
            currentUserItem.innerHTML = `
                <div class="participant-avatar">${Utils.createAvatar(this.initData.currentUserName || '用户')}</div>
                <div class="participant-info">
                    <div class="participant-name">${this.initData.currentUserName || '用户'} (您)</div>
                    <div class="participant-status">${this.isIncomingCall ? '观看中' : '共享中'}</div>
                </div>
            `;
            participantsList.appendChild(currentUserItem);

            // 添加目标用户
            const targetUserItem = document.createElement('div');
            targetUserItem.className = 'participant-item';
            targetUserItem.innerHTML = `
                <div class="participant-avatar">${Utils.createAvatar(this.initData.targetUserName || '对方')}</div>
                <div class="participant-info">
                    <div class="participant-name">${this.initData.targetUserName || '对方'}</div>
                    <div class="participant-status">${this.isIncomingCall ? '共享中' : '观看中'}</div>
                </div>
            `;
            participantsList.appendChild(targetUserItem);

        } catch (error) {
            Logger.error('填充参与者列表失败:', error);
        }
    }

    // 启动接听通话
    async startIncomingCall() {
        try {
            Logger.info('开始处理来电...');

            // await this.getAudioStream();

            this.playRingtone();

            this.startAnswerTimeout();

            // 显示接听按钮
            this.showIncomingCallUI();

        } catch (error) {
            Logger.error('处理来电失败:', error);
            this.showError('处理来电失败: ' + error.message);
        }
    }

    // 启动呼叫通话
    async startOutgoingCall() {
        try {
            Logger.info('开始发起桌面共享...');

            await this.startScreenShare();

            await this.getAudioStream();

            await this.initializeMediasoup();

            this.playCallTone();

            this.startCallTimeout();

            // 显示发起方按钮
            this.showOutgoingCallUI();

        } catch (error) {
            Logger.error('发起桌面共享失败:', error);
            this.showError('发起桌面共享失败: ' + error.message);
        }
    }

    // 显示发起方界面
    showOutgoingCallUI() {
        const shareScreenBtn = document.getElementById('shareScreenBtn');
        const hangupBtn = document.getElementById('hangupBtn');
        const incomingCallActions = document.getElementById('incomingCallActions');

        if (shareScreenBtn) {
            // shareScreenBtn.style.display = 'flex';
        }
        if (hangupBtn) {
            hangupBtn.style.display = 'flex';
        }
        if (incomingCallActions) {
            incomingCallActions.style.display = 'none';
        }

        // 更新占位符文本
        const placeholderText = document.getElementById('screenPlaceholderText');
        if (placeholderText) {
            placeholderText.textContent = '点击开始共享屏幕';
        }
    }

    // 显示接听通话页面
    showIncomingCallUI() {
        const incomingCallActions = document.getElementById('incomingCallActions');
        const hangupBtn = document.getElementById('hangupBtn');

        if (incomingCallActions) {
            incomingCallActions.style.display = 'flex';
        }
        if (hangupBtn) {
            hangupBtn.style.display = 'none';
        }

        // 更新占位符文本
        const placeholderText = document.getElementById('screenPlaceholderText');
        if (placeholderText) {
            placeholderText.textContent = '收到桌面共享邀请...';
        }
    }

    // 在通话接听后显示通话中界面
    showCallActiveUI() {
        const shareScreenBtn = document.getElementById('shareScreenBtn');
        const hangupBtn = document.getElementById('hangupBtn');
        const incomingCallActions = document.getElementById('incomingCallActions');

        if (this.isIncomingCall) {
            // 接听方：显示共享控制按钮
            if (shareScreenBtn) {
                //shareScreenBtn.style.display = 'flex';
            }
        }

        if (hangupBtn) {
            hangupBtn.style.display = 'flex';
        }
        if (incomingCallActions) {
            incomingCallActions.style.display = 'none';
        }

        // 更新占位符文本
        const placeholderText = document.getElementById('screenPlaceholderText');
        if (placeholderText) {
            if (this.isIncomingCall) {
                placeholderText.textContent = '等待对方共享屏幕...';
            } else {
                placeholderText.textContent = '点击开始共享屏幕';
            }
        }
    }

    // 消费远程媒体流（音频和视频）
    async consumeRemoteMedia(roomId) {
        try {
            Logger.info('开始消费远程媒体流...');

            const producersRes = await this.signalingHandler.getRoomProducers(roomId);
            const roomProducers = JSON.parse(producersRes).roomProducersResponse;

            Logger.info('房间生产者列表:', roomProducers);
            if (roomProducers && roomProducers.producers) {
                for (const producer of roomProducers.producers) {
                    // 消费对方的音频（排除自己的音频生产者）
                    if (producer.kind === 'audio' && producer.producerId !== this.audioProducer?.id) {
                        Logger.info(`创建音频消费者，消费对方音频: ${producer.producerId}`);
                        await this.createAudioConsumer(roomId, producer.producerId);
                    }
                    // 消费对方的视频（屏幕共享）
                    else if (producer.kind === 'video' && producer.producerId !== this.screenProducer?.id) {
                        Logger.info(`创建视频消费者，消费对方屏幕共享: ${producer.producerId}`);
                        await this.createVideoConsumer(roomId, producer.producerId);
                    }
                }
            }

            Logger.info('远程媒体流消费完成');

        } catch (error) {
            Logger.error('消费远程媒体流失败:', error);
        }
    }

    // 创建视频消费者
    async createVideoConsumer(roomId, producerId) {
        try {
            Logger.info(`创建视频消费者，producerId: ${producerId}`);

            const consumer = await this.mediasoupClient.consume(producerId);

            // 播放远程视频（屏幕共享）
            await this.playRemoteVideo(consumer.track);

            this.videoConsumer = consumer;

            Logger.info('视频消费者创建成功');

        } catch (error) {
            Logger.error('创建视频消费者失败:', error);
        }
    }

    // 播放远程视频（屏幕共享）
    async playRemoteVideo(track) {
        try {
            // 创建远程视频流
            const remoteStream = new MediaStream([track]);

            // 创建或获取视频元素来播放远程屏幕共享
            let remoteVideo = document.getElementById('remoteScreenVideo');
            if (!remoteVideo) {
                remoteVideo = document.createElement('video');
                remoteVideo.id = 'remoteScreenVideo';
                remoteVideo.style.cssText = `
                width: 100%;
                height: 100%;
                object-fit: contain;
                background: #000;
            `;
                remoteVideo.autoplay = true;
                remoteVideo.controls = true;

                // 添加到共享屏幕区域
                const sharedScreen = document.getElementById('sharedScreen');
                if (sharedScreen) {
                    // 隐藏占位符
                    const placeholder = document.getElementById('screenPlaceholder');
                    if (placeholder) {
                        placeholder.style.display = 'none';
                    }
                    sharedScreen.appendChild(remoteVideo);
                }
            }

            remoteVideo.srcObject = remoteStream;
            await remoteVideo.play();

            Logger.info('远程屏幕共享视频播放开始');

        } catch (error) {
            Logger.error('播放远程屏幕共享视频失败:', error);
        }
    }

    // 获取本地音频流
    async getAudioStream() {
        try {
            const audioDeviceSelect = document.getElementById('audioDeviceSelect');
            const selectedDevice = audioDeviceSelect ? audioDeviceSelect.value : null;
            const constraints = selectedDevice ? { deviceId: selectedDevice } : {};

            const result = await this.deviceManager.getAudioStream(constraints);
            this.audioStream = result.stream;

            this.setupAudioAnalyser();

            Logger.info('音频流获取成功');

        } catch (error) {
            Logger.error('获取音频流失败:', error);
            throw error;
        }
    }

    // 获取屏幕共享流
    async getScreenStream() {
        try {
            Logger.info('开始获取屏幕共享流...');

            this.screenStream = await this.deviceManager.getDisplayStream();

            // 显示屏幕共享视频
            const screenVideo = document.getElementById('screenVideo');
            const screenPlaceholder = document.getElementById('screenPlaceholder');
            if (screenVideo && screenPlaceholder) {
                screenVideo.srcObject = this.screenStream;
                screenVideo.style.display = 'block';
                screenPlaceholder.style.display = 'none';
            }

            // 更新控制按钮
            const shareBtn = document.getElementById('shareScreenBtn');
            const pauseBtn = document.getElementById('pauseShareBtn');
            const stopBtn = document.getElementById('stopShareBtn');

            if (shareBtn) shareBtn.style.display = 'none';
            if (pauseBtn) pauseBtn.style.display = 'flex';
            if (stopBtn) stopBtn.style.display = 'flex';
            Logger.info('屏幕共享流获取成功');

        } catch (error) {
            Logger.error('获取屏幕共享流失败:', error);
            throw error;
        }
    }

    // 暂停屏幕共享
    async pauseScreenShare() {
        try {
            if (this.screenStream && this.isScreenSharing && !this.isScreenPaused) {
                this.screenStream.getVideoTracks().forEach(track => {
                    track.enabled = false;
                });

                const screenVideo = document.getElementById('screenVideo');
                if (screenVideo) {
                    screenVideo.style.opacity = '0.5';
                }
                this.isScreenPaused = true;

                Logger.info('屏幕共享已暂停');
            }
        } catch (error) {
            Logger.error('暂停屏幕共享失败:', error);
        }
    }

    // 恢复屏幕共享
    async resumeScreenShare() {
        try {
            if (this.screenStream && this.isScreenSharing && this.isScreenPaused) {
                this.screenStream.getVideoTracks().forEach(track => {
                    track.enabled = true;
                });

                const screenVideo = document.getElementById('screenVideo');
                if (screenVideo) {
                    screenVideo.style.opacity = '1';
                }
                this.isScreenPaused = false;

                Logger.info('屏幕共享已恢复');
            }
        } catch (error) {
            Logger.error('恢复屏幕共享失败:', error);
        }
    }

    // 停止屏幕共享
    async stopScreenShare() {
        try {
            if (this.screenStream) {
                // 停止屏幕共享生产者
                if (this.screenProducer) {
                    await this.mediasoupClient.closeProducer(this.screenProducer.id);
                    this.screenProducer = null;
                }

                // 停止屏幕流
                this.deviceManager._stopStream(this.screenStream);
                this.screenStream = null;

                this.isScreenSharing = false;
                this.isScreenPaused = false;

                this.updateScreenShareUI();

                Logger.info('屏幕共享已停止');
            }
        } catch (error) {
            Logger.error('停止屏幕共享失败:', error);
        }
    }

    updateScreenShareUI() {
        const screenVideo = document.getElementById('screenVideo');
        const screenPlaceholder = document.getElementById('screenPlaceholder');
        const shareBtn = document.getElementById('shareScreenBtn');
        const pauseBtn = document.getElementById('pauseShareBtn');
        const stopBtn = document.getElementById('stopShareBtn');
        const placeholderText = document.getElementById('screenPlaceholderText');

        // 重置视频显示
        if (screenVideo) {
            screenVideo.srcObject = null;
            screenVideo.style.display = 'none';
        }

        if (screenPlaceholder) {
            screenPlaceholder.style.display = 'flex';
        }

        if (placeholderText) {
            placeholderText.textContent = '点击开始共享屏幕';
        }

        // 重置按钮状态
        if (shareBtn) {
            shareBtn.style.display = 'flex';
            shareBtn.disabled = false;
            shareBtn.classList.remove('disabled');
        }

        if (pauseBtn) {
            pauseBtn.style.display = 'none';
            pauseBtn.classList.remove('active');
        }

        if (stopBtn) {
            stopBtn.style.display = 'none';
        }

        Logger.info('屏幕共享UI已重置');
    }

    // 设置音频分析器
    setupAudioAnalyser() {
        try {
            if (!this.audioStream) {
                Logger.error('音频流不存在');
                return;
            }

            const audioTracks = this.audioStream.getAudioTracks();
            if (audioTracks.length === 0) {
                Logger.error('音频流中没有音频轨道');
                return;
            }

            Logger.info('音频轨道状态:', {
                enabled: audioTracks[0].enabled,
                readyState: audioTracks[0].readyState,
                kind: audioTracks[0].kind
            });

            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            this.audioAnalyser = this.audioContext.createAnalyser();

            const source = this.audioContext.createMediaStreamSource(this.audioStream);
            source.connect(this.audioAnalyser);

            this.audioAnalyser.fftSize = 256;
            this.audioAnalyser.smoothingTimeConstant = 0.8;

            Logger.info('音频分析器设置完成');

        } catch (error) {
            Logger.warn('音频分析器设置失败:', error);
        }
    }

    // 初始化 mediasoup
    async initializeMediasoup() {
        try {
            Logger.info('初始化mediasoup...');

            const roomId = this.roomId || `room_${this.initData.sessionId}`;

            let rtpCapabilitiesRes = '{}';
            if (this.isIncomingCall) {
                rtpCapabilitiesRes = await this.signalingHandler.joinRoom(this.initData.sessionId);
            } else {
                rtpCapabilitiesRes = await this.signalingHandler.getRouterRtpCapabilities(roomId);
            }

            const rtpCapabilities = JSON.parse(JSON.parse(rtpCapabilitiesRes).rtpCapabilitiesResponse.rtpCapabilities);

            await this.mediasoupClient.initializeDevice(rtpCapabilities.rtpCapabilities);

            if (!this.mediasoupClient.canProduce('audio')) {
                throw new Error('设备不支持音频生产，RTP能力不匹配');
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

            if (!this.isIncomingCall) {
                await this.getScreenStream();
                // 生产视频流
                this.isScreenSharing = true;
                // 生产屏幕共享流
                if (this.mediasoupClient.canProduce('video')) {
                    this.screenProducer = await this.mediasoupClient.produceVideo(this.screenStream);
                    Logger.info('屏幕共享生产者创建成功');
                }

            }

            Logger.info('mediasoup初始化完成');

        } catch (error) {
            Logger.error('mediasoup初始化失败:', error);
            throw error;
        }
    }

    // 消费远程音频
    async consumeRemoteAudio(roomId) {
        try {
            Logger.info('开始消费远程音频...');

            const producersRes = await this.signalingHandler.getRoomProducers(roomId);
            const roomProducers = JSON.parse(producersRes).roomProducersResponse;

            Logger.info('房间生产者列表:', roomProducers);
            if (roomProducers && roomProducers.producers) {
                for (const producer of roomProducers.producers) {
                    if (producer.kind === 'audio' && producer.producerId !== this.audioProducer.id) {
                        Logger.info(`创建消费者，消费对方对应的生产者: ${producer.producerId}`);
                        await this.createAudioConsumer(roomId, producer.producerId);
                    }
                }
            }

            Logger.info('远程音频消费完成');

        } catch (error) {
            Logger.error('消费远程音频失败:', error);
        }
    }

    // 创建音频消费者
    async createAudioConsumer(roomId, producerId) {
        try {
            Logger.info(`创建音频消费者，producerId: ${producerId}`);

            const consumer = await this.mediasoupClient.consume(producerId);

            await this.playRemoteAudio(consumer.track);

            this.audioConsumer = consumer;

            Logger.info('音频消费者创建成功');

        } catch (error) {
            Logger.error('创建音频消费者失败:', error);
        }
    }

    // 播放远程音频
    async playRemoteAudio(track) {
        try {
            const remoteStream = new MediaStream([track]);

            if (!this.remoteAudio) {
                this.remoteAudio = document.createElement('audio');
                this.remoteAudio.style.display = 'none';
                this.remoteAudio.volume = this.isSpeakerMuted ? 0 : 1.0; // 初始化音量
                document.body.appendChild(this.remoteAudio);
            }

            this.remoteAudio.srcObject = remoteStream;
            await this.remoteAudio.play();

            Logger.info('远程音频播放开始');

        } catch (error) {
            Logger.error('播放远程音频失败:', error);
        }
    }

    // 创建音量控制界面
    createVolumeControl() {
        const speakerBtn = document.getElementById('speakerBtn');
        if (!speakerBtn) return null;

        // 移除已存在的音量控制
        const existingControl = document.querySelector('.volume-control');
        if (existingControl) {
            existingControl.remove();
        }

        const volumeControl = document.createElement('div');
        volumeControl.className = 'volume-control';

        // 获取扬声器按钮的位置
        const rect = speakerBtn.getBoundingClientRect();

        volumeControl.style.cssText = `
        position: fixed;
        top: ${rect.top - 125}px;
        left: ${rect.left}px;
        width: 40px;
        background: rgba(0, 0, 0, 0.9);
        padding: 10px 5px;
        border-radius: 8px;
        backdrop-filter: blur(10px);
        border: 1px solid rgba(255, 255, 255, 0.3);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
        z-index: 1000;
        display: none;
    `;

        volumeControl.innerHTML = `
        <div style="height: 100px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
            <input type="range" id="volumeSlider" 
                   min="0" max="100" value="${this.volumeLevel * 100}"
                   style="
                       width: 100px;
                       height: 4px;
                       transform: rotate(-90deg);
                       margin: 0;
                   "
                   class="volume-slider" />
        </div>
    `;

        document.body.appendChild(volumeControl);

        // 音量滑块事件
        const volumeSlider = document.getElementById('volumeSlider');

        volumeSlider.addEventListener('input', (e) => {
            const volume = e.target.value / 100;
            this.setSpeakerVolume(volume);

            // 更新静音状态
            this.isSpeakerMuted = volume === 0;
            this.updateSpeakerButton();
        });

        return volumeControl;
    }

    // 显示/隐藏音量控制
    toggleVolumeControl() {
        let volumeControl = document.querySelector('.volume-control');

        if (!volumeControl) {
            volumeControl = this.createVolumeControl();
        }

        if (!volumeControl) return;

        // 每次都要重新计算位置
        const speakerBtn = document.getElementById('speakerBtn');
        if (speakerBtn) {
            const rect = speakerBtn.getBoundingClientRect();
            volumeControl.style.top = `${rect.top - 120}px`;
            volumeControl.style.left = `${rect.left}px`;
        }

        if (this.volumeControlVisible) {
            volumeControl.style.display = 'none';
            this.volumeControlVisible = false;
        } else {
            volumeControl.style.display = 'block';
            this.volumeControlVisible = true;

            // 点击页面其他地方关闭音量控制
            setTimeout(() => {
                const closeHandler = (e) => {
                    if (!volumeControl.contains(e.target) && e.target.id !== 'speakerBtn') {
                        volumeControl.style.display = 'none';
                        this.volumeControlVisible = false;
                        document.removeEventListener('click', closeHandler);
                    }
                };
                document.addEventListener('click', closeHandler);
            }, 100);
        }
    }

    // 从本地存储加载音量设置
    loadVolumeSettings() {
        try {
            const savedVolume = localStorage.getItem('speakerVolume');
            if (savedVolume) {
                this.volumeLevel = parseFloat(savedVolume);
                if (this.remoteAudio) {
                    this.remoteAudio.volume = this.volumeLevel;
                }
                this.isSpeakerMuted = this.volumeLevel === 0;
                this.updateSpeakerButton();
            }
        } catch (e) {
            // 忽略本地存储错误
        }
    }

    // 开启音频可视化
    startAudioVisualization() {
        const audioBars = document.getElementById('audioBars');
        if (!audioBars) return;

        audioBars.innerHTML = '';

        for (let i = 0; i < 32; i++) {
            const bar = document.createElement('div');
            bar.className = 'audio-bar';
            bar.style.height = '2px';
            audioBars.appendChild(bar);
        }

        this.visualizeAudio();
    }

    // 可视化音频
    visualizeAudio() {
        if (!this.audioAnalyser) return;

        const bufferLength = this.audioAnalyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);
        const bars = document.querySelectorAll('.audio-bar');

        const updateVisualization = () => {
            if (!this.isCallActive) {
                return;
            }

            this.audioAnalyser.getByteFrequencyData(dataArray);

            bars.forEach((bar, index) => {
                const barIndex = Math.floor((index / bars.length) * bufferLength);
                const value = dataArray[barIndex] || 0;
                const height = Math.max(2, (value / 255) * 40);
                bar.style.height = `${height}px`;

                const intensity = value / 255;
                if (intensity > 0.7) {
                    bar.style.background = '#ef4444';
                } else if (intensity > 0.4) {
                    bar.style.background = '#f59e0b';
                } else {
                    bar.style.background = '#10b981';
                }
            });

            requestAnimationFrame(updateVisualization);
        };

        updateVisualization();
    }

    // 接听通话
    async acceptCall() {
        try {
            Logger.info('接听桌面共享...');

            this.stopRingtone();

            this.stopAllTimeouts();

            await this.getAudioStream();

            await this.initializeMediasoup();

            this.startCallTimer();

            this.isCallActive = true;

            await this.signalingHandler.callAccepted({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId
            });

            this.roomId = `room_${this.initData.sessionId}`;

            // await this.consumeRemoteAudio(this.roomId || `room_${this.initData.sessionId}`);
            // 对方接受通话后，开始消费远程媒体
            await this.consumeRemoteMedia(this.roomId || `room_${this.initData.sessionId}`);

            this.startAudioVisualization();

            // 显示通话中界面
            this.showCallActiveUI();

            Logger.info('桌面共享已接听');

        } catch (error) {
            Logger.error('接听桌面共享失败:', error);
            this.showError('接听失败: ' + error.message);
        }
    }

    // 拒绝通话
    async rejectCall() {
        try {
            Logger.info('拒绝桌面共享...');

            this.stopRingtone();
            this.playHangupTone();

            await this.signalingHandler.callRejected({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId
            });
            this.endCall('桌面共享已拒绝');

        } catch (error) {
            Logger.error('拒绝桌面共享失败:', error);
        }
    }

    // 开启通话计时器显示
    startCallTimer() {
        this.callStartTime = Date.now();

        const timerElement = document.getElementById('timer');
        if (timerElement) {
            timerElement.classList.add('active');
        }

        this.callTimer = setInterval(() => {
            const elapsed = Math.floor((Date.now() - this.callStartTime) / 1000);
            const timerElement = document.getElementById('timer');

            if (timerElement) {
                timerElement.textContent = Utils.formatTime(elapsed);
            }
        }, 1000);
    }

    // 结束通话计时器显示
    stopCallTimer() {
        if (this.callTimer) {
            clearInterval(this.callTimer);
            this.callTimer = null;
        }

        const timerElement = document.getElementById('timer');
        if (timerElement) {
            timerElement.classList.remove('active');
        }
    }

    // 处理通话接受
    handleCallAccepted() {
        Logger.info('对方已接受桌面共享');

        this.stopCallTone();
        this.stopAllTimeouts();

        this.isCallActive = true;
        this.startCallTimer();

        // 只消费对方的音频流
        this.consumeRemoteAudio(this.roomId || `room_${this.initData.sessionId}`);

        this.startAudioVisualization();

        // 显示通话中界面
        this.showCallActiveUI();
    }

    // 处理对方拒绝通话
    handleCallRejected(reason) {
        Logger.info(`桌面共享被拒绝: ${reason}`);
        this.stopCallTone();
        this.playHangupTone();
        this.endCall(`${reason}`);
    }

    // 处理对方挂断通话
    handleCallHangup() {
        Logger.info('对方已挂断桌面共享');
        this.stopRingtone();
        this.stopCallTone();
        this.playHangupTone();
        this.endCall('对方已挂断');
    }

    // 处理对方取消通话
    handleCallCancel() {
        Logger.info('对方已取消桌面共享');
        this.stopRingtone();
        this.stopCallTone();
        this.playHangupTone();
        this.endCall('对方已取消');
    }

    // UI事件处理
    setupUIEvents() {
        // 静音按钮
        const muteBtn = document.getElementById('muteBtn');
        if (muteBtn) {
            muteBtn.onclick = () => this.toggleMute();
        }

        // 静音按钮
        document.getElementById('muteBtn').onclick = () => this.toggleMute();

        // 扬声器按钮 - 点击显示/隐藏音量控制
        const speakerBtn = document.getElementById('speakerBtn');
        if (speakerBtn) {
            speakerBtn.onclick = () => this.toggleVolumeControl();
        }

        // 设备按钮
        const deviceBtn = document.getElementById('deviceBtn');
        if (deviceBtn) {
            deviceBtn.onclick = () => this.toggleDeviceSelector();
        }

        // 挂断按钮
        const hangupBtn = document.getElementById('hangupBtn');
        if (hangupBtn) {
            hangupBtn.onclick = () => this.hangup();
        }

        // 接听按钮
        const acceptCallBtn = document.getElementById('acceptCallBtn');
        if (acceptCallBtn) {
            acceptCallBtn.onclick = () => this.acceptCall();
        }

        // 拒绝按钮
        const rejectCallBtn = document.getElementById('rejectCallBtn');
        if (rejectCallBtn) {
            rejectCallBtn.onclick = () => this.rejectCall();
        }

        // 屏幕共享控制按钮
        const shareScreenBtn = document.getElementById('shareScreenBtn');
        if (shareScreenBtn) {
            shareScreenBtn.onclick = () => this.getScreenStream();
            shareScreenBtn.classList.add("hidden"); // 暂时不用，默认发起呼叫就自动共享，无需手动共享
        }

        const pauseShareBtn = document.getElementById('pauseShareBtn');
        if (pauseShareBtn) {
            pauseShareBtn.onclick = () => this.togglePauseScreenShare();
        }

        const stopShareBtn = document.getElementById('stopShareBtn');
        if (stopShareBtn) {
            stopShareBtn.onclick = () => this.stopScreenShare();
        }

        // 关闭设备选择器
        const closeDevices = document.getElementById('closeDevices');
        if (closeDevices) {
            closeDevices.onclick = () => this.hideDeviceSelector();
        }

        // 设备选择
        const audioDeviceSelect = document.getElementById('audioDeviceSelect');
        if (audioDeviceSelect) {
            audioDeviceSelect.onchange = (e) => {
                this.switchAudioDevice(e.target.value);
            };
        }
    }

    // 静音切换
    async toggleMute() {
        this.isMuted = !this.isMuted;

        if (this.audioStream) {
            this.audioStream.getAudioTracks().forEach(track => {
                track.enabled = !this.isMuted;
            });
        }

        const muteBtn = document.getElementById('muteBtn');
        if (muteBtn) {
            if (this.isMuted) {
                muteBtn.classList.add('active');
                muteBtn.innerHTML = '<i class="fas fa-microphone-slash"></i>';
                muteBtn.title = '取消静音';
                Logger.info('麦克风已静音');
            } else {
                muteBtn.classList.remove('active');
                muteBtn.innerHTML = '<i class="fas fa-microphone"></i>';
                muteBtn.title = '静音';
                Logger.info('麦克风已取消静音');
            }
        }
    }

    // 扬声器静音切换方法
    async toggleSpeaker() {
        if (this.volumeLevel === 0) {
            // 如果当前是静音，恢复到50%音量
            await this.setSpeakerVolume(0.5);
            this.isSpeakerMuted = false;
        } else {
            // 如果当前有音量，静音
            await this.setSpeakerVolume(0);
            this.isSpeakerMuted = true;
        }

        this.updateSpeakerButton();

        // 更新音量滑块显示
        const volumeSlider = document.getElementById('volumeSlider');
        if (volumeSlider) {
            volumeSlider.value = this.volumeLevel * 100;
            volumeSlider.style.background = `linear-gradient(to bottom, #4CAF50 0%, #4CAF50 ${this.volumeLevel * 100}%, #555 ${this.volumeLevel * 100}%, #555 100%)`;
        }
    }

    // 设置扬声器音量
    async setSpeakerVolume(volume) {
        if (volume < 0 || volume > 1) {
            throw new Error('音量必须在 0 到 1 之间');
        }

        this.volumeLevel = volume;

        if (this.remoteAudio) {
            this.remoteAudio.volume = volume;
        }

        // 更新按钮状态
        this.updateSpeakerButton();

        // 保存音量设置
        try {
            localStorage.setItem('speakerVolume', volume.toString());
        } catch (e) {
            // 忽略本地存储错误
        }
    }

    // 更新扬声器按钮状态
    updateSpeakerButton() {
        const speakerBtn = document.getElementById('speakerBtn');
        if (!speakerBtn) return;

        if (this.isSpeakerMuted || this.volumeLevel === 0) {
            speakerBtn.classList.add('active');
            speakerBtn.innerHTML = '<i class="fas fa-volume-mute"></i>';
            speakerBtn.title = '取消静音';
        } else {
            speakerBtn.classList.remove('active');
            speakerBtn.innerHTML = '<i class="fas fa-volume-up"></i>';
            speakerBtn.title = `音量控制`;
        }
    }

    // 切换屏幕共享暂停状态
    async togglePauseScreenShare() {
        const pauseBtn = document.getElementById('pauseShareBtn');

        if (this.isScreenPaused) {
            await this.resumeScreenShare();
            if (pauseBtn) {
                pauseBtn.classList.remove('active');
                pauseBtn.innerHTML = '<i class="fas fa-pause"></i><div class="tooltip">暂停共享</div>';
            }
        } else {
            await this.pauseScreenShare();
            if (pauseBtn) {
                pauseBtn.classList.add('active');
                pauseBtn.innerHTML = '<i class="fas fa-play"></i><div class="tooltip">继续共享</div>';
            }
        }
    }

    // 取消通话
    async cancelCall() {
        try {
            Logger.info('取消桌面共享...');

            this.stopCallTone();
            this.playHangupTone();

            await this.signalingHandler.callCancelled({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                reason: '用户主动取消'
            });

            this.endCall('桌面共享已取消');

        } catch (error) {
            Logger.error('取消桌面共享失败:', error);
        }
    }

    // 挂断通话
    async hangup() {
        try {
            Logger.info('挂断桌面共享...');

            this.stopRingtone();
            this.stopCallTone();
            this.playHangupTone();

            await this.signalingHandler.callEnded({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                reason: '用户主动挂断'
            });
            this.endCall('桌面共享已结束');

        } catch (error) {
            Logger.error('挂断桌面共享失败:', error);
        }
    }

    // 切换设备
    async switchAudioDevice(deviceId) {
        try {
            Logger.info(`切换音频设备: ${deviceId}`);

            if (this.audioStream) {
                this.deviceManager.stopStream(this.audioStream.id);
            }

            if (this.audioProducer) {
                await this.mediasoupClient.closeProducer(this.audioProducer.id);
            }

            await this.getAudioStream();

            this.audioProducer = await this.mediasoupClient.produceAudio(this.audioStream);

            Logger.info('音频设备切换完成');

        } catch (error) {
            Logger.error('切换音频设备失败:', error);
            this.showError('设备切换失败: ' + error.message);
        }
    }

    // 填充设备
    populateAudioDevices() {
        try {
            const select = document.getElementById('audioDeviceSelect');
            if (!select) return;

            const devices = this.deviceManager.getAvailableDevices().audioinput;

            select.innerHTML = '<option value="">选择麦克风</option>';

            devices.forEach(device => {
                const option = document.createElement('option');
                option.value = device.deviceId;
                option.textContent = device.label;

                if (device.deviceId === this.deviceManager.selectedDevices.audioinput) {
                    option.selected = true;
                }

                select.appendChild(option);
            });

            Logger.info('音频设备列表已更新');

        } catch (error) {
            Logger.error('填充音频设备列表失败:', error);
        }
    }

    // 启动呼叫超时计时器（发起方）
    startCallTimeout() {
        this.stopAllTimeouts();

        Logger.info(`启动呼叫超时计时器: ${this.callTimeoutDuration}ms`);

        this.callTimeoutTimer = setTimeout(async () => {
            Logger.warn('呼叫超时，自动结束桌面共享');

            this.stopCallTone();

            await this.signalingHandler.callTimeout({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                timeoutType: 'CALL_TIMEOUT',
                reason: '呼叫超时（35秒）'
            });

            this.endCall('呼叫超时，对方未接听');

        }, this.callTimeoutDuration);
    }

    // 启动接听超时计时器（接收方）
    startAnswerTimeout() {
        this.stopAllTimeouts();

        Logger.info(`启动接听超时计时器: ${this.answerTimeoutDuration}ms`);

        this.answerTimeoutTimer = setTimeout(async () => {
            Logger.warn('接听超时，自动结束桌面共享');

            this.stopRingtone();

            await this.signalingHandler.callTimeout({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                timeoutType: 'ANSWER_TIMEOUT',
                reason: '接听超时（30秒）'
            });

            this.endCall('接听超时');

        }, this.answerTimeoutDuration);
    }

    // 停止所有超时计时器
    stopAllTimeouts() {
        if (this.callTimeoutTimer) {
            clearTimeout(this.callTimeoutTimer);
            this.callTimeoutTimer = null;
            Logger.info('停止呼叫超时计时器');
        }

        if (this.answerTimeoutTimer) {
            clearTimeout(this.answerTimeoutTimer);
            this.answerTimeoutTimer = null;
            Logger.info('停止接听超时计时器');
        }
    }

    // 处理超时信令
    handleCallTimeout() {
        Logger.warn(`收到超时信令`);

        this.stopRingtone();
        this.stopCallTone();

        let reason = '桌面共享已结束';
        if (!this.isIncomingCall) {
            reason = '呼叫超时，对方未接听';
        } else {
            reason = '接听超时';
        }

        this.endCall(reason);
    }

    // 结束通话
    endCall(reason) {
        Logger.info(`结束桌面共享: ${reason}`);

        this.stopRingtone();
        this.stopCallTone();

        this.stopAllTimeouts();

        this.stopCallTimer();

        this.isCallActive = false;

        // 隐藏所有按钮
        const shareScreenBtn = document.getElementById('shareScreenBtn');
        const pauseShareBtn = document.getElementById('pauseShareBtn');
        const stopShareBtn = document.getElementById('stopShareBtn');
        const hangupBtn = document.getElementById('hangupBtn');
        const incomingCallActions = document.getElementById('incomingCallActions');

        if (shareScreenBtn) shareScreenBtn.style.display = 'none';
        if (pauseShareBtn) pauseShareBtn.style.display = 'none';
        if (stopShareBtn) stopShareBtn.style.display = 'none';
        if (hangupBtn) hangupBtn.style.display = 'none';
        if (incomingCallActions) incomingCallActions.style.display = 'none';

        this.cleanup();

        this.showCallEnded(reason);

        setTimeout(() => {
            this.signalingHandler.closeWindow();
        }, 2000);
    }

    // 清理资源
    cleanup() {
        this.stopRingtone();
        this.stopCallTone();

        if (this.deviceManager) {
            this.deviceManager.stopAllStreams();
        }

        if (this.mediasoupClient) {
            this.mediasoupClient.closeAll();
        }

        if (this.audioContext) {
            this.audioContext.close();
        }

        if (this.remoteAudio) {
            this.remoteAudio.pause();
            this.remoteAudio.srcObject = null;
            this.remoteAudio.remove();
            this.remoteAudio = null;
        }
    }

    // 展示结束通话信息
    showCallEnded(reason) {
        const callStatus = document.getElementById('callStatus');
        if (callStatus) {
            callStatus.textContent = reason;
        }

        const timer = document.querySelector('.timer');
        if (timer) {
            timer.classList.add('hidden');
        }
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

    // 销毁
    destroy() {
        this.cleanup();

        if (this.deviceManager) {
            this.deviceManager.destroy();
        }

        if (this.signalingHandler) {
            this.signalingHandler.destroy();
        }

        if (this.mediasoupClient) {
            this.mediasoupClient.destroy();
        }

        Logger.info('桌面共享应用已销毁');
    }
}

// 全局函数供Java调用
window.handleCallInitData = function (initData) {
    console.log('window.handleCallInitData called')
    if (!window.screenShareApp) {
        window.screenShareApp = new ScreenShareApp();
    }
    window.screenShareApp.handleCallInit(initData);
};

window.handleCallAccepted = function () {
    if (window.screenShareApp) {
        window.screenShareApp.handleCallAccepted();
    }
};

window.handleCallRejected = function (reason) {
    if (window.screenShareApp) {
        window.screenShareApp.handleCallRejected(reason);
    }
};

window.handleCallHangup = function () {
    if (window.screenShareApp) {
        window.screenShareApp.handleCallHangup();
    }
};

window.handleCallCancel = function () {
    if (window.screenShareApp) {
        window.screenShareApp.handleCallCancel();
    }
};

window.handleCallTimeout = function () {
    if (window.screenShareApp) {
        window.screenShareApp.handleCallTimeout();
    }
};

window.handleSignalingMessage = function () {

};

window.handleError = function (message) {
    if (window.screenShareApp) {
        window.screenShareApp.showError(message);
    }
};

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function () {
    console.log('DOMContentLoaded')
    window.screenShareApp = new ScreenShareApp();
});

// 通知mediasoup库已加载
if (typeof mediasoupClient !== 'undefined') {
    window.dispatchEvent(new Event('mediasoupClientLoaded'));
}