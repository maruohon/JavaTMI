package net.blay09.javatmi;

public abstract class TMIAdapter implements TMIListener {
    @Override
    public void onUnhandledException(TMIClient client, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onChatMessage(TMIClient client, String channel, TwitchUser user, TwitchMessage message) {

    }

    @Override
    public void onResubscribe(TMIClient client, String channel, TwitchUser user, int months, String message) {

    }

    @Override
    public void onReconnectInbound(TMIClient client) {

    }

    @Override
    public void onWhisperMessage(TMIClient client, TwitchUser user, String message) {

    }

    @Override
    public void onServerMessage(TMIClient client, String channel, String messageId, String message) {

    }

    @Override
    public void onConnected(TMIClient client) {

    }

    @Override
    public void onDisconnected(TMIClient client) {

    }

    @Override
    public void onSubscribe(TMIClient client, String channel, String username, boolean prime) {

    }

    @Override
    public void onHost(TMIClient client, String channel, String username, int viewers) {

    }

    @Override
    public void onUnhost(TMIClient client, String channel, int viewers) {

    }

    @Override
    public void onTimeout(TMIClient client, String channel, String username) {

    }

    @Override
    public void onClearChat(TMIClient client, String channel) {

    }

    @Override
    public void onEmoteOnly(TMIClient client, String channel, boolean enabled) {

    }

    @Override
    public void onR9kBeta(TMIClient client, String channel, boolean enabled) {

    }

    @Override
    public void onSlowMode(TMIClient client, String channel, boolean enabled, int seconds) {

    }

    @Override
    public void onSubMode(TMIClient client, String channel, boolean enabled) {

    }

    @Override
    public void onHosted(TMIClient client, String channel, String username, int viewers) {

    }

    @Override
    public void onUserState(TMIClient client, String channel, TwitchUser user) {

    }

}
