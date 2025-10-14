class AudioCallApp {
    constructor() {
        this.initData = null;
        this.deviceManager = null;
        this.signalingHandler = null;
        this.mediasoupClient = null;

        this.audioStream = null;
        this.audioProducer = null;
        this.audioConsumer = null; // 添加音频消费者
        this.audioAnalyser = null;
        this.audioContext = null;
        this.remoteAudio = null; // 远程音频元素

        this.callStartTime = null;
        this.callTimer = null;
        this.isMuted = false;
        this.isIncomingCall = false;
        this.isCallActive = false;

        this.isInitialized = false;

        // 音效控制
        this.ringtone = null;
        this.callTone = null;
        this.hangupTone = null;
        this.isRinging = false;
        this.isCalling = false;

        this.callTimeoutDuration = 35000;  // 呼叫超时35秒
        this.answerTimeoutDuration = 30000; // 接听超时30秒

        // 只初始化基础组件，不初始化设备
        this.initializeBaseComponents();
    }

    // 初始化音效
    initializeSounds() {
        try {
            this.ringtone = document.getElementById('ringtone');
            this.callTone = document.getElementById('callTone');
            this.hangupTone = document.getElementById('hangupTone');

            // 设置音量
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
                this.callTone.loop = true; // 设置为循环播放
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

    // 在初始化基础组件时初始化音效
    async initializeBaseComponents() {
        try {
            Logger.info('初始化基础组件...');

            // 初始化管理器（但不初始化设备）
            this.deviceManager = new DeviceManager();
            this.signalingHandler = new SignalingHandler();
            this.mediasoupClient = new MediasoupClient(this.signalingHandler);

            // 初始化音效
            this.initializeSounds();

            // 设置UI事件
            this.setupUIEvents();

            // 隐藏加载界面，显示等待初始化数据的界面
            this.showWaitingForInitData();

            Logger.info('基础组件初始化完成，等待初始化数据...');

            // 主动请求初始化数据
            await this.requestCallInitData();

        } catch (error) {
            Logger.error('基础组件初始化失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // 请求初始化数据
    async requestCallInitData() {
        try {
            Logger.info('主动请求通话初始化数据...');
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
            this.showError('获取通话数据失败: ' + error.message);
        }
    }

    // 显示等待初始化数据的界面
    showWaitingForInitData() {
        document.getElementById('loadingOverlay').classList.add('hidden');

        // 显示等待消息
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
                <div style="margin-top: 20px; font-size: 1.1rem;">等待通话初始化数据...</div>
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

    // 添加切换设备选择器的方法
    toggleDeviceSelector() {
        const deviceSelector = document.getElementById('deviceSelector');

        if (deviceSelector.classList.contains('active')) {
            this.hideDeviceSelector();
        } else {
            this.showDeviceSelector();
        }
    }

    // 修改显示设备选择器方法
    showDeviceSelector() {
        try {
            Logger.info('打开设备设置');
            const deviceSelector = document.getElementById('deviceSelector');

            deviceSelector.classList.add('active');

            // 如果设备列表还没有填充，现在填充
            if (this.deviceManager && this.deviceManager.isInitialized) {
                this.populateAudioDevices();
            }
        } catch (error) {
            Logger.error('打开设备设置失败:', error);
        }
    }

    // 修改隐藏设备选择器方法
    hideDeviceSelector() {
        try {
            Logger.info('关闭设备设置');
            const deviceSelector = document.getElementById('deviceSelector');

            deviceSelector.classList.remove('active');
        } catch (error) {
            Logger.error('关闭设备设置失败:', error);
        }
    }

    // 初始化本地设备信息
    async initializeDevices() {
        try {
            // 初始化设备管理器
            await this.deviceManager.initialize();

            // 检查音频设备
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

    // 发起呼叫
    async startCall() {
        // 通知Java端设备就绪，并发起呼叫
        const callResponseStr = await this.signalingHandler.devicesReady({
            sessionId: this.initData.sessionId,
            targetUserId: this.initData.targetUserId,
            deviceId: this.initData.deviceInfo.deviceId
        });
        Logger.info('收到呼叫请求的响应:', callResponseStr);

        const callResponse = JSON.parse(callResponseStr);

        if (callResponse && callResponse.callRequestResponse) {
            if (callResponse.callRequestResponse.success) {
                // 更新sessionId和roomId
                this.sessionId = callResponse.callRequestResponse.sessionId;
                this.roomId = callResponse.callRequestResponse.roomId;
                Logger.info(`更新会话信息: sessionId=${this.sessionId}, roomId=${this.roomId}`);
            } else {
                throw new Error('呼叫请求失败: ' + (callResponse.callRequestResponse.errorMessage || '未知错误'));
            }
        } else {
            throw new Error('呼叫请求失败: 未知错误');
        }
    }

    // 处理通话初始化数据
    async handleCallInit(initData) {
        try {
            Logger.info('收到通话初始化数据，开始初始化设备...');
            this.initData = initData;
            this.isIncomingCall = initData.isIncoming;

            // 隐藏等待界面
            this.hideWaitingForInitData();

            // 开始初始化设备
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
            Logger.error('处理通话初始化数据失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // 更新窗口主要区域UI
    updateCallInfo() {
        try {
            // 更新用户信息
            const userName = this.initData.targetUserName;

            document.getElementById('userName').textContent = userName;
            document.getElementById('userAvatar').textContent = Utils.createAvatar(userName);

            // 更新状态
            const status = this.isIncomingCall ? '来电中...' : '呼叫中...';
            document.getElementById('callStatus').textContent = status;

            // 显示设备选择器
            this.populateAudioDevices();
            document.querySelector('.device-selector').classList.remove('hidden');

        } catch (error) {
            Logger.error('updateCallInfo error:', error);
        }
    }

    // 启动接听通话
    async startIncomingCall() {
        try {
            Logger.info('开始处理来电...');

            // 显示来电界面
            this.showIncomingCallUI();

            // 获取音频流，提前获取变相用户交互，可以自动播放振铃
            // await this.getAudioStream();

            // 播放振铃音
            this.playRingtone();

            // 启动接听超时计时器
            this.startAnswerTimeout();

        } catch (error) {
            Logger.error('处理来电失败:', error);
            this.showError('处理来电失败: ' + error.message);
        }
    }

    // 显示接听通话页面
    showIncomingCallUI() {
        // 被叫方：显示接听/拒接按钮
        document.getElementById('incomingCallActions').classList.remove('hidden');
        document.getElementById('outgoingCallActions').classList.add('hidden');

        // 设置接听和拒绝按钮事件
        document.getElementById('acceptCallBtn').onclick = () => this.acceptCall();
        document.getElementById('rejectCallBtn').onclick = () => this.rejectCall();

        document.getElementById('callStatus').textContent = '来电中...';
        document.getElementById('controls').classList.remove('active');
    }

    // 启动呼叫通话
    async startOutgoingCall() {
        try {
            Logger.info('开始发起通话...');

            // 发起呼叫
            await this.startCall();

            // 获取音频流
            await this.getAudioStream();

            // 初始化mediasoup
            await this.initializeMediasoup();

            // 主叫方：显示挂断按钮
            document.getElementById('outgoingCallActions').classList.remove('hidden');
            document.getElementById('incomingCallActions').classList.add('hidden');
            document.getElementById('outgoingHangupBtn').onclick = () => this.cancelCall();
            document.getElementById('callStatus').textContent = '等待接听...';
            document.getElementById('controls').classList.remove('active');

            // 播放呼叫音
            this.playCallTone();

            // 启动呼叫超时计时器
            this.startCallTimeout();

        } catch (error) {
            Logger.error('发起通话失败:', error);
            this.showError('发起通话失败: ' + error.message);
        }
    }

    // 获取本地音频流
    async getAudioStream() {
        try {
            const selectedDevice = document.getElementById('audioDeviceSelect').value;
            const constraints = selectedDevice ? { deviceId: selectedDevice } : {};

            const result = await this.deviceManager.getAudioStream(constraints);
            this.audioStream = result.stream;

            // 设置音频分析器
            this.setupAudioAnalyser();

            Logger.info('音频流获取成功');

        } catch (error) {
            Logger.error('获取音频流失败:', error);
            throw error;
        }
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

    // 处理新消费者事件
    async handleNewConsumer(data) {
        try {
            const { roomId, producerId, kind } = data;

            if (kind === 'audio' && producerId !== this.audioProducer.id) {
                await this.createAudioConsumer(roomId, producerId);
            }
        } catch (error) {
            Logger.error('处理新消费者失败:', error);
        }
    }

    // 创建音频消费者
    async createAudioConsumer(roomId, producerId) {
        try {
            Logger.info(`创建音频消费者，producerId: ${producerId}`);

            const consumer = await this.mediasoupClient.consume(producerId);

            // 播放远程音频
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
            // 创建远程音频流
            const remoteStream = new MediaStream([track]);

            // 创建隐藏的音频元素来播放远程音频
            if (!this.remoteAudio) {
                this.remoteAudio = document.createElement('audio');
                this.remoteAudio.style.display = 'none';
                this.remoteAudio.volume = 1.0;
                document.body.appendChild(this.remoteAudio);
            }

            this.remoteAudio.srcObject = remoteStream;
            await this.remoteAudio.play();

            Logger.info('远程音频播放开始');

        } catch (error) {
            Logger.error('播放远程音频失败:', error);
        }
    }

    // 开启可视化音频
    startAudioVisualization() {
        const audioBars = document.getElementById('audioBars');
        audioBars.innerHTML = '';

        // 创建32个音频条
        for (let i = 0; i < 32; i++) {
            const bar = document.createElement('div');
            bar.className = 'audio-bar';
            bar.style.height = '2px';
            audioBars.appendChild(bar);
        }

        // 开始可视化循环
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

            // 更新音频条高度
            bars.forEach((bar, index) => {
                const barIndex = Math.floor((index / bars.length) * bufferLength);
                const value = dataArray[barIndex] || 0;
                const height = Math.max(2, (value / 255) * 60);
                bar.style.height = `${height}px`;

                // 根据音量设置颜色
                const intensity = value / 255;
                if (intensity > 0.7) {
                    bar.style.background = '#ef4444'; // 红色 - 高音量
                } else if (intensity > 0.4) {
                    bar.style.background = '#f59e0b'; // 黄色 - 中音量
                } else {
                    bar.style.background = '#10b981'; // 绿色 - 低音量
                }
            });

            requestAnimationFrame(updateVisualization);
        };

        updateVisualization();
    }

    // 接听通话
    async acceptCall() {
        try {
            Logger.info('接听通话...');

            // 停止振铃音
            this.stopRingtone();

            // 停止接听超时计时器
            this.stopAllTimeouts();

            // 隐藏接听界面
            document.getElementById('incomingCallActions').classList.add('hidden');

            // 获取音频流
            await this.getAudioStream();

            // 初始化mediasoup
            await this.initializeMediasoup();

            // 开始通话计时
            this.startCallTimer();

            // 更新状态
            this.isCallActive = true;
            document.getElementById('callStatus').textContent = '通话中';

            // 显示底部控制按钮
            document.getElementById('controls').classList.add('active');

            // 通知Java端通话已接受
            await this.signalingHandler.callAccepted({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId
            });

            this.roomId = `room_${this.initData.sessionId}`;

            // 接听后消费对方的音频
            await this.consumeRemoteAudio(this.roomId || `room_${this.initData.sessionId}`);

            // 开始音频可视化
            this.startAudioVisualization();

            Logger.info('通话已接听');

        } catch (error) {
            Logger.error('接听通话失败:', error);
            this.showError('接听失败: ' + error.message);
        }
    }

    // 拒绝通话
    async rejectCall() {
        try {
            Logger.info('拒绝通话...');

            // 停止振铃音并播放挂断音
            this.stopRingtone();
            this.playHangupTone();

            await this.signalingHandler.callRejected({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId
            });
            this.endCall('通话已拒绝');

        } catch (error) {
            Logger.error('拒绝通话失败:', error);
        }
    }

    // 开启通话计时器显示
    startCallTimer() {
        this.callStartTime = Date.now();

        // 显示通话中的计时器
        const callTimer = document.getElementById('callTimer');
        if (callTimer) {
            callTimer.classList.add('active');
        }

        this.callTimer = setInterval(() => {
            const elapsed = Math.floor((Date.now() - this.callStartTime) / 1000);
            const timerElement = document.getElementById('timer');
            const callTimerElement = document.getElementById('callTimer');

            if (timerElement) {
                timerElement.textContent = Utils.formatTime(elapsed);
            }
            if (callTimerElement) {
                callTimerElement.textContent = Utils.formatTime(elapsed);
            }
        }, 1000);
    }

    // 结束通话计时器显示
    stopCallTimer() {
        if (this.callTimer) {
            clearInterval(this.callTimer);
            this.callTimer = null;
        }

        // 隐藏通话中的计时器
        const callTimer = document.getElementById('callTimer');
        if (callTimer) {
            callTimer.classList.remove('active');
        }
    }

    // 处理通话接受
    handleCallAccepted() {
        Logger.info('对方已接受通话');

        // 停止呼叫音和呼叫超时计时器
        this.stopCallTone();
        this.stopAllTimeouts();

        this.isCallActive = true;
        this.startCallTimer();

        // 更新通话状态显示
        const callStatus = document.getElementById('callStatus');
        if (callStatus) {
            callStatus.textContent = '通话中';
        }

        // 隐藏等待中的挂断按钮
        document.getElementById('outgoingCallActions').classList.add('hidden');
        // 显示底部控制按钮
        document.getElementById('controls').classList.add('active');

        // 对方接受通话后，开始消费远程音频
        this.consumeRemoteAudio(this.roomId || `room_${this.initData.sessionId}`);

        // 开始音频可视化
        this.startAudioVisualization();
    }

    // 处理对方拒绝通话
    handleCallRejected(reason) {
        Logger.info(`通话被拒绝: ${reason}`);
        this.stopCallTone();
        this.playHangupTone();
        this.endCall(`${reason}`);
    }

    // 处理对方挂断通话
    handleCallHangup() {
        Logger.info('对方已挂断通话');
        this.stopRingtone();
        this.stopCallTone();
        this.playHangupTone();
        this.endCall('对方已挂断');
    }

    // 处理对方取消通话
    handleCallCancel() {
        Logger.info('对方已取消通话');
        this.stopRingtone();
        this.stopCallTone();
        this.playHangupTone();
        this.endCall('对方已取消');
    }

    handleMediaControl(message) {
        const control = message.mediaControl;
        if (control.controlType === 'MUTE_AUDIO') {
            this.setMute(control.enabled);
        }
    }

    // UI事件处理
    setupUIEvents() {
        // 静音按钮（底部）
        document.getElementById('muteBtn').onclick = () => this.toggleMute();

        // 挂断按钮（底部）
        document.getElementById('hangupBtn').onclick = () => this.hangup();

        // 设备按钮（底部）
        document.getElementById('deviceBtn').onclick = () => this.toggleDeviceSelector();

        // 关闭设备选择器
        document.getElementById('closeDevices').onclick = () => this.hideDeviceSelector();

        // 设备选择
        document.getElementById('audioDeviceSelect').onchange = (e) => {
            this.switchAudioDevice(e.target.value);
        };
    }

    // 静音切换
    async toggleMute() {
        this.isMuted = !this.isMuted;

        // 控制音频轨道
        if (this.audioStream) {
            this.audioStream.getAudioTracks().forEach(track => {
                track.enabled = !this.isMuted;
            });
        }

        // 更新UI
        const muteBtn = document.getElementById('muteBtn');
        if (this.isMuted) {
            muteBtn.classList.add('active');
            muteBtn.innerHTML = '<i class="fas fa-microphone-slash"></i><div class="tooltip">取消静音</div>';
            Logger.info('麦克风已静音');
        } else {
            muteBtn.classList.remove('active');
            muteBtn.innerHTML = '<i class="fas fa-microphone"></i><div class="tooltip">静音</div>';
            Logger.info('麦克风已取消静音');
        }
    }

    // 取消通话
    async cancelCall() {
        try {
            Logger.info('取消通话...');

            // 停止呼叫音并播放挂断音
            this.stopCallTone();
            this.playHangupTone();

            await this.signalingHandler.callCancelled({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                reason: '用户主动取消'
            });

            this.endCall('通话已取消');

        } catch (error) {
            Logger.error('取消通话失败:', error);
        }
    }

    // 挂断通话
    async hangup() {
        try {
            Logger.info('挂断通话...');

            // 停止所有循环音效并播放挂断音
            this.stopRingtone();
            this.stopCallTone();
            this.playHangupTone();

            await this.signalingHandler.callEnded({
                sessionId: this.initData.sessionId,
                targetUserId: this.initData.targetUserId,
                reason: '用户主动挂断'
            });
            this.endCall('通话已结束');

        } catch (error) {
            Logger.error('挂断通话失败:', error);
        }
    }

    // 切换设备
    async switchAudioDevice(deviceId) {
        try {
            Logger.info(`切换音频设备: ${deviceId}`);

            // 停止当前流
            if (this.audioStream) {
                this.deviceManager.stopStream(this.audioStream.id);
            }

            // 关闭当前生产者
            if (this.audioProducer) {
                await this.mediasoupClient.closeProducer(this.audioProducer.id);
            }

            // 获取新设备流
            await this.getAudioStream();

            // 创建新的生产者
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
            const devices = this.deviceManager.getAvailableDevices().audioinput;

            select.innerHTML = '<option value="">选择麦克风</option>';

            devices.forEach(device => {
                const option = document.createElement('option');
                option.value = device.deviceId;
                option.textContent = device.label;

                // 如果是当前选中的设备，设置为选中状态
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

        // 停止所有音效
        this.stopRingtone();
        this.stopCallTone();

        // 根据超时类型显示不同的提示信息
        let reason = '通话已结束';
        if (!this.isIncomingCall) {
            reason = '呼叫超时，对方未接听';
        } else {
            reason = '接听超时';
        }

        this.endCall(reason);
    }

    // 结束通话
    endCall(reason) {
        Logger.info(`结束通话: ${reason}`);

        // 停止所有音效
        this.stopRingtone();
        this.stopCallTone();

        // 停止所有超时计时器
        this.stopAllTimeouts();

        // 隐藏所有操作按钮
        document.getElementById('incomingCallActions').classList.add('hidden');
        document.getElementById('outgoingCallActions').classList.add('hidden');
        document.getElementById('controls').classList.remove('active');

        // 停止通话计时器
        this.stopCallTimer();

        // 停止音频可视化
        this.isCallActive = false;

        // 重置布局状态
        document.getElementById('header').classList.remove('call-active');
        document.getElementById('timer').classList.remove('active');
        document.getElementById('controls').classList.remove('active');

        // 清理资源
        this.cleanup();

        // 显示结束信息
        this.showCallEnded(reason);

        // 延迟关闭窗口
        setTimeout(() => {
            this.signalingHandler.closeWindow();
        }, 2000);
    }

    // 清理资源
    cleanup() {
        // 停止所有音效
        this.stopRingtone();
        this.stopCallTone();

        // 停止所有媒体流
        if (this.deviceManager) {
            this.deviceManager.stopAllStreams();
        }

        // 关闭mediasoup连接
        if (this.mediasoupClient) {
            this.mediasoupClient.closeAll();
        }

        // 关闭音频上下文
        if (this.audioContext) {
            this.audioContext.close();
        }

        // 移除远程音频元素
        if (this.remoteAudio) {
            this.remoteAudio.pause();
            this.remoteAudio.srcObject = null;
            this.remoteAudio.remove();
            this.remoteAudio = null;
        }
    }

    // 展示结束通话信息
    showCallEnded(reason) {
        document.getElementById('callStatus').textContent = reason;
        document.querySelector('.timer').classList.add('hidden');
        document.querySelector('.controls').classList.add('hidden');
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

        Logger.info('语音通话应用已销毁');
    }
}

// 全局函数供Java调用
window.handleCallInitData = function (initData) {
    console.log('window.handleCallInitData called')
    if (!window.audioCallApp) {
        // 如果应用还没创建，先创建应用
        window.audioCallApp = new AudioCallApp();
    }
    // 处理初始化数据，这会触发设备初始化
    window.audioCallApp.handleCallInit(initData);
};

window.handleCallAccepted = function () {
    if (window.audioCallApp) {
        window.audioCallApp.handleCallAccepted();
    }
};

window.handleCallRejected = function (reason) {
    if (window.audioCallApp) {
        window.audioCallApp.handleCallRejected(reason);
    }
};

window.handleCallHangup = function () {
    if (window.audioCallApp) {
        window.audioCallApp.handleCallHangup();
    }
};

window.handleCallCancel = function () {
    if (window.audioCallApp) {
        window.audioCallApp.handleCallCancel();
    }
};

window.handleCallTimeout = function () {
    if (window.audioCallApp) {
        window.audioCallApp.handleCallTimeout();
    }
};

window.handleSignalMessage = function () {
};

window.handleError = function (message) {
    if (window.audioCallApp) {
        window.audioCallApp.showError(message);
    }
};

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function () {
    console.log('DOMContentLoaded')
    window.audioCallApp = new AudioCallApp();
    window.audioCallApp.setupUIEvents();
});

// 通知mediasoup库已加载
if (typeof mediasoupClient !== 'undefined') {
    window.dispatchEvent(new Event('mediasoupClientLoaded'));
}