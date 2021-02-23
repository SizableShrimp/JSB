/*
 * Copyright (c) 2021 SizableShrimp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.sizableshrimp.jsb.commands.utility.grid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.commands.AbstractCommand;
import me.sizableshrimp.jsb.commands.utility.mod.GetModCommand;
import me.sizableshrimp.jsb.data.Mod;
import me.sizableshrimp.jsb.util.CachedMap;
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.QTemplate;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;
import org.imgscalr.Scalr;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class GetGridCellCommand extends AbstractCommand {
    private static final int TARGET_SIZE = 128; // The pixel size to upscale all tilesheet images to for easier viewing
    private static final QTemplate LIST_TILES = new QTemplate(FL.pMap("list", "tiles"), "tslimit", "tiles");
    private static final QTemplate LIST_TILESHEETS = new QTemplate(FL.pMap("list", "tilesheets"), "tslimit", "tilesheets");
    private static final Pattern ILLEGAL_FILE_CHARS = Pattern.compile("[^a-zA-Z0-9.\\-]");
    private final CachedMap<String, Map<String, Tile>> cachedTiles = new CachedMap<>();
    private final CachedMap<String, Set<Integer>> cachedSizes = new CachedMap<>();

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <mod abbreviation> <item name>", """
                Get a grid cell from the specified mod.
                """);
    }

    @Override
    public String getName() {
        return "getgridcell";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("getgc", "gc", "getblock", "getitem", "p");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 2) {
            return incorrectUsage(context, event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String modAbbrv = args.getArg(0);
            Mod mod = Mod.getByAbbreviation(context.wiki(), modAbbrv);
            if (mod == null) {
                return GetModCommand.formatModDoesntExistMessage(channel, modAbbrv);
            }

            String item = args.getArgRange(1);

            Set<Integer> sizes = this.cachedSizes.getOrRetrieve(mod.getAbbrv(), k -> {
                List<JsonObject> reply = new WQuery(context.wiki(), 1, LIST_TILESHEETS).set("tsfrom", k).next().listComp("tilesheets");
                if (reply.isEmpty())
                    return null;
                Set<Integer> result = new HashSet<>();
                JsonObject obj = reply.get(0);
                if (!k.equals(GSONP.getStr(obj, "mod")))
                    return null;
                for (JsonElement size : obj.getAsJsonArray("sizes")) {
                    result.add(size.getAsInt());
                }
                return result.isEmpty() ? null : result;
            });
            if (sizes == null) {
                return sendMessage(String.format("A **%s** tilesheet could not be found.", mod.getAbbrv()), channel);
            }
            int size = sizes.stream().mapToInt(i -> i).max().getAsInt();
            Map<String, Tile> tiles = this.cachedTiles.getOrRetrieve(mod.getAbbrv(), k -> {
                List<JsonObject> reply = WikiUtil.getQueryRepliesAsList(LIST_TILES.createQuery(context.wiki()).set("tsmod", k), "tiles");
                Map<String, Tile> result = new HashMap<>();
                for (JsonObject obj : reply) {
                    Tile tile = new Tile(obj.get("id").getAsLong(), obj.get("mod").getAsString(), obj.get("name").getAsString(), obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt());
                    result.put(tile.name().toLowerCase(), tile);
                }
                return result;
            });
            Tile selected = tiles.get(item.toLowerCase());
            if (selected == null) {
                return sendMessage(String.format("The item specified (**%s**) does not exist in the **%s** tilesheet.", item, mod.getAbbrv()), channel);
            }

            String file = String.format("File:Tilesheet %s %d %d.png", mod.getAbbrv(), size, selected.z);
            String fileUrl = WikiUtil.getLatestFileUrl(context.wiki(), file);
            if (fileUrl == null)
                return sendMessage(String.format("The tilesheet file `%s` doesn't exist!", file), channel);

            InputStream image = getImage(fileUrl, selected, mod, size);
            if (image == null)
                return sendMessage("An error occurred when attempting to retrieve the tilesheet. Please try again later.", channel);

            String disambiguated = String.format("%s (%s)", selected.name(), mod.getName());
            String page = context.wiki().exists(disambiguated) ? disambiguated : selected.name();
            String pageUrl = WikiUtil.getBaseWikiPageUrl(context.wiki(), page);
            String attachment = ILLEGAL_FILE_CHARS.matcher(selected.name().replace(' ', '_') ).replaceAll("") + ".png";
            return channel.createMessage(message -> message.addFile(attachment, image)
                    .setEmbed(createRetrievalEmbed(embed -> embed.setImage("attachment://" + attachment)
                            .setTitle(selected.name())
                            .setUrl(pageUrl))));
        });
    }

    private InputStream getImage(String fileUrl, Tile tile, Mod mod, int size) {
        try {
            URL url = new URL(fileUrl);
            BufferedImage originalImage = ImageIO.read(url);
            if (originalImage == null) {
                Bot.LOGGER.warn("Original tilesheet image was null with tile {}, mod {}, and file url {}", tile, mod, fileUrl);
                return null;
            }

            int x = tile.x * size;
            int y = tile.y * size;
            BufferedImage cropped = Scalr.crop(originalImage, x, y, size, size);
            BufferedImage resultImage;
            if (TARGET_SIZE == size) {
                resultImage = cropped;
            } else {
                AffineTransform transform = new AffineTransform();
                double scale = TARGET_SIZE / (double) size;
                transform.scale(scale, scale);
                AffineTransformOp scaleOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

                resultImage = scaleOp.filter(cropped, new BufferedImage(TARGET_SIZE, TARGET_SIZE, cropped.getType()));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resultImage, "png", out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException | IllegalArgumentException | ImagingOpException e) {
            Bot.LOGGER.error("Error when reading tilesheet image with tile {}, mod {}, and file url {}", tile, mod, fileUrl, e);
            return null;
        }
    }

    public record Tile(long id, String mod, String name, int x, int y, int z) {}
}
