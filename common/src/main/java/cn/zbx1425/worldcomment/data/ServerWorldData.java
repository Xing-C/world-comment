package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.UplinkDispatcher;
import cn.zbx1425.worldcomment.data.persist.FileSerializer;
import cn.zbx1425.worldcomment.data.sync.Synchronizer;
import cn.zbx1425.worldcomment.network.PacketEntryUpdateS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;

public class ServerWorldData {

    public static final Snowflake SNOWFLAKE = new Snowflake();

    public final MinecraftServer server;
    public final Path basePath;

    public boolean isHost;

    public final CommentCache comments = new CommentCache();

    public final FileSerializer fileSerializer;
    public final UplinkDispatcher uplinkDispatcher;
    public Synchronizer peerChannel;

    public ServerWorldData(MinecraftServer server, boolean isHost) {
        this.server = server;
        this.basePath = Path.of(server.getWorldPath(LevelResource.ROOT).toString(), "world-comment");
        fileSerializer = new FileSerializer(basePath);
        this.isHost = isHost;
        this.peerChannel = Synchronizer.NOOP;
        uplinkDispatcher = new UplinkDispatcher(Main.CONFIG.uplinkUrl.value);
    }

    public void load() throws IOException {
        if (isHost) {
            fileSerializer.loadInto(comments);
            peerChannel.kvWriteAll(comments.timeIndex);
        } else {
            peerChannel.kvReadAllInto(comments);
        }
    }

    public void insert(CommentEntry newEntry, boolean fromPeer) throws IOException {
        if (CommentCommand.isCommand(newEntry)) {
            if (isHost) {
                CommentCommand.executeCommand(newEntry, this);
            }
            return;
        }
        comments.insert(newEntry);
        if (isHost) {
            fileSerializer.insert(newEntry);
            uplinkDispatcher.insert(newEntry);
            peerChannel.kvWriteEntry(newEntry);
        }
        if (!fromPeer) {
            peerChannel.notifyInsert(newEntry);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketEntryUpdateS2C.send(player, newEntry, false);
        }
    }

    public void update(CommentEntry newEntry, boolean fromPeer) throws IOException {
        CommentEntry trustedEntry = comments.update(newEntry);
        if (isHost) {
            fileSerializer.update(trustedEntry);
            uplinkDispatcher.update(trustedEntry);
            peerChannel.kvWriteEntry(trustedEntry);
        }
        if (!fromPeer) {
            peerChannel.notifyUpdate(trustedEntry);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketEntryUpdateS2C.send(player, trustedEntry, true);
        }
    }
}
