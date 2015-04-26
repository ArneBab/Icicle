package net.pterodactylus.fcp;

import java.io.InputStream;
/**
 * The “TextFeed” messages contains N2N messages from peers. This code was taken from
 * net.pterodactylus.fcp.WatchGlobal and modified.
 *
 */
public class TextFeed extends BaseMessage {

    /** The payload. */
    private InputStream payloadInputStream;

    /**
     * Creates a “TextFeed” message that wraps the received message.
     *
     * @param receivedMessage
     *            The received message
     * @param payloadInputStream
     *            The payload
     */
    TextFeed(FcpMessage receivedMessage, InputStream payloadInputStream) {
        super(receivedMessage);
        this.payloadInputStream = payloadInputStream;
    }

    /**
     * Returns the length of the data.
     *
     * @return The length of the data, or <code>-1</code> if the length could
     *         not be parsed
     */
    public long getDataLength() {
        return FcpUtils.safeParseLong(getField("DataLength"));
    }
    public long getTextLength() {
        return FcpUtils.safeParseLong(getField("TextLength"));
    }
    public long getMessageTextLength() {
        return FcpUtils.safeParseLong(getField("MessageTextLength"));
    }

    public long getTimeReceived() {
        return FcpUtils.safeParseLong(getField("TimeReceived"));
    }

    public long getTimeSent() {
        return FcpUtils.safeParseLong(getField("TimeSent"));
    }

    public String getSourceNodeName() {
        return getField("SourceNodeName");
    }
    public String getHeader() {
        return getField("Header");
    }
    public String getShortText(){
        return getField("ShortText");
    }
    public String getField(String field){
        return super.getField(field);
    }

    /**
     * Returns the payload input stream. You <strong>have</strong> consume the
     * input stream before returning from the
     * {@link net.pterodactylus.fcp.FcpListener#receivedTextFeed(net.pterodactylus.fcp.FcpConnection, TextFeed)} method!
     *
     * @return The payload
     */
    public InputStream getPayloadInputStream() {
        return payloadInputStream;
    }

}
