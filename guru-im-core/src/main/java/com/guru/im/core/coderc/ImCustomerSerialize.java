package com.guru.im.core.coderc;

import com.guru.im.core.common.exception.MessageConvertException;
import com.guru.im.protocol.model.ImMessage;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImCustomerSerialize {

    // 序列化类型定义
    public static final byte PROTOBUF_SERIALIZE = 1;
    public static final byte JSON_SERIALIZE = 2;
    public static final byte JAVA_SERIALIZE = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(ImCustomerSerialize.class);
    // 可以继续添加其他序列化类型
    
    public static ImMessage decode(ByteBuf inByteBuf) throws MessageConvertException {
        try {
            int length = inByteBuf.readInt();

            byte serializeType = inByteBuf.readByte(); // 读取序列化类型
            
            if (length-1 > inByteBuf.readableBytes()) { // +1是因为长度不包括序列化类型本身
                throw new MessageConvertException("Message length exceeds readable bytes");
            }


            byte[] bytes = new byte[length - 1]; // 减去序列化类型字段的1字节
            inByteBuf.readBytes(bytes);
            switch (serializeType) {
                case PROTOBUF_SERIALIZE:
                    return ImMessage.parseFrom(bytes);
                case JSON_SERIALIZE:
                    // TODO: 实现JSON反序列化
                    throw new MessageConvertException("JSON serialization not implemented yet");
                case JAVA_SERIALIZE:
                    // TODO: 实现Java原生序列化
                    throw new MessageConvertException("Java serialization not implemented yet");
                default:
                    throw new MessageConvertException("Unsupported serialize type: " + serializeType);
            }
        } catch (Exception e) {
            LOGGER.error("e: {}", e.getMessage());
            throw new MessageConvertException("Failed to decode message", e);
        }
    }

    public static void encode(ImMessage imMessage, ByteBuf outByteBuf) {
        byte[] bytes = imMessage.toByteArray();
        outByteBuf.writeInt(bytes.length + 1); // 总长度=数据长度+1字节的序列化类型
        outByteBuf.writeByte(PROTOBUF_SERIALIZE); // 写入序列化类型
        outByteBuf.writeBytes(bytes);
    }
}