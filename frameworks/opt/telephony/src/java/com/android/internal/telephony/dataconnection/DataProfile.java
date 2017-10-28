/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2017. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver\'s
 * applicable license agreements with MediaTek Inc.
 */
/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.internal.telephony.dataconnection;

import android.telephony.ServiceState;
import android.text.TextUtils;

import com.android.internal.telephony.RILConstants;

public class DataProfile {

    protected static final int TYPE_COMMON = 0;
    protected static final int TYPE_3GPP = 1;
    protected static final int TYPE_3GPP2 = 2;

    //id of the data profile
    public final int profileId;
    //the APN to connect to
    public final String apn;
    //one of the PDP_type values in TS 27.007 section 10.1.1.
    //For example, "IP", "IPV6", "IPV4V6", or "PPP".
    public final String protocol;
    //authentication protocol used for this PDP context
    //(None: 0, PAP: 1, CHAP: 2, PAP&CHAP: 3)
    public final int authType;
    //the username for APN, or NULL
    public final String user;
    //the password for APN, or NULL
    public final String password;
    //the profile type, TYPE_COMMON, TYPE_3GPP, TYPE_3GPP2
    public final int type;
    //the period in seconds to limit the maximum connections
    public final int maxConnsTime;
    //the maximum connections during maxConnsTime
    public final int maxConns;
    //the required wait time in seconds after a successful UE initiated
    //disconnect of a given PDN connection before the device can send
    //a new PDN connection request for that given PDN
    public final int waitTime;
    //true to enable the profile, false to disable
    public final boolean enabled;
    //supported APN types bitmap. See RIL_ApnTypes for the value of each bit.
    public final int supportedApnTypesBitmap;
    //one of the PDP_type values in TS 27.007 section 10.1.1 used on roaming network.
    //For example, "IP", "IPV6", "IPV4V6", or "PPP".
    public final String roamingProtocol;
    //The bearer bitmap. See RIL_RadioAccessFamily for the value of each bit.
    public final int bearerBitmap;
    //maximum transmission unit (MTU) size in bytes
    public final int mtu;
    //the MVNO type: possible values are "imsi", "gid", "spn"
    public final String mvnoType;
    //MVNO match data. For example, SPN: A MOBILE, BEN NL, ...
    //IMSI: 302720x94, 2060188, ...
    //GID: 4E, 33, ...
    public final String mvnoMatchData;
    //indicating the data profile was sent to the modem through setDataProfile earlier.
    public final boolean modemCognitive;

    protected DataProfile(int profileId, String apn, String protocol, int authType,
                String user, String password, int type, int maxConnsTime, int maxConns,
                int waitTime, boolean enabled, int supportedApnTypesBitmap, String roamingProtocol,
                int bearerBitmap, int mtu, String mvnoType, String mvnoMatchData,
                boolean modemCognitive) {

        this.profileId = profileId;
        this.apn = apn;
        this.protocol = protocol;
        if (authType == -1) {
            authType = TextUtils.isEmpty(user) ? RILConstants.SETUP_DATA_AUTH_NONE
                    : RILConstants.SETUP_DATA_AUTH_PAP_CHAP;
        }
        this.authType = authType;
        this.user = user;
        this.password = password;
        this.type = type;
        this.maxConnsTime = maxConnsTime;
        this.maxConns = maxConns;
        this.waitTime = waitTime;
        this.enabled = enabled;

        this.supportedApnTypesBitmap = supportedApnTypesBitmap;
        this.roamingProtocol = roamingProtocol;
        this.bearerBitmap = bearerBitmap;
        this.mtu = mtu;
        this.mvnoType = mvnoType;
        this.mvnoMatchData = mvnoMatchData;
        this.modemCognitive = modemCognitive;
    }

    public DataProfile(ApnSetting apn) {
        this(apn, apn.profileId);
    }

    public DataProfile(ApnSetting apn, int profileId) {
        this(profileId, apn.apn, apn.protocol,
                apn.authType, apn.user, apn.password, apn.bearerBitmask == 0
                        ? TYPE_COMMON : (ServiceState.bearerBitmapHasCdma(apn.bearerBitmask)
                        ? TYPE_3GPP2 : TYPE_3GPP),
                apn.maxConnsTime, apn.maxConns, apn.waitTime, apn.carrierEnabled, apn.typesBitmap,
                apn.roamingProtocol, apn.bearerBitmask, apn.mtu, apn.mvnoType, apn.mvnoMatchData,
                apn.modemCognitive);
    }

    @Override
    public String toString() {
        return "DataProfile=" + profileId + "/" + apn + "/" + protocol + "/" + authType
                + "/" + user + "/" + password + "/" + type + "/" + maxConnsTime
                + "/" + maxConns + "/" + waitTime + "/" + enabled + "/" + supportedApnTypesBitmap
                + "/" + roamingProtocol + "/" + bearerBitmap + "/" + mtu + "/" + mvnoType + "/"
                + mvnoMatchData + "/" + modemCognitive;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DataProfile == false) return false;
        return (o == this || toString().equals(o.toString()));
    }
}
