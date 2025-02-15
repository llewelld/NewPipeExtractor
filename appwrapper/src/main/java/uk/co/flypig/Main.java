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
import okhttp3.OkHttpClient;
import java.io.IOException;

final class Main {
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
}

