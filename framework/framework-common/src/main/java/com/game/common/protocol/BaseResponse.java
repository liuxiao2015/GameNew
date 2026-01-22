package com.game.common.protocol;

import com.game.common.enums.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 响应基类
 * <p>
 * 所有服务端响应消息都应继承此类
 * </p>
 *
 * @author GameServer
 */
@Data
public class BaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求序号 (与请求对应)
     */
    private int seqId;

    /**
     * 错误码 (0 表示成功)
     */
    private int code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 服务器时间戳
     */
    private long serverTime;

    /**
     * 创建成功响应
     */
    public static BaseResponse success(int seqId) {
        BaseResponse response = new BaseResponse();
        response.setSeqId(seqId);
        response.setCode(ErrorCode.SUCCESS.getCode());
        response.setMessage(ErrorCode.SUCCESS.getMessage());
        response.setServerTime(System.currentTimeMillis());
        return response;
    }

    /**
     * 创建失败响应
     */
    public static BaseResponse fail(int seqId, ErrorCode errorCode) {
        BaseResponse response = new BaseResponse();
        response.setSeqId(seqId);
        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());
        response.setServerTime(System.currentTimeMillis());
        return response;
    }

    /**
     * 创建失败响应
     */
    public static BaseResponse fail(int seqId, int code, String message) {
        BaseResponse response = new BaseResponse();
        response.setSeqId(seqId);
        response.setCode(code);
        response.setMessage(message);
        response.setServerTime(System.currentTimeMillis());
        return response;
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS.getCode();
    }
}
