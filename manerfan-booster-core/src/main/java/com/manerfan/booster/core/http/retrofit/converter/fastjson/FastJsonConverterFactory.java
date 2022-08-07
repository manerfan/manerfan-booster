package com.manerfan.booster.core.http.retrofit.converter.fastjson;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.alibaba.fastjson.serializer.SerializerFeature;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * FastJsonConverterFactory
 *
 * <pre>
 *      fastjson的json转换器
 * </pre>
 *
 * @author ManerFan
 * @date 2022/8/7
 */
public class FastJsonConverterFactory extends Converter.Factory {

    private final SerializerFeature[] serializeFeatures;

    private FastJsonConverterFactory(SerializerFeature... serializeFeatures) {
        this.serializeFeatures = serializeFeatures;
    }

    public static FastJsonConverterFactory create(SerializerFeature... serializeFeatures) {
        return new FastJsonConverterFactory(serializeFeatures);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(
        Type type, Annotation[] parameterAnnotations,
        Annotation[] methodAnnotations, Retrofit retrofit) {
        return new FastJsonRequestBodyConverter<>(serializeFeatures);
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new FastJsonResponseBodyConvert<>(type);
    }
}