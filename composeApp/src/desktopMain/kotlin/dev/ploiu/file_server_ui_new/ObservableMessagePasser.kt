package dev.ploiu.file_server_ui_new

/**
 * represents something that can pass messages to a `consumer`
 */
class ObservableMessagePasser {
    private val consumers: MutableSet<MessageObserver> = mutableSetOf()

    fun passMessage(msg: MessageTypes) {
        for (consumer in consumers) {
            consumer(msg)
        }
    }

    infix fun handles(observer: MessageObserver) {
        consumers.add(observer)
    }

    infix fun ignores(observer: MessageObserver) {
        consumers.remove(observer)
    }
}

class MessageObserver(private val msgTypes: MessageTypes, private val fn: (MessageTypes) -> Unit) {
    operator fun invoke(msgType: MessageTypes) = if (msgType == this.msgTypes) fn(msgType) else Unit
}

enum class MessageTypes {
    FOCUS_SEARCHBAR, HIDE_ACTIVE_ELEMENT, NAVIGATE_FORWARD, REFRESH_PAGE, NAVIGATE_BACKWARDS;

    operator fun invoke(handler: (MessageTypes) -> Unit) = MessageObserver(this, handler)
}
