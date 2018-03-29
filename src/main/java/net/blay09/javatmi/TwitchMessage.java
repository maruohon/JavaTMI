package net.blay09.javatmi;

public class TwitchMessage {
    public String message;
    public int channelId;
    public boolean isAction;
    public int bits;

    public TwitchMessage(String message, int channelId, boolean isAction, int bits)
    {
        this.message = message;
        this.channelId = channelId;
        this.isAction = isAction;
        this.bits = bits;
    }

    public String getMessage()
    {
        return message;
    }

    public int getChannelId()
    {
        return channelId;
    }

    public boolean isAction()
    {
        return isAction;
    }

    public int getBits()
    {
        return bits;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setChannelId(int channelId)
    {
        this.channelId = channelId;
    }

    public void setAction(boolean isAction)
    {
        this.isAction = isAction;
    }

    public void setBits(int bits)
    {
        this.bits = bits;
    }
}
