package com.github.peacockxt.infrastructure.constants;

import java.time.Duration;

public enum RedisKey {

    //USER & AUTHENTICATION
    USER_PROFILE("peacockxt:user:%s:profile", DataType.VALUE, Duration.ofDays(7)),
    USER_MAPPING_USERNAME("peacockxt:user:mapping:username:%s", DataType.VALUE, Duration.ofDays(7)),
    SESSION_TOKEN("peacockxt:session:token:%s", DataType.VALUE, Duration.ofMinutes(15)),
    REFRESH_TOKEN("peacockxt:session:refresh_token:%s", DataType.VALUE, Duration.ofDays(7)),
    USER_ONLINE("peacockxt:user:%s:online", DataType.VALUE, Duration.ofSeconds(10)),
    //TEAM & MEMBERSHIP
    TEAM_INFO("peacockxt:team:%s:info", DataType.VALUE, Duration.ofDays(3)),
    MBS_IN_TEAMS("peacockxt:teams:%s:mbs", DataType.SET, Duration.ofDays(7)),
    USER_MBS("peacockxt:user:%s:mbs", DataType.SET, Duration.ofDays(7)),
    MBS_INFO("peacockxt:mbs:%s:info", DataType.VALUE, Duration.ofDays(30)),

    // CHANNEL
    TEAM_CHANNELS("peacockxt:team:%s:channels", DataType.ZSET, Duration.ofDays(7)),
    CHANNEL_INFO("peacockxt:channel:%s:info", DataType.VALUE, Duration.ofDays(7)),

    // PRIVATE SESSION (CHAT 1-1)
    USER_INBOX_INDEX("peacockxt:user:%s:inbox", DataType.ZSET, Duration.ofDays(7)),
    PRIVATE_SESSION_INFO("peacockxt:private_session:%s:info", DataType.VALUE, Duration.ofDays(7)),

    // MESSAGE (CHANNEL)
    CHANNEL_MSG_INDEX("peacockxt:channel:%s:msg_index", DataType.ZSET, Duration.ofDays(3)),
    MESSAGE_DATA("peacockxt:message:%s", DataType.VALUE, Duration.ofDays(7)),

    // DIRECT MESSAGE
    SESSION_MSG_INDEX("peacockxt:session:%s:msg_index", DataType.ZSET, Duration.ofDays(3)),
    DIRECT_MESSAGE_DATA("peacockxt:direct_message:%s", DataType.VALUE, Duration.ofDays(7)),

    // EMOJI
    TEAM_EMOJIS("peacockxt:team:%s:emojis", DataType.HASH, Duration.ofDays(7)),

    // EVENT
    TEAM_EVENT_INDEX("peacockxt:team:%s:event_index", DataType.ZSET, Duration.ofDays(3)),
    EVENT_DATA("peacockxt:event:%s", DataType.VALUE, Duration.ofDays(7)),
    EVENT_REGISTRATION_COUNT("peacockxt:event:%s:reg_count", DataType.VALUE, Duration.ofDays(7)),

    // POST & COMMENT
    TEAM_POST_INDEX("peacockxt:team:%s:post_index", DataType.ZSET, Duration.ofDays(3)),
    POST_DATA("peacockxt:post:%s", DataType.VALUE, Duration.ofDays(7)),
    POST_COMMENT_INDEX("peacockxt:post:%s:comment_index", DataType.ZSET, Duration.ofDays(3)),
    COMMENT_DATA("peacockxt:comment:%s", DataType.VALUE, Duration.ofDays(7)),

    // TICKET
    WS_TICKET("peacockxt:ws_ticket:%s", DataType.VALUE, Duration.ofSeconds(30));

    // Declare Data Type for Redis
    public enum DataType {
        VALUE, HASH, LIST, SET, ZSET
    }
    private final String pattern;
    private final DataType type;
    private final Duration baseTtl;

    RedisKey(String pattern, DataType type, Duration ttl) {
        this.pattern = pattern;
        this.type = type;
        this.baseTtl = ttl;
    }

    public String build(Object... args) {
        return String.format(this.pattern, args);
    }

    public DataType getKeyType() {
        return type;
    }

    public Duration getTtl() {
        return this.baseTtl;
    }
}
