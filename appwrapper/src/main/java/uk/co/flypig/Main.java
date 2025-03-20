package uk.co.flypig;

/*
 * Created by David Llewellyn-Jones on 2025-02-15.
 *
 * Copyright (C) 2025 David Llewellyn-Jones <david@flypig.co.uk>
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this code.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.OkHttpClient;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import com.fasterxml.jackson.core.type.TypeReference;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;

final class MethodInfo<In, Out> {
    public MethodInfo(Function<In, Out> method, Class<In> className) {
        this.method = method;
        this.className = className;
    }

    Function<In, Out> method;
    Class<In> className;
}

final class Main {
    static ObjectMapper jsonMapper = new JsonMapper();
    static Map<String, MethodInfo> methodInfo;
    static CCharPointer emptyString;
    static DownloaderImpl downloader = null;
    static HashMap<String, StreamingService> services;

    private Main() {
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) {
        throw new UnsupportedOperationException();
    }

    private static <In, Out> CCharPointer call(Function<In, Out> method, CCharPointer in, Class<In> className) {
        String output = new String();
        try {
            final String parameters = CTypeConversion.toJavaString(in);
            final In input = Main.jsonMapper.readValue(parameters, className);
            final Out result = method.apply(input);
            output = Main.jsonMapper.writeValueAsString(result);
        } catch (final JsonProcessingException e) {
            System.out.println("Exception: " + e);
        }

        final CTypeConversion.CCharPointerHolder holder = CTypeConversion.toCString(output);
        return holder.get();
    }

    private static StreamingService convertStringToService(final String service) {
        StreamingService serviceId = ServiceList.YouTube;
        if (services.containsKey(service)) {
            serviceId = services.get(service);
        }
        return serviceId;
    }

    private static DownloadExtracted downloadExtract(DownloadLocater download) {
        DownloadExtracted extracted = new DownloadExtracted();
        StreamingService service = convertStringToService(download.service);

        try {
            System.out.println("Downloading video");
            System.out.println("URL: " + download.url);
            final StreamExtractor extractor = service.getStreamExtractor(download.url);
            extractor.fetchPage();

            extracted.name = extractor.getName();
            extracted.uploaderName = extractor.getUploaderName();
            extracted.category = extractor.getCategory();
            extracted.likeCount = extractor.getLikeCount();
            extracted.viewCount = extractor.getViewCount();

            final java.util.List<VideoStream> videoStreams = extractor.getVideoStreams();
            for (final VideoStream stream : videoStreams) {
                extracted.content = stream.getContent();
            }
            if (extracted.content == null) {
                final java.util.List<AudioStream> audioStreams = extractor.getAudioStreams();
                for (final AudioStream stream : audioStreams) {
                    extracted.content = stream.getContent();
                }
            }
        }
        catch (final Exception e) {
            System.out.println("Exception: " + e);
        }

        return extracted;
    }

    private static ParamNone tearDown(ParamNone empty) {
        if (downloader != null) {
            try {
                downloader.teardown();
            }
            catch (final IOException e) {
                System.out.println("IOException in tearDown: " + e);
            }
            downloader = null;
        }
        return empty;
    }

    private static SuggestionsResponse getSuggestions(SuggestionsQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        List<String> suggestions = Collections.emptyList();
        try {
            final SuggestionExtractor extractor = serviceId.getSuggestionExtractor();
            if (extractor != null) {
                suggestions = extractor.suggestionList(query.query);
            }
        }
        catch (final IOException e) {
            System.out.println("IOException in getSuggestions: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in getSuggestions: " + e);
        }
        final SuggestionsResponse response = new SuggestionsResponse(suggestions);

        return response;
    }

    @CEntryPoint(name = "init")
    static void init(IsolateThread thread) {
        emptyString = CTypeConversion.toCString("").get();
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        methodInfo = new HashMap<>();
        methodInfo.put("downloadExtract", new MethodInfo<DownloadLocater, DownloadExtracted>(Main::downloadExtract, DownloadLocater.class));
        methodInfo.put("tearDown", new MethodInfo<ParamNone, ParamNone>(Main::tearDown, ParamNone.class));
        methodInfo.put("getSuggestions", new MethodInfo<SuggestionsQuery, SuggestionsResponse>(Main::getSuggestions, SuggestionsQuery.class));

        services = new HashMap<String, StreamingService>();
        services.put("Bandcamp", ServiceList.Bandcamp);
        services.put("MediaCCC", ServiceList.MediaCCC);
        services.put("PeerTube", ServiceList.PeerTube);
        services.put("SoundCloud", ServiceList.SoundCloud);
        services.put("YouTube", ServiceList.YouTube);

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        downloader = DownloaderImpl.init(builder);
        NewPipe.init(downloader);
    }

    @CEntryPoint(name = "invoke")
    public static CCharPointer invoke(IsolateThread thread, CCharPointer methodNamePtr, CCharPointer in) {
        final String methodName = CTypeConversion.toJavaString(methodNamePtr);
        CCharPointer result = emptyString;
        if (methodInfo.containsKey(methodName)) {
            final MethodInfo info = methodInfo.get(methodName);
            result = call(info.method, in, info.className);
        } else {
            System.out.println("Invocation failed, unknown method: " + methodName);
        }
        return result;
    }
}

