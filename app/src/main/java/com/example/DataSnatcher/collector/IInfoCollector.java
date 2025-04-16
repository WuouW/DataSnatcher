package com.example.DataSnatcher.collector;

import org.json.JSONObject;

public interface IInfoCollector {
    /* 注：如果需要Context或Activity，则在InfoCollectionManager中构建对象的时候，
    可以直接将其中的Context或Activity作为参数传入构造方法
     */

    // 获取信息的类型名称.
    String getCategory();

    // 采集信息，存放到JSONObject中。
    // key: 具体的信息名称；value: 数据
    JSONObject collect();
}
