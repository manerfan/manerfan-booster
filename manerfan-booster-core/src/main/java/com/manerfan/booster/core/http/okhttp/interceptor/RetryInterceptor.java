package com.manerfan.booster.core.http.okhttp.interceptor;

import java.io.IOException;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * RetryInterceptor
 *
 * <pre>
 *      重试
 * </pre>
 *
 * @author ManerFan
 * @date 2022/8/7
 */
@Slf4j
public class RetryInterceptor implements Interceptor {
    private static final int MAX_RETRY_COUNT = 3;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        boolean responseReady = false;
        int tryCount = 0;

        while (!responseReady && tryCount < MAX_RETRY_COUNT) {
            try {
                if (Objects.nonNull(response)) {
                    response.close();
                }
                response = chain.proceed(request);
                responseReady = response.isSuccessful();
            } catch (IOException ex) {
                log.warn("Request[{}] is not successful - retry {} times ", request.url(), tryCount, ex);
                if (tryCount >= MAX_RETRY_COUNT - 1) {
                    // 如果异常，最后再将异常抛出
                    throw ex;
                }
            } finally {
                tryCount++;
            }
        }

        // otherwise just pass the original response on
        return response;
    }
}
