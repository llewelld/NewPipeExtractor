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
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.OkHttpClient;
import okhttp3.ConnectionSpec;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.function.Function;
import com.fasterxml.jackson.core.type.TypeReference;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;

// Reflection
import io.micronaut.core.annotation.TypeHint;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.timeago.patterns.en_GB;
import org.schabi.newpipe.extractor.MetaInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import java.time.OffsetDateTime;

// offsetDateTime
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.PropertyName;
import java.lang.reflect.Method;
import java.lang.reflect.AnnotatedElement;

@TypeHint(
    value = {
        Page.class,
        InfoItem.class,
        StreamInfoItem.class,
        ChannelInfoItem.class,
        PlaylistInfoItem.class,
        Image.class,
        en_GB.class,
        MetaInfo.class,
        CommentsInfoItem.class,
        Description.class,
        DateWrapper.class,
        OffsetDateTime.class,
        LinkHandler.class,
        ListLinkHandler.class
    },
    accessType = {
        TypeHint.AccessType.ALL_PUBLIC_CONSTRUCTORS,
        TypeHint.AccessType.ALL_DECLARED_FIELDS,
        TypeHint.AccessType.ALL_DECLARED_METHODS
    }
)

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

    @CEntryPoint(name = "init")
    static void init(IsolateThread thread) {
        // Set up static data
        emptyString = CTypeConversion.toCString("").get();
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Allow DateWrapper.offsetDateTime to be serialised
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public PropertyName findNameForSerialization(Annotated annotated) {
                PropertyName result = super.findNameForSerialization(annotated);
                if (result == null) {
                    final AnnotatedElement classMember = annotated.getAnnotated();
                    if (classMember instanceof Method) {
                        if (((Method)classMember).getName() == "offsetDateTime") {
                            result = new PropertyName("offsetDateTime");
                        }
                    }
                }
                return result;
            }
        });

        // Create the HTTP client
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = builder
            .connectionSpecs(Arrays.asList(ConnectionSpec.RESTRICTED_TLS))
            .build();
        downloader = DownloaderImpl.init(builder);
        NewPipe.init(downloader);

        // The available services
        services = new HashMap<String, StreamingService>();
        services.put("Bandcamp", ServiceList.Bandcamp);
        services.put("MediaCCC", ServiceList.MediaCCC);
        services.put("PeerTube", ServiceList.PeerTube);
        services.put("SoundCloud", ServiceList.SoundCloud);
        services.put("YouTube", ServiceList.YouTube);

        // The exposed functions
        methodInfo = new HashMap<>();
        methodInfo.put("downloadExtract", new MethodInfo<DownloadLocater,
            DownloadExtracted>(Main::downloadExtract, DownloadLocater.class)
        );
        methodInfo.put("tearDown", new MethodInfo<ParamNone,
            ParamNone>(Main::tearDown, ParamNone.class)
        );
        methodInfo.put("getSuggestions", new MethodInfo<SuggestionsQuery,
            SuggestionsResponse>(Main::getSuggestions, SuggestionsQuery.class)
        );
        methodInfo.put("searchFor", new MethodInfo<SearchQuery,
            SearchInfoResponse>(Main::searchFor, SearchQuery.class)
        );
        methodInfo.put("getMoreSearchItems", new MethodInfo<SearchQuery,
            InfoItemsPageResponse>(Main::getMoreSearchItems, SearchQuery.class)
        );
        methodInfo.put("getAvailableContentFilter", new MethodInfo<ParamString,
            ParamStringList>(Main::getAvailableContentFilter, ParamString.class)
        );
        methodInfo.put("getCommentsInfo", new MethodInfo<CommentsInfoQuery,
            CommentsInfoResponse>(Main::getCommentsInfo, CommentsInfoQuery.class)
        );
        methodInfo.put("getMoreCommentItems", new MethodInfo<CommentsInfoQuery,
            InfoItemsPageCommentsResponse>(Main::getMoreCommentItems, CommentsInfoQuery.class)
        );
        methodInfo.put("getChannelInfo", new MethodInfo<ChannelInfoQuery,
            ChannelInfoResponse>(Main::getChannelInfo, ChannelInfoQuery.class)
        );
        methodInfo.put("getChannelTabInfo", new MethodInfo<ChannelTabInfoQuery,
            ChannelTabInfoResponse>(Main::getChannelTabInfo, ChannelTabInfoQuery.class)
        );
        methodInfo.put("getMoreChannelItems", new MethodInfo<ChannelMoreItemsQuery,
            ChannelMoreItemsResponse>(Main::getMoreChannelItems, ChannelMoreItemsQuery.class)
        );
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

    /*
    * The exposed NewPipe Extractor API starts here
    */

    private static DownloadExtracted downloadExtract(DownloadLocater download) {
        DownloadExtracted extracted = new DownloadExtracted();
        StreamingService service = convertStringToService(download.service);

        try {
            final StreamExtractor extractor = service.getStreamExtractor(download.url);
            extractor.fetchPage();

            extracted.name = extractor.getName();
            extracted.uploaderName = extractor.getUploaderName();
            extracted.category = extractor.getCategory();
            extracted.likeCount = extractor.getLikeCount();
            extracted.viewCount = extractor.getViewCount();
            extracted.uploadDate = extractor.getUploadDate().offsetDateTime().toEpochSecond();
            extracted.description = extractor.getDescription();
            extracted.length = extractor.getLength();
            extracted.licence = extractor.getLicence();

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

    private static SearchInfoResponse searchFor(SearchQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        SearchInfoResponse response = new SearchInfoResponse();
        try {
            SearchInfo result = SearchInfo.getInfo(
                serviceId,
                serviceId.getSearchQHFactory().fromQuery(
                    query.searchString,
                    query.contentFilter,
                    query.sortFilter
                )
            );
            final List<Throwable> exceptions = result.getErrors();
            if (!exceptions.isEmpty()) {
                System.out.println("Search exceptions: " + exceptions.size());
                System.out.println("Search exception: " + exceptions.get(0));
                System.out.println("Search exception stack trace:");
                exceptions.get(0).printStackTrace();
            }
            response = new SearchInfoResponse(
                result.getSearchString(),
                result.getSearchSuggestion(),
                result.isCorrectedSearch(),
                result.getNextPage(),
                result.getContentFilters(),
                result.getSortFilter(),
                result.getRelatedItems(),
                result.getMetaInfo()
            );
        }
        catch (final IOException e) {
            System.out.println("IOException in searchFor: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in searchFor: " + e);
        }

        return response;
    }

    private static InfoItemsPageResponse getMoreSearchItems(SearchQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        InfoItemsPageResponse response = new InfoItemsPageResponse();
        try {
            InfoItemsPage result = SearchInfo.getMoreItems(
                serviceId,
                serviceId.getSearchQHFactory().fromQuery(
                    query.searchString,
                    query.contentFilter,
                    query.sortFilter
                ),
                query.page
            );
            response = new InfoItemsPageResponse(
                result.getNextPage(),
                result.getItems()
            );
        }
        catch (final IOException e) {
            System.out.println("IOException in getMoreSearchItems: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in getMoreSearchItems: " + e);
        }

        return response;
    }

    private static ParamStringList getAvailableContentFilter(ParamString service) {
        StreamingService serviceId = convertStringToService(service.string);
        ParamStringList result = new ParamStringList();
        result.stringList = serviceId.getSearchQHFactory().getAvailableContentFilter();
        return result;
    }

    private static CommentsInfoResponse getCommentsInfo(CommentsInfoQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        CommentsInfoResponse response = new CommentsInfoResponse();
        try {
            CommentsInfo result = CommentsInfo.getInfo(serviceId, query.url);
            if (result != null) {
                final List<Throwable> exceptions = result.getErrors();
                if (!exceptions.isEmpty()) {
                    System.out.println("Comments exceptions: " + exceptions.size());
                    System.out.println("Comments exception: " + exceptions.get(0));
                    System.out.println("Comments exception stack trace:");
                    exceptions.get(0).printStackTrace();
                }
                response = new CommentsInfoResponse(
                    result.getNextPage(),
                    result.getContentFilters(),
                    result.getSortFilter(),
                    result.getRelatedItems()
                );
            }
        }
        catch (final IOException e) {
            System.out.println("IOException in getCommentsInfo: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in getCommentsInfo: " + e);
        }

        return response;
    }

    private static InfoItemsPageCommentsResponse getMoreCommentItems(CommentsInfoQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        InfoItemsPageCommentsResponse response = new InfoItemsPageCommentsResponse();
        try {
            InfoItemsPage<CommentsInfoItem> result = CommentsInfo.getMoreItems(
                serviceId,
                query.url,
                query.page
            );
            response = new InfoItemsPageCommentsResponse(
                result.getNextPage(),
                result.getItems()
            );
        }
        catch (final IOException e) {
            System.out.println("IOException in getMoreCommentItems: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in getMoreCommentItems: " + e);
        }

        return response;
    }

    public static ChannelInfoResponse getChannelInfo(ChannelInfoQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        ChannelInfoResponse response = new ChannelInfoResponse();

        try {
            ChannelInfo result = ChannelInfo.getInfo(
                serviceId,
                query.url
            );
            response = new ChannelInfoResponse(
                result.getId(),
                result.getName(),
                result.getUrl(),
                result.getDescription(),
                result.getTabs()
            );
        }
        catch (final IOException e) {
            System.out.println("IOException in getChannelInfo: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in getChannelInfo: " + e);
        }

        return response;
    }

    public static ChannelTabInfoResponse getChannelTabInfo(ChannelTabInfoQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        ChannelTabInfoResponse response = new ChannelTabInfoResponse();

        try {
            ListLinkHandler linkHandler = new ListLinkHandler(query.originalUrl, query.url, query.id, query.contentFilters, query.sortFilter);
            ChannelTabInfo result = ChannelTabInfo.getInfo(
                serviceId,
                linkHandler
            );
            response = new ChannelTabInfoResponse(
                result.getRelatedItems(),
                result.getNextPage(),
                result.getContentFilters(),
                result.getSortFilter()
            );
        }
        catch (final IOException e) {
            System.out.println("IOException in getChannelTabInfo: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in getChannelTabInfo: " + e);
        }

        return response;
    }

    private static ChannelMoreItemsResponse getMoreChannelItems(ChannelMoreItemsQuery query) {
        StreamingService serviceId = convertStringToService(query.service);

        ChannelMoreItemsResponse response = new ChannelMoreItemsResponse();
        try {
            ListLinkHandler linkHandler = new ListLinkHandler(query.originalUrl, query.url, query.id, query.contentFilters, query.sortFilter);

            ListExtractor.InfoItemsPage<InfoItem> result = ChannelTabInfo.getMoreItems(serviceId, linkHandler, query.page);
            if (result != null) {
                final List<Throwable> exceptions = result.getErrors();
                if (!exceptions.isEmpty()) {
                    System.out.println("Channel Tab exceptions: " + exceptions.size());
                    System.out.println("Channel Tab exception: " + exceptions.get(0));
                    System.out.println("Channel Tab exception stack trace:");
                    exceptions.get(0).printStackTrace();
                }

                response = new ChannelMoreItemsResponse(
                    result.getItems(),
                    result.getNextPage()
                );
            }
        }
        catch (final IOException e) {
            System.out.println("IOException in getMoreChannelItems: " + e);
        }
        catch (final ExtractionException e) {
            System.out.println("ExtractionException in getMoreChannelItems: " + e);
        }

        return response;
    }
}

