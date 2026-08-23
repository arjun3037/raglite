package com.raglite.chat;

import java.util.function.Consumer;

public interface ChatClient {

    void stream(String prompt, Consumer<String> onToken,
                Consumer<Throwable> onError, Runnable onComplete);
}
