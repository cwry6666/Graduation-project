package com.graduate.design.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.graduate.design.R;
import com.graduate.design.activity.HomeActivity;
import com.graduate.design.utils.GraduateDesignApplication;
import com.graduate.design.utils.ToastUtils;

public class ChangePasswordFragment extends Fragment implements View.OnClickListener {
    private Button backButton;
    private ImageButton backImageButton;
    private Button confirmButton;
    private String token;
    private HomeActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化数据
        initData();
        // 拿到页面元素
        getComponentsById(view);
        // 设置监听事件
        setListeners();
    }

    private void initData(){
        token = GraduateDesignApplication.getToken();
        activity = (HomeActivity) getActivity();
    }

    private void getComponentsById(View view){
        backButton = view.findViewById(R.id.back_btn_disk);
        backImageButton = view.findViewById(R.id.back_image_btn_disk);
        confirmButton = view.findViewById(R.id.change_pwd_confirm_button);
    }

    private void setListeners(){
        backButton.setOnClickListener(this);
        backImageButton.setOnClickListener(this);
        confirmButton.setOnClickListener(this);
    }

    // 为按钮元素设置对应的点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back_btn_disk:
            case R.id.back_image_btn_disk:
                goBackToMine();
                break;
            case R.id.change_pwd_confirm_button:
                confirmChangePassword();
                break;
            default:
                ToastUtils.showShortToastCenter("错误的页面元素ID");
                break;
        }
    }

    // 返回到我的页面
    private void goBackToMine(){
        activity.getSupportFragmentManager().popBackStack();
    }

    private void confirmChangePassword(){
        // TODO 修改密码
    }
}
