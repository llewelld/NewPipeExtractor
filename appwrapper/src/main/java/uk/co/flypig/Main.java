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
import org.schabi.newpipe.extractor.stream.VideoStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.OkHttpClient;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
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

    private Main() {
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) {
        System.out.println("Initialising");
        final String url = args.length > 0
            ? args[0]
            : "https://www.youtube.com/watch?v=xvFZjo5PgG0";
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final DownloaderImpl downloader = DownloaderImpl.init(builder);
        NewPipe.init(downloader);

        try {
            System.out.println("Downloading video");
            System.out.println("URL: " + url);
            final StreamingService service = ServiceList.YouTube;
            final StreamExtractor extractor = service.getStreamExtractor(url);
            extractor.fetchPage();

            System.out.println("Video name: " + extractor.getName());
            System.out.println("Uploader: " + extractor.getUploaderName());
            System.out.println("Category: " + extractor.getCategory());
            System.out.println("Likes: " + extractor.getLikeCount());
            System.out.println("Views: " + extractor.getViewCount());

            final java.util.List<VideoStream> streams = extractor.getVideoStreams();
            for (final VideoStream stream : streams) {
                System.out.println("Content: " + stream.getContent());
            }

        } catch (final Exception e) {
            System.out.println("Exception: " + e);
        }

        try {
            downloader.teardown();
        } catch (final IOException e) {
            System.out.println("IOException: " + e);
        }

        System.out.println("Completed");
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

    private static HelloWorldOut helloWorld(HelloWorldIn input) {
        final String hello = "Hello " + input.name;
        final HelloWorldOut result = new HelloWorldOut();
        result.result = hello;
        return result;
    }

    private static HelloWorldIn worldHello(HelloWorldOut input) {
        final String hello = input.result + " Hello";
        HelloWorldIn result = new HelloWorldIn();
        result.name = hello;
        return result;
    }

    @CEntryPoint(name = "init")
    static void init(IsolateThread thread) {
        emptyString = CTypeConversion.toCString("").get();
        methodInfo = new HashMap<>();
        methodInfo.put("helloWorld", new MethodInfo<HelloWorldIn, HelloWorldOut>(Main::helloWorld, HelloWorldIn.class));
        methodInfo.put("worldHello", new MethodInfo<HelloWorldOut, HelloWorldIn>(Main::worldHello, HelloWorldOut.class));
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

