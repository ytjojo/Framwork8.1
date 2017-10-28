/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "shared.rsh"

// Testing unsigned types for Bug 6764163
unsigned int ui = 37;
unsigned char uc = 5;

static bool test_unsigned() {
    bool failed = false;

    rsDebug("ui", ui);
    rsDebug("uc", uc);
    _RS_ASSERT(ui == 0x7fffffff);
    _RS_ASSERT(uc == 129);

    if (failed) {
        rsDebug("test_unsigned FAILED", -1);
    }
    else {
        rsDebug("test_unsigned PASSED", 0);
    }

    return failed;
}

void unsigned_test() {
    bool failed = false;
    failed |= test_unsigned();

    if (failed) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    }
    else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

