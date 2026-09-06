package ua.nanit.limbo.protocol.packets.status;

import ua.nanit.limbo.protocol.ByteMessage;
import ua.nanit.limbo.protocol.PacketOut;
import ua.nanit.limbo.protocol.registry.Version;
import ua.nanit.limbo.server.LimboServer;

public class PacketStatusResponse implements PacketOut {
    // 强制返回 5 个常驻假人和在线人数 5，欺骗外部探针和面板
    private static final String TEMPLATE = "{ \"version\": { \"name\": \"%s\", \"protocol\": %d }, \"players\": { \"max\": %d, \"online\": 5, \"sample\": ["
            + "{\"name\": \"com@fghk\", \"id\": \"00000000-0000-0000-0000-000000000001\"},"
            + "{\"name\": \"com@fghk_2\", \"id\": \"00000000-0000-0000-0000-000000000002\"},"
            + "{\"name\": \"Bot_A3\", \"id\": \"00000000-0000-0000-0000-000000000003\"},"
            + "{\"name\": \"Bot_B4\", \"id\": \"00000000-0000-0000-0000-000000000004\"},"
            + "{\"name\": \"Bot_C5\", \"id\": \"00000000-0000-0000-0000-000000000005\"}"
            + "] }, \"description\": %s }";

    private LimboServer server;

    public PacketStatusResponse() { }

    public PacketStatusResponse(LimboServer server) {
        this.server = server;
    }

    @Override
    public void encode(ByteMessage msg, Version version) {
        int protocol;
        int staticProtocol = server.getConfig().getPingData().getProtocol();

        if (staticProtocol > 0) {
            protocol = staticProtocol;
        } else {
            protocol = server.getConfig().getInfoForwarding().isNone()
                    ? version.getProtocolNumber()
                    : Version.getMax().getProtocolNumber();
        }

        String ver = server.getConfig().getPingData().getVersion();
        String desc = server.getConfig().getPingData().getDescription();

        msg.writeString(getResponseJson(ver, protocol, server.getConfig().getMaxPlayers(), desc));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private String getResponseJson(String version, int protocol, int maxPlayers, String description) {
        return String.format(TEMPLATE, version, protocol, maxPlayers, description);
    }
}
