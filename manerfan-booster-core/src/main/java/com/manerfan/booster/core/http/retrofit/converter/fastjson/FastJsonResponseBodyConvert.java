package com.manerfan.booster.core.http.retrofit.converter.fastjson;

import java.io.IOException;
import java.lang.reflect.Type;

import com.alibaba.fastjson.JSON;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * FastJsonResponseBodyConvert
 *
 * <pre>
 *      fastjson的响应体转换器
 * </pre>
 *
 * @author ManerFan
 * @date 2022/8/7
 */
final class FastJsonResponseBodyConvert<T> implements Converter<ResponseBody, T> {

    private Type type;

    public FastJsonResponseBodyConvert(Type type) {
        this.type = type;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        return JSON.parseObject(value.string(), type);
    }
}
