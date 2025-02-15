package uk.co.flypig;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public final class DownloaderImpl extends Downloader {
    /**
     * Should be the latest Firefox ESR version.
     */
    private static final String USER_AGENT
            = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0";
    private static DownloaderImpl instance;
    private final OkHttpClient client;
    private HashMap<String, String> cookies = new HashMap<String, String>();

    private static final String YOUTUBE_RESTRICTED_MODE_COOKIE_KEY = "youtube_restricted_mode_key";
    private static final String YOUTUBE_DOMAIN = "youtube.com";
    private static final String RECAPTCHA_COOKIES_KEY = "recaptcha_cookies";

    private String getCookies(final String url) {
        final List<String> resultCookies = new ArrayList<String>();
        if (url.contains(YOUTUBE_DOMAIN)) {
            final String youtubeCookie = getCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY);
            if (youtubeCookie != null) {
                resultCookies.add(youtubeCookie);
            }
        }
        final String recaptchaCookie = getCookie(RECAPTCHA_COOKIES_KEY);
        if (recaptchaCookie != null) {
            resultCookies.add(recaptchaCookie);
        }
        return concatCookies(resultCookies);
    }

    private String getCookie(final String key) {
        return cookies.get(key);
    }

    private DownloaderImpl(final OkHttpClient.Builder builder) {
        this.client = builder.readTimeout(30, TimeUnit.SECONDS).build();
    }

    private String concatCookies(final Collection<String> cookieStrings) {
        final HashSet<String> cookieSet = new HashSet<String>();
        for (final String cookieString: cookieStrings) {
            cookieSet.addAll(splitCookies(cookieString));
        }
        return String.join("; ", cookieSet).trim();
    }

    private Set<String> splitCookies(final String cookieString) {
        return new HashSet<String>(Arrays.asList(cookieString.split("; *")));
    }

    /**
     * In general there's no need to call teardown unless you really want
     * the app to shutdown immediately.
     */
    public void teardown() throws IOException {
        this.client.dispatcher().executorService().shutdown();
        this.client.connectionPool().evictAll();
        if (this.client.cache() != null) {
            this.client.cache().close();
        }
    }

    /**
     * It's recommended to call exactly once in the entire lifetime of the application.
     *
     * @param builder if null, default builder will be used
     * @return a new instance of {@link DownloaderImpl}
     */
    public static DownloaderImpl init(final OkHttpClient.Builder builder) {
        instance = new DownloaderImpl(
                builder != null ? builder : new OkHttpClient.Builder());
        return instance;
    }

    public static DownloaderImpl getInstance() {
        if (instance == null) {
            init(null);
        }
        return instance;
    }

    @Override
    public Response execute(final Request request)
            throws IOException, ReCaptchaException {
        final String httpMethod = request.httpMethod();
        final String url = request.url();
        final Map<String, List<String>> headers = request.headers();
        final byte[] dataToSend = request.dataToSend();

        RequestBody requestBody = null;
        if (dataToSend != null) {
            requestBody = RequestBody.create(null, dataToSend);
        }

        final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .method(httpMethod, requestBody).url(url)
                .addHeader("User-Agent", USER_AGENT);

        final String cookieString = getCookies(url);
        if (!cookieString.isEmpty()) {
            requestBuilder.addHeader("Cookie", cookieString);
        }

        for (final Map.Entry<String, List<String>> pair : headers.entrySet()) {
            final String headerName = pair.getKey();
            final List<String> headerValueList = pair.getValue();

            if (headerValueList.size() > 1) {
                requestBuilder.removeHeader(headerName);
                for (final String headerValue : headerValueList) {
                    requestBuilder.addHeader(headerName, headerValue);
                }
            } else if (headerValueList.size() == 1) {
                requestBuilder.header(headerName, headerValueList.get(0));
            }

        }

        final okhttp3.Response response = client.newCall(requestBuilder.build()).execute();

        if (response.code() == 429) {
            response.close();

            throw new ReCaptchaException("reCaptcha Challenge requested", url);
        }

        final ResponseBody body = response.body();
        String responseBodyToReturn = null;

        if (body != null) {
            responseBodyToReturn = body.string();
        }

        final String latestUrl = response.request().url().toString();
        return new Response(response.code(), response.message(), response.headers().toMultimap(),
                responseBodyToReturn, latestUrl);
    }
}
