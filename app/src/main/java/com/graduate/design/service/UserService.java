package com.graduate.design.service;

import com.google.protobuf.ByteString;
import com.graduate.design.proto.Common;
import com.graduate.design.proto.FileUpload;
import com.graduate.design.proto.GetRecvFile;
import com.graduate.design.proto.SearchFile;
import com.graduate.design.proto.SendSearchToken;

import java.util.List;

public interface UserService {
    int ping(String name, String token);

    int login(String username, String password);

    int register(String username, String password, String email, String biIndex);

    int createDir(String dirName, Long parentId, String token);

    int uploadFile(String fileName, Long parentId, List<FileUpload.indexToken> indexList,
                   ByteString content, String biIndex, Long fileId, String token);

    List<Common.Node> searchFile(List<Long> idList, String token);

    int registerFile(Long fileId, Long dirId, ByteString secretKey,
                  Boolean isWeb, Long shareId, String token);

    List<Common.Node> getNodeList(Long nodeId, String token);
    String getNodeContent(Long nodeId, String token);

    int shareFile(String username, Long fileId, ByteString key, String token);

    List<GetRecvFile.SharedFile> getRecvFile(String token);

    Long getNodeId(String token);

    List<String> sendSearchToken(SendSearchToken.SearchToken searchToken, String token);
}
