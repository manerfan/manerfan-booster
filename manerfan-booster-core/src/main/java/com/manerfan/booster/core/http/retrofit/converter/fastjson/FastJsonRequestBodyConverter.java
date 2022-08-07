package com.manerfan.booster.core.http.retrofit.converter.fastjson;

import java.io.IOException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

/**
 * FastJsonRequestBodyConverter
 *
 * <pre>
 *      fastjson的请求体转换器
 * </pre>
 *
 * @author ManerFan
 * @date 2022/8/7
 */
final class FastJsonRequestBodyConverter<T> implements Converter<T, RequestBody> {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

    private SerializerFeature[] serializeFeatures;

    public FastJsonRequestBodyConverter(SerializerFeature... serializeFeatures) {
        this.serializeFeatures = serializeFeatures;
    }

    @Override
    public RequestBody convert(T value) throws IOException {
        return RequestBody.create(JSON.toJSONBytes(value, serializeFeatures), MEDIA_TYPE);
    }
}
