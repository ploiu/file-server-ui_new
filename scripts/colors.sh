# utility functions for color output
function redFg() {
    echo -e "\033[31m$1\033[0m"
}

function grayFg() {
    echo -e "\033[90m$1\033[0m"
}

function cyanFg() {
    echo -e "\033[36m$1\033[0m"
}

function yellowFg() {
    echo -e "\033[33m$1\033[0m"
}

function greenFg() {
    echo -e "\033[32m$1\033[0m"
}

function magentaFg() {
    echo -e "\033[35m$1\033[0m"
}
