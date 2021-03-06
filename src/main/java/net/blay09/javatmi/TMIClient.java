package net.blay09.javatmi;

import net.blay09.javairc.*;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TMIClient {
    private static final Pattern SUBSCRIBE_PATTERN = Pattern.compile("([^ ]+) just subscribed( with Twitch Prime)?!");
    private static final Pattern HOST_PATTERN = Pattern.compile("([^ ]+) is now hosting you(?: for)?([0-9]+)");

    private final TMIListener listener;
    private final IRCConnection client;
    private final TwitchCommands twitchCommands;

    public TMIClient(TMIListener listener) {
        this(getAnonymousUsername(), null, Collections.<String>emptyList(), listener);
    }

    public TMIClient(String username, String oauth, Collection<String> channels, TMIListener listener) {
        this(defaultBuilder()
                .nick(username)
                .password(oauth)
                .autoJoinChannels(channels)
                .build(), listener);
    }

    public TMIClient(final IRCConfiguration configuration, final TMIListener listener) {
        this.listener = listener;
        client = new IRCConnection(configuration, new IRCAdapter() {
            @Override
            public void onConnected(IRCConnection connection) {
                listener.onConnected(TMIClient.this);
            }

            @Override
            public void onDisconnected(IRCConnection connection) {
                listener.onDisconnected(TMIClient.this);
            }

            @Override
            public void onUnhandledException(IRCConnection connection, Exception e) {
                listener.onUnhandledException(TMIClient.this, e);
            }

            @Override
            public boolean onRawMessage(IRCConnection connection, IRCMessage message) {
                switch(message.getCommand()) {
                    case "HOSTTARGET": // channel, target & space & viewers
                        if(message.arg(1).charAt(0) == '-') {
                            listener.onUnhost(TMIClient.this, message.arg(0), tryParseInt(message.arg(1), 0));
                        } else {
                            String targetChannelAndViewers = message.arg(1);
                            String hostChannel;
                            int hostViewers;
                            int spaceIdx = targetChannelAndViewers.indexOf(' ');
                            if(spaceIdx != -1) {
                                hostChannel = targetChannelAndViewers.substring(0, spaceIdx);
                                hostViewers = tryParseInt(targetChannelAndViewers.substring(spaceIdx + 1), 0);
                            } else {
                                hostChannel = targetChannelAndViewers;
                                hostViewers = 0;
                            }
                            listener.onHost(TMIClient.this, message.arg(0), hostChannel, hostViewers);
                        }
                        break;
                    case "USERSTATE": // channel
                        TwitchUser userState = TwitchUser.fromMessage(message);
                        TwitchUser thisUser = new TwitchUser(new IRCUser(connection.getNick(), null, null));
                        thisUser.setColor(userState.getColor());
                        thisUser.setDisplayName(userState.getDisplayName());
                        thisUser.setSubscriber(userState.isSubscriber());
                        thisUser.setSubscribedMonths(userState.getSubscribedMonths());
                        thisUser.setMod(userState.isMod());
                        thisUser.setCheeredBits(userState.getCheeredBits());
                        thisUser.setTurbo(userState.isTurbo());
                        listener.onUserState(TMIClient.this, message.arg(0), thisUser);
                        break;
                    case "RECONNECT":
                        listener.onReconnectInbound(TMIClient.this);
                        break;
                    case "ROOMSTATE": // channel
                        // Listen for slow mode here to grab the time, as it's not sent within the notice
                        String slow = message.getTagByKey("slow");
                        if(slow != null && message.getTagByKey("subs-only") == null) { // Only trigger event if ROOMSTATE occured from change (will only contain changed tag); other tag checked is no relevant
                            int slowTime = Integer.parseInt(slow);
                            if(slowTime == 0) {
                                listener.onSlowMode(TMIClient.this, message.arg(0), false, 0);
                            } else {
                                listener.onSlowMode(TMIClient.this, message.arg(0), true, slowTime);
                            }
                        }
                        break;
                    case "USERNOTICE": // channel, [message]
                        String msgId = message.getTagByKey("msg-id");
                        if("resub".equals(msgId)) {
                            String login = message.getTagByKey("login");
                            String monthsTag = message.getTagByKey("msg-param-months");
                            int months = monthsTag != null ? Integer.parseInt(monthsTag) : 0;
                            String messageText = message.argCount() > 1 ? message.arg(1) : null;
                            listener.onResubscribe(TMIClient.this, message.arg(0), TwitchUser.fromMessageTags(message, login), months, messageText);
                        }
                        break;
                    case "CLEARCHAT": // channel, [username]
                        if(message.argCount() > 1) {
                            listener.onTimeout(TMIClient.this, message.arg(0), message.arg(1));
                        } else {
                            listener.onClearChat(TMIClient.this, message.arg(0));
                        }
                        break;
                    case "WHISPER": // username, message
                        listener.onWhisperMessage(TMIClient.this, TwitchUser.fromMessage(message), message.arg(1));
                        break;
                }
                return true;
            }

            @Override
            public void onChannelChat(IRCConnection connection, IRCMessage message, IRCUser user, String channel, String text) {
                if(user.getNick().equals("twitchnotify")) {
                    Matcher matcher = SUBSCRIBE_PATTERN.matcher(text);
                    if(matcher.find()) {
                        listener.onSubscribe(TMIClient.this, channel, matcher.group(1), matcher.group(2) != null);
                    }
                } else if(user.getNick().equals("jtv")) {
                    Matcher matcher = HOST_PATTERN.matcher(text);
                    if(matcher.find()) {
                        listener.onHosted(TMIClient.this, channel, matcher.group(1), matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0);
                    }
                } else {
                    boolean isAction = false;
                    if(text.startsWith("\u0001ACTION ") && text.endsWith("\u0001")) {
                        text = text.substring(8, text.length() - 1);
                        isAction = true;
                    }
                    String bitsTag = message.getTagByKey("bits");
                    int bits = (bitsTag != null && !bitsTag.isEmpty()) ? Integer.parseInt(bitsTag) : 0;
                    String channelTag = message.getTagByKey("room-id");
                    int channelId = (channelTag != null && !channelTag.isEmpty()) ? Integer.parseInt(channelTag): -1;
                    listener.onChatMessage(TMIClient.this, channel, TwitchUser.fromMessage(message), new TwitchMessage(text, channelId, isAction, bits));
                }
            }

            @Override
            public void onChannelNotice(IRCConnection connection, IRCMessage message, IRCUser user, String channel, String text) {
                String messageId = message.getTagByKey("msg-id");
                listener.onServerMessage(TMIClient.this, channel, messageId, text);
                switch(messageId) {
                    case "subs_on":
                        listener.onSubMode(TMIClient.this, channel, true);
                        break;
                    case "subs_off":
                        listener.onSubMode(TMIClient.this, channel, false);
                        break;
                    case "emote_only_on":
                        listener.onEmoteOnly(TMIClient.this, channel, true);
                        break;
                    case "emote_only_off":
                        listener.onEmoteOnly(TMIClient.this, channel, false);
                        break;
                    case "r9k_on":
                        listener.onR9kBeta(TMIClient.this, channel, true);
                        break;
                    case "r9k_off":
                        listener.onR9kBeta(TMIClient.this, channel, false);
                        break;
                }
            }
        });
        twitchCommands = new TwitchCommands(client);
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void connect() {
        client.start();
    }

    public void disconnect() {
        client.stop();
    }

    public void join(String channel) {
        if(!channel.startsWith("#")) {
            channel = "#" + channel;
        }
        client.sendRaw("JOIN " + channel.toLowerCase());
    }

    public void part(String channel) {
        if(!channel.startsWith("#")) {
            channel = "#" + channel;
        }
        client.sendRaw("PART " + channel.toLowerCase());
    }

    public void send(String channel, String message) {
        if(message.toLowerCase().startsWith("/me ")) {
            twitchCommands.action(channel, message.substring(4));
        } else {
            client.message(channel, message);
        }
    }

    public IRCConnection getIRCConnection() {
        return client;
    }

    public TwitchCommands getTwitchCommands() {
        return twitchCommands;
    }

    private static String getAnonymousUsername() {
        return "justinfan" + (int) (Math.floor((Math.random() * 80000) + 1000));
    }

    public static IRCConfiguration.IRCConfigurationBuilder defaultBuilder() {
        return IRCConfiguration.builder()
                .server("irc.chat.twitch.tv")
                .port(6667)
                .capability("twitch.tv/commands")
                .capability("twitch.tv/tags");
    }

    private static int tryParseInt(String s, int defaultVal) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
