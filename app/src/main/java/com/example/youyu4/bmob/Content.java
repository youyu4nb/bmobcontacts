package com.example.youyu4.bmob;

import cn.bmob.v3.BmobObject;

public class Content extends BmobObject {

    private String name;

    private String num;

    public Content() {

    }

    public Content(String name, String num) {
        this.name = name;
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }
}
