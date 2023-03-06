package com.graduate.design.service.impl;

import android.app.Application;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.graduate.design.R;
import com.graduate.design.entity.FileInfo;
import com.graduate.design.proto.Common;
import com.graduate.design.proto.CreateDir;
import com.graduate.design.proto.FileUpload;
import com.graduate.design.proto.GetNode;
import com.graduate.design.proto.GetRecvFile;
import com.graduate.design.proto.Ping;
import com.graduate.design.proto.RegisterFile;
import com.graduate.design.proto.SearchFile;
import com.graduate.design.proto.ShareFile;
import com.graduate.design.proto.UserLogin;
import com.graduate.design.proto.UserRegister;
import com.graduate.design.service.NetWorkService;
import com.graduate.design.service.UserService;
import com.graduate.design.utils.GraduateDesignApplication;
import com.graduate.design.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserServiceImpl implements UserService {
    private final String base = "https://192.168.43.39:8888";
    private NetWorkService netWorkService = new NetWorkServiceImpl();

    @Override
    public int ping(String name, String token) {
        Ping.PingRequest req = Ping.PingRequest.newBuilder()
                .setName(name)
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .build();

        JSONObject jsonObject = sendData(req, 0);
        return jsonObject == null ? 0 : 1;
    }

    @Override
    public int login(String username, String password) {
        // 构造登录请求对象
        UserLogin.UserLoginRequest req = UserLogin.UserLoginRequest.newBuilder()
                .setUserName(username)
                .setPassword(password)
                .build();

        JSONObject jsonObject = sendData(req, 1);
        // 请求失败
        if(jsonObject==null) return 1;

        // 拿到登陆成功后的token值
        String token = jsonObject.getString("token");

        // 将token值设置到全局变量中
        GraduateDesignApplication.setToken(token);

        // 构造UserInfo对象
        JSONObject userInfoJson = jsonObject.getJSONObject("userInfo");
        Long rootId = Long.parseLong(userInfoJson.getString("rootId"));
        String respUsername = userInfoJson.getString("userName");
        String email = userInfoJson.getString("email");
        UserLogin.UserInfo userInfo = UserLogin.UserInfo.newBuilder()
                .setRootId(rootId)
                .setUserName(respUsername)
                .setEmail(email)
                .build();

        // 将userInfo设置到全局变量中
        GraduateDesignApplication.setUserInfo(userInfo);

        // 请求成功
        return 0;
    }

    @Override
    public int register(String username, String password, String email) {
        // 构造注册请求对象
        UserRegister.UserRegisterRequest req = UserRegister.UserRegisterRequest.newBuilder()
                .setUserName(username)
                .setPassword(password)
                .setEmail(email)
                .build();

        JSONObject jsonObject = sendData(req, 2);
        // 请求失败
        return jsonObject==null ? 1 : 0;
    }

    @Override
    public int createDir(String dirName, Long parentId, String token) {
        CreateDir.CreateDirRequest req = CreateDir.CreateDirRequest.newBuilder()
                .setDirName(dirName)
                .setParentId(parentId)
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .build();

        return sendData(req, 3) == null ? 1 : 0;
    }


    @Override
    public int uploadFile(String fileName, Long parentId, List<String> indexList, ByteString content, ByteString secretKey, String token) {
        FileUpload.UploadFileRequest.Builder builder = FileUpload.UploadFileRequest.newBuilder()
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .setFileName(fileName)
                .setParentId(parentId)
                .setContent(content)
                .setSecretKey(secretKey);
        for(int i=0;i<indexList.size();i++){
            builder.setIndexList(i, indexList.get(i));
        }
        FileUpload.UploadFileRequest req = builder.build();

        JSONObject jsonObject = sendData(req, 4);

        return jsonObject == null ? 1 : 0;
    }

    @Override
    public List<Common.Node> searchFile(String keyword, String token) {
        List<Common.Node> res = new ArrayList<>();

        SearchFile.SearchFileRequest req = SearchFile.SearchFileRequest.newBuilder()
                .setKeyword(keyword)
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .build();

        JSONObject jsonObject = sendData(req, 5);

        if(jsonObject==null) return null;

        JSONArray nodeList = jsonObject.getJSONArray("nodeList");

        if(nodeList==null) return res;

        // 遍历所有文件节点
        for(int i=0;i<nodeList.size();i++) {
            JSONObject nodeJson = nodeList.getJSONObject(i);
            // 获取节点类型
            Common.NodeType realType;
            String nodeType = nodeJson.getString("nodeType");
            switch (nodeType) {
                case "File":
                    realType = Common.NodeType.File;
                    break;
                case "Dir":
                    realType = Common.NodeType.Dir;
                    break;
                default:
                    realType = Common.NodeType.Unknown;
                    break;
            }
            // 构造子节点
            Common.Node node = Common.Node.newBuilder()
                    .setNodeType(realType)
                    .setNodeId(Long.parseLong(nodeJson.getString("nodeId")))
                    .setNodeName(nodeJson.getString("nodeName"))
                    .setCreateTime(Long.parseLong(nodeJson.getString("createTime")))
                    .setUpdateTime(Long.parseLong(nodeJson.getString("updateTime")))
                    .build();
            res.add(node);
        }

        return res;
    }

    @Override
    public List<Common.Node> getNode(Long nodeId, String token) {
        List<Common.Node> res = new ArrayList<>();

        GetNode.GetNodeRequest req = GetNode.GetNodeRequest.newBuilder()
                .setNodeId(nodeId)
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .build();

        JSONObject jsonObject = sendData(req, 6);
        // 请求失败
        if(jsonObject==null) return null;

        JSONObject node = jsonObject.getJSONObject("node");

        // 拿到子节点数组，即文件数组
        JSONArray subNodeList = node.getJSONArray("subNodeList");
        // 没有文件节点
        if(subNodeList==null) return res;
        // 遍历所有文件节点
        for(int i=0;i<subNodeList.size();i++){
            JSONObject sonJson = subNodeList.getJSONObject(i);
            // 获取节点类型
            Common.NodeType realType;
            String nodeType = sonJson.getString("nodeType");
            switch (nodeType) {
                case "File": realType = Common.NodeType.File; break;
                case "Dir": realType = Common.NodeType.Dir; break;
                default: realType = Common.NodeType.Unknown; break;
            }
            // 构造子节点
            Common.Node son = Common.Node.newBuilder()
                    .setNodeType(realType)
                    .setNodeId(Long.parseLong(sonJson.getString("nodeId")))
                    .setNodeName(sonJson.getString("nodeName"))
                    .setCreateTime(Long.parseLong(sonJson.getString("createTime")))
                    .setUpdateTime(Long.parseLong(sonJson.getString("updateTime")))
                    .build();
            res.add(son);
        }
        return res;
    }

    @Override
    public int registerFile(Long fileId, Long dirId, ByteString secretKey, Boolean isWeb, Long shareId, String token) {
        RegisterFile.RegisterFileRequest req = RegisterFile.RegisterFileRequest.newBuilder()
                .setFileId(fileId)
                .setDirId(dirId)
                .setSecretKey(secretKey)
                .setIsWeb(isWeb)
                .setShareId(shareId)
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .build();

        return sendData(req, 7) == null ? 1 : 0;
    }

    @Override
    public int shareFile(String username, Long fileId, ByteString key, String token) {
        ShareFile.ShareFileRequest req = ShareFile.ShareFileRequest.newBuilder()
                .setUserName(username)
                .setFileId(fileId)
                .setKey(key)
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .build();

        return sendData(req, 8) == null ? 1 : 0;
    }

    @Override
    public List<GetRecvFile.SharedFile> getRecvFile(String token) {
        List<GetRecvFile.SharedFile> res = new ArrayList<>();

        GetRecvFile.GetRecvFileRequest req = GetRecvFile.GetRecvFileRequest.newBuilder()
                .setBaseReq(Common.BaseReq.newBuilder().setToken(token).build())
                .build();

        JSONObject jsonObject = sendData(req, 9);

        if(jsonObject==null) return null;

        JSONArray shareFileList = jsonObject.getJSONArray("shareFileList");

        if(shareFileList==null) return res;

        for(int i=0;i<shareFileList.size();i++){
            JSONObject shareFileJson = shareFileList.getJSONObject(i);

            GetRecvFile.SharedFile sharedFile = GetRecvFile.SharedFile.newBuilder()
                    .setShareId(shareFileJson.getLong("shareId"))
                    .setFileId(shareFileJson.getLong("fileId"))
                    .setFileName(shareFileJson.getString("fileName"))
                    .setKey(ByteString.copyFrom(shareFileJson.getBytes("key")))
                    .setFrom(shareFileJson.getString("from"))
                    .build();

            res.add(sharedFile);
        }
        return res;
    }

    private JSONObject sendData(Object req, int number) {
        String[] urls = {
                "ping",
                "/user/login",
                "/user/register",
                "/dir/create",
                "/file/upload",
                "/file/search",
                "/node/get",
                "/file/register",
                "/file/share",
                "/file/recv/get"
        };
        // 将请求对象转换成json格式，不要使用Gson
        String data = JsonUtils.toJson(req);

        // 向服务器发送请求并拿到返回值
        String respData = netWorkService.request(base + urls[number], data);

        // 将返回值解析成json格式
        JSONObject jsonObject = JSONObject.parseObject(respData);

        // 拿到返回信息
        JSONObject baseResp = jsonObject.getJSONObject("baseResp");
        String message = baseResp.getString("message");
        // 请求失败
        if(!"success".equals(message)) return null;

        return jsonObject;
    }
}