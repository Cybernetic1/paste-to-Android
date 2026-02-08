package com.paste.android;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public class SSHClientHelper {
    private static final int TIMEOUT = 30000; // 30 seconds

    public interface TransferCallback {
        void onSuccess(String filename);
        void onError(String error);
    }

    public static void sendClipboardToLinux(
            LinuxDestination destination,
            String privateKeyPath,
            String clipboardContent,
            TransferCallback callback) {

        new Thread(() -> {
            Session session = null;
            ChannelSftp channelSftp = null;

            try {
                // Generate filename with timestamp
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String filename = "clipboard_" + timestamp + ".txt";

                // Setup JSch (modern fork with algorithm support)
                JSch jsch = new JSch();
                jsch.addIdentity(privateKeyPath);

                // Create session
                session = jsch.getSession(destination.getUser(), destination.getHost(), destination.getPort());
                
                // Configure for modern SSH servers
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                // Modern fork supports newer algorithms automatically
                
                session.setConfig(config);
                session.setTimeout(TIMEOUT);

                // Connect
                session.connect();

                // Open SFTP channel
                Channel channel = session.openChannel("sftp");
                channel.connect();
                channelSftp = (ChannelSftp) channel;

                // Change to target directory
                channelSftp.cd(destination.getDirectory());

                // Upload file
                InputStream inputStream = new ByteArrayInputStream(clipboardContent.getBytes("UTF-8"));
                channelSftp.put(inputStream, filename);

                callback.onSuccess(filename);

            } catch (JSchException e) {
                callback.onError("SSH Error: " + e.getMessage());
            } catch (SftpException e) {
                callback.onError("SFTP Error: " + e.getMessage());
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            } finally {
                if (channelSftp != null && channelSftp.isConnected()) {
                    channelSftp.disconnect();
                }
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }).start();
    }
}
