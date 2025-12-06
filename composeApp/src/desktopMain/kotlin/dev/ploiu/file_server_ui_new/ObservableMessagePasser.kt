package dev.ploiu.file_server_ui_new

/**
 * represents something that can pass messages to a `consumer`
 */
class ObservableMessagePasser {
    private val consumers: MutableSet<MessageObserver> = mutableSetOf()

    operator fun plusAssign(observer: MessageObserver) {
        consumers.add(observer)
    }

    operator fun minusAssign(observer: MessageObserver) {
        consumers.remove(observer)
    }

    fun passMessage(msg: MessageTypes) {
        for (consumer in consumers) {
            consumer(msg)
        }
    }
}

class MessageObserver(private val msgTypes: MessageTypes, private val fn: (MessageTypes) -> Unit) {
    operator fun invoke(msgType: MessageTypes) = if (msgType == this.msgTypes) fn(msgType) else Unit
}

enum class MessageTypes {
    FOCUS_SEARCHBAR,
    HIDE_ACTIVE_ELEMENT,
    NAVIGATE_FORWARD,
    NAVIGATE_BACKWARDS;

    operator fun invoke(handler: (MessageTypes) -> Unit) = MessageObserver(this, handler)
}
