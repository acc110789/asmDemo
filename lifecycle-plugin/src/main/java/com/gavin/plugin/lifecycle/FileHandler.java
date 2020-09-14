package com.gavin.plugin.lifecycle;

import java.io.File;
import java.io.IOException;

interface FileHandler {
    void handleFile(File file) throws IOException;
}
