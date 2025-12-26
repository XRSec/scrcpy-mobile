/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define TRACE_TAG TRANSPORT

#include "sysdeps.h"

#include "client/usb.h"

#include <memory>

#include "sysdeps.h"
#include "transport.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "adb.h"

#if defined(__APPLE__)
#define CHECK_PACKET_OVERFLOW 0
#else
#define CHECK_PACKET_OVERFLOW 1
#endif

// Call usb_read using a buffer having a multiple of usb_get_max_packet_size() bytes
// to avoid overflow. See http://libusb.sourceforge.net/api-1.0/packetoverflow.html.
static int UsbReadMessage(usb_handle* h, amessage* msg) {
    D("UsbReadMessage");
    return 0;
}

// Call usb_read using a buffer having a multiple of usb_get_max_packet_size() bytes
// to avoid overflow. See http://libusb.sourceforge.net/api-1.0/packetoverflow.html.
static int UsbReadPayload(usb_handle* h, apacket* p) {
    return 0;
}

static int remote_read(apacket* p, usb_handle* usb) {
    return 0;
}

UsbConnection::~UsbConnection() {
}

bool UsbConnection::Read(apacket* packet) {
    return false;
}

bool UsbConnection::Write(apacket* packet) {
    return false;
}

bool UsbConnection::DoTlsHandshake(RSA* key, std::string* auth_key) {
    // TODO: support TLS for usb connections
    LOG(FATAL) << "Not supported yet.";
    return false;
}

void UsbConnection::Reset() {
}

void UsbConnection::Close() {
}

void init_usb_transport(atransport* t, usb_handle* h) {
    D("transport: usb");
}

bool is_adb_interface(int usb_class, int usb_subclass, int usb_protocol) {
    return false;
}

bool is_libusb_enabled() {
    return false;
}
