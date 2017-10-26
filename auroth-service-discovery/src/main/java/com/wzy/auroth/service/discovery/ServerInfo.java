package com.wzy.auroth.service.discovery;

public interface ServerInfo {

    boolean isRunning();

    String getHost();

    int getPort();
}
