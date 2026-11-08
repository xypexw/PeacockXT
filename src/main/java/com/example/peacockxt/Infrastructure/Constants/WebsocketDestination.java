package com.example.peacockxt.Infrastructure.Constants;

public enum WebsocketDestination {

    // 1. TEAM LEVEL (Used for: Team updates, Memberships, Custom Emojis, Events, Posts)
    TEAM("/topic/team.%s"),

    // 2. CHANNEL LEVEL (Used for: Messages within a specific Channel)
    CHANNEL("/topic/channel.%s"),

    // 3. POST LEVEL (Used for: Comments within a specific Post)
    POST("/topic/post.%s"),

    // 4. PRIVATE SESSION LEVEL (Used for: Direct Messages in 1-on-1 chats)
    PRIVATE_SESSION("/topic/session.%s"),

    // 5. USER LEVEL (Used for: Personal notifications such as being kicked, new invites...)
    USER("/topic/user.%s");

    private final String pattern;

    WebsocketDestination(String pattern) {
        this.pattern = pattern;
    }

    /**
     * Automatically injects the ID into the pattern string to generate the Destination/RoutingKey.
     * Example: WebsocketDestination.TEAM.getDestination(123) -> "/topic/team.123"
     */
    public String getDestination(Object id) {
        return String.format(this.pattern, id);
    }
}
