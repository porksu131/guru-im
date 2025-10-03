package com.guru.im.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.file.model.FileUploadComplete;
import com.guru.im.demo.gui.file.model.FileUploadInit;
import com.guru.im.demo.model.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiService {
    public static String GATEWAY_HTTP_BASE_URL;
    public static String GATEWAY_TCP_SERVER;

    public static String LOGIN_URL = "/api/auth/login";
    public static String LOGOUT_URL = "/api/auth/logout";
    public static String REGISTER_URL = "/api/auth/register";
    public static String REFRESH_TOKEN_URL = "/api/auth/refresh";
    public static String ALL_USER_INFO_URL = "/api/user/queryAllUser";
    public static String QUERY_USER_BY_NAME_URL = "/api/user/queryByUserName";
    public static String QUERY_USER_BY_UID_URL = "/api/user/queryByUserId";
    public static String GET_USER_BY_UID_URL = "/api/user/getByUserId";
    public static String ALL_FRIEND_URL = "/api/friend/list";
    public static String FRIEND_REQUEST_URL = "/api/friend/request";
    public static String FRIEND_RESPONSE_URL = "/api/friend/response";
    public static String CONVERSATION_LIST_URL = "/api/conversation/list";
    public static String FILE_COMPLETE_URL = "/api/file/complete";
    public static String FILE_INIT_UPLOAD_URL = "/api/file/initUpload";
    public static String FILE_COMPLETE_PART_URL = "/api/file/completePart";
    public static String GROUP_CREATE_URL = "/api/group/create";
    public static String GROUP_LIST_URL = "/api/group/list";

    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static TokenManager tokenManager;

    public static void init(String environment) {
        if ("dev".equals(environment)) {
            // 本地环境
            ApiService.GATEWAY_HTTP_BASE_URL = "http://127.0.0.1:10921";
            ApiService.GATEWAY_TCP_SERVER = "127.0.0.1:30420";
        } else if ("k8s".equals(environment)) {
            // k8s环境
            ApiService.GATEWAY_HTTP_BASE_URL = "http://guru-im-gateway-http.com:32395";
            ApiService.GATEWAY_TCP_SERVER = "guru-im-gateway-tcp.com:31030";
        }
    }

    public static void setTokenManager(TokenManager tokenManager) {
        ApiService.tokenManager = tokenManager;
    }

    // 令牌刷新
    public static ResponseResult<UserInfo> refreshToken(String refreshToken) throws Exception {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("refreshToken", refreshToken);

        return sendPostRequest(REFRESH_TOKEN_URL, reqMap, UserInfo.class);
    }


    // 登录
    public static ResponseResult<UserInfo> login(String userName, String password) throws Exception {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("userName", userName);
        reqMap.put("password", password);

        return sendPostRequest(LOGIN_URL, reqMap, UserInfo.class);
    }

    // 注册
    public static ResponseResult<UserInfo> register(String userName, String password) throws Exception {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("userName", userName);
        reqMap.put("password", password);
        reqMap.put("isCreateDefaultRelation", "false");

        return sendPostRequest(REGISTER_URL, reqMap, UserInfo.class);
    }

    // 登出
    public static ResponseResult<Void> logout(String accessToken) {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("accessToken", accessToken);
        try {
            return sendPostRequest(LOGOUT_URL, reqMap, Void.class);
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    // 获取所有用户
    public static ResponseResult<List<UserInfo>> queryAllUser() {
        try {
            String result = sendPostRequest(ALL_USER_INFO_URL, null, "");
            return objectMapper.readValue(result, new TypeReference<ResponseResult<List<UserInfo>>>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    // 根据用户名模糊查询用户
    public static ResponseResult<List<UserInfo>> queryByUserName(String input, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("userName", input);
            String result = sendPostRequest(QUERY_USER_BY_NAME_URL, reqMap, accessToken);
            return objectMapper.readValue(result, new TypeReference<ResponseResult<List<UserInfo>>>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    // 根据ID查询用户
    public static ResponseResult<List<UserInfo>> queryByUserId(Long input, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("uid", input);
            String result = sendPostRequest(QUERY_USER_BY_UID_URL, reqMap, accessToken);
            return objectMapper.readValue(result, new TypeReference<ResponseResult<List<UserInfo>>>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    // 根据ID获取用户信息
    public static ResponseResult<UserInfo> getByUserId(Long userId, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("uid", userId);
            String result = sendPostRequest(GET_USER_BY_UID_URL, reqMap, accessToken);
            return objectMapper.readValue(result, new TypeReference<ResponseResult<UserInfo>>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }



    // 获取用户会话列表
    public static ResponseResult<List<UserConversation>> getUserConversationList(long uid, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("uid", uid);

            String result = sendPostRequest(CONVERSATION_LIST_URL, reqMap, accessToken);
            return objectMapper.readValue(result, new TypeReference<>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    // 获取好友列表
    public static ResponseResult<List<Friend>> getFriendList(long uid, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("uid", uid);

            String result = sendPostRequest(ALL_FRIEND_URL, reqMap, accessToken);
            return objectMapper.readValue(result, new TypeReference<ResponseResult<List<Friend>>>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    // 获取好友列表
    public static ResponseResult<List<Group>> getGroupList(long uid, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("uid", uid);

            String result = sendPostRequest(GROUP_LIST_URL, reqMap, accessToken);
            return objectMapper.readValue(result, new TypeReference<ResponseResult<List<Group>>>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }


    // 添加好友关系
    public static ResponseResult<Void> sendFriendRequest(long uid, long friendId, String userName, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("requesterId", uid);
            reqMap.put("responderId", friendId);
            reqMap.put("requesterName", userName);
            reqMap.put("requestType", FriendRequest.RequestType.FRIEND_REQUEST.getValue());
            reqMap.put("requestStatus", FriendRequest.RequestStatus.PENDING.getValue());
            reqMap.put("requestMsg", "您好，我是" + userName + "!");
            return sendPostRequest(FRIEND_REQUEST_URL, reqMap, accessToken, Void.class);
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }


    // 处理好友申请
    public static ResponseResult<Void> sendFriendResponse(long uid, FriendRequest friendRequest, boolean isAccept, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("id", friendRequest.getId());
            reqMap.put("requesterId", friendRequest.getRequesterId());
            reqMap.put("responderId", uid);
            reqMap.put("requesterName", friendRequest.getRequesterName());
            reqMap.put("requestType", FriendRequest.RequestType.FRIEND_REQUEST.getValue());
            reqMap.put("requestStatus", isAccept ? FriendRequest.RequestStatus.ACCEPTED.getValue()
                    : FriendRequest.RequestStatus.REJECTED.getValue());
            reqMap.put("requestMsg", friendRequest.getRequestMsg());
            return sendPostRequest(FRIEND_RESPONSE_URL, reqMap, accessToken, Void.class);
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    // 创建群聊
    public static ResponseResult<Void> sendGroupCreate(long uid, String groupName,
                                                       String groupIntro, String groupNotice,
                                                       List<Long> memberIds, String accessToken) {
        try {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("groupOwner", uid);
            reqMap.put("groupName", groupName);
            reqMap.put("groupIntro", groupIntro);
            reqMap.put("memberIds", memberIds);
            reqMap.put("groupNotice", groupNotice);
            return sendPostRequest(GROUP_CREATE_URL, reqMap, accessToken, Void.class);
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    public static ResponseResult<FileUploadInit> initFileUpload(Map<String, Object> requestBody, String accessToken) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String result = sendPostRequest(FILE_INIT_UPLOAD_URL, requestBody, accessToken);
            return objectMapper.readValue(result, new TypeReference<>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    public static ResponseResult<Boolean> completePart(Long fileId, int partNumber, String partMd5, String accessToken) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("fileId", fileId);
            requestBody.put("partNumber", partNumber);
            requestBody.put("partMd5", partMd5);
            String result = sendPostRequest(FILE_COMPLETE_PART_URL, requestBody, accessToken);
            return objectMapper.readValue(result, new TypeReference<>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    public static ResponseResult<FileUploadComplete> completeFileUpload(Long fileId, String accessToken) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("fileId", fileId);
            String result = sendPostRequest(FILE_COMPLETE_URL, requestBody, accessToken);
            return objectMapper.readValue(result, new TypeReference<ResponseResult<FileUploadComplete>>() {
            });
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }


    private static <T> ResponseResult<T> sendPostRequest(String endpoint, Class<T> clazz) throws Exception {
        return sendPostRequest(endpoint, null, null, clazz);
    }

    private static <T> ResponseResult<T> sendPostRequest(String endpoint, Object reqBody, Class<T> clazz) throws Exception {
        return sendPostRequest(endpoint, reqBody, null, clazz);
    }

    private static <T> ResponseResult<T> sendPostRequest(String endpoint, Object reqBody, String accessToken, Class<T> clazz) throws Exception {
        String result = sendPostRequest(endpoint, reqBody, accessToken);
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ResponseResult.class, clazz);
        return objectMapper.readValue(result, type);
    }

    private static String sendPostRequest(String endpoint, Object reqBody, String accessToken) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(GATEWAY_HTTP_BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            if (StringUtils.isNotBlank(accessToken)) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            if (reqBody != null) {
                String jsonData = objectMapper.writeValueAsString(reqBody);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                return getResponseContent(connection.getInputStream());
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                // token失效，退出重新登录
                if (tokenManager != null) {
                    tokenManager.showLoginDialog();
                }
                return getResponseContent(connection.getInputStream());
            } else {
                throw new RuntimeException("HTTP错误: " + responseCode + "\n" + getResponseContent(connection.getErrorStream()));
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String getResponseContent(InputStream inputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
}
