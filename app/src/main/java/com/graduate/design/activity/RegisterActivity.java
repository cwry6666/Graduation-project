package com.graduate.design.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.protobuf.ByteString;
import com.graduate.design.R;
import com.graduate.design.entity.BiIndex;
import com.graduate.design.service.EncryptionService;
import com.graduate.design.service.UserService;
import com.graduate.design.service.impl.EncryptionServiceImpl;
import com.graduate.design.service.impl.UserServiceImpl;
import com.graduate.design.utils.ActivityJumpUtils;
import com.graduate.design.utils.FileUtils;
import com.graduate.design.utils.GraduateDesignApplication;
import com.graduate.design.utils.InitViewUtils;
import com.graduate.design.utils.ToastUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText usernameInRegister;
    private EditText passwordInRegister;
    private EditText emailInRegister;
    private Button registerBtnInRegister;
    private Button gotoLoginBtn;

    private UserService userService;
    private EncryptionService encryptionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化页面
        InitViewUtils.initView(this);
        // 初始化数据
        initData();
        // 拿到页面元素
        getComponentsById();
        // 设置监听事件
        setListeners();
    }

    private void initData(){
        userService = new UserServiceImpl();
        encryptionService = new EncryptionServiceImpl();
    }

    private void getComponentsById(){
        usernameInRegister = findViewById(R.id.username_register);
        passwordInRegister = findViewById(R.id.password_register);
        emailInRegister = findViewById(R.id.email_register);
        registerBtnInRegister = findViewById(R.id.register_btn);
        gotoLoginBtn = findViewById(R.id.goto_login_btn);
    }

    private void setListeners(){
        registerBtnInRegister.setOnClickListener(this);
        gotoLoginBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.register_btn:
                register();
                break;
            case R.id.goto_login_btn:
                gotoLogin();
                break;
            default:
                ToastUtils.showShortToastCenter("错误的页面元素ID");
                break;
        }
    }

    private void register(){
        String username = usernameInRegister.getText().toString().trim();
        String password = passwordInRegister.getText().toString().trim();
        String email = emailInRegister.getText().toString().trim();

        // 判断注册信息是否合法
        // 用户名只能包含字母、数字、下划线、连字符，且长度为7到14个字符
        String usernameRegex = "^[a-zA-Z0-9_]{7,14}$";
        if(!Pattern.matches(usernameRegex, username)){
            ToastUtils.showShortToastCenter("用户名只能包含字母、数字、下划线，长度为7~14");
            clearEditText();
            return;
        }

        // 密码不能为空
        if(password.length()==0){
            ToastUtils.showShortToastCenter("密码不能为空");
            clearEditText();
            return;
        }

        // 验证邮箱是否合法
        String emailRegex = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$";
        if(!Pattern.matches(emailRegex, email)){
            ToastUtils.showShortToastCenter("邮箱格式错误");
            clearEditText();
            return;
        }

        // 使用SHA256生成用户主密钥
        byte[] mainSecret = encryptionService.getSecretKey(username, password);
        // 截取前16个字节给用户密码加密
        byte[] key1 = new byte[16];
        for(int i=0;i<16;i++) key1[i] = mainSecret[i];
        // 用主密钥加密用户密码后上传
        String encryptPassword = FileUtils.bytes2Base64(encryptionService.encryptByAES128(password, key1));
        // 给注册用户新建一个双向索引表上传
        BiIndex biIndex = new BiIndex();
        String biIndexString = FileUtils.bytes2Base64(biIndex.writeObject());

        int res = userService.register(username, encryptPassword, email, biIndexString);

        // 请求失败
        if(res==1){
            ToastUtils.showShortToastCenter("用户名已存在!");
            return;
        }

        ToastUtils.showShortToastCenter("注册成功!");

        // 请求成功，跳转到登录页面
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        ActivityJumpUtils.jumpActivity(RegisterActivity.this, intent, 100L, true);
    }

    private void clearEditText(){
        usernameInRegister.setText("");
        passwordInRegister.setText("");
        emailInRegister.setText("");
    }

    private void gotoLogin(){
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        ActivityJumpUtils.jumpActivity(RegisterActivity.this, intent, 100L, true);
    }
}
