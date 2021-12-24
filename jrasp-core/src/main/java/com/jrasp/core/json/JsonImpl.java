package com.jrasp.core.json;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jrasp.api.json.JSONObject;

public class JsonImpl implements JSONObject {

    @Override
    public String toJSONString(Object object) {
        return com.alibaba.fastjson.JSONObject.toJSONString(object);
    }

    @Override
    public String toFormatJSONString(Object object) {
        return com.alibaba.fastjson.JSONObject.toJSONString(object, SerializerFeature.PrettyFormat);
    }

}
