package dev.ploiu.file_server_ui_new.extensions

/**
 * returns a ui-friendly representation of a collection's size. This returns either the exact number of items in the list, or "999+" if the count goes
 */
fun <T> Collection<T>.uiCount(): String {
    return if(this.size < 1000) {
        this.size.toString()
    } else if (this.size < 10_000) {
        (this.size / 1000f).toString().substring(0, 3).replace(".0", "") + "k+"
    } else {
        // assume size is less than 1 million because there will never be a folder with that many items
        (this.size / 1000).toString() + "k+"
    }
}
