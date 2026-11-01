package com.udesc.scriptumapp.ui;

final class Event<T> {
    private final T content;
    private boolean handled;

    Event(T content) {
        this.content = content;
    }

    T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }
}
