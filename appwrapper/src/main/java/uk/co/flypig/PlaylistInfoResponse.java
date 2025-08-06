package uk.co.flypig;

/*
 * Created by David Llewellyn-Jones on 2025-08-06.
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

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.Description;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class PlaylistInfoResponse {
    public String originalUrl;
    public long streamCount;
    public Description description;
    public List<Image> thumbnails;
    public String uploaderUrl;
    public String uploaderName;
    public List<Image> uploaderAvatars;
    public String subChannelUrl;
    public String subChannelName;
    public List<Image> subChannelAvatars;
    public List<Image> banners;
    public PlaylistInfo.PlaylistType playlistType;
    public Page nextPage;
    public List<StreamInfoItem> relatedItems;
}

