package com.guru.im.core.manager;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.listener.InvokeCallback;
import com.guru.im.core.common.listener.ResponseCallBack;
import com.guru.im.core.common.listener.ResponseFuture;
import com.guru.im.core.common.listener.SemaphoreReleaseOnlyOnce;
import com.guru.im.core.common.processor.MessageProcessor;
import com.guru.im.core.common.exception.SendRequestException;
import com.guru.im.core.common.exception.SendTimeoutException;
import com.guru.im.core.common.processor.Pair;
import com.guru.im.core.common.util.ChannelUtil;
import com.guru.im.core.common.util.ExceptionUtils;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class MessageProcessManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessManager.class);
    private final ConcurrentMap<Long, ResponseFuture> responseTable = new ConcurrentHashMap<>(256);
    private final Semaphore semaphoreAsync = new Semaphore(65535, true);
    private final Semaphore semaphoreOneway = new Semaphore(65535, true);

    private Pair<MessageProcessor, ExecutorService> messageProcessorPair = null;

    private ExecutorService callbackExecutor;

    public MessageProcessManager() {
    }

    public MessageProcessManager(ExecutorService callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    public void processMessageReceived(final ChannelHandlerContext ctx, final ImMessage msg) {
        if (msg == null) {
            return;
        }
        ImMessage.MsgType msgType = msg.getMsgType();
        switch (msgType) {
            case REQUEST:
                LOGGER.debug("收到请求消息：{}", msg.getMsgId());
                processRequestCommand(ctx, msg);
                break;
            case RESPONSE:
                LOGGER.debug("收到响应消息：{}", msg.getMsgId());
                processResponseCommand(ctx, msg);
                break;
            case ONEWAY:
                LOGGER.debug("收到单向消息：{}", msg.getMsgId());
                processOnewayCommand(ctx, msg);
                break;
            default:
                break;
        }
    }

    public void processRequestCommand(final ChannelHandlerContext ctx, final ImMessage cmd) {
        if (this.messageProcessorPair == null) {
            String error = " no processor found for cmd ";
            writeResponseErr(ctx.channel(), cmd, ResponseCode.REQUEST_CODE_NOT_SUPPORTED, error);
            LOGGER.error("{}: {}", ChannelUtil.parseChannelRemoteAddr(ctx.channel()), error);
            return;
        }

        MessageProcessor processor = this.messageProcessorPair.getObject1();
        ExecutorService executorService = this.messageProcessorPair.getObject2();

        try {
            Runnable runProcessor = buildProcessRequestHandler(ctx, cmd, processor);
            executorService.submit(runProcessor);
        } catch (RejectedExecutionException e) {
            String error = " too many requests and system thread pool busy, RejectedExecutionException "
                    + this.messageProcessorPair.getObject2().toString();
            LOGGER.warn("{}{}", ChannelUtil.parseChannelRemoteAddr(ctx.channel()), error);
            writeResponseErr(ctx.channel(), cmd, ResponseCode.SYSTEM_BUSY, "[OVERLOAD]system busy, start flow control for a while");
        }
    }

    public void processResponseCommand(ChannelHandlerContext ctx, ImMessage cmd) {
        final long opaque = cmd.getMsgId();
        final ResponseFuture responseFuture = responseTable.get(opaque);
        if (responseFuture != null) {
            responseFuture.setResponse(cmd);

            responseTable.remove(opaque);

            if (responseFuture.getInvokeCallback() != null) {
                executeInvokeCallback(responseFuture);
            }
        } else {
            LOGGER.warn("receive response, cmd={}, but not matched any request, address={}, opaque={}", cmd, ChannelUtil.parseChannelRemoteAddr(ctx.channel()), opaque);
        }
    }

    public void processOnewayCommand(final ChannelHandlerContext ctx, final ImMessage cmd) {
        if (this.messageProcessorPair == null) {
            String error = " no processor found for request ";
            LOGGER.error("{}: {}", ChannelUtil.parseChannelRemoteAddr(ctx.channel()), error);
            return;
        }

        MessageProcessor processor = this.messageProcessorPair.getObject1();
        ExecutorService executorService = this.messageProcessorPair.getObject2();

        try {
            executorService.submit(() -> {
                try {
                    processor.processOneway(ctx, cmd);
                } catch (Exception e) {
                    LOGGER.error("processOneway exception: {}", e.getMessage(), e);
                }
            });
        } catch (RejectedExecutionException e) {
            String error = " too many requests and system thread pool busy, RejectedExecutionException "
                    + this.messageProcessorPair.getObject2().toString();
            LOGGER.warn("{}{}", ChannelUtil.parseChannelRemoteAddr(ctx.channel()), error);
        }
    }


    private Runnable buildProcessRequestHandler(ChannelHandlerContext ctx, ImMessage request, MessageProcessor processor) {
        return () -> {
            String remoteAddr = "";
            ImMessage response = null;
            try {
                remoteAddr = ChannelUtil.parseChannelRemoteAddr(ctx.channel());
                response = processor.processRequest(ctx, request);

                writeResponse(ctx.channel(), request, response);
            } catch (Exception e) {
                response = MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, e.getMessage());
                writeResponse(ctx.channel(), request, response);
            } catch (Throwable e) {
                LOGGER.error("process request exception, remoteAddr: {}", remoteAddr, e);
                response = MessageBuilder.createImResponse(request, ResponseCode.SYSTEM_ERROR, e.getMessage());
                writeResponse(ctx.channel(), request, response);
            }
        };
    }

    public void writeResponseErr(Channel channel, ImMessage request, int errCode, String errMsg) {
        ImMessage response = MessageBuilder.createImResponse(request, errCode, errMsg);
        writeResponse(channel, request, response);
    }

    public void writeResponse(Channel channel, ImMessage request, ImMessage response) {
        writeResponse(channel, request, response, null);
    }

    public void writeResponse(Channel channel, ImMessage request, ImMessage response,
                              Consumer<Future<?>> callback) {
        if (response == null) {
            return;
        }
        try {
            channel.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    LOGGER.debug("response [{}] is written successfully", request.getMsgId());
                } else {
                    LOGGER.error("Failed to write response[{}] to channel", response.getMsgId(), future.cause());
                }
                if (callback != null) {
                    callback.accept(future);
                }
            });
        } catch (Throwable e) {
            LOGGER.error("process request over, but response failed", e);
        }
    }

    public void writeMessage(Channel channel, ImMessage message) {
        try {
            channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    LOGGER.debug("notify [{}] is written successfully", message.getMsgId());
                } else {
                    LOGGER.error("Failed to write notify[{}] to channel", message.getMsgId(), future.cause());
                }
            });
        } catch (Throwable e) {
            LOGGER.error("write notify failed", e);
        }
    }

    public void executeInvokeCallback(final ResponseFuture responseFuture) {
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null && !executor.isShutdown()) {
            invokeCallbackInExecutor(executor, responseFuture);
        } else {
            invokeCallbackThisThread(responseFuture);
        }
    }

    private void invokeCallbackInExecutor(ExecutorService executor, ResponseFuture responseFuture) {
        try {
            executor.submit(() -> {
                try {
                    responseFuture.executeInvokeCallback();
                } catch (Throwable e) {
                    LOGGER.warn("executeInvokeCallback Exception", e);
                } finally {
                    responseFuture.release();
                }
            });
        } catch (Exception e) {
            LOGGER.warn("execute callback in executor exception, maybe executor busy", e);
        } finally {
            responseFuture.release();
        }
    }

    private void invokeCallbackThisThread(ResponseFuture responseFuture) {
        try {
            responseFuture.executeInvokeCallback();
        } catch (Throwable e) {
            LOGGER.warn("executeInvokeCallback Exception", e);
        } finally {
            responseFuture.release();
        }
    }


    public ImMessage sendSync(final Channel channel, final ImMessage request, final long timeoutMillis)
            throws SendTimeoutException, SendRequestException {
        try {
            return invokeSendAsync(channel, request, timeoutMillis).thenApply(ResponseFuture::getResponse)
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new SendRequestException(channel.remoteAddress().toString(), e.getCause());
        } catch (TimeoutException e) {
            throw new SendTimeoutException(channel.remoteAddress().toString(), timeoutMillis, e.getCause());
        }
    }

    public CompletableFuture<ResponseFuture> invokeSendAsync(final Channel channel, final ImMessage request,
                                                             final long timeoutMillis) {
        CompletableFuture<ResponseFuture> future = new CompletableFuture<>();
        long beginStartTime = System.currentTimeMillis();
        final long msgId = request.getMsgId();
        boolean acquired;
        try {
            acquired = this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!acquired) {
                return semaphoreTimeoutExceptionalFuture(timeoutMillis, future);
            }

            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);
            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeoutMillis < costTime) {
                once.release();
                future.completeExceptionally(new SendTimeoutException("invokeSendAsync call timeout"));
                return future;
            }
            long leftTime = timeoutMillis - costTime;

            final ResponseFuture responseFuture = new ResponseFuture(channel, msgId, request, leftTime);
            responseFuture.setInvokeCallback(new ResponseCallBack(future, responseFuture)); // 当收到响应时，将响应写回future
            responseFuture.setSemaphoreReleaseOnlyOnce(once); // 当收到响应时，再释放这个信号量

            this.responseTable.put(msgId, responseFuture); // 请求和响应通过唯一id关联起来
            try {
                channel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        responseFuture.setSendRequestOK(true);
                        return;
                    }
                    requestFail(request.getMsgId());
                    LOGGER.warn("send a request command to channel <{}>, channelId={}, failed.", ChannelUtil.parseChannelRemoteAddr(channel), channel.id());
                });
            } catch (Exception e) {
                responseTable.remove(msgId).release();
                LOGGER.warn("send a request command to channel <{}> channelId={} Exception", ChannelUtil.parseChannelRemoteAddr(channel), channel.id(), e);
                future.completeExceptionally(new SendRequestException(ChannelUtil.parseChannelRemoteAddr(channel), e));
            }

        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<ResponseFuture> semaphoreTimeoutExceptionalFuture(long timeoutMillis, CompletableFuture<ResponseFuture> future) {
        if (timeoutMillis <= 0) {
            future.completeExceptionally(new SendTimeoutException("invokeAsyncImpl invoke too fast"));
        } else {
            String info = String.format("invokeAsyncImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                    timeoutMillis,
                    this.semaphoreAsync.getQueueLength(),
                    this.semaphoreAsync.availablePermits()
            );
            LOGGER.warn(info);
            future.completeExceptionally(new SendTimeoutException(info));
        }
        return future;
    }

    public void invokeOneway(final Channel channel, final ImMessage request, final long timeoutMillis)
            throws InterruptedException, SendTimeoutException, SendRequestException {
        if (request.getMsgType() != ImMessage.MsgType.ONEWAY) {
            LOGGER.warn("request msgType <{}> not support.", request.getMsgType());
            return;
        }
        boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);
            try {
                channel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                    once.release();
                    if (!f.isSuccess()) {
                        LOGGER.warn("send a request command to channel <{}> failed.", channel.remoteAddress());
                    }
                });
            } catch (Exception e) {
                once.release();
                LOGGER.warn("write send a request command to channel <{}> failed.", channel.remoteAddress());
                throw new SendRequestException(ChannelUtil.parseChannelRemoteAddr(channel), e);
            }
        } else {
            String info = String.format(
                    "invokeOneway tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreOnewayValue: %d",
                    timeoutMillis,
                    this.semaphoreOneway.getQueueLength(),
                    this.semaphoreOneway.availablePermits()
            );
            LOGGER.warn(info);
            throw new SendTimeoutException(info);
        }
    }

    public void sendAsync(final Channel channel, final ImMessage request, final long timeoutMillis,
                          final InvokeCallback invokeCallback) {
        invokeSendAsync(channel, request, timeoutMillis)
                .whenComplete((v, t) -> {
                    if (t == null) {
                        invokeCallback.operationComplete(v);
                    } else {
                        ResponseFuture responseFuture = new ResponseFuture(channel, request.getMsgId(), request, timeoutMillis, null, null);
                        responseFuture.setCause(t);
                        invokeCallback.operationComplete(responseFuture);
                    }
                })
                .thenAccept(responseFuture -> invokeCallback.operationSucceed(responseFuture.getResponse()))
                .exceptionally(t -> {
                    invokeCallback.operationFail(ExceptionUtils.getRealException(t));
                    return null;
                });
    }

    private void requestFail(final long opaque) {
        ResponseFuture responseFuture = responseTable.remove(opaque);
        if (responseFuture != null) {
            responseFuture.setSendRequestOK(false);
            invokeCallbackThisThread(responseFuture);
        }
    }

    public void bindRequestProcessor(MessageProcessor messageProcessor, ExecutorService executorService) {
        this.messageProcessorPair = new Pair<>(messageProcessor, executorService);
    }

    public ExecutorService getCallbackExecutor() {
        return callbackExecutor;
    }

    public void setCallbackExecutor(ExecutorService callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    public void clear() {
        responseTable.clear();
        if (callbackExecutor != null) {
            callbackExecutor.shutdown();
        }
    }


}
